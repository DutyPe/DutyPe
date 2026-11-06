package com.example.dutype.announcements

import androidx.annotation.Keep
import com.google.firebase.Timestamp

@Keep
data class InAppAnnouncement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "INFO",
    val priority: String = "NORMAL",
    val targetRole: String? = null,
    val actionRoute: String = "",
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
)
