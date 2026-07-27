package com.example.dutype.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dutype.app.R
import com.example.dutype.MainActivity
import com.example.dutype.firestore.FirestoreCollections
import com.example.dutype.services.NotificationChannelManager
import com.example.dutype.services.NotificationIcons
import com.example.dutype.utils.PhoneNumberUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Local role-aware engagement worker.
 *
 * WorkManager keeps this repeating work alive while the app is normally closed,
 * so authenticated users continue receiving daily re-engagement notifications
 * without needing to reopen the app first. Android can still delay background
 * work for battery/Doze, and no app can run after the user force-stops it.
 */
@HiltWorker
class GuestEngagementWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(context, params) {

    private data class EngagementMessage(
        val title: String,
        val body: String,
        val deepLink: String
    )

    companion object {
        const val WORK_NAME_BACKGROUND = "guest_engagement_background"
        private const val WORK_NAME_RECURRING = "role_engagement_recurring"
        private const val BACKGROUND_DELAY_MINUTES = 3L
        private const val RECURRING_INTERVAL_HOURS = 8L
        private const val BASE_NOTIFICATION_ID = 7000
        // Quiet hours — never post local engagement notifications late night / early morning.
        private const val QUIET_HOURS_START_HOUR = 22 // 10 PM (inclusive)
        private const val QUIET_HOURS_END_HOUR = 8    // 8 AM (exclusive)

        private const val ENGAGEMENT_PREFS = "engagement_worker_prefs"
        private const val KEY_DAY_START = "day_start"
        private const val KEY_COUNT_TODAY = "count_today"
        private const val KEY_TITLES_TODAY = "titles_today"
        private const val KEY_LAST_SENT_AT = "last_sent_at"
        private const val TITLE_SEPARATOR = "\u001F"

        private val WORKER_MESSAGES = listOf(
            listOf(
                EngagementMessage("Fresh jobs picked for today", "Open DutyPe and apply early before nearby openings fill up.", "dutype://worker/jobs"),
                EngagementMessage("A job near you is hiring today", "Open DutyPe and apply early before this opening fills up.", "dutype://worker/jobs"),
                EngagementMessage("Evening jobs are moving fast", "Check the latest openings and save the best match for tomorrow.", "dutype://worker/jobs")
            ),
            listOf(
                EngagementMessage("New local work is available", "A quick check now can put your application near the top.", "dutype://worker/jobs"),
                EngagementMessage("Someone nearby is hiring now", "Check the newest opening and send a quick application.", "dutype://worker/jobs"),
                EngagementMessage("Do not miss today's best match", "Nearby jobs change daily. Open DutyPe and see what is active.", "dutype://worker/jobs")
            ),
            listOf(
                EngagementMessage("Employers are looking today", "Apply to suitable roles now while response chances are higher.", "dutype://worker/jobs"),
                EngagementMessage("A fresh job just opened near you", "Tap to view the details and apply in seconds.", "dutype://worker/jobs"),
                EngagementMessage("Your next duty may be nearby", "Browse fresh job posts and keep your applications moving.", "dutype://worker/jobs")
            ),
            listOf(
                EngagementMessage("Start the day with new openings", "Good jobs go quickly. Check DutyPe before the rush.", "dutype://worker/jobs"),
                EngagementMessage("New work is waiting for you", "See today's openings and pick the one that fits you.", "dutype://worker/jobs"),
                EngagementMessage("Tonight's chance is still open", "Review saved and new jobs before the day ends.", "dutype://worker/jobs")
            ),
            listOf(
                EngagementMessage("More jobs, better timing", "Apply early today and improve your chance of getting noticed.", "dutype://worker/jobs"),
                EngagementMessage("Don't miss today's best job", "Good openings get filled fast — apply before they close.", "dutype://worker/jobs"),
                EngagementMessage("Keep your job search active", "Open DutyPe for fresh nearby work and quick applications.", "dutype://worker/jobs")
            ),
            listOf(
                EngagementMessage("Today's openings need quick action", "Check new jobs and apply before other workers fill the queue.", "dutype://worker/jobs"),
                EngagementMessage("A job that fits you is live now", "Open DutyPe and grab it before other workers do.", "dutype://worker/jobs"),
                EngagementMessage("New work can appear anytime", "See what changed today and save the roles you like.", "dutype://worker/jobs")
            ),
            listOf(
                EngagementMessage("A fresh week of jobs starts here", "Open DutyPe and find work close to your location.", "dutype://worker/jobs"),
                EngagementMessage("Your next job could be one tap away", "Browse fresh openings near you and apply right now.", "dutype://worker/jobs"),
                EngagementMessage("End the day with one smart apply", "Choose one good job and send your application now.", "dutype://worker/jobs")
            )
        )

        private val EMPLOYER_MESSAGES = listOf(
            listOf(
                EngagementMessage("Good workers respond fastest", "Review new applications and contact strong matches today.", "dutype://employer/applications"),
                EngagementMessage("Need staff for tomorrow?", "Post or refresh a job so workers can apply before morning.", "dutype://employer/post-job")
            ),
            listOf(
                EngagementMessage("Your next hire may be waiting", "Check applications and shortlist workers before they move on.", "dutype://employer/applications"),
                EngagementMessage("Keep your job post visible", "A fresh post attracts more serious workers in your area.", "dutype://employer/jobs")
            ),
            listOf(
                EngagementMessage("Hire faster with quick follow-up", "Open DutyPe and call the best applicants while they are active.", "dutype://employer/applications"),
                EngagementMessage("One new post can solve today's need", "Tell workers what you need and start receiving applications.", "dutype://employer/post-job")
            ),
            listOf(
                EngagementMessage("Workers are checking jobs today", "Make sure your openings are active and easy to apply for.", "dutype://employer/jobs"),
                EngagementMessage("Do not lose a strong applicant", "Review pending applications and move quickly on good matches.", "dutype://employer/applications")
            ),
            listOf(
                EngagementMessage("Need reliable help this week?", "Post a clear job with pay, timing, and location now.", "dutype://employer/post-job"),
                EngagementMessage("Your hiring queue needs a look", "Shortlist or contact applicants before the day ends.", "dutype://employer/applications")
            ),
            listOf(
                EngagementMessage("Fresh applicants can arrive anytime", "Open DutyPe and check who is ready to work.", "dutype://employer/applications"),
                EngagementMessage("Better job details bring better workers", "Update your post and attract people who fit the role.", "dutype://employer/jobs")
            ),
            listOf(
                EngagementMessage("Start the week with stronger hiring", "Post your staffing need and reach local workers quickly.", "dutype://employer/post-job"),
                EngagementMessage("Close hiring gaps before tomorrow", "Review applications and contact the best worker now.", "dutype://employer/applications")
            )
        )

        fun scheduleForeground(context: Context) {
            scheduleRecurring(context)
        }

        fun scheduleRecurring(context: Context) {
            if (FirebaseAuth.getInstance().currentUser == null) return

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<GuestEngagementWorker>(
                RECURRING_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
                .setInitialDelay(2, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.MINUTES
                )
                .addTag(WORK_NAME_RECURRING)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_RECURRING,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Timber.d("EngagementWorker: recurring role notifications scheduled every %dh", RECURRING_INTERVAL_HOURS)
        }

        fun scheduleBackground(context: Context) {
            if (FirebaseAuth.getInstance().currentUser == null) return

            scheduleRecurring(context)

            val shortRequest = OneTimeWorkRequestBuilder<GuestEngagementWorker>()
                .setInitialDelay(BACKGROUND_DELAY_MINUTES, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    5,
                    TimeUnit.MINUTES
                )
                .addTag(WORK_NAME_BACKGROUND)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_BACKGROUND,
                ExistingWorkPolicy.REPLACE,
                shortRequest
            )
            Timber.d("EngagementWorker: short background notification scheduled in %dm", BACKGROUND_DELAY_MINUTES)
        }

        fun cancelAll(context: Context) {
            val wm = WorkManager.getInstance(context)
            wm.cancelUniqueWork(WORK_NAME_BACKGROUND)
            wm.cancelUniqueWork(WORK_NAME_RECURRING)
            Timber.d("EngagementWorker: local engagement work cancelled")
        }

        private fun workerSlot(calendar: Calendar): Int {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            return when {
                hour < 12 -> 0
                hour < 18 -> 1
                else -> 2
            }
        }

        private fun employerSlot(calendar: Calendar): Int {
            return if (calendar.get(Calendar.HOUR_OF_DAY) < 15) 0 else 1
        }
    }

