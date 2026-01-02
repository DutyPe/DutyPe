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
    private val savedJobsStateManager: SavedJobsStateManager,
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
     * Saved job IDs - delegated to SavedJobsStateManager
     */
    val savedJobIds: StateFlow<Set<String>> = savedJobsStateManager.savedJobIds
    
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
    val globalRefreshTrigger = combine(
        savedJobsStateManager.refreshTrigger,
        applicationStateManager.refreshTrigger
    ) { savedRefresh, appRefresh ->
        savedRefresh + appRefresh
    }
    
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
        savedJobsStateManager.clearSavedJobs()
        applicationStateManager.clearAll()
        profileSetupStateManager.resetProfileSetupState()
    }
    
    // ==================== SAVED JOBS METHODS (Delegated) ====================
    
    /**
     * Add a job to saved jobs
     */
    fun saveJob(jobId: String) {
        savedJobsStateManager.addSavedJob(jobId)
    }
    
    /**
     * Remove a job from saved jobs
     */
    fun unsaveJob(jobId: String) {
        savedJobsStateManager.removeSavedJob(jobId)
    }
    
    /**
     * Check if a job is saved
     */
    fun isJobSaved(jobId: String): Boolean {
        return savedJobsStateManager.isJobSaved(jobId)
    }
    
    /**
     * Set all saved job IDs (bulk update)
     */
    fun setSavedJobIds(jobIds: Set<String>) {
        savedJobsStateManager.setSavedJobIds(jobIds)
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
