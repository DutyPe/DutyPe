package com.example.dutype.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.dutype.components.EmployerBottomBar
import com.example.dutype.employer.screens.AnalyticsScreen
import com.example.dutype.employer.screens.EditJobScreen
import com.example.dutype.employer.screens.EmployerAboutScreen
import com.example.dutype.employer.screens.EmployerCompanyDetailsScreen
import com.example.dutype.employer.screens.EmployerHomeScreen
import com.example.dutype.employer.screens.EmployerNotificationScreen
import com.example.dutype.employer.screens.EmployerReferEarnScreen
import com.example.dutype.employer.screens.EmployerSupportScreen
import com.example.dutype.employer.screens.MandatoryEmployerProfileSetupScreen
import com.example.dutype.employer.screens.PostJobScreen
import com.example.dutype.employer.screens.PostedJobsScreen
import com.example.dutype.employer.screens.ProfessionalWorkerProfileViewScreen
import com.example.dutype.employer.screens.applications.ApplicationDetailScreen
import com.example.dutype.employer.screens.applications.EmployerApplicationManagementScreen
import com.example.dutype.employer.screens.profilescreen.EmployerProfileScreen
import com.example.dutype.employer.screens.settings.EmployerAddressManagementScreen
import com.example.dutype.utils.rememberScrollStateManager

