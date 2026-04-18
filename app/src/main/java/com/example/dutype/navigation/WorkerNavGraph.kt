package com.example.dutype.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.worker.screens.MandatoryWorkerProfileSetupScreen
import timber.log.Timber

/**
 * WorkerNavGraph - Worker-specific navigation graph
 * 
 * Contains all routes accessible only to workers:
 * - Job browsing and search
 * - Job applications
 * - Worker profile management
 * - My Jobs (applications tracking)
 * - Earnings dashboard
 * - Work verification
 * 
 * ARCHITECTURE IMPROVEMENT (January 2026):
 * Split from MainNavGraph for better maintainability and faster compile times.
 * 
 * @author DutyPe Engineering Team
 * @since 2.2.0
 */
@Composable
fun WorkerNavGraph(
    navController: NavHostController,
    rootNavController: NavHostController,
    onStatusBarColorChange: (Color) -> Unit = {},
    scrollStateManager: ScrollStateManager? = null,
    notificationPermissionManager: com.example.dutype.utils.NotificationPermissionManager
) {
    NavHost(
        navController = navController,
        startDestination = WorkerBottomRoutes.HOME
    ) {
        // Home Tab
        composable(WorkerBottomRoutes.HOME) {
            com.example.dutype.worker.screens.WorkerHomeScreen(
                navController = navController,
                rootNavController = rootNavController,
                onStatusBarColorChange = onStatusBarColorChange,
                scrollStateManager = scrollStateManager,
                notificationPermissionManager = notificationPermissionManager
            )
        }
        
        // My Jobs Tab
        composable(WorkerBottomRoutes.MY_JOBS) {
            com.example.dutype.worker.screens.myJobs.MyJobsScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange,
                scrollStateManager = scrollStateManager
            )
        }
        
        // Profile Tab
        composable(WorkerBottomRoutes.PROFILE) {
            val context = LocalContext.current
            val dataStore = remember { com.example.dutype.data.ApplicationFormDataStore(context) }
            com.example.dutype.worker.screens.profile.WorkerProfileScreen(
                rootNavController = rootNavController,
                localNavController = navController,
                onStatusBarColorChange = onStatusBarColorChange,
                scrollStateManager = scrollStateManager,
                dataStore = dataStore
            )
        }
        
        // Job Detail - with interstitial ad on back navigation
        // MODERN DEEP LINK IMPLEMENTATION (2024-2026 Standard)
        // Uses navDeepLink() as per official Android documentation
        composable(
            route = Routes.JOB_DETAIL,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType }),
            deepLinks = listOf(
                // App scheme: dutype://job/{jobId}
                androidx.navigation.navDeepLink {
                    uriPattern = "dutype://job/{jobId}"
                },
                // Web scheme: https://dutype.in/jobs/{jobId}
                androidx.navigation.navDeepLink {
                    uriPattern = "https://dutype.in/jobs/{jobId}"
                }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            Timber.i("🔗 DEEP LINK: JobDescriptionScreen opened with jobId: $jobId")
            // Get FirestoreJobViewModel using hiltViewModel() and then access its adManager
            val firestoreJobViewModel: com.example.dutype.viewmodels.FirestoreJobViewModel = hiltViewModel()
            com.example.dutype.worker.screens.JobDescriptionScreen(
                jobId = jobId,
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange,
                adManager = firestoreJobViewModel.adManager
            )
        }
        
        // Job Application Screen
        composable(
            route = Routes.JOB_APPLICATION,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            com.example.dutype.worker.screens.JobApplicationScreen(
                jobId = jobId,
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        
        // All Jobs (with filter and optional voice query)
        composable(
            route = "${Routes.WORKER_ALL_JOBS}?filter={filter}&voiceQuery={voiceQuery}",
            arguments = listOf(
                navArgument("filter") { type = NavType.StringType; defaultValue = "All Jobs"; nullable = true },
                navArgument("voiceQuery") { type = NavType.StringType; defaultValue = null; nullable = true }
            )
        ) { backStackEntry ->
            val filter = backStackEntry.arguments?.getString("filter") ?: "All Jobs"
            val voiceQuery = backStackEntry.arguments?.getString("voiceQuery")
            com.example.dutype.worker.screens.AllJobsScreen(
                navController = navController,
                rootNavController = rootNavController,
                initialFilter = filter,
                voiceQuery = voiceQuery,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        
        // All Jobs (primary route, supports ?filter=&voiceQuery=)
        // Legacy path-based variant removed — callers now use query params.
        
        // Categories Screen (all categories)
        composable(Routes.WORKER_CATEGORIES) {
            com.example.dutype.worker.screens.CategoriesScreen(
                navController = navController,
                rootNavController = rootNavController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        
        // Categories Screen (with pre-selected category)
        composable(
            route = Routes.WORKER_CATEGORIES_FILTERED,
            arguments = listOf(navArgument("category") { type = NavType.StringType; defaultValue = "All" })
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: "All"
            com.example.dutype.worker.screens.CategoriesScreen(
                navController = navController,
                rootNavController = rootNavController,
                initialCategory = category,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        
        // Worker Profile Details
        composable(Routes.WORKER_PROFILE_DETAILS) {
            val context = LocalContext.current
            val dataStore = remember { com.example.dutype.data.ApplicationFormDataStore(context) }
            com.example.dutype.worker.screens.profile.WorkerProfileDetailsScreen(
                navController = navController,
                dataStore = dataStore
            )
        }
        
        // Worker Visiting Card
        composable(Routes.WORKER_VISITING_CARD) {
            com.example.dutype.worker.screens.profile.DigitalVisitingCardScreen(
                navController = navController
            )
        }
       
        
        // Worker Notifications
        composable(Routes.WORKER_NOTIFICATIONS) {
            com.example.dutype.worker.screens.WorkerNotificationScreen(
                onBackClick = { navController.popBackStack() },
                navController = navController
            )
        }
        
        // Worker Job Map
        composable(Routes.WORKER_JOB_MAP) {
            com.example.dutype.worker.screens.map.JobMapScreen(
                navController = navController,
                rootNavController = rootNavController
            )
        }
        
        // Worker Earnings Dashboard
        composable(Routes.WORKER_EARNINGS) {
            com.example.dutype.worker.screens.EarningsDashboardScreen(
                navController = navController
            )
        }
        
        // Worker About Screen
        composable(Routes.ABOUT_US) {
            com.example.dutype.worker.screens.WorkerAboutScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        
        // Worker History
        composable(Routes.WORKER_HISTORY) {
            com.example.dutype.worker.screens.WorkerHistoryScreen(
                navController = navController
            )
        }
        
        // Help
        composable(Routes.HELP) {
            com.example.dutype.common.screens.support.HelpMainScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        
        // Contact Us
        composable(Routes.CONTACT_US) {
            com.example.dutype.common.screens.support.ContactUsScreen(
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
        
        // Worker Refer & Earn
        composable(Routes.WORKER_REFER_EARN) {
            com.example.dutype.worker.screens.WorkerReferEarnScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        
        // Profile Setup (for job application flow when profile is incomplete)
        composable(Routes.PROFILE_SETUP) {
            MandatoryWorkerProfileSetupScreen(navController = navController)
        }
        
        // Profile Setup with return route (for job application flow)
        composable(
            route = "profile_setup?returnRoute={returnRoute}",
            arguments = listOf(
                navArgument("returnRoute") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val returnRoute = backStackEntry.arguments?.getString("returnRoute")?.let {
                try { java.net.URLDecoder.decode(it, "UTF-8") } catch (e: Exception) { null }
            }
            MandatoryWorkerProfileSetupScreen(
                navController = navController,
                returnRoute = returnRoute
            )
        }
    }
}

/**
 * Worker bottom navigation routes (used in WorkerMainScreen)
 */
object WorkerBottomRoutes {
    const val HOME = "home"
    const val MY_JOBS = "myjobs"
    const val PROFILE = "profile"
}
