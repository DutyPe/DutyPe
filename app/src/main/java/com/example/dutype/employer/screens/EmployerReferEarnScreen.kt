package com.example.dutype.employer.screens

import com.example.dutype.ui.theme.fg
import com.example.dutype.models.formatPaise
import com.example.dutype.firestore.FirestoreSchema.Values
import com.dutype.app.R
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.models.*
import com.example.dutype.navigation.Routes
import com.example.dutype.di.rememberInAppReviewTriggerService
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.viewmodels.ReferralViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EmployerReferEarnScreen(
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
    val balanceRupees = (uiState.wallet?.balancePaise ?: 0L) / 100.0

    var isVisible by remember { mutableStateOf(false) }
    var showCopySuccess by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var isCheckingProfileStatus by remember { mutableStateOf(true) }
    var isProfileCompleted by remember { mutableStateOf(false) }
    // Removed: showQRCode state

    val context = LocalContext.current
    val playStoreUrl = "https://play.google.com/store/apps/details?id=com.dutype.app"

    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)

        val localProfileComplete = runCatching {
            profileCompletionViewModel.isProfileComplete(UserRole.EMPLOYER)
        }.getOrDefault(false)

        val remoteProfileComplete = FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            profileCompletionViewModel
                .isProfileComplete(uid, UserRole.EMPLOYER)
                .getOrElse { false }
        } ?: false

        isProfileCompleted = localProfileComplete || remoteProfileComplete
        isCheckingProfileStatus = false

        viewModel.loadReferralData()

        delay(100)
        isVisible = true
    }

    LaunchedEffect(uiState.withdrawalSuccess) {
        if (!uiState.withdrawalSuccess) return@LaunchedEffect
        val activity = context as? Activity
        if (activity != null) {
            reviewTriggerService.onReferralWithdrawalSuccess(activity)
        }
        viewModel.clearWithdrawalSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
    ) {
        CommonHeader(
            title = stringResource(R.string.refer_earn),
            subtitle = stringResource(R.string.invite_employers_earn),
            onBackClick = { navController.popBackStack() },
            backgroundColor = com.example.dutype.ui.theme.EmployerColors.ScreenBackground
            // Removed: QR Code icon button from actions
        )

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = com.example.dutype.ui.theme.EmployerColors.TextPrimary, strokeWidth = 3.dp)
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, null, tint = EmployerColors.Error, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.ExtraLarge))
                        Spacer(Modifier.height(16.dp))
                        Text(uiState.error ?: stringResource(R.string.something_went_wrong), color = EmployerColors.TextSecondary)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadReferralData() }) { Text(stringResource(R.string.retry)) }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!isProfileCompleted) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = EmployerColors.ChipBackground
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
                                        tint = EmployerColors.Primary.fg(),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.refer_complete_profile_title),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = EmployerColors.TextPrimary
                                            )
                                        )
                                        Text(
                                            text = stringResource(R.string.refer_complete_profile_to_withdraw),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = EmployerColors.TextSecondary
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = { navController.navigate(Routes.EMPLOYER_PROFILE) }
                                    ) {
                                        Text(
                                            stringResource(R.string.refer_complete_profile_button),
                                            color = EmployerColors.Primary.fg(),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Tier Badge
                    item {
                        AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(300)) + slideInVertically(tween(300))) {
                            EmployerTierBadgeCard(
                                tier = uiState.wallet?.tier ?: ReferralTier.BRONZE,
                                successfulReferrals = uiState.wallet?.successfulReferrals ?: 0
                            )
                        }
                    }

                    // Removed: QR Code section

                    // Referral Code Card
                    item {
                        AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(400)) + slideInVertically(tween(400))) {
                            EmployerReferralCodeCard(
                                referralCode = uiState.wallet?.referralCode.orEmpty(),
                                onCopyClick = {
                                    copyTextToClipboard(
                                        context = context,
                                        label = context.getString(R.string.refer_code_clipboard_label),
                                        text = uiState.wallet?.referralCode.orEmpty()
                                    )
                                    showCopySuccess = true
                                },
                                onShareClick = {
                                    val code = uiState.wallet?.referralCode.orEmpty()
                                    val shareText = context.getString(
                                        R.string.refer_employer_share_text,
                                        code,
                                        playStoreUrl,
                                        referralConfig.signupBonus.toInt()
                                    )

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
                        AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(450, 100)) + slideInVertically(tween(450, 100))) {
                            EmployerReferrerInfoCard(referrer = uiState.myReferrer)
                        }
                    }

                    // Stats
                    item {
                        AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(500, 100)) + slideInVertically(tween(500, 100))) {
                            EmployerStatsCard(
                                totalReferrals = uiState.referralHistory.size,
                                successfulReferrals = uiState.wallet?.successfulReferrals ?: 0,
                                totalEarnings = (uiState.wallet?.lifetimeEarnedPaise ?: 0L) / 100.0,
                                availableBalance = balanceRupees,
                                signupBonusReceived = joinBonusPaise > 0L,
                                signupBonusAmount = joinBonusPaise / 100.0
                            )
                        }
                    }

                    // Withdraw Button
                    if (balanceRupees >= referralConfig.minWithdrawal) {
                        item {
                            AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(600, 200)) + slideInVertically(tween(600, 200))) {
                                EmployerWithdrawCard(
                                    availableBalance = balanceRupees,
                                    onWithdrawClick = { showWithdrawDialog = true }
                                )
                            }
                        }
                    }

                    // Withdrawal History
                    item {
                        AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(850, 450)) + slideInVertically(tween(850, 450))) {
                            EmployerWithdrawalHistoryCard(withdrawals = uiState.withdrawalHistory)
                        }
                    }

                    // Referral History
                    item {
                        AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(900, 500)) + slideInVertically(tween(900, 500))) {
                            EmployerReferralHistoryCard(referralHistory = uiState.referralHistory)
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    // Copy success dialog
    if (showCopySuccess) {
        LaunchedEffect(Unit) {
            delay(2000)
            showCopySuccess = false
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Card(
                modifier = Modifier.padding(16.dp).padding(bottom = 32.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = EmployerColors.Primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = EmployerColors.Success, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.code_copied), color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    // Withdraw Dialog
    if (showWithdrawDialog) {
        EmployerWithdrawDialog(
            availableBalance = minOf(balanceRupees, referralConfig.maxWithdrawalPerDay),
            minWithdrawal = referralConfig.minWithdrawal,
            onDismiss = { showWithdrawDialog = false },
            onWithdraw = { upiId ->
                viewModel.requestWithdrawal(upiId)
                showWithdrawDialog = false
            }
        )
    }
}


@Composable
private fun EmployerTierBadgeCard(tier: ReferralTier, successfulReferrals: Int) {
    val tierName = when (tier) {
        ReferralTier.BRONZE -> stringResource(R.string.tier_bronze)
        ReferralTier.SILVER -> stringResource(R.string.tier_silver)
        ReferralTier.GOLD -> stringResource(R.string.tier_gold)
        ReferralTier.PLATINUM -> stringResource(R.string.tier_platinum)
        ReferralTier.DIAMOND -> stringResource(R.string.tier_diamond)
    }
    val tierColor = when (tier) {
        ReferralTier.BRONZE -> Color(0xFFCD7F32)
        ReferralTier.SILVER -> Color(0xFF94A3B8)
        ReferralTier.GOLD -> Color(0xFFF59E0B)
        ReferralTier.PLATINUM -> Color(0xFF6366F1)
        ReferralTier.DIAMOND -> Color(0xFF8B5CF6)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmployerColors.Border)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp)
                    .background(tierColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = tierColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tierName, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = tierColor))
                Text(stringResource(R.string.refer_successful_referrals_count, successfulReferrals), style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary))
            }
        }
    }
}

