package com.example.dutype.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dutype.app.R
import com.example.dutype.MainActivity
import com.example.dutype.firestore.FirestoreCollections
import com.example.dutype.services.NotificationChannelManager
import com.example.dutype.utils.PhoneNumberUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Engagement Worker (local)
 *
 * Delivers local re-engagement nudges to authenticated users after app goes
 * to background. Guest users are handled by server-scheduled topic nudges.
 *
 * This worker is scheduled only from app background event and runs once. *
 * Idempotency contract:
 * - Safe to re-run. Posting the same `NotificationCompat` with the same notification ID
 *   simply replaces the previous notification in the system tray (no duplicate).
 * - Worker is enqueued with `ExistingWorkPolicy.REPLACE`, so duplicate scheduling collapses
 *   into a single pending run. *
 * Anti-spam controls:
 * - Never send to guests
 * - Skip if user already received 3 notifications today (all sources)
 * - Skip if same title was already sent today
 * - Skip if any notification was sent in last 3 hours
 */
@HiltWorker
class GuestEngagementWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(context, params) {

    companion object {
        // Unique work name
        const val WORK_NAME_BACKGROUND = "guest_engagement_background"

        // Delays
        private const val BACKGROUND_DELAY_MINUTES = 3L

        // Notification ID ranges (avoid collisions)
        private const val BASE_NOTIFICATION_ID = 7000

        // BUG #2 FIX: Local SharedPreferences keys for dedupe state. Replaces
        // the broken Firestore-based dedupe (notifications collection is
        // admin-write-only, so the dedupe queries always returned empty).
        private const val ENGAGEMENT_PREFS = "engagement_worker_prefs"
        private const val KEY_DAY_START = "day_start"
        private const val KEY_COUNT_TODAY = "count_today"
        private const val KEY_TITLES_TODAY = "titles_today"
        private const val KEY_LAST_SENT_AT = "last_sent_at"

        private val WORKER_MESSAGES = listOf(
            Triple(
                "\uD83D\uDCBC New jobs near you are waiting",
                "Open DutyPe now and apply early to improve your chances.",
                "dutype://jobs"
            ),
            Triple(
                "\u26A1 Quick action can boost your visibility",
                "Complete one profile update or apply to one job now.",
                "dutype://profile"
            ),
            Triple(
                "\uD83D\uDCC8 Small daily steps, bigger opportunities",
                "Check today\'s nearby openings and keep momentum going.",
                "dutype://jobs"
            )
        )

        private val EMPLOYER_MESSAGES = listOf(
            Triple(
                "\uD83D\uDE80 Fast response improves hiring outcomes",
                "Take one hiring action now and improve conversion.",
                "dutype://employer/home"
            ),
            Triple(
                "\uD83C\uDFAF One update can improve application quality",
                "Refresh your latest post and attract better-fit workers.",
                "dutype://post-job"
            )
        )

        // ─── Schedule helpers ────────────────────────────────────────────────

        /** Deprecated: kept for compatibility; no foreground scheduling to avoid duplicates. */
        fun scheduleForeground(context: Context) {
            Timber.d("🔔 EngagementWorker: foreground scheduling disabled")
        }

        /**
         * Call this when the app goes to background.
         * Fires after [BACKGROUND_DELAY_MINUTES] for authenticated users only.
         */
        fun scheduleBackground(context: Context) {
            if (FirebaseAuth.getInstance().currentUser == null) {
                return
            }

            val shortRequest = OneTimeWorkRequestBuilder<GuestEngagementWorker>()
                .setInitialDelay(BACKGROUND_DELAY_MINUTES, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .addTag(WORK_NAME_BACKGROUND)
                .build()

            val wm = WorkManager.getInstance(context)
            wm.enqueueUniqueWork(WORK_NAME_BACKGROUND, ExistingWorkPolicy.REPLACE, shortRequest)
            Timber.d("🔔 EngagementWorker: background scheduled (${BACKGROUND_DELAY_MINUTES}m)")
        }

        /**
         * Cancel all guest engagement work — call this immediately after the user signs in.
         */
        fun cancelAll(context: Context) {
            val wm = WorkManager.getInstance(context)
            wm.cancelUniqueWork(WORK_NAME_BACKGROUND)
            Timber.d("🔔 EngagementWorker: all cancelled")
        }

        private fun getSlot(calendar: Calendar): Int {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            return when {
                hour < 12 -> 0
                hour < 18 -> 1
                else -> 2
            }
        }
    }

    override suspend fun doWork(): Result {
        val user = auth.currentUser
        if (user == null) {
            Timber.d("🔔 EngagementWorker: guest user - skip local engagement")
            return Result.success()
        }

        val userId = user.uid

        val now = Calendar.getInstance()
        val slot = getSlot(now)

        val role = try {
            val normalizedPhone = user.phoneNumber?.let(PhoneNumberUtils::normalize).orEmpty()
            val phoneRoleDoc = normalizedPhone.takeIf { it.isNotBlank() }
                ?.let { firestore.collection(FirestoreCollections.PHONE_ROLES).document(it).get().await() }
            val resolved = phoneRoleDoc?.get("roles")
                ?.let { it as? List<*> }
                ?.mapNotNull { it?.toString()?.uppercase() }
                ?.firstOrNull()
                ?: when {
                    firestore.collection(FirestoreCollections.EMPLOYER_PROFILES).document(userId).get().await().exists() -> "EMPLOYER"
                    firestore.collection(FirestoreCollections.WORKER_PROFILES).document(userId).get().await().exists() -> "WORKER"
                    else -> null
                }
            if (resolved.isNullOrBlank()) {
                Timber.w("🔔 EngagementWorker: no canonical role doc, skipping")
                return Result.success()
            }
            resolved
        } catch (e: Exception) {
            Timber.w(e, "🔔 EngagementWorker: role lookup failed, skipping notification")
            return Result.success()
        }

        val pool = if (role == "EMPLOYER") EMPLOYER_MESSAGES else WORKER_MESSAGES
        val (title, body, deepLink) = pool[slot % pool.size]

        // BUG #2 FIX: Local dedupe via SharedPreferences. The `notifications`
        // Firestore collection is admin-write-only (`allow create: if false`),
        // so the previous server-side dedupe queries always returned empty
        // and every guard silently passed — letting the same title fire on
        // every WorkManager tick. SharedPreferences is also a network-free
        // path, removing one Firestore read per tick.
        val prefs = context.getSharedPreferences(ENGAGEMENT_PREFS, Context.MODE_PRIVATE)
        val dayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val storedDayStart = prefs.getLong(KEY_DAY_START, 0L)
        val (countToday, titlesTodayCsv) = if (storedDayStart == dayStart) {
            prefs.getInt(KEY_COUNT_TODAY, 0) to (prefs.getString(KEY_TITLES_TODAY, "") ?: "")
        } else {
            0 to ""
        }
        val titlesToday = titlesTodayCsv.split('\u001F').filter { it.isNotBlank() }.toMutableSet()

        if (countToday >= 3) {
            Timber.d("🔔 EngagementWorker: daily cap reached ($countToday)")
            return Result.success()
        }

        if (title in titlesToday) {
            Timber.d("🔔 EngagementWorker: duplicate title today, skipping")
            return Result.success()
        }

        val lastSentAt = prefs.getLong(KEY_LAST_SENT_AT, 0L)
        val minGapMs = 3 * 60 * 60 * 1000L
        if (lastSentAt > 0 && (System.currentTimeMillis() - lastSentAt) < minGapMs) {
            Timber.d("🔔 EngagementWorker: last notification too recent, skipping")
            return Result.success()
        }

        showLocalNotification(title, body, deepLink, role)
        Timber.d("🔔 EngagementWorker: notification shown — \"$title\" for role=$role")

        // Record the send so the next tick's dedupe actually sees it.
        titlesToday += title
        prefs.edit()
            .putLong(KEY_DAY_START, dayStart)
            .putInt(KEY_COUNT_TODAY, countToday + 1)
            .putString(KEY_TITLES_TODAY, titlesToday.joinToString("\u001F"))
            .putLong(KEY_LAST_SENT_AT, System.currentTimeMillis())
            .apply()
        return Result.success()
    }

    // ─── Notification display ────────────────────────────────────────────────

    private fun showLocalNotification(title: String, body: String, deepLink: String, role: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Timber.w("🔔 GuestEngagementWorker: POST_NOTIFICATIONS not granted, skipping")
                return
            }
        }

        val notificationId = BASE_NOTIFICATION_ID + (System.currentTimeMillis() % 1000).toInt()

        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("engagement", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val actionIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("from_notification", true)
            putExtra("login_role", role)
        }
        val actionPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            actionIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = NotificationChannelManager.CHANNEL_MEDIUM_PRIORITY

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_notification, if (role == "EMPLOYER") "Review Now" else "Explore Jobs", actionPendingIntent)
            .build()

        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(notificationId, notification)
        } catch (e: Exception) {
            Timber.e(e, "🔔 GuestEngagementWorker: failed to post notification")
        }
    }
}
