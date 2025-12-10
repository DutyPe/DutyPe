package com.example.dutype

import com.example.dutype.utils.ValidationUtils

/**
 * Manual verification of address validation logic
 * This demonstrates the validation behavior for documentation purposes
 */
fun main() {
    println("=== Address Validation Tests ===\n")
    
    // Test cases that should FAIL (return false)
    println("--- Should REJECT (too short) ---")
    testAddress("", false)
    testAddress("Short", false)
    testAddress("123 Main", false)
    testAddress("Test St", false)
    
    println("\n--- Should REJECT (only special characters) ---")
    testAddress("!@#$%^&*()", false)
    testAddress("----------", false)
    testAddress("..........", false)
    
    // Test cases that should PASS (return true)
    println("\n--- Should ACCEPT (valid addresses) ---")
    testAddress("123 Main Street", true)
    testAddress("Apartment 5B", true)
    testAddress("1234567890", true)
    testAddress("New York, NY", true)
    testAddress("Building A, Floor 3", true)
    testAddress("123 Main St, Apt 4B", true)
    testAddress("Valid Address 123", true)
    testAddress("123 Main St.", true)
    testAddress("Apt #5, Building B", true)
    
    println("\n--- Edge cases ---")
    testAddress("  123 Main Street  ", true) // With whitespace
    testAddress("  Short  ", false) // Too short after trim
}

fun testAddress(address: String, expected: Boolean) {
    val result = ValidationUtils.isValidAddress(address)
    val status = if (result == expected) "✓" else "✗"
    val displayAddress = if (address.isEmpty()) "(empty)" else "\"$address\""
    println("$status $displayAddress -> $result (expected: $expected)")
}
