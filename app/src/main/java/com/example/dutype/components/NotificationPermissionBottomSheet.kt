package com.example.dutype.components

import com.dutype.app.R
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.ui.theme.WorkerColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPermissionBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onEnableNotifications: () -> Unit,
    userRole: String = "worker", // "worker" or "employer"
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Role-specific content
    val (title, description, benefits) = when (userRole.lowercase()) {
        "employer" -> Triple(
            "Enable Notifications",
            "Stay updated with job applications, candidate responses, and important updates from your job postings.",
            listOf(
                "📋" to "Application notifications",
                "👥" to "Candidate responses", 
                "📊" to "Job posting updates"
            )
        )
        else -> Triple(
            "Enable Notifications", 
            "Stay updated with job alerts, application status updates, and new opportunities from employers.",
            listOf(
                "🔔" to "Job alerts",
                "📱" to "Application updates",
                "💼" to "New opportunities"
            )
        )
    }
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            modifier = modifier,
            containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground,
            contentColor = WorkerColors.TextPrimary,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                
                // Notification icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(WorkerColors.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = WorkerColors.Primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                    ),
                    textAlign = TextAlign.Center
                )
                
                // Description
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = WorkerColors.TextSecondary,
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )
                
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Action buttons in single row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cancel button (smaller with same height)
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.3f)
                            .height(52.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = WorkerColors.TextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.auto_cancel),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    
                    // Enable Notifications button (bigger with proper text wrapping)
                    Button(
                        onClick = {
                            onEnableNotifications()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(0.7f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WorkerColors.Primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.auto_turn_on_notifications),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 2,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                // Bottom spacing
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}


/**
 * Helper function to open app settings for notification permissions
 */
fun openNotificationSettings(context: android.content.Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to general app settings
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS)
            context.startActivity(intent)
        } catch (e2: Exception) {
            // If all else fails, show a toast
            android.widget.Toast.makeText(
                context,
                "Please enable notifications in your device settings",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
}