// Removed: EmployerQRCodeCard function - QR code feature removed

@Composable
private fun EmployerReferralCodeCard(referralCode: String, onCopyClick: () -> Unit, onShareClick: () -> Unit) {
    var showLinkCopied by remember { mutableStateOf(false) }

    LaunchedEffect(showLinkCopied) {
        if (showLinkCopied) {
            delay(2000)
            showLinkCopied = false
        }
    }

    Surface(modifier = Modifier.fillMaxWidth().border(1.dp, EmployerColors.Border, RoundedCornerShape(16.dp)), color = com.example.dutype.ui.theme.EmployerColors.CardBackground, shape = RoundedCornerShape(16.dp), shadowElevation = 0.dp) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(48.dp).background(EmployerColors.WarningLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = EmployerColors.Warning,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(stringResource(R.string.your_referral_code), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = EmployerColors.TextSecondary))
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier.background(EmployerColors.ChipBackground, RoundedCornerShape(12.dp)).padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                SelectionContainer {
                    Text(referralCode, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary, letterSpacing = 3.sp, fontSize = 28.sp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.share_code_friend_bonus), style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary), textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCopyClick, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = EmployerColors.TextSecondary)) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.copy_button), fontWeight = FontWeight.SemiBold)
                }
                Button(onClick = onShareClick, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Primary)) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.share_button), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun copyTextToClipboard(context: android.content.Context, label: String, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

@SuppressLint("DefaultLocale")
@Composable
private fun EmployerStatsCard(
    totalReferrals: Int,
    successfulReferrals: Int,
    totalEarnings: Double,
    availableBalance: Double,
    signupBonusReceived: Boolean,
    signupBonusAmount: Double
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = com.example.dutype.ui.theme.EmployerColors.CardBackground, shape = RoundedCornerShape(16.dp), shadowElevation = 0.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.your_stats), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.total_referrals), style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary))
                    Text(totalReferrals.toString(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.successful), style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary))
                    Text(successfulReferrals.toString(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = EmployerColors.Border)
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.total_earned), style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary))
                    Text(stringResource(R.string.rupees_amount, String.format("%.0f", totalEarnings)), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.available), style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary))
                    Text(stringResource(R.string.rupees_amount, String.format("%.0f", availableBalance)), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
                }
            }

            if (signupBonusReceived && signupBonusAmount > 0.0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.refer_signup_bonus_earned, String.format("%.0f", signupBonusAmount)),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = EmployerColors.Success)
                )
            }
        }
    }
}

