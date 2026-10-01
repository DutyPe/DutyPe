package com.example.dutype.employer.models

import com.example.dutype.firestore.FirestoreSchema.Values

/** jobmetadata.payType — [key] is the stored value. */
enum class PayType(val key: String, val displayName: String, val perUnit: String) {
    DAILY(Values.PayType.DAILY, "Daily", "day"),
    MONTHLY(Values.PayType.MONTHLY, "Monthly", "month"),
    HOURLY(Values.PayType.HOURLY, "Hourly", "hour"),
    NEGOTIABLE(Values.PayType.NEGOTIABLE, "Negotiable", "");

    companion object {
        fun fromKey(key: String?): PayType =
            entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) } ?: DAILY
    }
}

/** jobmetadata.employmentType — [key] is the stored value. */
enum class EmploymentType(val key: String, val displayName: String, val titleRes: Int) {
    FULL_TIME(Values.EmploymentType.FULL_TIME, "Full-time", com.dutype.app.R.string.full_time),
    PART_TIME(Values.EmploymentType.PART_TIME, "Part-time", com.dutype.app.R.string.part_time),
    DAILY(Values.EmploymentType.DAILY, "Daily wage", com.dutype.app.R.string.urgent_need_daily_wage);

    companion object {
        fun fromKey(key: String?): EmploymentType =
            entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) } ?: FULL_TIME
    }
}

/** jobmetadata.shift — [key] is the stored value. */
enum class JobShift(val key: String, val displayName: String) {
    DAY(Values.Shift.DAY, "Day shift"),
    NIGHT(Values.Shift.NIGHT, "Night shift"),
    ANY(Values.Shift.ANY, "Any shift");

    companion object {
        fun fromKey(key: String?): JobShift =
            entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) } ?: ANY
    }
}

/** jobmetadata.urgency — [key] is the stored value. */
enum class JobUrgency(val key: String) {
    NORMAL(Values.Urgency.NORMAL),
    HIGH(Values.Urgency.HIGH);

    companion object {
        fun fromKey(key: String?): JobUrgency =
            entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) } ?: NORMAL
    }
}

/** jobmetadata.category — the enum name is the stored value. */
enum class JobCategory(val displayName: String, val icon: String, val titleRes: Int) {
    COOK("Cook", "👨‍🍳", com.dutype.app.R.string.category_cook),
    MAID("Maid", "🧹", com.dutype.app.R.string.category_maid),
    DRIVER("Driver", "🚗", com.dutype.app.R.string.category_driver),
    HELPER("Helper", "🤝", com.dutype.app.R.string.category_helper),
    SECURITY("Security", "🛡️", com.dutype.app.R.string.category_security),
    GARDENER("Gardener", "🌱", com.dutype.app.R.string.category_gardener),
    CARETAKER("Caretaker", "👥", com.dutype.app.R.string.category_caretaker),
    DELIVERY("Delivery", "📦", com.dutype.app.R.string.category_delivery),
    WAITER("Waiter", "🍽️", com.dutype.app.R.string.category_waiter),
    ELECTRICIAN("Electrician", "⚡", com.dutype.app.R.string.category_electrician),
    PLUMBER("Plumber", "🔧", com.dutype.app.R.string.category_plumber),
    PAINTER("Painter", "🎨", com.dutype.app.R.string.category_painter),
    CARPENTER("Carpenter", "🪚", com.dutype.app.R.string.category_carpenter),
    RECEPTIONIST("Receptionist", "💼", com.dutype.app.R.string.category_receptionist),
    CASHIER("Cashier", "💵", com.dutype.app.R.string.category_cashier),
    PACKER("Packer", "📦", com.dutype.app.R.string.category_packer),
    SALES("Sales", "🛍️", com.dutype.app.R.string.category_sales),
    TELECALLER("Telecaller", "📞", com.dutype.app.R.string.category_telecaller),
    TEACHER("Teacher", "📚", com.dutype.app.R.string.category_teacher),
    OFFICE_STAFF("Office Staff", "🗂️", com.dutype.app.R.string.category_office_staff),
    CUSTOMER_SUPPORT("Customer Support", "🎧", com.dutype.app.R.string.category_customer_support),
    FIELD_EXECUTIVE("Field Work", "🧭", com.dutype.app.R.string.category_field_executive),
    MARKETING("Marketing", "📣", com.dutype.app.R.string.category_marketing),
    FINANCE("Finance", "🏦", com.dutype.app.R.string.category_finance),
    HEALTHCARE("Healthcare", "⚕️", com.dutype.app.R.string.category_healthcare),
    BEAUTICIAN("Beautician", "💇", com.dutype.app.R.string.category_beautician),
    TAILOR("Tailor", "🧵", com.dutype.app.R.string.category_tailor),
    MECHANIC("Mechanic", "🔩", com.dutype.app.R.string.category_mechanic),
    DATA_ENTRY("Data Entry", "⌨️", com.dutype.app.R.string.category_data_entry),
    LEGAL("Legal", "⚖️", com.dutype.app.R.string.category_legal),
    OTHER("Other", "📋", com.dutype.app.R.string.category_other);

    companion object {
        fun fromKey(key: String?): JobCategory =
            entries.firstOrNull { it.name.equals(key?.trim(), ignoreCase = true) } ?: OTHER
    }
}
