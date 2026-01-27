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
    payoutStatus: PayoutRequestStatus?,
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
        WithdrawalStatus.ON_HOLD -> Triple(
            Color(0xFFF59E0B),
            Icons.Default.Warning,
            "On Hold"
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
                .format(Date(payoutStatus.requestedAt))
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
            
            // Estimated Completion (for pending/processing)
            if ((payoutStatus.status == WithdrawalStatus.PENDING || payoutStatus.status == WithdrawalStatus.PROCESSING) 
                && payoutStatus.estimatedCompletionDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val estimatedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(Date(payoutStatus.estimatedCompletionDate))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Estimated Completion",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                    Text(
                        estimatedDate,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4B5563)
                        )
                    )
                }
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
            
            // Rejection Reason (for failed/cancelled)
            if ((payoutStatus.status == WithdrawalStatus.FAILED || payoutStatus.status == WithdrawalStatus.CANCELLED) 
                && payoutStatus.rejectionReason != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Reason: ${payoutStatus.rejectionReason}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFEF4444)
                    )
                )
            }
        }
    }
}

/**
 * Transaction History Card
 */
@SuppressLint("DefaultLocale")
@Composable
fun TransactionHistoryCard(
    transactions: List<ReferralTransaction>,
    modifier: Modifier = Modifier
) {
    if (transactions.isEmpty()) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Transaction History",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            transactions.take(5).forEach { transaction ->
                TransactionItem(transaction)
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            if (transactions.size > 5) {
                Text(
                    text = "Showing 5 of ${transactions.size} transactions",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF9CA3AF)
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun TransactionItem(transaction: ReferralTransaction) {
    val (icon, color, title) = when (transaction.type) {
        TransactionType.REFERRAL_EARNED -> Triple(
            Icons.Default.PersonAdd,
            Color(0xFF10B981),
            "Referral Earned"
        )
        TransactionType.MILESTONE_BONUS -> Triple(
            Icons.Default.EmojiEvents,
            Color(0xFFF59E0B),
            "Milestone Bonus"
        )
        TransactionType.SIGNUP_BONUS -> Triple(
            Icons.Default.CardGiftcard,
            Color(0xFF6366F1),
            "Signup Bonus"
        )
        TransactionType.WITHDRAWAL -> Triple(
            Icons.Default.AccountBalance,
            Color(0xFF6366F1),
            "Withdrawal"
        )
        TransactionType.WITHDRAWAL_REVERSED -> Triple(
            Icons.Default.Undo,
            Color(0xFFEF4444),
            "Withdrawal Reversed"
        )
        TransactionType.ADJUSTMENT -> Triple(
            Icons.Default.SwapHoriz,
            Color(0xFF6B7280),
            "Adjustment"
        )
    }
    
    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        .format(Date(transaction.timestamp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                )
                Text(
                    dateStr,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF9CA3AF)
                    )
                )
            }
        }
        Text(
            "${if (transaction.amount >= 0) "+" else ""}₹${String.format("%.0f", transaction.amount)}",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (transaction.amount >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
            )
        )
    }
}

