package com.example.dutype.employer.screens

import com.example.dutype.navigation.Routes

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import androidx.compose.runtime.LaunchedEffect
import com.example.dutype.models.formatPaise
import com.dutype.app.R
import android.app.Activity
import android.net.Uri
import android.widget.Toast
import com.example.dutype.payment.RazorpayPaymentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.example.dutype.models.EmployerSubscription
import com.example.dutype.repositories.PaymentRequest
import com.example.dutype.models.Plan
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.viewmodels.SubscriptionViewModel
import com.example.dutype.viewmodels.SubmitState
import com.example.dutype.utils.ImageUploadUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =============================================================================
// Redesigned Subscription Palette
// =============================================================================
private val Brand = Color(0xFF1C3A27) // Dark Green for main buttons
private val BrandSoft = Color(0xFFECFDF5)
private val IndicatorYellow = Color(0xFFFBBF24)
private val IndicatorGreen = Color(0xFF22C55E)
private val IndicatorOrange = Color(0xFFF97316)
private val IndicatorPurple = Color(0xFF8B5CF6)
private val Success = Color(0xFF10B981)
private val Ink900 = Color(0xFF0F172A)
private val Ink700 = Color(0xFF334155)
private val Ink600 = Color(0xFF475569)
private val Ink500 = Color(0xFF64748B)
private val Ink400 = Color(0xFF94A3B8)
private val Hairline = Color(0xFFE2E8F0)
private val SurfaceMuted = Color(0xFFFFFFFF)
private val Danger = Color(0xFFEF4444)
private val ScreenBg = Color(0xFFFFFFFF)
private val BlueAccent = Color(0xFF2563EB)
private val BlueSoft = Color(0xFFEFF6FF)
private val RingTrack = Color(0xFF1E293B)
private val PlanTitleInk = Color(0xFF0F0F0F)

// Plan class is imported from models

@Composable
private fun planBenefits(plan: com.example.dutype.models.Plan): List<AnnotatedString> = buildList {
    val isSingle = plan.id.contains("single", ignoreCase = true) || plan.priceRupees in 1..49
    val isBasic = !isSingle && (plan.id.contains("basic", ignoreCase = true) || plan.priceRupees <= 99)
    val isLaunch = plan.id.contains("launch", ignoreCase = true) || plan.name.contains("Launch", ignoreCase = true) || plan.priceRupees in 100..250

    val jobCredits = if (plan.jobs > 0) plan.jobs else if (isBasic) 3 else if (isLaunch) 5 else 20
    val contactUnlocks = if (plan.instantUnlocks > 0) plan.instantUnlocks else if (isBasic) 25 else if (isLaunch) 50 else 100

    if (isSingle) {
        add(buildAnnotatedString { append("1 job posting credit (Instant publish)") })
        add(buildAnnotatedString { append("10 worker contact unlocks (Phone & WhatsApp)") })
        add(buildAnnotatedString {
            withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                append("DutyPe AI hiring assistant")
            }
            append(" (10 prompts/day)")
        })
        add(buildAnnotatedString { append("Instant alert sent to nearby workers") })
        add(buildAnnotatedString { append("Direct calling with zero agent fee") })
        add(buildAnnotatedString { append("30-day single job validity") })
    } else if (isBasic) {
        add(buildAnnotatedString { append("$jobCredits job posting credits") })
        add(buildAnnotatedString { append("$contactUnlocks worker contact unlocks") })
        add(buildAnnotatedString {
            withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                append("DutyPe AI hiring assistant")
            }
            append(" (25 prompts/day)")
        })
        add(buildAnnotatedString { append("Browse verified local workers") })
        add(buildAnnotatedString { append("Direct candidate calling on phone") })
        add(buildAnnotatedString { append("30-day plan validity") })
    } else if (isLaunch) {
        add(buildAnnotatedString { append("$jobCredits job posting credits") })
        add(buildAnnotatedString { append("$contactUnlocks worker contact unlocks") })
        add(buildAnnotatedString {
            withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                append("Liri AI voice assistant")
            }
            append(" (50 actions/day)")
        })
        add(buildAnnotatedString { append("AI-powered top 3 recommendations") })
        add(buildAnnotatedString { append("Voice in Telugu, Hindi & English") })
        add(buildAnnotatedString { append("45-day extended plan validity") })
    } else {
        // Business Pro
        add(buildAnnotatedString { append("20 job posting credits (Unlimited)") })
        add(buildAnnotatedString { append("100 worker contact unlocks") })
        add(buildAnnotatedString {
            withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                append("Liri AI voice assistant Pro")
            }
            append(" (500 actions/day)")
        })
        add(buildAnnotatedString { append("Priority applicant match notifications") })
        add(buildAnnotatedString { append("Voice in Telugu, Hindi & English") })
        add(buildAnnotatedString { append("45-day extended plan validity") })
    }
}

