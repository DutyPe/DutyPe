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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.dutype.models.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pending Referrals Card - Shows referrals in progress
 */
@Composable
fun PendingReferralsCard(
    pendingReferrals: List<Referral>,
    pendingBalance: Double,
    modifier: Modifier = Modifier
) {
    if (pendingReferrals.isEmpty()) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
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
                        Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Pending Referrals",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        )
                        Text(
                            "${pendingReferrals.size} friends signed up",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFB45309)
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
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFFED7AA))
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "Waiting for profile completion",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFB45309)
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Show first 3 pending referrals
            pendingReferrals.take(3).forEach { referral ->
                val expiresAt = referral.getExpiresAt()
                val daysLeft = ((expiresAt - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "New User",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF92400E)
                        )
                    )
                    Text(
                        "$daysLeft days left",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFB45309)
                        )
                    )
                }
            }
            
            if (pendingReferrals.size > 3) {
                Text(
                    "+${pendingReferrals.size - 3} more",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFB45309),
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * "Referred By" Welcome Banner for new users
 */
@Composable
fun ReferredByBanner(
    referrerName: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
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
                    .size(48.dp)
                    .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "🎁 Welcome!",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46)
                    )
                )
                Text(
                    "You were referred by $referrerName",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF047857)
                    )
                )
                Text(
                    "Complete your profile to help them earn ₹25!",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF059669)
                    )
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * In-App Withdrawal Request Dialog (replaces email instructions)
 */
@Composable
fun WithdrawalRequestDialog(
    availableBalance: Double,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, upiId: String, phone: String) -> Unit
) {
    var amount by remember { mutableStateOf(availableBalance.toString()) }
    var upiId by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Request Withdrawal",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = Color(0xFF6B7280))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Available Balance
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Available Balance",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                        Text(
                            "₹${String.format("%.0f", availableBalance)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Amount Input
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1F2937),
                        focusedLabelColor = Color(0xFF1F2937)
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // UPI ID Input
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("UPI ID") },
                    placeholder = { Text("yourname@upi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1F2937),
                        focusedLabelColor = Color(0xFF1F2937)
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Phone Input
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("Registered phone number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1F2937),
                        focusedLabelColor = Color(0xFF1F2937)
                    )
                )
                
                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFEF4444)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Processing Time: 3-5 business days",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF1E40AF),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                "You'll receive status updates in the app",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF3B82F6)
                                )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Submit Button
                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull()
                        when {
                            amountValue == null || amountValue <= 0 -> {
                                showError = true
                                errorMessage = "Please enter a valid amount"
                            }
                            amountValue > availableBalance -> {
                                showError = true
                                errorMessage = "Amount exceeds available balance"
                            }
                            amountValue < 50 -> {
                                showError = true
                                errorMessage = "Minimum withdrawal amount is ₹50"
                            }
                            upiId.isBlank() -> {
                                showError = true
                                errorMessage = "Please enter your UPI ID"
                            }
                            phone.isBlank() -> {
                                showError = true
                                errorMessage = "Please enter your phone number"
                            }
                            else -> {
                                showError = false
                                onSubmit(amountValue, upiId, phone)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937))
                ) {
                    Text(
                        "Submit Request",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}
