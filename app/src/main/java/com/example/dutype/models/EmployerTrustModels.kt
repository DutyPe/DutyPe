package com.example.dutype.models

/**
 * Employer Trust Tier System
 * 
 * Simplified 3-Tier System for Hyper-Local Gig Economy:
 * - ✅ VERIFIED: Phone OTP + Selfie verified (default after profile setup)
 * - ⭐ TRUSTED: 10+ completed jobs + 4.0+ rating
 * - 🏢 BUSINESS: GST verified
 * 
 * Flow:
 * 1. User signs up with Phone OTP → Phone verified
 * 2. User completes profile with Selfie → Gets VERIFIED badge (Blue)
 * 3. After 10+ completed jobs with 4.0+ rating → Auto-upgrades to TRUSTED (Gold)
 * 4. If GST provided and valid → Gets BUSINESS badge (Purple)
 */

enum class EmployerTrustTier {
    VERIFIED,   // Phone + Selfie verified (default after profile setup)
    TRUSTED,    // 10+ completed jobs + 4.0+ rating
    BUSINESS    // GST/Business verified
}

/**
 * Trust tier display information
 */
data class TrustTierInfo(
    val tier: EmployerTrustTier,
    val displayName: String,
    val emoji: String,
    val description: String,
    val color: Long, // ARGB color
    val backgroundColor: Long
)

/**
 * Get display info for each trust tier
 */
fun EmployerTrustTier.getDisplayInfo(): TrustTierInfo {
    return when (this) {
        EmployerTrustTier.VERIFIED -> TrustTierInfo(
            tier = this,
            displayName = "Verified",
            emoji = "✅",
            description = "Phone & selfie verified",
            color = 0xFF3B82F6, // Blue
            backgroundColor = 0xFFEFF6FF
        )
        EmployerTrustTier.TRUSTED -> TrustTierInfo(
            tier = this,
            displayName = "Trusted",
            emoji = "⭐",
            description = "10+ jobs completed, 4.0+ rating",
            color = 0xFFF59E0B, // Amber/Gold
            backgroundColor = 0xFFFEF3C7
        )
        EmployerTrustTier.BUSINESS -> TrustTierInfo(
            tier = this,
            displayName = "Business",
            emoji = "🏢",
            description = "GST verified business",
            color = 0xFF8B5CF6, // Purple
            backgroundColor = 0xFFF3E8FF
        )
    }
}

/**
 * Employer verification status
 */
data class EmployerVerificationStatus(
    val isPhoneVerified: Boolean = true, // Always true after OTP signup
    val isSelfieVerified: Boolean = false,
    val isGstVerified: Boolean = false,
    val gstNumber: String? = null,
    val completedJobsCount: Int = 0,
    val averageRating: Float = 0f,
    val totalRatings: Int = 0,
    val trustTier: EmployerTrustTier = EmployerTrustTier.VERIFIED
)

/**
 * Calculate trust tier based on verification status
 */
fun calculateTrustTier(status: EmployerVerificationStatus): EmployerTrustTier {
    return when {
        // Business tier: GST verified (highest priority)
        status.isGstVerified && status.gstNumber?.isNotBlank() == true -> EmployerTrustTier.BUSINESS
        
        // Trusted tier: 10+ completed jobs AND 4.0+ rating
        status.completedJobsCount >= 10 && status.averageRating >= 4.0f && status.totalRatings >= 5 -> EmployerTrustTier.TRUSTED
        
        // Verified tier: Default (everyone who completes profile gets this)
        else -> EmployerTrustTier.VERIFIED
    }
}

/**
 * Parse trust tier from string (Firestore)
 */
fun parseTrustTier(tierString: String?): EmployerTrustTier {
    return when (tierString?.uppercase()) {
        "BUSINESS" -> EmployerTrustTier.BUSINESS
        "TRUSTED" -> EmployerTrustTier.TRUSTED
        else -> EmployerTrustTier.VERIFIED // Default for everyone
    }
}

/**
 * GST Number validation for Indian businesses
 * Format: 22AAAAA0000A1Z5 (15 characters)
 */
fun isValidGstNumber(gst: String): Boolean {
    if (gst.isBlank()) return false
    val gstRegex = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
    return gst.uppercase().matches(gstRegex)
}

/**
 * Format GST number for display
 */
fun formatGstNumber(gst: String): String {
    val cleaned = gst.uppercase().replace(" ", "")
    return if (cleaned.length == 15) {
        "${cleaned.substring(0, 2)} ${cleaned.substring(2, 7)} ${cleaned.substring(7, 11)} ${cleaned.substring(11, 12)} ${cleaned.substring(12, 13)} ${cleaned.substring(13, 14)} ${cleaned.substring(14, 15)}"
    } else {
        cleaned
    }
}
