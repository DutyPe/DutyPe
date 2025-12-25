package com.example.dutype.state

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dutype.models.UserRole
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
    private val context: Context
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
        
        // Profile completion percentage keys
        private val WORKER_COMPLETION_PERCENTAGE = stringPreferencesKey("worker_completion_percentage")
        private val EMPLOYER_COMPLETION_PERCENTAGE = stringPreferencesKey("employer_completion_percentage")
        
            // Missing fields keys
            private val WORKER_MISSING_FIELDS = stringPreferencesKey("worker_missing_fields")
            private val EMPLOYER_MISSING_FIELDS = stringPreferencesKey("employer_missing_fields")
            
            // First-time user detection
            private val APP_OPENED_BEFORE = booleanPreferencesKey("app_opened_before")
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
        Timber.i("saveUserInfo - email: $email, name: $name, role: $role")
        
        context.dataStore.edit { preferences ->
            preferences[USER_EMAIL] = email
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
     */
    suspend fun saveCompletionPercentage(role: UserRole, percentage: Int) {
        context.dataStore.edit { preferences ->
            when (role) {
                UserRole.WORKER -> preferences[WORKER_COMPLETION_PERCENTAGE] = percentage.toString()
                UserRole.EMPLOYER -> preferences[EMPLOYER_COMPLETION_PERCENTAGE] = percentage.toString()
                else -> { /* Do nothing */ }
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
     * Check if the app has been opened before
     */
    suspend fun hasAppBeenOpenedBefore(): Boolean {
        Timber.d("hasAppBeenOpenedBefore - Checking...")
        return context.dataStore.data.map { preferences ->
            val result = preferences[APP_OPENED_BEFORE] ?: false
            Timber.d("hasAppBeenOpenedBefore - Result: $result")
            result
        }.first()
    }

    /**
     * Mark that the app has been opened
     */
    suspend fun markAppAsOpened() {
        Timber.d("markAppAsOpened - Marking app as opened...")
        context.dataStore.edit { preferences ->
            preferences[APP_OPENED_BEFORE] = true
        }
        Timber.d("markAppAsOpened - App marked as opened successfully")
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
