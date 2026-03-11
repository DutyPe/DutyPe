package com.example.dutype.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
/**
 * Referral validation result
 */
data class ReferralValidationResult(
    val isValid: Boolean,
    val message: String,
    val referrerName: String? = null
)

/**
 * Validate referral code format
 * Accepts both uppercase and lowercase (case-insensitive)
 */
fun isValidReferralCode(code: String): Boolean {
    if (code.isBlank()) return false
    // Referral codes are 7-10 alphanumeric characters (case-insensitive)
    // Format: nameXXXX (e.g., vamsi9843, sai8273)
    return code.matches(Regex("^[a-zA-Z0-9]{7,10}$", RegexOption.IGNORE_CASE))
}

/**
 * Generate a personalized referral code from user's name
 * Format: nameXXXX (7-10 characters, lowercase)
 * Matches Cloud Function format: first name (3-6 chars) + 4 random digits
 * Examples: vamsi9843, sai8273, priya3921
 */
fun generateReferralCode(userName: String): String {
    val digits = "0123456789"
    val letters = "abcdefghjklmnpqrstuvwxyz"

    // Extract first name, lowercase, alphabetic only, max 6 chars
    var namePrefix = userName.trim().split("\\s+".toRegex())
        .firstOrNull()?.lowercase()?.replace(Regex("[^a-z]"), "")
        ?.take(6) ?: ""

    // Fallback: 4 random letters if no valid name
    if (namePrefix.isEmpty()) {
        namePrefix = (1..4).map { letters.random() }.joinToString("")
    }

    // Append 4 random digits
    val digitSuffix = (1..4).map { digits.random() }.joinToString("")
    return "$namePrefix$digitSuffix"
}

/**
 * Referral Code Input Component
 */
@Composable
fun ReferralCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    validationResult: ReferralValidationResult?,
    isValidating: Boolean = false,
    modifier: Modifier = Modifier,
    label: String = "Referral Code (Optional)",
    placeholder: String = "Enter code"
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                // Convert to lowercase and limit to 10 characters (matches backend format)
                onValueChange(newValue.lowercase().take(10))
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            trailingIcon = {
                when {
                    isValidating -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    validationResult != null -> {
                        Icon(
                            imageVector = if (validationResult.isValid) 
                                Icons.Default.CheckCircle 
                            else 
                                Icons.Default.Error,
                            contentDescription = null,
                            tint = if (validationResult.isValid) 
                                Color(0xFF10B981) 
                            else 
                                Color(0xFFEF4444)
                        )
                    }
                }
            },
            isError = validationResult != null && !validationResult.isValid,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Validation message
        if (validationResult != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = validationResult.message,
                style = MaterialTheme.typography.bodySmall,
                color = if (validationResult.isValid) 
                    Color(0xFF10B981) 
                else 
                    Color(0xFFEF4444),
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        
        // Helper text
        if (value.isEmpty() && validationResult == null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Get â‚¹50 bonus when you use a referral code",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}