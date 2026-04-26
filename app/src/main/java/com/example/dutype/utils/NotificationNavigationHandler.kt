package com.example.dutype.utils

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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

    private const val PLAY_STORE_PACKAGE_ID = "com.dutype.app"
    private const val PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id=$PLAY_STORE_PACKAGE_ID"

    private fun isAppUpdateNotification(notification: Notification): Boolean {
        return notification.type == NotificationType.SYSTEM_UPDATE &&
            notification.actionData["preset"].equals("APP_UPDATE", ignoreCase = true)
    }

    private fun openPlayStore(navController: NavController): Boolean {
        val context = navController.context

        return try {
            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$PLAY_STORE_PACKAGE_ID")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
            true
        } catch (_: ActivityNotFoundException) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_WEB_URL)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                true
            } catch (e: Exception) {
                Timber.e(e, "🔔 Failed to open Play Store for app update notification")
                false
            }
        }
    }
    
    /** Navigate to a job detail by extracting jobId from notification data, with fallback dialog. */
    private fun navigateToJobOrFallback(
        notification: Notification,
        navController: NavController,
        onMarkAsRead: (String) -> Unit,
        onShowDialog: (NotificationDialogData) -> Unit
    ): Boolean {
        val jobId = notification.actionData["jobId"]
        return if (!jobId.isNullOrEmpty()) {
            onMarkAsRead(notification.id)
            navController.navigate(Routes.jobDetailRoute(jobId))
            true
        } else {
            showFallbackDialog(notification, onMarkAsRead, onShowDialog)
            false
        }
    }
    
    /** Show a simple informational dialog, marking the notification as read. */
    private fun showInfoDialog(
        notification: Notification,
        onMarkAsRead: (String) -> Unit,
        onShowDialog: (NotificationDialogData) -> Unit,
        icon: String,
        buttonLabel: String = "Got it",
        secondaryAction: NotificationDialogAction? = null
    ) {
        onMarkAsRead(notification.id)
        onShowDialog(
            NotificationDialogData(
                title = notification.title,
                message = notification.message,
                type = notification.type,
                icon = icon,
                primaryAction = NotificationDialogAction(
                    label = buttonLabel,
                    action = { /* dismiss */ }
                ),
                secondaryAction = secondaryAction
            )
        )
    }
    
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
                navigateToJobOrFallback(notification, navController, onMarkAsRead, onShowDialog)
            }
            
            // Worker: Job Alerts & Recommendations
            NotificationType.NEW_JOB_ALERT,
            NotificationType.JOB_RECOMMENDATION -> {
                val jobId = notification.actionData["jobId"]
                onMarkAsRead(notification.id)
                if (!jobId.isNullOrEmpty()) {
                    navController.navigate(Routes.jobDetailRoute(jobId))
                } else {
                    navController.navigate(Routes.WORKER_ALL_JOBS)
                }
            }
            
            // Worker: Interview Scheduled
            NotificationType.INTERVIEW_SCHEDULED -> {
                navigateToJobOrFallback(notification, navController, onMarkAsRead, onShowDialog)
            }
            
            // Worker: Hired Notification
            NotificationType.WORKER_HIRED -> {
                if (userRole == "WORKER") {
                    navigateToJobOrFallback(notification, navController, onMarkAsRead, onShowDialog)
                } else {
                    val jobId = notification.actionData["jobId"]
                    onMarkAsRead(notification.id)
                    navController.navigate(
                        if (!jobId.isNullOrEmpty()) Routes.EMPLOYER_APPLICATIONS_JOB.replace("{jobId}", jobId)
                        else Routes.EMPLOYER_APPLICATIONS
                    )
                }
            }
            
            // Employer: New Application
            NotificationType.NEW_APPLICATION -> {
                val applicationId = notification.actionData["applicationId"]
                run {
                    // Route directly to the applications list scoped to the job if we know it
                    val jobId = notification.actionData["jobId"]
                    if (!jobId.isNullOrEmpty()) {
                        Timber.i("🔔 Navigating to job applications: $jobId (appId=$applicationId)")
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
                onMarkAsRead(notification.id)
                navController.navigate(
                    if (!jobId.isNullOrEmpty()) Routes.EMPLOYER_APPLICATIONS_JOB.replace("{jobId}", jobId)
                    else Routes.EMPLOYER_APPLICATIONS
                )
            }
            
            // Employer: Job Expiry Reminder
            NotificationType.JOB_EXPIRY_REMINDER -> {
                val jobId = notification.actionData["jobId"]
                onMarkAsRead(notification.id)
                navController.navigate(
                    if (!jobId.isNullOrEmpty()) Routes.editJobRoute(jobId)
                    else Routes.EMPLOYER_MY_JOBS
                )
            }
            
            // ========================================
            // MEDIUM PRIORITY - Show Dialog
            // ========================================
            
            // Birthday Wishes
            NotificationType.BIRTHDAY -> {
                showInfoDialog(notification, onMarkAsRead, onShowDialog, icon = "🎂", buttonLabel = "Thank You! 🎉")
            }
            
            // Welcome & Profile Complete
            NotificationType.WELCOME,
            NotificationType.PROFILE_COMPLETE -> {
                showInfoDialog(notification, onMarkAsRead, onShowDialog, icon = "👋", buttonLabel = "Let's Go!")
            }
            
            // Job Posted Success
            NotificationType.JOB_POSTED -> {
                onMarkAsRead(notification.id)
                onShowDialog(
                    NotificationDialogData(
                        title = notification.title,
                        message = notification.message,
                        type = notification.type,
                        icon = "🎉",
                        primaryAction = NotificationDialogAction("View Analytics") {
                            navController.navigate(Routes.ANALYTICS)
                        },
                        secondaryAction = NotificationDialogAction("Got it") { /* dismiss */ }
                    )
                )
            }
            
            // Referral Milestone
            NotificationType.REFERRAL_MILESTONE -> {
                onMarkAsRead(notification.id)
                onShowDialog(
                    NotificationDialogData(
                        title = notification.title,
                        message = notification.message,
                        type = notification.type,
                        icon = "🎁",
                        primaryAction = NotificationDialogAction("View Rewards") {
                            if (userRole == "WORKER") navController.navigate(Routes.WORKER_REFER_EARN)
                        },
                        secondaryAction = NotificationDialogAction("Nice!") { /* dismiss */ }
                    )
                )
            }
            
            // Re-engagement & Weekly Summary
            NotificationType.RE_ENGAGEMENT,
            NotificationType.WEEKLY_SUMMARY -> {
                onMarkAsRead(notification.id)
                onShowDialog(
                    NotificationDialogData(
                        title = notification.title,
                        message = notification.message,
                        type = notification.type,
                        icon = "📊",
                        primaryAction = NotificationDialogAction("Explore") {
                            val route = if (userRole == "WORKER") Routes.WORKER_ALL_JOBS else Routes.EMPLOYER_HOME
                            navController.navigate(route)
                        },
                        secondaryAction = NotificationDialogAction("Later") { /* dismiss */ }
                    )
                )
            }
            
            // System Updates & General
            NotificationType.SYSTEM_UPDATE,
            NotificationType.GENERAL -> {
                if (isAppUpdateNotification(notification)) {
                    onMarkAsRead(notification.id)
                    if (!openPlayStore(navController)) {
                        showInfoDialog(notification, onMarkAsRead, onShowDialog, icon = "ℹ️")
                    }
                } else {
                    showInfoDialog(notification, onMarkAsRead, onShowDialog, icon = "ℹ️")
                }
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
