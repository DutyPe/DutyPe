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
import com.example.dutype.employer.screens.EmployerJobPreviewScreen

import com.example.dutype.employer.screens.EmployerCompanyDetailsScreen
import com.example.dutype.employer.screens.EmployerHomeScreen
import com.example.dutype.employer.screens.EmployerNotificationScreen
import com.example.dutype.employer.screens.EmployerReferEarnScreen
import com.example.dutype.employer.screens.EmployerSupportScreen
import com.example.dutype.employer.screens.EmployerAboutScreen
import com.example.dutype.employer.screens.EmployerUrgentNeedDetailScreen
import com.example.dutype.employer.screens.settings.EmployerAddressManagementScreen

import com.example.dutype.employer.screens.MandatoryEmployerProfileSetupScreen
import com.example.dutype.employer.screens.PostJobScreen
import com.example.dutype.employer.screens.PostUrgentNeedScreen
import com.example.dutype.employer.screens.ProfessionalWorkerProfileViewScreen
import com.example.dutype.employer.screens.applications.EmployerApplicationManagementScreen
import com.example.dutype.employer.screens.profilescreen.EmployerProfileScreen

import com.example.dutype.utils.rememberScrollStateManager

@Composable
fun EmployerMainScreen(
    rootNavController: NavController,
    notificationPermissionManager: com.example.dutype.utils.NotificationPermissionManager
) {
    val navController = rememberNavController()
    val scrollStateManager = rememberScrollStateManager()

    // BUG #1 FIX: Drain any inner-route handoff queued by the outer graph
    // (e.g. when the user finishes employer profile setup with
    // `returnRoute = employer_post_job`). Runs once after this NavHost mounts
    // so the desired destination is pushed onto the inner controller.
    androidx.compose.runtime.LaunchedEffect(navController) {
        EmployerInnerNavQueue.consume()?.let { pending ->
            navController.navigate(pending) {
                launchSingleTop = true
            }
        }
    }

    // Track current route to conditionally show bottom bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Create a unified state for the current screen to manage status bar color.
    // Defaults to the employer surface; updated via LaunchedEffect when the
    // dark-mode-aware token resolves.
    val initialStatusBarColor = com.example.dutype.ui.theme.EmployerColors.StatusBarColor
    var currentStatusBarColor by remember { mutableStateOf(initialStatusBarColor) }
    androidx.compose.runtime.LaunchedEffect(initialStatusBarColor) {
        currentStatusBarColor = initialStatusBarColor
    }
    val isBottomBarVisible by scrollStateManager.isBottomBarVisible

    // Routes where bottom bar should be hidden
    val routesWithoutBottomBar = listOf(
        Routes.EMPLOYER_APPLICATIONS,
        Routes.EMPLOYER_APPLICATIONS_JOB,
        Routes.WORKER_PROFILE_VIEW, // Batch-p #6.1: hide bottom bar on worker profile view
        Routes.CONTACT_US,
        Routes.EMPLOYER_NOTIFICATIONS,
        Routes.EMPLOYER_POST_JOB,
        Routes.EMPLOYER_POST_URGENT_NEED,
        Routes.EMPLOYER_URGENT_NEED_DETAIL,
        Routes.EMPLOYER_COMPANY_DETAILS,
        Routes.ANALYTICS,
        Routes.EDIT_JOB,
        Routes.EMPLOYER_JOB_PREVIEW,
        Routes.EMPLOYER_PROFILE_SETUP, // Hide bottom bar on profile setup
        Routes.HELP,
        Routes.ABOUT_US,
        Routes.EMPLOYER_ABOUT,
        Routes.EMPLOYER_HELP,
        Routes.EMPLOYER_MANAGE_ADDRESSES,
        Routes.EMPLOYER_HISTORY,
        Routes.EMPLOYER_REFER_EARN,
    )
    
    // Check if current route should hide bottom bar
    val shouldShowBottomBar = currentRoute?.let { route ->
        !routesWithoutBottomBar.any { hiddenRoute ->
            route == hiddenRoute || route.startsWith(hiddenRoute.substringBefore("{"))
        }
    } ?: true

    // ---- ROLE THEME ----
    // Publish the employer palette via LocalRoleColors so every screen and
    // card hosted under this scaffold reads its background / card / divider
    // colour from the same source (no per-screen hardcoded hex).
    com.example.dutype.ui.theme.DutyPeEmployerTheme {
        val roleColors = com.example.dutype.ui.theme.LocalRoleColors.current

        // Main container that handles all system bars
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(roleColors.screenBackground)
        ) {

        // Status bar overlay - ALWAYS at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(currentStatusBarColor)
                .align(Alignment.TopCenter)
                .zIndex(1000f) // Ensure it's always on top
        )

        // Navigation bar overlay — rendered only on routes where the
        // EmployerBottomBar is NOT shown. The bottom bar itself now
        // extends under the gesture area and paints that region, so
        // drawing a second overlay there would create a visible flat
        // "second bar" below the rounded bottom nav (bug #2).
        if (!shouldShowBottomBar) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    .background(roleColors.navigationBar)
                    .align(Alignment.BottomCenter)
                    .zIndex(1000f) // Ensure it's always on top
            )
        }

        // Main content area
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = roleColors.screenBackground,
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
                            onJobPosted = { newJobId ->
                                if (!newJobId.isNullOrBlank()) {
                                    navController.navigate(Routes.employerApplicationsJobRoute(newJobId)) {
                                        popUpTo(Routes.EMPLOYER_DASHBOARD) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate(Routes.EMPLOYER_DASHBOARD) {
                                        popUpTo(Routes.EMPLOYER_DASHBOARD) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    composable(Routes.EMPLOYER_POST_URGENT_NEED) {
                        PostUrgentNeedScreen(navController = navController)
                    }
                    composable(
                        route = Routes.EMPLOYER_URGENT_NEED_DETAIL,
                        arguments = listOf(navArgument("requestId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
                        EmployerUrgentNeedDetailScreen(
                            navController = navController,
                            requestId = requestId
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
                        com.example.dutype.employer.screens.EmployerHistoryScreen(
                            navController = navController,
                            onStatusBarColorChange = { color: Color ->
                                currentStatusBarColor = color
                            }
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

                    // Apr 2026: OLX-style preview shown after a fresh post and
                    // when an employer taps a card on the My Job Posts screen.
                    composable(
                        route = Routes.EMPLOYER_JOB_PREVIEW,
                        arguments = listOf(navArgument("jobId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
                        EmployerJobPreviewScreen(
                            navController = navController,
                            jobId = jobId
                        )
                    }
                    
                    // Application Management Routes - CRITICAL MISSING ROUTES
                    composable(Routes.EMPLOYER_APPLICATIONS) {
                        EmployerApplicationManagementScreen(
                            jobId = null, // View all applications
                            onApplicationClick = { application ->
                                navController.navigate(Routes.workerProfileViewRoute(application.workerId, application.id))
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
                                navController.navigate(Routes.workerProfileViewRoute(application.workerId, application.id))
                            },
                            onBackClick = { navController.popBackStack() },
                            onPostUrgentNeed = { navController.navigate(Routes.EMPLOYER_POST_URGENT_NEED) }
                        )
                    }

                    // Worker Profile View Route
                    composable(
                        Routes.WORKER_PROFILE_VIEW,
                        arguments = listOf(
                            navArgument("workerId") { type = NavType.StringType },
                            navArgument("applicationId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        ),
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
                        val applicationId = backStackEntry.arguments?.getString("applicationId")
                        ProfessionalWorkerProfileViewScreen(
                            navController = navController,
                            workerId = workerId,
                            applicationId = applicationId,
                            scrollStateManager = scrollStateManager
                        )
                    }
                    
                    composable(Routes.EMPLOYER_NOTIFICATIONS) {
                        EmployerNotificationScreen(
                            onBackClick = { navController.popBackStack() },
                            navController = navController
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

                    // Help & About — registered locally so the employer profile
                    // menu can navigate without leaving the bottom-bar host.
                    composable(Routes.HELP) {
                        com.example.dutype.common.screens.support.HelpMainScreen(
                            navController = navController,
                            onStatusBarColorChange = { color -> currentStatusBarColor = color }
                        )
                    }
                    composable(Routes.ABOUT_US) {
                        com.example.dutype.worker.screens.WorkerAboutScreen(
                            navController = navController,
                            onStatusBarColorChange = { color -> currentStatusBarColor = color }
                        )
                    }

                    composable(Routes.EMPLOYER_MANAGE_ADDRESSES) {
                        EmployerAddressManagementScreen(
                            navController = navController,
                            onStatusBarColorChange = { color -> currentStatusBarColor = color }
                        )
                    }

                    composable(Routes.EMPLOYER_HELP) {
                        EmployerSupportScreen(
                            navController = navController,
                            onStatusBarColorChange = { color -> currentStatusBarColor = color }
                        )
                    }

                    composable(Routes.EMPLOYER_ABOUT) {
                        EmployerAboutScreen(
                            navController = navController,
                            onStatusBarColorChange = { color -> currentStatusBarColor = color }
                        )
                    }

                    composable(Routes.EMPLOYER_REFER_EARN) {
                        EmployerReferEarnScreen(
                            navController = navController,
                            onStatusBarColorChange = { color -> currentStatusBarColor = color }
                        )
                    }

                    composable(Routes.EMPLOYER_HISTORY) {
                        com.example.dutype.employer.screens.EmployerHistoryScreen(
                            navController = navController
                        )
                    }
                }
            }
        }
    }
    } // end DutyPeEmployerTheme
}

