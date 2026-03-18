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
        private const val KEY_LOCATION_MODE = "location_mode"

        private const val LOCATION_MODE_AUTO = "AUTO"
        private const val LOCATION_MODE_MANUAL = "MANUAL"
    }

    init {
        // Migrate old Float lat/lon values to String (one-time fix for existing installs)
        migrateFloatToString()
        // Load saved location on initialization
        loadSavedLocation()
    }
    
    /**
     * One-time migration: if lat/lon was stored as Float, re-save as String
     * SharedPreferences.getString() throws ClassCastException on Float values
     */
    private fun migrateFloatToString() {
        try {
            // Test if lat is stored as Float by trying getFloat
            val latFloat = prefs.getFloat(KEY_LATITUDE, Float.MIN_VALUE)
            if (latFloat != Float.MIN_VALUE) {
                // Old Float values exist — re-write as String and keep all other data
                val lonFloat = prefs.getFloat(KEY_LONGITUDE, 0f)
                Timber.d("📍 Migrating location from Float to String: lat=$latFloat, lon=$lonFloat")
                prefs.edit().apply {
                    remove(KEY_LATITUDE)
                    remove(KEY_LONGITUDE)
                    putString(KEY_LATITUDE, latFloat.toDouble().toString())
                    putString(KEY_LONGITUDE, lonFloat.toDouble().toString())
                    apply()
                }
            }
        } catch (_: ClassCastException) {
            // Already stored as String — no migration needed
        } catch (_: Exception) {
            // Ignore any other errors during migration
        }
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
    fun saveLocation(locationData: LocationData, forceManualOverride: Boolean = false) {
        if (isManualLocationLocked() && !forceManualOverride) {
            Timber.d("📍 LocationPreferences: Ignoring auto location save because manual location is locked")
            return
        }

        Timber.d("📍 LocationPreferences: Saving location - ${locationData.getShortAddress()}")
        Timber.d("📍 LocationPreferences: Coordinates - lat=${locationData.latitude}, lon=${locationData.longitude}")
        
        // Save to SharedPreferences first
        // PRECISION FIX: Store lat/lon as String to preserve Double precision (15 digits vs Float's 7)
        prefs.edit().apply {
            putString(KEY_ADDRESS, locationData.address)
            putString(KEY_LATITUDE, locationData.latitude.toString())
            putString(KEY_LONGITUDE, locationData.longitude.toString())
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
     * Save a user-selected preferred location and lock it from automatic GPS refreshes.
     */
    fun savePreferredLocation(locationData: LocationData) {
        setLocationModeManual()
        saveLocation(locationData, forceManualOverride = true)
    }

    /**
     * Save manual location data to preferences
     */
    fun saveManualLocation(city: String, area: String, displayName: String, latitude: Double = 0.0, longitude: Double = 0.0) {
        Timber.d("📍 LocationPreferences: Saving manual location - $city, $area (lat=$latitude, lon=$longitude)")
        setLocationModeManual()
        prefs.edit().apply {
            putString(KEY_ADDRESS, displayName)
            putString(KEY_CITY, city)
            putString(KEY_AREA, area)
            putString(KEY_STATE, "") 
            putString(KEY_COUNTRY, "India")
            if (latitude != 0.0 || longitude != 0.0) {
                putString(KEY_LATITUDE, latitude.toString())
                putString(KEY_LONGITUDE, longitude.toString())
            }
            putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
            putBoolean(KEY_LOCATION_ENABLED, true)
            apply()
        }

        val locationData = LocationData(
            address = displayName,
            latitude = latitude,
            longitude = longitude,
            city = city,
            state = null,
            country = "India",
            area = area
        )
        _currentLocation.value = locationData
    }

    /**
     * Use automatic current location updates (GPS can refresh location).
     */
    fun setLocationModeAuto() {
        prefs.edit().putString(KEY_LOCATION_MODE, LOCATION_MODE_AUTO).apply()
    }

    /**
     * Lock to manually selected location until user changes it.
     */
    fun setLocationModeManual() {
        prefs.edit().putString(KEY_LOCATION_MODE, LOCATION_MODE_MANUAL).apply()
    }

    fun isManualLocationLocked(): Boolean {
        return prefs.getString(KEY_LOCATION_MODE, LOCATION_MODE_AUTO) == LOCATION_MODE_MANUAL
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
            // PRECISION FIX: Read lat/lon as String→Double, with Float fallback for migration
            // getString() throws ClassCastException if the stored value is a Float, so wrap in try-catch
            val lat = try {
                prefs.getString(KEY_LATITUDE, null)?.toDoubleOrNull()
            } catch (_: ClassCastException) { null }
                ?: try { prefs.getFloat(KEY_LATITUDE, 0f).toDouble() } catch (_: Exception) { 0.0 }
            val lon = try {
                prefs.getString(KEY_LONGITUDE, null)?.toDoubleOrNull()
            } catch (_: ClassCastException) { null }
                ?: try { prefs.getFloat(KEY_LONGITUDE, 0f).toDouble() } catch (_: Exception) { 0.0 }
            LocationData(
                address = prefs.getString(KEY_ADDRESS, "") ?: "",
                latitude = lat,
                longitude = lon,
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
     * Return saved location only when coordinates are valid and the reading is recent enough.
     * This prevents distance sorting against stale coordinates from previous sessions.
     */
    fun getSavedLocationIfFresh(maxAgeMs: Long = 15 * 60 * 1000L): LocationData? {
        val location = getSavedLocation() ?: return null
        val lastUpdated = prefs.getLong(KEY_LAST_UPDATED, 0L)
        val ageMs = System.currentTimeMillis() - lastUpdated
        val hasFreshTimestamp = ageMs in 0 until maxAgeMs
        val hasUsableCoordinates = location.hasValidCoordinates()

        return if (hasFreshTimestamp && hasUsableCoordinates) {
            location
        } else {
            Timber.d(
                "📍 LocationPreferences: Ignoring stale/invalid saved location (ageMs=$ageMs, valid=$hasUsableCoordinates)"
            )
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

    fun isLocationFresh(maxAgeMs: Long): Boolean {
        val lastUpdated = prefs.getLong(KEY_LAST_UPDATED, 0L)
        return (System.currentTimeMillis() - lastUpdated) < maxAgeMs
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
