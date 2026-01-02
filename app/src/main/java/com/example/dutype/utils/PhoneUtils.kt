package com.example.dutype.utils

/**
 * Phone number utility functions
 * CANONICAL implementation for phone normalization across the app
 * 
 * @author DutyPe Engineering Team
 * @since 2.0.0
 */
object PhoneUtils {
    
    /**
     * Normalize phone number for consistent comparison and storage
     * Removes country code (+91), spaces, dashes, parentheses
     * Returns last 10 digits (Indian phone number format)
     * 
     * @param phone Raw phone number string
     * @return Normalized 10-digit phone number
     */
    fun normalizePhone(phone: String): String {
        return phone
            .replace("+91", "")
            .replace("+", "")
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace(Regex("[^0-9]"), "") // Remove any remaining non-digits
            .trim()
            .takeLast(10) // Keep only last 10 digits
    }
    
    /**
     * Format phone number for display with country code
     * 
     * @param phone Normalized or raw phone number
     * @return Formatted phone number with +91 prefix
     */
    fun formatForDisplay(phone: String): String {
        val normalized = normalizePhone(phone)
        return if (normalized.length == 10) "+91 $normalized" else phone
    }
    
    /**
     * Format phone number for WhatsApp URL
     * 
     * @param phone Normalized or raw phone number
     * @return Phone number with 91 prefix (no +)
     */
    fun formatForWhatsApp(phone: String): String {
        val normalized = normalizePhone(phone)
        return "91$normalized"
    }
    
    /**
     * Check if phone number is valid Indian mobile number
     * Must be 10 digits starting with 6, 7, 8, or 9
     * 
     * @param phone Phone number to validate
     * @return true if valid Indian mobile number
     */
    fun isValidIndianMobile(phone: String): Boolean {
        val normalized = normalizePhone(phone)
        return normalized.length == 10 && normalized.firstOrNull() in '6'..'9'
    }
}
