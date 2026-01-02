package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.JobApplication
import com.example.dutype.models.JobApplicationUiState
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.ApplicationStats
import com.example.dutype.models.JobVacancyStatus
import com.example.dutype.services.JobApplicationService
import com.example.dutype.state.ApplicationStateManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for managing job applications
 * Provides access to JobApplicationService and application state management
 * 
 * Used by both worker and employer screens to access job application functionality
 */
@HiltViewModel
class JobApplicationViewModel @Inject constructor(
    val jobApplicationService: JobApplicationService,
    private val applicationStateManager: ApplicationStateManager,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobApplicationUiState())
    val uiState: StateFlow<JobApplicationUiState> = _uiState.asStateFlow()
    
    private val _stats = MutableStateFlow(ApplicationStats())
    val stats: StateFlow<ApplicationStats> = _stats.asStateFlow()
    
    // Guard to prevent duplicate loadMyApplications calls
    private var hasInitiallyLoaded = false

    init {
        // Load applications on init
        loadMyApplications()
        
        // Listen to application state changes
        viewModelScope.launch {
            applicationStateManager.applications.collect { applications ->
                // Update UI state when application state changes
            }
        }
    }

    /**
     * Load all applications for current user
     */
    fun loadMyApplications() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(
                hasError = true,
                error = "User not authenticated"
            )
            return
        }
        
        // Skip if already loading or has loaded (prevents duplicate calls from recomposition)
        if (_uiState.value.isLoading && hasInitiallyLoaded) {
            return
        }
        
        // Skip if we already have applications and this is a duplicate call (not a refresh)
        if (hasInitiallyLoaded && _uiState.value.applications.isNotEmpty()) {
            return
        }
        
        hasInitiallyLoaded = true
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false)
            
            jobApplicationService.getWorkerApplications(currentUser.uid).collect { result ->
                result.fold(
                    onSuccess = { applications ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            applications = applications
                        )
                        loadApplicationStats()
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasError = true,
                            error = exception.message ?: "Failed to load applications"
                        )
                    }
                )
            }
        }
    }
    
    /**
     * Refresh applications (force reload)
     */
    fun refreshApplications() {
        hasInitiallyLoaded = false
        loadMyApplications()
    }

    /**
     * Withdraw application
     */
    fun withdrawApplication(applicationId: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)

            val currentUser = auth.currentUser
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(isSubmitting = false)
                onResult(false, "User not authenticated")
                return@launch
            }

            val result = jobApplicationService.withdrawApplication(applicationId, currentUser.uid)
            result.fold(
                onSuccess = {
                    // Remove from local state
                    val updatedApplications = _uiState.value.applications.filter { 
                        it.applicationId != applicationId 
                    }
                    _uiState.value = _uiState.value.copy(
                        applications = updatedApplications,
                        isSubmitting = false
                    )
                    // Update application state
                    applicationStateManager.removeAppliedJob(applicationId)
                    loadApplicationStats()
                    onResult(true, null)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        hasError = true,
                        error = exception.message ?: "Failed to withdraw application",
                        isSubmitting = false
                    )
                    onResult(false, exception.message)
                }
            )
        }
    }

    /**
     * Get job vacancy status
     */
    fun getJobVacancyStatus(jobId: String, onResult: (JobVacancyStatus?) -> Unit) {
        viewModelScope.launch {
            jobApplicationService.getJobVacancyStatus(jobId).fold(
                onSuccess = { status -> onResult(status) },
                onFailure = { onResult(null) }
            )
        }
    }
    
    /**
     * PERFORMANCE FIX: Get job vacancy statuses in BATCH
     * Eliminates N+1 query pattern - single call for multiple jobs
     */
    fun getJobVacancyStatusBatch(jobIds: List<String>, onResult: (Map<String, JobVacancyStatus>?) -> Unit) {
        viewModelScope.launch {
            jobApplicationService.getJobVacancyStatusBatch(jobIds).fold(
                onSuccess = { statusMap -> onResult(statusMap) },
                onFailure = { onResult(null) }
            )
        }
    }
    
    /**
     * Check if user has applied for a job
     */
    fun hasUserApplied(jobId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                onResult(false)
                return@launch
            }
            
            val result = jobApplicationService.hasUserApplied(jobId, currentUser.uid)
            result.fold(
                onSuccess = { hasApplied -> onResult(hasApplied) },
                onFailure = { onResult(false) }
            )
        }
    }
    
    /**
     * PERFORMANCE FIX: Batch pre-application check
     * Runs all eligibility checks in parallel instead of sequentially
     * Reduces 5+ API calls to a single parallel operation
     */
    fun preApplicationCheck(jobId: String, onResult: (JobApplicationService.PreApplicationCheckResult) -> Unit) {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                onResult(JobApplicationService.PreApplicationCheckResult(
                    canApply = false,
                    hasAlreadyApplied = false,
                    isProfileComplete = false,
                    hasReachedLimit = false,
                    errorMessage = "User not authenticated"
                ))
                return@launch
            }
            
            val result = jobApplicationService.preApplicationCheck(jobId, currentUser.uid)
            onResult(result)
        }
    }
    
    /**
     * Submit a job application
     */
    fun submitApplication(application: JobApplication) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, hasError = false)
            
            jobApplicationService.submitApplication(application).fold(
                onSuccess = { submittedApplication ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submissionSuccess = true,
                        applications = listOf(submittedApplication) + _uiState.value.applications
                    )
                    applicationStateManager.addAppliedJob(application.jobId)
                    loadApplicationStats()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        hasError = true,
                        error = exception.message ?: "Failed to submit application"
                    )
                }
            )
        }
    }

    /**
     * Load application statistics
     */
    private fun loadApplicationStats() {
        val currentUser = auth.currentUser ?: return
        
        viewModelScope.launch {
            jobApplicationService.getWorkerApplicationStats(currentUser.uid).fold(
                onSuccess = { stats ->
                    _stats.value = stats
                },
                onFailure = {
                    // Don't show error for stats, just keep default values
                }
            )
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(hasError = false, error = null)
    }
}
