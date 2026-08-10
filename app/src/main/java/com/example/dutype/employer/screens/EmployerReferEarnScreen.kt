package com.example.dutype.employer.screens

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
    val referrerInfo by viewModel.referrerInfo.collectAsStateWithLifecycle()
    
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

        if (isProfileCompleted) {
            viewModel.loadReferralData()
        }

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
            isCheckingProfileStatus || (isProfileCompleted && uiState.isLoading) -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = com.example.dutype.ui.theme.EmployerColors.TextPrimary, strokeWidth = 3.dp)
                }
            }
            isProfileCompleted && uiState.error != null -> {
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
            !isProfileCompleted -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Icon
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(
                                        EmployerColors.ChipBackground,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Business,
                                    contentDescription = null,
                                    tint = EmployerColors.TextSecondary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Title
                            Text(
                                text = stringResource(R.string.refer_complete_profile_title),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                                ),
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Description
                            Text(
                                text = stringResource(R.string.refer_employer_complete_profile_desc),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = EmployerColors.TextSecondary,
                                    lineHeight = 24.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Complete Profile Button
                            Button(
                                onClick = { navController.navigate(Routes.EMPLOYER_PROFILE) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmployerColors.Primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.refer_complete_profile_button),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tier Badge
                    item {
                        AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(300)) + slideInVertically(tween(300))) {
                            EmployerTierBadgeCard(
                                tier = uiState.stats?.currentTier ?: ReferralTier.BRONZE,
                                successfulReferrals = uiState.stats?.successfulReferrals ?: 0
                            )
                        }
                    }
                    
                    // Removed: QR Code section
                    
                    // Referral Code Card
                    item {
                        AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(400)) + slideInVertically(tween(400))) {
                            EmployerReferralCodeCard(
                                referralCode = uiState.stats?.referralCode ?: "",
                                onCopyClick = {
                                    copyTextToClipboard(
                                        context = context,
                                        label = context.getString(R.string.refer_code_clipboard_label),
                                        text = uiState.stats?.referralCode.orEmpty()
                                    )
                                    showCopySuccess = true
                                },
                                onShareClick = {
                                    val code = uiState.stats?.referralCode ?: ""
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
                            EmployerReferrerInfoCard(referrerInfo = referrerInfo)
                        }
                    }

                    // Stats
                    item {
                        AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(500, 100)) + slideInVertically(tween(500, 100))) {
                            EmployerStatsCard(
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
                    
                    // Withdraw Button
                    if ((uiState.stats?.availableBalance ?: 0.0) >= referralConfig.minWithdrawal) {
                        item {
                            AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(600, 200)) + slideInVertically(tween(600, 200))) {
                                EmployerWithdrawCard(
                                    availableBalance = uiState.stats?.availableBalance ?: 0.0,
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
            availableBalance = uiState.stats?.availableBalance ?: 0.0,
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
    val tierName = ReferralRewards.getTierDisplayName(tier)
    val tierColor = when (tier) {
        ReferralTier.BRONZE -> Color(0xFFCD7F32)
        ReferralTier.SILVER -> Color(0xFF94A3B8)
        ReferralTier.GOLD -> Color(0xFFF59E0B)
        ReferralTier.PLATINUM -> Color(0xFF6366F1)
        ReferralTier.DIAMOND -> Color(0xFF8B5CF6)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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
    
    Surface(modifier = Modifier.fillMaxWidth(), color = com.example.dutype.ui.theme.EmployerColors.CardBackground, shape = RoundedCornerShape(20.dp), shadowElevation = 0.dp) {
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
private fun EmployerMilestoneProgressCard(successfulReferrals: Int, nextMilestone: Int) {
    val progress = if (nextMilestone > 0) successfulReferrals.toFloat() / nextMilestone.toFloat() else 0f
    val bonus = ReferralRewards.getMilestoneBonus(nextMilestone)
    val freePostings = ReferralRewards.getEmployerFreePostings(nextMilestone)
    
    Surface(modifier = Modifier.fillMaxWidth(), color = com.example.dutype.ui.theme.EmployerColors.CardBackground, shape = RoundedCornerShape(16.dp), shadowElevation = 0.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(stringResource(R.string.next_milestone), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = com.example.dutype.ui.theme.EmployerColors.TextPrimary,
                trackColor = EmployerColors.Border
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.refer_milestone_progress, successfulReferrals, nextMilestone), style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF4B5563)))
                Column(horizontalAlignment = Alignment.End) {
                    if (bonus > 0) {
                        Text(stringResource(R.string.refer_bonus_amount, bonus.toInt()), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
                    }
                    if (freePostings > 0) {
                        Text(stringResource(R.string.refer_free_posts_count, freePostings), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
                    }
                }
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
                    Text(step, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF4B5563)), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EmployerRewardsCard() {
    Surface(modifier = Modifier.fillMaxWidth(), color = com.example.dutype.ui.theme.EmployerColors.CardBackground, shape = RoundedCornerShape(16.dp), shadowElevation = 0.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.rewards_milestones), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
            Spacer(Modifier.height(16.dp))
            val rewards = listOf(
                stringResource(R.string.refer_reward_per_referral),
                stringResource(R.string.refer_reward_friend_bonus),
                stringResource(R.string.refer_reward_5_employer),
                stringResource(R.string.refer_reward_10_employer),
                stringResource(R.string.refer_reward_15),
                stringResource(R.string.refer_reward_25_employer),
                stringResource(R.string.refer_reward_50),
                stringResource(R.string.refer_reward_100)
            )
            rewards.forEach { text ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = EmployerColors.TextPrimary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(text, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF4B5563)))
                }
            }
        }
    }
}

@Composable
private fun EmployerRedemptionInstructionsCard() {
    Surface(modifier = Modifier.fillMaxWidth(), color = com.example.dutype.ui.theme.EmployerColors.CardBackground, shape = RoundedCornerShape(16.dp), shadowElevation = 0.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.how_to_redeem), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.refer_withdrawal_threshold_info),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary)
            )
            Spacer(Modifier.height(12.dp))

            val steps = listOf(
                stringResource(R.string.refer_redeem_step_1),
                stringResource(R.string.refer_redeem_step_2),
                stringResource(R.string.refer_redeem_step_3),
                stringResource(R.string.refer_redeem_step_4)
            )

            steps.forEachIndexed { index, step ->
                Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                        ),
                        modifier = Modifier.width(24.dp)
                    )
                    Text(step, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF4B5563)))
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.refer_min_withdrawal_info),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                )
            )
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
    val rewardBreakdown = remember(
        referral.status,
        referral.rewardAmount,
        referral.bonusAmount,
        referral.referredUserReward
    ) {
        if (referral.status != ReferralStatus.COMPLETED) {
            ""
        } else {
            buildList {
                add(context.getString(R.string.refer_you_earned, String.format("%.0f", referral.rewardAmount)))
                if (referral.bonusAmount > 0) {
                    add(context.getString(R.string.refer_milestone_earned, String.format("%.0f", referral.bonusAmount)))
                }
                if (referral.referredUserReward > 0) {
                    add(context.getString(R.string.refer_signup_bonus_earned, String.format("%.0f", referral.referredUserReward)))
                }
            }.joinToString(" | ")
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            when (referral.status) {
                ReferralStatus.COMPLETED -> Icons.Default.CheckCircle
                ReferralStatus.PENDING -> Icons.AutoMirrored.Filled.TrendingUp
                else -> Icons.Default.People
            },
            null,
            tint = EmployerColors.TextPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(referral.referredUserName.ifBlank { stringResource(R.string.employer_label) }, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
            Text(dateStr, style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary))
            if (rewardBreakdown.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = rewardBreakdown,
                    style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary)
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
                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
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
            Text(stringResource(R.string.rupees_amount, String.format("%.0f", withdrawal.amount)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
            Text(dateStr, style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary))
        }
        Text(withdrawal.status.name.lowercase().replaceFirstChar { char -> char.titlecase(Locale.getDefault()) }, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
    }
}

@Composable
private fun EmployerReferrerInfoCard(referrerInfo: ReferrerInfo) {
    if (referrerInfo.referredByCode.isBlank() && referrerInfo.referredByUserId.isBlank()) return

    Surface(modifier = Modifier.fillMaxWidth(), color = com.example.dutype.ui.theme.EmployerColors.CardBackground, shape = RoundedCornerShape(16.dp), shadowElevation = 0.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.who_referred_you), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
            Spacer(Modifier.height(8.dp))
            Text(referrerInfo.referrerName.ifBlank { stringResource(R.string.referred_by_unknown) }, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
            if (referrerInfo.referredByCode.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.referral_code_format, referrerInfo.referredByCode), style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary))
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

@SuppressLint("DefaultLocale")
@Composable
private fun EmployerAnalyticsDashboardCard(analytics: ReferralAnalytics) {
    Surface(modifier = Modifier.fillMaxWidth(), color = com.example.dutype.ui.theme.EmployerColors.CardBackground, shape = RoundedCornerShape(16.dp), shadowElevation = 0.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = EmployerColors.Success, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.performance_analytics), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Conversion Rate
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.conversion_rate), style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary))
                    Text("${String.format("%.1f", analytics.conversionRate)}%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = EmployerColors.Success))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.total_clicks), style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary))
                    Text(analytics.totalClicks.toString(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
                }
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = EmployerColors.Border)
            Spacer(Modifier.height(16.dp))
            
            // Rankings
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.your_rank), style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary))
                    Text("#${analytics.rankOverall}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.EmployerColors.TextPrimary))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.top_percentile), style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary))
                    Text(stringResource(R.string.top_percent_format, analytics.percentile), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = EmployerColors.Warning))
                }
            }

            if (analytics.projectedMonthlyEarnings > 0) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = EmployerColors.Border)
                Spacer(Modifier.height(16.dp))
                
                // Projected Earnings
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(stringResource(R.string.projected_monthly), style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary))
                        Text(stringResource(R.string.rupees_amount, analytics.projectedMonthlyEarnings.toString()), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = EmployerColors.Success))
                    }
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = EmployerColors.Success, modifier = Modifier.size(32.dp))
                }
            }
            
        }
    }
}

