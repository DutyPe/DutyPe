package com.example.dutype.worker.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Production AdMob Rewarded Ad Unit ID provided by user
private const val PRODUCTION_REWARDED_AD_UNIT_ID = "ca-app-pub-5503082977524600/2148771632"
// Google Official Rewarded Video Test Ad Unit ID (Fallback for test devices)
private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

// Wheel Segments Definition
data class WheelSegment(
    val label: String,
    val subLabel: String,
    val color: Color,
    val isJackpot: Boolean = false,
    val isScratchCard: Boolean = false,
    val coinValue: Int = 0,
    val cashValue: Int = 0
)

val WHEEL_SEGMENTS = listOf(
    WheelSegment("₹100", "JACKPOT", Color(0xFFF59E0B), isJackpot = true, cashValue = 100),
    WheelSegment("50", "COINS", Color(0xFF10B981), coinValue = 50),
    WheelSegment("₹2", "CASH", Color(0xFF3B82F6), cashValue = 2),
    WheelSegment("100", "COINS", Color(0xFF8B5CF6), coinValue = 100),
    WheelSegment("TRY", "AGAIN", Color(0xFF64748B)),
    WheelSegment("₹5", "SCRATCH", Color(0xFFEF4444), isScratchCard = true, cashValue = 5),
    WheelSegment("25", "COINS", Color(0xFF14B8A6), coinValue = 25),
    WheelSegment("2X", "BOOST", Color(0xFFF97316), coinValue = 10)
)

