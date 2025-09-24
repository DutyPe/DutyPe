package com.example.partimes.models

data class User(
    val id: String? = null,
    val email: String,
    val password: String? = null, // Only for registration/login
    val fullName: String,
    val phoneNumber: String? = null,
    val role: UserRole = UserRole.JOBSEEKER,
    val isVerified: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    
    // Profile information
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    
    // Jobseeker specific fields
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
    JOBSEEKER,
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
