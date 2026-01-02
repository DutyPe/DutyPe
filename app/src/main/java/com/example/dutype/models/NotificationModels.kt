package com.example.dutype.models

import androidx.annotation.Keep
import com.example.dutype.utils.DateTimeUtils
import com.google.firebase.firestore.PropertyName

/**
 * Professional Notification Models
 * Enterprise-level notification data structures
 * 
 * REFACTORED: Consolidated from duplicate notifications/models/NotificationModels.kt
 * This is now the SINGLE SOURCE OF TRUTH for all notification models.
 */

// =============================================================================
// FIRESTORE DATA MODEL
// =============================================================================

/**
 * NotificationData - Firestore document model
 * Used for storing/retrieving notifications from Firebase
 */
@Keep
data class NotificationData(
    val id: String = "",
    val recipientId: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.GENERAL,
    val data: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,
    val read: Boolean = false,
    val readAt: Long? = null,
    val sentAt: Any? = null,
    val error: String? = null,
    val fcmMessageId: String? = null
)

// =============================================================================
// UI DISPLAY MODEL
// =============================================================================

/**
 * Notification - UI display model
 * Used for rendering notifications in screens
 */
@Keep
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
    val actionData: Map<String, String> = emptyMap(),
    val imageUrl: String? = null,
    val userId: String = "",
    val relatedJobId: String? = null,
    val relatedApplicationId: String? = null
)

// =============================================================================
// ENUMS
// =============================================================================

/**
 * NotificationType - All notification types in the app
 * Used for both Firestore storage and UI display
 */
enum class NotificationType {
    // Application related
    APPLICATION_STATUS,
    APPLICATION_STATUS_UPDATE, // Alias for UI compatibility
    NEW_APPLICATION,
    SHORTLISTED,
    REJECTED,
    
    // Job related
    JOB_UPDATE,
    JOB_POSTED,
    JOB_PAUSED,
    NEW_JOB_ALERT,
    JOB_RECOMMENDATION,
    JOB_EXPIRY_REMINDER,
    
    // Interview & Work
    INTERVIEW_SCHEDULED,
    WORKER_HIRED,
    
    // Profile & Account
    PROFILE_COMPLETE,
    PROFILE_REMINDER,
    WELCOME,
    
    // Communication
    EMPLOYER_MESSAGE,
    
    // System
    SYSTEM_UPDATE,
    WEEKLY_SUMMARY,
    GENERAL
}

/**
 * NotificationPriority - Priority levels for notifications
 */
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * NotificationFilter - Filter types for notification lists
 */
enum class NotificationFilter(val displayName: String) {
    ALL("All"),
    UNREAD("Unread"),
    READ("Read"),
    ARCHIVED("Archived"),
    APPLICATIONS("Applications"),
    JOBS("Jobs"),
    MESSAGES("Messages"),
    SYSTEM("System")
}

// =============================================================================
// UI STATE & STATS
// =============================================================================

/**
 * NotificationUiState - UI state for notification screens
 */
data class NotificationUiState(
    val notifications: List<NotificationData> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val error: String? = null,
    val unreadCount: Int = 0
)

/**
 * NotificationStats - Statistics for notifications
 */
data class NotificationStats(
    val totalNotifications: Int = 0,
    val unreadCount: Int = 0,
    val todayCount: Int = 0,
    val thisWeekCount: Int = 0,
    val byType: Map<NotificationType, Int> = emptyMap()
)

/**
 * NotificationPreferences - User notification preferences
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

// =============================================================================
// EXTENSION FUNCTIONS
// =============================================================================

/**
 * Get display name for notification type
 */
fun NotificationType.getDisplayName(): String {
    return when (this) {
        NotificationType.APPLICATION_STATUS,
        NotificationType.APPLICATION_STATUS_UPDATE -> "Application Update"
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
        NotificationType.PROFILE_COMPLETE -> "Profile Complete"
        NotificationType.PROFILE_REMINDER -> "Profile Reminder"
        NotificationType.WELCOME -> "Welcome"
        NotificationType.EMPLOYER_MESSAGE -> "Message"
        NotificationType.SYSTEM_UPDATE -> "System Update"
        NotificationType.WEEKLY_SUMMARY -> "Weekly Summary"
        NotificationType.GENERAL -> "Notification"
    }
}

/**
 * Get icon emoji for notification type
 */
fun NotificationType.getIcon(): String {
    return when (this) {
        NotificationType.APPLICATION_STATUS,
        NotificationType.APPLICATION_STATUS_UPDATE -> "📋"
        NotificationType.NEW_APPLICATION -> "👤"
        NotificationType.SHORTLISTED -> "✅"
        NotificationType.REJECTED -> "❌"
        NotificationType.JOB_UPDATE -> "📝"
        NotificationType.JOB_POSTED -> "📝"
        NotificationType.JOB_PAUSED -> "⏸️"
        NotificationType.NEW_JOB_ALERT -> "🔔"
        NotificationType.JOB_RECOMMENDATION -> "💡"
        NotificationType.JOB_EXPIRY_REMINDER -> "⏰"
        NotificationType.INTERVIEW_SCHEDULED -> "📅"
        NotificationType.WORKER_HIRED -> "🎉"
        NotificationType.PROFILE_COMPLETE -> "✅"
        NotificationType.PROFILE_REMINDER -> "📝"
        NotificationType.WELCOME -> "👋"
        NotificationType.EMPLOYER_MESSAGE -> "💬"
        NotificationType.SYSTEM_UPDATE -> "⚙️"
        NotificationType.WEEKLY_SUMMARY -> "📊"
        NotificationType.GENERAL -> "🔔"
    }
}

/**
 * Get color for notification type (as Long for Compose Color)
 */
fun NotificationType.getColor(): Long {
    return when (this) {
        NotificationType.APPLICATION_STATUS,
        NotificationType.APPLICATION_STATUS_UPDATE -> 0xFFFF9800
        NotificationType.NEW_APPLICATION -> 0xFF2196F3
        NotificationType.SHORTLISTED -> 0xFF4CAF50
        NotificationType.REJECTED -> 0xFFF44336
        NotificationType.JOB_UPDATE -> 0xFF2196F3
        NotificationType.JOB_POSTED -> 0xFF4CAF50
        NotificationType.JOB_PAUSED -> 0xFFFF9800
        NotificationType.NEW_JOB_ALERT -> 0xFF2196F3
        NotificationType.JOB_RECOMMENDATION -> 0xFF3F51B5
        NotificationType.JOB_EXPIRY_REMINDER -> 0xFFFF5722
        NotificationType.INTERVIEW_SCHEDULED -> 0xFF9C27B0
        NotificationType.WORKER_HIRED -> 0xFF4CAF50
        NotificationType.PROFILE_COMPLETE -> 0xFF4CAF50
        NotificationType.PROFILE_REMINDER -> 0xFF795548
        NotificationType.WELCOME -> 0xFF2196F3
        NotificationType.EMPLOYER_MESSAGE -> 0xFF00BCD4
        NotificationType.SYSTEM_UPDATE -> 0xFF607D8B
        NotificationType.WEEKLY_SUMMARY -> 0xFF673AB7
        NotificationType.GENERAL -> 0xFF607D8B
    }
}

/**
 * Get display name for notification priority
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
 * Get formatted time for notification
 */
fun Notification.getFormattedTime(): String = DateTimeUtils.formatTimeAgo(createdAt)

/**
 * Get formatted time for notification data
 */
fun NotificationData.getFormattedTime(): String = DateTimeUtils.formatTimeAgo(createdAt)
