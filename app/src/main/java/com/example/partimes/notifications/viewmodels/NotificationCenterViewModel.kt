package com.example.partimes.notifications.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.notifications.models.*
import com.example.partimes.notifications.services.NotificationService
import com.example.partimes.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Notification Center
 */
data class NotificationCenterUiState(
    val notifications: List<Notification> = emptyList(),
    val filteredNotifications: List<Notification> = emptyList(),
    val selectedFilter: NotificationFilter = NotificationFilter.ALL,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val unreadCount: Int = 0,
    val stats: NotificationStats = NotificationStats()
)

/**
 * ViewModel for Notification Center
 */
@HiltViewModel
class NotificationCenterViewModel @Inject constructor(
    private val notificationService: NotificationService,
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NotificationCenterUiState())
    val uiState: StateFlow<NotificationCenterUiState> = _uiState.asStateFlow()
    
    init {
        loadNotifications()
    }
    
    /**
     * Load notifications
     */
    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val userId = userRepository.currentUser.value.id
                
                // Load notifications
                notificationService.getNotifications(userId).collect { notifications ->
                    val stats = notificationService.getNotificationStats(userId)
                    val unreadCount = notifications.count { !it.isRead }
                    
                    _uiState.value = _uiState.value.copy(
                        notifications = notifications,
                        isLoading = false,
                        unreadCount = unreadCount,
                        stats = stats
                    )
                    
                    // Apply current filter
                    applyFilter(_uiState.value.selectedFilter)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load notifications: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Refresh notifications
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            
            try {
                val userId = userRepository.currentUser.value.id
                notificationService.getNotifications(userId).collect { notifications ->
                    val stats = notificationService.getNotificationStats(userId)
                    val unreadCount = notifications.count { !it.isRead }
                    
                    _uiState.value = _uiState.value.copy(
                        notifications = notifications,
                        isRefreshing = false,
                        unreadCount = unreadCount,
                        stats = stats
                    )
                    
                    // Apply current filter
                    applyFilter(_uiState.value.selectedFilter)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = "Failed to refresh notifications: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update filter
     */
    fun updateFilter(filter: NotificationFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        applyFilter(filter)
    }
    
    /**
     * Apply filter to notifications
     */
    private fun applyFilter(filter: NotificationFilter) {
        val filtered = when (filter) {
            NotificationFilter.ALL -> _uiState.value.notifications
            NotificationFilter.UNREAD -> _uiState.value.notifications.filter { !it.isRead }
            NotificationFilter.READ -> _uiState.value.notifications.filter { it.isRead }
            NotificationFilter.ARCHIVED -> _uiState.value.notifications.filter { it.isArchived }
            NotificationFilter.APPLICATIONS -> _uiState.value.notifications.filter { 
                it.type in listOf(
                    NotificationType.APPLICATION_STATUS_UPDATE,
                    NotificationType.SHORTLISTED,
                    NotificationType.REJECTED,
                    NotificationType.INTERVIEW_SCHEDULED
                )
            }
            NotificationFilter.JOBS -> _uiState.value.notifications.filter { 
                it.type in listOf(
                    NotificationType.NEW_JOB_ALERT,
                    NotificationType.JOB_RECOMMENDATION
                )
            }
        }
        
        _uiState.value = _uiState.value.copy(filteredNotifications = filtered)
    }
    
    /**
     * Mark notification as read
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationService.markAsRead(notificationId)
                // Refresh to update UI
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to mark as read: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Mark all notifications as read
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val userId = userRepository.currentUser.value.id
                notificationService.markAllAsRead(userId)
                // Refresh to update UI
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to mark all as read: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Archive notification
     */
    fun archiveNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationService.archiveNotification(notificationId)
                // Refresh to update UI
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to archive notification: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Delete notification
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationService.deleteNotification(notificationId)
                // Refresh to update UI
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete notification: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear all notifications
     */
    fun clearAllNotifications() {
        viewModelScope.launch {
            try {
                val userId = userRepository.currentUser.value.id
                notificationService.clearAllNotifications(userId)
                // Refresh to update UI
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to clear all notifications: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
