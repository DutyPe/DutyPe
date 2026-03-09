package com.example.dutype.state

import com.example.dutype.models.JobApplication
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified App State Manager
 * 
 * Single source of truth for all app-wide state, coordinating:
 * - Saved jobs state
 * - Application state
 * - User session state
 * 
 * This eliminates state duplication and ensures consistency across the app.
 */
@Singleton
class AppStateManager @Inject constructor(
    private val applicationStateManager: ApplicationStateManager,
    private val profileSetupStateManager: ProfileSetupStateManager
) {
    
    // ==================== USER SESSION STATE ====================
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()
    
    private val _currentUserRole = MutableStateFlow<UserRole?>(null)
    val currentUserRole: StateFlow<UserRole?> = _currentUserRole.asStateFlow()
    
    // ==================== DELEGATED STATE (Single Source of Truth) ====================
    
    /**
     * Saved job IDs - handled by SavedJobsViewModel
     */
    val savedJobIds: StateFlow<Set<String>> = MutableStateFlow<Set<String>>(emptySet()).asStateFlow()
    
    /**
     * Applied job IDs - delegated to ApplicationStateManager
     */
    val appliedJobIds: StateFlow<Set<String>> = applicationStateManager.appliedJobIds
    
    /**
     * Applications list - delegated to ApplicationStateManager
     */
    val applications: StateFlow<List<JobApplication>> = applicationStateManager.applications
    
    /**
     * Application statuses - delegated to ApplicationStateManager
     */
    val applicationStatuses: StateFlow<Map<String, ApplicationStatus>> = applicationStateManager.applicationStatuses
    
    // ==================== COMBINED STATE FLOWS ====================
    
    /**
     * Combined refresh trigger - emits when any state changes
     */
    val globalRefreshTrigger = applicationStateManager.refreshTrigger
    
    // ==================== USER SESSION METHODS ====================
    
    /**
     * Initialize user session
     */
    fun initializeSession(userId: String, role: UserRole) {
        Timber.d("AppStateManager: Initializing session for user $userId with role $role")
        _currentUserId.value = userId
        _currentUserRole.value = role
        _isLoggedIn.value = true
    }
    
    /**
     * Clear user session (logout)
     */
    suspend fun clearSession() {
        Timber.d("AppStateManager: Clearing session")
        _currentUserId.value = null
        _currentUserRole.value = null
        _isLoggedIn.value = false
        
        // Clear all state managers
        applicationStateManager.clearAll()
        profileSetupStateManager.resetProfileSetupState()
    }
    
    // ==================== SAVED JOBS METHODS (Delegated) ====================
    
    /**
     * Add a job to saved jobs - handled by SavedJobsViewModel
     */
    fun saveJob(jobId: String) {
        // Placeholder - use SavedJobsViewModel.saveJob() instead
    }
    
    /**
     * Remove a job from saved jobs - handled by SavedJobsViewModel
     */
    fun unsaveJob(jobId: String) {
        // Placeholder - use SavedJobsViewModel.unsaveJob() instead
    }
    
    /**
     * Check if a job is saved - use SavedJobsViewModel instead
     */
    fun isJobSaved(jobId: String): Boolean {
        return false // Placeholder
    }
    
    /**
     * Set all saved job IDs - use SavedJobsViewModel instead
     */
    fun setSavedJobIds(jobIds: Set<String>) {
        // Placeholder
    }
    
    // ==================== APPLICATION METHODS (Delegated) ====================
    
    /**
     * Add an applied job
     */
    fun addAppliedJob(jobId: String) {
        applicationStateManager.addAppliedJob(jobId)
    }
    
    /**
     * Check if user has applied to a job
     */
    fun hasAppliedToJob(jobId: String): Boolean {
        return applicationStateManager.isJobApplied(jobId)
    }
    
    /**
     * Update applications list
     */
    fun updateApplications(applications: List<JobApplication>) {
        applicationStateManager.updateApplications(applications)
    }
    
    /**
     * Get application by job ID
     */
    fun getApplicationByJobId(jobId: String): JobApplication? {
        return applicationStateManager.getApplicationByJobId(jobId)
    }
    
    /**
     * Get application status by job ID
     */
    fun getApplicationStatus(jobId: String): ApplicationStatus? {
        return applicationStateManager.getApplicationStatus(jobId)
    }
    
    /**
     * Update application status
     */
    fun updateApplicationStatus(jobId: String, status: ApplicationStatus) {
        applicationStateManager.updateApplicationStatus(jobId, status)
    }
    
    // ==================== PROFILE METHODS (Delegated) ====================
    
    /**
     * Check if profile is complete
     */
    suspend fun isProfileComplete(role: UserRole): Boolean {
        return profileSetupStateManager.isProfileComplete(role)
    }
    
    /**
     * Get profile setup status
     */
    suspend fun getProfileSetupStatus(role: UserRole): ProfileSetupStatus {
        return profileSetupStateManager.getProfileSetupStatus(role)
    }
    
    // ==================== CONVENIENCE METHODS ====================
    
    /**
     * Get job status summary for a specific job
     * Returns a combined status including saved and application status
     */
    fun getJobStatusSummary(jobId: String): JobStatusSummary {
        return JobStatusSummary(
            isSaved = isJobSaved(jobId),
            hasApplied = hasAppliedToJob(jobId),
            applicationStatus = getApplicationStatus(jobId)
        )
    }
}

/**
 * Data class representing combined job status
 */
data class JobStatusSummary(
    val isSaved: Boolean,
    val hasApplied: Boolean,
    val applicationStatus: ApplicationStatus?
)
