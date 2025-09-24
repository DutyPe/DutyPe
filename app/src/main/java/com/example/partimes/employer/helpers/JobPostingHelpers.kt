package com.example.partimes.employer.helpers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.partimes.employer.models.JobPostingModel
import com.example.partimes.employer.models.enums.*

/**
 * Helper functions and utilities for employer job posting functionality
 */
object JobPostingHelpers {

    /**
     * Get display icon for pay type
     */
    fun getPayTypeIcon(payType: PayType): ImageVector {
        return when (payType) {
            PayType.HOURLY -> Icons.Default.AccessTime
            PayType.DAILY -> Icons.Default.CalendarToday
            PayType.MONTHLY -> Icons.Default.Work
            PayType.TASK -> Icons.Default.Build
        }
    }

    /**
     * Get color for job urgency
     */
    fun getUrgencyColor(urgency: JobUrgency): Color {
        return when (urgency) {
            JobUrgency.IMMEDIATE -> Color(0xFFE53E3E)
            JobUrgency.URGENT -> Color(0xFFFF8C00)
            JobUrgency.NORMAL -> Color(0xFF3B82F6)
            JobUrgency.FLEXIBLE -> Color(0xFF10B981)
        }
    }

    /**
     * Format time ago from timestamp
     */
    fun getTimeAgo(timestamp: Long): String {
        val currentTime = System.currentTimeMillis()
        val diffInMillis = currentTime - timestamp
        val diffInHours = diffInMillis / (1000 * 60 * 60)
        val diffInDays = diffInHours / 24
        val diffInWeeks = diffInDays / 7

        return when {
            diffInHours < 1 -> "Just now"
            diffInHours < 24 -> "${diffInHours}h ago"
            diffInDays < 7 -> "${diffInDays}d ago"
            diffInWeeks < 4 -> "${diffInWeeks}w ago"
            else -> "${diffInWeeks / 4}m ago"
        }
    }

    /**
     * Validate job posting data
     */
    fun validateJobPosting(jobPosting: JobPostingModel): ValidationResult {
        val errors = mutableListOf<String>()

        if (jobPosting.title.isBlank()) {
            errors.add("Job title is required")
        }

        if (jobPosting.payAmount.isBlank()) {
            errors.add("Pay amount is required")
        }

        if (jobPosting.location.isBlank()) {
            errors.add("Job location is required")
        }

        if (jobPosting.description.isBlank()) {
            errors.add("Job description is required")
        }

        if (jobPosting.contactNumber.isBlank()) {
            errors.add("Contact number is required")
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Generate job posting summary for preview
     */
    fun generateJobSummary(jobPosting: JobPostingModel): String {
        return """
            📋 ${jobPosting.title}
            💰 ₹${jobPosting.payAmount} ${jobPosting.payType.displayName}
            📍 ${jobPosting.location}
            👥 ${jobPosting.vacancies} position(s)
            ⏰ ${jobPosting.shiftTiming.displayName}
            🔥 ${jobPosting.urgency.displayName} hiring
        """.trimIndent()
    }

    /**
     * Get recommended improvements for job posting
     */
    fun getJobImprovementSuggestions(jobPosting: JobPostingModel): List<String> {
        val suggestions = mutableListOf<String>()

        if (jobPosting.description.length < 50) {
            suggestions.add("Add more details to job description")
        }

        // Perks validation removed as per user request

        if (!jobPosting.isVerified) {
            suggestions.add("Verify your employer profile to gain trust")
        }

        return suggestions
    }

    /**
     * Calculate estimated application count based on job details
     */
    fun getEstimatedApplications(jobPosting: JobPostingModel): IntRange {
        var baseCount = 10

        // Adjust based on urgency
        when (jobPosting.urgency) {
            JobUrgency.IMMEDIATE -> baseCount += 15
            JobUrgency.URGENT -> baseCount += 10
            JobUrgency.NORMAL -> baseCount += 5
            JobUrgency.FLEXIBLE -> baseCount += 3
        }

        // Perks adjustment removed as per user request

        // Adjust based on pay competitiveness (simplified)
        val payAmount = jobPosting.payAmount.toIntOrNull() ?: 0
        when (jobPosting.payType) {
            PayType.DAILY -> if (payAmount > 400) baseCount += 10
            PayType.HOURLY -> if (payAmount > 50) baseCount += 10
            PayType.MONTHLY -> if (payAmount > 15000) baseCount += 10
            PayType.TASK -> if (payAmount > 200) baseCount += 5
        }

        return (baseCount..(baseCount * 1.5).toInt())
    }
}

// Validation result data class
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)
