package com.example.dutype.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.dutype.app.R
import com.example.dutype.firestore.FirestoreSchema.Notifications
import com.example.dutype.models.NotificationData
import com.example.dutype.models.NotificationType
import com.example.dutype.utils.epochMillis
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The signed-in user's notification inbox and on-device notifications.
 *
 * Inbox documents (`notifications`) are written only by Cloud Functions (lib/notify.ts), which
 * also send the FCM push; they expire through a TTL policy on `expireAt`. The app pages its own
 * inbox, marks items read and deletes them. Notifications about the user's own actions
 * (profile complete, birthday…) are shown locally only.
 */
@Singleton
class NotificationService @Inject constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore
) {
    data class NotificationPage(
        val notifications: List<NotificationData>,
        val lastVisible: DocumentSnapshot?,
        val hasMore: Boolean
    )

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannelManager.createNotificationChannels(context)
        }
    }

    /** One page of the inbox, newest first (cursor pagination, never the whole collection). */
    @Suppress("UNUSED_PARAMETER")
    fun getUserNotificationsPage(
        userId: String,
        activeRole: String? = null,
        pageSize: Int = 30,
        lastVisible: DocumentSnapshot? = null
    ): Flow<Result<NotificationPage>> = flow {
        try {
            val size = pageSize.coerceIn(10, 50)
            var query: Query = firestore.collection(Notifications.COLLECTION)
                .whereEqualTo(Notifications.RECIPIENT_ID, userId)
                .orderBy(Notifications.CREATED_AT, Query.Direction.DESCENDING)
                .limit(size.toLong())
            if (lastVisible != null) query = query.startAfter(lastVisible)
            val snapshot = query.get().await()
            val now = System.currentTimeMillis()
            emit(
                Result.success(
                    NotificationPage(
                        notifications = snapshot.documents.mapNotNull { parse(it, now) },
                        lastVisible = snapshot.documents.lastOrNull(),
                        hasMore = snapshot.size() >= size
                    )
                )
            )
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> = runCatching {
        firestore.collection(Notifications.COLLECTION).document(notificationId)
            .update(Notifications.READ, true).await()
        Unit
    }

    /** Unread badge: one count aggregation (no documents downloaded). */
    @Suppress("UNUSED_PARAMETER")
    suspend fun getUnreadNotificationCount(userId: String, activeRole: String? = null): Result<Int> = runCatching {
        firestore.collection(Notifications.COLLECTION)
            .whereEqualTo(Notifications.RECIPIENT_ID, userId)
            .whereEqualTo(Notifications.READ, false)
            .count().get(AggregateSource.SERVER).await().count.toInt()
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> = runCatching {
        firestore.collection(Notifications.COLLECTION).document(notificationId).delete().await()
        Unit
    }

    /**
     * Shows [notification] on this device when it is for the signed-in user. Notifications for
     * other people are sent by Cloud Functions, never from the app.
     */
    fun sendNotification(notification: NotificationData, recipientId: String) {
        val me = FirebaseAuth.getInstance().currentUser?.uid
        if (me == null || recipientId != me) {
            Timber.d("NotificationService: skipped notification for another user (server-side only)")
            return
        }
        val deepLink = com.example.dutype.utils.NotificationDeepLinkBuilder.buildDeepLink(notification.type, notification.data)
        showLocalNotification(notification.copy(recipientId = recipientId, data = notification.data + ("deepLink" to deepLink)))
    }

    /** Local welcome message after the profile is completed for the first time. */
    fun sendProfileCompleteNotification(userName: String, userId: String, userRole: String): Result<Unit> = runCatching {
        val isEmployer = userRole.equals("EMPLOYER", ignoreCase = true)
        val title = com.example.dutype.utils.LocaleHelper.getLocalizedString(context, R.string.notif_profile_complete_title)
        val message = com.example.dutype.utils.LocaleHelper.getLocalizedString(
            context,
            if (isEmployer) R.string.notif_profile_complete_employer_msg else R.string.notif_profile_complete_worker_msg,
            userName
        )
        sendNotification(
            NotificationData(
                id = UUID.randomUUID().toString(),
                title = title,
                message = message,
                type = NotificationType.PROFILE_COMPLETE,
                targetRole = userRole.uppercase(),
                data = mapOf("action" to if (isEmployer) "view_employer_home" else "view_worker_home"),
                createdAt = System.currentTimeMillis()
            ),
            userId
        )
    }

    private fun parse(doc: DocumentSnapshot, now: Long): NotificationData? {
        val d = doc.data ?: return null
        val createdAt = d[Notifications.CREATED_AT].epochMillis().takeIf { it > 0 } ?: now
        val expiresAt = d[Notifications.EXPIRE_AT].epochMillis().takeIf { it > 0 } ?: (createdAt + RETENTION_MS)
        if (expiresAt <= now) return null // TTL deletion can lag by up to a day
        return NotificationData(
            id = doc.id,
            recipientId = d[Notifications.RECIPIENT_ID] as? String ?: "",
            title = d[Notifications.TITLE] as? String ?: "",
            message = d[Notifications.BODY] as? String ?: "",
            type = parseNotificationType(d[Notifications.TYPE] as? String ?: "GENERAL"),
            data = (d[Notifications.DATA] as? Map<*, *>)
                ?.entries?.associate { it.key.toString() to it.value.toString() }
                .orEmpty(),
            createdAt = createdAt,
            expiresAt = expiresAt,
            isRead = d[Notifications.READ] as? Boolean ?: false
        )
    }

    private fun showLocalNotification(notification: NotificationData) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Timber.w("NotificationService: POST_NOTIFICATIONS permission not granted")
            return
        }
        val deepLinkUri = notification.data["deepLink"]?.takeIf { it.isNotBlank() }?.let(Uri::parse)
        val isExternal = deepLinkUri?.scheme == "market" ||
            (deepLinkUri?.scheme in listOf("http", "https") && deepLinkUri?.host.orEmpty().contains("play.google.com"))
        val intent = when {
            deepLinkUri == null -> Intent(context, com.example.dutype.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("notificationId", notification.id)
            }
            isExternal -> Intent(Intent.ACTION_VIEW, deepLinkUri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            else -> Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
                setClass(context, com.example.dutype.MainActivity::class.java)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
        val systemId = notification.id.hashCode()
        intent.putExtra("from_notification", true)
        intent.putExtra("notification_system_id", systemId)
        val pendingIntent = PendingIntent.getActivity(
            context, systemId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_HIGH_PRIORITY)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(NotificationIcons.tintColor(context))
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply { NotificationIcons.largeIcon(context)?.let { setLargeIcon(it) } }
        try {
            notificationManager.notify(systemId, builder.build())
        } catch (e: SecurityException) {
            Timber.e(e, "NotificationService: failed to show local notification")
        }
    }

    private companion object {
        const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000
    }
}

/**
 * Server type string -> app type. Wallet events (WITHDRAWAL_*, REFERRAL_REWARD, SIGNUP_BONUS, ...)
 * map to PAYMENT so they show under "Payments".
 */
internal fun parseNotificationType(raw: String): NotificationType {
    val upper = raw.trim().uppercase()
    val isPayment = upper.startsWith("WITHDRAWAL") ||
        upper in setOf(
            "REFERRAL_REWARD", "REFERRAL_PENDING", "SIGNUP_BONUS", "SIGNUP_BONUS_PENDING",
            "MILESTONE_REWARD", "WELCOME_BONUS", "ADMIN_TEST_CREDIT", "PAYMENT", "PAYOUT", "WALLET_CREDIT"
        )
    if (isPayment) return NotificationType.PAYMENT
    return runCatching { NotificationType.valueOf(upper) }.getOrDefault(NotificationType.GENERAL)
}
