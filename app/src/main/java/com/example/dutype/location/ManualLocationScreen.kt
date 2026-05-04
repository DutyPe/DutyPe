package com.example.dutype.location

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.navigation.Routes
import com.example.dutype.components.ReusableSearchBar
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.compose.ui.res.stringResource
import com.dutype.app.R


@Composable
fun ManualLocationScreen(navController: NavController) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    // LocationPreferences and LocationService accessed via FirestoreJobViewModel (proper DI pattern)
    val jobViewModel: com.example.dutype.viewmodels.FirestoreJobViewModel = hiltViewModel()
    val locationPreferences = jobViewModel.locationPreferences
    val locationService = jobViewModel.locationService
    val scope = rememberCoroutineScope()
    
    var searchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<LocationSuggestion>>(emptyList()) }
    var isVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isFetchingCurrentLocation by remember { mutableStateOf(false) }

    // Runs the actual GPS + reverse-geocode + navigate flow. Split out so the
    // permission launcher can invoke the exact same path once the user grants.
    suspend fun fetchAndUseCurrentLocation() {
        isFetchingCurrentLocation = true
        errorMessage = ""
        try {
            val locationInfo = locationService.getHighAccuracyLocation(
                timeoutMs = 15000L,
                minAccuracyMeters = 10f
            )
            if (locationInfo != null && (locationInfo.latitude != 0.0 || locationInfo.longitude != 0.0)) {
                val finalLocationData = locationService.toLocationData(locationInfo)
                locationPreferences.savePreferredLocation(finalLocationData)
                locationPreferences.setPermissionGranted(true)
                Timber.d("📍 Location saved: ${finalLocationData.getShortAddress()}")
                navController.navigate(Routes.WORKER_HOME) {
                    popUpTo(Routes.MANUAL_LOCATION_ROUTE) { inclusive = true }
                }
            } else {
                errorMessage = "Could not fetch location. Check GPS settings."
            }
        } catch (e: Exception) {
            Timber.e(e, "Location fetch error")
            errorMessage = "Error: ${e.message}"
        } finally {
            isFetchingCurrentLocation = false
        }
    }

    // Re-prompt system permission dialog whenever the user taps "current location"
    // without having granted access yet (including after a previous denial).
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch { fetchAndUseCurrentLocation() }
        } else {
            errorMessage = "Location permission required to use current location."
        }
    }
    
    // Search effect
    LaunchedEffect(searchText) {
        if (searchText.isNotEmpty() && searchText.length >= 2) {
            delay(300)
            isSearching = true
            errorMessage = ""
            
            val placeResults = locationService.searchPlaces(searchText, maxResults = 8)
                .map { it.toLocationSuggestion() }
            if (placeResults.isNotEmpty()) {
                suggestions = placeResults
                errorMessage = ""
            } else {
                suggestions = emptyList()
                errorMessage = "No locations found. Try a different search."
            }
            isSearching = false
        } else {
            suggestions = emptyList()
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.dutype.ui.theme.WorkerColors.CardBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Common Header for consistency
            CommonHeader(
                title = stringResource(R.string.select_location),
                subtitle = stringResource(R.string.choose_preferred_location),
                onBackClick = { navController.popBackStack() },
                backgroundColor = Color.White
            )
            
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Search Bar
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(800)) + slideInVertically(tween(800), initialOffsetY = { -it / 4 })
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        ReusableSearchBar(
                            query = searchText,
                            onQueryChange = { searchText = it },
                            placeholder = stringResource(R.string.search_area_street_city),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Current Location Option
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1000, 400)) + slideInVertically(tween(1000, 400), initialOffsetY = { it / 4 })
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .shadow(8.dp, RoundedCornerShape(14.dp))
                            .clickable(enabled = !isFetchingCurrentLocation) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (locationService.hasLocationPermission()) {
                                    scope.launch { fetchAndUseCurrentLocation() }
                                } else {
                                    // Re-prompt the OS permission dialog even after a previous denial.
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (isFetchingCurrentLocation) Color(0xFF4CAF50).copy(alpha = 0.25f)
                                        else Color(0xFF4CAF50).copy(alpha = 0.15f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isFetchingCurrentLocation) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color(0xFF4CAF50),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Use Current Location",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
                                )
                                Text(
                                    text = if (isFetchingCurrentLocation) "Getting your location..." else "Using GPS",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF757575))
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFBDBDBD))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))


                // Search Results Section
                AnimatedVisibility(
                    visible = isVisible && (searchText.isNotEmpty() || suggestions.isNotEmpty()),
                    enter = fadeIn(tween(600))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column {
                            if (searchText.isNotEmpty()) {
                                Text(
                                    text = "Search Results",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1976D2),
                                        fontSize = 16.sp
                                    ),
                                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                                )
                            }

                            // Error message
                            if (errorMessage.isNotEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Error, contentDescription = "Error", tint = Color(0xFFD32F2F), modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                                        Text(
                                            text = errorMessage,
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFD32F2F), fontSize = 13.sp)
                                        )
                                    }
                                }
                            }

                            // Loading indicator
                            if (isSearching) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF1976D2), modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                                }
                            } else {
                                // Suggestions list
                                LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                                    items(suggestions) { suggestion ->
                                        LocationSuggestionItem(
                                            suggestion = suggestion,
                                            onSelected = {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                // Save full location data with coordinates
                                                val locationData = com.example.dutype.models.LocationData(
                                                    address = suggestion.displayName,
                                                    latitude = suggestion.latitude,
                                                    longitude = suggestion.longitude,
                                                    city = suggestion.city,
                                                    state = suggestion.state,
                                                    country = suggestion.country,
                                                    area = suggestion.area.ifEmpty { suggestion.city },
                                                    timestamp = System.currentTimeMillis()
                                                )
                                                locationPreferences.savePreferredLocation(locationData)
                                                Timber.d("📍 Location selected: ${suggestion.displayName}")
                                                Timber.d("📍   City: ${suggestion.city}, State: ${suggestion.state}")
                                                Timber.d("📍   Coords: lat=${suggestion.latitude}, lon=${suggestion.longitude}")
                                                navController.navigate(Routes.WORKER_HOME) {
                                                    popUpTo(Routes.MANUAL_LOCATION_ROUTE) { inclusive = true }
                                                }
                                            }
                                        )
                                    }
                                    
                                    if (suggestions.isEmpty() && searchText.isNotEmpty() && !isSearching && errorMessage.isEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(Icons.Default.LocationOff, contentDescription = "No results", tint = Color(0xFFBDBDBD), modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.ExtraLarge))
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(text = stringResource(R.string.no_locations_found), style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF9E9E9E)), textAlign = TextAlign.Center)
                                                    Text(text = stringResource(R.string.try_different_search), style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFBDBDBD)), textAlign = TextAlign.Center)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
            }
        }
    }
}


@Composable
fun LocationSuggestionItem(
    suggestion: LocationSuggestion,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFE3F2FD), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.city.ifEmpty { suggestion.displayName.split(",").firstOrNull() ?: suggestion.displayName },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A)),
                maxLines = 1
            )
            Text(
                text = suggestion.displayName,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF757575)),
                maxLines = 2
            )
        }
        Icon(Icons.Default.NorthWest, contentDescription = null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Small))
    }
}

data class LocationSuggestion(
    val placeId: String,
    val displayName: String,
    val city: String,
    val state: String,
    val country: String,
    val area: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

private fun com.example.dutype.models.PlaceSuggestion.toLocationSuggestion(): LocationSuggestion {
    val firstPart = description.substringBefore(",").trim()
    return LocationSuggestion(
        placeId = placeId,
        displayName = description,
        city = "",
        state = "",
        country = "India",
        area = firstPart,
        latitude = latitude,
        longitude = longitude
    )
}
