package com.example.dutype.urgent

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dutype.app.R
import com.example.dutype.MainActivity
import com.example.dutype.services.NotificationChannelManager
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * Urgent job offers — the worker side of dispatch (server: functions/src/urgent.ts).
 *
 * An offer arrives as a data push (type URGENT_OFFER) on the "urgent_offers" channel with
 * Accept / Skip buttons. Accept works straight from the notification ([UrgentOfferReceiver]) or
 * from the full-screen offer page (dutype://urgent/{requestId}); both call [accept].
 */
object UrgentOffers {
    const val TYPE = "URGENT_OFFER"
    const val ACTION_ACCEPT = "com.dutype.app.URGENT_ACCEPT"
    const val ACTION_SKIP = "com.dutype.app.URGENT_SKIP"
    const val EXTRA_REQUEST_ID = "requestId"
    const val EXTRA_NOTIFICATION_ID = "notificationId"

    /** What happened when the worker tapped Accept. */
    sealed interface Result {
        data class Accepted(
            val title: String,
            val contactNumber: String,
            val addressText: String,
            val lat: Double,
            val lng: Double
        ) : Result
        /** All places were taken by faster workers. */
        data object Filled : Result
        /** The job was cancelled, expired or removed. */
        data object Closed : Result
        /** The employer took this worker off the job; they cannot take it again. */
        data object Removed : Result
        /** The worker already has an unfinished urgent job. */
        data object Busy : Result
    }

    fun deepLink(requestId: String): String = "dutype://urgent/$requestId"

    fun notificationId(requestId: String): Int = ("urgent_$requestId").hashCode()

    suspend fun accept(functions: FirebaseFunctions, requestId: String): Result {
        @Suppress("UNCHECKED_CAST")
        val data = functions.getHttpsCallable("acceptUrgentOffer")
            .call(mapOf("requestId" to requestId))
            .await().data as? Map<String, Any?> ?: emptyMap()
        return when (data["result"]) {
            "accepted" -> Result.Accepted(
                title = data["title"] as? String ?: "",
                contactNumber = data["contactNumber"] as? String ?: "",
                addressText = data["addressText"] as? String ?: "",
                lat = (data["lat"] as? Number)?.toDouble() ?: 0.0,
                lng = (data["lng"] as? Number)?.toDouble() ?: 0.0
            )
            "filled" -> Result.Filled
            "removed" -> Result.Removed
            "busy" -> Result.Busy
            else -> Result.Closed
        }
    }

    private fun canNotify(context: Context): Boolean =
        android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openOfferIntent(context: Context, requestId: String, requestCode: Int): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink(requestId))).apply {
            setClass(context, MainActivity::class.java)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("from_notification", true)
            putExtra("notification_deep_link", deepLink(requestId))
        }
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun actionIntent(context: Context, action: String, requestId: String, notificationId: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, UrgentOfferReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_REQUEST_ID, requestId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** The ringing offer: big text, Accept / Skip right on the notification, gone after 10 minutes. */
    fun showOffer(context: Context, data: Map<String, String>) {
        val requestId = data["requestId"]?.takeIf { it.isNotBlank() } ?: return
        if (UrgentSoundAlertManager.isIgnored(context, requestId)) return
        if (!canNotify(context)) return
        val id = notificationId(requestId)
        val title = data["title"].orEmpty().ifBlank { context.getString(R.string.urgent_offer_title_fallback) }
        val body = data["body"].orEmpty()
        val notification = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_URGENT_OFFERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setSound(android.provider.Settings.System.DEFAULT_RINGTONE_URI)
            .setVibrate(longArrayOf(0, 800, 400, 800, 400, 800))
            .setTimeoutAfter(10 * 60 * 1000L)
            .setContentIntent(openOfferIntent(context, requestId, id))
            .addAction(0, context.getString(R.string.urgent_offer_accept), actionIntent(context, ACTION_ACCEPT, requestId, id, id + 1))
            .addAction(0, context.getString(R.string.urgent_offer_skip), actionIntent(context, ACTION_SKIP, requestId, id, id + 2))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
            UrgentSoundAlertManager.playAlertIfNew(context, requestId)
        } catch (_: SecurityException) {
        }
    }

    /** Accept from the notification could not reach the server: tap opens the offer page to retry. */
    fun showRetry(context: Context, requestId: String) =
        showMessage(context, requestId, context.getString(R.string.urgent_offer_retry_title), context.getString(R.string.urgent_offer_retry_body))

    /** Replaces the offer with what happened after Accept from the notification. */
    fun showResult(context: Context, requestId: String, result: Result) {
        val (title, body) = when (result) {
            is Result.Accepted -> context.getString(R.string.urgent_offer_yours_title) to
                context.getString(R.string.urgent_offer_yours_body, result.title, result.addressText)
            Result.Filled -> context.getString(R.string.urgent_offer_filled_title) to context.getString(R.string.urgent_offer_filled_body)
            Result.Busy -> context.getString(R.string.urgent_offer_busy_title) to context.getString(R.string.urgent_offer_busy_body)
            Result.Removed, Result.Closed -> context.getString(R.string.urgent_offer_closed_title) to context.getString(R.string.urgent_offer_closed_body)
        }
        showMessage(context, requestId, title, body)
    }

    private fun showMessage(context: Context, requestId: String, title: String, body: String) {
        if (!canNotify(context)) return
        val id = notificationId(requestId)
        val notification = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_HIGH_PRIORITY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(openOfferIntent(context, requestId, id + 3))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
        }
    }
}
