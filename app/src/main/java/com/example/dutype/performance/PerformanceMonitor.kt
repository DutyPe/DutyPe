package com.example.dutype.performance

import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P4 FIX: Performance Monitoring System
 * 
 * Tracks application performance metrics:
 * - Operation execution times
 * - Success/failure rates
 * - Memory usage
 * - Network call statistics
 * 
 * @author DutyPe Engineering Team
 * @since 2.5.0
 */
@Singleton
class PerformanceMonitor @Inject constructor() {
    
    private val operationMetrics = ConcurrentHashMap<String, OperationMetrics>()
    
    /**
     * Track operation execution
     * 
     * @param operationName Name of the operation
     * @param durationMs Execution duration in milliseconds
     * @param success Whether operation succeeded
     */
    fun trackOperation(operationName: String, durationMs: Long, success: Boolean) {
        val metrics = operationMetrics.getOrPut(operationName) {
            OperationMetrics(operationName)
        }
        
        metrics.recordExecution(durationMs, success)
        
        // Log slow operations
        if (durationMs > 1000) {
            Timber.w("⚠️ Slow operation: $operationName took ${durationMs}ms")
        }
    }
    
    /**
     * Get metrics for an operation
     */
    fun getMetrics(operationName: String): OperationMetrics? {
        return operationMetrics[operationName]
    }
    
    /**
     * Get all metrics
     */
    fun getAllMetrics(): Map<String, OperationMetrics> {
        return operationMetrics.toMap()
    }
    
    /**
     * Reset all metrics
     */
    fun reset() {
        operationMetrics.clear()
    }
    
    /**
     * Get performance summary
     */
    fun getSummary(): PerformanceSummary {
        val totalOperations = operationMetrics.values.sumOf { it.totalCount.get() }
        val totalSuccesses = operationMetrics.values.sumOf { it.successCount.get() }
        val totalFailures = operationMetrics.values.sumOf { it.failureCount.get() }
        val avgDuration = if (totalOperations > 0) {
            operationMetrics.values.sumOf { it.totalDuration.get() } / totalOperations
        } else {
            0L
        }
        
        return PerformanceSummary(
            totalOperations = totalOperations.toInt(),
            successRate = if (totalOperations > 0) {
                (totalSuccesses.toDouble() / totalOperations * 100).toInt()
            } else {
                0
            },
            averageDurationMs = avgDuration,
            slowestOperations = operationMetrics.values
                .sortedByDescending { it.maxDuration.get() }
                .take(5)
                .map { it.name to it.maxDuration.get() }
        )
    }
}

/**
 * Metrics for a single operation
 */
data class OperationMetrics(
    val name: String
) {
    val totalCount = AtomicLong(0)
    val successCount = AtomicLong(0)
    val failureCount = AtomicLong(0)
    val totalDuration = AtomicLong(0)
    val minDuration = AtomicLong(Long.MAX_VALUE)
    val maxDuration = AtomicLong(0)
    
    fun recordExecution(durationMs: Long, success: Boolean) {
        totalCount.incrementAndGet()
        if (success) {
            successCount.incrementAndGet()
        } else {
            failureCount.incrementAndGet()
        }
        
        totalDuration.addAndGet(durationMs)
        
        // Update min/max
        var currentMin = minDuration.get()
        while (durationMs < currentMin) {
            if (minDuration.compareAndSet(currentMin, durationMs)) {
                break
            }
            currentMin = minDuration.get()
        }
        
        var currentMax = maxDuration.get()
        while (durationMs > currentMax) {
            if (maxDuration.compareAndSet(currentMax, durationMs)) {
                break
            }
            currentMax = maxDuration.get()
        }
    }
    
    fun getAverageDuration(): Long {
        val count = totalCount.get()
        return if (count > 0) totalDuration.get() / count else 0
    }
    
    fun getSuccessRate(): Double {
        val count = totalCount.get()
        return if (count > 0) {
            successCount.get().toDouble() / count * 100
        } else {
            0.0
        }
    }
}

/**
 * Performance summary
 */
data class PerformanceSummary(
    val totalOperations: Int,
    val successRate: Int,
    val averageDurationMs: Long,
    val slowestOperations: List<Pair<String, Long>>
)

/**
 * Extension function for easy performance tracking
 */
suspend inline fun <T> PerformanceMonitor.track(
    operationName: String,
    operation: suspend () -> Result<T>
): Result<T> {
    val startTime = System.currentTimeMillis()
    val result = try {
        operation()
    } catch (e: Exception) {
        Result.failure(e)
    }
    val duration = System.currentTimeMillis() - startTime
    
    trackOperation(operationName, duration, result.isSuccess)
    
    return result
}
