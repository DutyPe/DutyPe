package com.example.dutype.core.observability

import android.content.Context
import com.example.dutype.core.error.DutyPeError
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enterprise Observability Manager
 * 
 * Provides structured logging, performance metrics, and user journey tracking.
 * Integrates with Firebase Analytics, Crashlytics, and Performance Monitoring.
 * 
 * Features:
 * - Structured logging with correlation IDs
 * - Performance metrics tracking
 * - User journey tracking
 * - Error aggregation
 * - Network call monitoring
 * - Custom event tracking
 * 
 * @author DutyPe Engineering Team
 * @since 3.0.0
 */
@Singleton
class ObservabilityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analytics: FirebaseAnalytics,
    private val crashlytics: FirebaseCrashlytics,
    private val performance: FirebasePerformance
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeTraces = ConcurrentHashMap<String, Trace>()
    private val correlationIdThreadLocal = ThreadLocal<String>()
    
    /**
     * Log structured event with properties
     */
    fun logEvent(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        scope.launch {
            try {
                val correlationId = getOrCreateCorrelationId()
                val enrichedProperties = properties.toMutableMap().apply {
                    put("correlation_id", correlationId)
                    put("timestamp", System.currentTimeMillis())
                    put("event_category", event.category.name)
                }
                
                // Log to Firebase Analytics
                val bundle = android.os.Bundle().apply {
                    enrichedProperties.forEach { (key, value) ->
                        when (value) {
                            is String -> putString(key, value)
                            is Int -> putInt(key, value)
                            is Long -> putLong(key, value)
                            is Double -> putDouble(key, value)
                            is Boolean -> putBoolean(key, value)
                            else -> putString(key, value.toString())
                        }
                    }
                }
                analytics.logEvent(event.name, bundle)
                
                // Log to Timber with structured format
                val logMessage = buildStructuredLog(
                    level = LogLevel.INFO,
                    event = event.name,
                    properties = enrichedProperties
                )
                Timber.i(logMessage)
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to log event: ${event.name}")
            }
        }
    }
    
    /**
     * Log error with context
     */
    fun logError(
        error: DutyPeError,
        context: ErrorContext,
        additionalData: Map<String, Any> = emptyMap()
    ) {
        scope.launch {
            try {
                val correlationId = getOrCreateCorrelationId()
                
                // Set Crashlytics context
                crashlytics.setCustomKey("correlation_id", correlationId)
                crashlytics.setCustomKey("error_type", error::class.simpleName ?: "Unknown")
                crashlytics.setCustomKey("context", context.operation)
                crashlytics.setCustomKey("user_id", context.userId ?: "anonymous")
                
                additionalData.forEach { (key, value) ->
                    crashlytics.setCustomKey(key, value.toString())
                }
                
                // Log to Crashlytics
                val exception = when (error) {
                    is DutyPeError.NetworkError -> Exception("Network Error: ${error.message}")
                    is DutyPeError.AuthError -> Exception("Auth Error: ${error.message}")
                    is DutyPeError.ValidationError -> Exception("Validation Error: ${error.message}")
                    is DutyPeError.BusinessError -> Exception("Business Error: ${error.message}")
                    is DutyPeError.RateLimitError -> Exception("Rate Limit: ${error.message}")
                    is DutyPeError.FirestoreError -> Exception("Firestore Error: ${error.message}")
                    is DutyPeError.SystemError -> Exception("System Error: ${error.message}")
                }
                crashlytics.recordException(exception)
                
                // Log to Timber
                val logMessage = buildStructuredLog(
                    level = LogLevel.ERROR,
                    event = "error_occurred",
                    properties = mapOf(
                        "correlation_id" to correlationId,
                        "error_type" to (error::class.simpleName ?: "Unknown"),
                        "operation" to context.operation,
                        "user_id" to (context.userId ?: "anonymous")
                    ) + additionalData
                )
                Timber.e(logMessage)
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to log error")
            }
        }
    }
    
    /**
     * Track performance metric
     */
    fun trackPerformance(
        operation: String,
        duration: Long,
        metadata: Map<String, String> = emptyMap()
    ) {
        scope.launch {
            try {
                val correlationId = getOrCreateCorrelationId()
                
                // Log to Firebase Performance
                val trace = performance.newTrace(operation)
                metadata.forEach { (key, value) ->
                    trace.putAttribute(key, value)
                }
                trace.putAttribute("correlation_id", correlationId)
                trace.putMetric("duration_ms", duration)
                
                // Log to Timber
                val logMessage = buildStructuredLog(
                    level = LogLevel.DEBUG,
                    event = "performance_metric",
                    properties = mapOf(
                        "correlation_id" to correlationId,
                        "operation" to operation,
                        "duration_ms" to duration
                    ) + metadata
                )
                Timber.d(logMessage)
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to track performance")
            }
        }
    }
    
    /**
     * Start performance trace
     */
    fun startTrace(traceName: String): String {
        val traceId = UUID.randomUUID().toString()
        try {
            val trace = performance.newTrace(traceName)
            trace.putAttribute("correlation_id", getOrCreateCorrelationId())
            trace.start()
            activeTraces[traceId] = trace
            
            Timber.d("🔍 Trace started: $traceName (id: $traceId)")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start trace: $traceName")
        }
        return traceId
    }
    
    /**
     * Stop performance trace
     */
    fun stopTrace(traceId: String, metadata: Map<String, String> = emptyMap()) {
        try {
            val trace = activeTraces.remove(traceId)
            if (trace != null) {
                metadata.forEach { (key, value) ->
                    trace.putAttribute(key, value)
                }
                trace.stop()
                Timber.d("🔍 Trace stopped: $traceId")
            } else {
                Timber.w("⚠️ Trace not found: $traceId")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop trace: $traceId")
        }
    }
    
    /**
     * Track network call
     */
    fun trackNetworkCall(
        endpoint: String,
        method: String,
        duration: Long,
        statusCode: Int,
        requestSize: Long = 0,
        responseSize: Long = 0
    ) {
        scope.launch {
            try {
                val correlationId = getOrCreateCorrelationId()
                
                // Log to Firebase Performance
                val metric = performance.newHttpMetric(endpoint, method)
                metric.setRequestPayloadSize(requestSize)
                metric.setResponsePayloadSize(responseSize)
                metric.setHttpResponseCode(statusCode)
                metric.putAttribute("correlation_id", correlationId)
                
                // Log to Timber
                val logMessage = buildStructuredLog(
                    level = LogLevel.DEBUG,
                    event = "network_call",
                    properties = mapOf(
                        "correlation_id" to correlationId,
                        "endpoint" to endpoint,
                        "method" to method,
                        "duration_ms" to duration,
                        "status_code" to statusCode,
                        "request_size" to requestSize,
                        "response_size" to responseSize
                    )
                )
                Timber.d(logMessage)
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to track network call")
            }
        }
    }
    
    /**
     * Track user journey
     */
    fun trackUserFlow(
        flow: UserFlow,
        step: String,
        metadata: Map<String, Any> = emptyMap()
    ) {
        scope.launch {
            try {
                val correlationId = getOrCreateCorrelationId()
                
                // Log to Firebase Analytics
                val bundle = android.os.Bundle().apply {
                    putString("flow", flow.name)
                    putString("step", step)
                    putString("correlation_id", correlationId)
                    metadata.forEach { (key, value) ->
                        when (value) {
                            is String -> putString(key, value)
                            is Int -> putInt(key, value)
                            is Long -> putLong(key, value)
                            is Double -> putDouble(key, value)
                            is Boolean -> putBoolean(key, value)
                            else -> putString(key, value.toString())
                        }
                    }
                }
                analytics.logEvent("user_flow_${flow.name.lowercase()}", bundle)
                
                // Log to Timber
                val logMessage = buildStructuredLog(
                    level = LogLevel.INFO,
                    event = "user_flow",
                    properties = mapOf(
                        "correlation_id" to correlationId,
                        "flow" to flow.name,
                        "step" to step
                    ) + metadata
                )
                Timber.i(logMessage)
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to track user flow")
            }
        }
    }
    
    /**
     * Generate correlation ID for request tracing
     */
    fun generateCorrelationId(): String {
        val correlationId = "corr_${UUID.randomUUID()}"
        correlationIdThreadLocal.set(correlationId)
        return correlationId
    }
    
    /**
     * Attach correlation ID to current thread
     */
    fun attachCorrelationId(id: String) {
        correlationIdThreadLocal.set(id)
    }
    
    /**
     * Get current correlation ID
     */
    fun getCorrelationId(): String? = correlationIdThreadLocal.get()
    
    /**
     * Clear correlation ID
     */
    fun clearCorrelationId() {
        correlationIdThreadLocal.remove()
    }
    
    /**
     * Set user properties for analytics
     */
    fun setUserProperties(properties: Map<String, String>) {
        properties.forEach { (key, value) ->
            analytics.setUserProperty(key, value)
        }
    }
    
    /**
     * Set user ID for tracking
     */
    fun setUserId(userId: String) {
        analytics.setUserId(userId)
        crashlytics.setUserId(userId)
    }
    
    // ==================== PRIVATE METHODS ====================
    
    private fun getOrCreateCorrelationId(): String {
        return correlationIdThreadLocal.get() ?: generateCorrelationId()
    }
    
    private fun buildStructuredLog(
        level: LogLevel,
        event: String,
        properties: Map<String, Any>
    ): String {
        val timestamp = System.currentTimeMillis()
        val propertiesJson = properties.entries.joinToString(", ") { (k, v) ->
            "\"$k\":\"$v\""
        }
        return "[${level.name}] [$timestamp] event=$event {$propertiesJson}"
    }
}

