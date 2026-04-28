package com.example.dutype.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import timber.log.Timber

/**
 * Thin wrapper around FirebaseAnalytics so the SDK we already ship actually
 * earns its weight in the APK (cold-start ContentProvider + ~300-600 KB dex).
 *
 * Auto-collected events (`first_open`, `session_start`, `screen_view`) flow
 * automatically once `FirebaseAnalytics.getInstance(...)` is touched. The
 * methods below add the few high-leverage business events (job_apply,
 * job_post, otp_verified, rewarded_ad_completed) that drive the marketplace
 * funnel and FCM audience targeting.
 *
 * Call [init] exactly once from `Application.onCreate` (deferred is fine).
 * After that the helpers below are no-ops if [init] was missed, so they will
 * never crash a release build.
 */
object Analytics {

    @Volatile
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)
            Timber.d("📈 FirebaseAnalytics initialized")
        }
    }

    private fun log(name: String, params: Bundle? = null) {
        val fa = firebaseAnalytics ?: return
        try {
            fa.logEvent(name, params)
        } catch (t: Throwable) {
            // Analytics must never crash the app.
            Timber.w(t, "📈 logEvent(%s) failed", name)
        }
    }

    /** Worker successfully submitted an application to a job. */
    fun jobApply(jobId: String, employerId: String) {
        log(
            "job_apply",
            Bundle().apply {
                putString("job_id", jobId)
                putString("employer_id", employerId)
            }
        )
    }

    /** Employer successfully posted a job. */
    fun jobPost(jobId: String, employerId: String) {
        log(
            "job_post",
            Bundle().apply {
                putString("job_id", jobId)
                putString("employer_id", employerId)
            }
        )
    }

    /** Phone OTP verified and user signed in. Called once per successful sign-in. */
    fun otpVerified(role: String, isNewUser: Boolean) {
        log(
            "otp_verified",
            Bundle().apply {
                putString("role", role)
                putString("is_new_user", isNewUser.toString())
            }
        )
    }

    /** User watched a rewarded ad to completion and earned the reward. */
    fun rewardedAdCompleted(placement: String) {
        log(
            "rewarded_ad_completed",
            Bundle().apply {
                putString("placement", placement)
            }
        )
    }
}