@Composable
fun EmployerMainScreen(
    rootNavController: NavController,
    notificationPermissionManager: com.example.dutype.utils.NotificationPermissionManager
) {
    val navController = rememberNavController()
    val scrollStateManager = rememberScrollStateManager()

    // Track current route to conditionally show bottom bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Create a unified state for the current screen to manage status bar color
    // Initialize with Blue for employer side
    var currentStatusBarColor by remember { mutableStateOf(com.example.dutype.ui.theme.EmployerColors.StatusBarColor) }
    val isBottomBarVisible by scrollStateManager.isBottomBarVisible

    // Routes where bottom bar should be hidden
    val routesWithoutBottomBar = listOf(
        Routes.EMPLOYER_APPLICATIONS,
        Routes.EMPLOYER_APPLICATIONS_JOB,
        Routes.EMPLOYER_APPLICATION_DETAIL,
        Routes.EMPLOYER_ABOUT,
        Routes.CONTACT_US,
        Routes.EMPLOYER_NOTIFICATIONS,
        Routes.EMPLOYER_POST_JOB,
        Routes.EMPLOYER_HELP,
        Routes.EMPLOYER_MANAGE_ADDRESSES,
        Routes.EMPLOYER_COMPANY_DETAILS,
        Routes.ANALYTICS,
        Routes.EDIT_JOB,
        Routes.EMPLOYER_HISTORY,
        Routes.EMPLOYER_MORE_SETTINGS,
        Routes.EMPLOYER_MY_RATINGS,
        Routes.EMPLOYER_TRUST_BADGES, // Hide bottom bar on trust badges screen
        Routes.EMPLOYER_AI_POST_JOB,
        Routes.EMPLOYER_VOICE_POST_JOB, // Hide bottom bar for voice job posting
        Routes.EMPLOYER_PROFILE_SETUP, // Hide bottom bar on profile setup
        Routes.EMPLOYER_VISITING_CARD, // Hide bottom bar on visiting card
        Routes.VIEW_APPLICANTS, // Hide bottom bar on applicant management screen
        // Routes.EMPLOYER_REFER_EARN // Commented out - will be released in v2
    )
    
    // Check if current route should hide bottom bar
    val shouldShowBottomBar = currentRoute?.let { route ->
        !routesWithoutBottomBar.any { hiddenRoute ->
            route == hiddenRoute || route.startsWith(hiddenRoute.substringBefore("{"))
        }
    } ?: true

    // Main container that handles all system bars
    Box(modifier = Modifier.fillMaxSize()) {

        // Status bar overlay - ALWAYS at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(currentStatusBarColor)
                .align(Alignment.TopCenter)
                .zIndex(1000f) // Ensure it's always on top
        )

        // Navigation bar overlay - Always show (white background)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(Color.White)
                .align(Alignment.BottomCenter)
                .zIndex(1000f) // Ensure it's always on top
        )

        // Main content area
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (shouldShowBottomBar) {
                    // Use custom EmployerBottomBar with interstitial ad before Post Job
                    EmployerBottomBar(
                        navController = navController,
                        selectedItemColor = com.example.dutype.ui.theme.EmployerColors.BottomNavSelected
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = paddingValues.calculateBottomPadding()
                    )
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.EMPLOYER_DASHBOARD
                ) {
                    composable(Routes.EMPLOYER_DASHBOARD) {
                        EmployerHomeScreen(
                            navController = navController,
                            rootNavController = rootNavController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            },
                            scrollStateManager = scrollStateManager,
                            notificationPermissionManager = notificationPermissionManager
                        )
                    }
                    composable(Routes.EMPLOYER_POST_JOB) {
                        PostJobScreen(
                            navController = navController,
                            rootNavController = rootNavController,
                            employerId = null,
                            onJobPosted = {
                                // Navigate back to dashboard after job is posted
                                navController.navigate(Routes.EMPLOYER_DASHBOARD) {
                                    popUpTo(Routes.EMPLOYER_DASHBOARD) { inclusive = false }
                                }
                            },
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    composable(Routes.EMPLOYER_VOICE_POST_JOB) {
                        com.example.dutype.employer.screens.VoiceJobPostingScreen(navController = navController)
                    }
                    composable(Routes.EMPLOYER_PROFILE) {
                        EmployerProfileScreen(
                            rootNavController = rootNavController,
                            localNavController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    composable(Routes.EMPLOYER_COMPANY_DETAILS) {
                        EmployerCompanyDetailsScreen(
                            navController = navController
                        )
                    }
                    composable(Routes.EMPLOYER_MY_JOBS) {
                        PostedJobsScreen(
                            navController = navController
                        )
                    }
                    
                    // CRITICAL FIX: Add EDIT_JOB route
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
                    
                    composable(
                        Routes.VIEW_APPLICANTS,
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
                    
                    // Application Management Routes - CRITICAL MISSING ROUTES
                    composable(Routes.EMPLOYER_APPLICATIONS) {
                        EmployerApplicationManagementScreen(
                            jobId = null, // View all applications
                            onApplicationClick = { application ->
                                navController.navigate("employer_application_detail/${application.id}")
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Routes.EMPLOYER_APPLICATIONS_JOB,
                        arguments = listOf(navArgument("jobId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
                        EmployerApplicationManagementScreen(
                            jobId = jobId, // View applications for specific job
                            onApplicationClick = { application ->
                                navController.navigate("employer_application_detail/${application.id}")
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }

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
                                // For ACCEPTED status, use hireApplicant which checks vacancy limits
                                if (newStatus == com.example.dutype.models.ApplicationStatus.ACCEPTED) {
                                    // Get the application to find jobId
                                    val application = employerViewModel.uiState.value.applications.find { it.applicationId == applicationId }
                                    if (application != null) {
                                        employerViewModel.hireApplicant(
                                            applicationId = applicationId,
                                            jobId = application.jobId,
                                            onSuccess = {
                                                android.widget.Toast.makeText(context, "Applicant hired successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                                navController.popBackStack()
                                            },
                                            onError = { error ->
                                                android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } else {
                                        // Fallback to regular update
                                        employerViewModel.updateApplicationStatus(
                                            applicationId = applicationId,
                                            newStatus = newStatus,
                                            notes = notes
                                        )
                                        navController.popBackStack()
                                    }
                                } else {
                                    // For other statuses, use regular update
                                    employerViewModel.updateApplicationStatus(
                                        applicationId = applicationId,
                                        newStatus = newStatus,
                                        notes = notes
                                    )
                                    navController.popBackStack()
                                }
                            },
                            onVerifyWork = { jobId, appId ->
                                // Navigate to Work Verification screen
                                navController.navigate(Routes.employerVerifyWorkRoute(jobId, appId))
                            }
                        )
                    }

                    // Worker Profile View Route
                    composable(
                        Routes.WORKER_PROFILE_VIEW,
                        arguments = listOf(navArgument("workerId") { type = NavType.StringType }),
                        deepLinks = listOf(
                            navDeepLink {
                                uriPattern = "dutype://worker/{workerId}"
                            },
                            navDeepLink {
                                uriPattern = "https://dutype.in/worker/{workerId}"
                            }
                        )
                    ) { backStackEntry ->
                        val workerId = backStackEntry.arguments?.getString("workerId") ?: ""
                        ProfessionalWorkerProfileViewScreen(
                            navController = navController,
                            workerId = workerId,
                            scrollStateManager = scrollStateManager
                        )
                    }
                    
                  
                    
                    composable(Routes.EMPLOYER_MANAGE_ADDRESSES) {
                        EmployerAddressManagementScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    
                    composable(Routes.EMPLOYER_NOTIFICATIONS) {
                        EmployerNotificationScreen(
                            onBackClick = { navController.popBackStack() },
                            navController = navController
                        )
                    }
                    
                    composable(Routes.EMPLOYER_HELP) {
                        EmployerSupportScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    
                    composable(Routes.EMPLOYER_ABOUT) {
                        EmployerAboutScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    
                    composable(Routes.EMPLOYER_REFER_EARN) {
                        EmployerReferEarnScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    
                    // Analytics Route
                    composable(Routes.ANALYTICS) {
                        AnalyticsScreen(
                            navController = navController
                        )
                    }
                    
                    // Contact Us Route
                    composable(Routes.CONTACT_US) {
                        com.example.dutype.common.screens.support.ContactUsScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    
                    // Employer History Route
                    composable(Routes.EMPLOYER_HISTORY) {
                        com.example.dutype.employer.screens.EmployerHistoryScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    
                    // Employer More Settings Route
                    composable(Routes.EMPLOYER_MORE_SETTINGS) {
                        com.example.dutype.employer.screens.settings.EmployerMoreSettingsScreen(
                            navController = navController,
                            rootNavController = rootNavController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    
                    // Employer Trust Badges Explanation Screen
                    composable(Routes.EMPLOYER_TRUST_BADGES) {
                        com.example.dutype.employer.screens.TrustBadgesScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    
                    // Work Start Verification - Employer Verify Screen
                    composable(
                        route = Routes.EMPLOYER_VERIFY_WORK,
                        arguments = listOf(
                            navArgument("jobId") { type = NavType.StringType },
                            navArgument("applicationId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
                        val applicationId = backStackEntry.arguments?.getString("applicationId") ?: ""
                        // WorkVerificationService accessed via WorkVerificationViewModel (proper DI pattern)
                        val workVerificationViewModel = hiltViewModel<com.example.dutype.viewmodels.WorkVerificationViewModel>()
                        com.example.dutype.employer.screens.EmployerVerifyWorkScreen(
                            jobId = jobId,
                            applicationId = applicationId,
                            navController = navController,
                            workVerificationService = workVerificationViewModel.workVerificationService
                        )
                    }
                    
                    // AI-Enhanced Job Posting
                    composable(Routes.EMPLOYER_AI_POST_JOB) {
                        com.example.dutype.employer.screens.AIJobPostingScreen(
                            navController = navController,
                            onJobPosted = {
                                navController.navigate(Routes.EMPLOYER_DASHBOARD) {
                                    popUpTo(Routes.EMPLOYER_DASHBOARD) { inclusive = false }
                                }
                            },
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    
                    // Employer Profile Setup Route - CRITICAL: Must be in EmployerMainScreen for visiting card navigation
                    composable(
                        route = "${Routes.EMPLOYER_PROFILE_SETUP}?returnRoute={returnRoute}",
                        arguments = listOf(navArgument("returnRoute") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        })
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
                    
                    // Employer Visiting Card Route
                    composable(Routes.EMPLOYER_VISITING_CARD) {
                        com.example.dutype.employer.screens.profile.EmployerDigitalVisitingCardScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            },
                            onBottomBarVisibilityChange = { /* handled by routesWithoutBottomBar */ }
                        )
                    }
                }
            }
        }
    }
}

