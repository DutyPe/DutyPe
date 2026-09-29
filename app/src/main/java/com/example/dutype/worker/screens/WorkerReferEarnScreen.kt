package com.example.dutype.worker.screens

import com.dutype.app.R
import android.widget.Toast
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.example.dutype.components.CommonHeader
import com.example.dutype.models.*
import com.example.dutype.navigation.Routes
import com.example.dutype.di.rememberInAppReviewTriggerService
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.viewmodels.ReferralViewModel
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.ui.theme.IconSizes
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun WorkerReferEarnScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    val viewModel: ReferralViewModel = hiltViewModel()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val reviewTriggerService = rememberInAppReviewTriggerService()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val referralConfig by viewModel.referralConfig.collectAsStateWithLifecycle()
    val referrerInfo by viewModel.referrerInfo.collectAsStateWithLifecycle()
    
    var isVisible by remember { mutableStateOf(false) }
    var showCopySuccess by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var isCheckingProfileStatus by remember { mutableStateOf(true) }
    var isProfileCompleted by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val playStoreUrl = "https://play.google.com/store/apps/details?id=com.dutype.app"
    val coroutineScope = rememberCoroutineScope()

    // Load data on screen launch
    val screenBg = Color.White
    LaunchedEffect(Unit) {
        onStatusBarColorChange(screenBg)

        // Kick off the referral data fetch IMMEDIATELY so the code/stats are
        // ready by the time the profile-completion gate resolves. Previously
        // we awaited two sequential network checks before even starting the
        // referral request, which made the screen feel slow on every open.
        viewModel.loadReferralData()

        // Run local + remote profile-completion checks in parallel.
        val localProfileDeferred = async {
            runCatching {
                profileCompletionViewModel.isProfileComplete(UserRole.WORKER)
            }.getOrDefault(false)
        }
        val remoteProfileDeferred = async {
            FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                profileCompletionViewModel
                    .isProfileComplete(uid, UserRole.WORKER)
                    .getOrElse { false }
            } ?: false
        }

        isProfileCompleted = localProfileDeferred.await() || remoteProfileDeferred.await()
        isCheckingProfileStatus = false

        delay(100)
        isVisible = true
    }

    LaunchedEffect(uiState.withdrawalSuccess) {
        if (!uiState.withdrawalSuccess) return@LaunchedEffect
        showWithdrawDialog = false
        Toast.makeText(
            context,
            "Withdrawal request of \u20B9${uiState.lastWithdrawalAmount.toInt()} sent. We'll transfer it to your UPI soon.",
            Toast.LENGTH_LONG
        ).show()
        val activity = context as? Activity
        if (activity != null) {
            reviewTriggerService.onReferralWithdrawalSuccess(activity)
        }
        viewModel.clearWithdrawalSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        ReferText(
            text = stringResource(R.string.refer_earn),
            color = Color(0xFF0F0F0F),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 16.dp)
        )
        
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = com.example.dutype.ui.theme.WorkerColors.TextPrimary, strokeWidth = 3.dp)
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = WorkerColors.Error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.error ?: stringResource(R.string.something_went_wrong),
                            color = WorkerColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadReferralData() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (!isProfileCompleted) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = WorkerColors.ChipBackground
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = WorkerColors.Primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.refer_complete_profile_title),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = WorkerColors.TextPrimary
                                            )
                                        )
                                        Text(
                                            text = stringResource(R.string.refer_complete_profile_to_withdraw),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = WorkerColors.TextSecondary
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = { navController.navigate(Routes.WORKER_PROFILE_DETAILS) }
                                    ) {
                                        Text(
                                            stringResource(R.string.refer_complete_profile_button),
                                            color = WorkerColors.Primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Hero, referral code, how-it-works steps and my referrals summary
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(300)) + slideInVertically(tween(300))
                        ) {
                            val topStats = uiState.stats
                            ReferEarnTopSections(
                                referralCode = topStats?.referralCode ?: "",
                                rewardAmount = referralConfig.rewardPerReferral.toInt(),
                                onCopyClick = {
                                    if (uiState.stats?.referralCode.isNullOrBlank()) {
                                        Toast.makeText(context, "Your referral code is being created. Please try again in a moment.", Toast.LENGTH_SHORT).show()
                                        viewModel.loadReferralData()
                                        return@ReferEarnTopSections
                                    }
                                    copyTextToClipboard(
                                        context = context,
                                        label = context.getString(R.string.refer_code_clipboard_label),
                                        text = uiState.stats?.referralCode.orEmpty()
                                    )
                                    showCopySuccess = true
                                },
                                onShareClick = {
                                    val code = uiState.stats?.referralCode ?: ""
                                    if (code.isBlank()) {
                                        Toast.makeText(context, "Your referral code is being created. Please try again in a moment.", Toast.LENGTH_SHORT).show()
                                        viewModel.loadReferralData()
                                        return@ReferEarnTopSections
                                    }
                                    val shareText = context.getString(R.string.refer_worker_share_text, code, playStoreUrl, referralConfig.signupBonus.toInt())

                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.refer_share_chooser_title)))

                                    val activity = context as? Activity
                                    if (activity != null) {
                                        reviewTriggerService.onReferralCodeShared(activity)
                                    }
                                }
                            )
                        }
                    }

                    
                    // Who referred you
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(450, 100)) + slideInVertically(tween(450, 100))
                        ) {
                            ReferrerInfoCard(referrerInfo = referrerInfo)
                        }
                    }

                    // Stats Grid
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(500, 100)) + slideInVertically(tween(500, 100))
                        ) {
                            StatsGrid(
                                totalReferrals = uiState.stats?.totalReferrals ?: 0,
                                successfulReferrals = uiState.stats?.successfulReferrals ?: 0,
                                totalEarnings = uiState.stats?.totalEarnings ?: 0.0,
                                availableBalance = uiState.stats?.availableBalance ?: 0.0,
                                signupBonusReceived = uiState.stats?.signupBonusReceived == true || uiState.stats?.welcomeBonusReceived == true,
                                signupBonusAmount = uiState.stats?.signupBonusAmount
                                    ?.takeIf { it > 0.0 }
                                    ?: (uiState.stats?.welcomeBonusAmount ?: 0.0)
                            )
                        }
                    }
                    
                    // Withdraw Button � always visible once data is loaded
                    item {
                        val balance = uiState.stats?.availableBalance ?: 0.0
                        val minWithdrawal = referralConfig.minWithdrawal
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(550, 150)) + slideInVertically(tween(550, 150))
                        ) {
                            WithdrawCard(
                                availableBalance = balance,
                                minWithdrawal = minWithdrawal,
                                onWithdrawClick = {
                                    if (balance < minWithdrawal) return@WithdrawCard
                                    // Re-check profile completion before opening withdraw dialog
                                    coroutineScope.launch {
                                        val localOk = runCatching {
                                            profileCompletionViewModel.isProfileComplete(UserRole.WORKER)
                                        }.getOrDefault(false)
                                        val remoteOk = isProfileCompleted || (FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                                            profileCompletionViewModel.isProfileComplete(uid, UserRole.WORKER).getOrDefault(false)
                                        } ?: false)

                                        if (!localOk && !remoteOk) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.refer_complete_profile_to_withdraw),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            if (remoteOk && !localOk) {
                                                profileCompletionViewModel.markProfileComplete(UserRole.WORKER)
                                            }
                                            showWithdrawDialog = true
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // Milestone Progress
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(600, 200)) + slideInVertically(tween(600, 200))
                        ) {
                            MilestoneProgressCard(
                                successfulReferrals = uiState.stats?.successfulReferrals ?: 0,
                                nextMilestone = uiState.stats?.let { stats ->
                                    referralConfig.milestones.keys.sorted()
                                        .firstOrNull { it > stats.successfulReferrals } ?: 0
                                } ?: 0,
                                milestoneBonus = uiState.stats?.let { stats ->
                                    val next = referralConfig.milestones.keys.sorted()
                                        .firstOrNull { it > stats.successfulReferrals } ?: 0
                                    referralConfig.milestones[next] ?: 0.0
                                } ?: 0.0
                            )
                        }
                    }

                    // How it works / Rewards & milestones / How to redeem (accordion)
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(700, 300)) + slideInVertically(tween(700, 300))
                        ) {
                            ReferInfoAccordion(referralConfig = referralConfig)
                        }
                    }

                    // Withdrawal History
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(800, 400)) + slideInVertically(tween(800, 400))
                        ) {
                            WithdrawalHistorySection(withdrawals = uiState.withdrawalHistory)
                        }
                    }

 // Referral History
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(900, 500)) + slideInVertically(tween(900, 500))
                        ) {
                            ReferralHistorySection(referralHistory = uiState.referralHistory)
                        }
                    }

                    
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    // Copy success snackbar
    if (showCopySuccess) {
        LaunchedEffect(Unit) {
            delay(2000)
            showCopySuccess = false
        }
        
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = WorkerColors.Primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = WorkerColors.Success,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.code_copied),
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
    
    // Withdraw Dialog
    if (showWithdrawDialog) {
        WithdrawDialog(
            // Show what will actually be requested (capped at the daily maximum).
            availableBalance = minOf(uiState.stats?.availableBalance ?: 0.0, referralConfig.maxWithdrawalPerDay),
            minWithdrawal = referralConfig.minWithdrawal,
            isProcessing = uiState.isProcessingWithdrawal,
            serverError = uiState.withdrawalError,
            onDismiss = {
                if (!uiState.isProcessingWithdrawal) {
                    showWithdrawDialog = false
                    viewModel.clearError()
                }
            },
            onWithdraw = { upiId ->
                // Dialog stays open with a spinner until the server answers.
                viewModel.requestWithdrawal(upiId)
            }
        )
    }
}

