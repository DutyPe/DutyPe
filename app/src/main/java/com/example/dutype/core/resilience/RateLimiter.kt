package com.example.dutype.core.resilience

import com.example.dutype.core.error.DutyPeError
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enterprise Rate Limiter
 * 
 * Implements Token Bucket algorithm for rate limiting API calls.
 * Prevents API abuse and ensures fair resource usage.
 * 
 * Features:
 * - Per-endpoint rate limiting
 * - Per-user rate limiting
 * - Automatic token refill
 * - Quota management
 * - Graceful degradation
 * 
 * @author DutyPe Engineering Team
 * @since 3.0.0
 */
@Singleton
class RateLimiter @Inject constructor() {
    
    private val endpointBuckets = ConcurrentHashMap<String, TokenBucket>()
    private val userBuckets = ConcurrentHashMap<String, TokenBucket>()
    private val mutex = Mutex()
    
    /**
     * Check if request is allowed and consume token
     * 
     * @param endpoint API endpoint identifier
     * @param userId User identifier (optional)
     * @param config Rate limit configuration
     * @return true if allowed, false if rate limited
     */
    suspend fun checkRateLimit(
        endpoint: String,
        userId: String? = null,
        config: RateLimitConfig = RateLimitConfig.DEFAULT
    ): Boolean {
        // Check endpoint rate limit
        val endpointAllowed = checkEndpointLimit(endpoint, config)
        if (!endpointAllowed) {
            Timber.w("⚠️ Rate limit exceeded for endpoint: $endpoint")
            return false
        }
        
        // Check user rate limit if userId provided
        if (userId != null) {
            val userAllowed = checkUserLimit(userId, config)
            if (!userAllowed) {
                Timber.w("⚠️ Rate limit exceeded for user: $userId")
                return false
            }
        }
        
        return true
    }
    
    /**
     * Execute with rate limiting
     * Throws RateLimitError if limit exceeded
     */
    suspend fun <T> executeWithRateLimit(
        endpoint: String,
        userId: String? = null,
        config: RateLimitConfig = RateLimitConfig.DEFAULT,
        block: suspend () -> T
    ): T {
        val allowed = checkRateLimit(endpoint, userId, config)
        
        if (!allowed) {
            val retryAfter = getRetryAfter(endpoint)
            val error = DutyPeError.RateLimitError(
                message = "Rate limit exceeded for $endpoint",
                retryAfterSeconds = retryAfter / 1000 // Convert ms to seconds
            )
            throw error.toException()
        }
        
        return block()
    }
    
