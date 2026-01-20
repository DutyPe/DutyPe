package com.example.dutype.worker.screens

import android.annotation.SuppressLint
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
import com.example.dutype.components.QRCodeGenerator
import com.example.dutype.models.*
import com.example.dutype.viewmodels.ReferralViewModel
import com.example.dutype.ui.theme.WorkerColors
import kotlinx.coroutines.delay

@Composable
fun WorkerReferEarnScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    val viewModel: ReferralViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    var isVisible by remember { mutableStateOf(false) }
    var showCopySuccess by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val playStoreUrl = "https://play.google.com/store/apps/details?id=com.dutype.app"

    // Load data on screen launch
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        viewModel.loadReferralData()
        delay(100)
        isVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkerColors.ScreenBackground)
    ) {
        CommonHeader(
            title = stringResource(R.string.refer_earn),
            onBackClick = { navController.popBackStack() },
            backgroundColor = WorkerColors.CardBackground
        )
        
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF1F2937), strokeWidth = 3.dp)
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
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.error ?: "Something went wrong",
                            color = Color(0xFF6B7280)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadReferralData() }) {
                            Text("Retry")
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
                            QRCodeSection(
                                referralCode = uiState.stats?.referralCode ?: "",
                                onCopyClick = {
                                    clipboardManager.setText(AnnotatedString(uiState.stats?.referralCode ?: ""))
                                    showCopySuccess = true
                                },
                                onShareClick = {
                                    val code = uiState.stats?.referralCode ?: ""
                                    val shareText = """
🎁 Join DutyPe and start earning!

Use my referral code: $code

📲 Download: $playStoreUrl

Find local jobs near you and earn ₹10 bonus!
                                    """.trimIndent()
                                    
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Referral"))
                                }
                            )
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
                                availableBalance = uiState.stats?.availableBalance ?: 0.0
                            )
                        }
                    }
                    
                    // Withdraw Button
                    if ((uiState.stats?.canWithdraw == true) && (uiState.stats?.availableBalance ?: 0.0) >= 50.0) {
                        item {
                            AnimatedVisibility(
                                visible = isVisible,
                                enter = fadeIn(tween(550, 150)) + slideInVertically(tween(550, 150))
                            ) {
                                WithdrawCard(
                                    availableBalance = uiState.stats?.availableBalance ?: 0.0,
                                    onWithdrawClick = { showWithdrawDialog = true }
                                )
                            }
                        }
                    }
                    
                    // Progress to Next Milestone
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(600, 200)) + slideInVertically(tween(600, 200))
                        ) {
                            MilestoneProgressCard(
                                successfulReferrals = uiState.stats?.successfulReferrals ?: 0,
                                nextMilestone = uiState.stats?.nextMilestone ?: 5
                            )
                        }
                    }

                    // How It Works
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(700, 300)) + slideInVertically(tween(700, 300))
                        ) {
                            HowItWorksSection()
                        }
                    }

                    // Rewards
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(800, 400)) + slideInVertically(tween(800, 400))
                        ) {
                            RewardsSection()
                        }
                    }
                    
                    // Redemption Instructions
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(850, 450)) + slideInVertically(tween(850, 450))
                        ) {
                            RedemptionInstructionsSection()
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Code copied!",
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
            onDismiss = { showWithdrawDialog = false },
            onWithdraw = { amount, upiId ->
                viewModel.requestWithdrawal(amount, upiId)
                showWithdrawDialog = false
            }
        )
    }
}


@Composable
private fun TierBadgeCard(tier: ReferralTier, successfulReferrals: Int) {
    val tierColor = Color(ReferralRewards.getTierColor(tier))
    val tierName = ReferralRewards.getTierDisplayName(tier)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = tierColor.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(tierColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (tier) {
                        ReferralTier.BRONZE -> "🥉"
                        ReferralTier.SILVER -> "🥈"
                        ReferralTier.GOLD -> "🥇"
                        ReferralTier.PLATINUM -> "💎"
                        ReferralTier.DIAMOND -> "👑"
                    },
                    fontSize = 24.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = tierName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
                Text(
                    text = "$successfulReferrals successful referrals",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280)
                    )
                )
            }
        }
    }
}