private val UPI_ID_PATTERN = Regex("^[a-zA-Z0-9._-]+@[a-zA-Z][a-zA-Z0-9.-]+$")

private fun copyTextToClipboard(context: android.content.Context, label: String, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}


@Composable
private fun ReferText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing,
            lineHeight = TextUnit.Unspecified
        )
    )
}

@SuppressLint("DefaultLocale")
@Composable
private fun ReferEarnTopSections(
    referralCode: String,
    rewardAmount: Int,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val ink = Color(0xFF0F0F0F)
    val green = Color(0xFF10B981)
    val slate = Color(0xFF64748B)
    val mute = Color(0xFF94A3B8)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Hero card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF0FDF4))
                .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CardGiftcard,
                contentDescription = null,
                tint = green,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(10.dp))
            ReferText(
                text = stringResource(R.string.refer_hero_title, rewardAmount),
                color = ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            ReferText(
                text = stringResource(R.string.refer_hero_subtitle),
                color = slate,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(14.dp))

        // Referral code box with dashed border
        val dashShape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(dashShape)
                .drawBehind {
                    val stroke = 2.dp.toPx()
                    val inset = stroke / 2f
                    drawRoundRect(
                        color = green,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius(16.dp.toPx() - inset, 16.dp.toPx() - inset),
                        style = Stroke(
                            width = stroke,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
                                0f
                            )
                        )
                    )
                }
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ReferText(
                text = stringResource(R.string.refer_code_label).uppercase(),
                color = mute,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(6.dp))
            SelectionContainer {
                ReferText(
                    text = referralCode.ifBlank { "Getting your code…" },
                    color = if (referralCode.isBlank()) mute else ink,
                    fontSize = if (referralCode.isBlank()) 16.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(ink)
                        .clickable(onClick = onCopyClick),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    ReferText(
                        text = stringResource(R.string.refer_copy_code),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF25D366))
                        .clickable(onClick = onShareClick),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReferText(
                        text = stringResource(R.string.refer_share_whatsapp),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // 3-step explainer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            ReferStep(Icons.Outlined.Share, ink, stringResource(R.string.refer_step_share), slate)
            ReferArrow(mute)
            ReferStep(Icons.Outlined.PersonAdd, ink, stringResource(R.string.refer_step_joins), slate)
            ReferArrow(mute)
            ReferStep(Icons.Outlined.CurrencyRupee, green, stringResource(R.string.refer_step_each, rewardAmount), slate)
        }

    }
}

