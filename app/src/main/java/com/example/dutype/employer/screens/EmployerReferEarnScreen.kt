package com.example.dutype.employer.screens

import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.components.CommonHeader
import com.example.dutype.models.*
import com.example.dutype.viewmodels.InAppReviewTriggerServiceHolder
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
    val reviewTriggerServiceHolder: InAppReviewTriggerServiceHolder = hiltViewModel()
    val reviewTriggerService = reviewTriggerServiceHolder.service
    val uiState by viewModel.uiState.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    val successStories by viewModel.successStories.collectAsState()
    
    var isVisible by remember { mutableStateOf(false) }
    var showCopySuccess by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    // Removed: showQRCode state
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val playStoreUrl = "https://play.google.com/store/apps/details?id=com.dutype.app"

    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        viewModel.loadReferralData()
        viewModel.loadAnalytics()
        viewModel.loadSuccessStories()
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
            .background(Color.White)
    ) {
        CommonHeader(
            title = stringResource(R.string.refer_earn),
            subtitle = "Invite employers & earn rewards",
            onBackClick = { navController.popBackStack() },
            backgroundColor = Color.White
            // Removed: QR Code icon button from actions
        )
        
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF1F2937), strokeWidth = 3.dp)
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, null, tint = Color(0xFFEF4444), modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.ExtraLarge))
                        Spacer(Modifier.height(16.dp))
                        Text(uiState.error ?: "Something went wrong", color = Color(0xFF6B7280))
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadReferralData() }) { Text("Retry") }
                    }
                }
            }
            // Check if referral code exists - if not, show "Complete Profile" message
            uiState.stats?.referralCode.isNullOrEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                                        Color(0xFFF3F4F6),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Business,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Title
                            Text(
                                text = "Complete Your Profile",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F2937)
                                ),
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Description
                            Text(
                                text = "To generate your personalized referral code and start earning rewards, please complete your company profile first.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color(0xFF6B7280),
                                    lineHeight = 24.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Complete Profile Button
                            Button(
                                onClick = { navController.navigate("employer_profile") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1F2937)
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
                                    "Complete Profile",
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
                                    clipboardManager.setText(AnnotatedString(uiState.stats?.referralCode ?: ""))
                                    showCopySuccess = true
                                },
                                onShareClick = {
                                    val code = uiState.stats?.referralCode ?: ""
                                    val shareText = """
Join DutyPe for hiring!

Use my referral code: $code

Download DutyPe: $playStoreUrl

Find reliable workers for your business and earn Rs.25 bonus!
                                    """.trimIndent()
                                    
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Referral Code"))

                                    val activity = context as? Activity
                                    if (activity != null) {
                                        reviewTriggerService.onReferralCodeShared(activity)
                                    }
                                }
                            )
                        }
                    }

                    // Stats
                    item {
                        AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(500, 100)) + slideInVertically(tween(500, 100))) {
                            EmployerStatsCard(
                                totalReferrals = uiState.stats?.totalReferrals ?: 0,
                                successfulReferrals = uiState.stats?.successfulReferrals ?: 0,
                                totalEarnings = uiState.stats?.totalEarnings ?: 0.0,
                                availableBalance = uiState.stats?.availableBalance ?: 0.0
                            )
                        }
                    }
                    
                    // Analytics Dashboard (V2.0 Professional Feature)
                    if (analytics != null && (analytics?.totalClicks ?: 0) > 0) {
                        item {
                            AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(525, 125)) + slideInVertically(tween(525, 125))) {
                                EmployerAnalyticsDashboardCard(analytics = analytics!!)
                            }
                        }
                    }
                    
                    // Success Stories (V2.0 Social Proof)
                    if (successStories.isNotEmpty()) {
                        item {
                            AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(540, 140)) + slideInVertically(tween(540, 140))) {
                                EmployerSuccessStoriesCard(stories = successStories)
                            }
                        }
                    }
                    
                    // Free Job Postings Card (if available)
                    val freePostings = uiState.stats?.freeJobPostings ?: 0
                    val freePostingsExpiry = uiState.stats?.freeJobPostingsExpiry
                    if (freePostings > 0 && freePostingsExpiry != null && freePostingsExpiry > System.currentTimeMillis()) {
                        item {
                            AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(550, 150)) + slideInVertically(tween(550, 150))) {
                                FreeJobPostingsCard(freePostings = freePostings, expiryDate = freePostingsExpiry)
                            }
                        }
                    }
                    
                    // Withdraw Button
                    if ((uiState.stats?.canWithdraw == true) && (uiState.stats?.availableBalance ?: 0.0) >= 50.0) {
                        item {
                            AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(600, 200)) + slideInVertically(tween(600, 200))) {
                                EmployerWithdrawCard(
                                    availableBalance = uiState.stats?.availableBalance ?: 0.0,
                                    onWithdrawClick = { showWithdrawDialog = true }
                                )
                            }
                        }
                    }
                    
                    // Milestone Progress
                    item {
                        AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(650, 250)) + slideInVertically(tween(650, 250))) {
                            EmployerMilestoneProgressCard(
                                successfulReferrals = uiState.stats?.successfulReferrals ?: 0,
                                nextMilestone = uiState.stats?.nextMilestone ?: 5
                            )
                        }
                    }
                    // Referral History
                    item {
                        AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(900, 500)) + slideInVertically(tween(900, 500))) {
                            EmployerReferralHistoryCard(referralHistory = uiState.referralHistory)
                        }
                    }
                    // How It Works
                    item {
                        AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(700, 300)) + slideInVertically(tween(700, 300))) {
                            EmployerHowItWorksCard()
                        }
                    }

                    // Rewards
                    item {
                        AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(800, 400)) + slideInVertically(tween(800, 400))) {
                            EmployerRewardsCard()
                        }
                    }
                    
                    // Redemption Instructions
                    item {
                        AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(850, 450)) + slideInVertically(tween(850, 450))) {
                            EmployerRedemptionInstructionsCard()
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
            ) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                    Spacer(Modifier.width(10.dp))
                    Text("Code copied!", color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
    
    // Withdraw Dialog
    if (showWithdrawDialog) {
        EmployerWithdrawDialog(
            availableBalance = uiState.stats?.availableBalance ?: 0.0,
            onDismiss = { showWithdrawDialog = false },
            onWithdraw = { amount, upiId ->
                viewModel.requestWithdrawal(amount, upiId)
                showWithdrawDialog = false
            }
        )
    }
}


