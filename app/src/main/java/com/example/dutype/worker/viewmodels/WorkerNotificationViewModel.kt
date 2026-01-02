package com.example.dutype.worker.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.repositories.AuthRepository
import com.example.dutype.models.UserRole
import com.example.dutype.models.Notification
import com.example.dutype.models.NotificationFilter
import com.example.dutype.models.NotificationStats
import com.example.dutype.models.NotificationType
import com.example.dutype.models.NotificationData
import com.example.dutype.services.NotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WorkerNotificationViewModel @Inject constructor(
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
                Timber.d("WorkerNotificationViewModel - Current user: $currentUser")
                Timber.d("WorkerNotificationViewModel - User ID: $userId")
                Timber.d("WorkerNotificationViewModel - User role: $userRole")

                if (userRole != UserRole.WORKER) {
                    Timber.i("WorkerNotificationViewModel - User is not a worker, showing no notifications")
                    _uiState.value = _uiState.value.copy(
                        notifications = emptyList(),
                        filteredNotifications = emptyList(),
                        isLoading = false,
                        unreadCount = 0,
                        stats = NotificationStats()
                    )
                    return@launch
                }

                // Load notifications from Firestore
                notificationService.getUserNotifications(userId).collect { result ->
                    result.fold(
                        onSuccess = { notifications ->
                            Timber.d("WorkerNotificationViewModel - Loaded ${notifications.size} notifications")

                            // Filter for worker-specific notifications only
                            val workerNotifications = filterWorkerNotifications(notifications)
                            Timber.d("WorkerNotificationViewModel - Filtered to ${workerNotifications.size} worker notifications")

                            val unreadCount = workerNotifications.count { !it.isRead }

                            // Convert NotificationData to Notification for UI
                            val convertedNotifications = workerNotifications.map { notificationData ->
                                Notification(
                                    id = notificationData.id,
                                    userId = notificationData.recipientId,
                                    title = notificationData.title,
                                    message = notificationData.message,
                                    type = convertNotificationType(notificationData.type),
                                    isRead = notificationData.isRead,
                                    createdAt = notificationData.createdAt,
                                    actionData = notificationData.data
                                )
                            }

                            _uiState.value = _uiState.value.copy(
                                notifications = convertedNotifications,
                                filteredNotifications = convertedNotifications,
                                isLoading = false,
                                unreadCount = unreadCount,
                                stats = NotificationStats()
                            )
                        },
                        onFailure = { error ->
                            Timber.e(error, "WorkerNotificationViewModel - Error loading notifications")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Failed to load notifications: ${error.message}"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "WorkerNotificationViewModel - Exception loading notifications")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load notifications: ${e.message}"
                )
            }
        }
    }

    /**
     * Filter notifications specifically for workers
     */
    private fun filterWorkerNotifications(
        notifications: List<NotificationData>
    ): List<NotificationData> {
        Timber.d("Filtering ${notifications.size} notifications for WORKER role")

        val filteredNotifications = notifications.filter { notification ->
            val isWorkerNotification = when (notification.type) {
                NotificationType.APPLICATION_STATUS,
                NotificationType.APPLICATION_STATUS_UPDATE,
                NotificationType.INTERVIEW_SCHEDULED,
                NotificationType.PROFILE_COMPLETE,
                NotificationType.WORKER_HIRED,
                NotificationType.WELCOME,
                NotificationType.GENERAL -> true
                else -> false
            }
            if (isWorkerNotification) {
                Timber.d("WORKER notification: ${notification.title} (${notification.type})")
            }
            isWorkerNotification
        }

        Timber.d("Filtered from ${notifications.size} to ${filteredNotifications.size} worker notifications")
        return filteredNotifications
    }

    /**
     * Convert NotificationData type to UI-compatible type
     * Now uses unified NotificationType enum
     */
    private fun convertNotificationType(type: NotificationType): NotificationType {
        return when (type) {
            NotificationType.APPLICATION_STATUS -> NotificationType.APPLICATION_STATUS_UPDATE
            else -> type
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
                    val testNotification = NotificationData(
                        id = java.util.UUID.randomUUID().toString(),
                        recipientId = userId,
                        title = "Test Notification",
                        message = "This is a test notification for worker - ${System.currentTimeMillis()}",
                        type = NotificationType.APPLICATION_STATUS,
                        data = mapOf(
                            "test" to "true",
                            "timestamp" to System.currentTimeMillis().toString()
                        ),
                        createdAt = System.currentTimeMillis(),
                        isRead = false
                    )
                    
                    notificationService.sendNotification(testNotification, userId)
                    Timber.i("WorkerNotificationViewModel - Test notification created and sent")
                    
                    // Refresh notifications to show the new test notification
                    loadNotifications()
                } else {
                    Timber.w("WorkerNotificationViewModel - No user ID available for test notification")
                }
            } catch (e: Exception) {
                Timber.e(e, "WorkerNotificationViewModel - Error creating test notification")
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
