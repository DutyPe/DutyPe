package com.example.dutype.employer.screens

import com.dutype.app.R
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.dutype.models.PaymentRequest
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
private val Ink500 = Color(0xFF64748B)
private val Ink400 = Color(0xFF94A3B8)
private val Hairline = Color(0xFFE2E8F0)
private val SurfaceMuted = Color(0xFFF8FAFC)
private val Danger = Color(0xFFEF4444)
private val ScreenBg = Color(0xFFF8FAFC)
private val BlueAccent = Color(0xFF2563EB)
private val BlueSoft = Color(0xFFEFF6FF)
private val RingTrack = Color(0xFF1E293B)
private val PlanTitleInk = Color(0xFF0F0F0F)
private const val TRIAL_JOB_LIMIT = 2

private fun getPlanColor(planId: String): Color = when {
    planId.contains("starter") -> IndicatorGreen
    planId.contains("growth") -> IndicatorOrange
    planId.contains("premium") -> IndicatorPurple
    else -> IndicatorYellow
}

// Plan class is imported from models

@Composable
private fun planBenefits(plan: com.example.dutype.models.Plan): List<AnnotatedString> = buildList {
    when {
        plan.name.contains("Single", ignoreCase = true) -> {
            add(buildAnnotatedString { append(stringResource(R.string.sub_single_job_credit)) })
            add(buildAnnotatedString { append(stringResource(R.string.sub_single_instant_unlocks)) })
        }
        plan.name.contains("Starter", ignoreCase = true) -> {
            add(buildAnnotatedString { append(stringResource(R.string.sub_starter_job_credits)) })
            add(buildAnnotatedString { append(stringResource(R.string.sub_starter_instant_unlocks)) })
        }
        plan.name.contains("Growth", ignoreCase = true) -> {
            add(buildAnnotatedString {
                append(stringResource(R.string.sub_growth_job_credits))
                withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(R.string.sub_growth_bonus)) }
                append(")")
            })
            add(buildAnnotatedString { append(stringResource(R.string.sub_growth_instant_unlocks)) })
        }
        plan.name.contains("Premium", ignoreCase = true) -> {
            add(buildAnnotatedString {
                append(stringResource(R.string.sub_premium_job_credits))
                withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(R.string.sub_premium_bonus)) }
                append(")")
            })
            add(buildAnnotatedString { append(stringResource(R.string.sub_premium_instant_unlocks)) })
            add(buildAnnotatedString { append(stringResource(R.string.sub_premium_ceo_support)) })
        }
        else -> {
            add(buildAnnotatedString { append(stringResource(R.string.sub_dynamic_job_credits, plan.jobs.toString())) })
            add(buildAnnotatedString { append(stringResource(R.string.sub_dynamic_instant_unlocks, plan.instantUnlocks.toString())) })
        }
    }
    
    // Additional Universal Features
    add(buildAnnotatedString { append(stringResource(R.string.sub_30_days_validity)) })
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

    val hasActiveSub = subState.status == "ACTIVE" || subState.status == "TRIAL"
    val currentPlanObj = plans.find { it.id == subState.planId }

    Scaffold(
        containerColor = ScreenBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ScreenBg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 40.dp)
        ) {
            PlansHeader(onBack = { navController.popBackStack() })

            Spacer(modifier = Modifier.height(20.dp))

            if (hasActiveSub) {
                CurrentPlanCard(
                    planName = currentPlanObj?.name ?: "Trial (2 Free Posts)",
                    plan = currentPlanObj,
                    status = subState.status,
                    remainingJobs = subState.normalCredits,
                    remainingContacts = subState.instantCredits,
                    trialJobsUsed = subState.trialJobsUsed,
                    expiryDate = subState.expiryDate
                )
            } else {
                NoPlanCard()
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (plans.isNotEmpty()) {
                PlansCarousel(
                    plans = plans,
                    currentPlanId = subState.planId,
                    hasActiveSub = hasActiveSub,
                    onProceed = { plan ->
                        selectedPlan = plan
                        viewModel.resetSubmitState()
                        showPaymentSheet = true
                    },
                    onTalkToSales = { openSalesContact(context) }
                )
            }

            if (paymentRequests.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 24.dp, end = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionHeader(title = "Recent Transactions")
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
                    StepLabel(step = 1, text = stringResource(R.string.auto_copy_upi_id_pay))
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
                        StepLabel(step = 2, text = stringResource(R.string.auto_or_scan_the_active_payment_qr))
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
                        .background(SurfaceMuted, RoundedCornerShape(10.dp))
                        .border(1.dp, Hairline, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Ink500
                        )
                        Text(
                            text = stringResource(R.string.auto_payments_are_non_refundable_verify_your_ut),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Ink700
                        )
                    }
                }

                // Step 3: UTR Number input
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepLabel(step = if (qrs.isNotEmpty()) 3 else 2, text = stringResource(R.string.auto_enter_12_digit_utr_number))
                    OutlinedTextField(
                        value = utrNumber,
                        onValueChange = { if (it.length <= 12 && it.all { char -> char.isDigit() }) utrNumber = it },
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
                        text = stringResource(R.string.auto_attach_payment_screenshot_optional)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (selectedImageUri == null) screenshotPicker.launch("image/*") }
                            .background(SurfaceMuted, RoundedCornerShape(10.dp))
                            .border(1.dp, Hairline, RoundedCornerShape(10.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedImageUri != null) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Selected Screenshot",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.auto_screenshot_attached),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = Ink700
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Camera",
                                    tint = Ink500
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.auto_upload_screenshot),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = Ink700
                                )
                            }
                        }
                        if (selectedImageUri != null) {
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove screenshot",
                                    tint = Danger
                                )
                            }
                        }
                    }
                }

                // Submit Button
                val isSubmitEnabled = utrNumber.trim().length == 12 && !isUploadingScreenshot && (submitState !is SubmitState.Loading)
                Button(
                    onClick = {
                        if (selectedImageUri != null) {
                            isUploadingScreenshot = true
                            scope.launch {
                                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
                                val path = "payment_screenshots/$userId/payment_${System.currentTimeMillis()}.jpg"
                                val uploadResult = ImageUploadUtils.uploadWithRetry(context, selectedImageUri!!, path)
                                isUploadingScreenshot = false
                                when (uploadResult) {
                                    is ImageUploadUtils.UploadResult.Success -> {
                                        viewModel.submitPayment(plan.id, plan.price, utrNumber, uploadResult.downloadUrl)
                                    }
                                    is ImageUploadUtils.UploadResult.Failure -> {
                                        Toast.makeText(context, "Image upload failed: ${uploadResult.error}", Toast.LENGTH_LONG).show()
                                    }
                                    is ImageUploadUtils.UploadResult.Progress -> { /* progress handled locally if needed */ }
                                }
                            }
                        } else {
                            viewModel.submitPayment(plan.id, plan.price, utrNumber, "")
                        }
                    },
                    enabled = isSubmitEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (submitState is SubmitState.Loading || isUploadingScreenshot) {
                        com.example.dutype.components.DutyPeLoader(color = Color.White, size = 24.dp)
                    } else {
                        Text(
                            text = stringResource(R.string.auto_submit_transaction_details),
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
                        text = stringResource(R.string.auto_submission_successful),
                        style = AppTypography.displayTitle.copy(fontWeight = FontWeight.Bold, color = Ink900),
                        textAlign = TextAlign.Center,
                        fontFamily = MeeshoFontFamily
                    )
                    Text(
                        text = stringResource(R.string.auto_your_upi_reference_has_been_submitted_admi),
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
                    OutlinedButton(
                        onClick = {
                            val phoneNumber = "918500717800"
                            val message = "Hi DutyPe Team, please verify my purchased subscription plan. UTR: $utrNumber"
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${android.net.Uri.encode(message)}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Success),
                        border = BorderStroke(1.dp, Success)
                    ) {
                        Text("Contact Admin on WhatsApp", fontWeight = FontWeight.Bold)
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
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$step.",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Brand
        )
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Ink700
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
        data = Uri.parse("https://api.whatsapp.com/send?phone=918500717800&text=${Uri.encode(message)}")
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
                contentDescription = "Back",
                tint = Ink900,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Plans & Credits",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Ink900
        )
    }
}

