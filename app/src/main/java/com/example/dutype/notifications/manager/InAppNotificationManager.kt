package com.example.dutype.notifications.manager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.dutype.notifications.components.InAppNotificationBanner
import com.example.dutype.notifications.components.NotificationToast
import com.example.dutype.notifications.models.*
import com.example.dutype.notifications.services.NotificationService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for in-app notifications
 * Handles displaying notifications in the UI
 */
@Singleton
class InAppNotificationManager @Inject constructor(
    private val notificationService: NotificationService
) {
    
    private val _activeNotifications = MutableStateFlow<List<Notification>>(emptyList())
    val activeNotifications: StateFlow<List<Notification>> = _activeNotifications.asStateFlow()
    
    private val _toastMessages = MutableStateFlow<List<ToastMessage>>(emptyList())
    val toastMessages: StateFlow<List<ToastMessage>> = _toastMessages.asStateFlow()
    
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    
    /**
     * Show a notification banner
     */
    suspend fun showNotification(notification: Notification) {
        val currentNotifications = _activeNotifications.value.toMutableList()
        
        // Remove any existing notification with the same ID
        currentNotifications.removeAll { it.id == notification.id }
        
        // Add the new notification
        currentNotifications.add(notification)
        
        // Keep only the latest 3 notifications
        if (currentNotifications.size > 3) {
            currentNotifications.removeAt(0)
        }
        
        _activeNotifications.value = currentNotifications
        
        // Update unread count
        updateUnreadCount()
    }
    
    /**
     * Show a toast message
     */
    suspend fun showToast(message: String, type: NotificationType = NotificationType.SYSTEM_UPDATE) {
        val toast = ToastMessage(
            id = System.currentTimeMillis().toString(),
            message = message,
            type = type,
            timestamp = System.currentTimeMillis()
        )
        
        val currentToasts = _toastMessages.value.toMutableList()
        currentToasts.add(toast)
        
        // Keep only the latest 2 toast messages
        if (currentToasts.size > 2) {
            currentToasts.removeAt(0)
        }
        
        _toastMessages.value = currentToasts
    }
    
    /**
     * Dismiss a notification banner
     */
    suspend fun dismissNotification(notificationId: String) {
        val currentNotifications = _activeNotifications.value.toMutableList()
        currentNotifications.removeAll { it.id == notificationId }
        _activeNotifications.value = currentNotifications
    }
    
    /**
     * Dismiss a toast message
     */
    suspend fun dismissToast(toastId: String) {
        val currentToasts = _toastMessages.value.toMutableList()
        currentToasts.removeAll { it.id == toastId }
        _toastMessages.value = currentToasts
    }
    
    /**
     * Mark notification as read
     */
    suspend fun markAsRead(notificationId: String) {
        notificationService.markAsRead(notificationId)
        updateUnreadCount()
    }
    
    /**
     * Clear all active notifications
     */
    suspend fun clearAllNotifications() {
        _activeNotifications.value = emptyList()
    }
    
    /**
     * Clear all toast messages
     */
    suspend fun clearAllToasts() {
        _toastMessages.value = emptyList()
    }
    
    /**
     * Update unread count
     */
    private suspend fun updateUnreadCount() {
        // This would typically get the count from the notification service
        // For now, we'll use a simple calculation
        val count = _activeNotifications.value.count { !it.isRead }
        _unreadCount.value = count
    }
    
    /**
     * Initialize with user notifications
     */
    suspend fun initializeForUser(userId: String) {
        notificationService.getNotifications(userId).collect { notifications ->
            // Filter for unread notifications to show as banners
            val unreadNotifications = notifications.filter { !it.isRead }
            _activeNotifications.value = unreadNotifications.take(3)
            updateUnreadCount()
        }
    }
}

/**
 * Data class for toast messages
 */
data class ToastMessage(
    val id: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long
)

/**
 * Composable for managing in-app notifications
 */
@Composable
fun InAppNotificationProvider(
    userId: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val notificationManager = remember { InAppNotificationManager(NotificationService()) }
    val scope = rememberCoroutineScope()
    
    // Initialize notifications for user
    LaunchedEffect(userId) {
        notificationManager.initializeForUser(userId)
    }
    
    // Collect active notifications
    val activeNotifications by notificationManager.activeNotifications.collectAsState()
    val toastMessages by notificationManager.toastMessages.collectAsState()
    
    Box {
        content()
        
        // Show notification banners
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            activeNotifications.forEach { notification ->
                InAppNotificationBanner(
                    notification = notification,
                    onDismiss = {
                        scope.launch {
                            notificationManager.dismissNotification(notification.id)
                        }
                    },
                    onClick = {
                        scope.launch {
                            notificationManager.markAsRead(notification.id)
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Show toast messages at the bottom
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                toastMessages.forEach { toast ->
                    NotificationToast(
                        message = toast.message,
                        type = toast.type,
                        onDismiss = {
                            scope.launch {
                                notificationManager.dismissToast(toast.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Hook for accessing notification manager
 */
@Composable
fun rememberInAppNotificationManager(): InAppNotificationManager {
    return remember { InAppNotificationManager(NotificationService()) }
}
