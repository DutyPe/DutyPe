package com.example.dutype.utils

import com.example.dutype.employer.models.JobCategory

object JobCategoryResolver {
    private val aliases = mapOf(
        "ALL" to null,
        "ALL_JOBS" to null,
        "SHOP_HELPER" to JobCategory.HELPER,
        "HOUSEKEEPING" to JobCategory.MAID,
        "KITCHEN" to JobCategory.COOK,
        "EVENTS" to JobCategory.WAITER,
        "CONSTRUCTION" to JobCategory.HELPER,
        "FIELD_WORK" to JobCategory.FIELD_EXECUTIVE,
        "ADMIN" to JobCategory.OFFICE_STAFF,
        "OFFICE_ADMIN" to JobCategory.OFFICE_STAFF,
        "OFFICE_BOY" to JobCategory.HELPER,
        "CALL_CENTER" to JobCategory.CUSTOMER_SUPPORT,
        "CUSTOMER_SERVICE" to JobCategory.CUSTOMER_SUPPORT,
        "NURSE" to JobCategory.HEALTHCARE,
        "MEDICAL" to JobCategory.HEALTHCARE,
        "INSURANCE" to JobCategory.FINANCE,
        "BANKING" to JobCategory.FINANCE
    )

    private val keywordRules: List<Pair<JobCategory, List<String>>> = listOf(
        JobCategory.DELIVERY to listOf("delivery", "courier", "swiggy", "zomato", "dunzo", "parcel", "last mile", "rider", "bike rider"),
        JobCategory.DRIVER to listOf("driver", "chauffeur", "cab", "taxi", "ola", "uber", "rapido", "truck"),
        JobCategory.COOK to listOf("cook", "chef", "kitchen", "tandoor", "chapati", "biryani", "catering"),
        JobCategory.MAID to listOf("maid", "housekeep", "domestic", "house cleaner", "cleaner", "sweeper", "janitor", "house help"),
        JobCategory.SECURITY to listOf("security", "guard", "watchman", "bouncer"),
        JobCategory.GARDENER to listOf("gardener", "garden", "landscap", "lawn", "horticult"),
        JobCategory.CARETAKER to listOf("caretaker", "care taker", "nanny", "babysitter", "childcare", "elder care", "caregiver", "ayah"),
        JobCategory.WAITER to listOf("waiter", "waitress", "steward", "bartender", "server ", "banquet", "cafe staff", "restaurant staff", "hotel staff"),
        JobCategory.ELECTRICIAN to listOf("electric", "wiring"),
        JobCategory.PLUMBER to listOf("plumb", "pipe fit"),
        JobCategory.PAINTER to listOf("painter", "painting"),
        JobCategory.CARPENTER to listOf("carpenter", "carpentry", "woodwork"),
        JobCategory.RECEPTIONIST to listOf("receptionist", "front desk", "front office"),
        JobCategory.CASHIER to listOf("cashier", "billing", "checkout", "counter"),
        JobCategory.PACKER to listOf("packer", "packing", "packaging", "warehouse", "loader", "loading", "unloading"),
        JobCategory.SALES to listOf("sales", "retail", "shop sales", "store sales", "sales associate", "sales executive"),
        JobCategory.TELECALLER to listOf("telecaller", "tele caller", "telesales", "tele sales", "calling", "call center", "bpo", "voice process"),
        JobCategory.TEACHER to listOf("teacher", "tutor", "teaching", "trainer", "instructor", "pre primary", "pre-primary", "primary school"),
        JobCategory.OFFICE_STAFF to listOf("office admin", "office staff", "admin", "back office", "office assistant", "clerk", "office boy", "peon"),
        JobCategory.CUSTOMER_SUPPORT to listOf("customer support", "customer service", "support executive", "process associate", "service associate"),
        JobCategory.FIELD_EXECUTIVE to listOf("field executive", "field officer", "field work", "field sales", "field service", "promoter", "brand promoter"),
        JobCategory.MARKETING to listOf("marketing", "business development", "bde", "bd executive", "relationship manager"),
        JobCategory.FINANCE to listOf("loan", "finance", "banking", "collection", "insurance", "credit card", "home loan"),
        JobCategory.HEALTHCARE to listOf("nurse", "nursing", "medical", "healthcare", "patient care", "ward boy", "pharmacy"),
        JobCategory.BEAUTICIAN to listOf("beautician", "beauty", "salon", "makeup", "hair", "spa"),
        JobCategory.TAILOR to listOf("tailor", "stitching", "sewing", "garment", "alteration"),
        JobCategory.MECHANIC to listOf("mechanic", "auto mechanic", "vehicle repair", "garage", "technician"),
        JobCategory.DATA_ENTRY to listOf("data entry", "computer operator", "typing", "excel operator", "mis executive"),
        JobCategory.LEGAL to listOf("advocate", "legal", "lawyer", "law clerk"),
        JobCategory.HELPER to listOf("helper", "assistant", "labour", "labor", "construction", "mason", "site work")
    )

    fun enumNameForDisplay(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return null
        val token = raw.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_')
        aliases[token]?.let { return it.name }
        if (aliases.containsKey(token)) return null
        JobCategory.entries.firstOrNull { it.name == token }?.let { return it.name }
        JobCategory.entries.firstOrNull { it.displayName.equals(raw, ignoreCase = true) }?.let { return it.name }
        return null
    }

    fun inferCategory(title: String, description: String = ""): JobCategory? {
        val text = "$title $description".lowercase()
        if (text.length < 3) return null
        return keywordRules.firstOrNull { (_, keywords) ->
            keywords.any { text.contains(it) }
        }?.first
    }

    fun inferCategoryName(title: String, description: String = "", explicit: String? = null): String {
        val explicitName = enumNameForDisplay(explicit)
        if (!explicitName.isNullOrBlank() && explicitName != JobCategory.OTHER.name) return explicitName
        return inferCategory(title, description)?.name ?: JobCategory.OTHER.name
    }

    fun matchesCategory(title: String, description: String, categoryName: String): Boolean {
        val expected = enumNameForDisplay(categoryName) ?: categoryName.uppercase()
        return inferCategory(title, description)?.name == expected
    }

    fun displayNameForName(categoryName: String): String {
        return runCatching { JobCategory.valueOf(categoryName.uppercase()).displayName }.getOrDefault(categoryName)
    }

    fun searchQueryTokens(query: String): List<String> {
        val tokens = linkedSetOf<String>()

        fun addTokens(value: String) {
            value.lowercase()
                .replace(Regex("[^a-z0-9]+"), " ")
                .split(' ')
                .map { it.trim() }
                .filter { it.length >= 2 }
                .forEach { tokens += it }
        }

        addTokens(query)

        val categoryName = enumNameForDisplay(query) ?: inferCategory(query)?.name
        if (!categoryName.isNullOrBlank()) {
            addTokens(categoryName)
            addTokens(displayNameForName(categoryName))
        }

        return tokens.take(10)
    }
}
