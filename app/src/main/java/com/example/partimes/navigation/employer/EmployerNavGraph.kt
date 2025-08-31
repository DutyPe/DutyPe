package com.example.partimes.navigation.employer

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.partimes.screens.employer.profilescreen.EmployerProfileScreen
import com.example.partimes.screens.employer.EmployerScreen
import com.example.partimes.screens.employer.MyJobsScreen
import com.example.partimes.screens.employer.postingnewJob.PostJobScreen
import com.example.partimes.screens.employer.ViewApplicantsScreen
import com.example.partimes.screens.employer.editjob.EditJobScreen
import com.example.partimes.screens.jobseekers.HomeScreen

@Composable
fun EmployerNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = EmployerScreen.Dashboard.route
    ) {
        composable(EmployerScreen.Dashboard.route) {
            HomeScreen(navController)
        }

        // PostJob with optional jobId param for editing existing jobs or posting new
        composable(
            route = EmployerScreen.PostJob.route,
            arguments = listOf(navArgument("jobId") { defaultValue = ""; nullable = true })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId")
            PostJobScreen(navController, jobId)
        }

        composable(EmployerScreen.MyJobs.route) {
            MyJobsScreen(navController)
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
        composable("edit_job/{jobId}") { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId")
            EditJobScreen(navController, jobId)
        }

    }
}

