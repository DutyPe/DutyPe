package com.example.dutype.utils

/**
 * Parses the free-form pay-amount input from the post-job form into a numeric
 * value compatible with the strict Firestore schema (`salary >= 1`).
 *
 * Supported inputs (case-insensitive, comma / ₹ allowed anywhere):
 *  - Plain: "18000"            -> Result(18000.0,  null)
 *  - Range: "15000-20000"      -> Result(15000.0,  "15000-20000")  // lower bound
 *  - Plus:  "18000+"           -> Result(18000.0,  "18000+")
 *  - Text:  "Negotiable"       -> Result(1.0,      "Negotiable")    // positive sentinel
 *  - Mixed: "Based on exp"     -> Result(1.0,      "Based on exp")
 *
 * Why a positive sentinel for text-only inputs:
 *  - [com.example.dutype.services.firestore.JobFirestoreService.createJob] and
 *    the Firestore rules require `salary > 0` for the post to succeed. Storing
 *    `1.0` keeps the post valid while the human-readable original text is
 *    surfaced via [displayText] for callers that want to show it back.
 *
 * @param numeric Filterable / sortable numeric value (≥ 1.0).
 * @param displayText The user's original text if it wasn't a plain number;
 *   `null` when the input was already a clean number with no extras.
 */
object PayAmountParser {

    data class Result(val numeric: Double, val displayText: String?)

    fun parse(input: String): Result {
        val raw = input.trim()
        if (raw.isEmpty()) return Result(0.0, null)

        // Strip currency symbol + thousands separators; keep digits, dot,
        // dash, plus, and letters so we can detect ranges / "+" / pure text.
        val cleaned = raw.replace("₹", "").replace(",", "").trim()

        // Plain number ("18000", "18000.5") — fast path.
        cleaned.toDoubleOrNull()?.let { return Result(it.coerceAtLeast(1.0), null) }

        // Range: "15000-20000" -> use lower bound for filtering, keep original text.
        if (cleaned.contains('-')) {
            val lower = cleaned.split('-').firstOrNull()?.trim()?.toDoubleOrNull()
            if (lower != null && lower > 0.0) return Result(lower, raw)
        }

        // Plus: "18000+" -> use the floor, keep original text.
        if (cleaned.endsWith('+')) {
            val floor = cleaned.dropLast(1).trim().toDoubleOrNull()
            if (floor != null && floor > 0.0) return Result(floor, raw)
        }

        // Pure text ("Negotiable", "Based on experience").
        return Result(1.0, raw)
    }
}
