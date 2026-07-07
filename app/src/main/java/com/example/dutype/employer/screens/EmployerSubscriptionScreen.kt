package com.example.dutype.employer.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.viewmodels.SubscriptionViewModel
import com.example.dutype.viewmodels.SubmitState
import com.example.dutype.utils.ImageUploadUtils
import kotlinx.coroutines.launch

data class Plan(val id: String, val name: String, val price: Double, val jobs: Int, val description: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerSubscriptionScreen(
    navController: NavController,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val qrs by viewModel.activeQrCodes.collectAsState()
    val submitState by viewModel.submitState.collectAsState()
    val paymentRequests by viewModel.paymentRequests.collectAsState()
    val subState by viewModel.activeSubscription.collectAsState()

    val plans = listOf(
        Plan("extend_49", "Single Job Credit", 49.0, 1, "1 Credit to post a new job or extend an existing one"),
        Plan("starter_119", "Starter Plan", 119.0, 3, "Ideal for micro-employers with few active needs"),
        Plan("growth_179", "Growth Plan", 179.0, 6, "Best value! 5 + 1 Free job posts with extensions"),
        Plan("premium_299", "Premium Plan", 299.0, 12, "Enterprise plan with 10 + 2 Free posts")
    )

    val dateTimeFormatter = remember {
        java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
    }
    val dateFormatter = remember {
        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
    }

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
                    is ImageUploadUtils.UploadResult.Progress -> {
                        // Progress is updated automatically
                    }
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
                title = {
                    Text(
                        text = "Subscriptions",
                        fontFamily = MeeshoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1F2937)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1F2937)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EmployerColors.ScreenBackground)
            )
        },
        bottomBar = {
            selectedPlan?.let { plan ->
                val isCurrentPlan = subState.planId == plan.id && (subState.status == "ACTIVE" || subState.status == "TRIAL")
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showPaymentSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isCurrentPlan) "Renew Plan (₹${plan.price.toInt()})" else "Upgrade to ${plan.name} (₹${plan.price.toInt()})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        containerColor = EmployerColors.ScreenBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Plan Section if active
            val hasActiveSub = subState.status == "ACTIVE" || subState.status == "TRIAL"
            if (hasActiveSub) {
                item {
                    val currentPlanObj = plans.find { it.id == subState.planId }
                    val currentPlanName = currentPlanObj?.name ?: "Trial (Free Posting)"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
                        border = BorderStroke(1.dp, Color(0xFFE9D5FF))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Current Plan Details",
                                    style = AppTypography.cardTitle.copy(fontWeight = FontWeight.Bold, color = Color(0xFF6B21A8)),
                                    fontFamily = MeeshoFontFamily
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFF3E8FF), RoundedCornerShape(99.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = subState.status,
                                        color = Color(0xFF7E22CE),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = currentPlanName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF111827)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                 modifier = Modifier.fillMaxWidth(),
                                 horizontalArrangement = Arrangement.spacedBy(16.dp)
                             ) {
                                 Column(modifier = Modifier.weight(1f)) {
                                     Text("Remaining Credits", fontSize = 11.sp, color = Color(0xFF6B7280))
                                     Text("${subState.normalCredits + subState.instantCredits} posts left (Normal or Urgent)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                                 }
                             }
                            if (subState.expiryDate > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Expires on: ${dateFormatter.format(java.util.Date(subState.expiryDate))}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF7E22CE)
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD97706))
                            Column {
                                Text(
                                    text = "No Active Subscription",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Buy a subscription below to get credits and start posting jobs.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Select a Plan to Start Posting",
                    style = AppTypography.pageTitle.copy(fontWeight = FontWeight.Bold, color = Color(0xFF111827)),
                    fontFamily = MeeshoFontFamily
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose the subscription level that fits your hiring velocity.",
                    style = AppTypography.bodySmall.copy(color = Color(0xFF6B7280))
                )
            }

            // Plan list
            items(plans.size) { index ->
                val plan = plans[index]
                val isSelected = selectedPlan?.id == plan.id
                val isCurrentPlan = subState.planId == plan.id && (subState.status == "ACTIVE" || subState.status == "TRIAL")
                
                val borderStroke = when {
                    isCurrentPlan -> BorderStroke(2.dp, Color(0xFF10B981))
                    isSelected -> BorderStroke(2.dp, Color(0xFF8B5CF6))
                    else -> BorderStroke(1.dp, Color(0xFFE5E7EB))
                }
                val bg = when {
                    isCurrentPlan -> Color(0xFFECFDF5)
                    isSelected -> Color(0xFFFAF5FF)
                    else -> Color.White
                }

                val benefits = when (plan.id) {
                    "starter_119" -> listOf(
                        "3 Standard Vacancy Posts",
                        "30 Days Plan Validity",
                        "View Matching Candidates Profiles",
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

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedPlan = plan
                            viewModel.resetSubmitState()
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = bg),
                    border = borderStroke,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = plan.name,
                                style = AppTypography.cardTitle.copy(fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (isCurrentPlan) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF10B981), RoundedCornerShape(99.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "CURRENT PLAN",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (plan.id == "growth_179") {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF8B5CF6), RoundedCornerShape(99.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "RECOMMENDED",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "₹${plan.price.toInt()} / Month",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF8B5CF6),
                                    fontFamily = MeeshoFontFamily
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = plan.description,
                                    style = AppTypography.bodySmall.copy(color = Color(0xFF6B7280))
                                )
                            }
                            Text(
                                text = "${plan.jobs} posts",
                                style = AppTypography.cardTitle.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF8B5CF6))
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        benefits.forEach { benefit ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF8B5CF6) else Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = benefit,
                                    fontSize = 12.sp,
                                    color = Color(0xFF374151),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "⚠️ Note: Both Instant Gigs and Normal Vacancy postings are counted under this plan's credits.",
                            style = AppTypography.caption.copy(color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            if (paymentRequests.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Recent Transactions",
                        style = AppTypography.pageTitle.copy(fontWeight = FontWeight.Bold, color = Color(0xFF111827)),
                        fontFamily = MeeshoFontFamily
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(paymentRequests.size) { index ->
                    val req = paymentRequests[index]
                    val statusColor = when (req.status) {
                        "VERIFIED" -> Color(0xFF10B981)
                        "REJECTED" -> Color(0xFFEF4444)
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
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = planName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF111827)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(99.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "UTR: ${req.utrNumber}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4B5563),
                                    fontFamily = MeeshoFontFamily
                                )
                                Text(
                                    text = "₹${req.amount.toInt()}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color(0xFF8B5CF6)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Requested: ${dateTimeFormatter.format(java.util.Date(req.requestTimestamp))}",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                            if (req.status == "VERIFIED" && req.expiryTimestamp != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Expiry Date: ${dateFormatter.format(java.util.Date(req.expiryTimestamp))}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                            }
                            if (req.status == "REJECTED" && !req.rejectionReason.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Reason: ${req.rejectionReason}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
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
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Amount Payable: ₹${plan.price.toInt()}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF8B5CF6)
                )

                // Step 1: Copy UPI ID
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Step 1: Copy UPI ID & Pay",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF374151)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "dutypeindia@ybl",
                            fontFamily = MeeshoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF111827)
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString("dutypeindia@ybl"))
                                Toast.makeText(context, "UPI ID copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = Color(0xFF6B7280)
                            )
                        }
                    }
                }

                // Step 2: Show QR Codes if configured
                if (qrs.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "OR Scan Active Payment QR Code:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF374151)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF3F4F6)),
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
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Step 3: Non-Refundable Warning
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Warning",
                            tint = Color(0xFFEF4444)
                        )
                        Text(
                            text = "Money once paid is strictly non-refundable and non-returnable. Please confirm your UTR number before submitting.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF991B1B)
                        )
                    }
                }

                // Step 4: UTR Number input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Step 2: Enter 12-Digit UTR Number",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF374151)
                    )
                    OutlinedTextField(
                        value = utrNumber,
                        onValueChange = { if (it.length <= 12) utrNumber = it },
                        placeholder = { Text("e.g. 630987123456") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Step 5: Upload Screenshot (Optional but recommended)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Step 3: Attach Payment Screenshot (Optional)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF374151)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { screenshotPicker.launch("image/*") }
                            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Camera",
                            tint = Color(0xFF4B5563)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (screenshotUrl.isNotEmpty()) "Screenshot Attached!" else "Upload Screenshot",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Color(0xFF4B5563)
                        )
                    }
                    if (isUploadingScreenshot) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                    }
                }

                // Submit Button
                val isSubmitEnabled = utrNumber.trim().length == 12 && !isUploadingScreenshot
                Button(
                    onClick = {
                        viewModel.submitPayment(plan.id, plan.price, utrNumber, screenshotUrl)
                    },
                    enabled = isSubmitEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    shape = RoundedCornerShape(8.dp)
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
                        color = Color.Red,
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
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Submission Successful!",
                        style = AppTypography.displayTitle.copy(fontWeight = FontWeight.Bold, color = Color(0xFF111827)),
                        textAlign = TextAlign.Center,
                        fontFamily = MeeshoFontFamily
                    )
                    Text(
                        text = "Your UPI reference has been submitted. Admin will verify details with the bank and activate your subscription plan within 2-4 hours.",
                        style = AppTypography.bodySmall.copy(color = Color(0xFF4B5563)),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            viewModel.resetSubmitState()
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        Text("Go to Dashboard", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
