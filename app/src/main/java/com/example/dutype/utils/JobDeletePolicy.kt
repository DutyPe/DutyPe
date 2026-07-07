package com.example.dutype.utils

object JobDeletePolicy {
    const val DELETE_WINDOW_MINUTES = 30L
    const val DELETE_WINDOW_MILLIS = DELETE_WINDOW_MINUTES * 60 * 1000L

    const val INSTANT_DELETE_WINDOW_MINUTES = 5L
    const val INSTANT_DELETE_WINDOW_MILLIS = INSTANT_DELETE_WINDOW_MINUTES * 60 * 1000L

    fun canDeleteNormal(createdAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
        return createdAtMillis <= 0L || nowMillis - createdAtMillis <= DELETE_WINDOW_MILLIS
    }

    fun canDeleteInstant(createdAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
        return createdAtMillis <= 0L || nowMillis - createdAtMillis <= INSTANT_DELETE_WINDOW_MILLIS
    }

    fun blockedMessage(): String {
        return "Jobs can only be deleted within 30 minutes of posting."
    }
}
