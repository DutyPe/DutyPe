package com.example.dutype.utils

object JobDeletePolicy {
    fun canDeleteNormal(createdAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
        return false
    }

    fun canDeleteInstant(createdAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
        return false
    }

    fun blockedMessage(): String {
        return "Jobs cannot be deleted once posted. You can pause or close the job instead."
    }
}
