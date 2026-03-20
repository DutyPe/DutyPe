package com.example.dutype.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.example.dutype.repositories.FirestoreJobRepository
import com.example.dutype.location.LocationPreferences
import com.example.dutype.utils.GeoUtils
import com.example.dutype.utils.LocationService
import com.example.dutype.utils.toJobListing
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
    val locationService: LocationService,
    private val performanceTracker: com.example.dutype.performance.PerformanceTracker,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    companion object {
        private const val PAGE_SIZE = 15L // Optimized for smooth infinite scroll
    }
    
    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()
    
    // User location for distance calculation
    private var userLatitude: Double = savedStateHandle.get<Double>("categoriesUserLatitude") ?: 0.0
    private var userLongitude: Double = savedStateHandle.get<Double>("categoriesUserLongitude") ?: 0.0
    
    init {
        // Load user location from preferences
        viewModelScope.launch {
            val savedLocation = locationPreferences.getSavedLocationIfFresh()
            if (savedLocation != null) {
                userLatitude = savedLocation.latitude
                userLongitude = savedLocation.longitude
                Timber.d("📍 CategoriesVM: User location loaded - lat=$userLatitude, lon=$userLongitude")
            }
        }
        
        // P0 FIX: Observe location changes and re-sort jobs immediately
        viewModelScope.launch {
            locationPreferences.currentLocation.collect { newLocation ->
                if (newLocation != null && _uiState.value.jobs.isNotEmpty()) {
                    Timber.d("📍 CategoriesVM: Location changed - re-sorting jobs")
                    setUserLocation(newLocation.latitude, newLocation.longitude)
                }
            }
        }
    }

    fun setUserLocation(latitude: Double, longitude: Double) {
        if (!GeoUtils.hasValidCoordinates(latitude, longitude)) {
            Timber.w("📍 CategoriesVM: Ignoring invalid user location lat=$latitude lon=$longitude")
            return
        }

        userLatitude = latitude
        userLongitude = longitude
        savedStateHandle["categoriesUserLatitude"] = latitude
        savedStateHandle["categoriesUserLongitude"] = longitude

        if (_uiState.value.jobs.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                jobs = com.example.dutype.engine.NearestJobsEngine.getNearbyJobs(_uiState.value.jobs, userLatitude, userLongitude)
            )
        }

        Timber.d("📍 CategoriesVM: User location set - lat=$latitude, lon=$longitude")
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
        val hasLocation = GeoUtils.hasValidCoordinates(userLatitude, userLongitude)
        // Keep "All" category paginated from first paint to avoid loading hundreds of jobs upfront.
        val summaryFlow = firestoreJobRepository.getAllJobsSummary(
            limit = limit,
            lastDocumentId = lastDocumentId,
            category = null,
            userLatitude = if (hasLocation) userLatitude else null,
            userLongitude = if (hasLocation) userLongitude else null,
            radiusKm = 10.0
        )
        
        summaryFlow.collect { result ->
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
        val hasLocation = GeoUtils.hasValidCoordinates(userLatitude, userLongitude)
        val summaryFlow = firestoreJobRepository.getAllJobsSummary(
            limit = limit,
            lastDocumentId = lastDocumentId,
            category = categoryQuery,
            userLatitude = if (hasLocation) userLatitude else null,
            userLongitude = if (hasLocation) userLongitude else null,
            radiusKm = 10.0
        )
        
        summaryFlow.collect { result ->
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
        var processedSummaries = summaries
        if (GeoUtils.hasValidCoordinates(userLatitude, userLongitude)) {
            processedSummaries = firestoreJobRepository.calculateSummaryDistances(
                summaries, userLatitude, userLongitude
            )
        }
        
        val newJobs = processedSummaries.map { it.toJobListing() }
        
        val mergedJobs = if (isLoadingMore) {
            PaginationHelper.appendJobs(_uiState.value.jobs, newJobs, maxInMemory = 0)
        } else {
            newJobs
        }

        val finalJobs = if (GeoUtils.hasValidCoordinates(userLatitude, userLongitude)) {
            com.example.dutype.engine.NearestJobsEngine.getNearbyJobs(mergedJobs, userLatitude, userLongitude)
        } else {
            mergedJobs
        }
        
        val lastJob = newJobs.lastOrNull()
        
        _uiState.value = _uiState.value.copy(
            jobs = finalJobs,
            isLoading = false,
            isLoadingMore = false,
            hasMore = PaginationHelper.hasMorePages(newJobs.size),
            lastDocumentId = lastJob?.id
        )
    }
}
