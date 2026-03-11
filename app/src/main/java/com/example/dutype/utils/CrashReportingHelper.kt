package com.example.dutype.utils

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import timber.log.Timber

/**
 * Global crash handler initialization.
 * Must run pre-DI in Application.onCreate().
 * All other crash reporting methods consolidated into ErrorHandler.
 */
object CrashReportingHelper {
    
    /**
     * Initialize global exception handler.
     * Call this in Application.onCreate()
     */
    fun initialize(application: Application) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            logCrashContext(thread, exception)
            Firebase.crashlytics.recordException(exception)
            defaultHandler?.uncaughtException(thread, exception)
        }
        
        Timber.d("✅ Global crash reporting initialized")
    }
    
    private fun logCrashContext(thread: Thread, exception: Throwable) {
        try {
            Firebase.crashlytics.setCustomKey("crash_thread", thread.name ?: "unknown")
            Firebase.crashlytics.setCustomKey("exception_class", exception::class.simpleName ?: "unknown")
            exception.message?.let {
                Firebase.crashlytics.setCustomKey("exception_message", it.take(500))
            }
            Firebase.crashlytics.setCustomKey("stack_trace_depth", exception.stackTrace.size)
            Timber.e(exception, "🔴 CRASH DETECTED")
        } catch (e: Exception) {
            Timber.e(e, "Failed to log crash context")
        }
    }
}
