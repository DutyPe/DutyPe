package com.example.partimes.location

import android.content.Context
import android.content.SharedPreferences
import com.example.partimes.models.LocationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages location preferences and storage for the application
 */
class LocationPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _currentLocation = MutableStateFlow<LocationData?>(null)
    val currentLocation: StateFlow<LocationData?> get() = _currentLocation.asStateFlow()

    companion object {
        private const val PREFS_NAME = "location_preferences"
        private const val KEY_ADDRESS = "address"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_CITY = "city"
        private const val KEY_STATE = "state"
        private const val KEY_COUNTRY = "country"
        private const val KEY_POSTAL_CODE = "postal_code"
        private const val KEY_LAST_UPDATED = "last_updated"
        private const val KEY_LOCATION_ENABLED = "location_enabled"
    }

    init {
        // Load saved location on initialization
        loadSavedLocation()
    }

    /**
     * Save location data to preferences
     */
    fun saveLocation(locationData: LocationData) {
        prefs.edit().apply {
            putString(KEY_ADDRESS, locationData.address)
            putFloat(KEY_LATITUDE, locationData.latitude.toFloat())
            putFloat(KEY_LONGITUDE, locationData.longitude.toFloat())
            putString(KEY_CITY, locationData.city)
            putString(KEY_STATE, locationData.state)
            putString(KEY_COUNTRY, locationData.country)
            putString(KEY_POSTAL_CODE, locationData.postalCode)
            putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
            putBoolean(KEY_LOCATION_ENABLED, true)
            apply()
        }

        _currentLocation.value = locationData
    }

    /**
     * Save manual location data to preferences
     */
    fun saveManualLocation(city: String, state: String, displayName: String) {
        prefs.edit().apply {
            putString(KEY_ADDRESS, displayName)
            putString(KEY_CITY, city)
            putString(KEY_STATE, state)
            putString(KEY_COUNTRY, "India") // Default for your app
            putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
            putBoolean(KEY_LOCATION_ENABLED, true)
            apply()
        }

        val locationData = LocationData(
            address = displayName,
            latitude = 0.0, // You can add coordinates later if needed
            longitude = 0.0,
            city = city,
            state = state,
            country = "India",
            postalCode = null // Fix: add missing parameter
        )
        _currentLocation.value = locationData
    }

    /**
     * Get saved location data
     */
    fun getSavedLocation(): LocationData? {
        return if (prefs.contains(KEY_ADDRESS)) {
            LocationData(
                address = prefs.getString(KEY_ADDRESS, "") ?: "",
                latitude = prefs.getFloat(KEY_LATITUDE, 0f).toDouble(),
                longitude = prefs.getFloat(KEY_LONGITUDE, 0f).toDouble(),
                city = prefs.getString(KEY_CITY, null),
                state = prefs.getString(KEY_STATE, null),
                country = prefs.getString(KEY_COUNTRY, null),
                postalCode = prefs.getString(KEY_POSTAL_CODE, null)
            )
        } else {
            null
        }
    }

    /**
     * Clear saved location data
     */
    fun clearLocation() {
        prefs.edit().apply {
            remove(KEY_ADDRESS)
            remove(KEY_LATITUDE)
            remove(KEY_LONGITUDE)
            remove(KEY_CITY)
            remove(KEY_STATE)
            remove(KEY_COUNTRY)
            remove(KEY_POSTAL_CODE)
            remove(KEY_LAST_UPDATED)
            putBoolean(KEY_LOCATION_ENABLED, false)
            apply()
        }

        _currentLocation.value = null
    }

    /**
     * Check if location data is recent (within 24 hours)
     */
    fun isLocationRecent(): Boolean {
        val lastUpdated = prefs.getLong(KEY_LAST_UPDATED, 0)
        val twentyFourHours = 24 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - lastUpdated) < twentyFourHours
    }

    /**
     * Check if user has enabled location services
     */
    fun isLocationEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOCATION_ENABLED, false)
    }

    /**
     * Set location service preference
     */
    fun setLocationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCATION_ENABLED, enabled).apply()
    }

    /**
     * Get user's city for job filtering
     */
    fun getUserCity(): String? {
        return getSavedLocation()?.city
    }

    /**
     * Get user's state for job filtering
     */
    fun getUserState(): String? {
        return getSavedLocation()?.state
    }

    /**
     * Get formatted location string for display
     */
    fun getLocationDisplayString(): String? {
        val location = getSavedLocation()
        return when {
            location?.city != null && location.state != null -> "${location.city}, ${location.state}"
            location?.city != null -> location.city
            location?.address?.isNotBlank() == true -> location.address
            else -> null
        }
    }

    /**
     * Check if we have valid location coordinates
     */
    fun hasValidCoordinates(): Boolean {
        val location = getSavedLocation()
        return location != null && location.latitude != 0.0 && location.longitude != 0.0
    }

    private fun loadSavedLocation() {
        _currentLocation.value = getSavedLocation()
    }
}