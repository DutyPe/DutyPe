package com.example.dutype.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.jobs.Geohash
import com.example.dutype.jobs.JobQuery
import com.example.dutype.jobs.JobRepository
import com.example.dutype.jobs.JobFeedPager
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.example.dutype.state.AppStateManager
import com.example.dutype.utils.GeoUtils
import com.example.dutype.utils.JobCategoryResolver
import com.example.dutype.utils.JobQueryCorrector
import com.example.dutype.utils.SalaryFormatter
import com.example.dutype.utils.toJobListing
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import timber.log.Timber
import javax.inject.Inject

private const val PAGE_SIZE = 10L

/**
 * Find Jobs filters. [category], [urgentOnly] and [maxDistance] are applied by the server query;
 * the rest are matched on the loaded job cards.
 * Values are schema values ([com.example.dutype.firestore.FirestoreSchema.Values]) or "Any".
 */
data class JobFilters(
    /** Minimum monthly-equivalent pay in rupees (0 = any). */
    val payMin: Int = 0,
    val maxDistance: Float? = null,
    val sortBy: String = SORT_RELEVANCE,
    val payType: String = ANY,
    val employmentType: String = ANY,
    val category: String = ANY,
    val shift: String = ANY,
    val urgentOnly: Boolean = false
) {
    companion object {
        const val ANY = "Any"
        const val SORT_RELEVANCE = "Relevance"
        const val SORT_NEWEST = "Newest"
        const val SORT_PAY_HIGH = "Pay: High to Low"
        const val SORT_DISTANCE = "Distance"
    }
}

data class AllJobsUiState(
    val jobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val hasError: Boolean = false,
    val hasMore: Boolean = true,
    val loadMoreFailed: Boolean = false,
    /** "All Jobs" or a category display name from the category rail. */
    val selectedChip: String = ALL_JOBS,
    val searchQuery: String = "",
    /** Spelling-corrected query actually searched ("diver" -> "driver"), null when unchanged. */
    val correctedQuery: String? = null,
    val filters: JobFilters = JobFilters()
)

private const val ALL_JOBS = "All Jobs"

/**
 * Find Jobs: nearest-first pages from [JobRepository.pager]. Every filter / search / location
 * change starts a fresh pager (a new generation), so late results from an old query are dropped.
 */