private fun planTagline(plan: Plan): String = when {
    plan.id.contains("single", ignoreCase = true) || plan.priceRupees in 1..49 -> "Ideal for: One-time micro employer (e.g. 1 cook or 1 maid)"
    plan.id.contains("basic", ignoreCase = true) || plan.priceRupees in 50..99 -> "Ideal for: Small shops, clinics, small coaching institutes"
    plan.id.contains("launch", ignoreCase = true) || plan.name.contains("Launch", ignoreCase = true) || plan.priceRupees in 100..250 -> "Ideal for: Restaurants, supermarkets, schools, hostels"
    else -> "Ideal for: Manpower agencies, factories, hotel chains"
}

private fun planSummary(plan: Plan): String = when {
    plan.id.contains("single", ignoreCase = true) || plan.priceRupees in 1..49 -> "Post 1 single job & get direct calls"
    plan.id.contains("basic", ignoreCase = true) || plan.priceRupees in 50..99 -> "Post 3 jobs & contact 25 candidates directly"
    plan.id.contains("launch", ignoreCase = true) || plan.name.contains("Launch", ignoreCase = true) || plan.priceRupees in 100..250 -> "Hire faster with AI voice assistance"
    else -> "Full hiring power with 100 unlocks & AI Pro"
}

private fun planButtonLabel(plan: Plan, isCurrent: Boolean): String = when {
    isCurrent -> "Renew Plan"
    plan.id.contains("single", ignoreCase = true) || plan.priceRupees in 1..49 -> "Choose Single Job (₹49)"
    plan.id.contains("basic", ignoreCase = true) || plan.priceRupees in 50..99 -> "Choose Starter Basic (₹99)"
    plan.id.contains("launch", ignoreCase = true) || plan.name.contains("Launch", ignoreCase = true) || plan.priceRupees in 100..250 -> "Choose Launch Pass (45 Days)"
    else -> "Choose Business Pro (45 Days)"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerSubscriptionScreen(
    navController: NavController,
    rootNavController: NavController? = null,
    isExtension: Boolean = false,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val qrs by viewModel.activeQrCodes.collectAsState()
    val submitState by viewModel.submitState.collectAsState()
    val paymentRequests by viewModel.paymentRequests.collectAsState()
    val subState by viewModel.activeSubscription.collectAsState()
    val plans by viewModel.plans.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadPurchaseData() }

    val dateTimeFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var isProcessingPayment by remember { mutableStateOf(false) }
    var paymentProcessingMessage by remember { mutableStateOf("Connecting to Razorpay...") }
    var successfulSubscription by remember { mutableStateOf<Triple<String, String, Long>?>(null) } // (planName, paymentId, amountRupees)

    val razorpayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val paymentId = data?.getStringExtra(RazorpayPaymentActivity.EXTRA_PAYMENT_ID).orEmpty()
            val orderId = data?.getStringExtra(RazorpayPaymentActivity.EXTRA_ORDER_ID_RES).orEmpty()
            val signature = data?.getStringExtra(RazorpayPaymentActivity.EXTRA_SIGNATURE).orEmpty()

            if (paymentId.isNotBlank() && orderId.isNotBlank() && signature.isNotBlank()) {
                isProcessingPayment = true
                paymentProcessingMessage = "Verifying payment with bank & activating plan..."

                viewModel.verifyRazorpayPayment(
                    orderId = orderId,
                    paymentId = paymentId,
                    signature = signature,
                    type = "SUBSCRIPTION",
                    onSuccess = {
                        isProcessingPayment = false
                        viewModel.loadPurchaseData()
                        viewModel.loadPlans()
                        val currentSub = viewModel.activeSubscription.value
                        val activePlan = plans.find { it.id == currentSub.planId }
                        successfulSubscription = Triple(activePlan?.name ?: "Subscription Plan", paymentId, activePlan?.priceRupees ?: 0L)
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
            val data = result.data
            val err = data?.getStringExtra(RazorpayPaymentActivity.EXTRA_ERROR_MESSAGE)
                ?: "Payment was cancelled"
            timber.log.Timber.w("Razorpay payment cancelled or dismissed: $err")
            Toast.makeText(context, "Payment cancelled. No amount was deducted.", Toast.LENGTH_SHORT).show()
        }
    }

    val hasActiveSub = subState.isActive
    val currentPlanObj = plans.find { it.id == subState.planId }

    Scaffold(
        containerColor = ScreenBg.bg(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ScreenBg.bg())
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 40.dp)
        ) {
            PlansHeader(onBack = { navController.popBackStack() })

            Spacer(modifier = Modifier.height(20.dp))

            val employerProfile by viewModel.employer.collectAsState()
            val aiStatus = when {
                subState.hasAi -> stringResource(R.string.sub_ai_status_included, subState.aiPerDay.takeIf { it > 0 } ?: 100)
                else -> stringResource(R.string.sub_ai_status_trial, (10 - (employerProfile?.aiTrialUsed ?: 0)).coerceAtLeast(0))
            }
            if (hasActiveSub) {
                CurrentPlanCard(
                    aiStatus = aiStatus,
                    planName = currentPlanObj?.name ?: if (subState.isUnlimitedCampaign) stringResource(R.string.unlimited_campaign) else stringResource(R.string.sub_current_plan_default),
                    plan = currentPlanObj,
                    remainingJobs = subState.normalCredits,
                    remainingContacts = subState.instantCredits,
                    expiryDate = subState.expiresAt
                )
            } else {
                NoPlanCard(aiStatus)
            }

            Spacer(modifier = Modifier.height(20.dp))

            val sortedPlans = remember(plans) { plans.sortedBy { it.pricePaise } }

            if (sortedPlans.isNotEmpty()) {
                PlansCarousel(
                    plans = sortedPlans,
                    currentPlanId = subState.planId,
                    hasActiveSub = hasActiveSub,
                    onProceed = { plan ->
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (currentUser == null) {
                            Toast.makeText(context, "Please login first to subscribe", Toast.LENGTH_SHORT).show()
                        } else {
                            isProcessingPayment = true
                            paymentProcessingMessage = "Creating secure Razorpay order..."

                            viewModel.createRazorpayOrder(
                                planId = plan.id,
                                type = "SUBSCRIPTION",
                                amountRupees = plan.priceRupees.toInt(),
                                onReady = { order ->
                                    paymentProcessingMessage = "Opening Razorpay payment gateway..."
                                    val intent = RazorpayPaymentActivity.createIntent(
                                        context = context,
                                        orderId = order.orderId,
                                        amountPaise = order.amountPaise,
                                        planName = plan.name,
                                        userPhone = currentUser.phoneNumber.orEmpty(),
                                        userEmail = currentUser.email.orEmpty(),
                                        keyId = order.keyId
                                    )
                                    razorpayLauncher.launch(intent)
                                    isProcessingPayment = false
                                },
                                onError = { err ->
                                    isProcessingPayment = false
                                    Toast.makeText(context, "Order creation failed: $err", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    onTalkToSales = { openSalesContact(context) }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        com.example.dutype.components.ThreeDotsBouncingLoader(
                            dotSize = 9.dp,
                            dotColor = Color(0xFF222222),
                            bounceHeight = 7.dp,
                            spacing = 7.dp
                        )
                        Text(
                            text = "Loading subscription plans...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Ink500.fg()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            CreditsEducationSection(context)

            if (paymentRequests.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 24.dp, end = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionHeader(title = stringResource(R.string.recent_transactions))
                    paymentRequests.forEach { req ->
                        TransactionCard(
                            req = req,
                            dateTimeFormatter = dateTimeFormatter,
                            dateFormatter = dateFormatter
                        )
                    }
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

    // Success Celebration Dialog
    if (successfulSubscription != null) {
        val (planTitle, paymentId, amount) = successfulSubscription!!
        AlertDialog(
            onDismissRequest = { successfulSubscription = null },
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
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Text(
                        text = "Payment Successful! 🎉",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        fontFamily = MeeshoFontFamily
                    )

                    Text(
                        text = "₹$amount paid successfully via Razorpay",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF15803D)
                    )

                    Text(
                        text = "Your $planTitle is active immediately! You now have full access to candidate contact unlocks and priority job posts.",
                        fontSize = 12.5.sp,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Payment ID: $paymentId",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        successfulSubscription = null
                        try {
                            navController.navigate(Routes.EMPLOYER_POST_JOB) {
                                popUpTo(Routes.EMPLOYER_HOME) { inclusive = false }
                                launchSingleTop = true
                            }
                        } catch (e: Exception) {
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                ) {
                    Text("🚀 Post Vacancy Now", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { successfulSubscription = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Active Subscription", color = Color(0xFF475569))
                }
            }
        )
    }

}

// =============================================================================
// Reusable building blocks
// =============================================================================

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = AppTypography.pageTitle.copy(fontWeight = FontWeight.Bold, color = Ink900.fg()),
            fontFamily = MeeshoFontFamily
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = AppTypography.bodySmall.copy(color = Ink500.fg())
            )
        }
    }
}

@Composable
private fun PlanPill(text: String, background: Color, foreground: Color) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(99.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = foreground,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StepLabel(step: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$step.",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Brand.fg()
        )
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Ink700.fg()
        )
    }
}

private fun daysRemaining(expiry: Long): Int {
    if (expiry <= 0L) return -1
    val ms = expiry - System.currentTimeMillis()
    return if (ms <= 0L) 0 else ((ms + 86399999L) / 86400000L).toInt()
}

private fun openSalesContact(context: android.content.Context) {
    val message = "Hi DutyPe Team, I would like to talk to sales about an Enterprise plan."
    val whatsapp = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://api.whatsapp.com/send?phone=918019151847&text=${Uri.encode(message)}")
    }
    try {
        context.startActivity(whatsapp)
    } catch (e: Exception) {
        val mail = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:support@dutype.in")
        }
        try {
            context.startActivity(mail)
        } catch (e2: Exception) {
            Toast.makeText(context, "Email support@dutype.in to talk to sales", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun PlansHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Ink900.fg(),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.plans_and_credits),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Ink900.fg()
        )
    }
}

@Composable
private fun ProgressRing(used: Int, limit: Int, color: Color, label: String) {
    val fraction = if (limit > 0) (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f) else 0f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            val ringTrack = RingTrack.bd()
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 5.dp.toPx()
                val inset = strokePx / 2f
                val arcSize = Size(size.width - strokePx, size.height - strokePx)
                val arcTopLeft = Offset(inset, inset)
                drawArc(
                    color = ringTrack,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx)
                )
                if (fraction > 0f) {
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            }
            Text(
                text = "$used/$limit",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, fontSize = 11.sp, color = Color.White)
    }
}

@Composable
private fun CurrentPlanCard(
    planName: String,
    plan: Plan?,
    remainingJobs: Int,
    remainingContacts: Int,
    expiryDate: Long,
    aiStatus: String
) {
    val days = daysRemaining(expiryDate)
    val displayName = if (planName.contains("plan", ignoreCase = true) || planName.contains("trial", ignoreCase = true)) planName else "$planName Plan"

    val jobLimit = maxOf(plan?.jobs ?: 0, remainingJobs)
    val jobUsed = (jobLimit - remainingJobs).coerceAtLeast(0)

    val contactLimit = maxOf(plan?.instantUnlocks ?: 0, remainingContacts)
    val contactUsed = (contactLimit - remainingContacts).coerceAtLeast(0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.2.dp, Color(0xFF86EFAC)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF0FDF4))))
                .padding(20.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ACTIVE PLAN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00A859),
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = displayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }
            if (days >= 0) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (days == 1) stringResource(R.string.day_remaining_single) else stringResource(R.string.days_remaining_plural, days),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD97706),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
        if (jobLimit > 0 || contactLimit > 0) {
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (jobLimit > 0) {
                    ProgressRing(used = jobUsed, limit = jobLimit, color = Color(0xFF2563EB), label = stringResource(R.string.job_posts_label))
                }
                if (contactLimit > 0) {
                    ProgressRing(used = contactUsed, limit = contactLimit, color = Color(0xFF059669), label = stringResource(R.string.contacts_label))
                }
            }
        }
    }
    }
}

