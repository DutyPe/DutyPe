package com.example.dutype.employer.screens

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.dutype.navigation.Routes
import com.example.dutype.models.Notification
import com.example.dutype.models.NotificationType
import com.example.dutype.utils.DateTimeUtils
import com.example.dutype.employer.viewmodels.EmployerNotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerNotificationDetailScreen(
    notificationId: String,
    onBackClick: () -> Unit,
    navController: NavController,
    viewModel: EmployerNotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notification = uiState.notifications.find { it.id == notificationId }
    
    LaunchedEffect(notificationId) {
        viewModel.markAsRead(notificationId)
    }
    
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        com.example.dutype.components.CommonHeader(
            title = "Notification",
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
                        text = "Notification not found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF6B7280)
                    )
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("Go Back")
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
                            .background(getEmployerNotificationDetailColor(notification.type).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getEmployerNotificationDetailIcon(notification.type),
                            contentDescription = null,
                            tint = getEmployerNotificationDetailColor(notification.type),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = getEmployerNotificationTypeLabel(notification.type),
                            style = MaterialTheme.typography.labelMedium,
                            color = getEmployerNotificationDetailColor(notification.type),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatEmployerDetailTime(notification.createdAt),
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
                val applicationId = notification.actionData["applicationId"]
                
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
                                text = "Job: $jobTitle",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF374151)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                val actionButton = getEmployerActionButton(notification)
                if (actionButton != null) {
                    Button(
                        onClick = {
                            when (actionButton.route) {
                                "application_detail" -> applicationId?.let { 
                                    navController.navigate(Routes.EMPLOYER_APPLICATION_DETAIL.replace("{applicationId}", it))
                                }
                                "view_applicants" -> jobId?.let { 
                                    navController.navigate(Routes.viewApplicantsRoute(it))
                                }
                                "applications" -> navController.navigate(Routes.EMPLOYER_APPLICATIONS)
                                "analytics" -> navController.navigate(Routes.ANALYTICS)
                                "my_jobs" -> navController.navigate(Routes.EMPLOYER_MY_JOBS)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(actionButton.icon, contentDescription = null, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = actionButton.label, fontWeight = FontWeight.SemiBold)
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
                    Text("Delete Notification")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private data class EmployerActionButtonData(
    val label: String,
    val icon: ImageVector,
    val route: String
)

private fun getEmployerActionButton(notification: Notification): EmployerActionButtonData? {
    val jobId = notification.actionData["jobId"]
    val applicationId = notification.actionData["applicationId"]
    
    return when (notification.type) {
        NotificationType.NEW_APPLICATION -> {
            if (!applicationId.isNullOrEmpty()) {
                EmployerActionButtonData("View Application", Icons.Default.Person, "application_detail")
            } else if (!jobId.isNullOrEmpty()) {
                EmployerActionButtonData("View All Applicants", Icons.Default.People, "view_applicants")
            } else {
                EmployerActionButtonData("View Applications", Icons.Default.People, "applications")
            }
        }
        NotificationType.JOB_POSTED -> EmployerActionButtonData("View Analytics", Icons.Default.Analytics, "analytics")
        NotificationType.JOB_PAUSED -> EmployerActionButtonData("View My Jobs", Icons.Default.Work, "my_jobs")
        NotificationType.APPLICATION_STATUS_UPDATE -> {
            if (!applicationId.isNullOrEmpty()) {
                EmployerActionButtonData("View Application", Icons.Default.Person, "application_detail")
            } else {
                EmployerActionButtonData("View Applications", Icons.Default.People, "applications")
            }
        }
        else -> null
    }
}

private fun getEmployerNotificationDetailIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.NEW_APPLICATION -> Icons.Default.PersonAdd
        NotificationType.JOB_POSTED -> Icons.Default.AddCircle
        NotificationType.JOB_PAUSED -> Icons.Default.Pause
        NotificationType.APPLICATION_STATUS_UPDATE -> Icons.Default.CheckCircle
        NotificationType.INTERVIEW_SCHEDULED -> Icons.Default.Schedule
        NotificationType.SYSTEM_UPDATE -> Icons.Default.Info
        else -> Icons.Default.Notifications
    }
}

private fun getEmployerNotificationDetailColor(type: NotificationType): Color {
    return when (type) {
        NotificationType.NEW_APPLICATION -> Color(0xFF10B981)
        NotificationType.JOB_POSTED -> Color(0xFF3B82F6)
        NotificationType.JOB_PAUSED -> Color(0xFFF59E0B)
        NotificationType.APPLICATION_STATUS_UPDATE -> Color(0xFF8B5CF6)
        NotificationType.INTERVIEW_SCHEDULED -> Color(0xFFEF4444)
        NotificationType.SYSTEM_UPDATE -> Color(0xFF6B7280)
        else -> Color(0xFF6B7280)
    }
}

private fun getEmployerNotificationTypeLabel(type: NotificationType): String {
    return when (type) {
        NotificationType.NEW_APPLICATION -> "New Application"
        NotificationType.JOB_POSTED -> "Job Posted"
        NotificationType.JOB_PAUSED -> "Job Update"
        NotificationType.APPLICATION_STATUS_UPDATE -> "Application Update"
        NotificationType.INTERVIEW_SCHEDULED -> "Interview"
        NotificationType.SYSTEM_UPDATE -> "System"
        else -> "Notification"
    }
}

/**
 * Format detail time using centralized DateTimeUtils
 * For detail screens, we use the full datetime format
 */
private fun formatEmployerDetailTime(timestamp: Long): String = DateTimeUtils.formatDateTime(timestamp)
