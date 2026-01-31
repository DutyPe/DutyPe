package com.example.dutype.models

/**
 * Lightweight model for displaying recently hired workers
 * Used for social proof on worker home screen
 */
data class RecentHire(
    val workerName: String,      // "Raj K." (first name + last initial)
    val jobTitle: String,         // "Delivery Driver"
    val timeAgo: String,          // "2 mins ago"
    val acceptedAt: Long = 0L     // Timestamp for sorting
)

/**
 * Convert full name to privacy-friendly format
 * Example: "Rajesh Kumar" -> "Rajesh K."
 */
fun String.toPrivacyFriendlyName(): String {
    val parts = this.trim().split(" ")
    return when {
        parts.size >= 2 -> "${parts[0]} ${parts[1].first()}."
        parts.size == 1 -> parts[0]
        else -> "User"
    }
}

/**
 * Convert timestamp to relative time
 * Example: 120000 ms ago -> "2 mins ago"
 */
fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} mins ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        else -> "${diff / 86400_000} days ago"
    }
}
