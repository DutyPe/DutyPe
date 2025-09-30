package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.auth.AuthManager
import com.example.partimes.models.JobApplication
import com.example.partimes.models.ApplicationStatus
import com.example.partimes.network.ApiClient
import com.example.partimes.repositories.ApplicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ApplicationUiState(
    val myApplications: List<JobApplication> = emptyList(),
    val jobApplications: List<JobApplication> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val hasError: Boolean = false,
    val isUpdatingStatus: Boolean = false
)

class ApplicationViewModel : ViewModel() {
    
    private lateinit var applicationRepository: ApplicationRepository
    private lateinit var authManager: AuthManager
    
    private val _uiState = MutableStateFlow(ApplicationUiState())
    val uiState: StateFlow<ApplicationUiState> = _uiState.asStateFlow()
    
    fun initialize(authManager: AuthManager) {
        this.authManager = authManager
        this.applicationRepository = ApplicationRepository(ApiClient.getApiService(), authManager)
    }
    
    fun loadMyApplications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                val result = applicationRepository.getMyApplications()
                result.fold(
                    onSuccess = { data ->
                        val applications = data["applications"] as? List<Map<String, Any>> ?: emptyList()
                        val jobApplications = applications.map { appData ->
                            convertMapToJobApplication(appData)
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            myApplications = jobApplications,
                            isLoading = false
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
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load applications"
                )
            }
        }
    }
    
    fun loadApplicationsForJob(jobId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                val result = applicationRepository.getApplicationsForJob(jobId)
                result.fold(
                    onSuccess = { data ->
                        val applications = data["applications"] as? List<Map<String, Any>> ?: emptyList()
                        val jobApplications = applications.map { appData ->
                            convertMapToJobApplication(appData)
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            jobApplications = jobApplications,
                            isLoading = false
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasError = true,
                            error = exception.message ?: "Failed to load job applications"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load job applications"
                )
            }
        }
    }
    
    fun submitApplication(application: JobApplication, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null, hasError = false)
            
            try {
                val result = applicationRepository.submitApplication(application)
                result.fold(
                    onSuccess = { submittedApplication ->
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            myApplications = _uiState.value.myApplications + submittedApplication
                        )
                        callback(true, null)
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            hasError = true,
                            error = exception.message ?: "Failed to submit application"
                        )
                        callback(false, exception.message)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    hasError = true,
                    error = e.message ?: "Failed to submit application"
                )
                callback(false, e.message)
            }
        }
    }
    
    fun updateApplicationStatus(
        applicationId: String,
        status: String,
        notes: String? = null,
        rejectionReason: String? = null,
        callback: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingStatus = true, error = null, hasError = false)
            
            try {
                val result = applicationRepository.updateApplicationStatus(
                    applicationId, status, notes, rejectionReason
                )
                result.fold(
                    onSuccess = { updatedApplication ->
                        _uiState.value = _uiState.value.copy(
                            isUpdatingStatus = false,
                            jobApplications = _uiState.value.jobApplications.map { 
                                if (it.applicationId == applicationId) updatedApplication else it 
                            },
                            myApplications = _uiState.value.myApplications.map { 
                                if (it.applicationId == applicationId) updatedApplication else it 
                            }
                        )
                        callback(true, null)
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isUpdatingStatus = false,
                            hasError = true,
                            error = exception.message ?: "Failed to update application status"
                        )
                        callback(false, exception.message)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUpdatingStatus = false,
                    hasError = true,
                    error = e.message ?: "Failed to update application status"
                )
                callback(false, e.message)
            }
        }
    }
    
    fun withdrawApplication(applicationId: String, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingStatus = true, error = null, hasError = false)
            
            try {
                val result = applicationRepository.withdrawApplication(applicationId)
                result.fold(
                    onSuccess = { withdrawnApplication ->
                        _uiState.value = _uiState.value.copy(
                            isUpdatingStatus = false,
                            myApplications = _uiState.value.myApplications.map { 
                                if (it.applicationId == applicationId) withdrawnApplication else it 
                            }
                        )
                        callback(true, null)
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isUpdatingStatus = false,
                            hasError = true,
                            error = exception.message ?: "Failed to withdraw application"
                        )
                        callback(false, exception.message)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUpdatingStatus = false,
                    hasError = true,
                    error = e.message ?: "Failed to withdraw application"
                )
                callback(false, e.message)
            }
        }
    }
    
    fun getApplicationsByStatus(status: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                val result = applicationRepository.getApplicationsByStatus(status)
                result.fold(
                    onSuccess = { data ->
                        val applications = data["applications"] as? List<Map<String, Any>> ?: emptyList()
                        val jobApplications = applications.map { appData ->
                            convertMapToJobApplication(appData)
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            myApplications = jobApplications,
                            isLoading = false
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasError = true,
                            error = exception.message ?: "Failed to load applications by status"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load applications by status"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, hasError = false)
    }
    
    private fun convertMapToJobApplication(appData: Map<String, Any>): JobApplication {
        return JobApplication(
            applicationId = appData["id"] as? String,
            jobId = appData["jobId"] as? String ?: "",
            workerId = appData["workerId"] as? String,
            workerEmail = appData["workerEmail"] as? String,
            workerName = appData["workerName"] as? String,
            workerPhone = appData["workerPhone"] as? String,
            status = ApplicationStatus.valueOf(appData["status"] as? String ?: "PENDING"),
            fullName = appData["fullName"] as? String ?: "",
            email = appData["email"] as? String ?: "",
            phoneNumber = appData["phoneNumber"] as? String ?: "",
            resumeUrl = appData["resumeUrl"] as? String,
            coverLetter = appData["coverLetter"] as? String,
            appliedAt = appData["applicationDate"] as? String,
            notes = appData["additionalNotes"] as? String
        )
    }
}
