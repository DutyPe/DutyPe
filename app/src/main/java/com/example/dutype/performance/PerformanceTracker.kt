package com.example.dutype.performance

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

/**
 * PerformanceTracker - Firebase Performance Monitoring Integration
 * 
 * Tracks key performance metrics across the app:
 * - API call latency and success rates
 * - Screen load times
 * - Database operations
 * - Image loading performance
 * - Background sync operations
 * 
 * Usage:
 * ```kotlin
 * // Track API call
 * performanceTracker.trackApiCall("get_jobs", durationMs, success = true)
 * 
 * // Track with inline measurement
 * val result = performanceTracker.measureApiCall("get_jobs") {
 *     repository.getJobs()
 * }
 * 
 * // Track screen load
 * performanceTracker.trackScreenLoad("WorkerHomeScreen", loadTimeMs)
 * ```
 * 
 * @author DutyPe Engineering Team
 * @since 2.2.0
 */
@Singleton
class PerformanceTracker @Inject constructor() {
    
    private val firebasePerformance: FirebasePerformance by lazy {
        FirebasePerformance.getInstance()
    }
    
    // Active traces for manual start/stop
    private val activeTraces = mutableMapOf<String, Trace>()
    
    // ==========================================
    // API CALL TRACKING
    // ==========================================
    
    /**
     * Track an API call with duration and success status
     * 
     * @param endpoint API endpoint name (e.g., "get_jobs", "create_application")
     * @param durationMs Duration in milliseconds
     * @param success Whether the call succeeded
     * @param responseSize Optional response size in bytes
     */
    fun trackApiCall(
        endpoint: String,
        durationMs: Long,
        success: Boolean,
        responseSize: Long? = null
    ) {
        try {
            val trace = firebasePerformance.newTrace("api_$endpoint")
            trace.start()
            
            // Add metrics
            trace.putMetric("duration_ms", durationMs)
            trace.putMetric("success", if (success) 1 else 0)
            responseSize?.let { trace.putMetric("response_size_bytes", it) }
            
            // Add attributes
            trace.putAttribute("success", success.toString())
            trace.putAttribute("endpoint", endpoint)
            
            trace.stop()
            
            Timber.d("📊 PERF: API $endpoint - ${durationMs}ms, success=$success")
        } catch (e: Exception) {
            Timber.w(e, "📊 PERF: Failed to track API call")
        }
    }
    
    /**
     * Measure and track an API call inline
     */
    suspend fun <T> measureApiCall(
        endpoint: String,
        block: suspend () -> Result<T>
    ): Result<T> {
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        
        trackApiCall(endpoint, duration, result.isSuccess)
        return result
    }
    
    /**
     * Measure and track a synchronous API call
     */
    fun <T> measureApiCallSync(
        endpoint: String,
        block: () -> Result<T>
    ): Result<T> {
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        
        trackApiCall(endpoint, duration, result.isSuccess)
        return result
    }
    
    // ==========================================
    // SCREEN LOAD TRACKING
    // ==========================================
    
    /**
     * Track screen load time
     * 
     * @param screenName Name of the screen
     * @param loadTimeMs Time to load in milliseconds
     * @param isFromCache Whether data was loaded from cache
     */
    fun trackScreenLoad(
        screenName: String,
        loadTimeMs: Long,
        isFromCache: Boolean = false
    ) {
        try {
            val trace = firebasePerformance.newTrace("screen_$screenName")
            trace.start()
            
            trace.putMetric("load_time_ms", loadTimeMs)
            trace.putMetric("from_cache", if (isFromCache) 1 else 0)
            trace.putAttribute("screen", screenName)
            trace.putAttribute("cache_hit", isFromCache.toString())
            
            trace.stop()
            
            Timber.d("📊 PERF: Screen $screenName - ${loadTimeMs}ms, cache=$isFromCache")
        } catch (e: Exception) {
            Timber.w(e, "📊 PERF: Failed to track screen load")
        }
    }
    
