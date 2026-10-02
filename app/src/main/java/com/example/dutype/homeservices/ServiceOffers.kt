package com.example.dutype.homeservices

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
import timber.log.Timber

/**
 * A DutyPe Services job offer for a partner (data push type SERVICE_OFFER from services.ts).
 * Rings on the urgent-offers channel; tapping opens the offer screen (dutype://partner/offer/{id}).
 */
object ServiceOffers {
    const val TYPE = "SERVICE_OFFER"

    fun showOffer(context: Context, data: Map<String, String>) {
        val bookingId = data["bookingId"]?.takeIf { it.isNotBlank() } ?: return
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val deepLink = data["deepLink"]?.takeIf { it.isNotBlank() } ?: "dutype://partner/offer/$bookingId"
        val id = ("service_$bookingId").hashCode()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
            setClass(context, MainActivity::class.java)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("from_notification", true)
            putExtra("notification_deep_link", deepLink)
        }
        val pending = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val body = data["body"].orEmpty()
        val notification = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_URGENT_OFFERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(data["title"].orEmpty().ifBlank { context.getString(R.string.svc_offer_title) })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(10 * 60 * 1000L)
            .setContentIntent(pending)
            .addAction(0, context.getString(R.string.svc_offer_accept), pending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            Timber.w(e, "service offer notification blocked")
        }
    }
}
