package com.example.dutype.components

import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.example.dutype.ui.theme.bd
import com.dutype.app.R
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HomeRepairService
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dutype.navigation.WorkerBottomRoutes
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.WorkerColors

/**
 * Worker bottom bar — flat, docked bottom navigation with sleek vector icons.
 * Tabs: Home, Jobs, Services, My Jobs, Account.
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

    val items = listOf(
        WorkerBottomBarItem(
            route = WorkerBottomRoutes.HOME,
            labelResId = R.string.bottom_nav_home,
            iconRes = R.drawable.ic_home_unfilled,
            iconFilledRes = R.drawable.ic_home_filled
        ),
        WorkerBottomBarItem(
            route = WorkerBottomRoutes.JOBS,
            matchPrefix = Routes.WORKER_ALL_JOBS,
            labelResId = R.string.bottom_nav_jobs,
            iconRes = R.drawable.ic_emp_briefcase,
            iconFilledRes = R.drawable.ic_emp_briefcase_filled
        ),
        WorkerBottomBarItem(
            route = WorkerBottomRoutes.MY_JOBS,
            labelResId = R.string.bottom_nav_my_jobs,
            iconRes = R.drawable.ic_profile_history,
            iconFilledRes = R.drawable.ic_profile_history_filled
        ),
        WorkerBottomBarItem(
            route = WorkerBottomRoutes.PROFILE,
            labelResId = R.string.bottom_nav_account,
            iconRes = R.drawable.ic_person_unfilled,
            iconFilledRes = R.drawable.ic_person_filled
        )
    )
    val selectedColor = Color(0xFF0F172A).fg()
    val unselectedColor = Color(0xFF64748B).fg()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White.bg(),
        shape = RectangleShape,
        shadowElevation = 6.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE2E8F0).bd())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = if (item.matchPrefix != null) {
                        currentRoute?.startsWith(item.matchPrefix) == true
                    } else {
                        currentRoute == item.route
                    }
                    val label = stringResource(id = item.labelResId)
                    val tint = if (isSelected) selectedColor else unselectedColor
                    val activeIconRes = if (isSelected) (item.iconFilledRes ?: item.iconRes) else item.iconRes
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
                                if (!isSelected) navigateToWorkerTab(navController, item.route)
                            }
                    ) {
                        if (activeIconRes != null) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = activeIconRes),
                                contentDescription = label,
                                modifier = Modifier.size(25.dp),
                                tint = tint
                            )
                        } else {
                            val vectorIcon = if (isSelected) (item.iconSelected ?: item.icon) else item.icon
                            if (vectorIcon != null) {
                                Icon(
                                    imageVector = vectorIcon,
                                    contentDescription = label,
                                    modifier = Modifier.size(25.dp),
                                    tint = tint
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = tint,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Switch worker tabs (Home / Jobs / Services / My Jobs / Account) cleanly from the bottom bar.
 */
fun navigateToWorkerTab(navController: NavController, route: String, restoreState: Boolean = true) {
    val startId = navController.graph.findStartDestination().id
    if (route == WorkerBottomRoutes.HOME) {
        if (!navController.popBackStack(startId, inclusive = false, saveState = true)) {
            navController.navigate(route) { launchSingleTop = true }
        }
        return
    }
    navController.navigate(route) {
        popUpTo(startId) { saveState = true }
        launchSingleTop = true
        this.restoreState = restoreState
    }
}

// Data class for worker bottom bar items
private data class WorkerBottomBarItem(
    val route: String,
    @StringRes val labelResId: Int,
    val icon: ImageVector? = null,
    val iconSelected: ImageVector? = null,
    val iconRes: Int? = null,
    val iconFilledRes: Int? = null,
    val matchPrefix: String? = null
)