@Composable
private fun ProgressRing(used: Int, limit: Int, color: Color, label: String) {
    val fraction = if (limit > 0) (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f) else 0f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 5.dp.toPx()
                val inset = strokePx / 2f
                val arcSize = Size(size.width - strokePx, size.height - strokePx)
                val arcTopLeft = Offset(inset, inset)
                drawArc(
                    color = RingTrack,
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
    status: String,
    remainingJobs: Int,
    remainingContacts: Int,
    trialJobsUsed: Int,
    expiryDate: Long
) {
    val isTrial = status == "TRIAL"
    val days = daysRemaining(expiryDate)
    val displayName = if (planName.contains("plan", ignoreCase = true) || planName.contains("trial", ignoreCase = true)) planName else "$planName Plan"

    val jobLimitBase = if (isTrial) TRIAL_JOB_LIMIT else (plan?.jobs ?: 0)
    val jobLimit = if (isTrial) jobLimitBase else maxOf(jobLimitBase, remainingJobs)
    val jobUsed = if (isTrial) trialJobsUsed.coerceIn(0, jobLimit) else (jobLimit - remainingJobs).coerceAtLeast(0)

    val contactLimitBase = if (isTrial) 0 else (plan?.instantUnlocks ?: 0)
    val contactLimit = maxOf(contactLimitBase, if (isTrial) 0 else remainingContacts)
    val contactUsed = (contactLimit - remainingContacts).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(Ink900, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Current: $displayName", fontSize = 13.sp, color = Ink400)
            if (days >= 0) {
                Text(
                    text = if (days == 1) "1 day remaining" else "$days days remaining",
                    fontSize = 13.sp,
                    color = IndicatorYellow
                )
            }
        }
        if (jobLimit > 0 || contactLimit > 0) {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (jobLimit > 0) {
                    ProgressRing(used = jobUsed, limit = jobLimit, color = BlueAccent, label = "Job Posts")
                }
                if (contactLimit > 0) {
                    ProgressRing(used = contactUsed, limit = contactLimit, color = Success, label = "Contacts")
                }
            }
        }
    }
}

