package com.example.partimes.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.partimes.models.UserRole
import com.example.partimes.navigation.Routes
import com.example.partimes.common.chat.SelectRoleScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EnhancedLoginScreen(
    navController: NavController,
    googleSignInManager: GoogleSignInManager? = null
) {
    val context = LocalContext.current
    val googleSignInManagerInstance = googleSignInManager ?: remember { GoogleSignInManager(context) }
    var showRoleSelection by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
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
                .requestIdToken("396636512278-0sp9t6nnusg7iumho5nhpsnrq4fbceof.apps.googleusercontent.com")
                .requestEmail()
                .requestServerAuthCode("396636512278-0sp9t6nnusg7iumho5nhpsnrq4fbceof.apps.googleusercontent.com")
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
                                selectedRole = selectedRole!!,
                                phoneNumber = phoneNumber.ifEmpty { null }
                            ).collect { signInResult ->
                                signInResult.fold(
                                    onSuccess = { user ->
                                        println("✅ Google Sign-In successful! User: ${user.email}")
                                        Toast.makeText(context, "Welcome ${user.fullName}!", Toast.LENGTH_LONG).show()
                                        authManager.saveUser(user)
                                        authManager.setLoggedIn(true)
                                        isLoading = false
                                        
                                        // Navigate based on profile completion and role
                                        if (!user.isProfileComplete) {
                                            // Show onboarding for incomplete profiles
                                            when (user.role) {
                                                UserRole.WORKER -> navController.navigate(Routes.WORKER_ONBOARDING) {
                                                    popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                                                }
                                                UserRole.EMPLOYER -> navController.navigate(Routes.EMPLOYER_ONBOARDING) {
                                                    popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                                                }
                                                else -> navController.navigate(Routes.SELECT_ROLE)
                                            }
                                        } else {
                                            // Profile complete, go to home
                                            when (user.role) {
                                                UserRole.WORKER -> navController.navigate(Routes.WORKER_HOME) {
                                                    popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                                                }
                                                UserRole.EMPLOYER -> navController.navigate(Routes.EMPLOYER_HOME) {
                                                    popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                                                }
                                                else -> navController.navigate(Routes.SELECT_ROLE)
                                            }
                                        }
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
                showPhoneInput = true
                // Automatically start Google Sign-In after role selection
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        )
    } else {
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
            // App Logo placeholder
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PT",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "ParTimes",
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
            
            // Google Sign-In Button (only show when no role is selected)
            AnimatedVisibility(
                visible = selectedRole == null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                GoogleSignInButton(
                    onClick = {
                        showRoleSelection = true
                    },
                    isLoading = isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            
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