@Composable
private fun EmployerStatItem(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = com.example.dutype.ui.theme.EmployerColors.CardBackground, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun EmployerWithdrawCard(availableBalance: Double, onWithdrawClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = com.example.dutype.ui.theme.EmployerColors.CardBackground, shape = RoundedCornerShape(16.dp), shadowElevation = 0.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.available_to_withdraw), style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary))
                Text(stringResource(R.string.rupees_amount, String.format("%.0f", availableBalance)), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
            }
            Button(onClick = onWithdrawClick, colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Primary), shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.withdraw_button))
            }
        }
    }
}

@Composable
private fun EmployerHowItWorksCard() {
    Surface(modifier = Modifier.fillMaxWidth(), color = com.example.dutype.ui.theme.EmployerColors.CardBackground, shape = RoundedCornerShape(16.dp), shadowElevation = 0.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.how_it_works), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
            Spacer(Modifier.height(16.dp))
            val steps = listOf(
                stringResource(R.string.refer_employer_step_1),
                stringResource(R.string.refer_step_2),
                stringResource(R.string.refer_step_3),
                stringResource(R.string.refer_employer_step_4)
            )
            steps.forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                        ),
                        modifier = Modifier.width(24.dp)
                    )
                    Text(step, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF4B5563).fg()), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun EmployerReferralHistoryCard(referralHistory: List<Referral>) {
    Surface(modifier = Modifier.fillMaxWidth(), color = com.example.dutype.ui.theme.EmployerColors.CardBackground, shape = RoundedCornerShape(16.dp), shadowElevation = 0.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.recent_referrals), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
            Spacer(Modifier.height(16.dp))
            if (referralHistory.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                    Box(modifier = Modifier.size(64.dp).background(EmployerColors.ChipBackground, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.People, null, tint = EmployerColors.TextTertiary, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Large))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.no_referrals_yet), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = EmployerColors.TextSecondary))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.share_code_employers), style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary), textAlign = TextAlign.Center)
                }
            } else {
                referralHistory.forEachIndexed { index, referral ->
                    EmployerReferralHistoryItem(referral)
                    if (index < referralHistory.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = EmployerColors.ChipBackground)
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun EmployerReferralHistoryItem(referral: Referral) {
    val context = LocalContext.current
    val dateStr = remember(referral.createdAt) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(referral.createdAt))
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
                else -> Icons.Default.People
            },
            null,
            tint = when (referral.status) {
                ReferralStatus.COMPLETED -> EmployerColors.Success
                ReferralStatus.PENDING -> EmployerColors.Warning
                else -> EmployerColors.TextSecondary
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.refer_default_user_name), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
            Text(dateStr, style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary))
            if (rewardBreakdown.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = rewardBreakdown,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (referral.status == ReferralStatus.PENDING)
                            EmployerColors.Warning
                        else
                            EmployerColors.TextSecondary
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
                        ReferralStatus.COMPLETED -> EmployerColors.Success
                        ReferralStatus.PENDING -> EmployerColors.Warning
                        else -> com.example.dutype.ui.theme.EmployerColors.TextPrimary
                    }
                )
            )
        }
    }
}

