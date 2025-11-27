package com.example.dutype.employer.models.enums

enum class PayType(val displayName: String) {
    DAILY("Daily"),
    HOURLY("Hourly"),
    MONTHLY("Monthly"),
    TASK("Per Task")
}

enum class EmployerPayType(val displayName: String) {
    DAILY("Daily"),
    HOURLY("Hourly"),
    MONTHLY("Monthly"),
    PER_TASK("Per Task")
}

enum class JobCategory(val displayName: String, val icon: String) {
    COOK("Cook", "👨‍🍳"),
    MAID("Maid/Cleaner", "🧹"),
    DRIVER("Driver", "🚗"),
    HELPER("Helper", "🤝"),
    SECURITY("Security", "🛡️"),
    GARDENER("Gardener", "🌱"),
    CARETAKER("Caretaker", "👥"),
    DELIVERY("Delivery", "📦"),
    // HELP("Help/link", "❓")

}

enum class ShiftTiming(val displayName: String, val icon: String) {
    MORNING("Morning (6 AM - 12 PM)", "☀️"),
    AFTERNOON("Afternoon (12 PM - 6 PM)", "🏙️"),
    EVENING("Evening (6 PM - 12 AM)", "🌆"),
    NIGHT("Night (12 AM - 6 AM)", "🌙"),
    FULL_DAY("Full Day", "⏰"),
    FLEXIBLE("Flexible", "⏳")
}

enum class JobUrgency(val displayName: String) {
    IMMEDIATE("Today"),
    URGENT("Within 3days"),
    NORMAL("Within 1week"),
    FLEXIBLE("Flexible")
}

enum class JobPerk(val displayName: String, val icon: String) {
    MEALS("Free Meals", "🍽️"),
    TRANSPORT("Transport Provided", "🚌"),
    ACCOMMODATION("Accommodation", "🏠"),
    OVERTIME_PAY("Overtime Pay", "💰"),
    BONUS("Performance Bonus", "🎯"),
    MEDICAL("Medical Benefits", "🏥"),
    PAID_LEAVES("Paid Leaves", "📅"),
    TRAINING("Training Provided", "📚")
}

enum class ApplicationStatus(val displayName: String, val color: String) {
    PENDING("Pending", "#F59E0B"),
    VIEWED("Viewed", "#3B82F6"),
    SHORTLISTED("Shortlisted", "#10B981"),
    SELECTED("Selected", "#059669"),
    REJECTED("Rejected", "#EF4444"),
    INTERVIEW_SCHEDULED("Interview Scheduled", "#8B5CF6"),
    DOCUMENTS_PENDING("Documents Pending", "#F59E0B"),
    HIRED("Hired", "#10B981"),
    WITHDRAWN("Withdrawn", "#6B7280")
}
