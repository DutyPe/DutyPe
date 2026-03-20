package com.example.dutype.state

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dutype.models.UserRole
import com.example.dutype.models.normalizeReferralCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enterprise-level Profile Setup State Manager
 * Manages profile completion status and provides smart navigation decisions
 * with 30+ years of Android development experience
 */
@Singleton
class ProfileSetupStateManager @Inject constructor(
    private val context: Context,
    private val smartNotificationManager: com.example.dutype.services.SmartNotificationManager
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "profile_setup")
        
        // Profile completion keys
        private val WORKER_PROFILE_COMPLETE = booleanPreferencesKey("worker_profile_complete")
        private val EMPLOYER_PROFILE_COMPLETE = booleanPreferencesKey("employer_profile_complete")
        private val WORKER_PROFILE_SETUP_SHOWN = booleanPreferencesKey("worker_profile_setup_shown")
        private val EMPLOYER_PROFILE_SETUP_SHOWN = booleanPreferencesKey("employer_profile_setup_shown")
        private val USER_ROLE = stringPreferencesKey("user_role")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_PHONE = stringPreferencesKey("user_phone")
        private val AUTH_METHOD = stringPreferencesKey("auth_method") // "GOOGLE" or "PHONE_OTP"
        private val REFERRAL_CODE = stringPreferencesKey("referral_code") // Referral code from signup
        
        // Profile completion percentage keys
        private val WORKER_COMPLETION_PERCENTAGE = stringPreferencesKey("worker_completion_percentage")
        private val EMPLOYER_COMPLETION_PERCENTAGE = stringPreferencesKey("employer_completion_percentage")
        
            // Missing fields keys
            private val WORKER_MISSING_FIELDS = stringPreferencesKey("worker_missing_fields")
            private val EMPLOYER_MISSING_FIELDS = stringPreferencesKey("employer_missing_fields")
            
            // First-time user detection
            private val APP_OPENED_BEFORE = booleanPreferencesKey("app_opened_before")
            
            // Install time tracking for fresh install detection
            private val SAVED_INSTALL_TIME = stringPreferencesKey("saved_install_time")
            
            // Onboarding completion tracking (separate from app opened)
            private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
    
    /**
     * Check if profile setup has been shown for the given role
     */
    suspend fun hasProfileSetupBeenShown(role: UserRole): Boolean {
        Timber.d("hasProfileSetupBeenShown - role: $role")
        
        val result = context.dataStore.data.map { preferences ->
            when (role) {
                UserRole.WORKER -> preferences[WORKER_PROFILE_SETUP_SHOWN] ?: false
                UserRole.EMPLOYER -> preferences[EMPLOYER_PROFILE_SETUP_SHOWN] ?: false
                else -> false
            }
        }.first()
        
        Timber.d("hasProfileSetupBeenShown result: $result")
        return result
    }
    
    /**
     * Mark profile setup as shown for the given role
     */
    suspend fun markProfileSetupAsShown(role: UserRole) {
        context.dataStore.edit { preferences ->
            when (role) {
                UserRole.WORKER -> preferences[WORKER_PROFILE_SETUP_SHOWN] = true
                UserRole.EMPLOYER -> preferences[EMPLOYER_PROFILE_SETUP_SHOWN] = true
                else -> { /* Do nothing */ }
            }
        }
    }
    
    /**
     * Reset profile setup state (for logout or role change)
     */
    suspend fun resetProfileSetupState() {
        context.dataStore.edit { preferences ->
            preferences.remove(WORKER_PROFILE_SETUP_SHOWN)
            preferences.remove(EMPLOYER_PROFILE_SETUP_SHOWN)
            preferences.remove(WORKER_PROFILE_COMPLETE)
            preferences.remove(EMPLOYER_PROFILE_COMPLETE)
            preferences.remove(USER_ROLE)
            preferences.remove(USER_EMAIL)
            preferences.remove(USER_NAME)
            preferences.remove(USER_PHONE)
            preferences.remove(AUTH_METHOD)
            preferences.remove(WORKER_COMPLETION_PERCENTAGE)
            preferences.remove(EMPLOYER_COMPLETION_PERCENTAGE)
            preferences.remove(WORKER_MISSING_FIELDS)
            preferences.remove(EMPLOYER_MISSING_FIELDS)
        }
    }
    
    /**
     * Save user information from Google Sign-In
     */
    suspend fun saveUserInfo(email: String, name: String, role: UserRole) {
        Timber.i("saveUserInfo - name: $name, role: $role")
        
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
            preferences[USER_ROLE] = role.name
        }
        
        Timber.i("User info saved successfully")
    }
    
    suspend fun saveUserRole(role: UserRole) {
        Timber.i("saveUserRole - role: $role")
        
        context.dataStore.edit { preferences ->
            preferences[USER_ROLE] = role.name
        }
        
        Timber.i("User role saved successfully")
    }
    
    /**
     * Get saved user email
     */
    suspend fun getUserEmail(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[USER_EMAIL]
        }.first()
    }
    
    /**
     * Get saved user name
     */
    suspend fun getUserName(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[USER_NAME]
        }.first()
    }
    
    /**
     * Get saved user role
     */
    suspend fun getUserRole(): UserRole? {
        Timber.d("getUserRole - Getting user role...")
        return context.dataStore.data.map { preferences ->
            val roleString = preferences[USER_ROLE]
            val role = roleString?.let { UserRole.valueOf(it) }
            Timber.d("getUserRole - roleString: $roleString, role: $role")
            role
        }.first()
    }
    
    /**
     * Check if profile is complete for the given role
     */
    suspend fun isProfileComplete(role: UserRole): Boolean {
        Timber.d("isProfileComplete - role: $role")
        
        val result = context.dataStore.data.map { preferences ->
            when (role) {
                UserRole.WORKER -> preferences[WORKER_PROFILE_COMPLETE] ?: false
                UserRole.EMPLOYER -> preferences[EMPLOYER_PROFILE_COMPLETE] ?: false
                else -> false
            }
        }.first()
        
        Timber.d("isProfileComplete result: $result")
        return result
    }
    
    /**
     * Mark profile as complete for the given role
     */
    suspend fun markProfileComplete(role: UserRole) {
        context.dataStore.edit { preferences ->
            when (role) {
                UserRole.WORKER -> preferences[WORKER_PROFILE_COMPLETE] = true
                UserRole.EMPLOYER -> preferences[EMPLOYER_PROFILE_COMPLETE] = true
                else -> { /* Do nothing */ }
            }
        }
    }
    
    /**
     * Save profile completion percentage
     * 🔔 SMART NOTIFICATION: Triggers milestone notifications at 75% (reminder) and 100% (celebration)
     */
    suspend fun saveCompletionPercentage(role: UserRole, percentage: Int) {
        context.dataStore.edit { preferences ->
            when (role) {
                UserRole.WORKER -> preferences[WORKER_COMPLETION_PERCENTAGE] = percentage.toString()
                UserRole.EMPLOYER -> preferences[EMPLOYER_COMPLETION_PERCENTAGE] = percentage.toString()
                else -> { /* Do nothing */ }
            }
        }
        
        // 🔔 SMART NOTIFICATION: Trigger milestone notification
        // Only at 75% (reminder to complete) and 100% (celebration)
        if (percentage in listOf(75, 100)) {
            try {
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    smartNotificationManager.notifyProfileMilestone(userId, percentage)
                    Timber.d("🔔 SMART NOTIFICATION: Profile milestone $percentage% triggered for user $userId")
                }
            } catch (e: Exception) {
                Timber.e(e, "🔔 SMART NOTIFICATION: Failed to trigger profile milestone (non-critical)")
            }
        }
    }
    
    /**
     * Get profile completion percentage
     */
    suspend fun getCompletionPercentage(role: UserRole): Int {
        return context.dataStore.data.map { preferences ->
            when (role) {
                UserRole.WORKER -> preferences[WORKER_COMPLETION_PERCENTAGE]?.toIntOrNull() ?: 0
                UserRole.EMPLOYER -> preferences[EMPLOYER_COMPLETION_PERCENTAGE]?.toIntOrNull() ?: 0
                else -> 0
            }
        }.first()
    }
    
    /**
     * Save missing profile fields
     */
    suspend fun saveMissingFields(role: UserRole, missingFields: List<String>) {
        context.dataStore.edit { preferences ->
            val fieldsString = missingFields.joinToString(",")
            when (role) {
                UserRole.WORKER -> preferences[WORKER_MISSING_FIELDS] = fieldsString
                UserRole.EMPLOYER -> preferences[EMPLOYER_MISSING_FIELDS] = fieldsString
                else -> { /* Do nothing */ }
            }
        }
    }
    
    /**
     * Get missing profile fields
     */
    suspend fun getMissingFields(role: UserRole): List<String> {
        return context.dataStore.data.map { preferences ->
            val fieldsString = when (role) {
                UserRole.WORKER -> preferences[WORKER_MISSING_FIELDS] ?: ""
                UserRole.EMPLOYER -> preferences[EMPLOYER_MISSING_FIELDS] ?: ""
                else -> ""
            }
            if (fieldsString.isEmpty()) emptyList() else fieldsString.split(",")
        }.first()
    }
    
    /**
     * Check if user should be redirected to profile setup
     * This is the main decision-making function
     */
    suspend fun shouldRedirectToProfileSetup(role: UserRole): Boolean {
        val hasBeenShown = hasProfileSetupBeenShown(role)
        val isComplete = isProfileComplete(role)
        
        Timber.d("shouldRedirectToProfileSetup - role: $role, hasBeenShown: $hasBeenShown, isComplete: $isComplete, result: ${!hasBeenShown || !isComplete}")
        
        // Only redirect if profile setup hasn't been shown OR profile is not complete
        // If both have been shown and profile is complete, don't redirect
        return !hasBeenShown || !isComplete
    }
    
    /**
     * Get profile setup status for UI display
     */
    suspend fun getProfileSetupStatus(role: UserRole): ProfileSetupStatus {
        val isComplete = isProfileComplete(role)
        val percentage = getCompletionPercentage(role)
        val missingFields = getMissingFields(role)
        
        return ProfileSetupStatus(
            isComplete = isComplete,
            completionPercentage = percentage,
            missingFields = missingFields,
            shouldShowSetup = !isComplete
        )
    }

    /**
     * Get the app's first install time from PackageManager
     */
    private fun getAppInstallTime(): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.firstInstallTime
        } catch (e: Exception) {
            Timber.e(e, "Error getting app install time")
            0L
        }
    }

    /**
     * Check if the app has been opened before
     * Uses install time comparison to detect fresh installs even when DataStore persists
     * (Android auto-backup can restore DataStore across reinstalls)
     */
    suspend fun hasAppBeenOpenedBefore(): Boolean {
        Timber.d("hasAppBeenOpenedBefore - Checking...")
        
        val currentInstallTime = getAppInstallTime()
        Timber.d("hasAppBeenOpenedBefore - Current install time: $currentInstallTime")
        
        return context.dataStore.data.map { preferences ->
            val savedInstallTime = preferences[SAVED_INSTALL_TIME]?.toLongOrNull() ?: 0L
            val appOpenedBefore = preferences[APP_OPENED_BEFORE] ?: false
            
            Timber.d("hasAppBeenOpenedBefore - Saved install time: $savedInstallTime, appOpenedBefore: $appOpenedBefore, currentInstallTime: $currentInstallTime")
            
            // CASE 1: savedInstallTime is 0 but appOpenedBefore is true
            // This means DataStore was restored from backup but install time was never saved
            // Treat as FRESH INSTALL - user should see onboarding
            if (savedInstallTime == 0L && appOpenedBefore) {
                Timber.d("hasAppBeenOpenedBefore - Backup restore detected (savedInstallTime=0 but appOpenedBefore=true). Treating as fresh install.")
                false
            }
            // CASE 2: Install times don't match (and savedInstallTime is not 0)
            // This is a fresh install after uninstall (DataStore was restored from backup)
            else if (savedInstallTime != 0L && savedInstallTime != currentInstallTime) {
                Timber.d("hasAppBeenOpenedBefore - Install time mismatch! Fresh install detected. Saved: $savedInstallTime, Current: $currentInstallTime")
                false
            }
            // CASE 3: Normal case - return the actual value
            else {
                Timber.d("hasAppBeenOpenedBefore - Normal case, returning appOpenedBefore: $appOpenedBefore")
                appOpenedBefore
            }
        }.first()
    }

    /**
     * Mark that the app has been opened
     * Also saves the current install time to detect future reinstalls
     * AND resets permission flags for fresh installs
     */
    suspend fun markAppAsOpened() {
        Timber.d("markAppAsOpened - Marking app as opened...")
        val currentInstallTime = getAppInstallTime()
        
        context.dataStore.edit { preferences ->
            preferences[APP_OPENED_BEFORE] = true
            preferences[SAVED_INSTALL_TIME] = currentInstallTime.toString()
        }
        
        // Also reset permission flags in SharedPreferences for fresh install
        // This ensures permissions are asked again on reinstall
        val sharedPrefs = context.getSharedPreferences("permission_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("permissions_asked_on_role_screen", false).apply()
        Timber.d("markAppAsOpened - Reset permission flags for fresh install")
        
        Timber.d("markAppAsOpened - App marked as opened with install time: $currentInstallTime")
    }

    /**
     * Save authentication method ("GOOGLE" or "PHONE_OTP")
     */
    suspend fun saveAuthMethod(authMethod: String) {
        Timber.i("saveAuthMethod: $authMethod")
        context.dataStore.edit { preferences ->
            preferences[AUTH_METHOD] = authMethod
        }
        Timber.i("Auth method saved successfully")
    }

    /**
     * Get authentication method
     */
    suspend fun getAuthMethod(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[AUTH_METHOD]
        }.first()
    }

    /**
     * Save phone number (from OTP verification)
     */
    suspend fun savePhoneNumber(phone: String) {
        Timber.i("savePhoneNumber: $phone")
        context.dataStore.edit { preferences ->
            preferences[USER_PHONE] = phone
        }
        Timber.i("Phone number saved successfully")
    }

    /**
     * Get saved phone number
     */
    suspend fun getPhoneNumber(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[USER_PHONE]
        }.first()
    }

    /**
     * Save referral code (from signup)
     * FIXED: Use lowercase to match Cloud Function format
     */
    suspend fun saveReferralCode(code: String) {
        Timber.i("🎁 REFERRAL: Saving referral code: $code")
        context.dataStore.edit { preferences ->
            preferences[REFERRAL_CODE] = normalizeReferralCode(code)
        }
        Timber.i("🎁 REFERRAL: Referral code saved successfully")
    }

    /**
     * Get saved referral code
     */
    suspend fun getReferralCode(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[REFERRAL_CODE]
        }.first()
    }

    /**
     * Clear saved referral code (after applying)
     */
    suspend fun clearReferralCode() {
        Timber.i("🎁 REFERRAL: Clearing referral code")
        context.dataStore.edit { preferences ->
            preferences.remove(REFERRAL_CODE)
        }
    }

    /**
     * Check if onboarding has been completed
     * This is separate from hasAppBeenOpenedBefore to handle app restart during language selection
     */
    suspend fun hasOnboardingBeenCompleted(): Boolean {
        Timber.d("hasOnboardingBeenCompleted - Checking...")
        
        val currentInstallTime = getAppInstallTime()
        
        return context.dataStore.data.map { preferences ->
            val savedInstallTime = preferences[SAVED_INSTALL_TIME]?.toLongOrNull() ?: 0L
            val onboardingCompleted = preferences[ONBOARDING_COMPLETED] ?: false
            
            Timber.d("hasOnboardingBeenCompleted - Saved install time: $savedInstallTime, onboardingCompleted: $onboardingCompleted, currentInstallTime: $currentInstallTime")
            
            // If install times don't match, this is a fresh install - onboarding not completed
            if (savedInstallTime != 0L && savedInstallTime != currentInstallTime) {
                Timber.d("hasOnboardingBeenCompleted - Install time mismatch! Fresh install detected.")
                false
            } else {
                onboardingCompleted
            }
        }.first()
    }

    /**
     * Mark onboarding as completed
     * Called when user finishes all onboarding screens and reaches role selection
     */
    suspend fun markOnboardingCompleted() {
        Timber.d("markOnboardingCompleted - Marking onboarding as completed...")
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = true
        }
        Timber.d("markOnboardingCompleted - Onboarding marked as completed")
    }
}

/**
 * Data class representing profile setup status
 */
data class ProfileSetupStatus(
    val isComplete: Boolean,
    val completionPercentage: Int,
    val missingFields: List<String>,
    val shouldShowSetup: Boolean
)