    /**
     * Start tracking screen load (call when screen composition starts)
     */
    fun startScreenTrace(screenName: String) {
        try {
            val trace = firebasePerformance.newTrace("screen_$screenName")
            trace.start()
            activeTraces[screenName] = trace
            Timber.d("📊 PERF: Started trace for $screenName")
        } catch (e: Exception) {
            Timber.w(e, "📊 PERF: Failed to start screen trace")
        }
    }
    
    /**
     * Stop tracking screen load (call when data is loaded)
     */
    fun stopScreenTrace(screenName: String, isFromCache: Boolean = false) {
        try {
            activeTraces.remove(screenName)?.let { trace ->
                trace.putAttribute("cache_hit", isFromCache.toString())
                trace.putMetric("from_cache", if (isFromCache) 1 else 0)
                trace.stop()
                Timber.d("📊 PERF: Stopped trace for $screenName, cache=$isFromCache")
            }
        } catch (e: Exception) {
            Timber.w(e, "📊 PERF: Failed to stop screen trace")
        }
    }
    
    // ==========================================
    // DATABASE OPERATION TRACKING
    // ==========================================
    
    /**
     * Track database operation
     * 
     * @param operation Operation name (e.g., "insert_jobs", "query_applications")
     * @param durationMs Duration in milliseconds
     * @param rowCount Number of rows affected/returned
     */
    fun trackDatabaseOperation(
        operation: String,
        durationMs: Long,
        rowCount: Int = 0
    ) {
        try {
            val trace = firebasePerformance.newTrace("db_$operation")
            trace.start()
            
            trace.putMetric("duration_ms", durationMs)
            trace.putMetric("row_count", rowCount.toLong())
            trace.putAttribute("operation", operation)
            
            trace.stop()
            
            Timber.d("📊 PERF: DB $operation - ${durationMs}ms, rows=$rowCount")
        } catch (e: Exception) {
            Timber.w(e, "📊 PERF: Failed to track DB operation")
        }
    }
    
    /**
     * Measure and track a database operation inline
     */
    suspend fun <T> measureDbOperation(
        operation: String,
        block: suspend () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        
        val rowCount = when (result) {
            is List<*> -> result.size
            is Int -> result
            else -> 0
        }
        
        trackDatabaseOperation(operation, duration, rowCount)
        return result
    }
    
    // ==========================================
    // CACHE PERFORMANCE TRACKING
    // ==========================================
    
    /**
     * Track cache hit/miss
     * 
     * @param cacheType Type of cache (e.g., "jobs", "applications", "user_summary")
     * @param isHit Whether it was a cache hit
     * @param lookupTimeMs Time to lookup in cache
     */
    fun trackCacheAccess(
        cacheType: String,
        isHit: Boolean,
        lookupTimeMs: Long = 0
    ) {
        try {
            val trace = firebasePerformance.newTrace("cache_$cacheType")
            trace.start()
            
            trace.putMetric("hit", if (isHit) 1 else 0)
            trace.putMetric("lookup_time_ms", lookupTimeMs)
            trace.putAttribute("cache_type", cacheType)
            trace.putAttribute("hit", isHit.toString())
            
            trace.stop()
            
            Timber.d("📊 PERF: Cache $cacheType - hit=$isHit, ${lookupTimeMs}ms")
        } catch (e: Exception) {
            Timber.w(e, "📊 PERF: Failed to track cache access")
        }
    }
    
    // ==========================================
    // IMAGE LOADING TRACKING
    // ==========================================
    
