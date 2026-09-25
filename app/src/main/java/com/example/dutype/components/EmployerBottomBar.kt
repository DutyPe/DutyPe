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

    val sideItems = listOf(
        Triple(Routes.EMPLOYER_DASHBOARD, R.string.bottom_nav_home, Pair(R.drawable.ic_home_unfilled, R.drawable.ic_home_filled)),
        Triple(Routes.EMPLOYER_PROFILE, R.string.profile, Pair(R.drawable.ic_person_unfilled, R.drawable.ic_person_filled))
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
                // Hairline top divider
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
                    // Home tab
                    val homeItem = sideItems[0]
                    EmployerBottomTab(
                        route = homeItem.first,
                        labelResId = homeItem.second,
                        iconUnfilled = homeItem.third.first,
                        iconFilled = homeItem.third.second,
                        isSelected = currentRoute == homeItem.first,
                        selectedItemColor = selectedItemColor,
                        unselectedItemColor = unselectedItemColor,
                        onClick = { navigateTo(homeItem.first) }
                    )

                    // Post Job tab — middle position
                    val postSelected = currentRoute == Routes.EMPLOYER_POST_JOB
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = ::openPostJob
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.post_job),
                            contentDescription = stringResource(id = R.string.bottom_nav_post),
                            modifier = Modifier.size(IconSizes.Standard),
                            tint = if (postSelected) selectedItemColor else unselectedItemColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = R.string.bottom_nav_post),
                            style = if (postSelected) AppTypography.bottomNavLabelSelected else AppTypography.bottomNavLabel,
                            color = if (postSelected) selectedItemColor else unselectedItemColor,
                            maxLines = 1
                        )
                    }

                    // Profile tab
                    val profileItem = sideItems[1]
                    EmployerBottomTab(
                        route = profileItem.first,
                        labelResId = profileItem.second,
                        iconUnfilled = profileItem.third.first,
                        iconFilled = profileItem.third.second,
                        isSelected = currentRoute == profileItem.first,
                        selectedItemColor = selectedItemColor,
                        unselectedItemColor = unselectedItemColor,
                        onClick = { navigateTo(profileItem.first) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.EmployerBottomTab(
    route: String,
    labelResId: Int,
    iconUnfilled: Int,
    iconFilled: Int,
    isSelected: Boolean,
    selectedItemColor: Color,
    unselectedItemColor: Color,
    onClick: () -> Unit,
) {
    val label = stringResource(id = labelResId)
    val iconRes = if (isSelected) iconFilled else iconUnfilled
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(IconSizes.Standard),
            tint = if (isSelected) selectedItemColor else unselectedItemColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = if (isSelected) AppTypography.bottomNavLabelSelected else AppTypography.bottomNavLabel,
            color = if (isSelected) selectedItemColor else unselectedItemColor,
            maxLines = 1
        )
    }
}
