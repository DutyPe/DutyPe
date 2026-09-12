package com.example.dutype.components

import com.dutype.app.R
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.IconSizes
import com.example.dutype.ui.theme.WorkerColors

/**
 * Custom Employer Bottom Bar - Compact floating translucent pill design
 */
@Composable
fun EmployerBottomBar(
    navController: NavController,
    backgroundColor: Color = Color.White.copy(alpha = 0.92f),
    selectedItemColor: Color = com.example.dutype.ui.theme.EmployerColors.BottomNavSelected,
    unselectedItemColor: Color = com.example.dutype.ui.theme.EmployerColors.BottomNavUnselected,
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
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 32.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 240.dp)
                .fillMaxWidth(),
            shape = CircleShape,
            color = backgroundColor,
            shadowElevation = 8.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.85f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .then(
                            if (postSelected) {
                                Modifier
                                    .shadow(
                                        elevation = 3.dp,
                                        shape = CircleShape,
                                        spotColor = Color(0x30000000),
                                        ambientColor = Color(0x20000000)
                                    )
                                    .background(
                                        color = if (com.example.dutype.ui.theme.isAppInDarkTheme()) {
                                            Color(0xFF26262B)
                                        } else {
                                            Color(0xFFE2E8F0).copy(alpha = 0.65f)
                                        },
                                        shape = CircleShape
                                    )
                            } else {
                                Modifier
                            }
                        )
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = ::openPostJob
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.post_job),
                            contentDescription = stringResource(id = R.string.bottom_nav_post),
                            modifier = Modifier.size(24.dp),
                            tint = if (postSelected) selectedItemColor else unselectedItemColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = R.string.bottom_nav_post),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (postSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = if (postSelected) selectedItemColor else unselectedItemColor,
                            maxLines = 1
                        )
                    }
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
    Box(
        modifier = Modifier
            .size(56.dp)
            .then(
                if (isSelected) {
                    Modifier
                        .shadow(
                            elevation = 3.dp,
                            shape = CircleShape,
                            spotColor = Color(0x30000000),
                            ambientColor = Color(0x20000000)
                        )
                        .background(
                            color = if (com.example.dutype.ui.theme.isAppInDarkTheme()) {
                                Color(0xFF26262B)
                            } else {
                                Color(0xFFE2E8F0).copy(alpha = 0.65f)
                            },
                            shape = CircleShape
                        )
                } else {
                    Modifier
                }
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) selectedItemColor else unselectedItemColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = if (isSelected) selectedItemColor else unselectedItemColor,
                maxLines = 1
            )
        }
    }
}
