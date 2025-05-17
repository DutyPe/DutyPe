package com.example.partimes.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.partimes.screens.*
import com.example.partimes.auth.*
import com.example.partimes.navigation.employer.EmployerMainScreen
import com.example.partimes.navigation.jobSeekerNavGraph.JobSeekerMainScreen

@Composable
fun MainNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "select_role"
    ) {
        // 1. Splash Screen
        composable("splash") {
            SplashScreen(navController)
        }

        // 2. Login
        composable("login") {
            LoginScreen(navController)
        }

        // 3. OTP Verification
        composable(
            "otp_screen/{phoneNumber}",
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            OtpVerificationScreen(navController, phoneNumber)
        }

        // 4. Role Selection
        composable("select_role") {
            SelectRoleScreen(navController)
        }

        // 5. Jobseeker Home
        composable("jobseeker_home") {
            JobSeekerMainScreen()
        }

        // 6. Employer Home
        composable("employer_home") {
            EmployerMainScreen()
        }
    }
}
