package com.example.dutype.models

import androidx.annotation.Keep

/**
 * UserSummary - MINIMAL for list views
 */
@Keep
data class UserSummary(
    val id: String = "",
    val fullName: String = "",
    val profileImageUrl: String? = null,
    val phone: String = "",
    val companyName: String? = null  // fetched from employer_profiles at runtime
) {
    companion object {
        fun fromUser(user: User): UserSummary = UserSummary(
            id = user.id,
            fullName = user.fullName,
            profileImageUrl = user.profileImageUrl,
            phone = user.phone,
            companyName = null  // not stored on users doc — fetch from employer_profiles if needed
        )

        fun fromMap(data: Map<String, Any>): UserSummary = UserSummary(
            id = data["id"] as? String ?: "",
            fullName = data["fullName"] as? String ?: "",
            profileImageUrl = data["profileImageUrl"] as? String,
            phone = data["phone"] as? String ?: "",
            companyName = data["companyName"] as? String
        )
    }
}
