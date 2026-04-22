package com.example.dutype.utils

/**
 * Bug #6: salary is stored verbatim as a String. This parser only trims
 * whitespace and strips a leading currency symbol — values like
 * "Negotiable", "1000-2000", "2000+" all flow through to Firestore
 * unchanged. Numeric features (filtering, earnings) call
 * [SalaryFormatter.lowerBound] on read.
 */
object PayAmountParser {

    /** Result wrapper kept for API compatibility with older call sites. */
    data class Result(
        /** Lower numeric bound used by call sites that need a Double. */
        val numeric: Double,
        /** The salary string to send to Firestore (trimmed, ₹ stripped). */
        val text: String
    ) {
        /** Backwards-compat alias used by some older call sites. */
        val displayText: String? get() = text.takeIf { it.isNotBlank() }
    }

    fun parse(input: String): Result {
        val cleaned = input.trim().removePrefix("₹").trim().replace(",", "")
        val lower = SalaryFormatter.lowerBound(cleaned)
        return Result(numeric = lower, text = cleaned)
    }
}

