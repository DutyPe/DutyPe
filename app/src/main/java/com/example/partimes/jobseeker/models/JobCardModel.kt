package com.example.partimes.jobseeker.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Build
import androidx.compose.ui.graphics.vector.ImageVector

// Job Card Model for Jobseeker Home Screen
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
    fun getDisplayText(): String = "₹$amount/$period"
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
    fun getDisplayText(): String = "$area, $city"
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
)

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