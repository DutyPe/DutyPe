package com.example.partimes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.partimes.auth.PhoneLoginScreen
import com.example.partimes.auth.EnhancedLoginScreen
import com.example.partimes.common.chat.SelectRoleScreen
import com.example.partimes.common.chat.SplashScreen
import com.example.partimes.common.employer.AnalyticsScreen
import com.example.partimes.common.employer.CompanyDetailsScreen
import com.example.partimes.common.employer.EmployerProfileScreen
import com.example.partimes.common.employer.EmployerTermsScreen
import com.example.partimes.common.employer.TalentSearchScreen
import com.example.partimes.location.LocationServiceScreen
import com.example.partimes.location.ManualLocationScreen
import com.example.partimes.navigation.employer.EmployerMainScreen
import com.example.partimes.navigation.workerNavGraph.WorkerMainScreen
import com.example.partimes.worker.onboarding.WorkerOnboardingScreen
import com.example.partimes.worker.screens.ProfileSetupScreen
import com.example.partimes.employer.screens.EmployerProfileSetupScreen

@Composable
fun MainNavGraph(
    navController: NavHostController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Routes.ENHANCED_LOGIN
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(navController)
        }
        composable(Routes.ENHANCED_LOGIN) {
            EnhancedLoginScreen(navController = navController)
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
        composable(Routes.WORKER_ONBOARDING) {
            WorkerOnboardingScreen(navController)
        }
        composable(Routes.EMPLOYER_ONBOARDING) {
            EmployerProfileSetupScreen(navController)
        }
        composable(Routes.WORKER_HOME) {
            WorkerMainScreen(
                rootNavController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        composable(Routes.EMPLOYER_HOME) {
            EmployerMainScreen()
        }
        composable(Routes.EMPLOYER_PROFILE_SETUP) {
            EmployerProfileSetupScreen(navController)
        }
        composable(Routes.PROFILE_SETUP) {
            ProfileSetupScreen(
                jobId = "",
                navController = navController
            )
        }
        
        // Missing employer routes - add placeholder screens
        composable(Routes.EMPLOYER_PROFILE) {
            // Placeholder for employer profile
            EmployerProfileScreen(navController)
        }
        composable(Routes.TALENT_SEARCH) {
            // Placeholder for talent search
            TalentSearchScreen(navController)
        }
        composable(Routes.EMPLOYER_TERMS) {
            // Placeholder for employer terms
      EmployerTermsScreen(navController)
        }
        composable(Routes.COMPANY_DETAILS) {
            // Placeholder for company details
         CompanyDetailsScreen(navController)
        }
        composable(Routes.ANALYTICS) {
            // Placeholder for analytics
           AnalyticsScreen(navController)
        }
    }
}