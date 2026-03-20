package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.Notification
import com.example.dutype.models.NotificationFilter
import com.example.dutype.models.NotificationStats
import com.example.dutype.models.NotificationType
import com.example.dutype.models.NotificationData
import com.example.dutype.services.NotificationService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Shared UI state for notification screens.
 */
data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val filteredNotifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val unreadCount: Int = 0,
    val selectedFilter: NotificationFilter = NotificationFilter.ALL,
    val stats: NotificationStats = NotificationStats()
)

/**
 * Base class for Worker/Employer notification ViewModels.
 * Encapsulates all shared logic; subclasses only provide role-specific filtering.
 */
abstract class BaseNotificationViewModel(
    private val notificationService: NotificationService,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    /** Role label used in log messages (e.g. "WORKER", "EMPLOYER"). */
    protected abstract val roleLabel: String

    /** Filter raw notifications to only those relevant to this role. */
    protected abstract fun filterRoleNotifications(notifications: List<NotificationData>): List<NotificationData>

    /** Notification type used when creating a test notification. */
    protected abstract val testNotificationType: NotificationType

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val userId = auth.currentUser?.uid

                if (userId.isNullOrBlank()) {
                    Timber.d("$roleLabel NotificationVM - User not authenticated, skipping")
                    _uiState.value = _uiState.value.copy(
                        notifications = emptyList(),
                        filteredNotifications = emptyList(),
                        isLoading = false,
                        unreadCount = 0,
                        stats = NotificationStats()
                    )
                    return@launch
                }

                Timber.d("$roleLabel NotificationVM - Loading notifications for user: $userId")

                notificationService.getUserNotifications(userId, activeRole = roleLabel).collect { result ->
                    result.fold(
                        onSuccess = { notifications ->
                            val roleNotifications = filterRoleNotifications(notifications)
                            Timber.d("$roleLabel NotificationVM - ${roleNotifications.size}/${notifications.size} after role filter")

                            val unreadCount = roleNotifications.count { !it.isRead }

                            val converted = roleNotifications.map { data ->
                                Notification(
                                    id = data.id,
                                    userId = data.recipientId,
                                    title = data.title,
                                    message = data.message,
                                    type = convertNotificationType(data.type),
                                    isRead = data.isRead,
                                    createdAt = data.createdAt,
                                    actionData = data.data
                                )
                            }

                            _uiState.value = _uiState.value.copy(
                                notifications = converted,
                                filteredNotifications = converted,
                                isLoading = false,
                                unreadCount = unreadCount,
                                stats = NotificationStats()
                            )
                        },
                        onFailure = { error ->
                            Timber.e(error, "$roleLabel NotificationVM - Error loading notifications")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Failed to load notifications: ${error.message}"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "$roleLabel NotificationVM - Exception loading notifications")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load notifications: ${e.message}"
                )
            }
        }
    }

    private fun convertNotificationType(type: NotificationType): NotificationType {
        return when (type) {
            NotificationType.APPLICATION_STATUS -> NotificationType.APPLICATION_STATUS_UPDATE
            else -> type
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationService.markNotificationAsRead(notificationId)
                .onSuccess {
                    loadNotifications()
                }
                .onFailure { error ->
                    Timber.e(error, "$roleLabel NotificationVM - Failed to mark notification as read")
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to update notification state"
                    )
                }
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
    }

    fun createTestNotification() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: ""
                if (userId.isNotEmpty()) {
                    val testNotification = NotificationData(
                        id = java.util.UUID.randomUUID().toString(),
                        recipientId = userId,
                        title = "Test Notification",
                        message = "This is a test notification for ${roleLabel.lowercase()} - ${System.currentTimeMillis()}",
                        type = testNotificationType,
                        data = mapOf(
                            "test" to "true",
                            "timestamp" to System.currentTimeMillis().toString()
                        ),
                        createdAt = System.currentTimeMillis(),
                        isRead = false
                    )
                    notificationService.sendNotification(testNotification, userId)
                    Timber.i("$roleLabel NotificationVM - Test notification created")
                    loadNotifications()
                }
            } catch (e: Exception) {
                Timber.e(e, "$roleLabel NotificationVM - Error creating test notification")
            }
        }
    }
}
