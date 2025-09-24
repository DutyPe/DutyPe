package com.example.partimes.notifications.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.partimes.notifications.models.*

/**
 * In-app notification banner that appears at the top of the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppNotificationBanner(
    notification: Notification,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    
    // Auto-dismiss after 5 seconds
    LaunchedEffect(notification.id) {
        kotlinx.coroutines.delay(5000)
        isVisible = false
        onDismiss()
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onClick() },
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Notification Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Color(notification.type.getColor()).copy(alpha = 0.1f)
                        ),
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
                    Text(
                        text = notification.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = notification.message,
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = notification.getFormattedTime(),
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
                
                // Priority Indicator
                if (notification.priority == NotificationPriority.HIGH || 
                    notification.priority == NotificationPriority.URGENT) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (notification.priority) {
                                    NotificationPriority.HIGH -> Color(0xFFFF9800)
                                    NotificationPriority.URGENT -> Color(0xFFF44336)
                                    else -> Color.Transparent
                                }
                            )
                    )
                }
                
                // Dismiss Button
                IconButton(
                    onClick = {
                        isVisible = false
                        onDismiss()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Notification toast for quick messages
 */
@Composable
fun NotificationToast(
    message: String,
    type: NotificationType = NotificationType.SYSTEM_UPDATE,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    
    // Auto-dismiss after 3 seconds
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(3000)
        isVisible = false
        onDismiss()
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1F2937)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = type.getIcon(),
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = {
                        isVisible = false
                        onDismiss()
                    },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

