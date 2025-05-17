//package com.example.jobsnearby.components
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.content.Intent
//import android.net.Uri
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.unit.dp
//import com.google.android.gms.maps.CameraUpdateFactory
//import com.google.android.gms.maps.model.CameraPosition
//import com.google.android.gms.maps.model.LatLng
//import com.google.maps.android.compose.*
//
//@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
//@Composable
//fun GoogleMapsCard(
//    context: Context,
//    selectedLocation: LatLng?,
//    modifier: Modifier = Modifier,
//    selectedShopName: String,
//    vacancies: String
//) {
//    Card(
//        modifier = modifier
//            .fillMaxWidth()
//            .height(233.dp), // Reduced height
//        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
//        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
//        shape = RoundedCornerShape(0.dp) // Flat edge for full width (optional)
//    ) {
//        Box(modifier = Modifier.fillMaxSize()) {
//            GoogleMapView(selectedLocation, selectedShopName, vacancies)
//
//            // Overlay on top of the map
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .align(Alignment.TopCenter)
//                    .padding(top = 12.dp), // Only vertical padding
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Text(
//                    text = "📍 Job Location",
//                    style = MaterialTheme.typography.titleMedium,
//                    color = Color.White
//                )
//            }
//
//            if (selectedLocation != null) {
//                Button(
//                    onClick = {
//                        val uri = "google.navigation:q=${selectedLocation.latitude},${selectedLocation.longitude}"
//                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
//                        intent.setPackage("com.google.android.apps.maps")
//                        context.startActivity(intent)
//                    },
//                    shape = RoundedCornerShape(10.dp),
//                    modifier = Modifier
//                        .align(Alignment.BottomCenter)
//                        .padding(bottom = 12.dp) // Only bottom padding
//                ) {
//                    Text("Navigate 🚗")
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun GoogleMapView(
//    selectedLocation: LatLng?,
//    selectedShopName: String,
//    vacancies: String
//) {
//    val defaultLocation = LatLng(17.3850, 78.4867) // Hyderabad coordinates
//    val cameraPositionState = rememberCameraPositionState {
//        position = CameraPosition.fromLatLngZoom(selectedLocation ?: defaultLocation, 5f)
//    }
//
//    val markerState = remember {
//        MarkerState(position = selectedLocation ?: defaultLocation)
//    }
//
//    LaunchedEffect(selectedLocation) {
//        selectedLocation?.let {
//            markerState.position = it
//            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 15f))
//            markerState.showInfoWindow()
//        }
//    }
//
//    GoogleMap(
//        modifier = Modifier
//            .fillMaxSize(), // Full width and height inside card
//        cameraPositionState = cameraPositionState
//    ) {
//        selectedLocation?.let {
//            Marker(
//                state = markerState,
//                title = selectedShopName,
//                snippet = "Vacancies: $vacancies"
//            )
//        }
//    }
//}
