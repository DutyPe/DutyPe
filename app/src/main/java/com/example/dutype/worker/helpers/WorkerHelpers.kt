package com.example.dutype.worker.helpers

import androidx.compose.ui.graphics.Color
import com.example.dutype.R
import com.example.dutype.worker.models.ApplicationStatus

object WorkerHelpers {

    fun getStatusColor(status: ApplicationStatus?): Color {
        return when (status) {
            ApplicationStatus.DRAFT -> Color(0xFF9E9E9E)
            ApplicationStatus.SUBMITTED -> Color(0xFF2196F3)
            ApplicationStatus.UNDER_REVIEW -> Color(0xFFFF9800)
            ApplicationStatus.SHORTLISTED -> Color(0xFF9C27B0)
            ApplicationStatus.INTERVIEW_SCHEDULED -> Color(0xFF00BCD4)
            ApplicationStatus.INTERVIEWED -> Color(0xFF3F51B5)
            ApplicationStatus.SELECTED -> Color(0xFF4CAF50)
            ApplicationStatus.REJECTED -> Color(0xFFF44336)
            ApplicationStatus.WITHDRAWN -> Color(0xFF607D8B)
            ApplicationStatus.EXPIRED -> Color(0xFF795548)
            else -> Color.Gray
        }
    }

    fun getStatusEmoji(status: ApplicationStatus?): String {
        return when (status) {
            ApplicationStatus.DRAFT -> "📝"
            ApplicationStatus.SUBMITTED -> "📤"
            ApplicationStatus.UNDER_REVIEW -> "👀"
            ApplicationStatus.SHORTLISTED -> "⭐"
            ApplicationStatus.INTERVIEW_SCHEDULED -> "📅"
            ApplicationStatus.INTERVIEWED -> "💼"
            ApplicationStatus.SELECTED -> "🎉"
            ApplicationStatus.REJECTED -> "❌"
            ApplicationStatus.WITHDRAWN -> "↩️"
            ApplicationStatus.EXPIRED -> "⏰"
            else -> ""
        }
    }

    fun getCategoryImageForJob(title: String): Int? {
        val categoryMapping = mapOf(
            "Delivery" to R.drawable.delivery,
            "Driver" to R.drawable.delivery,
            "Waiter" to R.drawable.waiters,
            "Cook" to R.drawable.cleaning,
            // Add more mappings here...
        )
        return categoryMapping.entries.firstOrNull { title.contains(it.key, ignoreCase = true) }?.value
    }


}
