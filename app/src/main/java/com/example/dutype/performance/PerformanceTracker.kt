package com.example.dutype.performance

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight performance facade.
 *
 * Firebase Performance was removed from the release binary to keep the base dex
 * update chunk below Play's ~7 MB threshold. This keeps the existing API stable
 * for repositories/ViewModels while relying on local timing logs.
 */
@Singleton
class PerformanceTracker @Inject constructor() {
    internal val anrWarningThresholdMs = 100L

    fun trackApiCall(
        endpoint: String,
        durationMs: Long,
        success: Boolean,
        responseSize: Long? = null
    ) {
        Timber.d(
            "PERF api=%s durationMs=%d success=%s responseSize=%s",
            endpoint,
            durationMs,
            success,
            responseSize?.toString() ?: "n/a"
        )
    }

    suspend fun <T> measureApiCall(
        endpoint: String,
        block: suspend () -> Result<T>
    ): Result<T> {
        val startTime = System.currentTimeMillis()
        val result = block()
        trackApiCall(endpoint, System.currentTimeMillis() - startTime, result.isSuccess)
        return result
    }

    fun <T> measureApiCallSync(
        endpoint: String,
        block: () -> Result<T>
    ): Result<T> {
        val startTime = System.currentTimeMillis()
        val result = block()
        trackApiCall(endpoint, System.currentTimeMillis() - startTime, result.isSuccess)
        return result
    }

    fun trackScreenLoad(
        screenName: String,
        loadTimeMs: Long,
        isFromCache: Boolean = false
    ) {
        Timber.d("PERF screen=%s loadTimeMs=%d cache=%s", screenName, loadTimeMs, isFromCache)
    }

    fun startScreenTrace(screenName: String) {
        Timber.d("PERF screen trace start=%s", screenName)
    }

    fun stopScreenTrace(screenName: String, isFromCache: Boolean = false) {
        Timber.d("PERF screen trace stop=%s cache=%s", screenName, isFromCache)
    }

    fun trackDatabaseOperation(
        operation: String,
        durationMs: Long,
        rowCount: Int = 0
    ) {
        Timber.d("PERF db=%s durationMs=%d rows=%d", operation, durationMs, rowCount)
    }

    suspend fun <T> measureDbOperation(
        operation: String,
        block: suspend () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val rowCount = when (result) {
            is List<*> -> result.size
            is Int -> result
            else -> 0
        }
        trackDatabaseOperation(operation, System.currentTimeMillis() - startTime, rowCount)
        return result
    }

    fun trackCacheAccess(
        cacheType: String,
        isHit: Boolean,
        lookupTimeMs: Long = 0
    ) {
        Timber.d("PERF cache=%s hit=%s lookupMs=%d", cacheType, isHit, lookupTimeMs)
    }

    fun trackImageLoad(
        imageType: String,
        loadTimeMs: Long,
        isFromDiskCache: Boolean = false,
        isFromMemoryCache: Boolean = false
    ) {
        Timber.d(
            "PERF image=%s loadTimeMs=%d disk=%s memory=%s",
            imageType,
            loadTimeMs,
            isFromDiskCache,
            isFromMemoryCache
        )
    }

    fun trackBackgroundSync(
        syncType: String,
        durationMs: Long,
        itemsSynced: Int,
        success: Boolean
    ) {
        Timber.d(
            "PERF sync=%s durationMs=%d items=%d success=%s",
            syncType,
            durationMs,
            itemsSynced,
            success
        )
    }

    fun startTrace(traceName: String): Any? {
        Timber.d("PERF trace start=%s", traceName)
        return null
    }

    fun stopTrace(traceName: String) {
        Timber.d("PERF trace stop=%s", traceName)
    }

    fun addMetricToTrace(traceName: String, metricName: String, value: Long) {
        Timber.d("PERF trace=%s metric=%s value=%d", traceName, metricName, value)
    }

    fun addAttributeToTrace(traceName: String, attributeName: String, value: String) {
        Timber.d("PERF trace=%s attr=%s value=%s", traceName, attributeName, value)
    }

    inline fun <T> measure(block: () -> T): Pair<T, Long> {
        val startTime = System.currentTimeMillis()
        val result = block()
        return result to (System.currentTimeMillis() - startTime)
    }

    fun <T> trackSlowOperation(
        operationName: String,
        warningThresholdMs: Long = anrWarningThresholdMs,
        block: () -> T
    ): T {
        val isMainThread = MainThreadChecker.isMainThread()
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime

        if (isMainThread && duration > warningThresholdMs) {
            Timber.w(
                "SLOW OPERATION: %s took %dms on main thread (threshold=%dms)",
                operationName,
                duration,
                warningThresholdMs
            )
        }

        return result
    }

    fun trackOperation(operationName: String) {
        Timber.d("PERF operation=%s", operationName)
    }

    fun setPerformanceCollectionEnabled(enabled: Boolean) {
        Timber.d("PERF collection requested=%s (Firebase Performance removed)", enabled)
    }
}
