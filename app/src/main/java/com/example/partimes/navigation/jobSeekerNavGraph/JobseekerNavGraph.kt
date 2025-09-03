package com.example.partimes.navigation.jobSeekerNavGraph

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.partimes.screens.jobseekers.HomeScreen
import com.example.partimes.common.chat.help.HelpMainScreen
import com.example.partimes.common.chat.help.ChatSupportScreen
import com.example.partimes.common.chat.help.CallSupportScreen
import com.example.partimes.common.chat.help.ReportProblemScreen
import com.example.partimes.common.chat.help.TutorialScreen
import com.example.partimes.screens.jobseekers.JobDescriptionScreen
import com.example.partimes.auth.LogoutDialog
import com.example.partimes.navigation.Routes
import com.example.partimes.common.chat.ProfileScreen
import com.example.partimes.common.chat.help.SecurityScreen
import com.example.partimes.common.chat.chat.ChatScreen
import com.example.partimes.common.chat.chat.ChatDetailScreen
import com.example.partimes.common.chat.info.AboutUsScreen
import com.example.partimes.common.chat.info.FaqScreen
import com.example.partimes.common.chat.info.PrivacyPolicyScreen
import com.example.partimes.common.chat.info.TermsAndConditionsScreen
import com.example.partimes.jobseeker.screens.myJobs.MyJobsScreen
import com.example.partimes.navigation.jobSeekerNavGraph.BottomNavItem

@Composable
fun JobSeekerNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route
    ) {
        // Main bottom navigation destinations
        composable(BottomNavItem.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(BottomNavItem.MyJobs.route) {
            MyJobsScreen()
        }
        composable(BottomNavItem.Chat.route) {
            ChatScreen(navController = navController)
        }
        composable(BottomNavItem.Profile.route) {
            ProfileScreen(rootNavController = navController)
        }

        // Additional screens
        composable("security") { SecurityScreen(navController) }
        composable("logout") { LogoutDialog(navController) }

        // Job Description Screen
        composable(Routes.JOB_DETAIL_ROUTE) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            JobDescriptionScreen(jobId = jobId, navController = navController)
        }

        // Chat Detail Screen
        composable("chat_detail/{name}") { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: "Unknown"

        }

        // Help Section Routes
        composable("help") { HelpMainScreen(navController) }
        composable("chat_support") { ChatSupportScreen(navController) }
        composable("call_support") { CallSupportScreen(navController) }
        composable("report") { ReportProblemScreen(navController) }
        composable("tutorial") { TutorialScreen(navController) }
        composable("faq") { FaqScreen(navController) }
        composable("aboutUs") { AboutUsScreen(navController) }
        composable("privacy") { PrivacyPolicyScreen(navController) }
        composable("terms") { TermsAndConditionsScreen(navController) }
    }
}