@Composable
private fun EmployerTierBadgeCard(tier: ReferralTier, successfulReferrals: Int) {
    val tierName = ReferralRewards.getTierDisplayName(tier)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Current Tier",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF6B7280)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tierName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$successfulReferrals successful referrals",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF4B5563)
                )
            )
        }
    }
}

// Removed: EmployerQRCodeCard function - QR code feature removed

@Composable
private fun EmployerReferralCodeCard(referralCode: String, onCopyClick: () -> Unit, onShareClick: () -> Unit) {
    var showLinkCopied by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    
    LaunchedEffect(showLinkCopied) {
        if (showLinkCopied) {
            delay(2000)
            showLinkCopied = false
        }
    }
    
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Your Referral Code", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
            Spacer(Modifier.height(16.dp))
            SelectionContainer {
                Text(referralCode, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937), letterSpacing = 2.sp))
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCopyClick, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF374151))) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                    Spacer(Modifier.width(8.dp))
                    Text("Copy Code")
                }
                Button(onClick = onShareClick, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937))) {
                    Icon(Icons.Default.Share, null, Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Removed: QR Code and Copy Referral Link buttons
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun EmployerStatsCard(totalReferrals: Int, successfulReferrals: Int, totalEarnings: Double, availableBalance: Double) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Your Stats", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Referrals", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)))
                    Text(totalReferrals.toString(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Successful", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)))
                    Text(successfulReferrals.toString(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
                }
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE5E7EB))
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Earned", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)))
                    Text("Rs.${String.format("%.0f", totalEarnings)}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Available", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)))
                    Text("Rs.${String.format("%.0f", availableBalance)}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
                }
            }
        }
    }
}

