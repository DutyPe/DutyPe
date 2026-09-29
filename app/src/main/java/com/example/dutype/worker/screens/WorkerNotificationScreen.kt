package com.example.dutype.worker.screens

import com.dutype.app.R
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dutype.components.NotificationItemShimmer
import com.example.dutype.models.Notification
import com.example.dutype.models.NotificationType
import com.example.dutype.models.getDisplayName
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.DateTimeUtils
import com.example.dutype.worker.viewmodels.WorkerNotificationViewModel
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.example.dutype.ui.theme.MeeshoFontFamily
import androidx.compose.ui.res.stringResource


private val NotifBg = Color(0xFFF8FAFC)
private val NotifInk = Color(0xFF0F0F0F)
private val NotifMuted = Color(0xFF64748B)
private val NotifFaint = Color(0xFF94A3B8)
private val NotifBorder = Color(0xFFE2E8F0)
private val NotifEmerald = Color(0xFF10B981)
private val NotifCobalt = Color(0xFF2563EB)
private val NotifEmeraldTint = Color(0xFFF0FDF4)
private val NotifCobaltTint = Color(0xFFEFF6FF)

/** Filter categories shown as chips. */
private enum class NotifCategory(val label: String) {
    ALL("All"), JOBS("Jobs"), PAYMENTS("Payments"), UPDATES("Updates")
}

