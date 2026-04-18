package com.example.dutype.employer.models

enum class PayType(val displayName: String) {
    DAILY("Daily"),
    HOURLY("Hourly"),
    MONTHLY("Monthly"),
    TASK("Per Task")
}

enum class JobCategory(val displayName: String, val icon: String) {
    COOK("Cook", "👨‍🍳"),
    MAID("Maid", "🧹"),
    DRIVER("Driver", "🚗"),
    HELPER("Helper", "🤝"),
    SECURITY("Security", "🛡️"),
    GARDENER("Gardener", "🌱"),
    CARETAKER("Caretaker", "👥"),
    DELIVERY("Delivery", "📦"),
    WAITER("Waiter", "🍽️"),
    ELECTRICIAN("Electrician", "⚡"),
    PLUMBER("Plumber", "🔧"),
    PAINTER("Painter", "🎨"),
    CARPENTER("Carpenter", "🪚"),
    RECEPTIONIST("Receptionist", "💼"),
    CASHIER("Cashier", "💵"),
    PACKER("Packer", "📦"),
    OTHER("Other", "📋")
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
