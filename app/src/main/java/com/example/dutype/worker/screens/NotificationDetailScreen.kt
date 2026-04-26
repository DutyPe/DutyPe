package com.example.dutype.worker.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.example.dutype.navigation.Routes
import com.example.dutype.models.Notification
import com.example.dutype.models.NotificationType
import com.example.dutype.utils.DateTimeUtils
import com.example.dutype.worker.viewmodels.WorkerNotificationViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.dutype.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    notificationId: String,
    onBackClick: () -> Unit,
    navController: NavController,
    viewModel: WorkerNotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val notification = uiState.notifications.find { it.id == notificationId }
    
    LaunchedEffect(notificationId) {
        viewModel.markAsRead(notificationId)
    }
    
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        com.example.dutype.components.CommonHeader(
            title = stringResource(R.string.notification_label),
            onBackClick = onBackClick
        )

        if (notification == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color(0xFFBDBDBD),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = stringResource(R.string.notif_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF6B7280)
                    )
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937))
                    ) {
                        Text(stringResource(R.string.go_back))
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(getNotificationDetailColor(notification.type).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getNotificationDetailIcon(notification.type),
                            contentDescription = null,
                            tint = getNotificationDetailColor(notification.type),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = getNotificationTypeLabel(notification.type),
                            style = MaterialTheme.typography.labelMedium,
                            color = getNotificationDetailColor(notification.type),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatDetailTime(notification.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
                
                HorizontalDivider(color = Color(0xFFF3F4F6))
                
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF4B5563),
                    lineHeight = 26.sp
                )

                val jobId = notification.actionData["jobId"]
                val jobTitle = notification.actionData["jobTitle"]
                
                if (!jobTitle.isNullOrEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Work,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.notif_job_label, jobTitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF374151)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                val actionButton = getActionButton(notification)
                if (actionButton != null) {
                    Button(
                        onClick = {
                            when (actionButton.route) {
                                "job_detail" -> jobId?.let { navController.navigate(Routes.jobDetailRoute(it)) }
                                "my_jobs" -> navController.navigate(com.example.dutype.navigation.WorkerBottomRoutes.MY_JOBS)
                                "all_jobs" -> navController.navigate(Routes.WORKER_ALL_JOBS)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(actionButton.icon, contentDescription = null, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (actionButton.label) {
                                "VIEW_APPLICATIONS" -> stringResource(R.string.notif_action_view_applications)
                                "VIEW_JOB_DETAILS" -> stringResource(R.string.notif_action_view_job_details)
                                "BROWSE_JOBS" -> stringResource(R.string.notif_action_browse_jobs)
                                else -> actionButton.label
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                OutlinedButton(
                    onClick = {
                        viewModel.deleteNotification(notificationId)
                        onBackClick()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.delete_notification))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}


private data class ActionButtonData(
    val label: String,
    val icon: ImageVector,
    val route: String
)

private fun getActionButton(notification: Notification): ActionButtonData? {
    val jobId = notification.actionData["jobId"]
    return when (notification.type) {
        NotificationType.APPLICATION_STATUS_UPDATE,
        NotificationType.SHORTLISTED,
        NotificationType.REJECTED -> ActionButtonData("VIEW_APPLICATIONS", Icons.Default.Work, "my_jobs")
        NotificationType.JOB_POSTED,
        NotificationType.NEW_JOB_ALERT,
        NotificationType.JOB_RECOMMENDATION -> {
            if (!jobId.isNullOrEmpty()) ActionButtonData("VIEW_JOB_DETAILS", Icons.Default.Visibility, "job_detail")
            else ActionButtonData("BROWSE_JOBS", Icons.Default.Search, "all_jobs")
        }
        else -> null
    }
}

private fun getNotificationDetailIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.APPLICATION_STATUS_UPDATE -> Icons.Default.CheckCircle
        NotificationType.SHORTLISTED -> Icons.Default.Star
        NotificationType.REJECTED -> Icons.Default.Cancel
        NotificationType.INTERVIEW_SCHEDULED -> Icons.Default.Schedule
        NotificationType.EMPLOYER_MESSAGE -> Icons.Default.Message
        NotificationType.NEW_JOB_ALERT -> Icons.Default.Work
        NotificationType.JOB_RECOMMENDATION -> Icons.Default.ThumbUp
        NotificationType.JOB_POSTED -> Icons.Default.AddCircle
        NotificationType.SYSTEM_UPDATE -> Icons.Default.Info
        else -> Icons.Default.Notifications
    }
}

private fun getNotificationDetailColor(type: NotificationType): Color {
    return when (type) {
        NotificationType.APPLICATION_STATUS_UPDATE -> Color(0xFF10B981)
        NotificationType.SHORTLISTED -> Color(0xFF10B981)
        NotificationType.REJECTED -> Color(0xFFEF4444)
        NotificationType.INTERVIEW_SCHEDULED -> Color(0xFF3B82F6)
        NotificationType.EMPLOYER_MESSAGE -> Color(0xFF8B5CF6)
        NotificationType.NEW_JOB_ALERT -> Color(0xFFF59E0B)
        NotificationType.JOB_RECOMMENDATION -> Color(0xFF06B6D4)
        NotificationType.JOB_POSTED -> Color(0xFF10B981)
        NotificationType.SYSTEM_UPDATE -> Color(0xFF6B7280)
        else -> Color(0xFF6B7280)
    }
}

@Composable
private fun getNotificationTypeLabel(type: NotificationType): String {
    return when (type) {
        NotificationType.APPLICATION_STATUS_UPDATE -> stringResource(R.string.notif_type_application_update)
        NotificationType.SHORTLISTED -> stringResource(R.string.notif_type_shortlisted)
        NotificationType.REJECTED -> stringResource(R.string.notif_type_application_update)
        NotificationType.INTERVIEW_SCHEDULED -> stringResource(R.string.notif_type_interview)
        NotificationType.EMPLOYER_MESSAGE -> stringResource(R.string.notif_type_message)
        NotificationType.NEW_JOB_ALERT -> stringResource(R.string.notif_type_new_job)
        NotificationType.JOB_RECOMMENDATION -> stringResource(R.string.notif_type_recommended)
        NotificationType.JOB_POSTED -> stringResource(R.string.notif_type_job_posted)
        NotificationType.SYSTEM_UPDATE -> stringResource(R.string.notif_type_system)
        else -> stringResource(R.string.notif_type_notification)
    }
}

/**
 * Format detail time using centralized DateTimeUtils
 * For detail screens, we use the full datetime format
 */
private fun formatDetailTime(timestamp: Long): String = DateTimeUtils.formatDateTime(timestamp)
