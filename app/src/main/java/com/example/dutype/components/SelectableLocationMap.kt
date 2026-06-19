package com.example.dutype.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.GeoUtils
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.abs

@Composable
fun SelectableLocationMap(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
    markerTitle: String,
    markerSnippet: String? = null,
    onLocationPicked: (latitude: Double, longitude: Double) -> Unit
) {
    if (!GeoUtils.hasValidCoordinates(latitude, longitude)) {
        return
    }

    key(latitude, longitude) {
        val selectedPoint = LatLng(latitude, longitude)
        var isMapLoaded by remember(latitude, longitude) { mutableStateOf(false) }
        var showFallback by remember(latitude, longitude) { mutableStateOf(false) }
        val markerState = remember(latitude, longitude) { MarkerState(position = selectedPoint) }
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(selectedPoint, 16f)
        }
        val mapProperties = remember { MapProperties(isBuildingEnabled = true) }
        val mapUiSettings = remember {
            MapUiSettings(
                compassEnabled = true,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false,
                rotationGesturesEnabled = false,
                scrollGesturesEnabled = true,
                tiltGesturesEnabled = false,
                zoomControlsEnabled = false,
                zoomGesturesEnabled = true
            )
        }

        LaunchedEffect(markerState.position) {
            val position = markerState.position
            val moved = abs(position.latitude - latitude) > 0.000001 ||
                abs(position.longitude - longitude) > 0.000001
            if (moved) {
                onLocationPicked(position.latitude, position.longitude)
            }
        }

        LaunchedEffect(latitude, longitude, isMapLoaded) {
            showFallback = false
            delay(5000)
            if (!isMapLoaded) {
                showFallback = true
            }
        }

        Box(modifier = modifier) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = mapUiSettings,
                onMapClick = { point ->
                    markerState.position = point
                    onLocationPicked(point.latitude, point.longitude)
                },
                onMapLoaded = { isMapLoaded = true }
            ) {
                Marker(
                    state = markerState,
                    title = markerTitle,
                    snippet = markerSnippet,
                    draggable = true
                )
            }

            if (!isMapLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(WorkerColors.ChipBackground),
                    contentAlignment = Alignment.Center
                ) {
                    if (showFallback) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Map unavailable",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = WorkerColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            )
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.6f", latitude)}, ${String.format(java.util.Locale.US, "%.6f", longitude)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = WorkerColors.TextSecondary,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = WorkerColors.TextPrimary,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}