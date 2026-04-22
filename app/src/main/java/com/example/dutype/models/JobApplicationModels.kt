package com.example.dutype.models

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * JobApplication — strict target schema model.
 *
 * Firestore applications collection:
 *   id (doc ID = jobId_workerId), jobId, workerId,
 *   employerId, status, createdAt
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

    // ── UI-only enrichment (hydrated by FirestoreJobApplicationMapper from
    //    the jobs / users collections during READ). NEVER written to Firestore —
    //    `toFirestoreMap()` is the single write path and enforces the strict
    //    6-field canonical schema.
    val jobTitle: String = "",
    val jobLocation: String = "",
    val companyName: String = "",
    val workerName: String = "",
    val workerPhone: String? = null,
    val workerProfileImageUrl: String? = null,
    // Bug #18 / #19 fix: denormalized at write time so the employer can
    // render the applicant card without reading worker_profiles (locked
    // to the owner). Source of truth stays in users + worker_profiles.
    val workerSkills: List<String> = emptyList(),
    // Quick-call feature: denormalized employer contact phone (taken from
    // job_details.contactNumber at apply time) so the worker can dial the
    // employer directly from the MyJobs card without an extra read.
    val employerPhone: String? = null,
    val coverLetter: String = ""
) {
    /**
     * Canonical write path. Includes the denormalized worker snapshot fields
     * (workerName, workerPhone, workerProfileImageUrl, workerSkills, jobTitle,
     * companyName, jobLocation) when present so the employer can render the
     * applicant card without an extra worker_profiles read. Firestore rules
     * accept these as optional on create.
     */
    fun toFirestoreMap(): Map<String, Any> {
        val base = mutableMapOf<String, Any>(
            "jobId" to jobId,
            "workerId" to workerId,
            "employerId" to employerId,
            "status" to status.toFirestoreValue(),
            "createdAt" to com.google.firebase.Timestamp(createdAt / 1000, ((createdAt % 1000) * 1_000_000).toInt())
        )
        if (jobTitle.isNotBlank()) base["jobTitle"] = jobTitle
        if (companyName.isNotBlank()) base["companyName"] = companyName
        if (jobLocation.isNotBlank()) base["jobLocation"] = jobLocation
        if (workerName.isNotBlank()) base["workerName"] = workerName
        workerPhone?.takeIf { it.isNotBlank() }?.let { base["workerPhone"] = it }
        workerProfileImageUrl?.takeIf { it.isNotBlank() }?.let { base["workerProfileImageUrl"] = it }
        if (workerSkills.isNotEmpty()) base["workerSkills"] = workerSkills.take(20)
        employerPhone?.takeIf { it.isNotBlank() }?.let { base["employerPhone"] = it }
        return base
    }
}

// ─── Supporting types ────────────────────────────────────────────────────────

/**
 * 5-state machine. Bug #15 fix: added COMPLETED so the employer can mark a
 * hired worker's job as finished, which unlocks the worker's earnings entry.
 *   APPLIED     -> worker submitted, awaiting employer review
 *   SHORTLISTED -> employer marked as candidate
 *   REJECTED    -> rejected (employer) or withdrawn (worker) — terminal failure
 *   HIRED       -> worker hired, work underway
 *   COMPLETED   -> employer marked work done — terminal success, earnings unlocked
 */
enum class ApplicationStatus {
    APPLIED,
    SHORTLISTED,
    REJECTED,
    HIRED,
    COMPLETED;

    fun toFirestoreValue(): String = when (this) {
        APPLIED -> "applied"
        SHORTLISTED -> "shortlisted"
        REJECTED -> "rejected"
        HIRED -> "hired"
        COMPLETED -> "completed"
    }

    companion object {
        fun fromFirestoreValue(value: String): ApplicationStatus = when (value.lowercase()) {
            "applied" -> APPLIED
            "shortlisted" -> SHORTLISTED
            "rejected" -> REJECTED
            "hired" -> HIRED
            "completed" -> COMPLETED
            else -> APPLIED
        }
    }
}

fun ApplicationStatus.getDisplayName(): String = when (this) {
    ApplicationStatus.APPLIED -> "Applied"
    ApplicationStatus.SHORTLISTED -> "Shortlisted"
    ApplicationStatus.REJECTED -> "Rejected"
    ApplicationStatus.HIRED -> "Hired"
    ApplicationStatus.COMPLETED -> "Completed"
}

fun ApplicationStatus.getStatusColor(): Color = when (this) {
    ApplicationStatus.APPLIED -> Color(0xFFFFA500)
    ApplicationStatus.SHORTLISTED -> Color(0xFF2196F3)
    ApplicationStatus.HIRED -> Color(0xFF4CAF50)
    ApplicationStatus.COMPLETED -> Color(0xFF1F8B4C)
    ApplicationStatus.REJECTED -> Color(0xFFF44336)
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
    val shortlistedApplications: Int = 0,
    val rejectedApplications: Int = 0,
    val hiredApplications: Int = 0,
    val recentApplications: List<JobApplication> = emptyList()
)
