package com.example.dutype.performance

import android.app.AlertDialog
import android.content.Context
import com.dutype.app.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ANRHandler - Production-ready ANR detection and user communication
 * 
 * Behavior:
 * - DEBUG builds: Show developer dialog with technical details
 * - RELEASE builds: Log to Crashlytics, show user-friendly message
 * 
 * @author DutyPe Engineering Team
 * @since 2.5.0
 */
@Singleton
class ANRHandler @Inject constructor(
    private val crashlytics: FirebaseCrashlytics
) {
    
    /**
     * Handle potential ANR situation
     * 
     * @param context Android context for showing dialogs
     * @param operationName Name of the operation that might cause ANR
     * @param threadName Name of the thread where violation occurred
     * @param stackTrace Stack trace of the violation
     */
    fun handlePotentialANR(
        context: Context?,
        operationName: String,
        threadName: String,
        stackTrace: String
    ) {
        // Log to Timber (always)
        Timber.e("⚠️ POTENTIAL ANR: $operationName on $threadName")
        
        // Log to Crashlytics (production monitoring)
        crashlytics.log("ANR_RISK: $operationName on $threadName")
        crashlytics.recordException(
            ANRRiskException(operationName, threadName, stackTrace)
        )
        
        if (BuildConfig.DEBUG) {
            // DEBUG: Show developer dialog with technical details
            context?.let { showDeveloperDialog(it, operationName, threadName, stackTrace) }
        } else {
            // RELEASE: Silent logging only (don't crash, don't show technical details)
            // User won't see anything - we'll fix it based on Crashlytics reports
            Timber.w("ANR risk detected in production - logged to Crashlytics")
        }
    }
    
    /**
     * Show developer-friendly dialog (DEBUG builds only)
     */
    private fun showDeveloperDialog(
        context: Context,
        operationName: String,
        threadName: String,
        stackTrace: String
    ) {
        try {
            // Only show dialog if context is an Activity (has window token)
            if (context !is android.app.Activity) {
                Timber.w("⚠️ Cannot show ANR dialog - context is not an Activity")
                Timber.e("ANR Details:\nOperation: $operationName\nThread: $threadName\nStack:\n$stackTrace")
                return
            }
            
            // Check if activity is finishing
            if (context.isFinishing || context.isDestroyed) {
                Timber.w("⚠️ Cannot show ANR dialog - activity is finishing/destroyed")
                return
            }
            
            AlertDialog.Builder(context)
                .setTitle("⚠️ ANR Risk Detected")
                .setMessage("""
                    Operation: $operationName
                    Thread: $threadName
                    
                    This operation is running on the main thread and may cause ANR.
                    
                    Fix: Move to background thread using:
                    - Dispatchers.IO for I/O operations
                    - Dispatchers.Default for CPU work
                    
                    Stack trace logged to Logcat.
                """.trimIndent())
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .setNegativeButton("View Stack") { _, _ ->
                    Timber.e("Stack trace:\n$stackTrace")
                }
                .setCancelable(true)
                .show()
        } catch (e: Exception) {
            Timber.e(e, "Failed to show ANR dialog")
            // Always log the ANR details even if dialog fails
            Timber.e("ANR Details:\nOperation: $operationName\nThread: $threadName\nStack:\n$stackTrace")
        }
    }
    
    /**
     * Custom exception for ANR risks (logged to Crashlytics)
     */
    class ANRRiskException(
        operationName: String,
        threadName: String,
        stackTrace: String
    ) : Exception("ANR Risk: $operationName on $threadName\n$stackTrace")
}