@Composable
private fun NoPlanCard(aiStatus: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF7F7F7))))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.sub_no_active_plan),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontFamily = MeeshoFontFamily
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.sub_choose_plan_desc),
                        fontSize = 12.5.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 17.sp
                    )
                }

            }

        }

    }

}



private fun carouselStartPage(plans: List<Plan>, currentPlanId: String, hasActiveSub: Boolean): Int {
    if (hasActiveSub) {
        val current = plans.indexOfFirst { it.id == currentPlanId }
        if (current >= 0) return current
    }
    return 0
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlansCarousel(
    plans: List<Plan>,
    currentPlanId: String,
    hasActiveSub: Boolean,
    onProceed: (Plan) -> Unit,
    onTalkToSales: () -> Unit
) {
    val totalPages = plans.size + 1
    val pagerState = rememberPagerState(
        initialPage = carouselStartPage(plans, currentPlanId, hasActiveSub),
        pageCount = { totalPages }
    )
    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(start = 20.dp, end = 52.dp),
        pageSpacing = 14.dp,
        modifier = Modifier.fillMaxWidth()
    ) { page ->
        if (page < plans.size) {
            val plan = plans[page]
            PlanPageCard(
                plan = plan,
                isCurrent = hasActiveSub && currentPlanId == plan.id,
                isRecommended = plan.id.contains("launch", ignoreCase = true) || plan.name.contains("Launch", ignoreCase = true) || plan.priceRupees in 100..250,
                onProceed = { onProceed(plan) }
            )
        } else {
            EnterprisePageCard(onTalkToSales = onTalkToSales)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    PagerDots(total = totalPages, current = pagerState.currentPage)
}

@Composable
private fun PagerDots(total: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until total) {
            if (i == current) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(6.dp)
                        .background(Ink900.bg(), RoundedCornerShape(50))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Hairline.bg(), RoundedCornerShape(50))
                )
            }
        }
    }
}

