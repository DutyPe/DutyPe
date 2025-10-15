package com.example.dutype.notifications.models

import java.util.Date

/**
 * Types of notifications in the app
 */
enum class NotificationType {
    NEW_JOB_ALERT,
    APPLICATION_STATUS_UPDATE,
    SHORTLISTED,
    REJECTED,
    INTERVIEW_SCHEDULED,
    EMPLOYER_MESSAGE,
    JOB_RECOMMENDATION,
    SYSTEM_UPDATE
}

/**
 * Priority levels for notifications
 */
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Filter types for notifications
 */
enum class NotificationFilter(val displayName: String) {
    ALL("All"),
    UNREAD("Unread"),
    READ("Read"),
    ARCHIVED("Archived"),
    APPLICATIONS("Applications"),
    JOBS("Jobs")
}

/**
 * Data class representing a notification
 */
data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val isRead: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val readAt: Long? = null,
    val archivedAt: Long? = null,
    val actionData: Map<String, String> = emptyMap(), // For deep linking
    val imageUrl: String? = null,
    val userId: String,
    val relatedJobId: String? = null,
    val relatedApplicationId: String? = null
)

/**
 * Notification preferences for user
 */
data class NotificationPreferences(
    val userId: String,
    val enablePushNotifications: Boolean = true,
    val enableEmailNotifications: Boolean = true,
    val enableInAppNotifications: Boolean = true,
    val newJobAlerts: Boolean = true,
    val applicationUpdates: Boolean = true,
    val interviewReminders: Boolean = true,
    val employerMessages: Boolean = true,
    val jobRecommendations: Boolean = true,
    val systemUpdates: Boolean = false,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "08:00",
    val timezone: String = "UTC"
)

/**
 * Notification statistics
 */
data class NotificationStats(
    val totalNotifications: Int = 0,
    val unreadCount: Int = 0,
    val todayCount: Int = 0,
    val thisWeekCount: Int = 0,
    val byType: Map<NotificationType, Int> = emptyMap()
)

/**
 * Extension functions for NotificationType
 */
fun NotificationType.getDisplayName(): String {
    return when (this) {
        NotificationType.NEW_JOB_ALERT -> "New Job Alert"
        NotificationType.APPLICATION_STATUS_UPDATE -> "Application Update"
        NotificationType.SHORTLISTED -> "Shortlisted"
        NotificationType.REJECTED -> "Application Rejected"
        NotificationType.INTERVIEW_SCHEDULED -> "Interview Scheduled"
        NotificationType.EMPLOYER_MESSAGE -> "Employer Message"
        NotificationType.JOB_RECOMMENDATION -> "Job Recommendation"
        NotificationType.SYSTEM_UPDATE -> "System Update"
    }
}

fun NotificationType.getIcon(): String {
    return when (this) {
        NotificationType.NEW_JOB_ALERT -> "🔔"
        NotificationType.APPLICATION_STATUS_UPDATE -> "📋"
        NotificationType.SHORTLISTED -> "✅"
        NotificationType.REJECTED -> "❌"
        NotificationType.INTERVIEW_SCHEDULED -> "📅"
        NotificationType.EMPLOYER_MESSAGE -> "💬"
        NotificationType.JOB_RECOMMENDATION -> "💡"
        NotificationType.SYSTEM_UPDATE -> "⚙️"
    }
}

fun NotificationType.getColor(): Long {
    return when (this) {
        NotificationType.NEW_JOB_ALERT -> 0xFF2196F3
        NotificationType.APPLICATION_STATUS_UPDATE -> 0xFFFF9800
        NotificationType.SHORTLISTED -> 0xFF4CAF50
        NotificationType.REJECTED -> 0xFFF44336
        NotificationType.INTERVIEW_SCHEDULED -> 0xFF9C27B0
        NotificationType.EMPLOYER_MESSAGE -> 0xFF00BCD4
        NotificationType.JOB_RECOMMENDATION -> 0xFF3F51B5
        NotificationType.SYSTEM_UPDATE -> 0xFF607D8B
    }
}

/**
 * Extension functions for NotificationPriority
 */
fun NotificationPriority.getDisplayName(): String {
    return when (this) {
        NotificationPriority.LOW -> "Low"
        NotificationPriority.NORMAL -> "Normal"
        NotificationPriority.HIGH -> "High"
        NotificationPriority.URGENT -> "Urgent"
    }
}

/**
 * Helper function to format notification time
 */
fun Notification.getFormattedTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - createdAt
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} minutes ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        diff < 604800_000 -> "${diff / 86400_000} days ago"
        else -> Date(createdAt).toString()
    }
}
