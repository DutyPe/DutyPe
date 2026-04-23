package com.example.dutype.employer.helpers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.dutype.employer.models.JobPostingModel
import com.example.dutype.employer.models.*
import com.example.dutype.utils.DateTimeUtils

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
        }
    }

    /**
     * Format time ago from timestamp
     * Uses centralized DateTimeUtils for consistency
     */
    fun getTimeAgo(timestamp: Long): String = DateTimeUtils.formatRelativeTime(timestamp)

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

