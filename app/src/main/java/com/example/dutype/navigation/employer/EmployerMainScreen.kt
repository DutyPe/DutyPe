package com.example.dutype.navigation.employer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import com.example.dutype.common.chat.help.SecurityScreen
import com.example.dutype.common.chat.help.SecurityLegalScreen
import com.example.dutype.common.chat.info.PrivacyPolicyScreen
import com.example.dutype.common.chat.info.TermsAndConditionsScreen
import com.example.dutype.common.employer.EmployerProfileScreen
import com.example.dutype.components.EmployerBottomBarItems
import com.example.dutype.components.ReusableBottomBar
import com.example.dutype.employer.screens.AnalyticsScreen
import com.example.dutype.employer.screens.EmployerNotificationDetailScreen
import com.example.dutype.employer.screens.EmployerNotificationScreen
import com.example.dutype.employer.screens.ProfessionalApplicantManagementScreen
import com.example.dutype.employer.screens.ProfessionalWorkerProfileViewScreen
import com.example.dutype.employer.screens.about.EmployerAboutScreen
import com.example.dutype.employer.screens.applications.ApplicationDetailScreen
import com.example.dutype.employer.screens.applications.EmployerApplicationManagementScreen
import com.example.dutype.employer.screens.editjob.EditJobScreen
import com.example.dutype.employer.screens.homeScreen.EmployerHomeScreen
import com.example.dutype.employer.screens.postedJobs.PostedJobsScreen
import com.example.dutype.employer.screens.postjob.PostJobScreen
import com.example.dutype.employer.screens.profile.EmployerCompanyDetailsScreen
import com.example.dutype.employer.screens.referral.EmployerReferEarnScreen
import com.example.dutype.employer.screens.settings.EmployerAddressManagementScreen
import com.example.dutype.employer.screens.support.EmployerSupportScreen
import com.example.dutype.navigation.Routes
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
    var currentStatusBarColor by remember { mutableStateOf(Color(0xFF1A237E)) }
    val isBottomBarVisible by scrollStateManager.isBottomBarVisible

    // Routes where bottom bar should be hidden
    val routesWithoutBottomBar = listOf(
        Routes.EMPLOYER_APPLICATIONS,
        Routes.EMPLOYER_APPLICATIONS_JOB,
        Routes.EMPLOYER_APPLICATION_DETAIL,
        Routes.EMPLOYER_ABOUT,
        Routes.PRIVACY,
        Routes.TERMS,
        Routes.EMPLOYER_NOTIFICATIONS,
        Routes.EMPLOYER_NOTIFICATION_DETAIL,
        Routes.EMPLOYER_POST_JOB,
        Routes.EMPLOYER_HELP,
        Routes.SECURITY,
        Routes.SECURITY_LEGAL,
        Routes.EMPLOYER_MANAGE_ADDRESSES,
        Routes.EMPLOYER_COMPANY_DETAILS,
        Routes.EMPLOYER_NOTIFICATION_SETTINGS,
        Routes.ANALYTICS,
        Routes.EDIT_JOB,
        Routes.EMPLOYER_HISTORY
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

        // Navigation bar overlay - Always show
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(Color.Black)
                .align(Alignment.BottomCenter)
                .zIndex(1000f) // Ensure it's always on top
        )

        // Main content area
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (shouldShowBottomBar) {
                    ReusableBottomBar(
                        navController = navController,
                        items = EmployerBottomBarItems.items,
                        selectedItemColor = Color(0xFF3B82F6) // Blue for employer bottom bar
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    )
                    .padding(
                        top = paddingValues.calculateTopPadding(),
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
                    composable(
                        Routes.VIEW_APPLICANTS,
                        arguments = listOf(navArgument("jobId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
                        ProfessionalApplicantManagementScreen(
                            navController = navController,
                            jobId = jobId,
                            jobTitle = "Job Applications",
                            scrollStateManager = scrollStateManager
                        )
                    }
                    
                    // Application Management Routes - CRITICAL MISSING ROUTES
                    composable(Routes.EMPLOYER_APPLICATIONS) {
                        EmployerApplicationManagementScreen(
                            jobId = null, // View all applications
                            onApplicationClick = { application ->
                                navController.navigate("employer_application_detail/${application.applicationId}")
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
                                navController.navigate("employer_application_detail/${application.applicationId}")
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
                        
                        ApplicationDetailScreen(
                            applicationId = applicationId,
                            onBackClick = { navController.popBackStack() },
                            onUpdateStatus = { newStatus, notes ->
                                // Update application status using the EmployerApplicationViewModel
                                employerViewModel.updateApplicationStatus(
                                    applicationId = applicationId,
                                    newStatus = newStatus,
                                    notes = notes
                                )
                                navController.popBackStack()
                            }
                        )
                    }

                    // Worker Profile View Route
                    composable(
                        Routes.WORKER_PROFILE_VIEW,
                        arguments = listOf(navArgument("workerId") { type = NavType.StringType })
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
                    
                    composable(
                        route = Routes.EMPLOYER_NOTIFICATION_DETAIL,
                        arguments = listOf(navArgument("notificationId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val notificationId = backStackEntry.arguments?.getString("notificationId") ?: ""
                        EmployerNotificationDetailScreen(
                            notificationId = notificationId,
                            onBackClick = { navController.popBackStack() },
                            navController = navController
                        )
                    }
                    
                    composable(Routes.EMPLOYER_NOTIFICATION_SETTINGS) {
                        com.example.dutype.employer.screens.settings.EmployerNotificationSettingsScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
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
                    
                    // Edit Job Route
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
                    
                    // Analytics Route
                    composable(Routes.ANALYTICS) {
                        AnalyticsScreen(
                            navController = navController
                        )
                    }
                    
                    // Privacy, Terms, and Security Routes
                    composable(Routes.PRIVACY) {
                        PrivacyPolicyScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    
                    composable(Routes.TERMS) {
                        TermsAndConditionsScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    
                    composable(Routes.SECURITY) {
                        SecurityScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    
                    composable(Routes.SECURITY_LEGAL) {
                        SecurityLegalScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    
                    // Employer History Route
                    composable(Routes.EMPLOYER_HISTORY) {
                        com.example.dutype.employer.screens.history.EmployerHistoryScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                }
            }
        }
    }
}
