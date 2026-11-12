package com.example.dutype.notifications

import android.content.Context
import com.example.dutype.models.NotificationData
import com.example.dutype.models.NotificationType
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.UUID

/**
 * Local Notification Store
 *
 * Persists received push notifications and default guest onboarding notifications
 * to local storage so that:
 * 1. Guest users always see rich in-app notifications (welcome, 0% commission, profile primer).
 * 2. Broadcast push notifications (e.g., from topics guest_users, all_users, workers)
 *    are captured and displayed in the in-app notification inbox even if they aren't
 *    stored as per-user documents in Firestore.
 * 3. Unread badges on Worker and Employer home screens work for guest users.
 */
object LocalNotificationStore {

    private const val PREF_NAME = "dutype_local_notifications"
    private const val KEY_NOTIFICATIONS = "cached_notifications_json"
    private const val KEY_GUEST_DISMISSED_DEFAULTS = "guest_dismissed_defaults"
    private const val MAX_STORED_NOTIFICATIONS = 30

    @Synchronized
    fun addNotification(context: Context, notification: NotificationData) {
        try {
            val list = getNotificationsInternal(context).toMutableList()
            // Deduplicate by ID
            list.removeAll { it.id == notification.id }
            list.add(0, notification)
            if (list.size > MAX_STORED_NOTIFICATIONS) {
                list.removeAt(list.lastIndex)
            }
            saveNotificationsInternal(context, list)
            Timber.d("LocalNotificationStore: Saved notification ${notification.id} ('${notification.title}')")
        } catch (e: Exception) {
            Timber.w(e, "LocalNotificationStore: Failed to add notification")
        }
    }

    @Synchronized
    fun getNotifications(context: Context, targetRole: String? = null): List<NotificationData> {
        val stored = getNotificationsInternal(context)
        val normalizedRole = targetRole?.uppercase().orEmpty()

        val roleFiltered = if (normalizedRole.isNotBlank()) {
            stored.filter { notif ->
                notif.targetRole.isBlank() || notif.targetRole.equals(normalizedRole, ignoreCase = true)
            }
        } else {
            stored
        }

        // If no stored push notifications yet, serve default guest notifications
        if (roleFiltered.isEmpty()) {
            return getDefaultGuestNotifications(context, targetRole ?: "WORKER")
        }

        return roleFiltered
    }

    /**
     * Retrieve ONLY real stored push notifications received on this device (excluding guest demo cards).
     * Used for logged-in users to merge with server-side notifications.
     */
    @Synchronized
    fun getStoredBroadcastNotifications(context: Context, targetRole: String? = null): List<NotificationData> {
        val stored = getNotificationsInternal(context)
        val normalizedRole = targetRole?.uppercase().orEmpty()

        val filtered = if (normalizedRole.isNotBlank()) {
            stored.filter { notif ->
                notif.targetRole.isBlank() || notif.targetRole.equals(normalizedRole, ignoreCase = true)
            }
        } else {
            stored
        }

        return filtered.filterNot { it.id.startsWith("guest_") }
    }

