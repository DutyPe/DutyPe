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

    override fun filterRoleNotifications(notifications: List<NotificationData>): List<NotificationData> {
        return notifications.filter { notification ->
            when (notification.type) {
                NotificationType.APPLICATION_STATUS,
                NotificationType.APPLICATION_STATUS_UPDATE,
                NotificationType.INTERVIEW_SCHEDULED,
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
