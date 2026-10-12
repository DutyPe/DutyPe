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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dutype.models.NotificationType
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.NotificationDialogData

/**
 * Material 3 Notification Dialog Component
 * 
 * Features:
 * - Smooth animations (Material 3 Expressive)
 * - Material icons for clear, native Android presentation
 * - Primary and optional secondary actions
 * - Type-based color theming
 * 
 * Based on Google Material Design 3 guidelines
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDialog(
    data: NotificationDialogData,
    onDismiss: () -> Unit,
    visible: Boolean = true
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon with type-based background
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(
                        color = getNotificationDialogColor(data.type).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getNotificationDialogIcon(data.type),
                    contentDescription = null,
                    tint = getNotificationDialogColor(data.type),
                    modifier = Modifier.size(32.dp)
                )
            }

            // Title
            Text(
                text = data.title,
                style = AppTypography.pageTitle,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            // Message
            Text(
                text = data.message,
                style = AppTypography.bodyMedium,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Action
                Button(
                    onClick = {
                        data.primaryAction.action()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = getNotificationDialogColor(data.type)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = data.primaryAction.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
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
                            contentColor = Color(0xFF475569)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(14.dp)
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

/**
 * Get color for notification dialog based on type
 */
private fun getNotificationDialogColor(type: NotificationType): Color {
    return when (type) {
        NotificationType.APPLICATION_STATUS,
        NotificationType.APPLICATION_STATUS_UPDATE,
        NotificationType.INTERVIEW_SCHEDULED -> Color(0xFF3B82F6) // Blue
        
        NotificationType.NEW_APPLICATION,
        NotificationType.WORKER_HIRED -> Color(0xFF10B981) // Green
        
        NotificationType.REJECTED -> Color(0xFFEF4444) // Red
        
        NotificationType.JOB_POSTED,
        NotificationType.NEW_JOB_ALERT,
        NotificationType.JOB_RECOMMENDATION -> Color(0xFF8B5CF6) // Purple
        
        NotificationType.BIRTHDAY,
        NotificationType.REFERRAL_MILESTONE, NotificationType.PAYMENT -> Color(0xFFFF69B4) // Pink
        
        NotificationType.WELCOME,
        NotificationType.PROFILE_COMPLETE -> Color(0xFF06B6D4) // Cyan
        
        NotificationType.APPLICATION_REMINDER,
        NotificationType.JOB_EXPIRY_REMINDER -> Color(0xFFF59E0B) // Orange
        
        else -> Color(0xFF6B7280) // Gray
    }
}

private fun getNotificationDialogIcon(type: NotificationType): ImageVector = when (type) {
    NotificationType.APPLICATION_STATUS,
    NotificationType.APPLICATION_STATUS_UPDATE,
    NotificationType.PROFILE_COMPLETE -> Icons.Default.CheckCircle
    NotificationType.INTERVIEW_SCHEDULED -> Icons.Default.EventAvailable
    NotificationType.NEW_APPLICATION -> Icons.Default.PersonAdd
    NotificationType.WORKER_HIRED,
    NotificationType.JOB_POSTED,
    NotificationType.NEW_JOB_ALERT,
    NotificationType.JOB_RECOMMENDATION -> Icons.Default.Work
    NotificationType.REJECTED -> Icons.Default.Error
    NotificationType.BIRTHDAY -> Icons.Default.Cake
    NotificationType.REFERRAL_MILESTONE, NotificationType.PAYMENT -> Icons.Default.Campaign
    NotificationType.WELCOME -> Icons.Default.Info
    else -> Icons.Default.Notifications
}
