package com.example.dutype.utils

import android.net.Uri
import com.example.dutype.models.NotificationType
import timber.log.Timber

/**
 * Notification Deep Link Builder
 * 
 * Builds deep links for notifications following industry best practices.
 * Each notification type maps to a specific deep link that navigates to the exact screen.
 * 
 * Based on patterns from:
 * - Swiggy: Order notifications → Order detail screen
 * - PhonePe: Transaction notifications → Transaction detail screen
 * - LinkedIn: Job notifications → Job detail screen
 */
object NotificationDeepLinkBuilder {
    
    /**
     * Build deep link for notification based on type and data
     * 
     * @param type Notification type
     * @param data Notification data containing IDs and other info
     * @return Deep link URI string
     */
    fun buildDeepLink(type: NotificationType, data: Map<String, String>): String {
        val deepLink = when (type) {
            // Job Notifications
            NotificationType.NEW_JOB_ALERT,
            NotificationType.JOB_RECOMMENDATION -> {
                val jobId = data["jobId"] ?: data["job_id"]
                if (jobId != null) {
                    "dutype://job/$jobId"
                } else {
                    "dutype://worker/jobs" // Fallback to jobs list
                }
            }
            
            NotificationType.JOB_POSTED,
            NotificationType.JOB_UPDATE,
            NotificationType.JOB_EXPIRY_REMINDER -> {
                val jobId = data["jobId"] ?: data["job_id"]
                if (jobId != null) {
                    "dutype://employer/jobs/$jobId"
                } else {
                    "dutype://employer/jobs" // Fallback to employer jobs
                }
            }
            
            // Application Notifications - Worker Side
            NotificationType.APPLICATION_STATUS,
            NotificationType.APPLICATION_STATUS_UPDATE,
            NotificationType.SHORTLISTED,
            NotificationType.REJECTED -> {
                val applicationId = data["applicationId"] ?: data["application_id"]
                val jobId = data["jobId"] ?: data["job_id"]
                when {
                    applicationId != null -> "dutype://worker/applications/$applicationId"
                    jobId != null -> "dutype://job/$jobId"
                    else -> "dutype://worker/applications" // Fallback to applications list
                }
            }
            
            // Application Notifications - Employer Side
            NotificationType.NEW_APPLICATION,
            NotificationType.APPLICATION_REMINDER -> {
                val applicationId = data["applicationId"] ?: data["application_id"]
                val jobId = data["jobId"] ?: data["job_id"]
                when {
                    applicationId != null -> "dutype://employer/applications/$applicationId"
                    jobId != null -> "dutype://employer/jobs/$jobId"
                    else -> "dutype://employer/applications" // Fallback to applications list
                }
            }
            
            // Worker Hired
            NotificationType.WORKER_HIRED -> {
                val jobId = data["jobId"] ?: data["job_id"]
                val applicationId = data["applicationId"] ?: data["application_id"]
                when {
                    jobId != null -> "dutype://job/$jobId"
                    applicationId != null -> "dutype://worker/applications/$applicationId"
                    else -> "dutype://worker/applications"
                }
            }
            
            // Interview
            NotificationType.INTERVIEW_SCHEDULED -> {
                val jobId = data["jobId"] ?: data["job_id"]
                val applicationId = data["applicationId"] ?: data["application_id"]
                when {
                    applicationId != null -> "dutype://worker/applications/$applicationId"
                    jobId != null -> "dutype://job/$jobId"
                    else -> "dutype://worker/applications"
                }
            }
            
            // Profile Notifications
            NotificationType.PROFILE_COMPLETE,
            NotificationType.PROFILE_REMINDER,
            NotificationType.PROFILE_MILESTONE -> {
                val userRole = data["userRole"] ?: data["role"]
                when {
                    userRole?.equals("employer", ignoreCase = true) == true -> "dutype://employer/profile"
                    userRole?.equals("worker", ignoreCase = true) == true -> "dutype://worker/profile"
                    else -> "dutype://profile"
                }
            }
            
            // Welcome & Re-engagement
            NotificationType.WELCOME,
            NotificationType.RE_ENGAGEMENT -> {
                val userRole = data["userRole"] ?: data["role"]
                val action = data["action"]
                when {
                    action == "view_jobs" -> "dutype://worker/jobs"
                    action == "post_job" -> "dutype://employer/post-job"
                    userRole?.equals("employer", ignoreCase = true) == true -> "dutype://employer/home"
                    userRole?.equals("worker", ignoreCase = true) == true -> "dutype://worker/home"
                    else -> "dutype://home"
                }
            }
            
            // Messages (chat feature removed - navigate to home)
            NotificationType.EMPLOYER_MESSAGE -> {
                "dutype://home"
            }
            
            // Birthday
            NotificationType.BIRTHDAY -> {
                "dutype://profile"
            }
            
            // Referrals
            NotificationType.REFERRAL_MILESTONE -> {
                "dutype://refer"
            }
            
            // System & General
            NotificationType.SYSTEM_UPDATE,
            NotificationType.WEEKLY_SUMMARY,
            NotificationType.GENERAL -> {
                val userRole = data["userRole"] ?: data["role"]
                when {
                    userRole?.equals("employer", ignoreCase = true) == true -> "dutype://employer/notifications"
                    userRole?.equals("worker", ignoreCase = true) == true -> "dutype://worker/notifications"
                    else -> "dutype://notifications"
                }
            }
        }
        
        Timber.d("📱 NotificationDeepLink: Built deep link for $type → $deepLink")
        return deepLink
    }
    
    /**
     * Build deep link URI for notification
     */
    fun buildDeepLinkUri(type: NotificationType, data: Map<String, String>): Uri {
        val deepLinkString = buildDeepLink(type, data)
        return Uri.parse(deepLinkString)
    }
    
    /**
     * Build web link for notification (for sharing/external use)
     */
    fun buildWebLink(type: NotificationType, data: Map<String, String>): String {
        return when (type) {
            NotificationType.NEW_JOB_ALERT,
            NotificationType.JOB_RECOMMENDATION -> {
                val jobId = data["jobId"] ?: data["job_id"]
                if (jobId != null) {
                    "https://dutype.in/jobs/$jobId"
                } else {
                    "https://dutype.in/jobs"
                }
            }
            
            NotificationType.NEW_APPLICATION -> {
                val applicationId = data["applicationId"] ?: data["application_id"]
                if (applicationId != null) {
                    "https://dutype.in/application/$applicationId"
                } else {
                    "https://dutype.in/applications"
                }
            }
            
            else -> "https://dutype.in"
        }
    }
    
    /**
     * Extract notification type from deep link
     * Useful for analytics and tracking
     */
    fun extractNotificationTypeFromDeepLink(deepLink: String): String? {
        return when {
            deepLink.contains("/job/") -> "job"
            deepLink.contains("/applications/") -> "application"
            deepLink.contains("/profile") -> "profile"
            deepLink.contains("/chat/") -> null // chat feature removed
            else -> null
        }
    }
}