@Composable
private fun EmployerSuccessStoriesCard(stories: List<ReferralSuccessStory>) {
    Surface(modifier = Modifier.fillMaxWidth(), color = EmployerColors.WarningLight, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = EmployerColors.Warning, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.top_performers), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmployerColors.Warning))
            }
            
            Spacer(Modifier.height(12.dp))
            
            stories.take(3).forEach { story ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = EmployerColors.Warning, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.refer_performer_from_city, story.userName, story.city), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = EmployerColors.Warning), modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.rupees_amount, story.totalEarnings.toInt().toString()), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = EmployerColors.Warning))
                }
            }
            
            if (stories.size > 3) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.refer_more_top_performers, stories.size - 3), style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA16207), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
            }
        }
    }
}


@Composable
private fun EmployerLegalDisclaimerCard() {
    Surface(modifier = Modifier.fillMaxWidth(), color = EmployerColors.WarningLight, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = EmployerColors.Warning, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.important_information), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EmployerColors.Warning))
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text(stringResource(R.string.refer_legal_not_pyramid), style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.Warning), modifier = Modifier.padding(vertical = 4.dp))
            Text(stringResource(R.string.refer_legal_taxable), style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.Warning), modifier = Modifier.padding(vertical = 4.dp))
            Text(stringResource(R.string.refer_legal_kyc), style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.Warning), modifier = Modifier.padding(vertical = 4.dp))
            Text(stringResource(R.string.refer_legal_pan), style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.Warning), modifier = Modifier.padding(vertical = 4.dp))
            Text(stringResource(R.string.refer_legal_fraud), style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.Warning), modifier = Modifier.padding(vertical = 4.dp))
            
            Spacer(Modifier.height(8.dp))
            
            Text(stringResource(R.string.terms_rbi_consent), style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA16207), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
        }
    }
}
