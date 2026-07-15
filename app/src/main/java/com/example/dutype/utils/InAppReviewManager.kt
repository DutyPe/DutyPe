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
import kotlinx.coroutines.flow.first
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

        private const val MIN_DAYS_BETWEEN_REQUESTS = 30
        private const val MAX_DISMISS_COUNT = 5

        const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.dutype.app"
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
        val dismissCount = prefs[KEY_REVIEW_DISMISSED_COUNT] ?: 0
        if (dismissCount >= MAX_DISMISS_COUNT) return false

        val lastRequest = prefs[KEY_LAST_REVIEW_REQUEST] ?: 0
        val daysSinceLastRequest = (System.currentTimeMillis() - lastRequest) / (1000 * 60 * 60 * 24)
        return daysSinceLastRequest >= MIN_DAYS_BETWEEN_REQUESTS
    }

    suspend fun requestInAppReview(activity: Activity) {
        if (!shouldShowReviewPrompt()) {
            Timber.d("Review prompt skipped")
            return
        }

        try {
            val manager = com.google.android.play.core.review.ReviewManagerFactory.create(context)
            val requestInfoTask = manager.requestReviewFlow()
            requestInfoTask.addOnCompleteListener { request ->
                if (request.isSuccessful) {
                    val reviewInfo = request.result
                    val flow = manager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener { _ ->
                        Timber.d("In-app review flow completed")
                    }
                } else {
                    Timber.w(request.exception, "Failed to request review flow")
                }
            }
            context.reviewDataStore.edit { prefs ->
                prefs[KEY_LAST_REVIEW_REQUEST] = System.currentTimeMillis()
                prefs[KEY_HAS_RATED] = true
            }
        } catch (e: Exception) {
            Timber.e(e, "Error launching in-app review")
        }
    }

    fun openPlayStore(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.dutype.app"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e2: Exception) {
                Timber.e(e2, "Failed to open Play Store")
            }
        }
    }

    suspend fun trackDismissal() {
        context.reviewDataStore.edit { prefs ->
            val current = prefs[KEY_REVIEW_DISMISSED_COUNT] ?: 0
            prefs[KEY_REVIEW_DISMISSED_COUNT] = current + 1
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
