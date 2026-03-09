package com.example.dutype.employer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.auth.AuthManager
import com.example.dutype.models.UserRole
import com.example.dutype.models.Notification
import com.example.dutype.models.NotificationFilter
import com.example.dutype.models.NotificationStats
import com.example.dutype.models.NotificationType
import com.example.dutype.models.NotificationData
import com.example.dutype.services.NotificationService
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class EmployerNotificationViewModel @Inject constructor(
    private val notificationService: NotificationService,
    private val authManager: AuthManager,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // CRITICAL FIX: Get userId directly from Firebase Auth instead of cached AuthManager
                // This ensures we always have the latest user ID even during role switches
                val userId = auth.currentUser?.uid
                
                Timber.d("🔔 EmployerNotificationViewModel - User ID from Firebase: $userId")

                // Check if user is logged in
                if (userId.isNullOrBlank()) {
                    Timber.w("🔔 EmployerNotificationViewModel - No user ID, user not logged in")
                    _uiState.value = _uiState.value.copy(
                        notifications = emptyList(),
                        filteredNotifications = emptyList(),
                        isLoading = false,
                        error = "Please log in to view notifications",
                        unreadCount = 0,
                        stats = NotificationStats()
                    )
                    return@launch
                }

                Timber.d("🔔 EmployerNotificationViewModel - Loading EMPLOYER notifications for user: $userId")

                // Load notifications from Firestore
                notificationService.getUserNotifications(userId).collect { result ->
                    result.fold(
                        onSuccess = { notifications ->
                            Timber.d("🔔 EmployerNotificationViewModel - Loaded ${notifications.size} notifications")
                            Timber.d("🔔 EmployerNotificationViewModel - Raw notifications: $notifications")

                            // Filter for employer-specific notifications only
                            val employerNotifications = filterEmployerNotifications(notifications)
                            Timber.d("🔔 EmployerNotificationViewModel - Filtered to ${employerNotifications.size} employer notifications")

                            val unreadCount = employerNotifications.count { !it.isRead }

                            // Convert NotificationData to Notification for UI
                            val convertedNotifications = employerNotifications.map { notificationData ->
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
        notifications: List<NotificationData>
    ): List<NotificationData> {
        Timber.d("🔔 Filtering ${notifications.size} notifications for EMPLOYER role")

        val filteredNotifications = notifications.filter { notification ->
            val isEmployerNotification = when (notification.type) {
                NotificationType.JOB_POSTED,
                NotificationType.JOB_PAUSED,
                NotificationType.NEW_APPLICATION,
                NotificationType.PROFILE_COMPLETE,
                NotificationType.WORKER_HIRED,
                NotificationType.WELCOME,
                NotificationType.GENERAL -> true
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
                // CRITICAL FIX: Get userId directly from Firebase Auth
                val userId = auth.currentUser?.uid ?: ""
                
                if (userId.isNotEmpty()) {
                    val testNotification = NotificationData(
                        id = java.util.UUID.randomUUID().toString(),
                        recipientId = userId,
                        title = "Test Notification",
                        message = "This is a test notification for employer - ${System.currentTimeMillis()}",
                        type = NotificationType.JOB_POSTED,
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
