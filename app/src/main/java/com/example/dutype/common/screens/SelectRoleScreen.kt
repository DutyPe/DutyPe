package com.example.dutype.common.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.dutype.app.R
import com.example.dutype.location.LocationPreferences
import com.example.dutype.models.LocationData
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.LocationService
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun SelectRoleScreen(
    navController: NavHostController,
    onRoleSelected: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    // Check current permission status (no location fetch on startup for fast loading)
    LaunchedEffect(Unit) {
        hasNotificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Notifications don't require runtime permission on older versions
        }

        hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        Timber.d("📍 SelectRoleScreen - Initial permission check: notification=$hasNotificationPermission, location=$hasLocationPermission")
        // Location will be fetched when user navigates to a screen that needs it (e.g., WorkerHomeScreen)
    }

    // Location permission launcher - FETCH LOCATION IMMEDIATELY after permission granted
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        Timber.d("📍 SelectRoleScreen - Location permission result: $hasLocationPermission")

        // CRITICAL FIX: If permission granted, fetch location immediately using lite speed
        if (hasLocationPermission) {
            val locationPreferences = LocationPreferences(context)
            locationPreferences.setPermissionGranted(true)
            Timber.d("📍 SelectRoleScreen - Permission saved, fetching location NOW at LIGHT SPEED...")

            // Fetch location immediately in background (LIGHT SPEED - highest priority)
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val locationService = LocationService(context)
                    // Use getLocationFast for immediate fetch with high priority
                    locationService.getLocationFast(locationPreferences) { locationInfo ->
                        if (locationInfo != null) {
                            Timber.d("📍 SelectRoleScreen - ⚡ LIGHT SPEED location fetched: ${locationInfo.getFullAddress()}")
                            // Convert LocationInfo to LocationData for saving
                            val locationData = LocationData(
                                latitude = locationInfo.latitude,
                                longitude = locationInfo.longitude,
                                address = locationInfo.address,
                                city = locationInfo.city,
                                area = locationInfo.area,
                                state = locationInfo.state,
                                country = locationInfo.country,
                                accuracy = locationInfo.accuracy,
                                timestamp = locationInfo.timestamp
                            )
                            // Save to preferences immediately so WorkerHomeScreen can use it
                            scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                locationPreferences.saveLocation(locationData)
                                Timber.d("📍 SelectRoleScreen - ✅ Location saved to preferences, WorkerHomeScreen will show it immediately")
                            }
                        } else {
                            Timber.w("📍 SelectRoleScreen - Location fetch returned null")
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "📍 SelectRoleScreen - Error fetching location at light speed")
                }
            }
        }

        isVisible = true
    }

    // Notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        Timber.d("🔔 SelectRoleScreen - Notification permission result: $isGranted")

        // After notification permission, request location permission (no toast)
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
            .background(Color(0xFFF8FAFC))
    ) {
        // Clean white backdrop with subtle color accents
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFBEB),
                            Color(0xFFEFF6FF),
                            Color(0xFFF8FAFC)
                        )
                    )
                )
        )

        // Decorative background elements — soft radial glows
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF60A5FA).copy(alpha = 0.12f),
                        Color.Transparent
                    )
                ),
                center = Offset(x = canvasWidth * 0.84f, y = canvasHeight * 0.08f),
                radius = canvasWidth * 0.44f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFA78BFA).copy(alpha = 0.10f),
                        Color.Transparent
                    )
                ),
                center = Offset(x = canvasWidth * 0.15f, y = canvasHeight * 0.9f),
                radius = canvasWidth * 0.5f
            )
        }

        Box(
            modifier = Modifier
                .size(172.dp)
                .offset(x = 176.dp, y = 128.dp)
                .graphicsLayer { rotationZ = 18f; alpha = 0.22f }
                .clip(RoundedCornerShape(42.dp))
                .background(Color.White.copy(alpha = 0.55f))
        )

        Box(
            modifier = Modifier
                .size(126.dp, 88.dp)
                .offset(x = (-24).dp, y = 478.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xFFDDD6FE).copy(alpha = 0.35f))
                .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(30.dp))
        )

        Box(
            modifier = Modifier
                .size(210.dp)
                .offset(x = (-62).dp, y = 612.dp)
                .clip(CircleShape)
                .background(Color(0xFFA78BFA).copy(alpha = 0.08f))
                .blur(24.dp)
        )

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
                    text = stringResource(R.string.how_can_we_help),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = MeeshoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(320, delayMillis = 60))
            ) {
                Text(
                    text = "Choose your path. You can switch roles anytime.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = MeeshoFontFamily,
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                    // Worker Role
                    RoleCard(
                        icon = "👷",
                        title = stringResource(R.string.worker),
                        subtitle = stringResource(R.string.find_jobs_earn),
                        primaryColor = Color(0xFF86EFAC),
                        containerColor = Color(0xFF67E8F9),
                        arrowColor = Color(0xFFE2E8F0),
                        delay = 50,
                        onClick = {
                            Timber.d("🔍 Worker role selected")
                            if (onRoleSelected != null) {
                                onRoleSelected.invoke("WORKER")
                            } else {
                                // Navigate directly to Worker Home (guest mode)
                                navController.navigate(Routes.WORKER_HOME) {
                                    popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                }
                            }
                        }
                    )

                    // Employer Role
                    RoleCard(
                        icon = "🏢",
                        title = stringResource(R.string.employer),
                        subtitle = stringResource(R.string.hire_skilled_workers),
                        primaryColor = Color(0xFF93C5FD),
                        containerColor = Color(0xFFA78BFA),
                        arrowColor = Color(0xFFE2E8F0),
                        delay = 150,
                        onClick = {
                            Timber.d("🔍 Employer role selected")
                            if (onRoleSelected != null) {
                                onRoleSelected.invoke("EMPLOYER")
                            } else {
                                // Navigate directly to Employer Home (guest mode)
                                navController.navigate(Routes.EMPLOYER_HOME) {
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
fun RoleCard(
    icon: String,
    title: String,
    subtitle: String,
    primaryColor: Color,
    containerColor: Color,
    arrowColor: Color,
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
        ) { if (title.contains("Worker")) -100 else 100 } +
            fadeIn(animationSpec = tween(300))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    onClick()
                },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp,
                pressedElevation = 1.dp
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = Color(0xFFE2E8F0)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Container - Minimal and clean
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.18f),
                                    containerColor.copy(alpha = 0.12f)
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 36.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text Content - Clean typography
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = MeeshoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            fontSize = 20.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = MeeshoFontFamily,
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            color = Color(0xFFF1F5F9)
                        )
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniInfoPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFF1F5F9))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = MeeshoFontFamily,
                color = Color(0xFFE2E8F0),
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSelectRoleScreen() {
    MaterialTheme {
        SelectRoleScreen(navController = rememberNavController())
    }
}
