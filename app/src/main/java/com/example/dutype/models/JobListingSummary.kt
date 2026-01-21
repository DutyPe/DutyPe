package com.example.dutype.models

import androidx.annotation.Keep
import com.example.dutype.utils.DateTimeUtils

/**
 * JobListingSummary - Lightweight model for job cards in list views
 * 
 * PERFORMANCE OPTIMIZATION: This model contains only the fields needed to render
 * a job card. Full job details are fetched only when user clicks on the card.
 * 
 * Firestore reads are charged per document, but bandwidth matters for mobile.
 * By fetching only ~15 fields instead of 50+, we reduce:
 * - Network payload by ~70%
 * - Parse time significantly
 * - Memory footprint for large lists
 */
@Keep
data class JobListingSummary(
    val id: String = "",
    val jobId: String = "",
    val employerId: String = "",
    val title: String = "",
    val companyName: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val payAmount: String = "",
    val payType: String = "",
    val category: String = "",
    val jobType: String = "",
    val vacancies: Int = 0,
    val postedAt: Long = 0L,
    val urgency: String = "",
    val employerTrustTier: String = "VERIFIED",
    val jobImageUrl: String = "",
    val isFilled: Boolean = false,
    val isSaved: Boolean = false,
    val distance: Double? = null
) {
    /**
     * Get formatted pay display text
     */
    fun getPayDisplayText(): String = "₹$payAmount"
    
    /**
     * Get formatted time ago display text
     * Uses centralized DateTimeUtils to avoid code duplication
     */
    fun getTimeAgoDisplayText(): String = DateTimeUtils.formatTimeAgo(postedAt)
    
    /**
     * Check if job is urgent for highlighting
     */
    fun isUrgent(): Boolean = urgency == "URGENT" || urgency == "IMMEDIATE"
    
    /**
     * Convert to full JobListing (for backward compatibility during transition)
     * Note: This creates a partial JobListing - full data should be fetched from Firestore
     */
    fun toJobListing(): JobListing = JobListing(
        id = id,
        jobId = jobId,
        employerId = employerId,
        title = title,
        companyName = companyName,
        location = location,
        latitude = latitude,
        longitude = longitude,
        payAmount = payAmount,
        payType = payType,
        category = category,
        jobType = jobType,
        vacancies = vacancies,
        postedAt = postedAt,
        urgency = urgency,
        employerTrustTier = employerTrustTier,
        jobImageUrl = jobImageUrl,
        isFilled = isFilled,
        isSaved = isSaved,
        distance = distance
    )
    
    companion object {
        /**
         * Fields to select from Firestore for summary queries
         * Use with .select() to minimize data transfer
         */
        val SUMMARY_FIELDS = listOf(
            "jobId", "employerId", "title", "companyName", "location",
            "latitude", "longitude", "payAmount", "payType", "category",
            "jobType", "vacancies", "createdAt", "urgency", "employerTrustTier",
            "jobImageUrl", "isFilled"
        )
        
        /**
         * Create JobListingSummary from Firestore document map
         */
        fun fromMap(data: Map<String, Any>): JobListingSummary {
            return JobListingSummary(
                id = data["jobId"] as? String ?: "",
                jobId = data["jobId"] as? String ?: "",
                employerId = data["employerId"] as? String ?: "",
                title = data["title"] as? String ?: "",
                companyName = data["companyName"] as? String ?: "",
                location = data["location"] as? String ?: "",
                latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
                payAmount = data["payAmount"] as? String ?: "",
                payType = data["payType"] as? String ?: "",
                category = data["category"] as? String ?: "",
                jobType = data["jobType"] as? String ?: "",
                vacancies = (data["vacancies"] as? Number)?.toInt() ?: 0,
                postedAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                urgency = data["urgency"] as? String ?: "",
                employerTrustTier = data["employerTrustTier"] as? String ?: "VERIFIED",
                jobImageUrl = data["jobImageUrl"] as? String ?: "",
                isFilled = data["isFilled"] as? Boolean ?: false
            )
        }
        
        /**
         * Create JobListingSummary from full JobListing
         */
        fun fromJobListing(job: JobListing): JobListingSummary {
            return JobListingSummary(
                id = job.id,
                jobId = job.jobId.ifEmpty { job.id },
                employerId = job.employerId,
                title = job.title,
                companyName = job.companyName,
                location = job.location,
                latitude = job.latitude,
                longitude = job.longitude,
                payAmount = job.payAmount,
                payType = job.payType,
                category = job.category,
                jobType = job.jobType,
                vacancies = job.vacancies,
                postedAt = job.postedAt,
                urgency = job.urgency,
                employerTrustTier = job.employerTrustTier,
                jobImageUrl = job.jobImageUrl,
                isFilled = job.isFilled,
                isSaved = job.isSaved,
                distance = job.distance
            )
        }
    }
}
