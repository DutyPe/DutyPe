package com.example.dutype.utils

import com.example.dutype.core.resilience.CircuitBreaker
import timber.log.Timber

/**
 * P4 FIX: Service Extension Functions
 * 
 * Provides reusable patterns for service operations:
 * - Circuit breaker integration
 * - Retry logic with exponential backoff
 * - Error handling standardization
 * 
 * @author DutyPe Engineering Team
 * @since 2.5.0
 */

/**
 * Execute operation with circuit breaker protection
 * 
 * Prevents cascading failures by opening circuit after threshold failures.
 * Automatically retries after timeout period.
 * 
 * @param circuitBreaker Circuit breaker instance
 * @param operation Operation to execute
 * @return Result of operation or failure if circuit is open
 */
suspend fun <T> executeWithCircuitBreaker(
    circuitBreaker: CircuitBreaker,
    operation: suspend () -> Result<T>
): Result<T> {
    // Check if circuit allows execution
    if (!circuitBreaker.canExecute()) {
        Timber.w("⚠️ Circuit breaker open - operation blocked")
        return Result.failure(Exception("Service temporarily unavailable"))
    }
    
    return try {
        val result = operation()
        
        if (result.isSuccess) {
            circuitBreaker.recordSuccess()
        } else {
            circuitBreaker.recordFailure()
        }
        
        result
    } catch (e: Exception) {
        circuitBreaker.recordFailure()
        Result.failure(e)
    }
}

/**
 * Execute operation with retry logic
 * 
 * Retries failed operations with exponential backoff.
 * Useful for transient network failures.
 * 
 * @param maxRetries Maximum number of retry attempts
 * @param initialDelayMs Initial delay before first retry
 * @param maxDelayMs Maximum delay between retries
 * @param operation Operation to execute
 * @return Result of operation
 */
suspend fun <T> executeWithRetry(
    maxRetries: Int = 3,
    initialDelayMs: Long = 1000,
    maxDelayMs: Long = 10000,
    operation: suspend () -> Result<T>
): Result<T> {
    var currentDelay = initialDelayMs
    var lastException: Exception? = null
    
    repeat(maxRetries + 1) { attempt ->
        try {
            val result = operation()
            if (result.isSuccess) {
                return result
            }
            lastException = result.exceptionOrNull() as? Exception
        } catch (e: Exception) {
            lastException = e
        }
        
        // Don't delay after last attempt
        if (attempt < maxRetries) {
            Timber.d("Retry attempt ${attempt + 1}/$maxRetries after ${currentDelay}ms")
            kotlinx.coroutines.delay(currentDelay)
            currentDelay = (currentDelay * 2).coerceAtMost(maxDelayMs)
        }
    }
    
    return Result.failure(lastException ?: Exception("Operation failed after $maxRetries retries"))
}

/**
 * Execute operation with timeout
 * 
 * Prevents operations from hanging indefinitely.
 * 
 * @param timeoutMs Timeout in milliseconds
 * @param operation Operation to execute
 * @return Result of operation or timeout error
 */
suspend fun <T> executeWithTimeout(
    timeoutMs: Long,
    operation: suspend () -> Result<T>
): Result<T> {
    return try {
        kotlinx.coroutines.withTimeout(timeoutMs) {
            operation()
        }
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
        Timber.w("⏱️ Operation timed out after ${timeoutMs}ms")
        Result.failure(Exception("Operation timed out"))
    }
}

/**
 * Combine circuit breaker, retry, and timeout
 * 
 * P4 FIX: Enterprise-grade operation execution with all resilience patterns.
 * 
 * @param circuitBreaker Circuit breaker instance
 * @param maxRetries Maximum retry attempts
 * @param timeoutMs Operation timeout
 * @param operation Operation to execute
 * @return Result of operation
 */
suspend fun <T> executeResilient(
    circuitBreaker: CircuitBreaker,
    maxRetries: Int = 3,
    timeoutMs: Long = 30000,
    operation: suspend () -> Result<T>
): Result<T> {
    return executeWithCircuitBreaker(circuitBreaker) {
        executeWithRetry(maxRetries) {
            executeWithTimeout(timeoutMs) {
                operation()
            }
        }
    }
}
