package com.example.dutype.utils

import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.example.dutype.employer.models.PayType
import com.example.dutype.worker.models.PayInfo
import com.example.dutype.worker.models.LocationInfo
import com.example.dutype.worker.models.TimeInfo

/**
 * Extension functions for converting JobListing to UI models
 */

/**
 * Convert JobListing to PayInfo for display
 */
fun JobListing.toPayInfo(): PayInfo {
    val payTypeEnum = when {
        payType.contains("hour", ignoreCase = true) -> PayType.HOURLY
        payType.contains("day", ignoreCase = true) || payType.contains("daily", ignoreCase = true) -> PayType.DAILY
        payType.contains("month", ignoreCase = true) -> PayType.MONTHLY
        payType.contains("task", ignoreCase = true) || payType.contains("delivery", ignoreCase = true) -> PayType.TASK
        else -> PayType.DAILY
    }
    
    return PayInfo(
        amount = payAmount.ifBlank { "₹0" },
        type = payTypeEnum,
        period = payType
    )
}

/**
 * Convert JobListing to LocationInfo for display
 */
fun JobListing.toLocationInfo(): LocationInfo {
    // Format distance as string
    val distanceStr = when {
        distance == null -> "N/A"
        distance < 1.0 -> "${(distance * 1000).toInt()}m"
        else -> String.format("%.1fkm", distance)
    }
    
    return LocationInfo(
        address = location.ifBlank { specificLocation },
        city = city ?: "",
        area = area ?: "",
        distance = distanceStr
    )
}

/**
 * Convert JobListing to TimeInfo for display
 */
fun JobListing.toTimeInfo(): TimeInfo {
    return TimeInfo(
        shiftTiming = shiftTiming,
        workingDays = workingHours,
        startTime = timing.substringBefore("-", "").trim(),
        endTime = timing.substringAfter("-", "").trim()
    )
}

/**
 * Check if job has urgent hiring flag
 */
fun JobListing.hasUrgentHiring(): Boolean {
    return urgency.contains("urgent", ignoreCase = true) ||
           urgency.contains("immediate", ignoreCase = true) ||
           isUrgent()
}

/**
 * Convert Map<String, Any?> to JobListing
 * Used by repositories to convert Firestore documents to JobListing objects
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toJobListing(isSaved: Boolean = false): JobListing {
    return JobListing(
        id = (this["id"] as? String) ?: (this["jobId"] as? String) ?: "",
        jobId = (this["jobId"] as? String) ?: (this["id"] as? String) ?: "",
        title = (this["title"] as? String) ?: "",
        company = (this["company"] as? String) ?: (this["companyName"] as? String) ?: "",
        companyName = (this["companyName"] as? String) ?: (this["company"] as? String) ?: "",
        location = (this["location"] as? String) ?: "",
        specificLocation = (this["specificLocation"] as? String) ?: "",
        area = this["area"] as? String,
        city = this["city"] as? String,
        salary = (this["salary"] as? String) ?: "",
        payAmount = (this["payAmount"] as? String) ?: "",
        payType = (this["payType"] as? String) ?: "",
        payRate = (this["payRate"] as? Number)?.toDouble() ?: 0.0,
        jobType = (this["jobType"] as? String) ?: "",
        category = (this["category"] as? String) ?: "",
        description = (this["description"] as? String) ?: "",
        requirements = (this["requirements"] as? List<String>) ?: emptyList(),
        benefits = (this["benefits"] as? List<String>) ?: emptyList(),
        vacancies = (this["vacancies"] as? Number)?.toInt() ?: 1,
        timing = (this["timing"] as? String) ?: "",
        shiftTiming = (this["shiftTiming"] as? String) ?: "",
        workingHours = (this["workingHours"] as? String) ?: "",
        urgency = (this["urgency"] as? String) ?: "",
        employerId = (this["employerId"] as? String) ?: "",
        employerTrustTier = (this["employerTrustTier"] as? String) ?: "",
        contactNumber = (this["contactNumber"] as? String) ?: "",
        phoneNumber = (this["phoneNumber"] as? String) ?: "",
        postedAt = (this["postedAt"] as? Number)?.toLong() ?: (this["createdAt"] as? Number)?.toLong() ?: 0L,
        expiresAt = (this["expiresAt"] as? Number)?.toLong() ?: 0L,
        latitude = (this["latitude"] as? Number)?.toDouble() ?: 0.0,
        longitude = (this["longitude"] as? Number)?.toDouble() ?: 0.0,
        distance = (this["distance"] as? Number)?.toDouble(),
        isFilled = (this["isFilled"] as? Boolean) ?: false,
        isActive = (this["isActive"] as? Boolean) ?: true,
        isSaved = isSaved,
        applicationCount = (this["applicationCount"] as? Number)?.toLong() ?: 0L,
        jobImageUrl = (this["jobImageUrl"] as? String) ?: "",
        landmark = (this["landmark"] as? String) ?: ""
    )
}

/**
 * Convert Map<String, Any?> to JobListingSummary
 * Used by repositories to convert Firestore documents to JobListingSummary objects
 */
fun Map<String, Any?>.toJobListingSummary(isSaved: Boolean = false): JobListingSummary {
    return JobListingSummary(
        id = (this["id"] as? String) ?: (this["jobId"] as? String) ?: "",
        jobId = (this["jobId"] as? String) ?: (this["id"] as? String) ?: "",
        employerId = (this["employerId"] as? String) ?: "",
        title = (this["title"] as? String) ?: "",
        companyName = (this["companyName"] as? String) ?: (this["company"] as? String) ?: "",
        location = (this["location"] as? String) ?: "",
        payAmount = (this["payAmount"] as? String) ?: "",
        payType = (this["payType"] as? String) ?: "",
        category = (this["category"] as? String) ?: "",
        jobType = (this["jobType"] as? String) ?: "",
        vacancies = (this["vacancies"] as? Number)?.toInt() ?: 1,
        postedAt = (this["postedAt"] as? Number)?.toLong() ?: (this["createdAt"] as? Number)?.toLong() ?: 0L,
        employerTrustTier = (this["employerTrustTier"] as? String) ?: "",
        latitude = (this["latitude"] as? Number)?.toDouble() ?: 0.0,
        longitude = (this["longitude"] as? Number)?.toDouble() ?: 0.0,
        distance = (this["distance"] as? Number)?.toDouble(),
        isFilled = (this["isFilled"] as? Boolean) ?: false,
        isSaved = isSaved,
        jobImageUrl = (this["jobImageUrl"] as? String) ?: "",
        urgency = (this["urgency"] as? String) ?: ""
    )
}
