package com.example.dutype.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Location information data class with detailed address fields
 */
data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val city: String,
    val area: String,
    val state: String = "",
    val postalCode: String = "",
    val streetName: String = "",
    val buildingName: String = "",
    val landmark: String = "",
    val district: String = "",
    val country: String = "India",
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Get short display address (Area, City)
     * Example: "Nallagandla, Serilingampalle"
     */
    fun getShortAddress(): String {
        return when {
            area.isNotBlank() && city.isNotBlank() -> "$area, $city"
            city.isNotBlank() -> city
            area.isNotBlank() -> area
            else -> address.split(",").firstOrNull()?.trim() ?: address
        }
    }
    
    /**
     * Get medium display address (Area, City, State PostalCode)
     * Example: "Nallagandla, Serilingampalle (M), Telangana 500019"
     */
    fun getMediumAddress(): String {
        val parts = mutableListOf<String>()
        if (area.isNotBlank()) parts.add(area)
        if (city.isNotBlank()) parts.add(city)
        if (state.isNotBlank()) {
            val stateWithPostal = if (postalCode.isNotBlank()) "$state $postalCode" else state
            parts.add(stateWithPostal)
        }
        return if (parts.isNotEmpty()) parts.joinToString(", ") else address
    }
    
    /**
     * Get full detailed address
     * Example: "Road No. 10, HUDA Layout, Nallagandla, Serilingampalle (M), Telangana 500019"
     */
    fun getFullAddress(): String {
        val parts = mutableListOf<String>()
        if (buildingName.isNotBlank()) parts.add(buildingName)
        if (streetName.isNotBlank()) parts.add(streetName)
        if (area.isNotBlank()) parts.add(area)
        if (landmark.isNotBlank()) parts.add(landmark)
        if (city.isNotBlank()) parts.add(city)
        if (district.isNotBlank() && district != city) parts.add(district)
        if (state.isNotBlank()) parts.add(state)
        if (postalCode.isNotBlank()) parts.add(postalCode)
        return if (parts.isNotEmpty()) parts.joinToString(", ") else address
    }
    
    /**
     * Get display address for header (Area, City, State PostalCode)
     * Example: "Nallagandla, Serilingampalle (M), Telangana 500019"
     */
    fun getDisplayAddress(): String {
        val parts = mutableListOf<String>()
        
        // Add area/locality
        if (area.isNotBlank()) parts.add(area)
        
        // Add city/municipality
        if (city.isNotBlank()) parts.add(city)
        
        // Add state with postal code
        val statePostal = buildString {
            if (state.isNotBlank()) append(state)
            if (postalCode.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(postalCode)
            }
        }
        if (statePostal.isNotBlank()) parts.add(statePostal)
        
        return if (parts.isNotEmpty()) parts.joinToString(", ") else address
    }
    
    /**
     * Get complete address with coordinates
     * Example: "17.4567, 78.3456, Road No. 10, HUDA Layout, Nallagandla, Serilingampalle (M), Telangana 500019"
     */
    fun getCompleteAddressWithCoordinates(): String {
        val coordsStr = if (latitude != 0.0 || longitude != 0.0) {
            "${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}"
        } else ""
        
        val fullAddr = getFullAddress()
        
        return when {
            coordsStr.isNotBlank() && fullAddr.isNotBlank() -> "$coordsStr, $fullAddr"
            fullAddr.isNotBlank() -> fullAddr
            coordsStr.isNotBlank() -> coordsStr
            else -> address
        }
    }
}

/**
 * Location state for UI
 */
sealed class LocationState {
    object Idle : LocationState()
    object Loading : LocationState()
    data class Success(val location: LocationInfo) : LocationState()
    data class Error(val message: String) : LocationState()
}

/**
 * Enhanced Location Service with real-time updates and better error handling
 * 
 * Uses a hybrid approach:
 * - Azure Maps for reverse geocoding (better Indian address support)
 * - Android Geocoder as fallback
 * - FusedLocationProvider for GPS coordinates
 */
class LocationService(private val context: Context) {
    
