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
    const val USERS = "users"
    const val WORKER_PROFILES = "worker_profiles"
    const val EMPLOYER_PROFILES = "employer_profiles"

    // ── Jobs (2-collection split) ───────────────────────
    const val JOBS = "jobmetadata"             // Card data for list views
    const val JOB_DETAILS = "job_details"      // Full data loaded on click

    // ── Applications & Saved ────────────────────────────
    const val APPLICATIONS = "applications"
    const val SAVED_JOBS = "saved_jobs"

    // ── Social ──────────────────────────────────────────
    const val RATINGS = "ratings"
    const val JOB_REPORTS = "job_reports"
    const val NOTIFICATIONS = "notifications"

    // ── Referral ────────────────────────────────────────
    const val REFERRALS = "referrals"
    const val REFERRAL_CODES = "referral_codes"
}
