package com.example.dutype.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.Locale
import kotlin.coroutines.resume

data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val city: String,
    val area: String
)

class LocationService(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.getDefault())

    fun hasLocationPermission(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        Timber.d("📍 LOCATION SERVICE: hasLocationPermission = $hasPermission")
        return hasPermission
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        Timber.d("📍 LOCATION SERVICE: GPS enabled = $gpsEnabled, Network enabled = $networkEnabled")
        return gpsEnabled || networkEnabled
    }

    suspend fun getCurrentLocation(): LocationInfo? {
        Timber.d("📍 LOCATION SERVICE: getCurrentLocation() called")
        
        if (!hasLocationPermission()) {
            Timber.w("📍 LOCATION SERVICE: No location permission")
            return null
        }
        
        if (!isLocationEnabled()) {
            Timber.w("📍 LOCATION SERVICE: Location not enabled")
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }

            try {
                Timber.d("📍 LOCATION SERVICE: Requesting current location...")
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location ->
                    Timber.d("📍 LOCATION SERVICE: Location callback received")
                    if (location != null) {
                        Timber.d("📍 LOCATION SERVICE: Raw location - lat: ${location.latitude}, lon: ${location.longitude}")
                        try {
                            val addresses = geocoder.getFromLocation(
                                location.latitude,
                                location.longitude,
                                1
                            )

                            if (!addresses.isNullOrEmpty()) {
                                val address = addresses[0]
                                val locationInfo = LocationInfo(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    address = getFormattedAddress(address),
                                    city = address.locality ?: address.subAdminArea ?: "",
                                    area = address.subLocality ?: address.thoroughfare ?: ""
                                )
                                Timber.d("📍 LOCATION SERVICE: ✅ Location info created:")
                                Timber.d("📍   - Latitude: ${locationInfo.latitude}")
                                Timber.d("📍   - Longitude: ${locationInfo.longitude}")
                                Timber.d("📍   - Address: ${locationInfo.address}")
                                Timber.d("📍   - City: ${locationInfo.city}")
                                Timber.d("📍   - Area: ${locationInfo.area}")
                                continuation.resume(locationInfo)
                            } else {
                                Timber.w("📍 LOCATION SERVICE: Geocoder returned empty addresses")
                                continuation.resume(null)
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "📍 LOCATION SERVICE: Geocoder error")
                            continuation.resume(null)
                        }
                    } else {
                        Timber.w("📍 LOCATION SERVICE: Location is null")
                        continuation.resume(null)
                    }
                }.addOnFailureListener { e ->
                    Timber.e(e, "📍 LOCATION SERVICE: Failed to get location")
                    continuation.resume(null)
                }
            } catch (e: SecurityException) {
                Timber.e(e, "📍 LOCATION SERVICE: Security exception")
                continuation.resume(null)
            }
        }
    }

    private fun getFormattedAddress(address: Address): String {
        val addressParts = mutableListOf<String>()

        address.subThoroughfare?.let { addressParts.add(it) }
        address.thoroughfare?.let { addressParts.add(it) }
        address.subLocality?.let { addressParts.add(it) }
        address.locality?.let { addressParts.add(it) }

        return addressParts.joinToString(", ")
    }
}
