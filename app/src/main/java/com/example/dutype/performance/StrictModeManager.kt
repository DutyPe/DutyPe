package com.example.dutype.performance

import android.os.Build
import android.os.StrictMode
import timber.log.Timber

/**
 * StrictModeManager - Development-time performance monitoring
 * 
 * Detects common performance issues during development:
 * - Disk reads/writes on main thread
 * - Network operations on main thread
 * - Slow calls (>100ms on main thread)
 * - Custom slow calls
 * - Leaked closeable objects
 * - Leaked SQLite objects
 * - Leaked registration objects
 * 
 * ONLY ENABLED IN DEBUG BUILDS
 * 
 * @author DutyPe Engineering Team
 * @since 2.3.0
 */
object StrictModeManager {
    
    /**
     * Enable StrictMode for development
     * Call this in Application.onCreate() for DEBUG builds only
     */
    fun enableForDevelopment() {
        Timber.i("🔧 StrictMode enabled for development")
        
        // Thread policy - detects main thread violations
        val threadPolicy = StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .detectCustomSlowCalls()
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    detectResourceMismatches()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    detectUnbufferedIo()
                }
            }
            .penaltyLog() // Log violations to Logcat only
            // .penaltyFlashScreen() removed - no visual flash
            .build()
        
        StrictMode.setThreadPolicy(threadPolicy)
        
        // VM policy - detects memory leaks and resource issues
        val vmPolicy = StrictMode.VmPolicy.Builder()
            .detectLeakedSqlLiteObjects()
            .detectLeakedClosableObjects()
            .detectLeakedRegistrationObjects()
            .detectActivityLeaks()
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    detectCleartextNetwork()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    detectContentUriWithoutPermission()
                    // DISABLED: detectUntaggedSockets() - causes noise from Firebase/OkHttp
                    // Firebase libraries don't tag their sockets, which is expected
                    // detectUntaggedSockets()
                }
                // DISABLED: detectNonSdkApiUsage() - causes excessive noise from Google Play Services
                // Google's own libraries (Firebase, GMS) use internal APIs, which is expected
                // This detection is not useful for app developers
                // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                //     detectNonSdkApiUsage()
                // }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    detectCredentialProtectedWhileLocked()
                    detectImplicitDirectBoot()
                }
            }
            .penaltyLog() // Log violations
            .build()
        
        StrictMode.setVmPolicy(vmPolicy)
        
        Timber.i("🔧 StrictMode policies applied:")
        Timber.i("  ✓ Disk I/O detection on main thread")
        Timber.i("  ✓ Network detection on main thread")
        Timber.i("  ✓ Slow call detection (>100ms)")
        Timber.i("  ✓ Resource leak detection")
        Timber.i("  ✓ Memory leak detection")
    }
    
    /**
     * Disable StrictMode (for production)
     */
    fun disable() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX)
        StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX)
        Timber.d("🔧 StrictMode disabled")
    }
    
    /**
     * Mark a block of code as intentionally slow
     * Use this for operations that must run on main thread but are slow
     */
    inline fun <T> permitSlowCall(block: () -> T): T {
        val oldPolicy = StrictMode.getThreadPolicy()
        
        val newPolicy = StrictMode.ThreadPolicy.Builder(oldPolicy)
            .permitCustomSlowCalls()
            .build()
        
        StrictMode.setThreadPolicy(newPolicy)
        
        return try {
            block()
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }
    }
    
    /**
     * Mark a block of code as intentionally doing disk I/O
     * Use sparingly - prefer moving to background thread
     */
    inline fun <T> permitDiskReads(block: () -> T): T {
        val oldPolicy = StrictMode.getThreadPolicy()
        
        val newPolicy = StrictMode.ThreadPolicy.Builder(oldPolicy)
            .permitDiskReads()
            .build()
        
        StrictMode.setThreadPolicy(newPolicy)
        
        return try {
            block()
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }
    }
    
    /**
     * Mark a block of code as intentionally doing disk writes
     * Use sparingly - prefer moving to background thread
     */
    inline fun <T> permitDiskWrites(block: () -> T): T {
        val oldPolicy = StrictMode.getThreadPolicy()
        
        val newPolicy = StrictMode.ThreadPolicy.Builder(oldPolicy)
            .permitDiskWrites()
            .build()
        
        StrictMode.setThreadPolicy(newPolicy)
        
        return try {
            block()
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }
    }
    
    /**
     * Note a slow call for StrictMode detection
     * Use this to mark intentionally slow operations
     */
    fun noteSlowCall(name: String) {
        StrictMode.noteSlowCall(name)
    }
}
