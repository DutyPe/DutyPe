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
import kotlinx.coroutines.launch
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
import com.example.dutype.models.*
import com.example.dutype.navigation.Routes
import com.example.dutype.di.rememberInAppReviewTriggerService
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.viewmodels.ReferralViewModel
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.ui.theme.IconSizes
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

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
    val screenBg = com.example.dutype.ui.theme.WorkerColors.ScreenBackground
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
            onBackClick = { navController.popBackStack() },
            backgroundColor = WorkerColors.CardBackground
        )
        
        when {
            isCheckingProfileStatus || (isProfileCompleted && uiState.isLoading) -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = com.example.dutype.ui.theme.WorkerColors.TextPrimary, strokeWidth = 3.dp)
                }
            }
            isProfileCompleted && uiState.error != null -> {
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
                            containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground
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
                                        WorkerColors.ChipBackground,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = WorkerColors.TextSecondary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Title
                            Text(
                                text = stringResource(R.string.refer_complete_profile_title),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                                ),
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Description
                            Text(
                                text = stringResource(R.string.refer_worker_complete_profile_desc),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = WorkerColors.TextSecondary,
                                    lineHeight = 24.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Complete Profile Button
                            Button(
                                onClick = { navController.navigate(Routes.WORKER_PROFILE_DETAILS) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WorkerColors.Primary
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
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Tier Badge
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(300)) + slideInVertically(tween(300))
                        ) {
                            TierBadgeCard(
                                tier = uiState.stats?.currentTier ?: ReferralTier.BRONZE,
                                successfulReferrals = uiState.stats?.successfulReferrals ?: 0
                            )
                        }
                    }
                    
                    // QR Code Section
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(400)) + slideInVertically(tween(400))
                        ) {
                            ReferralCodeSection(
                                referralCode = uiState.stats?.referralCode ?: "",
                                signupBonus = referralConfig.signupBonus,
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
                    
                    // Withdraw Button ï¿½ always visible once data is loaded
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
                                        val profileOk = runCatching {
                                            profileCompletionViewModel.isProfileComplete(UserRole.WORKER)
                                        }.getOrDefault(false)
                                        if (!profileOk) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.refer_complete_profile_to_withdraw),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
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

                    // Rewards & Milestones (config-driven)
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(700, 300)) + slideInVertically(tween(700, 300))
                        ) {
                            ConfigDrivenRewardsSection(referralConfig = referralConfig)
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

                    // How It Works
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(950, 550)) + slideInVertically(tween(950, 550))
                        ) {
                            HowItWorksSection()
                        }
                    }

                    // How to Redeem (config-driven threshold)
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(1000, 600)) + slideInVertically(tween(1000, 600))
                        ) {
                            RedemptionInstructionsSection(minWithdrawal = referralConfig.minWithdrawal)
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

private fun copyTextToClipboard(context: android.content.Context, label: String, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}


@Composable
private fun TierBadgeCard(tier: ReferralTier, successfulReferrals: Int) {
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
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, WorkerColors.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tier emoji in glowing circle
            Box(
                modifier = Modifier
                    .size(56.dp)
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
                Text(
                    text = tierName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = tierColor
                    )
                )
                Text(
                    text = stringResource(R.string.refer_successful_referrals_count, successfulReferrals),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = WorkerColors.TextSecondary
                    )
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = WorkerColors.Success,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ReferralCodeSection(
    referralCode: String,
    signupBonus: Double,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gift icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFF3F4F6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.your_referral_code),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Code in a dashed box
            Box(
                modifier = Modifier
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = referralCode,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            letterSpacing = 3.sp,
                            fontSize = 28.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.refer_share_friend_bonus, signupBonus.toInt()),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Black
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WorkerColors.TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WorkerColors.Border)
                ) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.copy_button), fontWeight = FontWeight.SemiBold)
                }
                
                Button(
                    onClick = onShareClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WorkerColors.Primary)
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.share_button), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
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
        modifier = Modifier.fillMaxWidth(),
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
        modifier = Modifier.fillMaxWidth(),
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
                    text = "Earn ?${String.format("%.0f", remaining)} more to unlock withdrawal (min ?${String.format("%.0f", minWithdrawal)})",
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
        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun HowItWorksSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.how_it_works),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary)
            )
            Spacer(Modifier.height(16.dp))

            val steps = listOf(
                stringResource(R.string.refer_worker_step_1),
                stringResource(R.string.refer_step_2),
                stringResource(R.string.refer_step_3),
                stringResource(R.string.refer_worker_step_4)
            )

            steps.forEachIndexed { index, step ->
                Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                        ),
                        modifier = Modifier.width(24.dp)
                    )
                    Text(step, style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary))
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun ConfigDrivenRewardsSection(referralConfig: com.example.dutype.repositories.ReferralConfig) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.rewards_milestones),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary)
            )
            Spacer(Modifier.height(16.dp))

            // Dynamic per-referral reward
            RewardRow(stringResource(R.string.refer_reward_per_referral, referralConfig.rewardPerReferral.toInt()))
            // Dynamic signup bonus
            RewardRow(stringResource(R.string.refer_reward_friend_bonus, referralConfig.signupBonus.toInt()))

            // Dynamic milestones from config
            referralConfig.milestones.entries.sortedBy { it.key }.forEach { (count, bonus) ->
                RewardRow(stringResource(R.string.refer_milestone_item, count, bonus.toInt()))
            }
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

@Composable
private fun RewardsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.rewards_milestones),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary)
            )
            Spacer(Modifier.height(16.dp))

            val rewards = listOf(
                stringResource(R.string.refer_reward_per_referral),
                stringResource(R.string.refer_reward_friend_bonus),
                stringResource(R.string.refer_reward_5_worker),
                stringResource(R.string.refer_reward_10_worker),
                stringResource(R.string.refer_reward_15),
                stringResource(R.string.refer_reward_25_worker),
                stringResource(R.string.refer_reward_50),
                stringResource(R.string.refer_reward_100)
            )

            rewards.forEach { text ->
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
        }
    }
}

@Composable
private fun RedemptionInstructionsSection(minWithdrawal: Double = 100.0) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.how_to_redeem),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary)
            )
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.refer_withdrawal_threshold_info, minWithdrawal.toInt()),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary)
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
                            color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                        ),
                        modifier = Modifier.width(24.dp)
                    )
                    Text(step, style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary))
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.refer_min_withdrawal_info, minWithdrawal.toInt()),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                )
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun ReferralHistorySection(referralHistory: List<Referral>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            tint = WorkerColors.TextPrimary,
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
                    style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextSecondary)
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
                    color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                )
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun WithdrawalHistorySection(withdrawals: List<WithdrawalRequest>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
        Text(
            text = withdrawal.status.name.lowercase().replaceFirstChar { char -> char.titlecase(java.util.Locale.getDefault()) },
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary)
        )
    }
}

@Composable
private fun ReferrerInfoCard(referrerInfo: ReferrerInfo) {
    if (referrerInfo.referredByCode.isBlank() && referrerInfo.referredByUserId.isBlank()) {
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    onValueChange = { upiId = it },
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
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = WorkerColors.Error, style = MaterialTheme.typography.bodySmall)
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
                colors = ButtonDefaults.buttonColors(containerColor = WorkerColors.Success)
            ) { Text(stringResource(R.string.withdraw_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = WorkerColors.TextSecondary) }
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
        modifier = Modifier.fillMaxWidth(),
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
        modifier = Modifier.fillMaxWidth(),
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
        modifier = Modifier.fillMaxWidth(),
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
