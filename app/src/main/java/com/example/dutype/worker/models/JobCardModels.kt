package com.example.dutype.worker.models

import com.example.dutype.employer.models.PayType

/**
 * Pay information for job cards
 * Uses PayType from employer.models to avoid duplication
 */
data class PayInfo(
    val amount: String,
    val type: PayType,
    val period: String = ""
)

/**
 * Location information for job cards
 */
data class LocationInfo(
    val address: String,
    val city: String = "",
    val area: String = "",
    val distance: String = "N/A"
) {
    /**
     * Get short display text for job cards - area, city only (no full address)
     * This keeps the location concise so distance can be shown clearly
     */
    fun getDisplayText(): String {
        return when {
            area.isNotBlank() && city.isNotBlank() -> "$area, $city"
            city.isNotBlank() -> city
            area.isNotBlank() -> area
            // For full address, extract just the first part (area/locality)
            address.isNotBlank() -> {
                val parts = address.split(",").map { it.trim() }
                when {
                    parts.size >= 2 -> "${parts[0]}, ${parts[1]}"
                    parts.isNotEmpty() -> parts[0]
                    else -> address.take(30)
                }
            }
            else -> "Location not specified"
        }
    }
}

/**
 * Time information for job cards
 */
data class TimeInfo(
    val shiftTiming: String = "",
    val workingDays: String = "",
    val startTime: String = "",
    val endTime: String = ""
) {
    fun getDisplayText(): String {
        return when {
            shiftTiming.isNotBlank() -> shiftTiming
            startTime.isNotBlank() && endTime.isNotBlank() -> "$startTime - $endTime"
            workingDays.isNotBlank() -> workingDays
            else -> "Flexible timing"
        }
    }
}