    fun getDefaultGuestNotifications(context: Context, role: String): List<NotificationData> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_GUEST_DISMISSED_DEFAULTS, false)) {
            return emptyList()
        }

        val isEmployer = role.equals("EMPLOYER", ignoreCase = true)
        val now = System.currentTimeMillis()

        return if (isEmployer) {
            listOf(
                NotificationData(
                    id = "guest_emp_welcome_1",
                    title = "Welcome to DutyPe Employer! 🏢",
                    message = "Hire verified local workers near you in minutes with direct employer contact.",
                    type = NotificationType.WELCOME,
                    targetRole = "EMPLOYER",
                    createdAt = now - 60_000,
                    data = mapOf("deepLink" to "dutype://employer/post-job"),
                    isRead = false
                ),
                NotificationData(
                    id = "guest_emp_welcome_2",
                    title = "Zero Agent Commission ⚡",
                    message = "Post job openings for free and connect directly with workers near your business.",
                    type = NotificationType.JOB_UPDATE,
                    targetRole = "EMPLOYER",
                    createdAt = now - 300_000,
                    data = mapOf("deepLink" to "dutype://employer/home"),
                    isRead = false
                )
            )
        } else {
            listOf(
                NotificationData(
                    id = "guest_worker_welcome_1",
                    title = "Welcome to DutyPe! 🇮🇳",
                    message = "Explore 300+ local jobs in your area. Direct spot hiring with 0% commission.",
                    type = NotificationType.WELCOME,
                    targetRole = "WORKER",
                    createdAt = now - 60_000,
                    data = mapOf("deepLink" to "dutype://worker/home"),
                    isRead = false
                ),
                NotificationData(
                    id = "guest_worker_welcome_2",
                    title = "Direct Employer Contact ⚡",
                    message = "Call verified employers directly. No agents, no middlemen, 100% free.",
                    type = NotificationType.NEW_JOB_ALERT,
                    targetRole = "WORKER",
                    createdAt = now - 300_000,
                    data = mapOf("deepLink" to "dutype://worker/all-jobs"),
                    isRead = false
                ),
                NotificationData(
                    id = "guest_worker_welcome_3",
                    title = "Complete Profile to Get Hired 🚀",
                    message = "Set your job category and location to receive direct calls from employers.",
                    type = NotificationType.PROFILE_REMINDER,
                    targetRole = "WORKER",
                    createdAt = now - 600_000,
                    data = mapOf("deepLink" to "dutype://worker/profile"),
                    isRead = false
                )
            )
        }
    }

    @Synchronized
    fun markAsRead(context: Context, notificationId: String) {
        try {
            val list = getNotificationsInternal(context).toMutableList()
            var modified = false
            for (i in list.indices) {
                if (list[i].id == notificationId) {
                    list[i] = list[i].copy(isRead = true)
                    modified = true
                    break
                }
            }
            if (modified) {
                saveNotificationsInternal(context, list)
            }
        } catch (e: Exception) {
            Timber.w(e, "LocalNotificationStore: Failed to mark as read")
        }
    }

    @Synchronized
    fun markAllAsRead(context: Context) {
        try {
            val list = getNotificationsInternal(context).map { it.copy(isRead = true) }
            saveNotificationsInternal(context, list)
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_GUEST_DISMISSED_DEFAULTS, true).apply()
        } catch (e: Exception) {
            Timber.w(e, "LocalNotificationStore: Failed to mark all as read")
        }
    }

    @Synchronized
    fun deleteNotification(context: Context, notificationId: String) {
        try {
            val list = getNotificationsInternal(context).toMutableList()
            list.removeAll { it.id == notificationId }
            saveNotificationsInternal(context, list)
            if (notificationId.startsWith("guest_")) {
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY_GUEST_DISMISSED_DEFAULTS, true).apply()
            }
        } catch (e: Exception) {
            Timber.w(e, "LocalNotificationStore: Failed to delete notification")
        }
    }

    @Synchronized
    fun getUnreadCount(context: Context): Int {
        val stored = getNotificationsInternal(context)
        if (stored.isNotEmpty()) {
            return stored.count { !it.isRead }
        }
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return if (!prefs.getBoolean(KEY_GUEST_DISMISSED_DEFAULTS, false)) {
            // Default guest unread count (shows red dot on bell)
            2
        } else {
            0
        }
    }

    private fun getNotificationsInternal(context: Context): List<NotificationData> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val result = mutableListOf<NotificationData>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val dataMap = mutableMapOf<String, String>()
                val dataObj = obj.optJSONObject("data")
                if (dataObj != null) {
                    val keys = dataObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        dataMap[key] = dataObj.optString(key)
                    }
                }
                result.add(
                    NotificationData(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        recipientId = obj.optString("recipientId", ""),
                        title = obj.optString("title", ""),
                        message = obj.optString("message", ""),
                        type = runCatching { NotificationType.valueOf(obj.optString("type")) }.getOrDefault(NotificationType.GENERAL),
                        targetRole = obj.optString("targetRole", ""),
                        data = dataMap,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        isRead = obj.optBoolean("isRead", false)
                    )
                )
            }
            result
        } catch (e: Exception) {
            Timber.w(e, "LocalNotificationStore: Parse error")
            emptyList()
        }
    }

    private fun saveNotificationsInternal(context: Context, notifications: List<NotificationData>) {
        val arr = JSONArray()
        for (item in notifications) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("recipientId", item.recipientId)
            obj.put("title", item.title)
            obj.put("message", item.message)
            obj.put("type", item.type.name)
            obj.put("targetRole", item.targetRole)
            obj.put("createdAt", item.createdAt)
            obj.put("isRead", item.isRead)
            val dataObj = JSONObject()
            item.data.forEach { (k, v) -> dataObj.put(k, v) }
            obj.put("data", dataObj)
            arr.put(obj)
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NOTIFICATIONS, arr.toString())
            .apply()
    }
}
