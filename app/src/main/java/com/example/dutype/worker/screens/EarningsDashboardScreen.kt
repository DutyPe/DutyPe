package com.example.dutype.worker.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.viewmodels.EarningsViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Earnings Dashboard Screen
 * 
 * P2 Feature: Worker Financial Clarity
 * Track monthly income, job history, payment status
 * Updated with Meesho-style colors and typography
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsDashboardScreen(
    navController: NavController,
    viewModel: EarningsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedPeriod by remember { mutableStateOf(EarningsPeriod.THIS_MONTH) }
    
    LaunchedEffect(Unit) {
        viewModel.loadEarnings()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // CommonHeader with back button
        CommonHeader(
            title = "Earnings",
            navController = navController,
            showBackButton = true,
            backgroundColor = WorkerColors.CardBackground,
            titleColor = WorkerColors.TextPrimary
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Total Earnings Card
            item {
                TotalEarningsCard(
                    totalEarnings = uiState.totalEarnings,
                    pendingAmount = uiState.pendingAmount,
                    completedJobs = uiState.completedJobs,
                    isLoading = uiState.isLoading
                )
            }
            
            // Period Filter
            item {
                PeriodFilterRow(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { 
                        selectedPeriod = it
                        viewModel.filterByPeriod(it)
                    }
                )
            }
            
            // Stats Grid
            item {
                StatsGrid(
                    completedJobs = uiState.periodCompletedJobs,
                    avgEarningPerJob = uiState.avgEarningPerJob,
                    onTimePayments = uiState.onTimePaymentPercentage,
                    totalHoursWorked = uiState.totalHoursWorked
                )
            }
            
            // Earnings Chart
            item {
                EarningsChartCard(
                    weeklyEarnings = uiState.weeklyEarnings
                )
            }
            
            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = AppTypography.sectionHeader.copy(
                            color = WorkerColors.TextPrimary
                        )
                    )
                    TextButton(onClick = { /* View all */ }) {
                        Text(
                            "View All", 
                            style = AppTypography.buttonSmall.copy(
                                color = WorkerColors.Info
                            )
                        )
                    }
                }
            }
            
            // Transaction List
            if (uiState.transactions.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyTransactionsCard()
                }
            } else {
                items(uiState.transactions.take(10)) { transaction ->
                    TransactionCard(transaction = transaction)
                }
            }
            
            // Bottom spacing
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun TotalEarningsCard(
    totalEarnings: Double,
    pendingAmount: Double,
    completedJobs: Int,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            WorkerColors.TextPrimary,
                            Color(0xFF374151)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Total Earnings",
                            style = AppTypography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = formatCurrency(totalEarnings),
                                style = AppTypography.displayTitle.copy(
                                    color = Color.White
                                )
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Pending Amount
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pending",
                            style = AppTypography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            text = formatCurrency(pendingAmount),
                            style = AppTypography.cardTitle.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = WorkerColors.Warning
                            )
                        )
                    }
                    
                    // Completed Jobs
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Jobs Done",
                            style = AppTypography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            text = "$completedJobs",
                            style = AppTypography.cardTitle.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = WorkerColors.Success
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodFilterRow(
    selectedPeriod: EarningsPeriod,
    onPeriodSelected: (EarningsPeriod) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(EarningsPeriod.entries) { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = { 
                    Text(
                        period.displayName,
                        style = AppTypography.labelMedium
                    ) 
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WorkerColors.TextPrimary,
                    selectedLabelColor = Color.White,
                    containerColor = WorkerColors.CardBackground,
                    labelColor = WorkerColors.TextSecondary
                )
            )
        }
    }
}

