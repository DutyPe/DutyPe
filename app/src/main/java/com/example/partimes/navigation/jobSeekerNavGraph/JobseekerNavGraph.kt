package com.example.partimes.navigation.jobSeekerNavGraph

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.partimes.jobseeker.screens.JobseekerHomeScreen
import com.example.partimes.common.chat.help.HelpMainScreen
import com.example.partimes.common.chat.help.ChatSupportScreen
import com.example.partimes.common.chat.help.CallSupportScreen
import com.example.partimes.common.chat.help.ReportProblemScreen
import com.example.partimes.common.chat.help.TutorialScreen
import com.example.partimes.screens.jobseekers.JobDescriptionScreen
import com.example.partimes.auth.LogoutDialog
import com.example.partimes.navigation.Routes
import com.example.partimes.jobseeker.screens.profile.JobseekerProfileScreen
import com.example.partimes.common.chat.help.SecurityScreen
import com.example.partimes.common.chat.info.AboutUsScreen
import com.example.partimes.common.chat.info.FaqScreen
import com.example.partimes.common.chat.info.PrivacyPolicyScreen
import com.example.partimes.common.chat.info.TermsAndConditionsScreen
import com.example.partimes.jobseeker.screens.myJobs.MyJobsScreen
import com.example.partimes.utils.ScrollStateManager

@Composable
fun JobSeekerNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onStatusBarColorChange: (Color) -> Unit = {},
    scrollStateManager: ScrollStateManager? = null
) {
    NavHost(
        navController = navController,
        startDestination = Routes.JOBSEEKER_HOME_TAB
    ) {
        composable(Routes.JOBSEEKER_HOME_TAB) {
            JobseekerHomeScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange,
                scrollStateManager = scrollStateManager
            )
        }
        composable(Routes.JOBSEEKER_MY_JOBS) {
            MyJobsScreen(onStatusBarColorChange = onStatusBarColorChange)
        }
        composable(Routes.JOBSEEKER_PROFILE) {
            JobseekerProfileScreen(
                rootNavController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }

        // Additional screens with status bar color management
        composable(Routes.SECURITY) {
            SecurityScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        composable(Routes.LOGOUT) {
            LogoutDialog(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        composable(Routes.JOB_DETAIL) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            JobDescriptionScreen(
                jobId = jobId,
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        composable(Routes.CHAT_DETAIL) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: "Unknown"
            // Your chat detail screen implementation
        }
        composable(Routes.HELP) {
            HelpMainScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        composable(Routes.CHAT_SUPPORT) {
            ChatSupportScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        composable(Routes.CALL_SUPPORT) {
            CallSupportScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        composable(Routes.REPORT) {
            ReportProblemScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        composable(Routes.TUTORIAL) {
            TutorialScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        composable(Routes.FAQ) {
            FaqScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        composable(Routes.ABOUT_US) {
            AboutUsScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        composable(Routes.PRIVACY) {
            PrivacyPolicyScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        composable(Routes.TERMS) {
            TermsAndConditionsScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
    }
}