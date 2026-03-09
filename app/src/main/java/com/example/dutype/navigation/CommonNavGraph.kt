package com.example.dutype.navigation

import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.dutype.location.ManualLocationScreen
import com.example.dutype.onboarding.OnboardingScreen
import com.example.dutype.utils.AppConstants
import timber.log.Timber

/**
 * CommonNavGraph - Shared navigation routes for both workers and employers
 * 
 * Contains routes accessible to all users:
 * - Authentication (login, role selection)
 * - Onboarding
 * - Chat/messaging
 * - Location selection
 * - Help & support
 * - Language selection
 * 
 * Note: Legal pages (privacy, terms, security, refund) are available only on the web app.
 * 
 * ARCHITECTURE IMPROVEMENT (January 2026):
 * Split from MainNavGraph for better maintainability and faster compile times.
 * 
 * @author DutyPe Engineering Team
 * @since 2.2.0
 */
fun NavGraphBuilder.commonNavGraph(
    navController: NavHostController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    // Onboarding
    composable(Routes.ONBOARDING) {
        OnboardingScreen(navController)
    }
    
    // Enhanced Login - Redirects to role selection
    composable(
        route = "${Routes.ENHANCED_LOGIN}?role={role}",
        arguments = listOf(navArgument("role") { type = NavType.StringType; defaultValue = "WORKER" })
    ) { backStackEntry ->
        val role = backStackEntry.arguments?.getString("role") ?: "WORKER"
        val userRole = try {
            com.example.dutype.models.UserRole.valueOf(role.uppercase())
        } catch (e: Exception) {
            com.example.dutype.models.UserRole.WORKER
        }
        
        com.example.dutype.auth.EnhancedLoginScreen(
            navController = navController,
            skipRoleSelection = true,
            initialRole = role
        )
    }
    
    // Manual Location Selection
    composable(Routes.MANUAL_LOCATION_ROUTE) {
        ManualLocationScreen(navController = navController)
    }
    
    // Language Selection - Now handled via bottom sheet in profile screens
    composable(Routes.LANGUAGE_SELECTION) {
        // Navigate back - language selection is now a bottom sheet
        androidx.compose.runtime.LaunchedEffect(Unit) {
            navController.popBackStack()
        }
    }
    
    // Chat Conversations List
    composable(Routes.CHAT_CONVERSATIONS) {
        val chatService: com.example.dutype.services.ChatService = hiltViewModel()
        com.example.dutype.common.chat.ConversationListScreen(
            chatService = chatService,
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
        val chatService: com.example.dutype.services.ChatService = hiltViewModel()
        com.example.dutype.common.chat.ChatDetailScreen(
            conversationId = conversationId,
            chatService = chatService,
            onBackClick = { navController.popBackStack() }
        )
    }
    
    // Chat Detail (legacy route)
    composable(
        route = Routes.CHAT_DETAIL,
        arguments = listOf(navArgument("name") { type = NavType.StringType })
    ) { backStackEntry ->
        val chatService: com.example.dutype.services.ChatService = hiltViewModel()
        com.example.dutype.common.chat.ConversationListScreen(
            chatService = chatService,
            onBackClick = { navController.popBackStack() },
            onConversationClick = { conversationId ->
                navController.navigate(Routes.chatConversationDetailRoute(conversationId))
            }
        )
    }
    
    // Contact Us
    composable(Routes.CONTACT_US) {
        com.example.dutype.common.chat.info.ContactUsScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Help
    composable(Routes.HELP) {
        com.example.dutype.common.chat.help.HelpMainScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // FAQ - Opens in external browser (web app)
    composable(Routes.FAQ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        androidx.compose.runtime.LaunchedEffect(Unit) {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(AppConstants.FAQ_URL))
            context.startActivity(intent)
            navController.popBackStack()
        }
    }
    
    // Report
    composable(Routes.REPORT) {
        com.example.dutype.common.chat.help.ReportProblemScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Tutorial
    composable(Routes.TUTORIAL) {
        com.example.dutype.common.chat.help.TutorialScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Worker Profile View (Public) - For viewing any worker's profile via deep link
    // This allows visiting card links to work for everyone (workers, employers, guests)
    composable(
        route = Routes.WORKER_PROFILE_VIEW,
        arguments = listOf(navArgument("workerId") { type = NavType.StringType })
    ) { backStackEntry ->
        val workerId = backStackEntry.arguments?.getString("workerId") ?: ""
        Timber.d("🔗 CommonNavGraph: Opening worker profile view for workerId: $workerId")
        
        // Use the professional worker profile view screen
        com.example.dutype.employer.screens.ProfessionalWorkerProfileViewScreen(
            navController = navController,
            workerId = workerId
        )
    }
}

