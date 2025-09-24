package com.example.partimes.navigation.jobSeekerNavGraph

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
import com.example.partimes.components.ReusableBottomBar
import com.example.partimes.components.JobSeekerBottomBarItems
import com.example.partimes.navigation.Routes
import com.example.partimes.utils.rememberScrollStateManager
import com.example.partimes.utils.rememberWindowSizeClass

@Composable
fun JobseekerMainScreen(
    rootNavController: NavHostController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val navController = rememberNavController()
    val view = LocalView.current
    val scrollStateManager = rememberScrollStateManager()
    val windowSizeClass = rememberWindowSizeClass()
    
    var showBottomBar by remember { mutableStateOf(true) }
    val isBottomBarVisible by scrollStateManager.isBottomBarVisible

    // Fixed colors for jobseeker side
    val statusBarColor = Color.White // White status bar
    val navigationBarColor = Color.Black // Always show navigation bar

    // Apply system bar colors immediately
    LaunchedEffect(Unit) {
        val window = (view.context as android.app.Activity).window
        window.statusBarColor = statusBarColor.toArgb()
        window.navigationBarColor = navigationBarColor.toArgb()
        
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true // Dark icons on white
        insetsController.isAppearanceLightNavigationBars = false // Light icons on black
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define routes that should not show the bottom bar
    val routesWithoutBottomBar = listOf(
        Routes.SECURITY, Routes.LOGOUT, Routes.JOB_DETAIL, Routes.CHAT_DETAIL,
        Routes.HELP, Routes.CHAT_SUPPORT, Routes.CALL_SUPPORT, Routes.REPORT, 
        Routes.TUTORIAL, Routes.FAQ, Routes.ABOUT_US, Routes.PRIVACY, Routes.TERMS,
        Routes.NOTIFICATION_CENTER, Routes.ADVANCED_PROFILE, Routes.SKILLS_MANAGEMENT,
        Routes.RESUME_UPLOAD, Routes.VERIFICATION, Routes.PERSONAL_INFO,
        Routes.WORK_EXPERIENCE, Routes.EDUCATION, Routes.WORK_PREFERENCES
    )

    // Update bottom bar visibility based on current route
    showBottomBar = when {
        currentRoute == null -> true
        routesWithoutBottomBar.any { route -> currentRoute.startsWith(route) } -> false
        else -> true
    }

    // Main container that handles all system bars with white background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        // Status bar overlay - ALWAYS at the top with light blue
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
                if (showBottomBar) {
                    ReusableBottomBar(
                        navController = navController,
                        items = JobSeekerBottomBarItems.items
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = paddingValues.calculateBottomPadding()
                    )
            ) {
                JobseekerNavGraph(
                    navController = navController,
                    rootNavController = rootNavController,
                    onStatusBarColorChange = onStatusBarColorChange,
                    scrollStateManager = scrollStateManager
                )
            }
        }
    }
}