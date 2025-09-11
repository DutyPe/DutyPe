package com.example.partimes.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.*
import com.example.partimes.navigation.Routes
import kotlinx.coroutines.launch
import com.example.partimes.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationServiceScreen(navController: NavController) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Animation and UI state
    var isVisible by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var showLocationDialog by remember { mutableStateOf(false) }

    // Button press states
    var currentLocationPressed by remember { mutableStateOf(false) }
    var manualLocationPressed by remember { mutableStateOf(false) }

    // Permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (hasLocationPermission) {
            // Fetch location after permission is granted
            scope.launch {
                isLoadingLocation = true
                locationError = null
                try {
                    val location = fetchUserLocation(context)
                    currentLocation = location
                    if (location == "Location unavailable") {
                        locationError = "Unable to fetch your current location"
                    }
                } catch (e: Exception) {
                    locationError = "Error getting location: ${e.message}"
                } finally {
                    isLoadingLocation = false
                }
            }
        }
    }

    // Check if GPS is enabled
    fun isGPSEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // Handle location button click
    fun handleLocationRequest() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        when {
            !hasLocationPermission -> {
                // Request permission
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            !isGPSEnabled() -> {
                // Show GPS enable dialog
                showLocationDialog = true
            }
            else -> {
                // Fetch location
                scope.launch {
                    isLoadingLocation = true
                    locationError = null
                    try {
                        val location = fetchUserLocation(context)
                        currentLocation = location
                        if (location != "Location unavailable") {
                            // Navigate to next screen with location
                            println("DEBUG LocationService: Saving location: $location")
                            // Save the location using manual location method (since we only have the address string)
                            val locationPreferences = LocationPreferences(context)
                            locationPreferences.saveManualLocation(
                                city = location, // Use the full location as city for now
                                state = "", // We don't have state info from GPS
                                displayName = location
                            )

                            navController.navigate("select_role") {
                                popUpTo(Routes.LOCATION_SERVICE_SCREEN_ROUTE) { 
                                    inclusive = true 
                                }
                            }
                        } else {
                            locationError = "Unable to get precise location. Please try manual entry."
                        }
                    } catch (e: Exception) {
                        locationError = "Error: ${e.message}"
                    } finally {
                        isLoadingLocation = false
                    }
                }
            }
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
//                        Color(0xFF0D47A1), // Deep blue
                        Color(0xFF0066FF),
//                        Color(0xFF1976D2), // Medium blue
                        Color(0xFF004BD9),
//                        Color(0xFF42A5F5), // Light blue
                        Color(0xFFE3F2FD)  // Very light blue
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip button with improved blue-themed styling
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInHorizontally() + fadeIn(),
                        exit = slideOutHorizontally() + fadeOut()
                    ) {
                        Button(
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                navController.navigate("select_role") {
                                    popUpTo(Routes.LOCATION_SERVICE_SCREEN_ROUTE) { 
                                    inclusive = true 
                                }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                "Skip",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top spacer
                Spacer(modifier = Modifier.weight(0.3f))

                // Animated Location Icon Section
                AnimatedVisibility(
                    visible = isVisible,
                    enter = scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        // Main Location Icon with Lottie animation
                        Box(
                            modifier = Modifier.size(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Lottie animation setup for main location pin
                            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.redlocationpin))
                            val progress by animateLottieCompositionAsState(
                                composition = composition,
                                iterations = LottieConstants.IterateForever,
                                isPlaying = true
                            )

                            LottieAnimation(
                                composition = composition,
                                progress = { progress },
                                modifier = Modifier.size(120.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Title with animation
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = slideInVertically(
                                initialOffsetY = { 50 },
                                animationSpec = tween(600, delayMillis = 200)
                            ) + fadeIn(animationSpec = tween(600, delayMillis = 200))
                        ) {
                            Text(
                                text = "Choose Your Location",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 28.sp
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // Subtitle with animation
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = slideInVertically(
                                initialOffsetY = { 30 },
                                animationSpec = tween(600, delayMillis = 400)
                            ) + fadeIn(animationSpec = tween(600, delayMillis = 400))
                        ) {
                            Text(
                                text = "Turn on location to find local jobs or the right people faster",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    lineHeight = 22.sp
                                ),
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        // Current Location Display
                        if (currentLocation != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.95f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "📍 $currentLocation",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF1976D2),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        // Error Display
                        if (locationError != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFEBEE)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = Color(0xFFE53E3E),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = locationError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFE53E3E)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom spacer
                Spacer(modifier = Modifier.weight(0.3f))

                // Animated Button Section with improved colors for blue background
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { 100 },
                        animationSpec = tween(800, delayMillis = 600)
                    ) + fadeIn(animationSpec = tween(800, delayMillis = 600))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Current Location Button with enhanced styling for blue background
                        val currentLocationScale by animateFloatAsState(
                            targetValue = if (currentLocationPressed) 0.95f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "current_location_scale"
                        )

                        Button(
                            onClick = {
                                currentLocationPressed = true
                                handleLocationRequest()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .graphicsLayer {
                                    scaleX = currentLocationScale
                                    scaleY = currentLocationScale
                                }
                                .shadow(
                                    elevation = 12.dp,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1976D2)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                            enabled = !isLoadingLocation
                        ) {
                            if (isLoadingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color(0xFF1976D2)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Getting Location...",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Current Location",
                                        modifier = Modifier.size(20.dp),
                                        tint = Color(0xFF1976D2)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (currentLocation != null) "Use This Location" else "Use My Current Location",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        // Manual Location Button with enhanced styling for blue background
                        val manualLocationScale by animateFloatAsState(
                            targetValue = if (manualLocationPressed) 0.95f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "manual_location_scale"
                        )

                        Button(
                            onClick = {
                                manualLocationPressed = true
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                navController.navigate(Routes.MANUAL_LOCATION_ROUTE)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .graphicsLayer {
                                    scaleX = manualLocationScale
                                    scaleY = manualLocationScale
                                }
                                .shadow(
                                    elevation = 12.dp,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1976D2)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Manual Location",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFF1976D2)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Enter Location Manually",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF1976D2)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // GPS Enable Dialog
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Enable GPS",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            text = {
                Text(
                    text = "To get your precise location, please enable GPS in your device settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationDialog = false
                        // Open location settings
                        context.startActivity(
                            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    Text("Open Settings", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLocationDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF666666)
                    )
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Reset button press states
    LaunchedEffect(currentLocationPressed) {
        if (currentLocationPressed) {
            delay(150)
            currentLocationPressed = false
        }
    }

    LaunchedEffect(manualLocationPressed) {
        if (manualLocationPressed) {
            delay(150)
            manualLocationPressed = false
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLocationServiceScreen() {
    MaterialTheme {
        LocationServiceScreen(navController = rememberNavController())
    }
}
