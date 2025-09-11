package com.example.partimes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.partimes.auth.LoginBottomSheetScreen
import com.example.partimes.common.chat.SelectRoleScreen
import com.example.partimes.common.chat.SplashScreen
import com.example.partimes.location.LocationServiceScreen
import com.example.partimes.location.ManualLocationScreen
import com.example.partimes.navigation.employer.EmployerMainScreen
import com.example.partimes.navigation.jobSeekerNavGraph.JobSeekerMainScreen
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
            LoginBottomSheetScreen(navController = navController)
        }
        composable(Routes.LOCATION_SERVICE) {
            LocationServiceScreen(navController = navController)
        }
        composable(Routes.SELECT_ROLE) {
            SelectRoleScreen(navController)
        }
        composable(Routes.JOBSEEKER_HOME) {
            JobSeekerMainScreen(onStatusBarColorChange = onStatusBarColorChange)
        }
        composable(Routes.EMPLOYER_HOME) {
            EmployerMainScreen()
        }
    }
}