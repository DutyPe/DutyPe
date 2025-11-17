package com.example.dutype.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.common.chat.SelectRoleScreen
import com.example.dutype.models.User
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import com.parttime.dutype.R
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun EnhancedLoginScreen(
    navController: NavController,
    googleSignInManager: GoogleSignInManager? = null,
    skipRoleSelection: Boolean = false,
    initialRole: String = "WORKER"
) {
    val context = LocalContext.current
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val googleSignInManagerInstance = googleSignInManager ?: remember { GoogleSignInManager(context) }
    var showRoleSelection by remember { mutableStateOf(!skipRoleSelection) }
    var selectedRole by remember { 
        mutableStateOf<UserRole?>(
            if (skipRoleSelection) {
                when (initialRole) {
                    "WORKER" -> UserRole.WORKER
                    "EMPLOYER" -> UserRole.EMPLOYER
                    else -> UserRole.WORKER
                }
            } else null
        )
    }
    var shouldNavigate by remember { mutableStateOf(false) }
    var navigationUser by remember { mutableStateOf<User?>(null) }
    val scope = rememberCoroutineScope()
    
    // Debug logging
    LaunchedEffect(skipRoleSelection, initialRole, showRoleSelection, selectedRole) {
        Timber.d("EnhancedLoginScreen Debug:")
        Timber.d("  skipRoleSelection: $skipRoleSelection")
        Timber.d("  initialRole: $initialRole")
        Timber.d("  showRoleSelection: $showRoleSelection")
        Timber.d("  selectedRole: $selectedRole")
        Timber.d("  Current state: ${if (showRoleSelection) "Showing role selection" else if (selectedRole != null) "Showing Google Sign-In" else "Unknown state"}")
    }
    
    // Handle navigation after successful Google Sign-In
    LaunchedEffect(shouldNavigate, navigationUser) {
        if (shouldNavigate && navigationUser != null) {
            val user = navigationUser!!
            Timber.d("Starting navigation process for user: ${user.email}")
            
            try {
                Timber.d("============================================================")
                Timber.d("DATABASE EXISTENCE CHECK")
                Timber.d("============================================================")
                Timber.d("User Email: ${user.email}")
                Timber.d("User Role: ${user.role}")
                Timber.d("Checking if user data exists in database...")
                
                // Use high-level approach to check if user already has a complete profile in Firebase
                val hasExistingProfile = profileCompletionViewModel.checkExistingProfileHighLevel(user.email, user.role)
                Timber.d("============================================================")
                Timber.d("RESULT: User data exists in database: $hasExistingProfile")
                Timber.d("============================================================")
                
                if (hasExistingProfile) {
                    Timber.d("Found existing profile, loading data and navigating to home...")
                    Timber.d("User data status: COMPLETE - Existing user detected")
                    Timber.d("Next action: Loading profile data and navigating to HOME SCREEN")
                    
                    // Load existing profile data into local state
                    profileCompletionViewModel.loadExistingProfileData(user.email, user.role)
                    
                    // Also save current user info to local storage for consistency
                    profileCompletionViewModel.saveUserInfoToLocalStorage(
                        user.email, 
                        user.fullName, 
                        user.role
                    )
                    
                    // IMPORTANT: Mark profile as complete in local DataStore so navigation checks work correctly
                    profileCompletionViewModel.markProfileComplete(user.role)
                    profileCompletionViewModel.markProfileSetupAsShown(user.role)
                    
                    // Navigate directly to home screen
                    when (user.role) {
                        UserRole.WORKER -> {
                            Timber.d("Navigating to WORKER_HOME (existing user)")
                            navController.navigate(Routes.WORKER_HOME) {
                                popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                            }
                        }
                        UserRole.EMPLOYER -> {
                            // Check if employer has company details
                            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            if (currentUser != null) {
                                scope.launch {
                                    try {
                                        val employerProfileData = profileCompletionViewModel.getEmployerProfileData(currentUser.uid)
                                        employerProfileData.fold(
                                            onSuccess = { data ->
                                                // Check if company details exist (companyName is mandatory)
                                                val hasCompanyDetails = data != null && 
                                                    data["companyName"] != null && 
                                                    (data["companyName"] as? String)?.isNotBlank() == true
                                                
                                                if (hasCompanyDetails) {
                                                    Timber.d("Employer has company details, navigating to EMPLOYER_HOME")
                                                    navController.navigate(Routes.EMPLOYER_HOME) {
                                                        popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                                                    }
                                                } else {
                                                    Timber.d("Employer missing company details, navigating to EMPLOYER_PROFILE_SETUP")
                                                    navController.navigate(Routes.EMPLOYER_PROFILE_SETUP) {
                                                        popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                                                    }
                                                }
                                            },
                                            onFailure = { exception ->
                                                Timber.e(exception, "Error checking employer profile")
                                                // On error, navigate to company details setup
                                                navController.navigate(Routes.EMPLOYER_PROFILE_SETUP) {
                                                    popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                                                }
                                            }
                                        )
                                    } catch (e: Exception) {
                                        Timber.e(e, "Exception checking employer profile")
                                        // On exception, navigate to company details setup
                                        navController.navigate(Routes.EMPLOYER_PROFILE_SETUP) {
                                            popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                                        }
                                    }
                                }
                            } else {
                                Timber.d("No current user, navigating to EMPLOYER_PROFILE_SETUP")
                                navController.navigate(Routes.EMPLOYER_PROFILE_SETUP) {
                                    popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                                }
                            }
                        }
                        else -> navController.navigate(Routes.SELECT_ROLE)
                    }
                } else {
                    Timber.d("No existing profile found, proceeding with new user flow...")
                    Timber.d("User data status: NOT FOUND - New user detected")
                    Timber.d("Next action: Navigating to PROFILE SETUP SCREEN")
                    
                    // Save user info for profile setup (both Firebase and local storage)
                    profileCompletionViewModel.saveUserInfo(
                        user.email, 
                        user.fullName, 
                        user.role
                    )
                    
                    // Also save to local storage for profile setup screen
                    profileCompletionViewModel.saveUserInfoToLocalStorage(
                        user.email, 
                        user.fullName, 
                        user.role
                    )
                    
                    // Navigate to profile setup
                    Timber.d("Navigating to profile setup...")
                    when (user.role) {
                        UserRole.WORKER -> {
                            Timber.d("Navigating to PROFILE_SETUP")
                            navController.navigate(Routes.PROFILE_SETUP) {
                                popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                            }
                        }
                        UserRole.EMPLOYER -> {
                            // For new employer users, always go to company details setup
                            Timber.d("New employer user, navigating to EMPLOYER_PROFILE_SETUP")
                            navController.navigate(Routes.EMPLOYER_PROFILE_SETUP) {
                                popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                            }
                        }
                        else -> navController.navigate(Routes.SELECT_ROLE)
                    }
                }
                
                // Reset navigation state
                shouldNavigate = false
                navigationUser = null
                
            } catch (e: Exception) {
                Timber.e(e, "Error in profile setup check")
                
                // Save user info to local storage in case of error
                try {
                    profileCompletionViewModel.saveUserInfoToLocalStorage(
                        user.email, 
                        user.fullName, 
                        user.role
                    )
                } catch (saveException: Exception) {
                    Timber.e(saveException, "Failed to save user info to local storage")
                }
                
                // Fallback navigation - always go to onboarding for new users
                when (user.role) {
                    UserRole.WORKER -> {
                        Timber.d("Fallback: Navigating to PROFILE_SETUP")
                        navController.navigate(Routes.PROFILE_SETUP) {
                            popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                        }
                    }
                    UserRole.EMPLOYER -> {
                        Timber.d("Fallback: Navigating to EMPLOYER_PROFILE_SETUP")
                        navController.navigate(Routes.EMPLOYER_PROFILE_SETUP) {
                            popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                        }
                    }
                    else -> navController.navigate(Routes.SELECT_ROLE)
                }
                
                // Reset navigation state
                shouldNavigate = false
                navigationUser = null
            }
        }
    }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val authManager = remember { AuthManager(context) }
    
    // Google Sign-In client
    val googleSignInClient = remember {
        try {
            val webClientId = context.getString(R.string.default_web_client_id)
            Timber.d("Using Web Client ID from google-services.json: $webClientId")
            GoogleSignIn.getClient(
                context,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
            ).also {
                Timber.d("Google Sign-In client initialized successfully")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Google Sign-In client")
            throw e
        }
    }
    
    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Timber.d("Google Sign-In result: ${result.resultCode}")
        Timber.d("Result data: ${result.data}")
        
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                
                Timber.d("Got Google account: ${account.email}")
                Timber.d("ID Token present: ${idToken != null}")
                
                if (idToken != null && selectedRole != null) {
                    scope.launch {
                        try {
                            isLoading = true
                            errorMessage = null
                            
                            Timber.d("Starting Google Sign-In process...")
                            googleSignInManagerInstance.signInWithGoogle(
                                idToken = idToken,
                                selectedRole = selectedRole!!
                            ).collect { signInResult ->
                                signInResult.fold(
                                    onSuccess = { user ->
                                        Timber.d("Google Sign-In successful! User: ${user.email}")
                                        Timber.d("Now checking if user data exists in database...")
                                        Toast.makeText(context, "Welcome ${user.fullName}!", Toast.LENGTH_LONG).show()
                                        authManager.saveUser(user)
                                        authManager.setLoggedIn(true)
                                        isLoading = false
                                        
                                        // Trigger navigation using LaunchedEffect
                                        // This will check if user data exists in Firebase and navigate accordingly
                                        shouldNavigate = true
                                        navigationUser = user
                                    },
                                    onFailure = { exception ->
                                        isLoading = false
                                        errorMessage = exception.message ?: "Sign-in failed"
                                        Timber.e(exception, "Sign-in failed")
                                        Toast.makeText(context, "Sign-in failed: ${exception.message}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = e.message ?: "An error occurred during sign-in"
                            Timber.e(e, "Exception during sign-in")
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    errorMessage = "Failed to get ID token from Google"
                    Timber.e("ID Token is null or selectedRole is null. ID Token: ${idToken != null}, Role: ${selectedRole != null}")
                    Toast.makeText(context, "Failed to get ID token from Google", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                isLoading = false
                Timber.e(e, "ApiException")
                when (e.statusCode) {
                    12501 -> {
                        errorMessage = "Sign-in was cancelled"
                        Toast.makeText(context, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
                    }
                    7 -> {
                        errorMessage = "Network error. Please check your connection"
                        Toast.makeText(context, "Network error. Please check your connection", Toast.LENGTH_LONG).show()
                    }
                    10 -> {
                        errorMessage = "Developer error. Please contact support"
                        Toast.makeText(context, "Developer error. Please contact support", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        errorMessage = "Google Sign-In failed: ${e.message}"
                        Toast.makeText(context, "Google Sign-In failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else if (result.resultCode == android.app.Activity.RESULT_CANCELED) {
            // User explicitly cancelled the sign-in
            Timber.d("User cancelled Google Sign-In")
            Toast.makeText(context, "Sign-in was cancelled", Toast.LENGTH_SHORT).show()
        } else {
            // Google Sign-In was cancelled or failed
            Timber.d("Google Sign-In failed. Result code: ${result.resultCode}")
            Toast.makeText(context, "Sign-in failed. Please try again.", Toast.LENGTH_LONG).show()
        }
    }
    
    if (showRoleSelection) {
        SelectRoleScreen(
            navController = navController,
            onRoleSelected = { roleString ->
                val role = when (roleString) {
                    "WORKER" -> UserRole.WORKER
                    "EMPLOYER" -> UserRole.EMPLOYER
                    else -> UserRole.WORKER
                }
                Timber.d("Role selected: $role")
                selectedRole = role
                showRoleSelection = false
                // Now show Google Sign-In screen
            }
        )
    } else if (selectedRole != null) {
        // Show Google Sign-In screen after role selection
        // Only show login for EMPLOYER (mandatory), WORKER can skip
        if (selectedRole == UserRole.EMPLOYER) {
            // Employer MUST login - show professional login screen
            ProfessionalLoginScreen(
                role = selectedRole,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onGoogleSignInClick = {
                    Timber.d("Google Sign-In button clicked (Employer)")
                    Timber.d("Selected role: $selectedRole")
                    try {
                        googleSignInClient.signOut().addOnCompleteListener {
                            Timber.d("Previous account signed out")
                            val signInIntent = googleSignInClient.signInIntent
                            Timber.d("Launching Google Sign-In intent")
                            googleSignInLauncher.launch(signInIntent)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error launching Google Sign-In")
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                onSkipClick = null
            )
        } else {
            // WORKER selected - show skip option
            ProfessionalLoginScreen(
                role = selectedRole,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onGoogleSignInClick = {
                    Timber.d("Google Sign-In button clicked (Worker)")
                    Timber.d("Selected role: $selectedRole")
                    try {
                        googleSignInClient.signOut().addOnCompleteListener {
                            Timber.d("Previous account signed out")
                            val signInIntent = googleSignInClient.signInIntent
                            Timber.d("Launching Google Sign-In intent")
                            googleSignInLauncher.launch(signInIntent)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error launching Google Sign-In")
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                onSkipClick = {
                    Timber.d("Skip button clicked - Worker navigating to home screen")
                    // Save anonymous user info to local storage with WORKER role
                    scope.launch {
                        profileCompletionViewModel.saveUserInfoToLocalStorage(
                            email = "guest@worker.local",
                            name = "Guest Worker",
                            role = UserRole.WORKER
                        )
                        
                        Timber.d("Guest user info saved, navigating to WORKER_HOME")
                        navController.navigate(Routes.WORKER_HOME) {
                            popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun GoogleSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .height(58.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0xFF4285F4).copy(alpha = 0.3f)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF212529)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color(0xFF4285F4),
                    strokeWidth = 2.dp
                )
            } else {
                // Google Logo styling
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF4285F4), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = if (isLoading) "Signing in..." else "Continue with Google",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            )
        }
    }
}

@Composable
private fun ProfessionalLoginScreen(
    role: UserRole?,
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleSignInClick: () -> Unit,
    onSkipClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(0f, 1000f)
                )
            )
    ) {
        // Decorative circle - top right
        Box(
            modifier = Modifier
                .size(400.dp)
                .background(
                    color = Color(0xFF64748B).copy(alpha = 0.05f),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-100).dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo/Icon with gradient
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF3B82F6),
                                Color(0xFF8B5CF6)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "D",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 42.sp
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main Title
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    lineHeight = 40.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle with role
            Text(
                text = buildString {
                    append("Sign in as ")
                    append(role?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "User")
                    append(" to explore opportunities")
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                color = Color(0xFFA1A5AF),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Error Message with better styling
            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF7F1D1D).copy(alpha = 0.2f)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    color = Color(0xFFF87171).copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("!", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFCA5A5),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Google Sign-In Button with enhanced styling
            Button(
                onClick = onGoogleSignInClick,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp),
                        spotColor = Color(0xFF3B82F6).copy(alpha = 0.4f)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1E293B),
                    disabledContainerColor = Color(0xFFE2E8F0).copy(alpha = 0.3f),
                    disabledContentColor = Color(0xFFA1A5AF)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF3B82F6),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF4285F4), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = if (isLoading) "Signing in..." else "Continue with Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp
                    )
                }
            }

            // Skip Button for Workers with enhanced styling
            if (onSkipClick != null) {
                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onSkipClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .background(
                            color = Color(0xFF334155).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFCBD5E1)
                    )
                ) {
                    Text(
                        text = "Skip for now",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Info section with benefits/features
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF1E293B).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoRow(
                    icon = "✓",
                    text = "Secure Google authentication"
                )
                InfoRow(
                    icon = "✓",
                    text = "Access exclusive opportunities"
                )
                InfoRow(
                    icon = "✓",
                    text = "Quick profile setup"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Terms and Privacy text
            Text(
                text = "By signing in, you agree to our Terms of Service and Privacy Policy",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                ),
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: String,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = Color(0xFF10B981)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            color = Color(0xFFA1A5AF),
            modifier = Modifier.weight(1f)
        )
    }
}
