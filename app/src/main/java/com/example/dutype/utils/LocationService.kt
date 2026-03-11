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
 * Location information data class - simplified for job search
 * Matches LocationData structure for easy conversion
 */
data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val city: String,
    val area: String,
    val state: String = "",
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
     * Get medium display address (Area, City, State)
     * Example: "Nallagandla, Serilingampalle, Telangana"
     */
    fun getMediumAddress(): String {
        val parts = mutableListOf<String>()
        if (area.isNotBlank()) parts.add(area)
        if (city.isNotBlank()) parts.add(city)
        if (state.isNotBlank()) parts.add(state)
        return if (parts.isNotEmpty()) parts.joinToString(", ") else address
    }
    
    /**
     * Get full detailed address
     * Example: "Nallagandla, Serilingampalle, Telangana, India"
     */
    fun getFullAddress(): String {
        val parts = mutableListOf<String>()
        if (area.isNotBlank()) parts.add(area)
        if (city.isNotBlank()) parts.add(city)
        if (state.isNotBlank()) parts.add(state)
        if (country.isNotBlank()) parts.add(country)
        return if (parts.isNotEmpty()) parts.joinToString(", ") else address
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
         * FAST APPROXIMATION: Calculate distance using Euclidean approximation
         * This is 10x FASTER than Haversine - use for initial sorting of large datasets
         * 
         * Accuracy: Within 0.5% for distances < 100km (perfect for job search)
         * Performance: ~10x faster than Haversine formula
         * 
         * Use case: Sort 10,000 jobs by distance, then use exact calculation for display
         * 
         * @param lat1 Latitude of first point
         * @param lon1 Longitude of first point
         * @param lat2 Latitude of second point
         * @param lon2 Longitude of second point
         * @return Approximate distance in kilometers
         */
        fun approximateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            // Euclidean distance with latitude correction
            val dLat = lat2 - lat1
            val dLon = (lon2 - lon1) * Math.cos(Math.toRadians((lat1 + lat2) / 2))
            
            // Convert degrees to kilometers (1 degree ≈ 111 km)
            return Math.sqrt(dLat * dLat + dLon * dLon) * 111.0
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
    private val CACHE_DURATION = 5 * 60 * 1000L // 5 minutes cache (Uber/Swiggy approach)
    
    // Accuracy threshold in meters - only accept locations more accurate than this
    private val ACCURACY_THRESHOLD = 100f // 100 meters
    
    /**
     * UBER/SWIGGY STRATEGY: Get location instantly using hybrid approach
     * 
     * 1. Return last known location immediately (0ms)
     * 2. Start GPS in background to get accurate location
     * 3. Update location when GPS fix is available
     * 
     * This gives instant results while improving accuracy in background
     * 
     * @param onLocationUpdate Callback that may be called twice:
     *        - First with last known location (instant)
     *        - Second with GPS location (after 2-5 seconds)
     */
    suspend fun getLocationFast(
        locationPreferences: com.example.dutype.location.LocationPreferences,
        onLocationUpdate: (LocationInfo?) -> Unit
    ): LocationInfo? {
        Timber.d("📍 FAST LOCATION: Starting hybrid location strategy...")
        
        if (!hasLocationPermission()) {
            Timber.w("📍 FAST LOCATION: No permission")
            locationPreferences.setLoading(false)
            locationPreferences.setError("Location permission not granted")
            return null
        }
        
        if (!isLocationEnabled()) {
            Timber.w("📍 FAST LOCATION: Location disabled")
            locationPreferences.setLoading(false)
            locationPreferences.setError("Please enable location services")
            return null
        }
        
        // Set loading state
        locationPreferences.setLoading(true)
        locationPreferences.setError(null)
        
        // STEP 1: Return cached location immediately (0ms)
        val cached = getCachedLocation()
        if (cached != null) {
            Timber.d("📍 FAST LOCATION: ⚡ Returning cached location instantly (0ms)")
            val locationData = toLocationData(cached)
            locationPreferences.saveLocation(locationData)
            locationPreferences.setLoading(false)
            onLocationUpdate(cached)
            // Don't return yet - continue to get fresh GPS location
        }
        
        // STEP 2: Get last known location (usually < 50ms)
        var lastKnownReturned = false
        withContext(Dispatchers.IO) {
            try {
                @Suppress("MissingPermission")
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null && !lastKnownReturned) {
                        lastKnownReturned = true
                        Timber.d("📍 FAST LOCATION: ⚡ Got last known location (${location.accuracy}m)")
                        processLocationWithAccuracy(location.latitude, location.longitude, location.accuracy) { locationInfo ->
                            if (locationInfo != null) {
                                cachedLocation = locationInfo
                                lastLocationTime = System.currentTimeMillis()
                                
                                // Save to preferences immediately
                                val locationData = toLocationData(locationInfo)
                                locationPreferences.saveLocation(locationData)
                                locationPreferences.setLoading(false)
                                
                                onLocationUpdate(locationInfo)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "📍 FAST LOCATION: Error getting last known location")
            }
        }
        
        // STEP 3: Get fresh GPS location in background (2-5 seconds)
        // This runs in parallel and updates when ready
        serviceScope.launch {
            try {
                val gpsLocation = getHighAccuracyLocation(
                    timeoutMs = 5000L, // 5 second timeout (Swiggy approach)
                    minAccuracyMeters = 20f // Accept 20m accuracy
                )
                if (gpsLocation != null) {
                    Timber.d("📍 FAST LOCATION: 🎯 Got GPS location (${gpsLocation.accuracy}m)")
                    
                    // Save GPS location to preferences
                    val locationData = toLocationData(gpsLocation)
                    locationPreferences.saveLocation(locationData)
                    locationPreferences.setLoading(false)
                    
                    onLocationUpdate(gpsLocation)
                }
            } catch (e: Exception) {
                Timber.e(e, "📍 FAST LOCATION: GPS update failed")
                locationPreferences.setLoading(false)
            }
        }
        
        // Return cached or last known immediately
        return cached
    }

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
            // Extract address components (simplified for job search)
            val city = address.locality ?: address.subAdminArea ?: ""
            val area = address.subLocality ?: ""
            val state = address.adminArea ?: ""
            val country = address.countryName ?: "India"
            
            LocationInfo(
                latitude = latitude,
                longitude = longitude,
                address = getFormattedAddress(address),
                city = city,
                area = area,
                state = state,
                country = country,
                accuracy = accuracy
            ).also {
                Timber.d("📍 LOCATION SERVICE: ✅ Location info created:")
                Timber.d("📍   - Latitude: ${it.latitude}")
                Timber.d("📍   - Longitude: ${it.longitude}")
                Timber.d("📍   - Accuracy: ${it.accuracy}m")
                Timber.d("📍   - City: ${it.city}, Area: ${it.area}")
            }
        } else {
            LocationInfo(
                latitude = latitude,
                longitude = longitude,
                address = "Location found",
                city = "",
                area = "",
                state = "",
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
            // Extract address components (simplified for job search)
            val city = address.locality ?: address.subAdminArea ?: ""
            val area = address.subLocality ?: ""
            val state = address.adminArea ?: ""
            val country = address.countryName ?: "India"
            
            LocationInfo(
                latitude = latitude,
                longitude = longitude,
                address = getFormattedAddress(address),
                city = city,
                area = area,
                state = state,
                country = country
            ).also {
                Timber.d("📍 LOCATION SERVICE: ✅ Location info created:")
                Timber.d("📍   - Latitude: ${it.latitude}")
                Timber.d("📍   - Longitude: ${it.longitude}")
                Timber.d("📍   - City: ${it.city}, Area: ${it.area}")
                Timber.d("📍   - State: ${it.state}")
            }
        } else {
            LocationInfo(
                latitude = latitude,
                longitude = longitude,
                address = "Location found",
                city = "",
                area = "",
                state = ""
            )
        }
    }

    /**
     * Format address for display - simplified for job search
     * Includes: Area, City, State (essential for job matching)
     */
    private fun getFormattedAddress(address: Address): String {
        val addressParts = mutableListOf<String>()

        // Sub-locality/Area/Neighborhood
        address.subLocality?.let { addressParts.add(it) }
        // Locality/City
        address.locality?.let { addressParts.add(it) }
        // Admin area (State)
        address.adminArea?.let { addressParts.add(it) }

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
                                area = address.subLocality ?: "",
                                state = address.adminArea ?: ""
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
                            area = address.subLocality ?: "",
                            state = address.adminArea ?: ""
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
            latitude = locationInfo.latitude,
            longitude = locationInfo.longitude,
            address = locationInfo.address,
            city = locationInfo.city,
            area = locationInfo.area,
            state = locationInfo.state,
            country = locationInfo.country,
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
    
    /**
     * Search for places using Geocoder (location autocomplete)
     * Returns a list of place suggestions based on the search query
     * 
     * @param query The search query (e.g., "Hyderabad", "Gachibowli")
     * @param maxResults Maximum number of results to return (default 5)
     * @return List of PlaceSuggestion objects
     */
    suspend fun searchPlaces(
        query: String,
        maxResults: Int = 5
    ): List<com.example.dutype.models.PlaceSuggestion> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) return@withContext emptyList()
            
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(query, maxResults) { addresses ->
                        continuation.resume(addresses)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(query, maxResults) ?: emptyList()
            }
            
            addresses.mapNotNull { address ->
                val description = buildString {
                    address.featureName?.let { append("$it, ") }
                    address.subLocality?.let { append("$it, ") }
                    address.locality?.let { append("$it, ") }
                    address.adminArea?.let { append("$it, ") }
                    address.countryName?.let { append(it) }
                }.trim().removeSuffix(",")
                
                if (description.isNotBlank() && address.hasLatitude() && address.hasLongitude()) {
                    com.example.dutype.models.PlaceSuggestion(
                        placeId = "${address.latitude},${address.longitude}",
                        description = description,
                        latitude = address.latitude,
                        longitude = address.longitude
                    )
                } else null
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ LocationService: Failed to search places for query: $query")
            emptyList()
        }
    }
}

