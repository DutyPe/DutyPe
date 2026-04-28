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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Apr 2026 fix: drive the status-bar colour deterministically from the
    // current route, not from a per-screen `LaunchedEffect` callback. The
    // old approach had a race on cold install — `WorkerHomeScreen`'s
    // `LaunchedEffect(Unit)` ran AFTER the activity's initial
    // `enableEdgeToEdge`, so the home screen showed white on first install
    // and only flipped to purple after a process restart. Routing the
    // colour from `currentRoute` makes it correct at every transition.
    //
    // Rule: ONLY the worker home screen uses the purple header colour.
    // Every other worker screen (and any unknown route) gets pure white
    // so the status bar matches the screen's white app-bar background.
    val statusBarColor = if (currentRoute == WorkerBottomRoutes.HOME) {
        com.example.dutype.worker.screens.WorkerHomeHeaderTopColor
    } else {
        com.example.dutype.ui.theme.WorkerColors.ScreenBackground
    }
    val navigationBarColor = com.example.dutype.ui.theme.WorkerColors.BottomNavBackground

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

    // Define routes that should not show the bottom bar
    val routesWithoutBottomBar = listOf(
        Routes.JOB_DETAIL,
        Routes.HELP, Routes.REPORT,
        Routes.ABOUT_US,
        Routes.WORKER_NOTIFICATIONS, Routes.WORKER_ALL_JOBS, "worker_all_jobs",
        Routes.WORKER_JOB_MAP, // Hide bottom bar on map screen
        Routes.WORKER_EARNINGS, // Hide bottom bar on earnings screen
        Routes.WORKER_HISTORY, // Hide bottom bar on work history screen
        Routes.WORKER_REFER_EARN, // Hide bottom bar on refer & earn screen
        Routes.WORKER_CATEGORIES, "worker_categories", // Hide bottom bar on categories screen
        Routes.PROFILE_SETUP, "profile_setup", // Hide bottom bar on profile setup screen
        Routes.JOB_APPLICATION, "job_application" // Hide bottom bar on apply for job screen
    )

    // Update bottom bar visibility based on current route and scroll state
    val shouldShowBottomBar = when {
        currentRoute == null -> true
        routesWithoutBottomBar.any { route -> currentRoute.startsWith(route) } -> false
        else -> isBottomBarVisible // Use scroll-aware visibility
    }
    
    showBottomBar = shouldShowBottomBar

    // ---- ROLE THEME ----
    // Publish the worker palette via LocalRoleColors so every screen and card
    // hosted under this scaffold reads its background / card / divider colour
    // from the same source (no per-screen hardcoded hex).
    com.example.dutype.ui.theme.DutyPeWorkerTheme {
        val roleColors = com.example.dutype.ui.theme.LocalRoleColors.current

        // Main container that handles all system bars with the role background.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(roleColors.screenBackground)
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

        // Main content area with role background
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(roleColors.screenBackground),
            containerColor = roleColors.screenBackground,
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
                    .background(roleColors.screenBackground)
                    .padding(
                        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = if (showBottomBar) paddingValues.calculateBottomPadding() else 0.dp
                    )
            ) {
                WorkerNavGraph(
                    navController = navController,
                    rootNavController = rootNavController,
                    // Per-screen colour callbacks are now no-ops; the
                    // status-bar colour is derived from `currentRoute`
                    // above. We still forward to the parent in case the
                    // host activity (MainActivity) wants to react.
                    onStatusBarColorChange = { color -> onStatusBarColorChange(color) },
                    scrollStateManager = scrollStateManager,
                    notificationPermissionManager = notificationPermissionManager
                )
            }
        }
    }
    } // end DutyPeWorkerTheme
}
