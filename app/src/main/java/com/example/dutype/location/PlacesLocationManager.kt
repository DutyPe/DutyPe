package com.example.dutype.location

import android.content.Context
import androidx.activity.ComponentActivity
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Thread-safe manager for Google Places SDK client initialization.
 * Ensures that Places SDK is initialized exactly once with proper Activity context,
 * preventing IllegalStateException: "Cannot find caller" crashes.
 *
 * The Places SDK requires Activity context for UI operations like startActivityForResult.
 * This manager validates context and maintains a per-activity cache of PlacesClient instances.
 *
 * Root cause of crash: Places SDK initialized with application context, but requires Activity context
 * for lifecycle-aware operations. This manager enforces proper Activity context usage.
 *
 * @see <a href="https://developers.google.com/maps/documentation/places/android-sdk/start">Places SDK Documentation</a>
 */
object PlacesLocationManager {
    private val lock = ReentrantReadWriteLock()
    private val isInitialized = AtomicBoolean(false)
    private val placesClientCache = mutableMapOf<String, PlacesClient>()
    private var apiKey: String? = null
    private var lastInitializationContext: Context? = null

    /**
     * Initialize Places SDK with proper Activity context.
     *
     * @param activity Activity context required for Places SDK UI operations
     * @param mapsApiKey Google Maps API key for Places SDK
     * @return true if initialization successful, false if already initialized or error occurred
     */
    fun initialize(activity: ComponentActivity, mapsApiKey: String): Boolean {
        if (!isValidPlacesContext(activity)) {
            Timber.e("PlacesLocationManager: Invalid Activity context provided")
            return false
        }

        try {
            lock.writeLock().lock()
            return try {
                if (isInitialized.get()) {
                    Timber.d("PlacesLocationManager: Already initialized, skipping re-initialization")
                    return true
                }

                if (!Places.isInitialized()) {
                    Places.initialize(activity, mapsApiKey)
                    apiKey = mapsApiKey
                    lastInitializationContext = activity
                    isInitialized.set(true)
                    Timber.d("PlacesLocationManager: Initialized successfully with Activity context")
                    return true
                } else {
                    Timber.d("PlacesLocationManager: Places already initialized externally")
                    isInitialized.set(true)
                    apiKey = mapsApiKey
                    return true
                }
            } finally {
                lock.writeLock().unlock()
            }
        } catch (e: Exception) {
            Timber.e(e, "PlacesLocationManager: Failed to initialize Places SDK")
            return false
        }
    }

    /**
     * Get or create PlacesClient for the given Activity.
     * Caches clients per activity to prevent excessive re-creation.
     *
     * @param activity Activity context for the PlacesClient
     * @return PlacesClient instance, or null if initialization failed
     */
    fun getPlacesClient(activity: ComponentActivity): PlacesClient? {
        if (!isValidPlacesContext(activity)) {
            Timber.e("PlacesLocationManager: Invalid Activity context for getPlacesClient")
            return null
        }

        try {
            lock.readLock().lock()
            return try {
                if (!isInitialized.get() || !Places.isInitialized()) {
                    Timber.e("PlacesLocationManager: Places not initialized, call initialize() first")
                    return null
                }

                val activityKey = activity::class.java.simpleName
                val cachedClient = placesClientCache[activityKey]
                if (cachedClient != null) {
                    Timber.d("PlacesLocationManager: Returning cached PlacesClient for $activityKey")
                    return cachedClient
                }

                // Need to create new client, upgrade to write lock
                lock.readLock().unlock()
                lock.writeLock().lock()

                return try {
                    val newClient = Places.createClient(activity)
                    placesClientCache[activityKey] = newClient
                    Timber.d("PlacesLocationManager: Created new PlacesClient for $activityKey")
                    newClient
                } catch (e: Exception) {
                    Timber.e(e, "PlacesLocationManager: Failed to create PlacesClient")
                    null
                } finally {
                    lock.writeLock().unlock()
                    lock.readLock().lock()
                }
            } finally {
                lock.readLock().unlock()
            }
        } catch (e: Exception) {
            Timber.e(e, "PlacesLocationManager: Exception in getPlacesClient")
            return null
        }
    }

    /**
     * Clear cached PlacesClient instances.
     * Useful for cleanup or forcing re-creation on next access.
     */
    fun clearCache() {
        lock.writeLock().lock()
        try {
            placesClientCache.clear()
            Timber.d("PlacesLocationManager: Cleared PlacesClient cache")
        } finally {
            lock.writeLock().unlock()
        }
    }

    /**
     * Check if Places is properly initialized.
     */
    fun isInitialized(): Boolean = isInitialized.get() && Places.isInitialized()

    /**
     * Get diagnostics about the current state.
     */
    fun getDiagnostics(): String {
        lock.readLock().lock()
        return try {
            """
            PlacesLocationManager Status:
              - IsInitialized: ${isInitialized.get()}
              - PlacesSDKInitialized: ${Places.isInitialized()}
              - CachedClients: ${placesClientCache.size}
              - LastInitContext: ${lastInitializationContext?.javaClass?.simpleName}
              - APIKey set: ${!apiKey.isNullOrEmpty()}
            """.trimIndent()
        } finally {
            lock.readLock().unlock()
        }
    }

    /**
     * Validate that context is an Activity (required by Places SDK).
     * Places SDK UI operations require Activity context, not application context.
     */
    private fun validateContext(context: Context?): Boolean {
        return context is ComponentActivity
    }
}

/**
 * Extension function to safely get PlacesClient from Activity context.
 * Handles initialization and context validation.
 */
fun ComponentActivity.getPlacesClient(): PlacesClient? {
    return if (isValidPlacesContext(this)) {
        PlacesLocationManager.getPlacesClient(this)
    } else {
        Timber.e("Invalid context for Places SDK")
        null
    }
}

/**
 * Extension function to validate if context is suitable for Places SDK operations.
 * Places SDK requires Activity context for UI operations.
 */
fun isValidPlacesContext(context: Context?): Boolean {
    return context != null && context is ComponentActivity && context.isFinishing.not()
}
