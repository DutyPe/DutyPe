package com.example.dutype.models

import androidx.annotation.Keep

/**
 * JobListingSummary — strict target schema model for job card list views.
 *
 * Maps directly to the jobs collection fields:
 *   jobId, employerId, title, jobType, salary, salaryType,
 *   location:{lat,lng}, geohash, urgency, status, createdAt, expiresAt
 *
 * Runtime-only fields (never stored in Firestore):
 *   distance, isSaved, isApplied
 */
@Keep
data class JobListingSummary(
    val id: String = "",
    val employerId: String = "",
    val title: String = "",
    val jobType: String = "",
    val salary: Double = 0.0,
    val salaryType: String = "",        // "HOURLY" | "DAILY" | "MONTHLY"
    val geohash: String = "",
    val urgency: String = "MEDIUM",     // "LOW" | "MEDIUM" | "HIGH"
    val status: String = "open",        // "open" | "closed" | "expired"
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val lat: Double = 0.0,
    val lng: Double = 0.0,

    // Runtime-only (never stored in Firestore)
    var distance: Double? = null,
    var isSaved: Boolean = false,
    var isApplied: Boolean = false
) {
    val jobId: String get() = id

    fun isExpired(): Boolean = expiresAt > 0L && System.currentTimeMillis() > expiresAt

    companion object {
        /**
         * Create from Firestore document map — reads only canonical schema fields.
         */
        fun fromMap(data: Map<String, Any>, docId: String = ""): JobListingSummary {
            val locationMap = data["location"] as? Map<*, *>
            val lat = (locationMap?.get("lat") as? Number)?.toDouble() ?: 0.0
            val lng = (locationMap?.get("lng") as? Number)?.toDouble() ?: 0.0
            val salary = (data["salary"] as? Number)?.toDouble() ?: 0.0
            val id = (data["jobId"] as? String) ?: (data["documentId"] as? String) ?: docId

            return JobListingSummary(
                id = id,
                employerId = data["employerId"] as? String ?: "",
                title = data["title"] as? String ?: "",
                jobType = data["jobType"] as? String ?: "",
                salary = salary,
                salaryType = data["salaryType"] as? String ?: "",
                geohash = data["geohash"] as? String ?: "",
                urgency = data["urgency"] as? String ?: "MEDIUM",
                status = data["status"] as? String ?: "open",
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
                expiresAt = (data["expiresAt"] as? Number)?.toLong() ?: 0L,
                lat = lat,
                lng = lng
            )
        }

        /**
         * Create from full JobListing.
         */
        fun fromJobListing(job: JobListing): JobListingSummary = JobListingSummary(
            id = job.id,
            employerId = job.employerId,
            title = job.title,
            jobType = job.jobType,
            salary = job.salary,
            salaryType = job.salaryType,
            geohash = job.geohash,
            urgency = job.urgency,
            status = job.status,
            createdAt = job.createdAt,
            expiresAt = job.expiresAt,
            lat = job.lat,
            lng = job.lng,
            distance = job.distance,
            isSaved = job.isSaved
        )
    }
}