/**
 * Analytics event
 */
data class AnalyticsEvent(
    val name: String,
    val category: EventCategory
)

/**
 * Event categories
 */
enum class EventCategory {
    USER_ACTION,
    SYSTEM_EVENT,
    BUSINESS_EVENT,
    ERROR_EVENT,
    PERFORMANCE_EVENT
}

/**
 * Error context
 */
data class ErrorContext(
    val operation: String,
    val userId: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * User flow
 */
enum class UserFlow {
    ONBOARDING,
    JOB_APPLICATION,
    JOB_POSTING,
    PROFILE_SETUP,
    AUTHENTICATION,
    PAYMENT,
    REFERRAL
}

/**
 * Log level
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

/**
 * Common analytics events
 */
object AnalyticsEvents {
    val APP_OPENED = AnalyticsEvent("app_opened", EventCategory.USER_ACTION)
    val JOB_VIEWED = AnalyticsEvent("job_viewed", EventCategory.USER_ACTION)
    val JOB_APPLIED = AnalyticsEvent("job_applied", EventCategory.BUSINESS_EVENT)
    val JOB_SAVED = AnalyticsEvent("job_saved", EventCategory.USER_ACTION)
    val PROFILE_COMPLETED = AnalyticsEvent("profile_completed", EventCategory.BUSINESS_EVENT)
    val LOGIN_SUCCESS = AnalyticsEvent("login_success", EventCategory.USER_ACTION)
    val LOGIN_FAILED = AnalyticsEvent("login_failed", EventCategory.ERROR_EVENT)
    val SEARCH_PERFORMED = AnalyticsEvent("search_performed", EventCategory.USER_ACTION)
    val FILTER_APPLIED = AnalyticsEvent("filter_applied", EventCategory.USER_ACTION)
}