@Composable
private fun EmployerStatItem(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Color.White, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun FreeJobPostingsCard(freePostings: Int, expiryDate: Long) {
    val expiryStr = remember(expiryDate) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(expiryDate))
    }
    
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF6366F1).copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(Color(0xFF6366F1).copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.WorkOutline, null, tint = Color(0xFF6366F1), modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Free Job Postings", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF4338CA)))
                Text("$freePostings posts available", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6366F1)))
                Text("Expires: $expiryStr", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF818CF8)))
            }
        }
    }
}


@SuppressLint("DefaultLocale")
@Composable
private fun EmployerWithdrawCard(availableBalance: Double, onWithdrawClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Available to Withdraw", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)))
                Text("Rs.${String.format("%.0f", availableBalance)}", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
            }
            Button(onClick = onWithdrawClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)), shape = RoundedCornerShape(12.dp)) {
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
    
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Next Milestone", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF1F2937),
                trackColor = Color(0xFFE5E7EB)
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$successfulReferrals / $nextMilestone referrals", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF4B5563)))
                Column(horizontalAlignment = Alignment.End) {
                    if (bonus > 0) {
                        Text("Rs.${bonus.toInt()} bonus", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937)))
                    }
                    if (freePostings > 0) {
                        Text("$freePostings free posts", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937)))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployerHowItWorksCard() {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("How It Works", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
            Spacer(Modifier.height(16.dp))
            val steps = listOf(
                "Share your referral code with employers or workers",
                "They sign up using your code",
                "You earn Rs.25 and they earn Rs.25 instantly",
                "Reach milestones for bonus cash and free job posts"
            )
            steps.forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2937)
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
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Rewards & Milestones", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
            Spacer(Modifier.height(16.dp))
            val rewards = listOf(
                "You earn Rs.25 for every successful referral",
                "Your friend gets an instant Rs.25 signup bonus",
                "5 referrals: Rs.50 bonus + 5 free job posts",
                "10 referrals: Rs.100 bonus + 10 free job posts",
                "15 referrals: Rs.150 milestone bonus",
                "25 referrals: Rs.250 bonus + 25 free job posts",
                "50 referrals: Rs.500 milestone bonus",
                "100 referrals: Rs.1000 milestone bonus"
            )
            rewards.forEach { text ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF1F2937)
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
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("How to Redeem", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Withdrawals open once your available balance reaches Rs.50.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            )
            Spacer(Modifier.height(12.dp))

            val steps = listOf(
                "Tap the Withdraw button on this screen",
                "Enter the amount and your UPI ID",
                "Submit the request for payout review",
                "Your referral wallet balance updates immediately after the request"
            )

            steps.forEachIndexed { index, step ->
                Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2937)
                        ),
                        modifier = Modifier.width(24.dp)
                    )
                    Text(step, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF4B5563)))
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Minimum withdrawal: Rs.50",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun EmployerReferralHistoryCard(referralHistory: List<Referral>) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Recent Referrals", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
            Spacer(Modifier.height(16.dp))
            if (referralHistory.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                    Box(modifier = Modifier.size(64.dp).background(Color(0xFFF3F4F6), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.People, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Large))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.no_referrals_yet), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF374151)))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.share_code_employers), style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)), textAlign = TextAlign.Center)
                }
            } else {
                referralHistory.take(5).forEachIndexed { index, referral ->
                    EmployerReferralHistoryItem(referral)
                    if (index < referralHistory.size - 1 && index < 4) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF3F4F6))
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun EmployerReferralHistoryItem(referral: Referral) {
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
                add("You: Rs.${String.format("%.0f", referral.rewardAmount)}")
                if (referral.bonusAmount > 0) {
                    add("Milestone: Rs.${String.format("%.0f", referral.bonusAmount)}")
                }
                if (referral.referredUserReward > 0) {
                    add("Signup bonus: Rs.${String.format("%.0f", referral.referredUserReward)}")
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
            tint = Color(0xFF1F2937),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(referral.referredUserName.ifBlank { "Employer" }, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = Color(0xFF1F2937)))
            Text(dateStr, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)))
            if (rewardBreakdown.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = rewardBreakdown,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                when (referral.status) {
                    ReferralStatus.COMPLETED -> "Rs.${String.format("%.0f", referral.getTotalReferrerReward())}"
                    ReferralStatus.PENDING -> "Pending"
                    ReferralStatus.EXPIRED -> "Expired"
                    else -> "Cancelled"
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
        }
    }
}

