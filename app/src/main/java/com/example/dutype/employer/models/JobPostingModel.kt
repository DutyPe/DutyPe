package com.example.dutype.employer.models

import com.example.dutype.utils.DateTimeUtils
import java.util.UUID

/**
 * JobPostingModel - For EMPLOYERS to POST jobs
 * This is the minimal data needed when an employer creates a job posting
 * Focus: Essential fields for job creation, no worker-specific data
 */
data class JobPostingModel(
    val jobId: String = UUID.randomUUID().toString(),
    val title: String,                              // Job Title (Cook, Driver, Helper)
    val payAmount: String,                          // 400, 10,000, etc.
    val payType: PayType,                           // DAILY, HOURLY, MONTHLY, TASK
    val location: String,                           // Area/Street/City
    val description: String,                        // Short description
    val contactNumber: String,                      // Direct contact
    val category: JobCategory,                      // Category (Cook, Maid, Driver, etc.)
    val shiftTiming: ShiftTiming = ShiftTiming.FLEXIBLE,
    val urgency: JobUrgency = JobUrgency.FLEXIBLE,
    val vacancies: Int = 1,                         // Default 1
    val postedTime: Long = System.currentTimeMillis(),
    val isVerified: Boolean = false,                // From employer verification
    val employerId: String? = null,
    val employerName: String = "",
    val isActive: Boolean = true,                   // For card display
    val applicationsReceived: Int = 0,              // For card display
    val isFilled: Boolean = false                   // For vacancy status
) {
    /**
     * Check if job posting is complete and ready to publish.
     * @return `true` if all essential fields are filled, `false` otherwise.
     */
    fun isReadyToPublish(): Boolean {
        return title.isNotBlank() &&
                payAmount.isNotBlank() &&
                location.isNotBlank() &&
                description.isNotBlank() &&
                contactNumber.isNotBlank()
    }

    /**
     * Get completion percentage for the job posting.
     * This can be used to show a progress bar to the employer.
     * @return An integer between 0 and 100.
     */
    fun getCompletionPercentage(): Int {
        var completed = 0
        val total = 6

        if (title.isNotBlank()) completed++
        if (payAmount.isNotBlank()) completed++
        if (location.isNotBlank()) completed++
        if (description.isNotBlank()) completed++
        if (contactNumber.isNotBlank()) completed++
        if (vacancies > 0) completed++

        return (completed * 100) / total
    }

    /**
     * Get a formatted "time ago" string representing how long ago the job was posted.
     * e.g., "Just now", "5m ago", "3h ago", "2d ago", "4w ago".
     * @return A formatted string.
     */
    fun getTimeAgo(): String = DateTimeUtils.formatRelativeTime(postedTime)

    // Helper properties for backward compatibility with the card
    val emoji: String get() = category.icon
    val id: String get() = jobId // Backward compatibility

    /**
     * Returns a display string with an icon and title for the job.
     * Example: "👨‍🍳 Cook"
     */
    fun getDisplayText(): String = "${category.icon} $title"

    // PayInfo wrapper for compatibility
    val payInfo = PayInfoWrapper(payAmount, payType)

    // LocationInfo wrapper for compatibility
    val locationInfo = LocationWrapper(location)

    /**
     * A wrapper for pay-related information for display purposes.
     */
    data class PayInfoWrapper(
        val amount: String,
        val type: PayType,
        val isNegotiable: Boolean = false
    ) {
        /**
         * Returns a formatted string for pay information.
         * Example: "₹400 Daily"
         */
        fun getDisplayText(): String = "₹$amount ${type.displayName}"
    }

    /**
     * A wrapper for location information for display purposes.
     */
    data class LocationWrapper(
        val area: String
    ) {
        /**
         * Returns the location area.
         */
        fun getDisplayText(): String = area
    }
}
