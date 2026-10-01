package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.jobs.Geohash
import com.example.dutype.jobs.JobQuery
import com.example.dutype.jobs.JobRepository
import com.example.dutype.location.LocationPreferences
import com.example.dutype.models.JobListing
import com.example.dutype.state.AppStateManager
import com.example.dutype.state.ApplicationStateManager
import com.example.dutype.utils.GeoUtils
import com.example.dutype.utils.LocationService
import com.example.dutype.utils.toJobListing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** Worker home: the nearest open jobs plus a separate urgent-jobs strip. */
data class WorkerHomeUiState(
    val jobs: List<JobListing> = emptyList(),
    val urgentJobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isUrgentLoading: Boolean = false,
    val error: String? = null,
    val hasError: Boolean = false
)

/**
 * Loads home jobs with [JobRepository.pager] — nearest-first over cells that have jobs, so
 * a home open costs a couple of cell-index reads plus the jobs actually shown.
 * Results are kept for [RELOAD_COOLDOWN_MS]; tab switches and small GPS refinements reuse them.
 */
@HiltViewModel
class WorkerHomeViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val placeRepository: com.example.dutype.jobs.PlaceRepository,
    private val profileStore: com.example.dutype.profile.CurrentProfileStore,
    private val workerApplicationsStore: com.example.dutype.applications.WorkerApplicationsStore,
    private val appStateManager: AppStateManager,
    private val applicationStateManager: ApplicationStateManager,
    val locationService: LocationService,
    val locationPreferences: LocationPreferences,
    val notificationRepository: com.example.dutype.notifications.NotificationRepository
) : ViewModel() {

    private companion object {
        const val HOME_PAGE_SIZE = 15
        const val URGENT_PAGE_SIZE = 6
        const val URGENT_RADIUS_KM = 20.0
        const val RELOAD_COOLDOWN_MS = 60_000L
        const val RELOAD_DISTANCE_KM = 0.5
    }

    private val _uiState = MutableStateFlow(WorkerHomeUiState())
    val uiState: StateFlow<WorkerHomeUiState> = _uiState.asStateFlow()

    private var userLatitude: Double? = null
    private var userLongitude: Double? = null

    /** Prevents re-fetching GPS on every visit; reset when the ViewModel is recreated. */
    var locationFetchedInSession: Boolean = false

    private var hasRequestedLoad = false
    private var loadJob: Job? = null
    private var urgentJob: Job? = null
    private var lastLoadAtMs = 0L

    /** Open jobs the worker has not applied to, with the saved flag applied. Order is kept. */
    val filteredJobs: StateFlow<List<JobListing>> = combine(
        _uiState, applicationStateManager.appliedJobIds, appStateManager.savedJobIds
    ) { state, appliedIds, savedIds -> visible(state.jobs, appliedIds, savedIds) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredUrgentJobs: StateFlow<List<JobListing>> = combine(
        _uiState, applicationStateManager.appliedJobIds, appStateManager.savedJobIds
    ) { state, appliedIds, savedIds -> visible(state.urgentJobs, appliedIds, savedIds) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // The switch follows the saved profile (it may have been changed on another phone).
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            ?.takeIf { !it.isAnonymous }
            ?.let { profileStore.start(com.example.dutype.firestore.FirestoreSchema.Values.Role.WORKER) }
        viewModelScope.launch {
            profileStore.worker.collect { worker -> worker?.let { placeRepository.syncOnline(it.available) } }
        }
        locationPreferences.getSavedLocation()?.takeIf { it.hasValidCoordinates() }?.let {
            userLatitude = it.latitude
            userLongitude = it.longitude
        }
        viewModelScope.launch {
            locationPreferences.currentLocation.collect { location ->
                if (location != null && location.hasValidCoordinates()) {
                    setUserLocation(location.latitude, location.longitude)
                }
            }
        }
    }

    /** GPS refinement / saved location change. Reloads only after a real move. */
    fun setUserLocation(latitude: Double, longitude: Double, immediate: Boolean = false) {
        if (!GeoUtils.hasValidCoordinates(latitude, longitude)) return
        val movedKm = movedKm(latitude, longitude)
        userLatitude = latitude
        userLongitude = longitude
        if (!hasRequestedLoad) return
        if (movedKm >= RELOAD_DISTANCE_KM || (immediate && _uiState.value.jobs.isEmpty() && loadJob?.isActive != true)) {
            loadJobsSummaryForHome(forceRefresh = true)
        }
    }

    /** The worker picked another location: drop the old area's jobs and reload. */
    fun onLocationChanged(latitude: Double, longitude: Double) {
        if (!GeoUtils.hasValidCoordinates(latitude, longitude)) return
        userLatitude = latitude
        userLongitude = longitude
        hasRequestedLoad = true
        _uiState.update { it.copy(jobs = emptyList(), urgentJobs = emptyList(), isLoading = true) }
        loadJobsSummaryForHome(forceRefresh = true)
    }

    /** Online switch: urgent job offers ring on this phone only while Online. */
    val isOnline: StateFlow<Boolean> = placeRepository.online
    private val _savingOnline = MutableStateFlow(false)
    val isSavingOnline: StateFlow<Boolean> = _savingOnline.asStateFlow()

    fun setOnline(online: Boolean, onError: (String) -> Unit = {}) {
        if (_savingOnline.value) return
        _savingOnline.value = true
        viewModelScope.launch {
            placeRepository.setOnline(online).onFailure { onError(it.message ?: "Could not change. Check your internet.") }
            _savingOnline.value = false
        }
    }

    /**
     * A real GPS fix (not a manually picked place): records the worker's current area — once per
     * ~5 km cell — so nearby employers can find them and urgent needs there reach this phone.
     */
    fun onGpsFix(latitude: Double, longitude: Double) {
        if (!GeoUtils.hasValidCoordinates(latitude, longitude)) return
        viewModelScope.launch { placeRepository.onWorkerLocation(latitude, longitude) }
    }

    /** Cheap to call on every resume: reuses a fresh result or an in-flight load. */
    fun loadJobsSummaryForHome(forceRefresh: Boolean = false) {
        hasRequestedLoad = true
        if (!forceRefresh) {
            if (loadJob?.isActive == true) return
            val state = _uiState.value
            if (state.jobs.isNotEmpty() && System.currentTimeMillis() - lastLoadAtMs < RELOAD_COOLDOWN_MS) return
        }
        startLoad(showRefreshing = false)
    }

    /** Pull to refresh: current jobs stay visible until the new ones arrive. */
    fun refreshJobs() {
        hasRequestedLoad = true
        startLoad(showRefreshing = true)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, hasError = false) }
    }

    private fun startLoad(showRefreshing: Boolean) {
        loadJob?.cancel()
        urgentJob?.cancel()
        val lat = userLatitude
        val lng = userLongitude
        primeAppliedJobs()
        _uiState.update {
            it.copy(isLoading = it.jobs.isEmpty(), isRefreshing = showRefreshing, isUrgentLoading = true,
                hasError = false, error = null)
        }
        val notApplied = { id: String -> id !in applicationStateManager.appliedJobIds.value }

        loadJob = viewModelScope.launch {
            jobRepository.pager(lat, lng, JobQuery(matches = { notApplied(it.id) }))
                .nextPage(HOME_PAGE_SIZE)
                .onSuccess { page ->
                    lastLoadAtMs = System.currentTimeMillis()
                    _uiState.update {
                        it.copy(jobs = page.jobs.map { job -> job.toJobListing() }, isLoading = false, isRefreshing = false)
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Timber.w(error, "WorkerHomeViewModel: nearest jobs failed")
                    _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false, hasError = it.jobs.isEmpty(),
                            error = if (it.jobs.isEmpty()) error.message ?: "Failed to load jobs" else null)
                    }
                }
        }
        urgentJob = viewModelScope.launch {
            val urgent = jobRepository
                .pager(lat, lng, JobQuery(urgentOnly = true, maxRadiusKm = URGENT_RADIUS_KM.takeIf { lat != null },
                    matches = { notApplied(it.id) }))
                .nextPage(URGENT_PAGE_SIZE)
                .onFailure { if (it is CancellationException) throw it; Timber.w(it, "WorkerHomeViewModel: urgent jobs failed") }
                .getOrNull()?.jobs.orEmpty()
            _uiState.update { it.copy(urgentJobs = urgent.map { job -> job.toJobListing() }, isUrgentLoading = false) }
        }
    }

    private fun movedKm(latitude: Double, longitude: Double): Double {
        val lat = userLatitude ?: return Double.MAX_VALUE
        val lng = userLongitude ?: return Double.MAX_VALUE
        return Geohash.distanceKm(lat, lng, latitude, longitude)
    }

    private fun visible(jobs: List<JobListing>, appliedIds: Set<String>, savedIds: Set<String>): List<JobListing> =
        jobs.filter { it.isOpen() && it.id !in appliedIds }
            .map { if (!it.isSaved && it.id in savedIds) it.copy(isSaved = true) else it }

    /** Applied jobs come from the session-wide applications listener (started once). */
    private fun primeAppliedJobs() = workerApplicationsStore.start()
}
