package com.example.dutype.employer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.repositories.AuthRepository
import com.example.dutype.models.UserRole
import com.example.dutype.notifications.models.Notification
import com.example.dutype.notifications.models.NotificationFilter
import com.example.dutype.notifications.models.NotificationStats
import com.example.dutype.services.NotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class EmployerNotificationViewModel @Inject constructor(
    private val notificationService: NotificationService,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val currentUser = authRepository.getCurrentUser()
                val userId = currentUser?.id ?: ""
                val userRole = currentUser?.role
                Timber.d("🔔 EmployerNotificationViewModel - Current user: $currentUser")
                Timber.d("🔔 EmployerNotificationViewModel - User ID: $userId")
                Timber.d("🔔 EmployerNotificationViewModel - User role: $userRole")

                // Allow both EMPLOYER and WORKER roles to see notifications
                // The filtering will be done based on notification type
                Timber.d("🔔 EmployerNotificationViewModel - User role: $userRole, loading notifications")

                // Load notifications from Firestore
                notificationService.getUserNotifications(userId).collect { result ->
                    result.fold(
                        onSuccess = { notifications ->
                            Timber.d("🔔 EmployerNotificationViewModel - Loaded ${notifications.size} notifications")
                            Timber.d("🔔 EmployerNotificationViewModel - Raw notifications: $notifications")

                            // Show all notifications for now (can be filtered later if needed)
                            val allNotifications = notifications
                            Timber.d("🔔 EmployerNotificationViewModel - Showing ${allNotifications.size} notifications")
                            Timber.d("🔔 EmployerNotificationViewModel - All notifications: $allNotifications")

                            val unreadCount = allNotifications.count { !it.isRead }

                            // Convert NotificationData to Notification for UI
                            val convertedNotifications = allNotifications.map { notificationData ->
                                val converted = Notification(
                                    id = notificationData.id,
                                    userId = notificationData.recipientId,
                                    title = notificationData.title,
                                    message = notificationData.message,
                                    type = convertNotificationType(notificationData.type),
                                    isRead = notificationData.isRead,
                                    createdAt = notificationData.createdAt,
                                    actionData = notificationData.data
                                )
                                Timber.d("🔔 EmployerNotificationViewModel - Converted notification: $converted")
                                converted
                            }

                            Timber.d("🔔 EmployerNotificationViewModel - Final converted notifications: $convertedNotifications")

                            _uiState.value = _uiState.value.copy(
                                notifications = convertedNotifications,
                                filteredNotifications = convertedNotifications,
                                isLoading = false,
                                unreadCount = unreadCount,
                                stats = NotificationStats()
                            )
                            
                            Timber.d("🔔 EmployerNotificationViewModel - UI State updated with ${convertedNotifications.size} notifications")
                        },
                        onFailure = { error ->
                            Timber.e("🔔 EmployerNotificationViewModel - Error loading notifications: ${error.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Failed to load notifications: ${error.message}"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "🔔 EmployerNotificationViewModel - Exception loading notifications: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load notifications: ${e.message}"
                )
            }
        }
    }

    /**
     * Filter notifications specifically for employers
     */
    private fun filterEmployerNotifications(
        notifications: List<com.example.dutype.models.NotificationData>
    ): List<com.example.dutype.models.NotificationData> {
        Timber.d("🔔 Filtering ${notifications.size} notifications for EMPLOYER role")

        val filteredNotifications = notifications.filter { notification ->
            val isEmployerNotification = when (notification.type) {
                com.example.dutype.models.NotificationType.JOB_POSTED,
                com.example.dutype.models.NotificationType.JOB_PAUSED,
                com.example.dutype.models.NotificationType.NEW_APPLICATION,
                com.example.dutype.models.NotificationType.PROFILE_COMPLETE,
                com.example.dutype.models.NotificationType.WORKER_HIRED,
                com.example.dutype.models.NotificationType.WELCOME,
                com.example.dutype.models.NotificationType.GENERAL -> true
                else -> false
            }
            if (isEmployerNotification) {
                Timber.d("🔔 EMPLOYER notification: ${notification.title} (${notification.type})")
            }
            isEmployerNotification
        }

        Timber.d("🔔 Filtered from ${notifications.size} to ${filteredNotifications.size} employer notifications")
        return filteredNotifications
    }

    /**
     * Convert NotificationData type to Notification type
     */
    private fun convertNotificationType(type: com.example.dutype.models.NotificationType): com.example.dutype.notifications.models.NotificationType {
        return when (type) {
            com.example.dutype.models.NotificationType.JOB_POSTED -> com.example.dutype.notifications.models.NotificationType.JOB_POSTED
            com.example.dutype.models.NotificationType.JOB_PAUSED -> com.example.dutype.notifications.models.NotificationType.JOB_PAUSED
            com.example.dutype.models.NotificationType.NEW_APPLICATION -> com.example.dutype.notifications.models.NotificationType.NEW_APPLICATION
            com.example.dutype.models.NotificationType.APPLICATION_STATUS -> com.example.dutype.notifications.models.NotificationType.APPLICATION_STATUS_UPDATE
            com.example.dutype.models.NotificationType.PROFILE_COMPLETE -> com.example.dutype.notifications.models.NotificationType.SYSTEM_UPDATE
            com.example.dutype.models.NotificationType.WORKER_HIRED -> com.example.dutype.notifications.models.NotificationType.APPLICATION_STATUS_UPDATE
            com.example.dutype.models.NotificationType.WELCOME -> com.example.dutype.notifications.models.NotificationType.SYSTEM_UPDATE
            com.example.dutype.models.NotificationType.GENERAL -> com.example.dutype.notifications.models.NotificationType.SYSTEM_UPDATE
            else -> com.example.dutype.notifications.models.NotificationType.SYSTEM_UPDATE
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationService.markNotificationAsRead(notificationId)
            // Reload notifications to update UI
            loadNotifications()
        }
    }

    fun archiveNotification(notificationId: String) {
        viewModelScope.launch {
            notificationService.archiveNotification(notificationId)
            loadNotifications()
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationService.deleteNotification(notificationId)
            loadNotifications()
        }
    }

    fun updateFilter(filter: NotificationFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        // No filtering applied - show all notifications
    }
    
    fun createTestNotification() {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                val userId = currentUser?.id ?: ""
                
                if (userId.isNotEmpty()) {
                    val testNotification = com.example.dutype.models.NotificationData(
                        id = java.util.UUID.randomUUID().toString(),
                        recipientId = userId,
                        title = "Test Notification",
                        message = "This is a test notification for employer - ${System.currentTimeMillis()}",
                        type = com.example.dutype.models.NotificationType.JOB_POSTED,
                        data = mapOf(
                            "test" to "true",
                            "timestamp" to System.currentTimeMillis().toString()
                        ),
                        createdAt = System.currentTimeMillis(),
                        isRead = false
                    )
                    
                    notificationService.sendNotification(testNotification, userId)
                    Timber.i("🔔 EmployerNotificationViewModel - Test notification created and sent")
                    
                    // Refresh notifications to show the new test notification
                    loadNotifications()
                } else {
                    Timber.w("🔔 EmployerNotificationViewModel - No user ID available for test notification")
                }
            } catch (e: Exception) {
                Timber.e(e, "🔔 EmployerNotificationViewModel - Error creating test notification: ${e.message}")
            }
        }
    }
}

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val filteredNotifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val unreadCount: Int = 0,
    val selectedFilter: NotificationFilter = NotificationFilter.ALL,
    val stats: NotificationStats = NotificationStats()
)
