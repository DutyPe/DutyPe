package com.example.dutype.analytics

import android.content.Context
import android.os.Bundle
import timber.log.Timber

/**
 * Lightweight analytics facade.
 *
 * Firebase Analytics was removed from the Android binary to keep the Play
 * update dex chunk below the 7 MB threshold. Server-side/referral analytics
 * remain intact; these app-side hooks are intentionally no-op logs.
 */
object Analytics {
    fun init(context: Context) {
        Timber.d("Analytics facade initialized for %s", context.packageName)
    }

    private fun log(name: String, params: Bundle? = null) {
        Timber.d("Analytics event=%s params=%s", name, params?.keySet()?.joinToString())
    }

    fun jobApply(jobId: String, employerId: String) {
        log(
            "job_apply",
            Bundle().apply {
                putString("job_id", jobId)
                putString("employer_id", employerId)
            }
        )
    }

    fun jobPost(jobId: String, employerId: String) {
        log(
            "job_post",
            Bundle().apply {
                putString("job_id", jobId)
                putString("employer_id", employerId)
            }
        )
    }

    fun otpVerified(role: String, isNewUser: Boolean) {
        log(
            "otp_verified",
            Bundle().apply {
                putString("role", role)
                putString("is_new_user", isNewUser.toString())
            }
        )
    }

    fun rewardedAdCompleted(placement: String) {
        log(
            "rewarded_ad_completed",
            Bundle().apply {
                putString("placement", placement)
            }
        )
    }
}
