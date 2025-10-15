package com.example.dutype.worker.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Build
import androidx.compose.ui.graphics.vector.ImageVector

// Job Card Model for Worker Home Screen
data class JobCardModel(
    val jobId: String,
    val title: String,
    val employerName: String,
    val payInfo: PayInfo,
    val location: LocationInfo,
    val tags: List<JobTag>,
    val timeInfo: TimeInfo,
    val distance: String = "",
    val isVerifiedEmployer: Boolean = false,
    val imageUrl: String? = null,
    // Additional fields for job description
    val phoneNumber: String,
    val description: String,
    val requirements: List<String> = emptyList(),
    val benefits: List<String> = emptyList(),
    val workingHours: String = "",
    val experienceRequired: String = "",
    val ageRange: String = "",
    val gender: String = "Any",
    val vacancies: Int = 1,
    val jobType: String = "Part-time", // Full-time, Part-time, Contract, etc.
    val applicationDeadline: String? = null,
    val companySize: String = "",
    val industry: String = "",
    val viewCount: Int = 0,
    val applicationCount: Int = 0,
    val isBookmarked: Boolean = false,
    val isSaved: Boolean = false,
    val isApplied: Boolean = false,
    val postedAt: Long = System.currentTimeMillis()
) {
    fun getShareableText(): String {
        return """
🚀 *${title}* at *${employerName}*

💰 ${payInfo.getDisplayText()} ${payInfo.getTypeEmoji()}
📍 ${location.getDisplayText()} (${location.getDistanceText()})

✨ *Job Highlights:*
${tags.take(3).joinToString("\n") { "• ${it.emoji} ${it.text}" }}

📝 *Description:*
${description.take(200)}${if (description.length > 200) "..." else ""}

📞 Contact: ${phoneNumber}

💼 Apply now through DutyPe App!
""".trimIndent()
    }
}

data class PayInfo(
    val amount: String,
    val type: PayType,
    val period: String
) {
    fun getDisplayText(): String {
        val periodLabel = when (type) {
            PayType.HOURLY -> "hourly"
            PayType.DAILY -> "daily"
            PayType.MONTHLY -> "monthly"
            PayType.PER_TASK -> "per task"
        }
        val trimmedAmount = amount.trim()
        return if (trimmedAmount.isNotEmpty()) "$trimmedAmount/$periodLabel" else ""
    }
    fun getTypeIcon(): ImageVector = when (type) {
        PayType.HOURLY -> Icons.Default.AccessTime
        PayType.DAILY -> Icons.Default.CalendarToday
        PayType.MONTHLY -> Icons.Default.Work
        PayType.PER_TASK -> Icons.Default.Build
    }
    fun getTypeEmoji(): String = when (type) {
        PayType.HOURLY -> "⏰"
        PayType.DAILY -> "📅"
        PayType.MONTHLY -> "💼"
        PayType.PER_TASK -> "🛠️"
    }
}

enum class PayType {
    HOURLY,
    DAILY,
    MONTHLY,
    PER_TASK
}

data class LocationInfo(
    val area: String,
    val city: String,
    val distance: String
) {
    fun getDisplayText(): String {
        // If area and city are the same, show only one
        if (area.equals(city, ignoreCase = true)) {
            return area
        }
        // If area is empty or null, show only city
        if (area.isBlank()) {
            return city
        }
        // If city is empty or null, show only area
        if (city.isBlank()) {
            return area
        }
        // Show both if they're different
        return "$area, $city"
    }
    fun getDistanceText(): String = "$distance km away"
}

data class JobTag(
    val text: String,
    val emoji: String,
    val type: TagType = TagType.BENEFIT
)

enum class TagType {
    VERIFICATION,
    URGENCY,
    BENEFIT,
    SCHEDULE
}

data class TimeInfo(
    val postedTime: String,
    val urgency: UrgencyLevel = UrgencyLevel.NORMAL
) {
    fun getRelativeTime(): String {
        return try {
            val timestamp = postedTime.toLongOrNull()
            if (timestamp != null) {
                val currentTime = System.currentTimeMillis()
                val diffInMillis = currentTime - timestamp
                val diffInDays = diffInMillis / (24 * 60 * 60 * 1000)
                val diffInHours = diffInMillis / (60 * 60 * 1000)
                val diffInMinutes = diffInMillis / (60 * 1000)
                
                when {
                    diffInDays > 0 -> "${diffInDays.toInt()} day${if (diffInDays > 1) "s" else ""} ago"
                    diffInHours > 0 -> "${diffInHours.toInt()} hour${if (diffInHours > 1) "s" else ""} ago"
                    diffInMinutes > 0 -> "${diffInMinutes.toInt()} minute${if (diffInMinutes > 1) "s" else ""} ago"
                    else -> "Just now"
                }
            } else {
                postedTime // Return original if not a valid timestamp
            }
        } catch (e: Exception) {
            postedTime // Return original if any error
        }
    }
}

enum class UrgencyLevel {
    IMMEDIATE,
    URGENT,
    NORMAL
}

// Predefined common tags
object CommonTags {
    val VERIFIED_EMPLOYER = JobTag("Verified Employer", "✅", TagType.VERIFICATION)
    val IMMEDIATE_HIRING = JobTag("Immediate Hiring", "⚡", TagType.URGENCY)
    val FLEXIBLE_HOURS = JobTag("Flexible Hours", "🕒", TagType.SCHEDULE)
    val MEALS_PROVIDED = JobTag("Meals Provided", "🍲", TagType.BENEFIT)
    val TRANSPORT_PROVIDED = JobTag("Transport Provided", "🚌", TagType.BENEFIT)
    val WEEKLY_PAYMENT = JobTag("Weekly Payment", "💰", TagType.BENEFIT)
    val NO_EXPERIENCE = JobTag("No Experience", "🎯", TagType.BENEFIT)
    val PART_TIME = JobTag("Part-time", "⏰", TagType.SCHEDULE)
    val FULL_TIME = JobTag("Full-time", "💼", TagType.SCHEDULE)
    val WEEKEND_ONLY = JobTag("Weekend Only", "📅", TagType.SCHEDULE)
    val CREATIVE_WORK = JobTag("Creative Work", "🎨", TagType.BENEFIT)
    val GROWTH_OPPORTUNITY = JobTag("Growth Opportunity", "📈", TagType.BENEFIT)
    val REMOTE_WORK = JobTag("Remote Work", "🏠", TagType.BENEFIT)
}

// Dummy data for testing
object JobCardDummyData {
    fun getDummyJobCards(): List<JobCardModel> {
        // Return empty list - no dummy data
        return emptyList()
    }
}
