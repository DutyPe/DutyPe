package com.example.dutype.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
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

@SuppressLint("MissingPermission")
suspend fun fetchUserLocation(context: Context): String {
    val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    return suspendCancellableCoroutine { continuation ->
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            val address = if (location != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                addresses?.firstOrNull()?.getAddressLine(0) ?: "Location unavailable"
            } else {
                "Location unavailable"
            }
            continuation.resume(address)
        }.addOnFailureListener {
            Log.e("Location", "Failed to get location", it)
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
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val address = addresses?.firstOrNull()
                    
                    val locationData = UserLocationData(
                        address = address?.getAddressLine(0) ?: "Location unavailable",
                        latitude = location.latitude,
                        longitude = location.longitude,
                        city = address?.locality ?: address?.subAdminArea,
                        state = address?.adminArea
                    )
                    continuation.resume(locationData)
                } catch (e: Exception) {
                    Log.e("Location", "Failed to geocode location", e)
                    // Return coordinates even if geocoding fails
                    continuation.resume(UserLocationData(
                        address = "Location unavailable",
                        latitude = location.latitude,
                        longitude = location.longitude,
                        city = null,
                        state = null
                    ))
                }
            } else {
                continuation.resume(null)
            }
        }.addOnFailureListener {
            Log.e("Location", "Failed to get location", it)
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
 * Format distance for display
 * @param distanceKm Distance in kilometers
 * @return Formatted string (e.g., "2.5" for 2.5km, "15" for 15km)
 */
fun formatDistance(distanceKm: Double): String {
    return when {
        distanceKm < 1.0 -> String.format("%.1f", distanceKm)
        distanceKm < 10.0 -> String.format("%.1f", distanceKm)
        else -> String.format("%.0f", distanceKm)
    }
}

