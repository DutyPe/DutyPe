package com.example.dutype.components

import com.dutype.app.R
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.dutype.ui.theme.IconSizes
import com.example.dutype.ui.theme.WorkerColors

/**
 * Worker Bottom Bar - Compact floating translucent capsule with circular selected indicator
 */
@Composable
fun WorkerBottomBar(
    navController: NavController,
    backgroundColor: Color = Color.White.copy(alpha = 0.92f),
    selectedItemColor: Color = WorkerColors.BottomNavSelected,
    unselectedItemColor: Color = WorkerColors.BottomNavUnselected,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    // Worker bottom bar items
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
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 32.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 240.dp)
                .fillMaxWidth(),
            shape = CircleShape,
            color = backgroundColor,
            shadowElevation = 8.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.85f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val label = stringResource(id = item.labelResId)

                    // Circular highlight container enclosing BOTH icon and text when selected
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .shadow(
                                            elevation = 3.dp,
                                            shape = CircleShape,
                                            spotColor = Color(0x30000000),
                                            ambientColor = Color(0x20000000)
                                        )
                                        .background(
                                            color = if (com.example.dutype.ui.theme.isAppInDarkTheme()) {
                                                Color(0xFF26262B)
                                            } else {
                                                Color(0xFFE2E8F0).copy(alpha = 0.65f)
                                            },
                                            shape = CircleShape
                                        )
                                } else {
                                    Modifier
                                }
                            )
                            .clip(CircleShape)
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
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val iconRes = if (isSelected) item.iconResFilled else item.iconResUnfilled
                            if (iconRes != null) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = label,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isSelected) selectedItemColor else unselectedItemColor
                                )
                            } else if (item.icon != null) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = label,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isSelected) selectedItemColor else unselectedItemColor
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
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
