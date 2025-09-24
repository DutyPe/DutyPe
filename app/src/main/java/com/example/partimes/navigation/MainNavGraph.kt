package com.example.partimes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.partimes.auth.PhoneLoginScreen
import com.example.partimes.common.chat.SelectRoleScreen
import com.example.partimes.common.chat.SplashScreen
import com.example.partimes.location.LocationServiceScreen
import com.example.partimes.location.ManualLocationScreen
import com.example.partimes.navigation.employer.EmployerMainScreen
import com.example.partimes.navigation.jobSeekerNavGraph.JobseekerMainScreen
import com.example.partimes.jobseeker.onboarding.JobseekerOnboardingScreen
import com.example.partimes.jobseeker.screens.ProfileSetupScreen

@Composable
fun MainNavGraph(
    navController: NavHostController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SELECT_ROLE
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(navController)
        }
        composable(Routes.LOGIN_BOTTOM_SHEET) {
            PhoneLoginScreen(navController = navController)
        }
        composable(Routes.LOCATION_SERVICE) {
            LocationServiceScreen(navController = navController)
        }
        composable(Routes.MANUAL_LOCATION_ROUTE) {
            ManualLocationScreen(navController = navController)
        }
        composable(Routes.SELECT_ROLE) {
            SelectRoleScreen(navController)
        }
        composable(Routes.JOBSEEKER_ONBOARDING) {
            JobseekerOnboardingScreen(navController)
        }
        composable(Routes.JOBSEEKER_HOME) {
            JobseekerMainScreen(
                rootNavController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        composable(Routes.EMPLOYER_HOME) {
            EmployerMainScreen()
        }
        composable(Routes.PROFILE_SETUP) {
            ProfileSetupScreen(
                jobId = "",
                navController = navController
            )
        }
    }
}