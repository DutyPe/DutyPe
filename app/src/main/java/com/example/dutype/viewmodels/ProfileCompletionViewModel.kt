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
import timber.log.Timber

/**
 * ViewModel for Profile Completion operations
 * Wraps ProfileCompletionService and ProfileSetupStateManager for Compose integration
 * 
 * REFACTORED: Added AuthManager, RatingService, LocationService, FCMTokenManager, NotificationService
 * This eliminates the need for ServiceProvider anti-pattern
 */
@HiltViewModel
class ProfileCompletionViewModel @Inject constructor(
    val profileCompletionService: ProfileCompletionService,
    private val profileSetupStateManager: ProfileSetupStateManager,
    private val jobApplicationService: JobApplicationService,
    val authManager: com.example.dutype.auth.AuthManager,
    val ratingService: com.example.dutype.services.RatingService,
    val locationService: com.example.dutype.utils.LocationService,
    val fcmTokenManager: com.example.dutype.services.FCMTokenManager,
    val notificationService: com.example.dutype.services.NotificationService,
    val locationPreferences: com.example.dutype.location.LocationPreferences
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
         * Check if onboarding has been completed
         * This is separate from hasAppBeenOpenedBefore to handle app restart during language selection
         */
        suspend fun hasOnboardingBeenCompleted(): Boolean =
            profileSetupStateManager.hasOnboardingBeenCompleted()

        /**
         * Mark onboarding as completed
         * Called when user finishes all onboarding screens and reaches role selection
         */
        suspend fun markOnboardingCompleted() =
            profileSetupStateManager.markOnboardingCompleted()

    /**
     * High-level approach: Check if user has existing profile using multiple strategies
     * Now properly checks ROLE-SPECIFIC profile completion
     */
    suspend fun checkExistingProfileHighLevel(email: String, role: UserRole): Boolean {
        Timber.d("🔍 ProfileCompletionViewModel.checkExistingProfileHighLevel: Checking for email: $email, role: $role")
        
        // First check if user document exists and has basic profile complete
        val currentUserCheck = profileCompletionService.checkExistingProfileByCurrentUser().getOrElse { false }
        Timber.d("🔍 ProfileCompletionViewModel.checkExistingProfileHighLevel: Current user check result: $currentUserCheck")
        
        if (!currentUserCheck) {
            // No profile exists at all
            Timber.d("🔍 ProfileCompletionViewModel.checkExistingProfileHighLevel: No basic profile found")
            return false
        }
        
        // Profile exists, now check ROLE-SPECIFIC completion
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Timber.d("🔍 ProfileCompletionViewModel.checkExistingProfileHighLevel: No authenticated user")
            return false
        }
        
        return when (role) {
            UserRole.EMPLOYER -> {
                // For EMPLOYER: Check if employer-specific fields are complete (companyName is mandatory)
                val employerProfileResult = profileCompletionService.getEmployerProfileData(currentUser.uid)
                val hasEmployerProfile = employerProfileResult.fold(
                    onSuccess = { data ->
                        val companyName = data["companyName"] as? String
                        val hasCompanyName = !companyName.isNullOrBlank()
                        Timber.d("🔍 ProfileCompletionViewModel.checkExistingProfileHighLevel: EMPLOYER - companyName: $companyName, hasCompanyName: $hasCompanyName")
                        hasCompanyName
                    },
                    onFailure = { 
                        Timber.d("🔍 ProfileCompletionViewModel.checkExistingProfileHighLevel: EMPLOYER - Failed to get employer profile data")
                        false 
                    }
                )
                Timber.d("🔍 ProfileCompletionViewModel.checkExistingProfileHighLevel: EMPLOYER profile complete: $hasEmployerProfile")
                hasEmployerProfile
            }
            UserRole.WORKER -> {
                // For WORKER: Check if worker-specific fields are complete (fullName and phone are mandatory)
                val workerProfileResult = profileCompletionService.getWorkerProfileData(currentUser.uid)
                val hasWorkerProfile = workerProfileResult.fold(
                    onSuccess = { data ->
                        val fullName = data["fullName"] as? String
                        val phoneNumber = data["phoneNumber"] as? String
                        val hasRequiredFields = !fullName.isNullOrBlank() && !phoneNumber.isNullOrBlank()
                        Timber.d("🔍 ProfileCompletionViewModel.checkExistingProfileHighLevel: WORKER - fullName: $fullName, phoneNumber: $phoneNumber, hasRequiredFields: $hasRequiredFields")
                        hasRequiredFields
                    },
                    onFailure = { 
                        Timber.d("🔍 ProfileCompletionViewModel.checkExistingProfileHighLevel: WORKER - Failed to get worker profile data")
                        false 
                    }
                )
                Timber.d("🔍 ProfileCompletionViewModel.checkExistingProfileHighLevel: WORKER profile complete: $hasWorkerProfile")
                hasWorkerProfile
            }
            else -> {
                Timber.d("🔍 ProfileCompletionViewModel.checkExistingProfileHighLevel: Unknown role, returning false")
                false
            }
        }
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

        /**
         * Check if phone number exists with a different role
         * Returns the existing role if found, null otherwise
         * Used to prevent dual-role accounts
         */
        suspend fun checkPhoneExistsWithDifferentRole(phone: String, currentRole: UserRole): String? {
            return try {
                val result = profileCompletionService.checkPhoneExistsWithDifferentRole(phone, currentRole.name)
                result.getOrNull()
            } catch (e: Exception) {
                Timber.e(e, "Error checking phone existence with different role")
                null
            }
        }
        
        /**
         * Save phone-role mapping to phone_roles collection
         * Called when user completes profile setup
         * Includes device fingerprint for fraud prevention
         */
        suspend fun savePhoneRole(phone: String, role: UserRole, context: android.content.Context? = null) {
            try {
                // Note: DeviceFingerprintService is not available here, pass null
                // Device fingerprint should be handled separately during registration
                profileCompletionService.savePhoneRole(phone, role.name, null, context)
            } catch (e: Exception) {
                Timber.e(e, "Error saving phone role mapping")
            }
        }
        
        // ============================================
        // REFERRAL SYSTEM METHODS
        // ============================================
        
        /**
         * Validate a referral code
         * Returns the referrer's userId and role if valid
         */
        suspend fun validateReferralCode(code: String): Result<Pair<String, String>?> {
            return try {
                profileCompletionService.validateReferralCode(code)
            } catch (e: Exception) {
                Timber.e(e, "Error validating referral code")
                Result.failure(e)
            }
        }
        
        /**
         * Apply a referral code for a new user
         */
        suspend fun applyReferralCode(
            referralCode: String,
            newUserId: String,
            newUserRole: String,
            newUserName: String,
            newUserPhone: String
        ): Result<Unit> {
            return try {
                profileCompletionService.applyReferralCode(
                    referralCode = referralCode,
                    newUserId = newUserId,
                    newUserRole = newUserRole,
                    newUserName = newUserName,
                    newUserPhone = newUserPhone
                )
            } catch (e: Exception) {
                Timber.e(e, "Error applying referral code")
                Result.failure(e)
            }
        }
        
        /**
         * Complete a referral when user finishes profile setup
         */
        suspend fun completeReferral(userId: String): Result<Unit> {
            return try {
                profileCompletionService.completeReferral(userId)
            } catch (e: Exception) {
                Timber.e(e, "Error completing referral")
                Result.failure(e)
            }
        }
        
        /**
         * Get referral stats for current user
         */
        suspend fun getReferralStats() = profileCompletionService.getReferralStats()
        
        /**
         * Get referral history for current user
         */
        suspend fun getReferralHistory(limit: Int = 20) = profileCompletionService.getReferralHistory(limit)
        
        /**
         * Create referral stats for a new user (generates their unique referral code)
         * Called when user completes profile setup
         */
        suspend fun createReferralStats(userId: String, userRole: String, userName: String = ""): Result<Unit> {
            return try {
                profileCompletionService.createReferralStats(userId, userRole, userName)
            } catch (e: Exception) {
                Timber.e(e, "Error creating referral stats")
                Result.failure(e)
            }
        }
    }
