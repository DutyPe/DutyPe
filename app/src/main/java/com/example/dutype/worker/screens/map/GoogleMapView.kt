package com.example.dutype.worker.screens.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.dutype.models.JobListing
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import timber.log.Timber

/**
 * Google Maps Composable for Job Map Screen
 * Shows job markers on a native Google Map
 */
@Composable
fun GoogleMapView(
    modifier: Modifier = Modifier,
    jobs: List<JobListing>,
    userLatitude: Double?,
    userLongitude: Double?,
    initialLatitude: Double = 17.385044,
    initialLongitude: Double = 78.486671,
    initialZoom: Float = 14f,
    onMarkerClick: (JobListing) -> Unit = {},
    onMapReady: () -> Unit = {}
) {
    // Camera position state
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(userLatitude ?: initialLatitude, userLongitude ?: initialLongitude),
            initialZoom
        )
    }
    
    // Update camera when user location changes
    LaunchedEffect(userLatitude, userLongitude) {
        if (userLatitude != null && userLongitude != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(userLatitude, userLongitude),
                    initialZoom
                )
            )
        }
    }
    
    // Map properties - enable my location if permission granted
    val mapProperties by remember(userLatitude, userLongitude) {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = userLatitude != null && userLongitude != null,
                mapType = MapType.NORMAL
            )
        )
    }
    
    // Map UI settings
    val mapUiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false, // We have custom zoom controls
                myLocationButtonEnabled = false, // We have custom my location button
                compassEnabled = true,
                mapToolbarEnabled = false
            )
        )
    }
    
    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = mapUiSettings,
        onMapLoaded = {
            Timber.d("📍 GoogleMap: Map loaded with ${jobs.size} jobs")
            onMapReady()
        }
    ) {
        // Job markers
        jobs.forEach { job ->
            val position = LatLng(job.latitude, job.longitude)
            val isUrgent = job.urgency == "URGENT" || job.urgency == "IMMEDIATE"
            
            Marker(
                state = MarkerState(position = position),
                title = job.title,
                snippet = "${job.companyName.ifEmpty { job.company }} - ${job.payAmount.ifEmpty { "Negotiable" }}",
                icon = BitmapDescriptorFactory.defaultMarker(
                    if (isUrgent) BitmapDescriptorFactory.HUE_RED 
                    else BitmapDescriptorFactory.HUE_GREEN
                ),
                onClick = {
                    Timber.d("📍 GoogleMap: Marker clicked - ${job.title}")
                    onMarkerClick(job)
                    true // Consume the click
                }
            )
        }
    }
}
