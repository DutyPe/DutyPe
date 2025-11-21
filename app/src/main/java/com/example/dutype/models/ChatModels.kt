package com.example.dutype.models

import com.google.firebase.Timestamp

data class ChatChannel(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageTimestamp: Long = 0,
    val participants: List<User> = emptyList(), // Hydrated user data
    val unreadCount: Map<String, Int> = emptyMap() // userId -> count
)

data class ChatMessage(
    val id: String = "",
    val channelId: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: MessageType = MessageType.TEXT
)

enum class MessageType {
    TEXT,
    IMAGE,
    JOB_OFFER,
    APPLICATION_STATUS
}
