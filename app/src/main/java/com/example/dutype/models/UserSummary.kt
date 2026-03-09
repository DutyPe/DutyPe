package com.example.dutype.models

import androidx.annotation.Keep

/**
 * UserSummary - MINIMAL for list views (8 fields)
 */
@Keep
data class UserSummary(
    val id: String = "",
    val fullName: String = "",
    val profileImageUrl: String? = null,
    val phone: String = "",
    val address: String = "",
    val skills: String? = null,
    val companyName: String? = null,
    val trustTier: String = "NEW"
) {
    companion object {
        fun fromUser(user: User): UserSummary = UserSummary(
            id = user.id,
            fullName = user.fullName,
            profileImageUrl = user.profileImageUrl,
            phone = user.phone,
            address = user.address,
            skills = user.skills,
            companyName = user.companyName,
            trustTier = user.trustTier
        )
        
        fun fromMap(data: Map<String, Any>): UserSummary = UserSummary(
            id = data["id"] as? String ?: "",
            fullName = data["fullName"] as? String ?: "",
            profileImageUrl = data["profileImageUrl"] as? String,
            phone = data["phone"] as? String ?: "",
            address = data["address"] as? String ?: "",
            skills = data["skills"] as? String,
            companyName = data["companyName"] as? String,
            trustTier = data["trustTier"] as? String ?: "NEW"
        )
    }
}