@Composable
private fun StatsGrid(
    completedJobs: Int,
    avgEarningPerJob: Double,
    onTimePayments: Int,
    totalHoursWorked: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.CheckCircle,
            iconColor = WorkerColors.Success,
            label = "Jobs Done",
            value = "$completedJobs"
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.TrendingUp,
            iconColor = WorkerColors.Info,
            label = "Avg/Job",
            value = formatCurrency(avgEarningPerJob)
        )
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Schedule,
            iconColor = WorkerColors.Warning,
            label = "On-Time Pay",
            value = "$onTimePayments%"
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.AccessTime,
            iconColor = WorkerColors.Primary,
            label = "Hours",
            value = "${totalHoursWorked}h"
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = value,
                style = AppTypography.pageTitle.copy(
                    color = WorkerColors.TextPrimary
                )
            )
            
            Text(
                text = label,
                style = AppTypography.bodySmall.copy(
                    color = WorkerColors.TextSecondary
                )
            )
        }
    }
}

@Composable
private fun EarningsChartCard(
    weeklyEarnings: List<Double>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Weekly Earnings",
                style = AppTypography.sectionHeader.copy(
                    color = WorkerColors.TextPrimary
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Simple bar chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                val maxEarning = weeklyEarnings.maxOrNull() ?: 1.0
                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                
                weeklyEarnings.forEachIndexed { index, earning ->
                    val heightFraction = if (maxEarning > 0) (earning / maxEarning).toFloat() else 0f
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height((100 * heightFraction).dp.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (index == Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2)
                                        WorkerColors.Info
                                    else
                                        WorkerColors.Border
                                )
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = days.getOrElse(index) { "" },
                            style = AppTypography.labelSmall.copy(
                                color = WorkerColors.TextSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: EarningsTransaction
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when (transaction.status) {
                            PaymentStatus.PAID -> WorkerColors.SuccessLight
                            PaymentStatus.PENDING -> WorkerColors.WarningLight
                            PaymentStatus.FAILED -> WorkerColors.ErrorLight
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (transaction.status) {
                        PaymentStatus.PAID -> Icons.Default.CheckCircle
                        PaymentStatus.PENDING -> Icons.Default.Schedule
                        PaymentStatus.FAILED -> Icons.Default.Error
                    },
                    contentDescription = null,
                    tint = when (transaction.status) {
                        PaymentStatus.PAID -> WorkerColors.Success
                        PaymentStatus.PENDING -> WorkerColors.Warning
                        PaymentStatus.FAILED -> WorkerColors.Error
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.jobTitle,
                    style = AppTypography.cardTitle.copy(
                        color = WorkerColors.TextPrimary
                    )
                )
                Text(
                    text = transaction.companyName,
                    style = AppTypography.bodySmall.copy(
                        color = WorkerColors.TextSecondary
                    )
                )
                Text(
                    text = formatDate(transaction.date),
                    style = AppTypography.caption.copy(
                        color = WorkerColors.TextTertiary
                    )
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(transaction.amount),
                    style = AppTypography.cardTitle.copy(
                        fontWeight = FontWeight.Bold,
                        color = when (transaction.status) {
                            PaymentStatus.PAID -> WorkerColors.Success
                            PaymentStatus.PENDING -> WorkerColors.Warning
                            PaymentStatus.FAILED -> WorkerColors.Error
                        }
                    )
                )
                Text(
                    text = transaction.status.displayName,
                    style = AppTypography.caption.copy(
                        color = WorkerColors.TextSecondary
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyTransactionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Receipt,
                contentDescription = null,
                tint = WorkerColors.IconSecondary,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "No transactions yet",
                style = AppTypography.emptyStateTitle.copy(
                    color = WorkerColors.TextSecondary
                )
            )
            
            Text(
                text = "Complete jobs to see your earnings here",
                style = AppTypography.emptyStateSubtitle.copy(
                    color = WorkerColors.TextTertiary
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper functions
private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(amount).replace(".00", "")
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// Data classes
enum class EarningsPeriod(val displayName: String) {
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_3_MONTHS("3 Months"),
    ALL_TIME("All Time")
}

enum class PaymentStatus(val displayName: String) {
    PAID("Paid"),
    PENDING("Pending"),
    FAILED("Failed")
}

data class EarningsTransaction(
    val id: String = "",
    val jobId: String = "",
    val jobTitle: String = "",
    val companyName: String = "",
    val amount: Double = 0.0,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val date: Long = System.currentTimeMillis()
)
