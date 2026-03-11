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
import kotlin.onSuccess

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
    private val auth: FirebaseAuth,
    private val performanceTracker: com.example.dutype.performance.PerformanceTracker
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
            Timber.e("❌ loadMyApplications: User not authenticated")
            _uiState.value = _uiState.value.copy(
                hasError = true,
                error = "User not authenticated"
            )
            return
        }
        
        Timber.d("📱 loadMyApplications: Starting for userId=${currentUser.uid}")
        
        // Skip if already loading or has loaded (prevents duplicate calls from recomposition)
        if (_uiState.value.isLoading && hasInitiallyLoaded) {
            Timber.d("📱 loadMyApplications: Already loading, skipping")
            return
        }
        
        // Skip if we already have applications and this is a duplicate call (not a refresh)
        if (hasInitiallyLoaded && _uiState.value.applications.isNotEmpty()) {
            Timber.d("📱 loadMyApplications: Already loaded ${_uiState.value.applications.size} applications, skipping")
            return
        }
        
        hasInitiallyLoaded = true
        
        viewModelScope.launch {
            com.example.dutype.performance.MainThreadChecker.assertMainThread()
            performanceTracker.trackOperation("loadMyApplications")
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false)
            
            Timber.d("📱 loadMyApplications: Calling jobApplicationService.getWorkerApplications")
            
            jobApplicationService.getWorkerApplications(currentUser.uid).collect { result ->
                result.fold(
                    onSuccess = { applications: List<JobApplication> ->
                        Timber.i("✅ loadMyApplications: SUCCESS - Loaded ${applications.size} applications")
                        applications.forEachIndexed { index, app ->
                            Timber.d("  [$index] ${app.jobTitle} - ${app.status} - appliedAt=${app.appliedAt}")
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            applications = applications
                        )
                        // Update shared state for other ViewModels
                        applicationStateManager.updateApplications(applications)
                        loadApplicationStats()
                    },
                    onFailure = { exception: Throwable ->
                        Timber.e(exception, "❌ loadMyApplications: FAILED - ${exception.message}")
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
            result.onSuccess {
                // Remove from local state
                val updatedApplications = _uiState.value.applications.filter { 
                    it.id != applicationId 
                }
                _uiState.value = _uiState.value.copy(
                    applications = updatedApplications,
                    isSubmitting = false
                )
                // Update application state
                applicationStateManager.removeAppliedJob(applicationId)
                loadApplicationStats()
                onResult(true, null)
            }.onFailure { exception: Throwable ->
                _uiState.value = _uiState.value.copy(
                    hasError = true,
                    error = exception.message ?: "Failed to withdraw application",
                    isSubmitting = false
                )
                onResult(false, exception.message ?: "Failed to withdraw application")
            }
        }
    }

    /**
     * Get job vacancy status
     */
    fun getJobVacancyStatus(jobId: String, onResult: (JobVacancyStatus?) -> Unit) {
        viewModelScope.launch {
            jobApplicationService.getJobVacancyStatus(jobId)
                .onSuccess { status -> onResult(status) }
                .onFailure { onResult(null) }
        }
    }
    
    /**
     * PERFORMANCE FIX: Get job vacancy statuses in BATCH
     * Eliminates N+1 query pattern - single call for multiple jobs
     */
    fun getJobVacancyStatusBatch(jobIds: List<String>, onResult: (Map<String, JobVacancyStatus>?) -> Unit) {
        viewModelScope.launch {
            jobApplicationService.getJobVacancyStatusBatch(jobIds)
                .onSuccess { statusMap -> onResult(statusMap) }
                .onFailure { onResult(null) }
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
            result.onSuccess { hasApplied -> onResult(hasApplied) }
                .onFailure { onResult(false) }
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
            com.example.dutype.performance.MainThreadChecker.assertMainThread()
            performanceTracker.trackOperation("submitApplication")
            _uiState.value = _uiState.value.copy(isSubmitting = true, hasError = false)
            
            jobApplicationService.submitApplication(application)
                .onSuccess { submittedApplication ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submissionSuccess = true,
                        applications = listOf(submittedApplication) + _uiState.value.applications
                    )
                    applicationStateManager.addAppliedJob(application.id)
                    loadApplicationStats()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        hasError = true,
                        error = exception.message ?: "Failed to submit application"
                    )
                }
        }
    }

    /**
     * Load application statistics
     */
    private fun loadApplicationStats() {
        val currentUser = auth.currentUser ?: return
        
        viewModelScope.launch {
            val result = jobApplicationService.getWorkerApplicationStats(currentUser.uid)
            result.onSuccess { stats ->
                _stats.value = stats
            }.onFailure { _ ->
                // Don't show error for stats, just keep default values
            }
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(hasError = false, error = "")
    }
}
