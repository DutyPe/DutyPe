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
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonSearch
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.ui.theme.IconSizes
import com.example.dutype.ui.theme.WorkerColors

/**
 * Custom Employer Bottom Bar - Classic edge-to-edge docked bottom navigation
 * Matches Worker bottom bar 1:1 with Home, Post Job, and Profile tabs
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

    fun openPostJob() {
        navigateTo(Routes.EMPLOYER_POST_JOB)
    }

    data class EmployerNavTab(
        val route: String,
        val title: String,
        val vernacular: String,
        val iconResUnfilled: Int? = null,
        val iconResFilled: Int? = null,
        val vectorIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
    )

    val navTabs = listOf(
        EmployerNavTab(
            route = Routes.EMPLOYER_DASHBOARD,
            title = "Home",
            vernacular = "होम",
            iconResUnfilled = R.drawable.ic_home_unfilled,
            iconResFilled = R.drawable.ic_home_filled
        ),
        EmployerNavTab(
            route = Routes.EMPLOYER_MY_JOBS,
            title = "My Jobs",
            vernacular = "मेरे काम",
            iconResUnfilled = R.drawable.myjobs,
            iconResFilled = R.drawable.myjobs
        ),
        EmployerNavTab(
            route = Routes.EMPLOYER_POST_JOB,
            title = "Find Workers",
            vernacular = "कारीगर खोजें",
            vectorIcon = Icons.Default.PersonSearch
        ),
        EmployerNavTab(
            route = Routes.EMPLOYER_PROFILE,
            title = "Account",
            vernacular = "खाता",
            iconResUnfilled = R.drawable.ic_person_unfilled,
            iconResFilled = R.drawable.ic_person_filled
        )
    )

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Hairline top divider
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE2E8F0))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
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
                                    modifier = Modifier.size(22.dp),
                                    tint = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B)
                                )
                            } else {
                                val iconRes = if (isSelected) tab.iconResFilled ?: tab.iconResUnfilled!! else tab.iconResUnfilled!!
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
                                color = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B),
                                maxLines = 1
                            )
                            Text(
                                text = tab.vernacular,
                                fontSize = 9.5.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                                color = if (isSelected) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
