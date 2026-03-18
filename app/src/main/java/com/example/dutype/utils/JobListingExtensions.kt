package com.example.dutype.utils

import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary

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
        jobType = (this["jobType"] as? String) ?: "",
        geohash = (this["geohash"] as? String) ?: "",
        urgency = (this["urgency"] as? String) ?: "MEDIUM",
        status = status,
        createdAt = (this["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        expiresAt = (this["expiresAt"] as? Number)?.toLong()
            ?: (System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)),
        lat = lat,
        lng = lng,
        // job_details fields (runtime only, loaded on click)
        description = (this["description"] as? String) ?: "",
        contactNumber = (this["contactNumber"] as? String) ?: "",
        location = (this["addressText"] as? String) ?: "",
        addressText = (this["addressText"] as? String) ?: "",
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
