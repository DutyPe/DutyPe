package com.example.dutype.utils

import android.app.Application
import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import timber.log.Timber

/**
 * Helper for enhanced crash reporting to Play Console
 * Captures additional context before crashes occur
 * 
 * ✅ No performance impact - only runs when crashes happen
 * ✅ No APK size increase - uses existing Crashlytics
 * ✅ Reports to Play Console Crashes & ANRs section
 */
object CrashReportingHelper {
    
    /**
     * Initialize global exception handler and crash reporting
     * Call this in Application.onCreate()
     */
    fun initialize(application: Application) {
        // Set default uncaught exception handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            // Log exception context before crashing
            logCrashContext(thread, exception)
            
            // Let Crashlytics handle it
            Firebase.crashlytics.recordException(exception)
            
            // Call original handler
            defaultHandler?.uncaughtException(thread, exception)
        }
        
        Timber.d("✅ Global crash reporting initialized")
    }
    
    /**
     * Log additional context information for crash investigation
     * This information will appear in Play Console crash reports
     */
    private fun logCrashContext(thread: Thread, exception: Throwable) {
        try {
            // Log thread name
            Firebase.crashlytics.setCustomKey("crash_thread", thread.name ?: "unknown")
            
            // Log exception class
            Firebase.crashlytics.setCustomKey("exception_class", exception::class.simpleName ?: "unknown")
            
            // Log exception message
            exception.message?.let {
                Firebase.crashlytics.setCustomKey("exception_message", it.take(500)) // Limit to 500 chars
            }
            
            // Log stack trace depth
            Firebase.crashlytics.setCustomKey("stack_trace_depth", exception.stackTrace.size)
            
            // Record in Timber for local debugging
            Timber.e(exception, "🔴 CRASH DETECTED - Reporting to Play Console")
        } catch (e: Exception) {
            Timber.e(e, "Failed to log crash context")
        }
    }
    
    /**
     * Log custom events or state information for crash investigation
     * Use this to track app state before crashes
     * 
     * Example:
     * CrashReportingHelper.logEvent("user_logged_in", true)
     * CrashReportingHelper.logEvent("current_screen", "HomeScreen")
     */
    fun logEvent(key: String, value: Any) {
        try {
            when (value) {
                is String -> Firebase.crashlytics.setCustomKey(key, value.take(500))
                is Int -> Firebase.crashlytics.setCustomKey(key, value)
                is Long -> Firebase.crashlytics.setCustomKey(key, value)
                is Float -> Firebase.crashlytics.setCustomKey(key, value)
                is Double -> Firebase.crashlytics.setCustomKey(key, value)
                is Boolean -> Firebase.crashlytics.setCustomKey(key, value)
                else -> Firebase.crashlytics.setCustomKey(key, value.toString().take(500))
            }
            Timber.d("📊 Crash event logged: $key = $value")
        } catch (e: Exception) {
            Timber.e(e, "Failed to log crash event")
        }
    }
    
    /**
     * Log important user information for crash investigation
     * 
     * Example:
     * CrashReportingHelper.setUserInfo("userId123", "user@example.com")
     */
    fun setUserInfo(userId: String, email: String = "") {
        try {
            Firebase.crashlytics.setUserId(userId)
            if (email.isNotEmpty()) {
                Firebase.crashlytics.setCustomKey("user_email", email)
            }
            Timber.d("👤 User info set for crash reports: $userId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to set user info")
        }
    }
    
    /**
     * Log important breadcrumb information for crash investigation
     * These appear as "custom logs" in Play Console crash reports
     * 
     * Example:
     * CrashReportingHelper.logBreadcrumb("OTP verification started")
     * CrashReportingHelper.logBreadcrumb("Database query failed")
     */
    fun logBreadcrumb(message: String) {
        try {
            Timber.i("🔗 [Breadcrumb] $message")
            // Timber logs automatically get captured by Crashlytics
            Firebase.crashlytics.log(message)
        } catch (e: Exception) {
            Timber.e(e, "Failed to log breadcrumb")
        }
    }
}
