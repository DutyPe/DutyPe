package com.example.dutype.models

import androidx.compose.runtime.Stable
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.utils.SalaryFormatter

/**
 * A full job — the card (`jobmetadata/{id}`) plus its details (`job_details/{id}`),
 * read when a job is opened. Field names are exactly the schema's
 * ([com.example.dutype.firestore.FirestoreSchema.Jobs] + [com.example.dutype.firestore.FirestoreSchema.JobDetails]).
 * Built by [com.example.dutype.utils.toJobListing].
 */
@Stable
data class JobListing(
    // jobmetadata
    val id: String = "",
    val employerId: String = "",
    val title: String = "",
    val category: String = "",
    val employmentType: String = Values.EmploymentType.FULL_TIME,
    val companyName: String = "",
    val photoUrl: String? = null,
    val payAmount: Long = 0L,
    val payType: String = Values.PayType.DAILY,
    val vacancies: Int = 1,
    val urgency: String = Values.Urgency.NORMAL,
    val shift: String = Values.Shift.ANY,
    val area: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val geohash: String = "",
    val district: String = "",
    val state: String = "",
    val status: String = Values.JobStatus.OPEN,
    val applicationCount: Int = 0,
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,

    // job_details
    val description: String = "",
    val addressText: String = "",
    val contactNumber: String = "",
    val gender: String = "",
    val experienceRequired: String = "",
    val educationRequired: String = "",
    val benefits: List<String> = emptyList(),

    // Runtime only (never stored)
    var distance: Double? = null,
    var distanceApprox: Boolean = false,
    var feedSection: FeedSection? = null,
    var isSaved: Boolean = false
) {
    val jobId: String get() = id

    val payText: String get() = SalaryFormatter.display(payAmount, payType)

    val isUrgent: Boolean get() = urgency == Values.Urgency.HIGH

    fun isExpired(): Boolean = status == Values.JobStatus.EXPIRED ||
        (status == Values.JobStatus.OPEN && expiresAt > 0L && System.currentTimeMillis() > expiresAt)

    fun isOpen(): Boolean = status == Values.JobStatus.OPEN && !isExpired()

    fun toSummary(): JobListingSummary = JobListingSummary(
        id = id,
        employerId = employerId,
        title = title,
        category = category,
        employmentType = employmentType,
        companyName = companyName,
        photoUrl = photoUrl,
        payAmount = payAmount,
        payType = payType,
        vacancies = vacancies,
        urgency = urgency,
        shift = shift,
        area = area,
        lat = lat,
        lng = lng,
        geohash = geohash,
        district = district,
        state = state,
        status = status,
        applicationCount = applicationCount,
        createdAt = createdAt,
        expiresAt = expiresAt,
        distance = distance,
        distanceApprox = distanceApprox,
        feedSection = feedSection,
        isSaved = isSaved
    )
}
