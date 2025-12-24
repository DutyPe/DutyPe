package com.example.dutype.common.chat.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data classes for better structure
data class ChatMessage(
    val id: String,
    val text: String,
    val timestamp: Long,
    val isFromUser: Boolean,
    val isDelivered: Boolean = true,
    val isRead: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(chatName: String, navController: NavController) {
    // Enhanced color scheme
    val primaryColor = Color(0xFF3B82F6)
    val lightGray = Color(0xFFF3F4F6)
    val darkText = Color(0xFF111827)
    val mediumGray = Color(0xFF6B7280)

    // State management
    var messageText by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            ChatMessage("1", "Any availability, this Friday?", System.currentTimeMillis() - 3600000, false),
            ChatMessage("2", "I'm available 12 PM-4 PM", System.currentTimeMillis() - 1800000, true),
            ChatMessage("3", "2 PM works for me", System.currentTimeMillis() - 900000, false),
            ChatMessage("4", "Perfect! See you then", System.currentTimeMillis() - 300000, true),
            ChatMessage("5", "Looking forward to it", System.currentTimeMillis() - 60000, false)
        )
    }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        // Enhanced Header with better styling
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding()))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = darkText
                        )
                    }

                    // Profile avatar
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = primaryColor.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = primaryColor,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chatName,
                            color = darkText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Online",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(onClick = { /* Call */ }) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = primaryColor
                        )
                    }

                    IconButton(onClick = { /* More options */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = mediumGray
                        )
                    }
                }
            }
        }

        // Enhanced Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 0.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                EnhancedChatBubble(
                    message = message,
                    primaryColor = primaryColor,
                    darkText = darkText
                )
            }
        }

        // Enhanced Message Input
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Emoji button
                IconButton(
                    onClick = { /* Open emoji picker */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEmotions,
                        contentDescription = "Emoji",
                        tint = mediumGray
                    )
                }

                // Message input field
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = {
                        Text(
                            "Type a message...",
                            color = mediumGray,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp, max = 120.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = lightGray,
                        unfocusedContainerColor = lightGray,
                        focusedIndicatorColor = primaryColor,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = primaryColor
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Attach file button
                IconButton(
                    onClick = { /* Attach file */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach",
                        tint = mediumGray
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Send button
                Surface(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            val newMessage = ChatMessage(
                                id = (messages.size + 1).toString(),
                                text = messageText.trim(),
                                timestamp = System.currentTimeMillis(),
                                isFromUser = true
                            )
                            messages.add(newMessage)
                            messageText = ""
                        }
                    },
                    shape = CircleShape,
                    color = if (messageText.isNotBlank()) primaryColor else lightGray,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank()) Color.White else mediumGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedChatBubble(
    message: ChatMessage,
    primaryColor: Color,
    darkText: Color
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (message.isFromUser) Modifier.padding(start = 48.dp)
                else Modifier.padding(end = 48.dp)
            ),
        horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (message.isFromUser) 20.dp else 6.dp,
                bottomEnd = if (message.isFromUser) 6.dp else 20.dp
            ),
            color = if (message.isFromUser) primaryColor else Color.White,
            shadowElevation = if (message.isFromUser) 0.dp else 1.dp
        ) {
            Text(
                text = message.text,
                color = if (message.isFromUser) Color.White else darkText,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                lineHeight = 20.sp
            )
        }

        Row(
            modifier = Modifier
                .padding(top = 4.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
        ) {
            Text(
                text = timeFormatter.format(Date(message.timestamp)),
                color = Color(0xFF9CA3AF),
                fontSize = 11.sp
            )

            if (message.isFromUser) {
                Spacer(modifier = Modifier.width(4.dp))
                // Delivery status indicators
                Text(
                    text = if (message.isRead) "Read" else if (message.isDelivered) "Delivered" else "Sent",
                    color = Color(0xFF9CA3AF),
                    fontSize = 10.sp
                )
            }
        }
    }
}
