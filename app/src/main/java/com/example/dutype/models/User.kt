package com.example.dutype.models

import androidx.annotation.Keep

/**
 * User — strict target schema model.
 *
 * Firestore users collection:
 *   userId (doc ID), phone, fullName, profileImageUrl,
 *   roles:[], activeRole, location:{lat,lng}, geohash,
 *   isVerified, isActive, fcmToken, createdAt, lastActiveAt
 */
@Keep
data class User(
    val id: String = "",
    val email: String = "",
    val phone: String = "",
    val fullName: String = "",
    val profileImageUrl: String? = null,
    val companyName: String = "",

    // Role management
    val roles: List<String> = listOf("WORKER"),
    val activeRole: UserRole = UserRole.WORKER,

    // Location (nested map in Firestore: location:{lat,lng})
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val geohash: String = "",

    // System
    val isVerified: Boolean = false,
    val isActive: Boolean = true,
    val fcmToken: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
) {
    fun hasRole(role: UserRole): Boolean = roles.contains(role.name)
    fun isDualRole(): Boolean = roles.size > 1
    fun getEnabledRoles(): List<UserRole> = roles.mapNotNull {
        try { UserRole.valueOf(it) } catch (_: Exception) { null }
    }
}

enum class UserRole {
    WORKER,
    EMPLOYER,
    ADMIN
}
