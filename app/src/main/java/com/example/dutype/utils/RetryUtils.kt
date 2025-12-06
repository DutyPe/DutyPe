package com.example.dutype.utils

import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.math.pow

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
    
    /**
     * Calculate exponential backoff delay
     */
    fun calculateBackoffDelay(
        attempt: Int,
        initialDelay: Long = 1000L,
        maxDelay: Long = 10000L,
        factor: Double = 2.0
    ): Long {
        val delay = (initialDelay * factor.pow(attempt.toDouble())).toLong()
        return delay.coerceAtMost(maxDelay)
    }
}

/**
 * Rate limiter to prevent excessive API calls
 */
class RateLimiter(
    private val maxCalls: Int,
    private val timeWindowMs: Long = 60000L // 1 minute default
) {
    private val timestamps = mutableListOf<Long>()
    
    /**
     * Check if request is allowed based on rate limit
     */
    fun allowRequest(): Boolean {
        val now = System.currentTimeMillis()
        
        synchronized(timestamps) {
            // Remove timestamps outside the time window
            timestamps.removeAll { it < now - timeWindowMs }
            
            return if (timestamps.size < maxCalls) {
                timestamps.add(now)
                true
            } else {
                Timber.w("Rate limit exceeded: $maxCalls calls per ${timeWindowMs}ms")
                false
            }
        }
    }
    
    /**
     * Get time until next request is allowed
     */
    fun getTimeUntilNextRequest(): Long {
        val now = System.currentTimeMillis()
        synchronized(timestamps) {
            if (timestamps.size < maxCalls) return 0
            val oldest = timestamps.minOrNull() ?: return 0
            return (oldest + timeWindowMs - now).coerceAtLeast(0)
        }
    }
    
    /**
     * Reset the rate limiter
     */
    fun reset() {
        synchronized(timestamps) {
            timestamps.clear()
        }
    }
}
