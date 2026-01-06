package com.example.dutype.location

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


@Composable
fun ManualLocationScreen(navController: NavController) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    // LocationPreferences and LocationService accessed via FirestoreJobViewModel (proper DI pattern)
    val jobViewModel: com.example.dutype.viewmodels.FirestoreJobViewModel = hiltViewModel()
    val locationPreferences = jobViewModel.locationPreferences
    val locationService = jobViewModel.locationService
    val scope = rememberCoroutineScope()
    
    // Azure Maps service
    val azureMapsService = remember {
        if (LocationSearchConfig.isAzureMapsEnabled()) {
            AzureMapsService(LocationSearchConfig.AZURE_MAPS_KEY)
        } else null
    }

    var searchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<LocationSuggestion>>(emptyList()) }
    var isVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isFetchingCurrentLocation by remember { mutableStateOf(false) }
    
    // Search with Azure Maps
    suspend fun searchWithAzureMaps(query: String): List<LocationSuggestion> {
        if (azureMapsService == null) {
            Timber.w("Azure Maps not configured")
            return emptyList()
        }
        return try {
            val result = azureMapsService.searchLocations(query, limit = 8)
            result.getOrNull()?.map { it.toLocationSuggestion() } ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Azure Maps search failed")
            emptyList()
        }
    }
    
    // Fallback geocoder search
    suspend fun searchWithGeocoder(query: String): List<LocationSuggestion> {
        return try {
            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(query, 5) { addresses ->
                        val results = addresses.mapIndexed { index, address ->
                            LocationSuggestion(
                                placeId = "geocoder_$index",
                                displayName = address.getAddressLine(0) ?: query,
                                city = address.locality ?: address.subAdminArea ?: "",
                                state = address.adminArea ?: "",
                                country = address.countryName ?: "India",
                                postalCode = address.postalCode ?: "",
                                area = address.subLocality ?: "",
                                latitude = address.latitude,
                                longitude = address.longitude
                            )
                        }
                        continuation.resume(results) {}
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 5)
                addresses?.mapIndexed { index, address ->
                    LocationSuggestion(
                        placeId = "geocoder_$index",
                        displayName = address.getAddressLine(0) ?: query,
                        city = address.locality ?: address.subAdminArea ?: "",
                        state = address.adminArea ?: "",
                        country = address.countryName ?: "India",
                        postalCode = address.postalCode ?: "",
                        area = address.subLocality ?: "",
                        latitude = address.latitude,
                        longitude = address.longitude
                    )
                } ?: emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Geocoder search failed")
            emptyList()
        }
    }

    // Search effect
    LaunchedEffect(searchText) {
        if (searchText.isNotEmpty() && searchText.length >= 2) {
            delay(300)
            isSearching = true
            errorMessage = ""
            
            scope.launch {
                // Try Azure Maps first
                if (azureMapsService != null) {
                    Timber.d("Searching with Azure Maps: $searchText")
                    val azureResults = searchWithAzureMaps(searchText)
                    if (azureResults.isNotEmpty()) {
                        suggestions = azureResults
                        errorMessage = ""
                        isSearching = false
                        return@launch
                    }
                }
                
                // Fallback to Geocoder
                val geocoderResults = searchWithGeocoder(searchText)
                if (geocoderResults.isNotEmpty()) {
                    suggestions = geocoderResults
                    errorMessage = ""
                } else {
                    suggestions = emptyList()
                    errorMessage = if (azureMapsService == null) {
                        "Azure Maps not configured. Add your API key."
                    } else {
                        "No locations found. Try a different search."
                    }
                }
                isSearching = false
            }
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
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Common Header for consistency
            CommonHeader(
                title = "Select Location",
                subtitle = "Choose your preferred location",
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
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        ReusableSearchBar(
                            query = searchText,
                            onQueryChange = { searchText = it },
                            placeholder = "Search for area, street, city...",
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
                                scope.launch {
                                    isFetchingCurrentLocation = true
                                    errorMessage = ""
                                    try {
                                        if (locationService.hasLocationPermission()) {
                                            // Get high accuracy GPS location (5-10m precision)
                                            val locationInfo = locationService.getHighAccuracyLocation(
                                                timeoutMs = 15000L,
                                                minAccuracyMeters = 10f
                                            )
                                            
                                            if (locationInfo != null && (locationInfo.latitude != 0.0 || locationInfo.longitude != 0.0)) {
                                                // Use Android Geocoder result directly (no Azure Maps reverse geocoding)
                                                val finalLocationData = locationService.toLocationData(locationInfo)
                                                
                                                locationPreferences.saveLocation(finalLocationData)
                                                locationPreferences.setPermissionGranted(true)
                                                Timber.d("📍 Location saved: ${finalLocationData.getShortAddress()}")
                                                navController.navigate(Routes.WORKER_HOME) {
                                                    popUpTo(Routes.MANUAL_LOCATION_ROUTE) { inclusive = true }
                                                }
                                            } else {
                                                errorMessage = "Could not fetch location. Check GPS settings."
                                            }
                                        } else {
                                            errorMessage = "Location permission required."
                                        }
                                    } catch (e: Exception) {
                                        Timber.e(e, "Location fetch error")
                                        errorMessage = "Error: ${e.message}"
                                    } finally {
                                        isFetchingCurrentLocation = false
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
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
                        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                                        Icon(Icons.Default.Error, contentDescription = "Error", tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
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
                                                    postalCode = suggestion.postalCode,
                                                    area = suggestion.area.ifEmpty { suggestion.city },
                                                    timestamp = System.currentTimeMillis()
                                                )
                                                locationPreferences.saveLocation(locationData)
                                                Timber.d("📍 Location selected: ${suggestion.displayName}")
                                                Timber.d("📍   City: ${suggestion.city}, State: ${suggestion.state}, Postal: ${suggestion.postalCode}")
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
                                                    Icon(Icons.Default.LocationOff, contentDescription = "No results", tint = Color(0xFFBDBDBD), modifier = Modifier.size(40.dp))
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(text = "No locations found", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF9E9E9E)), textAlign = TextAlign.Center)
                                                    Text(text = "Try a different search term", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFBDBDBD)), textAlign = TextAlign.Center)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Azure Maps info when not configured
                if (!LocationSearchConfig.isAzureMapsEnabled() && searchText.isEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Location Search",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Using basic search. For better results, configure Azure Maps API key.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF795548))
                            )
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
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(20.dp))
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
        Icon(Icons.Default.NorthWest, contentDescription = null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(16.dp))
    }
}

data class LocationSuggestion(
    val placeId: String,
    val displayName: String,
    val city: String,
    val state: String,
    val country: String,
    val postalCode: String = "",
    val area: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
