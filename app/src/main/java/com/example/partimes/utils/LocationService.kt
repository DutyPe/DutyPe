package com.example.partimes.utils

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
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    suspend fun getCurrentLocation(): LocationInfo? {
        if (!hasLocationPermission() || !isLocationEnabled()) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }

            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location ->
                    if (location != null) {
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
                                continuation.resume(locationInfo)
                            } else {
                                continuation.resume(null)
                            }
                        } catch (e: Exception) {
                            continuation.resume(null)
                        }
                    } else {
                        continuation.resume(null)
                    }
                }.addOnFailureListener {
                    continuation.resume(null)
                }
            } catch (e: SecurityException) {
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
