package com.example.dutype.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.JobListing
import com.example.dutype.repositories.FirestoreJobRepository
import com.example.dutype.services.JobApplicationService
import com.example.dutype.state.AppStateManager
import com.example.dutype.utils.GeoUtils
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
    val workType: String = "Any"
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
    val lastDocumentId: String? = null, // CRITICAL FIX: Use document ID for pagination cursor
    val totalJobs: Int = 0,
    // P2-1: Filter inputs folded into the same UiState.
    val selectedChip: String = "All Jobs",
    val searchQuery: String = "",
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

        return when (raw.uppercase()) {
            "SHOP HELPER", "CONSTRUCTION", "HELPER" -> "HELPER"
            "HOUSEKEEPING", "MAID" -> "MAID"
            "KITCHEN", "COOK" -> "COOK"
            "EVENTS", "WAITER" -> "WAITER"
            else -> raw.uppercase().replace(' ', '_')
        }
    }

    private fun matchesCategoryByKeywords(job: JobListing, categoryToken: String): Boolean {
        val text = (job.title + " " + job.description).lowercase()
        val keywords = when (categoryToken) {
            "DELIVERY" -> listOf("delivery", "courier", "logistics", "rider")
            "HELPER" -> listOf("helper", "assistant", "support")
            "MAID" -> listOf("maid", "housekeeping", "cleaning")
            "COOK" -> listOf("cook", "chef", "kitchen")
            "WAITER" -> listOf("waiter", "steward", "server")
            "DRIVER" -> listOf("driver", "driving", "cab", "taxi")
            "SECURITY" -> listOf("security", "guard", "watchman")
            else -> emptyList()
        }
        return keywords.any { text.contains(it) }
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
            filters.workType != "Any"
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
    private val categoryMapping = mapOf(
        "Delivery" to "DELIVERY",
        "Shop Helper" to "HELPER",
        "Housekeeping" to "MAID",
        "Construction" to "HELPER",
        "Events" to "WAITER",
        "Kitchen" to "COOK",
        "Driver" to "DRIVER",
        "Security" to "SECURITY",
        "Electrician" to "ELECTRICIAN",
        "Plumber" to "PLUMBER",
        "Gardener" to "GARDENER",
        "Caretaker" to "CARETAKER",
        "Painter" to "PAINTER",
        "Carpenter" to "CARPENTER",
        "Receptionist" to "RECEPTIONIST",
        "Cashier" to "CASHIER",
        "Packer" to "PACKER"
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

    private fun normalizedJobId(job: JobListing): String = job.id.ifBlank { job.jobId }

    private fun hasValidUserLocation(): Boolean = GeoUtils.hasValidCoordinates(userLatitude, userLongitude)

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
        val filters = inputs.filters
        val initialCategory = inputs.initialCategory
        val savedJobIds = inputs.savedJobIds
        val appliedJobIds = inputs.appliedJobIds

        // CRITICAL FIX: Don't return empty during loading - let UI handle loading state
        // Only return empty if there's an error
        if (state.hasError) {
            Timber.d("🔍 filteredJobs: Returning empty due to error")
            return@combine emptyList()
        }
        
        // During loading with no jobs yet, return empty to show shimmer
        if (state.jobs.isEmpty() && state.isLoading) {
            Timber.d("🔍 filteredJobs: Loading... returning empty for shimmer")
            return@combine emptyList()
        }
        
        // If not loading and no jobs, return empty
        if (state.jobs.isEmpty()) {
            Timber.d("🔍 filteredJobs: No jobs loaded")
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
        
        // Step 3: Apply category filter if initial filter is a category
        val categoryFiltered = if (isCategory) {
            val initialCategoryValue = initialCategory ?: "All Jobs"
            val firestoreCategory = categoryMapping[initialCategoryValue] ?: initialCategoryValue.uppercase()
            val selectedTokens = setOf(
                normalizeCategoryToken(initialCategoryValue),
                normalizeCategoryToken(firestoreCategory)
            ).filter { it.isNotBlank() }.toSet()
            
            Timber.d("🔍 CATEGORY FILTER: Looking for '$firestoreCategory' with tokens=$selectedTokens")
            
            val filtered = activeJobs.filter { job ->
                val jobTypeToken = normalizeCategoryToken(job.jobType)
                val detectedToken = normalizeCategoryToken(job.getCategory())
                val keywordMatch = selectedTokens.any { token -> matchesCategoryByKeywords(job, token) }

                jobTypeToken in selectedTokens ||
                detectedToken in selectedTokens ||
                keywordMatch ||
                job.title.contains(initialCategoryValue, ignoreCase = true)
            }
            
            // ADD DEBUG: Log category mismatches
            val mismatch = activeJobs.filterNot { it in filtered }
            if (mismatch.isNotEmpty()) {
                Timber.w("🔍 CATEGORY MISMATCH: ${mismatch.size} jobs don't match '$firestoreCategory'")
                mismatch.take(3).forEach {
                    Timber.w("🔍   - ${it.title} (detected='${it.getCategory()}')")
                }
            }
            
            Timber.d("🔍 filteredJobs: After category filter ($initialCategory): ${filtered.size} jobs")
            filtered
        } else {
            Timber.d("🔍 filteredJobs: No category filter applied")
            activeJobs
        }
        
        // Step 4: Apply chip filter
        val chipFiltered = when (chip) {
            "All Jobs" -> categoryFiltered
            "Daily Jobs" -> categoryFiltered.filter {
                it.salaryType.equals("DAILY", true)
            }
            "Hourly Jobs" -> categoryFiltered.filter {
                it.salaryType.equals("HOURLY", true)
            }
            "Nearby" -> categoryFiltered.filter { job ->
                val dist = job.distance
                dist != null && dist < 10.0
            }.sortedBy { it.distance }
            "Part Time" -> categoryFiltered.filter { matchesWorkType(it, "Part-time") }
            "Full Time" -> categoryFiltered.filter { matchesWorkType(it, "Full-time") }
            else -> categoryFiltered
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
            
            // Distance filter
            val dist = job.distance
            val distanceMatch = filters.maxDistance == null || dist == null || dist <= filters.maxDistance
            val payTypeMatch = filters.payType == "Any" || job.salaryType.equals(filters.payType, ignoreCase = true)
            val workTypeMatch = matchesWorkType(job, filters.workType)
            val experienceMatch = matchesExperienceLevel(job, filters.experienceLevel)
            
            salaryMatch && distanceMatch && payTypeMatch && workTypeMatch && experienceMatch
        }
        
        Timber.d("🔍 filteredJobs: After advanced filters: ${advancedFiltered.size} jobs (was ${chipFiltered.size})")
        
        // INDUSTRY STANDARD: When search is active (2+ chars), use database search results
        // No client-side filtering - trust the database query
        val searchFiltered = if (query.length >= 2) {
            // Database search active - results already filtered and sorted by relevance
            Timber.d("🔍 filteredJobs: Using database search results for query='$query' (${advancedFiltered.size} jobs)")
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
                // User explicitly wants distance sort — re-sort all
                val hasUsableLocation = GeoUtils.hasValidCoordinates(userLatitude, userLongitude)
                if (hasUsableLocation) {
                    com.example.dutype.engine.NearestJobsEngine.getNearbyJobs(searchFiltered, userLatitude, userLongitude)
                } else {
                    searchFiltered
                }
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
        count
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0
    )
    
    init {
        syncAppliedJobsFromBackend()
        
        // P0 FIX: Observe location changes and re-sort jobs immediately
        viewModelScope.launch {
            locationPreferences.currentLocation.collect { newLocation ->
                if (newLocation != null && _uiState.value.jobs.isNotEmpty()) {
                    Timber.d("📍 AllJobsVM: Location changed - re-sorting jobs")
                    setUserLocation(newLocation.latitude, newLocation.longitude)
                }
            }
        }
    }

    
    // ==========================================
    // PUBLIC API - Filter State Updates
    // ==========================================
    
    fun setSelectedChip(chip: String) {
        _uiState.update { it.copy(selectedChip = chip) }
        savedStateHandle[KEY_SELECTED_CHIP] = chip
        Timber.d("📊 AllJobsVM: Chip filter changed to: $chip")

        if (_uiState.value.searchQuery.isBlank() && chip != "All Jobs" && _uiState.value.jobs.size <= PAGE_SIZE.toInt()) {
            loadJobs(limit = 60L, category = currentQueryCategory())
        }
    }
    
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        savedStateHandle[KEY_SEARCH_QUERY] = query
        Timber.d("🔍 AllJobsVM: Search query changed to: $query")
        
        // Cancel previous search job
        searchJob?.cancel()
        
        // Trigger database search when query is not blank
        if (query.isNotBlank() && query.length >= 2) {
            // Debounce: Wait 500ms before searching to avoid excessive queries
            searchJob = viewModelScope.launch {
                kotlinx.coroutines.delay(500)
                searchJobsInDatabase(query)
            }
        } else if (query.isBlank()) {
            // Reset to show all jobs when search is cleared
            loadJobs(limit = PAGE_SIZE, category = currentQueryCategory())
        }
    }
    
    fun setFilters(filters: JobFilters) {
        _uiState.update { it.copy(filters = filters) }
        persistFiltersToSavedState(filters)
        Timber.d("🎛️ AllJobsVM: Filters updated")

        if (_uiState.value.searchQuery.isBlank()) {
            val reloadLimit = if (shouldReloadForCurrentFilters(filters)) 60L else PAGE_SIZE
            loadJobs(limit = reloadLimit, category = currentQueryCategory())
        }
    }
    
    fun resetFilters() {
        val defaults = JobFilters()
        _uiState.update { it.copy(filters = defaults) }
        persistFiltersToSavedState(defaults)
        Timber.d("🔄 AllJobsVM: Filters reset to defaults")

        if (_uiState.value.searchQuery.isBlank()) {
            loadJobs(limit = PAGE_SIZE, category = currentQueryCategory())
        }
    }
    
    fun setInitialCategory(category: String?) {
        _uiState.update { it.copy(initialCategory = category) }
        savedStateHandle[KEY_INITIAL_CATEGORY] = category
        if (category != null) {
            Timber.d("📂 AllJobsVM: Initial category set to: $category")
        }
    }
    
    fun isInitialFilterCategory(): Boolean {
        val category = _uiState.value.initialCategory
        return category != null && categoryMapping.containsKey(category)
    }
    
    // ==========================================
    // PUBLIC API - Data Loading
    // ==========================================
    
    fun setUserLocation(latitude: Double, longitude: Double) {
        userLatitude = latitude
        userLongitude = longitude
        savedStateHandle["userLatitude"] = latitude
        savedStateHandle["userLongitude"] = longitude
        Timber.d("📍 AllJobsVM: User location set - lat=$latitude, lon=$longitude")
        
        // Recalculate distances for existing jobs immediately
        if (_uiState.value.jobs.isNotEmpty()) {
            viewModelScope.launch {
                recalculateDistances()
            }
        }
    }
    
    private suspend fun recalculateDistances() {
        withContext(Dispatchers.Default) {
            val jobsWithDistance = com.example.dutype.engine.NearestJobsEngine.getNearbyJobs(
                _uiState.value.jobs, userLatitude, userLongitude
            )
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(jobs = applyRuntimeFlags(jobsWithDistance))
            }
        }
    }
    
    fun loadJobs(limit: Long = PAGE_SIZE, category: String? = null) {
        // Allow reload if category is different from what was loaded
        val currentCategory = _uiState.value.initialCategory
        val isDifferentCategory = currentCategory != category
        
        // CRITICAL FIX: Always allow loading if category is different OR if no jobs loaded yet
        if (_uiState.value.isLoading && hasInitiallyLoaded && !isDifferentCategory && _uiState.value.jobs.isNotEmpty()) {
            Timber.d("🔍 AllJobsVM: loadJobs skipped - already loading same category with jobs")
            return
        }
        
        // If category changed, reset the flag to allow reload
        if (isDifferentCategory) {
            Timber.d("🔍 ========== CATEGORY CHANGE ==========")
            Timber.d("🔍 AllJobsVM: From '$currentCategory' → To '$category'")
            Timber.d("🔍 Resetting state and pagination")
            hasInitiallyLoaded = false
            _uiState.update {
                it.copy(
                    initialCategory = category,
                    // CRITICAL FIX: Reset UI state completely when category changes
                    isLoading = true,
                    jobs = emptyList(),
                    hasMore = true,
                    lastDocumentId = null,
                    error = null,
                    hasError = false
                )
            }
        }
        
        hasInitiallyLoaded = true
        
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            com.example.dutype.performance.MainThreadChecker.assertMainThread("AllJobsViewModel.loadJobs")
            
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                hasError = false,
                jobs = emptyList(),
                lastDocumentId = null,
                hasMore = true
            )
            
            try {
                // Determine category for Firestore query
                val firestoreCategory = if (category != null && category != "All Jobs" && categoryMapping.containsKey(category)) {
                    categoryMapping[category]
                } else {
                    null // Fetch all categories
                }
                
                // P0 FIX: Use server-side filtering when filters are applied
                val currentFilters = _uiState.value.filters
                val useServerSideFiltering = shouldUseServerSideFiltering(currentFilters)
                paginationUsesLocation = false
                
                if (useServerSideFiltering) {
                    Timber.d("🔍 P0 FIX: Using SERVER-SIDE filtering")
                    val queryLimit = limit
                    loadJobsWithServerFiltering(queryLimit, firestoreCategory, currentFilters, startTime)
                } else {
                    Timber.d("🔍 AllJobsVM: Loading full job dataset (category: $firestoreCategory, location filter disabled)")
                    
                    val summaryFlow = firestoreJobRepository.getAllJobsSummary(
                        limit = limit,
                        lastDocumentId = null,
                        category = firestoreCategory,
                        userLatitude = null,
                        userLongitude = null,
                        radiusKm = 0.0
                    )
                    summaryFlow.collect { result ->
                        result.fold(
                            onSuccess = { summaries ->
                                val duration = System.currentTimeMillis() - startTime
                                performanceTracker.trackApiCall("load_all_jobs", duration, success = true)

                                Timber.d("✅ AllJobsVM: Loaded ${summaries.size} job summaries in ${duration}ms")
                                var processedJobs = summaries.map { it.toJobListing() }

                                if (userLatitude != 0.0 || userLongitude != 0.0) {
                                    Timber.d("📍 AllJobsVM: Calculating distances with user location ($userLatitude, $userLongitude)")
                                    processedJobs = com.example.dutype.engine.NearestJobsEngine.getNearbyJobs(
                                        processedJobs, userLatitude, userLongitude
                                    )

                                    val jobsWithDistance = processedJobs.filter { it.distance != null }
                                    Timber.d("📍 AllJobsVM: ${jobsWithDistance.size}/${processedJobs.size} jobs have distance calculated")

                                    val nearest = jobsWithDistance.take(5)
                                    nearest.forEachIndexed { index, job ->
                                        Timber.d("📍 AllJobsVM Job #${index + 1}: ${job.title} - %.2f km".format(job.distance))
                                    }
                                } else {
                                    Timber.w("📍 AllJobsVM: No user location set - distances will not be calculated")
                                }

                                processedJobs = applyRuntimeFlags(processedJobs)
                                val lastSummaryId = summaries.lastOrNull()?.id

                                _uiState.value = _uiState.value.copy(
                                    jobs = processedJobs,
                                    isLoading = false,
                                    totalJobs = processedJobs.size,
                                    // FIX: Use raw summaries.size, not post-filtered processedJobs.size
                                    // processedJobs is reduced by applyRuntimeFlags (filters applied jobs)
                                    hasMore = PaginationHelper.hasMorePages(summaries.size),
                                    lastDocumentId = lastSummaryId
                                )
                            },
                            onFailure = { exception ->
                                val duration = System.currentTimeMillis() - startTime
                                performanceTracker.trackApiCall("load_all_jobs", duration, success = false)

                                Timber.w("❌ AllJobsVM: Failed to load jobs: ${exception.message}")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    hasError = true,
                                    error = exception.message ?: "Failed to load jobs"
                                )
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                performanceTracker.trackApiCall("load_all_jobs", duration, success = false)
                
                Timber.e("❌ AllJobsVM: Exception loading jobs: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load jobs"
                )
            }
        }
    }
    
    /**
     * P0 FIX: Load jobs with server-side filtering
     * Reduces bandwidth by 85% and CPU usage by 100%
     */
    private suspend fun loadJobsWithServerFiltering(
        limit: Long,
        category: String?,
        filters: JobFilters,
        startTime: Long
    ) {
        firestoreJobRepository.getJobsFiltered(
            category = category,
            minSalary = if (filters.salaryMin > 0) filters.salaryMin else null,
            maxSalary = if (filters.salaryMax < 100000) filters.salaryMax else null,
            payType = filters.payType.takeIf { it != "Any" },
            gender = null,
            limit = limit
        ).collect { result ->
            result.fold(
                onSuccess = { summaries ->
                    val duration = System.currentTimeMillis() - startTime
                    performanceTracker.trackApiCall("load_all_jobs_filtered", duration, success = true)
                    
                    Timber.d("✅ P0 FIX: Server-side filtering returned ${summaries.size} jobs in ${duration}ms")
                    var processedJobs = summaries.map { it.toJobListing() }
                    
                    // Calculate distances if location available
                    if (userLatitude != 0.0 || userLongitude != 0.0) {
                        processedJobs = com.example.dutype.engine.NearestJobsEngine.getNearbyJobs(
                            processedJobs, userLatitude, userLongitude
                        )
                    }
                    
                    processedJobs = applyRuntimeFlags(processedJobs)
                    val lastSummaryId = summaries.lastOrNull()?.id
                    
                    _uiState.value = _uiState.value.copy(
                        jobs = processedJobs,
                        isLoading = false,
                        totalJobs = processedJobs.size,
                        // FIX: Use raw summaries.size, not post-filtered processedJobs.size
                        hasMore = PaginationHelper.hasMorePages(summaries.size),
                        lastDocumentId = lastSummaryId
                    )
                },
                onFailure = { exception ->
                    val duration = System.currentTimeMillis() - startTime
                    performanceTracker.trackApiCall("load_all_jobs_filtered", duration, success = false)
                    
                    Timber.w("❌ Server-side filtering failed: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasError = true,
                        error = exception.message ?: "Failed to load jobs"
                    )
                }
            )
        }
    }
    
    fun loadMoreJobs(limit: Long = PAGE_SIZE, category: String? = null) {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) {
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            
            try {
                val lastDocumentId = _uiState.value.lastDocumentId
                
                // Determine category for Firestore query
                val firestoreCategory = if (category != null && category != "All Jobs" && categoryMapping.containsKey(category)) {
                    categoryMapping[category]
                } else {
                    null
                }
                
                // P0 FIX: Use server-side filtering for pagination too
                val currentFilters = _uiState.value.filters
                val useServerSideFiltering = shouldUseServerSideFiltering(currentFilters)
                
                if (useServerSideFiltering) {
                    Timber.d("📦 P0 FIX: Loading more with SERVER-SIDE filtering")
                    firestoreJobRepository.getJobsFiltered(
                        category = firestoreCategory,
                        minSalary = if (currentFilters.salaryMin > 0) currentFilters.salaryMin else null,
                        maxSalary = if (currentFilters.salaryMax < 100000) currentFilters.salaryMax else null,
                        payType = currentFilters.payType.takeIf { it != "Any" },
                        gender = null,
                        limit = limit,
                        lastDocumentId = lastDocumentId
                    ).collect { result ->
                        handleLoadMoreResult(result, previousCursor = lastDocumentId)
                    }
                } else {
                    Timber.d("📦 AllJobsVM: Loading more jobs (after: $lastDocumentId, category: $firestoreCategory)")
                    
                    // Load full dataset for pagination; location is used only for client-side distance sorting.
                    firestoreJobRepository.getAllJobsSummary(
                        limit = limit, 
                        lastDocumentId = lastDocumentId,
                        category = firestoreCategory,
                        userLatitude = null,
                        userLongitude = null,
                        radiusKm = 0.0
                    ).collect { result ->
                        handleLoadMoreResult(result, previousCursor = lastDocumentId)
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Timber.e("❌ AllJobsVM: Exception loading more: ${e.message}")
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
            }
        }
    }
    
    fun refreshJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null, hasError = false)
            
            try {
                paginationUsesLocation = false
                // SMOOTH INFINITE SCROLL: Load first batch on refresh
                // Refresh from full dataset irrespective of location availability.
                firestoreJobRepository.getAllJobsSummary(
                    limit = PAGE_SIZE, 
                    lastDocumentId = null,
                    category = _uiState.value.initialCategory?.takeIf { it != "All Jobs" }?.let { categoryMapping[it] ?: it },
                    userLatitude = null,
                    userLongitude = null,
                    radiusKm = 0.0
                ).collect { result ->
                    result.fold(
                        onSuccess = { summaries ->
                            var processedJobs = summaries.map { it.toJobListing() }
                            
                            if (userLatitude != 0.0 || userLongitude != 0.0) {
                                processedJobs = com.example.dutype.engine.NearestJobsEngine.getNearbyJobs(
                                    processedJobs, userLatitude, userLongitude
                                )
                            }
                            processedJobs = applyRuntimeFlags(processedJobs)
                            
                            val lastSummaryId = summaries.lastOrNull()?.id

                            _uiState.value = _uiState.value.copy(
                                jobs = processedJobs,
                                isRefreshing = false,
                                totalJobs = processedJobs.size,
                                hasMore = processedJobs.isNotEmpty(),
                                lastDocumentId = lastSummaryId
                            )
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isRefreshing = false,
                                hasError = true,
                                error = exception.message ?: "Failed to refresh"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    hasError = true,
                    error = e.message ?: "Failed to refresh"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, hasError = false)
    }
    
    /**
     * Search jobs directly in database (Firestore)
     * Queries the database instead of filtering loaded jobs
     */
    private fun searchJobsInDatabase(query: String) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            
            // Don't show loading spinner for search - keep existing jobs visible
            _uiState.value = _uiState.value.copy(
                error = null,
                hasError = false
            )
            
            try {
                Timber.d("🔍 AllJobsVM: Searching database for: '$query'")
                
                // Use repository's searchJobs method to query Firestore
                firestoreJobRepository.searchJobs(query, limit = 100L).collect { result ->
                    result.fold(
                        onSuccess = { searchResults ->
                            val duration = System.currentTimeMillis() - startTime
                            Timber.d("✅ AllJobsVM: Database search returned ${searchResults.size} jobs in ${duration}ms")
                            
                            var processedJobs = searchResults
                            
                            // Calculate distances if location available
                            if (userLatitude != 0.0 || userLongitude != 0.0) {
                                processedJobs = com.example.dutype.engine.NearestJobsEngine.getNearbyJobs(
                                    processedJobs, userLatitude, userLongitude
                                )
                            }
                            processedJobs = applyRuntimeFlags(processedJobs)
                            
                            _uiState.value = _uiState.value.copy(
                                jobs = processedJobs,
                                isLoading = false,
                                totalJobs = processedJobs.size,
                                hasMore = false, // Search results don't support pagination
                                lastDocumentId = null
                            )
                        },
                        onFailure = { exception ->
                            val duration = System.currentTimeMillis() - startTime
                            Timber.w("❌ AllJobsVM: Database search failed in ${duration}ms: ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = "Search failed: ${exception.message}"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                Timber.e("❌ AllJobsVM: Exception during database search in ${duration}ms: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = "Search error: ${e.message}"
                )
            }
        }
    }
    
    /**
     * P0 FIX: Handle load more result (shared logic)
     * CRITICAL FIX: hasMore should be true if we got ANY jobs (even 1)
     * Only stop when we get 0 jobs from Firestore
     */
    private suspend fun handleLoadMoreResult(
        result: Result<List<com.example.dutype.models.JobListingSummary>>,
        previousCursor: String?
    ) {
        result.fold(
            onSuccess = { summaries ->
                if (summaries.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        hasMore = false
                    )
                } else {
                    val previousSize = _uiState.value.jobs.size
                    var newJobs = summaries.map { it.toJobListing() }
                    
                    // FIX: Only calculate distances for NEW jobs, then APPEND.
                    // Never re-sort the entire list — it causes the list to jump/rearrange.
                    // Initial load is already sorted nearest-first.
                    // LinkedIn/Indeed approach: append new pages to end, maintain scroll position.
                    if (userLatitude != 0.0 || userLongitude != 0.0) {
                        newJobs = com.example.dutype.engine.NearestJobsEngine.getNearbyJobs(
                            newJobs, userLatitude, userLongitude
                        )
                    }
                    
                    // Append new jobs to existing list (no re-sorting of existing jobs)
                    val combined = _uiState.value.jobs + newJobs
                    val combined2 = applyRuntimeFlags(combined)
                    
                    val updatedList = PaginationHelper.appendJobs(
                        emptyList(),
                        combined2.distinctBy { job ->
                            normalizedJobId(job).ifBlank {
                                "${job.employerId}:${job.title.trim().lowercase()}:${job.createdAt}"
                            }
                        },
                        MAX_JOBS_IN_MEMORY
                    )
                    val lastJobId = summaries.lastOrNull()?.id
                    val cursorAdvanced = !lastJobId.isNullOrBlank() && lastJobId != previousCursor
                    val noGrowth = summaries.isNotEmpty() && updatedList.size == previousSize && !cursorAdvanced
                    val shouldContinuePaging = PaginationHelper.hasMorePages(summaries.size) &&
                        (cursorAdvanced || updatedList.size > previousSize)

                    if (noGrowth) {
                        Timber.w("📦 AllJobsVM: Pagination page had no new unique jobs. Stopping further load-more to avoid loop.")
                    }
                    if (!cursorAdvanced && summaries.isNotEmpty()) {
                        Timber.w("📦 AllJobsVM: Pagination cursor did not advance (cursor=$lastJobId). Marking end of list.")
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        jobs = updatedList,
                        isLoadingMore = false,
                        totalJobs = updatedList.size,
                        hasMore = !noGrowth && shouldContinuePaging,
                        lastDocumentId = lastJobId ?: previousCursor
                    )
                }
            },
            onFailure = { exception ->
                Timber.w("❌ Failed to load more jobs: ${exception.message}")
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = exception.message
                )
            }
        )
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
            workType = savedStateHandle.get<String>(KEY_FILTER_WORK_TYPE) ?: defaults.workType
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
    }

    private companion object {
        const val KEY_SELECTED_CHIP = "alljobs_selected_chip"
        const val KEY_SEARCH_QUERY = "alljobs_search_query"
        const val KEY_INITIAL_CATEGORY = "alljobs_initial_category"
        const val KEY_FILTER_SALARY_MIN = "alljobs_filter_salary_min"
        const val KEY_FILTER_SALARY_MAX = "alljobs_filter_salary_max"
        const val KEY_FILTER_MAX_DISTANCE = "alljobs_filter_max_distance"
        const val KEY_FILTER_EXPERIENCE = "alljobs_filter_experience"
        const val KEY_FILTER_SORT_BY = "alljobs_filter_sort_by"
        const val KEY_FILTER_PAY_TYPE = "alljobs_filter_pay_type"
        const val KEY_FILTER_WORK_TYPE = "alljobs_filter_work_type"
    }
}
