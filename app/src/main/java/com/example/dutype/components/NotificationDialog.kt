package com.example.dutype.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dutype.models.NotificationType
import com.example.dutype.utils.NotificationDialogData

/**
 * Material 3 Notification Dialog Component
 * 
 * Features:
 * - Smooth animations (Material 3 Expressive)
 * - Emoji icons for visual appeal
 * - Primary and optional secondary actions
 * - Type-based color theming
 * 
 * Based on Google Material Design 3 guidelines
 */
@Composable
fun NotificationDialog(
    data: NotificationDialogData,
    onDismiss: () -> Unit,
    visible: Boolean = true
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) + 
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
        exit = fadeOut(animationSpec = tween(200)) + 
               scaleOut(targetScale = 0.8f, animationSpec = tween(200))
    ) {
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
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Icon with background
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = getNotificationDialogColor(data.type).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = data.icon,
                            fontSize = 36.sp
                        )
                    }
                    
                    // Title
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        textAlign = TextAlign.Center
                    )
                    
                    // Message
                    Text(
                        text = data.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF4B5563),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Actions
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Primary Action
                        Button(
                            onClick = {
                                data.primaryAction.action()
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = getNotificationDialogColor(data.type)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = data.primaryAction.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        // Secondary Action (if provided)
                        data.secondaryAction?.let { secondaryAction ->
                            OutlinedButton(
                                onClick = {
                                    secondaryAction.action()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF6B7280)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(0xFFE5E7EB)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = secondaryAction.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get color for notification dialog based on type
 */
private fun getNotificationDialogColor(type: NotificationType): Color {
    return when (type) {
        NotificationType.APPLICATION_STATUS,
        NotificationType.APPLICATION_STATUS_UPDATE,
        NotificationType.SHORTLISTED,
        NotificationType.INTERVIEW_SCHEDULED -> Color(0xFF3B82F6) // Blue
        
        NotificationType.NEW_APPLICATION,
        NotificationType.WORKER_HIRED -> Color(0xFF10B981) // Green
        
        NotificationType.REJECTED -> Color(0xFFEF4444) // Red
        
        NotificationType.JOB_POSTED,
        NotificationType.NEW_JOB_ALERT,
        NotificationType.JOB_RECOMMENDATION -> Color(0xFF8B5CF6) // Purple
        
        NotificationType.BIRTHDAY,
        NotificationType.REFERRAL_MILESTONE -> Color(0xFFFF69B4) // Pink
        
        NotificationType.WELCOME,
        NotificationType.PROFILE_COMPLETE -> Color(0xFF06B6D4) // Cyan
        
        NotificationType.APPLICATION_REMINDER,
        NotificationType.JOB_EXPIRY_REMINDER -> Color(0xFFF59E0B) // Orange
        
        else -> Color(0xFF6B7280) // Gray
    }
}
