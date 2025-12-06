package com.example.dutype.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.common.chat.SelectRoleScreen
import com.example.dutype.models.User
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.DividerRow
import com.example.dutype.utils.FirestoreUtils
import com.example.dutype.utils.ValidationUtils
import com.example.dutype.viewmodels.OtpViewModel
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import timber.log.Timber
import java.security.MessageDigest
import java.util.UUID

@Composable
fun EnhancedLoginScreen(
    navController: NavController,
    googleSignInManager: GoogleSignInManager? = null,
    skipRoleSelection: Boolean = false,
    initialRole: String = "WORKER",
    otpViewModel: OtpViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
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
                Timber.d("=============================================================")
                Timber.d("DATABASE EXISTENCE CHECK")
                Timber.d("=============================================================")
                Timber.d("User Email: ${user.email}")
                Timber.d("User Role: ${user.role}")
                Timber.d("Checking if user data exists in database...")
                
                // Use high-level approach to check if user already has a complete profile in Firebase
                val hasExistingProfile = profileCompletionViewModel.checkExistingProfileHighLevel(user.email, user.role)
                Timber.d("=============================================================")
                Timber.d("RESULT: User data exists in database: $hasExistingProfile")
                Timber.d("=============================================================")
                
                if (hasExistingProfile) {
                    Timber.d("Found existing profile, loading data and navigating to home...")
                    Timber.d("User data status: COMPLETE - Existing user detected")
                    Timber.d("Next action: Loading profile data and navigating to HOME SCREEN")
                    
                    // Load existing profile data into local state
                    profileCompletionViewModel.loadExistingProfileData(user.email, user.role)
                    
                    // Save authentication method for consistency
                    profileCompletionViewModel.saveAuthMethod("GOOGLE")
                    
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
                                                val hasCompanyDetails = data["companyName"] != null && (data["companyName"] as? String)?.isNotBlank() == true
                                                
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
                    
                    // Save authentication method for conditional field rendering
                    profileCompletionViewModel.saveAuthMethod("GOOGLE")
                    
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
                navigationUser = null
            }
        }
    }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val authManager = remember { AuthManager(context) }
    
    // Credential Manager (New approach)
    val credentialManager = remember { CredentialManager.create(context) }
    
    // Generate nonce for security
    val generateNonce: () -> String = {
        val ranNonce = UUID.randomUUID().toString()
        val bytes = ranNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        digest.fold("") { str, it -> str + "%02x".format(it) }
    }
    
    // Handle credential response
    val handleSignInResult: (GetCredentialResponse) -> Unit = { result ->
        val credential = result.credential
        
        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        
                        Timber.d("Got Google ID token from Credential Manager")
                        Timber.d("User: ${googleIdTokenCredential.id}")
                        
                        // Sign in with Firebase using the ID token
                        scope.launch {
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
                                        
                                        // Trigger navigation
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
                        }
                        
                    } catch (e: GoogleIdTokenParsingException) {
                        isLoading = false
                        errorMessage = "Invalid Google credentials"
                        Timber.e(e, "GoogleIdTokenParsingException")
                        Toast.makeText(context, "Invalid Google credentials", Toast.LENGTH_LONG).show()
                    }
                } else {
                    isLoading = false
                    errorMessage = "Unexpected credential type"
                    Timber.e("Unexpected credential type: ${credential.type}")
                    Toast.makeText(context, "Unexpected credential type", Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                isLoading = false
                errorMessage = "Unexpected credential type"
                Timber.e("Unexpected credential class: ${credential::class.java.name}")
                Toast.makeText(context, "Unexpected credential type", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    // Google Sign-In with Credential Manager
    val handleGoogleSignIn: suspend () -> Unit = {
        if (selectedRole == null) {
            errorMessage = "Please select a role first"
        } else {
            try {
                isLoading = true
                errorMessage = null
                
                val nonce = generateNonce()
                Timber.d("Generated nonce for Google Sign-In")
                
                // Build GetGoogleIdOption
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true) // Allow account selection
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .setAutoSelectEnabled(false) // Disable auto-select for Play Store issues
                    .setNonce(nonce)
                    .build()
                
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                
                Timber.d("Requesting Google credentials...")
                Timber.d("Server Client ID: ${context.getString(R.string.default_web_client_id)}")
                
                try {
                    val result = credentialManager.getCredential(
                        request = request,
                        context = context
                    )
                    
                    handleSignInResult(result)
                    
                } catch (e: GetCredentialException) {
                    isLoading = false
                    Timber.e(e, "GetCredentialException: ${e.message}")
                    Timber.e("Error details: ${e.errorMessage ?: "No error message provided"}")
                    
                    errorMessage = when {
                        e.message?.contains("no_credentials_available", ignoreCase = true) == true -> {
                            "❌ No Google credentials available. " +
                            "\n\n🔧 This usually means:" +
                            "\n• SHA-1 fingerprint mismatch" +
                            "\n• Google Play Services not configured" +
                            "\n\n📝 Please contact support with details:" +
                            "\n${e.errorMessage ?: e.message}"
                        }
                        e.message?.contains("cancelled", ignoreCase = true) == true -> {
                            "Sign-in was cancelled"
                        }
                        e.message?.contains("network", ignoreCase = true) == true -> {
                            "Network error. Please check your connection"
                        }
                        e.message?.contains("invalid_request", ignoreCase = true) == true -> {
                            "⚠️ Invalid request. This might be a configuration issue.\n\nPlease try:\n1. Clearing app cache\n2. Updating Google Play Services\n3. Trying Phone Sign-In instead"
                        }
                        e.message?.contains("client_mismatch", ignoreCase = true) == true -> {
                            "⚠️ Client ID mismatch.\n\nPlease ensure you've added the correct SHA-1 fingerprint in Google Cloud Console."
                        }
                        else -> {
                            "Google Sign-In failed: ${e.errorMessage ?: e.message}\n\n💡 Try Phone Sign-In instead"
                        }
                    }
                    Timber.e("Full error message: $errorMessage")
                    Toast.makeText(context, "Sign-in failed", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                isLoading = false
                Timber.e(e, "Exception during Credential Manager sign-in")
                errorMessage = when (e) {
                    is SecurityException -> {
                        "🔒 Security error: ${e.message}\n\nTry clearing app cache and Google Play Services cache"
                    }
                    else -> e.message ?: "An error occurred during sign-in"
                }
                Timber.e(e, "Exception: $errorMessage")
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
        // Both WORKER and EMPLOYER can skip now
        if (selectedRole == UserRole.EMPLOYER) {
            // Employer - show professional login screen with skip option
            ProfessionalLoginScreen(
                role = selectedRole,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onGoogleSignInClick = {
                    Timber.d("Google Sign-In button clicked (Employer)")
                    Timber.d("Selected role: $selectedRole")
                    scope.launch {
                        handleGoogleSignIn()
                    }
                },
                onSkipClick = {
                    Timber.d("Skip button clicked - Employer navigating to home screen")
                    // Save anonymous user info to local storage with EMPLOYER role
                    scope.launch {
                        profileCompletionViewModel.saveUserInfoToLocalStorage(
                            email = "guest@employer.local",
                            name = "Guest Employer",
                            role = UserRole.EMPLOYER
                        )
                        
                        Timber.d("Guest employer info saved, navigating to EMPLOYER_HOME")
                        navController.navigate(Routes.EMPLOYER_HOME) {
                            popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                        }
                    }
                },
                onPhoneLoginClick = null,
                otpViewModel = otpViewModel,
                profileCompletionViewModel = profileCompletionViewModel,
                navController = navController
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
                    scope.launch {
                        handleGoogleSignIn()
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
                },
                onPhoneLoginClick = null,
                otpViewModel = otpViewModel,
                profileCompletionViewModel = profileCompletionViewModel,
                navController = navController
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
                // Google Logo image (preserve aspect ratio)
                Image(
                    painter = painterResource(id = R.drawable.google),
                    contentDescription = "Google",
                    modifier = Modifier
                        .size(24.dp)
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ProfessionalLoginScreen(
    role: UserRole?,
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleSignInClick: () -> Unit,
    onSkipClick: (() -> Unit)? = null,
    onPhoneLoginClick: (() -> Unit)? = null,
    otpViewModel: OtpViewModel,
    profileCompletionViewModel: ProfileCompletionViewModel,
    navController: NavController
) {
    var phoneNumber by remember { mutableStateOf("") }
    var otpValue by remember { mutableStateOf("") }
    val selectedCountryCode = "+91"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val otpState by otpViewModel.otpState.collectAsState()

    // Handle OTP verification success
    LaunchedEffect(otpState.otpVerified) {
        if (otpState.otpVerified) {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val userId = currentUser.uid
                    val phoneNum = currentUser.phoneNumber ?: ""
                    
                    val existingUserData = FirestoreUtils.checkUserExistsByPhoneNumber(phoneNum)
                    
                    if (existingUserData != null) {
                        val userRole = existingUserData["role"] as? String
                        if (userRole != null) {
                            try {
                                val parsedRole = UserRole.valueOf(userRole.uppercase())
                                profileCompletionViewModel.markProfileComplete(parsedRole)
                                profileCompletionViewModel.markProfileSetupAsShown(parsedRole)
                            } catch (e: Exception) {
                                Timber.w(e, "Error parsing role")
                            }
                        }
                        navController.navigate(Routes.SELECT_ROLE) {
                            popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.PROFILE_SETUP) {
                            popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                        }
                    }
                } else {
                    navController.navigate(Routes.SELECT_ROLE) {
                        popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking profile")
                navController.navigate(Routes.PROFILE_SETUP) {
                    popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                }
            }
            otpViewModel.resetState()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            AnimatedContent(
                targetState = !otpState.otpSent,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { if (targetState) -400 else 400 },
                        animationSpec = tween(450, easing = EaseOutCubic)
                    ) + fadeIn(animationSpec = tween(450)) with
                    slideOutHorizontally(
                        targetOffsetX = { if (targetState) 400 else -400 },
                        animationSpec = tween(450, easing = EaseInCubic)
                    ) + fadeOut(animationSpec = tween(450))
                }
            ) { isPhoneNumberScreen ->
                if (isPhoneNumberScreen) {
                    PhoneInputSection(
                        phoneNumber = phoneNumber,
                        onPhoneNumberChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 10) {
                                phoneNumber = newValue
                            }
                        },
                        selectedCountryCode = selectedCountryCode,
                        otpState = otpState,
                        onContinueClick = {
                            val fullPhoneNumber = selectedCountryCode + phoneNumber
                            scope.launch {
                                try {
                                    profileCompletionViewModel.saveAuthMethod("PHONE_OTP")
                                    profileCompletionViewModel.savePhoneNumber(fullPhoneNumber)
                                } catch (e: Exception) {
                                    Timber.w(e, "Could not save auth method")
                                }
                            }
                            otpViewModel.sendOtp(fullPhoneNumber, context)
                        },
                        onGoogleSignInClick = onGoogleSignInClick,
                        onSkipClick = onSkipClick,
                        errorMessage = errorMessage,
                        isLoading = isLoading
                    )
                } else {
                    OtpInputSection(
                        otpValue = otpValue,
                        onOtpChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 6) {
                                otpValue = newValue
                            }
                        },
                        phoneNumber = phoneNumber,
                        otpState = otpState,
                        onVerifyClick = {
                            otpViewModel.verifyOtp(otpValue, context)
                        },
                        onResendClick = {
                            val fullPhoneNumber = selectedCountryCode + phoneNumber
                            otpViewModel.resendOtp(fullPhoneNumber, context)
                        },
                        onBackClick = {
                            otpViewModel.resetState()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneInputSection(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    selectedCountryCode: String,
    otpState: com.example.dutype.viewmodels.OtpState,
    onContinueClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onSkipClick: (() -> Unit)?,
    errorMessage: String?,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 13.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Enter your mobile number",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp
            ),
            color = Color.Black,
            textAlign = TextAlign.Start
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = Color(0xFFDC2626),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { /* Country picker */ },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier
                    .width(66.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(text = "🇮🇳", fontSize = 18.sp)
            }

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                placeholder = { Text(text = "9876543210", fontSize = 15.sp, color = Color(0xFF9CA3AF)) },
                leadingIcon = {
                    Text(
                        text = selectedCountryCode,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                trailingIcon = {
                    androidx.compose.material3.Icon(Icons.Filled.Person, contentDescription = "Profile")
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                singleLine = true,
                shape = RoundedCornerShape(6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF111827),
                    unfocusedBorderColor = Color(0xFF9CA3AF),
                    cursorColor = Color(0xFF111827),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val buttonEnabled = ValidationUtils.isValidIndianPhoneNumber(phoneNumber) && !otpState.isLoading

        Button(
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF111111),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFEEEEEE),
                disabledContentColor = Color(0xFF444444)
            ),
            shape = RoundedCornerShape(6.dp),
            enabled = buttonEnabled
        ) {
            if (otpState.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text("Continue", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        DividerRow()

        Spacer(modifier = Modifier.height(14.dp))

        // Google button
        Button(
            onClick = onGoogleSignInClick,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEEEEEE),
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(6.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.google),
                contentDescription = "Google",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text("Continue with Google", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }

        if (onSkipClick != null) {
            Spacer(modifier = Modifier.height(10.dp))

            // Use regular Button (not TextButton) so padding/minHeight match exactly
            Button(
                onClick = onSkipClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEEEEEE),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(6.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                contentPadding = PaddingValues(horizontal = 16.dp) // optional: adjust horizontal padding
            ) {
                Text(
                    "Continue as guest",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF444444)
                )
            }
        }


        Spacer(modifier = Modifier.height(13.dp))

        Text(
            text = buildAnnotatedString {
                append("By clicking continue, you agree to our ")

                // Terms of Service (bold + darker)
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF000000) // black
                    )
                ) {
                    append("Terms of Service")
                }

                append(" and ")

                // Privacy Policy (bold + darker)
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF000000) // black
                    )
                ) {
                    append("Privacy Policy")
                }
            },
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = Color(0xFF6B6B6B) // default text color
            ),
            textAlign = TextAlign.Start
        )



        AnimatedVisibility(
            visible = otpState.error != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                shape = RoundedCornerShape(6.dp),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = SolidColor(Color(0xFFFECACA)))
            ) {
                Text(
                    text = otpState.error ?: "",
                    color = Color(0xFFDC2626),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun OtpInputSection(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    phoneNumber: String,
    otpState: com.example.dutype.viewmodels.OtpState,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 13.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Welcome to DutyPe",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = Color(0xFF0F172A),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = buildAnnotatedString {
                append("Enter the 6-digit code sent via SMS at ")

                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF000000) // darker color for visibility
                    )
                ) {
                    append(phoneNumber)
                }

                append(".")
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = Color(0xFF64748B),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )


        Spacer(modifier = Modifier.height(3.dp))

        TextButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text(
                text = "Change your mobile number?",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ),
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(23.dp))

        OtpInputBoxes(
            otpValue = otpValue,
            onOtpChange = onOtpChange,
            digitCount = 6
        )

        Spacer(modifier = Modifier.height(18.dp))

        val otpButtonEnabled = otpValue.length == 6 && !otpState.isLoading
        
        // Timer state for 60 seconds resend cooldown
        var remainingSeconds by remember { mutableStateOf(60) }
        var timerActive by remember { mutableStateOf(true) }
        
        LaunchedEffect(timerActive) {
            while (timerActive && remainingSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                remainingSeconds--
                if (remainingSeconds == 0) {
                    timerActive = false
                }
            }
        }
        
        // Reset timer when OTP is successfully sent
        LaunchedEffect(otpState.otpSent) {
            if (otpState.otpSent && !timerActive && remainingSeconds == 0) {
                remainingSeconds = 60
                timerActive = true
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var isResending by remember { mutableStateOf(false) }
            val rotation = remember { androidx.compose.animation.core.Animatable(0f) }
            
            LaunchedEffect(otpState.isLoading) {
                if (otpState.isLoading) {
                    isResending = true
                    while (isResending) {
                        rotation.animateTo(
                            360f,
                            animationSpec = tween(1000, easing = androidx.compose.animation.core.LinearEasing)
                        )
                        rotation.snapTo(0f)
                    }
                } else {
                    isResending = false
                    rotation.snapTo(0f)
                }
            }
            
            // Resend button with timer and hint
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(70.dp)
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.IconButton(
                        onClick = {
                            if (timerActive && remainingSeconds > 0) return@IconButton
                            isResending = true
                            remainingSeconds = 60
                            timerActive = true
                            onResendClick()
                        },
                        modifier = Modifier.size(53.dp),
                        enabled = !isResending && (remainingSeconds == 0 || !timerActive)
                    ) {
                        androidx.compose.material3.Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_revert),
                            contentDescription = "Resend OTP",
                            tint = if (timerActive && remainingSeconds > 0) Color(0xFFCCCCCC) else Color.Black,
                            modifier = Modifier.rotate(rotation.value)
                        )
                    }
                    
                    // Timer display
                    if (timerActive && remainingSeconds > 0) {
                        Text(
                            text = remainingSeconds.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = Color(0xFF666666),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 2.dp, bottom = 1.dp)
                        )
                    }
                }
                
                // Resend hint text
                Text(
                    text = "Resend OTP",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = if (timerActive && remainingSeconds > 0) Color(0xFFCCCCCC) else Color(0xFF666666)
                    ),
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            Button(
                onClick = onVerifyClick,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111111),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE5E7EB),
                    disabledContentColor = Color(0xFF9CA3AF)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = otpButtonEnabled
            ) {
                if (otpState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text("Next", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        AnimatedVisibility(
            visible = otpState.error != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = SolidColor(Color(0xFFEFCACA)))
            ) {
                Text(
                    text = otpState.error ?: "",
                    color = Color(0xFFDC2626),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun OtpInputBoxes(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    digitCount: Int = 6
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isFocused = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            repeat(digitCount) { index ->
                val isFocusedIndex = index == otpValue.length && isFocused
                val isFilledIndex = index < otpValue.length
                val digit = otpValue.getOrNull(index)?.toString() ?: ""
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (isFilledIndex) Color(0xFF10B98130) else Color.White,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = when {
                                isFocusedIndex -> Color.Black
                                isFilledIndex -> Color(0xFF10B981)
                                else -> Color(0xFFE5E7EB)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color.Black
                    )
                }
            }
        }
        
        // Invisible text field for input with autofill support
        BasicTextField(
            value = otpValue,
            onValueChange = { newValue ->
                if (newValue.length <= digitCount && newValue.all { it.isDigit() }) {
                    onOtpChange(newValue)
                    isFocused = true
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable { isFocused = true }
                .alpha(0f),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            decorationBox = { innerTextField ->
                innerTextField()
            }
        )
    }
}
