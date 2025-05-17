package com.example.partimes.utils
import androidx.compose.ui.graphics.Color
import com.example.partimes.R
import com.example.partimes.models.ApplicationStatus

fun getJobTypeEmoji(wage: String): String {
    return when {
        wage.contains("/hour", true) -> "⏱"
        wage.contains("/day", true) -> "📆"
        wage.contains("/month", true) -> "🗓️"
        wage.contains("/poster", true) || wage.contains("/task", true) -> "🧩"
        else -> "🎯"
    }
}

object JobCardHelpers {

    fun getPreferenceIcon(tag: String): String {
        return when {
            tag.contains("female", true) -> "👩"
            tag.contains("male", true) -> "👨"
            tag.contains("experience", true) -> "💼"
            tag.contains("fresher", true) -> "🆓"
            tag.contains("graduate", true) -> "🎓"
            tag.contains("immediate", true) -> "⚡"
            else -> "🔖"
        }
    }

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
    fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "$days day(s) ago"
            hours > 0 -> "$hours hour(s) ago"
            minutes > 0 -> "$minutes minute(s) ago"
            else -> "just now"
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
fun getCategoryAnimationForJob(title: String): Int? {
    val categoryMapping = mapOf(
        "Delivery" to R.raw.delivery,
        "Driver" to R.raw.delivery,
        "Waiter" to R.raw.waiter,
        "Cook" to R.raw.cook,
        "Cleaner" to R.raw.cleaner,
        "Painter" to R.raw.painter,
        "Farming" to R.raw.farming,
//        "Retail" to R.raw.retail,
//        "Hospitality" to R.raw.hospitality,
    )
    return categoryMapping.entries.firstOrNull { title.contains(it.key, ignoreCase = true) }?.value
}


}
