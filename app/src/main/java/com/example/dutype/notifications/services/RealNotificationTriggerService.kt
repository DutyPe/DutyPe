package com.example.dutype.notifications.services

import com.example.dutype.notifications.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that triggers real notifications based on app events
 * This service monitors app activities and creates relevant notifications
 */
@Singleton
class RealNotificationTriggerService @Inject constructor(
    private val notificationService: LocalNotificationService,
    private val firebaseNotificationService: com.example.dutype.services.FirebaseNotificationService
) {
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Trigger notification when a worker applies for a job
     */
    fun onWorkerAppliedForJob(
        employerId: String,
        workerName: String,
        jobTitle: String,
        jobId: String,
        applicationId: String
    ) {
        coroutineScope.launch {
            // Create in-app notification
            notificationService.createNotification(
                userId = employerId,
                title = "New Job Application",
                message = "$workerName applied for your job: $jobTitle",
                type = NotificationType.APPLICATION_STATUS_UPDATE,
                priority = NotificationPriority.HIGH,
                relatedJobId = jobId,
                relatedApplicationId = applicationId,
                actionData = mapOf(
                    "action" to "view_application",
                    "jobId" to jobId,
                    "applicationId" to applicationId
                )
            )
            
            // Send Firebase push notification
            firebaseNotificationService.sendJobApplicationNotification(
                employerId = employerId,
                workerName = workerName,
                jobTitle = jobTitle,
                jobId = jobId,
                applicationId = applicationId
            )
        }
    }
    
    /**
     * Trigger notification when application status changes
     */
    fun onApplicationStatusChanged(
        workerId: String,
        jobTitle: String,
        status: String,
        jobId: String,
        applicationId: String
    ) {
        val notificationData = when (status.lowercase()) {
            "shortlisted" -> {
                Triple("Application Shortlisted", "Congratulations! Your application for $jobTitle has been shortlisted", NotificationType.SHORTLISTED to NotificationPriority.HIGH)
            }
            "rejected" -> {
                Triple("Application Update", "Your application for $jobTitle was not selected this time", NotificationType.REJECTED to NotificationPriority.NORMAL)
            }
            "accepted" -> {
                Triple("Application Accepted", "Great news! Your application for $jobTitle has been accepted", NotificationType.SHORTLISTED to NotificationPriority.URGENT)
            }
            "interview_scheduled" -> {
                Triple("Interview Scheduled", "An interview has been scheduled for your application to $jobTitle", NotificationType.INTERVIEW_SCHEDULED to NotificationPriority.HIGH)
            }
            else -> {
                Triple("Application Update", "Your application for $jobTitle status has been updated to $status", NotificationType.APPLICATION_STATUS_UPDATE to NotificationPriority.NORMAL)
            }
        }
        
        val (title, message, typePriority) = notificationData
        val (type, priority) = typePriority
        
        coroutineScope.launch {
            // Create in-app notification
            notificationService.createNotification(
                userId = workerId,
                title = title,
                message = message,
                type = type,
                priority = priority,
                relatedJobId = jobId,
                relatedApplicationId = applicationId,
                actionData = mapOf(
                    "action" to "view_application",
                    "jobId" to jobId,
                    "applicationId" to applicationId
                )
            )
            
            // Send Firebase push notification
            firebaseNotificationService.sendApplicationStatusNotification(
                workerId = workerId,
                jobTitle = jobTitle,
                status = status,
                jobId = jobId,
                applicationId = applicationId
            )
        }
    }
    
    /**
     * Trigger notification when a new job is posted that matches worker preferences
     */
    fun onNewJobPosted(
        workerId: String,
        jobTitle: String,
        companyName: String,
        jobId: String,
        category: String
    ) {
        coroutineScope.launch {
            notificationService.createNotification(
                userId = workerId,
                title = "New Job Alert",
                message = "New $category job posted by $companyName: $jobTitle",
                type = NotificationType.NEW_JOB_ALERT,
                priority = NotificationPriority.NORMAL,
                relatedJobId = jobId,
                actionData = mapOf(
                    "action" to "view_job",
                    "jobId" to jobId
                )
            )
        }
    }
    
    /**
     * Trigger notification when employer receives a message
     */
    fun onEmployerReceivedMessage(
        employerId: String,
        workerName: String,
        jobTitle: String,
        jobId: String
    ) {
        coroutineScope.launch {
            notificationService.createNotification(
                userId = employerId,
                title = "New Message",
                message = "You received a message from $workerName regarding $jobTitle",
                type = NotificationType.EMPLOYER_MESSAGE,
                priority = NotificationPriority.NORMAL,
                relatedJobId = jobId,
                actionData = mapOf(
                    "action" to "view_chat",
                    "jobId" to jobId
                )
            )
        }
    }
    
    /**
     * Trigger notification when worker receives a message
     */
    fun onWorkerReceivedMessage(
        workerId: String,
        employerName: String,
        jobTitle: String,
        jobId: String
    ) {
        coroutineScope.launch {
            notificationService.createNotification(
                userId = workerId,
                title = "New Message",
                message = "You received a message from $employerName regarding $jobTitle",
                type = NotificationType.EMPLOYER_MESSAGE,
                priority = NotificationPriority.NORMAL,
                relatedJobId = jobId,
                actionData = mapOf(
                    "action" to "view_chat",
                    "jobId" to jobId
                )
            )
        }
    }
    
    /**
     * Trigger notification for job recommendations
     */
    fun onJobRecommendation(
        workerId: String,
        jobTitle: String,
        companyName: String,
        jobId: String,
        reason: String
    ) {
        coroutineScope.launch {
            notificationService.createNotification(
                userId = workerId,
                title = "Job Recommendation",
                message = "Based on your profile, we recommend: $jobTitle at $companyName. $reason",
                type = NotificationType.JOB_RECOMMENDATION,
                priority = NotificationPriority.LOW,
                relatedJobId = jobId,
                actionData = mapOf(
                    "action" to "view_job",
                    "jobId" to jobId
                )
            )
        }
    }
    
    /**
     * Trigger notification when job is about to expire
     */
    fun onJobExpiringSoon(
        employerId: String,
        jobTitle: String,
        jobId: String,
        daysLeft: Int
    ) {
        coroutineScope.launch {
            notificationService.createNotification(
                userId = employerId,
                title = "Job Expiring Soon",
                message = "Your job posting '$jobTitle' expires in $daysLeft days. Consider extending it.",
                type = NotificationType.SYSTEM_UPDATE,
                priority = if (daysLeft <= 1) NotificationPriority.HIGH else NotificationPriority.NORMAL,
                relatedJobId = jobId,
                actionData = mapOf(
                    "action" to "extend_job",
                    "jobId" to jobId
                )
            )
        }
    }
    
    /**
     * Trigger notification for weekly summary
     */
    fun onWeeklySummary(
        userId: String,
        isEmployer: Boolean,
        summary: String
    ) {
        val title = if (isEmployer) "Weekly Hiring Summary" else "Weekly Job Search Summary"
        val type = if (isEmployer) NotificationType.SYSTEM_UPDATE else NotificationType.SYSTEM_UPDATE
        
        coroutineScope.launch {
            notificationService.createNotification(
                userId = userId,
                title = title,
                message = summary,
                type = type,
                priority = NotificationPriority.LOW,
                actionData = mapOf(
                    "action" to "view_summary"
                )
            )
        }
    }
    
    /**
     * Trigger notification for interview reminder
     */
    fun onInterviewReminder(
        workerId: String,
        jobTitle: String,
        companyName: String,
        interviewTime: String,
        jobId: String
    ) {
        coroutineScope.launch {
            notificationService.createNotification(
                userId = workerId,
                title = "Interview Reminder",
                message = "Your interview for $jobTitle at $companyName is scheduled for $interviewTime",
                type = NotificationType.INTERVIEW_SCHEDULED,
                priority = NotificationPriority.HIGH,
                relatedJobId = jobId,
                actionData = mapOf(
                    "action" to "view_interview",
                    "jobId" to jobId
                )
            )
        }
    }
    
    /**
     * Trigger notification for profile completion reminder
     */
    fun onProfileIncomplete(
        userId: String,
        isEmployer: Boolean,
        missingFields: List<String>
    ) {
        val title = if (isEmployer) "Complete Your Company Profile" else "Complete Your Profile"
        val message = if (isEmployer) {
            "Complete your company profile to attract better candidates. Missing: ${missingFields.joinToString(", ")}"
        } else {
            "Complete your profile to apply for jobs. Missing: ${missingFields.joinToString(", ")}"
        }
        
        coroutineScope.launch {
            notificationService.createNotification(
                userId = userId,
                title = title,
                message = message,
                type = NotificationType.SYSTEM_UPDATE,
                priority = NotificationPriority.NORMAL,
                actionData = mapOf(
                    "action" to "complete_profile"
                )
            )
        }
    }
    
    /**
     * Trigger notification for system updates
     */
    fun onSystemUpdate(
        userId: String,
        updateTitle: String,
        updateMessage: String
    ) {
        coroutineScope.launch {
            notificationService.createNotification(
                userId = userId,
                title = updateTitle,
                message = updateMessage,
                type = NotificationType.SYSTEM_UPDATE,
                priority = NotificationPriority.LOW,
                actionData = mapOf(
                    "action" to "view_update"
                )
            )
        }
    }
}
