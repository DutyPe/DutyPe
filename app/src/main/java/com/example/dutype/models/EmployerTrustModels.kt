package com.example.dutype.models

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

/**
 * Employer Trust Tier System
 * 
 * Simplified trust system for hyper-local hiring:
 * - ✅ VERIFIED: Phone OTP + Selfie verified (default after profile setup)
 * - ⭐ TRUSTED: 10+ completed jobs + 4.0+ rating
 * 
 * Flow:
 * 1. User signs up with Phone OTP → Phone verified
 * 2. User completes profile with Selfie → Gets VERIFIED badge (Blue)
 * 3. After 10+ completed jobs with 4.0+ rating → Auto-upgrades to TRUSTED (Gold)
 */

enum class EmployerTrustTier {
    VERIFIED,   // Phone + Selfie verified (default after profile setup)
    TRUSTED     // 10+ completed jobs + 4.0+ rating
}

/**
 * Trust tier display information
 */
@Keep
@Immutable
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
    }
}

/**
 * Employer verification status
 */
@Keep
data class EmployerVerificationStatus(
    val isPhoneVerified: Boolean = true, // Always true after OTP signup
    val isSelfieVerified: Boolean = false,
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
        "TRUSTED" -> EmployerTrustTier.TRUSTED
        else -> EmployerTrustTier.VERIFIED // Default for everyone
    }
}
