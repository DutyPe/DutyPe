package com.example.dutype.core.error

import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enterprise Error Handler
 * 
 * Centralized error handling with:
 * - Automatic error categorization
 * - Crashlytics integration
 * - Error event broadcasting
 * - User-friendly error messages
 * - Automatic logout on auth errors
 * - P2: Integrated with ObservabilityManager for structured logging
 * 
 * Usage:
 * ```kotlin
 * try {
 *     // operation
 * } catch (e: Exception) {
 *     errorHandler.handle(e, "JobApplication")
 * }
 * ```
 * 
 * @author DutyPe Engineering Team
 * @since 3.0.0
 */
@Singleton
class ErrorHandler @Inject constructor(
    private val crashlytics: FirebaseCrashlytics,
    private val observabilityManager: com.example.dutype.core.observability.ObservabilityManager
) {
    
    // Error events for UI to observe
    private val _errorEvents = MutableSharedFlow<ErrorEvent>(replay = 0, extraBufferCapacity = 10)
    val errorEvents: SharedFlow<ErrorEvent> = _errorEvents.asSharedFlow()
    
    // Logout events for auth errors
    private val _logoutEvents = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val logoutEvents: SharedFlow<Unit> = _logoutEvents.asSharedFlow()
    
    /**
     * Handle an exception and convert to DutyPeError
     */
    fun handle(
        throwable: Throwable,
        context: String = "Unknown",
        metadata: Map<String, String> = emptyMap()
    ): DutyPeError {
        val error = DutyPeError.from(throwable)
        
        // Log error
        logError(error, context, metadata)
        
        // P2: Log to ObservabilityManager
        observabilityManager.logError(
            error = error,
            context = com.example.dutype.core.observability.ErrorContext(
                operation = context,
                metadata = metadata
            )
        )
        
        // Report to Crashlytics (non-fatal)
        reportToCrashlytics(error, context, metadata)
        
        // Emit error event for UI
        emitErrorEvent(error, context)
        
        // Trigger logout if needed
        if (error.shouldLogout) {
            triggerLogout()
        }
        
        return error
    }
    
    /**
     * Handle error with custom DutyPeError
     */
    fun handle(
        error: DutyPeError,
        context: String = "Unknown",
        metadata: Map<String, String> = emptyMap()
    ) {
        logError(error, context, metadata)
        
        // P2: Log to ObservabilityManager
        observabilityManager.logError(
            error = error,
            context = com.example.dutype.core.observability.ErrorContext(
                operation = context,
                metadata = metadata
            )
        )
        
        reportToCrashlytics(error, context, metadata)
        emitErrorEvent(error, context)
        
        if (error.shouldLogout) {
            triggerLogout()
        }
    }
    
    /**
     * Log error with structured logging
     */
    private fun logError(
        error: DutyPeError,
        context: String,
        metadata: Map<String, String>
    ) {
        val logMessage = buildString {
            append("Error [${error.errorCode}] in $context")
            append("\n  Message: ${error.message}")
            append("\n  User Message: ${error.userMessage}")
            append("\n  Retryable: ${error.isRetryable}")
            if (metadata.isNotEmpty()) {
                append("\n  Metadata: $metadata")
            }
        }
        
        when (error) {
            is DutyPeError.NetworkError -> Timber.w(logMessage)
            is DutyPeError.AuthError -> Timber.e(logMessage)
            is DutyPeError.ValidationError -> Timber.d(logMessage)
            is DutyPeError.BusinessError -> Timber.i(logMessage)
            is DutyPeError.FirestoreError -> Timber.e(logMessage)
            is DutyPeError.SystemError -> Timber.e(logMessage)
        }
    }
    
    /**
     * Report error to Firebase Crashlytics
     */
    private fun reportToCrashlytics(
        error: DutyPeError,
        context: String,
        metadata: Map<String, String>
    ) {
        try {
            // Set custom keys
            crashlytics.setCustomKey("error_code", error.errorCode)
            crashlytics.setCustomKey("error_context", context)
            crashlytics.setCustomKey("is_retryable", error.isRetryable)
            crashlytics.setCustomKey("should_logout", error.shouldLogout)
            
            // Set metadata
            metadata.forEach { (key, value) ->
                crashlytics.setCustomKey(key, value)
            }
            
            // Record exception (non-fatal)
            val exception = when (error) {
                is DutyPeError.NetworkError -> error.cause ?: Exception(error.message)
                is DutyPeError.AuthError -> error.cause ?: Exception(error.message)
                is DutyPeError.ValidationError -> error.cause ?: Exception(error.message)
                is DutyPeError.BusinessError -> error.cause ?: Exception(error.message)
                is DutyPeError.FirestoreError -> error.cause ?: Exception(error.message)
                is DutyPeError.SystemError -> error.cause ?: Exception(error.message)
            }
            
            crashlytics.recordException(exception)
        } catch (e: Exception) {
            Timber.e(e, "Failed to report to Crashlytics")
        }
    }
    
    /**
     * Emit error event for UI to handle
     */
    private fun emitErrorEvent(error: DutyPeError, context: String) {
        try {
            _errorEvents.tryEmit(ErrorEvent(error, context))
        } catch (e: Exception) {
            Timber.e(e, "Failed to emit error event")
        }
    }
    
    /**
     * Trigger logout event
     */
    private fun triggerLogout() {
        try {
            _logoutEvents.tryEmit(Unit)
            Timber.w("🚪 Logout triggered due to auth error")
        } catch (e: Exception) {
            Timber.e(e, "Failed to trigger logout")
        }
    }
    
    /**
     * Get user-friendly error message
     */
    fun getUserMessage(error: DutyPeError): String {
        return error.userMessage
    }
    
    /**
     * Check if error is retryable
     */
    fun isRetryable(error: DutyPeError): Boolean {
        return error.isRetryable
    }

    // ============================================
    // CRASHLYTICS UTILITY METHODS
    // Consolidated from CrashReportingHelper
    // ============================================

    /**
     * Log a breadcrumb message to Crashlytics for debugging crash context
     */
    fun logBreadcrumb(message: String) {
        try {
            crashlytics.log(message)
            Timber.d("📍 $message")
        } catch (e: Exception) {
            Timber.e(e, "Failed to log breadcrumb")
        }
    }

    /**
     * Set a custom key-value pair in Crashlytics
     */
    fun logEvent(key: String, value: Any) {
        try {
            when (value) {
                is String -> crashlytics.setCustomKey(key, value)
                is Int -> crashlytics.setCustomKey(key, value)
                is Long -> crashlytics.setCustomKey(key, value)
                is Float -> crashlytics.setCustomKey(key, value)
                is Double -> crashlytics.setCustomKey(key, value)
                is Boolean -> crashlytics.setCustomKey(key, value)
                else -> crashlytics.setCustomKey(key, value.toString())
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to log event: $key")
        }
    }

    /**
     * Set user identity in Crashlytics
     */
    fun setUserInfo(userId: String, email: String = "") {
        try {
            crashlytics.setUserId(userId)
            if (email.isNotBlank()) {
                crashlytics.setCustomKey("user_email", email)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set user info")
        }
    }
}

/**
 * Error event for UI observation
 */
data class ErrorEvent(
    val error: DutyPeError,
    val context: String,
    val timestamp: Long = System.currentTimeMillis()
)