private data class PlanStyle(
    val cardGradient: Brush,
    val borderBrush: Brush,
    val borderWidth: Dp,
    val badgeBg: Color,
    val badgeBorder: Color,
    val badgeText: Color,
    val checkColor: Color,
    val checkBgColor: Color,
    val buttonBrush: Brush,
    val buttonTextColor: Color,
    val elevation: Dp
)

private fun getPlanStyle(plan: Plan, isRecommended: Boolean): PlanStyle {
    val price = plan.priceRupees
    return when {
        isRecommended || plan.id.contains("launch", ignoreCase = true) || plan.name.contains("Launch", ignoreCase = true) || price in 100..250 -> {
            // Hero / Launch Pass: Radiant Blue-Indigo Glassmorphism with Glowing Border
            PlanStyle(
                cardGradient = Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF0F7FF),
                        Color(0xFFE2EFFF)
                    )
                ),
                borderBrush = Brush.linearGradient(
                    listOf(
                        Color(0xFF2563EB),
                        Color(0xFF6366F1),
                        Color(0xFF38BDF8)
                    )
                ),
                borderWidth = 2.dp,
                badgeBg = Color(0xFFFEF3C7),
                badgeBorder = Color(0xFFF59E0B),
                badgeText = Color(0xFFB45309),
                checkColor = Color(0xFF2563EB),
                checkBgColor = Color(0xFFDBEAFE),
                buttonBrush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF1D4ED8),
                        Color(0xFF2563EB)
                    )
                ),
                buttonTextColor = Color.White,
                elevation = 6.dp
            )
        }
        price in 50..99 || plan.id.contains("basic", ignoreCase = true) -> {
            // Value Pack / Starter Basic: Clean Emerald Fintech
            PlanStyle(
                cardGradient = Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF0FDF4),
                        Color(0xFFDCFCE7)
                    )
                ),
                borderBrush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF86EFAC),
                        Color(0xFF4ADE80)
                    )
                ),
                borderWidth = 1.5.dp,
                badgeBg = Color(0xFFDCFCE7),
                badgeBorder = Color(0xFF86EFAC),
                badgeText = Color(0xFF15803D),
                checkColor = Color(0xFF16A34A),
                checkBgColor = Color(0xFFDCFCE7),
                buttonBrush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                ),
                buttonTextColor = Color.White,
                elevation = 3.dp
            )
        }
        price > 250 || plan.id.contains("pro", ignoreCase = true) -> {
            // Business Pro / Unlimited: Royal Amethyst Glass
            PlanStyle(
                cardGradient = Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFFAF5FF),
                        Color(0xFFF3E8FF)
                    )
                ),
                borderBrush = Brush.linearGradient(
                    listOf(
                        Color(0xFFC084FC),
                        Color(0xFF8B5CF6)
                    )
                ),
                borderWidth = 1.5.dp,
                badgeBg = Color(0xFFF3E8FF),
                badgeBorder = Color(0xFFD8B4FE),
                badgeText = Color(0xFF7E22CE),
                checkColor = Color(0xFF7C3AED),
                checkBgColor = Color(0xFFF3E8FF),
                buttonBrush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF4338CA),
                        Color(0xFF6366F1)
                    )
                ),
                buttonTextColor = Color.White,
                elevation = 4.dp
            )
        }
        else -> {
            // Single Job: Minimalist Modern Slate Glass
            PlanStyle(
                cardGradient = Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF7F7F7),
                        Color(0xFFF1F5F9)
                    )
                ),
                borderBrush = Brush.verticalGradient(
                    listOf(
                        Color(0xFFCBD5E1),
                        Color(0xFF94A3B8)
                    )
                ),
                borderWidth = 1.2.dp,
                badgeBg = Color(0xFFEFF6FF),
                badgeBorder = Color(0xFFBFDBFE),
                badgeText = Color(0xFF2563EB),
                checkColor = Color(0xFF2563EB),
                checkBgColor = Color(0xFFEFF6FF),
                buttonBrush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                ),
                buttonTextColor = Color.White,
                elevation = 2.dp
            )
        }
    }
}

