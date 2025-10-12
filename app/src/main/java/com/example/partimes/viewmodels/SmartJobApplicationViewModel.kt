package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.models.JobApplication
import com.example.partimes.models.ApplicationStatus
import com.example.partimes.services.JobApplicationService
import com.example.partimes.services.ProfileCompletionService
import com.example.partimes.state.ApplicationStateManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Enterprise-level Smart Job Application ViewModel
 * Handles both direct and profile-based job applications with 30 years of experience
 */
@HiltViewModel
class SmartJobApplicationViewModel @Inject constructor(
    private val jobApplicationService: JobApplicationService,
    private val profileCompletionService: ProfileCompletionService,
    private val applicationStateManager: ApplicationStateManager,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartJobApplicationUiState())
    val uiState: StateFlow<SmartJobApplicationUiState> = _uiState.asStateFlow()

    private val _canApplyDirectly = MutableStateFlow(false)
    val canApplyDirectly: StateFlow<Boolean> = _canApplyDirectly.asStateFlow()

    private val _profileCompletionPercentage = MutableStateFlow(0)
    val profileCompletionPercentage: StateFlow<Int> = _profileCompletionPercentage.asStateFlow()

    private val _missingFields = MutableStateFlow<List<String>>(emptyList())
    val missingFields: StateFlow<List<String>> = _missingFields.asStateFlow()

    init {
        // Load user's application capabilities
        loadUserCapabilities()
        
        // Listen to application state changes
        viewModelScope.launch {
            applicationStateManager.applications.collect { applications ->
                _uiState.value = _uiState.value.copy(applications = applications)
            }
        }
    }

    /**
     * Load user's application capabilities
     */
    private fun loadUserCapabilities() {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // Check if user can apply directly
                val canApplyResult = profileCompletionService.canApplyDirectly(currentUser.uid)
                if (canApplyResult.isSuccess) {
                    _canApplyDirectly.value = canApplyResult.getOrNull() ?: false
                }

                // Get profile completion percentage
                val completionResult = profileCompletionService.getProfileCompletionPercentage(
                    currentUser.uid, 
                    com.example.partimes.models.UserRole.WORKER
                )
                if (completionResult.isSuccess) {
                    _profileCompletionPercentage.value = completionResult.getOrNull() ?: 0
                }

                // Get missing fields
                val missingFieldsResult = profileCompletionService.getMissingProfileFields(
                    currentUser.uid,
                    com.example.partimes.models.UserRole.WORKER
                )
                if (missingFieldsResult.isSuccess) {
                    _missingFields.value = missingFieldsResult.getOrNull() ?: emptyList()
                }
            }
        }
    }

    /**
     * Apply for a job (smart application)
     */
    fun applyForJob(
        jobId: String,
        coverLetter: String? = null,
        additionalNotes: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true, error = null)

            val currentUser = auth.currentUser
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    error = "User not authenticated",
                    applicationSuccess = false
                )
                return@launch
            }
            
            val result = jobApplicationService.smartApplyForJob(jobId, currentUser.uid, coverLetter, additionalNotes)
            result.fold(
                onSuccess = { application ->
                    _uiState.value = _uiState.value.copy(
                        isApplying = false,
                        lastApplication = application,
                        applicationSuccess = true
                    )
                    // Update application state
                    applicationStateManager.addAppliedJob(jobId)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isApplying = false,
                        error = exception.message,
                        applicationSuccess = false
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
                    println("Error checking application status: ${exception.message}")
                    onResult(false)
                }
            )
        }
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
    fun withdrawApplication(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWithdrawing = true, error = null)

            val currentUser = auth.currentUser
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(
                    isWithdrawing = false,
                    error = "User not authenticated"
                )
                return@launch
            }

            val result = jobApplicationService.withdrawApplication(applicationId, currentUser.uid)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isWithdrawing = false,
                        withdrawalSuccess = true
                    )
                    // Update application state
                    applicationStateManager.removeAppliedJob(applicationId)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isWithdrawing = false,
                        error = exception.message,
                        withdrawalSuccess = false
                    )
                }
            )
        }
    }

    /**
     * Refresh user capabilities
     */
    fun refreshCapabilities() {
        loadUserCapabilities()
    }

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
            withdrawalSuccess = false
        )
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
