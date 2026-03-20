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
import com.example.dutype.employer.screens.applications.ApplicationDetailScreen
import com.example.dutype.employer.screens.applications.EmployerApplicationManagementScreen
import kotlinx.coroutines.tasks.await
import com.example.dutype.employer.screens.EditJobScreen
import com.example.dutype.employer.screens.EmployerCompanyDetailsScreen
import com.example.dutype.employer.screens.profilescreen.EmployerProfileScreen
import com.example.dutype.location.ManualLocationScreen
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.EmployerMainScreen
import com.example.dutype.navigation.WorkerMainScreen
import com.example.dutype.onboarding.OnboardingScreen
import com.example.dutype.worker.screens.MandatoryWorkerProfileSetupScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun MainNavGraph(
    navController: NavHostController,
    onStatusBarColorChange: (Color) -> Unit = {},
    notificationData: String? = null,
    notificationPermissionManager: com.example.dutype.utils.NotificationPermissionManager,
    notificationIntent: android.content.Intent? = null
) {
    val context = LocalContext.current
    val profileCompletionViewModel: com.example.dutype.viewmodels.ProfileCompletionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    
    // DEEP LINK FIX: Listen for deep link broadcasts from MainActivity.onNewIntent()
    LaunchedEffect(Unit) {
        val broadcastReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                val deepLinkUri = intent?.data
                if (deepLinkUri != null) {
                    Timber.i("📱 MainNavGraph: Received deep link broadcast: $deepLinkUri")
                    // Handle deep link using DeepLinkHandler
                    com.example.dutype.utils.DeepLinkHandler.handleDeepLinkUri(deepLinkUri, navController)
                }
            }
        }
        
        val filter = android.content.IntentFilter("com.example.dutype.DEEP_LINK")
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context)
            .registerReceiver(broadcastReceiver, filter)
        
        Timber.d("📱 MainNavGraph: Deep link broadcast receiver registered")
        
        // Cleanup on dispose - use try-finally to ensure unregister
        try {
            kotlinx.coroutines.awaitCancellation()
        } finally {
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context)
                .unregisterReceiver(broadcastReceiver)
            Timber.d("📱 MainNavGraph: Deep link broadcast receiver unregistered")
        }
    }
    
    // State management for determining start destination
    var isLoading by remember { mutableStateOf(true) }
    var startDestination by remember { mutableStateOf(Routes.ONBOARDING) } // Start with onboarding or role selection
    var navigationDetermined by remember { mutableStateOf(false) }
    
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
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(currentUser.uid)
                                .get()
                                .await()
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "ðŸš€ MainNavGraph - Error reading users doc")
                        null
                    }

                    // CRITICAL FIX: Try DataStore first, fallback to Firestore if needed
                    var userRole = profileCompletionViewModel.getUserRole()
                    Timber.d("🚀 MainNavGraph - DataStore userRole: $userRole")
                    
                    // FALLBACK: If DataStore is null or returns WORKER by default, check Firestore
                    // This handles cases where DataStore might not be synced yet
                    if (userRole == null) {
                        Timber.w("🚀 MainNavGraph - DataStore returned null, checking Firestore with timeout...")
                        try {
                            val activeRoleStr = userDoc?.getString("activeRole")
                            Timber.d("🚀 MainNavGraph - Firestore activeRole: $activeRoleStr")
                            
                            if (activeRoleStr != null) {
                                userRole = try {
                                    com.example.dutype.models.UserRole.valueOf(activeRoleStr.uppercase())
                                } catch (e: Exception) {
                                    Timber.e(e, "🚀 MainNavGraph - Invalid role in Firestore: $activeRoleStr")
                                    null
                                }
                                
                                // Update DataStore with Firestore value for next time
                                if (userRole != null) {
                                    profileCompletionViewModel.updateUserRole(userRole)
                                    Timber.d("🚀 MainNavGraph - Updated DataStore with Firestore role: $userRole")
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "🚀 MainNavGraph - Error reading from Firestore")
                        }
                    }
                    
                    if (userRole != null) {
                        val isProfileComplete = userDoc?.exists() == true &&
                            profileCompletionViewModel.isProfileComplete(userRole)
                        Timber.d("🚀 MainNavGraph - Profile complete for $userRole: $isProfileComplete")
                        
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
            
            // Set states immediately for instant navigation
            isLoading = false
            navigationDetermined = true
            Timber.d("🚀 MainNavGraph - Navigation completed, startDestination: $startDestination")
            
        } catch (e: Exception) {
            Timber.e(e, "🚀 MainNavGraph - Error determining start destination")
            // Fallback to onboarding
            startDestination = Routes.ONBOARDING
            isLoading = false
            navigationDetermined = true
            Timber.d("🚀 MainNavGraph - Error fallback - startDestination: $startDestination")
        }
    }
    
    // Safety timeout to ensure navigationDetermined is always set
    LaunchedEffect(Unit) {
        delay(3000) // P0 FIX: Increased to 3s — generous fallback if Firestore is slow
        if (!navigationDetermined) {
            Timber.w("MainNavGraph - Timeout reached, forcing navigationDetermined = true")
            navigationDetermined = true
        }
    }
    
    // Handle notification clicks - only after NavHost is ready and navigation is determined
    LaunchedEffect(notificationData, notificationIntent, navigationDetermined, isLoading) {
        // Only proceed if NavHost is ready (navigationDetermined), not loading, and we have notification data
        if (notificationIntent != null && navigationDetermined && !isLoading) {
            // Extract navigation data from intent
            val navigateTo = notificationIntent.getStringExtra("navigate_to")
            val notificationAction = notificationIntent.getStringExtra("notification_action")
            val jobId = notificationIntent.getStringExtra("job_id")
            val applicationId = notificationIntent.getStringExtra("application_id")
            val notificationId = notificationIntent.getStringExtra("notificationId")
            val directToLogin = notificationIntent.getBooleanExtra("direct_to_login", false)
            val loginRoleExtra = notificationIntent.getStringExtra("login_role")
            
            // Only process if there's actual notification data (not just a regular app launch)
            val hasNotificationData = navigateTo != null || notificationAction != null || 
                                      jobId != null || applicationId != null || notificationId != null ||
                                      directToLogin
            
            if (!hasNotificationData) {
                // No notification data - this is a regular app launch, skip notification handling
                return@LaunchedEffect
            }
            
            Timber.i("MainNavGraph - Notification clicked with intent")
            
            // Small delay to ensure NavHost is fully initialized
            delay(100)
            
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
                jobId = jobId,
                onApplicationClick = { application ->
                    // Navigate to detailed application view
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
            val employerViewModel: com.example.dutype.viewmodels.EmployerApplicationViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val context = androidx.compose.ui.platform.LocalContext.current
            
            ApplicationDetailScreen(
                applicationId = applicationId,
                onBackClick = { navController.popBackStack() },
                onUpdateStatus = { newStatus, notes ->
                    // For ACCEPTED status, use hireApplicant which checks vacancy limits
                    if (newStatus == com.example.dutype.models.ApplicationStatus.ACCEPTED) {
                        val application = employerViewModel.uiState.value.applications.find { it.applicationId == applicationId }
                        if (application != null) {
                            employerViewModel.hireApplicant(
                                applicationId = applicationId,
                                jobId = application.id,
                                onSuccess = {
                                    android.widget.Toast.makeText(context, "Applicant hired successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                },
                                onError = { error ->
                                    android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
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
        
       
        composable(Routes.COMPANY_DETAILS) {
            // Use EmployerCompanyDetailsScreen instead of deleted CompanyDetailsScreen
            EmployerCompanyDetailsScreen(navController)
        }
        composable(Routes.ANALYTICS) {
            // Analytics screen
            AnalyticsScreen(navController)
        }
        
        // Language Selection - Now handled via bottom sheet in profile screens
        // Route kept for backward compatibility but redirects to profile
        composable(Routes.LANGUAGE_SELECTION) {
            // Navigate back - language selection is now a bottom sheet
            androidx.compose.runtime.LaunchedEffect(Unit) {
                navController.popBackStack()
            }
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
                            profileCompletionViewModel.loadExistingProfileData(userEmail, userRole)
                            
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



