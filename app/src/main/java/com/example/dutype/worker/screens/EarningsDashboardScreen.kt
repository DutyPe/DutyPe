package com.example.dutype.worker.screens

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import androidx.compose.material.icons.filled.CurrencyRupee
import com.dutype.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import com.example.dutype.models.ReferralStatus
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.viewmodels.EarningsViewModel
import com.example.dutype.viewmodels.ReferralViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.example.dutype.components.EmptyListState
import com.example.dutype.components.EmptyStateAction
import androidx.compose.ui.res.stringResource

/**
 * Earnings Dashboard Screen ("17 - Worker Earnings Dashboard" mockup)
 *
 * Flat dark hero balance card (wallet balance from referral stats), job income /
 * referral bonus breakdown, and a merged recent-transactions list (job payments
 * from EarningsViewModel + completed referral bonuses from ReferralViewModel).
 */

// Design tokens (mockup 17)
private val ScreenBg = Color(0xFFF8FAFC)
private val InkBlack = Color(0xFF0F0F0F)
private val Emerald = Color(0xFF10B981)
private val EmeraldTint = Color(0xFFF0FDF4)
private val SlateBorder = Color(0xFFE2E8F0)
private val SlateMuted = Color(0xFF94A3B8)
private val SlateLabel = Color(0xFF64748B)
private val AmberFg = Color(0xFFD97706)
private val AmberBg = Color(0xFFFFFBEB)
private val RedFg = Color(0xFFEF4444)
private val RedBg = Color(0xFFFEF2F2)

private fun t(
    size: Int,
    weight: FontWeight,
    color: Color,
    letterSpacing: Float? = null
): TextStyle = TextStyle(
    fontFamily = MeeshoFontFamily,
    fontSize = size.sp,
    fontWeight = weight,
    color = color,
    letterSpacing = if (letterSpacing != null) letterSpacing.sp else androidx.compose.ui.unit.TextUnit.Unspecified
)

/** Unified row shown in the transactions list. */
private data class EarningsRow(
    val key: String,
    val isReferral: Boolean,
    val title: String,
    val date: Long,
    val amount: Double,
    val status: PaymentStatus
)

@Composable
fun EarningsDashboardScreen(
    navController: NavController,
    viewModel: EarningsViewModel = hiltViewModel(),
    referralViewModel: ReferralViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val referralState by referralViewModel.uiState.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadEarnings()
        referralViewModel.loadReferralData()
    }

    val wallet = referralState.wallet
    val walletBalance = (wallet?.balancePaise ?: 0L) / 100.0
    val referralBonus = (wallet?.lifetimeEarnedPaise ?: 0L) / 100.0
    val isBalanceLoading = referralState.isLoading && wallet == null

    val ledger = referralState.ledger
    val jobTransactions = uiState.transactions
    val rows = remember(jobTransactions, ledger) {
        val jobRows = jobTransactions.map { tx ->
            val who = tx.companyName.ifBlank { tx.jobTitle }
            EarningsRow(
                key = "job_${tx.id}",
                isReferral = false,
                title = context.getString(R.string.earnings_job_payment_title, who),
                date = tx.date,
                amount = tx.amount,
                status = tx.status
            )
        }
        val referralRows = ledger
            .filter { it.isCredit && it.type != com.example.dutype.firestore.FirestoreSchema.Values.LedgerType.REFUND }
            .map { entry ->
                EarningsRow(
                    key = "ref_${entry.id}",
                    isReferral = true,
                    title = entry.label,
                    date = entry.createdAt,
                    amount = entry.amountPaise / 100.0,
                    status = PaymentStatus.PAID
                )
            }
        (jobRows + referralRows).sortedByDescending { it.date }.take(10)
    }

    val goToWallet = { navController.navigate(Routes.WORKER_REFER_EARN) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg.bg())
    ) {
        Text(
            text = stringResource(R.string.my_earnings),
            style = t(22, FontWeight.Bold, InkBlack.fg()),
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp)
        ) {
            item {
                BalanceHeroCard(
                    balance = walletBalance,
                    isLoading = isBalanceLoading,
                    onWithdraw = { goToWallet() },
                    onHistory = { goToWallet() }
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                BreakdownRow(
                    jobIncome = uiState.totalEarnings,
                    referralBonus = referralBonus
                )
            }

            item { Spacer(modifier = Modifier.height(22.dp)) }

            item {
                Text(
                    text = stringResource(R.string.recent_transactions).uppercase(),
                    style = t(13, FontWeight.SemiBold, SlateLabel.fg(), 0.8f)
                )
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            if (rows.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyTransactionsCard()
                }
            } else {
                items(rows, key = { it.key }) { row ->
                    TransactionCard(row = row)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun BalanceHeroCard(
    balance: Double,
    isLoading: Boolean,
    onWithdraw: () -> Unit,
    onHistory: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(InkBlack.bg())
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(R.string.earnings_available_balance),
            style = t(12, FontWeight.Normal, SlateMuted.fg(), 0.8f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (isLoading) {
            Box(modifier = Modifier.height(44.dp), contentAlignment = Alignment.CenterStart) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
        } else {
            Text(
                text = formatCurrencyExact(balance),
                style = t(36, FontWeight.Bold, Color.White)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Emerald.bg())
                    .clickable(onClick = onWithdraw),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.earnings_withdraw_upi),
                    style = t(13, FontWeight.Bold, Color.White),
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, Color.White.bd(), RoundedCornerShape(22.dp))
                    .clickable(onClick = onHistory),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.earnings_history),
                    style = t(13, FontWeight.SemiBold, Color.White),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    jobIncome: Double,
    referralBonus: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BreakdownCard(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.earnings_job_income),
            value = formatCurrency(jobIncome),
            icon = Icons.Outlined.WorkOutline
        )
        BreakdownCard(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.earnings_referral_bonus),
            value = formatCurrency(referralBonus),
            icon = Icons.Outlined.CardGiftcard
        )
    }
}

