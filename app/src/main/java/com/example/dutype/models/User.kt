package com.example.dutype.models

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

/** The signed-in user as cached on the device by AuthManager (not a Firestore document). */
@Keep
@Immutable
data class User(
    val id: String = "",
    val phone: String = "",
    val fullName: String = "",
    val profileImageUrl: String? = null,
    /** The user's single, immutable product role. */
    val role: UserRole = UserRole.WORKER
)

enum class UserRole {
    WORKER,
    EMPLOYER,
    ADMIN
}
