package com.example.dutype.components

import com.dutype.app.R
import android.app.Activity
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.IconSizes
import com.example.dutype.ui.theme.WorkerColors
import timber.log.Timber

/**
 * Custom Employer Bottom Bar with Interstitial Ad before Post Job
 * 
 * Shows an interstitial ad when employer clicks "Post" button before navigating to Post Job screen
 * 
 * Material Design 3 Compliant:
 * - Height: 80dp (Material Design 3 standard)
 * - Icon size: 24dp (Standard size)
 * - Touch target: Adequate (80dp height provides ample touch area)
 */
@Composable
fun EmployerBottomBar(
    navController: NavController,
    backgroundColor: Color = WorkerColors.BottomNavBackground,
    selectedItemColor: Color = WorkerColors.BottomNavSelected,
    unselectedItemColor: Color = WorkerColors.BottomNavUnselected,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openPostJob() {
        navigateTo(Routes.EMPLOYER_POST_JOB)
    }
    
    val sideItems = listOf(
        Triple(Routes.EMPLOYER_DASHBOARD, R.string.bottom_nav_home, Pair(R.drawable.ic_home_unfilled, R.drawable.ic_home_filled)),
        Triple(Routes.EMPLOYER_PROFILE, R.string.profile, Pair(R.drawable.ic_person_unfilled, R.drawable.ic_person_filled))
    )

    // Bug (batch-j) #1: employer bottom bar now mirrors the worker bar's
    // clean, flat PhonePe/Paytm-style geometry â€” plain white Surface, a
    // 0.5dp top border, 64dp item row, three equally-weighted tabs, and
    // the system navigation-bar inset applied INSIDE the Surface so the
    // bar paints right to the gesture edge (no stray strip underneath).
    // Post Job is inlined as the middle tab (same size/shape as Home and
    // Profile) instead of the previous elevated FAB, so the geometry is
    // identical to the worker side.
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = backgroundColor,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Hairline top divider â€” same tone as the worker bar.
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(com.example.dutype.ui.theme.WorkerColors.Border)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
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

                    // Post Job tab â€” middle position, same shape & size as
                    // the other two. Keeps the selectedItemColor (blue)
                    // tint so Post Job still reads as the primary action
                    // without elevating above the bar line.
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
