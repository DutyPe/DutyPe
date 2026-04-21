package com.example.dutype.employer.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.dutype.models.Notification
import com.example.dutype.models.NotificationType
import com.example.dutype.models.getDisplayName
import com.example.dutype.utils.DateTimeUtils
import com.example.dutype.employer.viewmodels.EmployerNotificationViewModel
import com.example.dutype.components.NotificationItemShimmer
import com.example.dutype.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import timber.log.Timber
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.res.stringResource
import com.dutype.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerNotificationScreen(
    onBackClick: () -> Unit,
    navController: NavController,
    viewModel: EmployerNotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Dialog state for notification dialogs
    var dialogData by remember { mutableStateOf<com.example.dutype.utils.NotificationDialogData?>(null) }

    LaunchedEffect(uiState) {
        Timber.d("🔔 EmployerNotificationScreen - UI State updated:")
        Timber.d("🔔 EmployerNotificationScreen - Notifications count: ${uiState.notifications.size}")
    }

    // This screen is hardcoded to the EMPLOYER role via EmployerNotificationViewModel —
    // single-role accounts mean we just load once on enter and again when the dialog closes.
    LaunchedEffect(dialogData == null) {
        if (dialogData == null) {
            Timber.d("🔔 EmployerNotificationScreen - Loading employer notifications")
            viewModel.loadNotifications()
        }
    }

    LaunchedEffect(listState, uiState.notifications.size, uiState.hasMore, uiState.isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (
                    uiState.hasMore &&
                    !uiState.isLoadingMore &&
                    uiState.notifications.isNotEmpty() &&
                    lastVisibleIndex >= uiState.notifications.lastIndex - 3
                ) {
                    viewModel.loadMoreNotifications()
                }
            }
    }
    
    // Show notification dialog if data is present
    dialogData?.let { data ->
        com.example.dutype.components.NotificationDialog(
            data = data,
            onDismiss = { dialogData = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
    ) {
        com.example.dutype.components.CommonHeader(
            title = stringResource(R.string.notifications),
            onBackClick = onBackClick,
            backgroundColor = Color.White,
            titleColor = Color(0xFF1F2937)
        )

        when {
            uiState.isLoading -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                            text = stringResource(R.string.notif_failed_load),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Button(
                            onClick = { viewModel.loadNotifications() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                        ) {
                            Text(stringResource(R.string.retry), color = Color.White)
                        }
                    }
                }
            }
            uiState.notifications.isEmpty() -> {
                // Empty state with enhanced design (same look for guests and signed-in users)
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
                                .background(Color(0xFFF3F4F6), CircleShape),
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
                                text = stringResource(R.string.notif_no_notifications),
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color(0xFF1F2937),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.notif_employer_empty_desc),
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = uiState.notifications,
                        key = { it.id }
                    ) { notification ->
                        EmployerSwipeToDeleteNotificationItem(
                            notification = notification,
                            onNotificationClick = {
                                // Use smart navigation handler
                                com.example.dutype.utils.NotificationNavigationHandler.handleNotificationClick(
                                    notification = notification,
                                    navController = navController,
                                    onMarkAsRead = { notificationId ->
                                        viewModel.markAsRead(notificationId)
                                    },
                                    onShowDialog = { data ->
                                        dialogData = data
                                    },
                                    userRole = "EMPLOYER"
                                )
                            },
                            onDelete = {
                                viewModel.deleteNotification(notification.id)
                            }
                        )
                    }

                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF3B82F6),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerSwipeToDeleteNotificationItem(
    notification: Notification,
    onNotificationClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )
    
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEF4444))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.notif_delete),
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.notif_delete),
                        tint = Color.White
                    )
                }
            }
        },
        content = {
            EmployerNotificationItemContent(
                notification = notification,
                onClick = onNotificationClick
            )
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    )
}

@Composable
fun EmployerNotificationItemContent(
    notification: Notification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color(0xFFF8F9FA) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(getEmployerNotificationColor(notification.type).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getEmployerNotificationIcon(notification.type),
                    contentDescription = notification.type.getDisplayName(),
                    tint = getEmployerNotificationColor(notification.type),
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
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
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formatEmployerNotificationTime(notification.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF),
                            fontWeight = FontWeight.Normal
                        )
                        
                        // Red dot for unread
                        if (!notification.isRead) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            )
                        }
                    }
                }
                
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = if (notification.isRead) Color(0xFF6B7280) else Color(0xFF4B5563),
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (!notification.isRead) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.notif_swipe_to_delete),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFBDBDBD),
                        fontSize = 10.sp
                    )
                }
            }
        }
        
        HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
    }
}

private fun getEmployerNotificationIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.NEW_APPLICATION -> Icons.Default.PersonAdd
        NotificationType.JOB_POSTED -> Icons.Default.Work
        NotificationType.JOB_PAUSED -> Icons.Default.Pause
        NotificationType.APPLICATION_STATUS_UPDATE -> Icons.Default.CheckCircle
        NotificationType.INTERVIEW_SCHEDULED -> Icons.Default.Schedule
        NotificationType.BIRTHDAY -> Icons.Default.Cake
        else -> Icons.Default.Notifications
    }
}

private fun getEmployerNotificationColor(type: NotificationType): Color {
    return when (type) {
        NotificationType.NEW_APPLICATION -> Color(0xFF10B981)
        NotificationType.JOB_POSTED -> Color(0xFF3B82F6)
        NotificationType.JOB_PAUSED -> Color(0xFFF59E0B)
        NotificationType.APPLICATION_STATUS_UPDATE -> Color(0xFF8B5CF6)
        NotificationType.INTERVIEW_SCHEDULED -> Color(0xFFEF4444)
        NotificationType.BIRTHDAY -> Color(0xFFFF69B4) // Pink
        else -> Color(0xFF6B7280)
    }
}

/**
 * Format notification time using centralized DateTimeUtils
 */
private fun formatEmployerNotificationTime(timestamp: Long): String = DateTimeUtils.formatTimeAgo(timestamp)
