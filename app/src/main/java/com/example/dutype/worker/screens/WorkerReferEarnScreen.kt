package com.example.dutype.worker.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.components.CommonHeader
import com.example.dutype.components.ReferralQRCodeCard
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun WorkerReferEarnScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showCopySuccess by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showQRCode by remember { mutableStateOf(false) }
    
    // Real-time referral data
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid?.take(6)?.uppercase() ?: "WRK"
    var referralCode by remember { mutableStateOf("WRK$userId") }
    var userName by remember { mutableStateOf(currentUser?.displayName ?: "Worker") }
    var totalReferrals by remember { mutableStateOf(0) }
    var successfulReferrals by remember { mutableStateOf(0) }
    var totalEarnings by remember { mutableStateOf(0.0) }
    var pendingEarnings by remember { mutableStateOf(0.0) }
    var referralHistory by remember { mutableStateOf<List<WorkerReferralItem>>(emptyList()) }
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Status bar color - white for consistency
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        delay(100)
        isVisible = true
    }

    // Simulate real-time data loading
    LaunchedEffect(Unit) {
        delay(1000)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Common Header
        CommonHeader(
            title = stringResource(R.string.refer_earn),
            subtitle = "Invite friends & earn rewards",
            onBackClick = { navController.popBackStack() },
            backgroundColor = Color.White,
            actions = {
                IconButton(onClick = { showQRCode = !showQRCode }) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "Show QR Code",
                        tint = Color(0xFF1F2937)
                    )
                }
            }
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color(0xFF1F2937),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading...",
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // QR Code Card (toggleable)
                if (showQRCode) {
                    item {
                        AnimatedVisibility(
                            visible = showQRCode,
                            enter = fadeIn(tween(300)) + slideInVertically(tween(300))
                        ) {
                            ReferralQRCodeCard(
                                referralCode = referralCode,
                                userName = userName,
                                userRole = "Worker"
                            )
                        }
                    }
                }
                
                // Referral Code Card
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(400)) + slideInVertically(tween(400))
                    ) {
                        ReferralCodeCard(
                            referralCode = referralCode,
                            onCopyClick = {
                                clipboardManager.setText(AnnotatedString(referralCode))
                                showCopySuccess = true
                            },
                            onShareClick = { showShareDialog = true },
                            onQRClick = { showQRCode = !showQRCode }
                        )
                    }
                }

                // Stats Overview
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(500, 100)) + slideInVertically(tween(500, 100))
                    ) {
                        StatsCard(
                            totalReferrals = totalReferrals,
                            successfulReferrals = successfulReferrals,
                            totalEarnings = totalEarnings,
                            pendingEarnings = pendingEarnings
                        )
                    }
                }

                // How It Works
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(600, 200)) + slideInVertically(tween(600, 200))
                    ) {
                        HowItWorksCard()
                    }
                }

                // Rewards
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(700, 300)) + slideInVertically(tween(700, 300))
                    ) {
                        RewardsCard()
                    }
                }

                // Referral History
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(800, 400)) + slideInVertically(tween(800, 400))
                    ) {
                        ReferralHistoryCard(referralHistory = referralHistory)
                    }
                }
                
                // Bottom spacing
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    // Copy success dialog
    if (showCopySuccess) {
        AlertDialog(
            onDismissRequest = { showCopySuccess = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Copied!", fontWeight = FontWeight.Bold)
                }
            },
            text = { Text("Referral code copied to clipboard") },
            confirmButton = {
                Button(
                    onClick = { showCopySuccess = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937))
                ) { Text("OK") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Share dialog
    if (showShareDialog) {
        ShareDialog(
            referralCode = referralCode,
            onDismiss = { showShareDialog = false }
        )
    }
}

@Composable
private fun ReferralCodeCard(
    referralCode: String,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    onQRClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon with background
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF1F2937).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = Color(0xFF1F2937),
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Your Referral Code",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Code display
            Surface(
                color = Color(0xFFF3F4F6),
                shape = RoundedCornerShape(12.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = referralCode,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            letterSpacing = 3.sp
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF374151))
                ) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy")
                }
                
                OutlinedButton(
                    onClick = onQRClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF374151))
                ) {
                    Icon(Icons.Default.QrCode, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("QR")
                }
                
                Button(
                    onClick = onShareClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937))
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share")
                }
            }
        }
    }
}


@SuppressLint("DefaultLocale")
@Composable
private fun StatsCard(
    totalReferrals: Int,
    successfulReferrals: Int,
    totalEarnings: Double,
    pendingEarnings: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Your Performance",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF10B981), CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "LIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(
                    title = "Total",
                    value = totalReferrals.toString(),
                    icon = Icons.Default.People,
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    title = "Successful",
                    value = successfulReferrals.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(
                    title = "Earned",
                    value = "₹${String.format("%.0f", totalEarnings)}",
                    icon = Icons.Default.AttachMoney,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    title = "Pending",
                    value = "₹${String.format("%.0f", pendingEarnings)}",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
            )
        }
    }
}

@Composable
private fun HowItWorksCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "How It Works",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val steps = listOf(
                "Share your referral code or QR with friends",
                "They sign up using your code",
                "When they complete their first job, you earn ₹50",
                "Earn more as they continue working"
            )
            
            steps.forEachIndexed { index, step ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFF1F2937), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF4B5563)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RewardsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Rewards & Benefits",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val rewards = listOf(
                "💰" to "₹50 for each successful referral",
                "🎯" to "Bonus ₹100 for 5+ referrals",
                "⭐" to "Priority job matching",
                "🏆" to "Monthly leaderboard rewards"
            )
            
            rewards.forEach { (emoji, text) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Text(text = emoji, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF4B5563))
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferralHistoryCard(referralHistory: List<WorkerReferralItem>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Recent Referrals",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (referralHistory.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFF3F4F6), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No referrals yet",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Share your code with friends to start earning!",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                referralHistory.take(5).forEachIndexed { index, referral ->
                    ReferralHistoryItem(referral = referral)
                    if (index < referralHistory.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color(0xFFF3F4F6)
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun ReferralHistoryItem(referral: WorkerReferralItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    when (referral.status) {
                        "Completed" -> Color(0xFF10B981).copy(alpha = 0.1f)
                        "Pending" -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                        else -> Color(0xFF3B82F6).copy(alpha = 0.1f)
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (referral.status) {
                    "Completed" -> Icons.Default.CheckCircle
                    "Pending" -> Icons.AutoMirrored.Filled.TrendingUp
                    else -> Icons.Default.People
                },
                contentDescription = null,
                tint = when (referral.status) {
                    "Completed" -> Color(0xFF10B981)
                    "Pending" -> Color(0xFFF59E0B)
                    else -> Color(0xFF3B82F6)
                },
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = referral.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1F2937)
                )
            )
            Text(
                text = referral.date,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (referral.earnings > 0) "₹${String.format("%.0f", referral.earnings)}" else "Pending",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (referral.earnings > 0) Color(0xFF10B981) else Color(0xFFF59E0B)
                )
            )
        }
    }
}

@Composable
private fun ShareDialog(
    referralCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val playStoreUrl = "https://play.google.com/store/apps/details?id=com.dutype.app"
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Share Referral Code", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Share your referral code with friends:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFF3F4F6),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = referralCode,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val shareText = """
🎁 Join DutyPe and earn money!

Use my referral code: $referralCode

📲 Download DutyPe: $playStoreUrl

Find jobs near you and start earning today!
                    """.trimIndent()
                    
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Referral Code"))
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937))
            ) { Text("Share") }
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
