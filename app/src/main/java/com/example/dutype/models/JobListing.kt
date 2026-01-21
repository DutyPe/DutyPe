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
    val location: String = "",
    val area: String? = null,
    val city: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val payRate: Double = 0.0,
    val payAmount: String = "",
    val payType: String = "",
    val shiftTiming: String = "",
    val description: String = "",
    val benefits: List<String> = emptyList(),
    val requirements: List<String> = emptyList(),
    val vacancies: Int = 0,
    val isActive: Boolean = true,
    val isVerified: Boolean = false,
    val postedAt: Long = 0L,
    val contactNumber: String = "",
    val category: String = "",
    val jobType: String = "",
    val experienceRequired: String = "",
    val ageRange: String = "",
    val gender: String = "",
    val applicationCount: Long = 0L,
    val landmark: String = "",
    val urgency: String = "",
    val employerCreatedAt: Long? = null,
    val employerPaidOnTimePercentage: Int? = null,
    val isFilled: Boolean = false,
    val employerTrustTier: String = "VERIFIED",
    val jobImageUrl: String = "",
    val expiresAt: Long = 0L,
    val expiryDays: Int = 15,
    
    // Runtime/computed fields (not stored in Firestore, computed on client)
    var distance: Double? = null, // Computed based on user location
    var isSaved: Boolean = false // User-specific, managed separately
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
     * Get formatted time ago with exact days (no weeks)
     * Example: "2 hours ago", "15 days ago"
     * Used in JobDescriptionScreen for detailed view
     */
    fun getTimeAgoExactDays(): String = DateTimeUtils.formatTimeAgoExactDays(postedAt)
    
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
