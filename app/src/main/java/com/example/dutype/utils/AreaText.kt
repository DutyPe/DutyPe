package com.example.dutype.utils

/**
 * The short `area` stored on job cards and profiles ("Madhapur, Hyderabad"), taken from a full
 * geocoded address. Drops house numbers, PIN codes, state and country, keeps the last two
 * meaningful parts (locality, city).
 */
object AreaText {
    private val DROP = setOf(
        "india", "telangana", "andhra pradesh", "karnataka", "tamil nadu", "maharashtra", "odisha"
    )

    fun from(address: String): String {
        val parts = address.split(',')
            .map { it.trim() }
            .filter { part ->
                part.isNotBlank() &&
                    part.lowercase() !in DROP &&
                    part.count { it.isDigit() } < 3
            }
        val area = parts.takeLast(2).joinToString(", ").ifBlank { address.trim() }
        return area.take(MAX_LENGTH)
    }

    private const val MAX_LENGTH = 60
}
