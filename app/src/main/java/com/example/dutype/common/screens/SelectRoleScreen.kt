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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.WorkOutline
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.dutype.app.R
import com.example.dutype.location.LocationPreferences
import com.example.dutype.models.LocationData
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.AppTypography
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
    com.example.dutype.ui.theme.ForceLightTheme {
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
            .background(WorkerColors.ScreenBackground)
    ) {
        // Clean backdrop with a restrained vertical wash for first-run focus.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            WorkerColors.ScreenBackground
                        )
                    )
                )
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
                    style = AppTypography.displayTitle.copy(
                        fontWeight = FontWeight.Bold,
                        color = WorkerColors.TextPrimary
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
                    style = AppTypography.bodyMedium.copy(color = WorkerColors.TextSecondary),
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
                        icon = Icons.Default.WorkOutline,
                        title = stringResource(R.string.worker),
                        subtitle = stringResource(R.string.find_jobs_earn),
                        primaryColor = WorkerColors.Primary,
                        containerColor = WorkerColors.PrimaryLight,
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
                        icon = Icons.Default.Business,
                        title = stringResource(R.string.employer),
                        subtitle = stringResource(R.string.hire_skilled_workers),
                        primaryColor = Color(0xFF0EA5E9),
                        containerColor = Color(0xFFE0F2FE),
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
}

@Composable
fun RoleCard(
    icon: ImageVector,
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
                .height(112.dp)
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
            colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp,
                pressedElevation = 0.dp
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = WorkerColors.Border
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
                        .size(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(containerColor)
                        .border(1.dp, primaryColor.copy(alpha = 0.14f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = primaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text Content - Clean typography
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = AppTypography.cardTitle.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = WorkerColors.TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = AppTypography.bodySmall.copy(color = WorkerColors.TextSecondary)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(WorkerColors.ChipBackground)
                        .border(1.dp, WorkerColors.Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = WorkerColors.IconPrimary,
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