@Composable
private fun ReferStep(icon: ImageVector, iconTint: Color, label: String, labelColor: Color) {
    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))
        ReferText(
            text = label,
            color = labelColor,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReferArrow(color: Color) {
    ReferText(
        text = "→",
        color = color,
        fontSize = 16.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, end = 4.dp, bottom = 0.dp)
    )
}

@SuppressLint("DefaultLocale")
@Composable
private fun StatsGrid(
    totalReferrals: Int,
    successfulReferrals: Int,
    totalEarnings: Double,
    availableBalance: Double,
    signupBonusReceived: Boolean,
    signupBonusAmount: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.your_stats),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(stringResource(R.string.total_referrals), style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary))
                    Text(totalReferrals.toString(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.successful), style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary))
                    Text(successfulReferrals.toString(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = WorkerColors.Border)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(stringResource(R.string.total_earned), style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary))
                    Text(stringResource(R.string.rupees_amount, String.format("%.0f", totalEarnings)), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.available), style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary))
                    Text(stringResource(R.string.rupees_amount, String.format("%.0f", availableBalance)), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary))
                }
            }

            if (signupBonusReceived && signupBonusAmount > 0.0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.refer_signup_bonus_earned, String.format("%.0f", signupBonusAmount)),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = WorkerColors.Success
                    )
                )
            }
        }
    }
}


