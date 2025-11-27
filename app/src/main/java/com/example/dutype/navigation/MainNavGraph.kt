package com.example.dutype.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.delay
import com.example.dutype.auth.PhoneLoginScreen
import com.example.dutype.auth.EnhancedLoginScreen
import com.example.dutype.common.chat.SelectRoleScreen
import com.example.dutype.components.DutyPeSplashScreen
import com.example.dutype.onboarding.OnboardingScreen
import com.example.dutype.employer.screens.AnalyticsScreen
import com.example.dutype.employer.screens.editjob.EditJobScreen
import com.example.dutype.common.employer.CompanyDetailsScreen
import com.example.dutype.common.employer.EmployerProfileScreen
import com.example.dutype.employer.screens.profile.EmployerCompanyDetailsScreen
import com.example.dutype.employer.screens.MandatoryEmployerProfileSetupScreen
import com.example.dutype.location.LocationServiceScreen
import com.example.dutype.location.ManualLocationScreen
import com.example.dutype.navigation.employer.EmployerMainScreen
import com.example.dutype.navigation.workerNavGraph.WorkerMainScreen
import com.example.dutype.worker.screens.ProfileSetupScreen
import com.example.dutype.worker.screens.JobApplicationScreen
import com.example.dutype.worker.screens.MandatoryWorkerProfileSetupScreen
import com.example.dutype.worker.screens.profile.WorkerProfileDetailsScreen
import com.example.dutype.employer.screens.applications.EmployerApplicationManagementScreen
import com.example.dutype.employer.screens.applications.ApplicationDetailScreen
import com.example.dutype.worker.screens.SmartJobApplicationScreen

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
            println("🔍 MainNavGraph - Starting navigation logic...")
            
            // Show splash immediately for all users
            showLoadingIndicator = true
            
            // Check if user has ever opened the app before
            val hasOpenedBefore = profileCompletionViewModel.hasAppBeenOpenedBefore()
            println("🔍 MainNavGraph - hasOpenedBefore: $hasOpenedBefore")
            
            if (!hasOpenedBefore) {
                // First-time user - show onboarding after splash
                println("🔍 MainNavGraph - First-time user, will show splash then onboarding")
                startDestination = Routes.SPLASH
                isFirstTimeUser = true
                profileCompletionViewModel.markAppAsOpened()
            } else {
                // Returning user - check authentication and profile status
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                println("🔍 MainNavGraph - Returning user, currentUser: ${currentUser?.email}")
                
                if (currentUser != null) {
                    // User is authenticated, check if they have a complete profile
                    val userRole = profileCompletionViewModel.getUserRole()
                    println("🔍 MainNavGraph - Authenticated user role: $userRole")
                    
                    if (userRole != null) {
                        // CHECK LOCAL DATASTORE FIRST - this is the source of truth for what user sees
                        val isProfileCompleteLocal = profileCompletionViewModel.isProfileComplete(userRole)
                        println("🔍 MainNavGraph - Profile complete (LOCAL DataStore): $isProfileCompleteLocal")
                        
                        if (isProfileCompleteLocal) {
                            // Profile is complete locally, go directly to home
                            when (userRole) {
                                com.example.dutype.models.UserRole.WORKER -> {
                                    println("🔍 MainNavGraph - Local profile complete, navigating to WORKER_HOME")
                                    startDestination = Routes.WORKER_HOME
                                    println("🔍 MainNavGraph - Set startDestination to WORKER_HOME: $startDestination")
                                }
                                com.example.dutype.models.UserRole.EMPLOYER -> {
                                    println("🔍 MainNavGraph - Local profile complete, navigating to EMPLOYER_HOME")
                                    startDestination = Routes.EMPLOYER_HOME
                                    println("🔍 MainNavGraph - Set startDestination to EMPLOYER_HOME: $startDestination")
                                }
                                else -> {
                                    println("🔍 MainNavGraph - Unknown role, going to SELECT_ROLE")
                                    startDestination = Routes.SELECT_ROLE
                                    println("🔍 MainNavGraph - Set startDestination to SELECT_ROLE: $startDestination")
                                }
                            }
                        } else {
                            // Profile incomplete but has role - go to appropriate profile setup
                            when (userRole) {
                                com.example.dutype.models.UserRole.WORKER -> {
                                    println("🔍 MainNavGraph - Worker profile incomplete, going to PROFILE_SETUP")
                                    startDestination = Routes.PROFILE_SETUP
                                }
                                com.example.dutype.models.UserRole.EMPLOYER -> {
                                    println("🔍 MainNavGraph - Employer profile incomplete, going to EMPLOYER_PROFILE_SETUP")
                                    startDestination = Routes.EMPLOYER_PROFILE_SETUP
                                }
                                else -> {
                                    println("🔍 MainNavGraph - Profile incomplete with unknown role, going to SELECT_ROLE")
                                    startDestination = Routes.SELECT_ROLE
                                }
                            }
                        }
                    } else {
                        // No role set, go to role selection
                        println("🔍 MainNavGraph - No role set, going to SELECT_ROLE")
                        startDestination = Routes.SELECT_ROLE
                    }
                } else {
                    // User not authenticated, go to role selection
                    println("🔍 MainNavGraph - User not authenticated, going to SELECT_ROLE")
                    startDestination = Routes.SELECT_ROLE
                }
            }
            
            println("🔍 MainNavGraph - Final startDestination: $startDestination")
            
            // NO DELAY - Set states immediately for instant navigation
            isLoading = false
            navigationDetermined = true
            println("🔍 MainNavGraph - Navigation completed immediately, startDestination: $startDestination")
            
        } catch (e: Exception) {
            println("❌ MainNavGraph - Error determining start destination: ${e.message}")
            // Fallback to role selection
            startDestination = Routes.SELECT_ROLE
            isLoading = false
            navigationDetermined = true
            println("🔍 MainNavGraph - Error fallback - startDestination: $startDestination")
        }
    }
    
    // Safety timeout to ensure navigationDetermined is always set (shorter timeout now)
    LaunchedEffect(Unit) {
        delay(500) // Reduced from 3000ms to 500ms - quick fallback if something goes wrong
        if (!navigationDetermined) {
            println("⚠️ MainNavGraph - Timeout reached, forcing navigationDetermined = true")
            navigationDetermined = true
            if (startDestination == Routes.SELECT_ROLE) {
                // Fallback to role selection if no destination was determined
                println("⚠️ MainNavGraph - Using fallback destination: SELECT_ROLE")
            }
        }
    }
    
    // Handle notification clicks - only after NavHost is ready and navigation is determined
    LaunchedEffect(notificationData, notificationIntent, navigationDetermined, isLoading) {
        // Only proceed if NavHost is ready (navigationDetermined), not loading, and we have notification data
        if (notificationIntent != null && navigationDetermined && !isLoading) {
            println("🔔 MainNavGraph - Notification clicked with intent")
            
            // Add a small delay to ensure NavHost is fully initialized
            delay(200)
            
            // Extract navigation data from intent
            val navigateTo = notificationIntent.getStringExtra("navigate_to")
            val notificationAction = notificationIntent.getStringExtra("notification_action")
            val jobId = notificationIntent.getStringExtra("job_id")
            val applicationId = notificationIntent.getStringExtra("application_id")
            
            println("🔔 MainNavGraph - Navigation data: navigateTo=$navigateTo, action=$notificationAction, jobId=$jobId, applicationId=$applicationId")
            
            // Check if user is authenticated
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val userRole = profileCompletionViewModel.getUserRole()
                
                // Navigate to specific screen if provided
                if (navigateTo != null) {
                    try {
                        println("🔔 MainNavGraph - Navigating to specific screen: $navigateTo")
                        navController.navigate(navigateTo) {
                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                        }
                    } catch (e: Exception) {
                        println("❌ Error navigating to specific screen: ${e.message}")
                    }
                } else {
                    // Fallback to home screen based on user role - but first check profile completion
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (currentUser != null && userRole != null) {
                        // CHECK LOCAL DATASTORE FIRST - same as main navigation logic
                        val isProfileCompleteLocal = profileCompletionViewModel.isProfileComplete(userRole)
                        println("🔔 MainNavGraph - Notification handler profile complete check (LOCAL DataStore): $isProfileCompleteLocal")
                        
                        if (isProfileCompleteLocal) {
                            try {
                                if (userRole == com.example.dutype.models.UserRole.EMPLOYER) {
                                    println("🔔 MainNavGraph - Navigating to EMPLOYER_HOME from notification")
                                    navController.navigate(Routes.EMPLOYER_HOME) {
                                        popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                    }
                                } else if (userRole == com.example.dutype.models.UserRole.WORKER) {
                                    println("🔔 MainNavGraph - Navigating to WORKER_HOME from notification")
                                    navController.navigate(Routes.WORKER_HOME) {
                                        popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                    }
                                }
                            } catch (e: Exception) {
                                println("❌ Error navigating to home from notification: ${e.message}")
                            }
                        } else {
                            // Profile incomplete - navigate to appropriate onboarding
                            try {
                                if (userRole == com.example.dutype.models.UserRole.EMPLOYER) {
                                    println("🔔 MainNavGraph - Profile incomplete, navigating to EMPLOYER_PROFILE_SETUP from notification")
                                    navController.navigate(Routes.EMPLOYER_PROFILE_SETUP) {
                                        popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                    }
                                } else if (userRole == com.example.dutype.models.UserRole.WORKER) {
                                    println("🔔 MainNavGraph - Profile incomplete, navigating to PROFILE_SETUP from notification")
                                    navController.navigate(Routes.PROFILE_SETUP) {
                                        popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                    }
                                }
                            } catch (e: Exception) {
                                println("❌ Error navigating to onboarding from notification: ${e.message}")
                            }
                        }
                    }
                }
            }
        } else if (notificationData != null && navigationDetermined && !isLoading) {
            println("🔔 MainNavGraph - Legacy notification clicked: $notificationData")
            
            // Add a small delay to ensure NavHost is fully initialized
            delay(200)
            
            // Check if user is authenticated and profile complete
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val userRole = profileCompletionViewModel.getUserRole()
                
                if (userRole != null) {
                    // CHECK LOCAL DATASTORE FIRST - same as main navigation logic
                    val isProfileCompleteLocal = profileCompletionViewModel.isProfileComplete(userRole)
                    println("🔔 MainNavGraph - Legacy notification profile complete check (LOCAL DataStore): $isProfileCompleteLocal")
                    
                    if (isProfileCompleteLocal) {
                        // Navigate to home if profile is complete
                        try {
                            if (userRole == com.example.dutype.models.UserRole.EMPLOYER) {
                                println("🔔 MainNavGraph - Navigating to EMPLOYER_HOME from notification")
                                navController.navigate(Routes.EMPLOYER_HOME) {
                                    popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                }
                            } else if (userRole == com.example.dutype.models.UserRole.WORKER) {
                                println("🔔 MainNavGraph - Navigating to WORKER_HOME from notification")
                                navController.navigate(Routes.WORKER_HOME) {
                                    popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                }
                            }
                        } catch (e: Exception) {
                            println("❌ Error navigating to home from legacy notification: ${e.message}")
                        }
                    } else {
                        // Profile incomplete - navigate to appropriate onboarding
                        try {
                            if (userRole == com.example.dutype.models.UserRole.EMPLOYER) {
                                println("🔔 MainNavGraph - Legacy notification: Profile incomplete, navigating to EMPLOYER_PROFILE_SETUP")
                                navController.navigate(Routes.EMPLOYER_PROFILE_SETUP) {
                                    popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                }
                            } else if (userRole == com.example.dutype.models.UserRole.WORKER) {
                                println("🔔 MainNavGraph - Legacy notification: Profile incomplete, navigating to PROFILE_SETUP")
                                navController.navigate(Routes.PROFILE_SETUP) {
                                    popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                }
                            }
                        } catch (e: Exception) {
                            println("❌ Error navigating to onboarding from legacy notification: ${e.message}")
                        }
                    }
                }
            }
        }
    }
    
    // IMPORTANT: Only render NavHost AFTER navigation destination is determined
    // This prevents rendering with the wrong start destination
    if (navigationDetermined) {
        println("🔍 MainNavGraph - Rendering NavHost with startDestination: $startDestination (navigationDetermined: $navigationDetermined)")
        
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
        composable(Routes.SPLASH) {
            DutyPeSplashScreen(
                navController = navController,
                onSplashComplete = {
                    // Navigate based on whether this is a first-time user
                    if (isFirstTimeUser) {
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
            println("🔍 EnhancedLoginScreen received role parameter: $role")
            println("🔍 BackStackEntry arguments: ${backStackEntry.arguments}")
            println("🔍 BackStackEntry destination: ${backStackEntry.destination}")
            EnhancedLoginScreen(
                navController = navController,
                skipRoleSelection = true,
                initialRole = role
            )
        }
        composable(Routes.LOGIN_BOTTOM_SHEET) {
            PhoneLoginScreen(navController = navController)
        }
        composable(Routes.LOCATION_SERVICE) {
            LocationServiceScreen(navController = navController)
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
        composable(
            route = Routes.JOB_APPLICATION,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""

            // Use SmartJobApplicationScreen with correct parameters
            SmartJobApplicationScreen(
                jobId = jobId,
                navController = navController,
                onStatusBarColorChange = onStatusBarColorChange
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
            ApplicationDetailScreen(
                applicationId = applicationId,
                onBackClick = { navController.popBackStack() },
                onUpdateStatus = { newStatus, notes ->
                    // Handle status update
                    navController.popBackStack()
                }
            )
        }
        
       
        composable(Routes.COMPANY_DETAILS) {
            // Placeholder for company details
         CompanyDetailsScreen(navController)
        }
        composable(Routes.ANALYTICS) {
            // Analytics screen
            AnalyticsScreen(navController)
        }
    }
    } else {
        // Navigation destination is being determined - show minimal loading
        println("🔍 MainNavGraph - Navigation not yet determined, showing loading state...")
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
            println("🔍 Role selected: $role")
            
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
                        println("🔍 RoleSelectionWithNavigation - Has existing profile (high-level): $hasExistingProfile")
                        
                        if (hasExistingProfile) {
                            println("✅ Found existing profile, loading data and navigating to home...")
                            
                            // Load existing profile data into local state
                            profileCompletionViewModel.loadExistingProfileData(userEmail, userRole)
                            
                            // Navigate directly to home screen
                            when (userRole) {
                                com.example.dutype.models.UserRole.WORKER -> {
                                    println("📱 Existing user - Navigating to WORKER_HOME")
                                    navController.navigate(Routes.WORKER_HOME) {
                                        popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                    }
                                }
                                com.example.dutype.models.UserRole.EMPLOYER -> {
                                    println("📱 Existing user - Navigating to EMPLOYER_HOME")
                                    navController.navigate(Routes.EMPLOYER_HOME) {
                                        popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                    }
                                }
                                else -> {
                                    println("📱 Fallback - Navigating to WORKER_HOME")
                                    navController.navigate(Routes.WORKER_HOME) {
                                        popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                    }
                                }
                            }
                        } else {
                            // Check local state for profile completion
                            val isProfileComplete = profileCompletionViewModel.isProfileComplete(userRole)
                            println("🔍 Local profile complete: $isProfileComplete")
                            
                            if (isProfileComplete) {
                                // Profile is complete locally, navigate directly to home screen
                                when (userRole) {
                                    com.example.dutype.models.UserRole.WORKER -> {
                                        println("📱 Local profile complete - Navigating to WORKER_HOME")
                                        navController.navigate(Routes.WORKER_HOME) {
                                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                        }
                                    }
                                    com.example.dutype.models.UserRole.EMPLOYER -> {
                                        println("📱 Local profile complete - Navigating to EMPLOYER_HOME")
                                        navController.navigate(Routes.EMPLOYER_HOME) {
                                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                        }
                                    }
                                    else -> {
                                        println("📱 Fallback - Navigating to WORKER_HOME")
                                        navController.navigate(Routes.WORKER_HOME) {
                                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                        }
                                    }
                                }
                            } else {
                                // Profile not complete, navigate to profile setup
                                when (userRole) {
                                    com.example.dutype.models.UserRole.WORKER -> {
                                        println("📱 Profile incomplete - Navigating to PROFILE_SETUP")
                                        navController.navigate(Routes.PROFILE_SETUP) {
                                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                        }
                                    }
                                    com.example.dutype.models.UserRole.EMPLOYER -> {
                                        println("📱 Profile incomplete - Navigating to EMPLOYER_PROFILE_SETUP")
                                        navController.navigate(Routes.EMPLOYER_PROFILE_SETUP) {
                                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                        }
                                    }
                                    else -> {
                                        println("📱 Fallback - Navigating to PROFILE_SETUP")
                                        navController.navigate(Routes.PROFILE_SETUP) {
                                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                        }
                                    }
                                }
                            }
                        }
                } catch (e: Exception) {
                    // If there's an error, fall back to profile setup
                    println("❌ Error checking profile status: ${e.message}")
                    when (userRole) {
                        com.example.dutype.models.UserRole.WORKER -> {
                            println("🔄 Fallback: Navigating to PROFILE_SETUP")
                            navController.navigate(Routes.PROFILE_SETUP) {
                                popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                            }
                        }
                        com.example.dutype.models.UserRole.EMPLOYER -> {
                            println("🔄 Fallback: Navigating to EMPLOYER_PROFILE_SETUP")
                            navController.navigate(Routes.EMPLOYER_PROFILE_SETUP) {
                                popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                            }
                        }
                        else -> {
                            println("🔄 Fallback: Navigating to PROFILE_SETUP")
                            navController.navigate(Routes.PROFILE_SETUP) {
                                popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                            }
                        }
                    }
                }
            } else {
                // User is not signed in, navigate to Google Sign-In
                println("🔍 User not signed in, navigating to Google Sign-In")
                navController.navigate("${Routes.ENHANCED_LOGIN}?role=$role")
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
