package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.location.LocationPreferences
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.example.dutype.models.JobVacancyStatus
import com.example.dutype.services.JobApplicationService
import com.example.dutype.state.AppStateManager
import com.example.dutype.state.ApplicationStateManager
import com.example.dutype.utils.GeoUtils
import com.example.dutype.utils.LocationService
import com.example.dutype.utils.toJobListing
import com.example.dutype.worker.data.HomeJobsSource
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Consolidated UI state for WorkerHomeScreen (single immutable object, one recomposition source).
 *
 * [jobs] is the nearest-first home list (instant/urgent jobs included). [urgentJobs] is the
 * dedicated instant/urgent section (urgency == HIGH), also nearest-first.
 */
data class WorkerHomeUiState(
    // Job data
    val jobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasError: Boolean = false,

    // Notifications
    val unreadNotificationCount: Int = 0,

    // Location state
    val isLocationLoading: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val locationFetchInProgress: Boolean = false,

    // Permission state
    val hasNotificationPermission: Boolean = false,
    val permissionsRequested: Boolean = false,
    val isFirstTimeUser: Boolean = true,

    // Bottom sheets
    val showNotificationBottomSheet: Boolean = false,
    val bottomSheetsShownInSession: Boolean = false,

    // Birthday
    val birthdayInfo: com.example.dutype.services.BirthdayInfo? = null,
    val showBirthdayBanner: Boolean = false,

    // Recently hired workers (social proof)
    val recentHires: List<com.example.dutype.models.RecentHire> = emptyList(),

    // Vacancy statuses folded into UiState for atomic mutation.
    val jobVacancyStatuses: Map<String, JobVacancyStatus> = emptyMap(),

    // Job interaction
    val clickedJobId: String? = null,

    // Instant / urgent jobs (urgency == HIGH), nearest-first
    val urgentJobs: List<JobListing> = emptyList(),
    val isUrgentLoading: Boolean = false,
    val isShowingCachedJobs: Boolean = false
)

/**
 * WorkerHomeViewModel: fast home feed.
 *
 * Load strategy (see [HomeJobsSource]):
 *  1. Last known coordinates are used immediately (any age) - no waiting for a GPS fix.
 *  2. Cached jobs (memory / Room) are shown instantly, ranked by distance.
 *  3. In PARALLEL: nearest-jobs stream (small page, radius 5/10/25/50 km widened only when
 *     too few) and instant/urgent jobs query. Each publishes independently.
 *  4. A newer location / refresh cancels the previous requests (generation guard).
 * Applied-jobs sync runs in the background and never blocks the first paint.
 */