    override suspend fun doWork(): Result {
        val user = auth.currentUser ?: run {
            Timber.d("EngagementWorker: no authenticated user, skipping")
            return Result.success()
        }

        val role = resolveRole(user.uid, user.phoneNumber) ?: return Result.success()
        val now = Calendar.getInstance()

        // Quiet hours: never disturb users late at night / early morning (no ~2 AM pings).
        val hourOfDay = now.get(Calendar.HOUR_OF_DAY)
        if (hourOfDay >= QUIET_HOURS_START_HOUR || hourOfDay < QUIET_HOURS_END_HOUR) {
            Timber.d("EngagementWorker: within quiet hours (hour=%d), skipping notification", hourOfDay)
            return Result.success()
        }

        val dayIndex = now.get(Calendar.DAY_OF_YEAR) % 7
        val slot = if (role == "EMPLOYER") employerSlot(now) else workerSlot(now)
        val dailyCap = if (role == "EMPLOYER") 2 else 3
        val message = if (role == "EMPLOYER") {
            EMPLOYER_MESSAGES[dayIndex][slot]
        } else {
            WORKER_MESSAGES[dayIndex][slot]
        }

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
        val titlesToday = titlesTodayCsv.split(TITLE_SEPARATOR).filter { it.isNotBlank() }.toMutableSet()

        if (countToday >= dailyCap) {
            Timber.d("EngagementWorker: daily cap reached for role=%s count=%d", role, countToday)
            return Result.success()
        }

        if (message.title in titlesToday) {
            Timber.d("EngagementWorker: duplicate title today, skipping")
            return Result.success()
        }

        val lastSentAt = prefs.getLong(KEY_LAST_SENT_AT, 0L)
        val minGapMs = 3 * 60 * 60 * 1000L
        if (lastSentAt > 0 && (System.currentTimeMillis() - lastSentAt) < minGapMs) {
            Timber.d("EngagementWorker: last notification too recent, skipping")
            return Result.success()
        }

        // Advanced: right after the app goes to the background, try to surface a job that
        // matches the worker's skills and open it straight on the Job Description screen
        // (deep link dutype://job/{id}) so the tap feels personal and relevant.
        if (tags.contains(WORK_NAME_BACKGROUND) && role == "WORKER" && trySendJobMatchNotification(user.uid)) {
            prefs.edit()
                .putLong(KEY_DAY_START, dayStart)
                .putInt(KEY_COUNT_TODAY, countToday + 1)
                .putString(KEY_TITLES_TODAY, titlesToday.joinToString(TITLE_SEPARATOR))
                .putLong(KEY_LAST_SENT_AT, System.currentTimeMillis())
                .apply()
            Timber.d("EngagementWorker: skill-matched job notification sent (background run)")
            return Result.success()
        }

        val shown = showLocalNotification(message, role)
        if (!shown) return Result.success()

        titlesToday += message.title
        prefs.edit()
            .putLong(KEY_DAY_START, dayStart)
            .putInt(KEY_COUNT_TODAY, countToday + 1)
            .putString(KEY_TITLES_TODAY, titlesToday.joinToString(TITLE_SEPARATOR))
            .putLong(KEY_LAST_SENT_AT, System.currentTimeMillis())
            .apply()

        Timber.d("EngagementWorker: notification shown for role=%s title=%s", role, message.title)
        return Result.success()
    }

