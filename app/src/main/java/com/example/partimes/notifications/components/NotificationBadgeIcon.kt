package com.example.partimes.notifications.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
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
import com.example.partimes.notifications.viewmodels.NotificationCenterViewModel

/**
 * Notification badge icon for bottom navigation
 */
@Composable
fun NotificationBadgeIcon(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Notifications,
    contentDescription: String = "Notifications"
) {
    val viewModel: NotificationCenterViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    Box(
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface
        )
        
        // Show badge if there are unread notifications
        if (uiState.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.unreadCount > 99) "99+" else uiState.unreadCount.toString(),
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Notification badge for any icon
 */
@Composable
fun NotificationBadge(
    count: Int,
    modifier: Modifier = Modifier,
    maxCount: Int = 99
) {
    if (count > 0) {
        Box(
            modifier = modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444))
        )
    }
}

/**
 * Floating notification indicator
 */
@Composable
fun FloatingNotificationIndicator(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Box(
            modifier = modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444))
        )
    }
}