    /**
     * Execute with automatic retry on rate limit
     */
    suspend fun <T> executeWithRetry(
        endpoint: String,
        userId: String? = null,
        config: RateLimitConfig = RateLimitConfig.DEFAULT,
        maxRetries: Int = 3,
        block: suspend () -> T
    ): T {
        var lastError: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                return executeWithRateLimit(endpoint, userId, config, block)
            } catch (e: Exception) {
                lastError = e
                if (attempt < maxRetries - 1) {
                    // Extract retry time from message if possible, default to 1 second
                    val delayMs = 1000L
                    Timber.d("⏳ Rate limited, retrying after ${delayMs}ms (attempt ${attempt + 1}/$maxRetries)")
                    delay(delayMs)
                }
            }
        }
        
        throw lastError ?: DutyPeError.RateLimitError(
            message = "Rate limit exceeded after $maxRetries retries",
            retryAfterSeconds = 60
        ).toException()
    }
    
    /**
     * Get retry-after duration in milliseconds
     */
    private fun getRetryAfter(endpoint: String): Long {
        val bucket = endpointBuckets[endpoint] ?: return 1000L
        return bucket.getRefillTime()
    }
    
    /**
     * Check endpoint rate limit
     */
    private suspend fun checkEndpointLimit(endpoint: String, config: RateLimitConfig): Boolean = mutex.withLock {
        val bucket = endpointBuckets.getOrPut(endpoint) {
            TokenBucket(config.maxTokens, config.refillRate, config.refillPeriodMs)
        }
        return bucket.tryConsume()
    }
    
    /**
     * Check user rate limit
     */
    private suspend fun checkUserLimit(userId: String, config: RateLimitConfig): Boolean = mutex.withLock {
        val bucket = userBuckets.getOrPut(userId) {
            TokenBucket(config.maxTokens, config.refillRate, config.refillPeriodMs)
        }
        return bucket.tryConsume()
    }
    
    /**
     * Reset rate limits for endpoint
     */
    suspend fun resetEndpoint(endpoint: String) = mutex.withLock {
        endpointBuckets.remove(endpoint)
        Timber.d("🔄 Rate limit reset for endpoint: $endpoint")
    }
    
    /**
     * Reset rate limits for user
     */
    suspend fun resetUser(userId: String) = mutex.withLock {
        userBuckets.remove(userId)
        Timber.d("🔄 Rate limit reset for user: $userId")
    }
    
    /**
     * Clear all rate limits
     */
    suspend fun clearAll() = mutex.withLock {
        endpointBuckets.clear()
        userBuckets.clear()
        Timber.d("🔄 All rate limits cleared")
    }
    
    /**
     * Get current rate limit status
     */
    fun getStatus(endpoint: String): RateLimitStatus {
        val bucket = endpointBuckets[endpoint]
        return if (bucket != null) {
            RateLimitStatus(
                endpoint = endpoint,
                tokensAvailable = bucket.getAvailableTokens(),
                maxTokens = bucket.capacity,
                refillRate = bucket.refillRate,
                nextRefillMs = bucket.getRefillTime()
            )
        } else {
            RateLimitStatus(endpoint, 0, 0, 0, 0)
        }
    }
}

/**
 * Token Bucket implementation
 */
private class TokenBucket(
    val capacity: Int,
    val refillRate: Int,
    val refillPeriodMs: Long
) {
    private var tokens: Int = capacity
    private var lastRefillTime: Long = System.currentTimeMillis()
    
    fun tryConsume(): Boolean {
        refill()
        
        return if (tokens > 0) {
            tokens--
            true
        } else {
            false
        }
    }
    
    fun getAvailableTokens(): Int {
        refill()
        return tokens
    }
    
    fun getRefillTime(): Long {
        val timeSinceRefill = System.currentTimeMillis() - lastRefillTime
        val timeUntilRefill = refillPeriodMs - timeSinceRefill
        return if (timeUntilRefill > 0) timeUntilRefill else 0
    }
    
    private fun refill() {
        val now = System.currentTimeMillis()
        val timePassed = now - lastRefillTime
        val refillCount = (timePassed / refillPeriodMs).toInt() * refillRate
        
        if (refillCount > 0) {
            tokens = minOf(capacity, tokens + refillCount)
            lastRefillTime = now
        }
    }
}

/**
 * Rate limit configuration
 */
data class RateLimitConfig(
    val maxTokens: Int,
    val refillRate: Int,
    val refillPeriodMs: Long
) {
    companion object {
        // 100 requests per minute
        val DEFAULT = RateLimitConfig(
            maxTokens = 100,
            refillRate = 100,
            refillPeriodMs = 60_000L
        )
        
        // 10 requests per minute (strict)
        val STRICT = RateLimitConfig(
            maxTokens = 10,
            refillRate = 10,
            refillPeriodMs = 60_000L
        )
        
        // 1000 requests per minute (lenient)
        val LENIENT = RateLimitConfig(
            maxTokens = 1000,
            refillRate = 1000,
            refillPeriodMs = 60_000L
        )
        
        // 5 requests per second (burst)
        val BURST = RateLimitConfig(
            maxTokens = 5,
            refillRate = 5,
            refillPeriodMs = 1_000L
        )
    }
}

/**
 * Rate limit status
 */
data class RateLimitStatus(
    val endpoint: String,
    val tokensAvailable: Int,
    val maxTokens: Int,
    val refillRate: Int,
    val nextRefillMs: Long
)
