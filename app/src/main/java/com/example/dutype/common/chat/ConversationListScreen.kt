package com.example.dutype.common.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dutype.components.CommonHeader
import com.example.dutype.services.ChatService
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.DateTimeUtils

/**
 * Conversation List Screen - P1 FIX #8
 * 
 * Shows all chat conversations for the current user.
 * Real-time updates via Firestore listeners.
 * Updated with Meesho-style colors and typography.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    chatService: ChatService,
    onBackClick: () -> Unit,
    onConversationClick: (String) -> Unit
) {
    val conversations by chatService.getConversationsFlow().collectAsState(initial = emptyList())
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkerColors.ScreenBackground)
    ) {
        // CommonHeader with no back button (accessed from bottom nav)
        CommonHeader(
            title = "Messages",
            showBackButton = false,
            backgroundColor = WorkerColors.CardBackground,
            titleColor = WorkerColors.TextPrimary
        )
        if (conversations.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = WorkerColors.IconSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No conversations yet",
                        style = AppTypography.emptyStateTitle.copy(
                            color = WorkerColors.TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start chatting with employers or workers",
                        style = AppTypography.emptyStateSubtitle.copy(
                            color = WorkerColors.TextTertiary
                        )
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                items(conversations) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        chatService = chatService,
                        onClick = { onConversationClick(conversation.id) }
                    )
                    HorizontalDivider(color = WorkerColors.Divider)
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: ChatService.Conversation,
    chatService: ChatService,
    onClick: () -> Unit
) {
    val otherParticipant = chatService.getOtherParticipant(conversation)
    val unreadCount = chatService.getUnreadCount(conversation)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WorkerColors.CardBackground)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Image
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(WorkerColors.ChipBackground),
            contentAlignment = Alignment.Center
        ) {
            if (otherParticipant?.profileImage != null) {
                AsyncImage(
                    model = otherParticipant.profileImage,
                    contentDescription = "Profile",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = WorkerColors.IconSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Name and last message
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = otherParticipant?.name ?: "Unknown",
                    style = AppTypography.cardTitle.copy(
                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                        color = WorkerColors.TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // Time
                conversation.lastMessageAt?.let { timestamp ->
                    Text(
                        text = formatTimestamp(timestamp),
                        style = AppTypography.caption.copy(
                            color = if (unreadCount > 0) WorkerColors.Info else WorkerColors.TextTertiary
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessage ?: "No messages yet",
                    style = AppTypography.bodyMedium.copy(
                        color = if (unreadCount > 0) WorkerColors.TextPrimary else WorkerColors.TextSecondary,
                        fontWeight = if (unreadCount > 0) FontWeight.Medium else FontWeight.Normal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // Unread badge
                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(WorkerColors.Info),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                            style = AppTypography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
            
            // Role badge
            otherParticipant?.role?.let { role ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (role == "EMPLOYER") "Employer" else "Worker",
                    style = AppTypography.labelSmall.copy(
                        color = if (role == "EMPLOYER") WorkerColors.Primary else WorkerColors.Success
                    ),
                    modifier = Modifier
                        .background(
                            color = if (role == "EMPLOYER") WorkerColors.PrimaryLight else WorkerColors.SuccessLight,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return DateTimeUtils.formatRelativeTime(timestamp)
}
