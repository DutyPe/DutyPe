package com.example.dutype.models

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * JobApplication — strict target schema model.
 *
 * Firestore applications collection:
 *   id (doc ID = jobId_workerId), jobId, workerId,
 *   employerId, status, createdAt, optional workerName
 */
@Keep
@Immutable
@com.google.firebase.firestore.IgnoreExtraProperties
data class JobApplication(
    val id: String = "",
    val jobId: String = "",
    val workerId: String = "",
    val employerId: String = "",
    val status: ApplicationStatus = ApplicationStatus.APPLIED,
    val createdAt: Long = System.currentTimeMillis(),

    // ── UI-only enrichment (hydrated by profile/application reads). The
    //    Firestore application document stays minimal; worker profile details
    //    live in worker_profiles and are loaded by workerId.
    val jobTitle: String = "",
    val jobLocation: String = "",
    val companyName: String = "",
    val workerName: String = "",
    val workerPhone: String? = null,
    // Bug #7 fix: surface the worker's contact email on the employer
    // application card / detail screen. Denormalized at apply time from
    // worker_profiles.email so we don't need an extra read (rules block
    // employers from reading worker_profiles directly).
    val workerEmail: String? = null,
    val workerProfileImageUrl: String? = null,
    // Bug #18 / #19 fix: denormalized at write time so the employer can
    // render the applicant card without reading worker_profiles (locked
    // to the owner). Source of truth stays in worker_profiles.
    val workerSkills: List<String> = emptyList(),
    val workerGender: String = "",
    val workerExperience: String = "",
    val workerEducationQualification: String = "",
    val workerDateOfBirth: String = "",
    val workerBio: String = "",
    // Quick-call feature: denormalized employer contact phone (taken from
    // job_details.contactNumber at apply time) so the worker can dial the
    // employer directly from the MyJobs card without an extra read.
    val employerPhone: String? = null,
    val jobStatus: String = "open",
    val coverLetter: String = ""
) {
    /**
     * Canonical write path. Keeps the application document strict and only
     * includes workerName as the allowed denormalized display field.
     */
    fun toFirestoreMap(): Map<String, Any> {
        return buildMap {
            put("jobId", jobId)
            put("workerId", workerId)
            put("employerId", employerId)
            put("status", status.toFirestoreValue())
            put("createdAt", com.google.firebase.Timestamp(createdAt / 1000, ((createdAt % 1000) * 1_000_000).toInt()))
            workerName.trim().takeIf { it.isNotBlank() }?.let { put("workerName", it) }
        }
    }
}

// ─── Supporting types ────────────────────────────────────────────────────────

/**
 * Simplified state machine for hyper-local hiring:
 *   APPLIED     -> worker submitted, awaiting employer review
 *   REJECTED    -> rejected (employer) or withdrawn (worker)
 *   WITHDRAWN   -> worker withdrew application
 *   HIRED       -> worker hired, work underway
 *   COMPLETED   -> employer marked work done
 */
enum class ApplicationStatus {
    APPLIED,
    REJECTED,
    WITHDRAWN,
    HIRED,
    COMPLETED;

    fun toFirestoreValue(): String = when (this) {
        APPLIED -> "applied"
        REJECTED -> "rejected"
        WITHDRAWN -> "withdrawn"
        HIRED -> "hired"
        COMPLETED -> "completed"
    }

    companion object {
        fun fromFirestoreValue(value: String): ApplicationStatus = when (value.lowercase().trim()) {
            "applied", "pending", "viewed", "seen", "under_review", "shortlisted" -> APPLIED // Legacy mapping back to APPLIED
            "accepted", "hired", "in_progress" -> HIRED
            "completed" -> COMPLETED
            "rejected" -> REJECTED
            "withdrawn" -> WITHDRAWN
            else -> APPLIED
        }
    }
}

fun ApplicationStatus.getDisplayName(): String = when (this) {
    ApplicationStatus.APPLIED -> "Applied"
    ApplicationStatus.WITHDRAWN -> "Withdrawn"
    ApplicationStatus.HIRED -> "Hired"
    ApplicationStatus.COMPLETED -> "Completed"
    ApplicationStatus.REJECTED -> "Rejected"
}

fun ApplicationStatus.getStatusColor(): Color = when (this) {
    ApplicationStatus.APPLIED -> Color(0xFFFFA500)
    ApplicationStatus.HIRED -> Color(0xFF4CAF50)
    ApplicationStatus.COMPLETED -> Color(0xFF1F8B4C)
    ApplicationStatus.REJECTED -> Color(0xFFF44336)
    ApplicationStatus.WITHDRAWN -> Color(0xFF6B7280)
}

enum class JobVacancyStatus { OPEN, FILLED, CLOSED, EXPIRED }

fun JobVacancyStatus.getDisplayName(): String = when (this) {
    JobVacancyStatus.OPEN -> "Open"
    JobVacancyStatus.FILLED -> "Filled"
    JobVacancyStatus.CLOSED -> "Closed"
    JobVacancyStatus.EXPIRED -> "Expired"
}

data class JobApplicationUiState(
    val applications: List<JobApplication> = emptyList(),
    val isLoading: Boolean = false
)

@Keep
@com.google.firebase.firestore.IgnoreExtraProperties
data class ApplicationStats(
    val totalApplications: Int = 0,
    val appliedApplications: Int = 0,
    val rejectedApplications: Int = 0,
    val hiredApplications: Int = 0,
    val recentApplications: List<JobApplication> = emptyList()
)
