package com.example.dutype.navigation

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.dutype.employer.screens.AnalyticsScreen
import com.example.dutype.employer.screens.EditJobScreen
import com.example.dutype.employer.screens.EmployerCompanyDetailsScreen
import com.example.dutype.employer.screens.MandatoryEmployerProfileSetupScreen
import com.example.dutype.employer.screens.applications.ApplicationDetailScreen
import com.example.dutype.employer.screens.applications.EmployerApplicationManagementScreen
import com.example.dutype.employer.screens.profilescreen.EmployerProfileScreen
import com.example.dutype.models.ApplicationStatus

/**
 * EmployerNavGraph - Employer-specific navigation routes
 * 
 * Contains all routes accessible only to employers:
 * - Job posting and management
 * - Application management
 * - Company profile
 * - Analytics dashboard
 * - Subscription management
 * - Work verification
 * 
 * ARCHITECTURE IMPROVEMENT (January 2026):
 * Split from MainNavGraph for better maintainability and faster compile times.
 * 
 * @author DutyPe Engineering Team
 * @since 2.2.0
 */
fun NavGraphBuilder.employerNavGraph(
    navController: NavHostController,
    onStatusBarColorChange: (Color) -> Unit = {},
    notificationPermissionManager: com.example.dutype.utils.NotificationPermissionManager
) {
    // Employer Home (main entry point handled in MainNavGraph)
    
    // Profile Setup - supports optional returnRoute parameter for navigation after completion
    composable(
        route = "${Routes.EMPLOYER_PROFILE_SETUP}?returnRoute={returnRoute}",
        arguments = listOf(
            navArgument("returnRoute") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val returnRoute = backStackEntry.arguments?.getString("returnRoute")
        MandatoryEmployerProfileSetupScreen(
            navController = navController,
            returnRoute = returnRoute
        )
    }
    
    // Also support the route without parameters for backward compatibility
    composable(Routes.EMPLOYER_PROFILE_SETUP) {
        MandatoryEmployerProfileSetupScreen(navController = navController)
    }
    
    // Employer Profile
    composable(Routes.EMPLOYER_PROFILE) {
        EmployerProfileScreen(navController)
    }
    
    // Employer Visiting Card
    composable(Routes.EMPLOYER_VISITING_CARD) {
        com.example.dutype.employer.screens.profile.EmployerDigitalVisitingCardScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Company Details
    composable(Routes.EMPLOYER_COMPANY_DETAILS) {
        EmployerCompanyDetailsScreen(navController = navController)
    }
    
    composable(Routes.COMPANY_DETAILS) {
        EmployerCompanyDetailsScreen(navController)
    }
    
    // Edit Job
    composable(
        route = Routes.EDIT_JOB,
        arguments = listOf(navArgument("jobId") { type = NavType.StringType })
    ) { backStackEntry ->
        val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
        EditJobScreen(
            navController = navController,
            jobId = jobId
        )
    }
    
    // View Applicants for a Job
    composable(
        route = Routes.VIEW_APPLICANTS,
        arguments = listOf(navArgument("jobId") { type = NavType.StringType })
    ) { backStackEntry ->
        val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
        EmployerApplicationManagementScreen(
            jobId = jobId,
            onApplicationClick = { application ->
                navController.navigate("employer_application_detail/${application.id}")
            },
            onBackClick = { navController.popBackStack() }
        )
    }
    
    // All Applications (across all jobs)
    composable(Routes.EMPLOYER_APPLICATIONS) {
        EmployerApplicationManagementScreen(
            jobId = null,
            onApplicationClick = { application ->
                navController.navigate("employer_application_detail/${application.id}")
            },
            onBackClick = { navController.popBackStack() }
        )
    }
    
    // Applications for specific job
    composable(
        route = Routes.EMPLOYER_APPLICATIONS_JOB,
        arguments = listOf(navArgument("jobId") { type = NavType.StringType })
    ) { backStackEntry ->
        val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
        EmployerApplicationManagementScreen(
            jobId = jobId,
            onApplicationClick = { application ->
                navController.navigate("employer_application_detail/${application.id}")
            },
            onBackClick = { navController.popBackStack() }
        )
    }
    
    // Application Detail
    composable(
        route = Routes.EMPLOYER_APPLICATION_DETAIL,
        arguments = listOf(navArgument("applicationId") { type = NavType.StringType })
    ) { backStackEntry ->
        val applicationId = backStackEntry.arguments?.getString("applicationId") ?: ""
        val employerViewModel: com.example.dutype.viewmodels.EmployerApplicationViewModel = hiltViewModel()
        val context = LocalContext.current
        
        ApplicationDetailScreen(
            applicationId = applicationId,
            onBackClick = { navController.popBackStack() },
            onUpdateStatus = { newStatus, notes ->
                if (newStatus == ApplicationStatus.ACCEPTED) {
                    val application = employerViewModel.uiState.value.applications.find { 
                        it.applicationId == applicationId 
                    }
                    if (application != null) {
                        employerViewModel.hireApplicant(
                            applicationId = applicationId,
                            jobId = application.jobId,
                            onSuccess = {
                                Toast.makeText(context, "Applicant hired successfully!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        employerViewModel.updateApplicationStatus(applicationId, newStatus, notes)
                        navController.popBackStack()
                    }
                } else {
                    employerViewModel.updateApplicationStatus(applicationId, newStatus, notes)
                    navController.popBackStack()
                }
            },
            onVerifyWork = null
        )
    }
    
    // Worker Profile View (for employers viewing applicant profiles)
    composable(
        route = Routes.WORKER_PROFILE_VIEW,
        arguments = listOf(navArgument("workerId") { type = NavType.StringType }),
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "dutype://worker/{workerId}"
            },
            navDeepLink {
                uriPattern = "https://dutype.in/worker/{workerId}"
            },
            navDeepLink {
                uriPattern = "https://dutypeapp.web.app/worker/{workerId}"
            }
        )
    ) { backStackEntry ->
        val workerId = backStackEntry.arguments?.getString("workerId") ?: ""
        com.example.dutype.employer.screens.ProfessionalWorkerProfileViewScreen(
            navController = navController,
            workerId = workerId
        )
    }
    
    // Employer Profile View (for workers viewing employer profiles)
    composable(
        route = Routes.EMPLOYER_PROFILE_VIEW,
        arguments = listOf(navArgument("employerId") { type = NavType.StringType }),
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "dutype://employer/{employerId}"
            },
            navDeepLink {
                uriPattern = "https://dutype.in/employer/{employerId}"
            },
            navDeepLink {
                uriPattern = "https://dutypeapp.web.app/employer/{employerId}"
            }
        )
    ) { backStackEntry ->
        val employerId = backStackEntry.arguments?.getString("employerId") ?: ""
        // For now, navigate to employer profile screen
        // TODO: Create dedicated employer profile view screen for workers
        EmployerProfileScreen(navController)
    }
    
    // Analytics
    composable(Routes.ANALYTICS) {
        AnalyticsScreen(navController)
    }
    
    // Employer Notifications
    composable(Routes.EMPLOYER_NOTIFICATIONS) {
        com.example.dutype.employer.screens.EmployerNotificationScreen(
            onBackClick = { navController.popBackStack() },
            navController = navController
        )
    }
    
    // Trust Badges
    composable(Routes.EMPLOYER_TRUST_BADGES) {
        com.example.dutype.employer.screens.TrustBadgesScreen(
            navController = navController
        )
    }
    
    // My Ratings - redirect to support for now
    composable(Routes.EMPLOYER_MY_RATINGS) {
        com.example.dutype.employer.screens.EmployerSupportScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Refer & Earn
    composable(Routes.EMPLOYER_REFER_EARN) {
        com.example.dutype.employer.screens.EmployerReferEarnScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Manage Addresses
    composable(Routes.EMPLOYER_MANAGE_ADDRESSES) {
        com.example.dutype.employer.screens.settings.EmployerAddressManagementScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // More Settings
    composable(Routes.EMPLOYER_MORE_SETTINGS) {
        com.example.dutype.employer.screens.settings.EmployerMoreSettingsScreen(
            navController = navController,
            rootNavController = navController
        )
    }
    
    // Employer About
    composable(Routes.EMPLOYER_ABOUT) {
        com.example.dutype.employer.screens.EmployerAboutScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Employer Help
    composable(Routes.EMPLOYER_HELP) {
        com.example.dutype.employer.screens.EmployerSupportScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Help (common route)
    composable(Routes.HELP) {
        com.example.dutype.common.screens.support.HelpMainScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // FAQ - Opens web URL
    composable(Routes.FAQ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        androidx.compose.runtime.LaunchedEffect(Unit) {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(com.example.dutype.utils.AppConstants.FAQ_URL))
            context.startActivity(intent)
            navController.popBackStack()
        }
    }
    
    // Report Problem
    composable(Routes.REPORT) {
        com.example.dutype.common.screens.support.ReportProblemScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Tutorial
    composable(Routes.TUTORIAL) {
        com.example.dutype.common.screens.support.TutorialScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Language Selection - Now handled via bottom sheet in profile screens
    composable(Routes.LANGUAGE_SELECTION) {
        // Navigate back - language selection is now a bottom sheet
        androidx.compose.runtime.LaunchedEffect(Unit) {
            navController.popBackStack()
        }
    }
    
    // Contact Us
    composable(Routes.CONTACT_US) {
        com.example.dutype.common.screens.support.ContactUsScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Employer History
    composable(Routes.EMPLOYER_HISTORY) {
        com.example.dutype.employer.screens.EmployerHistoryScreen(
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
    }
    
    // Work Verification (Employer scans worker QR)
    composable(
        route = Routes.EMPLOYER_VERIFY_WORK,
        arguments = listOf(
            navArgument("jobId") { type = NavType.StringType },
            navArgument("applicationId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
        val applicationId = backStackEntry.arguments?.getString("applicationId") ?: ""
        val workVerificationService: com.example.dutype.services.WorkVerificationService = hiltViewModel<com.example.dutype.viewmodels.WorkVerificationViewModel>().workVerificationService
        com.example.dutype.employer.screens.EmployerVerifyWorkScreen(
            jobId = jobId,
            applicationId = applicationId,
            navController = navController,
            workVerificationService = workVerificationService
        )
    }
}

/**
 * Employer bottom navigation routes (used in EmployerMainScreen)
 */
object EmployerBottomRoutes {
    const val DASHBOARD = "dashboard"
    const val MY_JOBS = "employer_my_jobs"
    const val POST_JOB = "employer_post_job"
    const val PROFILE = "employer_profile"
}
