package com.example.dutype.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    
    // Language Selection - Now handled via bottom sheet in profile screens
    composable(Routes.LANGUAGE_SELECTION) {
        // Navigate back - language selection is now a bottom sheet
        androidx.compose.runtime.LaunchedEffect(Unit) {
            navController.popBackStack()
        }
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
    
    // Cancellation & Refund - Opens web URL
    composable(Routes.CANCELLATION_REFUND) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.REFUND_URL))
            context.startActivity(intent)
            navController.popBackStack()
        }
    }
    
    // Contact Us
    composable(Routes.CONTACT_US) {
        com.example.dutype.common.chat.info.ContactUsScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Privacy Policy - Opens web URL
    composable(Routes.PRIVACY) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.PRIVACY_URL))
            context.startActivity(intent)
            navController.popBackStack()
        }
    }
    
    // Terms of Service - Opens web URL
    composable(Routes.TERMS) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.TERMS_URL))
            context.startActivity(intent)
            navController.popBackStack()
        }
    }
    
    // Security & Legal
    composable(Routes.SECURITY_LEGAL) {
        com.example.dutype.common.chat.help.SecurityLegalScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Security - Opens web URL (safety page)
    composable(Routes.SECURITY) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.SAFETY_URL))
            context.startActivity(intent)
            navController.popBackStack()
        }
    }
    
    // Help
    composable(Routes.HELP) {
        com.example.dutype.common.chat.help.HelpMainScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // FAQ - Opens web URL
    composable(Routes.FAQ) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.FAQ_URL))
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
