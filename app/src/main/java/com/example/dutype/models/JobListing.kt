package com.example.dutype.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.dutype.utils.DateTimeUtils

/**
 * JobListing - Simplified model for job postings
 * Removed deprecated fields for cleaner data structure
 * 
 * Changes from previous version:
 * - Removed: area, city (use location only)
 * - Removed: payRate (use payAmount only)
 * - Removed: contactNumber (fetch from employer profile)
 * - Removed: category (auto-detected from title/description)
 * - Removed: experienceRequired (include in requirements list)
 * - Removed: isVerified, employerTrustTier, employerPaidOnTimePercentage (in employer profile)
 * - Removed: expiresAt (calculated from postedAt + expiryDays)
 */
@Entity(tableName = "joblisting")
data class JobListing(
    @PrimaryKey
    val id: String = "",
    val jobId: String = "",
    val employerId: String = "",
    val title: String = "",
    val companyName: String = "",
    val location: String = "", // Full address for display
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val payAmount: String = "", // e.g., "400", "15000"
    val payType: String = "", // "HOURLY", "DAILY", "MONTHLY"
    val shiftTiming: String = "",
    val description: String = "",
    val benefits: List<String> = emptyList(),
    val requirements: List<String> = emptyList(), // Includes experience requirements
    val vacancies: Int = 0,
    val isActive: Boolean = true,
    val postedAt: Long = 0L,
    val contactNumber: String = "", // Employer contact number for this job
    val jobType: String = "", // "FULL_TIME", "PART_TIME", "CONTRACT"
    val ageRange: String = "",
    val gender: String = "",
    val applicationCount: Long = 0L,
    val landmark: String = "",
    val urgency: String = "",
    val isFilled: Boolean = false,
    val jobImageUrl: String = "",
    val expiryDays: Int = 30, // Default 30 days - used to calculate expiry
    
    // Runtime/computed fields (not stored in Firestore, computed on client)
    var distance: Double? = null, // Computed based on user location
    var isSaved: Boolean = false // User-specific, managed separately
) {
    /**
     * Get auto-detected category from title and description
     */
    fun getCategory(): String {
        return com.example.dutype.utils.CategoryDetector.detectCategory(title, description)
    }
    
    /**
     * Get calculated expiry timestamp (postedAt + expiryDays)
     */
    fun getExpiresAt(): Long {
        if (postedAt == 0L) return 0L
        return postedAt + (expiryDays * 24 * 60 * 60 * 1000L)
    }
    
    /**
     * Check if job is expired
     */
    fun isExpired(): Boolean {
        val expiryTime = getExpiresAt()
        if (expiryTime == 0L) return false
        return System.currentTimeMillis() > expiryTime
    }
    
    /**
     * Get days until expiry
     */
    fun getDaysUntilExpiry(): Int {
        val expiryTime = getExpiresAt()
        if (expiryTime == 0L) return -1 // No expiry set
        val remainingMillis = expiryTime - System.currentTimeMillis()
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
     * Get shareable text for job sharing with deep link
     */
    fun getShareableText(contactNumber: String): String {
        val jobDeepLink = com.example.dutype.utils.DeepLinkHandler.generateJobWebLink(id)
        val playStoreUrl = "https://play.google.com/store/apps/details?id=com.dutype.app"
        return """
🚀 *${title}* at *${companyName}*

💰 ${getPayDisplayText()}
📍 ${getLocationDisplayText()}
⏰ ${shiftTiming}
🏷️ ${getCategory()}

📝 *Description:*
${description.take(200)}${if (description.length > 200) "..." else ""}

📞 Contact: ${contactNumber}

👉 View & Apply Now:
$jobDeepLink

💼 Download DutyPe App:
📲 $playStoreUrl
        """.trimIndent()
    }
}