    /**
     * Track image load performance
     * 
     * @param imageType Type of image (e.g., "job_image", "profile_image")
     * @param loadTimeMs Time to load in milliseconds
     * @param isFromDiskCache Whether loaded from disk cache
     * @param isFromMemoryCache Whether loaded from memory cache
     */
    fun trackImageLoad(
        imageType: String,
        loadTimeMs: Long,
        isFromDiskCache: Boolean = false,
        isFromMemoryCache: Boolean = false
    ) {
        try {
            val trace = firebasePerformance.newTrace("image_$imageType")
            trace.start()
            
            trace.putMetric("load_time_ms", loadTimeMs)
            trace.putMetric("disk_cache", if (isFromDiskCache) 1 else 0)
            trace.putMetric("memory_cache", if (isFromMemoryCache) 1 else 0)
            
            val cacheSource = when {
                isFromMemoryCache -> "memory"
                isFromDiskCache -> "disk"
                else -> "network"
            }
            trace.putAttribute("cache_source", cacheSource)
            
            trace.stop()
            
            Timber.d("📊 PERF: Image $imageType - ${loadTimeMs}ms, source=$cacheSource")
        } catch (e: Exception) {
            Timber.w(e, "📊 PERF: Failed to track image load")
        }
    }
    
    // ==========================================
    // BACKGROUND SYNC TRACKING
    // ==========================================
    
    /**
     * Track background sync operation
     * 
     * @param syncType Type of sync (e.g., "jobs", "applications", "notifications")
     * @param durationMs Duration in milliseconds
     * @param itemsSynced Number of items synced
     * @param success Whether sync succeeded
     */
    fun trackBackgroundSync(
        syncType: String,
        durationMs: Long,
        itemsSynced: Int,
        success: Boolean
    ) {
        try {
            val trace = firebasePerformance.newTrace("sync_$syncType")
            trace.start()
            
            trace.putMetric("duration_ms", durationMs)
            trace.putMetric("items_synced", itemsSynced.toLong())
            trace.putMetric("success", if (success) 1 else 0)
            trace.putAttribute("sync_type", syncType)
            trace.putAttribute("success", success.toString())
            
            trace.stop()
            
            Timber.d("📊 PERF: Sync $syncType - ${durationMs}ms, items=$itemsSynced, success=$success")
        } catch (e: Exception) {
            Timber.w(e, "📊 PERF: Failed to track background sync")
        }
    }
    
    // ==========================================
    // CUSTOM TRACE HELPERS
    // ==========================================
    
    /**
     * Start a custom trace
     */
    fun startTrace(traceName: String): Trace? {
        return try {
            val trace = firebasePerformance.newTrace(traceName)
            trace.start()
            activeTraces[traceName] = trace
            trace
        } catch (e: Exception) {
            Timber.w(e, "📊 PERF: Failed to start trace $traceName")
            null
        }
    }
    
    /**
     * Stop a custom trace
     */
    fun stopTrace(traceName: String) {
        try {
            activeTraces.remove(traceName)?.stop()
        } catch (e: Exception) {
            Timber.w(e, "📊 PERF: Failed to stop trace $traceName")
        }
    }
    
    /**
     * Add metric to active trace
     */
    fun addMetricToTrace(traceName: String, metricName: String, value: Long) {
        try {
            activeTraces[traceName]?.putMetric(metricName, value)
        } catch (e: Exception) {
            Timber.w(e, "📊 PERF: Failed to add metric to trace")
        }
    }
    
    /**
     * Add attribute to active trace
     */
    fun addAttributeToTrace(traceName: String, attributeName: String, value: String) {
        try {
            activeTraces[traceName]?.putAttribute(attributeName, value)
        } catch (e: Exception) {
            Timber.w(e, "📊 PERF: Failed to add attribute to trace")
        }
    }
    
    // ==========================================
    // UTILITY
    // ==========================================
    
    /**
     * Measure execution time of a block
     */
    inline fun <T> measure(block: () -> T): Pair<T, Long> {
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        return result to duration
    }
    
    /**
     * Enable/disable performance collection
     */
    fun setPerformanceCollectionEnabled(enabled: Boolean) {
        firebasePerformance.isPerformanceCollectionEnabled = enabled
        Timber.d("📊 PERF: Collection ${if (enabled) "enabled" else "disabled"}")
    }
}
