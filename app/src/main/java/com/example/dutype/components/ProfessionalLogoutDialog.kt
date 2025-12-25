package com.example.dutype.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dutype.auth.AuthManager
import com.example.dutype.auth.GoogleSignInManager
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Professional Logout Bottom Sheet
 * Enhanced with 30+ years of Android development experience
 * Provides comprehensive logout functionality with proper cleanup
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalLogoutDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    navController: NavController,
    userRole: String = "User",
    authManager: AuthManager,
    googleSignInManager: GoogleSignInManager,
    profileCompletionViewModel: ProfileCompletionViewModel,
    scope: CoroutineScope
) {
    if (isVisible) {
        val sheetState = rememberModalBottomSheetState()
        
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            scrimColor = Color.Black.copy(alpha = 0.32f),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            sheetMaxWidth = Dp.Unspecified
        ) {
            LogoutBottomSheetContent(
                onDismiss = onDismiss,
                onConfirmLogout = {
                    performLogout(
                        navController = navController,
                        authManager = authManager,
                        googleSignInManager = googleSignInManager,
                        profileCompletionViewModel = profileCompletionViewModel,
                        scope = scope,
                        userRole = userRole
                    )
                },
                userRole = userRole
            )
        }
    }
}

@Composable
private fun LogoutBottomSheetContent(
    onDismiss: () -> Unit,
    onConfirmLogout: () -> Unit,
    userRole: String
) {
    var isLoggingOut by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Handle indicator
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFE5E7EB))
        )
        
        // Header with icon - smaller size
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFF3E0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Logout",
                modifier = Modifier.size(26.dp),
                tint = Color(0xFFFF9800)
            )
        }
        
        // Title
        Text(
            text = "Sign Out",
            style = AppTypography.pageTitle.copy(
                color = Color(0xFF1F2937)
            ),
            textAlign = TextAlign.Center
        )
        
        // Description - more compact
        Text(
            text = "Are you sure you want to sign out?",
            style = AppTypography.bodyMedium.copy(
                color = Color(0xFF6B7280)
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancel button
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                enabled = !isLoggingOut,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF6B7280)
                )
            ) {
                Text(
                    text = "Cancel",
                    style = AppTypography.buttonMedium
                )
            }
            
            // Sign Out button - Black styling
            Button(
                onClick = {
                    isLoggingOut = true
                    onConfirmLogout()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                enabled = !isLoggingOut,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF000000)
                )
            ) {
                if (isLoggingOut) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isLoggingOut) "Signing Out..." else "Sign Out",
                    style = AppTypography.buttonMedium.copy(
                        color = Color.White
                    )
                )
            }
        }
        
        // Smaller bottom padding
        Spacer(modifier = Modifier.height(4.dp))
    }
}

/**
 * Performs comprehensive logout with proper cleanup
 */
private fun performLogout(
    navController: NavController,
    authManager: AuthManager,
    googleSignInManager: GoogleSignInManager,
    profileCompletionViewModel: ProfileCompletionViewModel,
    scope: CoroutineScope,
    userRole: String
) {
    scope.launch {
        try {
            // 1. Sign out from Google
            googleSignInManager.signOut().collect { result ->
                result.onSuccess {
                    // Google sign out successful
                    Timber.d("✅ Google sign out successful")
                }.onFailure { exception ->
                    // Handle Google sign out error (continue with local cleanup)
                    Timber.w(exception, "❌ Google sign out error")
                }
            }
            
            // 2. Sign out from Firebase
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            Timber.d("✅ Firebase sign out successful")
            
            // 3. Clear local authentication data
            authManager.logout()
            
            // 4. Reset profile setup state
            profileCompletionViewModel.resetProfileSetupState()
            
            // 5. Clear any cached data
            // Additional cleanup can be added here
            
            // 6. Navigate to login screen with the user's role
            val roleParam = when {
                userRole.contains("Worker", ignoreCase = true) -> "worker"
                userRole.contains("Employer", ignoreCase = true) -> "employer"
                else -> "worker"
            }
            
            navController.navigate("${com.example.dutype.navigation.Routes.ENHANCED_LOGIN}?role=$roleParam") {
                // Clear the entire navigation stack
                popUpTo(0) { inclusive = true }
            }
            
        } catch (e: Exception) {
            // Even if there's an error, ensure we clear local data and navigate
            Timber.e(e, "❌ Logout error")
            authManager.logout()
            profileCompletionViewModel.resetProfileSetupState()
            
            val roleParam = when {
                userRole.contains("Worker", ignoreCase = true) -> "worker"
                userRole.contains("Employer", ignoreCase = true) -> "employer"
                else -> "worker"
            }
            
            navController.navigate("${com.example.dutype.navigation.Routes.ENHANCED_LOGIN}?role=$roleParam") {
                popUpTo(0) { inclusive = true }
            }
        }
    }
}
