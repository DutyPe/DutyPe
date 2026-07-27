package com.example.dutype.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.auth.AuthManager
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.utils.LocaleHelper
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Account Deletion Bottom Sheet / Dialog
 * Compliance with Google Play User Data Policy 4.8
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDeletionDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    navController: NavController,
    userRole: String = "User",
    authManager: AuthManager,
    profileCompletionViewModel: ProfileCompletionViewModel,
    scope: CoroutineScope
) {
    if (isVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val context = LocalContext.current
        val isTelugu = LocaleHelper.getLanguage(context) == LocaleHelper.LANGUAGE_TELUGU

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            scrimColor = Color.Black.copy(alpha = 0.45f),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            sheetMaxWidth = Dp.Unspecified
        ) {
            AccountDeletionContent(
                isTelugu = isTelugu,
                onDismiss = onDismiss,
                onOpenWebDeletion = {
                    val webUrl = "https://dutype.in/delete-account"
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Timber.e(e, "Error opening web deletion link")
                        try {
                            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.dutype.in/delete-account")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(fallbackIntent)
                        } catch (ex: Exception) {
                            Timber.e(ex, "Fallback web browser launch failed")
                        }
                    }
                },
                onConfirmDeletion = {
                    onDismiss()
                    performAccountDeletion(
                        navController = navController,
                        authManager = authManager,
                        profileCompletionViewModel = profileCompletionViewModel,
                        scope = scope,
                        userRole = userRole
                    )
                }
            )
        }
    }
}

@Composable
private fun AccountDeletionContent(
    isTelugu: Boolean,
    onDismiss: () -> Unit,
    onOpenWebDeletion: () -> Unit,
    onConfirmDeletion: () -> Unit
) {
    var isDeleting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Red Icon Circle
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFFFEE2E2)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = "Delete Account",
                modifier = Modifier.size(32.dp),
                tint = Color(0xFFDC2626)
            )
        }

        // Title
        Text(
            text = if (isTelugu) "ఖాతా శాశ్వతంగా తొలగించు" else "Delete Account Permanently",
            style = AppTypography.pageTitle.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            ),
            textAlign = TextAlign.Center
        )

        // Detailed Description
        Text(
            text = if (isTelugu)
                "మీ ఖాతాను తొలగించడం వల్ల మీ వ్యక్తిగత సమాచారం, జాబ్ అప్లికేషన్లు, సేవ్ చేసిన వివరాలు మరియు చరిత్ర శాశ్వతంగా తొలగించబడతాయి. ఈ చర్యను వెనక్కి తీసుకోలేము."
            else
                "Deleting your account will permanently remove your profile data, job applications, saved listings, and application history from DutyPe. This action cannot be undone.",
            style = AppTypography.bodyMedium.copy(
                color = Color(0xFF475569),
                lineHeight = 20.sp
            ),
            textAlign = TextAlign.Center
        )

        // Web Deletion Request Clean Text Link (No box around it, fully clickable)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onOpenWebDeletion() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF2563EB)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isTelugu) "వెబ్ తొలగింపు పేజీ (dutype.in/delete-account)" else "Web Deletion Page (dutype.in/delete-account)",
                style = AppTypography.buttonMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2563EB),
                    textDecoration = TextDecoration.Underline
                )
            )
        }

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
                    .height(48.dp),
                enabled = !isDeleting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF475569)
                )
            ) {
                Text(
                    text = if (isTelugu) "రద్దు చేయి" else "Cancel",
                    style = AppTypography.buttonMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Permanently Delete Button - Red
            Button(
                onClick = {
                    isDeleting = true
                    onConfirmDeletion()
                },
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp),
                enabled = !isDeleting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626)
                )
            ) {
                Text(
                    text = if (isDeleting) (if (isTelugu) "తొలగిస్తోంది..." else "Deleting...") else (if (isTelugu) "తొలగించు" else "Delete Account"),
                    style = AppTypography.buttonMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun performAccountDeletion(
    navController: NavController,
    authManager: AuthManager,
    profileCompletionViewModel: ProfileCompletionViewModel,
    scope: CoroutineScope,
    userRole: String
) {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    val uid = currentUser.uid

    scope.launch {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val collection = if (userRole.equals("EMPLOYER", ignoreCase = true)) "employerProfiles" else "workerProfiles"
            
            runCatching { db.collection("users").document(uid).delete() }
            runCatching { db.collection(collection).document(uid).delete() }

            authManager.logout()
            navController.navigate(com.example.dutype.navigation.Routes.SELECT_ROLE) {
                popUpTo(0) { inclusive = true }
            }
        } catch (e: Exception) {
            Timber.e(e, "Account deletion failed")
        }
    }
}
