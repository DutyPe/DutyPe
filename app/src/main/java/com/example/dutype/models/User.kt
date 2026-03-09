package com.example.dutype.models

import androidx.annotation.Keep

/**
 * User - MINIMAL CORE MODEL (Industry Standard)
 * Based on Urban Company, TaskRabbit, Fiverr patterns
 * 
 * DUAL-ROLE: Single account, multiple roles (Airbnb/Uber pattern)
 * 
 * Helper Functions:
 * - hasRole(role): Check if user has a specific role
 * - isDualRole(): Check if user has multiple roles
 * - getEnabledRoles(): Get list of enabled UserRole enums
 */
@Keep
data class User(
    // Core identity (5 fields)
    val id: String = "",
    val phone: String = "",
    val fullName: String = "",
    val profileImageUrl: String? = null,
    val email: String? = null,
    
    // Role management (2 fields)
    val roles: List<String> = listOf("WORKER"), // ["WORKER", "EMPLOYER"]
    val activeRole: UserRole = UserRole.WORKER,
    
    // Location (3 fields)
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    
    // Work Locations (1 field) - Industry standard (Uber, Swiggy, Zomato pattern)
    // Stores frequently used work locations for quick access
    val workLocations: List<WorkLocation> = emptyList(),
    
    // Profile (3 fields)
    val bio: String? = null,
    val skills: String? = null, // Comma-separated
    val experience: String? = null,
    
    // Employer fields (2 fields)
    val companyName: String? = null,
    val trustTier: String = "NEW", // NEW, BRONZE, SILVER, GOLD
    
    // System (4 fields)
    val fcmToken: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val profileCompleted: Boolean = false,
    
    // Referral System (2 fields) - OPTIMIZED: Moved from referral_stats collection
    val referralCode: String? = null,
    val referralStats: com.example.dutype.models.ReferralStats? = null,
    
    // Job Tracking (1 field) - Industry standard (LinkedIn, Indeed, Naukri pattern)
    val savedJobs: List<String> = emptyList() // List of saved job IDs
) {
    fun hasRole(role: UserRole): Boolean = roles.contains(role.name)
    fun isDualRole(): Boolean = roles.size > 1
    fun getEnabledRoles(): List<UserRole> = roles.mapNotNull { 
        try {
            UserRole.valueOf(it)
        } catch (e: Exception) {
            null
        }
    }
}

enum class UserRole {
    WORKER,
    EMPLOYER,
    ADMIN
}

/**
 * WorkLocation - Saved work location model
 * Industry standard pattern (Uber, Swiggy, Zomato, Google Maps)
 * Allows users to save frequently used work locations for quick access
 */
@Keep
data class WorkLocation(
    val id: String = "", // Unique ID for the location
    val label: String = "", // e.g., "Office", "Home", "Factory", "Shop"
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val addedAt: Long = System.currentTimeMillis(),
    val usageCount: Int = 0 // Track how often this location is used
)
