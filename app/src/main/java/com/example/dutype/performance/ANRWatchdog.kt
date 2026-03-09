package com.example.dutype.performance

import android.os.Handler
import android.os.Looper
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ANRWatchdog - Application Not Responding Detection System
 * 
 * Monitors main thread for blocking operations and potential ANRs
 * 
 * Features:
 * - Real-time main thread monitoring
 * - Configurable thresholds (5s warning, 10s critical)
 * - Stack trace capture for debugging
 * - Firebase Crashlytics integration
 * - Development warnings
 * - Production safeguards
 * 
 * How it works:
 * - Posts a task to main thread every 2 seconds
 * - If task doesn't execute within threshold, ANR is detected
 * - Captures stack traces and reports to Crashlytics
 * 
 * @author DutyPe Engineering Team
 * @since 2.3.0
 */
@Singleton
class ANRWatchdog @Inject constructor(
    private val crashlytics: FirebaseCrashlytics
) {
    
    private val isRunning = AtomicBoolean(false)
    private val lastResponseTime = AtomicLong(System.currentTimeMillis())
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var watchdogThread: Thread? = null
    private var listener: ANRListener? = null
    
    // Configuration
    private var warningThresholdMs = 5000L  // 5 seconds - warning
    private var criticalThresholdMs = 10000L // 10 seconds - critical ANR
    private var checkIntervalMs = 2000L      // Check every 2 seconds
    
    // Statistics
    private var warningCount = 0
    private var criticalCount = 0
    private var lastANRTime = 0L
    
    /**
     * Start ANR monitoring
     */
    fun start(listener: ANRListener? = null) {
        if (isRunning.getAndSet(true)) {
            Timber.w("🔴 ANR Watchdog already running")
            return
        }
        
        this.listener = listener
        lastResponseTime.set(System.currentTimeMillis())
        
        watchdogThread = Thread({
            Timber.i("🔴 ANR Watchdog started (warning: ${warningThresholdMs}ms, critical: ${criticalThresholdMs}ms)")
            
            while (isRunning.get()) {
                try {
                    // Post task to main thread
                    mainHandler.post {
                        lastResponseTime.set(System.currentTimeMillis())
                    }
                    
                    // Wait for check interval
                    Thread.sleep(checkIntervalMs)
                    
                    // Check if main thread responded
                    val timeSinceResponse = System.currentTimeMillis() - lastResponseTime.get()
                    
                    when {
                        timeSinceResponse >= criticalThresholdMs -> {
                            handleCriticalANR(timeSinceResponse)
                        }
                        timeSinceResponse >= warningThresholdMs -> {
                            handleWarning(timeSinceResponse)
                        }
                    }
                    
                } catch (e: InterruptedException) {
                    Timber.d("🔴 ANR Watchdog interrupted")
                    break
                } catch (e: Exception) {
                    Timber.e(e, "🔴 ANR Watchdog error")
                }
            }
            
            Timber.i("🔴 ANR Watchdog stopped (warnings: $warningCount, critical: $criticalCount)")
        }, "ANR-Watchdog")
        
        watchdogThread?.isDaemon = true
        watchdogThread?.start()
    }
    
    /**
     * Stop ANR monitoring
     */
    fun stop() {
        if (!isRunning.getAndSet(false)) {
            return
        }
        
        watchdogThread?.interrupt()
        watchdogThread = null
        
        Timber.i("🔴 ANR Watchdog stopped")
    }
    
    /**
     * Handle warning threshold breach
     */
    private fun handleWarning(blockTimeMs: Long) {
        warningCount++
        
        val stackTrace = captureMainThreadStackTrace()
        
        Timber.w("⚠️ ANR WARNING: Main thread blocked for ${blockTimeMs}ms")
        Timber.w("⚠️ Stack trace:\n${formatStackTrace(stackTrace)}")
        
        // Report to listener
        listener?.onANRWarning(blockTimeMs, stackTrace)
        
        // Log to Crashlytics (non-fatal)
        crashlytics.log("ANR_WARNING: Main thread blocked for ${blockTimeMs}ms")
        crashlytics.setCustomKey("anr_warning_count", warningCount)
        crashlytics.setCustomKey("last_warning_duration_ms", blockTimeMs)
    }
    
    /**
     * Handle critical ANR detection
     */
    private fun handleCriticalANR(blockTimeMs: Long) {
        criticalCount++
        lastANRTime = System.currentTimeMillis()
        
        val stackTrace = captureMainThreadStackTrace()
        val allThreads = captureAllThreadStackTraces()
        
        Timber.e("🔴 CRITICAL ANR: Main thread blocked for ${blockTimeMs}ms")
        Timber.e("🔴 Main thread stack:\n${formatStackTrace(stackTrace)}")
        
        // Report to listener
        listener?.onANRDetected(blockTimeMs, stackTrace, allThreads)
        
        // Report to Crashlytics
        val exception = ANRException(
            "ANR detected: Main thread blocked for ${blockTimeMs}ms",
            stackTrace
        )
        
        crashlytics.recordException(exception)
        crashlytics.setCustomKey("anr_critical_count", criticalCount)
        crashlytics.setCustomKey("anr_duration_ms", blockTimeMs)
        crashlytics.setCustomKey("anr_timestamp", lastANRTime)
        
        // Log all thread states
        crashlytics.log("=== ALL THREADS AT ANR ===")
        allThreads.forEach { (threadName, trace) ->
            crashlytics.log("Thread: $threadName")
            crashlytics.log(formatStackTrace(trace))
        }
    }
    
    /**
     * Capture main thread stack trace
     */
    private fun captureMainThreadStackTrace(): Array<StackTraceElement> {
        return Looper.getMainLooper().thread.stackTrace ?: emptyArray()
    }
    
    /**
     * Capture all thread stack traces
     */
    private fun captureAllThreadStackTraces(): Map<String, Array<StackTraceElement>> {
        return Thread.getAllStackTraces().mapKeys { it.key.name }
    }
    
    /**
     * Format stack trace for logging
     */
    private fun formatStackTrace(stackTrace: Array<StackTraceElement>?): String {
        return stackTrace?.joinToString("\n") { "  at $it" } ?: "No stack trace available"
    }
    
    /**
     * Configure thresholds
     */
    fun configure(
        warningThresholdMs: Long = 5000L,
        criticalThresholdMs: Long = 10000L,
        checkIntervalMs: Long = 2000L
    ) {
        this.warningThresholdMs = warningThresholdMs
        this.criticalThresholdMs = criticalThresholdMs
        this.checkIntervalMs = checkIntervalMs
        
        Timber.d("🔴 ANR Watchdog configured: warning=${warningThresholdMs}ms, critical=${criticalThresholdMs}ms")
    }
    
    /**
     * Get statistics
     */
    fun getStatistics(): ANRStatistics {
        return ANRStatistics(
            warningCount = warningCount,
            criticalCount = criticalCount,
            lastANRTime = lastANRTime,
            isRunning = isRunning.get()
        )
    }
    
    /**
     * Reset statistics
     */
    fun resetStatistics() {
        warningCount = 0
        criticalCount = 0
        lastANRTime = 0L
        Timber.d("🔴 ANR Watchdog statistics reset")
    }
}

/**
 * ANR detection listener
 */
interface ANRListener {
    /**
     * Called when main thread is blocked for warning threshold
     */
    fun onANRWarning(blockTimeMs: Long, stackTrace: Array<StackTraceElement>)
    
    /**
     * Called when critical ANR is detected
     */
    fun onANRDetected(
        blockTimeMs: Long,
        mainThreadStackTrace: Array<StackTraceElement>,
        allThreadStackTraces: Map<String, Array<StackTraceElement>>
    )
}

/**
 * ANR statistics
 */
data class ANRStatistics(
    val warningCount: Int,
    val criticalCount: Int,
    val lastANRTime: Long,
    val isRunning: Boolean
)

/**
 * Custom exception for ANR reporting
 */
class ANRException(
    message: String,
    private val mainThreadStackTrace: Array<StackTraceElement>?
) : Exception(message) {
    
    override fun fillInStackTrace(): Throwable {
        stackTrace = mainThreadStackTrace ?: emptyArray()
        return this
    }
    
    override fun toString(): String {
        val trace = mainThreadStackTrace?.joinToString("\n") { "  at $it" } ?: "No stack trace available"
        return "ANRException: $message\n$trace"
    }
}
