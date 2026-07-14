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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.res.stringResource
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
        containerColor = Color.White
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            // Current plan / empty state
            val hasActiveSub = subState.status == "ACTIVE" || subState.status == "TRIAL"
            if (hasActiveSub) {
                item {
                    val currentPlanObj = plans.find { it.id == subState.planId }
                    val currentPlanName = currentPlanObj?.name ?: "Trial (2 Free Posts)"
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        CurrentPlanCard(
                            planName = currentPlanName,
                            status = subState.status,
                            remainingCredits = subState.normalCredits,
                            expiryDate = subState.expiryDate,
                            dateFormatter = dateFormatter
                        )
                    }
                }
            } else {
                item { 
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        NoPlanCard() 
                    }
                }
            }

            // Plan list
            item {
                if (plans.isNotEmpty()) {
                    val pagerState = rememberPagerState(
                        initialPage = if (plans.size > 2) 2 else 0,
                        pageCount = { plans.size }
                    )
                    
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        pageSpacing = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val plan = plans[page]
                        val isSelected = selectedPlan?.id == plan.id
                        val isCurrentPlan = subState.planId == plan.id && (subState.status == "ACTIVE" || subState.status == "TRIAL")
                        
                        PlanCard(
                            plan = plan,
                            isSelected = isSelected,
                            isCurrent = isCurrentPlan,
                            isRecommended = plan.id.contains("growth"),
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    

                }
            }

            if (paymentRequests.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SectionHeader(title = "Recent Transactions")
                    }
                }

                items(paymentRequests.size) { index ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        TransactionCard(
                            req = paymentRequests[index],
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
                            text = "Payments are non-refundable. Verify your UTR before submitting.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Ink700
                        )
                    }
                }

                // Step 3: UTR Number input
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepLabel(step = if (qrs.isNotEmpty()) 3 else 2, text = "Enter 12-digit UTR number")
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
                        text = stringResource(R.string.sub_current_plan_pill),
                        background = IndicatorYellow.copy(alpha = 0.15f),
                        foreground = IndicatorYellow
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.sub_posts_left_expires, remainingCredits, dateFormatter.format(Date(expiryDate))),
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
                    text = stringResource(R.string.sub_no_active_plan),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Ink900
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.sub_choose_plan_desc),
                    fontSize = 13.sp,
                    color = Ink500
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: com.example.dutype.models.Plan,
    isSelected: Boolean,
    isCurrent: Boolean,
    isRecommended: Boolean,
    onSelect: () -> Unit,
    onProceed: () -> Unit
) {
    val planType = when {
        plan.name.contains("Premium", true) -> "premium"
        plan.name.contains("Growth", true) -> "growth"
        plan.name.contains("Starter", true) -> "starter"
        else -> "single"
    }
    
    val bgColors = when (planType) {
        "premium" -> listOf(Color(0xFF0066FF), Color(0xFF0044CC)) // Truecaller Vibrant Blue
        "growth" -> listOf(Color(0xFF059669), Color(0xFF047857)) // Emerald Green
        "starter" -> listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)) // Vibrant Purple
        else -> listOf(Color(0xFF475569), Color(0xFF1E293B)) // Slate
    }
    
    val accentColor = when (planType) {
        "premium" -> Color(0xFF60A5FA) // Lighter blue for accents
        "growth" -> Color(0xFF34D399) // Light green
        "starter" -> Color(0xFFA78BFA) // Light purple
        else -> Color(0xFF9CA3AF)
    }

    val textColor = Color.White
    val subTextColor = Color.White.copy(alpha = 0.7f)
    val featuresBg = Color.White.copy(alpha = 0.05f)
    val checkColor = Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 450.dp, max = 550.dp)
            .padding(horizontal = 4.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = if (isSelected) BorderStroke(2.dp, accentColor) else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 12.dp else 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = bgColors)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (plan.tag.isNotEmpty()) {
                            PlanPill(
                                text = plan.tag,
                                background = Color.White,
                                foreground = bgColors[0]
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        Text(
                            text = plan.name,
                            style = AppTypography.cardTitle.copy(
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                fontSize = 22.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = plan.description,
                            style = AppTypography.bodySmall.copy(color = subTextColor, fontSize = 14.sp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₹${plan.price.toInt()}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                        Text(
                            text = stringResource(R.string.sub_per_month),
                            fontSize = 12.sp,
                            color = subTextColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Expanded Features
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(featuresBg, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.sub_included_features),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = subTextColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        planBenefits(plan).forEach { benefit ->
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = checkColor,
                                    modifier = Modifier.size(18.dp).offset(y = 2.dp)
                                )
                                Text(
                                    text = benefit,
                                    fontSize = 13.sp,
                                    color = textColor,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = onProceed,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (planType == "single") Brand else Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isCurrent) stringResource(R.string.sub_renew_now) else stringResource(R.string.sub_get_plan, plan.name), 
                        fontWeight = FontWeight.Bold, 
                        color = if (planType == "single") Color.White else bgColors[1], 
                        fontSize = 16.sp
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
