package com.example.partimes.navigation

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.partimes.auth.PhoneLoginScreen
import com.example.partimes.auth.EnhancedLoginScreen
import com.example.partimes.common.chat.SelectRoleScreen
import com.example.partimes.common.chat.SplashScreen
import com.example.partimes.common.employer.AnalyticsScreen
import com.example.partimes.common.employer.CompanyDetailsScreen
import com.example.partimes.common.employer.EmployerProfileScreen
import com.example.partimes.common.employer.EmployerTermsScreen
import com.example.partimes.common.employer.TalentSearchScreen
import com.example.partimes.location.LocationServiceScreen
import com.example.partimes.location.ManualLocationScreen
import com.example.partimes.navigation.employer.EmployerMainScreen
import com.example.partimes.navigation.workerNavGraph.WorkerMainScreen
import com.example.partimes.worker.onboarding.WorkerOnboardingScreen
import com.example.partimes.worker.screens.ProfileSetupScreen
import com.example.partimes.worker.screens.JobApplicationScreen
import com.example.partimes.worker.screens.MandatoryWorkerProfileSetupScreen
import com.example.partimes.employer.screens.MandatoryEmployerProfileSetupScreen

@Composable
fun MainNavGraph(
    navController: NavHostController,
    onStatusBarColorChange: (Color) -> Unit = {},
    notificationData: String? = null
) {
    val context = LocalContext.current
    val profileCompletionViewModel: com.example.partimes.viewmodels.ProfileCompletionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    
    // State management for determining start destination
    var isLoading by remember { mutableStateOf(true) }
    var startDestination by remember { mutableStateOf(Routes.SELECT_ROLE) }
    var showLoadingIndicator by remember { mutableStateOf(false) } // Start with false
    var navigationDetermined by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        try {
            println("🔍 MainNavGraph - Starting navigation logic...")
            
            // Check if user has ever opened the app before
            val hasOpenedBefore = profileCompletionViewModel.hasAppBeenOpenedBefore()
            println("🔍 MainNavGraph - hasOpenedBefore: $hasOpenedBefore")
            
            if (!hasOpenedBefore) {
                // First-time user - show splash screen
                println("🔍 MainNavGraph - First-time user, showing splash screen")
                startDestination = Routes.SPLASH
                showLoadingIndicator = true // Show loading indicator only for first-time users
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
                        // Check if profile is complete
                        val isProfileComplete = profileCompletionViewModel.isProfileComplete(userRole)
                        println("🔍 MainNavGraph - Profile complete: $isProfileComplete")
                        
                        if (isProfileComplete) {
                            // Profile is complete, go directly to home
                            when (userRole) {
                                com.example.partimes.models.UserRole.WORKER -> {
                                    println("🔍 MainNavGraph - Navigating to WORKER_HOME")
                                    startDestination = Routes.WORKER_HOME
                                }
                                com.example.partimes.models.UserRole.EMPLOYER -> {
                                    println("🔍 MainNavGraph - Navigating to EMPLOYER_HOME")
                                    startDestination = Routes.EMPLOYER_HOME
                                }
                                else -> {
                                    println("🔍 MainNavGraph - Unknown role, going to SELECT_ROLE")
                                    startDestination = Routes.SELECT_ROLE
                                }
                            }
                        } else {
                            // Profile incomplete, go to role selection
                            println("🔍 MainNavGraph - Profile incomplete, going to SELECT_ROLE")
                            startDestination = Routes.SELECT_ROLE
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
        } catch (e: Exception) {
            println("❌ MainNavGraph - Error determining start destination: ${e.message}")
            // Fallback to role selection
            startDestination = Routes.SELECT_ROLE
        } finally {
            isLoading = false
            navigationDetermined = true
            println("🔍 MainNavGraph - Loading completed, isLoading = false, navigationDetermined = true")
        }
    }
    
    // Handle notification clicks
    LaunchedEffect(notificationData) {
        if (notificationData != null) {
            println("🔔 MainNavGraph - Notification clicked: $notificationData")
            
            // Check if user is authenticated
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val userRole = profileCompletionViewModel.getUserRole()
                
                // Navigate to employer home if user is an employer
                if (userRole == com.example.partimes.models.UserRole.EMPLOYER) {
                    println("🔔 MainNavGraph - Navigating to EMPLOYER_HOME from notification")
                    navController.navigate(Routes.EMPLOYER_HOME) {
                        popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                    }
                } else if (userRole == com.example.partimes.models.UserRole.WORKER) {
                    println("🔔 MainNavGraph - Navigating to WORKER_HOME from notification")
                    navController.navigate(Routes.WORKER_HOME) {
                        popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                    }
                }
            }
        }
    }
    
    // Show loading indicator while determining start destination
    if (isLoading && showLoadingIndicator) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1F2937)),
            contentAlignment = Alignment.Center
        ) {
            // Simple loading indicator - you can replace with your app logo
            androidx.compose.material3.CircularProgressIndicator(
                color = Color(0xFF3B82F6),
                modifier = Modifier.size(48.dp)
            )
        }
    } else if (navigationDetermined) {
        // Only show NavHost when navigation is determined
    NavHost(
        navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(navController)
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
        composable(Routes.WORKER_ONBOARDING) {
            WorkerOnboardingScreen(navController)
        }
        composable(Routes.EMPLOYER_ONBOARDING) {
            MandatoryEmployerProfileSetupScreen(navController)
        }
        composable(Routes.WORKER_HOME) {
            WorkerMainScreen(
                rootNavController = navController,
                onStatusBarColorChange = onStatusBarColorChange
            )
        }
        composable(Routes.EMPLOYER_HOME) {
            EmployerMainScreen(rootNavController = navController)
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
            // For now, use placeholder values - in a real app, you'd fetch job details here
            JobApplicationScreen(
                jobId = jobId,
                jobTitle = "Job Title", // This will be updated when job details are fetched
                companyName = "Company Name",
                jobLocation = "Location",
                jobType = "Job Type",
                payInfo = "Pay Info",
                onBackClick = { navController.popBackStack() },
                onApplicationSubmitted = { navController.popBackStack() }
            )
        }
        
        // Missing employer routes - add placeholder screens
        composable(Routes.EMPLOYER_PROFILE) {
            // Placeholder for employer profile
            EmployerProfileScreen(navController)
        }
        composable(
            route = Routes.EDIT_JOB,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            // Placeholder for edit job screen - you can create a proper EditJobScreen
            // For now, navigate back to employer home
            navController.popBackStack()
        }
        composable(Routes.TALENT_SEARCH) {
            // Placeholder for talent search
            TalentSearchScreen(navController)
        }
        composable(Routes.EMPLOYER_TERMS) {
            // Placeholder for employer terms
      EmployerTermsScreen(navController)
        }
        composable(Routes.COMPANY_DETAILS) {
            // Placeholder for company details
         CompanyDetailsScreen(navController)
        }
        composable(Routes.ANALYTICS) {
            // Placeholder for analytics
           AnalyticsScreen(navController)
        }
    }
    } else {
        // Show nothing while navigation is being determined
        // This prevents any intermediate screens from showing
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1F2937))
        )
    }
}

