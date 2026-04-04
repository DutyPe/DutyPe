package com.example.dutype.models

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import java.util.Date

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
    val companyCity: String = "",       // NEW: City name for location display

    // Runtime-only (never stored in Firestore)
    var distance: Double? = null,
    var isSaved: Boolean = false,
    var isApplied: Boolean = false
) {
    val jobId: String get() = id

    fun isExpired(): Boolean = expiresAt > 0L && System.currentTimeMillis() > expiresAt

    companion object {
        private fun parseSalary(value: Any?): Double {
            return when (value) {
                is Number -> value.toDouble()
                is String -> {
                    val cleaned = value.replace("₹", "").replace(",", "").trim()
                    val numbers = Regex("\\d+(?:\\.\\d+)?")
                        .findAll(cleaned)
                        .mapNotNull { it.value.toDoubleOrNull() }
                        .toList()

                    when {
                        numbers.isEmpty() -> 0.0
                        cleaned.contains("-") && numbers.size >= 2 -> (numbers[0] + numbers[1]) / 2.0
                        else -> numbers.first()
                    }
                }
                else -> 0.0
            }
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
            fun toEpochMillis(value: Any?): Long {
                return when (value) {
                    is Timestamp -> value.toDate().time
                    is Number -> value.toLong()
                    is Date -> value.time
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

            val salary = parseSalary(data["salary"]).takeIf { it > 0.0 }
                ?: parseSalary(data["payAmount"])

            val title = data["title"] as? String ?: ""
            val description = data["description"] as? String ?: ""
            val jobType = (data["jobType"] as? String)
                ?.takeIf { it.isNotBlank() }
                ?: com.example.dutype.utils.CategoryDetector.detectCategory(title, description)

            val companyCity = (data["companyCity"] as? String).orEmpty().ifBlank {
                (data["addressText"] as? String).orEmpty().ifBlank {
                    (data["location"] as? String).orEmpty()
                }
            }

            val salaryType = ((data["salaryType"] as? String)
                ?: (data["payType"] as? String)
                ?: "DAILY").uppercase()

            val id = (data["jobId"] as? String) ?: (data["documentId"] as? String) ?: docId

            return JobListingSummary(
                id = id,
                employerId = data["employerId"] as? String ?: "",
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
                companyCity = companyCity
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
            companyCity = job.addressText.ifBlank { job.location },
            distance = job.distance,
            isSaved = job.isSaved
        )
    }
}
