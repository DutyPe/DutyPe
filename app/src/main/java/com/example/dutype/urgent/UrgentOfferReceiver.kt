package com.example.dutype.urgent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber

/**
 * Accept / Skip tapped on an urgent offer notification — works without opening the app. The
 * answer replaces the offer notification ("The job is yours", "Already filled", …). If the
 * network fails, the notification says so and opens the offer page, where Accept can be retried.
 */
class UrgentOfferReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val requestId = intent.getStringExtra(UrgentOffers.EXTRA_REQUEST_ID) ?: return
        val notificationId = intent.getIntExtra(UrgentOffers.EXTRA_NOTIFICATION_ID, UrgentOffers.notificationId(requestId))
        when (intent.action) {
            UrgentOffers.ACTION_SKIP -> NotificationManagerCompat.from(context).cancel(notificationId)
            UrgentOffers.ACTION_ACCEPT -> {
                val pending = goAsync()
                val appContext = context.applicationContext
                scope.launch {
                    try {
                        val result = withTimeout(ACCEPT_TIMEOUT_MS) {
                            UrgentOffers.accept(FirebaseFunctions.getInstance("asia-south1"), requestId)
                        }
                        UrgentOffers.showResult(appContext, requestId, result)
                    } catch (e: Exception) {
                        Timber.w(e, "Accept from notification failed")
                        UrgentOffers.showRetry(appContext, requestId)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private companion object {
        /** A broadcast receiver has ~10 s; leave room to post the answer. */
        const val ACCEPT_TIMEOUT_MS = 8_000L
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
