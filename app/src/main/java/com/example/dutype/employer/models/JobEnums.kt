package com.example.dutype.employer.models

enum class PayType(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
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
    MORNING("Day shift", "☀️"),
    AFTERNOON("Day shift", "🏙️"),
    EVENING("Night shift", "🌆"),
    NIGHT("Night shift", "🌙"),
    BOTH("Both shift", ""),
    FULL_DAY("Any shift", "⏰"),
    // Batch-p #3: employer can specify their own start/end timing.
    CUSTOM("Any shift", "🕒"),
    FLEXIBLE("Any shift", "⏳")
}