@Composable
private fun RoleSelectionWithNavigation(
    navController: NavHostController,
    profileCompletionViewModel: com.example.partimes.viewmodels.ProfileCompletionViewModel
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
                    "WORKER" -> com.example.partimes.models.UserRole.WORKER
                    "EMPLOYER" -> com.example.partimes.models.UserRole.EMPLOYER
                    else -> com.example.partimes.models.UserRole.WORKER
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
                                com.example.partimes.models.UserRole.WORKER -> {
                                    println("📱 Existing user - Navigating to WORKER_HOME")
                                    navController.navigate(Routes.WORKER_HOME) {
                                        popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                    }
                                }
                                com.example.partimes.models.UserRole.EMPLOYER -> {
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
                                    com.example.partimes.models.UserRole.WORKER -> {
                                        println("📱 Local profile complete - Navigating to WORKER_HOME")
                                        navController.navigate(Routes.WORKER_HOME) {
                                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                        }
                                    }
                                    com.example.partimes.models.UserRole.EMPLOYER -> {
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
                                    com.example.partimes.models.UserRole.WORKER -> {
                                        println("📱 Profile incomplete - Navigating to PROFILE_SETUP")
                                        navController.navigate(Routes.PROFILE_SETUP) {
                                            popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                                        }
                                    }
                                    com.example.partimes.models.UserRole.EMPLOYER -> {
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
                        com.example.partimes.models.UserRole.WORKER -> {
                            println("🔄 Fallback: Navigating to PROFILE_SETUP")
                            navController.navigate(Routes.PROFILE_SETUP) {
                                popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                            }
                        }
                        com.example.partimes.models.UserRole.EMPLOYER -> {
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