    companion object {
        /**
         * Calculate distance between two coordinates using Haversine formula
         * Static method for use without LocationService instance
         * @return Distance in kilometers
         */
        fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val earthRadius = 6371.0 // Earth's radius in kilometers
            
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)
            
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            
            return earthRadius * c
        }
        
        /**
         * Format distance for display (static version)
         */
        fun formatDistanceStatic(distanceKm: Double): String {
            return when {
                distanceKm < 0.1 -> "< 100m"
                distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()}m"
                distanceKm < 10.0 -> String.format("%.1f km", distanceKm)
                else -> "${distanceKm.toInt()} km"
            }
        }
    }
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.getDefault())
    
    // Structured coroutine scope for background operations (replaces GlobalScope)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // State flow for real-time location updates
    private val _locationState = MutableStateFlow<LocationState>(LocationState.Idle)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()
    
    // Cached location
    private var cachedLocation: LocationInfo? = null
    private var lastLocationTime: Long = 0
    private val CACHE_DURATION = 24 * 60 * 60 * 1000L // 24 hours cache for persistent location
    
    // Accuracy threshold in meters - only accept locations more accurate than this
    private val ACCURACY_THRESHOLD = 100f // 100 meters

    /**
     * Check if app has location permission
     */
    fun hasLocationPermission(): Boolean {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasPermission = hasFineLocation || hasCoarseLocation
        Timber.d("📍 LOCATION SERVICE: hasLocationPermission = $hasPermission (fine=$hasFineLocation, coarse=$hasCoarseLocation)")
        return hasPermission
    }

    /**
     * Check if location services are enabled on device
     */
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        Timber.d("📍 LOCATION SERVICE: GPS enabled = $gpsEnabled, Network enabled = $networkEnabled")
        return gpsEnabled || networkEnabled
    }
    
    /**
     * Get cached location if still valid
     */
    fun getCachedLocation(): LocationInfo? {
        val now = System.currentTimeMillis()
        return if (cachedLocation != null && (now - lastLocationTime) < CACHE_DURATION) {
            Timber.d("📍 LOCATION SERVICE: Returning cached location")
            cachedLocation
        } else {
            null
        }
    }

    /**
     * Get current location with improved accuracy and error handling
     * Uses high accuracy priority and falls back to balanced if needed
     */
    suspend fun getCurrentLocation(): LocationInfo? {
        Timber.d("📍 LOCATION SERVICE: getCurrentLocation() called")
        _locationState.value = LocationState.Loading
        
        // Skip cache for fresh location - user wants accurate location
        // getCachedLocation()?.let { cached ->
        //     _locationState.value = LocationState.Success(cached)
        //     return cached
        // }
        
        if (!hasLocationPermission()) {
            Timber.w("📍 LOCATION SERVICE: No location permission")
            _locationState.value = LocationState.Error("Location permission not granted")
            return null
        }
        
        if (!isLocationEnabled()) {
            Timber.w("📍 LOCATION SERVICE: Location not enabled")
            _locationState.value = LocationState.Error("Please enable location services")
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }

            try {
                Timber.d("📍 LOCATION SERVICE: Requesting current location with HIGH_ACCURACY...")
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location ->
                    Timber.d("📍 LOCATION SERVICE: Location callback received")
                    if (location != null) {
                        Timber.d("📍 LOCATION SERVICE: Raw location - lat: ${location.latitude}, lon: ${location.longitude}, accuracy: ${location.accuracy}m")
                        processLocation(location.latitude, location.longitude) { locationInfo ->
                            if (locationInfo != null) {
                                cachedLocation = locationInfo
                                lastLocationTime = System.currentTimeMillis()
                                _locationState.value = LocationState.Success(locationInfo)
                            } else {
                                _locationState.value = LocationState.Error("Failed to get address")
                            }
                            continuation.resume(locationInfo)
                        }
                    } else {
                        Timber.w("📍 LOCATION SERVICE: Location is null, trying last known location...")
                        // Try last known location as fallback
                        tryLastKnownLocation { fallbackLocation ->
                            if (fallbackLocation != null) {
                                _locationState.value = LocationState.Success(fallbackLocation)
                            } else {
                                _locationState.value = LocationState.Error("Unable to get location")
                            }
                            continuation.resume(fallbackLocation)
                        }
                    }
                }.addOnFailureListener { e ->
                    Timber.e(e, "📍 LOCATION SERVICE: Failed to get location, trying fallback...")
                    // Try last known location as fallback
                    tryLastKnownLocation { fallbackLocation ->
                        if (fallbackLocation != null) {
                            _locationState.value = LocationState.Success(fallbackLocation)
                        } else {
                            _locationState.value = LocationState.Error("Location unavailable: ${e.message}")
                        }
                        continuation.resume(fallbackLocation)
                    }
                }
            } catch (e: SecurityException) {
                Timber.e(e, "📍 LOCATION SERVICE: Security exception")
                _locationState.value = LocationState.Error("Location permission denied")
                continuation.resume(null)
            } catch (e: Exception) {
                Timber.e(e, "📍 LOCATION SERVICE: Unexpected error")
                _locationState.value = LocationState.Error("Unexpected error: ${e.message}")
                continuation.resume(null)
            }
        }
    }
    
    /**
     * Get high accuracy location by requesting multiple updates and picking the best one
     * This method waits for GPS to get a fix and returns the most accurate location
     * Like Swiggy/Zomato - uses continuous updates to get the best possible accuracy
     * 
     * PRECISION TARGETS:
     * - Target accuracy: 5-10 meters (GPS-level precision)
     * - Timeout: 10-15 seconds max to balance accuracy vs user experience
     * 
     * @param timeoutMs Maximum time to wait for accurate location (default 15 seconds)
     * @param minAccuracyMeters Minimum accuracy required (default 10 meters for GPS precision)
     */
    @Suppress("MissingPermission")
    suspend fun getHighAccuracyLocation(
        timeoutMs: Long = 15000L,
        minAccuracyMeters: Float = 10f
    ): LocationInfo? {
        Timber.d("📍 LOCATION SERVICE: getHighAccuracyLocation() called (timeout: ${timeoutMs}ms, targetAccuracy: ${minAccuracyMeters}m)")
        _locationState.value = LocationState.Loading
        
        if (!hasLocationPermission()) {
            Timber.w("📍 LOCATION SERVICE: No location permission")
            _locationState.value = LocationState.Error("Location permission not granted")
            return null
        }
        
        if (!isLocationEnabled()) {
            Timber.w("📍 LOCATION SERVICE: Location not enabled")
            _locationState.value = LocationState.Error("Please enable location services")
            return null
        }
        
        return suspendCancellableCoroutine { continuation ->
            var bestLocation: android.location.Location? = null
            var hasResumed = false
            val startTime = System.currentTimeMillis()
            
            // Ultra-aggressive location request for 5-10m GPS precision
            // Request updates every 100ms to get the freshest GPS data
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100L)
                .setMinUpdateIntervalMillis(50L) // Fastest possible update interval
                .setMaxUpdateDelayMillis(200L) // Minimal delay for batching
                .setMinUpdateDistanceMeters(0f) // Update even for tiny movements
                .setWaitForAccurateLocation(true) // Wait for GPS fix
                .setMaxUpdates(150) // Allow many updates within timeout window
                .build()
            
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val elapsedTime = System.currentTimeMillis() - startTime
                    
                    result.lastLocation?.let { location ->
                        Timber.d("📍 LOCATION SERVICE: GPS update [${elapsedTime}ms] - lat: ${location.latitude}, lon: ${location.longitude}, accuracy: ${location.accuracy}m, provider: ${location.provider}")
                        
                        // Keep the most accurate location
                        if (bestLocation == null || location.accuracy < bestLocation!!.accuracy) {
                            bestLocation = location
                            Timber.d("📍 LOCATION SERVICE: 🎯 New best location with accuracy: ${location.accuracy}m")
                        }
                        
                        // Return immediately if we hit target accuracy (5-10m)
                        if (location.accuracy <= minAccuracyMeters && !hasResumed) {
                            hasResumed = true
                            fusedLocationClient.removeLocationUpdates(this)
                            Timber.d("📍 LOCATION SERVICE: ✅ Got GPS-precise location (${location.accuracy}m) in ${elapsedTime}ms!")
                            processLocationWithAccuracy(location.latitude, location.longitude, location.accuracy) { locationInfo ->
                                if (locationInfo != null) {
                                    cachedLocation = locationInfo
                                    lastLocationTime = System.currentTimeMillis()
                                    _locationState.value = LocationState.Success(locationInfo)
                                }
                                continuation.resume(locationInfo)
                            }
                        }
                        // Also return early if we have good accuracy (< 15m) after 5 seconds
                        // This balances precision with user experience
                        else if (location.accuracy <= 15f && elapsedTime >= 5000L && !hasResumed) {
                            hasResumed = true
                            fusedLocationClient.removeLocationUpdates(this)
                            Timber.d("📍 LOCATION SERVICE: ✅ Got good accuracy (${location.accuracy}m) after ${elapsedTime}ms, returning...")
                            processLocationWithAccuracy(location.latitude, location.longitude, location.accuracy) { locationInfo ->
                                if (locationInfo != null) {
                                    cachedLocation = locationInfo
                                    lastLocationTime = System.currentTimeMillis()
                                    _locationState.value = LocationState.Success(locationInfo)
                                }
                                continuation.resume(locationInfo)
                            }
                        }
                    }
                }
            }
            
            // Start location updates
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            
            // Set timeout to return best available location (max 15 seconds)
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                if (!hasResumed) {
                    hasResumed = true
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                    
                    if (bestLocation != null) {
                        val finalAccuracy = bestLocation!!.accuracy
                        Timber.d("📍 LOCATION SERVICE: ⏱️ Timeout after ${timeoutMs}ms - returning best location with accuracy: ${finalAccuracy}m")
                        processLocationWithAccuracy(bestLocation!!.latitude, bestLocation!!.longitude, finalAccuracy) { locationInfo ->
                            if (locationInfo != null) {
                                cachedLocation = locationInfo
                                lastLocationTime = System.currentTimeMillis()
                                _locationState.value = LocationState.Success(locationInfo)
                            } else {
                                _locationState.value = LocationState.Error("Failed to get address")
                            }
                            continuation.resume(locationInfo)
                        }
                    } else {
                        Timber.w("📍 LOCATION SERVICE: Timeout - no location received, trying fallback...")
                        tryLastKnownLocation { fallbackLocation ->
                            if (fallbackLocation != null) {
                                _locationState.value = LocationState.Success(fallbackLocation)
                            } else {
                                _locationState.value = LocationState.Error("Unable to get accurate location")
                            }
                            continuation.resume(fallbackLocation)
                        }
                    }
                }
            }, timeoutMs)
            
            continuation.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        }
    }
    
    /**
     * Process raw coordinates into LocationInfo with geocoding and accuracy
     * Uses Android Geocoder for reverse geocoding (fast, no network calls to external APIs)
     */
    private fun processLocationWithAccuracy(latitude: Double, longitude: Double, accuracy: Float, callback: (LocationInfo?) -> Unit) {
        Timber.d("📍 LOCATION SERVICE: Using Android Geocoder for reverse geocoding ($latitude, $longitude)")
        processLocationWithAndroidGeocoder(latitude, longitude, accuracy, callback)
    }
    
    /**
     * Fallback to Android's built-in Geocoder
     */
    private fun processLocationWithAndroidGeocoder(latitude: Double, longitude: Double, accuracy: Float, callback: (LocationInfo?) -> Unit) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Use async geocoder for Android 13+
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    val locationInfo = createLocationInfoWithAccuracy(latitude, longitude, accuracy, addresses.firstOrNull())
                    callback(locationInfo)
                }
            } else {
                // Use sync geocoder for older versions
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val locationInfo = createLocationInfoWithAccuracy(latitude, longitude, accuracy, addresses?.firstOrNull())
                callback(locationInfo)
            }
        } catch (e: Exception) {
            Timber.e(e, "📍 LOCATION SERVICE: Android Geocoder error")
            // Return location with coordinates only if geocoding fails
            val locationInfo = LocationInfo(
                latitude = latitude,
                longitude = longitude,
                address = "Lat: ${String.format("%.6f", latitude)}, Lon: ${String.format("%.6f", longitude)}",
                city = "",
                area = "",
                state = "",
                postalCode = "",
                accuracy = accuracy
            )
            callback(locationInfo)
        }
    }
    
    /**
     * Create LocationInfo from coordinates and address with detailed extraction and accuracy
     */
    private fun createLocationInfoWithAccuracy(latitude: Double, longitude: Double, accuracy: Float, address: Address?): LocationInfo {
        return if (address != null) {
            // Extract detailed address components
            val city = address.locality ?: address.subAdminArea ?: ""
            val area = address.subLocality ?: ""
            val streetName = address.thoroughfare ?: ""
            val buildingName = address.subThoroughfare ?: address.featureName ?: ""
            val district = address.subAdminArea ?: ""
            val state = address.adminArea ?: ""
            val postalCode = address.postalCode ?: ""
            val country = address.countryName ?: "India"
            
            // Try to extract landmark from premises or feature name
            val landmark = address.premises ?: ""
            
            LocationInfo(
                latitude = latitude,
                longitude = longitude,
                address = getFormattedAddress(address),
                city = city,
                area = area,
                state = state,
                postalCode = postalCode,
                streetName = streetName,
                buildingName = buildingName,
                landmark = landmark,
                district = district,
                country = country,
                accuracy = accuracy
            ).also {
                Timber.d("📍 LOCATION SERVICE: ✅ Detailed location info created:")
                Timber.d("📍   - Latitude: ${it.latitude}")
                Timber.d("📍   - Longitude: ${it.longitude}")
                Timber.d("📍   - Accuracy: ${it.accuracy}m")
                Timber.d("📍   - Full Address: ${it.address}")
                Timber.d("📍   - City: ${it.city}")
                Timber.d("📍   - Area: ${it.area}")
            }
        } else {
            LocationInfo(
                latitude = latitude,
                longitude = longitude,
                address = "Location found",
                city = "",
                area = "",
                state = "",
                postalCode = "",
                accuracy = accuracy
            )
        }
    }

    /**
     * Try to get last known location as fallback
     */
    @Suppress("MissingPermission")
    private fun tryLastKnownLocation(callback: (LocationInfo?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(null)
            return
        }
        
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                Timber.d("📍 LOCATION SERVICE: Using last known location - lat: ${location.latitude}, lon: ${location.longitude}")
                processLocation(location.latitude, location.longitude, callback)
            } else {
                Timber.w("📍 LOCATION SERVICE: No last known location available")
                callback(null)
            }
        }.addOnFailureListener { e ->
            Timber.e(e, "📍 LOCATION SERVICE: Failed to get last known location")
            callback(null)
        }
    }
    
    /**
     * Process raw coordinates into LocationInfo with geocoding
     * Uses Azure Maps as primary, Android Geocoder as fallback
     */
    private fun processLocation(latitude: Double, longitude: Double, callback: (LocationInfo?) -> Unit) {
        // Delegate to the accuracy version with 0 accuracy
        processLocationWithAccuracy(latitude, longitude, 0f, callback)
    }
    
    /**
     * Create LocationInfo from coordinates and address with detailed extraction
     */
    private fun createLocationInfo(latitude: Double, longitude: Double, address: Address?): LocationInfo {
        return if (address != null) {
            // Extract detailed address components
            val city = address.locality ?: address.subAdminArea ?: ""
            val area = address.subLocality ?: ""
            val streetName = address.thoroughfare ?: ""
            val buildingName = address.subThoroughfare ?: address.featureName ?: ""
            val district = address.subAdminArea ?: ""
            val state = address.adminArea ?: ""
            val postalCode = address.postalCode ?: ""
            val country = address.countryName ?: "India"
            
            // Try to extract landmark from premises or feature name
            val landmark = address.premises ?: ""
            
            LocationInfo(
                latitude = latitude,
                longitude = longitude,
                address = getFormattedAddress(address),
                city = city,
                area = area,
                state = state,
                postalCode = postalCode,
                streetName = streetName,
                buildingName = buildingName,
                landmark = landmark,
                district = district,
                country = country
            ).also {
                Timber.d("📍 LOCATION SERVICE: ✅ Detailed location info created:")
                Timber.d("📍   - Latitude: ${it.latitude}")
                Timber.d("📍   - Longitude: ${it.longitude}")
                Timber.d("📍   - Full Address: ${it.address}")
                Timber.d("📍   - City: ${it.city}")
                Timber.d("📍   - Area: ${it.area}")
                Timber.d("📍   - Street: ${it.streetName}")
                Timber.d("📍   - Building: ${it.buildingName}")
                Timber.d("📍   - District: ${it.district}")
                Timber.d("📍   - State: ${it.state}")
                Timber.d("📍   - Postal Code: ${it.postalCode}")
            }
        } else {
            LocationInfo(
                latitude = latitude,
                longitude = longitude,
                address = "Location found",
                city = "",
                area = "",
                state = "",
                postalCode = ""
            )
        }
    }

    /**
     * Format address for display - COMPLETE address like Swiggy/Zomato/Flipkart
     * Includes: Building, Street, Area, Landmark, City, District, State, PIN
     */
    private fun getFormattedAddress(address: Address): String {
        val addressParts = mutableListOf<String>()

        // Building/House number
        address.subThoroughfare?.let { addressParts.add(it) }
        // Street/Road name
        address.thoroughfare?.let { addressParts.add(it) }
        // Premises/Building name
        address.premises?.let { addressParts.add(it) }
        // Sub-locality/Area/Neighborhood
        address.subLocality?.let { addressParts.add(it) }
        // Feature name (landmark)
        address.featureName?.takeIf { it != address.subThoroughfare }?.let { addressParts.add(it) }
        // Locality/City
        address.locality?.let { addressParts.add(it) }
        // Sub-admin area (District)
        address.subAdminArea?.takeIf { it != address.locality }?.let { addressParts.add(it) }
        // Admin area (State)
        address.adminArea?.let { addressParts.add(it) }
        // Postal code (PIN)
        address.postalCode?.let { addressParts.add(it) }

        return if (addressParts.isNotEmpty()) {
            addressParts.joinToString(", ")
        } else {
            // Fallback to full address line from geocoder
            address.getAddressLine(0) ?: "Unknown location"
        }
    }

    /**
     * Get real-time location updates as a Flow
     * Useful for tracking location changes
     */
    @Suppress("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 10000L): Flow<LocationInfo> = callbackFlow {
        if (!hasLocationPermission()) {
            Timber.w("📍 LOCATION SERVICE: No permission for location updates")
            close()
            return@callbackFlow
        }
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMaxUpdateDelayMillis(intervalMs * 2)
            .build()
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Timber.d("📍 LOCATION SERVICE: Location update - lat: ${location.latitude}, lon: ${location.longitude}")
                    processLocation(location.latitude, location.longitude) { locationInfo ->
                        locationInfo?.let { 
                            cachedLocation = it
                            lastLocationTime = System.currentTimeMillis()
                            trySend(it)
                        }
                    }
                }
            }
        }
        
        Timber.d("📍 LOCATION SERVICE: Starting location updates (interval: ${intervalMs}ms)")
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        
        awaitClose {
            Timber.d("📍 LOCATION SERVICE: Stopping location updates")
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
    
    /**
     * Get coordinates from an address string (geocoding)
     * Returns LocationInfo with coordinates if successful, null otherwise
     */
    suspend fun getCoordinatesFromAddress(addressString: String): LocationInfo? {
        if (addressString.isBlank()) {
            Timber.w("📍 LOCATION SERVICE: Empty address string")
            return null
        }
        
        return suspendCancellableCoroutine { continuation ->
            try {
                Timber.d("📍 LOCATION SERVICE: Geocoding address: $addressString")
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Use async geocoder for Android 13+
                    geocoder.getFromLocationName(addressString, 1) { addresses ->
                        val locationInfo = addresses.firstOrNull()?.let { address ->
                            LocationInfo(
                                latitude = address.latitude,
                                longitude = address.longitude,
                                address = addressString,
                                city = address.locality ?: address.subAdminArea ?: "",
                                area = address.subLocality ?: address.thoroughfare ?: "",
                                state = address.adminArea ?: "",
                                postalCode = address.postalCode ?: ""
                            )
                        }
                        if (locationInfo != null) {
                            Timber.d("📍 LOCATION SERVICE: ✅ Geocoded successfully - lat: ${locationInfo.latitude}, lon: ${locationInfo.longitude}")
                        } else {
                            Timber.w("📍 LOCATION SERVICE: No results for address: $addressString")
                        }
                        continuation.resume(locationInfo)
                    }
                } else {
                    // Use sync geocoder for older versions
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(addressString, 1)
                    val locationInfo = addresses?.firstOrNull()?.let { address ->
                        LocationInfo(
                            latitude = address.latitude,
                            longitude = address.longitude,
                            address = addressString,
                            city = address.locality ?: address.subAdminArea ?: "",
                            area = address.subLocality ?: address.thoroughfare ?: "",
                            state = address.adminArea ?: "",
                            postalCode = address.postalCode ?: ""
                        )
                    }
                    if (locationInfo != null) {
                        Timber.d("📍 LOCATION SERVICE: ✅ Geocoded successfully - lat: ${locationInfo.latitude}, lon: ${locationInfo.longitude}")
                    } else {
                        Timber.w("📍 LOCATION SERVICE: No results for address: $addressString")
                    }
                    continuation.resume(locationInfo)
                }
            } catch (e: Exception) {
                Timber.e(e, "📍 LOCATION SERVICE: Geocoding error for: $addressString")
                continuation.resume(null)
            }
        }
    }
    
    /**
     * Calculate distance between two coordinates using Haversine formula
     * @return Distance in kilometers
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Format distance for display
     */
    fun formatDistance(distanceKm: Double): String {
        return when {
            distanceKm < 0.1 -> "< 100m"
            distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()}m"
            distanceKm < 10.0 -> String.format("%.1f km", distanceKm)
            else -> "${distanceKm.toInt()} km"
        }
    }
    
    /**
     * Clear cached location
     */
    fun clearCache() {
        cachedLocation = null
        lastLocationTime = 0
        _locationState.value = LocationState.Idle
    }
    
    /**
     * Convert LocationInfo to LocationData for storage
     */
    fun toLocationData(locationInfo: LocationInfo): com.example.dutype.models.LocationData {
        return com.example.dutype.models.LocationData(
            address = locationInfo.address,
            latitude = locationInfo.latitude,
            longitude = locationInfo.longitude,
            city = locationInfo.city,
            state = locationInfo.state,
            country = locationInfo.country,
            postalCode = locationInfo.postalCode,
            area = locationInfo.area,
            landmark = locationInfo.landmark,
            streetName = locationInfo.streetName,
            buildingName = locationInfo.buildingName,
            district = locationInfo.district,
            accuracy = locationInfo.accuracy,
            timestamp = locationInfo.timestamp
        )
    }
    
    /**
     * Get high accuracy location and return as LocationData
     * Convenience method for screens that need LocationData directly
     * 
     * @param timeoutMs Maximum time to wait (default 15 seconds)
     * @param minAccuracyMeters Target accuracy (default 10 meters for GPS precision)
     */
    suspend fun getHighAccuracyLocationData(
        timeoutMs: Long = 15000L,
        minAccuracyMeters: Float = 10f
    ): com.example.dutype.models.LocationData? {
        val locationInfo = getHighAccuracyLocation(timeoutMs, minAccuracyMeters)
        return locationInfo?.let { toLocationData(it) }
    }
}
