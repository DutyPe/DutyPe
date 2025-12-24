package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.JobListing
import com.example.dutype.repositories.FirestoreJobRepository
import com.example.dutype.repositories.FirestoreSavedJobRepository
import com.example.dutype.state.SavedJobsStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber

data class FirestoreJobUiState(
    val jobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasError: Boolean = false,
    val currentPage: Int = 0,
    val hasMore: Boolean = true,
    val totalJobs: Int = 0,
    val lastCreatedAt: Long? = null,
    val isLoadingMore: Boolean = false
)

@HiltViewModel
class FirestoreJobViewModel @Inject constructor(
    private val firestoreJobRepository: FirestoreJobRepository,
    private val savedJobRepository: FirestoreSavedJobRepository,
    private val savedJobsStateManager: SavedJobsStateManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FirestoreJobUiState())
    val uiState: StateFlow<FirestoreJobUiState> = _uiState.asStateFlow()
    
    // User location for distance calculation
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0
    
    init {
        // Listen to centralized saved jobs state and update job saved status
        viewModelScope.launch {
            combine(
                savedJobsStateManager.savedJobIds,
                savedJobsStateManager.refreshTrigger
            ) { savedJobIds, _ ->
                // Update saved status for all current jobs
                val currentJobs = _uiState.value.jobs
                val updatedJobs = currentJobs.map { job ->
                    job.copy(isSaved = savedJobIds.contains(job.id))
                }
                _uiState.value = _uiState.value.copy(jobs = updatedJobs)
            }.collect { }
        }
    }
    
    /**
     * Set user location for distance calculation
     */
    fun setUserLocation(latitude: Double, longitude: Double) {
        userLatitude = latitude
        userLongitude = longitude
        Timber.d("📍 User location set: lat=$latitude, lon=$longitude")
        
        // Recalculate distances for existing jobs
        if (_uiState.value.jobs.isNotEmpty()) {
            val jobsWithDistance = firestoreJobRepository.calculateJobsDistances(
                _uiState.value.jobs, userLatitude, userLongitude
            )
            _uiState.value = _uiState.value.copy(jobs = jobsWithDistance)
        }
    }
    
    fun loadJobs(limit: Long = 50L) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, 
                error = null, 
                hasError = false,
                jobs = emptyList(), // Reset list on fresh load
                lastCreatedAt = null,
                hasMore = true
            )
            
            try {
                Timber.d("🔍 Loading all jobs for workers (limit: $limit)")
                firestoreJobRepository.getAllJobs(limit).collect { result ->
                    result.fold(
                        onSuccess = { jobs ->
                            Timber.d("✅ Successfully loaded ${jobs.size} jobs for workers")
                            // Update saved status for all jobs
                            var jobsWithSavedStatus = updateJobsSavedStatus(jobs)
                            
                            // Calculate distances if user location is available
                            if (userLatitude != 0.0 || userLongitude != 0.0) {
                                jobsWithSavedStatus = firestoreJobRepository.calculateJobsDistances(
                                    jobsWithSavedStatus, userLatitude, userLongitude
                                )
                            }
                            
                            val lastJob = jobsWithSavedStatus.lastOrNull()
                            
                            _uiState.value = _uiState.value.copy(
                                jobs = jobsWithSavedStatus,
                                isLoading = false,
                                totalJobs = jobsWithSavedStatus.size,
                                hasMore = jobsWithSavedStatus.size >= limit,
                                lastCreatedAt = lastJob?.postedAt
                            )
                        },
                        onFailure = { exception ->
                            Timber.w("❌ Failed to load jobs for workers: ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = exception.message ?: "Failed to load jobs"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e("❌ Exception loading jobs for workers: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load jobs"
                )
            }
        }
    }

    fun loadMoreJobs(limit: Long = 20L) {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            
            try {
                val lastCreatedAt = _uiState.value.lastCreatedAt
                Timber.d("🔍 Loading more jobs (limit: $limit, after: $lastCreatedAt)")
                
                firestoreJobRepository.getAllJobs(limit, lastCreatedAt).collect { result ->
                    result.fold(
                        onSuccess = { newJobs ->
                            Timber.d("✅ Successfully loaded ${newJobs.size} more jobs")
                            if (newJobs.isEmpty()) {
                                _uiState.value = _uiState.value.copy(
                                    isLoadingMore = false,
                                    hasMore = false
                                )
                            } else {
                                var jobsWithSavedStatus = updateJobsSavedStatus(newJobs)
                                
                                // Calculate distances if user location is available
                                if (userLatitude != 0.0 || userLongitude != 0.0) {
                                    jobsWithSavedStatus = firestoreJobRepository.calculateJobsDistances(
                                        jobsWithSavedStatus, userLatitude, userLongitude
                                    )
                                }
                                
                                val currentJobs = _uiState.value.jobs
                                val updatedList = currentJobs + jobsWithSavedStatus
                                val lastJob = jobsWithSavedStatus.lastOrNull()
                                
                                _uiState.value = _uiState.value.copy(
                                    jobs = updatedList,
                                    isLoadingMore = false,
                                    totalJobs = updatedList.size,
                                    hasMore = jobsWithSavedStatus.size >= limit,
                                    lastCreatedAt = lastJob?.postedAt
                                )
                            }
                        },
                        onFailure = { exception ->
                            Timber.w("❌ Failed to load more jobs: ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoadingMore = false,
                                // Don't set global error for pagination failure, maybe show toast
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e("❌ Exception loading more jobs: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false
                )
            }
        }
    }
    
    fun refreshJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null, hasError = false)
            
            try {
                firestoreJobRepository.getAllJobs(50L).collect { result ->
                    result.fold(
                        onSuccess = { jobs ->
                            var jobsWithDistance = jobs
                            // Calculate distances if user location is available
                            if (userLatitude != 0.0 || userLongitude != 0.0) {
                                jobsWithDistance = firestoreJobRepository.calculateJobsDistances(
                                    jobs, userLatitude, userLongitude
                                )
                            }
                            
                            _uiState.value = _uiState.value.copy(
                                jobs = jobsWithDistance,
                                isRefreshing = false,
                                currentPage = 0,
                                totalJobs = jobsWithDistance.size,
                                hasMore = jobsWithDistance.size >= 50L
                            )
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isRefreshing = false,
                                hasError = true,
                                error = exception.message ?: "Failed to refresh jobs"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    hasError = true,
                    error = e.message ?: "Failed to refresh jobs"
                )
            }
        }
    }
    
    fun searchJobs(query: String, limit: Long = 20L) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                firestoreJobRepository.searchJobs(query, limit).collect { result ->
                    result.fold(
                        onSuccess = { jobs ->
                            _uiState.value = _uiState.value.copy(
                                jobs = jobs,
                                isLoading = false,
                                totalJobs = jobs.size,
                                hasMore = false // Search results don't have pagination
                            )
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = exception.message ?: "Failed to search jobs"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to search jobs"
                )
            }
        }
    }
    
    fun getJobsByCategory(category: String, limit: Long = 20L) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                firestoreJobRepository.getJobsByCategory(category, limit).collect { result ->
                    result.fold(
                        onSuccess = { jobs ->
                            _uiState.value = _uiState.value.copy(
                                jobs = jobs,
                                isLoading = false,
                                totalJobs = jobs.size,
                                hasMore = false // Category results don't have pagination
                            )
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = exception.message ?: "Failed to load jobs by category"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load jobs by category"
                )
            }
        }
    }
    
    fun getJobsByLocation(location: String, limit: Long = 20L) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                firestoreJobRepository.getJobsByLocation(location, limit).collect { result ->
                    result.fold(
                        onSuccess = { jobs ->
                            _uiState.value = _uiState.value.copy(
                                jobs = jobs,
                                isLoading = false,
                                totalJobs = jobs.size,
                                hasMore = false // Location results don't have pagination
                            )
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = exception.message ?: "Failed to load jobs by location"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load jobs by location"
                )
            }
        }
    }
    
    /**
     * Get a specific job by ID
     */
    suspend fun getJobById(jobId: String): Result<JobListing?> {
        return try {
            Timber.d("🔍 Getting job by ID: $jobId")
            var result: Result<JobListing?> = Result.failure(Exception("Job not found"))
            
            firestoreJobRepository.getJobById(jobId).collect { jobResult ->
                result = jobResult
            }
            
            result.fold(
                onSuccess = { job ->
                    Timber.d("✅ Successfully retrieved job: ${job?.title}")
                },
                onFailure = { exception ->
                    Timber.w("❌ Failed to get job by ID: ${exception.message}")
                }
            )
            
            result
        } catch (e: Exception) {
            Timber.e("❌ Exception getting job by ID: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, hasError = false)
    }
    
    /**
     * Update saved status for a list of jobs
     */
    private suspend fun updateJobsSavedStatus(jobs: List<JobListing>): List<JobListing> {
        return jobs.map { job ->
            val isSavedResult = savedJobRepository.isJobSaved(job.jobId)
            val isSaved = isSavedResult.getOrElse { false }
            job.copy(isSaved = isSaved)
        }
    }
}
