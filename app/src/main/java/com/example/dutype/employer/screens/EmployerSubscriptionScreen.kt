package com.example.dutype.employer.screens

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import androidx.compose.runtime.LaunchedEffect
import com.example.dutype.models.formatPaise
import com.dutype.app.R
import android.net.Uri
import android.widget.Toast
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

    var selectedPlan by remember { mutableStateOf<com.example.dutype.models.Plan?>(null) }
    var showPaymentSheet by remember { mutableStateOf(false) }

    var utrNumber by remember { mutableStateOf("") }
    var screenshotUrl by remember { mutableStateOf("") }
    var isUploadingScreenshot by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val screenshotPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    LaunchedEffect(submitState) {
        if (submitState is SubmitState.Success) {
            utrNumber = ""
            screenshotUrl = ""
            selectedImageUri = null
            showPaymentSheet = false
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
                        selectedPlan = plan
                        viewModel.resetSubmitState()
                        showPaymentSheet = true
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

    // Professional UPI Payment Gateway Bottom Sheet
    if (showPaymentSheet && selectedPlan != null) {
        val plan = selectedPlan!!
        val currentPhone = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.phoneNumber.orEmpty()
        val employerProfile = viewModel.employer.value
        val employerDisplayName = employerProfile?.businessName?.ifBlank { employerProfile.ownerName }.orEmpty()

        ModalBottomSheet(
            onDismissRequest = { showPaymentSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            scrimColor = Color.Black.copy(alpha = 0.45f),
            dragHandle = null,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: DutyPe Secure Checkout
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "DutyPe Secure Checkout",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Instant UPI Payment Rail",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    IconButton(onClick = { showPaymentSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                // 1. Order Summary Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = plan.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "${plan.jobs} Job Posts • ${plan.instantUnlocks} Contact Unlocks • ${plan.validityDays} Days",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${plan.priceRupees}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "All inclusive",
                                    fontSize = 11.sp,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // 2. Official Payee Assurance Banner (Payee: Banoth Vamsi)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Payee: ",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E40AF)
                                )
                                Text(
                                    text = "BANOTH VAMSI",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "• Official Account",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2563EB),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "Your UPI app will show 'Banoth Vamsi'. Please complete payment without worry.",
                                fontSize = 11.sp,
                                color = Color(0xFF3B82F6),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // 3. 1-Click Pay via Any UPI App (Direct Intent, Zero Friction)
                Button(
                    onClick = {
                        val refCode = "DP${(System.currentTimeMillis() % 1000000)}"
                        val upiUri = Uri.parse(
                            "upi://pay?pa=dutypein@ybl&pn=${Uri.encode("Banoth Vamsi")}&am=${plan.priceRupees}&cu=INR&tn=${Uri.encode("DutyPe Plan ₹${plan.priceRupees} - $currentPhone")}&tr=$refCode"
                        )
                        val upiIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, upiUri)
                        try {
                            context.startActivity(upiIntent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "No UPI app found. Please pay to dutypein@ybl", Toast.LENGTH_LONG).show()
                        }
                        // Automatically record the payment initiation with employer details! Zero UTR/Screenshot friction!
                        viewModel.recordUpiPaymentInitiated(
                            plan = plan,
                            referenceId = refCode,
                            employerName = employerDisplayName,
                            employerPhone = currentPhone
                        )
                        showPaymentSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                ) {
                    Text(
                        text = "⚡ Pay ₹${plan.priceRupees} via UPI (GPay / PhonePe / Paytm)",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 4. Copy UPI ID and Security Trust Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "UPI ID: dutypein@ybl",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF334155)
                        )
                    }
                    Text(
                        text = "Copy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF2563EB),
                        modifier = Modifier
                            .clickable {
                                clipboardManager.setText(AnnotatedString("dutypein@ybl"))
                                Toast.makeText(context, "UPI ID copied: dutypein@ybl", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Optional Manual UTR Toggle
                var showManualUtr by remember { mutableStateOf(false) }
                Text(
                    text = if (showManualUtr) "▲ Hide manual UTR" else "▼ Enter UTR manually (Optional)",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showManualUtr = !showManualUtr }
                        .padding(vertical = 4.dp)
                )

                if (showManualUtr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = utrNumber,
                            onValueChange = { if (it.length <= 12 && it.all { c -> c.isDigit() }) utrNumber = it },
                            placeholder = { Text("12-digit UTR", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Button(
                            onClick = {
                                if (utrNumber.length == 12) {
                                    viewModel.submitPayment(plan, utrNumber, "")
                                    showPaymentSheet = false
                                } else {
                                    Toast.makeText(context, "Please enter 12-digit UTR", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Text("Submit", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (submitState is SubmitState.Success) {
        Dialog(onDismissRequest = { viewModel.resetSubmitState() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Success.fg(),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Payment Initiated Successfully!",
                        style = AppTypography.displayTitle.copy(fontWeight = FontWeight.Bold, color = Ink900.fg()),
                        textAlign = TextAlign.Center,
                        fontFamily = MeeshoFontFamily
                    )
                    Text(
                        text = "Your payment to Banoth Vamsi (DutyPe) has been recorded. Our team will verify and activate your credits within 2-5 minutes!",
                        style = AppTypography.bodySmall.copy(color = Ink500.fg()),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            viewModel.resetSubmitState()
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Text("Go to Dashboard", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
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

@Composable
private fun NoPlanCard(aiStatus: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
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
                    color = Color(0xFF0F172A)
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

@Composable
private fun PlanBadgePill(tag: String) {
    val (bgColor, borderColor, textColor) = when (tag) {
        "Most Popular", "RECOMMENDED" -> Triple(Color(0xFFFEF3C7), Color(0xFFFCD34D), Color(0xFFD97706))
        "Instant Hire" -> Triple(Color(0xFFEFF6FF), Color(0xFFBFDBFE), Color(0xFF2563EB))
        "Value Pack" -> Triple(Color(0xFFECFDF5), Color(0xFFA7F3D0), Color(0xFF059669))
        else -> Triple(Color(0xFFF5F3FF), Color(0xFFDDD6FE), Color(0xFF7C3AED))
    }
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
private fun PlanBullets(benefits: List<AnnotatedString>, checkColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        benefits.forEach { benefit ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = checkColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = benefit, fontSize = 13.5.sp, color = Ink900.fg())
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
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
            .background(Color.White.bg(), cardShape)
            .border(
                if (isRecommended) 2.dp else 1.dp,
                if (isRecommended) Ink900.bd() else Hairline.bd(),
                cardShape
            )
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlanTitleInk.fg()
                )
                PlanBadgePill(badgeText)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = planTagline(plan),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink700.fg()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = planSummary(plan),
                fontSize = 11.5.sp,
                color = Ink500.fg(),
                lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isFree) stringResource(R.string.free_cost) else "\u20B9${plan.priceRupees}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlanTitleInk.fg()
                )
                if (!isFree && plan.originalPriceRupees > plan.priceRupees) {
                    Text(
                        text = "\u20B9${plan.originalPriceRupees}",
                        fontSize = 15.sp,
                        color = Ink400.fg(),
                        style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                    )
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${plan.discountPercentage}% OFF",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                if (!isFree) {
                    Text(
                        text = "/ ${plan.validityDays}d",
                        fontSize = 12.sp,
                        color = Ink500.fg(),
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            PlanBullets(benefits = benefits, checkColor = if (isFree) Ink400.fg() else BlueAccent.fg())
        }
        if (!isFree) {
            Button(
                onClick = onProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink900.bg(), contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = planButtonLabel(plan, isCurrent),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        } else {
            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}

@Composable
private fun EnterprisePageCard(onTalkToSales: () -> Unit) {
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(490.dp)
            .background(Color.White.bg(), cardShape)
            .border(1.dp, Hairline.bd(), cardShape)
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlanTitleInk.fg()
                )
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF1F5F9).bg(), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "CUSTOM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink700.fg()
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.custom_pricing),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlanTitleInk.fg()
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            val enterpriseBullets = listOf(
                AnnotatedString("Unlimited Job Postings"),
                AnnotatedString("Unlimited Direct Candidate Unlocks"),
                AnnotatedString("Dedicated Key Account Manager"),
                AnnotatedString("Custom GST Invoicing & Corporate Agreement"),
                AnnotatedString("Priority 24/7 Phone & WhatsApp Support")
            )
            PlanBullets(benefits = enterpriseBullets, checkColor = Color(0xFF10B981).fg())
        }
        OutlinedButton(
            onClick = onTalkToSales,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.5.dp, Ink900.bd()),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink900.fg())
        ) {
            Text(
                text = stringResource(R.string.talk_to_sales),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Ink900.fg()
            )
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
        colors = CardDefaults.cardColors(containerColor = Color.White.bg()),
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
