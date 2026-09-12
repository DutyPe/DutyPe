package com.example.dutype.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.JobApplication
import com.example.dutype.models.JobApplicationUiState
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobVacancyStatus
import com.example.dutype.services.JobApplicationService
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.models.UserRole
import com.example.dutype.state.ApplicationStateManager
import com.example.dutype.models.ApplicationStats
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
    val jobInteractionService: com.example.dutype.services.JobInteractionService,
    val reportingService: com.example.dutype.services.ReportingService,
    val jobCallFeedbackService: com.example.dutype.services.JobCallFeedbackService,
    private val savedStateHandle: SavedStateHandle,
    private val profileSetupStateManager: com.example.dutype.state.ProfileSetupStateManager
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
    val appliedJobIds: StateFlow<Set<String>> = applicationStateManager.appliedJobIds
    val applicationStatuses: StateFlow<Map<String, ApplicationStatus>> = applicationStateManager.applicationStatuses

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
    private var myApplicationsJob: kotlinx.coroutines.Job? = null
    
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
                loadApplicationStats()
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
            return
        }
        
        // Skip if already loading or has loaded (prevents duplicate calls from recomposition)
        if (myApplicationsJob != null && myApplicationsJob?.isActive == true) {
            return
        }
        
        // Skip if we already have applications and this is a duplicate call (not a refresh)
        if (hasInitiallyLoaded && _legacyUiState.value.applications.isNotEmpty()) {
            return
        }
        
        hasInitiallyLoaded = true
        
        myApplicationsJob = viewModelScope.launch {
            _legacyUiState.value = _legacyUiState.value.copy(isLoading = true)
            
            jobApplicationService.getWorkerApplications(currentUser.uid).collect { result ->
                result.fold(
                    onSuccess = { applications: List<JobApplication> ->
                        applicationStateManager.updateApplications(applications)
                        _legacyUiState.value = _legacyUiState.value.copy(
                            isLoading = false,
                            applications = applications
                        )
                        _uiState.value = _uiState.value.copy(applications = applications)
                        loadApplicationStats()
                    },
                    onFailure = { _: Throwable ->
                        _legacyUiState.value = _legacyUiState.value.copy(
                            isLoading = false
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
            _legacyUiState.value = _legacyUiState.value.copy(isLoading = true)
            
            jobApplicationService.getApplicationsByStatus(currentUser.uid, status).collect { result ->
                result.fold(
                    onSuccess = { applications: List<JobApplication> ->
                        _legacyUiState.value = _legacyUiState.value.copy(
                            isLoading = false,
                            applications = applications
                        )
                        _uiState.value = _uiState.value.copy(applications = applications)
                    },
                    onFailure = { _: Throwable ->
                        _legacyUiState.value = _legacyUiState.value.copy(
                            isLoading = false
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
        myApplicationsJob?.cancel()
        myApplicationsJob = null
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
                    // Get current user role dynamically, fallback to WORKER if not set
                    val userRole = profileSetupStateManager.getUserRole() ?: UserRole.WORKER

                    // Single API call to get all profile completion data
                    val status = profileCompletionService.getProfileCompletionStatus(
                        currentUser.uid,
                        userRole.name
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
     * Apply for a job with optional 15-second audio intro
     */
    fun applyForJob(
        jobId: String,
        coverLetter: String? = null,
        audioFile: java.io.File? = null,
        audioDurationSec: Int? = null,
        expectedSalary: String? = null,
        distanceKm: Double? = null
    ) {
        viewModelScope.launch {
            Timber.d("🚀 SmartJobApplicationViewModel: Starting application for jobId: $jobId")
            _uiState.value = _uiState.value.copy(isApplying = true, isSubmitting = true, error = null)

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Timber.e("❌ SmartJobApplicationViewModel: User not authenticated")
                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    isSubmitting = false,
                    error = "User not authenticated",
                    applicationSuccess = false
                )
                return@launch
            }

            var uploadedAudioUrl: String? = null
            if (audioFile != null && audioFile.exists()) {
                val duration = audioDurationSec ?: 15
                val uploadResult = com.example.dutype.utils.AudioRecordingHelper.uploadToFirebaseStorage(
                    file = audioFile,
                    userId = currentUser.uid,
                    durationSec = duration
                )
                if (uploadResult.isSuccess) {
                    uploadedAudioUrl = uploadResult.getOrNull()?.first
                    Timber.d("🎙️ Audio intro uploaded successfully: $uploadedAudioUrl")
                } else {
                    Timber.w("Audio intro upload failed, proceeding without audio: ${uploadResult.exceptionOrNull()?.message}")
                }
            }
            
            Timber.d("🚀 SmartJobApplicationViewModel: Calling applyForJob for user: ${currentUser.uid}")
            val result = jobApplicationService.applyForJob(
                jobId = jobId,
                userId = currentUser.uid,
                coverLetter = coverLetter,
                audioIntroUrl = uploadedAudioUrl,
                audioDurationSec = if (uploadedAudioUrl != null) audioDurationSec else null,
                expectedSalary = expectedSalary,
                distanceKm = distanceKm
            )
            result.onSuccess { application ->
                val canonicalId = application.id
                Timber.d("✅ SmartJobApplicationViewModel: Application successful! applicationId: ${application.id}")
                val updatedApplications = listOf(application) + _legacyUiState.value.applications
                    .filterNot { it.id == canonicalId || it.jobId == application.jobId }
                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    isSubmitting = false,
                    lastApplication = application,
                    applications = updatedApplications,
                    applicationSuccess = true,
                    submissionSuccess = true
                )
                _legacyUiState.value = _legacyUiState.value.copy(
                    applications = updatedApplications
                )
                applicationStateManager.updateApplications(updatedApplications)
                loadApplicationStats()
            }.onFailure { exception ->
                Timber.e("❌ SmartJobApplicationViewModel: Application failed - ${exception.message}")
                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    isSubmitting = false,
                    error = exception.message,
                    applicationSuccess = false
                )
            }
        }
    }
    
    /**
     * Submit a job application (direct submission)
     */
    fun submitApplication(application: JobApplication) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            
            jobApplicationService.submitApplication(application)
                .onSuccess { submittedApplication ->
                    val canonicalId = submittedApplication.id
                    val updatedApplications = listOf(submittedApplication) + _legacyUiState.value.applications
                        .filterNot { it.id == canonicalId || it.jobId == submittedApplication.jobId }
                    _legacyUiState.value = _legacyUiState.value.copy(
                        applications = updatedApplications
                    )
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submissionSuccess = true,
                        applications = updatedApplications,
                        lastApplication = submittedApplication,
                        applicationSuccess = true
                    )
                    applicationStateManager.updateApplications(updatedApplications)
                    loadApplicationStats()
                }
                .onFailure { _ ->
                    _uiState.value = _uiState.value.copy(isSubmitting = false)
                }
        }
    }

    fun applyFromCall(jobId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            val currentUser = auth.currentUser
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = "User not authenticated")
                return@launch
            }

            jobApplicationService.applyFromCall(jobId, currentUser.uid)
                .onSuccess { application ->
                    val canonicalId = application.id
                    val updatedApplications = listOf(application) + _legacyUiState.value.applications
                        .filterNot { it.id == canonicalId || it.jobId == application.jobId }
                    _legacyUiState.value = _legacyUiState.value.copy(applications = updatedApplications)
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submissionSuccess = true,
                        applications = updatedApplications,
                        lastApplication = application,
                        applicationSuccess = true
                    )
                    applicationStateManager.updateApplications(updatedApplications)
                    loadApplicationStats()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = exception.message,
                        applicationSuccess = false
                    )
                }
        }
    }

    /**
     * Check if user has applied for a job
     */
    fun hasUserApplied(jobId: String, onResult: (Boolean) -> Unit) {
        if (applicationStateManager.isJobApplied(jobId)) {
            onResult(true)
            return
        }

        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                onResult(false)
                return@launch
            }
            
            val result = jobApplicationService.hasUserApplied(jobId, currentUser.uid)
            result.onSuccess { hasApplied ->
                if (hasApplied) {
                    applicationStateManager.addAppliedJob(jobId)
                }
                onResult(hasApplied)
            }.onFailure { exception ->
                Timber.e(exception, "Error checking application status")
                onResult(false)
            }
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
            _uiState.value = _uiState.value.copy(isWithdrawing = true, isSubmitting = false, error = null)

            val currentUser = auth.currentUser
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(
                    isWithdrawing = false,
                    isSubmitting = false,
                    error = "User not authenticated"
                )
                onResult(false, "User not authenticated")
                return@launch
            }

            val result = jobApplicationService.withdrawApplication(applicationId, currentUser.uid)
            result.onSuccess { withdrawnApplication ->
                // Remove from local state
                val updatedApplications = _legacyUiState.value.applications.filter { 
                    it.id != applicationId 
                }
                _uiState.value = _uiState.value.copy(
                    isWithdrawing = false,
                    isSubmitting = false,
                    withdrawalSuccess = true
                )
                _legacyUiState.value = _legacyUiState.value.copy(
                    applications = updatedApplications
                )
                _uiState.value = _uiState.value.copy(applications = updatedApplications)
                applicationStateManager.updateApplications(updatedApplications)
                loadApplicationStats()
                onResult(true, null)
            }.onFailure { exception: Throwable ->
                _uiState.value = _uiState.value.copy(
                    isWithdrawing = false,
                    isSubmitting = false,
                    error = exception.message,
                    withdrawalSuccess = false
                )
                onResult(false, exception.message ?: "Failed to withdraw application")
            }
        }
    }

    // =============================================================================
    // STATISTICS & VACANCY STATUS
    // =============================================================================
    
    private fun loadApplicationStats() {
        val applications = _uiState.value.applications
        val stats = ApplicationStats(
            totalApplications = applications.size,
            appliedApplications = applications.count { it.status == ApplicationStatus.APPLIED },
            rejectedApplications = applications.count { it.status == ApplicationStatus.REJECTED },
            hiredApplications = applications.count { it.status == ApplicationStatus.HIRED },
            recentApplications = applications.take(5)
        )
        _stats.value = stats
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

    // =============================================================================
    // ERROR HANDLING
    // =============================================================================

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clear success states
     */
    fun clearSuccessStates() {
        _uiState.value = _uiState.value.copy(
            applicationSuccess = false,
            submissionSuccess = false,
            withdrawalSuccess = false
        )
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
    val isSubmitting: Boolean = false,
    val isWithdrawing: Boolean = false,
    val applications: List<JobApplication> = emptyList(),
    val lastApplication: JobApplication? = null,
    val applicationSuccess: Boolean = false,
    val submissionSuccess: Boolean = false,
    val withdrawalSuccess: Boolean = false,
    val error: String? = null
)
