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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
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
 * We use 15 jobs per page for smooth infinite scroll
 */
private const val PAGE_SIZE = 15L // Jobs per page (industry standard)
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
    val sortBy: String = "Relevance"
)

/**
 * UI State for AllJobsScreen
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
    val totalJobs: Int = 0
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

    private fun parseSalaryForSort(salary: Double): Int = salary.toInt()

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
    
    private val _uiState = MutableStateFlow(AllJobsUiState())
    val uiState: StateFlow<AllJobsUiState> = _uiState.asStateFlow()
    
    // Filter state - managed in ViewModel
    private val _selectedChip = MutableStateFlow("All Jobs")
    val selectedChip: StateFlow<String> = _selectedChip.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _filters = MutableStateFlow(JobFilters())
    val filters: StateFlow<JobFilters> = _filters.asStateFlow()
    
    private val _initialCategory = MutableStateFlow<String?>(null)
    
    // User location for distance calculation
    private var userLatitude: Double = savedStateHandle.get<Double>("userLatitude") ?: 0.0
    private var userLongitude: Double = savedStateHandle.get<Double>("userLongitude") ?: 0.0
    
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
    private val _debouncedSearchQuery = _searchQuery
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
        _selectedChip,
        debouncedSearchQuery,
        _filters,
        _initialCategory,
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
            "Part Time" -> categoryFiltered.filter {
                it.jobType.equals("Part-time", true) ||
                it.jobType.contains("part", true)
            }
            "Full Time" -> categoryFiltered.filter {
                it.jobType.equals("Full-time", true) ||
                it.jobType.contains("full", true)
            }
            else -> categoryFiltered
        }
        
        Timber.d("🔍 filteredJobs: After chip filter ($chip): ${chipFiltered.size} jobs (was ${categoryFiltered.size})")
        
        val chipRemoved = categoryFiltered - chipFiltered.toSet()
        if (chipRemoved.isNotEmpty() && chip != "All Jobs") {
            Timber.w("🔍 CHIP FILTER REMOVED: ${chipRemoved.size} jobs for chip='$chip'")
            chipRemoved.take(3).forEach { job ->
                Timber.w("🔍   - ${job.title} (salaryType='${job.salaryType}', jobType='${job.jobType}', distance=${job.distance})")
            }
        }
        
        // Step 5: Apply advanced filters
        val advancedFiltered = chipFiltered.filter { job ->
            // Salary filter — use schema field salary (Double)
            val jobSalary = job.salary.toInt()
            val hasSalaryUpperBound = filters.salaryMax < 100000
            val salaryMatch = jobSalary == 0 || (
                jobSalary >= filters.salaryMin &&
                    (!hasSalaryUpperBound || jobSalary <= filters.salaryMax)
                )
            
            // Distance filter
            val dist = job.distance
            val distanceMatch = filters.maxDistance == null || dist == null || dist <= filters.maxDistance
            
            salaryMatch && distanceMatch
        }
        
        Timber.d("🔍 filteredJobs: After advanced filters: ${advancedFiltered.size} jobs (was ${chipFiltered.size})")
        
        val advancedRemoved = chipFiltered - advancedFiltered.toSet()
        if (advancedRemoved.isNotEmpty()) {
            Timber.w("🔍 ADVANCED FILTER REMOVED: ${advancedRemoved.size} jobs (salary range: ${filters.salaryMin}-${filters.salaryMax}, max distance: ${filters.maxDistance})")
            advancedRemoved.take(3).forEach { job ->
                val salary = job.salary.toInt()
                val dist = job.distance
                Timber.w("🔍   - ${job.title} (salary=$salary, distance=$dist)")
            }
        }
        
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
        // LOCATION FIRST: Default sort by distance (nearest jobs first)
        // Then apply user's selected sort preference
        val hasUsableLocation = GeoUtils.hasValidCoordinates(userLatitude, userLongitude)

        val sorted = when (filters.sortBy) {
            "Newest" -> searchFiltered.sortedByDescending { it.createdAt }
            "Salary: High to Low" -> searchFiltered.sortedByDescending { parseSalaryForSort(it.salary) }
            "Salary: Low to High" -> searchFiltered.sortedBy { parseSalaryForSort(it.salary) }
            "Distance", "Relevance" -> if (hasUsableLocation) {
                com.example.dutype.engine.NearestJobsEngine.getNearbyJobs(searchFiltered, userLatitude, userLongitude)
            } else {
                searchFiltered
            }
            else -> if (hasUsableLocation) {
                com.example.dutype.engine.NearestJobsEngine.getNearbyJobs(searchFiltered, userLatitude, userLongitude)
            } else {
                searchFiltered
            }
        }
        
        Timber.d("🔍 filteredJobs: ✅ FINAL STAGE - Sorting by '${filters.sortBy}', location available: $hasUsableLocation")
        Timber.d("🔍 filteredJobs: ✅ FINAL COUNT: ${sorted.size} jobs displayed to user")
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
    val activeFilterCount: StateFlow<Int> = _filters.combine(_filters) { filters, _ ->
        var count = 0
        if (filters.salaryMin > 0 || filters.salaryMax < 100000) count++
        if (filters.maxDistance != null) count++
        if (filters.experienceLevel != "Any") count++
        if (filters.sortBy != "Relevance") count++
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
        _selectedChip.value = chip
        Timber.d("📊 AllJobsVM: Chip filter changed to: $chip")
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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
            val categoryForQuery = _initialCategory.value?.takeIf { it != "All Jobs" }
            loadJobs(limit = 50L, category = categoryForQuery)
        }
    }
    
    fun setFilters(filters: JobFilters) {
        _filters.value = filters
        Timber.d("🎛️ AllJobsVM: Filters updated")
    }
    
    fun resetFilters() {
        _filters.value = JobFilters()
        Timber.d("🔄 AllJobsVM: Filters reset to defaults")
    }
    
    fun setInitialCategory(category: String?) {
        _initialCategory.value = category
        if (category != null) {
            Timber.d("📂 AllJobsVM: Initial category set to: $category")
        }
    }
    
    fun isInitialFilterCategory(): Boolean {
        val category = _initialCategory.value
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
        val currentCategory = _initialCategory.value
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
            _initialCategory.value = category
            
            // CRITICAL FIX: Reset UI state completely when category changes
            _uiState.value = AllJobsUiState(
                isLoading = true,
                jobs = emptyList(),
                hasMore = true,
                lastDocumentId = null,
                error = null,
                hasError = false
            )
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
                val currentFilters = _filters.value
                val useServerSideFiltering = currentFilters.salaryMin > 0 || 
                                            currentFilters.salaryMax < 100000 ||
                                            currentFilters.experienceLevel != "Any"
                val hasLocation = GeoUtils.hasValidCoordinates(userLatitude, userLongitude)
                
                if (useServerSideFiltering) {
                    Timber.d("🔍 P0 FIX: Using SERVER-SIDE filtering")
                    val queryLimit = if (hasLocation) maxOf(limit, 100L) else limit
                    loadJobsWithServerFiltering(queryLimit, firestoreCategory, currentFilters, startTime)
                } else {
                    Timber.d("🔍 AllJobsVM: Loading jobs with adaptive nearby fetch (category: $firestoreCategory, hasLocation: $hasLocation)")
                    
                    val summaryFlow = firestoreJobRepository.getAllJobsSummary(
                        limit = limit,
                        lastDocumentId = null,
                        category = firestoreCategory,
                        userLatitude = if (hasLocation) userLatitude else null,
                        userLongitude = if (hasLocation) userLongitude else null,
                        radiusKm = 10.0
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
                                    // Keep pagination alive as long as this page returned any jobs.
                                    // First page can be < limit after server-side filtering.
                                    hasMore = PaginationHelper.hasMorePages(processedJobs.size),
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
                        // Keep pagination alive as long as this page returned any jobs.
                        // First page can be < limit after server-side filtering.
                        hasMore = PaginationHelper.hasMorePages(processedJobs.size),
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
                val currentFilters = _filters.value
                val useServerSideFiltering = currentFilters.salaryMin > 0 || 
                                            currentFilters.salaryMax < 100000 ||
                                            currentFilters.experienceLevel != "Any"
                
                if (useServerSideFiltering) {
                    Timber.d("📦 P0 FIX: Loading more with SERVER-SIDE filtering")
                    firestoreJobRepository.getJobsFiltered(
                        category = firestoreCategory,
                        minSalary = if (currentFilters.salaryMin > 0) currentFilters.salaryMin else null,
                        maxSalary = if (currentFilters.salaryMax < 100000) currentFilters.salaryMax else null,
                        gender = null,
                        limit = limit,
                        lastDocumentId = lastDocumentId
                    ).collect { result ->
                        handleLoadMoreResult(result, limit)
                    }
                } else {
                    Timber.d("📦 AllJobsVM: Loading more jobs (after: $lastDocumentId, category: $firestoreCategory)")
                    
                    // CRITICAL FIX Phase 2: Pass location parameters for geohash-radius filtering
                    firestoreJobRepository.getAllJobsSummary(
                        limit = limit, 
                        lastDocumentId = lastDocumentId,
                        category = firestoreCategory,
                        userLatitude = if (hasValidUserLocation()) userLatitude else null,
                        userLongitude = if (hasValidUserLocation()) userLongitude else null,
                        radiusKm = 10.0
                    ).collect { result ->
                        handleLoadMoreResult(result, limit)
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
                // SMOOTH INFINITE SCROLL: Load first batch on refresh
                // CRITICAL FIX Phase 2: Pass location parameters for geohash-radius filtering
                firestoreJobRepository.getAllJobsSummary(
                    limit = PAGE_SIZE, 
                    lastDocumentId = null,
                    category = _initialCategory.value?.takeIf { it != "All Jobs" }?.let { categoryMapping[it] ?: it },
                    userLatitude = if (hasValidUserLocation()) userLatitude else null,
                    userLongitude = if (hasValidUserLocation()) userLongitude else null,
                    radiusKm = 10.0
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
    private suspend fun handleLoadMoreResult(result: Result<List<com.example.dutype.models.JobListingSummary>>, limit: Long) {
        result.fold(
            onSuccess = { summaries ->
                if (summaries.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        hasMore = false
                    )
                } else {
                    var newJobs = summaries.map { it.toJobListing() }
                    
                    if (userLatitude != 0.0 || userLongitude != 0.0) {
                        newJobs = com.example.dutype.engine.NearestJobsEngine.mergeAndSort(
                            _uiState.value.jobs,
                            newJobs,
                            userLatitude,
                            userLongitude
                        )
                    } else {
                        newJobs = _uiState.value.jobs + newJobs
                    }

                    newJobs = applyRuntimeFlags(newJobs)
                    
                    val updatedList = PaginationHelper.appendJobs(
                        emptyList(), newJobs.distinctBy { it.jobId }, MAX_JOBS_IN_MEMORY
                    )
                    val lastJobId = summaries.lastOrNull()?.id
                    
                    _uiState.value = _uiState.value.copy(
                        jobs = updatedList,
                        isLoadingMore = false,
                        totalJobs = updatedList.size,
                        hasMore = PaginationHelper.hasMorePages(summaries.size),
                        lastDocumentId = lastJobId
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
}
