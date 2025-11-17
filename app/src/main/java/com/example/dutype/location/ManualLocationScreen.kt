package com.example.dutype.location

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.ui.components.ReusableSearchBar
import androidx.navigation.compose.rememberNavController
import com.example.dutype.navigation.Routes
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.parttime.dutype.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualLocationScreen(navController: NavController) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val locationPreferences = remember { LocationPreferences(context) }
    val placesClient = remember(context) { 
        // Only create once per context
        if (!Places.isInitialized()) {
            Places.initialize(context, BuildConfig.MAPS_API_KEY)
        }
        Places.createClient(context)
    }
    val token = remember { AutocompleteSessionToken.newInstance() }
    val scope = rememberCoroutineScope()

    var searchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<LocationSuggestion>>(emptyList()) }
    var isVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(searchText) {
        if (searchText.isNotEmpty() && searchText.length >= 2) {
            delay(300) // Debounce to avoid too many API calls
            isSearching = true
            val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(searchText)
                .build()

            placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
                suggestions = response.autocompletePredictions.map { prediction ->
                    LocationSuggestion(
                        placeId = prediction.placeId,
                        displayName = prediction.getFullText(null).toString(),
                        city = prediction.getPrimaryText(null).toString(),
                        state = prediction.getSecondaryText(null).toString(),
                        country = ""
                    )
                }
                isSearching = false
                errorMessage = ""
            }.addOnFailureListener { exception ->
                // Handle error
                android.util.Log.e("ManualLocationScreen", "Places autocomplete error: ${exception.message}", exception)
                errorMessage = if (exception.message?.contains("Billing") == true) {
                    "Location search requires billing. Please enable billing in Google Cloud Console."
                } else {
                    "Failed to search locations. Please try again."
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
            .background(Color(0xFFFAFAFA))
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Select Location",
                            color = Color(0xFF212121),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                "Back",
                                tint = Color(0xFF212121),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Search Header Section
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(600)) + slideInVertically(
                        tween(600),
                        initialOffsetY = { -it / 2 }
                    )
                ) {
                    Column {
                        Text(
                            text = "Where are you looking for work?",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF212121),
                                fontSize = 23.sp
                            ),
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )

                        Text(
                            text = "Enter your location to find nearby opportunities",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF757575),
                                fontSize = 14.sp
                            ),
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                        )
                    }
                }

                // Search Bar with improved styling
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(800, 200)) + slideInVertically(
                        tween(800, 200),
                        initialOffsetY = { it / 3 }
                    )
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        ReusableSearchBar(
                            query = searchText,
                            onQueryChange = { searchText = it },
                            placeholder = "Search city, area, or locality",
                            height = 56,
                            backgroundColor = Color.White,
                            borderColor = Color(0xFFE8E8E8),
                            focusedBorderColor = Color(0xFF1976D2),
                            searchIconColor = Color(0xFF1976D2),
                            placeholderColor = Color(0xFFAAAAAA),
                            textColor = Color(0xFF212121),
                            cornerRadius = 14,
                            fontSize = 15,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Current Location Option
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1000, 400)) + slideInVertically(
                        tween(1000, 400),
                        initialOffsetY = { it / 4 }
                    )
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(14.dp))
                            .clickable {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                // Navigate back to location service to get current location
                                navController.navigate(Routes.LOCATION_SERVICE)
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Current Location",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Use Current Location",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF212121),
                                        fontSize = 16.sp
                                    )
                                )
                                Text(
                                    text = "We'll detect your location automatically",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF757575),
                                        fontSize = 13.sp
                                    )
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Go",
                                tint = Color(0xFFBDBDBD),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Suggestions List
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1200, 600)) + slideInVertically(
                        tween(1200, 600),
                        initialOffsetY = { it / 5 }
                    )
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column {
                            // Header with improved styling
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

                            // Error message with improved styling
                            if (errorMessage.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = "Error",
                                            tint = Color(0xFFD32F2F),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = errorMessage,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFFD32F2F),
                                                fontSize = 13.sp
                                            )
                                        )
                                    }
                                }
                            }

                            // Loading indicator
                            if (isSearching) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF1976D2),
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                            } else {
                                // Suggestions with improved styling
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 300.dp)
                                ) {
                                    items(suggestions) { suggestion ->
                                        LocationSuggestionItem(
                                            suggestion = suggestion,
                                            onSelected = {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                                scope.launch {
                                                    val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS_COMPONENTS)
                                                    val request = FetchPlaceRequest.newInstance(suggestion.placeId, placeFields)
                                                    placesClient.fetchPlace(request).addOnSuccessListener { response ->
                                                        val place = response.place
                                                        val city = place.addressComponents?.asList()?.find { it.types.contains("locality") }?.name ?: ""
                                                        val state = place.addressComponents?.asList()?.find { it.types.contains("administrative_area_level_1") }?.name ?: ""
                                                        
                                                        locationPreferences.saveManualLocation(
                                                            city,
                                                            state,
                                                            suggestion.displayName
                                                        )

                                                        navController.navigate(Routes.WORKER_HOME) {
                                                            popUpTo(Routes.MANUAL_LOCATION_ROUTE) { 
                                                                inclusive = true 
                                                            }
                                                        }
                                                    }.addOnFailureListener { exception ->
                                                        // Handle error
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    if (suggestions.isEmpty() && searchText.isNotEmpty() && !isSearching) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.LocationOff,
                                                        contentDescription = "No results",
                                                        tint = Color(0xFFBDBDBD),
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .padding(bottom = 8.dp)
                                                    )
                                                    Text(
                                                        text = "No locations found",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = Color(0xFF757575),
                                                            fontWeight = FontWeight.Medium
                                                        ),
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Text(
                                                        text = "for \"$searchText\"",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = Color(0xFFBDBDBD)
                                                        ),
                                                        textAlign = TextAlign.Center
                                                    )
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
private fun LocationSuggestionItem(
    suggestion: LocationSuggestion,
    onSelected: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "location_item_scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) Color(0xFFF5F5F5) else Color.Transparent,
        label = "location_item_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .clickable {
                isPressed = true
                onSelected()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF1976D2).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.city,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121),
                    fontSize = 15.sp
                )
            )
            if (suggestion.state.isNotEmpty()) {
                Text(
                    text = "${suggestion.state}${if (suggestion.country.isNotEmpty()) ", ${suggestion.country}" else ""}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF757575),
                        fontSize = 13.sp
                    )
                )
            }
        }

        Icon(
            imageVector = Icons.Default.NorthWest,
            contentDescription = "Select",
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.size(18.dp)
        )
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}

data class LocationSuggestion(
    val placeId: String,
    val displayName: String,
    val city: String,
    val state: String,
    val country: String
)

@Preview(showBackground = true)
@Composable
fun PreviewManualLocationScreen() {
    MaterialTheme {
        ManualLocationScreen(navController = rememberNavController())
    }
}
