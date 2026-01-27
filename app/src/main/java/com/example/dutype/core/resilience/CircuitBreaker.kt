package com.example.dutype.core.resilience

import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Circuit Breaker Pattern Implementation
 * 
 * Prevents cascading failures by:
 * - Opening circuit after threshold failures
 * - Half-opening after timeout to test recovery
 * - Closing circuit when service recovers
 * 
 * States:
 * - CLOSED: Normal operation
 * - OPEN: Blocking requests (service is down)
 * - HALF_OPEN: Testing if service recovered
 * 
 * @author DutyPe Engineering Team
 * @since 3.0.0
 */
class CircuitBreaker(
    private val name: String,
    private val failureThreshold: Int = 5,
    private val successThreshold: Int = 2,
    private val timeoutMs: Long = 60000 // 1 minute
) {
    
    private var state: CircuitState = CircuitState.CLOSED
    private val failureCount = AtomicInteger(0)
    private val successCount = AtomicInteger(0)
    private val lastFailureTime = AtomicLong(0)
    
    /**
     * Check if operation can be executed
     */
    fun canExecute(): Boolean {
        return when (state) {
            CircuitState.CLOSED -> true
            CircuitState.OPEN -> {
                // Check if timeout elapsed
                if (System.currentTimeMillis() - lastFailureTime.get() >= timeoutMs) {
                    Timber.i("🔄 Circuit breaker $name: OPEN -> HALF_OPEN (timeout elapsed)")
                    state = CircuitState.HALF_OPEN
                    successCount.set(0)
                    true
                } else {
                    false
                }
            }
            CircuitState.HALF_OPEN -> true
        }
    }
    
    /**
     * Record successful operation
     */
    fun recordSuccess() {
        when (state) {
            CircuitState.CLOSED -> {
                // Reset failure count on success
                failureCount.set(0)
            }
            CircuitState.HALF_OPEN -> {
                val count = successCount.incrementAndGet()
                if (count >= successThreshold) {
                    Timber.i("✅ Circuit breaker $name: HALF_OPEN -> CLOSED (service recovered)")
                    state = CircuitState.CLOSED
                    failureCount.set(0)
                    successCount.set(0)
                }
            }
            CircuitState.OPEN -> {
                // Should not happen
            }
        }
    }
    
    /**
     * Record failed operation
     */
    fun recordFailure() {
        lastFailureTime.set(System.currentTimeMillis())
        
        when (state) {
            CircuitState.CLOSED -> {
                val count = failureCount.incrementAndGet()
                if (count >= failureThreshold) {
                    Timber.w("⚠️ Circuit breaker $name: CLOSED -> OPEN (threshold reached: $count failures)")
                    state = CircuitState.OPEN
                }
            }
            CircuitState.HALF_OPEN -> {
                Timber.w("⚠️ Circuit breaker $name: HALF_OPEN -> OPEN (test failed)")
                state = CircuitState.OPEN
                successCount.set(0)
            }
            CircuitState.OPEN -> {
                // Already open
            }
        }
    }
    
    /**
     * Get current state
     */
    fun getState(): CircuitState = state
    
    /**
     * Reset circuit breaker
     */
    fun reset() {
        state = CircuitState.CLOSED
        failureCount.set(0)
        successCount.set(0)
        lastFailureTime.set(0)
        Timber.i("🔄 Circuit breaker $name: RESET")
    }
}

/**
 * Circuit breaker states
 */
enum class CircuitState {
    CLOSED,    // Normal operation
    OPEN,      // Blocking requests
    HALF_OPEN  // Testing recovery
}

/**
 * Circuit Breaker Registry
 * Manages multiple circuit breakers by key
 */
@Singleton
class CircuitBreakerRegistry @Inject constructor() {
    
    private val breakers = mutableMapOf<String, CircuitBreaker>()
    
    /**
     * Get or create circuit breaker
     */
    fun get(key: String): CircuitBreaker {
        return breakers.getOrPut(key) {
            CircuitBreaker(
                name = key,
                failureThreshold = 5,
                successThreshold = 2,
                timeoutMs = 60000
            )
        }
    }
    
    /**
     * Reset all circuit breakers
     */
    fun resetAll() {
        breakers.values.forEach { it.reset() }
    }
    
    /**
     * Get all circuit breaker states
     */
    fun getAllStates(): Map<String, CircuitState> {
        return breakers.mapValues { it.value.getState() }
    }
}
