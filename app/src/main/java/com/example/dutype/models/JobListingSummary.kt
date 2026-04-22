package com.example.dutype.models

import androidx.annotation.Keep
import androidx.compose.runtime.Stable
import com.google.firebase.Timestamp
import java.util.Date

/**
 * JobListingSummary — strict target schema model for job card list views.
 *
 * Maps directly to the jobmetadata collection fields:
 *   jobId, employerId, title, jobType, salary, salaryType,
 *   location:{lat,lng}, geohash, urgency, status, createdAt, expiresAt
 *
 * Runtime-only fields (never stored in Firestore):
 *   distance, isSaved, isApplied
 */
@Keep
@Stable
data class JobListingSummary(
    val id: String = "",
    val employerId: String = "",
    val companyName: String = "",
    val title: String = "",
    val jobType: String = "",
    val salary: String = "",
    val salaryType: String = "",        // "HOURLY" | "DAILY" | "MONTHLY"
    val geohash: String = "",
    val urgency: String = "MEDIUM",     // "LOW" | "MEDIUM" | "HIGH"
    val status: String = "open",        // "open" | "closed" | "expired"
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val companyCity: String = "",       // NEW: City name for location display
    val locationText: String = "",      // NEW: Exact location text for card display

    // Runtime-only (never stored in Firestore)
    var distance: Double? = null,
    var isSaved: Boolean = false,
    var isApplied: Boolean = false,

    // #5 fix: optional employer-uploaded hero image URL stored on jobmetadata.
    val jobImageUrl: String? = null
) {
    val jobId: String get() = id

    fun isExpired(): Boolean = expiresAt > 0L && System.currentTimeMillis() > expiresAt

    companion object {
        /**
         * Read salary as the raw user-typed string. Accepts numbers,
         * ranges ("1000-2000"), open-ended ("2000+"), and free text
         * ("Negotiable"). Stored verbatim — numeric filters parse via
         * [com.example.dutype.utils.SalaryFormatter.lowerBound].
         */
        private fun parseSalary(value: Any?): String = when (value) {
            null -> ""
            is String -> value.trim()
            is Number -> {
                val d = value.toDouble()
                if (d <= 0.0) "" else if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
            }
            else -> value.toString().trim()
        }

        private fun normalizeStatus(data: Map<String, Any>): String {
            val explicit = (data["status"] as? String)?.trim()?.lowercase()
            if (explicit in listOf("open", "closed", "expired")) return explicit!!

            val isActive = data["isActive"] as? Boolean
            val isFilled = data["isFilled"] as? Boolean
            return if (isActive == true && isFilled != true) "open" else "closed"
        }

        /**
         * Create from Firestore document map — reads only canonical schema fields.
         */
        fun fromMap(data: Map<String, Any>, docId: String = ""): JobListingSummary {
            fun normalizeEpoch(raw: Long): Long {
                if (raw <= 0L) return 0L
                return when {
                    raw < 100_000_000_000L -> raw * 1000L
                    raw > 9_999_999_999_999L -> raw / 1000L
                    else -> raw
                }
            }

            fun toEpochMillis(value: Any?): Long {
                return when (value) {
                    is Timestamp -> normalizeEpoch(value.toDate().time)
                    is Number -> normalizeEpoch(value.toLong())
                    is Date -> normalizeEpoch(value.time)
                    else -> 0L
                }
            }

            val locationMap = data["location"] as? Map<*, *>
            val lat = (locationMap?.get("lat") as? Number)?.toDouble()
                ?: (data["latitude"] as? Number)?.toDouble()
                ?: 0.0
            val lng = (locationMap?.get("lng") as? Number)?.toDouble()
                ?: (data["longitude"] as? Number)?.toDouble()
                ?: 0.0

            val salary = parseSalary(data["salary"]).ifBlank { parseSalary(data["payAmount"]) }

            val title = data["title"] as? String ?: ""
            val description = data["description"] as? String ?: ""
            val jobType = (data["jobType"] as? String)
                ?.takeIf { it.isNotBlank() }
                ?: com.example.dutype.utils.CategoryDetector.detectCategory(title, description)

            val locationText = (data["addressText"] as? String).orEmpty().ifBlank {
                (data["location"] as? String).orEmpty()
            }

            val companyCity = (data["companyCity"] as? String).orEmpty().ifBlank {
                locationText
            }

            val salaryType = ((data["salaryType"] as? String)
                ?: (data["payType"] as? String)
                ?: "DAILY").uppercase()

            val id = (data["jobId"] as? String) ?: (data["documentId"] as? String) ?: docId

            return JobListingSummary(
                id = id,
                employerId = data["employerId"] as? String ?: "",
                companyName = (data["companyName"] as? String)
                    ?: (data["employerName"] as? String)
                    ?: (data["company"] as? String)
                    ?: (data["businessName"] as? String)
                    ?: (data["company_name"] as? String)
                    ?: "",
                title = title,
                jobType = jobType,
                salary = salary,
                salaryType = salaryType,
                geohash = data["geohash"] as? String ?: "",
                urgency = data["urgency"] as? String ?: "MEDIUM",
                status = normalizeStatus(data),
                createdAt = toEpochMillis(data["createdAt"]),
                expiresAt = toEpochMillis(data["expiresAt"]),
                lat = lat,
                lng = lng,
                companyCity = companyCity,
                locationText = locationText,
                jobImageUrl = (data["jobImageUrl"] as? String)?.takeIf { it.isNotBlank() }
            )
        }

        /**
         * Create from full JobListing.
         */
        fun fromJobListing(job: JobListing): JobListingSummary = JobListingSummary(
            id = job.id,
            employerId = job.employerId,
            companyName = job.companyName,
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
            companyCity = job.addressText.ifBlank { job.location },
            locationText = job.addressText.ifBlank { job.location },
            distance = job.distance,
            isSaved = job.isSaved,
            jobImageUrl = job.jobImageUrl
        )
    }
}
