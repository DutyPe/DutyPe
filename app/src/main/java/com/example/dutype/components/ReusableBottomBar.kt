package com.example.dutype.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dutype.app.R
import com.example.dutype.navigation.Routes

// Data class for bottom bar items
data class BottomBarItem(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
    val selectedIcon: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null,
    /** Optional drawable to show when this item is selected/active. If provided, this will be used instead of [iconRes] when selected. */
    @DrawableRes val iconResSelected: Int? = null
)

@Composable
fun ReusableBottomBar(
    navController: NavController,
    items: List<BottomBarItem>,
    backgroundColor: Color = Color.White,
    selectedItemColor: Color = Color.Black,
    unselectedItemColor: Color = Color(0xFF9CA3AF),
    modifier: Modifier = Modifier
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Clean bottom bar design
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = backgroundColor,
            shadowElevation = 12.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
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
                        // Icon
                        if (item.icon != null) {
                            val imageVector = if (isSelected && item.selectedIcon != null) item.selectedIcon else item.icon
                            Icon(
                                imageVector = imageVector!!,
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp),
                                tint = if (isSelected) selectedItemColor else unselectedItemColor
                            )
                        } else if (item.iconRes != null) {
                            val useRes = if (isSelected && item.iconResSelected != null) item.iconResSelected else item.iconRes
                            Icon(
                                painter = painterResource(id = useRes!!),
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp),
                                tint = if (isSelected) selectedItemColor else unselectedItemColor
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Label
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) selectedItemColor else unselectedItemColor
                        )
                    }
                }
            }
        }
    }
}

// Predefined bottom bar items for Worker
object WorkerBottomBarItems {
    val items = listOf(
        BottomBarItem(
            route = Routes.WORKER_HOME_TAB,
            label = "Jobs",
            iconRes = R.drawable.home
        ),
        BottomBarItem(
            route = Routes.WORKER_MY_JOBS,
            label = "My Jobs",
            iconRes = R.drawable.history
        ),
        BottomBarItem(
            route = Routes.WORKER_PROFILE,
            label = "Profile",
            icon = Icons.Outlined.Person,
            selectedIcon = Icons.Filled.Person
        )
    )
}

// Predefined bottom bar items for Employer
object EmployerBottomBarItems {
    val items = listOf(
        BottomBarItem(
            route = Routes.EMPLOYER_DASHBOARD,
            label = "Home",
            icon = Icons.Default.Home
        ),
        BottomBarItem(
            route = Routes.EMPLOYER_POST_JOB,
            label = "Post",
            icon = Icons.Default.AddCircle
        ),
        BottomBarItem(
            route = Routes.EMPLOYER_PROFILE,
            label = "Profile",
            icon = Icons.Default.Person
        )
    )
}
