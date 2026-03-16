package com.example.dutype.models

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

/**
 * NotificationData - MINIMAL MODEL (8 fields)
 * Based on Urban Company/TaskRabbit patterns
 */
@Keep
data class NotificationData(
    val id: String = "",
    val recipientId: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.GENERAL,
    val targetRole: String = "",  // "WORKER", "EMPLOYER", or "" (both)
    val data: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (45L * 24 * 60 * 60 * 1000),
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false
)

enum class NotificationType {
    APPLICATION_STATUS,
    APPLICATION_STATUS_UPDATE,
    APPLICATION_REMINDER,
    NEW_APPLICATION,
    SHORTLISTED,
    REJECTED,
    JOB_UPDATE,
    JOB_POSTED,
    JOB_PAUSED,
    NEW_JOB_ALERT,
    JOB_RECOMMENDATION,
    JOB_EXPIRY_REMINDER,
    INTERVIEW_SCHEDULED,
    WORKER_HIRED,
    PROFILE_REMINDER,
    PROFILE_MILESTONE,
    PROFILE_COMPLETE,
    WELCOME,
    EMPLOYER_MESSAGE,
    BIRTHDAY,
    REFERRAL_MILESTONE,
    RE_ENGAGEMENT,
    WEEKLY_SUMMARY,
    SYSTEM_UPDATE,
    GENERAL
}

fun NotificationType.getDisplayName(): String = when (this) {
    NotificationType.APPLICATION_STATUS -> "Application Update"
    NotificationType.APPLICATION_STATUS_UPDATE -> "Application Status Update"
    NotificationType.APPLICATION_REMINDER -> "Pending Applications"
    NotificationType.NEW_APPLICATION -> "New Application"
    NotificationType.SHORTLISTED -> "Shortlisted"
    NotificationType.REJECTED -> "Application Rejected"
    NotificationType.JOB_UPDATE -> "Job Update"
    NotificationType.JOB_POSTED -> "Job Posted"
    NotificationType.JOB_PAUSED -> "Job Status Update"
    NotificationType.NEW_JOB_ALERT -> "New Job Alert"
    NotificationType.JOB_RECOMMENDATION -> "Job Recommendation"
    NotificationType.JOB_EXPIRY_REMINDER -> "Job Expiry"
    NotificationType.INTERVIEW_SCHEDULED -> "Interview Scheduled"
    NotificationType.WORKER_HIRED -> "Worker Hired"
    NotificationType.PROFILE_REMINDER -> "Profile Reminder"
    NotificationType.PROFILE_MILESTONE -> "Profile Milestone"
    NotificationType.PROFILE_COMPLETE -> "Profile Complete"
    NotificationType.WELCOME -> "Welcome"
    NotificationType.EMPLOYER_MESSAGE -> "Message"
    NotificationType.BIRTHDAY -> "Birthday Wish"
    NotificationType.REFERRAL_MILESTONE -> "Referral Reward"
    NotificationType.RE_ENGAGEMENT -> "We Miss You"
    NotificationType.WEEKLY_SUMMARY -> "Weekly Summary"
    NotificationType.SYSTEM_UPDATE -> "System Update"
    NotificationType.GENERAL -> "Notification"
}


// UI display model for notifications
@Keep
data class Notification(
    val id: String,
    val userId: String = "",
    val title: String,
    val message: String,
    val type: NotificationType,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val isRead: Boolean = false,
    val readAt: Long? = null,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val relatedJobId: String? = null,
    val relatedApplicationId: String? = null,
    val actionData: Map<String, String> = emptyMap()
) {
    /**
     * Get color for notification type
     */
    fun getColor(): Long = type.getColor()
    
    /**
     * Get icon for notification type
     */
    fun getIcon(): String = type.getIcon()
    
    /**
     * Get formatted time ago text
     */
    fun getFormattedTime(): String {
        val diff = System.currentTimeMillis() - createdAt
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> "${days / 7}w ago"
        }
    }
}

/**
 * Notification priority levels
 */
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Extension function to get color for notification type
 */
fun NotificationType.getColor(): Long = when (this) {
    NotificationType.APPLICATION_STATUS,
    NotificationType.APPLICATION_STATUS_UPDATE,
    NotificationType.SHORTLISTED -> 0xFF2196F3 // Blue
    
    NotificationType.NEW_APPLICATION,
    NotificationType.WORKER_HIRED -> 0xFF4CAF50 // Green
    
    NotificationType.REJECTED -> 0xFFF44336 // Red
    
    NotificationType.JOB_UPDATE,
    NotificationType.JOB_POSTED,
    NotificationType.NEW_JOB_ALERT,
    NotificationType.JOB_RECOMMENDATION -> 0xFF9C27B0 // Purple
    
    NotificationType.APPLICATION_REMINDER,
    NotificationType.JOB_EXPIRY_REMINDER,
    NotificationType.PROFILE_REMINDER -> 0xFFFFA500 // Orange
    
    NotificationType.BIRTHDAY,
    NotificationType.REFERRAL_MILESTONE -> 0xFFFF9800 // Amber
    
    else -> 0xFF757575 // Gray
}

/**
 * Extension function to get icon for notification type
 */
fun NotificationType.getIcon(): String = when (this) {
    NotificationType.APPLICATION_STATUS,
    NotificationType.APPLICATION_STATUS_UPDATE -> "📋"
    
    NotificationType.NEW_APPLICATION -> "📨"
    NotificationType.SHORTLISTED -> "⭐"
    NotificationType.REJECTED -> "❌"
    NotificationType.WORKER_HIRED -> "🎉"
    
    NotificationType.JOB_UPDATE,
    NotificationType.JOB_POSTED -> "💼"
    
    NotificationType.NEW_JOB_ALERT,
    NotificationType.JOB_RECOMMENDATION -> "🔔"
    
    NotificationType.JOB_EXPIRY_REMINDER,
    NotificationType.APPLICATION_REMINDER -> "⏰"
    
    NotificationType.INTERVIEW_SCHEDULED -> "📅"
    
    NotificationType.PROFILE_REMINDER,
    NotificationType.PROFILE_MILESTONE,
    NotificationType.PROFILE_COMPLETE -> "👤"
    
    NotificationType.WELCOME -> "👋"
    NotificationType.BIRTHDAY -> "🎂"
    NotificationType.REFERRAL_MILESTONE -> "🎁"
    NotificationType.EMPLOYER_MESSAGE -> "💬"
    NotificationType.SYSTEM_UPDATE -> "🔧"
    
    else -> "📢"
}

/**
 * Notification preferences for user
 */
@Keep
data class NotificationPreferences(
    val userId: String = "",
    val enablePushNotifications: Boolean = true,
    val enableEmailNotifications: Boolean = true,
    val enableSmsNotifications: Boolean = false,
    val enableInAppNotifications: Boolean = true,
    
    // Notification type preferences
    val enableApplicationUpdates: Boolean = true,
    val enableJobAlerts: Boolean = true,
    val enableProfileReminders: Boolean = true,
    val enableSystemUpdates: Boolean = true,
    val enableMarketingMessages: Boolean = false,
    
    // Quiet hours
    val enableQuietHours: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "08:00",
    
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Notification filter for filtering notifications by type
 */
enum class NotificationFilter {
    ALL,
    APPLICATIONS,
    JOBS,
    PROFILE,
    SYSTEM
}

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
