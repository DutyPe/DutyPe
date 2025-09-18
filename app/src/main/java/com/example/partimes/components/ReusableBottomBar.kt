package com.example.partimes.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.partimes.R
import com.example.partimes.navigation.Routes

// Data class for bottom bar items
data class BottomBarItem(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null
)

@Composable
fun ReusableBottomBar(
    navController: NavController,
    items: List<BottomBarItem>,
    backgroundColor: Color = Color(0xF8F4F5FA),
    selectedItemColor: Color = Color(0xFF1E40AF),
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
        // Curved bottom bar with shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    spotColor = Color.Black.copy(alpha = 0.1f)
                )
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 1.dp)
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
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Use ImageVector if provided, otherwise use drawable resource
                            if (item.icon != null) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(28.dp),
                                    tint = if (isSelected) selectedItemColor else unselectedItemColor
                                )
                            } else if (item.iconRes != null) {
                                Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = item.label,
                                    modifier = Modifier.size(28.dp),
                                    tint = if (isSelected) selectedItemColor else unselectedItemColor
                                )
                            }
                        }

                        // Label
                        Text(
                            text = item.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) selectedItemColor else unselectedItemColor
                        )
                    }
                }
            }
        }
    }
}

// Predefined bottom bar items for JobSeeker
object JobSeekerBottomBarItems {
    val items = listOf(
        BottomBarItem(
            route = Routes.JOBSEEKER_HOME_TAB,
            label = "Jobs",
            iconRes = R.drawable.home
        ),
        BottomBarItem(
            route = Routes.JOBSEEKER_MY_JOBS,
            label = "My Jobs",
            iconRes = R.drawable.history
        ),
        BottomBarItem(
            route = Routes.JOBSEEKER_PROFILE,
            label = "Profile",
            iconRes = R.drawable.profile
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
            route = "employer_post_job",
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
