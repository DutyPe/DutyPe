package com.example.dutype.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.models.isValidReferralCode

/**
 * Referral Code Input Component
 * 
 * Used during registration to allow users to enter a referral code.
 * Shows validation status and rewards info.
 */
@Composable
fun ReferralCodeInput(
    referralCode: String,
    onReferralCodeChange: (String) -> Unit,
    isValidating: Boolean = false,
    validationResult: ReferralValidationResult? = null,
    onValidate: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(referralCode.isNotBlank()) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Expandable header
        Card(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFEF3C7).copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFF59E0B),
                                    Color(0xFFFBBF24)
                                )
                            ),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Have a referral code?",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF92400E)
                        )
                    )
                    Text(
                        text = "Enter it to get rewards!",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFB45309)
                        )
                    )
                }
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFF92400E)
                )
            }
        }
        
        // Expandable content
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                // Input field
                OutlinedTextField(
                    value = referralCode,
                    onValueChange = { newValue ->
                        // Convert to uppercase and limit length
                        val filtered = newValue.uppercase().filter { it.isLetterOrDigit() }.take(12)
                        onReferralCodeChange(filtered)
                    },
                    label = { Text("Referral Code") },
                    placeholder = { Text("e.g., WRK123ABC") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Redeem,
                            contentDescription = null,
                            tint = Color(0xFF6B7280)
                        )
                    },
                    trailingIcon = {
                        when {
                            isValidating -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF3B82F6)
                                )
                            }
                            validationResult?.isValid == true -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Valid",
                                    tint = Color(0xFF10B981)
                                )
                            }
                            validationResult?.isValid == false -> {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Invalid",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                            referralCode.length >= 6 && onValidate != null -> {
                                TextButton(
                                    onClick = { onValidate(referralCode) },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "Verify",
                                        color = Color(0xFF3B82F6),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = when {
                            validationResult?.isValid == true -> Color(0xFF10B981)
                            validationResult?.isValid == false -> Color(0xFFEF4444)
                            else -> Color(0xFF3B82F6)
                        },
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    ),
                    isError = validationResult?.isValid == false
                )
                
                // Validation message
                if (validationResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (validationResult.isValid) 
                                Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (validationResult.isValid) 
                                    Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (validationResult.isValid) 
                                    Color(0xFF059669) else Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = validationResult.message,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (validationResult.isValid) 
                                        Color(0xFF065F46) else Color(0xFF991B1B)
                                )
                            )
                        }
                    }
                }
                
                // Rewards info
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF0FDF4)
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "🎁 Referral Benefits",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val benefits = listOf(
                            "Both you and your referrer earn rewards",
                            "Get priority support as a referred user",
                            "Unlock special features faster"
                        )
                        
                        benefits.forEach { benefit ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF16A34A)
                                    ),
                                    modifier = Modifier.padding(end = 6.dp, top = 2.dp)
                                )
                                Text(
                                    text = benefit,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF166534)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Validation result for referral code
 */
data class ReferralValidationResult(
    val isValid: Boolean,
    val message: String,
    val referrerUserId: String? = null,
    val referrerRole: String? = null,
    val referrerName: String? = null
) {
    /**
     * Get display message with referrer info
     */
    fun getDisplayMessage(): String {
        if (!isValid) return message
        
        val roleDisplay = when (referrerRole?.uppercase()) {
            "EMPLOYER" -> "an Employer"
            "WORKER" -> "a Worker"
            else -> "a user"
        }
        
        return if (!referrerName.isNullOrBlank() && referrerName != roleDisplay) {
            "Valid code from $roleDisplay! You'll both earn ₹10."
        } else {
            "Valid code from $roleDisplay! You'll both earn ₹10."
        }
    }
}

/**
 * Compact referral code input for smaller spaces
 */
@Composable
fun CompactReferralCodeInput(
    referralCode: String,
    onReferralCodeChange: (String) -> Unit,
    isValidating: Boolean = false,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = referralCode,
            onValueChange = { newValue ->
                val filtered = newValue.uppercase().filter { it.isLetterOrDigit() }.take(12)
                onReferralCodeChange(filtered)
            },
            label = { Text("Referral Code (Optional)") },
            placeholder = { Text("Enter code if you have one") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B)
                )
            },
            trailingIcon = {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done
            ),
            isError = error != null,
            supportingText = if (error != null) {
                { Text(text = error, color = Color(0xFFEF4444)) }
            } else null
        )
    }
}
