package com.example.dutype.models

data class User(
    val id: String = "", // Firebase UID (unique, permanent)
    val email: String = "", // From Google (unique)
    val password: String? = null, // Only for registration/login
    val fullName: String = "",
    val phoneNumber: String? = null, // For contact, not auth
    val profileImageUrl: String? = null, // From Google
    val role: UserRole = UserRole.WORKER,
    val isProfileComplete: Boolean = false,
    val isVerified: Boolean = true, // Google verified
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    
    // Profile information
    val bio: String? = null,
    val location: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    
    // Worker specific fields
    val skills: List<String>? = null,
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
)

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
