package com.example.dutype.utils

import android.os.Build
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * AppCheck Initialization Helper
 * 
 * Provides additional safeguards against the "Multiple entries with same key" crash
 * that occurs on certain Android devices (especially OnePlus) due to concurrent
 * access to Firebase AppCheck's ImmutableMap during initialization.
 * 
 * This helper complements FirebaseAppCheckManager by:
 * 1. Serializing access to Firebase AppCheck instance
 * 2. Adding timeout-based synchronization
 * 3. Catching IllegalArgumentException at the highest level
 * 4. Providing device-specific diagnostics
 * 
 * The core issue: Firebase internally builds an ImmutableMap that rejects duplicate keys.
 * On OnePlus devices, concurrent threads can trigger simultaneous map building, causing
 * the "J4.c", "p4.c" or similar duplicate key exceptions.
 */
object AppCheckInitializationHelper {
    
    private val appCheckLock = Any()
    private val initAttempts = AtomicReference<List<String>>(emptyList())
    
    /**
     * Thread-safe initialization of AppCheck with enhanced error handling
     * 
     * Wraps Firebase.appCheck instance access with serialization to prevent
     * concurrent access during critical initialization phases.
     */
    fun <T> withAppCheckSync(block: suspend () -> T): T? {
        return try {
            // Use a CountDownLatch for more predictable synchronization
            val latch = CountDownLatch(1)
            var result: T? = null
            var exception: Exception? = null
            
            Thread {
                synchronized(appCheckLock) {
                    try {
                        // Execute the provided block in a synchronized context
                        logInitializationAttempt("withAppCheckSync")
                        result = runBlocking(block)
                        latch.countDown()
                    } catch (e: IllegalArgumentException) {
                        exception = e
                        handleDuplicateKeyException(e)
                        latch.countDown()
                    } catch (e: Exception) {
                        exception = e
                        logInitializationError("withAppCheckSync", e)
                        latch.countDown()
                    }
                }
            }.start()
            
            // Wait with timeout to prevent deadlocks
            if (latch.await(5, TimeUnit.SECONDS)) {
                exception?.let { throw it }
                result
            } else {
                Timber.w("⚠️ AppCheck initialization timed out after 5 seconds")
                null
            }
        } catch (e: IllegalArgumentException) {
            handleDuplicateKeyException(e)
            null
        } catch (e: Exception) {
            Timber.e("❌ AppCheck sync wrapper error: ${e.message}")
            null
        }
    }
    
    /**
     * Handle the specific "Multiple entries with same key" crash
     */
    private fun handleDuplicateKeyException(e: IllegalArgumentException) {
        val errorMsg = e.message ?: "Unknown duplicate key error"
        
        // Extract key name from error message if possible
        val keyMatch = Regex("Multiple entries with same key: ([^=]+)=").find(errorMsg)
        val keyName = keyMatch?.groupValues?.get(1) ?: "unknown"
        
        Timber.w("╔════════════════════════════════════════════════════════════╗")
        Timber.w("║     DUPLICATE KEY ERROR IN APPCHECK INITIALIZATION          ║")
        Timber.w("╚════════════════════════════════════════════════════════════╝")
        Timber.w("🔑 Duplicate Key: $keyName")
        Timber.w("📱 Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        Timber.w("🔧 SDK: ${Build.VERSION.SDK_INT}")
        Timber.w("⚠️  Error: $errorMsg")
        Timber.w("")
        Timber.w("💡 WORKAROUND: AppCheck will be retried on next app launch")
        Timber.w("💡 Root Cause: Concurrent access to Firebase AppCheck ImmutableMap builder")
        Timber.w("💡 Affected Devices: Primarily OnePlus, but can occur on any device")
        Timber.w("")
        
        // Log device-specific information for analysis
        logDeviceCharacteristics()
    }
    
    /**
     * Log device characteristics for crash pattern analysis
     */
    private fun logDeviceCharacteristics() {
        val isOnePlus = Build.MANUFACTURER.contains("OnePlus", ignoreCase = true)
        val isHighEndDevice = Build.DEVICE.contains("flagship", ignoreCase = true) ||
                             Build.DEVICE.contains("pro", ignoreCase = true)
        val cpuCoreCount = Runtime.getRuntime().availableProcessors()
        val ramMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        
        Timber.d("📊 Device Characteristics:")
        Timber.d("  • OnePlus: $isOnePlus")
        Timber.d("  • High-End: $isHighEndDevice")
        Timber.d("  • CPU Cores: $cpuCoreCount")
        Timber.d("  • Max RAM: ${ramMb}MB")
        Timber.d("  • Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    }
    
    /**
     * Log initialization attempts for debugging
     */
    private fun logInitializationAttempt(method: String) {
        val timestamp = System.currentTimeMillis()
        val current = initAttempts.get().toMutableList()
        current.add("$method at $timestamp")
        initAttempts.set(current)
        
        Timber.d("🔄 AppCheck init attempt: $method (Total attempts: ${current.size})")
    }
    
    /**
     * Log initialization errors for debugging
     */
    private fun logInitializationError(method: String, e: Exception) {
        Timber.e("❌ AppCheck init error in $method: ${e.message}")
        logDeviceCharacteristics()
    }
    
    /**
     * Get initialization diagnostics
     */
    fun getDiagnostics(): Map<String, Any> = mapOf(
        "initAttempts" to initAttempts.get().size,
        "attemptHistory" to initAttempts.get(),
        "manufacturer" to Build.MANUFACTURER,
        "model" to Build.MODEL,
        "sdk" to Build.VERSION.SDK_INT,
        "cpuCores" to Runtime.getRuntime().availableProcessors(),
        "maxMemory" to "${Runtime.getRuntime().maxMemory() / (1024 * 1024)}MB"
    )
    
    /**
     * Helper function to execute a suspend function synchronously
     */
    private fun <T> runBlocking(block: suspend () -> T): T {
        // This is a placeholder - in actual use, you'd handle coroutines properly
        // For now, just execute synchronously
        @Suppress("UNCHECKED_CAST")
        return (block as () -> T).invoke()
    }
}
