package com.example.partimes.jobseeker.models

import com.example.partimes.employer.models.enums.JobCategory
import com.example.partimes.employer.models.enums.PayType
import com.example.partimes.employer.models.enums.ShiftTiming
import com.example.partimes.employer.models.enums.JobUrgency

/**
 * JobDetailsModel - For detailed job view (when clicking on job card)
 * This model contains complete job information for the job details screen
 * Focus: Complete job data with all details for jobseeker decision making
 */
data class JobDetailsModel(
    val jobId: String,
    val title: String,
    val employerName: String,
    val employerId: String,
    val payAmount: String,
    val payType: PayType,
    val location: String,
    val specificLocation: String,
    val area: String,
    val city: String,
    val category: JobCategory,
    val shiftTiming: ShiftTiming,
    val urgency: JobUrgency,
    val description: String,                        // Full description
    val contactNumber: String,
    val phoneNumber: String,
    val contactInfo: String,
    val vacancies: Int,
    val postedAt: Long,
    val postedTime: String,
    val postedDate: String,
    val viewCount: Int = 0,
    val applicationCount: Int = 0,
    
    // Jobseeker-specific fields
    val isBookmarked: Boolean = false,
    val isApplied: Boolean = false,
    val applicationStatus: String = "NOT_APPLIED",
    val appliedAt: Long? = null,
    val bookmarkedAt: Long? = null,
    val personalNotes: String = "",
    
    // Additional detailed fields
    val benefits: List<String> = emptyList(),
    val requirements: List<String> = emptyList(),
    val skills: List<String> = emptyList(),
    val preferences: List<String> = emptyList(),
    val workingHours: String = "",
    val experienceRequired: String = "Entry Level",
    val experienceLevel: String = "Entry Level",
    val ageRange: String = "",
    val gender: String = "Any",
    val jobType: String = "Part-time",
    val applicationDeadline: String = "",
    val companySize: String = "",
    val industry: String = "",
    val companyDescription: String = "",
    val companyWebsite: String = "",
    val companyLogoUrl: String = "",
    val isVerified: Boolean = false,
    val isActive: Boolean = true,
    val isTrending: Boolean = false,
    val isRemote: Boolean = false,
    val imageUrl: String = "",
    val distance: Double? = null,
    
    // Additional metadata
    val salary: String = "",
    val payRate: Double = 0.0,
    val timing: String = "",
    val locationNearby: String = "",
    val companyName: String = "",
    val company: String = ""
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
     * Get formatted benefits display text
     */
    fun getBenefitsDisplayText(): String = benefits.joinToString(", ")
    
    /**
     * Get formatted requirements display text
     */
    fun getRequirementsDisplayText(): String = requirements.joinToString(", ")
    
    /**
     * Get formatted skills display text
     */
    fun getSkillsDisplayText(): String = skills.joinToString(", ")
    
    /**
     * Check if job is still accepting applications
     */
    fun isAcceptingApplications(): Boolean = isActive && !isApplied
    
    /**
     * Get application status display text
     */
    fun getApplicationStatusDisplayText(): String = when (applicationStatus) {
        "NOT_APPLIED" -> "Not Applied"
        "PENDING" -> "Application Pending"
        "VIEWED" -> "Application Viewed"
        "SHORTLISTED" -> "Shortlisted"
        "SELECTED" -> "Selected"
        "REJECTED" -> "Rejected"
        "INTERVIEW_SCHEDULED" -> "Interview Scheduled"
        "DOCUMENTS_PENDING" -> "Documents Pending"
        "HIRED" -> "Hired"
        "WITHDRAWN" -> "Withdrawn"
        else -> "Unknown"
    }
    
    /**
     * Get shareable text for job sharing
     */
    fun getShareableText(): String {
        return """
🚀 *${title}* at *${employerName}*

💰 ${getPayDisplayText()}
📍 ${getLocationDisplayText()}
⏰ ${getShiftTimingDisplayText()}
🏷️ ${category.displayName}
👥 ${vacancies} vacancy/vacancies

📝 *Description:*
${description}

✨ *Benefits:*
${getBenefitsDisplayText()}

📋 *Requirements:*
${getRequirementsDisplayText()}

📞 Contact: ${contactNumber}

💼 Apply now through ParTimes App!
        """.trimIndent()
    }
}