@Composable
private fun EmployerWithdrawalHistoryCard(withdrawals: List<WithdrawalRequest>) {
    Surface(modifier = Modifier.fillMaxWidth(), color = com.example.dutype.ui.theme.EmployerColors.CardBackground, shape = RoundedCornerShape(16.dp), shadowElevation = 0.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.withdrawal_history), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
            Spacer(Modifier.height(16.dp))
            if (withdrawals.isEmpty()) {
                Text(stringResource(R.string.no_withdrawals_yet), style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary))
            } else {
                withdrawals.forEachIndexed { index, withdrawal ->
                    EmployerWithdrawalHistoryItem(withdrawal)
                    if (index < withdrawals.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = EmployerColors.ChipBackground)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployerWithdrawalHistoryItem(withdrawal: WithdrawalRequest) {
    val dateStr = remember(withdrawal.createdAt) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(withdrawal.createdAt))
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AccountBalanceWallet, null, tint = EmployerColors.TextPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(formatPaise(withdrawal.amountPaise), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
            Text(dateStr, style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary))
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
                    style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary)
                )
            }
        }
    }
}

@Composable
private fun EmployerReferrerInfoCard(referrer: Referral?) {
    if (referrer == null || referrer.code.isBlank()) return

    Surface(modifier = Modifier.fillMaxWidth(), color = com.example.dutype.ui.theme.EmployerColors.CardBackground, shape = RoundedCornerShape(16.dp), shadowElevation = 0.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.who_referred_you), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.referred_by_unknown), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
            run {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.referral_code_format, referrer.code), style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary))
            }
        }
    }
}

@Composable
private fun EmployerWithdrawDialog(availableBalance: Double, minWithdrawal: Double, onDismiss: () -> Unit, onWithdraw: (String) -> Unit) {
    val context = LocalContext.current
    var upiId by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.withdraw_earnings), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(stringResource(R.string.refer_available_balance, availableBalance.toInt()), style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.Success))
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = upiId, onValueChange = { upiId = it }, label = { Text(stringResource(R.string.upi_id)) }, placeholder = { Text(stringResource(R.string.refer_upi_placeholder)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.refer_withdraw_full_balance_note), style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary))
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = EmployerColors.Error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        upiId.isBlank() -> error = context.getString(R.string.refer_error_enter_upi)
                        !upiId.contains("@") -> error = context.getString(R.string.refer_error_invalid_upi)
                        availableBalance < minWithdrawal -> error = context.getString(R.string.refer_error_min_withdrawal, minWithdrawal.toInt())
                        else -> onWithdraw(upiId)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Success)
            ) { Text(stringResource(R.string.withdraw_button)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = EmployerColors.TextSecondary) } },
        shape = RoundedCornerShape(16.dp)
    )
}

data class EmployerReferralItem(
    val name: String,
    val date: String,
    val status: String,
    val earnings: Double
)


// ============================================
// V2.0 PROFESSIONAL FEATURES
// ============================================

