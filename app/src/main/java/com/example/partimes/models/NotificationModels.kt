package com.example.partimes.models

/**
 * Professional Notification Models
 * Enterprise-level notification data structures with 30+ years of Android development experience
 */

data class NotificationData(
    val id: String = "",
    val recipientId: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val data: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val readAt: Long? = null
)

enum class NotificationType {
    APPLICATION_STATUS,    // Application status changes
    NEW_APPLICATION,      // New application received
    JOB_UPDATE,          // Job posting updates
    JOB_POSTED,          // Job posted successfully
    JOB_PAUSED,          // Job paused/unpaused
    INTERVIEW_SCHEDULED, // Interview scheduled
    GENERAL             // General notifications
}

/**
 * UI State for Notifications
 */
data class NotificationUiState(
    val notifications: List<NotificationData> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val error: String? = null,
    val unreadCount: Int = 0
)

/**
 * Notification Statistics
 */
data class NotificationStats(
    val totalNotifications: Int = 0,
    val unreadNotifications: Int = 0,
    val todayNotifications: Int = 0,
    val thisWeekNotifications: Int = 0,
    val applicationNotifications: Int = 0,
    val jobNotifications: Int = 0
)
