package com.example.dutype.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.dutype.employer.screens.AnalyticsScreen
import com.example.dutype.employer.screens.MandatoryEmployerProfileSetupScreen
import com.example.dutype.employer.screens.applications.EmployerApplicationManagementScreen
import kotlinx.coroutines.tasks.await
import com.example.dutype.employer.screens.EditJobScreen
import com.example.dutype.employer.screens.EmployerCompanyDetailsScreen
import com.example.dutype.employer.screens.EmployerPublicProfileScreen
import com.example.dutype.employer.screens.profilescreen.EmployerProfileScreen
import com.example.dutype.location.ManualLocationScreen
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.EmployerMainScreen
import com.example.dutype.navigation.WorkerMainScreen
import com.example.dutype.onboarding.OnboardingScreen
import com.example.dutype.worker.screens.MandatoryWorkerProfileSetupScreen
import com.example.dutype.employer.screens.ProfessionalWorkerProfileViewScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
            com.example.dutype.utils.DeepLinkHandler.handleDeepLinkUri(uri, navController)
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
    
    // State management for determining start destination.
    //
    // P1-7: Seed `startDestination` from a synchronous on-disk cache so the
    // NavHost is built with the correct route on the very first frame after a
    // cold start. The async resolver below still runs to reconcile against
    // DataStore + Firestore and corrects the route if reality differs.
    //
    // PERF: Ignore auth-gated cached routes (worker_home / employer_home) when
    // FirebaseAuth has no current user. Otherwise a previously signed-in launch
    // poisons the cache, the NavHost renders worker_home, kicks off Firestore +
    // announcement queries, then re-renders to select_role — causing a frame
    // skip and a wasted Firestore read on every guest cold start.
    val cachedStartDestination = remember {
        val cached = StartDestinationCache.read(context)
        val isAuthGated = cached == Routes.WORKER_HOME || cached == Routes.EMPLOYER_HOME
        if (isAuthGated && com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
            null
        } else {
            cached
        }
    }
    var isLoading by remember { mutableStateOf(cachedStartDestination == null) }
    var startDestination by remember {
        mutableStateOf(cachedStartDestination ?: Routes.ONBOARDING)
    }
    var navigationDetermined by remember { mutableStateOf(cachedStartDestination != null) }

    // Bug #9 / #4 fix: Always dismiss the system splash on the very first
    // composition. The NavHost is already built with either the cached
    // start destination (warm launch) or ONBOARDING (fresh install). The
    // async resolver below runs in parallel and will redirect if needed,
    // but the user never sees a frozen launcher-icon splash — they see
    // real UI on frame 1. This was the dominant cause of the "splash
    // takes too long" complaint, because even a 300ms Firestore handshake
    // on a cold-booted device showed as an eternity of dead splash.
    LaunchedEffect(Unit) {
        runCatching { onReady() }
    }
    
    LaunchedEffect(Unit) {
        try {
            Timber.d("🚀 MainNavGraph - Starting navigation logic...")
            
            // Check if onboarding has been completed
            val hasCompletedOnboarding = profileCompletionViewModel.hasOnboardingBeenCompleted()
            Timber.d("🚀 MainNavGraph - hasCompletedOnboarding: $hasCompletedOnboarding")
            
            // Check if user is authenticated
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            Timber.d("🚀 MainNavGraph - currentUser: ${currentUser?.uid}")
            
            // Determine start destination based on user state
            startDestination = when {
                !hasCompletedOnboarding -> {
                    Timber.d("🚀 MainNavGraph - Onboarding not completed, navigating to ONBOARDING")
                    Routes.ONBOARDING
                }
                currentUser == null -> {
                    Timber.d("🚀 MainNavGraph - No user authenticated, navigating to SELECT_ROLE")
                    Routes.SELECT_ROLE
                }
                else -> {
                    // User is authenticated, check their role and profile completion
                    Timber.d("🚀 MainNavGraph - User authenticated, checking role from DataStore...")
                    
                    val userDoc = try {
                        kotlinx.coroutines.withTimeoutOrNull(2000L) {
                            com.example.dutype.di.firestoreFromHilt(context)
                                .collection(com.example.dutype.firestore.FirestoreCollections.USERS)
                                .document(currentUser.uid)
                                .get()
                                .await()
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "ðŸš€ MainNavGraph - Error reading users doc")
                        null
                    }

                    // Single-role architecture: trust the Firestore document as the
                    // source of truth for the user's role. Falls back to DataStore for
                    // pre-network UX, but Firestore wins on conflict.
                    var userRole: com.example.dutype.models.UserRole? = null
                    val firestoreRoleStr = userDoc?.getString("role")
                        ?: userDoc?.getString("activeRole")
                        ?: (userDoc?.get("roles") as? List<*>)?.firstOrNull()?.toString()
                    if (firestoreRoleStr != null) {
                        userRole = runCatching {
                            com.example.dutype.models.UserRole.valueOf(firestoreRoleStr.uppercase())
                        }.getOrElse {
                            Timber.e(it, "🚀 MainNavGraph - Invalid role in Firestore: $firestoreRoleStr")
                            null
                        }
                        // Mirror to DataStore so pre-network UX is correct on next launch.
                        if (userRole != null) {
                            try {
                                profileCompletionViewModel.updateUserRole(userRole)
                            } catch (e: Exception) {
                                Timber.w(e, "🚀 MainNavGraph - Failed to sync role to DataStore")
                            }
                        }
                    }
                    if (userRole == null) {
                        userRole = profileCompletionViewModel.getUserRole()
                        Timber.d("🚀 MainNavGraph - Falling back to DataStore userRole: $userRole")
                    }
                    
                    if (userRole != null) {
                        val localProfileComplete = userDoc?.exists() == true &&
                            profileCompletionViewModel.isProfileComplete(userRole)

                        val firestoreProfileComplete = if (!localProfileComplete) {
                            runCatching {
                                profileCompletionViewModel.checkExistingProfileHighLevel(
                                    email = currentUser.email ?: "",
                                    role = userRole
                                )
                            }.getOrElse {
                                Timber.e(it, "🚀 MainNavGraph - Firestore profile completion fallback failed")
                                false
                            }
                        } else {
                            true
                        }

                        val isProfileComplete = localProfileComplete || firestoreProfileComplete

                        if (isProfileComplete && !localProfileComplete) {
                            runCatching {
                                profileCompletionViewModel.markProfileComplete(userRole)
                                profileCompletionViewModel.markProfileSetupAsShown(userRole)
                                Timber.d("🚀 MainNavGraph - Synced local profile completion from Firestore for $userRole")
                            }.onFailure {
                                Timber.e(it, "🚀 MainNavGraph - Failed syncing local profile completion state")
                            }
                        }

                        Timber.d("🚀 MainNavGraph - Profile complete for $userRole: $isProfileComplete (local=$localProfileComplete, firestore=$firestoreProfileComplete)")
                        
                        when {
                            isProfileComplete && userRole == com.example.dutype.models.UserRole.WORKER -> {
                                Timber.d("🚀 MainNavGraph - ✅ Worker profile complete, navigating directly to WORKER_HOME")
                                Routes.WORKER_HOME
                            }
                            isProfileComplete && userRole == com.example.dutype.models.UserRole.EMPLOYER -> {
                                Timber.d("🚀 MainNavGraph - ✅ Employer profile complete, navigating to EMPLOYER_HOME")
                                Routes.EMPLOYER_HOME
                            }
                            userRole == com.example.dutype.models.UserRole.WORKER -> {
                                Timber.d("🚀 MainNavGraph - Profile incomplete, navigating to PROFILE_SETUP")
                                Routes.PROFILE_SETUP
                            }
                            userRole == com.example.dutype.models.UserRole.EMPLOYER -> {
                                Timber.d("🚀 MainNavGraph - Profile incomplete, navigating to EMPLOYER_PROFILE_SETUP")
                                Routes.EMPLOYER_PROFILE_SETUP
                            }
                            else -> {
                                Timber.w("🚀 MainNavGraph - Unknown role state, navigating to SELECT_ROLE")
                                Routes.SELECT_ROLE
                            }
                        }
                    } else {
                        Timber.w("🚀 MainNavGraph - No role found in DataStore or Firestore, navigating to SELECT_ROLE")
                        Routes.SELECT_ROLE
                    }
                }
            }
            
            Timber.d("🚀 MainNavGraph - Final startDestination: $startDestination")

            // P1-7: persist the resolved route so the next cold start can
            // skip the loading state and draw the right screen instantly.
            runCatching { StartDestinationCache.save(context, startDestination) }

            // Set states immediately for instant navigation
            isLoading = false
            navigationDetermined = true
            // Signal MainActivity to dismiss the system splash \u2014 nav is ready.
            runCatching { onReady() }
            Timber.d("🚀 MainNavGraph - Navigation completed, startDestination: $startDestination")
            
        } catch (e: Exception) {
            Timber.e(e, "🚀 MainNavGraph - Error determining start destination")
            // Fallback to onboarding
            startDestination = Routes.ONBOARDING
            isLoading = false
            navigationDetermined = true
            runCatching { onReady() }
            Timber.d("🚀 MainNavGraph - Error fallback - startDestination: $startDestination")
        }
    }
    
    // Safety timeout to ensure navigationDetermined is always set.
    // Bug #4 fix: reduced from 1500ms → 800ms. The splash is already
    // dismissed on the first frame (see onReady() call above), so this
    // timeout only governs how long the ONBOARDING fallback stays on
    // screen before we accept that Firestore/DataStore is truly stuck.
    LaunchedEffect(Unit) {
        delay(800)
        if (!navigationDetermined) {
            Timber.w("MainNavGraph - Timeout reached, forcing navigationDetermined = true")
            navigationDetermined = true
            runCatching { onReady() }
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
                    } catch (e: Exception) {
                        Timber.e(e, "Error navigating to specific screen")
                    }
                } else {
                    // Fallback to home screen based on user role - but first check profile completion
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (currentUser != null && userRole != null) {
                        // CHECK LOCAL DATASTORE FIRST - same as main navigation logic
                        val isProfileCompleteLocal = profileCompletionViewModel.isProfileComplete(userRole)
                        Timber.d("MainNavGraph - Notification handler profile complete check (LOCAL DataStore): $isProfileCompleteLocal")
                        
                        if (isProfileCompleteLocal) {
                            try {
                                if (userRole == com.example.dutype.models.UserRole.EMPLOYER) {
                                    Timber.i("MainNavGraph - Navigating to EMPLOYER_HOME from notification")
                                    navController.navigate(Routes.EMPLOYER_HOME) {
                                        popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } else if (userRole == com.example.dutype.models.UserRole.WORKER) {
                                    Timber.i("MainNavGraph - Worker navigating to WORKER_HOME from notification")
                                    navController.navigate(Routes.WORKER_HOME) {
                                        popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Error navigating to home from notification")
                            }
                        } else {
                            // Profile incomplete - navigate to appropriate onboarding
                            try {
                                if (userRole == com.example.dutype.models.UserRole.EMPLOYER) {
                                    Timber.i("MainNavGraph - Profile incomplete, navigating to EMPLOYER_PROFILE_SETUP from notification")
                                    navController.navigate(Routes.EMPLOYER_PROFILE_SETUP) {
                                        popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } else if (userRole == com.example.dutype.models.UserRole.WORKER) {
                                    Timber.i("MainNavGraph - Profile incomplete, navigating to PROFILE_SETUP from notification")
                                    navController.navigate(Routes.PROFILE_SETUP) {
                                        popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Error navigating to onboarding from notification")
                            }
                        }
                    }
                }
            }
        } else if (notificationData != null && navigationDetermined && !isLoading) {
            Timber.i("MainNavGraph - Legacy notification clicked: $notificationData")
            
            // Add a small delay to ensure NavHost is fully initialized
            delay(200)
            
            // Check if user is authenticated and profile complete
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val userRole = profileCompletionViewModel.getUserRole()
                
                if (userRole != null) {
                    // CHECK LOCAL DATASTORE FIRST - same as main navigation logic
                    val isProfileCompleteLocal = profileCompletionViewModel.isProfileComplete(userRole)
                    Timber.d("MainNavGraph - Legacy notification profile complete check (LOCAL DataStore): $isProfileCompleteLocal")
                    
                    if (isProfileCompleteLocal) {
                        // Navigate to home if profile is complete
                        try {
                            if (userRole == com.example.dutype.models.UserRole.EMPLOYER) {
                                Timber.i("MainNavGraph - Navigating to EMPLOYER_HOME from legacy notification")
                                navController.navigate(Routes.EMPLOYER_HOME) {
                                    popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else if (userRole == com.example.dutype.models.UserRole.WORKER) {
                                Timber.i("MainNavGraph - Worker navigates to WORKER_HOME from legacy notification")
                                navController.navigate(Routes.WORKER_HOME) {
                                    popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error navigating to home from legacy notification")
                        }
                    } else {
                        // Profile incomplete - navigate to appropriate onboarding
                        try {
                            if (userRole == com.example.dutype.models.UserRole.EMPLOYER) {
                                Timber.i("MainNavGraph - Legacy notification: Profile incomplete, navigating to EMPLOYER_PROFILE_SETUP")
                                navController.navigate(Routes.EMPLOYER_PROFILE_SETUP) {
                                    popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else if (userRole == com.example.dutype.models.UserRole.WORKER) {
                                Timber.i("MainNavGraph - Legacy notification: Profile incomplete, navigating to PROFILE_SETUP")
                                navController.navigate(Routes.PROFILE_SETUP) {
                                    popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error navigating to onboarding from legacy notification")
                        }
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
        composable(Routes.MANUAL_LOCATION_ROUTE) {
            ManualLocationScreen(navController = navController)
        }
        composable(Routes.SELECT_ROLE) {
            RoleSelectionWithNavigation(
                navController = navController,
                profileCompletionViewModel = profileCompletionViewModel
            )
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
                onStatusBarColorChange = onStatusBarColorChange,
                adManager = firestoreJobViewModel.adManager
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
            arguments = listOf(navArgument("workerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workerId = backStackEntry.arguments?.getString("workerId") ?: ""
            ProfessionalWorkerProfileViewScreen(
                navController = navController,
                workerId = workerId
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
                    navController.navigate(Routes.workerProfileViewRoute(application.workerId))
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
                jobId = jobId,
                onApplicationClick = { application ->
                    navController.navigate(Routes.workerProfileViewRoute(application.workerId))
                },
                onBackClick = { navController.popBackStack() }
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
                .background(Color.White),
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
                // P0 FIX: Secure Guest Mode - Read-only access with login prompts for actions
                // User is not signed in - Guest Mode: Navigate to home with limited access
                Timber.d("User not signed in, navigating to home (Secure Guest Mode - Read Only)")
                val userRole = when (role) {
                    "WORKER" -> com.example.dutype.models.UserRole.WORKER
                    "EMPLOYER" -> com.example.dutype.models.UserRole.EMPLOYER
                    else -> com.example.dutype.models.UserRole.WORKER
                }
                
                // P0 FIX: Guest users can browse but will be prompted to login for actions
                // This is handled in individual screens (apply job, post job, etc.)
                when (userRole) {
                    com.example.dutype.models.UserRole.WORKER -> {
                        Timber.i("Secure Guest Mode - Worker navigates to WORKER_HOME (read-only)")
                        navController.navigate(Routes.WORKER_HOME) {
                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    com.example.dutype.models.UserRole.EMPLOYER -> {
                        Timber.i("Secure Guest Mode - Navigating to EMPLOYER_HOME (read-only)")
                        navController.navigate(Routes.EMPLOYER_HOME) {
                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    else -> {
                        Timber.i("Secure Guest Mode Fallback - Navigating to WORKER_HOME (read-only)")
                        navController.navigate(Routes.WORKER_HOME) {
                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
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
            selectedRole = role
        }
    )
}



