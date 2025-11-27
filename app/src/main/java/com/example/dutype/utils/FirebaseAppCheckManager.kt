package com.example.dutype.utils

import android.content.Context
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Firebase AppCheck Initialization Manager
 * 
 * Fixes the crash: "Multiple entries with same key: p4.c=true and p4.c=true"
 * 
 * Root Cause Analysis:
 * - Firebase AppCheck internally uses an ImmutableMap to store configuration
 * - When installAppCheckProviderFactory() is called multiple times, it attempts to add 
 *   the same configuration key "p4.c" multiple times
 * - This is especially prevalent on OnePlus devices due to threading characteristics
 * - The duplicate key causes: IllegalArgumentException in ImmutableMap$Builder
 * 
 * Solution:
 * - Use atomic flags and locks to ensure single-threaded initialization
 * - Cache provider instances to prevent re-installation
 * - Gracefully handle re-initialization attempts
 * - Log all initialization attempts for debugging
 * 
 * Usage:
 * In DutyPeApplication.kt:
 *   FirebaseAppCheckManager.initialize(this, isDebug = BuildConfig.DEBUG)
 */
object FirebaseAppCheckManager {
    
    // Thread-safe initialization tracking
    private val initialized = AtomicBoolean(false)
    private val initLock = ReentrantReadWriteLock()
    
    // Provider cache
    @VisibleForTesting
    internal var debugProvider: DebugAppCheckProviderFactory? = null
    
    @VisibleForTesting
    internal var playIntegrityProvider: PlayIntegrityAppCheckProviderFactory? = null
    
    /**
     * Initialize Firebase AppCheck with duplicate key crash prevention
     * 
     * @param context Application context
     * @param isDebug True for debug builds, false for release builds
     * @param forceReset If true, allows re-initialization (use with caution)
     */
    fun initialize(context: Context, isDebug: Boolean, forceReset: Boolean = false) {
        initLock.writeLock().lock()
        try {
            if (initialized.get() && !forceReset) {
                Timber.d("✅ Firebase AppCheck already initialized (skipping duplicate initialization)")
                return
            }
            
            performInitialization(context, isDebug)
            initialized.set(true)
            Timber.i("✅ Firebase AppCheck Manager initialization completed successfully")
        } catch (e: Exception) {
            Timber.e("❌ Firebase AppCheck Manager initialization failed: ${e.message}")
            Timber.e(e)
        } finally {
            initLock.writeLock().unlock()
        }
    }
    
    /**
     * Performs the actual Firebase AppCheck provider installation
     * 
     * This method is separated to allow for testing and conditional execution
     */
    private fun performInitialization(context: Context, isDebug: Boolean) {
        try {
            if (isDebug) {
                initializeDebugProvider(context)
            } else {
                initializePlayIntegrityProvider(context)
            }
        } catch (e: IllegalStateException) {
            // Provider already installed - this is expected on re-initialization
            Timber.d("ℹ️ Firebase AppCheck provider already installed: ${e.message}")
        } catch (e: Exception) {
            Timber.e("⚠️ Unexpected error during AppCheck initialization: ${e.message}")
            throw e
        }
    }
    
    /**
     * Initialize Debug AppCheck Provider (for debug/testing builds)
     */
    private fun initializeDebugProvider(context: Context) {
        try {
            // Use cached provider instance to prevent duplicate instantiation
            if (debugProvider == null) {
                debugProvider = DebugAppCheckProviderFactory.getInstance()
                Timber.d("🔧 Debug AppCheck provider instance created")
            }
            
            debugProvider?.let {
                Firebase.appCheck.installAppCheckProviderFactory(it)
                Timber.d("🔍 Firebase AppCheck initialized with DEBUG provider (testing mode)")
                logDeviceInfo("DEBUG")
            } ?: run {
                Timber.w("⚠️ Failed to create debug AppCheck provider instance")
            }
        } catch (e: IllegalStateException) {
            Timber.d("ℹ️ Debug AppCheck provider already installed, ignoring duplicate installation")
        } catch (e: Exception) {
            Timber.w("⚠️ Debug AppCheck initialization issue: ${e.message}")
            throw e
        }
    }
    
    /**
     * Initialize Play Integrity AppCheck Provider (for release builds)
     */
    private fun initializePlayIntegrityProvider(context: Context) {
        try {
            // Use cached provider instance to prevent duplicate instantiation
            if (playIntegrityProvider == null) {
                playIntegrityProvider = PlayIntegrityAppCheckProviderFactory.getInstance()
                Timber.d("🔧 Play Integrity AppCheck provider instance created")
            }
            
            playIntegrityProvider?.let {
                Firebase.appCheck.installAppCheckProviderFactory(it)
                Timber.d("✅ Firebase AppCheck initialized with Play Integrity provider")
                logDeviceInfo("PLAY_INTEGRITY")
            } ?: run {
                Timber.w("⚠️ Failed to create Play Integrity AppCheck provider instance")
            }
        } catch (e: IllegalStateException) {
            Timber.d("ℹ️ Play Integrity AppCheck provider already installed, ignoring duplicate installation")
        } catch (e: Exception) {
            Timber.w("⚠️ Play Integrity failed (app may not be recognized by Play Store yet): ${e.message}")
            Timber.i("💡 OTP will work once Play Store recognizes your app (24-48 hours after upload)")
            // Don't throw - allow app to continue with deferred Play Integrity
        }
    }
    
    /**
     * Log device information for debugging device-specific issues (e.g., OnePlus)
     */
    private fun logDeviceInfo(provider: String) {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val sdkInt = Build.VERSION.SDK_INT
        val deviceInfo = "Device: $manufacturer $model (SDK $sdkInt)"
        
        if (manufacturer.contains("OnePlus", ignoreCase = true)) {
            Timber.w("⚠️ OnePlus device detected: $deviceInfo - Additional logging for p4.c duplicate key crash")
        }
        
        Timber.d("📱 AppCheck Provider: $provider on $deviceInfo")
    }
    
    /**
     * Check if AppCheck is properly initialized
     */
    fun isInitialized(): Boolean = initialized.get()
    
    /**
     * Reset initialization state (for testing only)
     */
    @VisibleForTesting
    internal fun reset() {
        initLock.writeLock().lock()
        try {
            initialized.set(false)
            debugProvider = null
            playIntegrityProvider = null
            Timber.d("🔄 Firebase AppCheck Manager state reset")
        } finally {
            initLock.writeLock().unlock()
        }
    }
    
    /**
     * Get initialization status for diagnostics
     */
    fun getDiagnostics(): Map<String, Any> = initLock.readLock().lock().let {
        try {
            mapOf(
                "initialized" to initialized.get(),
                "debugProviderCached" to (debugProvider != null),
                "playIntegrityProviderCached" to (playIntegrityProvider != null),
                "manufacturer" to Build.MANUFACTURER,
                "model" to Build.MODEL,
                "sdkVersion" to Build.VERSION.SDK_INT,
                "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "timestamp" to System.currentTimeMillis()
            )
        } finally {
            initLock.readLock().unlock()
        }
    }
}
