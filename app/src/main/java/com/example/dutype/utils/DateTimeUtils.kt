package com.example.dutype.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Centralized Date/Time formatting utilities
 * 
 * Consolidates all timestamp formatting logic to avoid duplication across:
 * - ConversationListScreen (formatTimestamp)
 * - ChatDetailScreen (formatMessageTime)
 * - WorkStartQRScreen (formatTime)
 * - EmployerVerifyWorkScreen (formatTime)
 * - WorkerHistoryScreen (formatTimelineDate)
 * - EmployerHistoryScreen (formatTimelineDate)
 * - JobListing.getTimeAgoDisplayText()
 * - JobListingSummary.getTimeAgoDisplayText()
 */
object DateTimeUtils {
    
    private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dayFormatter = SimpleDateFormat("EEE", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("MMM d", Locale.getDefault())
    private val fullDateFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val fullDateTimeFormatter = SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault())
    
    /**
     * Format timestamp to relative time for chat/conversation lists
     * Returns: "Now", "5m", "2h", "Mon", "Dec 5"
     */
    fun formatRelativeTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Now"                    // Less than 1 minute
            diff < 3600_000 -> "${diff / 60_000}m"   // Less than 1 hour
            diff < 86400_000 -> "${diff / 3600_000}h" // Less than 1 day
            diff < 604800_000 -> dayFormatter.format(Date(timestamp)) // Less than 1 week
            else -> dateFormatter.format(Date(timestamp))
        }
    }
    
    /**
     * Format timestamp to "time ago" display text for job listings
     * Returns: "Just now", "5m ago", "2h ago", "3d ago", "2w ago"
     * 
     * CANONICAL IMPLEMENTATION - Use this instead of duplicating in models
     */
    fun formatTimeAgo(timestamp: Long): String {
        if (timestamp == 0L) return ""
        
        val currentTime = System.currentTimeMillis()
        val diffInMillis = currentTime - timestamp
        val diffInSeconds = diffInMillis / 1000
        val diffInMinutes = diffInSeconds / 60
        val diffInHours = diffInMinutes / 60
        val diffInDays = diffInHours / 24
        val diffInWeeks = diffInDays / 7

        return when {
            diffInSeconds < 60 -> "Just now"
            diffInMinutes < 60 -> "${diffInMinutes}m ago"
            diffInHours < 24 -> "${diffInHours}h ago"
            diffInDays < 7 -> "${diffInDays}d ago"
            else -> "${diffInWeeks}w ago"
        }
    }
    
    /**
     * Format timestamp to time only (h:mm a)
     * Returns: "2:30 PM", "10:15 AM"
     */
    fun formatTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        return timeFormatter.format(Date(timestamp))
    }
    
    /**
     * Format timestamp to short date (dd MMM)
     * Returns: "25 Dec", "01 Jan"
     */
    fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return ""
        return fullDateFormatter.format(Date(timestamp))
    }
    
    /**
     * Format timestamp to full date with time
     * Returns: "25 Dec, 2:30 PM"
     */
    fun formatDateTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        return fullDateTimeFormatter.format(Date(timestamp))
    }
    
    /**
     * Check if timestamp is from today
     */
    fun isToday(timestamp: Long): Boolean {
        if (timestamp == 0L) return false
        val now = System.currentTimeMillis()
        return now - timestamp < 24 * 60 * 60 * 1000
    }
}
