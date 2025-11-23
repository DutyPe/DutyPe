package com.example.dutype.auth

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch
import timber.log.Timber
import java.security.MessageDigest
import java.util.UUID

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
                    .setFilterByAuthorizedAccounts(false) // Allow account selection
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .setAutoSelectEnabled(true) // Auto-select for returning users
                    .setNonce(nonce)
                    .build()
                
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                
                Timber.d("Requesting Google credentials...")
                
                try {
                    val result = credentialManager.getCredential(
                        request = request,
                        context = context
                    )
                    
                    handleSignInResult(result)
                    
                } catch (e: GetCredentialException) {
                    isLoading = false
                    Timber.e(e, "GetCredentialException")
                    errorMessage = when {
                        e.message?.contains("cancelled", ignoreCase = true) == true -> {
                            "Sign-in was cancelled"
                        }
                        e.message?.contains("network", ignoreCase = true) == true -> {
                            "Network error. Please check your connection"
                        }
                        else -> "Google Sign-In failed: ${e.message}"
                    }
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "An error occurred during sign-in"
                Timber.e(e, "Exception during Credential Manager sign-in")
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
                onPhoneLoginClick = {
                    Timber.d("Phone Login button clicked - Navigating to phone login screen (Employer)")
                    navController.navigate(Routes.LOGIN_BOTTOM_SHEET)
                }
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
                onPhoneLoginClick = {
                    Timber.d("Phone Login button clicked - Navigating to phone login screen (Worker)")
                    navController.navigate(Routes.LOGIN_BOTTOM_SHEET)
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

@Composable
private fun ProfessionalLoginScreen(
    role: UserRole?,
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleSignInClick: () -> Unit,
    onSkipClick: (() -> Unit)? = null,
    onPhoneLoginClick: (() -> Unit)? = null
) {
    var agreeToTerms by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main Title - Clean and simple (moved to top without logo)
            Text(
                text = "Welcome to DutyPe",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp
                ),
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle - Concise
            Text(
                text = "Your opportunities await",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.sp
                ),
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(50.dp))

            // Error Message - Minimal styling
            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFEE2E2)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(
                                    color = Color(0xFEF2F2),
                                    shape = RoundedCornerShape(100.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("!", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text(
                            text = errorMessage,
                            color = Color(0xDC2626),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Privacy Agreement Checkbox - MOVED BEFORE BUTTONS (MANDATORY)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .background(
                        color = if (agreeToTerms) Color(0xFFF0F7FF) else Color(0xFFFFF9F5),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Checkbox(
                    checked = agreeToTerms,
                    onCheckedChange = { agreeToTerms = it },
                    modifier = Modifier.size(20.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF4285F4),
                        uncheckedColor = Color(0xFFD1D5DB)
                    )
                )
                Text(
                    text = "I agree to the Terms of Service and Privacy Policy",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (agreeToTerms) Color(0xFF4285F4) else Color(0xFF4B5563)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Sign-In Button - Enabled only if checkbox is checked
            Button(
                onClick = onGoogleSignInClick,
                enabled = !isLoading && agreeToTerms,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1F2937),
                    disabledContainerColor = Color(0xFFF3F4F6),
                    disabledContentColor = Color(0xFF9CA3AF)
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF3B82F6),
                            strokeWidth = 2.dp
                        )
                    } else {
                        // Official Google logo image (preserve aspect ratio)
                        Image(
                            painter = painterResource(id = R.drawable.google),
                            contentDescription = "Google",
                            modifier = Modifier
                                .size(18.dp)
                                .aspectRatio(1f),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = if (isLoading) "Signing in..." else "Continue with Google",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Phone Login Button - Enabled only if checkbox is checked
            if (onPhoneLoginClick != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onPhoneLoginClick,
                    enabled = !isLoading && agreeToTerms,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1F2937),
                        disabledContainerColor = Color(0xFFF3F4F6),
                        disabledContentColor = Color(0xFF9CA3AF)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!isLoading) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Phone",
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF1F2937)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Text(
                            text = "Continue with Mobile",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Skip / Continue as Guest Button - Enabled only if checkbox is checked
            if (onSkipClick != null) {
                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onSkipClick,
                    enabled = agreeToTerms,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF6B7280),
                        disabledContentColor = Color(0xFFD1D5DB)
                    )
                ) {
                    Text(
                        text = "Continue as guest",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Disabled button info message
            if (!agreeToTerms) {
                Text(
                    text = "✓ Please accept the terms to continue",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = Color(0xFFEA580C),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }
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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            color = Color(0xFF10B981)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp
            ),
            color = Color(0xFF4B5563),
            modifier = Modifier.weight(1f)
        )
    }
}
