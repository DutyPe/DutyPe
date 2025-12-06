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
}
