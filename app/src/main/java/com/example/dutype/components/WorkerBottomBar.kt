package com.example.dutype.components

import com.dutype.app.R
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.IconSizes
import com.example.dutype.ui.theme.WorkerColors

/**
 * Worker Bottom Bar - Classic edge-to-edge docked bottom navigation
 * Clean, lightweight outlined/filled icons with familiar PhonePe/Paytm style
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

    // Worker bottom bar items - Custom icons with filled/unfilled states
    val items = listOf(
        WorkerBottomBarItem(
            route = com.example.dutype.navigation.WorkerBottomRoutes.HOME,
            labelResId = R.string.bottom_nav_home,
            iconResUnfilled = R.drawable.ic_home_unfilled,
            iconResFilled = R.drawable.ic_home_filled
        ),
        WorkerBottomBarItem(
            route = com.example.dutype.navigation.WorkerBottomRoutes.MY_JOBS,
            labelResId = R.string.bottom_nav_my_jobs,
            iconResUnfilled = R.drawable.myjobs,
            iconResFilled = R.drawable.myjobs
        ),
        WorkerBottomBarItem(
            route = com.example.dutype.navigation.WorkerBottomRoutes.PROFILE,
            labelResId = R.string.bottom_nav_account,
            iconResUnfilled = R.drawable.ic_person_unfilled,
            iconResFilled = R.drawable.ic_person_filled
        )
    )

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = backgroundColor,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Top border - very subtle light divider
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(WorkerColors.Border)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
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
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                        ) {
                            val iconRes = if (isSelected) item.iconResFilled else item.iconResUnfilled
                            if (iconRes != null) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = label,
                                    modifier = Modifier.size(IconSizes.Standard),
                                    tint = if (isSelected) selectedItemColor else unselectedItemColor
                                )
                            } else if (item.icon != null) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = label,
                                    modifier = Modifier.size(IconSizes.Standard),
                                    tint = if (isSelected) selectedItemColor else unselectedItemColor
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = label,
                                style = if (isSelected) AppTypography.bottomNavLabelSelected else AppTypography.bottomNavLabel,
                                color = if (isSelected) selectedItemColor else unselectedItemColor,
                                maxLines = 1
                            )
                        }
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
    @DrawableRes val iconResUnfilled: Int? = null,
    @DrawableRes val iconResFilled: Int? = null
)
