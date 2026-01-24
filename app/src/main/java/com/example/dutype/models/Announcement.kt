package com.example.dutype.models

import com.google.firebase.Timestamp

/**
 * Announcement Model - For in-app announcements and banners
 */
data class Announcement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: AnnouncementType = AnnouncementType.INFO,
    val priority: AnnouncementPriority = AnnouncementPriority.NORMAL,
    val targetRole: String? = null, // null = all, "worker", "employer"
    val actionText: String? = null,
    val actionRoute: String? = null,
    val imageUrl: String? = null,
    val startDate: Timestamp = Timestamp.now(),
    val endDate: Timestamp? = null,
    val isDismissible: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Timestamp = Timestamp.now()
)

enum class AnnouncementType {
    INFO,       // Blue - General information
    SUCCESS,    // Green - Success messages
    WARNING,    // Yellow - Warnings
    ERROR,      // Red - Errors
    FEATURE,    // Purple - New features
    PROMOTION   // Orange - Promotions
}

enum class AnnouncementPriority {
    LOW,        // Show at bottom
    MEDIUM,     // Show in middle (alias for NORMAL)
    NORMAL,     // Show in middle
    HIGH,       // Show at top
    URGENT      // Show as modal
}
