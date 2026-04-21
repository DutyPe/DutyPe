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
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Professional Logout Bottom Sheet
 * Provides comprehensive logout functionality with proper cleanup
 * 
 * Google Sign-In has been removed - authentication is now OTP-only
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalLogoutDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    navController: NavController,
    userRole: String = "User",
    authManager: AuthManager,
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
 * 
 * Following Google's recommended practices for Firebase Auth logout:
 * 1. Call FirebaseAuth.signOut() to clear Firebase session
 * 2. Clear all local cached data (preferences, DataStore, etc.)
 * 3. Remove FCM token to stop receiving notifications
 * 4. Navigate to login with cleared back stack
 * 
 * AuthManager.logout() is the CANONICAL logout implementation and handles:
 * - Firebase sign out
 * - FCM token removal
 * - Profile state reset
 * - Local preferences clearing
 */
private fun performLogout(
    navController: NavController,
    authManager: AuthManager,
    profileCompletionViewModel: ProfileCompletionViewModel,
    scope: CoroutineScope,
    userRole: String
) {
    scope.launch {
        try {
            Timber.d("🔐 Starting logout process...")
            
            // Step 1: Use AuthManager.logout() as the single source of truth
            // This handles: Firebase signOut, FCM token removal, local state clearing
            authManager.logout()
            Timber.d("✅ AuthManager logout completed (Firebase + FCM + local state)")
            
            // Step 2: Reset profile setup state via ViewModel (for DataStore updates)
            profileCompletionViewModel.resetProfileSetupState()
            Timber.d("✅ Profile setup state reset via ViewModel")
            
            // Step 3: Navigate to the role-specific home screen so the user
            // stays in the same surface (Worker stays in worker home, Employer
            // stays in employer home) instead of being kicked back to the role
            // selection screen.
            // P1-7: invalidate cached start destination so a stale route can't
            // be picked up by the next cold start.
            runCatching {
                com.example.dutype.navigation.StartDestinationCache.clear(navController.context)
            }
            val homeRoute = when (userRole.lowercase()) {
                "employer" -> com.example.dutype.navigation.Routes.EMPLOYER_HOME
                "worker" -> com.example.dutype.navigation.Routes.WORKER_HOME
                else -> com.example.dutype.navigation.Routes.SELECT_ROLE
            }
            navController.navigate(homeRoute) {
                // Clear the entire navigation stack via named-root pop (Google recommended)
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
            
            Timber.d("✅ Logout completed successfully")
            
        } catch (e: Exception) {
            // Even if there's an error, ensure we clear local data and navigate
            Timber.e(e, "❌ Logout error - forcing cleanup")
            
            // Force cleanup even on error
            try {
                authManager.logout()
                profileCompletionViewModel.resetProfileSetupState()
            } catch (cleanupError: Exception) {
                Timber.e(cleanupError, "❌ Cleanup error during forced logout")
            }
            
            // Always navigate away from authenticated screens
            val fallbackHome = when (userRole.lowercase()) {
                "employer" -> com.example.dutype.navigation.Routes.EMPLOYER_HOME
                "worker" -> com.example.dutype.navigation.Routes.WORKER_HOME
                else -> com.example.dutype.navigation.Routes.SELECT_ROLE
            }
            navController.navigate(fallbackHome) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}
