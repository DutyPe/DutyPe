package com.example.partimes.navigation.employer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.partimes.employer.screens.EmployerScreen

@Composable
fun EmployerBottomBar(navController: NavHostController) {
    val items = listOf(
        EmployerScreen.Dashboard,
        EmployerScreen.PostJob,
        EmployerScreen.Profile,
    )

    // Enhanced light blue gradient for modern, smooth appearance
    val lightBlueGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE3F2FD), // Very light blue at top
            Color(0xFFBBDEFB), // Light blue
            Color(0xFF90CAF9)  // Slightly deeper blue at bottom
        ),
        startY = 0f,
        endY = 300f
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                spotColor = Color(0xFF1976D2).copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(lightBlueGradient)
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            val navBackStackEntry = navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry.value?.destination?.route

            items.forEach { screen ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            screen.icon,
                            contentDescription = screen.title,
                            modifier = Modifier.height(24.dp)
                        )
                    },
                    label = {
                        Text(
                            screen.title,
                            fontWeight = if (currentRoute == screen.route) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selected = currentRoute == screen.route,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1565C0), // Deep blue for selected
                        unselectedIconColor = Color(0xFF1976D2).copy(alpha = 0.7f), // Medium blue for unselected
                        selectedTextColor = Color(0xFF1565C0),
                        unselectedTextColor = Color(0xFF1976D2).copy(alpha = 0.7f),
                        indicatorColor = Color.White.copy(alpha = 0.8f) // Subtle white indicator
                    )
                )
            }
        }
    }
}
