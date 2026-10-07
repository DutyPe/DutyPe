package com.example.dutype.worker.screens

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.example.dutype.models.formatPaise
import com.example.dutype.firestore.FirestoreSchema.Values
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
import com.example.dutype.components.ReferAndEarnRedemptionCard
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
    val joinBonusPaise = uiState.ledger
        .filter { it.type == Values.LedgerType.SIGNUP_BONUS || it.type == Values.LedgerType.WELCOME_BONUS }
        .sumOf { it.amountPaise }
    val successful = uiState.wallet?.successfulReferrals ?: 0

    var showCopySuccess by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var isCheckingProfileStatus by remember { mutableStateOf(true) }
    var isProfileCompleted by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val playStoreUrl = "https://play.google.com/store/apps/details?id=com.dutype.app"
    val coroutineScope = rememberCoroutineScope()

    // Load data on screen launch
    val screenBg = Color.White.bg()
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
    }

    LaunchedEffect(uiState.withdrawalSuccess) {
        if (!uiState.withdrawalSuccess) return@LaunchedEffect
        showWithdrawDialog = false
        Toast.makeText(
            context,
            context.getString(R.string.refer_withdrawal_sent_toast, uiState.lastWithdrawalPaise / 100),
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
            .background(Color.White.bg())
    ) {
        CommonHeader(
            title = stringResource(R.string.refer_earn),
            navController = navController,
            backgroundColor = Color.White.bg()
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
                                        tint = WorkerColors.Primary.fg(),
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
                                            color = WorkerColors.Primary.fg(),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Hero, referral code, how-it-works steps and my referrals summary
                    item {
                        ReferEarnTopSections(
                            referralCode = uiState.wallet?.referralCode.orEmpty(),
                            rewardAmount = referralConfig.rewardPerReferral.toInt(),
                            onCopyClick = {
                                if (uiState.wallet?.referralCode.isNullOrBlank()) {
                                    Toast.makeText(context, context.getString(R.string.refer_code_creating_toast), Toast.LENGTH_SHORT).show()
                                    viewModel.loadReferralData()
                                    return@ReferEarnTopSections
                                }
                                copyTextToClipboard(
                                    context = context,
                                    label = context.getString(R.string.refer_code_clipboard_label),
                                    text = uiState.wallet?.referralCode.orEmpty()
                                )
                                showCopySuccess = true
                            },
                            onShareClick = {
                                val code = uiState.wallet?.referralCode.orEmpty()
                                if (code.isBlank()) {
                                    Toast.makeText(context, context.getString(R.string.refer_code_creating_toast), Toast.LENGTH_SHORT).show()
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

                    // Who referred you / Referral Code Redemption
                    item {
                        if (uiState.myReferrer != null) {
                            ReferrerInfoCard(referrer = uiState.myReferrer)
                        } else {
                            ReferAndEarnRedemptionCard(
                                hasExistingReferrer = false,
                                onSuccess = { viewModel.loadReferralData() }
                            )
                        }
                    }

                    // Stats Grid
                    item {
                        StatsGrid(
                            totalReferrals = uiState.referralHistory.size,
                            successfulReferrals = uiState.wallet?.successfulReferrals ?: 0,
                            totalEarnings = (uiState.wallet?.lifetimeEarnedPaise ?: 0L) / 100.0,
                            availableBalance = (uiState.wallet?.balancePaise ?: 0L) / 100.0,
                            signupBonusReceived = joinBonusPaise > 0L,
                            signupBonusAmount = joinBonusPaise / 100.0
                        )
                    }

                    // Withdraw Button — always visible once data is loaded
                    item {
                        val balance = (uiState.wallet?.balancePaise ?: 0L) / 100.0
                        val minWithdrawal = referralConfig.minWithdrawal
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


                    // How it works / Rewards & milestones / How to redeem (accordion)
                    item {
                        ReferInfoAccordion(referralConfig = referralConfig)
                    }

                    // Withdrawal History
                    item {
                        WithdrawalHistorySection(withdrawals = uiState.withdrawalHistory)
                    }

                    // Referral History
                    item {
                        ReferralHistorySection(referralHistory = uiState.referralHistory)
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
            availableBalance = minOf((uiState.wallet?.balancePaise ?: 0L) / 100.0, referralConfig.maxWithdrawalPerDay),
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
    val ink = Color(0xFF0F0F0F).fg()
    val green = Color(0xFF10B981).fg()
    val slate = Color(0xFF64748B).fg()
    val mute = Color(0xFF94A3B8).fg()

    Column(modifier = Modifier.fillMaxWidth()) {
        // Hero card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4).bg()),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0).bd()),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCFCE7).bg()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CardGiftcard,
                        contentDescription = null,
                        tint = green,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ReferText(
                        text = stringResource(R.string.refer_hero_title, rewardAmount),
                        color = ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    ReferText(
                        text = stringResource(R.string.refer_hero_subtitle),
                        color = slate,
                        fontSize = 13.sp
                    )
                }
            }
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
                    text = referralCode.ifBlank { stringResource(R.string.refer_getting_code) },
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
                        .background(Color(0xFF25D366).bg())
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
                        text = stringResource(R.string.rupees_amount, String.format(Locale.getDefault(), "%.0f", availableBalance)),
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

/**
 * How it works / How to redeem as one clean accordion.
 * One section open at a time; "How It Works" starts open.
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
                    stringResource(R.string.refer_step_3)
                )
            )
        }
        ReferAccordionCard(stringResource(R.string.how_to_redeem), expanded == 1, { toggle(1) }) {
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
            .background(Color.White.bg(), shape)
            .border(1.dp, Color(0xFFE2E8F0).bd(), shape)
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
                color = Color(0xFF0F0F0F).fg(),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF64748B).fg(),
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
    val rewardBreakdown = when (referral.status) {
        ReferralStatus.PENDING -> context.getString(R.string.refer_status_profile_pending)
        ReferralStatus.COMPLETED -> context.getString(R.string.refer_you_earned, (referral.rewardPaise / 100).toString())
        else -> ""
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
                context.getString(R.string.refer_default_user_name),
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
                        color = if (referral.status == ReferralStatus.PENDING)
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
                    ReferralStatus.COMPLETED -> formatPaise(referral.rewardPaise)
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
                text = formatPaise(withdrawal.amountPaise),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary)
            )
            Text(dateStr, style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextTertiary))
        }
        val statusColor = when (withdrawal.status) {
            WithdrawalStatus.COMPLETED -> Color(0xFF10B981)
            WithdrawalStatus.PROCESSING -> Color(0xFF3B82F6)
            WithdrawalStatus.FAILED -> Color(0xFFEF4444)
            WithdrawalStatus.PENDING -> Color(0xFFF59E0B)
        }
        val statusText = when (withdrawal.status) {
            WithdrawalStatus.COMPLETED -> stringResource(R.string.status_completed)
            WithdrawalStatus.PROCESSING -> stringResource(R.string.status_processing)
            WithdrawalStatus.FAILED -> stringResource(R.string.status_failed)
            WithdrawalStatus.PENDING -> stringResource(R.string.status_pending)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = statusColor)
            )
            if (withdrawal.txnRef.isNotBlank()) {
                Text(
                    text = stringResource(R.string.refer_txn_ref_format, withdrawal.txnRef),
                    style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextTertiary)
                )
            }
        }
    }
}

@Composable
private fun ReferrerInfoCard(referrer: Referral?) {
    if (referrer == null || referrer.code.isBlank()) return

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
                text = stringResource(R.string.referred_by_unknown),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary)
            )
            run {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.refer_code_colon_format, referrer.code),
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

