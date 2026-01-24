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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import com.example.dutype.ui.theme.ComponentHeights
import com.example.dutype.ui.theme.IconSizes
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
    
    // Preload interstitial ad when bottom bar is shown
    LaunchedEffect(Unit) {
        adViewModel.loadInterstitialAd(context)
    }
    
    // Employer bottom bar items - Custom icons from drawable
    val items = listOf(
        Triple(Routes.EMPLOYER_DASHBOARD, R.string.bottom_nav_home, R.drawable.home_icon), // Custom home icon
        Triple(Routes.EMPLOYER_POST_JOB, R.string.bottom_nav_post, R.drawable.post_job), // Custom post job icon
        Triple(Routes.EMPLOYER_PROFILE, R.string.profile, R.drawable.profile) // Custom profile icon
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = backgroundColor,
            shadowElevation = 0.dp, // No shadow for clean look
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top border - very subtle light gray
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color(0xFFE5E7EB))
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ComponentHeights.BottomNavigationBar), // Material Design 3: 80dp
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { (route, labelResId, iconRes) ->
                        val isSelected = currentRoute == route
                        val label = stringResource(id = labelResId)
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    // Special handling for Post Job - show interstitial ad first
                                    if (route == Routes.EMPLOYER_POST_JOB && activity != null) {
                                        Timber.d("📺 Post Job clicked - showing interstitial ad")
                                        adViewModel.showInterstitialAd(
                                            activity = activity,
                                            onAdDismissed = {
                                                // Navigate to Post Job after ad
                                                navController.navigate(route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            onAdNotReady = {
                                                // Ad not ready, navigate directly
                                                navController.navigate(route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                    } else {
                                        // Normal navigation for other items
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                        ) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = label,
                                modifier = Modifier.size(28.dp), // Larger icon size
                                tint = if (isSelected) selectedItemColor else unselectedItemColor
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = label,
                                fontFamily = MeeshoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = if (isSelected) selectedItemColor else unselectedItemColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
