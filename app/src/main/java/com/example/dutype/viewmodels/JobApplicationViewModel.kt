package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.JobApplication
import com.example.dutype.models.JobApplicationUiState
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.ApplicationStats
import com.example.dutype.services.JobApplicationService
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Professional Job Application ViewModel
 * Manages job application state and operations
 */
@HiltViewModel
class JobApplicationViewModel @Inject constructor(
    private val jobApplicationService: JobApplicationService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(JobApplicationUiState())
    val uiState: StateFlow<JobApplicationUiState> = _uiState.asStateFlow()
    
    private val _stats = MutableStateFlow(ApplicationStats())
    val stats: StateFlow<ApplicationStats> = _stats.asStateFlow()
    
    private val auth = FirebaseAuth.getInstance()
    
    init {
        loadMyApplications()
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
     * Load applications by status
     */
    fun loadApplicationsByStatus(status: ApplicationStatus) {
        val currentUser = auth.currentUser
        if (currentUser == null) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false)
            
            jobApplicationService.getApplicationsByStatus(currentUser.uid, status).collect { result ->
                result.fold(
                    onSuccess = { applications ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            applications = applications
                        )
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
     * Check if user has already applied to a job
     */
    fun hasAppliedToJob(jobId: String, onResult: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onResult(false)
            return
        }
        
        viewModelScope.launch {
            jobApplicationService.hasWorkerAppliedToJob(currentUser.uid, jobId).fold(
                onSuccess = { hasApplied ->
                    onResult(hasApplied)
                },
                onFailure = {
                    onResult(false)
                }
            )
        }
    }
    
    /**
     * Withdraw an application
     */
    fun withdrawApplication(applicationId: String) {
        val currentUser = auth.currentUser ?: return
        
        viewModelScope.launch {
            jobApplicationService.withdrawApplication(applicationId, currentUser.uid).fold(
                onSuccess = {
                    // Update local state
                    val updatedApplications = _uiState.value.applications.map { app ->
                        if (app.applicationId == applicationId) {
                            app.copy(status = ApplicationStatus.WITHDRAWN)
                        } else {
                            app
                        }
                    }
                    _uiState.value = _uiState.value.copy(applications = updatedApplications)
                    loadApplicationStats()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        hasError = true,
                        error = exception.message ?: "Failed to withdraw application"
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
            jobApplicationService.getApplicationStats(currentUser.uid).fold(
                onSuccess = { statsMap ->
                    val stats = ApplicationStats(
                        totalApplications = statsMap["total"] ?: 0,
                        pendingApplications = statsMap["status_pending"] ?: 0,
                        shortlistedApplications = statsMap["status_shortlisted"] ?: 0,
                        interviewedApplications = statsMap["status_interviewed"] ?: 0,
                        selectedApplications = statsMap["status_selected"] ?: 0,
                        rejectedApplications = statsMap["status_rejected"] ?: 0,
                        thisMonthApplications = statsMap["this_month"] ?: 0,
                        responseRate = calculateResponseRate(statsMap)
                    )
                    _stats.value = stats
                },
                onFailure = {
                    // Don't show error for stats, just keep default values
                }
            )
        }
    }
    
    /**
     * Calculate response rate percentage
     */
    private fun calculateResponseRate(statsMap: Map<String, Int>): Float {
        val total = statsMap["total"] ?: 0
        if (total == 0) return 0f
        
        val responded = (statsMap["status_reviewed"] ?: 0) + 
                       (statsMap["status_shortlisted"] ?: 0) + 
                       (statsMap["status_rejected"] ?: 0)
        
        return (responded.toFloat() / total.toFloat()) * 100f
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(hasError = false, error = null)
    }
    
    /**
     * Clear submission success state
     */
    fun clearSubmissionSuccess() {
        _uiState.value = _uiState.value.copy(submissionSuccess = false)
    }
    
    /**
     * Refresh applications
     */
    fun refreshApplications() {
        loadMyApplications()
    }
}
