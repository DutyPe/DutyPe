package com.example.dutype.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.dutype.worker.screens.WorkerHomeScreen
import com.example.dutype.common.chat.help.HelpMainScreen
import com.example.dutype.common.chat.help.ChatSupportScreen
import com.example.dutype.common.chat.help.CallSupportScreen
import com.example.dutype.common.chat.help.ReportProblemScreen
import com.example.dutype.common.chat.help.TutorialScreen
import com.example.dutype.worker.screens.JobDescriptionScreen
import com.example.dutype.navigation.Routes
import com.example.dutype.worker.screens.profile.WorkerProfileScreen
import com.example.dutype.worker.screens.profile.WorkerProfileDetailsScreen
import com.example.dutype.common.chat.help.SecurityScreen
import com.example.dutype.worker.screens.WorkerAboutScreen
import com.example.dutype.common.chat.info.FaqScreen
import com.example.dutype.common.chat.info.PrivacyPolicyScreen
import com.example.dutype.common.chat.info.TermsAndConditionsScreen
import com.example.dutype.worker.screens.myJobs.MyJobsScreen
import com.example.dutype.worker.screens.WorkerNotificationScreen
import com.example.dutype.worker.screens.NotificationDetailScreen
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.data.ApplicationFormDataStore
import com.example.dutype.worker.screens.SmartJobApplicationScreen
import com.example.dutype.worker.screens.map.JobMapScreen

@Composable
fun WorkerNavGraph(
    navController: NavHostController,
    rootNavController: NavHostController,
    modifier: Modifier = Modifier,
    onStatusBarColorChange: (Color) -> Unit = {},
    scrollStateManager: ScrollStateManager? = null,
    notificationPermissionManager: com.example.dutype.utils.NotificationPermissionManager
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
                scrollStateManager = scrollStateManager,
                notificationPermissionManager = notificationPermissionManager
            )
        }
        composable(Routes.WORKER_MY_JOBS) {
            MyJobsScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange,
                scrollStateManager = scrollStateManager
            )
        }
        composable(Routes.WORKER_PROFILE) {
            val context = LocalContext.current
            val dataStore = remember { ApplicationFormDataStore(context) }
            WorkerProfileScreen(
                rootNavController = rootNavController,
                localNavController = navController,
                onStatusBarColorChange = onStatusBarColorChange,
                scrollStateManager = scrollStateManager,
                dataStore = dataStore
            )
        }
        composable(Routes.WORKER_PROFILE_DETAILS) {
            val context = LocalContext.current
            val dataStore = remember { ApplicationFormDataStore(context) }
            WorkerProfileDetailsScreen(
                navController = navController,
                dataStore = dataStore
            )
        }
        
        // Digital Visiting Card Screen
        composable(Routes.WORKER_VISITING_CARD) {
            com.example.dutype.worker.screens.profile.DigitalVisitingCardScreen(
                navController = navController,
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
        composable(Routes.SECURITY_LEGAL) {
            com.example.dutype.common.chat.help.SecurityLegalScreen(
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
        
        
        composable(Routes.WORKER_NOTIFICATIONS) {
            WorkerNotificationScreen(
                onBackClick = { navController.popBackStack() },
                navController = navController
            )
        }
        
        composable(
            route = Routes.WORKER_NOTIFICATION_DETAIL,
            arguments = listOf(
                androidx.navigation.navArgument("notificationId") { 
                    type = androidx.navigation.NavType.StringType 
                }
            )
        ) { backStackEntry ->
            val notificationId = backStackEntry.arguments?.getString("notificationId") ?: ""
            NotificationDetailScreen(
                notificationId = notificationId,
                onBackClick = { navController.popBackStack() },
                navController = navController
            )
        }
        
        composable(Routes.WORKER_NOTIFICATION_SETTINGS) {
            com.example.dutype.worker.screens.WorkerNotificationSettingsScreen(
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
        

        composable(
            route = Routes.SMART_JOB_APPLICATION,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            SmartJobApplicationScreen(
                jobId = jobId,
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        
        // All Jobs Screen with filter
        composable(
            route = Routes.WORKER_ALL_JOBS_FILTERED,
            arguments = listOf(navArgument("filter") { type = NavType.StringType })
        ) { backStackEntry ->
            val filter = backStackEntry.arguments?.getString("filter") ?: "All Jobs"
            com.example.dutype.worker.screens.AllJobsScreen(
                navController = navController,
                initialFilter = filter,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        
        // All Jobs Screen without filter
        composable(Routes.WORKER_ALL_JOBS) {
            com.example.dutype.worker.screens.AllJobsScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        
        // Worker History Screen
        composable(Routes.WORKER_HISTORY) {
            com.example.dutype.worker.screens.WorkerHistoryScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        
        // Map-First Interface - Jobs on Map (Accessibility Feature)
        composable(Routes.WORKER_JOB_MAP) {
            JobMapScreen(
                navController = navController
            )
        }
        
        // Language Selection Screen
        composable(Routes.LANGUAGE_SELECTION) {
            com.example.dutype.common.LanguageSelectionScreen(
                navController = navController
            )
        }
        
        // Chat Conversations List
        composable(Routes.CHAT_CONVERSATIONS) {
            // ChatService accessed via ChatViewModel (proper DI pattern)
            val chatViewModel = androidx.hilt.navigation.compose.hiltViewModel<com.example.dutype.viewmodels.ChatViewModel>()
            com.example.dutype.common.chat.ConversationListScreen(
                chatService = chatViewModel.chatService,
                onBackClick = { navController.popBackStack() },
                onConversationClick = { conversationId ->
                    navController.navigate(Routes.chatConversationDetailRoute(conversationId))
                }
            )
        }
        
        // Chat Conversation Detail
        composable(
            route = Routes.CHAT_CONVERSATION_DETAIL,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            // ChatService accessed via ChatViewModel (proper DI pattern)
            val chatViewModel = androidx.hilt.navigation.compose.hiltViewModel<com.example.dutype.viewmodels.ChatViewModel>()
            com.example.dutype.common.chat.ChatDetailScreen(
                conversationId = conversationId,
                chatService = chatViewModel.chatService,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        // Earnings Dashboard - Worker Financial Clarity
        composable(Routes.WORKER_EARNINGS) {
            com.example.dutype.worker.screens.EarningsDashboardScreen(
                navController = navController
            )
        }
        
        // Work Start Verification - QR Code Screen for Worker
        composable(
            route = Routes.WORKER_WORK_START_QR,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            // WorkVerificationService accessed via WorkVerificationViewModel (proper DI pattern)
            val workVerificationViewModel = androidx.hilt.navigation.compose.hiltViewModel<com.example.dutype.viewmodels.WorkVerificationViewModel>()
            com.example.dutype.worker.screens.WorkStartQRScreen(
                jobId = jobId,
                navController = navController,
                workVerificationService = workVerificationViewModel.workVerificationService
            )
        }
    }
}
