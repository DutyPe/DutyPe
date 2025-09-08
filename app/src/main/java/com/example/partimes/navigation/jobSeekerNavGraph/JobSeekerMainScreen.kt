package com.example.partimes.navigation.jobSeekerNavGraph
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.partimes.utils.SystemUIConfigs

@Composable
fun JobSeekerMainScreen() {
    val navController = rememberNavController()
    var showBottomBar by remember { mutableStateOf(true) }

    // Apply jobseeker system UI configuration for consistent black navigation bar
    SystemUIConfigs.JobseekerHome()

    // Observe current route to determine when to show/hide bottom bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define routes that should not show the bottom bar
    val routesWithoutBottomBar = listOf(
         "security", "logout", "jobDetails", "chat_detail",
        "help", "chat_support", "call_support", "report", "tutorial",
        "faq", "aboutUs", "privacy", "terms"
    )

    // Update bottom bar visibility based on current route
    showBottomBar = when {
        currentRoute == null -> true
        routesWithoutBottomBar.any { route -> currentRoute.startsWith(route) } -> false
        else -> true
    }

    val animatedOffset by animateDpAsState(
        targetValue = if (showBottomBar) 0.dp else 100.dp,
        label = "bottom_bar_offset"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Fix for the system gesture indicator overlay
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier.offset(y = animatedOffset)
                ) {
                    BottomNavigationBar(navController = navController)
                }
            }
        }
    ) { innerPadding ->
        // Use NavHost directly here instead of wrapping in a Box
        JobSeekerNavGraph(
            navController = navController,
            modifier = Modifier.padding(
                // Apply bottom padding conditionally
                bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp,
                // Apply top/left/right padding always
                top = innerPadding.calculateTopPadding(),
                start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
            )
        )
    }
}
