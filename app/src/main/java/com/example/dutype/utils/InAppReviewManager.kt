package com.example.dutype.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reviewDataStore: DataStore<Preferences> by preferencesDataStore(name = "in_app_review")

@Singleton
class InAppReviewManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private val KEY_LAST_REVIEW_REQUEST = longPreferencesKey("last_review_request_time")
        private val KEY_HAS_RATED = booleanPreferencesKey("has_rated_app")
        private val KEY_REVIEW_DISMISSED_COUNT = longPreferencesKey("review_dismissed_count")
        private val KEY_POSITIVE_ACTIONS_COUNT = longPreferencesKey("positive_actions_count")

        private const val MIN_HOURS_BETWEEN_REQUESTS = 24L
        private const val MAX_DISMISS_COUNT = 10

        const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.dutype.app"
    }

    private val _showRatingPromptFlow = MutableStateFlow(false)
    val showRatingPromptFlow: StateFlow<Boolean> = _showRatingPromptFlow.asStateFlow()

    /**
     * Marks that a native Google Play review sheet should be shown. Eligibility (has rated,
     * dismiss cap, 24h cooldown) is preserved; `force` bypasses it as before. MainActivity
     * observes [showRatingPromptFlow] and launches the Play In-App Review flow.
     */
    fun triggerRatingPrompt(activity: Activity? = null, force: Boolean = false) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (force || shouldShowReviewPrompt()) {
                    if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                        launchNativeReview(activity)
                    } else {
                        _showRatingPromptFlow.value = true
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error evaluating review eligibility")
            }
        }
    }

    /**
     * Requests and launches the official Google Play In-App Review flow. Always clears the
     * pending flag, records the request time (cooldown), and never throws.
     */
    suspend fun launchNativeReview(activity: Activity) {
        _showRatingPromptFlow.value = false
        try {
            if (activity.isFinishing || activity.isDestroyed) {
                Timber.d("Review skipped: activity finishing")
                return
            }
            context.reviewDataStore.edit { prefs ->
                prefs[KEY_LAST_REVIEW_REQUEST] = System.currentTimeMillis()
            }
            val manager = com.google.android.play.core.review.ReviewManagerFactory.create(activity)
            manager.requestReviewFlow().addOnCompleteListener { request ->
                try {
                    if (request.isSuccessful && !activity.isFinishing && !activity.isDestroyed) {
                        manager.launchReviewFlow(activity, request.result).addOnCompleteListener {
                            Timber.d("In-app review flow completed")
                        }
                    } else {
                        Timber.w(request.exception, "Review flow not launched")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error launching review flow")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error requesting in-app review")
        }
    }

    fun dismissRatingPrompt() {
        _showRatingPromptFlow.value = false
        CoroutineScope(Dispatchers.IO).launch {
            trackDismissal()
        }
    }

    fun rateOnPlayStore(context: Context) {
        _showRatingPromptFlow.value = false
        CoroutineScope(Dispatchers.IO).launch {
            context.reviewDataStore.edit { prefs ->
                prefs[KEY_HAS_RATED] = true
                prefs[KEY_LAST_REVIEW_REQUEST] = System.currentTimeMillis()
            }
        }
        openPlayStore(context)
    }

    suspend fun trackPositiveAction() {
        context.reviewDataStore.edit { prefs ->
            val current = prefs[KEY_POSITIVE_ACTIONS_COUNT] ?: 0
            prefs[KEY_POSITIVE_ACTIONS_COUNT] = current + 1
        }
        Timber.d("Review positive action tracked")
    }

    suspend fun shouldShowReviewPrompt(): Boolean {
        val prefs = context.reviewDataStore.data.first()
        val hasRated = prefs[KEY_HAS_RATED] ?: false
        if (hasRated) return false

        val dismissCount = prefs[KEY_REVIEW_DISMISSED_COUNT] ?: 0
        if (dismissCount >= MAX_DISMISS_COUNT) return false

        val lastRequest = prefs[KEY_LAST_REVIEW_REQUEST] ?: 0L
        if (lastRequest == 0L) {
            return true
        }
        val hoursSinceLastRequest = (System.currentTimeMillis() - lastRequest) / (1000 * 60 * 60)
        return hoursSinceLastRequest >= MIN_HOURS_BETWEEN_REQUESTS
    }

    suspend fun requestInAppReview(activity: Activity, force: Boolean = false) {
        if (!force && !shouldShowReviewPrompt()) {
            Timber.d("Review prompt skipped")
            return
        }
        launchNativeReview(activity)
    }

    fun openPlayStore(context: Context) {
        val packageName = context.packageName.ifBlank { "com.dutype.app" }
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                setPackage("com.android.vending")
            }
            context.startActivity(marketIntent)
        } catch (_: Exception) {
            try {
                val genericMarketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericMarketIntent)
            } catch (_: Exception) {
                try {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(webIntent)
                } catch (e3: Exception) {
                    Timber.e(e3, "Failed to open Play Store")
                    android.widget.Toast.makeText(context, "Could not open Google Play Store", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    suspend fun trackDismissal() {
        context.reviewDataStore.edit { prefs ->
            val current = prefs[KEY_REVIEW_DISMISSED_COUNT] ?: 0
            prefs[KEY_REVIEW_DISMISSED_COUNT] = current + 1
            prefs[KEY_LAST_REVIEW_REQUEST] = System.currentTimeMillis()
        }
        Timber.d("Review dismissed")
    }

    suspend fun resetReviewData() {
        context.reviewDataStore.edit { prefs -> prefs.clear() }
        Timber.d("Review data reset")
    }

    suspend fun getReviewStats(): ReviewStats {
        val prefs = context.reviewDataStore.data.first()
        return ReviewStats(
            hasRated = prefs[KEY_HAS_RATED] ?: false,
            positiveActions = prefs[KEY_POSITIVE_ACTIONS_COUNT] ?: 0,
            dismissCount = prefs[KEY_REVIEW_DISMISSED_COUNT] ?: 0,
            lastRequestTime = prefs[KEY_LAST_REVIEW_REQUEST] ?: 0
        )
    }
}

data class ReviewStats(
    val hasRated: Boolean,
    val positiveActions: Long,
    val dismissCount: Long,
    val lastRequestTime: Long
)
