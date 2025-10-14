package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import com.example.partimes.services.ProfileCompletionService
import com.example.partimes.services.JobApplicationService
import com.example.partimes.state.ProfileSetupStateManager
import com.example.partimes.models.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * ViewModel for Profile Completion operations
 * Wraps ProfileCompletionService and ProfileSetupStateManager for Compose integration
 */
@HiltViewModel
class ProfileCompletionViewModel @Inject constructor(
    private val profileCompletionService: ProfileCompletionService,
    private val profileSetupStateManager: ProfileSetupStateManager,
    private val jobApplicationService: JobApplicationService
) : ViewModel() {

    /**
     * Get applications for a specific job
     */
    suspend fun getApplicationsForJob(jobId: String): Int {
        return try {
            jobApplicationService.getJobApplications(jobId).first().getOrNull()?.size ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Check if user has complete profile for their role
     */
    suspend fun isProfileComplete(userId: String, role: UserRole) = 
        profileCompletionService.isProfileComplete(userId, role)

    /**
     * Save user info for profile setup
     */
    suspend fun saveUserInfo(email: String, name: String, role: UserRole) =
        profileCompletionService.saveUserInfo(email, name, role)
    
    suspend fun updateUserRole(newRole: UserRole) =
        profileCompletionService.updateUserRole(newRole)

    /**
     * Check if profile setup should be shown
     */
    suspend fun shouldRedirectToProfileSetup(role: UserRole) = 
        profileCompletionService.shouldRedirectToProfileSetup(role)

    /**
     * Check if profile setup has been shown
     */
    suspend fun hasProfileSetupBeenShown(role: UserRole) = 
        profileSetupStateManager.hasProfileSetupBeenShown(role)

    /**
     * Check if profile is complete
     */
    suspend fun isProfileComplete(role: UserRole) = 
        profileSetupStateManager.isProfileComplete(role)

    /**
     * Get user email
     */
    suspend fun getUserEmail() = 
        profileSetupStateManager.getUserEmail()

    /**
     * Get user name
     */
    suspend fun getUserName() = 
        profileSetupStateManager.getUserName()

    /**
     * Get user role
     */
    suspend fun getUserRole() = 
        profileSetupStateManager.getUserRole()

    /**
     * Get completion percentage
     */
    suspend fun getCompletionPercentage(role: UserRole) = 
        profileSetupStateManager.getCompletionPercentage(role)

    /**
     * Get missing fields
     */
    suspend fun getMissingFields(role: UserRole) = 
        profileSetupStateManager.getMissingFields(role)

    /**
     * Mark profile as complete
     */
    suspend fun markProfileComplete(role: UserRole) = 
        profileSetupStateManager.markProfileComplete(role)

    /**
     * Get profile setup status
     */
    suspend fun getProfileSetupStatus(role: UserRole) = 
        profileSetupStateManager.getProfileSetupStatus(role)

    /**
     * Reset profile setup state
     */
    suspend fun resetProfileSetupState() = 
        profileSetupStateManager.resetProfileSetupState()

        /**
         * Mark profile setup as shown for a specific role
         */
        suspend fun markProfileSetupAsShown(role: UserRole) =
            profileSetupStateManager.markProfileSetupAsShown(role)

        /**
         * Save worker profile data to Firestore
         */
        suspend fun saveWorkerProfileData(userId: String, profileData: Map<String, Any>) =
            profileCompletionService.saveWorkerProfileData(userId, profileData)

        /**
         * Save employer profile data to Firestore
         */
        suspend fun saveEmployerProfileData(userId: String, profileData: Map<String, Any>) =
            profileCompletionService.saveEmployerProfileData(userId, profileData)

        /**
         * Get employer profile data from Firestore
         */
        suspend fun getEmployerProfileData(userId: String) =
            profileCompletionService.getEmployerProfileData(userId)

        /**
         * Get worker profile data from Firestore
         */
        suspend fun getWorkerProfileData(userId: String) =
            profileCompletionService.getWorkerProfileData(userId)

        /**
         * Check if the app has been opened before
         */
        suspend fun hasAppBeenOpenedBefore(): Boolean =
            profileSetupStateManager.hasAppBeenOpenedBefore()

    /**
     * High-level approach: Check if user has existing profile using multiple strategies
     */
    suspend fun checkExistingProfileHighLevel(email: String, role: UserRole): Boolean =
        profileCompletionService.checkExistingProfileHighLevel(email, role)

    /**
     * Check if user already has a profile in Firebase by email
     */
        suspend fun checkExistingProfileByEmail(email: String, role: UserRole): Boolean =
            profileCompletionService.checkExistingProfileByEmail(email, role)

        /**
         * Load existing profile data into local state for returning users
         */
        suspend fun loadExistingProfileData(email: String, role: UserRole) =
            profileCompletionService.loadExistingProfileData(email, role)

        /**
         * Mark that the app has been opened
         */
        suspend fun markAppAsOpened() =
            profileSetupStateManager.markAppAsOpened()
    }
