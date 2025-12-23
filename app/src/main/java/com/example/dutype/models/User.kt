package com.example.dutype.models

import androidx.annotation.Keep

@Keep // Add this annotation to prevent R8 from removing fields
data class User(
    val id: String = "", // Firebase UID (unique, permanent)
    val email: String = "", // From Google (unique)
    val password: String? = null, // Only for registration/login
    val fullName: String = "",
    // Phone - Firestore uses "phone", keep "phoneNumber" for backward compatibility
    val phone: String? = null,
    val phoneNumber: String? = null,
    val profileImageUrl: String? = null,
    val role: UserRole = UserRole.WORKER,
    val isProfileComplete: Boolean = false,
    val profileCompleted: Boolean = false, // Firestore uses this
    val completedAt: Long? = null,
    val isVerified: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    
    // Profile information - Firestore uses "address", keep "location" for backward compatibility
    val bio: String? = null,
    val address: String? = null,
    val location: String? = null,
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
    
    // Notification preferences
    val emailNotifications: Boolean = true,
    val pushNotifications: Boolean = true,
    val smsNotifications: Boolean = false
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

data class UserPreferences(
    val notificationsEnabled: Boolean = true,
    val emailNotifications: Boolean = true,
    val pushNotifications: Boolean = true,
    val smsNotifications: Boolean = false,
    val locationSharing: Boolean = false,
    val profileVisibility: ProfileVisibility = ProfileVisibility.PUBLIC,
    val language: String = "en",
    val theme: String = "light"
)

enum class ProfileVisibility {
    PUBLIC,
    PRIVATE,
    FRIENDS_ONLY
}
