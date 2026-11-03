package com.example.dutype.homeservices

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.window.Dialog
import com.example.dutype.payment.RazorpayPaymentActivity
import com.example.dutype.viewmodels.SubscriptionViewModel
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.models.LedgerEntry
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.MeeshoFontFamily
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceWalletScreen(
    navController: NavController,
    rootNavController: NavController? = null,
    viewModel: HomeServicesViewModel = hiltViewModel(),
    subscriptionViewModel: SubscriptionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val wallet by viewModel.wallet.collectAsState()
    val balanceRupees = (wallet?.balancePaise ?: 0L) / 100

    var isProcessingPayment by remember { mutableStateOf(false) }
    var paymentProcessingMessage by remember { mutableStateOf("Connecting to Razorpay...") }
    var successfulRechargeAmount by remember { mutableStateOf<Int?>(null) }

    val razorpayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val paymentId = data?.getStringExtra(RazorpayPaymentActivity.EXTRA_PAYMENT_ID).orEmpty()
            val orderId = data?.getStringExtra(RazorpayPaymentActivity.EXTRA_ORDER_ID_RES).orEmpty()
            val signature = data?.getStringExtra(RazorpayPaymentActivity.EXTRA_SIGNATURE).orEmpty()

            if (paymentId.isNotBlank() && orderId.isNotBlank() && signature.isNotBlank()) {
                isProcessingPayment = true
                paymentProcessingMessage = "Verifying payment with bank and crediting wallet..."

                subscriptionViewModel.verifyRazorpayPayment(
                    orderId = orderId,
                    paymentId = paymentId,
                    signature = signature,
                    type = "WALLET",
                    onSuccess = {
                        isProcessingPayment = false
                        successfulRechargeAmount = ((data?.getLongExtra(RazorpayPaymentActivity.EXTRA_AMOUNT_PAISE, 0L) ?: 0L) / 100).toInt().takeIf { it > 0 } ?: 100
                        Toast.makeText(context, "₹$successfulRechargeAmount loaded into wallet successfully!", Toast.LENGTH_LONG).show()
                    },
                    onError = { err ->
                        isProcessingPayment = false
                        Toast.makeText(context, "Verification failed: $err", Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                isProcessingPayment = false
                Toast.makeText(context, "Payment details missing from gateway response", Toast.LENGTH_LONG).show()
            }
        } else {
            isProcessingPayment = false
            Toast.makeText(context, "Recharge cancelled. No amount was deducted.", Toast.LENGTH_SHORT).show()
        }
    }

    var selectedAmount by remember { mutableIntStateOf(100) }
    var customAmountText by remember { mutableStateOf("100") }
    var isCustomSelected by remember { mutableStateOf(false) }

    val quickAmounts = listOf(50, 100, 200, 500)

    val currentRechargeAmount = if (isCustomSelected) {
        customAmountText.toIntOrNull() ?: 0
    } else {
        selectedAmount
    }

    val isAmountValid = currentRechargeAmount >= 50

    // Recent Transactions
    val transactions = remember { mutableStateListOf<LedgerEntry>() }
    var isLoadingLedger by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            viewModel.referralService.ledger(20).collectLatest { entries ->
                transactions.clear()
                transactions.addAll(entries)
                isLoadingLedger = false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading wallet ledger")
            isLoadingLedger = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Services Wallet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            fontFamily = MeeshoFontFamily
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "100% Razorpay Secured • Zero Hidden Fees",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFAFFFF)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HERO FINTECH WALLET BALANCE CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0F172A),
                                        Color(0xFF1E293B),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "DutyPe Service Wallet",
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF16A34A).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "● Active",
                                        color = Color(0xFF4ADE80),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            Column {
                                Text(
                                    text = "Available Balance",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "₹$balanceRupees",
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = MeeshoFontFamily
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Closed-loop PPI wallet • 1-click home service checkout",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. RECHARGE AMOUNT SELECTOR CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Add Money to Wallet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            fontFamily = MeeshoFontFamily
                        )

                        Text(
                            text = "Select a quick amount or enter custom recharge value (Min ₹50):",
                            fontSize = 12.5.sp,
                            color = Color(0xFF64748B)
                        )

                        // Quick Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickAmounts.forEach { amt ->
                                val isSelected = !isCustomSelected && selectedAmount == amt
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            isCustomSelected = false
                                            selectedAmount = amt
                                            customAmountText = amt.toString()
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.5.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "₹$amt",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF334155),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                }
                            }
                        }

                        // Custom Amount TextField
                        OutlinedTextField(
                            value = customAmountText,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() }.take(5)
                                customAmountText = clean
                                isCustomSelected = true
                                val num = clean.toIntOrNull() ?: 0
                                selectedAmount = num
                            },
                            label = { Text("Recharge Amount (₹)") },
                            prefix = {
                                Text(
                                    "₹ ",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0F172A),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        if (!isAmountValid) {
                            Text(
                                text = "⚠️ Minimum deposit amount is ₹50",
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Primary Action Button
                        Button(
                            onClick = {
                                if (!isAmountValid) {
                                    Toast.makeText(context, "Minimum recharge amount is ₹50", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                if (currentUser == null) {
                                    Toast.makeText(context, "Please login first to recharge wallet", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isProcessingPayment = true
                                paymentProcessingMessage = "Creating secure Razorpay order..."

                                subscriptionViewModel.createRazorpayOrder(
                                    type = "WALLET",
                                    amountRupees = currentRechargeAmount,
                                    onReady = { order ->
                                        paymentProcessingMessage = "Opening Razorpay payment gateway..."
                                        val intent = RazorpayPaymentActivity.createIntent(
                                            context = context,
                                            orderId = order.orderId,
                                            amountPaise = order.amountPaise,
                                            planName = "DutyPe Service Wallet Recharge",
                                            userPhone = currentUser.phoneNumber.orEmpty(),
                                            userEmail = currentUser.email.orEmpty(),
                                            keyId = order.keyId
                                        )
                                        razorpayLauncher.launch(intent)
                                        isProcessingPayment = false
                                    },
                                    onError = { err ->
                                        isProcessingPayment = false
                                        Toast.makeText(context, "Failed: $err", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = isAmountValid && !isProcessingPayment,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Add ₹$currentRechargeAmount via Razorpay ➔",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 3. TRUST & VALUE PROPOSITIONS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Why use DutyPe Service Wallet?",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        WalletBenefitRow(
                            icon = Icons.Default.Bolt,
                            iconTint = Color(0xFFEAB308),
                            title = "Instant 1-Click Bookings",
                            desc = "Zero payment failures or bank OTP delays during urgent service bookings"
                        )
                        WalletBenefitRow(
                            icon = Icons.Default.Security,
                            iconTint = Color(0xFF16A34A),
                            title = "100% Refundable Guarantee",
                            desc = "Unused money can be refunded back to your bank account anytime"
                        )
                        WalletBenefitRow(
                            icon = Icons.Default.Shield,
                            iconTint = Color(0xFF2563EB),
                            title = "Razorpay Bank-Grade Security",
                            desc = "Encrypted 256-bit SSL checkout supporting UPI (GPay, PhonePe, Paytm), Cards & NetBanking"
                        )
                    }
                }
            }

            // 4. TRANSACTION HISTORY
            item {
                Text(
                    text = "Recent Transactions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    fontFamily = MeeshoFontFamily,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (isLoadingLedger) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF0F172A),
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            } else if (transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No Transactions Yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            Text(
                                text = "Your wallet top-ups and service booking payments will appear here.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(transactions) { entry ->
                    TransactionItemCard(entry)
                }
            }
        }
    }

    // Processing Dialog
    if (isProcessingPayment) {
        Dialog(onDismissRequest = {}) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF0A66C2),
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = paymentProcessingMessage,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        }
    }

    // Success Dialog
    if (successfulRechargeAmount != null) {
        AlertDialog(
            onDismissRequest = { successfulRechargeAmount = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = null,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "Wallet Recharge Successful! 🎉",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        fontFamily = MeeshoFontFamily
                    )

                    Text(
                        text = "₹$successfulRechargeAmount has been loaded into your DutyPe Service Wallet immediately.",
                        fontSize = 13.sp,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { successfulRechargeAmount = null },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                ) {
                    Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun WalletBenefitRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(15.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
            Text(desc, fontSize = 11.5.sp, color = Color(0xFF64748B), lineHeight = 16.sp)
        }
    }
}

@Composable
private fun TransactionItemCard(entry: LedgerEntry) {
    val isCredit = entry.amountPaise > 0
    val amountRupees = Math.abs(entry.amountPaise) / 100
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(entry.createdAt) {
        runCatching { dateFormatter.format(Date(entry.createdAt)) }.getOrDefault("")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isCredit) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isCredit) Color(0xFF16A34A) else Color(0xFFDC2626),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = when {
                            entry.type.contains("DEPOSIT", ignoreCase = true) -> "Wallet Recharge (Razorpay)"
                            entry.type.contains("REFUND", ignoreCase = true) -> "Booking Fee Refund"
                            entry.type.contains("BOOKING", ignoreCase = true) -> "Service Booking Payment"
                            else -> entry.label
                        },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Text(
                text = "${if (isCredit) "+" else "-"}₹$amountRupees",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCredit) Color(0xFF16A34A) else Color(0xFFDC2626)
            )
        }
    }
}
