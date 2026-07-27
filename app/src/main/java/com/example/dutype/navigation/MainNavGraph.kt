package com.example.dutype.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.firebase.crashlytics.FirebaseCrashlytics
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.dutype.auth.EnhancedLoginScreen
import com.example.dutype.auth.RegisterScreen
import com.example.dutype.common.screens.SelectRoleScreen
import com.example.dutype.common.screens.TermsOfServiceScreen
import com.example.dutype.common.screens.PrivacyPolicyScreen

import com.example.dutype.employer.screens.AnalyticsScreen
import com.example.dutype.employer.screens.MandatoryEmployerProfileSetupScreen
import com.example.dutype.employer.screens.applications.EmployerApplicationManagementScreen
import kotlinx.coroutines.tasks.await
import com.example.dutype.employer.screens.EditJobScreen
import com.example.dutype.employer.screens.EmployerCompanyDetailsScreen
import com.example.dutype.employer.screens.EmployerSubscriptionScreen
import com.example.dutype.employer.screens.EmployerPublicProfileScreen
import com.example.dutype.employer.screens.profilescreen.EmployerProfileScreen
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.EmployerMainScreen
import com.example.dutype.navigation.WorkerMainScreen
import com.example.dutype.onboarding.OnboardingScreen
import com.example.dutype.worker.screens.MandatoryWorkerProfileSetupScreen
import com.example.dutype.employer.screens.ProfessionalWorkerProfileViewScreen
import com.example.dutype.utils.findActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

