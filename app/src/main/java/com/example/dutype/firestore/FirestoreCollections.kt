package com.example.dutype.firestore

/**
 * SINGLE SOURCE OF TRUTH for all Firestore collection names.
 *
 * Architecture (2-collection job split for 1M user scale):
 *   jobmetadata   → ultra-light card data (~250 bytes) for list scrolling
 *   job_details   → full job data (~1KB) loaded on click only
 *
 * All service files MUST use these constants instead of hardcoded strings.
 */
object FirestoreCollections {
    // ── Core ────────────────────────────────────────────
    const val PHONE_ROLES = "phoneRoles"
    const val WORKER_PROFILES = "worker_profiles"
    const val EMPLOYER_PROFILES = "employer_profiles"
    const val USER_TOKENS = "user_tokens"

    // ── Jobs (2-collection split) ───────────────────────
    const val JOBS = "jobmetadata"             // Card data for list views
    const val JOB_DETAILS = "job_details"      // Full data loaded on click

    // ── Applications & Saved ────────────────────────────
    const val APPLICATIONS = "applications"
    const val SAVED_JOBS = "saved_jobs"
    const val WORKER_JOB_REQUESTS = "worker_job_requests"

    // ── Social ──────────────────────────────────────────
    const val RATINGS = "ratings"
    const val JOB_REPORTS = "job_reports"
    const val JOB_CALL_FEEDBACK = "job_call_feedback"
    const val NOTIFICATIONS = "notifications"

    // ── Referral ────────────────────────────────────────
    const val REFERRALS = "referrals"
    const val REFERRAL_CODES = "referral_codes"
    const val REFERRAL_STATS = "referral_stats"
    /** Subcollection: `referral_stats/{uid}/withdrawals/{wId}`. CF-only writes. */
    const val WITHDRAWALS = "withdrawals"

    // ── Config & Ops ────────────────────────────────────
    /** Public admin-editable config (referral rewards, feature flags). */
    const val APP_CONFIG = "app_config"
    /** Worker/employer announcements. Pull-based; do not open snapshot listeners. */
    const val ANNOUNCEMENTS = "announcements"
}
