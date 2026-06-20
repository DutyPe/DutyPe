package com.example.dutype.worker.viewmodels

import com.example.dutype.auth.AuthManager
import com.example.dutype.models.NotificationType
import com.example.dutype.models.NotificationData
import com.example.dutype.services.NotificationService
import com.example.dutype.viewmodels.BaseNotificationViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WorkerNotificationViewModel @Inject constructor(
    notificationService: NotificationService,
    private val authManager: AuthManager,
    auth: FirebaseAuth
) : BaseNotificationViewModel(notificationService, auth) {

    override val roleLabel = "WORKER"
    override val testNotificationType = NotificationType.APPLICATION_STATUS

    // NotificationService already filters every notification by role (see
    // parseNotificationDocument), and that is the exact same filter the unread
    // badge count uses. Applying a second, narrower type whitelist here hid
    // legitimate worker notifications (referral rewards, profile reminders,
    // shortlisted/rejected, birthday, weekly summary, etc.), which made the
    // home-screen badge show a count while this list appeared empty. Pass the
    // already role-filtered list through unchanged so the badge and the list
    // always match.
    override fun filterRoleNotifications(notifications: List<NotificationData>): List<NotificationData> {
        return notifications
    }
}
