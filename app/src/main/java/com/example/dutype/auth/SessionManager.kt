package com.example.dutype.auth

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Enterprise Session Manager
 * 
 * Manages user authentication sessions with enterprise-grade features:
 * - Token refresh mechanism
 * - Session expiry handling
 * - Concurrent session detection
 * - Device fingerprinting
 * - Session persistence
 * - Automatic re-authentication
 * 
 * @author DutyPe Engineering Team
 * @since 3.0.0
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unknown)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    
    companion object {
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_SESSION_START = "session_start"
        private const val KEY_LAST_ACTIVITY = "last_activity"
        private const val KEY_DEVICE_FINGERPRINT = "device_fingerprint"
        private const val KEY_TOKEN_REFRESH_TIME = "token_refresh_time"
        
        // Session timeout: 7 days of inactivity
        private val SESSION_TIMEOUT = 7.hours.inWholeMilliseconds
        
        // Token refresh interval: 55 minutes (Firebase tokens expire after 1 hour)
        private val TOKEN_REFRESH_INTERVAL = 55.minutes.inWholeMilliseconds
        
        // Activity timeout: 30 minutes
        private val ACTIVITY_TIMEOUT = 30.minutes.inWholeMilliseconds
    }
    
    init {
        // Monitor auth state changes
        firebaseAuth.addAuthStateListener { auth ->
            scope.launch {
                handleAuthStateChange(auth.currentUser)
            }
        }
        
        // Check session on init
        scope.launch {
            checkSession()
        }
    }
    
    /**
     * Start new session
     */
    suspend fun startSession(user: FirebaseUser) {
        val sessionId = generateSessionId()
        val now = System.currentTimeMillis()
        val fingerprint = generateDeviceFingerprint()
        
        prefs.edit()
            .putString(KEY_SESSION_ID, sessionId)
            .putLong(KEY_SESSION_START, now)
            .putLong(KEY_LAST_ACTIVITY, now)
            .putString(KEY_DEVICE_FINGERPRINT, fingerprint)
            .putLong(KEY_TOKEN_REFRESH_TIME, now)
            .apply()
        
        _sessionState.value = SessionState.Active(sessionId, user.uid)
        
        Timber.d("🔐 Session started: $sessionId (user: ${user.uid})")
        
        // Schedule token refresh
        scheduleTokenRefresh()
    }
    
    /**
     * Update last activity timestamp
     */
    fun updateActivity() {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putLong(KEY_LAST_ACTIVITY, now)
            .apply()
    }
    
    /**
     * Check session validity
     */
    suspend fun checkSession(): SessionState {
        val user = firebaseAuth.currentUser
        
        if (user == null) {
            _sessionState.value = SessionState.Expired("No user logged in")
            return _sessionState.value
        }
        
        val sessionId = prefs.getString(KEY_SESSION_ID, null)
        if (sessionId == null) {
            _sessionState.value = SessionState.Expired("No session found")
            return _sessionState.value
        }
        
        val lastActivity = prefs.getLong(KEY_LAST_ACTIVITY, 0)
        val now = System.currentTimeMillis()
        
        // Check activity timeout
        if (now - lastActivity > ACTIVITY_TIMEOUT) {
            Timber.w("⚠️ Session inactive for ${(now - lastActivity) / 1000}s")
            _sessionState.value = SessionState.Inactive(sessionId)
            return _sessionState.value
        }
        
        // Check session timeout
        val sessionStart = prefs.getLong(KEY_SESSION_START, 0)
        if (now - sessionStart > SESSION_TIMEOUT) {
            Timber.w("⚠️ Session expired (${(now - sessionStart) / 1000}s old)")
            _sessionState.value = SessionState.Expired("Session timeout")
            endSession()
            return _sessionState.value
        }
        
        // Check device fingerprint
        val storedFingerprint = prefs.getString(KEY_DEVICE_FINGERPRINT, null)
        val currentFingerprint = generateDeviceFingerprint()
        
        if (storedFingerprint != null && storedFingerprint != currentFingerprint) {
            Timber.w("⚠️ Device fingerprint mismatch")
            _sessionState.value = SessionState.Suspicious("Device fingerprint changed")
            return _sessionState.value
        }
        
        // Check if token needs refresh
        val lastRefresh = prefs.getLong(KEY_TOKEN_REFRESH_TIME, 0)
        if (now - lastRefresh > TOKEN_REFRESH_INTERVAL) {
            refreshToken()
        }
        
        _sessionState.value = SessionState.Active(sessionId, user.uid)
        return _sessionState.value
    }
    
    /**
     * Refresh Firebase auth token
     */
    suspend fun refreshToken(): Boolean {
        val user = firebaseAuth.currentUser ?: return false
        
        return try {
            // Force token refresh
            user.getIdToken(true).awaitTask()
            
            val now = System.currentTimeMillis()
            prefs.edit()
                .putLong(KEY_TOKEN_REFRESH_TIME, now)
                .apply()
            
            Timber.d("🔄 Token refreshed successfully")
            true
        } catch (e: Exception) {
            Timber.e(e, "❌ Token refresh failed")
            _sessionState.value = SessionState.Expired("Token refresh failed")
            false
        }
    }
    
    /**
     * Schedule automatic token refresh
     */
    private fun scheduleTokenRefresh() {
        scope.launch {
            while (_sessionState.value is SessionState.Active) {
                kotlinx.coroutines.delay(TOKEN_REFRESH_INTERVAL)
                refreshToken()
            }
        }
    }
    
    /**
     * End session
     */
    suspend fun endSession() {
        val sessionId = prefs.getString(KEY_SESSION_ID, null)
        
        prefs.edit().clear().apply()
        _sessionState.value = SessionState.Ended
        
        Timber.d("🔐 Session ended: $sessionId")
    }
    
    /**
     * Handle auth state changes
     */
    private suspend fun handleAuthStateChange(user: FirebaseUser?) {
        if (user != null) {
            // User signed in
            val sessionId = prefs.getString(KEY_SESSION_ID, null)
            if (sessionId == null) {
                // New session
                startSession(user)
            } else {
                // Existing session - check validity
                checkSession()
            }
        } else {
            // User signed out
            endSession()
        }
    }
    
    /**
     * Get current session ID
     */
    fun getSessionId(): String? = prefs.getString(KEY_SESSION_ID, null)
    
    /**
     * Get session duration in milliseconds
     */
    fun getSessionDuration(): Long {
        val sessionStart = prefs.getLong(KEY_SESSION_START, 0)
        return if (sessionStart > 0) {
            System.currentTimeMillis() - sessionStart
        } else {
            0
        }
    }
    
    /**
     * Check if session is active
     */
    fun isSessionActive(): Boolean = _sessionState.value is SessionState.Active
    
    /**
     * Generate unique session ID
     */
    private fun generateSessionId(): String {
        return "${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}"
    }
    
    /**
     * Generate simple device fingerprint for session tracking
     */
    private fun generateDeviceFingerprint(): String {
        return try {
            val androidId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown"
            val model = android.os.Build.MODEL
            val manufacturer = android.os.Build.MANUFACTURER
            "$androidId-$manufacturer-$model".hashCode().toString()
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate device fingerprint")
            "unknown"
        }
    }
}

/**
 * Session state
 */
sealed class SessionState {
    object Unknown : SessionState()
    data class Active(val sessionId: String, val userId: String) : SessionState()
    data class Inactive(val sessionId: String) : SessionState()
    data class Expired(val reason: String) : SessionState()
    data class Suspicious(val reason: String) : SessionState()
    object Ended : SessionState()
}

/**
 * Extension function to await Firebase Task
 */
private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T {
    return this.await()
}