@Composable
private fun EmployerWithdrawDialog(availableBalance: Double, onDismiss: () -> Unit, onWithdraw: (Double, String) -> Unit) {
    var amount by remember { mutableStateOf(availableBalance.toString()) }
    var upiId by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Withdraw Earnings", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Available: Rs.${availableBalance.toInt()}", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF10B981)))
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount (Rs.)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = upiId, onValueChange = { upiId = it }, label = { Text("UPI ID") }, placeholder = { Text("yourname@upi") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    when {
                        amountValue < 50 -> error = "Minimum withdrawal is Rs.50"
                        amountValue > availableBalance -> error = "Insufficient balance"
                        upiId.isBlank() -> error = "Enter UPI ID"
                        !upiId.contains("@") -> error = "Invalid UPI ID format"
                        else -> onWithdraw(amountValue, upiId)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) { Text(stringResource(R.string.withdraw_button)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Color(0xFF6B7280)) } },
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
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Performance Analytics", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Conversion Rate
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Conversion Rate", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)))
                    Text("${String.format("%.1f", analytics.conversionRate)}%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF10B981)))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Clicks", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)))
                    Text(analytics.totalClicks.toString(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
                }
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE5E7EB))
            Spacer(Modifier.height(16.dp))
            
            // Rankings
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Your Rank", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)))
                    Text("#${analytics.rankOverall}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Top Percentile", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)))
                    Text("Top ${analytics.percentile}%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B)))
                }
            }

            if (analytics.projectedMonthlyEarnings > 0) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB))
                Spacer(Modifier.height(16.dp))
                
                // Projected Earnings
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Projected Monthly", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)))
                        Text("Rs.${analytics.projectedMonthlyEarnings}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF10B981)))
                    }
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(32.dp))
                }
            }
            
        }
    }
}

@Composable
private fun EmployerSuccessStoriesCard(stories: List<ReferralSuccessStory>) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFFEF3C7), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Top Performers", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF92400E)))
            }
            
            Spacer(Modifier.height(12.dp))
            
            stories.take(3).forEach { story ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${story.userName} from ${story.city}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = Color(0xFF92400E)), modifier = Modifier.weight(1f))
                    Text("Rs.${story.totalEarnings.toInt()}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF92400E)))
                }
            }
            
            if (stories.size > 3) {
                Spacer(Modifier.height(8.dp))
                Text("+${stories.size - 3} more top performers", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA16207), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
            }
        }
    }
}


@Composable
private fun EmployerLegalDisclaimerCard() {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFFEF3C7), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Important Information", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF92400E)))
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text("Ã¢â‚¬Â¢ This is a legitimate referral program, not a pyramid scheme", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF92400E)), modifier = Modifier.padding(vertical = 4.dp))
            Text("Ã¢â‚¬Â¢ Referral rewards are taxable income under Indian tax laws", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF92400E)), modifier = Modifier.padding(vertical = 4.dp))
            Text("Ã¢â‚¬Â¢ KYC required for withdrawals > Ã¢â€šÂ¹10,000/year", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF92400E)), modifier = Modifier.padding(vertical = 4.dp))
            Text("Ã¢â‚¬Â¢ PAN card mandatory for withdrawals > Ã¢â€šÂ¹50,000/year", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF92400E)), modifier = Modifier.padding(vertical = 4.dp))
            Text("Ã¢â‚¬Â¢ Fraudulent activity will result in account suspension", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF92400E)), modifier = Modifier.padding(vertical = 4.dp))
            
            Spacer(Modifier.height(8.dp))
            
            Text("By participating, you agree to our Terms & Conditions and RBI guidelines.", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA16207), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
        }
    }
}
