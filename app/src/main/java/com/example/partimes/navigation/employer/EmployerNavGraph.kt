package com.example.partimes.navigation.employer

import android.R.attr.type
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.partimes.screens.employer.DashboardScreen
import com.example.partimes.screens.employer.EmployerProfileScreen
import com.example.partimes.screens.employer.EmployerScreen
import com.example.partimes.screens.employer.MyJobsScreen
import com.example.partimes.screens.employer.PostJobScreen
import com.example.partimes.screens.employer.ViewApplicantsScreen

@Composable
fun EmployerNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = EmployerScreen.Dashboard.route
    ) {
        composable(EmployerScreen.Dashboard.route) { DashboardScreen(navController) }
        composable(EmployerScreen.PostJob.route) { PostJobScreen(navController) }
        composable(EmployerScreen.MyJobs.route) { MyJobsScreen(navController) }
        composable(EmployerScreen.Profile.route) { EmployerProfileScreen(navController) }
        composable(
            route = EmployerScreen.ViewApplicants.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId")
            ViewApplicantsScreen(navController, jobId ?: "")
        }
    }
}
