package com.example.dutype.announcements

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavHostController
import com.example.dutype.navigation.Routes
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

object AnnouncementManager {
    private const val PREFS_NAME = "dutype_in_app_announcements"
    private const val KEY_DISMISSED_PREFIX = "dismissed_"

    private val _activeAnnouncement = MutableStateFlow<InAppAnnouncement?>(null)
    val activeAnnouncement: StateFlow<InAppAnnouncement?> = _activeAnnouncement.asStateFlow()

    private val _showHungrBottomSheet = MutableStateFlow(false)
    val showHungrBottomSheet: StateFlow<Boolean> = _showHungrBottomSheet.asStateFlow()

    fun openHungrSurvey() {
        _showHungrBottomSheet.value = true
    }

    fun closeHungrSurvey() {
        _showHungrBottomSheet.value = false
    }

    fun isAnnouncementDismissed(context: Context, announcementId: String): Boolean {
        if (announcementId.isBlank()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("${KEY_DISMISSED_PREFIX}$announcementId", false)
    }

    fun dismissAnnouncement(context: Context, announcementId: String) {
        if (announcementId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("${KEY_DISMISSED_PREFIX}$announcementId", true).apply()
        if (_activeAnnouncement.value?.id == announcementId) {
            _activeAnnouncement.value = null
        }
    }

    fun refreshAnnouncements(context: Context, userRole: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection("announcements")
                    .whereEqualTo("active", true)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val nowMs = System.currentTimeMillis()
                        val candidates = snapshot.documents.mapNotNull { doc ->
                            val title = doc.getString("title") ?: ""
                            val message = doc.getString("message") ?: ""
                            if (title.isBlank() && message.isBlank()) return@mapNotNull null

                            val expiresAt = doc.getTimestamp("expiresAt")
                            if (expiresAt != null && expiresAt.toDate().time < nowMs) {
                                return@mapNotNull null
                            }

                            val targetRole = doc.getString("targetRole")
                            if (!targetRole.isNullOrBlank() && !userRole.isNullOrBlank()) {
                                if (!targetRole.equals(userRole, ignoreCase = true) && !targetRole.equals("ALL", ignoreCase = true)) {
                                    return@mapNotNull null
                                }
                            }

                            if (isAnnouncementDismissed(context, doc.id)) {
                                return@mapNotNull null
                            }

                            InAppAnnouncement(
                                id = doc.id,
                                title = title,
                                message = message,
                                type = doc.getString("type") ?: "INFO",
                                priority = doc.getString("priority") ?: "NORMAL",
                                targetRole = targetRole,
                                actionRoute = doc.getString("actionRoute") ?: "",
                                active = doc.getBoolean("active") ?: true,
                                createdAt = doc.getTimestamp("createdAt"),
                                expiresAt = expiresAt
                            )
                        }

                        // Pick highest priority or most recent
                        _activeAnnouncement.value = candidates.firstOrNull()
                    }
                    .addOnFailureListener { e ->
                        Timber.w(e, "Failed to load in-app announcements from Firestore")
                    }
            } catch (t: Throwable) {
                Timber.w(t, "Error querying announcements")
            }
        }
    }

    fun handleAction(context: Context, navController: NavHostController, actionRoute: String) {
        val trimmed = actionRoute.trim()
        if (trimmed.isBlank()) return

        try {
            when {
                trimmed.equals("dynamic_campaign", ignoreCase = true) ||
                trimmed.equals("dutype://dynamic_campaign", ignoreCase = true) ||
                trimmed.equals("campaign", ignoreCase = true) ||
                trimmed.equals("dutype://campaign", ignoreCase = true) ||
                trimmed.startsWith("showcase:", ignoreCase = true) ||
                (trimmed.contains("hungr", ignoreCase = true) && !trimmed.contains("survey", ignoreCase = true)) -> {
                    val scId = if (trimmed.startsWith("showcase:", ignoreCase = true)) {
                        trimmed.substringAfter("showcase:").trim()
                    } else {
                        "active_campaign"
                    }
                    navController.navigate(Routes.promoShowcaseRoute(scId.ifBlank { "active_campaign" }))
                }

                trimmed.equals("dutype://hungr-survey", ignoreCase = true) ||
                trimmed.equals("hungr-survey", ignoreCase = true) -> {
                    openHungrSurvey()
                }

                trimmed.startsWith("http://") || trimmed.startsWith("https://") -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trimmed))
                    context.startActivity(intent)
                }

                trimmed.startsWith("dutype://") -> {
                    val path = trimmed.removePrefix("dutype://").lowercase()
                    when (path) {
                        "services", "service" -> navController.navigate(Routes.SERVICES)
                        "jobs", "browse_jobs" -> navController.navigate(Routes.WORKER_ALL_JOBS)
                        "post-job", "post_job" -> navController.navigate(Routes.EMPLOYER_POST_JOB)
                        "refer", "referral" -> navController.navigate(Routes.WORKER_REFER_EARN)
                        "partner-apply", "partner" -> navController.navigate(Routes.PARTNER)
                        "profile" -> navController.navigate(Routes.WORKER_PROFILE_DETAILS)
                        else -> {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trimmed))
                            context.startActivity(intent)
                        }
                    }
                }

                else -> {
                    // Direct route name
                    val resolved = when (trimmed.lowercase()) {
                        "services" -> Routes.SERVICES
                        "jobs" -> Routes.WORKER_ALL_JOBS
                        "post-job", "post_job" -> Routes.EMPLOYER_POST_JOB
                        "refer" -> Routes.WORKER_REFER_EARN
                        "hungr" -> {
                            openHungrSurvey()
                            return
                        }
                        else -> trimmed
                    }
                    navController.navigate(resolved)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to navigate to announcement target: $trimmed")
        }
    }
}
