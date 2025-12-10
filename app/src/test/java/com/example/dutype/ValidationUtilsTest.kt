package com.example.dutype

import com.example.dutype.utils.ValidationUtils
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ValidationUtils
 */
class ValidationUtilsTest {
    
    @Test
    fun `isValidAddress rejects addresses shorter than 10 characters`() {
        // Test addresses with less than 10 characters
        assertFalse(ValidationUtils.isValidAddress(""))
        assertFalse(ValidationUtils.isValidAddress("Short"))
        assertFalse(ValidationUtils.isValidAddress("123 Main"))
        assertFalse(ValidationUtils.isValidAddress("Test St"))
        assertFalse(ValidationUtils.isValidAddress("         ")) // Only spaces
    }
    
    @Test
    fun `isValidAddress rejects addresses with only special characters`() {
        // Test addresses with only special characters (even if >= 10 chars)
        assertFalse(ValidationUtils.isValidAddress("!@#$%^&*()"))
        assertFalse(ValidationUtils.isValidAddress("----------"))
        assertFalse(ValidationUtils.isValidAddress(".........."))
        assertFalse(ValidationUtils.isValidAddress("!@#$%^&*()_+"))
    }
    
    @Test
    fun `isValidAddress accepts valid addresses with at least 10 characters`() {
        // Test valid addresses with at least 10 characters and alphanumeric content
        assertTrue(ValidationUtils.isValidAddress("123 Main Street"))
        assertTrue(ValidationUtils.isValidAddress("Apartment 5B"))
        assertTrue(ValidationUtils.isValidAddress("1234567890"))
        assertTrue(ValidationUtils.isValidAddress("New York, NY"))
        assertTrue(ValidationUtils.isValidAddress("Building A, Floor 3"))
        assertTrue(ValidationUtils.isValidAddress("123 Main St, Apt 4B"))
        assertTrue(ValidationUtils.isValidAddress("Valid Address 123"))
    }
    
    @Test
    fun `isValidAddress handles addresses with mixed content`() {
        // Test addresses with alphanumeric and special characters
        assertTrue(ValidationUtils.isValidAddress("123 Main St."))
        assertTrue(ValidationUtils.isValidAddress("Apt #5, Building B"))
        assertTrue(ValidationUtils.isValidAddress("123-456 Street"))
        assertTrue(ValidationUtils.isValidAddress("P.O. Box 1234"))
    }
    
    @Test
    fun `isValidAddress trims whitespace correctly`() {
        // Test that leading/trailing whitespace is handled
        assertTrue(ValidationUtils.isValidAddress("  123 Main Street  "))
        assertFalse(ValidationUtils.isValidAddress("  Short  ")) // Still too short after trim
    }
}