@Composable
private fun NoPlanCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(Ink900, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(R.string.sub_no_active_plan),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.sub_choose_plan_desc),
            fontSize = 13.sp,
            color = Ink400
        )
    }
}

private fun carouselStartPage(plans: List<Plan>, currentPlanId: String, hasActiveSub: Boolean): Int {
    if (hasActiveSub) {
        val current = plans.indexOfFirst { it.id == currentPlanId }
        if (current >= 0) return current
    }
    val growth = plans.indexOfFirst { it.name.contains("Growth", ignoreCase = true) || it.id.contains("growth") }
    return if (growth >= 0) growth else 0
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp),
        pageSpacing = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) { page ->
        if (page < plans.size) {
            val plan = plans[page]
            PlanPageCard(
                plan = plan,
                isCurrent = hasActiveSub && currentPlanId == plan.id,
                isRecommended = plan.name.contains("Growth", ignoreCase = true) || plan.id.contains("growth"),
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
                        .background(Ink900, RoundedCornerShape(50))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Hairline, RoundedCornerShape(50))
                )
            }
        }
    }
}

@Composable
private fun PopularPill() {
    Box(
        modifier = Modifier
            .background(BlueSoft, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "MOST POPULAR",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = BlueAccent
        )
    }
}

@Composable
private fun PlanBullets(benefits: List<AnnotatedString>, checkColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        benefits.forEach { benefit ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = checkColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = benefit, fontSize = 14.sp, color = Ink900)
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
    val isFree = plan.price <= 0.0
    val benefits = planBenefits(plan)
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, cardShape)
            .border(1.dp, Hairline, cardShape)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = plan.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PlanTitleInk
            )
            if (isRecommended) {
                PopularPill()
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (isFree) "Free" else "\u20B9${plan.price.toInt()}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = PlanTitleInk
            )
            if (!isFree) {
                Text(
                    text = " / month",
                    fontSize = 14.sp,
                    color = Ink500,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        PlanBullets(benefits = benefits, checkColor = if (isFree) Ink400 else BlueAccent)
        if (!isFree) {
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = onProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink900, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                )
            ) {
                Text(
                    text = if (isCurrent) stringResource(R.string.sub_renew_now) else "Upgrade with UPI \u2192",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun EnterprisePageCard(onTalkToSales: () -> Unit) {
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, cardShape)
            .border(1.dp, Hairline, cardShape)
            .padding(20.dp)
    ) {
        Text(
            text = "Enterprise",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = PlanTitleInk
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Custom Pricing",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = PlanTitleInk
        )
        Spacer(modifier = Modifier.height(22.dp))
        OutlinedButton(
            onClick = onTalkToSales,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.5.dp, Ink900),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink900)
        ) {
            Text(
                text = "Talk to Sales \u2192",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink900
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
        "VERIFIED" -> "Approved"
        "REJECTED" -> "Rejected"
        else -> "Pending Verification"
    }
    val planName = when (req.planId) {
        "starter_99" -> "Starter Plan"
        "growth_149" -> "Growth Plan"
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
