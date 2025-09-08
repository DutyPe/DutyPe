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
    val industry: String = ""
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

💼 Apply now through ParTimes App!
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
}

// Dummy data for testing
object JobCardDummyData {
    fun getDummyJobCards(): List<JobCardModel> {
        return listOf(
            JobCardModel(
                jobId = "1",
                title = "Cook Needed",
                employerName = "Sri Sai Tiffins",
                payInfo = PayInfo("400", PayType.DAILY, "day"),
                location = LocationInfo("Kukatpally", "Hyderabad", "2.5"),
                tags = listOf(
                    CommonTags.VERIFIED_EMPLOYER,
                    CommonTags.IMMEDIATE_HIRING,
                    CommonTags.MEALS_PROVIDED
                ),
                timeInfo = TimeInfo("2 hrs ago", UrgencyLevel.IMMEDIATE),
                isVerifiedEmployer = true,
                phoneNumber = "+91 9876543210",
                description = "Looking for a skilled cook to prepare delicious South Indian meals. Must have experience in traditional cooking methods and knowledge of various spices. Responsibility includes meal preparation for 50+ customers daily, maintaining hygiene standards, and managing kitchen inventory.",
                requirements = listOf("2+ years cooking experience", "Knowledge of South Indian cuisine", "Food safety certification", "Ability to work in fast-paced environment"),
                benefits = listOf("Free meals provided", "Transport allowance ₹100/day", "Performance bonus", "Festival bonuses"),
                workingHours = "6 AM - 2 PM (8 hours)",
                experienceRequired = "2+ years",
                ageRange = "20-45 years",
                gender = "Any",
                vacancies = 2,
                jobType = "Full-time",
                applicationDeadline = "2024-12-31",
                companySize = "10-50 employees",
                industry = "Food & Restaurant"
            ),
            JobCardModel(
                jobId = "2",
                title = "Delivery Executive",
                employerName = "Swiggy",
                payInfo = PayInfo("50", PayType.HOURLY, "hour"),
                location = LocationInfo("Gachibowli", "Hyderabad", "1.2"),
                tags = listOf(
                    CommonTags.VERIFIED_EMPLOYER,
                    CommonTags.FLEXIBLE_HOURS,
                    CommonTags.WEEKLY_PAYMENT
                ),
                timeInfo = TimeInfo("5 hrs ago", UrgencyLevel.NORMAL),
                isVerifiedEmployer = true,
                phoneNumber = "+91 9876543211",
                description = "Join our delivery team and earn flexibly! Deliver food orders to customers across Hyderabad. Work on your own schedule and earn extra income. Perfect for students and part-time workers.",
                requirements = listOf("Own bike with valid license", "Smartphone with GPS", "Good communication skills", "Know local area well"),
                benefits = listOf("Flexible working hours", "Weekly payment", "Fuel allowance", "Performance incentives", "Medical insurance"),
                workingHours = "Flexible (minimum 4 hours/day)",
                experienceRequired = "No experience required",
                ageRange = "18-50 years",
                gender = "Any",
                vacancies = 10,
                jobType = "Part-time",
                applicationDeadline = "2024-12-25",
                companySize = "1000+ employees",
                industry = "Food Delivery"
            ),
            JobCardModel(
                jobId = "3",
                title = "Sales Associate",
                employerName = "Reliance Digital",
                payInfo = PayInfo("15000", PayType.MONTHLY, "month"),
                location = LocationInfo("Ameerpet", "Hyderabad", "3.8"),
                tags = listOf(
                    CommonTags.VERIFIED_EMPLOYER,
                    CommonTags.FULL_TIME,
                    CommonTags.NO_EXPERIENCE
                ),
                timeInfo = TimeInfo("1 day ago", UrgencyLevel.NORMAL),
                isVerifiedEmployer = true,
                phoneNumber = "+91 9876543212",
                description = "Exciting opportunity to join India's leading electronics retailer! Handle customer inquiries, demonstrate products, and achieve sales targets. Great growth opportunities and employee benefits.",
                requirements = listOf("12th pass minimum", "Good communication skills", "Customer service orientation", "Basic English knowledge"),
                benefits = listOf("Monthly salary ₹15,000", "Sales incentives", "Employee discounts", "Health insurance", "Career advancement"),
                workingHours = "10 AM - 8 PM (9 hours + 1 hour break)",
                experienceRequired = "Freshers welcome",
                ageRange = "18-35 years",
                gender = "Any",
                vacancies = 5,
                jobType = "Full-time",
                applicationDeadline = "2024-12-20",
                companySize = "10000+ employees",
                industry = "Retail"
            ),
            JobCardModel(
                jobId = "4",
                title = "House Cleaning",
                employerName = "Urban Company",
                payInfo = PayInfo("300", PayType.PER_TASK, "task"),
                location = LocationInfo("Banjara Hills", "Hyderabad", "4.2"),
                tags = listOf(
                    CommonTags.FLEXIBLE_HOURS,
                    CommonTags.IMMEDIATE_HIRING,
                    CommonTags.TRANSPORT_PROVIDED
                ),
                timeInfo = TimeInfo("30 mins ago", UrgencyLevel.URGENT),
                isVerifiedEmployer = false,
                phoneNumber = "+91 9876543213",
                description = "Professional house cleaning services needed. Clean residential properties to high standards. Flexible timing and good earning potential. Training provided for cleaning techniques and customer service.",
                requirements = listOf("Attention to detail", "Physical fitness", "Reliability", "Basic cleaning experience preferred"),
                benefits = listOf("Flexible timing", "Transport provided", "Training provided", "Performance bonus", "Weekly payment"),
                workingHours = "Flexible (2-8 hours per task)",
                experienceRequired = "1 year preferred",
                ageRange = "20-50 years",
                gender = "Any",
                vacancies = 15,
                jobType = "Contract",
                applicationDeadline = "2024-12-30",
                companySize = "500-1000 employees",
                industry = "Home Services"
            ),
            JobCardModel(
                jobId = "5",
                title = "Waiter",
                employerName = "Paradise Restaurant",
                payInfo = PayInfo("350", PayType.DAILY, "day"),
                location = LocationInfo("Secunderabad", "Hyderabad", "5.1"),
                tags = listOf(
                    CommonTags.VERIFIED_EMPLOYER,
                    CommonTags.MEALS_PROVIDED,
                    CommonTags.PART_TIME
                ),
                timeInfo = TimeInfo("3 hrs ago", UrgencyLevel.NORMAL),
                isVerifiedEmployer = true,
                phoneNumber = "+91 9876543214",
                description = "Join the team at Hyderabad's famous Paradise Restaurant! Serve customers with traditional hospitality, take orders, and ensure customer satisfaction. Great tips and learning environment.",
                requirements = listOf("Good communication", "Customer service skills", "Physical stamina", "Pleasant personality", "Basic English"),
                benefits = listOf("Daily wage ₹350", "Free meals", "Tips from customers", "Festival bonuses", "Overtime pay"),
                workingHours = "11 AM - 11 PM (12 hours with breaks)",
                experienceRequired = "1+ years in hospitality",
                ageRange = "18-40 years",
                gender = "Male preferred",
                vacancies = 3,
                jobType = "Full-time",
                applicationDeadline = "2024-12-28",
                companySize = "100-500 employees",
                industry = "Restaurant"
            ),
            JobCardModel(
                jobId = "6",
                title = "Data Entry Operator",
                employerName = "Tech Solutions Pvt Ltd",
                payInfo = PayInfo("25", PayType.HOURLY, "hour"),
                location = LocationInfo("HITEC City", "Hyderabad", "6.7"),
                tags = listOf(
                    CommonTags.VERIFIED_EMPLOYER,
                    CommonTags.FLEXIBLE_HOURS,
                    CommonTags.WEEKEND_ONLY
                ),
                timeInfo = TimeInfo("6 hrs ago", UrgencyLevel.NORMAL),
                isVerifiedEmployer = true,
                phoneNumber = "+91 9876543215",
                description = "Work from office data entry position. Input data accurately into computer systems, maintain databases, and ensure data quality. Perfect for detail-oriented individuals. Weekend batches available.",
                requirements = listOf("Basic computer knowledge", "Typing speed 35+ WPM", "Attention to detail", "MS Excel knowledge", "12th pass minimum"),
                benefits = listOf("Hourly payment ₹25", "Flexible hours", "Air-conditioned office", "Weekend batches", "Performance bonus"),
                workingHours = "Flexible (4-8 hours/day)",
                experienceRequired = "6 months preferred",
                ageRange = "18-45 years",
                gender = "Any",
                vacancies = 8,
                jobType = "Part-time",
                applicationDeadline = "2024-12-22",
                companySize = "50-100 employees",
                industry = "IT Services"
            ),
            JobCardModel(
                jobId = "7",
                title = "Security Guard",
                employerName = "G4S Security",
                payInfo = PayInfo("12000", PayType.MONTHLY, "month"),
                location = LocationInfo("Madhapur", "Hyderabad", "2.9"),
                tags = listOf(
                    CommonTags.VERIFIED_EMPLOYER,
                    CommonTags.FULL_TIME,
                    CommonTags.NO_EXPERIENCE
                ),
                timeInfo = TimeInfo("4 hrs ago", UrgencyLevel.NORMAL),
                isVerifiedEmployer = true,
                phoneNumber = "+91 9876543216",
                description = "Security guard position for commercial buildings and residential complexes. Monitor premises, control access, and ensure safety. Training provided. Various shift options available.",
                requirements = listOf("Physical fitness", "Alert and responsible", "Basic reading/writing", "No criminal background", "Age 21-50"),
                benefits = listOf("Monthly salary ₹12,000", "Night shift allowance", "Uniform provided", "ESI/PF benefits", "Training provided"),
                workingHours = "12-hour shifts (Day/Night rotation)",
                experienceRequired = "No experience required",
                ageRange = "21-50 years",
                gender = "Male",
                vacancies = 6,
                jobType = "Full-time",
                applicationDeadline = "2024-12-26",
                companySize = "1000+ employees",
                industry = "Security Services"
            ),
            JobCardModel(
                jobId = "8",
                title = "Electrician",
                employerName = "BuildCon Solutions",
                payInfo = PayInfo("800", PayType.DAILY, "day"),
                location = LocationInfo("Kondapur", "Hyderabad", "3.3"),
                tags = listOf(
                    CommonTags.IMMEDIATE_HIRING,
                    CommonTags.TRANSPORT_PROVIDED,
                    CommonTags.WEEKLY_PAYMENT
                ),
                timeInfo = TimeInfo("1 hr ago", UrgencyLevel.IMMEDIATE),
                isVerifiedEmployer = false,
                phoneNumber = "+91 9876543217",
                description = "Skilled electrician needed for construction projects. Install wiring, troubleshoot electrical issues, and maintain electrical systems. Immediate joining required for ongoing projects.",
                requirements = listOf("ITI in Electrical", "3+ years experience", "Knowledge of electrical codes", "Own basic tools", "Safety consciousness"),
                benefits = listOf("High daily wage ₹800", "Weekly payment", "Transport provided", "Overtime opportunities", "Safety equipment provided"),
                workingHours = "8 AM - 6 PM (10 hours with breaks)",
                experienceRequired = "3+ years",
                ageRange = "22-55 years",
                gender = "Male",
                vacancies = 4,
                jobType = "Contract",
                applicationDeadline = "2024-12-15",
                companySize = "100-200 employees",
                industry = "Construction"
            )
        )
    }
}