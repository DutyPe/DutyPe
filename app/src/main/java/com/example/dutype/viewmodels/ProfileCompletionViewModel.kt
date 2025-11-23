package com.example.dutype.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.services.JobApplicationService
import com.example.dutype.state.ProfileSetupStateManager
import com.example.dutype.models.UserRole
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
        profileCompletionService.isProfileComplete(userId, role.name)

    /**
     * Save user info for profile setup (to Firebase)
     */
    suspend fun saveUserInfo(email: String, name: String, role: UserRole) =
        profileCompletionService.saveUserInfo(email, name, role.name)
    
    /**
     * Save user info to local storage for profile setup screen
     */
    suspend fun saveUserInfoToLocalStorage(email: String, name: String, role: UserRole) =
        profileSetupStateManager.saveUserInfo(email, name, role)
    
    suspend fun updateUserRole(newRole: UserRole) {
        // Update role in Firebase
        profileCompletionService.updateUserRole(newRole.name)
        // Also update role in local DataStore
        profileSetupStateManager.saveUserRole(newRole)
    }

    /**
     * Check if profile setup should be shown
     */
    suspend fun shouldRedirectToProfileSetup(role: UserRole) = 
        profileCompletionService.shouldRedirectToProfileSetup(role.name)

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
        suspend fun saveWorkerProfileData(profileData: Map<String, Any>) =
            profileCompletionService.saveWorkerProfileData(profileData)

        /**
         * Save employer profile data to Firestore
         */
        suspend fun saveEmployerProfileData(profileData: Map<String, Any>) =
            profileCompletionService.saveEmployerProfileData(profileData)

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
         * Upload profile image to Firebase Storage
         */
        suspend fun uploadProfileImage(imageUri: Uri, userId: String, userRole: String) =
            profileCompletionService.uploadProfileImage(imageUri, userId, userRole)

        /**
         * Check if the app has been opened before
         */
        suspend fun hasAppBeenOpenedBefore(): Boolean =
            profileSetupStateManager.hasAppBeenOpenedBefore()

    /**
     * High-level approach: Check if user has existing profile using multiple strategies
     */
    suspend fun checkExistingProfileHighLevel(email: String, role: UserRole): Boolean {
        println("🔍 ProfileCompletionViewModel.checkExistingProfileHighLevel: Checking for email: $email, role: $role")
        
        // First try using current authenticated user's UID (more reliable)
        val currentUserCheck = profileCompletionService.checkExistingProfileByCurrentUser().getOrElse { false }
        println("🔍 ProfileCompletionViewModel.checkExistingProfileHighLevel: Current user check result: $currentUserCheck")
        
        if (currentUserCheck) {
            return true
        }
        
        // Fallback to email-based check if current user check fails
        val emailCheck = profileCompletionService.checkExistingProfileByEmail(email).getOrElse { false }
        println("🔍 ProfileCompletionViewModel.checkExistingProfileHighLevel: Email check result: $emailCheck")
        return emailCheck
    }

    /**
     * Check if user already has a profile in Firebase by email
     */
        suspend fun checkExistingProfileByEmail(email: String, role: UserRole): Boolean =
            profileCompletionService.checkExistingProfileByEmail(email).getOrElse { false }

        /**
         * Load existing profile data into local state for returning users
         */
        suspend fun loadExistingProfileData(email: String, role: UserRole) {
            // First try using current authenticated user's UID
            profileCompletionService.loadExistingProfileDataByCurrentUser().getOrElse {
                // Fallback to email-based loading if current user loading fails
                profileCompletionService.loadExistingProfileDataByEmail(email)
            }
        }

        /**
         * Mark that the app has been opened
         */
        suspend fun markAppAsOpened() =
            profileSetupStateManager.markAppAsOpened()

        /**
         * Save authentication method ("GOOGLE" or "PHONE_OTP")
         */
        suspend fun saveAuthMethod(authMethod: String) =
            profileSetupStateManager.saveAuthMethod(authMethod)

        /**
         * Get authentication method
         */
        suspend fun getAuthMethod(): String? =
            profileSetupStateManager.getAuthMethod()

        /**
         * Save phone number (from OTP verification)
         */
        suspend fun savePhoneNumber(phone: String) =
            profileSetupStateManager.savePhoneNumber(phone)

        /**
         * Get saved phone number
         */
        suspend fun getPhoneNumber(): String? =
            profileSetupStateManager.getPhoneNumber()
    }
