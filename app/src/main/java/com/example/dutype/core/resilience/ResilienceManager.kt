package com.example.dutype.core.resilience

import com.example.dutype.core.error.DutyPeError
import com.example.dutype.core.error.ErrorHandler
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * Enterprise Resilience Manager
 * 
 * Provides:
 * - Exponential backoff retry
 * - Circuit breaker pattern
 * - Timeout handling
 * - Fallback mechanisms
 * 
 * Usage:
 * ```kotlin
 * val result = resilienceManager.executeWithResilience(
 *     operation = { firestoreCall() },
 *     retryPolicy = RetryPolicy.NETWORK,
 *     context = "FetchJobs"
 * )
 * ```
 * 
 * @author DutyPe Engineering Team
 * @since 3.0.0
 */
@Singleton
class ResilienceManager @Inject constructor(
    private val errorHandler: ErrorHandler,
    private val circuitBreakerRegistry: CircuitBreakerRegistry
) {
    
    /**
     * Execute operation with full resilience (retry + circuit breaker)
     */
    suspend fun <T> executeWithResilience(
        operation: suspend () -> T,
        retryPolicy: RetryPolicy = RetryPolicy.DEFAULT,
        circuitBreakerKey: String? = null,
        context: String = "Unknown",
        fallback: (suspend (DutyPeError) -> T)? = null
    ): Result<T> {
        // Check circuit breaker first
        if (circuitBreakerKey != null) {
            val circuitBreaker = circuitBreakerRegistry.get(circuitBreakerKey)
            if (!circuitBreaker.canExecute()) {
                val error = DutyPeError.SystemError(
                    message = "Circuit breaker is OPEN for $circuitBreakerKey",
                    cause = null
                )
                errorHandler.handle(error, context)
                
                return if (fallback != null) {
                    try {
                        Result.success(fallback(error))
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                } else {
                    Result.failure(Exception(error.userMessage))
                }
            }
        }
        
        // Execute with retry
        return executeWithRetry(
            operation = operation,
            retryPolicy = retryPolicy,
            circuitBreakerKey = circuitBreakerKey,
            context = context,
            fallback = fallback
        )
    }
    
    /**
     * Execute with exponential backoff retry
     */
    private suspend fun <T> executeWithRetry(
        operation: suspend () -> T,
        retryPolicy: RetryPolicy,
        circuitBreakerKey: String?,
        context: String,
        fallback: (suspend (DutyPeError) -> T)?
    ): Result<T> {
        var attempt = 0
        var lastError: DutyPeError? = null
        
        while (attempt <= retryPolicy.maxRetries) {
            try {
                Timber.d("🔄 Attempt ${attempt + 1}/${retryPolicy.maxRetries + 1} for $context")
                
                val result = operation()
                
                // Success - record in circuit breaker
                if (circuitBreakerKey != null) {
                    circuitBreakerRegistry.get(circuitBreakerKey).recordSuccess()
                }
                
                if (attempt > 0) {
                    Timber.i("✅ Operation succeeded after $attempt retries: $context")
                }
                
                return Result.success(result)
                
            } catch (e: Exception) {
                val error = errorHandler.handle(e, context)
                lastError = error
                
                // Record failure in circuit breaker
                if (circuitBreakerKey != null) {
                    circuitBreakerRegistry.get(circuitBreakerKey).recordFailure()
                }
                
                // Check if retryable
                if (!error.isRetryable || attempt >= retryPolicy.maxRetries) {
                    Timber.w("❌ Operation failed (not retryable or max retries): $context")
                    break
                }
                
                // Calculate backoff delay
                val delayMs = calculateBackoff(
                    attempt = attempt,
                    baseDelayMs = retryPolicy.baseDelayMs,
                    maxDelayMs = retryPolicy.maxDelayMs,
                    jitterFactor = retryPolicy.jitterFactor
                )
                
                Timber.d("⏳ Retrying in ${delayMs}ms (attempt ${attempt + 1})")
                delay(delayMs)
                
                attempt++
            }
        }
        
        // All retries exhausted - try fallback
        return if (fallback != null && lastError != null) {
            try {
                Timber.i("🔄 Executing fallback for $context")
                Result.success(fallback(lastError))
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(Exception(lastError?.userMessage ?: "Operation failed"))
        }
    }
    
    /**
     * Calculate exponential backoff with jitter
     */
    private fun calculateBackoff(
        attempt: Int,
        baseDelayMs: Long,
        maxDelayMs: Long,
        jitterFactor: Double
    ): Long {
        // Exponential backoff: baseDelay * 2^attempt
        val exponentialDelay = baseDelayMs * (2.0.pow(attempt.toDouble())).toLong()
        
        // Cap at max delay
        val cappedDelay = min(exponentialDelay, maxDelayMs)
        
        // Add jitter (random factor to prevent thundering herd)
        val jitter = (cappedDelay * jitterFactor * Math.random()).toLong()
        
        return cappedDelay + jitter
    }
}

/**
 * Retry policy configuration
 */
data class RetryPolicy(
    val maxRetries: Int,
    val baseDelayMs: Long,
    val maxDelayMs: Long,
    val jitterFactor: Double = 0.1
) {
    companion object {
        /**
         * Default policy: 3 retries, 1s base, 10s max
         */
        val DEFAULT = RetryPolicy(
            maxRetries = 3,
            baseDelayMs = 1000,
            maxDelayMs = 10000
        )
        
        /**
         * Network policy: 5 retries, 500ms base, 30s max
         */
        val NETWORK = RetryPolicy(
            maxRetries = 5,
            baseDelayMs = 500,
            maxDelayMs = 30000
        )
        
        /**
         * Quick policy: 2 retries, 200ms base, 2s max
         */
        val QUICK = RetryPolicy(
            maxRetries = 2,
            baseDelayMs = 200,
            maxDelayMs = 2000
        )
        
        /**
         * No retry policy
         */
        val NONE = RetryPolicy(
            maxRetries = 0,
            baseDelayMs = 0,
            maxDelayMs = 0
        )
    }
}
