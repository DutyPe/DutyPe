package com.example.dutype.utils

import timber.log.Timber

/**
 * Lightweight, field-measurable timing marks for the login funnel.
 *
 * Filter logcat with tag "AuthPerf". Events (in order): phone_check_start/end,
 * otp_send_start/end, otp_verify_start/end, login_resolve_start/end, navigate.
 *
 * Release builds plant no Timber tree (only debuggable builds do, see DutyPeApplication /
 * MainActivity), so these calls are effectively no-ops in production; the only cost is a
 * clock read and a string template. Use [now] for the start mark and pass it back to [log]
 * to print the elapsed milliseconds.
 */
object AuthPerf {
    const val TAG = "AuthPerf"

    fun now(): Long = System.currentTimeMillis()

    /** Logs [event]; when [startedAtMs] (from [now]) is given, appends the elapsed time. */
    fun log(event: String, startedAtMs: Long? = null, detail: String? = null) {
        if (Timber.treeCount == 0) return
        val elapsed = startedAtMs?.let { " elapsedMs=${now() - it}" }.orEmpty()
        val extra = detail?.let { " $it" }.orEmpty()
        Timber.tag(TAG).i("%s%s%s", event, elapsed, extra)
    }
}
