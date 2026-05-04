package com.example.dutype.utils

object JobEditPolicy {
    const val EDIT_WINDOW_HOURS = 48L
    const val EDIT_WINDOW_MILLIS = EDIT_WINDOW_HOURS * 60 * 60 * 1000L

    fun canEdit(createdAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
        return createdAtMillis <= 0L || nowMillis - createdAtMillis <= EDIT_WINDOW_MILLIS
    }

    fun blockedMessage(createdAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val ageMillis = (nowMillis - createdAtMillis).coerceAtLeast(0L)
        val hours = (ageMillis / (60 * 60 * 1000L)).coerceAtLeast(1L)
        val ageText = if (hours < 72L) "$hours hour${if (hours == 1L) "" else "s"}" else {
            val days = (hours / 24L).coerceAtLeast(1L)
            "$days day${if (days == 1L) "" else "s"}"
        }
        return "Jobs can only be edited within 48 hours of posting. This job was posted $ageText ago."
    }
}