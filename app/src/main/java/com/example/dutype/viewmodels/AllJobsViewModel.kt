package com.example.dutype.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.JobListing
import com.example.dutype.repositories.NearbyJobsPager
import com.example.dutype.repositories.FirestoreJobRepository
import com.example.dutype.services.JobApplicationService
import com.example.dutype.state.AppStateManager
import com.example.dutype.utils.GeoUtils
import com.example.dutype.utils.JobCategoryResolver
import com.example.dutype.utils.toJobListing
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * PAGINATION SETTINGS (Industry Standard)
 * 
 * Based on research of major job platforms:
 * - LinkedIn: 10-15 jobs per page
 * - Indeed: 15 jobs per page
 * - Apna: 10-15 jobs per page
 * - WorkIndia: 15 jobs per page
 * 
 * Product requirement: use 10 jobs per page for smoother incremental loading.
 */
private const val PAGE_SIZE = 10L // Jobs per page
private const val MAX_JOBS_IN_MEMORY = 500 // LinkedIn's sliding window

/**
 * P0 PERFORMANCE FIX: Job filters data class
 * Moved from AllJobsScreen to ViewModel for proper state management
 */
data class JobFilters(
    val salaryMin: Int = 0,
    val salaryMax: Int = 100000,
    val maxDistance: Float? = null,
    val experienceLevel: String = "Any",
    val sortBy: String = "Relevance",
    val payType: String = "Any",
    val workType: String = "Any",
    val category: String = "Any",
    val shiftTiming: String = "Any",
    /** Only jobs the employer marked urgent (urgency == HIGH). Lives in the filter sheet. */
    val urgentOnly: Boolean = false
)

/**
 * UI State for AllJobsScreen.
 *
 * P2-1: Consolidates the previously separate `_selectedChip`, `_searchQuery`,
 * `_filters`, and `_initialCategory` MutableStateFlows into a single atomic
 * state container. The screen still observes derived StateFlows for each
 * field (see [AllJobsViewModel.selectedChip], [AllJobsViewModel.searchQuery],
 * [AllJobsViewModel.filters]) so call sites are unchanged, but every mutation
 * now routes through one `_uiState.update { it.copy(...) }` so updates are
 * atomic and recompositions are bounded by `distinctUntilChanged`.
 */
data class AllJobsUiState(
    val jobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val hasError: Boolean = false,
    val hasMore: Boolean = true,
    val loadMoreFailed: Boolean = false,
    /** True from the moment page 1 is requested until it lands (also during silent refreshes). */
    val isSyncing: Boolean = false,
    val lastDocumentId: String? = null, // CRITICAL FIX: Use document ID for pagination cursor
    val totalJobs: Int = 0,
    // P2-1: Filter inputs folded into the same UiState.
    val selectedChip: String = "All Jobs",
    val searchQuery: String = "",
    val searchResultsQuery: String = "",
    /** Spelling-corrected query actually searched ("diver" -> "driver"), null when unchanged. */
    val correctedQuery: String? = null,
    val filters: JobFilters = JobFilters(),
    val initialCategory: String? = null
)

private data class FilterPipelineInputs(
    val chip: String = "All Jobs",
    val query: String = "",
    val filters: JobFilters = JobFilters(),
    val initialCategory: String? = null,
    val savedJobIds: Set<String> = emptySet(),
    val appliedJobIds: Set<String> = emptySet()
)

/**
 * P0 PERFORMANCE FIX: AllJobsViewModel
 * 
 * Moves all filtering logic from Composable to ViewModel to prevent
 * excessive recomposition when filter state changes.
 * 
 * Before: filteredJobs computed in remember{} block - triggers recomposition
 * After: filteredJobs as StateFlow - computed outside composition
 * 
 * @author DutyPe Engineering Team
 * @since 2.4.0
 */

