package com.example.dutype.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import kotlin.math.min
import kotlin.math.pow

/**
 * NetworkUtils - P1 Performance Fixes
 * 
 * Provides utilities for:
 * - Request timeouts (5 seconds)
 * - Exponential backoff for retries
 * - Network error handling
 * 
 * @author DutyPe Engineering Team
 * @since 2.5.0 (P1 Performance Fixes)
 */
object NetworkUtils {
    
    // P1 FIX: Request timeout - 5 seconds
    const val REQUEST_TIMEOUT_MS = 5000L
    
    // P1 FIX: Exponential backoff configuration
    private const val INITIAL_BACKOFF_MS = 1000L // 1 second
    private const val MAX_BACKOFF_MS = 32000L // 32 seconds
    private const val MAX_RETRIES = 3
    private const val BACKOFF_MULTIPLIER = 2.0
    
    /**
     * Execute a network request with timeout
     * 
     * P1 FIX: Adds 5-second timeout to prevent hanging requests
     * 
     * @param timeoutMs Timeout in milliseconds (default: 5000ms)
     * @param block The suspend function to execute
     * @return Result of the operation
     */
    suspend fun <T> withRequestTimeout(
        timeoutMs: Long = REQUEST_TIMEOUT_MS,
        block: suspend () -> T
    ): Result<T> {
        return try {
            val result = kotlinx.coroutines.withTimeout(timeoutMs) {
                block()
            }
            Result.success(result)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Timber.w("⏱️ Request timeout after ${timeoutMs}ms")
            Result.failure(NetworkTimeoutException("Request timed out after ${timeoutMs}ms"))
        } catch (e: Exception) {
            Timber.e(e, "❌ Network request failed")
            Result.failure(e)
        }
    }
    
    /**
     * Execute a network request with exponential backoff retry
     * 
     * P1 FIX: Implements exponential backoff for failed requests
     * - Retry 1: Wait 1 second
     * - Retry 2: Wait 2 seconds
     * - Retry 3: Wait 4 seconds
     * 
     * @param maxRetries Maximum number of retries (default: 3)
     * @param initialBackoffMs Initial backoff delay (default: 1000ms)
     * @param maxBackoffMs Maximum backoff delay (default: 32000ms)
     * @param shouldRetry Predicate to determine if error should be retried
     * @param block The suspend function to execute
     * @return Result of the operation
     */
    suspend fun <T> withExponentialBackoff(
        maxRetries: Int = MAX_RETRIES,
        initialBackoffMs: Long = INITIAL_BACKOFF_MS,
        maxBackoffMs: Long = MAX_BACKOFF_MS,
        shouldRetry: (Throwable) -> Boolean = { true },
        block: suspend () -> T
    ): Result<T> {
        var currentRetry = 0
        var lastException: Throwable? = null
        
        while (currentRetry <= maxRetries) {
            try {
                val result = block()
                if (currentRetry > 0) {
                    Timber.d("✅ Request succeeded after $currentRetry retries")
                }
                return Result.success(result)
            } catch (e: Exception) {
                lastException = e
                
                // Check if we should retry
                if (currentRetry >= maxRetries || !shouldRetry(e)) {
                    Timber.w("❌ Request failed after $currentRetry retries: ${e.message}")
                    return Result.failure(e)
                }
                
                // Calculate backoff delay with exponential increase
                val backoffMs = min(
                    initialBackoffMs * BACKOFF_MULTIPLIER.pow(currentRetry.toDouble()).toLong(),
                    maxBackoffMs
                )
                
                Timber.d("🔄 Retry $currentRetry/$maxRetries after ${backoffMs}ms: ${e.message}")
                delay(backoffMs)
                currentRetry++
            }
        }
        
        // Should never reach here, but just in case
        return Result.failure(lastException ?: Exception("Unknown error"))
    }
    
    /**
     * Execute a network request with both timeout and exponential backoff
     * 
     * P1 FIX: Combines timeout and retry logic for robust network calls
     * 
     * @param timeoutMs Timeout per attempt (default: 5000ms)
     * @param maxRetries Maximum number of retries (default: 3)
     * @param shouldRetry Predicate to determine if error should be retried
     * @param block The suspend function to execute
     * @return Result of the operation
     */
    suspend fun <T> withTimeoutAndRetry(
        timeoutMs: Long = REQUEST_TIMEOUT_MS,
        maxRetries: Int = MAX_RETRIES,
        shouldRetry: (Throwable) -> Boolean = { isRetryableError(it) },
        block: suspend () -> T
    ): Result<T> {
        return withExponentialBackoff(
            maxRetries = maxRetries,
            shouldRetry = shouldRetry
        ) {
            kotlinx.coroutines.withTimeout(timeoutMs) {
                block()
            }
        }
    }
    
    /**
     * Determine if an error is retryable
     * 
     * Retryable errors:
     * - Network timeouts
     * - Connection errors
     * - Server errors (5xx)
     * 
     * Non-retryable errors:
     * - Authentication errors (401, 403)
     * - Not found errors (404)
     * - Bad request errors (400)
     */
    fun isRetryableError(error: Throwable): Boolean {
        return when (error) {
            is NetworkTimeoutException -> true
            is java.net.SocketTimeoutException -> true
            is java.net.UnknownHostException -> true
            is java.net.ConnectException -> true
            is java.io.IOException -> true
            else -> {
                // Check error message for common retryable patterns
                val message = error.message?.lowercase() ?: ""
                message.contains("timeout") ||
                message.contains("connection") ||
                message.contains("network") ||
                message.contains("unavailable")
            }
        }
    }
    
    /**
     * Custom exception for network timeouts
     */
    class NetworkTimeoutException(message: String) : Exception(message)
}
