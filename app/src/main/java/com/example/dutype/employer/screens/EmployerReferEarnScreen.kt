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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Share
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.example.dutype.components.CommonHeader
import com.example.dutype.components.ReferAndEarnRedemptionCard
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

    var showCopySuccess by remember { mutableStateOf(false) }
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

                    // Removed: QR Code section

                    // Referral Code Card
                    item {
                        EmployerReferralCodeCard(
                            referralCode = uiState.wallet?.referralCode.orEmpty(),
                            rewardAmount = referralConfig.rewardPerReferral.toInt(),
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

                    // Who referred you / Referral Code Redemption
                    item {
                        if (uiState.myReferrer != null) {
                            EmployerReferrerInfoCard(referrer = uiState.myReferrer)
                        } else {
                            ReferAndEarnRedemptionCard(
                                hasExistingReferrer = false,
                                onSuccess = { viewModel.loadReferralData() }
                            )
                        }
                    }

                    // Stats
                    item {
                        EmployerStatsCard(
                            totalReferrals = uiState.referralHistory.size,
                            successfulReferrals = uiState.wallet?.successfulReferrals ?: 0,
                            totalEarnings = (uiState.wallet?.lifetimeEarnedPaise ?: 0L) / 100.0,
                            availableBalance = balanceRupees,
                            signupBonusReceived = joinBonusPaise > 0L,
                            signupBonusAmount = joinBonusPaise / 100.0
                        )
                    }

                    // DutyPe Balance & Usage Card (Credits for job posts and services)
                    item {
                        EmployerCreditsUsageCard(
                            availableBalance = balanceRupees,
                            onPostJob = { navController.navigate(com.example.dutype.navigation.Routes.EMPLOYER_POST_JOB) },
                            onBookService = { navController.navigate(com.example.dutype.navigation.Routes.SERVICES) }
                        )
                    }

                    // How It Works
                    item {
                        EmployerHowItWorksCard()
                    }

                    // Referral History
                    item {
                        EmployerReferralHistoryCard(referralHistory = uiState.referralHistory)
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
}



// Removed: EmployerQRCodeCard function - QR code feature removed

@Composable
private fun EmployerReferralCodeCard(
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
        // Hero card - horizontal Row matching Pronto/UC standards
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
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
                        .background(Color(0xFFDCFCE7)),
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
                    Text(
                        text = stringResource(R.string.refer_hero_title, rewardAmount),
                        color = ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
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
            Text(
                text = stringResource(R.string.refer_code_label).uppercase(),
                color = mute,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(6.dp))
            SelectionContainer {
                Text(
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
                    Text(
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
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.refer_share_whatsapp),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 3-step explainer (Clean, minimal timeline)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top
            ) {
                EmployerReferStep(Icons.Outlined.Share, ink, stringResource(R.string.refer_step_share), slate)
                EmployerReferArrow(mute)
                EmployerReferStep(Icons.Outlined.PersonAdd, ink, stringResource(R.string.refer_step_joins), slate)
                EmployerReferArrow(mute)
                EmployerReferStep(Icons.Outlined.CurrencyRupee, green, stringResource(R.string.refer_step_each, rewardAmount), slate)
            }
        }
    }
}

@Composable
private fun EmployerReferStep(icon: ImageVector, iconTint: Color, label: String, labelColor: Color) {
    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = labelColor,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmployerReferArrow(color: Color) {
    Text(
        text = "→",
        color = color,
        fontSize = 16.sp,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, end = 4.dp)
    )
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

@Composable
private fun EmployerCreditsUsageCard(
    availableBalance: Double,
    onPostJob: () -> Unit,
    onBookService: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = com.example.dutype.ui.theme.EmployerColors.CardBackground,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = EmployerColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DutyPe Balance",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = EmployerColors.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "₹${availableBalance.toInt()}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "Referral earnings are credited to your DutyPe balance. Use your balance anytime to post jobs or book home services on DutyPe.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = EmployerColors.TextSecondary,
                    lineHeight = 17.sp
                )
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPostJob,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("⚡ Post Job", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onBookService,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, EmployerColors.Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🛠️ Services", color = EmployerColors.Primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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

data class EmployerReferralItem(
    val name: String,
    val date: String,
    val status: String,
    val earnings: Double
)


// ============================================
// V2.0 PROFESSIONAL FEATURES
// ============================================

