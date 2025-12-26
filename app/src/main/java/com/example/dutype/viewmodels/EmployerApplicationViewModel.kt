package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.JobApplication
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.ApplicationStats
import com.example.dutype.models.ApplicationAnalytics
import com.example.dutype.services.JobApplicationService
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Enterprise-level Employer Application Management ViewModel
 * Handles all application-related operations for employers
 */
@HiltViewModel
class EmployerApplicationViewModel @Inject constructor(
    private val jobApplicationService: JobApplicationService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EmployerApplicationUiState())
    val uiState: StateFlow<EmployerApplicationUiState> = _uiState.asStateFlow()
    
    private val _stats = MutableStateFlow(ApplicationStats())
    val stats: StateFlow<ApplicationStats> = _stats.asStateFlow()
    
    private val _analytics = MutableStateFlow(ApplicationAnalytics())
    val analytics: StateFlow<ApplicationAnalytics> = _analytics.asStateFlow()
    
    private val auth = FirebaseAuth.getInstance()
    
    // Note: Don't load applications in init - let the screen decide what to load
    // based on whether it's viewing all applications or job-specific applications
    
    /**
     * Load all applications for current employer
     */
    fun loadEmployerApplications() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(
                hasError = true,
                error = "Employer not authenticated"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false)
            
            try {
                Timber.d("[EmployerVM] Loading employer applications for ${currentUser.uid}")
                
                // Add debug checks first
                jobApplicationService.debugApplicationData(currentUser.uid)
                jobApplicationService.debugJobData(currentUser.uid)
                
                jobApplicationService.getEmployerApplications(currentUser.uid).collect { result ->
                    result.fold(
                        onSuccess = { applications ->
                            Timber.d("[EmployerVM] Loaded ${applications.size} applications for employer")
                            _uiState.value = _uiState.value.copy(
                                applications = applications,
                                allApplications = applications,
                                isLoading = false,
                                hasError = false,
                                error = null
                            )
                            
                            // Update statistics and analytics
                            updateApplicationStats(applications)
                            loadApplicationAnalytics()
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = error.message ?: "Failed to load applications"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    /**
     * Load applications for a specific job
     */
    fun loadJobApplications(jobId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false)
            
            try {
                Timber.d("[EmployerApplicationViewModel] Loading job applications for jobId=$jobId")
                jobApplicationService.getJobApplications(jobId).collect { result ->
                    result.fold(
                        onSuccess = { applications ->
                            Timber.d("[EmployerApplicationViewModel] Successfully loaded ${applications.size} applications for job $jobId")
                            applications.forEach { app ->
                                Timber.d("[EmployerApplicationViewModel] Application: ${app.applicationId} for job ${app.jobId}, worker: ${app.workerName}")
                            }
                            _uiState.value = _uiState.value.copy(
                                applications = applications,
                                allApplications = applications,
                                isLoading = false,
                                hasError = false,
                                error = null
                            )
                        },
                        onFailure = { error ->
                            Timber.e("[EmployerApplicationViewModel] Failed to load job applications for $jobId: ${error.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = error.message ?: "Failed to load job applications"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e("[EmployerApplicationViewModel] Exception loading job applications for $jobId: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    /**
     * Load a single application by ID
     */
    fun loadApplicationById(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false)
            
            try {
                Timber.d("[EmployerApplicationViewModel] Loading application by ID: $applicationId")
                val result = jobApplicationService.getApplicationById(applicationId)
                
                result.fold(
                    onSuccess = { application ->
                        if (application != null) {
                            Timber.d("[EmployerApplicationViewModel] Successfully loaded application: ${application.applicationId}")
                            // Add to applications list if not already present
                            val currentApps = _uiState.value.applications.toMutableList()
                            val existingIndex = currentApps.indexOfFirst { it.applicationId == applicationId }
                            if (existingIndex >= 0) {
                                currentApps[existingIndex] = application
                            } else {
                                currentApps.add(application)
                            }
                            _uiState.value = _uiState.value.copy(
                                applications = currentApps,
                                isLoading = false,
                                hasError = false,
                                error = null
                            )
                        } else {
                            Timber.w("[EmployerApplicationViewModel] Application not found: $applicationId")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = "Application not found"
                            )
                        }
                    },
                    onFailure = { error ->
                        Timber.e("[EmployerApplicationViewModel] Failed to load application $applicationId: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasError = true,
                            error = error.message ?: "Failed to load application"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e("[EmployerApplicationViewModel] Exception loading application $applicationId: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    /**
     * Update application status with enterprise features
     */
    fun updateApplicationStatus(
        applicationId: String,
        newStatus: ApplicationStatus,
        notes: String? = null
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(
                hasError = true,
                error = "Employer not authenticated"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true)
            
            try {
                val result = jobApplicationService.updateApplicationStatus(
                    applicationId = applicationId,
                    newStatus = newStatus,
                    updatedBy = currentUser.uid,
                    notes = notes
                )
                
                result.fold(
                    onSuccess = { updatedApplication ->
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            hasError = false,
                            error = null
                        )
                        
                        // Refresh applications to show updated status
                        loadEmployerApplications()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            hasError = true,
                            error = error.message ?: "Failed to update application status"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    hasError = true,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    /**
     * Mark application as viewed (for analytics)
     */
    fun markApplicationAsViewed(applicationId: String) {
        val currentUser = auth.currentUser ?: return
        
        viewModelScope.launch {
            try {
                jobApplicationService.markApplicationAsViewed(applicationId, currentUser.uid)
            } catch (e: Exception) {
                // Silent fail for analytics
                Timber.w("Failed to mark application as viewed: ${e.message}")
            }
        }
    }
    
    /**
     * Mark application as under review when employer opens it
     */
    fun markApplicationAsUnderReview(applicationId: String) {
        val currentUser = auth.currentUser ?: return
        
        viewModelScope.launch {
            try {
                jobApplicationService.markApplicationAsUnderReview(applicationId, currentUser.uid)
                Timber.d("Application $applicationId marked as under review")
            } catch (e: Exception) {
                Timber.w("Failed to mark application as under review: ${e.message}")
            }
        }
    }
    
    /**
     * Load application analytics
     */
    private fun loadApplicationAnalytics() {
        val currentUser = auth.currentUser ?: return
        
        viewModelScope.launch {
            try {
                val result = jobApplicationService.getApplicationAnalytics(currentUser.uid)
                result.fold(
                    onSuccess = { analytics ->
                        _analytics.value = analytics
                    },
                    onFailure = { error ->
                        Timber.e("Failed to load analytics: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Timber.e("Failed to load analytics: ${e.message}")
            }
        }
    }
    
    /**
     * Update application statistics
     */
    private fun updateApplicationStats(applications: List<JobApplication>) {
        val stats = ApplicationStats(
            totalApplications = applications.size,
            pendingApplications = applications.count { it.status == ApplicationStatus.PENDING },
            reviewedApplications = applications.count { it.status == ApplicationStatus.UNDER_REVIEW },
            shortlistedApplications = applications.count { it.status == ApplicationStatus.ACCEPTED },
            rejectedApplications = applications.count { it.status == ApplicationStatus.REJECTED },
            hiredApplications = applications.count { it.status == ApplicationStatus.ACCEPTED },
            recentApplications = applications.take(5)
        )
        
        _stats.value = stats
    }
    
    /**
     * Filter applications by status
     */
    fun filterApplicationsByStatus(status: ApplicationStatus?) {
        val currentApplications = _uiState.value.allApplications
        val filteredApplications = if (status != null) {
            currentApplications.filter { it.status == status }
        } else {
            currentApplications
        }
        
        _uiState.value = _uiState.value.copy(
            applications = filteredApplications,
            selectedStatusFilter = status
        )
    }
    
    /**
     * Search applications
     */
    fun searchApplications(query: String) {
        val currentApplications = _uiState.value.allApplications
        val filteredApplications = if (query.isBlank()) {
            currentApplications
        } else {
            currentApplications.filter { application ->
                application.workerName.contains(query, ignoreCase = true) ||
                application.jobTitle.contains(query, ignoreCase = true) ||
                application.companyName.contains(query, ignoreCase = true)
            }
        }
        
        _uiState.value = _uiState.value.copy(
            applications = filteredApplications,
            searchQuery = query
        )
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(hasError = false, error = null)
    }
    
    /**
     * Refresh all data
     */
    fun refresh() {
        loadEmployerApplications()
    }
}

/**
 * UI State for Employer Application Management
 */
data class EmployerApplicationUiState(
    val applications: List<JobApplication> = emptyList(),
    val allApplications: List<JobApplication> = emptyList(), // Unfiltered list
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val hasError: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedStatusFilter: ApplicationStatus? = null
)
