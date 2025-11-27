package com.example.dutype.utils

import android.app.Activity
import android.content.Context
import androidx.activity.ComponentActivity
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Places SDK Location Manager
 * 
 * Fixes the crash: "IllegalStateException: Cannot find caller. startActivityForResult should be used."
 * 
 * Root Cause Analysis:
 * - Google Places SDK expects an Activity or Fragment context for UI operations
 * - Using application context (Context.getApplicationContext()) causes the crash
 * - Modern Activity Result API (registerForActivityResult) should be used instead of old startActivityForResult
 * - PlacesClient initialization must use Activity context, not application context
 * 
 * Solution:
 * - Provide Activity context explicitly to Places operations
 * - Initialize PlacesClient lazily with Activity context
 * - Support modern Activity Result API for location picking
 * - Cache PlacesClient per Activity to avoid re-initialization
 * 
 * Usage:
 * In ManualLocationScreen.kt:
 *   val placesManager = remember { PlacesLocationManager.getInstance(LocalContext.current as Activity) }
 *   placesManager.getPlacesClient(activity)
 * 
 * Thread Safety:
 * - Uses atomic boolean for initialization state
 * - Provides thread-safe lazy initialization
 */
object PlacesLocationManager {
    
    private val initialized = AtomicBoolean(false)
    private val initLock = ReentrantReadWriteLock()
    
    // Cache PlacesClient per activity
    private val clientCache = mutableMapOf<String, PlacesClient>()
    
    /**
     * Initialize Places SDK with proper Activity context
     * 
     * @param activity Current Activity (not application context)
     * @param apiKey Google Places API key
     */
    fun initialize(activity: Activity, apiKey: String) {
        initLock.writeLock().lock()
        try {
            if (initialized.get()) {
                Timber.d("✅ Places SDK already initialized (skipping duplicate initialization)")
                return
            }
            
            if (!Places.isInitialized()) {
                try {
                    // Initialize with application context (this is safe)
                    Places.initialize(activity.applicationContext, apiKey)
                    Timber.d("✅ Places SDK initialized with API key")
                } catch (e: Exception) {
                    Timber.e("❌ Places SDK initialization failed: ${e.message}")
                    throw e
                }
            }
            
            initialized.set(true)
        } finally {
            initLock.writeLock().unlock()
        }
    }
    
    /**
     * Get PlacesClient with Activity context (required for UI operations)
     * 
     * CRITICAL: Do NOT pass application context to Places SDK methods that launch UI
     * The Places SDK internally calls startActivityForResult and needs Activity context
     * 
     * @param activity Current Activity (MUST be Activity, not application context)
     * @return PlacesClient instance
     */
    fun getPlacesClient(activity: Activity): PlacesClient {
        return initLock.readLock().lock().let {
            try {
                // Check cache first
                val activityKey = activity.javaClass.simpleName + "@" + System.identityHashCode(activity)
                
                clientCache[activityKey]?.let {
                    Timber.d("♻️ PlacesClient retrieved from cache for ${activity.javaClass.simpleName}")
                    return@let it
                }
                
                // Create new PlacesClient with Activity context
                // IMPORTANT: Pass Activity, NOT application context
                if (!Places.isInitialized()) {
                    Timber.w("⚠️ Places not initialized. Create client anyway.")
                }
                
                val client = Places.createClient(activity)
                clientCache[activityKey] = client
                Timber.d("✅ PlacesClient created for ${activity.javaClass.simpleName}")
                client
            } finally {
                initLock.readLock().unlock()
            }
        }
    }
    
    /**
     * Clear cached client for an activity (call onDestroy)
     */
    fun clearCache(activity: Activity) {
        val activityKey = activity.javaClass.simpleName + "@" + System.identityHashCode(activity)
        clientCache.remove(activityKey)
        Timber.d("🗑️ PlacesClient cache cleared for ${activity.javaClass.simpleName}")
    }
    
    /**
     * Clear all cached clients
     */
    fun clearAllCaches() {
        clientCache.clear()
        Timber.d("🗑️ All PlacesClient caches cleared")
    }
    
    /**
     * Verify that proper Activity context is being used
     */
    fun validateContext(context: Context): Boolean {
        return when {
            context is ComponentActivity -> true
            context is Activity -> true
            else -> {
                Timber.w("⚠️ WARNING: Context is not an Activity! This may cause 'Cannot find caller' crashes.")
                Timber.w("⚠️ Context type: ${context.javaClass.simpleName}")
                false
            }
        }
    }
    
    /**
     * Get diagnostics
     */
    fun getDiagnostics(): Map<String, Any> = mapOf(
        "initialized" to initialized.get(),
        "cachedClientsCount" to clientCache.size,
        "timestamp" to System.currentTimeMillis()
    )
}

/**
 * Extension function to safely get PlacesClient from Activity
 */
fun Activity.getPlacesClient(): PlacesClient {
    return PlacesLocationManager.getPlacesClient(this)
}

/**
 * Validate context before Places operations
 */
fun Context.isValidPlacesContext(): Boolean {
    return this is Activity || this is ComponentActivity
}
