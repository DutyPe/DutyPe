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
    // Format distance as string - use local variable to avoid smart cast issues
    val dist = distance
    val distanceStr = when {
        dist == null -> "N/A"
        dist < 1.0 -> "${(dist * 1000).toInt()}m"
        else -> String.format("%.1fkm", dist)
    }
    
    return LocationInfo(
        address = location,
        // REMOVED: city, area - use location only
        city = "", // Deprecated field
        area = "", // Deprecated field
        distance = distanceStr
    )
}

/**
 * Convert JobListing to TimeInfo for display
 */
fun JobListing.toTimeInfo(): TimeInfo {
    return TimeInfo(
        shiftTiming = shiftTiming,
        workingDays = "",
        startTime = shiftTiming.substringBefore("-", "").trim(),
        endTime = shiftTiming.substringAfter("-", "").trim()
    )
}

/**
 * Check if job has urgent hiring flag
 */
fun JobListing.hasUrgentHiring(): Boolean {
    // Check title and description for urgent keywords
    val titleLower = title.lowercase()
    val descLower = description.lowercase()
    return titleLower.contains("urgent") || 
           titleLower.contains("immediate") ||
           descLower.contains("urgent hiring") ||
           descLower.contains("immediate joining")
}

/**
 * Convert Map<String, Any?> to JobListing
 * Used by repositories to convert Firestore documents to JobListing objects
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toJobListing(isSaved: Boolean = false): JobListing {
    return JobListing(
        id = (this["id"] as? String) ?: "",
        employerId = (this["employerId"] as? String) ?: "",
        title = (this["title"] as? String) ?: "",
        companyName = (this["companyName"] as? String) ?: (this["company"] as? String) ?: "",
        description = (this["description"] as? String) ?: "",
        
        // Location (3 fields)
        location = (this["location"] as? String) ?: "",
        latitude = (this["latitude"] as? Number)?.toDouble() ?: 0.0,
        longitude = (this["longitude"] as? Number)?.toDouble() ?: 0.0,
        
        // Pay (2 fields)
        payAmount = (this["payAmount"] as? String) ?: "",
        payType = (this["payType"] as? String) ?: "",
        
        // Timing
        shiftTiming = (this["shiftTiming"] as? String) ?: (this["timing"] as? String) ?: "",
        
        // Status (3 fields)
        isActive = (this["isActive"] as? Boolean) ?: true,
        isFilled = (this["isFilled"] as? Boolean) ?: false,
        postedAt = (this["postedAt"] as? Number)?.toLong() ?: (this["createdAt"] as? Number)?.toLong() ?: 0L,
        
        // Contact
        contactNumber = (this["contactNumber"] as? String) ?: (this["phoneNumber"] as? String) ?: "",
        
        // Optional details
        vacancies = (this["vacancies"] as? Number)?.toInt() ?: 1,
        jobType = (this["jobType"] as? String) ?: "FULL_TIME",
        gender = (this["gender"] as? String) ?: "ANY",
        
        // Runtime (not in Firestore)
        distance = (this["distance"] as? Number)?.toDouble(),
        isSaved = isSaved
    )
}

/**
 * Convert Map<String, Any?> to JobListingSummary
 * Used by repositories to convert Firestore documents to JobListingSummary objects
 */
fun Map<String, Any?>.toJobListingSummary(isSaved: Boolean = false): JobListingSummary {
    return JobListingSummary(
        id = (this["id"] as? String) ?: "",
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
        isSaved = isSaved
    )
}

