package com.example.partimes.employer.screens.referral

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerReferEarnScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showCopySuccess by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showNewReferralNotification by remember { mutableStateOf(false) }
    
    // Real-time referral data
    var referralCode by remember { mutableStateOf("EMP2024") }
    var totalReferrals by remember { mutableStateOf(0) }
    var successfulReferrals by remember { mutableStateOf(0) }
    var totalEarnings by remember { mutableStateOf(0.0) }
    var pendingEarnings by remember { mutableStateOf(0.0) }
    var referralHistory by remember { mutableStateOf<List<ReferralItem>>(emptyList()) }
    
    // Real-time simulation data
    val coroutineScope = rememberCoroutineScope()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Status bar color management
    val statusBarColor = Color(0xFF2193b0)
    LaunchedEffect(statusBarColor) {
        onStatusBarColorChange(statusBarColor)
    }

    // Animation states
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    // Simulate real-time data loading
    LaunchedEffect(Unit) {
        delay(2000) // Simulate API call
        isLoading = false
        
        // Start with zero data - completely real-time
        totalReferrals = 0
        successfulReferrals = 0
        totalEarnings = 0.0
        pendingEarnings = 0.0
        
        // Start with empty history - will be populated by real-time events
        referralHistory = emptyList()
        
        // Start real-time simulation
        val startRealTimeSimulation = Unit
        startRealTimeSimulation
    }
    
    // Real-time simulation function
    val startRealTimeSimulation: () -> Unit = {
        coroutineScope.launch {
            while (true) {
                delay(15000) // Check every 15 seconds for new referrals
                
                // Simulate random new referrals (30% chance)
                if ((1..100).random() <= 30) {
                    simulateNewReferral()
                }
                
                // Simulate status updates (20% chance)
                if ((1..100).random() <= 20) {
                    val simulateStatusUpdate = null
                    simulateStatusUpdate
                }
            }
        }
    }
    
    // Simulate new referral
    val simulateNewReferral: () -> Unit = {
        val names = listOf(
            "Alex Thompson", "Maria Garcia", "James Wilson", "Lisa Chen", "Robert Taylor",
            "Jennifer Lee", "Michael Brown", "Sarah Davis", "David Miller", "Emily Johnson"
        )
        val positions = listOf(
            "Software Engineer", "Marketing Manager", "Data Analyst", "UX Designer", 
            "Product Manager", "Sales Director", "HR Manager", "Finance Analyst"
        )
        
        val newName = names.random()
        val newPosition = positions.random()
        val currentDate = dateFormat.format(Date())
        
        val newReferral = ReferralItem(
            name = newName,
            position = newPosition,
            date = currentDate,
            status = "Pending",
            earnings = 0.0
        )
        
        // Update data
        totalReferrals++
        pendingEarnings += 30.0
        
        // Add to history (at the beginning)
        referralHistory = listOf(newReferral) + referralHistory.take(9) // Keep only 10 most recent
        
        // Show notification
        showNewReferralNotification = true
        
        // Auto-hide notification after 3 seconds
        coroutineScope.launch {
            delay(3000)
            showNewReferralNotification = false
        }
    }
    
    // Simulate status update (Pending -> Completed)
    val simulateStatusUpdate: () -> Unit = {
        val pendingReferrals = referralHistory.filter { it.status == "Pending" }
        if (pendingReferrals.isNotEmpty()) {
            val randomPending = pendingReferrals.random()
            val updatedHistory = referralHistory.map { referral ->
                if (referral == randomPending) {
                    referral.copy(
                        status = "Completed",
                        earnings = 30.0
                    )
                } else {
                    referral
                }
            }
            
            referralHistory = updatedHistory
            
            // Update stats
            successfulReferrals++
            totalEarnings += 30.0
            pendingEarnings -= 30.0
        }
    }

    // Gradient background
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2193b0), // Clean sky blue
            Color(0xFF6dd5ed), // Soft light blue
            Color(0xFFFFFFFF)  // Pure white
        ),
        startY = 0f,
        endY = 1000f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Refer & Earn",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.popBackStack() }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF2193b0),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading referral data...",
                            color = Color(0xFF2193b0),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = paddingValues.calculateTopPadding() + 16.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(800)) + slideInVertically(tween(800))
                        ) {
                            ReferralCodeCard(
                                referralCode = referralCode,
                                onCopyClick = { showCopySuccess = true },
                                onShareClick = { showShareDialog = true }
                            )
                        }
                    }

                    // Real-time notification
                    if (showNewReferralNotification) {
                        item {
                            AnimatedVisibility(
                                visible = showNewReferralNotification,
                                enter = fadeIn(tween(500)) + slideInVertically(tween(500))
                            ) {
                                NewReferralNotificationCard()
                            }
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(900, 100)) + slideInVertically(tween(900, 100))
                        ) {
                            TestReferralSystem(
                                onNewReferral = { newReferral ->
                                    // Update data
                                    totalReferrals++
                                    pendingEarnings += 30.0
                                    
                                    // Add to history (at the beginning)
                                    referralHistory = listOf(newReferral) + referralHistory.take(9) // Keep only 10 most recent
                                    
                                    // Show notification
                                    showNewReferralNotification = true
                                    
                                    // Auto-hide notification after 3 seconds
                                    coroutineScope.launch {
                                        delay(3000)
                                        showNewReferralNotification = false
                                    }
                                },
                                onStatusUpdate = { _ ->
                                    // This will be handled by the status update system
                                }
                            )
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                        ) {
                            StatsOverviewCard(
                                totalReferrals = totalReferrals,
                                successfulReferrals = successfulReferrals,
                                totalEarnings = totalEarnings,
                                pendingEarnings = pendingEarnings
                            )
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(1200, 400)) + slideInVertically(tween(1200, 400))
                        ) {
                            HowItWorksCard()
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(1400, 600)) + slideInVertically(tween(1400, 600))
                        ) {
                            RewardsCard()
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(1600, 800)) + slideInVertically(tween(1600, 800))
                        ) {
                            ReferralHistoryCard(
                                referralHistory = referralHistory
                            )
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(1800, 1000)) + slideInVertically(tween(1800, 1000))
                        ) {
                            TestStatusUpdateSystem(
                                onStatusUpdate = { _ ->
                                    // Simulate status update
                                    val pendingReferrals = referralHistory.filter { it.status == "Pending" }
                                    if (pendingReferrals.isNotEmpty()) {
                                        val randomPending = pendingReferrals.random()
                                        val updatedHistory = referralHistory.map { referral ->
                                            if (referral == randomPending) {
                                                referral.copy(
                                                    status = "Completed",
                                                    earnings = 30.0
                                                )
                                            } else {
                                                referral
                                            }
                                        }
                                        
                                        referralHistory = updatedHistory
                                        
                                        // Update stats
                                        successfulReferrals++
                                        totalEarnings += 30.0
                                        pendingEarnings -= 30.0
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Copy success dialog
    if (showCopySuccess) {
        AlertDialog(
            onDismissRequest = { showCopySuccess = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Copied!",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text("Referral code copied to clipboard")
            },
            confirmButton = {
                Button(
                    onClick = { showCopySuccess = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2193b0)
                    )
                ) {
                    Text("OK")
                }
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

private fun CoroutineScope.simulateNewReferral() {
    TODO("Not yet implemented")
}


@Composable
private fun NewReferralNotificationCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color(0xFF4CAF50),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "🎉 New Referral!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                )
                Text(
                    text = "Someone just used your referral code!",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF666666)
                    )
                )
            }
            
            Text(
                text = "+$30",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            )
        }
    }
}

