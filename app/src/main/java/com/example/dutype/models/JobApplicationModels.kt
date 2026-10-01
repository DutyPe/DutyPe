package com.example.dutype.models

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.dutype.firestore.FirestoreSchema.Applications
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.utils.epochMillis

/**
 * `applications/{jobId_workerId}` — exactly the schema fields ([Applications]) plus two runtime
 * joins that are never stored: [job] (the live job card, for the worker's lists) and
 * [worker] (the worker's public card, for the employer's applicant view).
 */
@Keep
@Immutable
data class JobApplication(
    val id: String = "",
    val jobId: String = "",
    val workerId: String = "",
    val employerId: String = "",
    val status: ApplicationStatus = ApplicationStatus.APPLIED,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val hiredAt: Long = 0L,
    val completedAt: Long = 0L,
    val callCount: Int = 0,
    val lastCalledAt: Long = 0L,
    val workerName: String = "",
    val workerPhoto: String = "",
    val workerSkill: String = "",

    // Runtime joins (never stored)
    val job: JobListingSummary? = null,
    val worker: WorkerCard? = null,
    /** Revealed to the employer via getWorkerContact; empty until then. */
    val workerPhone: String = ""
) {
    val jobTitle: String get() = job?.title.orEmpty()
    val companyName: String get() = job?.companyName.orEmpty()
    val jobArea: String get() = job?.area.orEmpty()
    val jobStatus: String get() = job?.status ?: Values.JobStatus.OPEN
    /** Worker only called the employer (no in-app apply yet). */
    val calledOnly: Boolean get() = callCount > 0

    companion object {
        fun from(id: String, data: Map<String, Any?>): JobApplication = JobApplication(
            id = id,
            jobId = data[Applications.JOB_ID] as? String ?: "",
            workerId = data[Applications.WORKER_ID] as? String ?: "",
            employerId = data[Applications.EMPLOYER_ID] as? String ?: "",
            status = ApplicationStatus.fromFirestoreValue(data[Applications.STATUS] as? String),
            createdAt = data[Applications.CREATED_AT].epochMillis(),
            updatedAt = data[Applications.UPDATED_AT].epochMillis(),
            hiredAt = data[Applications.HIRED_AT].epochMillis(),
            completedAt = data[Applications.COMPLETED_AT].epochMillis(),
            callCount = (data[Applications.CALL_COUNT] as? Number)?.toInt() ?: 0,
            lastCalledAt = data[Applications.LAST_CALLED_AT].epochMillis(),
            workerName = data[Applications.WORKER_NAME] as? String ?: "",
            workerPhoto = data[Applications.WORKER_PHOTO] as? String ?: "",
            workerSkill = data[Applications.WORKER_SKILL] as? String ?: ""
        )
    }
}

/**
 * `worker_cards/{uid}` — the public worker card employers see (never contains the phone).
 */
@Keep
@Immutable
data class WorkerCard(
    val uid: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val skills: List<String> = emptyList(),
    val experienceYears: Int = 0,
    val area: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val available: Boolean = false,
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val jobsCompleted: Int = 0,
    val lastActiveAt: Long = 0L,
    var distanceKm: Double? = null
) {
    companion object {
        fun from(uid: String, data: Map<String, Any?>): WorkerCard {
            val C = com.example.dutype.firestore.FirestoreSchema.WorkerCards
            return WorkerCard(
                uid = uid,
                name = data[C.NAME] as? String ?: "",
                photoUrl = data[C.PHOTO_URL] as? String ?: "",
                skills = (data[C.SKILLS] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
                experienceYears = (data[C.EXPERIENCE_YEARS] as? Number)?.toInt() ?: 0,
                area = data[C.AREA] as? String ?: "",
                lat = (data[C.LAT] as? Number)?.toDouble() ?: 0.0,
                lng = (data[C.LNG] as? Number)?.toDouble() ?: 0.0,
                available = data[C.AVAILABLE] as? Boolean ?: false,
                rating = (data[C.RATING] as? Number)?.toDouble() ?: 0.0,
                ratingCount = (data[C.RATING_COUNT] as? Number)?.toInt() ?: 0,
                jobsCompleted = (data[C.JOBS_COMPLETED] as? Number)?.toInt() ?: 0,
                lastActiveAt = data[C.LAST_ACTIVE_AT].epochMillis()
            )
        }
    }
}

/** Values.ApplicationStatus as an enum for the UI. */
enum class ApplicationStatus(val key: String) {
    APPLIED(Values.ApplicationStatus.APPLIED),
    HIRED(Values.ApplicationStatus.HIRED),
    COMPLETED(Values.ApplicationStatus.COMPLETED),
    REJECTED(Values.ApplicationStatus.REJECTED),
    WITHDRAWN(Values.ApplicationStatus.WITHDRAWN);

    fun toFirestoreValue(): String = key

    companion object {
        fun fromFirestoreValue(value: String?): ApplicationStatus =
            entries.firstOrNull { it.key == value?.trim()?.lowercase() } ?: APPLIED
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

data class JobApplicationUiState(
    val applications: List<JobApplication> = emptyList(),
    val isLoading: Boolean = false
)

@Keep
data class ApplicationStats(
    val totalApplications: Int = 0,
    val appliedApplications: Int = 0,
    val rejectedApplications: Int = 0,
    val hiredApplications: Int = 0,
    val recentApplications: List<JobApplication> = emptyList()
)
