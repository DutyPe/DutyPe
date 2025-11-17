package com.example.dutype.worker.helpers

import androidx.compose.ui.graphics.Color
import com.parttime.dutype.R
import com.example.dutype.models.ApplicationStatus

object WorkerHelpers {

    fun getStatusColor(status: ApplicationStatus?): Color {
        return when (status) {
            ApplicationStatus.PENDING -> Color(0xFFF59E0B)
            ApplicationStatus.UNDER_REVIEW -> Color(0xFF3B82F6)
            ApplicationStatus.ACCEPTED -> Color(0xFF10B981)
            ApplicationStatus.REJECTED -> Color(0xFFEF4444)
            else -> Color.Gray
        }
    }

    fun getStatusEmoji(status: ApplicationStatus?): String {
        return when (status) {
            ApplicationStatus.PENDING -> "⏳"
            ApplicationStatus.UNDER_REVIEW -> "👀"
            ApplicationStatus.ACCEPTED -> "✅"
            ApplicationStatus.REJECTED -> "❌"
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
