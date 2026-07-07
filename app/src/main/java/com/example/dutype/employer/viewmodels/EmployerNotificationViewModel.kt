package com.example.dutype.employer.viewmodels

import com.example.dutype.auth.AuthManager
import com.example.dutype.models.NotificationType
import com.example.dutype.models.NotificationData
import com.example.dutype.services.NotificationService
import com.example.dutype.viewmodels.BaseNotificationViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EmployerNotificationViewModel @Inject constructor(
    notificationService: NotificationService,
    private val authManager: AuthManager,
    auth: FirebaseAuth
) : BaseNotificationViewModel(notificationService, auth) {

    override val roleLabel = "EMPLOYER"
    override val testNotificationType = NotificationType.JOB_POSTED

    // NotificationService already filters every notification by role (see
    // parseNotificationDocument), which is the same filter the unread badge
    // count uses. A second narrower type whitelist here hid valid employer
    // notifications and made the badge count disagree with this list. Pass the
    // already role-filtered list through unchanged so badge and list match.
    override fun filterRoleNotifications(notifications: List<NotificationData>): List<NotificationData> {
        return notifications.filter { notif ->
            val typeStr = notif.type.name.uppercase()
            val titleLower = notif.title.lowercase()
            val msgLower = notif.message.lowercase()
            
            // Filter out refer/referral/milestone/signup bonus notifications
            !typeStr.contains("REFERRAL") &&
            !titleLower.contains("refer") &&
            !msgLower.contains("refer") &&
            !titleLower.contains("signup bonus") &&
            !msgLower.contains("signup bonus") &&
            !titleLower.contains("welcome gift") &&
            !msgLower.contains("welcome gift")
        }
    }
}
