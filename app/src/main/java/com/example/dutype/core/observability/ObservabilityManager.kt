package com.example.dutype.core.observability

import com.example.dutype.core.error.DutyPeError
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observability surface used by [com.example.dutype.core.error.ErrorHandler].
 *
 * The richer trace / network / user-flow / analytics-event API was removed in
 * 2026-04 cleanup because it had no callers; performance tracing lives in
 * [com.example.dutype.performance.PerformanceTracker].
 */
@Singleton
class ObservabilityManager @Inject constructor(
    private val crashlytics: FirebaseCrashlytics
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun logError(
        error: DutyPeError,
        context: ErrorContext,
        additionalData: Map<String, Any> = emptyMap()
    ) {
        scope.launch {
            try {
                crashlytics.setCustomKey("error_type", error::class.simpleName ?: "Unknown")
                crashlytics.setCustomKey("context", context.operation)
                crashlytics.setCustomKey("user_id", context.userId ?: "anonymous")
                additionalData.forEach { (key, value) -> crashlytics.setCustomKey(key, value.toString()) }

                val exception = when (error) {
                    is DutyPeError.NetworkError -> Exception("Network Error: ${error.message}")
                    is DutyPeError.AuthError -> Exception("Auth Error: ${error.message}")
                    is DutyPeError.ValidationError -> Exception("Validation Error: ${error.message}")
                    is DutyPeError.BusinessError -> Exception("Business Error: ${error.message}")
                    is DutyPeError.FirestoreError -> Exception("Firestore Error: ${error.message}")
                    is DutyPeError.SystemError -> Exception("System Error: ${error.message}")
                }
                crashlytics.recordException(exception)

                Timber.e(
                    "[ERROR] event=error_occurred {error_type=${error::class.simpleName}, " +
                        "operation=${context.operation}, user_id=${context.userId ?: "anonymous"}}"
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to log error")
            }
        }
    }
}

data class ErrorContext(
    val operation: String,
    val userId: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)