@HiltViewModel
class AllJobsViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val appStateManager: AppStateManager,
    private val workerApplicationsStore: com.example.dutype.applications.WorkerApplicationsStore,
    private val savedStateHandle: SavedStateHandle,
    private val auth: FirebaseAuth,
    val locationService: com.example.dutype.utils.LocationService,
    val locationPreferences: com.example.dutype.location.LocationPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AllJobsUiState(
            selectedChip = savedStateHandle.get<String>(KEY_SELECTED_CHIP) ?: ALL_JOBS,
            searchQuery = savedStateHandle.get<String>(KEY_SEARCH_QUERY).orEmpty()
        )
    )
    val uiState: StateFlow<AllJobsUiState> = _uiState.asStateFlow()

    val selectedChip: StateFlow<String> = slice { it.selectedChip }
    val searchQuery: StateFlow<String> = slice { it.searchQuery }
    val filters: StateFlow<JobFilters> = slice { it.filters }
    val correctedQuery: StateFlow<String?> = slice { it.correctedQuery }

    /** Loaded jobs minus applied ones, with the saved flag, in the chosen sort order. */
    val filteredJobs: StateFlow<List<JobListing>> = combine(
        _uiState, appStateManager.savedJobIds, appStateManager.appliedJobIds
    ) { state, savedIds, appliedIds ->
        val visible = state.jobs
            .filter { it.id !in appliedIds }
            .map { if (!it.isSaved && it.id in savedIds) it.copy(isSaved = true) else it }
        when (state.filters.sortBy) {
            JobFilters.SORT_NEWEST -> visible.sortedByDescending { it.createdAt }
            JobFilters.SORT_PAY_HIGH -> visible.sortedByDescending { SalaryFormatter.monthlyEquivalent(it.payAmount, it.payType) }
            JobFilters.SORT_DISTANCE -> visible.sortedBy { it.distance ?: Double.MAX_VALUE }
            else -> visible
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeFilterCount: StateFlow<Int> = filters.map { f ->
        listOf(
            f.payMin > 0, f.maxDistance != null, f.sortBy != JobFilters.SORT_RELEVANCE,
            f.payType != JobFilters.ANY, f.employmentType != JobFilters.ANY,
            f.category != JobFilters.ANY, f.shift != JobFilters.ANY, f.urgentOnly
        ).count { it }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private var userLatitude: Double? = null
    private var userLongitude: Double? = null
    private var pager: JobFeedPager? = null
    private var generation = 0
    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var loadedAtMs = 0L
    private var loadedKey: String? = null
    private var lastRouteFilter: String? = savedStateHandle.get<String>(KEY_ROUTE_FILTER)

    init {
        locationPreferences.getSavedLocation()?.takeIf { it.hasValidCoordinates() }?.let {
            userLatitude = it.latitude
            userLongitude = it.longitude
        }
        syncAppliedJobs()
        viewModelScope.launch {
            locationPreferences.currentLocation.collect { location ->
                if (location != null) setUserLocation(location.latitude, location.longitude)
            }
        }
    }

    // ───────────────────────────── screen events ─────────────────────────────

    /** Applies the category carried by the navigation route once per distinct value. */
    fun applyRouteFilter(filter: String) {
        if (filter == lastRouteFilter) return
        lastRouteFilter = filter
        savedStateHandle[KEY_ROUTE_FILTER] = filter
        if (filter != _uiState.value.selectedChip) {
            setChip(filter)
            loadedKey = null
        }
    }

    /** Category rail tap: reload with that category filtered on the server. */
    fun setCategoryAndReload(category: String) {
        val state = _uiState.value
        if (state.selectedChip == category && state.searchQuery.isBlank() && state.jobs.isNotEmpty() && !state.hasError) return
        searchJob?.cancel()
        setChip(category)
        setQuery("")
        startLoad(reset = true)
    }

    /** "View all jobs" from the empty state: clear category, search and filters. */
    fun showAllJobs() {
        searchJob?.cancel()
        setChip(ALL_JOBS)
        setQuery("")
        _uiState.update { it.copy(filters = JobFilters()) }
        startLoad(reset = true)
    }

    /** Search box. Waits for typing to pause, then reloads nearest-first with the query applied. */
    fun setSearchQuery(query: String) {
        setQuery(query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            startLoad(reset = true)
        }
    }

    fun setFilters(filters: JobFilters) {
        if (filters == _uiState.value.filters) return
        _uiState.update { it.copy(filters = filters) }
        startLoad(reset = true)
    }

    fun resetFilters() = setFilters(JobFilters())

    /** Manual location pick (search sheet / city chip): always reloads for the new place. */
    fun onLocationChanged(latitude: Double, longitude: Double) = setUserLocation(latitude, longitude, forceReload = true)

    /** A GPS refinement reloads only when the worker actually moved. */
    fun setUserLocation(latitude: Double, longitude: Double, forceReload: Boolean = false) {
        if (!GeoUtils.hasValidCoordinates(latitude, longitude)) return
        val lat = userLatitude
        val lng = userLongitude
        val movedKm = if (lat != null && lng != null) Geohash.distanceKm(lat, lng, latitude, longitude) else Double.MAX_VALUE
        if (!forceReload && movedKm < LOCATION_REFINE_KM) return
        userLatitude = latitude
        userLongitude = longitude
        if (loadedKey != null) startLoad(reset = forceReload)
    }

    /** Idempotent: returning to the tab keeps a fresh list and its scroll position. */
    fun ensureLoaded() {
        val fresh = System.currentTimeMillis() - loadedAtMs < FRESH_WINDOW_MS
        if (loadedKey == queryKey() && (loadJob?.isActive == true || fresh) && !_uiState.value.hasError) return
        startLoad(reset = _uiState.value.jobs.isEmpty())
    }

    fun retry() = startLoad(reset = true)

    fun refreshJobs() {
        if (_uiState.value.isRefreshing) return
        syncAppliedJobs()
        startLoad(reset = false, pull = true)
    }

    fun loadMoreJobs(limit: Long = PAGE_SIZE) {
        val state = _uiState.value
        val active = pager ?: return
        if (state.isLoading || state.isRefreshing || state.isLoadingMore || !state.hasMore || loadJob?.isActive == true) return
        val gen = generation
        _uiState.update { it.copy(isLoadingMore = true, loadMoreFailed = false) }
        loadJob = viewModelScope.launch {
            val result = active.nextPage(limit.toInt())
            if (gen != generation) return@launch
            result.onSuccess { page ->
                _uiState.update { current ->
                    val known = current.jobs.mapTo(HashSet()) { it.id }
                    current.copy(
                        jobs = current.jobs + page.jobs.filter { it.id !in known }.map { it.toJobListing() },
                        isLoadingMore = false,
                        hasMore = page.hasMore
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                Timber.w(error, "AllJobsVM: load more failed")
                _uiState.update { it.copy(isLoadingMore = false, loadMoreFailed = true) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null, hasError = false) }

    // ───────────────────────────── loading ─────────────────────────────

    private fun startLoad(reset: Boolean, pull: Boolean = false) {
        val gen = ++generation
        loadJob?.cancel()
        val query = buildQuery()
        val newPager = jobRepository.pager(userLatitude, userLongitude, query.first)
        pager = newPager
        loadedKey = queryKey()
        _uiState.update {
            it.copy(
                jobs = if (reset) emptyList() else it.jobs,
                isLoading = reset || it.jobs.isEmpty(),
                isRefreshing = pull,
                isLoadingMore = false,
                loadMoreFailed = false,
                hasMore = true,
                hasError = false,
                error = null,
                correctedQuery = query.second
            )
        }
        loadJob = viewModelScope.launch {
            val result = newPager.nextPage(PAGE_SIZE.toInt())
            if (gen != generation) return@launch
            result.onSuccess { page ->
                loadedAtMs = System.currentTimeMillis()
                _uiState.update {
                    it.copy(jobs = page.jobs.map { job -> job.toJobListing() }, isLoading = false, isRefreshing = false,
                        hasMore = page.hasMore)
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                Timber.w(error, "AllJobsVM: first page failed")
                loadedKey = null
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, hasError = it.jobs.isEmpty(),
                        error = error.message ?: "Failed to load jobs")
                }
            }
        }
    }

    /** Server filters + card predicate for the current chip, search and filters; plus the corrected query. */
    private fun buildQuery(): Pair<JobQuery, String?> {
        val state = _uiState.value
        val f = state.filters
        val typed = state.searchQuery.trim().takeIf { it.length >= 2 }
        val corrected = typed?.let { JobQueryCorrector.correct(it) }
        val searchText = (corrected ?: typed)?.lowercase()
        val searchCategory = searchText?.let { JobCategoryResolver.enumNameForDisplay(it) ?: JobCategoryResolver.inferCategory(it)?.name }
        val category = when {
            f.category != JobFilters.ANY -> f.category
            state.selectedChip != ALL_JOBS -> categoryKeyForChip(state.selectedChip)
            else -> searchCategory
        }
        val words = searchText?.let { com.example.dutype.jobs.searchWords(it) }.orEmpty()
        // A trade ("driver") filters by category; any other word is searched on the server by its
        // longest word, and the remaining words are checked on the loaded cards.
        val keyword = words.takeIf { searchCategory == null }?.maxByOrNull { it.length }
        val matches = { job: JobListingSummary ->
            (f.payMin <= 0 || SalaryFormatter.monthlyEquivalent(job.payAmount, job.payType) >= f.payMin) &&
                (f.payType == JobFilters.ANY || job.payType == f.payType) &&
                (f.employmentType == JobFilters.ANY || job.employmentType == f.employmentType) &&
                (f.shift == JobFilters.ANY || job.shift == f.shift) &&
                (words.isEmpty() || category == searchCategory && searchCategory != null ||
                    "${job.title} ${job.companyName} ${job.area} ${job.district} ${job.category.replace('_', ' ')}"
                        .lowercase().let { text -> words.all { it in text } })
        }
        return JobQuery(
            category = category,
            urgentOnly = f.urgentOnly,
            maxRadiusKm = f.maxDistance?.toDouble(),
            keyword = keyword,
            matches = matches
        ) to corrected
    }

    private fun categoryKeyForChip(chip: String): String? =
        JobCategory.entries.firstOrNull { it.displayName.equals(chip, true) || it.name.equals(chip, true) }?.name
            ?: JobCategoryResolver.enumNameForDisplay(chip)

    private fun queryKey(): String {
        val s = _uiState.value
        return "${s.selectedChip}|${s.searchQuery.trim()}|${s.filters}|${userLatitude?.let { "%.2f".format(it) }}|${userLongitude?.let { "%.2f".format(it) }}"
    }

    private fun setChip(chip: String) {
        _uiState.update { it.copy(selectedChip = chip) }
        savedStateHandle[KEY_SELECTED_CHIP] = chip
    }

    private fun setQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        savedStateHandle[KEY_SEARCH_QUERY] = query
    }

    private fun syncAppliedJobs() = workerApplicationsStore.start()

    private fun <T> slice(pick: (AllJobsUiState) -> T): StateFlow<T> =
        _uiState.map(pick).distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, pick(_uiState.value))

    private companion object {
        const val KEY_SELECTED_CHIP = "alljobs_selected_chip"
        const val KEY_ROUTE_FILTER = "alljobs_route_filter"
        const val KEY_SEARCH_QUERY = "alljobs_search_query"
        const val SEARCH_DEBOUNCE_MS = 400L
        const val LOCATION_REFINE_KM = 0.5
        const val FRESH_WINDOW_MS = 5 * 60 * 1000L
    }
}
