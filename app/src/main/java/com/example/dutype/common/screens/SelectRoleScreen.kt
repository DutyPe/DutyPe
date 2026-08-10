package com.example.dutype.common.screens

import com.dutype.app.R
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
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
    var showLanguageBottomSheet by remember { mutableStateOf(false) }

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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEFF6FF),
                        Color(0xFFDBEAFE),
                        Color(0xFFF8FAFC),
                        Color(0xFFFFFFFF)
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Top Language Selector Chip Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val activeLangName = when (com.example.dutype.utils.LocaleHelper.getLanguage(context)) {
                    com.example.dutype.utils.LocaleHelper.LANGUAGE_TELUGU -> "తెలుగు"
                    "hi" -> "హిन्दी"
                    else -> "English"
                }

                Surface(
                    onClick = { showLanguageBottomSheet = true },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeLangName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { -50 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(300))
            ) {
                RoleHero()
            }

            Spacer(modifier = Modifier.height(22.dp))

            Spacer(modifier = Modifier.weight(1f))

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
                        .padding(bottom = 16.dp)
                ) {
                    RoleCard(
                        icon = Icons.Default.WorkOutline,
                        title = stringResource(R.string.worker),
                        subtitle = stringResource(R.string.find_jobs_earn),
                        description = stringResource(R.string.worker_card_desc),
                        primaryColor = Color(0xFF0F0F0F),
                        containerColor = Color(0xFFF1F1F4),
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

                    RoleCard(
                        icon = Icons.Default.Business,
                        title = stringResource(R.string.employer),
                        subtitle = stringResource(R.string.hire_skilled_workers),
                        description = stringResource(R.string.employer_card_desc),
                        primaryColor = Color(0xFF0F0F0F),
                        containerColor = Color(0xFFF1F1F4),
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

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }

        if (showLanguageBottomSheet) {
            com.example.dutype.components.LanguageSelectionBottomSheet(
                onDismiss = { showLanguageBottomSheet = false }
            )
        }
    }
    }
}

@Composable
fun RoleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    description: String,
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
                .height(132.dp)
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
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            ),
            border = BorderStroke(
                width = 1.dp,
                color = Color(0xFFE8E8EC)
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .fillMaxWidth(0.56f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(topStart = 100.dp, bottomEnd = 22.dp))
                        .background(containerColor.copy(alpha = 0.72f))
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(containerColor)
                        .border(1.dp, primaryColor.copy(alpha = 0.10f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = primaryColor,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = AppTypography.displayTitle.copy(
                            fontSize = 22.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = AppTypography.bodyLarge.copy(
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 21.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                }
            }
        }
    }
}

@Composable
private fun SelectRoleDecorations() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = 56.dp)
                .size(210.dp)
                .clip(CircleShape)
                .background(Color(0xFFEDE7FF).copy(alpha = 0.42f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-24).dp, y = 336.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(topEnd = 40.dp))
                .background(Color(0xFF7C5CFF).copy(alpha = 0.34f))
        )
    }
}

@Composable
private fun RoleHero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = stringResource(R.string.how_can_we_help_today),
            style = AppTypography.displayTitle.copy(
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        )
    }
}

@Composable
private fun HeroPeopleIllustration(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(178.dp)
            .height(210.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(172.dp)
                .clip(CircleShape)
                .background(Color(0xFFEDE7FF).copy(alpha = 0.58f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .width(116.dp)
                .height(92.dp)
        ) {
            listOf(16.dp, 45.dp, 76.dp).forEachIndexed { index, xOffset ->
                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = (44 - index * 10).dp)
                        .width(22.dp)
                        .height((38 + index * 12).dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(Color(0xFFCFC6F8).copy(alpha = 0.36f))
                )
            }
        }

        PersonFigure(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 10.dp),
            shirtColor = Color(0xFF2563EB),
            headColor = Color(0xFFFFD7B5),
            accentColor = Color(0xFF1E40AF),
            isWorker = true
        )
        PersonFigure(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-8).dp, y = 4.dp),
            shirtColor = Color(0xFFB895F6),
            headColor = Color(0xFFFFDCC6),
            accentColor = Color(0xFF6D28D9),
            isWorker = false
        )
    }
}

@Composable
private fun PersonFigure(
    modifier: Modifier,
    shirtColor: Color,
    headColor: Color,
    accentColor: Color,
    isWorker: Boolean
) {
    Box(
        modifier = modifier
            .width(84.dp)
            .height(132.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(54.dp)
                .height(74.dp)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(shirtColor)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 26.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(headColor)
        )
        if (isWorker) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 18.dp)
                    .width(50.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(accentColor)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 6.dp, y = (-18).dp)
                    .width(18.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1F2937))
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 24.dp)
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2F1F3A).copy(alpha = 0.78f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 32.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(headColor)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-6).dp, y = (-20).dp)
                    .width(24.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF334155))
            )
        }
    }
}

@Composable
private fun RoleTrustLine() {
    Text(
        text = stringResource(R.string.auto_join_thousands_finding_opportunities_every),
        modifier = Modifier.fillMaxWidth(),
        style = AppTypography.bodyMedium.copy(
            color = Color(0xFF6B7280),
            fontWeight = FontWeight.Medium,
            lineHeight = 22.sp
        ),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SafeSecurePill() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF4F4F6))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = Color(0xFF0F0F0F),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.safe_secure_trusted),
            style = AppTypography.bodyMedium.copy(
                color = Color(0xFF6B7280),
                fontWeight = FontWeight.Medium
            )
        )
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