@SuppressLint("DefaultLocale")
@Composable
private fun WithdrawCard(availableBalance: Double, minWithdrawal: Double, onWithdrawClick: () -> Unit) {
    val canWithdraw = availableBalance >= minWithdrawal
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.available_to_withdraw),
                        style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary)
                    )
                    Text(
                        text = "Rs.${String.format("%.0f", availableBalance)}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                        )
                    )
                }
                Button(
                    onClick = onWithdrawClick,
                    enabled = canWithdraw,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WorkerColors.Primary,
                        disabledContainerColor = WorkerColors.TextTertiary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.withdraw_button))
                }
            }
            if (!canWithdraw) {
                Spacer(modifier = Modifier.height(8.dp))
                val remaining = (minWithdrawal - availableBalance).coerceAtLeast(0.0)
                Text(
                    text = stringResource(
                        R.string.refer_earn_more_to_withdraw,
                        String.format(Locale.getDefault(), "%.0f", remaining),
                        String.format(Locale.getDefault(), "%.0f", minWithdrawal)
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextTertiary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MilestoneProgressCard(successfulReferrals: Int, nextMilestone: Int, milestoneBonus: Double) {
    val progress = if (nextMilestone > 0) successfulReferrals.toFloat() / nextMilestone.toFloat() else 0f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = stringResource(R.string.next_milestone),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary)
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                trackColor = WorkerColors.Border
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.refer_milestone_progress, successfulReferrals, nextMilestone),
                    style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary)
                )
                if (milestoneBonus > 0) {
                    Text(
                        text = stringResource(R.string.refer_bonus_amount, milestoneBonus.toInt()),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                        )
                    )
                }
            }
        }
    }
}

/**
 * How it works / Rewards & milestones / How to redeem as one accordion (same look as the
 * Terms screen sections). One section open at a time; "How It Works" starts open.
 */
