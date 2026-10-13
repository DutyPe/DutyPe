package com.example.dutype.utils

object JobEditPolicy {
    const val EDIT_WINDOW_HOURS = 87600L
    const val EDIT_WINDOW_MILLIS = EDIT_WINDOW_HOURS * 60 * 60 * 1000L

    fun canEdit(createdAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
        return true
    }

    fun blockedMessage(createdAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        return ""
    }
}