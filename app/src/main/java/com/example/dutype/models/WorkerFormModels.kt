package com.example.dutype.models

import androidx.annotation.Keep

/**
 * Local/runtime-only models for worker profile enrichment.
 * These are NEVER written to Firestore — they are enriched at runtime
 * from worker_profiles, users, and other collections.
 */

@Keep
data class WorkExperience(
    val company: String = "",
    val position: String = "",
    val startDate: String = "",
    val endDate: String? = null,
    val description: String = "",
    val location: String? = null,
    val isCurrent: Boolean = false,
    val achievements: List<String> = emptyList()
)

@Keep
data class Education(
    val institution: String = "",
    val degree: String = "",
    val fieldOfStudy: String? = null,
    val startDate: String = "",
    val endDate: String? = null,
    val gpa: String? = null
)

@Keep
data class StatusHistoryEntry(
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "",
    val notes: String? = null,
    val systemUpdate: Boolean = false
) {
    // Legacy alias
    val timestamp: Long get() = updatedAt
}
