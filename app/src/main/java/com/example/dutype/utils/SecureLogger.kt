package com.example.dutype.utils

import timber.log.Timber

/**
 * P0 SECURITY FIX: Secure Logger
 * 
 * Prevents PII (Personally Identifiable Information) from being logged.
 * All sensitive data is sanitized before logging to comply with GDPR and privacy regulations.
 * 
 * CRITICAL: Use this instead of Timber.d() for any user data
 * 
 * @author DutyPe Security Team
 * @since 2.4.1
 */
object SecureLogger {
    
    /**
     * Log with automatic PII sanitization
     * Replaces sensitive data with safe indicators
     */
    fun d(tag: String, message: String, vararg sensitiveData: Pair<String, Any?>) {
        val sanitizedMessage = sanitizeMessage(message, sensitiveData.toList())
        Timber.tag(tag).d(sanitizedMessage)
    }
    
    /**
     * Log info with PII sanitization
     */
    fun i(tag: String, message: String, vararg sensitiveData: Pair<String, Any?>) {
        val sanitizedMessage = sanitizeMessage(message, sensitiveData.toList())
        Timber.tag(tag).i(sanitizedMessage)
    }
    
    /**
     * Log warning with PII sanitization
     */
    fun w(tag: String, message: String, vararg sensitiveData: Pair<String, Any?>) {
        val sanitizedMessage = sanitizeMessage(message, sensitiveData.toList())
        Timber.tag(tag).w(sanitizedMessage)
    }
    
    /**
     * Log error with PII sanitization
     */
    fun e(tag: String, throwable: Throwable?, message: String, vararg sensitiveData: Pair<String, Any?>) {
        val sanitizedMessage = sanitizeMessage(message, sensitiveData.toList())
        if (throwable != null) {
            Timber.tag(tag).e(throwable, sanitizedMessage)
        } else {
            Timber.tag(tag).e(sanitizedMessage)
        }
    }
    
    /**
     * Sanitize message by replacing sensitive data with safe indicators
     */
    private fun sanitizeMessage(message: String, sensitiveData: List<Pair<String, Any?>>): String {
        var sanitized = message
        
        sensitiveData.forEach { (key, value) ->
            val safeValue = when {
                value == null -> "<null>"
                key.contains("phone", ignoreCase = true) -> maskPhone(value.toString())
                key.contains("email", ignoreCase = true) -> maskEmail(value.toString())
                key.contains("address", ignoreCase = true) -> "<address_exists: ${value.toString().isNotBlank()}>"
                key.contains("url", ignoreCase = true) -> "<url_exists: ${value.toString().isNotBlank()}>"
                key.contains("token", ignoreCase = true) -> "<token_exists: ${value.toString().isNotBlank()}>"
                key.contains("password", ignoreCase = true) -> "<redacted>"
                key.contains("secret", ignoreCase = true) -> "<redacted>"
                key.contains("key", ignoreCase = true) -> "<redacted>"
                else -> "<exists: ${value.toString().isNotBlank()}>"
            }
            
            // Replace the actual value in message with safe value
            sanitized = sanitized.replace(value.toString(), safeValue)
        }
        
        return sanitized
    }
    
    /**
     * Mask phone number - show only last 4 digits
     * Example: +919876543210 -> +91****3210
     */
    private fun maskPhone(phone: String): String {
        if (phone.length <= 4) return "****"
        val lastFour = phone.takeLast(4)
        val prefix = if (phone.startsWith("+")) "+" else ""
        val countryCode = if (phone.startsWith("+") && phone.length > 6) phone.substring(1, 3) else ""
        return "$prefix$countryCode****$lastFour"
    }
    
    /**
     * Mask email - show only first char and domain
     * Example: user@example.com -> u***@example.com
     */
    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return "***@***.com"
        val username = parts[0]
        val domain = parts[1]
        val maskedUsername = if (username.isNotEmpty()) "${username[0]}***" else "***"
        return "$maskedUsername@$domain"
    }
    
    /**
     * Check if data exists without logging the actual value
     */
    fun logDataExists(tag: String, fieldName: String, value: Any?): String {
        val exists = value != null && value.toString().isNotBlank()
        val message = "$fieldName exists: $exists"
        Timber.tag(tag).d(message)
        return message
    }
    
    /**
     * Log collection size without exposing data
     */
    fun logCollectionSize(tag: String, collectionName: String, size: Int) {
        Timber.tag(tag).d("$collectionName size: $size items")
    }
}

/**
 * Extension functions for easy migration from Timber
 */
fun Timber.Tree.secureD(message: String, vararg sensitiveData: Pair<String, Any?>) {
    SecureLogger.d("SecureLog", message, *sensitiveData)
}

fun Timber.Tree.secureI(message: String, vararg sensitiveData: Pair<String, Any?>) {
    SecureLogger.i("SecureLog", message, *sensitiveData)
}

fun Timber.Tree.secureW(message: String, vararg sensitiveData: Pair<String, Any?>) {
    SecureLogger.w("SecureLog", message, *sensitiveData)
}

fun Timber.Tree.secureE(throwable: Throwable? = null, message: String, vararg sensitiveData: Pair<String, Any?>) {
    SecureLogger.e("SecureLog", throwable, message, *sensitiveData)
}
