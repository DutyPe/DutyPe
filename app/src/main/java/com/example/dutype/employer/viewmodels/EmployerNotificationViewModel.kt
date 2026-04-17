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

    override fun filterRoleNotifications(notifications: List<NotificationData>): List<NotificationData> {
        return notifications.filter { notification ->
            when (notification.type) {
                NotificationType.JOB_POSTED,
                NotificationType.JOB_PAUSED,
                NotificationType.NEW_APPLICATION,
                NotificationType.PROFILE_COMPLETE,
                NotificationType.WORKER_HIRED,
                NotificationType.WELCOME,
                NotificationType.SYSTEM_UPDATE,
                NotificationType.GENERAL -> true
                else -> false
            }
        }
    }
}
