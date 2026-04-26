package com.example.dutype.models

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

/**
 * User model built from canonical phoneRoles + role profile data.
 */
@Keep
@Immutable
data class User(
    val id: String = "",
    val phone: String = "",
    val fullName: String = "",
    val profileImageUrl: String? = null,

    /** The user's single, immutable product role. */
    val role: UserRole = UserRole.WORKER,

    // Location (nested map in Firestore: location:{lat,lng})
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val geohash: String = "",

    // System
    val fcmToken: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Parse canonical profile data into a [User].
         */
        @Suppress("UNCHECKED_CAST")
        fun fromFirestoreMap(uid: String, data: Map<String, Any?>): User {
            val roleStr = (data["role"] as? String)
                ?: (data["roles"] as? List<*>)?.firstOrNull()?.toString()
                ?: UserRole.WORKER.name
            val role = runCatching { UserRole.valueOf(roleStr.uppercase()) }
                .getOrDefault(UserRole.WORKER)
            val location = data["location"] as? Map<String, Any?>
            return User(
                id = uid,
                fullName = (data["fullName"] as? String) ?: "",
                phone = (data["phone"] as? String) ?: "",
                profileImageUrl = data["profileImageUrl"] as? String,
                role = role,
                lat = (location?.get("lat") as? Number)?.toDouble() ?: 0.0,
                lng = (location?.get("lng") as? Number)?.toDouble() ?: 0.0,
                geohash = (data["geohash"] as? String) ?: "",
                fcmToken = data["fcmToken"] as? String,
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        /**
         * Build the canonical Firestore field map for [role].
         *
         * Single source of truth = `role` (string). DutyPe enforces a single
         * immutable product role per phone number; dual-role accounts are not
         * supported. Read-side fallback to legacy `activeRole`/`roles[0]` is
         * handled in [fromFirestoreMap].
         */
        fun roleFieldsFor(role: UserRole): Map<String, Any> = mapOf(
            "role" to role.name
        )
    }
}

enum class UserRole {
    WORKER,
    EMPLOYER,
    ADMIN
}
