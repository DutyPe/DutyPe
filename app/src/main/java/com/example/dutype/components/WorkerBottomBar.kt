package com.example.dutype.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Custom Worker Floating Bottom Navigation Bar.
 * Matches the Employer floating capsule specification with deep black active pill:
 * - Elevated white capsule container with smooth corner radius (36.dp)
 * - Active tab highlighted as a solid deep black pill (#0F172A) with white icon + title
 * - Inactive tabs displayed as minimalist outline vector icons
 */
@Composable
fun WorkerBottomBar(
    navController: NavController,
    backgroundColor: Color = WorkerColors.BottomNavBackground,
    selectedItemColor: Color = Color(0xFF0F172A),
    unselectedItemColor: Color = Color(0xFF64748B),
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
            filledIconRes = R.drawable.ic_home_filled,
            outlinedIconRes = R.drawable.ic_home_unfilled,
            isMatchingRoute = { it == WorkerBottomRoutes.HOME }
        ),
        WorkerNavTab(
            route = WorkerBottomRoutes.JOBS,
            title = stringResource(R.string.bottom_nav_jobs),
            filledIconRes = R.drawable.ic_emp_briefcase_filled,
            outlinedIconRes = R.drawable.ic_emp_briefcase,
            isMatchingRoute = {
                it == WorkerBottomRoutes.JOBS ||
                it?.startsWith(Routes.WORKER_ALL_JOBS) == true
            }
        ),
        WorkerNavTab(
            route = WorkerBottomRoutes.MY_JOBS,
            title = stringResource(R.string.bottom_nav_my_jobs),
            filledIconRes = R.drawable.ic_profile_history_filled,
            outlinedIconRes = R.drawable.ic_profile_history,
            isMatchingRoute = { it == WorkerBottomRoutes.MY_JOBS }
        ),
        WorkerNavTab(
            route = WorkerBottomRoutes.PROFILE,
            title = stringResource(R.string.bottom_nav_account),
            filledIconRes = R.drawable.ic_person_filled,
            outlinedIconRes = R.drawable.ic_person_unfilled,
            isMatchingRoute = {
                it == WorkerBottomRoutes.PROFILE ||
                it?.startsWith("worker_profile") == true
            }
        )
    )

    // Floating Capsule Surface
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        color = Color.White,
        shape = RoundedCornerShape(36.dp),
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navTabs.forEach { tab ->
                val isSelected = tab.isMatchingRoute(currentRoute)

                if (isSelected) {
                    // Active Pill Capsule (Black-based #0F172A)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(26.dp))
                            .background(selectedItemColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { navigateToWorkerTab(navController, tab.route) }
                            )
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                            .animateContentSize(animationSpec = tween(180)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = tab.filledIconRes),
                                contentDescription = tab.title,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = tab.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    // Inactive Icon Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { navigateToWorkerTab(navController, tab.route) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = tab.outlinedIconRes),
                            contentDescription = tab.title,
                            tint = unselectedItemColor,
                            modifier = Modifier.size(25.dp)
                        )
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