@Composable
private fun ReferralCodeCard(
    referralCode: String,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CardGiftcard,
                contentDescription = null,
                tint = Color(0xFF2193b0),
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Your Referral Code",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            SelectionContainer {
                Text(
                    text = referralCode,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2193b0)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2193b0)
                    ),
                  border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                      brush = Brush.horizontalGradient(
                          colors = listOf(Color(0xFF2193b0), Color(0xFF6dd5ed))
                      )
                  ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy")
                }
                
                Button(
                    onClick = onShareClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2193b0)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }
            }
            
        }
    }
}


@SuppressLint("DefaultLocale")
@Composable
private fun StatsOverviewCard(
    totalReferrals: Int,
    successfulReferrals: Int,
    totalEarnings: Double,
    pendingEarnings: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Performance",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Live indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            Color(0xFF4CAF50),
                            CircleShape
                        )
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "LIVE",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(
                    title = "Total Referrals",
                    value = totalReferrals.toString(),
                    icon = Icons.Default.People,
                    color = Color(0xFF2193b0),
                    modifier = Modifier.weight(1f)
                )
                
                StatItem(
                    title = "Successful",
                    value = successfulReferrals.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(
                    title = "Total Earnings",
                    value = "$${String.format("%.0f", totalEarnings)}",
                    icon = Icons.Default.AttachMoney,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
                
                StatItem(
                    title = "Pending",
                    value = "$${String.format("%.0f", pendingEarnings)}",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    color = Color(0xFF9C27B0),
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
    Card(
        modifier = modifier.shadow(2.dp,
            RoundedCornerShape(12.dp)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Animated counter
            val animatedValue by animateFloatAsState(
                targetValue = value.toFloatOrNull() ?: 0f,
                animationSpec = tween(1000),
                label = "counter"
            )
            
            Text(
                text = if (value.contains("$")) {
                    value // Don't animate currency values
                } else {
                    animatedValue.toInt().toString()
                },
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF666666)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HowItWorksCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "How It Works",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val steps = listOf(
                "Share your referral code with potential employers",
                "They sign up using your code",
                "When they post their first job, you earn $30",
                "Earn more as they continue using the platform"
            )
            
            steps.forEachIndexed { index, step ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                Color(0xFF2193b0),
                                CircleShape
                            ),
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
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF666666)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RewardsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Rewards & Benefits",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val rewards = listOf(
                "💰 $30 for each successful employer referral",
                "🎯 Bonus $50 for 5+ successful referrals",
                "⭐ Premium features for top referrers",
                "🏆 Monthly leaderboard rewards"
            )
            
            rewards.forEach { reward ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = reward,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF666666)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferralHistoryCard(
    referralHistory: List<ReferralItem>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Referrals",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Live indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            Color(0xFF2193b0),
                            CircleShape
                        )
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "UPDATING",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2193b0)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (referralHistory.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = Color(0xFF2193b0).copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "No referrals yet",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                    )
                    
                    Text(
                        text = "Start sharing your code or use the test system above!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF666666)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                referralHistory.take(5).forEach { referral ->
                    ReferralHistoryItem(referral = referral)
                    if (referral != referralHistory.last()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun ReferralHistoryItem(
    referral: ReferralItem
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    when (referral.status) {
                        "Completed" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        "Pending" -> Color(0xFFFF9800).copy(alpha = 0.1f)
                        else -> Color(0xFF2193b0).copy(alpha = 0.1f)
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
                    "Completed" -> Color(0xFF4CAF50)
                    "Pending" -> Color(0xFFFF9800)
                    else -> Color(0xFF2193b0)
                },
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = referral.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A)
                )
            )
            
            Text(
                text = "${referral.position} • ${referral.date}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF666666)
                )
            )
        }
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = if (referral.earnings > 0) "$${String.format("%.0f", referral.earnings)}" else "Pending",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (referral.earnings > 0) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            )
            
            Text(
                text = referral.status,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = when (referral.status) {
                        "Completed" -> Color(0xFF4CAF50)
                        "Pending" -> Color(0xFFFF9800)
                        else -> Color(0xFF2193b0)
                    }
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
    val clipboardManager = LocalClipboard.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Share Referral Code",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Share your referral code with potential employers:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SelectionContainer {
                    Text(
                        text = referralCode,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2193b0)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Here you would implement actual sharing functionality
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2193b0)
                )
            ) {
                Text("Share")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF666666)
                )
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

data class ReferralItem(
    val name: String,
    val position: String,
    val date: String,
    val status: String,
    val earnings: Double
)