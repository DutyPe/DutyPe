package com.example.dutype.common.chat

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.*
import com.dutype.app.R
import com.example.dutype.location.LocationPreferences
import com.example.dutype.location.fetchUserLocation
import com.example.dutype.models.LocationData
import com.example.dutype.navigation.Routes
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun SelectRoleScreen(
    navController: NavController,
    onRoleSelected: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    
    // Location preferences for saving location app-wide
    val locationPreferences = remember { LocationPreferences(context) }
    
    // Check current permission status
    LaunchedEffect(Unit) {
        hasNotificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Notifications don't require runtime permission on older versions
        }
        
        hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        Timber.d("📍 SelectRoleScreen - Initial permission check: notification=$hasNotificationPermission, location=$hasLocationPermission")
        
        // If location permission already granted, fetch and save location
        if (hasLocationPermission) {
            scope.launch {
                try {
                    val locationAddress = fetchUserLocation(context)
                    if (locationAddress != "Location unavailable") {
                        val addressParts = locationAddress.split(",")
                        val city = addressParts.getOrNull(0)?.trim() ?: ""
                        val state = addressParts.getOrNull(1)?.trim()
                        
                        val locationData = LocationData(
                            address = locationAddress,
                            latitude = 0.0,
                            longitude = 0.0,
                            city = city,
                            state = state,
                            country = "India",
                            postalCode = null
                        )
                        locationPreferences.saveLocation(locationData)
                        Timber.d("📍 SelectRoleScreen - Saved existing location: $locationAddress")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "📍 SelectRoleScreen - Error fetching existing location")
                }
            }
        }
    }
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        Timber.d("📍 SelectRoleScreen - Location permission result: $hasLocationPermission")
        
        if (hasLocationPermission) {
            Toast.makeText(context, "Location enabled for nearby jobs", Toast.LENGTH_SHORT).show()
            
            // Fetch and save location immediately after permission granted
            scope.launch {
                try {
                    val locationAddress = fetchUserLocation(context)
                    if (locationAddress != "Location unavailable") {
                        val addressParts = locationAddress.split(",")
                        val city = addressParts.getOrNull(0)?.trim() ?: ""
                        val state = addressParts.getOrNull(1)?.trim()
                        
                        val locationData = LocationData(
                            address = locationAddress,
                            latitude = 0.0,
                            longitude = 0.0,
                            city = city,
                            state = state,
                            country = "India",
                            postalCode = null
                        )
                        locationPreferences.saveLocation(locationData)
                        Timber.d("📍 SelectRoleScreen - Location fetched and saved: $locationAddress")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "📍 SelectRoleScreen - Error fetching location after permission")
                }
            }
        }
        
        // Now show the role selection UI
        isVisible = true
    }
    
    // Notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        Timber.d("🔔 SelectRoleScreen - Notification permission result: $isGranted")
        
        if (isGranted) {
            Toast.makeText(context, "Notifications enabled for job alerts", Toast.LENGTH_SHORT).show()
        }
        
        // After notification permission, request location permission
        if (!hasLocationPermission) {
            Timber.d("📍 SelectRoleScreen - Requesting location permission...")
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            isVisible = true
        }
    }
    
    // Request permissions on first load
    LaunchedEffect(Unit) {
        val sharedPrefs = context.getSharedPreferences("permission_prefs", android.content.Context.MODE_PRIVATE)
        val permissionsAskedBefore = sharedPrefs.getBoolean("permissions_asked_on_role_screen", false)
        
        if (!permissionsAskedBefore) {
            Timber.d("🔔 SelectRoleScreen - First time on role screen, requesting permissions...")
            sharedPrefs.edit().putBoolean("permissions_asked_on_role_screen", true).apply()
            
            // Request notification permission first (Android 13+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                Timber.d("🔔 SelectRoleScreen - Requesting notification permission...")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else if (!hasLocationPermission) {
                // Skip notification, go straight to location
                Timber.d("📍 SelectRoleScreen - Requesting location permission...")
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                // Both permissions already granted
                isVisible = true
            }
        } else {
            // Permissions already asked before, just show the UI
            Timber.d("📍 SelectRoleScreen - Permissions already asked, showing UI")
            isVisible = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        // Decorative background elements
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            drawCircle(
                color = Color(0xFF4CAF50).copy(alpha = 0.04f),
                center = Offset(x = canvasWidth * 0.85f, y = canvasHeight * 0.1f),
                radius = canvasWidth * 0.5f
            )
            
            drawCircle(
                color = Color(0xFF2196F3).copy(alpha = 0.04f),
                center = Offset(x = canvasWidth * 0.15f, y = canvasHeight * 0.9f),
                radius = canvasWidth * 0.6f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Animated Header - Fast entrance
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { -50 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(300))
            ) {
                Text(
                    text = "How can we help\nyou today?",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A1C1E)
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Animated Cards at bottom
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { 50 },
                    animationSpec = tween(400, delayMillis = 50, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(400, delayMillis = 50))
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    // Worker Role - with cycling animations
                    WorkerRoleCard(
                        title = "Worker",
                        subtitle = "Find jobs & earn money",
                        primaryColor = Color(0xFF4CAF50),
                        containerColor = Color(0xFFE8F5E9),
                        delay = 50,
                        onClick = { 
                            Timber.d("🔍 Worker role selected")
                            if (onRoleSelected != null) {
                                onRoleSelected.invoke("WORKER")
                            } else {
                                navController.navigate(Routes.WORKER_HOME) {
                                    popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                }
                            }
                        }
                    )

                    // Employer Role
                    RoleCard(
                        animationRes = R.raw.employer,
                        title = "Employer",
                        subtitle = "Hire skilled workers",
                        primaryColor = Color(0xFF2196F3),
                        containerColor = Color(0xFFE3F2FD),
                        delay = 150, // Reduced from 250/600
                        onClick = { 
                            Timber.d("🔍 Employer role selected")
                            if (onRoleSelected != null) {
                                onRoleSelected.invoke("EMPLOYER")
                            } else {
                                navController.navigate("${Routes.ENHANCED_LOGIN}?role=EMPLOYER") {
                                    popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WorkerRoleCard(
    title: String,
    subtitle: String,
    primaryColor: Color,
    containerColor: Color,
    delay: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    // Cycling animations: delivery, cleaner, cook
    val animations = listOf(R.raw.delivery, R.raw.cleaner, R.raw.cook)
    var currentAnimationIndex by remember { mutableIntStateOf(0) }
    
    // Cycle through animations every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3000L)
            currentAnimationIndex = (currentAnimationIndex + 1) % animations.size
        }
    }

    // Entrance animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        isVisible = true
    }

    // Faster entrance animation
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) { -100 } + 
        fadeIn(animationSpec = tween(300))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .border(
                    width = 1.dp,
                    color = Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(24.dp)
                )
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = primaryColor.copy(alpha = 0.2f),
                    ambientColor = primaryColor.copy(alpha = 0.05f)
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    onClick()
                },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animation Container with crossfade between animations
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    // Crossfade between animations
                    Crossfade(
                        targetState = currentAnimationIndex,
                        animationSpec = tween(500),
                        label = "animation_crossfade"
                    ) { index ->
                        val composition by rememberLottieComposition(
                            LottieCompositionSpec.RawRes(animations[index])
                        )
                        val progress by animateLottieCompositionAsState(
                            composition,
                            iterations = LottieConstants.IterateForever,
                        )
                        
                        LottieAnimation(
                            composition = composition,
                            progress = { progress },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text Content - Middle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1C1E)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF757575),
                            fontSize = 14.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Circular Arrow with colored background
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RoleCard(
    animationRes: Int,
    title: String,
    subtitle: String,
    primaryColor: Color,
    containerColor: Color,
    delay: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationRes))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever,
    )

    // Entrance animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        isVisible = true
    }

    // Faster entrance animation
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) { if (title == "Worker") -100 else 100 } + 
        fadeIn(animationSpec = tween(300))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .border(
                    width = 1.dp,
                    color = Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(24.dp)
                )
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = primaryColor.copy(alpha = 0.2f),
                    ambientColor = primaryColor.copy(alpha = 0.05f)
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    onClick()
                },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animation Container - First (fits exactly in box)
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text Content - Middle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1C1E)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF757575),
                            fontSize = 14.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Circular Arrow with colored background
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSelectRoleScreen() {
    MaterialTheme {
        SelectRoleScreen(navController = rememberNavController())
    }
}