package com.example.partimes.navigation.employer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import com.example.partimes.navigation.Routes

@Composable
fun EmployerBottomBar(navController: NavHostController) {
    val items = listOf(
        Triple(Routes.EMPLOYER_DASHBOARD, "Home", Icons.Default.Home),
        Triple("employer_post_job", "Post", Icons.Default.AddCircle),
        Triple(Routes.EMPLOYER_PROFILE, "Profile", Icons.Default.Person),
    )

    val lightBlueGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE3F2FD),
            Color(0xFFBBDEFB),
            Color(0xFF90CAF9)
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
                shape = RoundedCornerShape(0.dp),
                spotColor = Color(0xFF1976D2).copy(alpha = 0.1f)
            )
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

            items.forEach { (route, title, icon) ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            icon,
                            contentDescription = title,
                            modifier = Modifier.height(24.dp)
                        )
                    },
                    label = {
                        Text(
                            title,
                            fontWeight = if (currentRoute == route) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selected = currentRoute == route,
                    onClick = {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1565C0),
                        unselectedIconColor = Color(0xFF1976D2).copy(alpha = 0.7f),
                        selectedTextColor = Color(0xFF1565C0),
                        unselectedTextColor = Color(0xFF1976D2).copy(alpha = 0.7f),
                        indicatorColor = Color.White.copy(alpha = 0.8f)
                    )
                )
            }
        }
    }
}