@OptIn(FlowPreview::class)
@HiltViewModel
class AllJobsViewModel @Inject constructor(
    private val firestoreJobRepository: FirestoreJobRepository,
    private val appStateManager: AppStateManager,
    private val jobApplicationService: JobApplicationService,
    private val savedStateHandle: SavedStateHandle,
    private val auth: FirebaseAuth,
    private val performanceTracker: com.example.dutype.performance.PerformanceTracker,
    val locationService: com.example.dutype.utils.LocationService,
    val locationPreferences: com.example.dutype.location.LocationPreferences
) : ViewModel() {

    private fun parseSalaryForSort(salary: String): Int = com.example.dutype.utils.SalaryFormatter.lowerBound(salary).toInt()

    private fun normalizeCategoryToken(value: String?): String {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return ""

        return JobCategoryResolver.enumNameForDisplay(raw) ?: raw.uppercase().replace(' ', '_')
    }

    private fun matchesCategoryByKeywords(job: JobListing, categoryToken: String): Boolean {
        val text = (job.title + " " + job.description).lowercase()
        return JobCategoryResolver.matchesCategory(job.title, job.description, categoryToken) ||
            JobCategoryResolver.displayNameForName(categoryToken).lowercase().let { it.isNotBlank() && text.contains(it) }
    }

    private fun matchesCategoryFilter(job: JobListing, categoryValue: String): Boolean {
        if (categoryValue.equals("Any", ignoreCase = true) || categoryValue.equals("All Jobs", ignoreCase = true)) {
            return true
        }
        val mappedCategory = categoryMapping[categoryValue] ?: categoryValue
        val selectedTokens = setOf(
            normalizeCategoryToken(categoryValue),
            normalizeCategoryToken(mappedCategory)
        ).filter { it.isNotBlank() }.toSet()
        val jobTypeToken = normalizeCategoryToken(job.jobType)
        val detectedToken = normalizeCategoryToken(job.getCategory())
        val keywordMatch = selectedTokens.any { token -> matchesCategoryByKeywords(job, token) }
        return jobTypeToken in selectedTokens || detectedToken in selectedTokens || keywordMatch
    }

    private fun currentQueryCategory(): String? = _uiState.value.initialCategory?.takeIf { it != "All Jobs" }

    private fun shouldUseServerSideFiltering(filters: JobFilters): Boolean {
        return filters.salaryMin > 0 ||
            filters.salaryMax < 100000 ||
            filters.payType != "Any"
    }
    private fun shouldReloadForCurrentFilters(filters: JobFilters): Boolean {
        return shouldUseServerSideFiltering(filters) ||
            filters.experienceLevel != "Any" ||
            filters.workType != "Any" ||
            filters.category != "Any" ||
            filters.shiftTiming != "Any"
    }

    private fun matchesWorkType(job: JobListing, selectedWorkType: String): Boolean {
        if (selectedWorkType.equals("Any", ignoreCase = true)) return true

        val text = listOf(job.jobType, job.shiftTiming, job.title, job.description)
            .joinToString(" ")
            .lowercase()

        return when (selectedWorkType.lowercase()) {
            "part-time" -> listOf("part-time", "part time", "parttime", "weekend", "student").any(text::contains)
            "full-time" -> listOf("full-time", "full time", "fulltime").any(text::contains)
            "contract" -> text.contains("contract")
            "temporary" -> text.contains("temporary") || text.contains("temp")
            else -> text.contains(selectedWorkType.lowercase())
        }
    }

    private fun matchesShiftTiming(job: JobListing, selectedShiftTiming: String): Boolean {
        if (selectedShiftTiming.equals("Any", ignoreCase = true)) return true

        val text = listOf(job.shiftTiming, job.description, job.title)
            .joinToString(" ")
            .lowercase()

        return when (selectedShiftTiming.lowercase()) {
            "morning" -> listOf("morning", "day shift").any(text::contains)
            "afternoon" -> text.contains("afternoon")
            "evening" -> text.contains("evening")
            "night" -> listOf("night", "graveyard").any(text::contains)
            "flexible" -> text.contains("flexible")
            else -> text.contains(selectedShiftTiming.lowercase())
        }
    }

    private fun matchesExperienceLevel(job: JobListing, selectedLevel: String): Boolean {
        if (selectedLevel.equals("Any", ignoreCase = true)) return true

        val text = listOf(job.experienceRequired, job.description, job.title)
            .joinToString(" ")
            .lowercase()

        return when (selectedLevel) {
            "Fresher" -> listOf("no experience", "fresher", "entry", "0 year", "0-", "0 ").any(text::contains)
            "1-3 years" -> Regex("1\\s*(to|-)?\\s*3|1 year|2 year|3 year").containsMatchIn(text)
            "3-5 years" -> Regex("3\\s*(to|-)?\\s*5|3 year|4 year|5 year").containsMatchIn(text)
            "5+ years" -> Regex("5\\+|5 year|6 year|7 year|8 year|9 year|10 year").containsMatchIn(text)
            else -> true
        }
    }
    
    // P2-1: Single source of truth. Initial values seeded from SavedStateHandle
    // (P2-2) so process death + recreate restores the user's filter context.
    private val _uiState = MutableStateFlow(
        AllJobsUiState(
            selectedChip = savedStateHandle.get<String>(KEY_SELECTED_CHIP) ?: "All Jobs",
            searchQuery = savedStateHandle.get<String>(KEY_SEARCH_QUERY).orEmpty(),
            filters = restoreFiltersFromSavedState(),
            initialCategory = savedStateHandle.get<String>(KEY_INITIAL_CATEGORY)
        )
    )
    val uiState: StateFlow<AllJobsUiState> = _uiState.asStateFlow()
    
    // Derived StateFlows so existing screen call sites (`viewModel.selectedChip`,
    // `viewModel.searchQuery`, `viewModel.filters`) keep working. distinctUntilChanged
    // ensures collectors recompose only when their slice actually changes.
    val selectedChip: StateFlow<String> = _uiState
        .map { it.selectedChip }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.selectedChip)
    
    val searchQuery: StateFlow<String> = _uiState
        .map { it.searchQuery }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.searchQuery)
    
    val filters: StateFlow<JobFilters> = _uiState
        .map { it.filters }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.filters)
    
    // User location for distance calculation
    private var userLatitude: Double = savedStateHandle.get<Double>("userLatitude") ?: 0.0
    private var userLongitude: Double = savedStateHandle.get<Double>("userLongitude") ?: 0.0

    // Freeze query mode for a pagination session so cursor semantics stay consistent.
    // If first page loads without location, load-more continues non-geo until refresh/reload.
    private var paginationUsesLocation: Boolean = false
    
    // Guard to prevent duplicate loads
    private var hasInitiallyLoaded = false
    
    // Debounce search queries to avoid excessive database calls
    private var searchJob: kotlinx.coroutines.Job? = null
    
    // Category mapping from UI names to Firestore category values
    private val categoryMapping = com.example.dutype.employer.models.JobCategory.entries
        .associate { it.displayName to it.name }
        .plus(
            mapOf(
                "Housekeeping" to "MAID",
                "Construction" to "HELPER",
                "Events" to "WAITER",
                "Kitchen" to "COOK"
            )
        )
    
    // SENIOR FIX: Debounced search query — delays recomputation only on user input, not on initial load
    // Lazy initialization ensures debounce is only applied when user actually searches
    private val _debouncedSearchQuery = searchQuery
        .debounce(300)
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    
    // PRODUCTION PATTERN: Use debounced query in filter pipeline to avoid excessive recomputation
    // Benefits: Reduces database queries during user typing (300ms debounce)
    //          Reduces filter pipeline recomputation from 1+N to 1 per user pause
    private val debouncedSearchQuery: StateFlow<String> = _debouncedSearchQuery

    /** "Showing results for …" hint when the search words were spelling-corrected. */
    val correctedQuery: StateFlow<String?> = _uiState
        .map { it.correctedQuery }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private fun normalizedJobId(job: JobListing): String = job.id.ifBlank { job.jobId }

    private fun hasValidUserLocation(): Boolean = GeoUtils.hasValidCoordinates(userLatitude, userLongitude)

    private fun queryUserLatitude(): Double? = userLatitude.takeIf { hasValidUserLocation() }

    private fun queryUserLongitude(): Double? = userLongitude.takeIf { hasValidUserLocation() }

    private fun applyRuntimeFlags(jobs: List<JobListing>): List<JobListing> {
        val savedJobIds = appStateManager.savedJobIds.value
        val appliedJobIds = appStateManager.appliedJobIds.value
        return jobs
            .map { job ->
                val normalizedId = normalizedJobId(job)
                job.copy(isSaved = job.isSaved || normalizedId in savedJobIds)
            }
            .filterNot { job -> normalizedJobId(job) in appliedJobIds }
    }

    private fun syncAppliedJobsFromBackend() {
        val currentUserId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            jobApplicationService.getAppliedJobIds(currentUserId)
                .onSuccess { appliedJobIds ->
                    appStateManager.setAppliedJobIds(appliedJobIds)
                }
                .onFailure { exception ->
                    Timber.w(exception, "AllJobsVM: Failed to prime applied jobs state")
                }
        }
    }

    private val filterPipelineInputs: StateFlow<FilterPipelineInputs> = combine(
        selectedChip,
        debouncedSearchQuery,
        filters,
        _uiState.map { it.initialCategory }.distinctUntilChanged(),
        combine(appStateManager.savedJobIds, appStateManager.appliedJobIds) { savedJobIds, appliedJobIds ->
            savedJobIds to appliedJobIds
        }
    ) { chip, query, filters, initialCategory, runtimeState ->
        FilterPipelineInputs(
            chip = chip,
            query = query,
            filters = filters,
            initialCategory = initialCategory,
            savedJobIds = runtimeState.first,
            appliedJobIds = runtimeState.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = FilterPipelineInputs()
    )

    /**
     * P0 PERFORMANCE FIX: Filtered jobs computed as StateFlow
     * 
     * Combines all filter inputs and computes filtered list outside of Compose.
     * This prevents recomposition storms when any filter changes.
     * 
     * Filter pipeline:
     * 1. Remove filled jobs
     * 2. Remove expired jobs
     * 3. Apply category filter (if initial filter is a category)
     * 4. Apply chip filter (All Jobs, Daily, Hourly, etc.)
     * 5. Apply advanced filters (salary, distance, experience, gender)
     * 6. Apply search query (debounced to 300ms)
     * 7. Apply sorting
     */
    val filteredJobs: StateFlow<List<JobListing>> = combine(
        _uiState,
        filterPipelineInputs
    ) { state, inputs ->
        val chip = inputs.chip
        val query = inputs.query
        val normalizedSearchQuery = query.trim()
        val filters = inputs.filters
        val initialCategory = inputs.initialCategory
        val savedJobIds = inputs.savedJobIds
        val appliedJobIds = inputs.appliedJobIds

        // CRITICAL FIX: Don't return empty during loading if jobs already exist in state
        if ((state.isLoading && state.jobs.isEmpty()) || (state.hasError && state.jobs.isEmpty())) {
            Timber.d("🔍 filteredJobs: Loading/error with empty jobs - returning empty for shimmer")
            return@combine emptyList()
        }
        
        // If not loading and no jobs, return empty
        if (state.jobs.isEmpty()) {
            Timber.d("🔍 filteredJobs: No jobs loaded")
            return@combine emptyList()
        }

        if (normalizedSearchQuery.length >= 2 && state.searchResultsQuery != normalizedSearchQuery) {
            Timber.d("🔍 filteredJobs: Waiting for database search results for query='$normalizedSearchQuery'")
            return@combine emptyList()
        }
        
        Timber.d("🔍 filteredJobs: Starting filter pipeline with ${state.jobs.size} jobs")
        Timber.d("🔍 filteredJobs: selectedChip='$chip', searchQuery='$query', initialCategory='$initialCategory'")
        Timber.d("🔍 filteredJobs: filters - salaryMin=${filters.salaryMin}, salaryMax=${filters.salaryMax}, maxDistance=${filters.maxDistance}")
        
        val isCategory = initialCategory != null && categoryMapping.containsKey(initialCategory)
        
        // Step 1: Filter out closed/expired jobs
        val availableJobs = state.jobs
            .map { job ->
                val normalizedId = normalizedJobId(job)
                job.copy(isSaved = job.isSaved || normalizedId in savedJobIds)
            }
            .filter { it.status == "open" }
            .filterNot { job -> normalizedJobId(job) in appliedJobIds }
        
        // ADD DEBUG: Log jobs filtered out by status
        val filteredByStatus = state.jobs.filter { it.status != "open" }
        if (filteredByStatus.isNotEmpty()) {
            Timber.w("🔍 STATUS FILTER REMOVED: ${filteredByStatus.size} jobs")
            filteredByStatus.take(3).forEach {
                Timber.w("🔍   - ${it.title} (status='${it.status}')")
            }
        }
        
        Timber.d("🔍 filteredJobs: After status filter: ${availableJobs.size} jobs")

        // Step 2: Keep expiry filtering server-side.
        val activeJobs = availableJobs

        if (activeJobs.isNotEmpty()) {
            Timber.d("🔍 filteredJobs: Sample jobs:")
            activeJobs.take(3).forEach { job ->
                Timber.d("🔍   - ${job.title}, salary=${job.salary}, salaryType=${job.salaryType}")
            }
        }
        
        // Step 3: The category rail is applied SERVER-SIDE (category == X on the query), so every
        // loaded job already belongs to it. Re-filtering here with title/description keyword
        // heuristics used to drop valid jobs and produced false "no jobs" states.
        val categoryFiltered = activeJobs

        val categoryOptionFiltered = if (filters.category != "Any") {
            categoryFiltered.filter { matchesCategoryFilter(it, filters.category) }
        } else {
            categoryFiltered
        }

        // Step 4: Apply chip filter
        val chipFiltered = when (chip) {
            "All Jobs" -> categoryOptionFiltered
            "Daily Jobs" -> categoryOptionFiltered.filter {
                it.salaryType.equals("DAILY", true)
            }
            "Hourly Jobs" -> categoryOptionFiltered.filter {
                it.salaryType.equals("HOURLY", true)
            }
            "Nearby" -> categoryOptionFiltered.sortedWith(
                compareBy<JobListing> { it.distance == null }
                    .thenBy { it.distance ?: Double.MAX_VALUE }
            )
            "Part Time" -> categoryOptionFiltered.filter { matchesWorkType(it, "Part-time") }
            "Full Time" -> categoryOptionFiltered.filter { matchesWorkType(it, "Full-time") }
            else -> categoryOptionFiltered
        }
        
        Timber.d("🔍 filteredJobs: After chip filter ($chip): ${chipFiltered.size} jobs (was ${categoryFiltered.size})")
        
        // Step 5: Apply advanced filters
        val advancedFiltered = chipFiltered.filter { job ->
            // Salary filter — Bug #6: salary is a free-form String now.
            val lower = com.example.dutype.utils.SalaryFormatter.lowerBound(job.salary)
            val upper = com.example.dutype.utils.SalaryFormatter.upperBound(job.salary)
            val jobSalary = lower.toInt()
            val hasSalaryUpperBound = filters.salaryMax < 100000
            val salaryMatch = jobSalary == 0 || (
                jobSalary >= filters.salaryMin &&
                    (!hasSalaryUpperBound || (if (upper == Double.MAX_VALUE) lower.toInt() <= filters.salaryMax else upper.toInt() <= filters.salaryMax))
                )
            
            // Distance filter: If user explicitly picked a maxDistance (e.g. 5km/10km/25km), enforce it.
            // Otherwise, allow NearestJobsEngine's multi-tier proximity sorting to provide nearby or district/state fallback.
            val dist = job.distance
            val distanceMatch = if (filters.maxDistance != null) {
                dist != null && dist <= filters.maxDistance
            } else {
                true
            }
            val payTypeMatch = filters.payType == "Any" || job.salaryType.equals(filters.payType, ignoreCase = true)
            val workTypeMatch = matchesWorkType(job, filters.workType)
            val experienceMatch = matchesExperienceLevel(job, filters.experienceLevel)
            val shiftTimingMatch = matchesShiftTiming(job, filters.shiftTiming)
            val urgentMatch = !filters.urgentOnly || job.urgency.equals("HIGH", ignoreCase = true)

            salaryMatch && distanceMatch && payTypeMatch && workTypeMatch && experienceMatch && shiftTimingMatch && urgentMatch
        }
        
        Timber.d("🔍 filteredJobs: After advanced filters: ${advancedFiltered.size} jobs (was ${chipFiltered.size})")
        
        // INDUSTRY STANDARD: When search is active (2+ chars), use database search results
        // No client-side filtering - trust the database query
        val searchFiltered = if (normalizedSearchQuery.length >= 2) {
            // Database search active - results already filtered and sorted by relevance
            Timber.d("🔍 filteredJobs: Using database search results for query='$normalizedSearchQuery' (${advancedFiltered.size} jobs)")
            advancedFiltered
        } else {
            // No search - show all filtered jobs
            advancedFiltered
        }
        
        // Step 7: Apply sorting
        // CRITICAL: For "Relevance" (default), PRESERVE the order from _uiState.jobs.
        // Initial load is already sorted nearest-first. Pagination appends to end.
        // Re-sorting here would cause the list to jump/rearrange on every new page load.
        // Only re-sort when user explicitly chooses a different sort option.
        val sorted = when (filters.sortBy) {
            "Newest" -> searchFiltered.sortedByDescending { it.createdAt }
            "Salary: High to Low" -> searchFiltered.sortedByDescending { parseSalaryForSort(it.salary) }
            "Salary: Low to High" -> searchFiltered.sortedBy { parseSalaryForSort(it.salary) }
            "Distance" -> {
                // Pager order is already nearest-first; a stable sort keeps loaded jobs
                // (never drops far ones like the tiered engine would).
                searchFiltered.sortedBy { it.distance ?: Double.MAX_VALUE }
            }
            else -> {
                // "Relevance" and default: preserve existing order from ViewModel state
                // Jobs are already sorted nearest-first on initial load
                searchFiltered
            }
        }
        
        Timber.d("filteredJobs: FINAL STAGE - Sorting by '${filters.sortBy}'")
        Timber.d("filteredJobs: FINAL COUNT: ${sorted.size} jobs displayed to user")
        if (sorted.isEmpty()) {
            Timber.w("🔍 ⚠️  NO JOBS TO DISPLAY - Check filters above for culprit")
        }
        sorted
    }.flowOn(Dispatchers.Default)  // SENIOR OPTIMIZATION: Compute filter pipeline off main thread
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly, // FIXED: Start immediately, don't wait for subscribers
        initialValue = emptyList()
    )
    
    /**
     * Active filter count computed as StateFlow
     */
    val activeFilterCount: StateFlow<Int> = filters.map { f ->
        var count = 0
        if (f.salaryMin > 0 || f.salaryMax < 100000) count++
        if (f.maxDistance != null) count++
        if (f.experienceLevel != "Any") count++
        if (f.sortBy != "Relevance") count++
        if (f.payType != "Any") count++
        if (f.workType != "Any") count++
        if (f.category != "Any") count++
        if (f.shiftTiming != "Any") count++
        if (f.urgentOnly) count++
        count
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0
    )
    
    // ==========================================
    // PAGING ENGINE (nearest-first, ring based)
    // ==========================================

    private enum class LoadMode {
        /** New query (first open / category / location picked): show cache or shimmer. */
        RESET,
        /** Pull-to-refresh: keep the list on screen, show the pull indicator. */
        PULL,
        /** Background refresh (location refined, stale return to tab): keep the list, no spinner. */
        SILENT
    }

    private var pager: NearbyJobsPager? = null
    private var firstPageLoaded = false
    private var started = false
    private var generation = 0
    private var loadJob: kotlinx.coroutines.Job? = null
    private var moreJob: kotlinx.coroutines.Job? = null
    private var loadedKey: String? = null
    private var loadedAtMs = 0L
    private var lastRouteFilter: String? = savedStateHandle.get<String>(KEY_ROUTE_FILTER)

    init {
        val savedLoc = locationPreferences.getSavedLocationIfFresh()
        if (savedLoc != null && (userLatitude == 0.0 && userLongitude == 0.0)) {
            userLatitude = savedLoc.latitude
            userLongitude = savedLoc.longitude
            savedStateHandle["userLatitude"] = userLatitude
            savedStateHandle["userLongitude"] = userLongitude
        }
        syncAppliedJobsFromBackend()

        // Location refinement: only reacts when the user moved more than ~500 m
        // (or a location arrives for the first time) and never wipes the visible list.
        viewModelScope.launch {
            locationPreferences.currentLocation.collect { newLocation ->
                if (newLocation != null) {
                    setUserLocation(newLocation.latitude, newLocation.longitude)
                }
            }
        }
    }

    private fun firestoreCategoryForQuery(): String? {
        val category = _uiState.value.initialCategory ?: return null
        if (category == "All Jobs") return null
        return categoryMapping[category]
    }

    private fun isSearchActive(): Boolean = _uiState.value.searchQuery.trim().length >= 2

    private fun queryKey(): String {
        val latKey = if (hasValidUserLocation()) (userLatitude * 100.0).roundToLong() else 0L
        val lngKey = if (hasValidUserLocation()) (userLongitude * 100.0).roundToLong() else 0L
        return "${firestoreCategoryForQuery()}|$latKey|$lngKey|${_uiState.value.filters.maxDistance}"
    }

    /**
     * Starts (or restarts) the nearest-first pager and loads page 1.
     * Every call bumps [generation] and cancels in-flight work, so a slow response for an
     * old category / location / filter can never overwrite newer state.
     */
    private fun startLoad(mode: LoadMode) {
        val gen = ++generation
        loadJob?.cancel()
        moreJob?.cancel()
        started = true

        val newPager = NearbyJobsPager(
            repository = firestoreJobRepository,
            userLatitude = queryUserLatitude(),
            userLongitude = queryUserLongitude(),
            category = firestoreCategoryForQuery(),
            maxRadiusKm = _uiState.value.filters.maxDistance?.toDouble()
        )
        pager = newPager
        firstPageLoaded = false
        val key = queryKey()
        loadedKey = key

        val cached = if (mode == LoadMode.RESET) AllJobsFirstPageCache.get(key) else null
        val cachedJobs = cached?.let { applyRuntimeFlags(it) }
        _uiState.update { state ->
            val seed = if (mode == LoadMode.RESET) (cachedJobs ?: emptyList()) else state.jobs
            state.copy(
                jobs = seed,
                isLoading = mode == LoadMode.RESET && seed.isEmpty(),
                isRefreshing = mode == LoadMode.PULL,
                isLoadingMore = false,
                loadMoreFailed = false,
                isSyncing = true,
                hasMore = true,
                error = null,
                hasError = false,
                totalJobs = seed.size,
                lastDocumentId = seed.lastOrNull()?.let { normalizedJobId(it) }
            )
        }
        if (mode == LoadMode.PULL) syncAppliedJobsFromBackend()

        loadJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val result: Result<NearbyJobsPager.Page> = try {
                newPager.nextPage(PAGE_SIZE.toInt())
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            if (gen != generation) return@launch
            val duration = System.currentTimeMillis() - startTime

            result.fold(
                onSuccess = { page ->
                    performanceTracker.trackApiCall("load_all_jobs", duration, success = true)
                    val jobs = applyRuntimeFlags(page.jobs.map { it.toJobListing() })
                    firstPageLoaded = true
                    loadedAtMs = System.currentTimeMillis()
                    AllJobsFirstPageCache.put(key, jobs)
                    Timber.d("AllJobsVM: page 1 = ${jobs.size} jobs in ${duration}ms (hasMore=${page.hasMore})")
                    _uiState.update {
                        it.copy(
                            jobs = jobs,
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                            loadMoreFailed = false,
                            isSyncing = false,
                            hasMore = page.hasMore,
                            hasError = false,
                            error = null,
                            totalJobs = jobs.size,
                            lastDocumentId = jobs.lastOrNull()?.let { job -> normalizedJobId(job) }
                        )
                    }
                },
                onFailure = { exception ->
                    performanceTracker.trackApiCall("load_all_jobs", duration, success = false)
                    Timber.w("AllJobsVM: page 1 failed: ${exception.message}")
                    loadedKey = null // allow ensureLoaded()/retry to try again
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isSyncing = false,
                            // Only take over the screen when there is nothing to show.
                            hasError = it.jobs.isEmpty(),
                            error = exception.message ?: "Failed to load jobs"
                        )
                    }
                }
            )
        }
    }

    // ==========================================
    // PUBLIC API - Filter State Updates
    // ==========================================

    /**
     * Applies the filter carried by the navigation route once per distinct value, so
     * coming back to the Jobs tab does not reset the category the user picked in the rail.
     */
    fun applyRouteFilter(filter: String) {
        if (filter == lastRouteFilter) return
        lastRouteFilter = filter
        savedStateHandle[KEY_ROUTE_FILTER] = filter
        val category = filter.takeIf { it != "All Jobs" }
        if (category != _uiState.value.initialCategory || filter != _uiState.value.selectedChip) {
            _uiState.update { it.copy(initialCategory = category, selectedChip = filter) }
            savedStateHandle[KEY_SELECTED_CHIP] = filter
            savedStateHandle[KEY_INITIAL_CATEGORY] = category
            loadedKey = null
        }
    }

    fun setSelectedChip(chip: String) {
        _uiState.update { it.copy(selectedChip = chip) }
        savedStateHandle[KEY_SELECTED_CHIP] = chip
        Timber.d("AllJobsVM: Chip filter changed to: $chip")
    }

    /**
     * Category rail tap. Reloads from the server with the category filter applied
     * server-side, so the nearest-first pager only ever walks jobs of that category.
     */
    fun setCategoryAndReload(category: String) {
        val categoryForQuery = category.takeIf { it != "All Jobs" }
        val current = _uiState.value
        if (current.selectedChip == category && current.initialCategory == categoryForQuery &&
            !current.hasError && current.jobs.isNotEmpty() && current.searchQuery.isBlank()
        ) {
            return
        }
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                selectedChip = category,
                initialCategory = categoryForQuery,
                searchQuery = "",
                searchResultsQuery = "",
                correctedQuery = null
            )
        }
        savedStateHandle[KEY_SELECTED_CHIP] = category
        savedStateHandle[KEY_INITIAL_CATEGORY] = categoryForQuery
        savedStateHandle[KEY_SEARCH_QUERY] = ""
        Timber.d("AllJobsVM: Category rail tapped, reloading for: $category")
        startLoad(LoadMode.RESET)
    }

    /** "View all jobs" from the empty state: drop category, search and filters and reload. */
    fun showAllJobs() {
        searchJob?.cancel()
        val defaults = JobFilters()
        _uiState.update {
            it.copy(
                selectedChip = "All Jobs",
                initialCategory = null,
                searchQuery = "",
                searchResultsQuery = "",
                filters = defaults
            )
        }
        savedStateHandle[KEY_SELECTED_CHIP] = "All Jobs"
        savedStateHandle[KEY_INITIAL_CATEGORY] = null
        savedStateHandle[KEY_SEARCH_QUERY] = ""
        persistFiltersToSavedState(defaults)
        startLoad(LoadMode.RESET)
    }

    fun setSearchQuery(query: String) {
        val normalizedQuery = query.trim()
        val wasShowingSearchResults = _uiState.value.searchResultsQuery.isNotBlank()
        _uiState.update {
            if (normalizedQuery.length >= 2) {
                it.copy(
                    searchQuery = query,
                    jobs = emptyList(),
                    isLoading = true,
                    isRefreshing = false,
                    isLoadingMore = false,
                    isSyncing = false,
                    hasMore = false,
                    lastDocumentId = null,
                    searchResultsQuery = "",
                    correctedQuery = null,
                    error = null,
                    hasError = false
                )
            } else {
                it.copy(searchQuery = query, correctedQuery = null)
            }
        }
        savedStateHandle[KEY_SEARCH_QUERY] = query
        Timber.d("AllJobsVM: Search query changed to: $query")

        searchJob?.cancel()

        if (normalizedQuery.length >= 2) {
            // Stop paging: search results replace the feed until the query is cleared.
            generation++
            loadJob?.cancel()
            moreJob?.cancel()
            loadedKey = null
            searchJob = viewModelScope.launch {
                kotlinx.coroutines.delay(500)
                searchJobsInDatabase(normalizedQuery)
            }
        } else if (query.isBlank() || wasShowingSearchResults) {
            _uiState.update { it.copy(searchResultsQuery = "") }
            startLoad(LoadMode.RESET)
        }
    }

    fun setFilters(filters: JobFilters) {
        val previous = _uiState.value.filters
        _uiState.update { it.copy(filters = filters) }
        persistFiltersToSavedState(filters)
        Timber.d("AllJobsVM: Filters updated")

        // Everything except the distance cap is applied client-side by filteredJobs.
        // The distance cap bounds how far the ring pager walks, so it needs a new pager.
        if (previous.maxDistance != filters.maxDistance && started && !isSearchActive()) {
            startLoad(LoadMode.SILENT)
        }
    }

    fun resetFilters() {
        setFilters(JobFilters())
    }

    fun setInitialCategory(category: String?) {
        _uiState.update { it.copy(initialCategory = category) }
        savedStateHandle[KEY_INITIAL_CATEGORY] = category
        if (category != null) {
            Timber.d("AllJobsVM: Initial category set to: $category")
        }
    }

    fun isInitialFilterCategory(): Boolean {
        val category = _uiState.value.initialCategory
        return category != null && categoryMapping.containsKey(category)
    }

    // ==========================================
    // PUBLIC API - Data Loading
    // ==========================================

    /** Manual location pick (search sheet / city chip): always reloads for the new place. */
    fun onLocationChanged(latitude: Double, longitude: Double) {
        setUserLocation(latitude, longitude, forceReload = true)
    }

    /**
     * Last-known location is used immediately; a later GPS fix only re-sorts when the user
     * actually moved more than [LOCATION_REFINE_THRESHOLD_M], and does so without clearing
     * the list (SILENT reload).
     */
    fun setUserLocation(latitude: Double, longitude: Double, forceReload: Boolean = false) {
        if (!GeoUtils.hasValidCoordinates(latitude, longitude)) return

        val movedMeters = if (hasValidUserLocation()) {
            GeoUtils.calculateHaversineDistance(userLatitude, userLongitude, latitude, longitude) * 1000.0
        } else {
            Double.MAX_VALUE
        }
        if (!forceReload && movedMeters <= LOCATION_REFINE_THRESHOLD_M) return

        userLatitude = latitude
        userLongitude = longitude
        savedStateHandle["userLatitude"] = latitude
        savedStateHandle["userLongitude"] = longitude
        Timber.d("AllJobsVM: User location set - lat=$latitude, lon=$longitude (force=$forceReload, moved=${movedMeters.toLong()}m)")

        if (!started || isSearchActive()) return
        startLoad(if (forceReload || _uiState.value.jobs.isEmpty()) LoadMode.RESET else LoadMode.SILENT)
    }

    /**
     * Idempotent entry point used by the screen. Returning to the tab keeps the list and
     * scroll position when the data is fresh; stale data is refreshed silently.
     */
    fun ensureLoaded() {
        loadJobs(PAGE_SIZE, _uiState.value.initialCategory)
    }

    fun loadJobs(limit: Long = PAGE_SIZE, category: String? = null) {
        if (category != _uiState.value.initialCategory) {
            _uiState.update {
                it.copy(initialCategory = category, selectedChip = category ?: "All Jobs")
            }
            savedStateHandle[KEY_INITIAL_CATEGORY] = category
            savedStateHandle[KEY_SELECTED_CHIP] = category ?: "All Jobs"
        }
        started = true
        if (isSearchActive()) return

        val state = _uiState.value
        val sameQuery = queryKey() == loadedKey && !state.hasError
        val inFlight = loadJob?.isActive == true
        val fresh = System.currentTimeMillis() - loadedAtMs < FRESH_WINDOW_MS
        if (sameQuery && (inFlight || (firstPageLoaded && fresh))) return

        startLoad(if (state.jobs.isEmpty()) LoadMode.RESET else LoadMode.SILENT)
    }

    /** "Try again" on the full-screen error. */
    fun retry() {
        if (isSearchActive()) {
            searchJobsInDatabase(_uiState.value.searchQuery.trim())
        } else {
            startLoad(LoadMode.RESET)
        }
    }

    /**
     * Loads the next page. Because [NearbyJobsPager] only leaves a ring when it is empty,
     * the returned jobs are always the next nearest ones.
     */
    fun loadMoreJobs(limit: Long = PAGE_SIZE, category: String? = null) {
        val state = _uiState.value
        val activePager = pager ?: return
        if (!firstPageLoaded || state.isSyncing || state.isLoading || state.isRefreshing || state.isLoadingMore || !state.hasMore) {
            return
        }

        val gen = generation
        _uiState.update { it.copy(isLoadingMore = true, loadMoreFailed = false) }

        moreJob = viewModelScope.launch {
            val result: Result<NearbyJobsPager.Page> = try {
                activePager.nextPage(limit.toInt().coerceAtLeast(1))
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            if (gen != generation || activePager !== pager) return@launch

            result.fold(
                onSuccess = { page ->
                    val incoming = applyRuntimeFlags(page.jobs.map { it.toJobListing() })
                    _uiState.update { current ->
                        val knownIds = current.jobs.mapTo(HashSet(current.jobs.size)) { normalizedJobId(it) }
                        val fresh = incoming.filter { normalizedJobId(it) !in knownIds }
                        val merged = if (fresh.isEmpty()) current.jobs else current.jobs + fresh
                        current.copy(
                            jobs = merged,
                            isLoadingMore = false,
                            loadMoreFailed = false,
                            hasMore = page.hasMore,
                            totalJobs = merged.size,
                            lastDocumentId = merged.lastOrNull()?.let { job -> normalizedJobId(job) }
                        )
                    }
                },
                onFailure = { exception ->
                    Timber.w("AllJobsVM: load more failed: ${exception.message}")
                    _uiState.update { it.copy(isLoadingMore = false, loadMoreFailed = true) }
                }
            )
        }
    }

    /** Pull-to-refresh. Keeps the current list visible and swaps in the fresh nearest page. */
    fun refreshJobs() {
        if (_uiState.value.isRefreshing) return
        if (isSearchActive()) {
            searchJob?.cancel()
            searchJobsInDatabase(_uiState.value.searchQuery.trim())
            return
        }
        startLoad(LoadMode.PULL)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, hasError = false) }
    }

    /**
     * Search jobs directly in database (Firestore)
     * Queries the database instead of filtering loaded jobs
     */
    private fun searchJobsInDatabase(query: String) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            _uiState.update {
                it.copy(
                    jobs = emptyList(),
                    isLoading = true,
                    isRefreshing = false,
                    hasMore = false,
                    lastDocumentId = null,
                    searchResultsQuery = "",
                    error = null,
                    hasError = false
                )
            }

            try {
                // Typo-tolerant: "diver" searches "driver". The original query stays the key
                // for stale-result checks and the pipeline; only the searched words change.
                val corrected = com.example.dutype.utils.JobQueryCorrector.correct(query)
                val searchText = corrected ?: query
                Timber.d("AllJobsVM: Searching database for: '$searchText' (typed '$query')")

                firestoreJobRepository.searchJobs(searchText, limit = 100L).collect { result ->
                    result.fold(
                        onSuccess = { searchResults ->
                            if (_uiState.value.searchQuery.trim() != query) {
                                Timber.d("AllJobsVM: Ignoring stale search results for '$query'")
                                return@fold
                            }

                            val duration = System.currentTimeMillis() - startTime
                            Timber.d("AllJobsVM: Database search returned ${searchResults.size} jobs in ${duration}ms")

                            var processedJobs = searchResults

                            if (hasValidUserLocation()) {
                                processedJobs = com.example.dutype.engine.NearestJobsEngine.getNearbyJobs(
                                    processedJobs, userLatitude, userLongitude
                                )
                            }
                            processedJobs = applyRuntimeFlags(processedJobs)

                            _uiState.update {
                                it.copy(
                                    jobs = processedJobs,
                                    isLoading = false,
                                    totalJobs = processedJobs.size,
                                    hasMore = false, // Search results are one relevance-ranked batch
                                    lastDocumentId = null,
                                    searchResultsQuery = query,
                                    correctedQuery = corrected
                                )
                            }
                        },
                        onFailure = { exception ->
                            val duration = System.currentTimeMillis() - startTime
                            Timber.w("AllJobsVM: Database search failed in ${duration}ms: ${exception.message}")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    hasError = true,
                                    error = "Search failed: ${exception.message}"
                                )
                            }
                        }
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                Timber.e("AllJobsVM: Exception during database search in ${duration}ms: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasError = true,
                        error = "Search error: ${e.message}"
                    )
                }
            }
        }
    }

    // ==========================================
    // P2-2: SavedStateHandle persistence helpers
    // ==========================================

    private fun restoreFiltersFromSavedState(): JobFilters {
        val defaults = JobFilters()
        return JobFilters(
            salaryMin = savedStateHandle.get<Int>(KEY_FILTER_SALARY_MIN) ?: defaults.salaryMin,
            salaryMax = savedStateHandle.get<Int>(KEY_FILTER_SALARY_MAX) ?: defaults.salaryMax,
            maxDistance = savedStateHandle.get<Float>(KEY_FILTER_MAX_DISTANCE),
            experienceLevel = savedStateHandle.get<String>(KEY_FILTER_EXPERIENCE) ?: defaults.experienceLevel,
            sortBy = savedStateHandle.get<String>(KEY_FILTER_SORT_BY) ?: defaults.sortBy,
            payType = savedStateHandle.get<String>(KEY_FILTER_PAY_TYPE) ?: defaults.payType,
            workType = savedStateHandle.get<String>(KEY_FILTER_WORK_TYPE) ?: defaults.workType,
            category = savedStateHandle.get<String>(KEY_FILTER_CATEGORY) ?: defaults.category,
            shiftTiming = savedStateHandle.get<String>(KEY_FILTER_SHIFT) ?: defaults.shiftTiming,
            urgentOnly = savedStateHandle.get<Boolean>(KEY_FILTER_URGENT) ?: defaults.urgentOnly
        )
    }

    private fun persistFiltersToSavedState(filters: JobFilters) {
        savedStateHandle[KEY_FILTER_SALARY_MIN] = filters.salaryMin
        savedStateHandle[KEY_FILTER_SALARY_MAX] = filters.salaryMax
        savedStateHandle[KEY_FILTER_MAX_DISTANCE] = filters.maxDistance
        savedStateHandle[KEY_FILTER_EXPERIENCE] = filters.experienceLevel
        savedStateHandle[KEY_FILTER_SORT_BY] = filters.sortBy
        savedStateHandle[KEY_FILTER_PAY_TYPE] = filters.payType
        savedStateHandle[KEY_FILTER_WORK_TYPE] = filters.workType
        savedStateHandle[KEY_FILTER_CATEGORY] = filters.category
        savedStateHandle[KEY_FILTER_SHIFT] = filters.shiftTiming
        savedStateHandle[KEY_FILTER_URGENT] = filters.urgentOnly
    }

    private companion object {
        const val KEY_SELECTED_CHIP = "alljobs_selected_chip"
        const val KEY_ROUTE_FILTER = "alljobs_route_filter"
        const val LOCATION_REFINE_THRESHOLD_M = 500.0
        const val FRESH_WINDOW_MS = 5 * 60 * 1000L
        const val KEY_SEARCH_QUERY = "alljobs_search_query"
        const val KEY_INITIAL_CATEGORY = "alljobs_initial_category"
        const val KEY_FILTER_SALARY_MIN = "alljobs_filter_salary_min"
        const val KEY_FILTER_SALARY_MAX = "alljobs_filter_salary_max"
        const val KEY_FILTER_MAX_DISTANCE = "alljobs_filter_max_distance"
        const val KEY_FILTER_EXPERIENCE = "alljobs_filter_experience"
        const val KEY_FILTER_SORT_BY = "alljobs_filter_sort_by"
        const val KEY_FILTER_PAY_TYPE = "alljobs_filter_pay_type"
        const val KEY_FILTER_WORK_TYPE = "alljobs_filter_work_type"
        const val KEY_FILTER_CATEGORY = "alljobs_filter_category"
        const val KEY_FILTER_SHIFT = "alljobs_filter_shift"
        const val KEY_FILTER_URGENT = "alljobs_filter_urgent"
    }
}

/**
 * Process-wide first-page cache so returning to the Jobs tab (or reopening the app within
 * a few minutes) paints the nearest jobs instantly while a silent refresh runs.
 * Keyed by category + location (~1 km grid) + distance cap.
 */
private object AllJobsFirstPageCache {
    private const val TTL_MS = 5 * 60 * 1000L
    private const val MAX_ENTRIES = 8

    private class Entry(val jobs: List<JobListing>, val savedAt: Long)

    @Volatile
    private var entries: Map<String, Entry> = emptyMap()

    fun get(key: String): List<JobListing>? {
        val entry = entries[key] ?: return null
        return if (System.currentTimeMillis() - entry.savedAt < TTL_MS) entry.jobs else null
    }

    fun put(key: String, jobs: List<JobListing>) {
        if (jobs.isEmpty()) return
        val next = entries.toMutableMap()
        next[key] = Entry(jobs, System.currentTimeMillis())
        while (next.size > MAX_ENTRIES) {
            val oldest = next.minByOrNull { it.value.savedAt }?.key ?: break
            next.remove(oldest)
        }
        entries = next
    }
}