    private suspend fun resolveRole(userId: String, phoneNumber: String?): String? {
        return try {
            val normalizedPhone = phoneNumber?.let(PhoneNumberUtils::normalize).orEmpty()
            val phoneRoleDoc = normalizedPhone.takeIf { it.isNotBlank() }
                ?.let { firestore.collection(FirestoreCollections.PHONE_ROLES).document(it).get().await() }
            val resolved = phoneRoleDoc?.getString("role")?.uppercase()
                ?: when {
                    firestore.collection(FirestoreCollections.EMPLOYER_PROFILES).document(userId).get().await().exists() -> "EMPLOYER"
                    firestore.collection(FirestoreCollections.WORKER_PROFILES).document(userId).get().await().exists() -> "WORKER"
                    else -> null
                }

            if (resolved !in setOf("WORKER", "EMPLOYER")) {
                Timber.w("EngagementWorker: no canonical role found, skipping")
                null
            } else {
                resolved
            }
        } catch (e: Exception) {
            Timber.w(e, "EngagementWorker: role lookup failed, skipping notification")
            null
        }
    }

    /**
     * Look up the worker's skills and find a recent open job that matches, then post a
     * notification that deep-links straight to that job's description screen.
     * Falls back to the most recent open job when no explicit skill match is found.
     */
    private suspend fun trySendJobMatchNotification(userId: String): Boolean {
        return try {
            val profileDoc = firestore.collection(FirestoreCollections.WORKER_PROFILES)
                .document(userId).get().await()
            val skills = (profileDoc.get("skills") as? List<*>)
                ?.mapNotNull { it?.toString()?.trim()?.lowercase() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()

            // Single-field order → no composite index required.
            val snapshot = firestore.collection(FirestoreCollections.JOBS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(40)
                .get()
                .await()

            val openJobs = snapshot.documents.filter { doc ->
                val status = doc.getString("status")?.lowercase()
                if (status != null) {
                    status == "open"
                } else {
                    (doc.getBoolean("isActive") ?: true) && (doc.getBoolean("isFilled") != true)
                }
            }
            if (openJobs.isEmpty()) return false

            val matched = if (skills.isEmpty()) {
                null
            } else {
                openJobs.firstOrNull { doc ->
                    val haystack = buildString {
                        append(doc.getString("title") ?: "")
                        append(' ')
                        append(doc.getString("category") ?: "")
                        append(' ')
                        append(doc.getString("jobType") ?: "")
                    }.lowercase()
                    skills.any { skill -> haystack.contains(skill) }
                }
            }

            val chosen = matched ?: openJobs.first()
            val jobId = chosen.id
            val jobTitle = (chosen.getString("title") ?: "").ifBlank { "A new job near you" }
            sendJobMatchNotification(jobId, jobTitle, isSkillMatch = matched != null)
        } catch (e: Exception) {
            Timber.w(e, "EngagementWorker: job-match lookup failed")
            false
        }
    }

    private fun sendJobMatchNotification(jobId: String, jobTitle: String, isSkillMatch: Boolean): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Timber.w("EngagementWorker: POST_NOTIFICATIONS not granted, skipping job match")
                return false
            }
        }

