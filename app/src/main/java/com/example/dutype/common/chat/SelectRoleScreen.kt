package com.example.dutype.common.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.dutype.app.R
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.ui.theme.WorkerColors
import timber.log.Timber

@Composable
fun SelectRoleScreen(
    navController: NavController,
    onRoleSelected: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
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
    
    // Location permission launcher (no location fetch - just grant permission for later use)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        Timber.d("📍 SelectRoleScreen - Location permission result: $hasLocationPermission")
        // Location will be fetched when user navigates to a screen that needs it
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
                    text = stringResource(R.string.how_can_we_help),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = MeeshoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = WorkerColors.TextPrimary
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
                        title = stringResource(R.string.worker),
                        subtitle = stringResource(R.string.find_jobs_earn),
                        primaryColor = Color(0xFF4CAF50),
                        containerColor = Color(0xFFE8F5E9),
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
                        animationRes = R.raw.employer,
                        title = stringResource(R.string.employer),
                        subtitle = stringResource(R.string.hire_skilled_workers),
                        primaryColor = Color(0xFF2196F3),
                        containerColor = Color(0xFFE3F2FD),
                        delay = 150, // Reduced from 250/600
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
                    color = WorkerColors.Border,
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
            colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
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
                        .background(WorkerColors.ChipBackground),
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
                            fontFamily = MeeshoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = WorkerColors.TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = MeeshoFontFamily,
                            color = WorkerColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Circular Arrow with black background (Worker)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F2937)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard)
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
                    color = WorkerColors.Border,
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
            colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
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
                            fontFamily = MeeshoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = WorkerColors.TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = MeeshoFontFamily,
                            color = WorkerColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Circular Arrow with blue background (Employer)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard)
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