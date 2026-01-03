package com.example.dutype.navigation

import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.dutype.auth.EnhancedLoginScreen
import com.example.dutype.components.DutyPeSplashScreen
import com.example.dutype.location.ManualLocationScreen
import com.example.dutype.onboarding.OnboardingScreen
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
 * - Legal pages (privacy, terms)
 * - Language selection
 * 
 * ARCHITECTURE IMPROVEMENT (January 2026):
 * Split from MainNavGraph for better maintainability and faster compile times.
 * 
 * @author DutyPe Engineering Team
 * @since 2.2.0
 */
fun NavGraphBuilder.commonNavGraph(
    navController: NavHostController,
    onStatusBarColorChange: (Color) -> Unit = {},
    isFirstTimeUser: Boolean = false,
    onSplashComplete: () -> Unit = {}
) {
    // Splash Screen
    composable(Routes.SPLASH) {
        DutyPeSplashScreen(
            navController = navController,
            onSplashComplete = onSplashComplete,
            duration = 2000L
        )
    }
    
    // Onboarding
    composable(Routes.ONBOARDING) {
        OnboardingScreen(navController)
    }
    
    // Enhanced Login
    composable(
        route = "${Routes.ENHANCED_LOGIN}?role={role}",
        arguments = listOf(navArgument("role") { type = NavType.StringType; defaultValue = "WORKER" })
    ) { backStackEntry ->
        val role = backStackEntry.arguments?.getString("role") ?: "WORKER"
        Timber.d("EnhancedLoginScreen received role parameter: $role")
        EnhancedLoginScreen(
            navController = navController,
            skipRoleSelection = true,
            initialRole = role
        )
    }
    
    // Manual Location Selection
    composable(Routes.MANUAL_LOCATION_ROUTE) {
        ManualLocationScreen(navController = navController)
    }
    
    // Language Selection
    composable(Routes.LANGUAGE_SELECTION) {
        com.example.dutype.common.LanguageSelectionScreen(
            navController = navController
        )
    }
    
    // Chat Conversations List
    composable(Routes.CHAT_CONVERSATIONS) {
        val chatViewModel: com.example.dutype.viewmodels.ChatViewModel = hiltViewModel()
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
        val chatViewModel: com.example.dutype.viewmodels.ChatViewModel = hiltViewModel()
        com.example.dutype.common.chat.ChatDetailScreen(
            conversationId = conversationId,
            chatService = chatViewModel.chatService,
            onBackClick = { navController.popBackStack() }
        )
    }
    
    // Chat Detail (legacy route)
    composable(
        route = Routes.CHAT_DETAIL,
        arguments = listOf(navArgument("name") { type = NavType.StringType })
    ) { backStackEntry ->
        // Redirect to conversation list for now
        com.example.dutype.common.chat.ConversationListScreen(
            chatService = hiltViewModel<com.example.dutype.viewmodels.ChatViewModel>().chatService,
            onBackClick = { navController.popBackStack() },
            onConversationClick = { conversationId ->
                navController.navigate(Routes.chatConversationDetailRoute(conversationId))
            }
        )
    }
    
    // Cancellation & Refund
    composable(Routes.CANCELLATION_REFUND) {
        com.example.dutype.common.chat.info.CancellationRefundScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Contact Us
    composable(Routes.CONTACT_US) {
        com.example.dutype.common.chat.info.ContactUsScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Privacy Policy
    composable(Routes.PRIVACY) {
        com.example.dutype.common.chat.info.PrivacyPolicyScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Terms of Service
    composable(Routes.TERMS) {
        com.example.dutype.common.chat.info.TermsAndConditionsScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Security & Legal
    composable(Routes.SECURITY_LEGAL) {
        com.example.dutype.common.chat.help.SecurityLegalScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Security
    composable(Routes.SECURITY) {
        com.example.dutype.common.chat.help.SecurityScreen(
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
    
    // FAQ
    composable(Routes.FAQ) {
        com.example.dutype.common.chat.info.FaqScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
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
}

/**
 * Authentication routes helper
 */
object AuthRoutes {
    const val SPLASH = Routes.SPLASH
    const val ONBOARDING = Routes.ONBOARDING
    const val SELECT_ROLE = Routes.SELECT_ROLE
    const val LOGIN = Routes.ENHANCED_LOGIN
}
