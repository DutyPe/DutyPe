package com.example.partimes.employer.models

import com.example.partimes.employer.models.enums.JobCategory
import com.example.partimes.employer.models.enums.PayType
import com.example.partimes.employer.models.enums.ShiftTiming
import com.example.partimes.employer.models.enums.JobUrgency

/**
 * EmployerJobListingModel - For EMPLOYERS to VIEW their posted jobs
 * This model is optimized for displaying job cards in the employer dashboard
 * Focus: Employer-specific data with analytics and management features
 */
data class EmployerJobListingModel(
    val jobId: String,
    val title: String,
    val payAmount: String,
    val payType: PayType,
    val location: String,
    val category: JobCategory,
    val shiftTiming: ShiftTiming,
    val urgency: JobUrgency,
    val description: String,
    val contactNumber: String,
    val vacancies: Int,
    val postedAt: Long,
    val employerId: String,
    val employerName: String,
    val isActive: Boolean = true,
    val isVerified: Boolean = false,
    val isTrending: Boolean = false,
    val isRemote: Boolean = false,
    
    // Employer-specific analytics
    val viewCount: Int = 0,
    val applicationCount: Int = 0,
    val applicationsReceived: Int = 0,
    val shortlistedCount: Int = 0,
    val selectedCount: Int = 0,
    val rejectedCount: Int = 0,
    
    // Additional fields
    val workingHours: String = "",
    val experienceRequired: String = "Entry Level",
    val ageRange: String = "",
    val gender: String = "Any",
    val jobType: String = "Part-time",
    val applicationDeadline: String = "",
    val companySize: String = "",
    val industry: String = "",
    val imageUrl: String = "",
    val benefits: List<String> = emptyList(),
    val requirements: List<String> = emptyList()
) {
    /**
     * Get formatted pay display text
     * Example: "₹400 Daily"
     */
    fun getPayDisplayText(): String = "₹$payAmount ${payType.displayName}"
    
    /**
     * Get formatted location display text
     * Example: "Nallagandla Road, Nallagandla"
     */
    fun getLocationDisplayText(): String = location
    
    /**
     * Get formatted time ago display text
     * Example: "2h ago", "1d ago"
     */
    fun getTimeAgoDisplayText(): String {
        val currentTime = System.currentTimeMillis()
        val diffInMillis = currentTime - postedAt
        val diffInSeconds = diffInMillis / 1000
        val diffInMinutes = diffInSeconds / 60
        val diffInHours = diffInMinutes / 60
        val diffInDays = diffInHours / 24
        val diffInWeeks = diffInDays / 7

        return when {
            diffInSeconds < 60 -> "Just now"
            diffInMinutes < 60 -> "${diffInMinutes}m ago"
            diffInHours < 24 -> "${diffInHours}h ago"
            diffInDays < 7 -> "${diffInDays}d ago"
            else -> "${diffInWeeks}w ago"
        }
    }
    
    /**
     * Get category icon for display
     */
    fun getCategoryIcon(): String = category.icon
    
    /**
     * Get urgency display text
     */
    fun getUrgencyDisplayText(): String = urgency.displayName
    
    /**
     * Get shift timing display text
     */
    fun getShiftTimingDisplayText(): String = shiftTiming.displayName
    
    /**
     * Check if job is urgent for highlighting
     */
    fun isUrgent(): Boolean = urgency == JobUrgency.URGENT || urgency == JobUrgency.IMMEDIATE
    
    /**
     * Get job status display text
     */
    fun getJobStatusDisplayText(): String = when {
        !isActive -> "Inactive"
        isUrgent() -> "Urgent"
        isTrending -> "Trending"
        else -> "Active"
    }
    
    /**
     * Get application rate (applications per view)
     */
    fun getApplicationRate(): Double = if (viewCount > 0) {
        (applicationCount.toDouble() / viewCount.toDouble()) * 100
    } else 0.0
    
    /**
     * Get formatted application rate display text
     */
    fun getApplicationRateDisplayText(): String = "${String.format("%.1f", getApplicationRate())}%"
    
    /**
     * Get selection rate (selected per application)
     */
    fun getSelectionRate(): Double = if (applicationCount > 0) {
        (selectedCount.toDouble() / applicationCount.toDouble()) * 100
    } else 0.0
    
    /**
     * Get formatted selection rate display text
     */
    fun getSelectionRateDisplayText(): String = "${String.format("%.1f", getSelectionRate())}%"
    
    /**
     * Check if job needs attention (low applications, high views)
     */
    fun needsAttention(): Boolean = viewCount > 10 && applicationCount < 2
    
    /**
     * Get job performance status
     */
    fun getJobPerformanceStatus(): String = when {
        applicationCount == 0 && viewCount > 5 -> "Needs Attention"
        applicationCount > 10 -> "High Interest"
        applicationCount > 5 -> "Good Interest"
        applicationCount > 0 -> "Low Interest"
        else -> "No Interest"
    }
    
    /**
     * Get formatted benefits display text
     */
    fun getBenefitsDisplayText(): String = benefits.joinToString(", ")
    
    /**
     * Get formatted requirements display text
     */
    fun getRequirementsDisplayText(): String = requirements.joinToString(", ")
    
    /**
     * Get job summary for employer dashboard
     */
    fun getJobSummary(): String = "$title - ${getPayDisplayText()} - $location"
    
    /**
     * Get analytics summary
     */
    fun getAnalyticsSummary(): String = "$viewCount views, $applicationCount applications, ${getApplicationRateDisplayText()} rate"
}
