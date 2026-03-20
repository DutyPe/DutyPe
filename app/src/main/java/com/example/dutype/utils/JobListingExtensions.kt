package com.example.dutype.utils

import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Extension functions for JobListing — strict schema only.
 * No legacy field aliases, no backward-compat fallbacks.
 */

/**
 * Convert Map<String, Any?> to JobListing.
 * Reads only canonical schema field names.
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toJobListing(isSaved: Boolean = false): JobListing {
    fun toEpochMillis(value: Any?): Long {
        return when (value) {
            is Timestamp -> value.toDate().time
            is Number -> value.toLong()
            is Date -> value.time
            else -> 0L
        }
    }

    val locationMap = this["location"] as? Map<*, *>
    val lat = (locationMap?.get("lat") as? Number)?.toDouble() ?: 0.0
    val lng = (locationMap?.get("lng") as? Number)?.toDouble() ?: 0.0
    val salary = (this["salary"] as? Number)?.toDouble() ?: 0.0
    val status = (this["status"] as? String)?.lowercase()?.let {
        if (it in listOf("open", "closed", "expired")) it else "open"
    } ?: "open"

    return JobListing(
        id = (this["jobId"] as? String) ?: (this["id"] as? String) ?: "",
        employerId = (this["employerId"] as? String) ?: "",
        title = (this["title"] as? String) ?: "",
        salary = salary,
        salaryType = (this["salaryType"] as? String) ?: "",
        jobType = (this["jobType"] as? String)
            ?.takeIf { it.isNotBlank() }
            ?: com.example.dutype.utils.CategoryDetector.detectCategory(
                (this["title"] as? String) ?: "",
                (this["description"] as? String) ?: ""
            ),
        geohash = (this["geohash"] as? String) ?: "",
        urgency = (this["urgency"] as? String) ?: "MEDIUM",
        gender = (this["gender"] as? String) ?: "Any",
        experienceRequired = (this["experienceRequired"] as? String) ?: "No Experience Required",
        shiftTiming = (this["shiftTiming"] as? String) ?: "Flexible",
        isVerified = (this["isVerified"] as? Boolean) ?: false,
        applicationCount = (this["applicationCount"] as? Number)?.toInt() ?: 0,
        status = status,
        createdAt = toEpochMillis(this["createdAt"]).takeIf { it > 0L } ?: System.currentTimeMillis(),
        expiresAt = toEpochMillis(this["expiresAt"]).takeIf { it > 0L }
            ?: (System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)),
        lat = lat,
        lng = lng,
        companyName = (this["companyName"] as? String) ?: "",
        // job_details fields (runtime only, loaded on click)
        description = (this["description"] as? String) ?: "",
        contactNumber = (this["contactNumber"] as? String) ?: "",
        whatsappNumber = (this["whatsappNumber"] as? String) ?: "",
        location = (this["addressText"] as? String) ?: "",
        addressText = (this["addressText"] as? String) ?: "",
        vacancies = (this["vacancies"] as? Number)?.toInt() ?: 1,
        workingHours = (this["workingHours"] as? String) ?: "",
        educationRequired = (this["educationRequired"] as? String) ?: "",
        benefits = (this["benefits"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
        distance = (this["distance"] as? Number)?.toDouble(),
        isSaved = isSaved
    )
}

/**
 * Convert Map<String, Any?> to JobListingSummary.
 * Reads only canonical schema field names.
 */
fun Map<String, Any?>.toJobListingSummary(isSaved: Boolean = false): JobListingSummary {
    val locationMap = this["location"] as? Map<*, *>
    val lat = (locationMap?.get("lat") as? Number)?.toDouble() ?: 0.0
    val lng = (locationMap?.get("lng") as? Number)?.toDouble() ?: 0.0
    val salary = (this["salary"] as? Number)?.toDouble() ?: 0.0
    val docId = (this["jobId"] as? String) ?: (this["documentId"] as? String) ?: (this["id"] as? String) ?: ""

    return JobListingSummary(
        id = docId,
        employerId = (this["employerId"] as? String) ?: "",
        title = (this["title"] as? String) ?: "",
        jobType = (this["jobType"] as? String) ?: "",
        salary = salary,
        salaryType = (this["salaryType"] as? String) ?: "",
        geohash = (this["geohash"] as? String) ?: "",
        urgency = (this["urgency"] as? String) ?: "MEDIUM",
        status = (this["status"] as? String) ?: "open",
        createdAt = (this["createdAt"] as? Number)?.toLong() ?: 0L,
        expiresAt = (this["expiresAt"] as? Number)?.toLong() ?: 0L,
        lat = lat,
        lng = lng,
        distance = (this["distance"] as? Number)?.toDouble(),
        isSaved = isSaved
    )
}

/**
 * Convert JobListingSummary to full JobListing.
 * job_details fields (description, contactNumber, addressText) are empty
 * until loaded on demand.
 */
fun JobListingSummary.toJobListing(): JobListing = JobListing(
    id = id,
    employerId = employerId,
    title = title,
    salary = salary,
    salaryType = salaryType,
    jobType = jobType,
    geohash = geohash,
    urgency = urgency,
    status = status,
    createdAt = createdAt,
    expiresAt = expiresAt,
    lat = lat,
    lng = lng,
    distance = distance,
    isSaved = isSaved
)