@Composable
private fun ReferInfoAccordion(referralConfig: com.example.dutype.repositories.ReferralConfig) {
    var expanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }
    val toggle: (Int) -> Unit = { index -> expanded = if (expanded == index) -1 else index }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ReferAccordionCard(stringResource(R.string.how_it_works), expanded == 0, { toggle(0) }) {
            ReferNumberedSteps(
                listOf(
                    stringResource(R.string.refer_worker_step_1),
                    stringResource(R.string.refer_step_2),
                    stringResource(R.string.refer_step_3),
                    stringResource(R.string.refer_worker_step_4)
                )
            )
        }
        ReferAccordionCard(stringResource(R.string.rewards_milestones), expanded == 1, { toggle(1) }) {
            RewardRow(stringResource(R.string.refer_reward_per_referral, referralConfig.rewardPerReferral.toInt()))
            RewardRow(stringResource(R.string.refer_reward_friend_bonus, referralConfig.signupBonus.toInt()))
            referralConfig.milestones.entries.sortedBy { it.key }.forEach { (count, bonus) ->
                RewardRow(stringResource(R.string.refer_milestone_item, count, bonus.toInt()))
            }
        }
        ReferAccordionCard(stringResource(R.string.how_to_redeem), expanded == 2, { toggle(2) }) {
            val minWithdrawal = referralConfig.minWithdrawal.toInt()
            Text(
                text = stringResource(R.string.refer_withdrawal_threshold_info, minWithdrawal),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = WorkerColors.TextPrimary)
            )
            Spacer(Modifier.height(6.dp))
            ReferNumberedSteps(
                listOf(
                    stringResource(R.string.refer_redeem_step_1),
                    stringResource(R.string.refer_redeem_step_2),
                    stringResource(R.string.refer_redeem_step_3),
                    stringResource(R.string.refer_redeem_step_4)
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.refer_min_withdrawal_info, minWithdrawal),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = WorkerColors.TextPrimary)
            )
        }
    }
}

@Composable
private fun ReferAccordionCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White, shape)
            .border(1.dp, Color(0xFFE2E8F0), shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable { onToggle() }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F0F0F),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(18.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ReferNumberedSteps(steps: List<String>) {
    steps.forEachIndexed { index, step ->
        Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
            Text(
                text = "${index + 1}.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = WorkerColors.TextPrimary),
                modifier = Modifier.width(24.dp)
            )
            Text(step, style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary))
        }
    }
}

