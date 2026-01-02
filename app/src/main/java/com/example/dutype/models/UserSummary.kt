package com.example.dutype.models

import androidx.annotation.Keep

/**
 * UserSummary - Lightweight model for user cards in list views
 * 
 * PERFORMANCE OPTIMIZATION: This model contains only the fields needed to render
 * a user card/avatar in lists. Full user profile is fetched only when viewing detail screen.
 * 
 * Use cases:
 * - Application list showing applicant info
 * - Chat list showing user avatars
 * - Employer viewing worker cards
 * - Worker viewing employer info on job cards
 */
@Keep
data class UserSummary(
    val id: String = "",
    val fullName: String = "",
    val profileImageUrl: String? = null,
    val role: UserRole = UserRole.WORKER,
    val phone: String? = null,
    val location: String? = null,
    val isVerified: Boolean = false,
    val createdAt: Long = 0L,
    
    // Worker-specific summary fields
    val skills: String? = null,
    val experience: String? = null,
    
    // Employer-specific summary fields
    val companyName: String? = null,
    val trustTier: String = "NEW",
    val completedJobsCount: Int = 0,
    val isSelfieVerified: Boolean = false
) {
    /**
     * Get display name - company name for employers, full name for workers
     */
    fun getDisplayName(): String = when (role) {
        UserRole.EMPLOYER -> companyName ?: fullName
        else -> fullName
    }
    
    /**
     * Get skills as list
     */
    fun getSkillsList(): List<String> =
        skills?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    
    /**
     * Get first 3 skills for preview
     */
    fun getSkillsPreview(): List<String> = getSkillsList().take(3)
    
    /**
     * Convert to full User (for backward compatibility during transition)
     * Note: This creates a partial User - full data should be fetched from Firestore
     */
    fun toUser(): User = User(
        id = id,
        fullName = fullName,
        profileImageUrl = profileImageUrl,
        role = role,
        phone = phone,
        location = location,
        isVerified = isVerified,
        createdAt = createdAt,
        skills = skills,
        experience = experience,
        companyName = companyName,
        trustTier = trustTier,
        completedJobsCount = completedJobsCount,
        isSelfieVerified = isSelfieVerified
    )
    
    companion object {
        /**
         * Fields to select from Firestore for worker summary queries
         */
        val WORKER_SUMMARY_FIELDS = listOf(
            "id", "fullName", "profileImageUrl", "role", "phone",
            "location", "isVerified", "createdAt", "skills", "experience"
        )
        
        /**
         * Fields to select from Firestore for employer summary queries
         */
        val EMPLOYER_SUMMARY_FIELDS = listOf(
            "id", "fullName", "profileImageUrl", "role", "phone",
            "location", "isVerified", "createdAt", "companyName",
            "trustTier", "completedJobsCount", "isSelfieVerified"
        )
        
        /**
         * Create UserSummary from Firestore document map
         */
        fun fromMap(data: Map<String, Any?>): UserSummary {
            val roleStr = data["role"] as? String ?: "WORKER"
            val role = try {
                UserRole.valueOf(roleStr.uppercase())
            } catch (e: Exception) {
                UserRole.WORKER
            }
            
            return UserSummary(
                id = data["id"] as? String ?: "",
                fullName = data["fullName"] as? String ?: "",
                profileImageUrl = data["profileImageUrl"] as? String,
                role = role,
                phone = data["phone"] as? String ?: data["phoneNumber"] as? String,
                location = data["address"] as? String ?: data["location"] as? String,
                isVerified = data["isVerified"] as? Boolean ?: false,
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
                skills = data["skills"] as? String,
                experience = data["experience"] as? String,
                companyName = data["companyName"] as? String,
                trustTier = data["trustTier"] as? String ?: "NEW",
                completedJobsCount = (data["completedJobsCount"] as? Number)?.toInt() ?: 0,
                isSelfieVerified = data["isSelfieVerified"] as? Boolean ?: false
            )
        }
        
        /**
         * Create UserSummary from full User
         */
        fun fromUser(user: User): UserSummary {
            return UserSummary(
                id = user.id,
                fullName = user.fullName,
                profileImageUrl = user.profileImageUrl,
                role = user.role,
                phone = user.getPhoneDisplay(),
                location = user.getAddressDisplay(),
                isVerified = user.isVerified,
                createdAt = user.createdAt,
                skills = user.skills,
                experience = user.experience,
                companyName = user.companyName,
                trustTier = user.trustTier,
                completedJobsCount = user.completedJobsCount,
                isSelfieVerified = user.isSelfieVerified
            )
        }
    }
}
