package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.models.JobListing
import com.example.partimes.repositories.FirestoreJobRepository
import com.example.partimes.repositories.FirestoreSavedJobRepository
import com.example.partimes.state.SavedJobsStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

data class FirestoreJobUiState(
    val jobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasError: Boolean = false,
    val currentPage: Int = 0,
    val hasMore: Boolean = true,
    val totalJobs: Int = 0
)

@HiltViewModel
class FirestoreJobViewModel @Inject constructor(
    private val firestoreJobRepository: FirestoreJobRepository,
    private val savedJobRepository: FirestoreSavedJobRepository,
    private val savedJobsStateManager: SavedJobsStateManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FirestoreJobUiState())
    val uiState: StateFlow<FirestoreJobUiState> = _uiState.asStateFlow()
    
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
    
    fun loadJobs(limit: Long = 50L) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                println("🔍 Loading all jobs for workers (limit: $limit)")
                firestoreJobRepository.getAllJobs(limit).collect { result ->
                    result.fold(
                        onSuccess = { jobs ->
                            println("✅ Successfully loaded ${jobs.size} jobs for workers")
                            // Update saved status for all jobs
                            val jobsWithSavedStatus = updateJobsSavedStatus(jobs)
                            _uiState.value = _uiState.value.copy(
                                jobs = jobsWithSavedStatus,
                                isLoading = false,
                                totalJobs = jobsWithSavedStatus.size,
                                hasMore = jobsWithSavedStatus.size >= limit
                            )
                        },
                        onFailure = { exception ->
                            println("❌ Failed to load jobs for workers: ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = exception.message ?: "Failed to load jobs"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                println("❌ Exception loading jobs for workers: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load jobs"
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
                            _uiState.value = _uiState.value.copy(
                                jobs = jobs,
                                isRefreshing = false,
                                currentPage = 0,
                                totalJobs = jobs.size,
                                hasMore = jobs.size >= 50L
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
    
    fun incrementJobViewCount(jobId: String) {
        viewModelScope.launch {
            try {
                firestoreJobRepository.incrementJobViewCount(jobId).collect { result ->
                    // We don't need to handle the result for view count increment
                    // It's a background operation
                }
            } catch (e: Exception) {
                // Silently fail for view count increment
            }
        }
    }
    
    /**
     * Get a specific job by ID
     */
    suspend fun getJobById(jobId: String): Result<JobListing?> {
        return try {
            println("🔍 Getting job by ID: $jobId")
            var result: Result<JobListing?> = Result.failure(Exception("Job not found"))
            
            firestoreJobRepository.getJobById(jobId).collect { jobResult ->
                result = jobResult
            }
            
            result.fold(
                onSuccess = { job ->
                    println("✅ Successfully retrieved job: ${job?.title}")
                },
                onFailure = { exception ->
                    println("❌ Failed to get job by ID: ${exception.message}")
                }
            )
            
            result
        } catch (e: Exception) {
            println("❌ Exception getting job by ID: ${e.message}")
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
