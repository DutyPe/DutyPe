package com.example.dutype.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.JobApplication
import com.example.dutype.models.JobApplicationUiState
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.ApplicationStats
import com.example.dutype.models.JobVacancyStatus
import com.example.dutype.services.JobApplicationService
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.state.ApplicationStateManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Enterprise-level Job Application ViewModel
 * CONSOLIDATED: Merged functionality from JobApplicationViewModel
 * 
 * Handles:
 * - Job applications with profile completion checks
 * - Loading and managing user's applications
 * - Application statistics
 * - Job vacancy status
 * - Application withdrawal
 */
@HiltViewModel
class SmartJobApplicationViewModel @Inject constructor(
    val jobApplicationService: JobApplicationService,
    private val profileCompletionService: ProfileCompletionService,
    private val applicationStateManager: ApplicationStateManager,
    private val auth: FirebaseAuth,
    val reportingService: com.example.dutype.services.ReportingService,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // =============================================================================
    // UI STATE
    // =============================================================================
    
    private val _uiState = MutableStateFlow(SmartJobApplicationUiState())
    val uiState: StateFlow<SmartJobApplicationUiState> = _uiState.asStateFlow()
    
    // Legacy UI state for backward compatibility with screens using JobApplicationUiState
    private val _legacyUiState = MutableStateFlow(JobApplicationUiState())
    val legacyUiState: StateFlow<JobApplicationUiState> = _legacyUiState.asStateFlow()
    
    private val _stats = MutableStateFlow(ApplicationStats())
    val stats: StateFlow<ApplicationStats> = _stats.asStateFlow()

    // =============================================================================
    // PROFILE COMPLETION STATE
    // =============================================================================
    
    private val _canApplyDirectly = MutableStateFlow(false)
    val canApplyDirectly: StateFlow<Boolean> = _canApplyDirectly.asStateFlow()

    private val _profileCompletionPercentage = MutableStateFlow(0)
    val profileCompletionPercentage: StateFlow<Int> = _profileCompletionPercentage.asStateFlow()

    private val _missingFields = MutableStateFlow<List<String>>(emptyList())
    val missingFields: StateFlow<List<String>> = _missingFields.asStateFlow()
    
    // =============================================================================
    // FILTER STATE (from JobApplicationViewModel)
    // =============================================================================
    
    private var _selectedStatusFilter: ApplicationStatus? = savedStateHandle.get<String>("selectedStatusFilter")?.let { 
        try { ApplicationStatus.valueOf(it) } catch (e: Exception) { null }
    }
    
    val selectedStatusFilter: ApplicationStatus?
        get() = _selectedStatusFilter
    
    fun setStatusFilter(status: ApplicationStatus?) {
        _selectedStatusFilter = status
        savedStateHandle["selectedStatusFilter"] = status?.name
    }
    
    // Guard to prevent duplicate loadMyApplications calls
    private var hasInitiallyLoaded = false
    
    // Guard to prevent duplicate loadUserCapabilities calls
    private var hasLoadedCapabilities = false

    init {
        // Load user's application capabilities (only once)
        loadUserCapabilities()
        
        // NOTE: Applications are NOT loaded here to avoid duplicate API calls
        // JobApplicationViewModel already loads applications on init
        // SmartJobApplicationViewModel listens to ApplicationStateManager for shared state
        
        // Listen to application state changes (shared state from JobApplicationViewModel)
        viewModelScope.launch {
            applicationStateManager.applications.collect { applications ->
                _uiState.value = _uiState.value.copy(applications = applications)
                _legacyUiState.value = _legacyUiState.value.copy(applications = applications)
            }
        }
    }

    // =============================================================================
    // APPLICATION LOADING (from JobApplicationViewModel)
    // =============================================================================
    
    /**
     * Load all applications for current user
     */
    fun loadMyApplications() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _legacyUiState.value = _legacyUiState.value.copy(
                hasError = true,
                error = "User not authenticated"
            )
            return
        }
        
        // Skip if already loading or has loaded (prevents duplicate calls from recomposition)
        if (_legacyUiState.value.isLoading && hasInitiallyLoaded) {
            return
        }
        
        // Skip if we already have applications and this is a duplicate call (not a refresh)
        if (hasInitiallyLoaded && _legacyUiState.value.applications.isNotEmpty()) {
            return
        }
        
        hasInitiallyLoaded = true
        
        viewModelScope.launch {
            _legacyUiState.value = _legacyUiState.value.copy(isLoading = true, hasError = false)
            
            jobApplicationService.getWorkerApplications(currentUser.uid).collect { result ->
                result.fold(
                    onSuccess = { applications ->
                        _legacyUiState.value = _legacyUiState.value.copy(
                            isLoading = false,
                            applications = applications
                        )
                        _uiState.value = _uiState.value.copy(applications = applications)
                        loadApplicationStats()
                    },
                    onFailure = { exception ->
                        _legacyUiState.value = _legacyUiState.value.copy(
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
        val currentUser = auth.currentUser ?: return
        
        viewModelScope.launch {
            _legacyUiState.value = _legacyUiState.value.copy(isLoading = true, hasError = false)
            
            jobApplicationService.getApplicationsByStatus(currentUser.uid, status).collect { result ->
                result.fold(
                    onSuccess = { applications ->
                        _legacyUiState.value = _legacyUiState.value.copy(
                            isLoading = false,
                            applications = applications
                        )
                    },
                    onFailure = { exception ->
                        _legacyUiState.value = _legacyUiState.value.copy(
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
     * Refresh applications
     */
    fun refreshApplications() {
        hasInitiallyLoaded = false
        loadMyApplications()
    }

    // =============================================================================
    // PROFILE CAPABILITIES
    // =============================================================================
    
    /**
     * Load user's application capabilities
     * OPTIMIZED: Single API call instead of 3 separate calls
     */
    private fun loadUserCapabilities() {
        // Skip if already loaded (prevents duplicate calls from recomposition)
        if (hasLoadedCapabilities) {
            return
        }
        hasLoadedCapabilities = true
        
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                try {
                    // Single API call to get all profile completion data
                    val status = profileCompletionService.getProfileCompletionStatus(
                        currentUser.uid,
                        com.example.dutype.models.UserRole.WORKER.name
                    )
                    
                    // Update all states from single response
                    _profileCompletionPercentage.value = status.completionPercentage
                    _canApplyDirectly.value = status.completionPercentage >= 80
                    _missingFields.value = status.missingFields
                    
                    Timber.d("🔍 loadUserCapabilities - completion: ${status.completionPercentage}%, canApply: ${status.completionPercentage >= 80}")
                } catch (e: Exception) {
                    Timber.e(e, "❌ loadUserCapabilities - Error: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Refresh user capabilities (force reload)
     */
    fun refreshCapabilities() {
        hasLoadedCapabilities = false
        loadUserCapabilities()
    }

    // =============================================================================
    // APPLICATION OPERATIONS
    // =============================================================================

    /**
     * Apply for a job
     */
    fun applyForJob(
        jobId: String,
        coverLetter: String? = null,
        additionalNotes: String? = null
    ) {
        viewModelScope.launch {
            Timber.d("🚀 SmartJobApplicationViewModel: Starting application for jobId: $jobId")
            _uiState.value = _uiState.value.copy(isApplying = true, error = null)
            _legacyUiState.value = _legacyUiState.value.copy(isSubmitting = true, hasError = false)

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Timber.e("❌ SmartJobApplicationViewModel: User not authenticated")
                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    error = "User not authenticated",
                    applicationSuccess = false
                )
                _legacyUiState.value = _legacyUiState.value.copy(
                    isSubmitting = false,
                    hasError = true,
                    error = "User not authenticated"
                )
                return@launch
            }
            
            Timber.d("🚀 SmartJobApplicationViewModel: Calling applyForJob for user: ${currentUser.uid}")
            val result = jobApplicationService.applyForJob(jobId, currentUser.uid, coverLetter, additionalNotes)
            result.fold(
                onSuccess = { application ->
                    Timber.d("✅ SmartJobApplicationViewModel: Application successful! applicationId: ${application.applicationId}")
                    _uiState.value = _uiState.value.copy(
                        isApplying = false,
                        lastApplication = application,
                        applicationSuccess = true
                    )
                    _legacyUiState.value = _legacyUiState.value.copy(
                        isSubmitting = false,
                        submissionSuccess = true,
                        applications = listOf(application) + _legacyUiState.value.applications
                    )
                    // Update application state
                    applicationStateManager.addAppliedJob(jobId)
                    loadApplicationStats()
                },
                onFailure = { exception ->
                    Timber.e("❌ SmartJobApplicationViewModel: Application failed - ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isApplying = false,
                        error = exception.message,
                        applicationSuccess = false
                    )
                    _legacyUiState.value = _legacyUiState.value.copy(
                        isSubmitting = false,
                        hasError = true,
                        error = exception.message ?: "Failed to submit application"
                    )
                }
            )
        }
    }
    
    /**
     * Submit a job application (direct submission)
     */
    fun submitApplication(application: JobApplication) {
        viewModelScope.launch {
            _legacyUiState.value = _legacyUiState.value.copy(isSubmitting = true, hasError = false)
            
            jobApplicationService.submitApplication(application).fold(
                onSuccess = { submittedApplication ->
                    _legacyUiState.value = _legacyUiState.value.copy(
                        isSubmitting = false,
                        submissionSuccess = true,
                        applications = listOf(submittedApplication) + _legacyUiState.value.applications
                    )
                    _uiState.value = _uiState.value.copy(
                        lastApplication = submittedApplication,
                        applicationSuccess = true
                    )
                    applicationStateManager.addAppliedJob(application.jobId)
                    loadApplicationStats()
                },
                onFailure = { exception ->
                    _legacyUiState.value = _legacyUiState.value.copy(
                        isSubmitting = false,
                        hasError = true,
                        error = exception.message ?: "Failed to submit application"
                    )
                }
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
                onSuccess = { hasApplied ->
                    onResult(hasApplied)
                },
                onFailure = { exception ->
                    Timber.e(exception, "Error checking application status")
                    onResult(false)
                }
            )
        }
    }
    
    /**
     * Check if user has already applied to a job (alias for hasUserApplied)
     */
    fun hasAppliedToJob(jobId: String, onResult: (Boolean) -> Unit) {
        hasUserApplied(jobId, onResult)
    }

    /**
     * Get application status for a job
     */
    fun getApplicationStatus(jobId: String): ApplicationStatus? {
        return applicationStateManager.getApplicationStatus(jobId)
    }

    /**
     * Withdraw application
     */
    fun withdrawApplication(applicationId: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWithdrawing = true, error = null)
            _legacyUiState.value = _legacyUiState.value.copy(isSubmitting = true)

            val currentUser = auth.currentUser
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(
                    isWithdrawing = false,
                    error = "User not authenticated"
                )
                _legacyUiState.value = _legacyUiState.value.copy(isSubmitting = false)
                onResult(false, "User not authenticated")
                return@launch
            }

            val result = jobApplicationService.withdrawApplication(applicationId, currentUser.uid)
            result.fold(
                onSuccess = {
                    // Remove from local state
                    val updatedApplications = _legacyUiState.value.applications.filter { 
                        it.applicationId != applicationId 
                    }
                    _uiState.value = _uiState.value.copy(
                        isWithdrawing = false,
                        withdrawalSuccess = true
                    )
                    _legacyUiState.value = _legacyUiState.value.copy(
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
                        isWithdrawing = false,
                        error = exception.message,
                        withdrawalSuccess = false
                    )
                    _legacyUiState.value = _legacyUiState.value.copy(
                        hasError = true,
                        error = exception.message ?: "Failed to withdraw application",
                        isSubmitting = false
                    )
                    onResult(false, exception.message)
                }
            )
        }
    }

    // =============================================================================
    // STATISTICS & VACANCY STATUS
    // =============================================================================
    
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

    // =============================================================================
    // ERROR HANDLING
    // =============================================================================

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
        _legacyUiState.value = _legacyUiState.value.copy(hasError = false, error = null)
    }

    /**
     * Clear success states
     */
    fun clearSuccessStates() {
        _uiState.value = _uiState.value.copy(
            applicationSuccess = false,
            withdrawalSuccess = false
        )
        _legacyUiState.value = _legacyUiState.value.copy(submissionSuccess = false)
    }
    
    /**
     * Clear submission success state (alias for backward compatibility)
     */
    fun clearSubmissionSuccess() {
        clearSuccessStates()
    }
}

/**
 * UI State for Smart Job Application
 */
data class SmartJobApplicationUiState(
    val isApplying: Boolean = false,
    val isWithdrawing: Boolean = false,
    val applications: List<JobApplication> = emptyList(),
    val lastApplication: JobApplication? = null,
    val applicationSuccess: Boolean = false,
    val withdrawalSuccess: Boolean = false,
    val error: String? = null
)
