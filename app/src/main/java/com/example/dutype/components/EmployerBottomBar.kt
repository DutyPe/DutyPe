package com.example.dutype.components

import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.example.dutype.ui.theme.bd
import com.dutype.app.R
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
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.EmployerColors

/**
 * Custom Employer Bottom Bar - Flat, docked bottom navigation with sleek outline/filled vector icons.
 * Features Home, Services, Post Job, and Account tabs.
 */
@Composable
fun EmployerBottomBar(
    navController: NavController,
    backgroundColor: Color = EmployerColors.BottomNavBackground,
    selectedItemColor: Color = EmployerColors.BottomNavSelected,
    unselectedItemColor: Color = EmployerColors.BottomNavUnselected,
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
        val iconRes: Int,
        val iconFilledRes: Int
    )

    // 4 clean tabs with filled/outlined icon states
    val navTabs = listOf(
        EmployerNavTab(
            route = Routes.SERVICES,
            title = stringResource(R.string.bottom_nav_home),
            iconRes = R.drawable.ic_home_unfilled,
            iconFilledRes = R.drawable.ic_home_filled
        ),
        EmployerNavTab(
            route = Routes.EMPLOYER_DASHBOARD,
            title = "Hire Workers",
            iconRes = R.drawable.ic_emp_briefcase,
            iconFilledRes = R.drawable.ic_emp_briefcase_filled
        ),
        EmployerNavTab(
            route = Routes.EMPLOYER_POST_JOB,
            title = stringResource(R.string.post_job),
            iconRes = R.drawable.ic_nav_post_job,
            iconFilledRes = R.drawable.ic_nav_post_job_filled
        ),
        EmployerNavTab(
            route = Routes.EMPLOYER_PROFILE,
            title = stringResource(R.string.bottom_nav_account),
            iconRes = R.drawable.ic_person_unfilled,
            iconFilledRes = R.drawable.ic_person_filled
        )
    )

    val selectedColor = Color(0xFF0F172A).fg()
    val unselectedColor = Color(0xFF64748B).fg()

    // Fully flat docked bar spanning edge-to-edge
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
                navTabs.forEach { tab ->
                    val isSelected = currentRoute == tab.route
                    val tint = if (isSelected) selectedColor else unselectedColor
                    val activeIconRes = if (isSelected) tab.iconFilledRes else tab.iconRes
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { navigateTo(tab.route) }
                            )
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = activeIconRes),
                            contentDescription = tab.title,
                            modifier = Modifier.size(25.dp),
                            tint = tint
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = tab.title,
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
