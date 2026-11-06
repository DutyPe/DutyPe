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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HomeRepairService
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import timber.log.Timber
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dutype.app.R
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.EmployerColors

/**
 * Airbnb-style Employer Bottom Navigation Bar.
 * - Flat, flush edge-to-edge layout with top hairline divider (0.75.dp)
 * - 3 cleanly positioned tabs: 1. Hire, 2. Post Job, 3. Account
 * - Exact outline & filled vector icons (25.dp) modeled after Airbnb
 * - Clean typography (10.5.sp) with #222222 active and #717171 inactive
 */
@Composable
fun EmployerBottomBar(
    navController: NavController,
    backgroundColor: Color = Color.White,
    selectedItemColor: Color = Color(0xFF222222),
    unselectedItemColor: Color = Color(0xFF717171),
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route
    var lastNavTime by remember { mutableLongStateOf(0L) }

    fun navigateTo(route: String) {
        val now = System.currentTimeMillis()
        if (now - lastNavTime < 400L) return // Debounce rapid consecutive taps

        // Lifecycle check: only navigate if screen is in RESUMED state
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            return
        }

        if (currentRoute != route) {
            lastNavTime = now
            try {
                if (navController.graph.findNode(route) != null) {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                } else {
                    Timber.w("EmployerBottomBar: route %s not found in graph", route)
                }
            } catch (e: Exception) {
                Timber.e(e, "EmployerBottomBar: navigation to %s failed", route)
            }
        }
    }

    data class EmployerNavTab(
        val route: String,
        val title: String,
        val filledIconRes: Int,
        val outlinedIconRes: Int,
        val isMatchingRoute: (String?) -> Boolean
    )

    // Order: 1. Hire, 2. Post Job, 3. Account
    val navTabs = listOf(
        EmployerNavTab(
            route = Routes.EMPLOYER_DASHBOARD,
            title = "Hire",
            filledIconRes = R.drawable.ic_airbnb_nav_home_filled,
            outlinedIconRes = R.drawable.ic_airbnb_nav_home,
            isMatchingRoute = { it == Routes.EMPLOYER_DASHBOARD || it == Routes.EMPLOYER_HOME }
        ),
        EmployerNavTab(
            route = Routes.EMPLOYER_POST_JOB,
            title = stringResource(R.string.post_job),
            filledIconRes = R.drawable.ic_airbnb_nav_post_filled,
            outlinedIconRes = R.drawable.ic_airbnb_nav_post,
            isMatchingRoute = {
                it == Routes.EMPLOYER_POST_JOB ||
                it?.startsWith(Routes.EMPLOYER_POST_JOB) == true ||
                it?.startsWith(Routes.EMPLOYER_POST_URGENT_NEED) == true
            }
        ),
        
        EmployerNavTab(
            route = Routes.EMPLOYER_PROFILE,
            title = stringResource(R.string.bottom_nav_account),
            filledIconRes = R.drawable.ic_airbnb_nav_profile_filled,
            outlinedIconRes = R.drawable.ic_airbnb_nav_profile,
            isMatchingRoute = { it == Routes.EMPLOYER_PROFILE || it?.startsWith("employer_profile") == true }
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
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "tabScale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { navigateTo(tab.route) }
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
                                modifier = Modifier
                                    .size(25.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
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
