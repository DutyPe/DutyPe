package com.example.partimes.notifications.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.partimes.notifications.models.*
import com.example.partimes.notifications.viewmodels.NotificationCenterViewModel
import com.example.partimes.ui.theme.WorkerGradientBackground

/**
 * Notification Center Screen
 * Displays all notifications with filtering and management options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    onBackClick: () -> Unit,
    onNotificationClick: (Notification) -> Unit = {}
) {
    val viewModel: NotificationCenterViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    WorkerGradientBackground {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            NotificationHeader(
                onBackClick = onBackClick,
                onMarkAllReadClick = { viewModel.markAllAsRead() },
                onClearAllClick = { viewModel.clearAllNotifications() },
                unreadCount = uiState.unreadCount
            )
            
            // Filter Tabs
            NotificationFilterTabs(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = viewModel::updateFilter
            )
            
            // Notifications List
            when {
                uiState.isLoading -> {
                    NotificationLoadingState()
                }
                uiState.filteredNotifications.isEmpty() -> {
                    NotificationEmptyState(
                        filter = uiState.selectedFilter,
                        onRefresh = { viewModel.refresh() }
                    )
                }
                else -> {
                    NotificationList(
                        notifications = uiState.filteredNotifications,
                        onNotificationClick = onNotificationClick,
                        onMarkAsRead = { viewModel.markAsRead(it) },
                        onArchive = { viewModel.archiveNotification(it) },
                        onDelete = { viewModel.deleteNotification(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationHeader(
    onBackClick: () -> Unit,
    onMarkAllReadClick: () -> Unit,
    onClearAllClick: () -> Unit,
    unreadCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF6366F1)
                        )
                    }
                    
                    Text(
                        text = "Notifications",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    
                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(
                            containerColor = Color(0xFFEF4444)
                        ) {
                            Text(
                                text = unreadCount.toString(),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                
                Row {
                    if (unreadCount > 0) {
                        IconButton(onClick = onMarkAllReadClick) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Mark all as read",
                                tint = Color(0xFF6366F1)
                            )
                        }
                    }
                    
                    IconButton(onClick = onClearAllClick) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Clear all",
                            tint = Color(0xFF6366F1)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationFilterTabs(
    selectedFilter: NotificationFilter,
    onFilterSelected: (NotificationFilter) -> Unit
) {
    val filters = listOf(
        NotificationFilter.ALL,
        NotificationFilter.UNREAD,
        NotificationFilter.APPLICATIONS,
        NotificationFilter.JOBS
    )
    
    ScrollableTabRow(
        selectedTabIndex = filters.indexOf(selectedFilter),
        containerColor = Color.Transparent,
        contentColor = Color(0xFF6366F1),
        edgePadding = 16.dp
    ) {
        filters.forEachIndexed { index, filter ->
            Tab(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                text = {
                    Text(
                        text = filter.displayName,
                        fontSize = 14.sp,
                        fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

@Composable
private fun NotificationList(
    notifications: List<Notification>,
    onNotificationClick: (Notification) -> Unit,
    onMarkAsRead: (String) -> Unit,
    onArchive: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notifications) { notification ->
            NotificationItem(
                notification = notification,
                onClick = { onNotificationClick(notification) },
                onMarkAsRead = { onMarkAsRead(notification.id) },
                onArchive = { onArchive(notification.id) },
                onDelete = { onDelete(notification.id) }
            )
        }
    }
}

@Composable
private fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit,
    onMarkAsRead: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else Color(0xFFF8FAFC)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Notification Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(notification.type.getColor())),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = notification.type.getIcon(),
                    fontSize = 20.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Notification Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 16.sp,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                        color = Color(0xFF111827),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = notification.getFormattedTime(),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = notification.message,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Priority indicator
                if (notification.priority == NotificationPriority.URGENT) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PriorityHigh,
                            contentDescription = "Urgent",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Urgent",
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Action Menu
            if (!notification.isRead) {
                IconButton(onClick = onMarkAsRead) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Mark as read",
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color(0xFF6366F1)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading notifications...",
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun NotificationEmptyState(
    filter: NotificationFilter,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = "No notifications",
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = when (filter) {
                    NotificationFilter.ALL -> "No notifications yet"
                    NotificationFilter.UNREAD -> "No unread notifications"
                    NotificationFilter.READ -> "No read notifications"
                    NotificationFilter.ARCHIVED -> "No archived notifications"
                    NotificationFilter.APPLICATIONS -> "No application updates"
                    NotificationFilter.JOBS -> "No job alerts"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when (filter) {
                    NotificationFilter.ALL -> "You'll see notifications about job applications, new job alerts, and updates here."
                    NotificationFilter.UNREAD -> "All caught up! Check back later for new notifications."
                    NotificationFilter.READ -> "Read notifications will appear here."
                    NotificationFilter.ARCHIVED -> "Archived notifications will appear here."
                    NotificationFilter.APPLICATIONS -> "Application status updates will appear here."
                    NotificationFilter.JOBS -> "New job recommendations and alerts will appear here."
                },
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh")
            }
        }
    }
}

