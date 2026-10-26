package com.example.dutype.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.dutype.auth.AuthManager
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.fg
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Airbnb-Style Logout Dialog
 * Matches the exact modal dialog layout from Airbnb:
 * - Rounded white card
 * - Top-right close icon
 * - "Are you sure you want to logout?" prompt
 * - Subtle hairline divider
 * - Cancel text on left, dark pill "Log out" button on right (#222222)
 */
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
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.bg()),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top row with close X icon at top right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, end = 10.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF222222).fg(),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Question prompt
                    Text(
                        text = "Are you sure you want to logout?",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF222222).fg()
                        ),
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 26.dp)
                    )

                    // Hairline divider
                    HorizontalDivider(
                        color = Color(0xFFF0F0F0).bd(),
                        thickness = 1.dp
                    )

                    // Bottom actions: Cancel on left, "Log out" button on right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cancel",
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF222222).fg()
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onDismiss)
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        )

                        Button(
                            onClick = {
                                onDismiss()
                                performLogout(
                                    navController = navController,
                                    authManager = authManager,
                                    profileCompletionViewModel = profileCompletionViewModel,
                                    scope = scope,
                                    userRole = userRole
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF222222),
                                contentColor = Color.White
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 24.dp,
                                vertical = 11.dp
                            )
                        ) {
                            Text(
                                text = "Log out",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
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

            // Step 2: Navigate IMMEDIATELY so the "Signing Out..." sheet
            // dismisses promptly. Heavy DataStore resets run in background;
            // they don't gate the UI transition because the destination
            // screen reads from a now-cleared session and renders fresh.
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

            // Step 3: Background reset of DataStore-backed profile setup state.
            // Fire-and-forget: failures are non-fatal and a fresh login will
            // re-derive the state from Firestore anyway.
            scope.launch {
                runCatching { profileCompletionViewModel.resetProfileSetupState() }
                    .onFailure { Timber.w(it, "Profile setup state reset failed (non-fatal)") }
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
