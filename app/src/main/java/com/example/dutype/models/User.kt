package com.example.dutype.models

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

/**
 * User â€” single-role canonical model.
 *
 * Firestore users collection (canonical fields):
 *   userId (doc ID), phone, fullName, profileImageUrl,
 *   role (single string),
 *   location{lat,lng}, geohash,
 *   fcmToken, createdAt, lastActiveAt,
 *   referralCode, referredByCode, referredByUserId
 *
 * Transient compat fields written for one migration window:
 *   roles (one-element array containing role)
 *   activeRole (mirror of role)
 *
 * The Kotlin model exposes only `role`. Read-side helpers below tolerate
 * legacy documents that still contain only `roles`/`activeRole`.
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
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Parse a `users/{uid}` Firestore document into a [User].
         *
         * Prefers the new canonical `role` field; falls back to `activeRole`
         * then `roles[0]` for unmigrated legacy documents.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromFirestoreMap(uid: String, data: Map<String, Any?>): User {
            val roleStr = (data["role"] as? String)
                ?: (data["activeRole"] as? String)
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
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                lastActiveAt = (data["lastActiveAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        /**
         * Build the canonical Firestore field map for [role], including
         * one-cycle compat mirrors so legacy CFs/admin tools keep working.
         */
        fun roleFieldsFor(role: UserRole): Map<String, Any> = mapOf(
            "role" to role.name,
            // Transient compat. Remove after CFs and admin web read `role`.
            "roles" to listOf(role.name),
            "activeRole" to role.name
        )
    }
}

enum class UserRole {
    WORKER,
    EMPLOYER,
    ADMIN
}
