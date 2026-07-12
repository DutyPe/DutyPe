package com.example.dutype.employer.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
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
import coil.compose.AsyncImage
import com.dutype.app.R
import com.example.dutype.models.EmployerSubscription
import com.example.dutype.models.PaymentRequest
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
private val Ink500 = Color(0xFF64748B)
private val Ink400 = Color(0xFF94A3B8)
private val Hairline = Color(0xFFE2E8F0)
private val SurfaceMuted = Color(0xFFF8FAFC)
private val Danger = Color(0xFFEF4444)

private fun getPlanColor(planId: String): Color = when(planId) {
    "starter_119" -> IndicatorGreen
    "growth_179" -> IndicatorOrange
    "premium_299" -> IndicatorPurple
    else -> IndicatorYellow
}

data class Plan(val id: String, val name: String, val price: Double, val jobs: Int, val description: String)

private fun planBenefits(planId: String): List<String> = when (planId) {
    "starter_119" -> listOf(
        "3 Standard Vacancy Posts",
        "30 Days Plan Validity",
        "View Matching Candidate Profiles",
        "Direct Dial/Chat with Candidates"
    )
    "growth_179" -> listOf(
        "6 Vacancy Posts (5 + 1 Free)",
        "30 Days Plan Validity",
        "Enable Job Expiration Extensions",
        "Priority Placement in Search List",
        "Dedicated Email Support"
    )
    "premium_299" -> listOf(
        "12 Vacancy Posts (10 + 2 Free)",
        "30 Days Plan Validity",
        "Unlimited Job Extensions",
        "Unlock Instant Gig Posting Tools",
        "Verified Partner Business Badge",
        "24/7 Priority Helpline Support"
    )
    else -> emptyList()
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

    val allPlans = listOf(
        Plan("extend_49", "Single Job Credit", 49.0, 1, "1 Credit to post a new job or extend an existing one"),
        Plan("starter_119", "Starter Plan", 119.0, 3, "Ideal for micro-employers with few active needs"),
        Plan("growth_179", "Growth Plan", 179.0, 6, "Best value! 5 + 1 Free job posts with extensions"),
        Plan("premium_299", "Premium Plan", 299.0, 12, "Enterprise plan with 10 + 2 Free posts")
    )
    
    val plans = remember(isExtension) {
        if (isExtension) allPlans else allPlans.filter { it.id != "extend_49" }
    }

    val dateTimeFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var selectedPlan by remember { mutableStateOf<Plan?>(null) }
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
            isUploadingScreenshot = true
            scope.launch {
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
                val path = "payment_screenshots/$userId/payment_${System.currentTimeMillis()}.jpg"
                val uploadResult = ImageUploadUtils.uploadWithRetry(context, uri, path)
                when (uploadResult) {
                    is ImageUploadUtils.UploadResult.Success -> {
                        screenshotUrl = uploadResult.downloadUrl
                        Toast.makeText(context, "Screenshot attached successfully!", Toast.LENGTH_SHORT).show()
                    }
                    is ImageUploadUtils.UploadResult.Failure -> {
                        Toast.makeText(context, "Upload failed: ${uploadResult.error}", Toast.LENGTH_LONG).show()
                    }
                    is ImageUploadUtils.UploadResult.Progress -> { /* progress handled below */ }
                }
                isUploadingScreenshot = false
            }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Ink900
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            selectedPlan?.let { plan ->
                val isCurrentPlan = subState.planId == plan.id && (subState.status == "ACTIVE" || subState.status == "TRIAL")
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 0.dp,
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showPaymentSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Brand),
                            shape = RoundedCornerShape(27.dp)
                        ) {
                            Text(
                                text = if (isCurrentPlan) "renew ${plan.name.lowercase()} ${plan.price.toInt()}/m" else "subscribe to ${plan.name.lowercase()} ${plan.price.toInt()}/m",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = buildAnnotatedString {
                                append("By subscribing, you agree to our ")
                                withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = Ink900)) {
                                    append("Terms of\nService")
                                }
                                append(" and ")
                                withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = Ink900)) {
                                    append("Privacy Policy")
                                }
                            },
                            fontSize = 11.sp,
                            color = Ink500,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = IndicatorGreen.copy(alpha = 0.15f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = IndicatorGreen,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Choose your plan",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink900,
                        fontFamily = MeeshoFontFamily
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Current plan / empty state
            val hasActiveSub = subState.status == "ACTIVE" || subState.status == "TRIAL"
            if (hasActiveSub) {
                item {
                    val currentPlanObj = plans.find { it.id == subState.planId }
                    val currentPlanName = currentPlanObj?.name ?: "Trial (2 Free Posts)"
                    CurrentPlanCard(
                        planName = currentPlanName,
                        status = subState.status,
                        remainingCredits = subState.normalCredits,
                        expiryDate = subState.expiryDate,
                        dateFormatter = dateFormatter
                    )
                }
            } else {
                item { NoPlanCard() }
            }

            // Plan list
            items(plans.size) { index ->
                val plan = plans[index]
                val isSelected = selectedPlan?.id == plan.id
                val isCurrentPlan = subState.planId == plan.id && (subState.status == "ACTIVE" || subState.status == "TRIAL")
                PlanCard(
                    plan = plan,
                    isSelected = isSelected,
                    isCurrent = isCurrentPlan,
                    isRecommended = plan.id == "growth_179",
                    onSelect = {
                        selectedPlan = plan
                        viewModel.resetSubmitState()
                    },
                    onProceed = {
                        selectedPlan = plan
                        viewModel.resetSubmitState()
                        showPaymentSheet = true
                    }
                )
            }

            if (paymentRequests.isNotEmpty()) {
                item {
                    SectionHeader(title = "Recent Transactions")
                }

                items(paymentRequests.size) { index ->
                    TransactionCard(
                        req = paymentRequests[index],
                        dateTimeFormatter = dateTimeFormatter,
                        dateFormatter = dateFormatter
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet for Checkout (UPI Details & UTR entry)
    if (showPaymentSheet && selectedPlan != null) {
        val plan = selectedPlan!!
        ModalBottomSheet(
            onDismissRequest = { showPaymentSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Checkout: ${plan.name}",
                        style = AppTypography.cardTitle.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                        fontFamily = MeeshoFontFamily
                    )
                    IconButton(onClick = { showPaymentSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Ink700)
                    }
                }

                Text(
                    text = "Amount Payable: ₹${plan.price.toInt()}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Brand
                )

                // Step 1: Copy UPI ID
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepLabel(step = 1, text = "Copy UPI ID & pay")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceMuted, RoundedCornerShape(10.dp))
                            .border(1.dp, Hairline, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "dutypein@ybl",
                            fontFamily = MeeshoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Ink900
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString("dutypein@ybl"))
                                Toast.makeText(context, "UPI ID copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = Ink500
                            )
                        }
                    }
                }

                // Step 2: QR Codes if configured
                if (qrs.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StepLabel(step = 2, text = "Or scan the active payment QR")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceMuted),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = qrs.first().imageUrl,
                                contentDescription = qrs.first().label,
                                modifier = Modifier.fillMaxHeight(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Text(
                            text = qrs.first().label,
                            fontSize = 11.sp,
                            color = Ink500,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Non-refundable notice
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Warning",
                            tint = Danger
                        )
                        Text(
                            text = "Payments are strictly non-refundable. Verify your UTR before submitting.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF991B1B)
                        )
                    }
                }

                // Step 3: UTR Number input
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepLabel(step = if (qrs.isNotEmpty()) 3 else 2, text = "Enter 12-digit UTR number")
                    OutlinedTextField(
                        value = utrNumber,
                        onValueChange = { if (it.length <= 12) utrNumber = it },
                        placeholder = { Text("e.g. 630987123456") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Brand,
                            unfocusedBorderColor = Hairline,
                            cursorColor = Brand
                        )
                    )
                }

                // Step 4: Upload screenshot
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepLabel(
                        step = if (qrs.isNotEmpty()) 4 else 3,
                        text = "Attach payment screenshot (optional)"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { screenshotPicker.launch("image/*") }
                            .background(SurfaceMuted, RoundedCornerShape(10.dp))
                            .border(1.dp, Hairline, RoundedCornerShape(10.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Camera",
                            tint = Ink500
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (screenshotUrl.isNotEmpty()) "Screenshot attached" else "Upload screenshot",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Ink700
                        )
                    }
                    if (isUploadingScreenshot) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            color = Brand
                        )
                    }
                }

                // Submit Button
                val isSubmitEnabled = utrNumber.trim().length == 12 && !isUploadingScreenshot
                Button(
                    onClick = { viewModel.submitPayment(plan.id, plan.price, utrNumber, screenshotUrl) },
                    enabled = isSubmitEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (submitState is SubmitState.Loading) {
                        com.example.dutype.components.DutyPeLoader(color = Color.White, size = 24.dp)
                    } else {
                        Text(
                            text = "Submit Transaction Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                if (submitState is SubmitState.Error) {
                    Text(
                        text = (submitState as SubmitState.Error).message,
                        color = Danger,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
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
                        tint = Success,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Submission Successful!",
                        style = AppTypography.displayTitle.copy(fontWeight = FontWeight.Bold, color = Ink900),
                        textAlign = TextAlign.Center,
                        fontFamily = MeeshoFontFamily
                    )
                    Text(
                        text = "Your UPI reference has been submitted. Admin will verify it with the bank and activate your plan within 2-4 hours.",
                        style = AppTypography.bodySmall.copy(color = Ink500),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            viewModel.resetSubmitState()
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand)
                    ) {
                        Text("Go to Dashboard", fontWeight = FontWeight.Bold)
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
            style = AppTypography.pageTitle.copy(fontWeight = FontWeight.Bold, color = Ink900),
            fontFamily = MeeshoFontFamily
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = AppTypography.bodySmall.copy(color = Ink500)
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Brand, RoundedCornerShape(99.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step.toString(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Ink700
        )
    }
}

@Composable
private fun CurrentPlanCard(
    planName: String,
    status: String,
    remainingCredits: Int,
    expiryDate: Long,
    dateFormatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, IndicatorYellow.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Left color bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(IndicatorYellow)
            )
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = planName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink900
                    )
                    PlanPill(
                        text = "✓ Current",
                        background = IndicatorYellow.copy(alpha = 0.15f),
                        foreground = IndicatorYellow
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$remainingCredits posts left • Expires ${dateFormatter.format(Date(expiryDate))}",
                    fontSize = 13.sp,
                    color = Ink500
                )
            }
        }
    }
}

