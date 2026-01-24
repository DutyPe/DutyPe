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
    val lastCreatedAt: Long? = null
)

/**
 * ViewModel for Categories Screen
 * Handles pagination for both "All Jobs" and category-specific jobs
 * Loads 15 jobs at a time for smooth infinite scroll
 */
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val firestoreJobRepository: FirestoreJobRepository,
    private val locationPreferences: LocationPreferences
) : ViewModel() {
    
    companion object {
        private const val PAGE_SIZE = 15L // Instagram standard - fast loads
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
     */
    fun loadJobsForCategory(category: String) {
        // Skip if already loading this category
        if (_uiState.value.isLoading && _uiState.value.currentCategory == category) {
            Timber.d("📦 Already loading $category, skipping duplicate call")
            return
        }
        
        Timber.d("📦 Loading jobs for category: $category")
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                jobs = emptyList(),
                hasMore = true,
                lastCreatedAt = null,
                currentCategory = category,
                error = null
            )
            
            try {
                if (category == "All") {
                    loadAllJobs(PAGE_SIZE)
                } else {
                    loadCategoryJobs(category, PAGE_SIZE)
                }
            } catch (e: Exception) {
                Timber.e("Error loading jobs for $category: ${e.message}")
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
                lastCreatedAt = null,
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
        
        if (currentState.isLoadingMore || !currentState.hasMore || currentState.isLoading) {
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            
            try {
                if (currentState.currentCategory == "All") {
                    loadAllJobs(PAGE_SIZE, currentState.lastCreatedAt)
                } else {
                    loadCategoryJobs(currentState.currentCategory, PAGE_SIZE, currentState.lastCreatedAt)
                }
            } catch (e: Exception) {
                Timber.e("Error loading more jobs: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }
    
    /**
     * Load all jobs with pagination
     * Uses proper pagination - loads 30 jobs at a time
     * NO MEMORY LIMITS - loads all jobs progressively
     */
    private suspend fun loadAllJobs(limit: Long, lastCreatedAt: Long? = null) {
        Timber.d("📦 Loading all jobs with limit=$limit, after=$lastCreatedAt")
        
        firestoreJobRepository.getAllJobsSummary(limit, lastCreatedAt).collect { result ->
            result.fold(
                onSuccess = { summaries ->
                    processLoadedJobs(summaries, limit, lastCreatedAt != null)
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
     * Load jobs for a specific category with pagination
     * Loads 15 jobs at a time for smooth scrolling
     * NO MEMORY LIMITS - loads all category jobs progressively
     */
    private suspend fun loadCategoryJobs(category: String, limit: Long, lastCreatedAt: Long? = null) {
        // Map display name to category enum name for query
        val categoryQuery = JobCategory.entries.find { 
            it.displayName.equals(category, ignoreCase = true) 
        }?.name ?: category.uppercase()
        
        Timber.d("📦 Loading category jobs: display='$category' -> query='$categoryQuery', limit=$limit")
        
        firestoreJobRepository.getJobsByCategoryPaginated(categoryQuery, limit, lastCreatedAt).collect { result ->
            result.fold(
                onSuccess = { summaries ->
                    processLoadedJobs(summaries, limit, lastCreatedAt != null)
                },
                onFailure = { e ->
                    Timber.w("Failed to load jobs for category $category: ${e.message}")
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
     */
    private fun processLoadedJobs(
        summaries: List<JobListingSummary>,
        limit: Long,
        isLoadingMore: Boolean
    ) {
        Timber.d("📦 Loaded ${summaries.size} jobs (isLoadingMore=$isLoadingMore)")
        
        // Calculate distances if user location is available
        var processedSummaries = summaries
        if (userLatitude != 0.0 || userLongitude != 0.0) {
            processedSummaries = firestoreJobRepository.calculateSummaryDistances(
                summaries, userLatitude, userLongitude
            )
        }
        
        // Convert to JobListing
        val newJobs = processedSummaries.map { it.toJobListing() }
        
        // Append or replace based on whether this is pagination
        val updatedJobs = if (isLoadingMore) {
            _uiState.value.jobs + newJobs
        } else {
            newJobs
        }
        
        val lastJob = newJobs.lastOrNull()
        
        // FIXED: hasMore is true ONLY if we got results
        // This allows loading ALL jobs until Firestore returns empty
        val hasMore = newJobs.isNotEmpty()
        
        _uiState.value = _uiState.value.copy(
            jobs = updatedJobs,
            isLoading = false,
            isLoadingMore = false,
            hasMore = hasMore,
            lastCreatedAt = lastJob?.postedAt
        )
        
        Timber.d("📦 Total jobs now: ${updatedJobs.size}, hasMore=$hasMore (got ${newJobs.size} new jobs)")
    }
}
