package com.example.dutype.utils

/**
 * Single source of truth for parsing the free-form `salary` string
 * stored on `jobmetadata` (and propagated via [com.example.dutype.models.JobListing]).
 *
 * Bug #6: the salary field is intentionally a String so the employer's
 * input is preserved verbatim — "Negotiable", "1000-2000", "2000+", or
 * a plain number. Numeric features (filtering, earnings, sorting) call
 * [lowerBound] / [upperBound] to extract a comparable Double.
 */
object SalaryFormatter {

    /**
     * Display the salary on a card. Returns "Negotiable" when blank,
     * otherwise returns the raw text with the appropriate period suffix
     * if the text doesn't already include one.
     */
    fun display(salary: String, salaryType: String): String {
        val trimmed = salary.trim()
        if (trimmed.isEmpty()) return "Negotiable"
        val period = when (salaryType.uppercase()) {
            "HOURLY" -> "hour"
            "MONTHLY" -> "month"
            "TASK" -> "task"
            else -> "day"
        }
        val lower = trimmed.lowercase()
        val alreadyHasPeriod = lower.contains("/") ||
            lower.contains("per ") ||
            lower.contains("hour") ||
            lower.contains("day") ||
            lower.contains("month") ||
            lower.contains("week") ||
            lower.contains("year")
        // Pure text like "Negotiable" / "Based on experience" — no period.
        val isPureText = !trimmed.any { it.isDigit() }
        return if (alreadyHasPeriod || isPureText) trimmed else "$trimmed/$period"
    }

    /**
     * Lower numeric bound used for filter / sort comparisons. Returns 0.0
     * when the salary is non-numeric (e.g. "Negotiable").
     */
    fun lowerBound(salary: String): Double {
        val cleaned = salary.replace("₹", "").replace(",", "").trim()
        if (cleaned.isEmpty()) return 0.0
        // "2000+" → 2000
        if (cleaned.endsWith("+")) {
            return cleaned.dropLast(1).trim().toDoubleOrNull() ?: 0.0
        }
        // "1000-2000" → 1000
        if (cleaned.contains('-')) {
            val first = cleaned.split('-').firstOrNull()?.trim()?.toDoubleOrNull()
            if (first != null) return first
        }
        // Plain number.
        cleaned.toDoubleOrNull()?.let { return it }
        // Pull first number from mixed text ("From 5000").
        return Regex("\\d+(?:\\.\\d+)?")
            .find(cleaned)
            ?.value
            ?.toDoubleOrNull()
            ?: 0.0
    }

    /**
     * Upper numeric bound. Returns [Double.MAX_VALUE] for "2000+" so the
     * salary always passes a "max <= filterMax" predicate when the
     * employer left it open-ended.
     */
    fun upperBound(salary: String): Double {
        val cleaned = salary.replace("₹", "").replace(",", "").trim()
        if (cleaned.isEmpty()) return 0.0
        if (cleaned.endsWith("+")) return Double.MAX_VALUE
        if (cleaned.contains('-')) {
            val parts = cleaned.split('-')
            val last = parts.lastOrNull()?.trim()?.toDoubleOrNull()
            if (last != null) return last
        }
        return cleaned.toDoubleOrNull()
            ?: Regex("\\d+(?:\\.\\d+)?")
                .findAll(cleaned)
                .mapNotNull { it.value.toDoubleOrNull() }
                .lastOrNull()
            ?: 0.0
    }

    /**
     * Earnings amount per completed application. Uses the lower bound
     * (conservative estimate) and falls back to 0 for non-numeric posts.
     */
    fun numericForEarnings(salary: String): Double = lowerBound(salary)
}