@Composable
fun SpinAndWinScreen(
    navController: NavController,
    referralCode: String = "DUTYPE",
    // Configured with production AdMob Rewarded Ad Unit ID provided by user
    adMobRewardedAdUnitId: String = PRODUCTION_REWARDED_AD_UNIT_ID
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { context.getSharedPreferences("spin_and_win_game_prefs", Context.MODE_PRIVATE) }

    // Daily reset check
    val todayDateStr = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date()) }
    var spinsUsedToday by remember { mutableIntStateOf(0) }
    var totalDummyCoins by remember { mutableIntStateOf(100) }
    var totalDummyCash by remember { mutableIntStateOf(0) }
    var consecutiveSpinCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val lastDate = prefs.getString("last_spin_date", "")
        if (lastDate != todayDateStr) {
            prefs.edit().putString("last_spin_date", todayDateStr).putInt("spins_used_today", 0).apply()
            spinsUsedToday = 0
        } else {
            spinsUsedToday = prefs.getInt("spins_used_today", 0)
        }
        totalDummyCoins = prefs.getInt("dummy_coins", 100)
        totalDummyCash = prefs.getInt("dummy_cash", 0)
        consecutiveSpinCount = prefs.getInt("consecutive_spins", 0)
    }

    // AdMob Rewarded Ad State
    var mRewardedAd by remember { mutableStateOf<RewardedAd?>(null) }
    var isAdLoading by remember { mutableStateOf(false) }

    fun loadRewardedAd() {
        if (isAdLoading || mRewardedAd != null) return
        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, adMobRewardedAdUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Timber.w("SpinAndWin: Rewarded ad failed to load: ${adError.message}")
                mRewardedAd = null
                isAdLoading = false
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                Timber.d("SpinAndWin: Rewarded ad loaded successfully")
                mRewardedAd = rewardedAd
                isAdLoading = false
            }
        })
    }

    // Preload Rewarded Ad on launch
    LaunchedEffect(Unit) {
        loadRewardedAd()
    }

    // Rotation Animatable
    val wheelRotation = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var resultSegment by remember { mutableStateOf<WheelSegment?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var showScratchCardDialog by remember { mutableStateOf(false) }
    var scratchCardAmount by remember { mutableIntStateOf(2) }

    val maxSpinsPerDay = 12
    val remainingSpins = (maxSpinsPerDay - spinsUsedToday).coerceAtLeast(0)

    fun executeSpin(targetSegmentIndex: Int) {
        if (isSpinning || remainingSpins <= 0) return
        isSpinning = true

        val segmentDegree = 360f / WHEEL_SEGMENTS.size // 45 degrees
        // Target angle points to top (270 degrees)
        val targetDegree = 360f - (targetSegmentIndex * segmentDegree + segmentDegree / 2f)
        val fullRotations = (5 + Random.nextInt(3)) * 360f
        val currentMod = wheelRotation.value % 360f
        val finalRotation = wheelRotation.value + (360f - currentMod) + fullRotations + targetDegree

        scope.launch {
            wheelRotation.animateTo(
                targetValue = finalRotation,
                animationSpec = tween(
                    durationMillis = 4400,
                    easing = CubicBezierEasing(0.18f, 0.85f, 0.22f, 1f)
                )
            )
            isSpinning = false
            val chosen = WHEEL_SEGMENTS[targetSegmentIndex]
            resultSegment = chosen

            // Update stats
            val newSpinsToday = spinsUsedToday + 1
            spinsUsedToday = newSpinsToday
            val newConsecutive = consecutiveSpinCount + 1
            consecutiveSpinCount = newConsecutive

            prefs.edit()
                .putInt("spins_used_today", newSpinsToday)
                .putInt("consecutive_spins", newConsecutive)
                .apply()

            // 5th Spin Golden Scratch Card Milestone
            if (newConsecutive % 5 == 0 || chosen.isScratchCard) {
                scratchCardAmount = listOf(1, 2, 3, 5).random()
                showScratchCardDialog = true
            } else {
                if (chosen.coinValue > 0) {
                    val updatedCoins = totalDummyCoins + chosen.coinValue
                    totalDummyCoins = updatedCoins
                    prefs.edit().putInt("dummy_coins", updatedCoins).apply()
                } else if (chosen.cashValue > 0) {
                    val updatedCash = totalDummyCash + chosen.cashValue
                    totalDummyCash = updatedCash
                    prefs.edit().putInt("dummy_cash", updatedCash).apply()
                }
                showResultDialog = true
            }

            // Preload next ad
            loadRewardedAd()
        }
    }

    fun onSpinButtonClicked() {
        if (remainingSpins <= 0) {
            Toast.makeText(context, "Daily limit reached ($maxSpinsPerDay spins/day). Come back tomorrow!", Toast.LENGTH_SHORT).show()
            return
        }

        // Every single spin must and should watch an ad as required
        val activity = context as? Activity
        if (mRewardedAd != null && activity != null) {
            mRewardedAd?.show(activity) { _ ->
                // User completed watching rewarded video ad
                mRewardedAd = null
                val nextCount = consecutiveSpinCount + 1
                // Every 5th consecutive spin awards scratch card, otherwise coins or try again
                val target = if (nextCount % 5 == 0) {
                    5 // Lands on Golden Scratch segment!
                } else {
                    listOf(1, 3, 4, 6, 7).random() // Coins, Try Again, 2x
                }
                executeSpin(target)
            }
        } else {
            Toast.makeText(context, "Loading sponsor video ad... please tap again in 2 seconds", Toast.LENGTH_SHORT).show()
            loadRewardedAd()
        }
    }

    fun shareToWhatsAppToUnlock() {
        runCatching {
            val shareText = "🎉 Spin the wheel on DutyPe & win up to ₹100 daily!\nUse my referral code: $referralCode\nDownload now: https://dutype.in"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(intent)
        }.onFailure {
            val genericIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Spin the wheel on DutyPe! Download: https://dutype.in")
            }
            context.startActivity(Intent.createChooser(genericIntent, "Share Spin"))
        }
    }

    BackHandler { navController.popBackStack() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F172A)
                    )
                }

                // Balance Pills
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(text = "🪙 $totalDummyCoins Coins", color = Color(0xFFD97706), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(text = "💵 ₹$totalDummyCash Won", color = Color(0xFF059669), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Headline
            Text(
                text = "SPIN & WIN UP TO ₹100",
                color = Color(0xFF0F172A),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "చక్రం తిప్పండి - ఉచిత బహుమతులు గెలవండి",
                color = Color(0xFF64748B),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )

            // Prominent Disclaimer Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(0.8.dp, Color(0xFFBFDBFE))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "🎮 For Fun & Entertainment Timepass Only: DutyPe Coins are dummy game tokens. Spin for fun!",
                        color = Color(0xFF1E40AF),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 5th Spin Milestone Progress Bar
            val currentInFive = consecutiveSpinCount % 5
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFFBEB),
                border = BorderStroke(1.dp, Color(0xFFFDE68A))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎁 Golden Scratch Card (Every 5th Spin)",
                            color = Color(0xFFB45309),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$currentInFive / 5 Spins",
                            color = Color(0xFFD97706),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(currentInFive / 5f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444))))
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Wheel Container with Pointer Stopper
            Box(
                modifier = Modifier
                    .size(310.dp),
                contentAlignment = Alignment.Center
            ) {
                // The Rotating Canvas Wheel
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(wheelRotation.value)
                ) {
                    drawFortuneWheel(WHEEL_SEGMENTS)
                }

                // Outer Glowing Rim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(6.dp, Brush.sweepGradient(listOf(Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF3B82F6), Color(0xFFF59E0B))), CircleShape)
                )

                // Top Pointer Needle
                Canvas(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                ) {
                    val needlePath = Path().apply {
                        moveTo(size.width / 2f, size.height)
                        lineTo(0f, 0f)
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(needlePath, color = Color(0xFFEF4444))
                    drawPath(needlePath, color = Color.White, style = Stroke(width = 2.dp.toPx()))
                }

                // Center Spin Dial Button
                Surface(
                    modifier = Modifier
                        .size(76.dp)
                        .clickable(enabled = !isSpinning, onClick = ::onSpinButtonClicked),
                    shape = CircleShape,
                    color = if (isSpinning) Color(0xFF94A3B8) else Color(0xFFEF4444),
                    shadowElevation = 8.dp,
                    border = BorderStroke(3.dp, Color.White)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isSpinning) "..." else "SPIN",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Spin Status & Actions
            Text(
                text = "Remaining Spins Today: $remainingSpins / $maxSpinsPerDay (Watch Ad to Spin)",
                color = Color(0xFF64748B),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(14.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Watch Ad for 1 Spin Button
                Button(
                    onClick = ::onSpinButtonClicked,
                    enabled = !isSpinning && remainingSpins > 0,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        disabledContainerColor = Color(0xFF94A3B8)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text = "Watch Ad to Spin", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // WhatsApp Viral Unlock Button
                Button(
                    onClick = ::shareToWhatsAppToUnlock,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text = "Share & Unlock", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // Standard Result Dialog
    if (showResultDialog && resultSegment != null) {
        val seg = resultSegment!!
        Dialog(
            onDismissRequest = { showResultDialog = false },
            properties = DialogProperties(dismissOnClickOutside = true)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🎉 Congratulations!", color = Color(0xFFD97706), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (seg.coinValue > 0) "You won ${seg.coinValue} DutyPe Coins! 🪙" else if (seg.cashValue > 0) "You won ₹${seg.cashValue} Cash! 💵" else "Nice try! Better luck next spin! 🤞",
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { showResultDialog = false },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("Claim & Spin Again", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Golden Scratch Card Dialog (Every 5th Spin)
    if (showScratchCardDialog) {
        Dialog(
            onDismissRequest = { showScratchCardDialog = false },
            properties = DialogProperties(dismissOnClickOutside = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⭐ 5th SPIN GOLDEN REWARD ⭐",
                        color = Color(0xFFD97706),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Scratch the card to reveal your guaranteed prize!",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))

                    // Scratch Card Surface
                    var scratchProgress by remember { mutableFloatStateOf(0f) }
                    val revealed = scratchProgress >= 0.70f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A)))),
                        contentAlignment = Alignment.Center
                    ) {
                        // Revealed prize underneath
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "₹$scratchCardAmount CASH",
                                color = Color(0xFFB45309),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Direct Wallet Reward",
                                color = Color(0xFF92400E),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Scratching silver cover
                        if (!revealed) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = 1f - (scratchProgress * 1.2f).coerceIn(0f, 1f) }
                                    .background(Brush.linearGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8))))
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { scratchProgress += 0.15f },
                                            onDrag = { _, dragAmount ->
                                                val dist = abs(dragAmount.x) + abs(dragAmount.y)
                                                scratchProgress = (scratchProgress + dist / 800f).coerceIn(0f, 1f)
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "👉 Scratch Here with Finger 👈",
                                    color = Color(0xFF1E293B),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    if (revealed) {
                        Button(
                            onClick = {
                                val updatedCash = totalDummyCash + scratchCardAmount
                                totalDummyCash = updatedCash
                                prefs.edit().putInt("dummy_cash", updatedCash).apply()
                                showScratchCardDialog = false
                                Toast.makeText(context, "Added ₹$scratchCardAmount to your game wallet!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                        ) {
                            Text("Collect Reward & Continue", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                scratchProgress = 1f
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Tap to Reveal", color = Color(0xFF0284C7), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// Draw the 8-segment Fortune Wheel
private fun DrawScope.drawFortuneWheel(segments: List<WheelSegment>) {
    val totalSegments = segments.size
    val sweepAngle = 360f / totalSegments
    val radius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)

    for (i in segments.indices) {
        val startAngle = i * sweepAngle
        val segment = segments[i]

        // Segment Wedge
        drawArc(
            color = segment.color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Fill
        )

        // Segment Border
        drawArc(
            color = Color.White.copy(alpha = 0.5f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Text label drawn on native canvas along radial angle
        val midAngleRad = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())
        val textRadius = radius * 0.65f
        val textX = (center.x + textRadius * cos(midAngleRad)).toFloat()
        val textY = (center.y + textRadius * sin(midAngleRad)).toFloat()

        drawContext.canvas.nativeCanvas.apply {
            save()
            rotate((startAngle + sweepAngle / 2f + 90f), textX, textY)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 34f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawText(segment.label, textX, textY, paint)

            val subPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(220, 255, 255, 255)
                textSize = 20f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawText(segment.subLabel, textX, textY + 28f, subPaint)
            restore()
        }
    }
}
