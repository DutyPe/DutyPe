package com.example.dutype.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * JobListing - Base model for all job-related data
 * This is the main model used by the backend and frontend
 * It contains all possible fields for different use cases
 */
@Entity(tableName = "joblisting")
data class JobListing(
    @PrimaryKey
    val id: String = "",
    val jobId: String = "",
    val employerId: String = "",
    val title: String = "",
    val companyName: String = "",
    val company: String = "",
    val location: String = "",
    val specificLocation: String = "",
    val locationNearby: String = "",
    val area: String? = null,
    val city: String? = null,
    val payRate: Double = 0.0,
    val payAmount: String = "",
    val payType: String = "",
    val payPeriod: String = "",
    val timing: String = "",
    val shiftTiming: String = "",
    val description: String = "",
    val preferences: List<String> = emptyList(),
    val benefits: List<String> = emptyList(),
    val requirements: List<String> = emptyList(),
    val skills: List<String> = emptyList(),
    val vacancies: Int = 0,
    val isActive: Boolean = true,
    val isTrending: Boolean = false,
    val isRemote: Boolean = false,
    val isVerified: Boolean = false,
    val isSaved: Boolean = false, // Worker-specific: whether this job is saved by current user
    // Note: isBookmarked and isApplied are worker-specific and handled separately
    val postedAt: Long = 0L,
    val postedTime: String = "",
    val postedDate: String = "",
    val imageUrl: String = "",
    val phoneNumber: String = "",
    val contactNumber: String = "",
    val contactInfo: String = "",
    val category: String = "",
    val jobType: String = "",
    val experienceLevel: String = "",
    val experienceRequired: String = "",
    val workingHours: String = "",
    val applicationDeadline: String = "",
    val ageRange: String = "",
    val gender: String = "",
    val companySize: String = "",
    val industry: String = "",
    val viewCount: Long = 0L,
    val applicationCount: Long = 0L,
    val distance: Double? = null,
    val salary: String = "",
    val urgency: String = ""
) {
    /**
     * Get formatted pay display text
     * Example: "₹400 Daily"
     */
    fun getPayDisplayText(): String = "₹$payAmount"
    
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
     * Check if job is urgent for highlighting
     */
    fun isUrgent(): Boolean = urgency == "URGENT" || urgency == "IMMEDIATE"
    
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
     * Get shareable text for job sharing
     */
    fun getShareableText(): String {
        return """
🚀 *${title}* at *${companyName}*

💰 ${getPayDisplayText()}
📍 ${getLocationDisplayText()}
⏰ ${shiftTiming}
🏷️ ${category}

📝 *Description:*
${description.take(200)}${if (description.length > 200) "..." else ""}

📞 Contact: ${contactNumber}

💼 Apply now through dutype App!
        """.trimIndent()
    }
}