        NotificationChannelManager.createNotificationChannels(context)
        val notificationId = BASE_NOTIFICATION_ID + 500 + (System.currentTimeMillis() % 400).toInt()
        val deepLinkString = "dutype://job/$jobId"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkString)).apply {
            setClass(context, MainActivity::class.java)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("from_notification", true)
            putExtra("login_role", "WORKER")
            putExtra("notification_system_id", notificationId)
            putExtra("notification_deep_link", deepLinkString)
            putExtra("notification_type", "JOB_MATCH")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (isSkillMatch) "A job that matches your skills \uD83C\uDFAF" else "New job near you \uD83D\uDD14"
        val body = "$jobTitle is hiring now. Tap to view the details and apply."

        val notification = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_MEDIUM_PRIORITY)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(NotificationIcons.tintColor(context))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply {
                NotificationIcons.largeIcon(context)?.let { setLargeIcon(it) }
            }
            .addAction(R.drawable.ic_notification, "View Job", pendingIntent)
            .build()

        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(notificationId, notification)
            true
        } catch (e: Exception) {
            Timber.w(e, "EngagementWorker: failed to post job-match notification")
            false
        }
    }

    private fun showLocalNotification(message: EngagementMessage, role: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Timber.w("EngagementWorker: POST_NOTIFICATIONS not granted, skipping")
                return false
            }
        }

        NotificationChannelManager.createNotificationChannels(context)
        val notificationId = BASE_NOTIFICATION_ID + (System.currentTimeMillis() % 1000).toInt()
        // Re-engagement "profile" nudges must not strand a logged-in worker on the
        // profile detail screen — route them to Home (which shows the profile prompt).
        val resolvedDeepLink = if (message.deepLink == "dutype://worker/profile") {
            "dutype://worker/home"
        } else {
            message.deepLink
        }
        val deepLink = Uri.parse(resolvedDeepLink)
        val intent = Intent(Intent.ACTION_VIEW, deepLink).apply {
            setClass(context, MainActivity::class.java)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("engagement", true)
            putExtra("from_notification", true)
            putExtra("login_role", role)
            putExtra("notification_system_id", notificationId)
            putExtra("notification_deep_link", resolvedDeepLink)
            putExtra("notification_type", "RE_ENGAGEMENT")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val actionPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_MEDIUM_PRIORITY)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(NotificationIcons.tintColor(context))
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply {
                NotificationIcons.largeIcon(context)?.let { setLargeIcon(it) }
            }
            .addAction(
                R.drawable.ic_notification,
                if (role == "EMPLOYER") "Open Hiring" else "Explore Jobs",
                actionPendingIntent
            )
            .build()

        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(notificationId, notification)
            true
        } catch (e: Exception) {
            Timber.e(e, "EngagementWorker: failed to post notification")
            false
        }
    }
}
