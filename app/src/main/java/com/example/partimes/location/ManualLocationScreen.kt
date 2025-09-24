package com.example.partimes.location

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
import com.example.partimes.ui.components.ReusableSearchBar
import androidx.navigation.compose.rememberNavController
import com.example.partimes.navigation.Routes
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualLocationScreen(navController: NavController) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val locationPreferences = remember { LocationPreferences(context) }

    var searchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<LocationSuggestion>>(emptyList()) }
    var isVisible by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LocationSuggestion?>(null) }

    // Sample location suggestions - in real app, you'd use Places API or similar
    val popularLocations = remember {
        listOf(
            LocationSuggestion("Hyderabad, Telangana", "Hyderabad", "Telangana", "India", 17.3850, 78.4867),
            LocationSuggestion("Khammam, Telangana", "Khammam", "Telangana", "India", 17.2473, 80.1514),
            LocationSuggestion("Bangalore, Karnataka", "Bangalore", "Karnataka", "India", 12.9716, 77.5946),
            LocationSuggestion("Mumbai, Maharashtra", "Mumbai", "Maharashtra", "India", 19.0760, 72.8777),
            LocationSuggestion("Delhi, Delhi", "Delhi", "Delhi", "India", 28.7041, 77.1025),
            LocationSuggestion("Chennai, Tamil Nadu", "Chennai", "Tamil Nadu", "India", 13.0827, 80.2707),
            LocationSuggestion("Pune, Maharashtra", "Pune", "Maharashtra", "India", 18.5204, 73.8567),
            LocationSuggestion("Kolkata, West Bengal", "Kolkata", "West Bengal", "India", 22.5726, 88.3639)
        )
    }

    LaunchedEffect(searchText) {
        if (searchText.isNotEmpty()) {
            isSearching = true
            delay(300) // Simulate search delay
            suggestions = popularLocations.filter { location ->
                location.displayName.contains(searchText, ignoreCase = true) ||
                location.city.contains(searchText, ignoreCase = true) ||
                location.state.contains(searchText, ignoreCase = true)
            }
            isSearching = false
        } else {
            suggestions = popularLocations.take(5) // Show popular locations when not searching
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D47A1),
                        Color(0xFF1976D2),
                        Color(0xFF42A5F5),
                        Color(0xFFE3F2FD)
                    )
                )
            )
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
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                "Back",
                                tint = Color.White,
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
                        // Removed bottom padding to prevent white space
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
                                color = Color.White,
                                fontSize = 22.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )

                        Text(
                            text = "Enter your location to find nearby opportunities",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                        )
                    }
                }

                // Search Bar
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
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        ReusableSearchBar(
                            query = searchText,
                            onQueryChange = { searchText = it },
                            placeholder = "Search for a city, area, or locality",
                            height = 48,
                            backgroundColor = Color.Transparent,
                            borderColor = Color.Transparent,
                            focusedBorderColor = Color(0xFF1976D2),
                            searchIconColor = Color(0xFF1976D2),
                            placeholderColor = Color.Gray,
                            textColor = Color.Black,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                            .shadow(4.dp, RoundedCornerShape(12.dp))
                            .clickable {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                // Navigate back to location service to get current location
                                navController.navigate(Routes.LOCATION_SERVICE)
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Current Location",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Use Current Location",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1A1A1A)
                                    )
                                )
                                Text(
                                    text = "We'll detect your location automatically",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.Gray
                                    )
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Go",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            .shadow(4.dp, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            // Header
                            Text(
                                text = if (searchText.isEmpty()) "Popular Locations" else "Search Results",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2)
                                ),
                                modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                            )

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
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                // Suggestions
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 300.dp)
                                ) {
                                    items(suggestions) { suggestion ->
                                        LocationSuggestionItem(
                                            suggestion = suggestion,
                                            onSelected = {
                                                selectedLocation = suggestion
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                                // Save location to preferences
                                                locationPreferences.saveManualLocation(
                                                    suggestion.city,
                                                                                       suggestion.state,
                                                    suggestion.displayName
                                                )

                                                // Navigate to select role or home
                                                navController.navigate(Routes.SELECT_ROLE) {
                                                    popUpTo(Routes.LOCATION_SERVICE) { 
                                                        inclusive = true 
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
                                                Text(
                                                    text = "No locations found for \"$searchText\"",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.Gray,
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable {
                isPressed = true
                onSelected()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFF1976D2).copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.city,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A)
                )
            )
            Text(
                text = "${suggestion.state}, ${suggestion.country}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.Gray
                )
            )
        }

        Icon(
            imageVector = Icons.Default.NorthWest,
            contentDescription = "Select",
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
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
    val displayName: String,
    val city: String,
    val state: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
)

@Preview(showBackground = true)
@Composable
fun PreviewManualLocationScreen() {
    MaterialTheme {
        ManualLocationScreen(navController = rememberNavController())
    }
}
