package com.example.dutype.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.dutype.navigation.WorkerBottomRoutes
import com.example.dutype.ui.theme.WorkerColors

/**
 * Airbnb-style Worker Bottom Navigation Bar.
 * - Flat, flush edge-to-edge layout with top hairline divider (0.75.dp)
 * - 4 cleanly positioned tabs: Home, Jobs, My Jobs, Account
 * - Subtle typography (10.5.sp) with #222222 active and #717171 inactive
 * - Lightweight outline vector icons when unselected, solid icons when active
 */
@Composable
fun WorkerBottomBar(
    navController: NavController,
    backgroundColor: Color = Color.White,
    selectedItemColor: Color = Color(0xFF222222),
    unselectedItemColor: Color = Color(0xFF717171),
    modifier: Modifier = Modifier
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    data class WorkerNavTab(
        val route: String,
        val title: String,
        val filledIconRes: Int,
        val outlinedIconRes: Int,
        val isMatchingRoute: (String?) -> Boolean
    )

    val navTabs = listOf(
        WorkerNavTab(
            route = WorkerBottomRoutes.HOME,
            title = stringResource(R.string.bottom_nav_home),
            filledIconRes = R.drawable.ic_airbnb_nav_home_filled,
            outlinedIconRes = R.drawable.ic_airbnb_nav_home,
            isMatchingRoute = { it == WorkerBottomRoutes.HOME }
        ),
        WorkerNavTab(
            route = WorkerBottomRoutes.JOBS,
            title = stringResource(R.string.bottom_nav_jobs),
            filledIconRes = R.drawable.ic_airbnb_nav_explore_filled,
            outlinedIconRes = R.drawable.ic_airbnb_nav_explore,
            isMatchingRoute = {
                it == WorkerBottomRoutes.JOBS ||
                it?.startsWith(Routes.WORKER_ALL_JOBS) == true
            }
        ),
        WorkerNavTab(
            route = WorkerBottomRoutes.MY_JOBS,
            title = stringResource(R.string.bottom_nav_my_jobs),
            filledIconRes = R.drawable.ic_airbnb_nav_briefcase_filled,
            outlinedIconRes = R.drawable.ic_airbnb_nav_briefcase,
            isMatchingRoute = { it == WorkerBottomRoutes.MY_JOBS }
        ),
        WorkerNavTab(
            route = WorkerBottomRoutes.PROFILE,
            title = stringResource(R.string.bottom_nav_account),
            filledIconRes = R.drawable.ic_airbnb_nav_profile_filled,
            outlinedIconRes = R.drawable.ic_airbnb_nav_profile,
            isMatchingRoute = {
                it == WorkerBottomRoutes.PROFILE ||
                it?.startsWith("worker_profile") == true
            }
        )
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Airbnb-style hairline top border
            HorizontalDivider(
                thickness = 0.75.dp,
                color = Color(0xFFEBEBEB)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(58.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navTabs.forEach { tab ->
                    val isSelected = tab.isMatchingRoute(currentRoute)
                    val iconColor = if (isSelected) selectedItemColor else unselectedItemColor
                    val textColor = if (isSelected) selectedItemColor else unselectedItemColor
                    val iconRes = if (isSelected) tab.filledIconRes else tab.outlinedIconRes

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { navigateToWorkerTab(navController, tab.route) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = tab.title,
                                tint = iconColor,
                                modifier = Modifier.size(25.dp)
                            )
                            Spacer(modifier = Modifier.height(3.5.dp))
                            Text(
                                text = tab.title,
                                color = textColor,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Switch worker tabs (Home / Jobs / My Jobs / Account) cleanly from the bottom bar.
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
