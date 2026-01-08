package com.example.dutype.utils

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P1 PERFORMANCE FIX: RequestDeduplicator
 * 
 * Prevents duplicate concurrent API calls for the same resource.
 * When multiple callers request the same data simultaneously,
 * only one actual request is made and the result is shared.
 * 
 * Use cases:
 * - Multiple screens requesting same job details
 * - Rapid navigation causing duplicate API calls
 * - Pull-to-refresh while data is still loading
 * 
 * Example:
 * ```kotlin
 * // Multiple concurrent calls to getJob("123") will result in only ONE API call
 * val job1 = deduplicator.dedupe("job_123") { repository.getJob("123") }
 * val job2 = deduplicator.dedupe("job_123") { repository.getJob("123") }
 * // job1 and job2 will receive the same result from a single API call
 * ```
 * 
 * @author DutyPe Engineering Team
 * @since 2.4.0
 */
@Singleton
class RequestDeduplicator @Inject constructor() {
    
    // Map of in-flight requests keyed by unique identifier
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<*>>()
    
    // Track request counts for monitoring
    private val requestCounts = ConcurrentHashMap<String, Int>()
    private val dedupeHits = ConcurrentHashMap<String, Int>()
    
    /**
     * Execute a suspending block with deduplication.
     * 
     * If a request with the same key is already in flight, this will
     * wait for that request to complete and return its result instead
     * of making a duplicate request.
     * 
     * @param key Unique identifier for this request (e.g., "job_123", "user_profile_456")
     * @param block The suspending block to execute if no duplicate is in flight
     * @return The result of the block (either from this call or a deduplicated call)
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> dedupe(key: String, block: suspend () -> T): T {
        // Check if there's already an in-flight request for this key
        val existingRequest = inFlightRequests[key] as? Deferred<T>
        
        if (existingRequest != null && existingRequest.isActive) {
            // Dedupe hit - wait for existing request
            dedupeHits[key] = (dedupeHits[key] ?: 0) + 1
            Timber.d("🔄 DEDUPE HIT: Reusing in-flight request for key=$key (hits: ${dedupeHits[key]})")
            return existingRequest.await()
        }
        
        // No existing request - create new one
        requestCounts[key] = (requestCounts[key] ?: 0) + 1
        Timber.d("🔄 DEDUPE: New request for key=$key (total: ${requestCounts[key]})")
        
        return coroutineScope {
            val deferred = async {
                try {
                    block()
                } finally {
                    // Clean up after completion
                    inFlightRequests.remove(key)
                }
            }
            
            // Store the deferred for potential deduplication
            inFlightRequests[key] = deferred
            
            deferred.await()
        }
    }
    
    /**
     * Execute a suspending block with deduplication and Result wrapper.
     * 
     * Same as dedupe() but wraps the result in Result<T> for error handling.
     * 
     * @param key Unique identifier for this request
     * @param block The suspending block to execute
     * @return Result<T> containing success or failure
     */
    suspend fun <T> dedupeResult(key: String, block: suspend () -> T): Result<T> {
        return try {
            Result.success(dedupe(key, block))
        } catch (e: Exception) {
            Timber.w(e, "🔄 DEDUPE: Request failed for key=$key")
            Result.failure(e)
        }
    }
    
    /**
     * Cancel any in-flight request for the given key.
     * 
     * Use this when you need to force a fresh request
     * (e.g., after a mutation that invalidates cached data).
     * 
     * @param key The request key to cancel
     */
    fun cancel(key: String) {
        val request = inFlightRequests.remove(key)
        if (request != null && request.isActive) {
            request.cancel()
            Timber.d("🔄 DEDUPE: Cancelled request for key=$key")
        }
    }
    
    /**
     * Cancel all in-flight requests.
     * 
     * Use this during cleanup (e.g., logout, app termination).
     */
    fun cancelAll() {
        val count = inFlightRequests.size
        inFlightRequests.forEach { (key, deferred) ->
            if (deferred.isActive) {
                deferred.cancel()
            }
        }
        inFlightRequests.clear()
        Timber.d("🔄 DEDUPE: Cancelled all $count in-flight requests")
    }
    
    /**
     * Check if a request is currently in flight for the given key.
     * 
     * @param key The request key to check
     * @return true if a request is in flight
     */
    fun isInFlight(key: String): Boolean {
        val request = inFlightRequests[key]
        return request != null && request.isActive
    }
    
    /**
     * Get statistics about deduplication effectiveness.
     * 
     * @return Map of key to (totalRequests, dedupeHits)
     */
    fun getStats(): Map<String, Pair<Int, Int>> {
        return requestCounts.keys.associateWith { key ->
            Pair(requestCounts[key] ?: 0, dedupeHits[key] ?: 0)
        }
    }
    
    /**
     * Clear statistics (for testing or monitoring reset).
     */
    fun clearStats() {
        requestCounts.clear()
        dedupeHits.clear()
    }
}
