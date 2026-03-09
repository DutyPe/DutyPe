package com.example.dutype.navigation

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dutype.components.WorkerBottomBar
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.rememberScrollStateManager
import com.example.dutype.utils.rememberWindowSizeClass

@Composable
fun WorkerMainScreen(
    rootNavController: NavHostController,
    onStatusBarColorChange: (Color) -> Unit = {},
    notificationPermissionManager: com.example.dutype.utils.NotificationPermissionManager
) {
    val navController = rememberNavController()
    val view = LocalView.current
    val scrollStateManager = rememberScrollStateManager()
    val windowSizeClass = rememberWindowSizeClass()
    
    var showBottomBar by remember { mutableStateOf(true) }
    val isBottomBarVisible by scrollStateManager.isBottomBarVisible

    // Status bar color state - starts with white, individual screens can change it
    var statusBarColor by remember { mutableStateOf(Color.White) }
    val navigationBarColor = Color.White // White navigation bar with dark icons

    // Apply system bar colors using enableEdgeToEdge (Android 15+ compatible)
    // This replaces deprecated window.statusBarColor and window.navigationBarColor
    LaunchedEffect(statusBarColor) {
        val activity = view.context as? ComponentActivity
        
        // Calculate luminance to determine if status bar is light or dark
        val luminance = (0.299 * statusBarColor.red + 0.587 * statusBarColor.green + 0.114 * statusBarColor.blue)
        val isLightStatusBar = luminance > 0.5f
        
        val statusBarStyle = if (isLightStatusBar) {
            // Light status bar - dark icons
            SystemBarStyle.light(
                scrim = statusBarColor.toArgb(),
                darkScrim = statusBarColor.toArgb()
            )
        } else {
            // Dark status bar - light icons
            SystemBarStyle.dark(scrim = statusBarColor.toArgb())
        }
        
        activity?.enableEdgeToEdge(
            statusBarStyle = statusBarStyle,
            navigationBarStyle = SystemBarStyle.light(
                scrim = navigationBarColor.toArgb(),
                darkScrim = navigationBarColor.toArgb()
            )
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define routes that should not show the bottom bar
    val routesWithoutBottomBar = listOf(
        Routes.LOGOUT, Routes.JOB_DETAIL, Routes.CHAT_DETAIL,
        Routes.HELP, Routes.CHAT_SUPPORT, Routes.CALL_SUPPORT, Routes.REPORT, 
        Routes.TUTORIAL, Routes.FAQ, Routes.ABOUT_US,
        Routes.WORKER_NOTIFICATIONS, Routes.WORKER_ALL_JOBS, "worker_all_jobs",
        Routes.WORKER_JOB_MAP, // Hide bottom bar on map screen
        Routes.WORKER_VISITING_CARD, // Hide bottom bar on visiting card screen
        Routes.WORKER_EARNINGS, // Hide bottom bar on earnings screen
        Routes.WORKER_HISTORY, // Hide bottom bar on work history screen
        Routes.WORKER_REFER_EARN, // Hide bottom bar on refer & earn screen
        Routes.WORKER_CATEGORIES, "worker_categories", // Hide bottom bar on categories screen
        Routes.PROFILE_SETUP, "profile_setup", // Hide bottom bar on profile setup screen
        Routes.JOB_APPLICATION, "job_application", // Hide bottom bar on apply for job screen
        Routes.WORKER_AI_CHAT, // Hide bottom bar on DutyPe Assistant (AI chatbot) screen
        Routes.EMPLOYER_AI_CHAT // Hide bottom bar on Employer AI chatbot screen
    )

    // Update bottom bar visibility based on current route and scroll state
    val shouldShowBottomBar = when {
        currentRoute == null -> true
        routesWithoutBottomBar.any { route -> currentRoute.startsWith(route) } -> false
        else -> isBottomBarVisible // Use scroll-aware visibility
    }
    
    showBottomBar = shouldShowBottomBar

    // Main container that handles all system bars with white background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        // Status bar overlay - Dynamic color based on current screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(statusBarColor)
                .align(Alignment.TopCenter)
                .zIndex(1000f) // Ensure it's always on top
        )

        // Navigation bar overlay - Always show
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(navigationBarColor)
                .align(Alignment.BottomCenter)
                .zIndex(1000f) // Ensure it's always on top
        )

        // Main content area with white background
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                // Animated bottom bar visibility
                androidx.compose.animation.AnimatedVisibility(
                    visible = showBottomBar,
                    enter = androidx.compose.animation.slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = androidx.compose.animation.core.tween(200)
                    ),
                    exit = androidx.compose.animation.slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = androidx.compose.animation.core.tween(200)
                    )
                ) {
                    WorkerBottomBar(navController = navController)
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(
                        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = if (showBottomBar) paddingValues.calculateBottomPadding() else 0.dp
                    )
            ) {
                WorkerNavGraph(
                    navController = navController,
                    rootNavController = rootNavController,
                    onStatusBarColorChange = { color ->
                        statusBarColor = color
                        onStatusBarColorChange(color)
                    },
                    scrollStateManager = scrollStateManager,
                    notificationPermissionManager = notificationPermissionManager
                )
            }
        }
    }
}
