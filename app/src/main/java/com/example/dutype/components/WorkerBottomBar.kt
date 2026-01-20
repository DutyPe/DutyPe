package com.example.dutype.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dutype.app.R
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.MeeshoFontFamily

/**
 * Worker Bottom Bar - PhonePe/Paytm style bottom navigation
 * Clean, lightweight outlined icons with light gray color scheme
 */
@Composable
fun WorkerBottomBar(
    navController: NavController,
    backgroundColor: Color = Color.White,
    selectedItemColor: Color = Color(0xFF1F2937), // Dark gray for selected
    unselectedItemColor: Color = Color(0xFF9CA3AF), // Light gray for unselected
    modifier: Modifier = Modifier
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    // Worker bottom bar items - Clean outlined icons
    val items = listOf(
        WorkerBottomBarItem(
            route = Routes.WORKER_HOME_TAB,
            labelResId = R.string.bottom_nav_home,
            icon = Icons.Outlined.Home
        ),
        WorkerBottomBarItem(
            route = Routes.WORKER_MY_JOBS,
            labelResId = R.string.bottom_nav_my_jobs,
            icon = Icons.Outlined.WorkOutline
        ),
        WorkerBottomBarItem(
            route = Routes.WORKER_PROFILE,
            labelResId = R.string.bottom_nav_account,
            icon = Icons.Outlined.AccountCircle
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = backgroundColor,
            shadowElevation = 0.dp, // No shadow for clean look
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // Slightly shorter for modern look
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val label = stringResource(id = item.labelResId)
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp), // No gap between icon and label
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                    ) {
                        // Icon - Outlined style (26dp for better visibility)
                        if (item.icon != null) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = label,
                                modifier = Modifier.size(26.dp), // Increased from 22dp to 26dp
                                tint = if (isSelected) selectedItemColor else unselectedItemColor
                            )
                        } else if (item.iconRes != null) {
                            Icon(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = label,
                                modifier = Modifier.size(26.dp), // Increased from 22dp to 26dp
                                tint = if (isSelected) selectedItemColor else unselectedItemColor
                            )
                        }
                        
                        // Label - Clean, lightweight text
                        Text(
                            text = label,
                            fontFamily = MeeshoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) selectedItemColor else unselectedItemColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// Data class for worker bottom bar items
private data class WorkerBottomBarItem(
    val route: String,
    @StringRes val labelResId: Int,
    val icon: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null
)
