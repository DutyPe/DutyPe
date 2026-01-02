package com.example.dutype.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.dutype.utils.DateTimeUtils

/**
 * JobListing - Base model for all job-related data
 * This is the main model used by the backend and frontend
 * It contains all possible fields for different use cases
 * 
 * NOTE: Some fields are deprecated but kept for Firestore backward compatibility.
 * Use the canonical field names in new code.
 */
@Entity(tableName = "joblisting")
data class JobListing(
    @PrimaryKey
    val id: String = "",
    val jobId: String = "",
    val employerId: String = "",
    val title: String = "",
    val companyName: String = "",
    @Deprecated("Use companyName instead", ReplaceWith("companyName"))
    val company: String = "", // Legacy field - use companyName
    val location: String = "",
    val specificLocation: String = "",
    val locationNearby: String = "",
    val area: String? = null,
    val city: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
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
    @Deprecated("Use postedAt (Long timestamp) instead", ReplaceWith("postedAt"))
    val postedTime: String = "", // Legacy field - use postedAt
    @Deprecated("Use postedAt (Long timestamp) instead", ReplaceWith("postedAt"))
    val postedDate: String = "", // Legacy field - use postedAt
    val imageUrl: String = "",
    @Deprecated("Use contactNumber instead", ReplaceWith("contactNumber"))
    val phoneNumber: String = "", // Legacy field - use contactNumber
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
    val applicationCount: Long = 0L,
    val distance: Double? = null,
    // ACCESSIBILITY: Landmark Navigation - helps workers find location by landmarks
    val landmark: String = "",
    val salary: String = "",
    val urgency: String = "",
    // Employer status fields
    val employerCreatedAt: Long? = null,
    val employerPaidOnTimePercentage: Int? = null,
    val isFilled: Boolean = false,
    // Employer Trust Tier (VERIFIED, TRUSTED, BUSINESS)
    val employerTrustTier: String = "VERIFIED",
    // Job Image uploaded by employer (optional)
    val jobImageUrl: String = "",
    // Job Expiry System
    val expiresAt: Long = 0L, // Timestamp when job expires (0 = no expiry)
    val expiryDays: Int = 15 // Default 15 days expiry
) {
    /**
     * Check if job is expired
     */
    fun isExpired(): Boolean {
        if (expiresAt == 0L) return false
        return System.currentTimeMillis() > expiresAt
    }
    
    /**
     * Get days until expiry
     */
    fun getDaysUntilExpiry(): Int {
        if (expiresAt == 0L) return -1 // No expiry set
        val remainingMillis = expiresAt - System.currentTimeMillis()
        if (remainingMillis <= 0) return 0
        return (remainingMillis / (24 * 60 * 60 * 1000)).toInt()
    }
    
    /**
     * Get expiry status text
     */
    fun getExpiryStatusText(): String {
        val daysLeft = getDaysUntilExpiry()
        return when {
            daysLeft < 0 -> "" // No expiry
            daysLeft == 0 -> "Expires today"
            daysLeft == 1 -> "Expires tomorrow"
            daysLeft <= 3 -> "Expires in $daysLeft days"
            daysLeft <= 7 -> "Expires this week"
            else -> ""
        }
    }
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
     * Uses centralized DateTimeUtils to avoid code duplication
     */
    fun getTimeAgoDisplayText(): String = DateTimeUtils.formatTimeAgo(postedAt)
    
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
        val playStoreUrl = "https://play.google.com/store/apps/details?id=com.dutype.app"
        return """
🚀 *${title}* at *${companyName}*

💰 ${getPayDisplayText()}
📍 ${getLocationDisplayText()}
⏰ ${shiftTiming}
🏷️ ${category}

📝 *Description:*
${description.take(200)}${if (description.length > 200) "..." else ""}

📞 Contact: ${contactNumber}

💼 Apply now through DutyPe App!
📲 Download: $playStoreUrl
        """.trimIndent()
    }
}
