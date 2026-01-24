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
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-App Review Manager
 * 
 * Implements Google's best practices for requesting app reviews:
 * - Uses Google Play In-App Review API (native dialog)
 * - Smart timing based on user actions
 * - Respects user preferences (don't spam)
 * - Graceful fallback to Play Store
 * 
 * Following patterns from top apps: Swiggy, Zomato, PhonePe, Paytm
 */

private val Context.reviewDataStore: DataStore<Preferences> by preferencesDataStore(name = "in_app_review")

@Singleton
class InAppReviewManager @Inject constructor(
    private val context: Context
) {
    private val reviewManager = ReviewManagerFactory.create(context)
    
    companion object {
        private val KEY_LAST_REVIEW_REQUEST = longPreferencesKey("last_review_request_time")
        private val KEY_HAS_RATED = booleanPreferencesKey("has_rated_app")
        private val KEY_REVIEW_DISMISSED_COUNT = longPreferencesKey("review_dismissed_count")
        private val KEY_POSITIVE_ACTIONS_COUNT = longPreferencesKey("positive_actions_count")
        
        // Timing constants (following industry best practices)
        private const val MIN_DAYS_BETWEEN_REQUESTS = 7 // Don't ask more than once per week
        private const val POSITIVE_ACTIONS_THRESHOLD = 3 // Ask after 3 positive actions
        private const val MAX_DISMISS_COUNT = 2 // Stop asking after 2 dismissals
        
        const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.dutype.app"
    }
    
    /**
     * Track positive user actions (job applied, profile completed, etc.)
     */
    suspend fun trackPositiveAction() {
        context.reviewDataStore.edit { prefs ->
            val current = prefs[KEY_POSITIVE_ACTIONS_COUNT] ?: 0
            prefs[KEY_POSITIVE_ACTIONS_COUNT] = current + 1
        }
        Timber.d("📊 Positive action tracked")
    }
    
    /**
     * Check if we should show the review prompt
     */
    suspend fun shouldShowReviewPrompt(): Boolean {
        val prefs = context.reviewDataStore.data.first()
        
        // Don't ask if user already rated
        if (prefs[KEY_HAS_RATED] == true) {
            Timber.d("⭐ User already rated, skipping")
            return false
        }
        
        // Don't ask if dismissed too many times
        val dismissCount = prefs[KEY_REVIEW_DISMISSED_COUNT] ?: 0
        if (dismissCount >= MAX_DISMISS_COUNT) {
            Timber.d("🚫 Max dismiss count reached, skipping")
            return false
        }
        
        // Check time since last request
        val lastRequest = prefs[KEY_LAST_REVIEW_REQUEST] ?: 0
        val daysSinceLastRequest = (System.currentTimeMillis() - lastRequest) / (1000 * 60 * 60 * 24)
        if (daysSinceLastRequest < MIN_DAYS_BETWEEN_REQUESTS) {
            Timber.d("⏰ Too soon since last request ($daysSinceLastRequest days), skipping")
            return false
        }
        
        // Check positive actions threshold
        val positiveActions = prefs[KEY_POSITIVE_ACTIONS_COUNT] ?: 0
        if (positiveActions < POSITIVE_ACTIONS_THRESHOLD) {
            Timber.d("📈 Not enough positive actions ($positiveActions/$POSITIVE_ACTIONS_THRESHOLD), skipping")
            return false
        }
        
        Timber.d("✅ All conditions met, should show review prompt")
        return true
    }
    
    /**
     * Request in-app review (Google Play native dialog)
     * This is the recommended approach by Google
     */
    suspend fun requestInAppReview(activity: Activity) {
        if (!shouldShowReviewPrompt()) {
            return
        }
        
        try {
            // Update last request time
            context.reviewDataStore.edit { prefs ->
                prefs[KEY_LAST_REVIEW_REQUEST] = System.currentTimeMillis()
            }
            
            // Request review info
            val request = reviewManager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    // Launch the in-app review flow
                    val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener {
                        // Review flow finished (user may or may not have rated)
                        Timber.i("⭐ In-app review flow completed")
                        markAsRated()
                    }
                } else {
                    // Failed to get review info, fallback to Play Store
                    Timber.e("❌ Failed to request review flow: ${task.exception?.message}")
                    openPlayStore(activity)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error requesting in-app review")
            openPlayStore(activity)
        }
    }
    
    /**
     * Open Play Store directly (fallback or explicit request)
     */
    fun openPlayStore(context: Context) {
        try {
            // Try to open in Play Store app
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.dutype.app"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Timber.i("🏪 Opened Play Store app")
        } catch (e: Exception) {
            // Fallback to browser
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Timber.i("🌐 Opened Play Store in browser")
            } catch (e2: Exception) {
                Timber.e(e2, "❌ Failed to open Play Store")
            }
        }
    }
    
    /**
     * Mark that user has rated (or completed the flow)
     */
    private fun markAsRated() {
        CoroutineScope(Dispatchers.IO).launch {
            context.reviewDataStore.edit { prefs ->
                prefs[KEY_HAS_RATED] = true
            }
            Timber.i("✅ Marked as rated")
        }
    }
    
    /**
     * Track when user dismisses the prompt
     */
    suspend fun trackDismissal() {
        context.reviewDataStore.edit { prefs ->
            val current = prefs[KEY_REVIEW_DISMISSED_COUNT] ?: 0
            prefs[KEY_REVIEW_DISMISSED_COUNT] = current + 1
        }
        Timber.d("👎 Review dismissed")
    }
    
    /**
     * Reset all review data (for testing)
     */
    suspend fun resetReviewData() {
        context.reviewDataStore.edit { prefs ->
            prefs.clear()
        }
        Timber.d("🔄 Review data reset")
    }
    
    /**
     * Get current review stats (for debugging)
     */
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