@Composable
private fun PlanBadgePill(
    tag: String,
    customBg: Color? = null,
    customBorder: Color? = null,
    customText: Color? = null
) {
    val (defaultBg, defaultBorder, defaultText) = when (tag) {
        "Most Popular", "RECOMMENDED" -> Triple(Color(0xFFFEF3C7), Color(0xFFFCD34D), Color(0xFFD97706))
        "Instant Hire" -> Triple(Color(0xFFEFF6FF), Color(0xFFBFDBFE), Color(0xFF2563EB))
        "Value Pack" -> Triple(Color(0xFFECFDF5), Color(0xFFA7F3D0), Color(0xFF059669))
        else -> Triple(Color(0xFFF5F3FF), Color(0xFFDDD6FE), Color(0xFF7C3AED))
    }
    val bgColor = customBg ?: defaultBg
    val borderColor = customBorder ?: defaultBorder
    val textColor = customText ?: defaultText

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(50))
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = tag.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun PopularPill() {
    PlanBadgePill("Most Popular")
}

@Composable
private fun PlanBullets(
    benefits: List<AnnotatedString>,
    checkColor: Color,
    checkBgColor: Color = checkColor.copy(alpha = 0.12f)
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        benefits.forEach { benefit ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(checkBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = checkColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = benefit,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B),
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun PlanPageCard(
    plan: Plan,
    isCurrent: Boolean,
    isRecommended: Boolean,
    onProceed: () -> Unit
) {
    val isFree = plan.pricePaise <= 0L
    val benefits = planBenefits(plan)
    val style = getPlanStyle(plan, isRecommended)
    val cardShape = RoundedCornerShape(22.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(525.dp),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = style.elevation),
        border = BorderStroke(style.borderWidth, style.borderBrush),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(style.cardGradient)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                val badgeText = when {
                    plan.tag.isNotBlank() -> plan.tag
                    isRecommended || plan.priceRupees in 100..250 -> "Most Popular"
                    plan.priceRupees <= 49 -> "Instant Hire"
                    plan.priceRupees <= 99 -> "Value Pack"
                    else -> "Best Value"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = plan.name.uppercase(),
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        fontFamily = MeeshoFontFamily
                    )
                    PlanBadgePill(
                        tag = badgeText,
                        customBg = style.badgeBg,
                        customBorder = style.badgeBorder,
                        customText = style.badgeText
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = planTagline(plan),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = planSummary(plan),
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isFree) stringResource(R.string.free_cost) else "\u20B9${plan.priceRupees}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        fontFamily = MeeshoFontFamily
                    )
                    if (!isFree && plan.originalPriceRupees > plan.priceRupees) {
                        Text(
                            text = "\u20B9${plan.originalPriceRupees}",
                            fontSize = 15.sp,
                            color = Color(0xFF94A3B8),
                            style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                        )
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, Color(0xFF86EFAC))
                        ) {
                            Text(
                                text = "${plan.discountPercentage}% OFF",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (!isFree) {
                        Text(
                            text = "/ ${plan.validityDays}d",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                PlanBullets(
                    benefits = benefits,
                    checkColor = style.checkColor,
                    checkBgColor = style.checkBgColor
                )
            }
            if (!isFree) {
                Surface(
                    onClick = onProceed,
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(style.buttonBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = planButtonLabel(plan, isCurrent),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = style.buttonTextColor
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
    }
}

@Composable
private fun EnterprisePageCard(onTalkToSales: () -> Unit) {
    val cardShape = RoundedCornerShape(22.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(525.dp),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.2.dp, Brush.verticalGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF7F7F7), Color(0xFFF1F5F9))))
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.enterprise_plan_title),
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        fontFamily = MeeshoFontFamily
                    )
                    PlanBadgePill(
                        tag = "Custom Pack",
                        customBg = Color(0xFFF1F5F9),
                        customBorder = Color(0xFFCBD5E1),
                        customText = Color(0xFF475569)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ideal for: Multi-city chains, staffing firms & large teams",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Custom requirements? Dedicated account manager & SLA",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stringResource(R.string.custom_pricing),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        fontFamily = MeeshoFontFamily
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                val enterpriseBullets = listOf(
                    AnnotatedString("Unlimited Job Postings across all states"),
                    AnnotatedString("Unlimited Direct Candidate Unlocks"),
                    AnnotatedString("Dedicated Key Account Manager"),
                    AnnotatedString("Custom GST Invoicing & Corporate Agreement"),
                    AnnotatedString("Priority 24/7 Phone & WhatsApp Support")
                )
                PlanBullets(
                    benefits = enterpriseBullets,
                    checkColor = Color(0xFF0A66C2),
                    checkBgColor = Color(0xFFE8F1FC)
                )
            }
            Surface(
                onClick = onTalkToSales,
                shape = RoundedCornerShape(14.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF334155)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.talk_to_sales),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(
    req: PaymentRequest,
    dateTimeFormatter: SimpleDateFormat,
    dateFormatter: SimpleDateFormat
) {
    val statusColor = when (req.status) {
        "VERIFIED" -> Success
        "REJECTED" -> Danger
        else -> Color(0xFFF59E0B)
    }
    val statusText = when (req.status) {
        "VERIFIED" -> stringResource(R.string.status_approved)
        "REJECTED" -> stringResource(R.string.status_rejected)
        else -> stringResource(R.string.status_pending_verification)
    }
    val planName = when (req.planId) {
        "starter_99" -> stringResource(R.string.starter_plan)
        "growth_149" -> stringResource(R.string.growth_plan)
        "premium_299" -> stringResource(R.string.premium_plan)
        else -> stringResource(R.string.subscription_plan)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (com.example.dutype.ui.theme.isAppInDarkTheme()) Color(0xFF1A2233) else Color.White),
        border = BorderStroke(1.dp, Hairline.bd()),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = planName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Ink900.fg()
                )
                PlanPill(
                    text = statusText,
                    background = statusColor.copy(alpha = 0.12f),
                    foreground = statusColor
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.utr_format, req.utrNumber),
                    fontSize = 12.sp,
                    color = Ink700.fg(),
                    fontFamily = MeeshoFontFamily
                )
                Text(
                    text = formatPaise(req.amountPaise),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Brand.fg()
                )
            }
            Text(
                text = stringResource(R.string.requested_date_format, dateTimeFormatter.format(Date(req.createdAt))),
                fontSize = 11.sp,
                color = Ink400.fg()
            )
            if (req.status == "VERIFIED" && req.verifiedAt > 0L) {
                Text(
                    text = stringResource(R.string.verified_date_format, dateFormatter.format(Date(req.verifiedAt))),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Success.fg()
                )
            }
            if (req.status == "REJECTED" && req.rejectionReason.isNotBlank()) {
                Text(
                    text = stringResource(R.string.reason_format, req.rejectionReason),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Danger.fg()
                )
            }
        }
    }
}

