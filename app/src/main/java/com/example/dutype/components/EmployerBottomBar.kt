package com.example.dutype.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dutype.app.R
import com.example.dutype.ads.AdManager
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.viewmodels.AdViewModel
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
    selectedItemColor: Color = Color(0xFF2563EB), // Blue for selected (Employer)
    unselectedItemColor: Color = WorkerColors.BottomNavUnselected,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adViewModel: AdViewModel = hiltViewModel()
    
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
        if (activity != null) {
            Timber.d("📺 Post Job clicked - showing interstitial ad")
            adViewModel.showInterstitialAd(
                activity = activity,
                onAdDismissed = { navigateTo(Routes.EMPLOYER_POST_JOB) },
                onAdNotReady = { navigateTo(Routes.EMPLOYER_POST_JOB) }
            )
        } else {
            navigateTo(Routes.EMPLOYER_POST_JOB)
        }
    }
    
    // Preload interstitial ad when bottom bar is shown
    LaunchedEffect(Unit) {
        adViewModel.loadInterstitialAd(context)
    }
    
    val sideItems = listOf(
        Triple(Routes.EMPLOYER_DASHBOARD, R.string.bottom_nav_home, Pair(R.drawable.ic_home_unfilled, R.drawable.ic_home_filled)),
        Triple(Routes.EMPLOYER_PROFILE, R.string.profile, Pair(R.drawable.ic_person_unfilled, R.drawable.ic_person_filled))
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Frosted glass background bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.97f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            shadowElevation = 20.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                sideItems.forEachIndexed { index, (route, labelResId, iconPair) ->
                    val isSelected = currentRoute == route
                    val label = stringResource(id = labelResId)
                    val (iconUnfilled, iconFilled) = iconPair
                    val iconRes = if (isSelected) iconFilled else iconUnfilled

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { navigateTo(route) }
                    ) {
                        // Active indicator dot
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(width = 20.dp, height = 3.dp)
                                    .background(
                                        selectedItemColor,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = label,
                            modifier = Modifier.size(24.dp),
                            tint = if (isSelected) selectedItemColor else unselectedItemColor
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = label,
                            fontFamily = MeeshoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) selectedItemColor else unselectedItemColor,
                            maxLines = 1
                        )
                    }

                    // Space for center FAB
                    if (index == 0) {
                        Spacer(modifier = Modifier.width(72.dp))
                    }
                }
            }
        }

        // Center floating Post Job button — pill shape with icon + text
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = ::openPostJob
                ),
            shape = RoundedCornerShape(20.dp),
            color = selectedItemColor,
            shadowElevation = 12.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.post_job),
                    contentDescription = stringResource(id = R.string.bottom_nav_post),
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Text(
                    text = "Post Job",
                    fontFamily = MeeshoFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
