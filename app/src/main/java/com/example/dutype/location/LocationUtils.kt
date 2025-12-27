package com.example.dutype.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Data class to hold location information with coordinates
 */
data class UserLocationData(
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val city: String?,
    val state: String?
)

/**
 * Safely perform geocoding with proper error handling
 * Returns null if geocoding fails for any reason
 */
private suspend fun safeGeocode(
    context: Context,
    latitude: Double,
    longitude: Double
): android.location.Address? = withContext(Dispatchers.IO) {
    try {
        // Check if Geocoder is available on this device
        if (!Geocoder.isPresent()) {
            Timber.w("Geocoder is not available on this device")
            return@withContext null
        }
        
        val geocoder = Geocoder(context, Locale.getDefault())
        
        // Use timeout to prevent hanging
        withTimeoutOrNull(5000L) {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()
            } catch (e: IOException) {
                // This catches the "gykk: UNAVAILABLE" and similar Geocoder service errors
                Timber.e(e, "Geocoder IOException - service may be unavailable")
                null
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Invalid coordinates for geocoding: $latitude, $longitude")
                null
            }
        }
    } catch (e: Exception) {
        Timber.e(e, "Unexpected error during geocoding")
        null
    }
}

@SuppressLint("MissingPermission")
suspend fun fetchUserLocation(context: Context): String {
    val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    return suspendCancellableCoroutine { continuation ->
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Launch geocoding in a safe manner
                GlobalScope.launch(Dispatchers.Main) {
                    val address = try {
                        val geocodedAddress = safeGeocode(context, location.latitude, location.longitude)
                        geocodedAddress?.getAddressLine(0) ?: "Location unavailable"
                    } catch (e: Exception) {
                        Timber.e(e, "Error in fetchUserLocation geocoding")
                        "Location unavailable"
                    }
                    if (continuation.isActive) {
                        continuation.resume(address)
                    }
                }
            } else {
                continuation.resume("Location unavailable")
            }
        }.addOnFailureListener { e ->
            Timber.e(e, "Failed to get location")
            continuation.resume("Location unavailable")
        }
    }
}

/**
 * Fetch user location with coordinates for distance calculation
 */
@SuppressLint("MissingPermission")
suspend fun fetchUserLocationWithCoordinates(context: Context): UserLocationData? {
    val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    return suspendCancellableCoroutine { continuation ->
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Launch geocoding in a safe manner
                GlobalScope.launch(Dispatchers.Main) {
                    val locationData = try {
                        val address = safeGeocode(context, location.latitude, location.longitude)
                        
                        UserLocationData(
                            address = address?.getAddressLine(0) ?: "Location unavailable",
                            latitude = location.latitude,
                            longitude = location.longitude,
                            city = address?.locality ?: address?.subAdminArea,
                            state = address?.adminArea
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error in fetchUserLocationWithCoordinates geocoding")
                        // Return coordinates even if geocoding fails
                        UserLocationData(
                            address = "Location unavailable",
                            latitude = location.latitude,
                            longitude = location.longitude,
                            city = null,
                            state = null
                        )
                    }
                    if (continuation.isActive) {
                        continuation.resume(locationData)
                    }
                }
            } else {
                continuation.resume(null)
            }
        }.addOnFailureListener { e ->
            Timber.e(e, "Failed to get location")
            continuation.resume(null)
        }
    }
}

/**
 * Calculate distance between two coordinates using Haversine formula
 * @param lat1 Latitude of first point
 * @param lon1 Longitude of first point
 * @param lat2 Latitude of second point
 * @param lon2 Longitude of second point
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
 * Format distance for display - Swiggy/Zomato style precision
 * @param distanceKm Distance in kilometers
 * @return Formatted string (e.g., "500m", "1.2 km", "15 km")
 */
fun formatDistance(distanceKm: Double): String {
    return when {
        distanceKm < 0.05 -> "< 50m"  // Very close
        distanceKm < 0.1 -> "${(distanceKm * 1000).toInt()}m"  // Show in meters
        distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()}m"  // Show in meters up to 1km
        distanceKm < 10.0 -> String.format("%.1f", distanceKm)  // Show 1 decimal for < 10km
        else -> String.format("%.0f", distanceKm)  // Round for > 10km
    }
}

