package com.example.dutype.utils

import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Extension functions for JobListing — strict schema only.
 * No legacy field aliases, no backward-compat fallbacks.
 */

private fun mapToEpochMillis(value: Any?): Long {
    fun normalizeEpoch(raw: Long): Long {
        if (raw <= 0L) return 0L
        return when {
            raw < 100_000_000_000L -> raw * 1000L
            raw > 9_999_999_999_999L -> raw / 1000L
            else -> raw
        }
    }

    return when (value) {
        is Timestamp -> normalizeEpoch(value.toDate().time)
        is Number -> normalizeEpoch(value.toLong())
        is Date -> normalizeEpoch(value.time)
        else -> 0L
    }
}

private fun mapToSalaryDouble(value: Any?): Double {
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

private fun normalizeJobStatus(status: Any?, isActive: Any?, isFilled: Any?): String {
    val explicit = status?.toString()?.trim()?.lowercase()
    if (explicit in listOf("open", "closed", "expired")) return explicit!!

    val active = isActive as? Boolean
    val filled = isFilled as? Boolean
    return if (active == true && filled != true) "open" else "closed"
}

private fun normalizeSalaryType(primary: Any?, fallback: Any?): String {
    val value = primary?.toString()?.trim().orEmpty()
        .ifBlank { fallback?.toString()?.trim().orEmpty() }
        .uppercase()
    return if (value.isBlank()) "DAILY" else value
}

/**
 * Convert Map<String, Any?> to JobListing.
 * Reads only canonical schema field names.
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toJobListing(isSaved: Boolean = false): JobListing {
    val locationMap = this["location"] as? Map<*, *>
    val lat = (locationMap?.get("lat") as? Number)?.toDouble()
        ?: (this["latitude"] as? Number)?.toDouble()
        ?: 0.0
    val lng = (locationMap?.get("lng") as? Number)?.toDouble()
        ?: (this["longitude"] as? Number)?.toDouble()
        ?: 0.0

    val salary = mapToSalaryDouble(this["salary"]).takeIf { it > 0.0 }
        ?: mapToSalaryDouble(this["payAmount"])

    val normalizedStatus = normalizeJobStatus(
        status = this["status"],
        isActive = this["isActive"],
        isFilled = this["isFilled"]
    )

    val addressText = (this["addressText"] as? String).orEmpty().ifBlank {
        (this["companyCity"] as? String).orEmpty().ifBlank {
            (this["location"] as? String).orEmpty()
        }
    }

    val description = (this["description"] as? String).orEmpty()
    val normalizedJobType = (this["jobType"] as? String)
        ?.takeIf { it.isNotBlank() }
        ?: com.example.dutype.utils.CategoryDetector.detectCategory(
            (this["title"] as? String) ?: "",
            description
        )

    return JobListing(
        id = (this["jobId"] as? String) ?: (this["id"] as? String) ?: "",
        employerId = (this["employerId"] as? String) ?: "",
        title = (this["title"] as? String) ?: "",
        salary = salary,
        salaryType = normalizeSalaryType(this["salaryType"], this["payType"]),
        jobType = normalizedJobType,
        geohash = (this["geohash"] as? String) ?: "",
        urgency = (this["urgency"] as? String) ?: "MEDIUM",
        gender = (this["gender"] as? String) ?: "Any",
        experienceRequired = (this["experienceRequired"] as? String) ?: "No Experience Required",
        shiftTiming = (this["shiftTiming"] as? String) ?: "Flexible",
        isVerified = (this["isVerified"] as? Boolean) ?: false,
        applicationCount = (this["applicationCount"] as? Number)?.toInt() ?: 0,
        status = normalizedStatus,
        createdAt = mapToEpochMillis(this["createdAt"]).takeIf { it > 0L } ?: System.currentTimeMillis(),
        expiresAt = mapToEpochMillis(this["expiresAt"]).takeIf { it > 0L }
            ?: (System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)),
        lat = lat,
        lng = lng,
        companyName = (this["companyName"] as? String)
            ?: (this["employerName"] as? String)
            ?: (this["company"] as? String)
            ?: (this["businessName"] as? String)
            ?: (this["company_name"] as? String)
            ?: "",
        // job_details fields (runtime only, loaded on click)
        description = description,
        contactNumber = (this["contactNumber"] as? String) ?: "",
        whatsappNumber = (this["whatsappNumber"] as? String) ?: "",
        location = addressText,
        addressText = addressText,
        vacancies = (this["vacancies"] as? Number)?.toInt() ?: 1,
        workingHours = (this["workingHours"] as? String) ?: "",
        educationRequired = (this["educationRequired"] as? String) ?: "",
        benefits = (this["benefits"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
        distance = (this["distance"] as? Number)?.toDouble(),
        isSaved = isSaved,
        jobImageUrl = (this["jobImageUrl"] as? String)?.takeIf { it.isNotBlank() }
    )
}

/**
 * Convert Map<String, Any?> to JobListingSummary.
 * Reads only canonical schema field names.
 */
fun Map<String, Any?>.toJobListingSummary(isSaved: Boolean = false): JobListingSummary {
    val locationMap = this["location"] as? Map<*, *>
    val lat = (locationMap?.get("lat") as? Number)?.toDouble()
        ?: (this["latitude"] as? Number)?.toDouble()
        ?: 0.0
    val lng = (locationMap?.get("lng") as? Number)?.toDouble()
        ?: (this["longitude"] as? Number)?.toDouble()
        ?: 0.0

    val salary = mapToSalaryDouble(this["salary"]).takeIf { it > 0.0 }
        ?: mapToSalaryDouble(this["payAmount"])

    val normalizedStatus = normalizeJobStatus(
        status = this["status"],
        isActive = this["isActive"],
        isFilled = this["isFilled"]
    )

    val title = (this["title"] as? String) ?: ""
    val description = (this["description"] as? String) ?: ""
    val normalizedJobType = (this["jobType"] as? String)
        ?.takeIf { it.isNotBlank() }
        ?: com.example.dutype.utils.CategoryDetector.detectCategory(title, description)

    val locationText = (this["addressText"] as? String).orEmpty().ifBlank {
        (this["location"] as? String).orEmpty()
    }

    val companyCity = (this["companyCity"] as? String).orEmpty().ifBlank {
        locationText
    }

    val docId = (this["jobId"] as? String) ?: (this["documentId"] as? String) ?: (this["id"] as? String) ?: ""

    return JobListingSummary(
        id = docId,
        employerId = (this["employerId"] as? String) ?: "",
        companyName = (this["companyName"] as? String)
            ?: (this["employerName"] as? String)
            ?: (this["company"] as? String)
            ?: (this["businessName"] as? String)
            ?: (this["company_name"] as? String)
            ?: "",
        title = title,
        jobType = normalizedJobType,
        salary = salary,
        salaryType = normalizeSalaryType(this["salaryType"], this["payType"]),
        geohash = (this["geohash"] as? String) ?: "",
        urgency = (this["urgency"] as? String) ?: "MEDIUM",
        status = normalizedStatus,
        createdAt = mapToEpochMillis(this["createdAt"]),
        expiresAt = mapToEpochMillis(this["expiresAt"]),
        lat = lat,
        lng = lng,
        companyCity = companyCity,
        locationText = locationText,
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
    companyName = companyName,
    title = title,
    salary = salary,
    salaryType = salaryType.uppercase().ifBlank { "DAILY" },
    jobType = jobType,
    geohash = geohash,
    urgency = urgency,
    status = status,
    createdAt = createdAt,
    expiresAt = expiresAt,
    lat = lat,
    lng = lng,
    location = locationText.ifBlank { companyCity },
    addressText = locationText.ifBlank { companyCity },
    distance = distance,
    isSaved = isSaved,
    jobImageUrl = jobImageUrl
)
