package com.example.dutype.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.dutype.R
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
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
    
    // Debug logging
    LaunchedEffect(skipRoleSelection, initialRole, showRoleSelection, selectedRole) {
        println("🔍 EnhancedLoginScreen Debug:")
        println("  skipRoleSelection: $skipRoleSelection")
        println("  initialRole: $initialRole")
        println("  showRoleSelection: $showRoleSelection")
        println("  selectedRole: $selectedRole")
        println("  Current state: ${if (showRoleSelection) "Showing role selection" else if (selectedRole != null) "Showing Google Sign-In" else "Unknown state"}")
    }
    
    // Handle navigation after successful Google Sign-In
    LaunchedEffect(shouldNavigate, navigationUser) {
        if (shouldNavigate && navigationUser != null) {
            val user = navigationUser!!
            println("🚀 Starting navigation process for user: ${user.email}")
            
            try {
                println("🔍 Checking if user has existing profile in Firebase...")
                println("🔍 User email: ${user.email}")
                println("🔍 User role: ${user.role}")
                
                // Use high-level approach to check if user already has a complete profile in Firebase
                val hasExistingProfile = profileCompletionViewModel.checkExistingProfileHighLevel(user.email, user.role)
                println("🔍 Has existing profile (high-level): $hasExistingProfile")
                
                if (hasExistingProfile) {
                    println("✅ Found existing profile, loading data and navigating to home...")
                    
                    // Load existing profile data into local state
                    profileCompletionViewModel.loadExistingProfileData(user.email, user.role)
                    
                    // Navigate directly to home screen
                    when (user.role) {
                        UserRole.WORKER -> {
                            println("📱 Navigating to WORKER_HOME (existing user)")
                            navController.navigate(Routes.WORKER_HOME) {
                                popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                            }
                        }
                        UserRole.EMPLOYER -> {
                            println("📱 Navigating to EMPLOYER_HOME (existing user)")
                            navController.navigate(Routes.EMPLOYER_HOME) {
                                popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                            }
                        }
                        else -> navController.navigate(Routes.SELECT_ROLE)
                    }
                } else {
                    println("❌ No existing profile found, proceeding with new user flow...")
                    
                    // Save user info for profile setup
                    profileCompletionViewModel.saveUserInfo(
                        user.email, 
                        user.fullName, 
                        user.role
                    )
                    
                    // Navigate to profile setup
                    println("🚀 Navigating to profile setup...")
                    when (user.role) {
                        UserRole.WORKER -> {
                            println("📱 Navigating to WORKER_ONBOARDING")
                            navController.navigate(Routes.WORKER_ONBOARDING) {
                                popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                            }
                        }
                        UserRole.EMPLOYER -> {
                            println("📱 Navigating to EMPLOYER_PROFILE_SETUP")
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
                println("❌ Error in profile setup check: ${e.message}")
                e.printStackTrace()
                // Fallback navigation - always go to onboarding for new users
                when (user.role) {
                    UserRole.WORKER -> {
                        println("🔄 Fallback: Navigating to WORKER_ONBOARDING")
                        navController.navigate(Routes.WORKER_ONBOARDING) {
                            popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                        }
                    }
                    UserRole.EMPLOYER -> {
                        println("🔄 Fallback: Navigating to EMPLOYER_PROFILE_SETUP")
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
    var phoneNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPhoneInput by remember { mutableStateOf(false) }
    
    val authManager = remember { AuthManager(context) }
    val scope = rememberCoroutineScope()
    
    // Google Sign-In client
    val googleSignInClient = remember {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("70012019193-5u30ftaapl9ra02r41ek7373nr6r49nj.apps.googleusercontent.com")
                .requestEmail()
                .build()
        )
    }
    
    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        println("📱 Google Sign-In result: ${result.resultCode}")
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                
                if (idToken != null && selectedRole != null) {
                    scope.launch {
                        try {
                            isLoading = true
                            errorMessage = null
                            
                            println("🚀 Starting Google Sign-In process...")
                            googleSignInManagerInstance.signInWithGoogle(
                                idToken = idToken,
                                selectedRole = selectedRole!!
                            ).collect { signInResult ->
                                signInResult.fold(
                                    onSuccess = { user ->
                                        println("✅ Google Sign-In successful! User: ${user.email}")
                                        Toast.makeText(context, "Welcome ${user.fullName}!", Toast.LENGTH_LONG).show()
                                        authManager.saveUser(user)
                                        authManager.setLoggedIn(true)
                                        isLoading = false
                                        
                                        // Trigger navigation using LaunchedEffect
                                        shouldNavigate = true
                                        navigationUser = user
                                    },
                                    onFailure = { exception ->
                                        isLoading = false
                                        errorMessage = exception.message ?: "Sign-in failed"
                                        Toast.makeText(context, "Sign-in failed: ${exception.message}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = e.message ?: "An error occurred during sign-in"
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    errorMessage = "Failed to get ID token from Google"
                    Toast.makeText(context, "Failed to get ID token from Google", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                isLoading = false
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
        } else {
            // Google Sign-In was cancelled or failed
            println("❌ Google Sign-In was cancelled or failed. Result code: ${result.resultCode}")
            when (result.resultCode) {
                0 -> {
                    Toast.makeText(context, "Sign-in was cancelled", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(context, "Sign-in failed. Please try again.", Toast.LENGTH_LONG).show()
                }
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
                println("✅ Role selected: $role")
                selectedRole = role
                showRoleSelection = false
                // Now show Google Sign-In screen
            }
        )
    } else if (selectedRole != null) {
        // Show Google Sign-In screen after role selection
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF6C63FF),
                            Color(0xFF4CAF50)
                        )
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo and Title
            Image(
                painter = painterResource(id = R.drawable.dutype),
                contentDescription = "DutyPe Logo",
                modifier = Modifier.size(240.dp), // Updated to match splash screen size
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "dutype",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Your Gateway to Part-Time Opportunities",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Google Sign-In Button (shows after role selection)
            AnimatedVisibility(
                visible = true, // Always show since we're in the Google Sign-In screen
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Column {
                    Text(
                        text = "Continue as ${selectedRole?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    GoogleSignInButton(
                        onClick = {
                            val signInIntent = googleSignInClient.signInIntent
                            googleSignInLauncher.launch(signInIntent)
                        },
                        isLoading = isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Error Message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = error,
                        color = Color.Red,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Info Text
            Text(
                text = "By signing in, you agree to our Terms of Service and Privacy Policy",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
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
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.Black
                )
            } else {
                // Google Logo (you can add a proper Google logo here)
                Text(
                    text = "G",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = if (isLoading) "Signing in..." else "Continue with Google",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

