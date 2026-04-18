package com.example.dutype.core.resilience

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Token-bucket rate limiter.
 * Only [checkRateLimit] is exposed; convenience execute/reset/status helpers
 * were removed in 2026-04 cleanup (no callers).
 */
@Singleton
class RateLimiter @Inject constructor() {

    private val endpointBuckets = ConcurrentHashMap<String, TokenBucket>()
    private val userBuckets = ConcurrentHashMap<String, TokenBucket>()
    private val mutex = Mutex()

    suspend fun checkRateLimit(
        endpoint: String,
        userId: String? = null,
        config: RateLimitConfig = RateLimitConfig.DEFAULT
    ): Boolean {
        val endpointAllowed = checkEndpointLimit(endpoint, config)
        if (!endpointAllowed) {
            Timber.w("⚠️ Rate limit exceeded for endpoint: $endpoint")
            return false
        }
        if (userId != null) {
            val userAllowed = checkUserLimit(userId, config)
            if (!userAllowed) {
                Timber.w("⚠️ Rate limit exceeded for user: $userId")
                return false
            }
        }
        return true
    }

    private suspend fun checkEndpointLimit(endpoint: String, config: RateLimitConfig): Boolean = mutex.withLock {
        val bucket = endpointBuckets.getOrPut(endpoint) {
            TokenBucket(config.maxTokens, config.refillRate, config.refillPeriodMs)
        }
        bucket.tryConsume()
    }

    private suspend fun checkUserLimit(userId: String, config: RateLimitConfig): Boolean = mutex.withLock {
        val bucket = userBuckets.getOrPut(userId) {
            TokenBucket(config.maxTokens, config.refillRate, config.refillPeriodMs)
        }
        bucket.tryConsume()
    }
}

private class TokenBucket(
    val capacity: Int,
    private val refillRate: Int,
    private val refillPeriodMs: Long
) {
    private var tokens: Int = capacity
    private var lastRefillTime: Long = System.currentTimeMillis()

    fun tryConsume(): Boolean {
        refill()
        return if (tokens > 0) {
            tokens--
            true
        } else false
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

data class RateLimitConfig(
    val maxTokens: Int,
    val refillRate: Int,
    val refillPeriodMs: Long
) {
    companion object {
        val DEFAULT = RateLimitConfig(maxTokens = 100, refillRate = 100, refillPeriodMs = 60_000L)
        val STRICT = RateLimitConfig(maxTokens = 10, refillRate = 10, refillPeriodMs = 60_000L)
        val LENIENT = RateLimitConfig(maxTokens = 1000, refillRate = 1000, refillPeriodMs = 60_000L)
        val BURST = RateLimitConfig(maxTokens = 5, refillRate = 5, refillPeriodMs = 1_000L)
    }
}