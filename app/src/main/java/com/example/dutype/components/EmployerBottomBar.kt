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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HomeRepairService
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dutype.app.R
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.EmployerColors

/**
 * Custom Employer Floating Bottom Navigation Bar.
 * Designed to strictly match the reference pill navigation:
 * - Elevated white capsule container with smooth corner radius (36.dp)
 * - Active tab highlighted as a solid vibrant orange pill (#E85D04) with white icon + title
 * - Inactive tabs displayed as minimalist outline vector icons
 */
@Composable
fun EmployerBottomBar(
    navController: NavController,
    backgroundColor: Color = EmployerColors.BottomNavBackground,
    selectedItemColor: Color = Color(0xFFE85D04),
    unselectedItemColor: Color = Color(0xFF64748B),
    modifier: Modifier = Modifier
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    fun navigateTo(route: String) {
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    data class EmployerNavTab(
        val route: String,
        val title: String,
        val filledIcon: ImageVector,
        val outlinedIcon: ImageVector,
        val isMatchingRoute: (String?) -> Boolean
    )

    val navTabs = listOf(
        EmployerNavTab(
            route = Routes.EMPLOYER_DASHBOARD,
            title = "Hire",
            filledIcon = Icons.Filled.Home,
            outlinedIcon = Icons.Outlined.Home,
            isMatchingRoute = { it == Routes.EMPLOYER_DASHBOARD || it == Routes.EMPLOYER_HOME }
        ),
        EmployerNavTab(
            route = Routes.SERVICES,
            title = "Services",
            filledIcon = Icons.Filled.HomeRepairService,
            outlinedIcon = Icons.Outlined.HomeRepairService,
            isMatchingRoute = {
                it == Routes.SERVICES ||
                it?.startsWith("services") == true
            }
        ),
        EmployerNavTab(
            route = Routes.EMPLOYER_POST_JOB,
            title = stringResource(R.string.post_job),
            filledIcon = Icons.Filled.AddCircle,
            outlinedIcon = Icons.Outlined.AddCircleOutline,
            isMatchingRoute = {
                it == Routes.EMPLOYER_POST_JOB ||
                it?.startsWith(Routes.EMPLOYER_POST_JOB) == true ||
                it?.startsWith(Routes.EMPLOYER_POST_URGENT_NEED) == true
            }
        ),
        EmployerNavTab(
            route = Routes.EMPLOYER_PROFILE,
            title = stringResource(R.string.bottom_nav_account),
            filledIcon = Icons.Filled.Person,
            outlinedIcon = Icons.Outlined.Person,
            isMatchingRoute = { it == Routes.EMPLOYER_PROFILE || it?.startsWith("employer_profile") == true }
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
                    // Active Pill Capsule
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(26.dp))
                            .background(selectedItemColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { navigateTo(tab.route) }
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
                                imageVector = tab.filledIcon,
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
                                onClick = { navigateTo(tab.route) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.outlinedIcon,
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