@Composable
fun MainNavGraph(
    navController: NavHostController,
    onStatusBarColorChange: (Color) -> Unit = {},
    onReady: () -> Unit = {},
    notificationData: String? = null,
    notificationPermissionManager: com.example.dutype.utils.NotificationPermissionManager,
    notificationIntent: android.content.Intent? = null
) {
    val context = LocalContext.current
    val profileCompletionViewModel: com.example.dutype.viewmodels.ProfileCompletionViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    // P2-4: Replaces the previous LocalBroadcastManager-based deep-link relay
    // with a Hilt-singleton SharedFlow. MainActivity.onNewIntent emits; we
    // collect here. One indirection instead of three, no broadcaster lifecycle.
    val deepLinkBus = remember(context) {
        com.example.dutype.di.ComposeServiceEntryPoint.from(context).deepLinkBus()
    }
    LaunchedEffect(deepLinkBus) {
        deepLinkBus.events.collect { uri ->
            Timber.i("📱 MainNavGraph: Received deep link: $uri")
            val activityIntent = context.findActivity()?.intent
            val handled = com.example.dutype.utils.DeepLinkHandler.handleDeepLinkUri(uri, navController)
            if (handled) {
                deepLinkBus.clearReplay()
                if (activityIntent?.getBooleanExtra("from_notification", false) == true) {
                    val routeBeforeNavigation = navController.currentBackStackEntry?.destination?.route
                    val openedRoute = withTimeoutOrNull(1000L) {
                        navController.currentBackStackEntryFlow
                            .map { it.destination.route ?: "unknown" }
                            .first { route -> route.isNotBlank() && route != routeBeforeNavigation }
                    } ?: navController.currentBackStackEntry?.destination?.route ?: "unknown"

                    runCatching {
                        FirebaseCrashlytics.getInstance().apply {
                            log("notification_destination_opened")
                            setCustomKey("notification_destination_source", "warm_deep_link_bus")
                            setCustomKey("notification_destination_route", openedRoute)
                            setCustomKey("notification_destination_deeplink", uri.toString())
                        }
                    }.onFailure { Timber.w(it, "Failed to log warm notification destination telemetry") }
                }
            }
        }
    }

    // Keep the cold-start cache fresh so next launch draws the correct screen
    // on the very first frame. Whenever the user lands on a stable entry-point
    // destination (home, role-select, profile-setup, onboarding), persist it.
    LaunchedEffect(navController) {
        val stableEntryPoints = setOf(
            Routes.ONBOARDING,
            Routes.SELECT_ROLE,
            Routes.WORKER_HOME,
            Routes.EMPLOYER_HOME,
            Routes.PROFILE_SETUP,
            Routes.EMPLOYER_PROFILE_SETUP,
        )
        navController.currentBackStackEntryFlow.collect { entry ->
            val route = entry.destination.route ?: return@collect
            if (route in stableEntryPoints) {
                runCatching { StartDestinationCache.save(context, route) }
            }
        }
    }
    
    val startupViewModel: com.example.dutype.viewmodels.AppStartupViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val startupState by startupViewModel.startupState.collectAsState()
    val isLoading = startupState is com.example.dutype.viewmodels.StartupState.Loading

    var startDestination by remember { mutableStateOf(Routes.ONBOARDING) }
    var navigationDetermined by remember { mutableStateOf(false) }

    LaunchedEffect(startupState) {
        when (val state = startupState) {
            is com.example.dutype.viewmodels.StartupState.Resolved -> {
                startDestination = state.startDestination
                navigationDetermined = true
                runCatching { onReady() }
                Timber.d("🚀 MainNavGraph - AppStartupViewModel resolved startDestination: ${state.startDestination}")
            }
            com.example.dutype.viewmodels.StartupState.Loading -> {
                // Keep loading until resolved
            }
        }
    }

    var pendingNotificationRouteTelemetry by remember {
        mutableStateOf(notificationIntent?.getBooleanExtra("from_notification", false) == true)
    }

    suspend fun logPendingNotificationDestinationOpened(source: String, deepLink: String? = null) {
        if (!pendingNotificationRouteTelemetry) return

        pendingNotificationRouteTelemetry = false
        val routeBeforeNavigation = navController.currentBackStackEntry?.destination?.route
        val openedRoute = withTimeoutOrNull(1000L) {
            navController.currentBackStackEntryFlow
                .map { it.destination.route ?: "unknown" }
                .first { route -> route.isNotBlank() && route != routeBeforeNavigation }
        } ?: navController.currentBackStackEntry?.destination?.route ?: "unknown"

        runCatching {
            FirebaseCrashlytics.getInstance().apply {
                log("notification_destination_opened")
                setCustomKey("notification_destination_source", source)
                setCustomKey("notification_destination_route", openedRoute)
                setCustomKey("notification_destination_deeplink", deepLink ?: notificationIntent?.data?.toString().orEmpty())
            }
        }.onFailure { Timber.w(it, "Failed to log notification destination telemetry") }
    }
    
    // Safety timeout to ensure navigationDetermined is always set.
    // Bug #4 fix: reduced from 800ms → 600ms. The splash is already
    // dismissed on the first frame (see onReady() call above), so this
    // timeout only governs how long the ONBOARDING fallback stays on
    // screen before we accept that Firestore/DataStore is truly stuck.
    LaunchedEffect(Unit) {
        delay(600)
        if (!navigationDetermined) {
            Timber.w("MainNavGraph - Timeout reached, forcing navigationDetermined = true")
            navigationDetermined = true
            runCatching { onReady() }
        }
    }

    LaunchedEffect(navigationDetermined, isLoading, startDestination) {
        if (!navigationDetermined || isLoading) return@LaunchedEffect
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        val correctableRoute = currentRoute in setOf(
            Routes.PROFILE_SETUP,
            Routes.EMPLOYER_PROFILE_SETUP,
            Routes.ONBOARDING,
            Routes.SELECT_ROLE
        )
        if (correctableRoute && currentRoute != startDestination) {
            Timber.i("MainNavGraph - Correcting cached start route from $currentRoute to $startDestination")
            try {
                navController.navigate(startDestination) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
                Timber.d("MainNavGraph - Successfully navigated to $startDestination from cached route $currentRoute")
            } catch (e: Exception) {
                Timber.w(e, "MainNavGraph - Cached start route correction failed")
            }
        }
    }
    
    // Handle startup deep links + notification clicks after NavHost is ready.
    LaunchedEffect(notificationData, notificationIntent, navigationDetermined, isLoading) {
        // Only proceed if NavHost is ready (navigationDetermined), not loading, and we have notification data
        if (notificationIntent != null && navigationDetermined && !isLoading) {
            val startupDeepLinkUri = notificationIntent.data

            // Extract navigation data from intent
            val navigateTo = notificationIntent.getStringExtra("navigate_to")
            val notificationAction = notificationIntent.getStringExtra("notification_action")
            val jobId = notificationIntent.getStringExtra("job_id")
            val applicationId = notificationIntent.getStringExtra("application_id")
            val notificationId = notificationIntent.getStringExtra("notificationId")
            val directToLogin = notificationIntent.getBooleanExtra("direct_to_login", false)
            val loginRoleExtra = notificationIntent.getStringExtra("login_role")
            
            // Only process if there's actual notification data (not just a regular app launch)
            val hasNotificationData = startupDeepLinkUri != null ||
                                      navigateTo != null || notificationAction != null || 
                                      jobId != null || applicationId != null || notificationId != null ||
                                      directToLogin
            
            if (!hasNotificationData) {
                // No notification data - this is a regular app launch, skip notification handling
                return@LaunchedEffect
            }
            
            Timber.i("MainNavGraph - Notification clicked with intent")
            
            // Small delay to ensure NavHost is fully initialized
            delay(100)

            if (startupDeepLinkUri != null) {
                Timber.i("MainNavGraph - Handling startup deep link: $startupDeepLinkUri")

                val handledByNavController = runCatching {
                    navController.handleDeepLink(notificationIntent)
                }.onFailure { error ->
                    Timber.e(error, "MainNavGraph - NavController.handleDeepLink failed, trying fallback handler")
                }.getOrDefault(false)

                if (!handledByNavController) {
                    Timber.i("MainNavGraph - Deep link not matched by nav graph, using DeepLinkHandler fallback")
                    com.example.dutype.utils.DeepLinkHandler.handleDeepLink(notificationIntent, navController)
                }
                logPendingNotificationDestinationOpened(
                    source = "cold_start_deep_link",
                    deepLink = startupDeepLinkUri.toString()
                )
                return@LaunchedEffect
            }
            
            Timber.i("MainNavGraph - Navigation data: navigateTo=$navigateTo, action=$notificationAction, jobId=$jobId, applicationId=$applicationId")

            if (directToLogin) {
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    val resolvedRole = when (loginRoleExtra?.uppercase()) {
                        "EMPLOYER" -> "EMPLOYER"
                        "WORKER" -> "WORKER"
                        else -> when (profileCompletionViewModel.getUserRole()) {
                            com.example.dutype.models.UserRole.EMPLOYER -> "EMPLOYER"
                            else -> "WORKER"
                        }
                    }

                    navController.navigate("${Routes.ENHANCED_LOGIN}?role=$resolvedRole") {
                        launchSingleTop = true
                    }
                    logPendingNotificationDestinationOpened(source = "cold_start_direct_login")
                }
                return@LaunchedEffect
            }
            
            // Check if user is authenticated
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val userRole = profileCompletionViewModel.getUserRole()
                
                // Navigate to specific screen if provided
                if (navigateTo != null) {
                    try {
                        Timber.i("MainNavGraph - Navigating to specific screen: $navigateTo")
                        navController.navigate(navigateTo) {
                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                            launchSingleTop = true
                        }
                        logPendingNotificationDestinationOpened(source = "cold_start_navigate_to")
                    } catch (e: Exception) {
                        Timber.e(e, "Error navigating to specific screen")
                    }
                } else {
                    // For logged-in users clicking notifications, go to home (NOT profile setup)
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (currentUser != null && userRole != null) {
                        try {
                            if (userRole == com.example.dutype.models.UserRole.EMPLOYER) {
                                Timber.i("MainNavGraph - Logged-in user notification: Navigating to EMPLOYER_HOME")
                                navController.navigate(Routes.EMPLOYER_HOME) {
                                    popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else if (userRole == com.example.dutype.models.UserRole.WORKER) {
                                Timber.i("MainNavGraph - Logged-in user notification: Navigating to WORKER_HOME")
                                navController.navigate(Routes.WORKER_HOME) {
                                    popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error navigating to home from notification")
                        }
                        logPendingNotificationDestinationOpened(source = "cold_start_notification_route")
                    }
                }
            }
        } else if (notificationData != null && navigationDetermined && !isLoading) {
            Timber.i("MainNavGraph - Legacy notification clicked: $notificationData")
            
            // Add a small delay to ensure NavHost is fully initialized
            delay(200)
            
            // Check if user is authenticated
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val userRole = profileCompletionViewModel.getUserRole()
                
                if (userRole != null) {
                    // For logged-in users with legacy notifications, go directly to home (NOT profile setup)
                    try {
                        if (userRole == com.example.dutype.models.UserRole.EMPLOYER) {
                            Timber.i("MainNavGraph - Legacy notification: Logged-in employer navigating to EMPLOYER_HOME")
                            navController.navigate(Routes.EMPLOYER_HOME) {
                                popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else if (userRole == com.example.dutype.models.UserRole.WORKER) {
                            Timber.i("MainNavGraph - Legacy notification: Logged-in worker navigating to WORKER_HOME")
                            navController.navigate(Routes.WORKER_HOME) {
                                popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error navigating to home from legacy notification")
                    }
                }
            }
        }
    }
    
    // IMPORTANT: Only render NavHost AFTER navigation destination is determined
    // This prevents rendering with the wrong start destination
    if (navigationDetermined) {
        Timber.d("MainNavGraph - Rendering NavHost with startDestination: $startDestination (navigationDetermined: $navigationDetermined)")
        
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(navController)
        }
        composable(
            route = "${Routes.ENHANCED_LOGIN}?role={role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType; defaultValue = "WORKER" })
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "WORKER"
            Timber.d("Enhanced login route accessed with role: $role")
            EnhancedLoginScreen(
                navController = navController,
                skipRoleSelection = true,
                initialRole = role,
                isRegisterMode = false
            )
        }
        composable(
            route = "${Routes.REGISTER}?role={role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType; defaultValue = "WORKER" })
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "WORKER"
            Timber.d("Register route accessed with role: $role")
            RegisterScreen(
                navController = navController,
                initialRole = role
            )
        }
        composable(Routes.SELECT_ROLE) {
            RoleSelectionWithNavigation(
                navController = navController,
                profileCompletionViewModel = profileCompletionViewModel
            )
        }
        composable(Routes.TERMS_OF_SERVICE) {
            TermsOfServiceScreen(navController)
        }
        composable(Routes.PRIVACY_POLICY) {
            PrivacyPolicyScreen(navController)
        }
        composable(Routes.WORKER_HOME) {
            WorkerMainScreen(
                rootNavController = navController,
                onStatusBarColorChange = onStatusBarColorChange,
                notificationPermissionManager = notificationPermissionManager
            )
        }

        // Root-level job detail destination for app-link/deep-link handling.
        // Deep links are processed on MainNavGraph's navController, so this route
        // must exist here in addition to WorkerNavGraph.
        composable(
            route = Routes.JOB_DETAIL,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType }),
            deepLinks = listOf(
                androidx.navigation.navDeepLink {
                    uriPattern = "dutype://job/{jobId}"
                },
                androidx.navigation.navDeepLink {
                    uriPattern = "https://dutype.in/jobs/{jobId}"
                }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            Timber.i("🔗 MainNavGraph: JobDescriptionScreen opened with jobId: $jobId")
            val firestoreJobViewModel: com.example.dutype.viewmodels.FirestoreJobViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            com.example.dutype.worker.screens.JobDescriptionScreen(
                jobId = jobId,
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }

        composable(Routes.EMPLOYER_HOME) {
            EmployerMainScreen(
                rootNavController = navController,
                notificationPermissionManager = notificationPermissionManager
            )
        }
        composable(Routes.EMPLOYER_PROFILE_SETUP) {
            MandatoryEmployerProfileSetupScreen(navController = navController)
        }
        
        // Employer profile setup with return route (for job posting flow)
        composable(
            route = "${Routes.EMPLOYER_PROFILE_SETUP}?returnRoute={returnRoute}",
            arguments = listOf(
                androidx.navigation.navArgument("returnRoute") {
                    type = androidx.navigation.NavType.StringType
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
        
        composable(Routes.PROFILE_SETUP) {
            MandatoryWorkerProfileSetupScreen(navController = navController)
        }
        
        // Profile setup with return route (for job application flow)
        composable(
            route = "profile_setup?returnRoute={returnRoute}",
            arguments = listOf(
                androidx.navigation.navArgument("returnRoute") {
                    type = androidx.navigation.NavType.StringType
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
        
        // Missing employer routes - add placeholder screens
        composable(Routes.EMPLOYER_PROFILE) {
            // Placeholder for employer profile
            EmployerProfileScreen(navController)
        }
        composable(Routes.EMPLOYER_COMPANY_DETAILS) {
            EmployerCompanyDetailsScreen(navController = navController)
        }
        composable(Routes.EMPLOYER_SUBSCRIPTION) {
            EmployerSubscriptionScreen(navController = navController)
        }
        composable(Routes.WORKER_PROFILE_DETAILS) {
            val context = LocalContext.current
            val dataStore = remember { com.example.dutype.data.ApplicationFormDataStore(context) }
            com.example.dutype.worker.screens.profile.WorkerProfileDetailsScreen(
                navController = navController,
                dataStore = dataStore
            )
        }
        composable(
            route = Routes.WORKER_PROFILE_VIEW,
            arguments = listOf(
                navArgument("workerId") { type = NavType.StringType },
                navArgument("applicationId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val workerId = backStackEntry.arguments?.getString("workerId") ?: ""
            val applicationId = backStackEntry.arguments?.getString("applicationId")
            ProfessionalWorkerProfileViewScreen(
                navController = navController,
                workerId = workerId,
                applicationId = applicationId
            )
        }
        composable(
            route = Routes.EMPLOYER_PROFILE_VIEW,
            arguments = listOf(navArgument("employerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val employerId = backStackEntry.arguments?.getString("employerId") ?: ""
            EmployerPublicProfileScreen(
                navController = navController,
                employerId = employerId,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
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

        composable(Routes.EMPLOYER_APPLICATIONS) {
            EmployerApplicationManagementScreen(
                jobId = null,
                onApplicationClick = { application ->
                    // Navigate to detailed application view
                    navController.navigate(Routes.workerProfileViewRoute(application.workerId, application.id))
                },
                onBackClick = { navController.popBackStack() },
                onSubscribeClick = {
                    navController.navigate(Routes.EMPLOYER_SUBSCRIPTION)
                }
            )
        }
        
        
        composable(
            route = Routes.EMPLOYER_APPLICATIONS_JOB,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            EmployerApplicationManagementScreen(
                jobId = jobId,
                onApplicationClick = { application ->
                    navController.navigate(Routes.workerProfileViewRoute(application.workerId, application.id))
                },
                onBackClick = { navController.popBackStack() },
                onSubscribeClick = {
                    navController.navigate(Routes.EMPLOYER_SUBSCRIPTION)
                }
            )
        }
        
       
        composable(Routes.ANALYTICS) {
            // Analytics screen
            AnalyticsScreen(navController)
        }
        
        // Contact Us Screen
        composable(Routes.CONTACT_US) {
            com.example.dutype.common.screens.support.ContactUsScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
    }
    } else {
        // Navigation destination is being determined - show minimal loading
        Timber.d("MainNavGraph - Navigation not yet determined, showing loading state...")
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(com.example.dutype.ui.theme.WorkerColors.CardBackground),
            contentAlignment = Alignment.Center
        ) {
            // Blank white screen - NavHost will be shown immediately once destination is determined
            // No loading indicator - should be instant
        }
    }
}

@Composable
fun RoleSelectionWithNavigation(
    navController: NavHostController,
    profileCompletionViewModel: com.example.dutype.viewmodels.ProfileCompletionViewModel
) {
    var selectedRole by remember { mutableStateOf<String?>(null) }
    
    // Handle navigation when role is selected
    LaunchedEffect(selectedRole) {
        selectedRole?.let { role ->
            Timber.d("Role selected: $role")
            
            // Check if user is already signed in
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // User is already signed in, check profile completion and navigate accordingly
                val userRole = when (role) {
                    "WORKER" -> com.example.dutype.models.UserRole.WORKER
                    "EMPLOYER" -> com.example.dutype.models.UserRole.EMPLOYER
                    else -> com.example.dutype.models.UserRole.WORKER
                }
                
                    try {
                        // Check if user has existing profile in Firebase using high-level approach
                        val userEmail = currentUser.email ?: ""
                        val hasExistingProfile = profileCompletionViewModel.checkExistingProfileHighLevel(userEmail, userRole)
                        Timber.d("RoleSelectionWithNavigation - Has existing profile (high-level): $hasExistingProfile")
                        
                        if (hasExistingProfile) {
                            Timber.i("Found existing profile, loading data and navigating to home...")
                            
                            // Load existing profile data into local state
                            profileCompletionViewModel.loadExistingProfileData()
                            
                            // Navigate directly to home screen
                            when (userRole) {
                                com.example.dutype.models.UserRole.WORKER -> {
                                    Timber.i("Existing user - Worker navigates to WORKER_HOME")
                                    navController.navigate(Routes.WORKER_HOME) {
                                        popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                                com.example.dutype.models.UserRole.EMPLOYER -> {
                                    Timber.i("Existing user - Navigating to EMPLOYER_HOME")
                                    navController.navigate(Routes.EMPLOYER_HOME) {
                                        popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                                else -> {
                                    Timber.i("Fallback - Navigating to WORKER_HOME")
                                    navController.navigate(Routes.WORKER_HOME) {
                                        popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        } else {
                            // Check local state for profile completion
                            val isProfileComplete = profileCompletionViewModel.isProfileComplete(userRole)
                            Timber.d("Local profile complete: $isProfileComplete")
                            
                            if (isProfileComplete) {
                                // Profile is complete locally, navigate directly to home screen
                                when (userRole) {
                                    com.example.dutype.models.UserRole.WORKER -> {
                                        Timber.i("Local profile complete - Worker navigates to WORKER_HOME")
                                        navController.navigate(Routes.WORKER_HOME) {
                                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                    com.example.dutype.models.UserRole.EMPLOYER -> {
                                        Timber.i("Local profile complete - Navigating to EMPLOYER_HOME")
                                        navController.navigate(Routes.EMPLOYER_HOME) {
                                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                    else -> {
                                        Timber.i("Fallback - Navigating to WORKER_HOME")
                                        navController.navigate(Routes.WORKER_HOME) {
                                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            } else {
                                // Profile not complete, navigate to profile setup
                                when (userRole) {
                                    com.example.dutype.models.UserRole.WORKER -> {
                                        Timber.i("Profile incomplete - Navigating to PROFILE_SETUP")
                                        navController.navigate(Routes.PROFILE_SETUP) {
                                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                    com.example.dutype.models.UserRole.EMPLOYER -> {
                                        Timber.i("Profile incomplete - Navigating to EMPLOYER_PROFILE_SETUP")
                                        navController.navigate(Routes.EMPLOYER_PROFILE_SETUP) {
                                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                    else -> {
                                        Timber.i("Fallback - Navigating to PROFILE_SETUP")
                                        navController.navigate(Routes.PROFILE_SETUP) {
                                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            }
                        }
                } catch (e: Exception) {
                    // If there's an error, fall back to profile setup
                    Timber.e(e, "Error checking profile status")
                    when (userRole) {
                        com.example.dutype.models.UserRole.WORKER -> {
                            Timber.i("Fallback: Navigating to PROFILE_SETUP")
                            navController.navigate(Routes.PROFILE_SETUP) {
                                popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        com.example.dutype.models.UserRole.EMPLOYER -> {
                            Timber.i("Fallback: Navigating to EMPLOYER_PROFILE_SETUP")
                            navController.navigate(Routes.EMPLOYER_PROFILE_SETUP) {
                                popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        else -> {
                            Timber.i("Fallback: Navigating to PROFILE_SETUP")
                            navController.navigate(Routes.PROFILE_SETUP) {
                                popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            } else {
                // Mandatory login: guests are not allowed into Home. Any role
                // selection without a signed-in user is routed to the login screen.
                // (This branch is normally unreachable because the onRoleSelected
                // callback below redirects unsigned users straight to login, but we
                // keep it consistent so no code path can leak a guest into Home.)
                val roleArg = if (role == "EMPLOYER") "EMPLOYER" else "WORKER"
                Timber.i("Mandatory login - unsigned role selection routed to ENHANCED_LOGIN ($roleArg)")
                navController.navigate("${Routes.ENHANCED_LOGIN}?role=$roleArg") {
                    launchSingleTop = true
                }
            }
            
            // Reset selected role
            selectedRole = null
        }
    }
    
    // Show the role selection screen
    SelectRoleScreen(
        navController = navController,
        onRoleSelected = { role ->
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                // Mandatory login: a guest must sign in before entering the app.
                // Keep SELECT_ROLE on the back stack so "back" returns to role choice.
                val roleArg = if (role == "EMPLOYER") "EMPLOYER" else "WORKER"
                Timber.i("Mandatory login - routing unsigned $roleArg to ENHANCED_LOGIN")
                navController.navigate("${Routes.ENHANCED_LOGIN}?role=$roleArg") {
                    launchSingleTop = true
                }
            } else {
                selectedRole = role
            }
        }
    )
}



