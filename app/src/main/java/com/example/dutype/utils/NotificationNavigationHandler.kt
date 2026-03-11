package com.example.dutype.utils

import androidx.navigation.NavController
import com.example.dutype.models.Notification
import com.example.dutype.models.NotificationType
import com.example.dutype.navigation.Routes
import timber.log.Timber

/**
 * Centralized Notification Navigation Handler
 * 
 * Based on Google Material Design guidelines and industry best practices:
 * - Direct navigation to specific content (not generic screens)
 * - Priority-based handling (actionable vs informational)
 * - Context preservation during navigation
 * 
 * Architecture inspired by: Uber, Swiggy, Zomato, Google apps
 */
object NotificationNavigationHandler {
    
    /**
     * Handle notification click with smart navigation
     * 
     * @param notification The notification that was clicked
     * @param navController Navigation controller for screen transitions
     * @param onMarkAsRead Callback to mark notification as read
     * @param onShowDialog Callback to show informational dialog
     * @param userRole Current user role (WORKER or EMPLOYER)
     */
    fun handleNotificationClick(
        notification: Notification,
        navController: NavController,
        onMarkAsRead: (String) -> Unit,
        onShowDialog: (NotificationDialogData) -> Unit,
        userRole: String
    ) {
        Timber.d("🔔 Handling notification click: ${notification.type} for $userRole")
        
        when (notification.type) {
            // ========================================
            // HIGH PRIORITY - Direct Navigation
            // ========================================
            
            // Worker: Application Status Updates (including pending reminders)
            NotificationType.APPLICATION_STATUS,
            NotificationType.APPLICATION_STATUS_UPDATE,
            NotificationType.SHORTLISTED,
            NotificationType.REJECTED -> {
                val jobId = notification.actionData["jobId"]
                val notificationType = notification.actionData["notificationType"]
                
                // Check if this is a pending application reminder
                if (notificationType == "PENDING_APPLICATION_REMINDER") {
                    Timber.i("🔔 Pending application reminder - Navigating to job detail: $jobId")
                }
                
                if (!jobId.isNullOrEmpty()) {
                    Timber.i("🔔 Navigating to job detail: $jobId")
                    onMarkAsRead(notification.id)
                    navController.navigate(Routes.jobDetailRoute(jobId))
                } else {
                    Timber.w("🔔 No jobId found in notification data")
                    showFallbackDialog(notification, onMarkAsRead, onShowDialog)
                }
            }
            
            // Worker: Job Alerts & Recommendations
            NotificationType.NEW_JOB_ALERT,
            NotificationType.JOB_RECOMMENDATION -> {
                val jobId = notification.actionData["jobId"]
                if (!jobId.isNullOrEmpty()) {
                    Timber.i("🔔 Navigating to recommended job: $jobId")
                    onMarkAsRead(notification.id)
                    navController.navigate(Routes.jobDetailRoute(jobId))
                } else {
                    // Fallback: Navigate to all jobs screen
                    Timber.i("🔔 No specific jobId, navigating to all jobs")
                    onMarkAsRead(notification.id)
                    navController.navigate(Routes.WORKER_ALL_JOBS)
                }
            }
            
            // Worker: Interview Scheduled
            NotificationType.INTERVIEW_SCHEDULED -> {
                val jobId = notification.actionData["jobId"]
                if (!jobId.isNullOrEmpty()) {
                    Timber.i("🔔 Navigating to interview job: $jobId")
                    onMarkAsRead(notification.id)
                    navController.navigate(Routes.jobDetailRoute(jobId))
                } else {
                    showFallbackDialog(notification, onMarkAsRead, onShowDialog)
                }
            }
            
            // Worker: Hired Notification
            NotificationType.WORKER_HIRED -> {
                if (userRole == "WORKER") {
                    val jobId = notification.actionData["jobId"]
                    if (!jobId.isNullOrEmpty()) {
                        Timber.i("🔔 Worker hired! Navigating to job: $jobId")
                        onMarkAsRead(notification.id)
                        navController.navigate(Routes.jobDetailRoute(jobId))
                    } else {
                        showFallbackDialog(notification, onMarkAsRead, onShowDialog)
                    }
                } else {
                    // Employer: Navigate to applications
                    val jobId = notification.actionData["jobId"]
                    if (!jobId.isNullOrEmpty()) {
                        Timber.i("🔔 Worker hired! Navigating to applications: $jobId")
                        onMarkAsRead(notification.id)
                        navController.navigate(
                            Routes.EMPLOYER_APPLICATIONS_JOB.replace("{jobId}", jobId)
                        )
                    } else {
                        onMarkAsRead(notification.id)
                        navController.navigate(Routes.EMPLOYER_APPLICATIONS)
                    }
                }
            }
            
            // Employer: New Application
            NotificationType.NEW_APPLICATION -> {
                val applicationId = notification.actionData["applicationId"]
                if (!applicationId.isNullOrEmpty()) {
                    Timber.i("🔔 Navigating to application detail: $applicationId")
                    onMarkAsRead(notification.id)
                    navController.navigate(
                        Routes.EMPLOYER_APPLICATION_DETAIL.replace("{applicationId}", applicationId)
                    )
                } else {
                    // Fallback: Navigate to all applications
                    val jobId = notification.actionData["jobId"]
                    if (!jobId.isNullOrEmpty()) {
                        Timber.i("🔔 No applicationId, navigating to job applications: $jobId")
                        onMarkAsRead(notification.id)
                        navController.navigate(
                            Routes.EMPLOYER_APPLICATIONS_JOB.replace("{jobId}", jobId)
                        )
                    } else {
                        Timber.i("🔔 No specific IDs, navigating to all applications")
                        onMarkAsRead(notification.id)
                        navController.navigate(Routes.EMPLOYER_APPLICATIONS)
                    }
                }
            }
            
            // Employer: Application Reminder
            NotificationType.APPLICATION_REMINDER -> {
                val jobId = notification.actionData["jobId"]
                if (!jobId.isNullOrEmpty()) {
                    Timber.i("🔔 Navigating to pending applications: $jobId")
                    onMarkAsRead(notification.id)
                    navController.navigate(
                        Routes.EMPLOYER_APPLICATIONS_JOB.replace("{jobId}", jobId)
                    )
                } else {
                    onMarkAsRead(notification.id)
                    navController.navigate(Routes.EMPLOYER_APPLICATIONS)
                }
            }
            
            // Employer: Job Expiry Reminder
            NotificationType.JOB_EXPIRY_REMINDER -> {
                val jobId = notification.actionData["jobId"]
                if (!jobId.isNullOrEmpty()) {
                    Timber.i("🔔 Navigating to edit job (expiry): $jobId")
                    onMarkAsRead(notification.id)
                    navController.navigate(Routes.editJobRoute(jobId))
                } else {
                    onMarkAsRead(notification.id)
                    navController.navigate(Routes.EMPLOYER_MY_JOBS)
                }
            }
            
            // ========================================
            // MEDIUM PRIORITY - Show Dialog
            // ========================================
            
            // Birthday Wishes
            NotificationType.BIRTHDAY -> {
                Timber.i("🔔 Showing birthday dialog")
                onMarkAsRead(notification.id)
                onShowDialog(
                    NotificationDialogData(
                        title = notification.title,
                        message = notification.message,
                        type = notification.type,
                        icon = "🎂",
                        primaryAction = NotificationDialogAction(
                            label = "Thank You! 🎉",
                            action = { /* Just dismiss */ }
                        )
                    )
                )
            }
            
            // Welcome & Profile Complete
            NotificationType.WELCOME,
            NotificationType.PROFILE_COMPLETE -> {
                Timber.i("🔔 Showing welcome dialog")
                onMarkAsRead(notification.id)
                onShowDialog(
                    NotificationDialogData(
                        title = notification.title,
                        message = notification.message,
                        type = notification.type,
                        icon = "👋",
                        primaryAction = NotificationDialogAction(
                            label = "Let's Go!",
                            action = { /* Just dismiss */ }
                        )
                    )
                )
            }
            
            // Job Posted Success
            NotificationType.JOB_POSTED -> {
                Timber.i("🔔 Showing job posted dialog")
                onMarkAsRead(notification.id)
                onShowDialog(
                    NotificationDialogData(
                        title = notification.title,
                        message = notification.message,
                        type = notification.type,
                        icon = "🎉",
                        primaryAction = NotificationDialogAction(
                            label = "View Analytics",
                            action = {
                                navController.navigate(Routes.ANALYTICS)
                            }
                        ),
                        secondaryAction = NotificationDialogAction(
                            label = "Got it",
                            action = { /* Just dismiss */ }
                        )
                    )
                )
            }
            
            // Job Paused/Unpaused
            NotificationType.JOB_PAUSED -> {
                Timber.i("🔔 Showing job status dialog")
                onMarkAsRead(notification.id)
                onShowDialog(
                    NotificationDialogData(
                        title = notification.title,
                        message = notification.message,
                        type = notification.type,
                        icon = if (notification.actionData["isPaused"] == "true") "⏸️" else "▶️",
                        primaryAction = NotificationDialogAction(
                            label = "View My Jobs",
                            action = {
                                navController.navigate(Routes.EMPLOYER_MY_JOBS)
                            }
                        ),
                        secondaryAction = NotificationDialogAction(
                            label = "OK",
                            action = { /* Just dismiss */ }
                        )
                    )
                )
            }
            
            // Referral Milestone
            NotificationType.REFERRAL_MILESTONE -> {
                Timber.i("🔔 Showing referral milestone dialog")
                onMarkAsRead(notification.id)
                onShowDialog(
                    NotificationDialogData(
                        title = notification.title,
                        message = notification.message,
                        type = notification.type,
                        icon = "🎁",
                        primaryAction = NotificationDialogAction(
                            label = "View Rewards",
                            action = {
                                if (userRole == "WORKER") {
                                    navController.navigate(Routes.WORKER_REFER_EARN)
                                } else {
                                    navController.navigate(Routes.EMPLOYER_REFER_EARN)
                                }
                            }
                        ),
                        secondaryAction = NotificationDialogAction(
                            label = "Nice!",
                            action = { /* Just dismiss */ }
                        )
                    )
                )
            }
            
            // Re-engagement & Weekly Summary
            NotificationType.RE_ENGAGEMENT,
            NotificationType.WEEKLY_SUMMARY -> {
                Timber.i("🔔 Showing engagement dialog")
                onMarkAsRead(notification.id)
                onShowDialog(
                    NotificationDialogData(
                        title = notification.title,
                        message = notification.message,
                        type = notification.type,
                        icon = "📊",
                        primaryAction = NotificationDialogAction(
                            label = "Explore",
                            action = {
                                if (userRole == "WORKER") {
                                    navController.navigate(Routes.WORKER_ALL_JOBS)
                                } else {
                                    navController.navigate(Routes.EMPLOYER_HOME)
                                }
                            }
                        ),
                        secondaryAction = NotificationDialogAction(
                            label = "Later",
                            action = { /* Just dismiss */ }
                        )
                    )
                )
            }
            
            // System Updates & General
            NotificationType.SYSTEM_UPDATE,
            NotificationType.GENERAL -> {
                Timber.i("🔔 Showing system update dialog")
                onMarkAsRead(notification.id)
                onShowDialog(
                    NotificationDialogData(
                        title = notification.title,
                        message = notification.message,
                        type = notification.type,
                        icon = "ℹ️",
                        primaryAction = NotificationDialogAction(
                            label = "Got it",
                            action = { /* Just dismiss */ }
                        )
                    )
                )
            }
            
            // ========================================
            // LOW PRIORITY - Just Mark as Read
            // ========================================
            else -> {
                Timber.i("🔔 Unknown notification type, showing fallback dialog")
                showFallbackDialog(notification, onMarkAsRead, onShowDialog)
            }
        }
    }
    
    /**
     * Show fallback dialog when navigation data is missing
     */
    private fun showFallbackDialog(
        notification: Notification,
        onMarkAsRead: (String) -> Unit,
        onShowDialog: (NotificationDialogData) -> Unit
    ) {
        onMarkAsRead(notification.id)
        onShowDialog(
            NotificationDialogData(
                title = notification.title,
                message = notification.message,
                type = notification.type,
                icon = "📢",
                primaryAction = NotificationDialogAction(
                    label = "OK",
                    action = { /* Just dismiss */ }
                )
            )
        )
    }
}

/**
 * Data class for notification dialog
 */
data class NotificationDialogData(
    val title: String,
    val message: String,
    val type: NotificationType,
    val icon: String,
    val primaryAction: NotificationDialogAction,
    val secondaryAction: NotificationDialogAction? = null
)

/**
 * Data class for dialog actions
 */
data class NotificationDialogAction(
    val label: String,
    val action: () -> Unit
)
