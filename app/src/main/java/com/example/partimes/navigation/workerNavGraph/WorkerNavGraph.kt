package com.example.partimes.navigation.workerNavGraph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.partimes.worker.screens.WorkerHomeScreen
import com.example.partimes.common.chat.help.HelpMainScreen
import com.example.partimes.common.chat.help.ChatSupportScreen
import com.example.partimes.common.chat.help.CallSupportScreen
import com.example.partimes.common.chat.help.ReportProblemScreen
import com.example.partimes.common.chat.help.TutorialScreen
import com.example.partimes.worker.screens.JobDescriptionScreen
import com.example.partimes.auth.LogoutDialog
import com.example.partimes.navigation.Routes
import com.example.partimes.worker.screens.profile.WorkerProfileScreen
import com.example.partimes.common.chat.help.SecurityScreen
import com.example.partimes.worker.screens.about.WorkerAboutScreen
import com.example.partimes.common.chat.info.FaqScreen
import com.example.partimes.common.chat.info.PrivacyPolicyScreen
import com.example.partimes.common.chat.info.TermsAndConditionsScreen
import com.example.partimes.worker.screens.myJobs.MyJobsScreen
import com.example.partimes.notifications.screens.NotificationCenterScreen
import com.example.partimes.profile.screens.SkillsManagementScreen
import com.example.partimes.profile.screens.ResumeUploadScreen
import com.example.partimes.profile.screens.VerificationScreen
import com.example.partimes.utils.ScrollStateManager
import com.example.partimes.data.ApplicationFormDataStore
import androidx.compose.ui.platform.LocalContext

@Composable
fun WorkerNavGraph(
    navController: NavHostController,
    rootNavController: NavHostController,
    modifier: Modifier = Modifier,
    onStatusBarColorChange: (Color) -> Unit = {},
    scrollStateManager: ScrollStateManager? = null
) {
    NavHost(
        navController = navController,
        startDestination = Routes.WORKER_HOME_TAB
    ) {
        composable(Routes.WORKER_HOME_TAB) {
            WorkerHomeScreen(
                navController = navController,
                rootNavController = rootNavController,
                onStatusBarColorChange = onStatusBarColorChange,
                scrollStateManager = scrollStateManager
            )
        }
        composable(Routes.WORKER_MY_JOBS) {
            MyJobsScreen(
                onStatusBarColorChange = onStatusBarColorChange,
                scrollStateManager = scrollStateManager
            )
        }
        composable(Routes.WORKER_PROFILE) {
            val context = LocalContext.current
            val dataStore = remember { ApplicationFormDataStore(context) }
            WorkerProfileScreen(
                rootNavController = rootNavController,
                onStatusBarColorChange = onStatusBarColorChange,
                scrollStateManager = scrollStateManager,
                dataStore = dataStore
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
                navController = rootNavController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        
        
        composable(Routes.NOTIFICATION_CENTER) {
            NotificationCenterScreen(
                onBackClick = { navController.popBackStack() }
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
            WorkerAboutScreen(
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
        
        // Advanced Profile Routes - Removed AdvancedProfileScreen
        
        composable(Routes.SKILLS_MANAGEMENT) {
            SkillsManagementScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        
        composable(Routes.RESUME_UPLOAD) {
            ResumeUploadScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        
        composable(Routes.VERIFICATION) {
            VerificationScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
    }
}