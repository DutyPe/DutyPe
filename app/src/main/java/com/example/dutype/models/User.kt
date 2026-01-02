package com.example.dutype.models

import androidx.annotation.Keep

/**
 * User - Core user model for authentication and profile data
 * 
 * NOTE: Some fields are deprecated but kept for Firestore backward compatibility.
 * Use the canonical field names in new code.
 */
@Keep // Add this annotation to prevent R8 from removing fields
data class User(
    val id: String = "", // Firebase UID (unique, permanent)
    val email: String = "", // From Google (unique)
    val password: String? = null, // Only for registration/login
    val fullName: String = "",
    // Phone - Firestore uses "phone", keep "phoneNumber" for backward compatibility
    val phone: String? = null,
    @Deprecated("Use phone instead", ReplaceWith("phone"))
    val phoneNumber: String? = null, // Legacy field - use phone
    val profileImageUrl: String? = null,
    val role: UserRole = UserRole.WORKER,
    @Deprecated("Use profileCompleted instead", ReplaceWith("profileCompleted"))
    val isProfileComplete: Boolean = false, // Legacy field - use profileCompleted
    val profileCompleted: Boolean = false, // Canonical field
    val completedAt: Long? = null,
    val isVerified: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null, // Firestore field
    
    // Profile information - Firestore uses "address", keep "location" for backward compatibility
    val bio: String? = null,
    val address: String? = null, // Canonical field
    @Deprecated("Use address instead", ReplaceWith("address"))
    val location: String? = null, // Legacy field - use address
    val dateOfBirth: String? = null,
    val gender: String? = null,
    
    // Worker specific fields
    val skills: String? = null, // Comma-separated string in Firestore
    val experience: String? = null,
    val education: String? = null,
    val resumeUrl: String? = null,
    val coverLetter: String? = null,
    
    // Employer specific fields
    val companyName: String? = null,
    val companyDescription: String? = null,
    val companyWebsite: String? = null,
    val companyLogoUrl: String? = null,
    val industry: String? = null,
    val companySize: String? = null,
    
    // Employer Trust & Verification fields
    val gstNumber: String? = null,           // GST number for business verification
    val isGstVerified: Boolean = false,      // GST verification status
    val trustTier: String = "NEW",           // NEW, VERIFIED, TRUSTED, BUSINESS
    val completedJobsCount: Int = 0,         // Number of completed jobs
    val isSelfieVerified: Boolean = false,   // Selfie verification status
    
    // Notification preferences
    val emailNotifications: Boolean = true,
    val pushNotifications: Boolean = true,
    val smsNotifications: Boolean = false,
    
    // FCM Token fields (Firestore)
    val fcmToken: String? = null,
    val fcmTokenUpdatedAt: Long? = null,
    val platform: String? = null
) {
    // Get phone - prioritize "phone" (Firestore field), fallback to "phoneNumber"
    fun getPhoneDisplay(): String? = phone ?: phoneNumber
    
    // Get address - prioritize "address" (Firestore field), fallback to "location"
    fun getAddressDisplay(): String? = address ?: location
    
    // Get skills as list
    fun getSkillsList(): List<String> =
        skills?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
}

enum class UserRole {
    WORKER,
    EMPLOYER,
    ADMIN
}
