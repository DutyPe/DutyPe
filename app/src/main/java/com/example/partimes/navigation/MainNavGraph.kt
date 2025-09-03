package com.example.partimes.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.partimes.auth.LoginBottomSheetScreen
import com.example.partimes.navigation.employer.EmployerMainScreen
import com.example.partimes.navigation.jobSeekerNavGraph.JobSeekerMainScreen
import com.example.partimes.common.chat.SelectRoleScreen
import com.example.partimes.common.chat.SplashScreen
import com.example.partimes.location.LocationServiceScreen
import com.example.partimes.location.ManualLocationScreen

@Composable
fun MainNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "login_bottom_sheet"
    ) {
        composable("splash") {
            SplashScreen(navController)
        }

        composable("login_bottom_sheet") {
            LoginBottomSheetScreen(navController = navController)
        }

        composable(Routes.LOCATION_SERVICE_SCREEN_ROUTE) {
            LocationServiceScreen(navController = navController)
        }

        composable(Routes.MANUAL_LOCATION_ROUTE) {
            ManualLocationScreen(navController = navController)
        }

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