@Composable
private fun RewardRow(text: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = WorkerColors.TextPrimary
        )
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary))
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun ReferralHistorySection(referralHistory: List<Referral>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.recent_referrals),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary)
            )
            Spacer(Modifier.height(16.dp))
            
            if (referralHistory.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.People, null, tint = WorkerColors.TextTertiary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.no_referrals_yet), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = WorkerColors.TextSecondary))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.share_code_to_earn), style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextTertiary))
                }
            } else {
                referralHistory.forEachIndexed { index, referral ->
                    ReferralHistoryItem(referral)
                    if (index < referralHistory.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = WorkerColors.Border)
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun ReferralHistoryItem(referral: Referral) {
    val context = LocalContext.current
    val dateStr = remember(referral.createdAt) {
        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(referral.createdAt))
    }
    val rewardBreakdown = remember(
        referral.status,
        referral.profileCompleted,
        referral.rewardAmount,
        referral.bonusAmount,
        referral.referredUserReward
    ) {
        if (referral.status != ReferralStatus.COMPLETED) {
            if (referral.status == ReferralStatus.PENDING && !referral.profileCompleted) {
                context.getString(R.string.refer_status_profile_pending)
            } else {
                ""
            }
        } else {
            buildList {
                add(context.getString(R.string.refer_you_earned, String.format("%.0f", referral.rewardAmount)))
                if (referral.bonusAmount > 0) {
                    add(context.getString(R.string.refer_milestone_earned, String.format("%.0f", referral.bonusAmount)))
                }
                if (referral.referredUserReward > 0) {
                    add(context.getString(R.string.refer_friend_bonus_earned, String.format("%.0f", referral.referredUserReward)))
                }
            }.joinToString(" | ")
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            when (referral.status) {
                ReferralStatus.COMPLETED -> Icons.Default.CheckCircle
                ReferralStatus.PENDING -> Icons.AutoMirrored.Filled.TrendingUp
                else -> Icons.Default.Person
            },
            null,
            tint = when (referral.status) {
                ReferralStatus.COMPLETED -> WorkerColors.Success
                ReferralStatus.PENDING -> WorkerColors.Warning
                else -> WorkerColors.TextSecondary
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                referral.referredUserName.ifBlank { context.getString(R.string.refer_default_user_name) },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                )
            )
            Text(dateStr, style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextTertiary))
            if (rewardBreakdown.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = rewardBreakdown,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (referral.status == ReferralStatus.PENDING && !referral.profileCompleted)
                            WorkerColors.Warning
                        else
                            WorkerColors.TextSecondary
                    )
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                when (referral.status) {
                    ReferralStatus.COMPLETED -> "Rs.${String.format("%.0f", referral.getTotalReferrerReward())}"
                    ReferralStatus.PENDING -> context.getString(R.string.refer_status_pending)
                    ReferralStatus.EXPIRED -> context.getString(R.string.refer_status_expired)
                    else -> context.getString(R.string.refer_status_cancelled)
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = when (referral.status) {
                        ReferralStatus.COMPLETED -> WorkerColors.Success
                        ReferralStatus.PENDING -> WorkerColors.Warning
                        else -> com.example.dutype.ui.theme.WorkerColors.TextPrimary
                    }
                )
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun WithdrawalHistorySection(withdrawals: List<WithdrawalRequest>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.withdrawal_history),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary)
            )
            Spacer(Modifier.height(16.dp))

            if (withdrawals.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_withdrawals_yet),
                    style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary)
                )
            } else {
                withdrawals.forEachIndexed { index, withdrawal ->
                    WithdrawalHistoryItem(withdrawal)
                    if (index < withdrawals.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = WorkerColors.Border)
                    }
                }
            }
        }
    }
}

@Composable
private fun WithdrawalHistoryItem(withdrawal: WithdrawalRequest) {
    val dateStr = remember(withdrawal.createdAt) {
        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(withdrawal.createdAt))
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            tint = WorkerColors.TextPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Rs.${String.format("%.0f", withdrawal.amount)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary)
            )
            Text(dateStr, style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextTertiary))
        }
        val statusColor = when (withdrawal.status) {
            WithdrawalStatus.COMPLETED -> Color(0xFF10B981)
            WithdrawalStatus.PROCESSING -> Color(0xFF3B82F6)
            WithdrawalStatus.FAILED, WithdrawalStatus.CANCELLED -> Color(0xFFEF4444)
            WithdrawalStatus.PENDING -> Color(0xFFF59E0B)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = withdrawal.status.name.lowercase().replaceFirstChar { char -> char.titlecase(java.util.Locale.getDefault()) },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = statusColor)
            )
            if (!withdrawal.transactionId.isNullOrBlank()) {
                Text(
                    text = "Ref: ${withdrawal.transactionId}",
                    style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextTertiary)
                )
            }
        }
    }
}

@Composable
private fun ReferrerInfoCard(referrerInfo: ReferrerInfo) {
    if (referrerInfo.referredByCode.isBlank() && referrerInfo.referredByUserId.isBlank()) {
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.who_referred_you),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = referrerInfo.referrerName.ifBlank { stringResource(R.string.referred_by_unknown) },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary)
            )
            if (referrerInfo.referredByCode.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Code: ${referrerInfo.referredByCode}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary)
                )
            }
        }
    }
}