@Composable
private fun BreakdownCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White.bg())
            .border(1.dp, SlateBorder.bd(), shape)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.padding(end = 24.dp)) {
            Text(
                text = label,
                style = t(12, FontWeight.Normal, SlateMuted.fg()),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = t(22, FontWeight.Bold, InkBlack.fg()),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Emerald.fg(),
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun TransactionCard(
    row: EarningsRow
) {
    val shape = RoundedCornerShape(16.dp)
    val accent = when (row.status) {
        PaymentStatus.PAID -> Emerald
        PaymentStatus.PENDING -> AmberFg
        PaymentStatus.FAILED -> RedFg
    }
    val pillBg = when (row.status) {
        PaymentStatus.PAID -> EmeraldTint
        PaymentStatus.PENDING -> AmberBg
        PaymentStatus.FAILED -> RedBg
    }
    val statusText = when (row.status) {
        PaymentStatus.PAID -> stringResource(R.string.status_completed)
        PaymentStatus.PENDING -> stringResource(R.string.status_pending)
        PaymentStatus.FAILED -> stringResource(R.string.status_failed)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.bg())
            .border(1.dp, SlateBorder.bd(), shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(EmeraldTint.bg()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (row.isReferral) Icons.Outlined.CardGiftcard else Icons.Outlined.WorkOutline,
                contentDescription = null,
                tint = Emerald.fg(),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = t(14, FontWeight.SemiBold, InkBlack.fg()),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatDate(row.date),
                style = t(12, FontWeight.Normal, SlateMuted.fg())
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "+" + formatCurrency(row.amount),
                style = t(16, FontWeight.Bold, accent),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusText,
                style = t(10, FontWeight.SemiBold, accent),
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(pillBg)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun EmptyTransactionsCard() {
    EmptyListState(
        icon = Icons.Filled.CurrencyRupee,
        tone = com.example.dutype.components.EmptyTone.GREEN,
        title = stringResource(R.string.no_transactions_yet),
        subtitle = stringResource(R.string.complete_jobs_see_earnings)
    )
}

// Helper functions
private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(amount).replace(".00", "")
}

/** Always two decimals, e.g. "₹1,450.00". */
private fun formatCurrencyExact(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.minimumFractionDigits = 2
    format.maximumFractionDigits = 2
    return format.format(amount)
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