@Composable
private fun QRCodeSection(
    referralCode: String,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit
) {
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val referralLink = remember(referralCode) { 
        if (referralCode.isNotBlank()) QRCodeGenerator.generateReferralLink(referralCode) else ""
    }
    
    LaunchedEffect(referralLink) {
        if (referralLink.isNotBlank()) {
            qrBitmap = QRCodeGenerator.generateQRCode(referralLink, 400)
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your Referral Code",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Card(
                modifier = Modifier.size(180.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color(0xFF1F2937),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SelectionContainer {
                        Text(
                            text = referralCode,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937),
                                letterSpacing = 3.sp
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF374151)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copy", fontWeight = FontWeight.Medium)
                }
                
                Button(
                    onClick = onShareClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937))
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share", fontWeight = FontWeight.Medium)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Scan QR or share code with friends",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF9CA3AF),
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun StatsGrid(
    totalReferrals: Int,
    successfulReferrals: Int,
    totalEarnings: Double,
    availableBalance: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Your Stats",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("Total", totalReferrals.toString(), Icons.Default.People, Color(0xFF3B82F6), Modifier.weight(1f))
            StatCard("Successful", successfulReferrals.toString(), Icons.Default.CheckCircle, Color(0xFF10B981), Modifier.weight(1f))
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("Total Earned", "₹${String.format("%.0f", totalEarnings)}", Icons.Default.AccountBalanceWallet, Color(0xFFF59E0B), Modifier.weight(1f))
            StatCard("Available", "₹${String.format("%.0f", availableBalance)}", Icons.AutoMirrored.Filled.TrendingUp, Color(0xFF8B5CF6), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
            Text(title, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)))
        }
    }
}


@SuppressLint("DefaultLocale")
@Composable
private fun WithdrawCard(availableBalance: Double, onWithdrawClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Available to Withdraw",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF065F46))
                )
                Text(
                    text = "₹${String.format("%.0f", availableBalance)}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                )
            }
            Button(
                onClick = onWithdrawClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AccountBalanceWallet, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Withdraw")
            }
        }
    }
}

@Composable
private fun MilestoneProgressCard(successfulReferrals: Int, nextMilestone: Int) {
    val progress = if (nextMilestone > 0) successfulReferrals.toFloat() / nextMilestone.toFloat() else 0f
    val bonus = ReferralRewards.getMilestoneBonus(nextMilestone)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Flag, null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Next Milestone: $nextMilestone referrals",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF6366F1),
                trackColor = Color(0xFFE5E7EB)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$successfulReferrals / $nextMilestone",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                )
                if (bonus > 0) {
                    Text(
                        text = "+₹${bonus.toInt()} bonus",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "How It Works",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            )
            Spacer(Modifier.height(16.dp))
            
            val steps = listOf(
                "Share your code or QR with friends",
                "They sign up using your code",
                "When they complete profile, you earn ₹10",
                "Reach milestones for bonus rewards!"
            )
            
            steps.forEachIndexed { index, step ->
                Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(28.dp).background(Color(0xFF1F2937), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(step, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF4B5563)))
                }
            }
        }
    }
}

@Composable
private fun RewardsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Rewards & Milestones",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                )
            }
            Spacer(Modifier.height(14.dp))
            
            val rewards = listOf(
                "💰" to "₹10 per successful referral",
                "🎯" to "5 referrals: +₹50 bonus (can withdraw)",
                "🏆" to "10 referrals: +₹100 bonus (can withdraw)",
                "⭐" to "15 referrals: +₹150 bonus (withdraw anytime)",
                "💎" to "25 referrals: +₹250 bonus (Platinum tier)",
                "👑" to "50 referrals: +₹500 bonus (Diamond tier)"
            )
            
            rewards.forEach { (emoji, text) ->
                Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 16.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(text, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF92400E)))
                }
            }
        }
    }
}