@Composable
private fun WithdrawDialog(
    availableBalance: Double,
    minWithdrawal: Double,
    isProcessing: Boolean,
    serverError: String?,
    onDismiss: () -> Unit,
    onWithdraw: (String) -> Unit
) {
    val context = LocalContext.current
    var upiId by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.withdraw_earnings), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(stringResource(R.string.refer_available_balance, availableBalance.toInt()), style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.Success))
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = upiId,
                    onValueChange = {
                        upiId = it
                        error = null
                    },
                    enabled = !isProcessing,
                    label = { Text(stringResource(R.string.upi_id)) },
                    placeholder = { Text(stringResource(R.string.refer_upi_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.refer_withdraw_full_balance_note),
                    style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextSecondary)
                )
                val shownError = error ?: serverError
                if (shownError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(shownError, color = WorkerColors.Error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = upiId.trim()
                    when {
                        trimmed.isBlank() -> error = context.getString(R.string.refer_error_enter_upi)
                        // Same pattern the server validates, so a bad ID never reaches it.
                        !UPI_ID_PATTERN.matches(trimmed) -> error = context.getString(R.string.refer_error_invalid_upi)
                        availableBalance < minWithdrawal -> error = context.getString(R.string.refer_error_min_withdrawal, minWithdrawal.toInt())
                        else -> onWithdraw(trimmed)
                    }
                },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = WorkerColors.Success)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text(stringResource(R.string.withdraw_button))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) { Text(stringResource(R.string.cancel), color = WorkerColors.TextSecondary) }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

data class WorkerReferralItem(
    val name: String,
    val date: String,
    val status: String,
    val earnings: Double
)


// ============================================
// V2.0 PROFESSIONAL FEATURES
// ============================================

@SuppressLint("DefaultLocale")
@Composable
private fun AnalyticsDashboardCard(analytics: ReferralAnalytics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = WorkerColors.Success,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.performance_analytics),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                    )
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Conversion Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(stringResource(R.string.conversion_rate), style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary))
                    Text(
                        "${String.format("%.1f", analytics.conversionRate)}%",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = WorkerColors.Success
                        )
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.total_clicks), style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary))
                    Text(
                        analytics.totalClicks.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                        )
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = WorkerColors.Border)
            Spacer(Modifier.height(16.dp))
            
            // Rankings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(stringResource(R.string.your_rank), style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary))
                    Text(
                        "#${analytics.rankOverall}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                        )
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.top_percentile), style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary))
                    Text(
                        "Top ${analytics.percentile}%",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = WorkerColors.Warning
                        )
                    )
                }
            }

            if (analytics.projectedMonthlyEarnings > 0) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = WorkerColors.Border)
                Spacer(Modifier.height(16.dp))
                
                // Projected Earnings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.projected_monthly), style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary))
                        Text(
                        "Rs.${analytics.projectedMonthlyEarnings}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = WorkerColors.Success
                            )
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = WorkerColors.Success,
                        modifier = Modifier.size(32.dp)
                    )
                    }
                }
            }
        }
    }

@Composable
private fun SuccessStoriesCard(stories: List<ReferralSuccessStory>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.WarningLight),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = WorkerColors.Warning,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.top_performers),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = WorkerColors.Warning
                    )
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            stories.take(3).forEach { story ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = WorkerColors.Warning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.refer_performer_from_city, story.userName, story.city),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = WorkerColors.Warning
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Rs.${story.totalEarnings.toInt()}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = WorkerColors.Warning
                        )
                    )
                }
            }
            
            if (stories.size > 3) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.refer_more_top_performers, stories.size - 3),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFA16207),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                )
            }
        }
    }
}


@Composable
private fun LegalDisclaimerCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.WarningLight),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = WorkerColors.Warning,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.auto_important_information),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = WorkerColors.Warning
                    )
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.refer_legal_not_pyramid),
                style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.Warning),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = stringResource(R.string.refer_legal_taxable),
                style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.Warning),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = stringResource(R.string.refer_legal_kyc),
                style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.Warning),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = stringResource(R.string.refer_legal_pan),
                style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.Warning),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = stringResource(R.string.refer_legal_fraud),
                style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.Warning),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.terms_rbi_consent),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFA16207),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            )
        }
    }
}
