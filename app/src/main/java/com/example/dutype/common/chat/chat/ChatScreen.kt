package com.example.dutype.common.chat.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.ui.components.ReusableSearchBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Enhanced data class
data class ChatItem(
    val id: String,
    val name: String,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val avatarColor: Color = Color(0xFF3B82F6)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController) {
    // Enhanced color scheme
    val primaryColor = Color(0xFF3B82F6)
    val backgroundColor = Color(0xFFFAFAFA)
    val cardColor = Color.White
    val textPrimary = Color(0xFF111827)
    val textSecondary = Color(0xFF6B7280)

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val chatList = remember {
        listOf(
            ChatItem(
                id = "1",
                name = "Alice Johnson",
                lastMessage = "Hey, are you available tomorrow for the project discussion?",
                timestamp = System.currentTimeMillis() - 300000, // 5 minutes ago
                unreadCount = 2,
                isOnline = true,
                avatarColor = Color(0xFF10B981)
            ),
            ChatItem(
                id = "2",
                name = "Bob Smith",
                lastMessage = "Let's catch up at 5. I have some updates to share.",
                timestamp = System.currentTimeMillis() - 3600000, // 1 hour ago
                unreadCount = 0,
                isOnline = false,
                avatarColor = Color(0xFFF59E0B)
            ),
            ChatItem(
                id = "3",
                name = "Charlie Davis",
                lastMessage = "Interview has been scheduled for next Monday.",
                timestamp = System.currentTimeMillis() - 7200000, // 2 hours ago
                unreadCount = 1,
                isOnline = true,
                avatarColor = Color(0xFF8B5CF6)
            ),
            ChatItem(
                id = "4",
                name = "Diana Wilson",
                lastMessage = "Thanks for the quick response! Much appreciated.",
                timestamp = System.currentTimeMillis() - 86400000, // 1 day ago
                unreadCount = 0,
                isOnline = false,
                avatarColor = Color(0xFFEF4444)
            ),
            ChatItem(
                id = "5",
                name = "Emma Thompson",
                lastMessage = "The documents are ready for review.",
                timestamp = System.currentTimeMillis() - 172800000, // 2 days ago
                unreadCount = 0,
                isOnline = true,
                avatarColor = Color(0xFF06B6D4)
            )
        )
    }

    val filteredChats = remember(searchQuery, chatList) {
        if (searchQuery.isEmpty()) {
            chatList
        } else {
            chatList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        ReusableSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = "Search conversations...",
                            height = 40,
                            backgroundColor = Color.Transparent,
                            borderColor = Color.Transparent,
                            focusedBorderColor = primaryColor,
                            searchIconColor = primaryColor,
                            textColor = Color.Black,
                            placeholderColor = textSecondary
                        )
                    } else {
                        Text(
                            "Messages",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cardColor,
                    titleContentColor = textPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Start new chat */ },
                containerColor = primaryColor,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Chat"
                )
            }
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        if (filteredChats.isEmpty() && searchQuery.isNotEmpty()) {
            // Search results empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No conversations found",
                        style = MaterialTheme.typography.headlineSmall,
                        color = textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Try searching with different keywords",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary
                    )
                }
            }
        } else if (chatList.isEmpty()) {
            // Empty state when no chats exist
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = primaryColor.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier
                                .size(40.dp)
                                .padding(20.dp)
                        )
                    }
                    Text(
                        text = "No conversations yet",
                        style = MaterialTheme.typography.headlineSmall,
                        color = textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Start a new conversation to connect with others",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(filteredChats) { chat ->
                    EnhancedChatListItem(
                        chat = chat,
                        onClick = {
                            navController.navigate("chat_detail/${chat.name}")
                        },
                        primaryColor = primaryColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedChatListItem(
    chat: ChatItem,
    onClick: () -> Unit,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val now = Date()
    val messageDate = Date(chat.timestamp)
    val isToday = now.time - chat.timestamp < 24 * 60 * 60 * 1000

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (chat.unreadCount > 0) primaryColor.copy(alpha = 0.05f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enhanced Avatar with online indicator
            Box {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = chat.avatarColor.copy(alpha = 0.1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = chat.avatarColor,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Online indicator
                if (chat.isOnline) {
                    Surface(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd),
                        shape = CircleShape,
                        color = Color.White
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(12.dp)
                                .padding(2.dp),
                            shape = CircleShape,
                            color = Color(0xFF10B981)
                        ) {}
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (chat.unreadCount > 0) FontWeight.SemiBold else FontWeight.Medium,
                            color = textPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = if (isToday) timeFormatter.format(messageDate) else dateFormatter.format(messageDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (chat.unreadCount > 0) primaryColor else textSecondary,
                        fontWeight = if (chat.unreadCount > 0) FontWeight.Medium else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (chat.unreadCount > 0) textPrimary else textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        fontWeight = if (chat.unreadCount > 0) FontWeight.Medium else FontWeight.Normal
                    )

                    if (chat.unreadCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = primaryColor,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (chat.unreadCount > 9) "9+" else chat.unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