@HiltViewModel
class WorkerHomeViewModel @Inject constructor(
    private val jobApplicationService: JobApplicationService,
    private val homeJobsSource: HomeJobsSource,
    private val appStateManager: AppStateManager,
    private val applicationStateManager: ApplicationStateManager,
    private val auth: FirebaseAuth,
    val locationService: LocationService,
    val locationPreferences: LocationPreferences,
    val savedWorkLocationsStore: com.example.dutype.services.SavedWorkLocationsStore
) : ViewModel() {

    companion object {
        private const val HOME_PAGE_SIZE = 15
        private const val MAX_HOME_JOBS = 30
        private const val URGENT_QUERY_LIMIT = 12
        private const val MAX_URGENT_ON_HOME = 6
        private const val HOME_RELOAD_COOLDOWN_MS = 30_000L
        private const val APPLIED_SYNC_COOLDOWN_MS = 60_000L
        private const val LOCATION_NOOP_KM = 0.05
        private const val RELOAD_DISTANCE_KM = 0.5
        private const val FAR_MOVE_KM = 30.0
    }

    private val _uiState = MutableStateFlow(WorkerHomeUiState())
    val uiState: StateFlow<WorkerHomeUiState> = _uiState.asStateFlow()

    // Public StateFlow preserved as derived slice of the single UI state.
    val jobVacancyStatuses: StateFlow<Map<String, JobVacancyStatus>> = _uiState
        .map { it.jobVacancyStatuses }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // Track which job IDs have had vacancy status loaded
    private val loadedVacancyJobIds = mutableSetOf<String>()

    // User location (last known is used immediately, refined later)
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0

    /** Prevents re-fetching GPS on every visit; reset when the ViewModel is recreated. */
    var locationFetchedInSession: Boolean = false

    // Request bookkeeping (all touched on the main thread only)
    private var hasRequestedLoad = false
    private var loadGeneration = 0
    private var nearestJob: Job? = null
    private var urgentJob: Job? = null
    private var appliedSyncJob: Job? = null
    private var lastAppliedSyncMs = 0L
    private var lastLoadAtMs = 0L
    private var loadedKey: String? = null
    private var inFlightKey: String? = null
    private var nearbyRaw: List<JobListingSummary> = emptyList()
    private var urgentRaw: List<JobListingSummary> = emptyList()
    private var nearestReady = false
    private var showingCache = false

    /**
     * Visible jobs: open, not applied, not filled. Order (nearest-first) is preserved.
     */
    val filteredJobs: StateFlow<List<JobListing>> = combine(
        _uiState,
        applicationStateManager.appliedJobIds,
        appStateManager.savedJobIds
    ) { state, appliedIds, savedIds ->
        visibleJobs(state.jobs, state.jobVacancyStatuses, appliedIds, savedIds)
    }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    /** Visible instant / urgent jobs (same filtering as [filteredJobs]). */
    val filteredUrgentJobs: StateFlow<List<JobListing>> = combine(
        _uiState,
        applicationStateManager.appliedJobIds,
        appStateManager.savedJobIds
    ) { state, appliedIds, savedIds ->
        visibleJobs(state.urgentJobs, state.jobVacancyStatuses, appliedIds, savedIds)
    }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    init {
        // Last known coordinates, regardless of age, so the first query does not wait for GPS.
        val savedLocation = locationPreferences.getSavedLocation()
        if (savedLocation != null && savedLocation.hasValidCoordinates()) {
            userLatitude = savedLocation.latitude
            userLongitude = savedLocation.longitude
        }

        // Cheap observer: only re-ranks / reloads when the home feed has been requested.
        viewModelScope.launch {
            locationPreferences.currentLocation.collect { location ->
                if (location != null && location.hasValidCoordinates()) {
                    setUserLocation(location.latitude, location.longitude)
                }
            }
        }
    }

    // ==========================================
    // LOCATION
    // ==========================================

    private fun hasValidUserLocation(): Boolean =
        GeoUtils.hasValidCoordinates(userLatitude, userLongitude)

    private fun locationKey(): String =
        if (hasValidUserLocation()) {
            "${Math.round(userLatitude * 1000.0)}_${Math.round(userLongitude * 1000.0)}"
        } else {
            "none"
        }

    /**
     * Location update (GPS refine, saved location change). Reloads only when the worker moved
     * far enough to matter; small moves just re-rank the current list locally. Duplicate calls
     * with the same coordinates are ignored.
     */
    fun setUserLocation(latitude: Double, longitude: Double, immediate: Boolean = false) {
        if (!GeoUtils.hasValidCoordinates(latitude, longitude)) return
        val hadLocation = hasValidUserLocation()
        val movedKm = if (hadLocation) {
            GeoUtils.calculateHaversineDistance(userLatitude, userLongitude, latitude, longitude)
        } else {
            Double.MAX_VALUE
        }

        if (hadLocation && movedKm < LOCATION_NOOP_KM) {
            if (immediate && hasRequestedLoad && _uiState.value.jobs.isEmpty() && nearestJob?.isActive != true) {
                loadJobsSummaryForHome()
            }
            return
        }

        userLatitude = latitude
        userLongitude = longitude
        if (!hasRequestedLoad) return

        if (!hadLocation || movedKm >= RELOAD_DISTANCE_KM) {
            if (hadLocation && movedKm >= FAR_MOVE_KM) {
                clearVisibleJobs()
            } else {
                rerankNow()
            }
            loadJobsSummaryForHome(forceRefresh = true)
        } else {
            rerankNow()
        }
    }

    /** Worker explicitly picked another location: drop the old area's jobs and reload. */
    fun onLocationChanged(latitude: Double, longitude: Double) {
        if (!GeoUtils.hasValidCoordinates(latitude, longitude)) return
        val hadLocation = hasValidUserLocation()
        val movedKm = if (hadLocation) {
            GeoUtils.calculateHaversineDistance(userLatitude, userLongitude, latitude, longitude)
        } else {
            Double.MAX_VALUE
        }
        userLatitude = latitude
        userLongitude = longitude
        hasRequestedLoad = true
        if (hadLocation && movedKm >= LOCATION_NOOP_KM) {
            clearVisibleJobs()
        }
        loadJobsSummaryForHome(forceRefresh = true)
    }

    private fun clearVisibleJobs() {
        nearbyRaw = emptyList()
        urgentRaw = emptyList()
        nearestReady = false
        showingCache = false
        _uiState.update { it.copy(jobs = emptyList(), urgentJobs = emptyList(), isLoading = true) }
    }

    private fun rerankNow() {
        if (nearbyRaw.isEmpty() && urgentRaw.isEmpty()) return
        val generation = loadGeneration
        viewModelScope.launch { publish(generation, loadingDone = false) }
    }

    // ==========================================
    // JOB LOADING
    // ==========================================

    /**
     * Load home jobs. Cheap to call repeatedly: an identical in-flight request or a fresh
     * (< 30 s) result for the same location is reused.
     */
    fun loadJobsSummaryForHome(forceRefresh: Boolean = false) {
        hasRequestedLoad = true
        val key = locationKey()
        if (!forceRefresh) {
            if (nearestJob?.isActive == true && inFlightKey == key) return
            val state = _uiState.value
            val fresh = state.jobs.isNotEmpty() && !state.hasError && key == loadedKey &&
                (System.currentTimeMillis() - lastLoadAtMs) < HOME_RELOAD_COOLDOWN_MS
            if (fresh) {
                if (state.isLoading) _uiState.update { it.copy(isLoading = false) }
                return
            }
        }
        startLoad(showRefreshing = forceRefresh && _uiState.value.jobs.isNotEmpty())
    }

    /**
     * Pull-to-refresh: previous jobs stay visible; nearest + urgent are re-fetched in parallel.
     */
    fun refreshJobs() {
        hasRequestedLoad = true
        _uiState.update { it.copy(jobVacancyStatuses = emptyMap()) }
        loadedVacancyJobIds.clear()
        startLoad(showRefreshing = true)
    }

    private fun startLoad(showRefreshing: Boolean) {
        loadGeneration += 1
        val generation = loadGeneration
        nearestJob?.cancel()
        urgentJob?.cancel()

        val hasLocation = hasValidUserLocation()
        val latitude = userLatitude
        val longitude = userLongitude
        val key = locationKey()
        loadedKey = key
        inFlightKey = key
        nearestReady = false

        _uiState.update {
            it.copy(
                isLoading = it.jobs.isEmpty(),
                isRefreshing = showRefreshing,
                isUrgentLoading = true,
                hasError = false,
                error = null
            )
        }

        primeAppliedJobs()

        // Two independent requests, run in parallel; each publishes on its own.
        nearestJob = viewModelScope.launch { loadNearest(generation, latitude, longitude, hasLocation) }
        urgentJob = viewModelScope.launch { loadUrgent(generation, latitude, longitude, hasLocation) }
    }

    private suspend fun loadNearest(generation: Int, latitude: Double, longitude: Double, hasLocation: Boolean) {
        try {
            // 1) Cache first: paint instantly if nothing is visible yet.
            if (_uiState.value.jobs.isEmpty()) {
                val cached = homeJobsSource.cachedNearby(
                    if (hasLocation) latitude else null,
                    if (hasLocation) longitude else null,
                    HOME_PAGE_SIZE
                )
                if (generation == loadGeneration && cached.isNotEmpty() && _uiState.value.jobs.isEmpty()) {
                    nearbyRaw = cached
                    showingCache = true
                    publish(generation, loadingDone = true)
                }
            }

            // 2) Fresh page from the server (small page, stepped radius).
            val batches = if (hasLocation) {
                homeJobsSource.nearestStream(
                    latitude,
                    longitude,
                    HOME_PAGE_SIZE,
                    excludeIds = applicationStateManager.appliedJobIds.value
                )
            } else {
                homeJobsSource.latestStream(HOME_PAGE_SIZE)
            }
            batches.collect { batch ->
                if (generation != loadGeneration) return@collect
                // While cached jobs are on screen, ignore small intermediate batches (no shrink/grow flicker).
                val shouldApply = batch.isFinal || (batch.jobs.isNotEmpty() && (!showingCache || nearbyRaw.size < 5))
                if (!shouldApply) return@collect
                nearbyRaw = batch.jobs
                nearestReady = true
                showingCache = false
                publish(generation, loadingDone = true)
                if (batch.isFinal) {
                    val toCache = batch.jobs
                    viewModelScope.launch(Dispatchers.IO) { homeJobsSource.persist(toCache) }
                }
            }

            if (generation == loadGeneration) {
                lastLoadAtMs = System.currentTimeMillis()
                _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "WorkerHomeViewModel: nearest jobs failed")
            if (generation == loadGeneration) {
                nearestReady = true
                showingCache = false
                publish(generation, loadingDone = true)
                _uiState.update { state ->
                    val nothingToShow = state.jobs.isEmpty() && state.urgentJobs.isEmpty()
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        hasError = nothingToShow,
                        error = if (nothingToShow) (e.message ?: "Failed to load jobs") else null
                    )
                }
            }
        }
    }

    private suspend fun loadUrgent(generation: Int, latitude: Double, longitude: Double, hasLocation: Boolean) {
        try {
            val urgent = homeJobsSource.fetchUrgent(
                if (hasLocation) latitude else null,
                if (hasLocation) longitude else null,
                URGENT_QUERY_LIMIT
            )
            if (generation != loadGeneration) return
            urgentRaw = urgent
            publish(generation, loadingDone = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "WorkerHomeViewModel: urgent jobs failed")
        } finally {
            if (generation == loadGeneration) {
                _uiState.update { it.copy(isUrgentLoading = false) }
            }
        }
    }

    /**
     * Merge raw pages into the two UI lists on a background dispatcher, then publish atomically.
     * Urgent jobs join the main list only after the first nearest batch arrived (no list jump).
     */
    private suspend fun publish(generation: Int, loadingDone: Boolean) {
        val latitude = userLatitude
        val longitude = userLongitude
        val hasLocation = hasValidUserLocation()
        val nearby = nearbyRaw
        val urgent = urgentRaw
        val includeUrgentInList = nearestReady
        val cached = showingCache

        val lists = withContext(Dispatchers.Default) {
            buildHomeLists(nearby, urgent, includeUrgentInList, latitude, longitude, hasLocation)
        }
        if (generation != loadGeneration) return
        _uiState.update {
            it.copy(
                jobs = lists.first,
                urgentJobs = lists.second,
                isShowingCachedJobs = cached,
                isLoading = if (loadingDone) false else it.isLoading,
                isRefreshing = if (loadingDone) false else it.isRefreshing
            )
        }
    }

    private fun buildHomeLists(
        nearby: List<JobListingSummary>,
        urgent: List<JobListingSummary>,
        includeUrgentInList: Boolean,
        latitude: Double,
        longitude: Double,
        hasLocation: Boolean
    ): Pair<List<JobListing>, List<JobListing>> {
        val listPool = if (includeUrgentInList) nearby + urgent else nearby
        val jobs = rank(listPool.distinctBy { it.id }, latitude, longitude, hasLocation)
            .take(MAX_HOME_JOBS)
            .map { it.toJobListing() }

        val urgentPool = (nearby + urgent)
            .filter { it.urgency.equals("HIGH", ignoreCase = true) }
            .distinctBy { it.id }
        val urgentJobs = rank(urgentPool, latitude, longitude, hasLocation)
            .take(MAX_URGENT_ON_HOME)
            .map { it.toJobListing() }
        return Pair(jobs, urgentJobs)
    }

    private fun rank(
        list: List<JobListingSummary>,
        latitude: Double,
        longitude: Double,
        hasLocation: Boolean
    ): List<JobListingSummary> {
        return if (hasLocation) {
            list.map { GeoUtils.attachDistanceToSummary(it, latitude, longitude) }
                .sortedBy { it.distance ?: Double.MAX_VALUE }
        } else {
            list.sortedByDescending { it.createdAt }
        }
    }

    private fun visibleJobs(
        jobs: List<JobListing>,
        vacancyStatuses: Map<String, JobVacancyStatus>,
        appliedIds: Set<String>,
        savedIds: Set<String>
    ): List<JobListing> {
        if (jobs.isEmpty()) return emptyList()
        return jobs
            .filter { it.status.equals("open", ignoreCase = true) }
            .filter { job ->
                val id = job.id.ifBlank { job.jobId }
                id !in appliedIds && vacancyStatuses[job.id] != JobVacancyStatus.FILLED
            }
            .map { job ->
                val id = job.id.ifBlank { job.jobId }
                if (!job.isSaved && id in savedIds) job.copy(isSaved = true) else job
            }
    }

    /** Applied-jobs sync in the background; never blocks the first paint. */
    private fun primeAppliedJobs() {
        val user = auth.currentUser ?: return
        if (user.isAnonymous) return
        if (appliedSyncJob?.isActive == true) return
        val now = System.currentTimeMillis()
        if (now - lastAppliedSyncMs < APPLIED_SYNC_COOLDOWN_MS) return
        lastAppliedSyncMs = now
        val uid = user.uid
        appliedSyncJob = viewModelScope.launch {
            jobApplicationService.getAppliedJobIds(uid)
                .onSuccess { appStateManager.setAppliedJobIds(it) }
                .onFailure { Timber.w(it, "WorkerHomeViewModel: applied jobs sync failed") }
        }
    }

    // ==========================================
    // RECENTLY HIRED FEED (Social Proof)
    // ==========================================

    /**
     * Load recently hired workers for social proof.
     * Uses dummy data only (no Firestore query) so home load stays light.
     */
    fun loadRecentlyHired() {
        val dummyHires = listOf(
            com.example.dutype.models.RecentHire(
                workerName = "Gopi B.",
                jobTitle = "Delivery Partner",
                timeAgo = "2 hours ago",
                acceptedAt = System.currentTimeMillis() - (2 * 60 * 60 * 1000)
            ),
            com.example.dutype.models.RecentHire(
                workerName = "Ravi Kumar.",
                jobTitle = "Shop Helper",
                timeAgo = "5 hours ago",
                acceptedAt = System.currentTimeMillis() - (5 * 60 * 60 * 1000)
            ),
            com.example.dutype.models.RecentHire(
                workerName = "Sai D.",
                jobTitle = "Kitchen Helper",
                timeAgo = "1 day ago",
                acceptedAt = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            )
        )
        _uiState.update { it.copy(recentHires = dummyHires) }
    }

    // ==========================================
    // VACANCY STATUS
    // ==========================================

    fun getUnloadedVacancyJobIds(jobIds: List<String>): List<String> {
        return jobIds.filter { !loadedVacancyJobIds.contains(it) }
    }

    fun markVacancyJobIdsAsLoaded(jobIds: List<String>) {
        loadedVacancyJobIds.addAll(jobIds)
    }

    fun updateVacancyStatuses(statuses: Map<String, JobVacancyStatus>) {
        _uiState.update { current ->
            current.copy(jobVacancyStatuses = current.jobVacancyStatuses + statuses)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, hasError = false) }
    }
}
