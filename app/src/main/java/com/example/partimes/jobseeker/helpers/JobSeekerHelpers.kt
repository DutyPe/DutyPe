package com.example.partimes.jobseeker.helpers

import androidx.compose.ui.graphics.Color
import com.example.partimes.R
import com.example.partimes.jobseeker.models.ApplicationStatus

object JobSeekerHelpers {

    fun getStatusColor(status: ApplicationStatus?): Color {
        return when (status) {
            ApplicationStatus.PENDING -> Color(0xFFFFC107)
            ApplicationStatus.SELECTED -> Color(0xFF4CAF50)
            ApplicationStatus.REJECTED -> Color(0xFFF44336)
            ApplicationStatus.VACANCY_FILLED -> Color(0xFF9E9E9E)
            ApplicationStatus.INTERVIEW_SCHEDULED -> Color(0xFF2979FF)
            ApplicationStatus.DOCUMENTS_PENDING -> Color(0xFFAB47BC)
            else -> Color.Gray
        }
    }

    fun getStatusEmoji(status: ApplicationStatus?): String {
        return when (status) {
            ApplicationStatus.PENDING -> "⏳"
            ApplicationStatus.SELECTED -> "🎉"
            ApplicationStatus.REJECTED -> "❌"
            ApplicationStatus.VACANCY_FILLED -> "🚫"
            ApplicationStatus.INTERVIEW_SCHEDULED -> "📅"
            ApplicationStatus.DOCUMENTS_PENDING -> "📂"
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