@Composable
private fun NoPlanCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(Ink400)
            )
            Column(
                modifier = Modifier.padding(16.dp).weight(1f)
            ) {
                Text(
                    text = "No Active Subscription",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Ink900
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose a plan below to get credits and start posting jobs.",
                    fontSize = 13.sp,
                    color = Ink500
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: Plan,
    isSelected: Boolean,
    isCurrent: Boolean,
    isRecommended: Boolean,
    onSelect: () -> Unit,
    onProceed: () -> Unit
) {
    val planColor = getPlanColor(plan.id)
    val border = if (isSelected) BorderStroke(2.dp, IndicatorGreen.copy(alpha = 0.4f)) else BorderStroke(1.dp, Hairline)
    val elevation = if (isSelected) 8.dp else 2.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Left color bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(planColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = plan.name,
                            style = AppTypography.cardTitle.copy(
                                fontWeight = FontWeight.Bold,
                                color = Ink900,
                                fontSize = 16.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = plan.description,
                            style = AppTypography.bodySmall.copy(color = Ink500, fontSize = 13.sp)
                        )
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "₹${plan.price.toInt()}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Ink900
                        )
                        Text(
                            text = " / month",
                            fontSize = 12.sp,
                            color = Ink400,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Expanded Features
                if (isSelected) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceMuted, RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Features",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink700
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                planBenefits(plan.id).forEach { benefit ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = IndicatorGreen,
                                            modifier = Modifier.size(16.dp).offset(y = 2.dp)
                                        )
                                        Text(
                                            text = benefit,
                                            fontSize = 13.sp,
                                            color = Ink700,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
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
        "VERIFIED" -> "Approved"
        "REJECTED" -> "Rejected"
        else -> "Pending Verification"
    }
    val planName = when (req.planId) {
        "starter_119" -> "Starter Plan"
        "growth_179" -> "Growth Plan"
        "premium_299" -> "Premium Plan"
        else -> "Subscription Plan"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Hairline),
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
                    color = Ink900
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
                    text = "UTR: ${req.utrNumber}",
                    fontSize = 12.sp,
                    color = Ink700,
                    fontFamily = MeeshoFontFamily
                )
                Text(
                    text = "₹${req.amount.toInt()}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Brand
                )
            }
            Text(
                text = "Requested: ${dateTimeFormatter.format(Date(req.requestTimestamp))}",
                fontSize = 11.sp,
                color = Ink400
            )
            if (req.status == "VERIFIED" && req.expiryTimestamp != null) {
                Text(
                    text = "Expiry Date: ${dateFormatter.format(Date(req.expiryTimestamp))}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Success
                )
            }
            if (req.status == "REJECTED" && !req.rejectionReason.isNullOrBlank()) {
                Text(
                    text = "Reason: ${req.rejectionReason}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Danger
                )
            }
        }
    }
}
