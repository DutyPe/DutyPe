package com.example.dutype.employer.screens

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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.CheckCircle
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
import com.example.dutype.employer.viewmodels.EmployerNotificationViewModel
import com.example.dutype.components.NotificationItemShimmer
import java.text.SimpleDateFormat
import java.util.*
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerNotificationScreen(
    onBackClick: () -> Unit,
    navController: NavController,
    viewModel: EmployerNotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Debug logging
    LaunchedEffect(uiState) {
        Timber.d("🔔 EmployerNotificationScreen - UI State updated:")
        Timber.d("🔔 EmployerNotificationScreen - Notifications count: ${uiState.notifications.size}")
        Timber.d("🔔 EmployerNotificationScreen - Is loading: ${uiState.isLoading}")
        Timber.d("🔔 EmployerNotificationScreen - Error: ${uiState.error}")
        Timber.d("🔔 EmployerNotificationScreen - Unread count: ${uiState.unreadCount}")
        uiState.notifications.forEachIndexed { index, notification ->
            Timber.d("🔔 EmployerNotificationScreen - Notification $index: $notification")
        }
    }

    // Load notifications when screen opens
    LaunchedEffect(Unit) {
        Timber.d("🔔 EmployerNotificationScreen - Loading notifications...")
        viewModel.loadNotifications()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Header with enhanced styling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF3B82F6),
                            Color(0xFF2563EB)
                        )
                    )
                )
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.ArrowBack, 
                        contentDescription = "Back", 
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (uiState.unreadCount > 0) {
                        Text(
                            text = "${uiState.unreadCount} unread",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
        }

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
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Failed to load notifications",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Button(
                            onClick = { viewModel.loadNotifications() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B82F6)
                            )
                        ) {
                            Text("Retry", color = Color.White)
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
                                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF3B82F6).copy(alpha = 0.1f),
                                            Color(0xFF3B82F6).copy(alpha = 0.05f)
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "No notifications",
                                tint = Color(0xFF3B82F6).copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "No notifications yet",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color(0xFF1F2937),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "You'll see job applications, job status updates, and other important updates here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280),
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
                        EmployerNotificationItem(
                            notification = notification,
                            onNotificationClick = { 
                                viewModel.markAsRead(notification.id)
                                // Navigate to related screen based on notification type
                                when (notification.type) {
                                    com.example.dutype.notifications.models.NotificationType.NEW_APPLICATION -> {
                                        // Navigate to applications management screen
                                        navController.navigate(com.example.dutype.navigation.Routes.EMPLOYER_APPLICATIONS)
                                    }
                                    com.example.dutype.notifications.models.NotificationType.JOB_POSTED -> {
                                        // Navigate to analytics screen to see job performance
                                        navController.navigate(com.example.dutype.navigation.Routes.ANALYTICS)
                                    }
                                    com.example.dutype.notifications.models.NotificationType.JOB_PAUSED -> {
                                        // Navigate to posted jobs or analytics screen
                                        navController.navigate(com.example.dutype.navigation.Routes.ANALYTICS)
                                    }
                                    com.example.dutype.notifications.models.NotificationType.APPLICATION_STATUS_UPDATE -> {
                                        // Navigate to applications management screen
                                        navController.navigate(com.example.dutype.navigation.Routes.EMPLOYER_APPLICATIONS)
                                    }
                                    else -> {
                                        // Default navigation or stay on current screen
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

private fun EmployerNotificationViewModel.createTestNotification() {
    TODO("Not yet implemented")
}

@Composable
fun EmployerNotificationItem(
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
                                Color(0xFF10B981).copy(alpha = 0.1f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Mark as read",
                            tint = Color(0xFF10B981),
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
                        tint = Color(0xFF6B7280),
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
                    tint = getNotificationColor(notification.type),
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
                                    .background(getNotificationColor(notification.type))
                            )
                        }
                    }
                }
                
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = if (notification.isRead) Color(0xFF6B7280) else Color(0xFF4B5563),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private fun getNotificationIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.NEW_APPLICATION -> Icons.Default.PersonAdd
        NotificationType.JOB_POSTED -> Icons.Default.Work
        NotificationType.JOB_PAUSED -> Icons.Default.Pause
        NotificationType.APPLICATION_STATUS_UPDATE -> Icons.Default.CheckCircle
        NotificationType.INTERVIEW_SCHEDULED -> Icons.Default.NotificationsActive
        else -> Icons.Default.Notifications
    }
}

private fun getNotificationColor(type: NotificationType): Color {
    return when (type) {
        NotificationType.NEW_APPLICATION -> Color(0xFF10B981) // Green
        NotificationType.JOB_POSTED -> Color(0xFF3B82F6) // Blue
        NotificationType.JOB_PAUSED -> Color(0xFFF59E0B) // Orange
        NotificationType.APPLICATION_STATUS_UPDATE -> Color(0xFF8B5CF6) // Purple
        NotificationType.INTERVIEW_SCHEDULED -> Color(0xFFEF4444) // Red
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
