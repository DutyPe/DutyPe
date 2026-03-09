package com.example.dutype.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dutype.models.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced Wallet Card showing pending, locked, and available balances
 */
@SuppressLint("DefaultLocale")
@Composable
fun EnhancedWalletCard(
    availableBalance: Double,
    pendingBalance: Double,
    lockedBalance: Double,
    totalEarnings: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Wallet Balance",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Available Balance (Primary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF10B981).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Available",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                        Text(
                            "Can withdraw now",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF9CA3AF)
                            )
                        )
                    }
                }
                Text(
                    "₹${String.format("%.0f", availableBalance)}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFE5E7EB))
            Spacer(modifier = Modifier.height(12.dp))
            
            // Pending Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Pending",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                        Text(
                            "Referrals in progress",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF9CA3AF)
                            )
                        )
                    }
                }
                Text(
                    "₹${String.format("%.0f", pendingBalance)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                )
            }
            
            // Locked Balance (if any)
            if (lockedBalance > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF6366F1).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Locked",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF6B7280)
                                )
                            )
                            Text(
                                "In withdrawal processing",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF9CA3AF)
                                )
                            )
                        }
                    }
                    Text(
                        "₹${String.format("%.0f", lockedBalance)}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFE5E7EB))
            Spacer(modifier = Modifier.height(12.dp))
            
            // Total Earnings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total Earned",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280)
                    )
                )
                Text(
                    "₹${String.format("%.0f", totalEarnings)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
            }
        }
    }
}

/**
 * Payout Status Card showing current withdrawal status
 */
@SuppressLint("DefaultLocale")
@Composable
fun PayoutStatusCard(
    payoutStatus: WithdrawalRequest?,
    modifier: Modifier = Modifier
) {
    if (payoutStatus == null) return
    
    val (statusColor, statusIcon, statusText) = when (payoutStatus.status) {
        WithdrawalStatus.PENDING -> Triple(
            Color(0xFFF59E0B),
            Icons.Default.HourglassEmpty,
            "Pending Review"
        )
        WithdrawalStatus.PROCESSING -> Triple(
            Color(0xFF6366F1),
            Icons.Default.Sync,
            "Processing Payment"
        )
        WithdrawalStatus.COMPLETED -> Triple(
            Color(0xFF10B981),
            Icons.Default.CheckCircle,
            "Payment Completed"
        )
        WithdrawalStatus.FAILED -> Triple(
            Color(0xFFEF4444),
            Icons.Default.Error,
            "Payment Failed"
        )
        WithdrawalStatus.CANCELLED -> Triple(
            Color(0xFF6B7280),
            Icons.Default.Cancel,
            "Request Cancelled"
        )
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Payout Status",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                        Text(
                            statusText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        )
                    }
                }
                Text(
                    "₹${String.format("%.0f", payoutStatus.amount)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = statusColor.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))
            
            // Request Date
            val requestDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(Date(payoutStatus.createdAt))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Requested",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280)
                    )
                )
                Text(
                    requestDate,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF4B5563)
                    )
                )
            }
            
            // Transaction ID (for completed)
            if (payoutStatus.status == WithdrawalStatus.COMPLETED && payoutStatus.transactionId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Transaction ID",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                    Text(
                        payoutStatus.transactionId,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4B5563)
                        )
                    )
                }
            }
        }
    }
}

// REMOVED: Transaction history not in optimized schema
// Can be added later if needed


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
 */
fun isValidReferralCode(code: String): Boolean {
    if (code.isBlank()) return false
    // Referral codes are 6-8 alphanumeric characters
    return code.matches(Regex("^[A-Z0-9]{6,8}$"))
}

/**
 * Generate a referral code from user ID
 */
fun generateReferralCode(userId: String): String {
    // Take first 6 characters of userId and convert to uppercase
    return userId.take(6).uppercase()
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
                // Convert to uppercase and limit to 8 characters
                onValueChange(newValue.uppercase().take(8))
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
                text = "Get ₹50 bonus when you use a referral code",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