@Composable
private fun RedemptionInstructionsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Color(0xFF059669), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "How to Redeem",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                )
            }
            Spacer(Modifier.height(14.dp))
            
            Text(
                text = "Once you reach ₹100 or more:",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF047857))
            )
            Spacer(Modifier.height(8.dp))
            
            val steps = listOf(
                "1. Take a screenshot of your earnings",
                "2. Send it to dutypein@gmail.com",
                "3. Include your registered phone number",
                "4. We'll transfer the amount within 3-5 days"
            )
            
            steps.forEach { step ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(step, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF065F46)))
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Surface(
                color = Color(0xFF059669).copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Email, null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "dutypein@gmail.com",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                    )
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun ReferralHistorySection(referralHistory: List<Referral>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Recent Referrals",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            )
            Spacer(Modifier.height(16.dp))
            
            if (referralHistory.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).background(Color(0xFFE5E7EB), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.People, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("No referrals yet", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = Color(0xFF374151)))
                    Spacer(Modifier.height(4.dp))
                    Text("Share your code to start earning!", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF)))
                }
            } else {
                referralHistory.take(5).forEachIndexed { index, referral ->
                    ReferralHistoryItem(referral)
                    if (index < referralHistory.size - 1 && index < 4) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE5E7EB))
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun ReferralHistoryItem(referral: Referral) {
    val dateStr = remember(referral.createdAt) {
        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(referral.createdAt))
    }
    
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(40.dp).background(
                when (referral.status) {
                    ReferralStatus.COMPLETED -> Color(0xFF10B981).copy(alpha = 0.1f)
                    ReferralStatus.PENDING -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                    ReferralStatus.EXPIRED -> Color(0xFF6B7280).copy(alpha = 0.1f)
                    else -> Color(0xFFEF4444).copy(alpha = 0.1f)
                },
                CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                when (referral.status) {
                    ReferralStatus.COMPLETED -> Icons.Default.CheckCircle
                    ReferralStatus.PENDING -> Icons.AutoMirrored.Filled.TrendingUp
                    else -> Icons.Default.Person
                },
                null,
                tint = when (referral.status) {
                    ReferralStatus.COMPLETED -> Color(0xFF10B981)
                    ReferralStatus.PENDING -> Color(0xFFF59E0B)
                    ReferralStatus.EXPIRED -> Color(0xFF6B7280)
                    else -> Color(0xFFEF4444)
                },
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(referral.referredUserName.ifBlank { "User" }, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = Color(0xFF1F2937)))
            Text(dateStr, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF)))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                when (referral.status) {
                    ReferralStatus.COMPLETED -> "₹${String.format("%.0f", referral.rewardAmount)}"
                    ReferralStatus.PENDING -> "Pending"
                    ReferralStatus.EXPIRED -> "Expired"
                    else -> "Cancelled"
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = when (referral.status) {
                        ReferralStatus.COMPLETED -> Color(0xFF10B981)
                        ReferralStatus.PENDING -> Color(0xFFF59E0B)
                        else -> Color(0xFF6B7280)
                    }
                )
            )
        }
    }
}

@Composable
private fun WithdrawDialog(
    availableBalance: Double,
    onDismiss: () -> Unit,
    onWithdraw: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf(availableBalance.toString()) }
    var upiId by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Withdraw Earnings", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Available: ₹${availableBalance.toInt()}", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF10B981)))
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("UPI ID") },
                    placeholder = { Text("yourname@upi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
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
                        amountValue < 50 -> error = "Minimum withdrawal is ₹50"
                        amountValue > availableBalance -> error = "Insufficient balance"
                        upiId.isBlank() -> error = "Enter UPI ID"
                        !upiId.contains("@") -> error = "Invalid UPI ID format"
                        else -> onWithdraw(amountValue, upiId)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) { Text("Withdraw") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF6B7280)) }
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
