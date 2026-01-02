package com.example.dutype.utils

import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Retry utilities with exponential backoff for network operations
 */
object RetryUtils {
    
    /**
     * Execute a suspend function with retry logic and exponential backoff
     * 
     * @param maxRetries Maximum number of retry attempts (default 3)
     * @param initialDelay Initial delay in milliseconds (default 1000ms)
     * @param maxDelay Maximum delay in milliseconds (default 10000ms)
     * @param factor Backoff multiplier factor (default 2.0 for exponential)
     * @param block The suspend function to execute
     * @return Result of the operation
     */
    suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelay: Long = 1000L,
        maxDelay: Long = 10000L,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                Timber.d("Attempt ${attempt + 1} of $maxRetries")
                return block()
            } catch (e: Exception) {
                lastException = e
                Timber.w(e, "Attempt ${attempt + 1} failed")
                
                // Don't delay on last attempt
                if (attempt < maxRetries - 1) {
                    Timber.d("Waiting ${currentDelay}ms before retry")
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
                }
            }
        }
        
        // All retries exhausted
        Timber.e(lastException, "All $maxRetries retry attempts failed")
        throw lastException ?: Exception("Max retries exceeded")
    }
    
    /**
     * Execute a suspend function with retry for Result type
     */
    suspend fun <T> retryWithBackoffResult(
        maxRetries: Int = 3,
        initialDelay: Long = 1000L,
        maxDelay: Long = 10000L,
        factor: Double = 2.0,
        block: suspend () -> Result<T>
    ): Result<T> {
        return try {
            retryWithBackoff(maxRetries, initialDelay, maxDelay, factor) {
                val result = block()
                if (result.isFailure) {
                    throw result.exceptionOrNull() ?: Exception("Operation failed")
                }
                result.getOrThrow()
            }.let { Result.success(it) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
}
