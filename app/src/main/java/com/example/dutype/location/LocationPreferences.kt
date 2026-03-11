package com.example.dutype.location

import android.content.Context
import android.content.SharedPreferences
import com.example.dutype.models.LocationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Enhanced Location Preferences Manager
 * Manages location preferences and storage with detailed address support
 */
class LocationPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _currentLocation = MutableStateFlow<LocationData?>(null)
    val currentLocation: StateFlow<LocationData?> get() = _currentLocation.asStateFlow()
    
    // Live location state for real-time updates
    private val _isLocationLoading = MutableStateFlow(false)
    val isLocationLoading: StateFlow<Boolean> get() = _isLocationLoading.asStateFlow()
    
    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> get() = _locationError.asStateFlow()

    companion object {
        private const val PREFS_NAME = "location_preferences"
        private const val KEY_ADDRESS = "address"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_CITY = "city"
        private const val KEY_STATE = "state"
        private const val KEY_COUNTRY = "country"
        private const val KEY_AREA = "area"
        private const val KEY_ACCURACY = "accuracy"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_LAST_UPDATED = "last_updated"
        private const val KEY_LOCATION_ENABLED = "location_enabled"
        private const val KEY_PERMISSION_GRANTED = "permission_granted"
    }

    init {
        // Load saved location on initialization
        loadSavedLocation()
    }
    
    /**
     * Set loading state
     */
    fun setLoading(loading: Boolean) {
        _isLocationLoading.value = loading
    }
    
    /**
     * Set error state
     */
    fun setError(error: String?) {
        _locationError.value = error
    }

    /**
     * Save complete location data to preferences
     */
    fun saveLocation(locationData: LocationData) {
        Timber.d("📍 LocationPreferences: Saving location - ${locationData.getShortAddress()}")
        Timber.d("📍 LocationPreferences: Coordinates - lat=${locationData.latitude}, lon=${locationData.longitude}")
        
        // Save to SharedPreferences first
        prefs.edit().apply {
            putString(KEY_ADDRESS, locationData.address)
            putFloat(KEY_LATITUDE, locationData.latitude.toFloat())
            putFloat(KEY_LONGITUDE, locationData.longitude.toFloat())
            putString(KEY_CITY, locationData.city)
            putString(KEY_STATE, locationData.state)
            putString(KEY_COUNTRY, locationData.country)
            putString(KEY_AREA, locationData.area)
            putFloat(KEY_ACCURACY, locationData.accuracy)
            putLong(KEY_TIMESTAMP, locationData.timestamp)
            putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
            putBoolean(KEY_LOCATION_ENABLED, true)
            apply()
        }
        
        // CRITICAL FIX: Update flow AFTER saving to prefs
        // This triggers all observers (ViewModels) to update immediately
        _currentLocation.value = locationData
        _locationError.value = null

        Timber.d("📍 LocationPreferences: Location saved successfully and flow updated")
        Timber.d("📍 LocationPreferences: Flow value is now - ${_currentLocation.value?.getShortAddress()}")
    }

    /**
     * Save manual location data to preferences
     */
    fun saveManualLocation(city: String, area: String, displayName: String) {
        Timber.d("📍 LocationPreferences: Saving manual location - $city, $area")
        prefs.edit().apply {
            putString(KEY_ADDRESS, displayName)
            putString(KEY_CITY, city)
            putString(KEY_AREA, area)
            putString(KEY_STATE, "") 
            putString(KEY_COUNTRY, "India")
            putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
            putBoolean(KEY_LOCATION_ENABLED, true)
            apply()
        }

        val locationData = LocationData(
            address = displayName,
            latitude = 0.0,
            longitude = 0.0,
            city = city,
            state = null,
            country = "India",
            area = area
        )
        _currentLocation.value = locationData
    }
    
    /**
     * Save permission granted state
     */
    fun setPermissionGranted(granted: Boolean) {
        prefs.edit().putBoolean(KEY_PERMISSION_GRANTED, granted).apply()
    }
    
    /**
     * Check if permission was previously granted
     */
    fun wasPermissionGranted(): Boolean {
        return prefs.getBoolean(KEY_PERMISSION_GRANTED, false)
    }

    /**
     * Get saved location data with all details
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
                area = prefs.getString(KEY_AREA, null),
                accuracy = prefs.getFloat(KEY_ACCURACY, 0f),
                timestamp = prefs.getLong(KEY_TIMESTAMP, System.currentTimeMillis())
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
            remove(KEY_AREA)
            remove(KEY_ACCURACY)
            remove(KEY_TIMESTAMP)
            remove(KEY_LAST_UPDATED)
            putBoolean(KEY_LOCATION_ENABLED, false)
            apply()
        }

        _currentLocation.value = null
    }

    /**
     * Check if location data is recent (within 30 minutes for live updates)
     */
    fun isLocationFresh(): Boolean {
        val lastUpdated = prefs.getLong(KEY_LAST_UPDATED, 0)
        val thirtyMinutes = 30 * 60 * 1000L
        return (System.currentTimeMillis() - lastUpdated) < thirtyMinutes
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
     * Get user's area for job filtering
     */
    fun getUserArea(): String? {
        return getSavedLocation()?.area
    }

    /**
     * Get user's state for job filtering
     */
    fun getUserState(): String? {
        return getSavedLocation()?.state
    }

    /**
     * Get formatted location string for display (short version)
     */
    fun getLocationDisplayString(): String? {
        val location = getSavedLocation()
        return location?.getShortAddress()
    }
    
    /**
     * Get formatted detailed location string for display
     */
    fun getDetailedLocationString(): String? {
        val location = getSavedLocation()
        return location?.getMediumAddress()
    }
    
    /**
     * Get full address for display
     */
    fun getFullAddressString(): String? {
        val location = getSavedLocation()
        return location?.getFullAddress()
    }

    /**
     * Check if we have valid location coordinates
     */
    fun hasValidCoordinates(): Boolean {
        val location = getSavedLocation()
        return location?.hasValidCoordinates() == true
    }
    
    /**
     * Get coordinates as Pair
     */
    fun getCoordinates(): Pair<Double, Double>? {
        val location = getSavedLocation()
        return if (location?.hasValidCoordinates() == true) {
            Pair(location.latitude, location.longitude)
        } else null
    }

    private fun loadSavedLocation() {
        _currentLocation.value = getSavedLocation()
    }
    
    /**
     * Refresh location from saved data
     */
    fun refreshLocation() {
        _currentLocation.value = getSavedLocation()
    }
}
