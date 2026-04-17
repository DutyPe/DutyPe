package com.example.dutype.models

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color

/**
 * JobApplication — strict target schema model.
 *
 * Firestore applications collection:
 *   applicationId (doc ID = jobId_workerId), jobId, workerId,
 *   employerId, status, createdAt
 */
@Keep
@com.google.firebase.firestore.IgnoreExtraProperties
data class JobApplication(
    val applicationId: String = "",
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
    val coverLetter: String = "",
    val statusHistory: List<StatusHistoryEntry> = emptyList()
) {
    val canonicalId: String get() = if (applicationId.isNotBlank()) applicationId else id
    val appliedAt: Long get() = createdAt
    val active: Boolean get() = status != ApplicationStatus.REJECTED

    /** ONLY the fields that belong in Firestore. Use for all writes. */
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "jobId" to jobId,
        "workerId" to workerId,
        "employerId" to employerId,
        "status" to status.toFirestoreValue(),
        "createdAt" to com.google.firebase.Timestamp(createdAt / 1000, ((createdAt % 1000) * 1_000_000).toInt())
    )
}

// ─── Supporting types ────────────────────────────────────────────────────────

@Keep
@com.google.firebase.firestore.IgnoreExtraProperties
data class DocumentAttachment(
    val id: String = "",
    val name: String = "",
    val url: String = "",
    val type: String = "",
    val uploadedAt: Long = System.currentTimeMillis()
) {
    // Aliases used in UI
    val fileName: String get() = name
    val fileUrl: String get() = url
    val fileType: DocumentFileType get() = DocumentFileType.fromString(type)
    val fileSize: Long get() = 0L  // not stored — display placeholder
}

enum class DocumentFileType {
    PDF, IMAGE, DOC, OTHER;

    companion object {
        fun fromString(type: String): DocumentFileType = when (type.uppercase()) {
            "PDF" -> PDF
            "IMAGE", "JPG", "JPEG", "PNG" -> IMAGE
            "DOC", "DOCX" -> DOC
            else -> OTHER
        }
    }
}

/**
 * Canonical 4-state machine. Matches [firestore.rules] exactly; no UI-only aliases.
 *   APPLIED     -> worker submitted, awaiting employer review
 *   SHORTLISTED -> employer marked as candidate
 *   REJECTED    -> rejected (by employer) or withdrawn (by worker) — terminal failure
 *   HIRED       -> worker hired — terminal success
 */
enum class ApplicationStatus {
    APPLIED,
    SHORTLISTED,
    REJECTED,
    HIRED;

    fun toFirestoreValue(): String = when (this) {
        APPLIED -> "applied"
        SHORTLISTED -> "shortlisted"
        REJECTED -> "rejected"
        HIRED -> "hired"
    }

    companion object {
        fun fromFirestoreValue(value: String): ApplicationStatus = when (value.lowercase()) {
            "applied" -> APPLIED
            "shortlisted" -> SHORTLISTED
            "rejected" -> REJECTED
            "hired" -> HIRED
            else -> APPLIED
        }
    }
}

fun ApplicationStatus.getDisplayName(): String = when (this) {
    ApplicationStatus.APPLIED -> "Applied"
    ApplicationStatus.SHORTLISTED -> "Shortlisted"
    ApplicationStatus.REJECTED -> "Rejected"
    ApplicationStatus.HIRED -> "Hired"
}

fun ApplicationStatus.getStatusColor(): Color = when (this) {
    ApplicationStatus.APPLIED -> Color(0xFFFFA500)
    ApplicationStatus.SHORTLISTED -> Color(0xFF2196F3)
    ApplicationStatus.HIRED -> Color(0xFF4CAF50)
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
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val hasError: Boolean = false,
    val error: String = "",
    val submissionSuccess: Boolean = false
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

@Keep
@com.google.firebase.firestore.IgnoreExtraProperties
data class ApplicationAnalytics(
    val totalApplications: Int = 0,
    val applicationsThisWeek: Int = 0,
    val applicationsThisMonth: Int = 0,
    val appliedApplications: Int = 0,
    val shortlistedApplications: Int = 0,
    val hiredApplications: Int = 0,
    val rejectedApplications: Int = 0,
    val averageResponseTime: Long = 0L,
    val topJobTitles: List<String> = emptyList(),
    val applicationTrends: Map<String, Int> = emptyMap()
)
