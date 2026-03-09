package com.example.dutype.performance

import android.content.Context
import android.os.Looper
import com.dutype.app.BuildConfig
import timber.log.Timber

/**
 * MainThreadChecker - Utility for detecting and preventing main thread violations
 * 
 * Provides runtime checks to ensure operations run on correct threads
 * 
 * Usage:
 * ```kotlin
 * // Ensure running on main thread
 * MainThreadChecker.assertMainThread("UI update")
 * 
 * // Ensure NOT running on main thread (production-safe)
 * MainThreadChecker.assertBackgroundThread("Database query")
 * 
 * // Check without throwing
 * if (MainThreadChecker.isMainThread()) {
 *     // Handle main thread case
 * }
 * ```
 * 
 * Production Behavior:
 * - DEBUG builds: Throws exception to catch bugs early
 * - RELEASE builds: Logs warning to Crashlytics, continues execution
 * 
 * @author DutyPe Engineering Team
 * @since 2.3.0
 */
object MainThreadChecker {
    
    // ANRHandler instance (injected via init)
    private var anrHandler: ANRHandler? = null
    private var appContext: Context? = null
    
    /**
     * Initialize with ANRHandler for production-safe error handling
     * Call this from Application.onCreate()
     */
    fun init(context: Context, handler: ANRHandler) {
        appContext = context.applicationContext
        anrHandler = handler
    }
    
    /**
     * Check if currently on main thread
     */
    fun isMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }
    
    /**
     * Assert that code is running on main thread
     * Throws exception if not on main thread
     * 
     * @param operation Description of operation for error message
     */
    fun assertMainThread(operation: String = "operation") {
        if (!isMainThread()) {
            val error = IllegalStateException(
                "❌ THREAD VIOLATION: $operation must run on main thread, but running on ${Thread.currentThread().name}"
            )
            Timber.e(error)
            throw error
        }
    }
    
    /**
     * Assert that code is NOT running on main thread
     * 
     * Production-Safe Behavior:
     * - DEBUG builds: Throws exception to catch bugs early
     * - RELEASE builds: Logs to Crashlytics, continues execution
     * 
     * @param operation Description of operation for error message
     */
    fun assertBackgroundThread(operation: String = "operation") {
        if (isMainThread()) {
            val threadName = getCurrentThreadName()
            val stackTrace = Exception().stackTraceToString()
            
            if (BuildConfig.DEBUG) {
                // DEBUG: Throw exception to catch bugs early
                val error = IllegalStateException(
                    "❌ THREAD VIOLATION: $operation must NOT run on main thread (potential ANR)"
                )
                Timber.e(error)
                
                // Show developer dialog if ANRHandler is available
                anrHandler?.handlePotentialANR(appContext, operation, threadName, stackTrace)
                
                throw error
            } else {
                // RELEASE: Log warning and continue (production-safe)
                Timber.w("⚠️ ANR RISK: $operation running on main thread")
                
                // Log to Crashlytics via ANRHandler
                anrHandler?.handlePotentialANR(appContext, operation, threadName, stackTrace)
                
                // ✅ Continue execution - don't crash in production
            }
        }
    }
    
    /**
     * Warn if running on main thread (non-fatal)
     * Logs warning but doesn't throw
     * 
     * @param operation Description of operation for warning message
     */
    fun warnIfMainThread(operation: String = "operation") {
        if (isMainThread()) {
            Timber.w("⚠️ PERFORMANCE WARNING: $operation running on main thread (potential ANR)")
            Timber.w("⚠️ Stack trace:", Exception())
        }
    }
    
    /**
     * Warn if NOT running on main thread (non-fatal)
     * Logs warning but doesn't throw
     * 
     * @param operation Description of operation for warning message
     */
    fun warnIfBackgroundThread(operation: String = "operation") {
        if (!isMainThread()) {
            Timber.w("⚠️ THREAD WARNING: $operation should run on main thread, but running on ${Thread.currentThread().name}")
        }
    }
    
    /**
     * Get current thread name
     */
    fun getCurrentThreadName(): String {
        return Thread.currentThread().name
    }
    
    /**
     * Log current thread for debugging
     */
    fun logCurrentThread(tag: String = "Thread") {
        val threadName = getCurrentThreadName()
        val isMain = isMainThread()
        Timber.d("🧵 [$tag] Running on: $threadName ${if (isMain) "(MAIN)" else "(BACKGROUND)"}")
    }
    
    /**
     * Measure time on current thread and warn if slow on main thread
     * 
     * @param operation Description of operation
     * @param warningThresholdMs Threshold in ms to trigger warning (default 16ms = 1 frame)
     * @param block Code block to measure
     */
    inline fun <T> measureAndWarn(
        operation: String,
        warningThresholdMs: Long = 16L,
        block: () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        
        if (isMainThread() && duration > warningThresholdMs) {
            Timber.w("⚠️ SLOW OPERATION: $operation took ${duration}ms on main thread (threshold: ${warningThresholdMs}ms)")
            Timber.w("⚠️ This may cause frame drops or ANR")
        }
        
        return result
    }
    
    /**
     * Execute block and ensure it completes within timeout on main thread
     * Logs warning if timeout exceeded
     * 
     * @param operation Description of operation
     * @param timeoutMs Maximum allowed time in ms
     * @param block Code block to execute
     */
    inline fun <T> executeWithTimeout(
        operation: String,
        timeoutMs: Long,
        block: () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        
        if (duration > timeoutMs) {
            Timber.e("🔴 TIMEOUT: $operation took ${duration}ms (timeout: ${timeoutMs}ms)")
            if (isMainThread()) {
                Timber.e("🔴 CRITICAL: Timeout on main thread - potential ANR!")
            }
        }
        
        return result
    }
}

/**
 * Extension function to assert main thread
 */
fun assertMainThread(operation: String = "operation") {
    MainThreadChecker.assertMainThread(operation)
}

/**
 * Extension function to assert background thread
 */
fun assertBackgroundThread(operation: String = "operation") {
    MainThreadChecker.assertBackgroundThread(operation)
}

/**
 * Extension function to check if on main thread
 */
fun isMainThread(): Boolean {
    return MainThreadChecker.isMainThread()
}
