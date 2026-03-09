package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.example.dutype.repositories.FirestoreJobRepository
import com.example.dutype.location.LocationPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI State for Categories Screen
 */
data class CategoriesUiState(
    val jobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val currentCategory: String = "All",
    val lastDocumentId: String? = null // CRITICAL FIX: Use document ID for pagination cursor
)

/**
 * ViewModel for Categories Screen
 * Handles pagination for both "All Jobs" and category-specific jobs
 * Loads 15 jobs at a time for smooth infinite scroll experience
 */
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val firestoreJobRepository: FirestoreJobRepository,
    val locationPreferences: LocationPreferences,  // Public for screen access
    private val performanceTracker: com.example.dutype.performance.PerformanceTracker
) : ViewModel() {
    
    companion object {
        private const val PAGE_SIZE = 15L // Optimized for smooth infinite scroll
    }
    
    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()
    
    // User location for distance calculation
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0
    
    init {
        // Load user location from preferences
        viewModelScope.launch {
            val savedLocation = locationPreferences.getSavedLocation()
            if (savedLocation != null) {
                userLatitude = savedLocation.latitude
                userLongitude = savedLocation.longitude
                Timber.d("📍 CategoriesVM: User location loaded - lat=$userLatitude, lon=$userLongitude")
            }
        }
    }
    
    /**
     * Load jobs for a specific category (or all jobs if category is "All")
     * Resets pagination and loads first page
     * CRITICAL FIX: Complete state reset when switching categories
     */
    fun loadJobsForCategory(category: String) {
        val currentCategory = _uiState.value.currentCategory
        val isLoading = _uiState.value.isLoading
        val isLoadingMore = _uiState.value.isLoadingMore
        
        // CRITICAL FIX: Prevent duplicate calls
        if (currentCategory == category && (isLoading || isLoadingMore)) {
            Timber.d("📦 CategoriesVM: Already loading $category, skipping duplicate call")
            return
        }
        
        // CRITICAL FIX: Always reset state completely when switching categories OR reloading same category
        Timber.d("📦 ========== LOAD CATEGORY ==========")
        Timber.d("📦 From: '$currentCategory' → To: '$category'")
        Timber.d("📦 Resetting ALL state (jobs, pagination, errors)")
        
        // Complete state reset - clear everything
        _uiState.value = CategoriesUiState(
            isLoading = true,
            currentCategory = category,
            jobs = emptyList(),
            hasMore = true,
            lastDocumentId = null,
            error = null,
            isLoadingMore = false
        )
        
        viewModelScope.launch {
            com.example.dutype.performance.MainThreadChecker.assertMainThread()
            performanceTracker.trackOperation("loadJobsForCategory_$category")
            try {
                if (category == "All") {
                    loadAllJobs(PAGE_SIZE)
                } else {
                    loadCategoryJobs(category, PAGE_SIZE)
                }
            } catch (e: Exception) {
                Timber.e("❌ Error loading jobs for $category: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Force reload jobs for current category
     */
    fun refreshJobs() {
        val currentCategory = _uiState.value.currentCategory
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                jobs = emptyList(),
                hasMore = true,
                lastDocumentId = null,
                error = null
            )
            
            try {
                if (currentCategory == "All") {
                    loadAllJobs(PAGE_SIZE)
                } else {
                    loadCategoryJobs(currentCategory, PAGE_SIZE)
                }
            } catch (e: Exception) {
                Timber.e("Error refreshing jobs: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Load more jobs for current category (infinite scroll)
     */
    fun loadMoreJobs() {
        val currentState = _uiState.value
        
        Timber.d("📦 ========== LOAD MORE CALLED ==========")
        Timber.d("📦 Category: ${currentState.currentCategory}")
        Timber.d("📦 Current jobs: ${currentState.jobs.size}")
        Timber.d("📦 hasMore: ${currentState.hasMore}")
        Timber.d("📦 isLoadingMore: ${currentState.isLoadingMore}")
        Timber.d("📦 isLoading: ${currentState.isLoading}")
        Timber.d("📦 lastDocumentId: ${currentState.lastDocumentId}")
        
        if (currentState.isLoadingMore) {
            Timber.d("📦 ❌ Already loading more, skipping")
            return
        }
        
        if (!currentState.hasMore) {
            Timber.d("📦 ❌ No more jobs to load, skipping")
            return
        }
        
        if (currentState.isLoading) {
            Timber.d("📦 ❌ Initial load in progress, skipping")
            return
        }
        
        Timber.d("📦 ✅ Proceeding with load more...")
        Timber.d("📦 =======================================")
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            
            try {
                if (currentState.currentCategory == "All") {
                    loadAllJobs(PAGE_SIZE, currentState.lastDocumentId)
                } else {
                    loadCategoryJobs(currentState.currentCategory, PAGE_SIZE, currentState.lastDocumentId)
                }
            } catch (e: Exception) {
                Timber.e("Error loading more jobs: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }
    
    /**
     * Load all jobs with pagination
     * Uses proper pagination - loads 15 jobs at a time
     * NO MEMORY LIMITS - loads all jobs progressively
     */
    private suspend fun loadAllJobs(limit: Long, lastDocumentId: String? = null) {
        Timber.d("📦 Loading all jobs with limit=$limit, after=$lastDocumentId")
        
        // Pass null for category to fetch ALL jobs
        firestoreJobRepository.getAllJobsSummary(limit, lastDocumentId, null).collect { result ->
            result.fold(
                onSuccess = { summaries ->
                    processLoadedJobs(summaries, limit, lastDocumentId != null)
                },
                onFailure = { e ->
                    Timber.w("Failed to load all jobs: ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = e.message
                    )
                }
            )
        }
    }
    
    /**
     * Load jobs for a specific category
     * INDUSTRY STANDARD: Single server-side query with proper index
     * No client-side fallbacks - keep it simple and fast
     */
    private suspend fun loadCategoryJobs(category: String, limit: Long, lastDocumentId: String? = null) {
        // CRITICAL FIX: Map WorkerHomeScreen display names to JobCategory enum names
        // WorkerHomeScreen uses different display names than JobCategory enum
        val categoryMapping = mapOf(
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
        
        // Try mapping first, then try JobCategory enum, then fallback to uppercase
        val categoryQuery = categoryMapping[category] 
            ?: JobCategory.entries.find { it.displayName.equals(category, ignoreCase = true) }?.name 
            ?: category.uppercase()
        
        Timber.d("📦 ========== LOAD CATEGORY JOBS ==========")
        Timber.d("📦 Display name: '$category'")
        Timber.d("📦 Firestore query: '$categoryQuery'")
        Timber.d("📦 Limit: $limit")
        Timber.d("📦 lastDocumentId: $lastDocumentId")
        Timber.d("📦 =========================================")
        
        firestoreJobRepository.getAllJobsSummary(limit, lastDocumentId, categoryQuery).collect { result ->
            result.fold(
                onSuccess = { summaries ->
                    Timber.d("✅ Loaded ${summaries.size} jobs for '$categoryQuery'")
                    processLoadedJobs(summaries, limit, lastDocumentId != null)
                },
                onFailure = { e ->
                    Timber.e("❌ Failed to load category '$category': ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = e.message
                    )
                }
            )
        }
    }
    
    /**
     * Process loaded job summaries and update UI state
     * NO MEMORY LIMITS - appends all loaded jobs
     * INFINITE SCROLL - Always has more until we get 0 results
     * SORTED BY DISTANCE - Nearest jobs first (1km, 2km, 3km...)
     * DEDUPLICATION - Ensures unique job IDs to prevent LazyColumn crashes
     * 
     * CRITICAL FIX: hasMore should be true if we got ANY jobs (even 1)
     * With client-side filtering, we might get 10 jobs from 15 fetched
     * Keep loading until Firestore returns 0 documents
     */
    private fun processLoadedJobs(
        summaries: List<JobListingSummary>,
        limit: Long,
        isLoadingMore: Boolean
    ) {
        Timber.d("📦 ========== PROCESS LOADED JOBS ==========")
        Timber.d("📦 Loaded ${summaries.size} jobs from Firestore")
        Timber.d("📦 isLoadingMore: $isLoadingMore")
        Timber.d("📦 Current jobs in state: ${_uiState.value.jobs.size}")
        
        // Calculate distances if user location is available
        var processedSummaries = summaries
        if (userLatitude != 0.0 || userLongitude != 0.0) {
            processedSummaries = firestoreJobRepository.calculateSummaryDistances(
                summaries, userLatitude, userLongitude
            )
            Timber.d("📦 Calculated distances for ${processedSummaries.size} jobs")
        }
        
        // Convert to JobListing
        val newJobs = processedSummaries.map { it.toJobListing() }
        Timber.d("📦 Converted to ${newJobs.size} JobListing objects")
        
        // CRITICAL FIX: Deduplicate using jobId field (not id which might be documentId)
        val combinedJobs = if (isLoadingMore) {
            val existingJobIds = _uiState.value.jobs.map { it.jobId }.toSet()
            val uniqueNewJobs = newJobs.filter { it.jobId !in existingJobIds }
            Timber.d("📦 Pagination: ${uniqueNewJobs.size} unique jobs out of ${newJobs.size} loaded (removed ${newJobs.size - uniqueNewJobs.size} duplicates)")
            _uiState.value.jobs + uniqueNewJobs
        } else {
            // Initial load - sort by distance
            Timber.d("📦 Initial load - sorting by distance")
            newJobs.sortedBy { it.distance ?: Double.MAX_VALUE }
        }
        
        // CRITICAL FIX: DO NOT re-sort after pagination!
        // Re-sorting breaks cursor-based pagination and causes jobs to jump around
        // Jobs are already sorted by createdAt from Firestore query
        val finalJobs = combinedJobs
        
        // Debug: Log top 5 jobs with distances (only on initial load)
        if (!isLoadingMore && finalJobs.isNotEmpty()) {
            Timber.d("📦 Top 5 jobs after sorting:")
            finalJobs.take(5).forEachIndexed { index, job ->
                val distanceStr = job.distance?.let { "%.2f km".format(it) } ?: "no location"
                Timber.d("📦   #${index + 1}: ${job.title} - $distanceStr")
            }
        }
        
        val lastJob = newJobs.lastOrNull()
        
        // CRITICAL FIX: hasMore should be true if we got ANY jobs
        // Only stop when we get 0 jobs from Firestore
        // With client-side filtering, 15 fetched might become 10 after filtering
        val hasMore = newJobs.isNotEmpty()
        
        Timber.d("📦 ========== PAGINATION RESULT ==========")
        Timber.d("📦 Loaded: ${newJobs.size} jobs")
        Timber.d("📦 Total now: ${finalJobs.size} jobs")
        Timber.d("📦 hasMore: $hasMore (got any jobs: ${newJobs.isNotEmpty()})")
        Timber.d("📦 lastDocumentId: ${lastJob?.id}")
        Timber.d("📦 =======================================")
        
        _uiState.value = _uiState.value.copy(
            jobs = finalJobs,
            isLoading = false,
            isLoadingMore = false,
            hasMore = hasMore,
            lastDocumentId = lastJob?.id // CRITICAL FIX: Store document ID for cursor
        )
    }
}