/** Maps the app's real notification types onto the three concrete categories. */
private fun categoryOf(type: NotificationType): NotifCategory = when (type) {
    NotificationType.REFERRAL_MILESTONE -> NotifCategory.PAYMENTS
    NotificationType.APPLICATION_STATUS,
    NotificationType.APPLICATION_STATUS_UPDATE,
    NotificationType.APPLICATION_REMINDER,
    NotificationType.NEW_APPLICATION,
    NotificationType.REJECTED,
    NotificationType.JOB_UPDATE,
    NotificationType.JOB_POSTED,
    NotificationType.NEW_JOB_ALERT,
    NotificationType.JOB_RECOMMENDATION,
    NotificationType.JOB_EXPIRY_REMINDER,
    NotificationType.INTERVIEW_SCHEDULED,
    NotificationType.WORKER_HIRED,
    NotificationType.EMPLOYER_MESSAGE -> NotifCategory.JOBS
    else -> NotifCategory.UPDATES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerNotificationScreen(
    onBackClick: () -> Unit,
    navController: NavController,
    viewModel: WorkerNotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var selectedCategory by remember { mutableStateOf(NotifCategory.ALL) }

    // Dialog state for notification dialogs
    var dialogData by remember { mutableStateOf<com.example.dutype.utils.NotificationDialogData?>(null) }

    // If the user previously denied POST_NOTIFICATIONS, landing on this screen is a
    // strong signal that they want notifications — re-prompt the system dialog.
    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        Timber.d("🔔 WorkerNotificationScreen - POST_NOTIFICATIONS granted=$granted")
    }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // This screen is hardcoded to the WORKER role via WorkerNotificationViewModel —
    // single-role accounts mean we just load once on enter and again when the dialog closes.
    LaunchedEffect(dialogData == null) {
        if (dialogData == null) {
            Timber.d("🔔 WorkerNotificationScreen - Loading worker notifications")
            viewModel.loadNotifications()
        }
    }

    val allNotifications = uiState.notifications
    val visibleNotifications = remember(allNotifications, selectedCategory) {
        if (selectedCategory == NotifCategory.ALL) allNotifications
        else allNotifications.filter { categoryOf(it.type) == selectedCategory }
    }

    LaunchedEffect(listState, visibleNotifications.size, uiState.hasMore, uiState.isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (
                    uiState.hasMore &&
                    !uiState.isLoadingMore &&
                    visibleNotifications.isNotEmpty() &&
                    lastVisibleIndex >= visibleNotifications.lastIndex - 3
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
            .background(NotifBg)
            // Title used to sit under the status bar (touching the top edge).
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.notifications),
                fontFamily = MeeshoFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = NotifInk
            )
            Text(
                text = "Mark all read",
                fontFamily = MeeshoFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = NotifMuted,
                modifier = Modifier.clickable {
                    // Uses the existing per-notification mark-as-read action
                    allNotifications.filter { !it.isRead }.forEach { viewModel.markAsRead(it.id) }
                }
            )
        }

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NotifCategory.values().forEach { category ->
                val selected = category == selectedCategory
                val chipShape = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(chipShape)
                        .background(if (selected) NotifInk else Color.White)
                        .border(1.dp, if (selected) NotifInk else NotifBorder, chipShape)
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.label,
                        fontFamily = MeeshoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) Color.White else NotifInk
                    )
                }
            }
        }

        // Content
        when {
            uiState.isLoading -> {
                // Show shimmer loading for notifications
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(8) {
                        NotificationItemShimmer()
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
                            tint = NotifFaint,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = stringResource(R.string.notif_failed_load),
                            fontFamily = MeeshoFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = NotifInk
                        )
                        Button(
                            onClick = { viewModel.loadNotifications() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NotifInk,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                stringResource(R.string.notif_retry),
                                fontFamily = MeeshoFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            visibleNotifications.isEmpty() -> {
                // Empty state (same look for guests, signed-in users and empty filters)
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
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, NotifBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "No notifications",
                                tint = NotifFaint,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.notif_no_notifications),
                                fontFamily = MeeshoFontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = NotifInk
                            )
                            Text(
                                text = stringResource(R.string.notif_worker_empty_desc),
                                fontFamily = MeeshoFontFamily,
                                fontSize = 13.sp,
                                color = NotifMuted,
                                modifier = Modifier.padding(horizontal = 40.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            else -> {
                // Notifications list with swipe to delete
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = visibleNotifications,
                        key = { index, notification -> "${notification.id.ifBlank { "notif" }}_$index" }
                    ) { _, notification ->
                        SwipeToDeleteNotificationItem(
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
                                    userRole = "WORKER"
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
                                androidx.compose.material3.CircularProgressIndicator(
                                    color = NotifMuted,
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
fun SwipeToDeleteNotificationItem(
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
        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
        backgroundContent = {
            // Delete background (red) - shown when swiping left
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WorkerColors.Error)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.notif_delete),
                        fontFamily = MeeshoFontFamily,
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
            NotificationItemContent(
                notification = notification,
                onClick = onNotificationClick
            )
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    )
}

@Composable
fun NotificationItemContent(
    notification: Notification,
    onClick: () -> Unit
) {
    val isUnread = !notification.isRead
    val category = categoryOf(notification.type)
    val isJobLike = category != NotifCategory.UPDATES
    val accent = if (isJobLike) NotifEmerald else NotifCobalt
    val tint = if (isJobLike) NotifEmeraldTint else NotifCobaltTint
    val cardShape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(if (isUnread) NotifBg else Color.White)
            .border(1.dp, NotifBorder, cardShape)
            .clickable { onClick() }
    ) {
        if (isUnread) {
            Box(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(accent)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(
                    start = if (isUnread) 16.dp else 12.dp,
                    top = 12.dp,
                    end = 12.dp,
                    bottom = 12.dp
                ),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getNotificationIcon(category),
                    contentDescription = notification.type.getDisplayName(),
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    fontFamily = MeeshoFontFamily,
                    fontSize = 15.sp,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
                    color = NotifInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notification.message,
                    fontFamily = MeeshoFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = NotifMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Time + unread dot
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatNotificationTime(notification.createdAt),
                    fontFamily = MeeshoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = NotifFaint,
                    maxLines = 1
                )
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NotifCobalt)
                    )
                }
            }
        }
    }
}

private fun getNotificationIcon(category: NotifCategory): ImageVector = when (category) {
    NotifCategory.JOBS -> Icons.Outlined.WorkOutline
    NotifCategory.PAYMENTS -> Icons.Outlined.CreditCard
    else -> Icons.Outlined.VerifiedUser
}

/**
 * Relative time: "Just now", "10m ago", "1h ago", "Yesterday", "2d ago";
 * falls back to the centralized DateTimeUtils for anything older than a week.
 */
private fun formatNotificationTime(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val minutes = diff / 60_000L
    val hours = minutes / 60L
    val days = hours / 24L
    return when {
        minutes < 1L -> "Just now"
        minutes < 60L -> "${minutes}m ago"
        hours < 24L -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7L -> "${days}d ago"
        else -> DateTimeUtils.formatTimeAgo(timestamp)
    }
}
