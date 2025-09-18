package com.example.partimes.notifications.services

import com.example.partimes.notifications.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for triggering notifications based on app events
 */
@Singleton
class NotificationTriggerService @Inject constructor(
    private val notificationService: NotificationService
) {
    
    private val _notificationEvents = MutableStateFlow<List<NotificationEvent>>(emptyList())
    val notificationEvents: StateFlow<List<NotificationEvent>> = _notificationEvents.asStateFlow()
    
    /**
     * Trigger notification when a new job matches user's saved search
     */
    suspend fun triggerNewJobAlert(
        userId: String,
        jobTitle: String,
        companyName: String,
        jobId: String,
        searchName: String? = null
    ) {
        val title = "New Job Alert"
        val message = if (searchName != null) {
            "New $jobTitle position at $companyName matches your '$searchName' search"
        } else {
            "New $jobTitle position at $companyName matches your profile"
        }
        
        notificationService.createInAppNotification(
            userId = userId,
            title = title,
            message = message,
            type = NotificationType.NEW_JOB_ALERT,
            priority = NotificationPriority.NORMAL,
            relatedJobId = jobId,
            actionData = mapOf(
                "action" to "view_job",
                "job_id" to jobId
            )
        )
        
        addNotificationEvent(NotificationEvent.NEW_JOB_ALERT)
    }
    
    /**
     * Trigger notification when application status changes
     */
    suspend fun triggerApplicationStatusUpdate(
        userId: String,
        jobTitle: String,
        companyName: String,
        status: String,
        applicationId: String
    ) {
        val title = "Application Update"
        val message = "Your application for $jobTitle at $companyName has been $status"
        
        val priority = when (status.lowercase()) {
            "shortlisted", "accepted" -> NotificationPriority.HIGH
            "rejected" -> NotificationPriority.NORMAL
            else -> NotificationPriority.NORMAL
        }
        
        val type = when (status.lowercase()) {
            "shortlisted" -> NotificationType.SHORTLISTED
            "rejected" -> NotificationType.REJECTED
            else -> NotificationType.APPLICATION_STATUS_UPDATE
        }
        
        notificationService.createInAppNotification(
            userId = userId,
            title = title,
            message = message,
            type = type,
            priority = priority,
            relatedApplicationId = applicationId,
            actionData = mapOf(
                "action" to "view_application",
                "application_id" to applicationId
            )
        )
        
        addNotificationEvent(NotificationEvent.APPLICATION_UPDATE)
    }
    
    /**
     * Trigger notification when interview is scheduled
     */
    suspend fun triggerInterviewScheduled(
        userId: String,
        jobTitle: String,
        companyName: String,
        interviewDate: String,
        applicationId: String
    ) {
        val title = "Interview Scheduled"
        val message = "Your interview for $jobTitle at $companyName is scheduled for $interviewDate"
        
        notificationService.createInAppNotification(
            userId = userId,
            title = title,
            message = message,
            type = NotificationType.INTERVIEW_SCHEDULED,
            priority = NotificationPriority.URGENT,
            relatedApplicationId = applicationId,
            actionData = mapOf(
                "action" to "view_interview",
                "application_id" to applicationId
            )
        )
        
        addNotificationEvent(NotificationEvent.INTERVIEW_SCHEDULED)
    }
    
    /**
     * Trigger notification when employer sends a message
     */
    suspend fun triggerEmployerMessage(
        userId: String,
        employerName: String,
        messagePreview: String,
        applicationId: String
    ) {
        val title = "Message from $employerName"
        val message = messagePreview
        
        notificationService.createInAppNotification(
            userId = userId,
            title = title,
            message = message,
            type = NotificationType.EMPLOYER_MESSAGE,
            priority = NotificationPriority.HIGH,
            relatedApplicationId = applicationId,
            actionData = mapOf(
                "action" to "view_message",
                "application_id" to applicationId
            )
        )
        
        addNotificationEvent(NotificationEvent.EMPLOYER_MESSAGE)
    }
    
    /**
     * Trigger notification for job recommendations
     */
    suspend fun triggerJobRecommendation(
        userId: String,
        jobTitle: String,
        companyName: String,
        jobId: String,
        reason: String
    ) {
        val title = "Job Recommendation"
        val message = "We recommend $jobTitle at $companyName - $reason"
        
        notificationService.createInAppNotification(
            userId = userId,
            title = title,
            message = message,
            type = NotificationType.JOB_RECOMMENDATION,
            priority = NotificationPriority.NORMAL,
            relatedJobId = jobId,
            actionData = mapOf(
                "action" to "view_job",
                "job_id" to jobId
            )
        )
        
        addNotificationEvent(NotificationEvent.JOB_RECOMMENDATION)
    }
    
    /**
     * Trigger system update notification
     */
    suspend fun triggerSystemUpdate(
        userId: String,
        title: String,
        message: String
    ) {
        notificationService.createInAppNotification(
            userId = userId,
            title = title,
            message = message,
            type = NotificationType.SYSTEM_UPDATE,
            priority = NotificationPriority.LOW,
            actionData = mapOf(
                "action" to "view_update"
            )
        )
        
        addNotificationEvent(NotificationEvent.SYSTEM_UPDATE)
    }
    
    /**
     * Trigger welcome notification for new users
     */
    suspend fun triggerWelcomeNotification(userId: String, userName: String) {
        notificationService.createInAppNotification(
            userId = userId,
            title = "Welcome to ParTimes!",
            message = "Hi $userName! Complete your profile to get better job matches.",
            type = NotificationType.SYSTEM_UPDATE,
            priority = NotificationPriority.NORMAL,
            actionData = mapOf(
                "action" to "complete_profile"
            )
        )
        
        addNotificationEvent(NotificationEvent.WELCOME)
    }
    
    /**
     * Trigger profile completion reminder
     */
    suspend fun triggerProfileCompletionReminder(userId: String, completionPercentage: Int) {
        notificationService.createInAppNotification(
            userId = userId,
            title = "Complete Your Profile",
            message = "Your profile is $completionPercentage% complete. Add more details to get better matches!",
            type = NotificationType.SYSTEM_UPDATE,
            priority = NotificationPriority.LOW,
            actionData = mapOf(
                "action" to "complete_profile"
            )
        )
        
        addNotificationEvent(NotificationEvent.PROFILE_REMINDER)
    }
    
    /**
     * Add notification event to tracking
     */
    private fun addNotificationEvent(event: NotificationEvent) {
        val currentEvents = _notificationEvents.value.toMutableList()
        currentEvents.add(event)
        
        // Keep only the last 50 events
        if (currentEvents.size > 50) {
            currentEvents.removeAt(0)
        }
        
        _notificationEvents.value = currentEvents
    }
    
    /**
     * Get notification events
     */
    fun getNotificationEvents(): Flow<List<NotificationEvent>> = notificationEvents
}

/**
 * Enum for tracking notification events
 */
enum class NotificationEvent {
    NEW_JOB_ALERT,
    APPLICATION_UPDATE,
    INTERVIEW_SCHEDULED,
    EMPLOYER_MESSAGE,
    JOB_RECOMMENDATION,
    SYSTEM_UPDATE,
    WELCOME,
    PROFILE_REMINDER
}
