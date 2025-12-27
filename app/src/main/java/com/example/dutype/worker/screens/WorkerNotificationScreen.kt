package com.example.dutype.worker.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.dutype.notifications.models.Notification
import com.example.dutype.notifications.models.NotificationType
import com.example.dutype.notifications.models.getDisplayName
import com.example.dutype.worker.viewmodels.WorkerNotificationViewModel
import com.example.dutype.components.NotificationShimmer
import com.example.dutype.components.NotificationItemShimmer
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerNotificationScreen(
    onBackClick: () -> Unit,
    navController: NavController,
    viewModel: WorkerNotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load notifications when screen opens
    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Use CommonHeader with optional subtitle
        com.example.dutype.components.CommonHeader(
            title = "Notifications",
            onBackClick = onBackClick,
            subtitle = if (uiState.unreadCount > 0) "${uiState.unreadCount} unread" else null
        )

        // Content
        when {
            uiState.isLoading -> {
                // Show shimmer loading for notifications
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(8) {
                        NotificationItemShimmer()
                        HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                    }
                }
            }
            uiState.error != null -> {
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
                            contentDescription = "Error",
                            tint = Color.Black,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Failed to load notifications",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.Black
                            )
                        )
                        Button(
                            onClick = { viewModel.loadNotifications() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF3F4F6)
                            )
                        ) {
                            Text(
                                "Retry", 
                                color = Color(0xFF374151),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF374151)
                                )
                            )
                        }
                    }
                }
            }
            uiState.notifications.isEmpty() -> {
                // Empty state with enhanced design
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(
                                    Color(0xFFF3F4F6),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "No notifications",
                                tint = Color.Black,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "No notifications yet",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "You'll see application updates, interview schedules, and job recommendations here.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.Black
                                ),
                                modifier = Modifier.padding(horizontal = 40.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
            else -> {
                // Notifications list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.notifications) { notification ->
                        WorkerNotificationItem(
                            notification = notification,
                            onNotificationClick = { 
                                viewModel.markAsRead(notification.id)
                                // Navigate to related screen based on notification type and data
                                val jobId = notification.actionData["jobId"]
                                val applicationId = notification.actionData["applicationId"]
                                val userRole = notification.actionData["userRole"]
                                
                                when (notification.type) {
                                    com.example.dutype.notifications.models.NotificationType.APPLICATION_STATUS_UPDATE -> {
                                        // Navigate to My Jobs screen to see applied jobs
                                        navController.navigate(com.example.dutype.navigation.Routes.WORKER_MY_JOBS)
                                    }
                                    com.example.dutype.notifications.models.NotificationType.JOB_POSTED -> {
                                        // Navigate to job details if jobId is available
                                        if (!jobId.isNullOrEmpty()) {
                                            navController.navigate(com.example.dutype.navigation.Routes.jobDetailRoute(jobId))
                                        } else {
                                            // Navigate to all jobs
                                            navController.navigate(com.example.dutype.navigation.Routes.WORKER_ALL_JOBS)
                                        }
                                    }
                                    com.example.dutype.notifications.models.NotificationType.JOB_PAUSED -> {
                                        // Navigate to My Jobs screen
                                        navController.navigate(com.example.dutype.navigation.Routes.WORKER_MY_JOBS)
                                    }
                                    com.example.dutype.notifications.models.NotificationType.NEW_APPLICATION -> {
                                        // For workers, this would be confirmation of their application
                                        navController.navigate(com.example.dutype.navigation.Routes.WORKER_MY_JOBS)
                                    }
                                    com.example.dutype.notifications.models.NotificationType.SYSTEM_UPDATE -> {
                                        // For profile complete/welcome notifications, navigate to home
                                        // Already on home, just mark as read
                                    }
                                    else -> {
                                        // Default: try to navigate to job if jobId available
                                        if (!jobId.isNullOrEmpty()) {
                                            navController.navigate(com.example.dutype.navigation.Routes.jobDetailRoute(jobId))
                                        }
                                    }
                                }
                            },
                            onMarkAsRead = {
                                viewModel.markAsRead(notification.id)
                            },
                            onDelete = {
                                viewModel.deleteNotification(notification.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkerNotificationItem(
    notification: Notification,
    onNotificationClick: () -> Unit,
    onMarkAsRead: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showActions by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "cardScale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onNotificationClick() }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onLongPress = {
                        showActions = true
                    }
                )
            },
//        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color(0xFFF8F9FA) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.isRead) 1.dp else 2.dp
        )
    ) {
        AnimatedVisibility(
            visible = showActions,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            // Action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFF8FAFC),
                                Color(0xFFE2E8F0)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    IconButton(
                        onClick = {
                            onMarkAsRead()
                            showActions = false
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Color(0xFF1F2937).copy(alpha = 0.1f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Mark as read",
                            tint = Color(0xFF1F2937),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            onDelete()
                            showActions = false
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Color(0xFFEF4444).copy(alpha = 0.1f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = { showActions = false },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Color(0xFF6B7280).copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        // Main content with modern layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Modern icon design
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        getNotificationColor(notification.type).copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getNotificationIcon(notification.type),
                    contentDescription = notification.type.getDisplayName(),
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content with clean typography
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.SemiBold,
                        color = if (notification.isRead) Color(0xFF374151) else Color(0xFF111827),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Time and unread indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formatNotificationTime(notification.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF),
                            fontWeight = FontWeight.Normal
                        )
                        
                        if (!notification.isRead) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                            )
                        }
                    }
                }
                
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = if (notification.isRead) Color(0xFF6B7280) else Color(0xFF4B5563),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private fun getNotificationIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.APPLICATION_STATUS_UPDATE -> Icons.Default.CheckCircle
        NotificationType.SHORTLISTED -> Icons.Default.CheckCircle
        NotificationType.REJECTED -> Icons.Default.Notifications
        NotificationType.INTERVIEW_SCHEDULED -> Icons.Default.Schedule
        NotificationType.EMPLOYER_MESSAGE -> Icons.Default.Message
        NotificationType.NEW_JOB_ALERT -> Icons.Default.Work
        NotificationType.JOB_RECOMMENDATION -> Icons.Default.Work
        else -> Icons.Default.Notifications
    }
}

private fun getNotificationColor(type: NotificationType): Color {
    return when (type) {
        NotificationType.APPLICATION_STATUS_UPDATE -> Color(0xFF10B981) // Green
        NotificationType.SHORTLISTED -> Color(0xFF10B981) // Green
        NotificationType.REJECTED -> Color(0xFFEF4444) // Red
        NotificationType.INTERVIEW_SCHEDULED -> Color(0xFF3B82F6) // Blue
        NotificationType.EMPLOYER_MESSAGE -> Color(0xFF8B5CF6) // Purple
        NotificationType.NEW_JOB_ALERT -> Color(0xFFF59E0B) // Orange
        NotificationType.JOB_RECOMMENDATION -> Color(0xFF06B6D4) // Cyan
        else -> Color(0xFF6B7280) // Gray
    }
}

private fun formatNotificationTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> {
            val date = Date(timestamp)
            val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
            formatter.format(date)
        }
    }
}
