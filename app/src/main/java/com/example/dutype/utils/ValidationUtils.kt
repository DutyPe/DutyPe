package com.example.dutype.utils

import android.util.Patterns

/**
 * Centralized validation utilities to eliminate code duplication
 * Used across profile setup, job posting, and application forms
 */
object ValidationUtils {
    
    /**
     * Validate Indian phone number
     * Accepts formats: +91XXXXXXXXXX, 91XXXXXXXXXX, or XXXXXXXXXX
     * Must start with 6-9 and be exactly 10 digits (excluding country code)
     */
    fun isValidIndianPhoneNumber(phone: String): Boolean {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "") // Remove non-numeric
            .removePrefix("91") // Remove country code if present
        return cleanPhone.length == 10 && cleanPhone.firstOrNull() in '6'..'9'
    }
    
    /**
     * Validate email address using Android's built-in pattern matcher
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    /**
     * Validate full name (at least 2 characters, alphabets and spaces only)
     */
    fun isValidFullName(name: String): Boolean {
        val nameRegex = Regex("^[a-zA-Z\\s]{2,}$")
        return name.isNotBlank() && name.length >= 2 && nameRegex.matches(name.trim())
    }
    
    /**
     * Validate salary/pay amount (positive number)
     */
    fun isValidSalary(salary: String): Boolean {
        return salary.toDoubleOrNull()?.let { it > 0 } ?: false
    }
    
    /**
     * Validate URL format
     */
    fun isValidUrl(url: String): Boolean {
        return url.isNotBlank() && Patterns.WEB_URL.matcher(url).matches()
    }
    
    /**
     * Sanitize user input to prevent XSS and injection attacks
     */
    fun String.sanitize(): String {
        return this.trim()
            .replace(Regex("<[^>]*>"), "") // Remove HTML tags
            .replace(Regex("\\s+"), " ") // Normalize whitespace
    }
    
    /**
     * Validate cover letter length (minimum 50 characters)
     */
    fun isValidCoverLetter(text: String): Boolean {
        return text.trim().length >= 50
    }
    
    /**
     * Validate company name
     */
    fun isValidCompanyName(name: String): Boolean {
        return name.isNotBlank() && name.length >= 2
    }
    
    /**
     * Validate address (minimum 10 characters, not just special chars)
     */
    fun isValidAddress(address: String): Boolean {
        val trimmedAddress = address.trim()
        // Must be at least 10 characters
        if (trimmedAddress.length < 10) return false
        // Must contain at least one alphanumeric character (not just special chars)
        return trimmedAddress.any { it.isLetterOrDigit() }
    }
    
    /**
     * Validate date of birth (18-70 years old)
     * Returns true if age is between 18 and 70 (inclusive)
     */
    fun isValidDateOfBirth(dateString: String, format: String = "dd/MM/yyyy"): Boolean {
        if (dateString.isBlank()) return false
        return try {
            val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
            sdf.isLenient = false
            val birthDate = sdf.parse(dateString) ?: return false
            val today = java.util.Calendar.getInstance()
            val birth = java.util.Calendar.getInstance().apply { time = birthDate }
            
            var age = today.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR)
            if (today.get(java.util.Calendar.DAY_OF_YEAR) < birth.get(java.util.Calendar.DAY_OF_YEAR)) {
                age--
            }
            age in 18..70
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get age from date of birth string
     * Returns null if date is invalid
     */
    fun getAgeFromDateOfBirth(dateString: String, format: String = "dd/MM/yyyy"): Int? {
        if (dateString.isBlank()) return null
        return try {
            val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
            sdf.isLenient = false
            val birthDate = sdf.parse(dateString) ?: return null
            val today = java.util.Calendar.getInstance()
            val birth = java.util.Calendar.getInstance().apply { time = birthDate }
            
            var age = today.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR)
            if (today.get(java.util.Calendar.DAY_OF_YEAR) < birth.get(java.util.Calendar.DAY_OF_YEAR)) {
                age--
            }
            age
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get date of birth validation error message
     * Returns appropriate error message based on age
     */
    fun getDateOfBirthError(dateString: String): String? {
        if (dateString.isBlank()) return "Date of birth is required"
        val age = getAgeFromDateOfBirth(dateString) ?: return "Please enter a valid date of birth"
        return when {
            age < 18 -> "You must be at least 18 years old to register"
            age > 70 -> "Age cannot exceed 70 years"
            else -> null
        }
    }
    
    /**
     * Validate that at least one item is selected from a list
     */
    fun hasMinimumSelection(items: Set<String>, minimum: Int = 1): Boolean {
        return items.size >= minimum
    }
    
    /**
     * Get phone validation error message
     */
    fun getPhoneError(phone: String, showError: Boolean): String? {
        if (!showError) return null
        if (phone.isBlank()) return "Phone number is required"
        val cleanPhone = phone.replace(Regex("[^0-9]"), "").removePrefix("91")
        if (cleanPhone.length != 10) return "Phone number must be exactly 10 digits"
        if (cleanPhone.firstOrNull() !in '6'..'9') return "Phone number must start with 6, 7, 8, or 9"
        return null
    }
    
    /**
     * Error message constants for consistent validation feedback
     */
    object ErrorMessages {
        const val FULL_NAME = "Please enter your full name (first and last name)"
        const val ADDRESS = "Please enter a valid address (minimum 10 characters)"
        const val DOB_UNDERAGE = "You must be at least 18 years old to register"
        const val DOB_OVERAGE = "Age cannot exceed 70 years"
        const val DOB_INVALID = "Please enter a valid date of birth"
        const val DOB_REQUIRED = "Date of birth is required"
        const val COMPANY_NAME = "Please enter a valid company name"
        const val WEBSITE = "Please enter a valid website URL (e.g., https://example.com)"
        const val SKILLS = "Please select at least one skill"
        const val INDUSTRY = "Please select at least one industry"
        const val PHONE = "Enter a valid 10-digit phone number"
        const val PHONE_REQUIRED = "Phone number is required"
        const val PHONE_LENGTH = "Phone number must be exactly 10 digits"
        const val PHONE_START = "Phone number must start with 6, 7, 8, or 9"
        const val EMAIL = "Enter a valid email address"
        const val GENDER = "Please select your gender"
        const val EXPERIENCE = "Please select your experience level"
        fun phoneExistsWithRole(role: String): String = "This phone number is already registered as ${role.lowercase()}. Please use a different number or login with that account."
    }
}
