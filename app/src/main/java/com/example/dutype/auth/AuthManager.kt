package com.example.dutype.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.dutype.cache.JobCacheManager
import com.example.dutype.models.User
import com.example.dutype.services.FCMTokenManager
import com.example.dutype.state.AppStateManager
import com.example.dutype.state.ProfileSetupStateManager
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthManager - Manages local authentication state
 * 
 * REFACTORED: 
 * - Removed dead code (token-related functions never used)
 * - Uses constructor-injected dependencies
 * - Fixed CoroutineScope to use SupervisorJob for proper lifecycle
 * - Integrated AppStateManager for proper session cleanup on logout
 * - ENTERPRISE: Integrated with SessionManager for token refresh and session tracking
 */
@Singleton
class AuthManager @Inject constructor(
    private val context: Context,
    private val fcmTokenManager: FCMTokenManager,
    private val profileSetupStateManager: ProfileSetupStateManager,
    private val appStateManager: AppStateManager,
    private val jobCacheManager: JobCacheManager,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // Use SupervisorJob to prevent child failures from cancelling other operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val KEY_USER = "current_user"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        prefs.edit().putString(KEY_USER, userJson).apply()
        Timber.d("AuthManager - User saved: ${user.id}")
    }
    
    fun getCurrentUser(): User? {
        val userJson = prefs.getString(KEY_USER, null)
        return if (userJson != null) {
            try {
                gson.fromJson(userJson, User::class.java)
            } catch (e: Exception) {
                Timber.e(e, "AuthManager - Error parsing user JSON")
                null
            }
        } else null
    }
    
    fun setLoggedIn(isLoggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
        Timber.d("AuthManager - Login state set: $isLoggedIn")
        
        // Start session when user logs in
        if (isLoggedIn) {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                scope.launch {
                    try {
                        sessionManager.startSession(firebaseUser)
                        Timber.d("AuthManager - Session started for user: ${firebaseUser.uid}")
                    } catch (e: Exception) {
                        Timber.e(e, "AuthManager - Failed to start session")
                    }
                }
            }
        }
    }
    
    fun isLoggedIn(): Boolean {
        // Check both local state and Firebase auth state
        val localLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val firebaseUser = firebaseAuth.currentUser
        return localLoggedIn && firebaseUser != null
    }
    
    /**
     * Logout - Clears all local auth state and Firebase session
     * 
     * This is the CANONICAL logout implementation. All logout operations
     * should route through this method.
     * 
     * Clears:
     * - Local SharedPreferences
     * - Firebase Auth session
     * - FCM token
     * - AppStateManager session (saved jobs, applications, profile state)
     * - SessionManager (ends session tracking)
     */
    fun logout() {
        Timber.d("AuthManager - Logout initiated")
        
        // Clear local preferences
        prefs.edit().clear().apply()
        
        // Sign out from Firebase
        firebaseAuth.signOut()
        
        // Clear all state managers and remove FCM token
        scope.launch {
            try {
                // End session tracking
                sessionManager.endSession()
                Timber.d("AuthManager - Session ended")
            } catch (e: Exception) {
                Timber.e(e, "AuthManager - Error ending session")
            }
            
            try {
                // Clear AppStateManager session (clears saved jobs, applications, profile state)
                appStateManager.clearSession()
                Timber.d("AuthManager - AppStateManager session cleared")
            } catch (e: Exception) {
                Timber.e(e, "AuthManager - Error clearing AppStateManager session")
            }

            try {
                jobCacheManager.clearAllCaches()
                Timber.d("AuthManager - Job caches cleared")
            } catch (e: Exception) {
                Timber.e(e, "AuthManager - Error clearing job caches")
            }
            
            try {
                fcmTokenManager.removeToken()
                Timber.d("AuthManager - FCM token removed")
            } catch (e: Exception) {
                Timber.e(e, "AuthManager - Error removing FCM token")
            }

            // Re-subscribe this device to guest topic so we can send
            // re-engagement notifications when the user is logged out.
            try {
                fcmTokenManager.subscribeToTopic(FCMTokenManager.TOPIC_GUEST_USERS)
                Timber.d("AuthManager - Re-subscribed to guest_users topic")
            } catch (e: Exception) {
                Timber.e(e, "AuthManager - Error subscribing to guest topic")
            }
            
            // Note: profileSetupStateManager.resetProfileSetupState() is now called 
            // inside appStateManager.clearSession(), so no need to call it separately
        }
        
        Timber.d("AuthManager - Logout completed")
    }
    
    fun updateUser(user: User) {
        saveUser(user)
    }
    
    /**
     * Refresh user data from Firestore
     * Used after role switches to ensure cached data is up-to-date
     */
    suspend fun refreshUserFromFirestore(): User? {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                Timber.w("AuthManager - Cannot refresh: No Firebase user")
                return null
            }
            
            val userId = firebaseUser.uid

            val userDoc = firestore.collection(com.example.dutype.firestore.FirestoreCollections.USERS)
                .document(userId)
                .get()
                .await()
            
            if (!userDoc.exists()) {
                Timber.w("AuthManager - Cannot refresh: User document not found")
                return null
            }
            
            val userData = userDoc.data ?: return null
            
            // Parse roles array
            @Suppress("UNCHECKED_CAST")
            val rolesArray = userData["roles"] as? List<String> ?: emptyList()
            
            // Parse active role
            val activeRoleStr = userData["activeRole"] as? String
            val activeRole = try {
                if (activeRoleStr != null) {
                    com.example.dutype.models.UserRole.valueOf(activeRoleStr.uppercase())
                } else {
                    rolesArray.firstOrNull()?.let {
                        com.example.dutype.models.UserRole.valueOf(it.uppercase())
                    } ?: com.example.dutype.models.UserRole.WORKER
                }
            } catch (e: Exception) {
                com.example.dutype.models.UserRole.WORKER
            }

            @Suppress("UNCHECKED_CAST")
            val location = userData["location"] as? Map<String, Any>
            
            // Create User object
            val user = User(
                id = userId,
                fullName = userData["fullName"] as? String ?: "",
                phone = userData["phone"] as? String ?: "",
                roles = rolesArray,
                activeRole = activeRole,
                profileImageUrl = userData["profileImageUrl"] as? String,
                lat = (location?.get("lat") as? Number)?.toDouble() ?: 0.0,
                lng = (location?.get("lng") as? Number)?.toDouble() ?: 0.0
            )
            
            // Update cached user
            saveUser(user)
            
            Timber.d("AuthManager - User refreshed from Firestore: activeRole=${user.activeRole}")
            user
        } catch (e: Exception) {
            Timber.e(e, "AuthManager - Error refreshing user from Firestore")
            null
        }
    }
}
