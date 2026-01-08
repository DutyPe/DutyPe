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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Person
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
import com.example.dutype.ui.theme.WorkerColors

/**
 * Worker Bottom Bar - Bottom navigation for worker side
 */
@Composable
fun WorkerBottomBar(
    navController: NavController,
    backgroundColor: Color = WorkerColors.BottomNavBackground,
    selectedItemColor: Color = WorkerColors.BottomNavSelected,
    unselectedItemColor: Color = WorkerColors.BottomNavUnselected,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    // Worker bottom bar items
    val items = listOf(
        WorkerBottomBarItem(
            route = Routes.WORKER_HOME_TAB,
            labelResId = R.string.bottom_nav_jobs,
            iconRes = R.drawable.home
        ),
        WorkerBottomBarItem(
            route = Routes.WORKER_MY_JOBS,
            labelResId = R.string.bottom_nav_my_jobs,
            iconRes = R.drawable.history
        ),
        WorkerBottomBarItem(
            route = Routes.WORKER_PROFILE,
            labelResId = R.string.profile,
            icon = Icons.Outlined.Person,
            selectedIcon = Icons.Filled.Person
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
            shadowElevation = 4.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val label = stringResource(id = item.labelResId)
                    
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
                                contentDescription = label,
                                modifier = Modifier.size(26.dp),
                                tint = if (isSelected) selectedItemColor else unselectedItemColor
                            )
                        } else if (item.iconRes != null) {
                            val useRes = if (isSelected && item.iconResSelected != null) item.iconResSelected else item.iconRes
                            Icon(
                                painter = painterResource(id = useRes!!),
                                contentDescription = label,
                                modifier = Modifier.size(26.dp),
                                tint = if (isSelected) selectedItemColor else unselectedItemColor
                            )
                        }
                        
                        // Label
                        Text(
                            text = label,
                            fontFamily = MeeshoFontFamily,
                            fontSize = 10.sp,
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
    val selectedIcon: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null,
    @DrawableRes val iconResSelected: Int? = null
)
