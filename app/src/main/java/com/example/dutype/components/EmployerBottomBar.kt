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
import androidx.compose.ui.res.painterResource
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
 * Custom Employer Bottom Bar - Fully flat, edge-to-edge docked bottom navigation.
 * No pill/tablet gap on sides. Features Home, Post Job, and Account tabs.
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
        val iconResUnfilled: Int? = null,
        val iconResFilled: Int? = null,
        val vectorIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
    )

    // 3 clean tabs: Home, Post Job (Instant + Regular), and Account (Profile)
    val navTabs = listOf(
        EmployerNavTab(
            route = Routes.EMPLOYER_DASHBOARD,
            title = stringResource(R.string.bottom_nav_home),
            iconResUnfilled = R.drawable.ic_home_unfilled,
            iconResFilled = R.drawable.ic_home_filled
        ),
        EmployerNavTab(
            route = Routes.EMPLOYER_POST_JOB,
            title = stringResource(R.string.post_job),
            iconResUnfilled = R.drawable.ic_emp_briefcase,
            iconResFilled = R.drawable.ic_emp_briefcase
        ),
        EmployerNavTab(
            route = Routes.EMPLOYER_PROFILE,
            title = stringResource(R.string.bottom_nav_account),
            iconResUnfilled = R.drawable.ic_person_unfilled,
            iconResFilled = R.drawable.ic_person_filled
        )
    )

    // Fully flat docked bar spanning edge-to-edge with no floating pill tablet gaps
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
                        if (tab.vectorIcon != null) {
                            Icon(
                                imageVector = tab.vectorIcon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(24.dp),
                                tint = if (isSelected) Color(0xFF0F172A).fg() else Color(0xFF64748B).fg()
                            )
                        } else {
                            val iconRes = if (isSelected) tab.iconResFilled ?: tab.iconResUnfilled!! else tab.iconResUnfilled!!
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = tab.title,
                                modifier = Modifier.size(24.dp),
                                tint = if (isSelected) Color(0xFF0F172A).fg() else Color(0xFF64748B).fg()
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = tab.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF0F172A).fg() else Color(0xFF64748B).fg(),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
