package com.example.dutype.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.dutype.models.UserRole
import com.example.dutype.profile.CurrentProfileStore
import com.example.dutype.profile.EmployerProfile
import com.example.dutype.profile.WorkerProfile
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.state.ProfileSetupStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Profile setup / editing for Compose screens: typed profile reads and field saves
 * ([ProfileCompletionService]), the live own profile ([CurrentProfileStore]) and the
 * local onboarding flags ([ProfileSetupStateManager]).
 */
@HiltViewModel
class ProfileCompletionViewModel @Inject constructor(
    val profileCompletionService: ProfileCompletionService,
    private val profileSetupStateManager: ProfileSetupStateManager,
    val profileStore: CurrentProfileStore,
    val authManager: com.example.dutype.auth.AuthManager,
    val locationService: com.example.dutype.utils.LocationService,
    val fcmTokenManager: com.example.dutype.services.FCMTokenManager,
    val notificationService: com.example.dutype.services.NotificationService,
    val locationPreferences: com.example.dutype.location.LocationPreferences,
    private val referralService: com.example.dutype.services.ReferralService
) : ViewModel() {

    // ─────────────────────────────── Firestore profile ───────────────────────────────

    suspend fun isProfileComplete(userId: String, role: UserRole) =
        profileCompletionService.isProfileComplete(userId, role.name)

    suspend fun getWorker(userId: String): Result<WorkerProfile?> = profileCompletionService.getWorker(userId)

    suspend fun getEmployer(userId: String): Result<EmployerProfile?> = profileCompletionService.getEmployer(userId)

    /** Keys from `FirestoreSchema.WorkerProfiles`. */
    suspend fun saveWorker(fields: Map<String, Any?>) = profileCompletionService.saveWorker(fields)

    /** Keys from `FirestoreSchema.EmployerProfiles`. */
    suspend fun saveEmployer(fields: Map<String, Any?>) = profileCompletionService.saveEmployer(fields)

    suspend fun uploadProfileImage(imageUri: Uri, userId: String, userRole: String) =
        profileCompletionService.uploadProfileImage(imageUri, userId, userRole)

    // ─────────────────────────────── local onboarding state ───────────────────────────────

    suspend fun saveUserInfoToLocalStorage(email: String, name: String, role: UserRole) =
        profileSetupStateManager.saveUserInfo(email, name, role)

    suspend fun saveUserInfoToLocalStorage(name: String, role: UserRole) =
        profileSetupStateManager.saveUserInfo("", name, role)

    suspend fun hasProfileSetupBeenShown(role: UserRole) = profileSetupStateManager.hasProfileSetupBeenShown(role)

    suspend fun isProfileComplete(role: UserRole) = profileSetupStateManager.isProfileComplete(role)

    suspend fun getUserEmail() = profileSetupStateManager.getUserEmail()

    suspend fun getUserName() = profileSetupStateManager.getUserName()

    suspend fun getUserRole() = profileSetupStateManager.getUserRole()

    suspend fun getCompletionPercentage(role: UserRole) = profileSetupStateManager.getCompletionPercentage(role)

    suspend fun getMissingFields(role: UserRole) = profileSetupStateManager.getMissingFields(role)

    suspend fun markProfileComplete(role: UserRole) = profileSetupStateManager.markProfileComplete(role)

    suspend fun getProfileSetupStatus(role: UserRole) = profileSetupStateManager.getProfileSetupStatus(role)

    suspend fun resetProfileSetupState() = profileSetupStateManager.resetProfileSetupState()

    suspend fun markProfileSetupAsShown(role: UserRole) = profileSetupStateManager.markProfileSetupAsShown(role)

    suspend fun hasAppBeenOpenedBefore(): Boolean = profileSetupStateManager.hasAppBeenOpenedBefore()

    suspend fun hasOnboardingBeenCompleted(): Boolean = profileSetupStateManager.hasOnboardingBeenCompleted()

    suspend fun markOnboardingCompleted() = profileSetupStateManager.markOnboardingCompleted()

    suspend fun markAppAsOpened() = profileSetupStateManager.markAppAsOpened()

    suspend fun saveAuthMethod(authMethod: String) = profileSetupStateManager.saveAuthMethod(authMethod)

    suspend fun getAuthMethod(): String? = profileSetupStateManager.getAuthMethod()

    suspend fun savePhoneNumber(phone: String) = profileSetupStateManager.savePhoneNumber(phone)

    suspend fun getPhoneNumber(): String? = profileSetupStateManager.getPhoneNumber()

    /** A code typed before sign-up; sent with `completeRegistration`, which applies it server-side. */
    suspend fun saveReferralCode(code: String) = profileSetupStateManager.saveReferralCode(code)

    suspend fun getReferralCode(): String? = profileSetupStateManager.getReferralCode()

    suspend fun clearReferralCode() = profileSetupStateManager.clearReferralCode()

    suspend fun hasUserUsedReferralCode(): Boolean = referralService.hasUsedReferralCode()

    /** Applies a code during profile setup (server checks it and the 7-day window). */
    suspend fun applyReferralCode(code: String): Result<Unit> =
        referralService.applyReferralCode(code).onSuccess { profileSetupStateManager.clearReferralCode() }

    suspend fun validateReferralCode(code: String) = referralService.validateReferralCode(code)
}
