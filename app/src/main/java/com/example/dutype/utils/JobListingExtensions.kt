package com.example.dutype.utils

import com.example.dutype.firestore.FirestoreSchema.JobDetails
import com.example.dutype.firestore.FirestoreSchema.Jobs
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.google.firebase.Timestamp
import java.util.Date

/**
 * The only Firestore → job model mapping. Reads schema field names only
 * ([Jobs] for the card, [JobDetails] for the details); the document id is passed in,
 * never read from a field.
 */

internal fun Any?.epochMillis(): Long = when (this) {
    is Timestamp -> toDate().time
    is Date -> time
    is Number -> toLong()
    else -> 0L
}

private fun Map<String, Any?>.str(key: String): String = (this[key] as? String)?.trim().orEmpty()
private fun Map<String, Any?>.dbl(key: String): Double = (this[key] as? Number)?.toDouble() ?: 0.0
private fun Map<String, Any?>.lng(key: String): Long = (this[key] as? Number)?.toLong() ?: 0L
private fun Map<String, Any?>.int(key: String, default: Int = 0): Int = (this[key] as? Number)?.toInt() ?: default

/** jobmetadata document → card. */
fun Map<String, Any?>.toJobListingSummary(id: String, isSaved: Boolean = false): JobListingSummary =
    JobListingSummary(
        id = id,
        employerId = str(Jobs.EMPLOYER_ID),
        title = str(Jobs.TITLE),
        category = str(Jobs.CATEGORY),
        employmentType = str(Jobs.EMPLOYMENT_TYPE).ifBlank { Values.EmploymentType.FULL_TIME },
        companyName = str(Jobs.COMPANY_NAME),
        photoUrl = str(Jobs.PHOTO_URL).ifBlank { null },
        payAmount = lng(Jobs.PAY_AMOUNT),
        payType = str(Jobs.PAY_TYPE).ifBlank { Values.PayType.DAILY },
        vacancies = int(Jobs.VACANCIES, 1).coerceAtLeast(1),
        urgency = str(Jobs.URGENCY).ifBlank { Values.Urgency.NORMAL },
        shift = str(Jobs.SHIFT).ifBlank { Values.Shift.ANY },
        area = str(Jobs.AREA),
        lat = dbl(Jobs.LAT),
        lng = dbl(Jobs.LNG),
        geohash = str(Jobs.GEOHASH),
        district = str(Jobs.DISTRICT),
        state = str(Jobs.STATE),
        status = str(Jobs.STATUS).ifBlank { Values.JobStatus.OPEN },
        applicationCount = int(Jobs.APPLICATION_COUNT),
        createdAt = this[Jobs.CREATED_AT].epochMillis(),
        expiresAt = this[Jobs.EXPIRES_AT].epochMillis(),
        isSaved = isSaved
    )

/** jobmetadata document (+ job_details document when loaded) → full job. */
fun Map<String, Any?>.toJobListing(
    id: String,
    details: Map<String, Any?>? = null,
    isSaved: Boolean = false
): JobListing {
    val card = toJobListingSummary(id, isSaved)
    val d = details.orEmpty()
    @Suppress("UNCHECKED_CAST")
    return JobListing(
        id = id,
        employerId = card.employerId,
        title = card.title,
        category = card.category,
        employmentType = card.employmentType,
        companyName = card.companyName,
        photoUrl = card.photoUrl,
        payAmount = card.payAmount,
        payType = card.payType,
        vacancies = card.vacancies,
        urgency = card.urgency,
        shift = card.shift,
        area = card.area,
        lat = card.lat,
        lng = card.lng,
        geohash = card.geohash,
        district = card.district,
        state = card.state,
        status = card.status,
        applicationCount = card.applicationCount,
        createdAt = card.createdAt,
        expiresAt = card.expiresAt,
        description = d.str(JobDetails.DESCRIPTION),
        addressText = d.str(JobDetails.ADDRESS_TEXT),
        gender = d.str(JobDetails.GENDER),
        experienceRequired = d.str(JobDetails.EXPERIENCE_REQUIRED),
        educationRequired = d.str(JobDetails.EDUCATION_REQUIRED),
        benefits = (d[JobDetails.BENEFITS] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
        isSaved = isSaved
    )
}

/** Card → full job with empty details (details load on open). */
fun JobListingSummary.toJobListing(): JobListing = JobListing(
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
