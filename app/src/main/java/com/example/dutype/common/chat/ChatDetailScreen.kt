package com.example.dutype.common.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dutype.services.ChatService
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.DateTimeUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Chat Detail Screen - P1 FIX #8
 * 
 * Real-time chat interface between two users.
 * Messages update in real-time via Firestore listeners.
 * Updated with Meesho-style colors and typography.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    chatService: ChatService,
    onBackClick: () -> Unit
) {
    val messages by chatService.getMessagesFlow(conversationId).collectAsState(initial = emptyList())
    val conversations by chatService.getConversationsFlow().collectAsState(initial = emptyList())
    
    val conversation = conversations.find { it.id == conversationId }
    val otherParticipant = conversation?.let { chatService.getOtherParticipant(it) }
    
    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
    // Mark messages as read when screen opens
    LaunchedEffect(conversationId) {
        chatService.markMessagesAsRead(conversationId)
    }
    
    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Profile image
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WorkerColors.ChipBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            if (otherParticipant?.profileImage != null) {
                                com.example.dutype.components.OptimizedProfileImage(
                                    imageUrl = otherParticipant.profileImage,
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard),
                                    tint = WorkerColors.IconSecondary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = otherParticipant?.name ?: "Chat",
                                style = AppTypography.cardTitle.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            otherParticipant?.role?.let { role ->
                                Text(
                                    text = if (role == "EMPLOYER") "Employer" else "Worker",
                                    style = AppTypography.caption.copy(
                                        color = WorkerColors.TextSecondary
                                    )
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = WorkerColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WorkerColors.CardBackground
                )
            )
        },
        bottomBar = {
            // Message input
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = WorkerColors.CardBackground
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...", style = AppTypography.bodyMedium.copy(color = WorkerColors.TextTertiary)) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WorkerColors.Info,
                            unfocusedBorderColor = WorkerColors.Border
                        ),
                        maxLines = 4
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Send button
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank() && !isSending) {
                                val text = messageText.trim()
                                messageText = ""
                                isSending = true
                                
                                scope.launch {
                                    chatService.sendMessage(conversationId, text)
                                    isSending = false
                                }
                            }
                        },
                        enabled = messageText.isNotBlank() && !isSending,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (messageText.isNotBlank()) WorkerColors.Info 
                                else WorkerColors.ChipBackground
                            )
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = WorkerColors.CardBackground,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (messageText.isNotBlank()) WorkerColors.CardBackground else WorkerColors.IconSecondary
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (messages.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WorkerColors.ScreenBackground)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No messages yet",
                        style = AppTypography.emptyStateTitle.copy(
                            color = WorkerColors.TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Say hello! 👋",
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
                    .background(WorkerColors.ScreenBackground)
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        isFromMe = message.senderId == currentUserId
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatService.ChatMessage,
    isFromMe: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isFromMe) 16.dp else 4.dp,
                        bottomEnd = if (isFromMe) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isFromMe) WorkerColors.Info else WorkerColors.ChipBackground
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.message,
                style = AppTypography.bodyLarge.copy(
                    color = if (isFromMe) WorkerColors.CardBackground else WorkerColors.TextPrimary
                )
            )
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        // Timestamp and read status
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatMessageTime(message.createdAt),
                style = AppTypography.labelSmall.copy(
                    color = WorkerColors.TextTertiary
                )
            )
            
            if (isFromMe && message.isRead) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "✓✓",
                    style = AppTypography.labelSmall.copy(
                        color = WorkerColors.Info
                    )
                )
            }
        }
    }
}

private fun formatMessageTime(timestamp: Long): String {
    return DateTimeUtils.formatTime(timestamp)
}
