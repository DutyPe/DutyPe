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
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),

    // Runtime fields used by UI/viewmodels. Firestore writes are still strict via toFirestoreMap().
    val jobTitle: String = "",
    val jobLocation: String = "",
    val companyName: String = "",
    val workerName: String = "",
    val workerEmail: String? = null,
    val workerPhone: String? = null,
    val workerLocation: String? = null,
    val workerProfileImageUrl: String? = null,
    val workerLocalRating: Float? = null,
    val workerTotalReviews: Int? = null,
    val workerJobsInArea: Int? = null,
    val workerAadhaarVerified: Boolean? = null,
    val workerPhoneVerified: Boolean? = null,
    val workerIdentityVerified: Boolean? = null,
    val workerBackgroundCheckPassed: Boolean? = null,
    val workerDateOfBirth: String? = null,
    val workerGender: String? = null,
    val skills: List<String> = emptyList(),
    val skillsText: String? = null,
    val workExperience: List<WorkExperience> = emptyList(),
    val workExperienceText: String? = null,
    val education: List<Education> = emptyList(),
    val certifications: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val availability: String? = null,
    val expectedSalary: String? = null,
    val resumeUrl: String? = null,
    val coverLetter: String = "",
    val statusHistory: List<StatusHistoryEntry> = emptyList(),
    val additionalDocuments: List<DocumentAttachment> = emptyList()
) {
    val canonicalId: String get() = if (applicationId.isNotBlank()) applicationId else id
    val appliedAt: Long get() = createdAt
    val active: Boolean
        get() = status != ApplicationStatus.WITHDRAWN && status != ApplicationStatus.REJECTED

    /**
     * Returns ONLY the fields that belong in Firestore.
     * Use this for all writes — never pass the full object to set().
     */
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

enum class ApplicationStatus {
    PENDING,        // "applied" in Firestore
    UNDER_REVIEW,   // "under_review" in Firestore
    REJECTED,
    ACCEPTED,       // "accepted" in Firestore
    COMPLETED,
    WITHDRAWN;

    fun toFirestoreValue(): String = when (this) {
        PENDING -> "applied"
        UNDER_REVIEW -> "under_review"
        REJECTED -> "rejected"
        ACCEPTED -> "accepted"
        COMPLETED -> "completed"
        WITHDRAWN -> "withdrawn"
    }

    companion object {
        fun fromFirestoreValue(value: String): ApplicationStatus = when (value.lowercase()) {
            "applied" -> PENDING
            "under_review" -> UNDER_REVIEW
            "rejected" -> REJECTED
            "accepted" -> ACCEPTED
            "completed" -> COMPLETED
            "withdrawn" -> WITHDRAWN
            else -> PENDING
        }
    }
}

fun ApplicationStatus.getDisplayName(): String = when (this) {
    ApplicationStatus.PENDING -> "Pending"
    ApplicationStatus.UNDER_REVIEW -> "Under Review"
    ApplicationStatus.REJECTED -> "Rejected"
    ApplicationStatus.ACCEPTED -> "Accepted"
    ApplicationStatus.COMPLETED -> "Completed"
    ApplicationStatus.WITHDRAWN -> "Withdrawn"
}

fun ApplicationStatus.getStatusColor(): Color = when (this) {
    ApplicationStatus.PENDING -> Color(0xFFFFA500)
    ApplicationStatus.UNDER_REVIEW -> Color(0xFF2196F3)
    ApplicationStatus.ACCEPTED -> Color(0xFF4CAF50)
    ApplicationStatus.REJECTED -> Color(0xFFF44336)
    ApplicationStatus.COMPLETED -> Color(0xFF9C27B0)
    ApplicationStatus.WITHDRAWN -> Color(0xFF757575)
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
    val pendingApplications: Int = 0,
    val reviewedApplications: Int = 0,
    val shortlistedApplications: Int = 0,
    val rejectedApplications: Int = 0,
    val hiredApplications: Int = 0,
    val acceptedApplications: Int = 0,
    val completedApplications: Int = 0,
    val withdrawnApplications: Int = 0,
    val recentApplications: List<JobApplication> = emptyList()
)

@Keep
@com.google.firebase.firestore.IgnoreExtraProperties
data class ApplicationAnalytics(
    val totalApplications: Int = 0,
    val applicationsThisWeek: Int = 0,
    val applicationsThisMonth: Int = 0,
    val newApplications: Int = 0,
    val underReviewApplications: Int = 0,
    val acceptedApplications: Int = 0,
    val rejectedApplications: Int = 0,
    val averageResponseTime: Long = 0L,
    val topJobTitles: List<String> = emptyList(),
    val applicationTrends: Map<String, Int> = emptyMap()
)
