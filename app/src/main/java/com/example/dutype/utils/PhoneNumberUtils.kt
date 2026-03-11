package com.example.dutype.utils

/**
 * PhoneNumberUtils - Utility for phone number normalization and validation
 * 
 * CRITICAL: Ensures consistent phone number format across the app
 * Fixes login issues caused by phone format mismatches
 * 
 * Standard Format: +{countryCode}{number}
 * Example: +919876543210
 */
object PhoneNumberUtils {
    
    /**
     * Normalize phone number to consistent format
     * Always returns: +{countryCode}{number}
     * 
     * Examples:
     * - "9876543210" -> "+919876543210" (adds +91 for India)
     * - "919876543210" -> "+919876543210" (adds +)
     * - "+919876543210" -> "+919876543210" (already normalized)
     * 
     * @param phoneNumber Input phone number in any format
     * @param defaultCountryCode Default country code (default: "91" for India)
     * @return Normalized phone number with + prefix
     */
    fun normalize(phoneNumber: String, defaultCountryCode: String = "91"): String {
        // Remove all non-digit characters except +
        val cleaned = phoneNumber.replace(Regex("[^0-9+]"), "")
        
        return when {
            // Already has + prefix
            cleaned.startsWith("+") -> cleaned
            
            // Has country code but no + prefix
            cleaned.startsWith(defaultCountryCode) -> "+$cleaned"
            
            // Only has phone number, add country code
            else -> "+$defaultCountryCode$cleaned"
        }
    }
    
    /**
     * Get all possible phone number variants for lookup
     * Returns list of variants to try when searching for user
     * 
     * Example for +919876543210:
     * - +919876543210 (normalized)
     * - 919876543210 (without +)
     * - 9876543210 (without country code)
     * 
     * @param phoneNumber Input phone number
     * @return List of phone number variants to try
     */
    fun getVariants(phoneNumber: String): List<String> {
        val normalized = normalize(phoneNumber)
        
        return listOf(
            normalized,                           // +919876543210
            normalized.removePrefix("+"),         // 919876543210
            normalized.removePrefix("+91")        // 9876543210 (India specific)
        ).distinct()
    }
    
    /**
     * Validate phone number format
     * 
     * @param phoneNumber Phone number to validate
     * @return true if valid, false otherwise
     */
    fun isValid(phoneNumber: String): Boolean {
        val cleaned = phoneNumber.replace(Regex("[^0-9+]"), "")
        
        // Must have at least 10 digits
        val digitCount = cleaned.count { it.isDigit() }
        if (digitCount < 10) return false
        
        // If has +, must be followed by digits
        if (cleaned.contains("+") && !cleaned.startsWith("+")) return false
        
        return true
    }
    
    /**
     * Format phone number for display
     * Example: +919876543210 -> +91 98765 43210
     * 
     * @param phoneNumber Phone number to format
     * @return Formatted phone number for display
     */
    fun formatForDisplay(phoneNumber: String): String {
        val normalized = normalize(phoneNumber)
        
        // India format: +91 XXXXX XXXXX
        if (normalized.startsWith("+91")) {
            val number = normalized.removePrefix("+91")
            if (number.length == 10) {
                return "+91 ${number.substring(0, 5)} ${number.substring(5)}"
            }
        }
        
        // Default: just add space after country code
        return normalized.replaceFirst(Regex("^(\\+\\d{1,3})"), "$1 ")
    }

    /**
     * Normalize phone number to 10-digit Indian format (without country code).
     * Used for Firestore queries where phone is stored as 10 digits.
     *
     * @param phone Raw phone number string
     * @return Normalized 10-digit phone number
     */
    fun normalizePhone(phone: String): String {
        return phone
            .replace(Regex("[^0-9]"), "")
            .takeLast(10)
    }

    /**
     * Format phone number for WhatsApp URL (91XXXXXXXXXX, no +).
     *
     * @param phone Raw or normalized phone number
     * @return Phone number with 91 prefix suitable for wa.me links
     */
    fun formatForWhatsApp(phone: String): String {
        val digits = normalizePhone(phone)
        return "91$digits"
    }

    /**
     * Check if phone number is a valid Indian mobile number.
     * Must be 10 digits starting with 6, 7, 8, or 9.
     *
     * @param phone Phone number to validate
     * @return true if valid Indian mobile number
     */
    fun isValidIndianMobile(phone: String): Boolean {
        val digits = normalizePhone(phone)
        return digits.length == 10 && digits.firstOrNull() in '6'..'9'
    }
}
