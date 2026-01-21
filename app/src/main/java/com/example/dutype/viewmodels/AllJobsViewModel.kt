package com.example.dutype.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.JobListing
import com.example.dutype.repositories.FirestoreJobRepository
import com.example.dutype.state.SavedJobsStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * P0 PERFORMANCE FIX: Job filters data class
 * Moved from AllJobsScreen to ViewModel for proper state management
 */
data class JobFilters(
    val salaryMin: Int = 0,
    val salaryMax: Int = 100000,
    val maxDistance: Float = 15f,
    val experienceLevel: String = "Any",
    val gender: String = "Any",
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
    val lastCreatedAt: Long? = null,
    val totalJobs: Int = 0
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
private const val MAX_JOBS_IN_MEMORY = 500

@OptIn(FlowPreview::class)
@HiltViewModel
class AllJobsViewModel @Inject constructor(
    private val firestoreJobRepository: FirestoreJobRepository,
    private val savedJobsStateManager: SavedJobsStateManager,
    private val savedStateHandle: SavedStateHandle,
    val locationService: com.example.dutype.utils.LocationService,
    val locationPreferences: com.example.dutype.location.LocationPreferences
) : ViewModel() {
    
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
     * 6. Apply search query
     * 7. Apply sorting
     */
    val filteredJobs: StateFlow<List<JobListing>> = combine(
        _uiState,
        _selectedChip,
        _searchQuery.debounce(300), // Debounce search for better performance
        _filters,
        _initialCategory
    ) { state, chip, query, filters, initialCategory ->
        if (state.isLoading || state.hasError) {
            return@combine emptyList()
        }
        
        val isCategory = initialCategory != null && categoryMapping.containsKey(initialCategory)
        
        // Step 1: Filter out filled jobs
        val availableJobs = state.jobs.filter { !it.isFilled }
        
        // Step 2: Filter out expired jobs
        val activeJobs = availableJobs.filter { !it.isExpired() }
        
        // Step 3: Apply category filter if initial filter is a category
        val categoryFiltered = if (isCategory && initialCategory != null) {
            val firestoreCategory = categoryMapping[initialCategory] ?: initialCategory.uppercase()
            activeJobs.filter { job ->
                job.getCategory().equals(firestoreCategory, ignoreCase = true) ||
                job.getCategory().equals(initialCategory, ignoreCase = true) ||
                job.title.contains(initialCategory, ignoreCase = true)
            }
        } else {
            activeJobs
        }
        
        // Step 4: Apply chip filter
        val chipFiltered = when (chip) {
            "All Jobs" -> categoryFiltered
            "Daily Jobs" -> categoryFiltered.filter {
                it.payType.equals("DAILY", true) ||
                it.payType.contains("day", true) ||
                it.jobType.equals("Daily", true)
            }
            "Hourly Jobs" -> categoryFiltered.filter {
                it.payType.equals("HOURLY", true) ||
                it.payType.contains("hour", true) ||
                it.jobType.equals("Hourly", true)
            }
            "Nearby" -> categoryFiltered.filter { job ->
                job.distance != null && job.distance!! < 10.0
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
        
        // Step 5: Apply advanced filters
        val advancedFiltered = chipFiltered.filter { job ->
            // Salary filter
            val jobSalary = job.payAmount.replace(",", "").replace("₹", "").toIntOrNull() ?: 0
            val salaryMatch = jobSalary == 0 || (jobSalary >= filters.salaryMin && jobSalary <= filters.salaryMax)
            
            // Distance filter
            val distanceMatch = job.distance == null || job.distance!! <= filters.maxDistance
            
            // Experience filter - check in requirements list
            val experienceMatch = filters.experienceLevel == "Any" ||
                job.requirements.any { it.contains(filters.experienceLevel, ignoreCase = true) }
            
            // Gender filter
            val genderMatch = filters.gender == "Any" ||
                job.gender.isEmpty() ||
                job.gender.equals(filters.gender, ignoreCase = true) ||
                job.gender.equals("Any", ignoreCase = true)
            
            salaryMatch && distanceMatch && experienceMatch && genderMatch
        }
        
        // Step 6: Apply search filter
        val searchFiltered = if (query.isNotBlank()) {
            advancedFiltered.filter { job ->
                job.title.contains(query, ignoreCase = true) ||
                job.companyName.contains(query, ignoreCase = true) ||
                job.getCategory().contains(query, ignoreCase = true) ||
                job.location.contains(query, ignoreCase = true)
            }
        } else {
            advancedFiltered
        }
        
        // Step 7: Apply sorting
        when (filters.sortBy) {
            "Salary: High to Low" -> searchFiltered.sortedByDescending {
                it.payAmount.replace(",", "").replace("₹", "").toIntOrNull() ?: 0
            }
            "Salary: Low to High" -> searchFiltered.sortedBy {
                it.payAmount.replace(",", "").replace("₹", "").toIntOrNull() ?: 0
            }
            "Distance" -> searchFiltered.sortedBy { it.distance ?: Float.MAX_VALUE.toDouble() }
            "Newest" -> searchFiltered.sortedByDescending { it.postedAt }
            else -> searchFiltered
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    /**
     * Active filter count computed as StateFlow
     */
    val activeFilterCount: StateFlow<Int> = _filters.combine(_filters) { filters, _ ->
        var count = 0
        if (filters.salaryMin > 0 || filters.salaryMax < 100000) count++
        if (filters.maxDistance < 15f) count++
        if (filters.experienceLevel != "Any") count++
        if (filters.gender != "Any") count++
        if (filters.sortBy != "Relevance") count++
        count
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )
    
    init {
        // Listen to saved jobs state and update job saved status
        viewModelScope.launch {
            savedJobsStateManager.savedJobIds.collect { savedJobIds ->
                val currentJobs = _uiState.value.jobs
                if (currentJobs.isNotEmpty()) {
                    val updatedJobs = currentJobs.map { job ->
                        job.copy(isSaved = savedJobIds.contains(job.id))
                    }
                    _uiState.value = _uiState.value.copy(jobs = updatedJobs)
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
        
        // Recalculate distances for existing jobs
        if (_uiState.value.jobs.isNotEmpty()) {
            viewModelScope.launch {
                recalculateDistances()
            }
        }
    }
    
    private suspend fun recalculateDistances() {
        withContext(Dispatchers.Default) {
            val jobsWithDistance = firestoreJobRepository.calculateJobsDistances(
                _uiState.value.jobs, userLatitude, userLongitude
            )
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(jobs = jobsWithDistance)
            }
        }
    }
    
    fun loadJobs(limit: Long = 500L) {
        if (_uiState.value.isLoading && hasInitiallyLoaded) {
            Timber.d("🔍 AllJobsVM: loadJobs skipped - already loading")
            return
        }
        
        if (hasInitiallyLoaded && _uiState.value.jobs.isNotEmpty() && !_uiState.value.isRefreshing) {
            Timber.d("🔍 AllJobsVM: loadJobs skipped - already loaded ${_uiState.value.jobs.size} jobs")
            return
        }
        
        hasInitiallyLoaded = true
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                hasError = false,
                jobs = emptyList(),
                lastCreatedAt = null,
                hasMore = true
            )
            
            try {
                Timber.d("🔍 AllJobsVM: Loading ALL jobs (limit: $limit)")
                // Use getAllJobsSummary with -1 to fetch ALL jobs
                firestoreJobRepository.getAllJobsSummary(-1L).collect { result ->
                    result.fold(
                        onSuccess = { summaries ->
                            Timber.d("✅ AllJobsVM: Loaded ${summaries.size} job summaries")
                            var processedJobs = summaries.map { it.toJobListing() }
                            
                            // Enforce max jobs limit
                            if (processedJobs.size > MAX_JOBS_IN_MEMORY) {
                                Timber.w("⚠️ AllJobsVM: Truncating to $MAX_JOBS_IN_MEMORY jobs")
                                processedJobs = processedJobs.take(MAX_JOBS_IN_MEMORY)
                            }
                            
                            // Calculate distances if location available
                            if (userLatitude != 0.0 || userLongitude != 0.0) {
                                processedJobs = firestoreJobRepository.calculateJobsDistances(
                                    processedJobs, userLatitude, userLongitude
                                )
                            }
                            
                            val lastJob = processedJobs.lastOrNull()
                            
                            _uiState.value = _uiState.value.copy(
                                jobs = processedJobs,
                                isLoading = false,
                                totalJobs = processedJobs.size,
                                hasMore = false, // All jobs loaded
                                lastCreatedAt = lastJob?.postedAt
                            )
                        },
                        onFailure = { exception ->
                            Timber.w("❌ AllJobsVM: Failed to load jobs: ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = exception.message ?: "Failed to load jobs"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e("❌ AllJobsVM: Exception loading jobs: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load jobs"
                )
            }
        }
    }
    
    fun loadMoreJobs(limit: Long = 30L) {
        if (_uiState.value.jobs.size >= MAX_JOBS_IN_MEMORY) {
            Timber.d("🔍 AllJobsVM: loadMoreJobs skipped - hit max limit")
            _uiState.value = _uiState.value.copy(hasMore = false)
            return
        }
        
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) {
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            
            try {
                val lastCreatedAt = _uiState.value.lastCreatedAt
                Timber.d("📦 AllJobsVM: Loading more jobs (after: $lastCreatedAt)")
                
                firestoreJobRepository.getAllJobsSummary(limit, lastCreatedAt).collect { result ->
                    result.fold(
                        onSuccess = { summaries ->
                            if (summaries.isEmpty()) {
                                _uiState.value = _uiState.value.copy(
                                    isLoadingMore = false,
                                    hasMore = false
                                )
                            } else {
                                var processedSummaries = summaries
                                if (userLatitude != 0.0 || userLongitude != 0.0) {
                                    processedSummaries = firestoreJobRepository.calculateSummaryDistances(
                                        summaries, userLatitude, userLongitude
                                    )
                                }
                                
                                val newJobs = processedSummaries.map { it.toJobListing() }
                                val currentJobs = _uiState.value.jobs
                                
                                val remainingCapacity = MAX_JOBS_IN_MEMORY - currentJobs.size
                                val jobsToAdd = if (newJobs.size > remainingCapacity) {
                                    newJobs.take(remainingCapacity)
                                } else {
                                    newJobs
                                }
                                
                                val updatedList = currentJobs + jobsToAdd
                                val lastJob = jobsToAdd.lastOrNull()
                                
                                _uiState.value = _uiState.value.copy(
                                    jobs = updatedList,
                                    isLoadingMore = false,
                                    totalJobs = updatedList.size,
                                    hasMore = newJobs.size >= limit && updatedList.size < MAX_JOBS_IN_MEMORY,
                                    lastCreatedAt = lastJob?.postedAt
                                )
                                
                                Timber.d("📦 AllJobsVM: Now showing ${updatedList.size} jobs")
                            }
                        },
                        onFailure = { exception ->
                            Timber.w("❌ AllJobsVM: Failed to load more: ${exception.message}")
                            _uiState.value = _uiState.value.copy(isLoadingMore = false)
                        }
                    )
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
                // Use getAllJobsSummary with -1 to fetch ALL jobs
                firestoreJobRepository.getAllJobsSummary(-1L).collect { result ->
                    result.fold(
                        onSuccess = { summaries ->
                            var processedJobs = summaries.map { it.toJobListing() }
                            
                            if (processedJobs.size > MAX_JOBS_IN_MEMORY) {
                                processedJobs = processedJobs.take(MAX_JOBS_IN_MEMORY)
                            }
                            
                            if (userLatitude != 0.0 || userLongitude != 0.0) {
                                processedJobs = firestoreJobRepository.calculateJobsDistances(
                                    processedJobs, userLatitude, userLongitude
                                )
                            }
                            
                            _uiState.value = _uiState.value.copy(
                                jobs = processedJobs,
                                isRefreshing = false,
                                totalJobs = processedJobs.size,
                                hasMore = false // All jobs loaded
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
}
