package com.example.dutype.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dutype.models.generateReferralCode as generateCanonicalReferralCode
import com.example.dutype.models.isValidNormalizedReferralCode
import com.example.dutype.models.normalizeReferralCode
import com.example.dutype.ui.theme.WorkerColors

/**
 * Referral validation result
 */
data class ReferralValidationResult(
    val isValid: Boolean,
    val message: String,
    val referrerName: String? = null
)

/**
 * Validate referral code format.
 * Accepts canonical uppercase codes and legacy alphanumeric codes.
 */
fun isValidReferralCode(code: String): Boolean {
    return code.isNotBlank() && isValidNormalizedReferralCode(code)
}

/**
 * Generate the canonical referral code format used by Cloud Functions.
 */
fun generateReferralCode(userName: String = ""): String = generateCanonicalReferralCode()

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
                onValueChange(normalizeReferralCode(newValue))
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            trailingIcon = {
                when {
                    isValidating -> {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp
                        )
                    }

                    validationResult != null -> {
                        Icon(
                            imageVector = if (validationResult.isValid) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.Error
                            },
                            contentDescription = null,
                            tint = if (validationResult.isValid) {
                                WorkerColors.Success
                            } else {
                                WorkerColors.Error
                            }
                        )
                    }
                }
            },
            isError = validationResult != null && !validationResult.isValid,
            modifier = Modifier.fillMaxWidth()
        )

        if (validationResult != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = validationResult.message,
                style = MaterialTheme.typography.bodySmall,
                color = if (validationResult.isValid) {
                    WorkerColors.Success
                } else {
                    WorkerColors.Error
                },
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        if (value.isEmpty() && validationResult == null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Use a referral code to unlock your Rs.20 signup bonus",
                style = MaterialTheme.typography.bodySmall,
                color = WorkerColors.TextSecondary,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
