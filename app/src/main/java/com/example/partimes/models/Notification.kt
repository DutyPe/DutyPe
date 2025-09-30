package com.example.partimes.models

data class Notification(
    val notificationId: String? = null,
    val userId: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val isRead: Boolean = false,
    val createdAt: String? = null,
    val readAt: String? = null,
    
    // Additional data for different notification types
    val jobId: String? = null,
    val applicationId: String? = null,
    val employerId: String? = null,
    val workerId: String? = null,
    val actionUrl: String? = null,
    val metadata: Map<String, Any>? = null
)

enum class NotificationType {
    JOB_APPLICATION,
    APPLICATION_STATUS_UPDATE,
    JOB_POSTED,
    JOB_EXPIRED,
    INTERVIEW_SCHEDULED,
    INTERVIEW_REMINDER,
    JOB_OFFER,
    JOB_REJECTION,
    PROFILE_UPDATE,
    SYSTEM_ANNOUNCEMENT,
    SECURITY_ALERT,
    PAYMENT_RECEIVED,
    REFERRAL_BONUS,
    GENERAL
}

enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}
