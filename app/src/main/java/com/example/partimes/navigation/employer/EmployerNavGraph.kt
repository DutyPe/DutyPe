package com.example.partimes.navigation.employer

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.partimes.common.employer.EmployerProfileScreen
import com.example.partimes.employer.screens.EmployerScreen
import com.example.partimes.employer.screens.postedJobs.PostedJobsScreen
//import com.example.partimes.employer.screens.editjob.EditJobScreen
import com.example.partimes.employer.screens.homeScreen.EmployerHomeScreen
import com.example.partimes.employer.screens.postjob.PostJobScreen
import com.example.partimes.employer.screens.ViewApplicantsScreen

@Composable
fun EmployerNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = EmployerScreen.Dashboard.route
    ) {
        composable(EmployerScreen.Dashboard.route) {
            EmployerHomeScreen(navController)
        }
        composable(route = EmployerScreen.PostJob.route) {
            PostJobScreen(navController, null)
        }

        composable(EmployerScreen.MyJobs.route) {
            PostedJobsScreen(navController)
        }

        composable(EmployerScreen.Profile.route) {
            EmployerProfileScreen(navController)
        }

        composable(
            route = EmployerScreen.ViewApplicants.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            ViewApplicantsScreen(navController, jobId)
        }
        composable(
            route = "post_job/{jobId}",
            arguments = listOf(navArgument("jobId") {
                nullable = true
                defaultValue = null
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId")
            PostJobScreen(navController, jobId)
        }

    }
}
