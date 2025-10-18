package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.JobListing
import com.example.dutype.repositories.FirestoreJobRepository
import com.example.dutype.services.NotificationService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

data class FirestoreEmployerJobUiState(
    val myJobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasError: Boolean = false,
    val isCreatingJob: Boolean = false,
    val isUpdatingJob: Boolean = false,
    val isDeletingJob: Boolean = false
)

@HiltViewModel
class FirestoreEmployerJobViewModel @Inject constructor(
    private val firestoreJobRepository: FirestoreJobRepository,
    private val notificationService: NotificationService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FirestoreEmployerJobUiState())
    val uiState: StateFlow<FirestoreEmployerJobUiState> = _uiState.asStateFlow()
    
    private val currentUser = FirebaseAuth.getInstance().currentUser
    
    fun loadMyJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                val employerId = currentUser?.uid
                if (employerId == null) {
                    println("❌ Employer ID is null - user not authenticated")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasError = true,
                        error = "User not authenticated"
                    )
                    return@launch
                }
                
                println("🔍 Loading jobs for employer: $employerId")
                firestoreJobRepository.getJobsByEmployer(employerId).collect { result ->
                    result.fold(
                        onSuccess = { jobs ->
                            println("✅ Successfully loaded ${jobs.size} jobs for employer")
                            _uiState.value = _uiState.value.copy(
                                myJobs = jobs,
                                isLoading = false
                            )
                        },
                        onFailure = { exception ->
                            println("❌ Failed to load employer jobs: ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = exception.message ?: "Failed to load your jobs"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                println("❌ Exception loading employer jobs: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load your jobs"
                )
            }
        }
    }
    
    fun refreshMyJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null, hasError = false)
            
            try {
                val employerId = currentUser?.uid
                if (employerId == null) {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        hasError = true,
                        error = "User not authenticated"
                    )
                    return@launch
                }
                
                firestoreJobRepository.getJobsByEmployer(employerId).collect { result ->
                    result.fold(
                        onSuccess = { jobs ->
                            _uiState.value = _uiState.value.copy(
                                myJobs = jobs,
                                isRefreshing = false
                            )
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isRefreshing = false,
                                hasError = true,
                                error = exception.message ?: "Failed to refresh your jobs"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    hasError = true,
                    error = e.message ?: "Failed to refresh your jobs"
                )
            }
        }
    }
    
    fun createJob(jobData: Map<String, Any>, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingJob = true, error = null, hasError = false)
            
            try {
                val employerId = currentUser?.uid
                if (employerId == null) {
                    _uiState.value = _uiState.value.copy(
                        isCreatingJob = false,
                        hasError = true,
                        error = "User not authenticated"
                    )
                    callback(false, "User not authenticated")
                    return@launch
                }
                
                // Add employer ID to job data
                val jobDataWithEmployer = jobData.toMutableMap()
                jobDataWithEmployer["employerId"] = employerId
                // Note: employerName is set in PostJobScreen from profile data
                // Only set it if not already present in jobData
                if (!jobDataWithEmployer.containsKey("employerName")) {
                    jobDataWithEmployer["employerName"] = currentUser.displayName ?: "Unknown Employer"
                }
                
                firestoreJobRepository.createJob(jobDataWithEmployer).collect { result ->
                    result.fold(
                        onSuccess = { jobId ->
                            _uiState.value = _uiState.value.copy(
                                isCreatingJob = false
                            )
                            
                            // Send notification for job posted successfully
                            val jobTitle = jobData["title"] as? String ?: "New Job"
                            println("🔔 DEBUG: Attempting to send job posted notification")
                            println("🔔 DEBUG: jobTitle = $jobTitle")
                            println("🔔 DEBUG: employerId = $employerId")
                            
                            try {
                                println("🔔 DEBUG: Calling notificationService.sendJobPostedNotification")
                                notificationService.sendJobPostedNotification(jobTitle, employerId)
                                println("🔔 DEBUG: Job posted notification service call completed")
                            } catch (e: Exception) {
                                println("🔔 ERROR: Failed to send job posted notification: ${e.message}")
                            }
                            
                            // Refresh jobs to show the new one
                            loadMyJobs()
                            callback(true, null)
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isCreatingJob = false,
                                hasError = true,
                                error = exception.message ?: "Failed to create job"
                            )
                            callback(false, exception.message)
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCreatingJob = false,
                    hasError = true,
                    error = e.message ?: "Failed to create job"
                )
                callback(false, e.message)
            }
        }
    }
    
    fun updateJob(jobId: String, updates: Map<String, Any>, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingJob = true, error = null, hasError = false)
            
            try {
                firestoreJobRepository.updateJob(jobId, updates).collect { result ->
                    result.fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(
                                isUpdatingJob = false
                            )
                            // Refresh jobs to show the updated one
                            loadMyJobs()
                            callback(true, null)
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isUpdatingJob = false,
                                hasError = true,
                                error = exception.message ?: "Failed to update job"
                            )
                            callback(false, exception.message)
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUpdatingJob = false,
                    hasError = true,
                    error = e.message ?: "Failed to update job"
                )
                callback(false, e.message)
            }
        }
    }
    
    fun deleteJob(jobId: String, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingJob = true, error = null, hasError = false)
            
            try {
                firestoreJobRepository.deleteJob(jobId).collect { result ->
                    result.fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(
                                isDeletingJob = false,
                                myJobs = _uiState.value.myJobs.filter { it.id != jobId }
                            )
                            callback(true, null)
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isDeletingJob = false,
                                hasError = true,
                                error = exception.message ?: "Failed to delete job"
                            )
                            callback(false, exception.message)
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeletingJob = false,
                    hasError = true,
                    error = e.message ?: "Failed to delete job"
                )
                callback(false, e.message)
            }
        }
    }
    
    fun getJobById(jobId: String, callback: (JobListing?) -> Unit) {
        viewModelScope.launch {
            try {
                firestoreJobRepository.getJobById(jobId).collect { result ->
                    result.fold(
                        onSuccess = { job ->
                            callback(job)
                        },
                        onFailure = { exception ->
                            callback(null)
                        }
                    )
                }
            } catch (e: Exception) {
                callback(null)
            }
        }
    }
    
    fun toggleJobStatus(jobId: String) {
        viewModelScope.launch {
            try {
                // Find the job in the current list
                val job = _uiState.value.myJobs.find { it.id == jobId }
                if (job == null) {
                    println("❌ Job not found: $jobId")
                    return@launch
                }
                
                // Toggle the active status
                val newActiveStatus = !job.isActive
                val updates = mapOf(
                    "isActive" to newActiveStatus,
                    "updatedAt" to System.currentTimeMillis()
                )
                
                println("🔄 Toggling job $jobId status to: $newActiveStatus")
                
                firestoreJobRepository.updateJob(jobId, updates).collect { result ->
                    result.fold(
                        onSuccess = {
                            println("✅ Successfully toggled job status")
                            
                            // Send notification for job pause/activate
                            val employerId = currentUser?.uid
                            println("🔔 DEBUG: Attempting to send job pause notification")
                            println("🔔 DEBUG: employerId = $employerId")
                            println("🔔 DEBUG: job.title = ${job.title}")
                            println("🔔 DEBUG: newActiveStatus = $newActiveStatus")
                            println("🔔 DEBUG: isPaused = ${!newActiveStatus}")
                            
                            if (employerId != null) {
                                try {
                                    println("🔔 DEBUG: Calling notificationService.sendJobPausedNotification")
                                    notificationService.sendJobPausedNotification(
                                        jobTitle = job.title,
                                        isPaused = !newActiveStatus, // If newActiveStatus is false, job is paused
                                        employerId = employerId
                                    )
                                    println("🔔 DEBUG: Notification service call completed")
                                } catch (e: Exception) {
                                    println("🔔 ERROR: Failed to send notification: ${e.message}")
                                }
                            } else {
                                println("🔔 ERROR: employerId is null, cannot send notification")
                            }
                            
                            // Refresh jobs to show the updated status
                            loadMyJobs()
                        },
                        onFailure = { exception ->
                            println("❌ Failed to toggle job status: ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                hasError = true,
                                error = exception.message ?: "Failed to update job status"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                println("❌ Exception toggling job status: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    hasError = true,
                    error = e.message ?: "Failed to update job status"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, hasError = false)
    }
}