@Composable
private fun CreditsEducationSection(context: android.content.Context) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Hairline.bd(), RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF0F0F0F),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Understanding Your Plan & Credits",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink900.fg()
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Ink900.fg(),
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CreditExplainerItem(
                    title = "1 Job Posting Credit",
                    description = "Lets you publish 1 job post visible to all workers in your selected area for 30 days."
                )
                CreditExplainerItem(
                    title = "1 Worker Contact Unlock",
                    description = "Unlocks direct phone number and WhatsApp contact for 1 worker who matches or applies."
                )
                CreditExplainerItem(
                    title = "30-Day Plan Validity",
                    description = "All credits, recommendations, and AI voice assistance are active for 30 days from purchase."
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Hairline.bg())
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?phone=918019151847&text=Hi%20DutyPe,%20I%20have%20questions%20about%20employer%20subscription%20plans.")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Contact WhatsApp: +91 8019151847", Toast.LENGTH_SHORT).show()
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Have questions before buying?",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink900.fg()
                        )
                        Text(
                            text = "Chat with DutyPe team on WhatsApp",
                            fontSize = 11.5.sp,
                            color = Color(0xFF059669)
                        )
                    }
                    Text(
                        text = "Chat now ›",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditExplainerItem(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink900.fg()
        )
        Text(
            text = description,
            fontSize = 12.sp,
            color = Ink500.fg(),
            lineHeight = 16.sp
        )
    }
}
