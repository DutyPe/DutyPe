package com.example.dutype.notifications

import com.example.dutype.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local notification service for managing in-memory notifications
 * NOTE: This is separate from the main NotificationService in services package
 * which handles Firebase/Firestore notifications
 */
@Singleton
class LocalNotificationService @Inject constructor() {
    
    // In-memory storage for notifications (in production, use Room database)
    private val notificationsList = mutableListOf<Notification>()
    private val notificationPreferences = mutableMapOf<String, NotificationPreferences>()
    
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
    
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    
    init {
        // Initialize with some dummy notifications for demo
        initializeDummyNotifications()
    }
    
    /**
     * Get all notifications for a user
     */
    fun getNotifications(userId: String): Flow<List<Notification>> = flow {
        val userNotifications = notificationsList.filter { it.userId == userId }
        emit(userNotifications)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get unread notifications count
     */
    fun getUnreadCount(userId: String): Flow<Int> = flow {
        val count = notificationsList.count { it.userId == userId && !it.isRead }
        emit(count)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Mark notification as read
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            val index = notificationsList.indexOfFirst { it.id == notificationId }
            if (index != -1) {
                notificationsList[index] = notificationsList[index].copy(
                    isRead = true,
                    readAt = System.currentTimeMillis()
                )
                _notifications.value = notificationsList.toList()
                updateUnreadCount()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Notification not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark all notifications as read
     */
    suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            notificationsList.replaceAll { notification ->
                if (notification.userId == userId && !notification.isRead) {
                    notification.copy(
                        isRead = true,
                        readAt = now
                    )
                } else {
                    notification
                }
            }
            _notifications.value = notificationsList.toList()
            updateUnreadCount()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Archive notification
     */
    suspend fun archiveNotification(notificationId: String): Result<Unit> {
        return try {
        val index = notificationsList.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            notificationsList[index] = notificationsList[index].copy(
                isArchived = true,
                archivedAt = System.currentTimeMillis()
            )
            _notifications.value = notificationsList.toList()
                updateUnreadCount()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Notification not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete notification
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            val removed = notificationsList.removeAll { it.id == notificationId }
            if (removed) {
                _notifications.value = notificationsList.toList()
                updateUnreadCount()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Notification not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a new notification
     */
    suspend fun createNotification(
        userId: String,
        title: String,
        message: String,
        type: NotificationType,
        priority: NotificationPriority = NotificationPriority.NORMAL,
        relatedJobId: String? = null,
        relatedApplicationId: String? = null,
        actionData: Map<String, String> = emptyMap()
    ): Result<Notification> {
        return try {
            val notification = Notification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = message,
                type = type,
                priority = priority,
                userId = userId,
                relatedJobId = relatedJobId,
                relatedApplicationId = relatedApplicationId,
                actionData = actionData
            )
            
            notificationsList.add(notification)
            _notifications.value = notificationsList.toList()
            updateUnreadCount()
            
            Result.success(notification)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get notification preferences
     */
    fun getNotificationPreferences(userId: String): NotificationPreferences {
        return notificationPreferences[userId] ?: NotificationPreferences(userId = userId)
    }
    
    /**
     * Update notification preferences
     */
    suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<Unit> {
        return try {
            notificationPreferences[preferences.userId] = preferences
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get notification statistics
     */
    fun getNotificationStats(userId: String): NotificationStats {
        val userNotifications = notificationsList.filter { it.userId == userId }
        val unreadCount = userNotifications.count { !it.isRead }
        val todayCount = userNotifications.count { 
            System.currentTimeMillis() - it.createdAt < 86400_000 
        }
        val thisWeekCount = userNotifications.count { 
            System.currentTimeMillis() - it.createdAt < 604800_000 
        }
        
        val byType = userNotifications.groupingBy { it.type }.eachCount()
        
        return NotificationStats(
            totalNotifications = userNotifications.size,
            unreadCount = unreadCount,
            todayCount = todayCount,
            thisWeekCount = thisWeekCount,
            byType = byType
        )
    }
    
    /**
     * Clear all notifications for a user
     */
    suspend fun clearAllNotifications(userId: String): Result<Unit> {
        return try {
            notificationsList.removeAll { it.userId == userId }
            _notifications.value = notificationsList.toList()
            updateUnreadCount()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update unread count
     */
    private fun updateUnreadCount() {
        val count = notificationsList.count { !it.isRead }
        _unreadCount.value = count
    }
    
    /**
     * Get notifications for in-app display (unread, recent)
     */
    fun getInAppNotifications(userId: String): Flow<List<Notification>> = flow {
        val userNotifications = notificationsList.filter { 
            it.userId == userId && !it.isRead 
        }.sortedByDescending { it.createdAt }.take(3)
        emit(userNotifications)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Create and show in-app notification
     */
    suspend fun createInAppNotification(
        userId: String,
        title: String,
        message: String,
        type: NotificationType,
        priority: NotificationPriority = NotificationPriority.NORMAL,
        relatedJobId: String? = null,
        relatedApplicationId: String? = null,
        actionData: Map<String, String> = emptyMap()
    ): Result<Notification> {
        return createNotification(
            userId = userId,
            title = title,
            message = message,
            type = type,
            priority = priority,
            relatedJobId = relatedJobId,
            relatedApplicationId = relatedApplicationId,
            actionData = actionData
        )
    }
    
    /**
     * Mark multiple notifications as read
     */
    suspend fun markMultipleAsRead(notificationIds: List<String>): Result<Unit> {
        return try {
            var successCount = 0
            notificationIds.forEach { id ->
                val result = markAsRead(id)
                if (result.isSuccess) successCount++
            }
            if (successCount == notificationIds.size) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Some notifications could not be marked as read"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get notification count by type
     */
    fun getNotificationCountByType(userId: String, type: NotificationType): Flow<Int> = flow {
        val count = notificationsList.count { 
            it.userId == userId && it.type == type && !it.isRead 
        }
        emit(count)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get recent notifications (last 24 hours)
     */
    fun getRecentNotifications(userId: String): Flow<List<Notification>> = flow {
        val recentTime = System.currentTimeMillis() - 86400_000 // 24 hours
        val recentNotifications = notificationsList.filter { 
            it.userId == userId && it.createdAt >= recentTime 
        }.sortedByDescending { it.createdAt }
        emit(recentNotifications)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Initialize dummy notifications for demo - No dummy data
     */
    private fun initializeDummyNotifications() {
        // No dummy notifications - return empty list
        _notifications.value = emptyList()
        updateUnreadCount()
    }
}
