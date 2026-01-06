package com.example.dutype.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.dutype.common.chat.SelectRoleScreen
import com.example.dutype.components.DutyPeSplashScreen
import com.example.dutype.employer.screens.AnalyticsScreen
import com.example.dutype.employer.screens.MandatoryEmployerProfileSetupScreen
import com.example.dutype.employer.screens.applications.ApplicationDetailScreen
import com.example.dutype.employer.screens.applications.EmployerApplicationManagementScreen
import com.example.dutype.employer.screens.EditJobScreen
import com.example.dutype.employer.screens.EmployerCompanyDetailsScreen
import com.example.dutype.employer.screens.profilescreen.EmployerProfileScreen
import com.example.dutype.location.ManualLocationScreen
import com.example.dutype.navigation.EmployerMainScreen
import com.example.dutype.navigation.WorkerMainScreen
import com.example.dutype.onboarding.OnboardingScreen
import com.example.dutype.worker.screens.MandatoryWorkerProfileSetupScreen
import kotlinx.coroutines.delay
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
    
    // State management for determining start destination
    var isLoading by remember { mutableStateOf(true) }
    var startDestination by remember { mutableStateOf(Routes.SELECT_ROLE) }
    var showLoadingIndicator by remember { mutableStateOf(true) } // Start with true to show splash immediately
    var navigationDetermined by remember { mutableStateOf(false) }
    var isFirstTimeUser by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        try {
            Timber.d("MainNavGraph - Starting navigation logic...")
            
            // Show splash immediately for all users
            showLoadingIndicator = true
            
            // Check if onboarding has been completed (not just app opened)
            // This handles the case where app restarts during language selection
            val hasCompletedOnboarding = profileCompletionViewModel.hasOnboardingBeenCompleted()
            Timber.d("MainNavGraph - hasCompletedOnboarding: $hasCompletedOnboarding")
            
            // Check if user is authenticated
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            Timber.d("MainNavGraph - currentUser: ${currentUser?.uid}")
            
            if (!hasCompletedOnboarding) {
                // User hasn't completed onboarding - show splash then onboarding
                Timber.d("MainNavGraph - Onboarding not completed, will show splash then onboarding")
                startDestination = Routes.SPLASH
                isFirstTimeUser = true
                // Don't mark app as opened yet - wait until onboarding is complete
            } else if (currentUser == null) {
                // Returning user but NOT authenticated - show splash then select role
                // This handles the case where user uninstalled/reinstalled or logged out
                Timber.d("MainNavGraph - Returning user but not authenticated, showing splash then select role")
                startDestination = Routes.SPLASH
                isFirstTimeUser = false // Will go to SELECT_ROLE after splash
            } else {
                // Returning user who IS authenticated - check profile status
                Timber.d("MainNavGraph - Returning authenticated user, checking profile...")
                val userRole = profileCompletionViewModel.getUserRole()
                Timber.d("MainNavGraph - Authenticated user role: $userRole")
                
                if (userRole != null) {
                    // CHECK LOCAL DATASTORE FIRST - this is the source of truth for what user sees
                    val isProfileCompleteLocal = profileCompletionViewModel.isProfileComplete(userRole)
                    Timber.d("MainNavGraph - Profile complete (LOCAL DataStore): $isProfileCompleteLocal")
                    
                    if (isProfileCompleteLocal) {
                        // Profile is complete locally, go directly to home
                        when (userRole) {
                            com.example.dutype.models.UserRole.WORKER -> {
                                Timber.d("MainNavGraph - Local profile complete, navigating to WORKER_HOME")
                                startDestination = Routes.WORKER_HOME
                            }
                            com.example.dutype.models.UserRole.EMPLOYER -> {
                                Timber.d("MainNavGraph - Local profile complete, navigating to EMPLOYER_HOME")
                                startDestination = Routes.EMPLOYER_HOME
                            }
                            else -> {
                                Timber.d("MainNavGraph - Unknown role, going to SELECT_ROLE")
                                startDestination = Routes.SELECT_ROLE
                            }
                        }
                    } else {
                        // Profile incomplete but has role - go to appropriate profile setup
                        when (userRole) {
                            com.example.dutype.models.UserRole.WORKER -> {
                                Timber.d("MainNavGraph - Worker profile incomplete, going to PROFILE_SETUP")
                                startDestination = Routes.PROFILE_SETUP
                            }
                            com.example.dutype.models.UserRole.EMPLOYER -> {
                                Timber.d("MainNavGraph - Employer profile incomplete, going to EMPLOYER_PROFILE_SETUP")
                                startDestination = Routes.EMPLOYER_PROFILE_SETUP
                            }
                            else -> {
                                Timber.d("MainNavGraph - Profile incomplete with unknown role, going to SELECT_ROLE")
                                startDestination = Routes.SELECT_ROLE
                            }
                        }
                    }
                } else {
                    // No role set, go to role selection
                    Timber.d("MainNavGraph - No role set, going to SELECT_ROLE")
                    startDestination = Routes.SELECT_ROLE
                }
            }
            
            Timber.d("MainNavGraph - Final startDestination: $startDestination")
            
            // NO DELAY - Set states immediately for instant navigation
            isLoading = false
            navigationDetermined = true
            Timber.d("MainNavGraph - Navigation completed immediately, startDestination: $startDestination")
            
        } catch (e: Exception) {
            Timber.e(e, "MainNavGraph - Error determining start destination")
            // Fallback to role selection
            startDestination = Routes.SELECT_ROLE
            isLoading = false
            navigationDetermined = true
            Timber.d("MainNavGraph - Error fallback - startDestination: $startDestination")
        }
    }
    
    // Safety timeout to ensure navigationDetermined is always set (shorter timeout now)
    LaunchedEffect(Unit) {
        delay(500) // Reduced from 3000ms to 500ms - quick fallback if something goes wrong
        if (!navigationDetermined) {
            Timber.w("MainNavGraph - Timeout reached, forcing navigationDetermined = true")
            navigationDetermined = true
            if (startDestination == Routes.SELECT_ROLE) {
                // Fallback to role selection if no destination was determined
                Timber.w("MainNavGraph - Using fallback destination: SELECT_ROLE")
            }
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
            
            // Only process if there's actual notification data (not just a regular app launch)
            val hasNotificationData = navigateTo != null || notificationAction != null || 
                                      jobId != null || applicationId != null || notificationId != null
            
            if (!hasNotificationData) {
                // No notification data - this is a regular app launch, skip notification handling
                return@LaunchedEffect
            }
            
            Timber.i("MainNavGraph - Notification clicked with intent")
            
            // Add a small delay to ensure NavHost is fully initialized
            delay(200)
            
            Timber.i("MainNavGraph - Navigation data: navigateTo=$navigateTo, action=$notificationAction, jobId=$jobId, applicationId=$applicationId")
            
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
                                    Timber.i("MainNavGraph - Navigating to WORKER_HOME from notification")
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
                                Timber.i("MainNavGraph - Navigating to EMPLOYER_HOME from notification")
                                navController.navigate(Routes.EMPLOYER_HOME) {
                                    popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else if (userRole == com.example.dutype.models.UserRole.WORKER) {
                                Timber.i("MainNavGraph - Navigating to WORKER_HOME from notification")
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
        composable(Routes.SPLASH) {
            // Use rememberUpdatedState to ensure the callback always uses the latest value
            val currentIsFirstTimeUser by rememberUpdatedState(isFirstTimeUser)
            
            DutyPeSplashScreen(
                navController = navController,
                onSplashComplete = {
                    // Navigate based on whether this is a first-time user
                    Timber.d("MainNavGraph - Splash complete, isFirstTimeUser: $currentIsFirstTimeUser")
                    if (currentIsFirstTimeUser) {
                        // First-time user - go to onboarding
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    } else {
                        // Returning user - go to role selection (will be handled by main navigation logic)
                        navController.navigate(Routes.SELECT_ROLE) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                },
                duration = 2000L // 2 seconds
            )
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(navController)
        }
        composable(
            route = "${Routes.ENHANCED_LOGIN}?role={role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType; defaultValue = "WORKER" })
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "WORKER"
            Timber.d("EnhancedLoginScreen received role parameter: $role")
            Timber.d("BackStackEntry arguments: ${backStackEntry.arguments}")
            Timber.d("BackStackEntry destination: ${backStackEntry.destination}")
            EnhancedLoginScreen(
                navController = navController,
                skipRoleSelection = true,
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
        composable(Routes.PROFILE_SETUP) {
            MandatoryWorkerProfileSetupScreen(navController = navController)
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
                jobId = jobId,
                onApplicationClick = { application ->
                    // Navigate to detailed application view
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
                            employerViewModel.updateApplicationStatus(applicationId, newStatus, notes)
                            navController.popBackStack()
                        }
                    } else {
                        employerViewModel.updateApplicationStatus(applicationId, newStatus, notes)
                        navController.popBackStack()
                    }
                },
                onMessageWorker = { conversationId ->
                    navController.navigate(Routes.chatConversationDetailRoute(conversationId))
                }
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
        
        // Subscription Screen
        composable(Routes.EMPLOYER_SUBSCRIPTION) {
            val subscriptionViewModel: com.example.dutype.viewmodels.SubscriptionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val uiState by subscriptionViewModel.uiState.collectAsState()
            val context = LocalContext.current
            val activity = context as? android.app.Activity
            
            com.example.dutype.employer.screens.SubscriptionScreen(
                currentSubscription = uiState.currentSubscription,
                onBackClick = { navController.popBackStack() },
                onSelectPlan = { plan, isYearly ->
                    activity?.let {
                        subscriptionViewModel.initializePayment(it, plan, isYearly)
                    }
                },
                isLoading = uiState.isLoading
            )
            
            // Handle payment success
            LaunchedEffect(uiState.paymentSuccess) {
                if (uiState.paymentSuccess) {
                    android.widget.Toast.makeText(
                        context,
                        "Subscription activated successfully!",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    subscriptionViewModel.clearPaymentSuccess()
                }
            }
            
            // Handle payment error
            LaunchedEffect(uiState.paymentError) {
                uiState.paymentError?.let { error ->
                    android.widget.Toast.makeText(
                        context,
                        "Payment failed: $error",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    subscriptionViewModel.clearError()
                }
            }
        }
        
        // Language Selection - Now handled via bottom sheet in profile screens
        // Route kept for backward compatibility but redirects to profile
        composable(Routes.LANGUAGE_SELECTION) {
            // Navigate back - language selection is now a bottom sheet
            androidx.compose.runtime.LaunchedEffect(Unit) {
                navController.popBackStack()
            }
        }
        
        // Chat Conversations List (Employer)
        composable(Routes.CHAT_CONVERSATIONS) {
            // ChatService accessed via ChatViewModel (proper DI pattern)
            val chatViewModel: com.example.dutype.viewmodels.ChatViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            com.example.dutype.common.chat.ConversationListScreen(
                chatService = chatViewModel.chatService,
                onBackClick = { navController.popBackStack() },
                onConversationClick = { conversationId ->
                    navController.navigate(Routes.chatConversationDetailRoute(conversationId))
                }
            )
        }
        
        // Chat Conversation Detail (Employer)
        composable(
            route = Routes.CHAT_CONVERSATION_DETAIL,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            // ChatService accessed via ChatViewModel (proper DI pattern)
            val chatViewModel: com.example.dutype.viewmodels.ChatViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            com.example.dutype.common.chat.ChatDetailScreen(
                conversationId = conversationId,
                chatService = chatViewModel.chatService,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        // Cancellation & Refund Screen
        composable(Routes.CANCELLATION_REFUND) {
            com.example.dutype.common.chat.info.CancellationRefundScreen(
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        
        // Contact Us Screen
        composable(Routes.CONTACT_US) {
            com.example.dutype.common.chat.info.ContactUsScreen(
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
                                    Timber.i("Existing user - Navigating to WORKER_HOME")
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
                                        Timber.i("Local profile complete - Navigating to WORKER_HOME")
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
                // User is not signed in - Guest Mode: Navigate directly to home screen
                Timber.d("User not signed in, navigating directly to home (Guest Mode)")
                val userRole = when (role) {
                    "WORKER" -> com.example.dutype.models.UserRole.WORKER
                    "EMPLOYER" -> com.example.dutype.models.UserRole.EMPLOYER
                    else -> com.example.dutype.models.UserRole.WORKER
                }
                
                when (userRole) {
                    com.example.dutype.models.UserRole.WORKER -> {
                        Timber.i("Guest Mode - Navigating to WORKER_HOME")
                        navController.navigate(Routes.WORKER_HOME) {
                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    com.example.dutype.models.UserRole.EMPLOYER -> {
                        Timber.i("Guest Mode - Navigating to EMPLOYER_HOME")
                        navController.navigate(Routes.EMPLOYER_HOME) {
                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    else -> {
                        Timber.i("Guest Mode Fallback - Navigating to WORKER_HOME")
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
