package com.example.dutype.employer.components

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.dutype.app.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.dutype.employer.voice.VoiceJobParsedData
import com.example.dutype.employer.voice.VoiceJobPostingManager
import com.example.dutype.employer.voice.VoiceLanguage
import com.example.dutype.employer.voice.VoicePostingState
import com.example.dutype.models.QuickUrgentNeedInput

private val VoicePrimaryNavy = Color(0xFF0F172A)
private val VoiceEmerald = Color(0xFF10B981)
private val VoiceSurfaceBg = Color(0xFFF8FAFC)
private val VoiceBorder = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceJobPostingBottomSheet(
    onDismiss: () -> Unit,
    onPostUrgentJob: (QuickUrgentNeedInput) -> Unit,
    onEditManually: (QuickUrgentNeedInput) -> Unit = {},
    initialLanguage: VoiceLanguage = VoiceLanguage.TELUGU,
    employerPhone: String = "",
    defaultAddress: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val manager = remember {
        VoiceJobPostingManager(context, scope).apply {
            setLanguage(initialLanguage)
        }
    }

    var currentLanguage by remember { mutableStateOf(initialLanguage) }

    DisposableEffect(Unit) {
        onDispose {
            manager.destroy()
        }
    }

    val state by manager.state.collectAsState()
    val transcript by manager.partialTranscript.collectAsState()
    val soundLevel by manager.soundLevel.collectAsState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            manager.startListening(currentLanguage)
        }
    }

    LaunchedEffect(Unit) {
        if (hasAudioPermission) {
            manager.startListening(currentLanguage)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            manager.stopSpeaking()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color.White.bg(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFCBD5E1).bg())
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.voice_speak_to_post_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoicePrimaryNavy.fg()
                    )
                    Text(
                        text = stringResource(R.string.voice_speak_to_post_subtitle),
                        fontSize = 12.sp,
                        color = Color(0xFF64748B).fg()
                    )
                }
                IconButton(
                    onClick = {
                        manager.stopSpeaking()
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B).fg()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Language Selector Chips (Telugu, English, Hindi)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VoiceLanguage.values().forEach { lang ->
                    val isSelected = currentLanguage == lang
                    Surface(
                        onClick = {
                            if (currentLanguage != lang) {
                                currentLanguage = lang
                                manager.setLanguage(lang)
                                if (hasAudioPermission) {
                                    manager.startListening(lang)
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) VoiceEmerald.bg().copy(alpha = 0.12f) else Color(0xFFF1F5F9).bg(),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) VoiceEmerald.fg() else Color(0xFFE2E8F0).fg()
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${lang.nativeName} (${lang.englishName})",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) VoiceEmerald.fg() else Color(0xFF475569).fg()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!hasAudioPermission) {
                // Audio permission needed card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = VoiceSurfaceBg.bg()),
                    border = BorderStroke(1.dp, VoiceBorder.bd())
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            contentDescription = null,
                            tint = Color(0xFFEF4444).fg(),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.voice_mic_permission_required),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = VoicePrimaryNavy.fg()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.voice_mic_permission_desc),
                            fontSize = 13.sp,
                            color = Color(0xFF64748B).fg(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VoicePrimaryNavy.bg()),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(stringResource(R.string.voice_grant_permission))
                        }
                    }
                }
            } else {
                when (val current = state) {
                    is VoicePostingState.Idle, is VoicePostingState.Listening -> {
                        val isListening = current is VoicePostingState.Listening
                        val animatedSize by animateFloatAsState(
                            targetValue = if (isListening) (64f + soundLevel * 20f) else 64f,
                            animationSpec = tween(150),
                            label = "micPulse"
                        )

                        // Pulsing Mic Icon
                        Box(
                            modifier = Modifier
                                .size(animatedSize.dp)
                                .clip(CircleShape)
                                .background(if (isListening) VoiceEmerald.bg().copy(alpha = 0.15f) else VoiceSurfaceBg.bg())
                                .clickable {
                                    if (isListening) manager.stopListening() else manager.startListening()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(if (isListening) VoiceEmerald.bg() else VoicePrimaryNavy.bg()),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Microphone",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val listeningPrompt = when (currentLanguage) {
                            VoiceLanguage.TELUGU -> "Listening... మాట్లాడండి"
                            VoiceLanguage.HINDI -> "Listening... बोलना शुरू करें"
                            VoiceLanguage.ENGLISH -> "Listening... Start speaking"
                        }

                        Text(
                            text = if (isListening) listeningPrompt else stringResource(R.string.voice_tap_mic_to_speak),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isListening) VoiceEmerald.fg() else VoicePrimaryNavy.fg()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = currentLanguage.sampleHint,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B).fg(),
                            textAlign = TextAlign.Center
                        )

                        if (transcript.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = VoiceSurfaceBg.bg()),
                                border = BorderStroke(1.dp, VoiceBorder.bd())
                            ) {
                                Text(
                                    text = "\"$transcript\"",
                                    fontSize = 14.sp,
                                    color = VoicePrimaryNavy.fg(),
                                    modifier = Modifier.padding(14.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    is VoicePostingState.Processing -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(VoiceSurfaceBg.bg()),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = VoicePrimaryNavy.fg(),
                                strokeWidth = 3.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.voice_understanding_details),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VoicePrimaryNavy.fg()
                        )
                        Text(
                            text = stringResource(R.string.voice_finding_role_workers_wage),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B).fg()
                        )
                    }

                    is VoicePostingState.Clarifying -> {
                        val isListening = current.isListening
                        val isSpeaking = current.isSpeakingQuestion
                        val animatedMicSize by animateFloatAsState(
                            targetValue = if (isListening) (58f + soundLevel * 16f) else 52f,
                            animationSpec = tween(150),
                            label = "clarifyingMicPulse"
                        )

                        // Clarification Question Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = VoiceSurfaceBg.bg()),
                            border = BorderStroke(1.dp, VoiceBorder.bd())
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Badges showing what was already recognized
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = VoiceEmerald.bg().copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "✓ ${current.parsedData.category}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = VoiceEmerald.fg(),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = VoiceEmerald.bg().copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "✓ ${current.parsedData.workersNeeded} Workers",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = VoiceEmerald.fg(),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Clarification question
                                Text(
                                    text = current.question.ifBlank { "Aap per day kitna payment denge?" },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VoicePrimaryNavy.fg()
                                )

                                if (current.hintMessage.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = current.hintMessage,
                                        fontSize = 12.sp,
                                        color = Color(0xFFD97706).fg(),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Quick-select wage chips
                                Text(
                                    text = stringResource(R.string.voice_tap_wage_or_speak),
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B).fg()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(500.0, 600.0, 700.0, 800.0, 1000.0).forEach { amount ->
                                        OutlinedButton(
                                            onClick = { manager.updateWageDirectly(amount) },
                                            shape = RoundedCornerShape(18.dp),
                                            border = BorderStroke(1.dp, Color(0xFFCBD5E1).bd()),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "₹${amount.toInt()}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VoicePrimaryNavy.fg()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Active listening / mic section for speaking the answer
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(animatedMicSize.dp)
                                    .clip(CircleShape)
                                    .background(if (isListening) VoiceEmerald.bg().copy(alpha = 0.18f) else VoiceSurfaceBg.bg())
                                    .clickable {
                                        if (isListening) manager.stopListening() else manager.startListening()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(if (isListening) VoiceEmerald.bg() else VoicePrimaryNavy.bg()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Microphone",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val answerPrompt = when {
                                isSpeaking -> "Asking... వినండి / सुनिए"
                                isListening -> "Listening... Speak wage (e.g. 800)"
                                else -> stringResource(R.string.voice_tap_mic_to_speak_wage)
                            }

                            Text(
                                text = answerPrompt,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isListening) VoiceEmerald.fg() else VoicePrimaryNavy.fg()
                            )

                            if (transcript.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "\"$transcript\"",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = VoicePrimaryNavy.fg()
                                )
                            }
                        }
                    }

                    is VoicePostingState.ReadyToPost -> {
                        val parsed = current.parsedData

                        // Summary Confirmation Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = VoiceSurfaceBg.bg()),
                            border = BorderStroke(1.dp, VoiceBorder.bd())
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = parsed.title,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VoicePrimaryNavy.fg()
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFDC2626).bg().copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.voice_urgent_job_badge),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626).fg(),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = stringResource(R.string.voice_workers_count_label), fontSize = 11.sp, color = Color(0xFF94A3B8).fg())
                                        Text(
                                            text = "${parsed.workersNeeded} Workers",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = VoicePrimaryNavy.fg()
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = stringResource(R.string.voice_daily_wage_label), fontSize = 11.sp, color = Color(0xFF94A3B8).fg())
                                        Text(
                                            text = parsed.budgetText.ifBlank { "₹${parsed.perPersonPayment.toInt()} / Day" },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = VoiceEmerald.fg()
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = stringResource(R.string.voice_duration_label), fontSize = 11.sp, color = Color(0xFF94A3B8).fg())
                                        Text(
                                            text = parsed.durationText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = VoicePrimaryNavy.fg()
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Primary CTA: "Post Urgent Job Now" (Tier 2/3 friendly as requested by user)
                        Button(
                            onClick = {
                                manager.stopSpeaking()
                                val input = manager.toQuickUrgentNeedInput(employerPhone, defaultAddress)
                                onPostUrgentJob(input)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VoicePrimaryNavy.bg()),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.voice_post_urgent_job_now),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Secondary: Edit Details Manually
                        TextButton(
                            onClick = {
                                manager.stopSpeaking()
                                val input = manager.toQuickUrgentNeedInput(employerPhone, defaultAddress)
                                onEditManually(input)
                                onDismiss()
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.voice_edit_details_manually),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B).fg()
                            )
                        }
                    }

                    is VoicePostingState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2).bg()),
                            border = BorderStroke(1.dp, Color(0xFFFECACA).bd())
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = current.message,
                                    fontSize = 13.sp,
                                    color = Color(0xFF991B1B).fg(),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { manager.startListening() },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VoicePrimaryNavy.bg())
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.voice_try_speaking_again))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Minimal Trigger Card for Voice Job Posting ("Bol Kar Post Karein")
 * Placed on Home Screen and Post Urgent Need Screen
 */
@Composable
fun VoiceJobTriggerCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.bg()),
        border = BorderStroke(1.dp, VoiceBorder.bd()),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(VoiceEmerald.bg().copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Speak to post",
                    tint = VoiceEmerald.fg(),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.voice_card_trigger_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoicePrimaryNavy.fg()
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = VoiceEmerald.bg().copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = stringResource(R.string.voice_card_trigger_badge),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VoiceEmerald.fg(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.voice_card_trigger_subtitle),
                    fontSize = 12.sp,
                    color = Color(0xFF64748B).fg(),
                    maxLines = 1
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = VoicePrimaryNavy.bg(),
                modifier = Modifier.padding(start = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.voice_card_trigger_speak_btn),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceLanguagePickerBottomSheet(
    onDismiss: () -> Unit,
    onLanguageSelected: (VoiceLanguage) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White.bg(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFCBD5E1).bg())
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.voice_select_language_title),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoicePrimaryNavy.fg()
                    )
                    Text(
                        text = stringResource(R.string.voice_select_language_subtitle),
                        fontSize = 12.sp,
                        color = Color(0xFF64748B).fg()
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B).fg(),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            VoiceLanguage.values().forEach { lang ->
                val badgeCode = when (lang) {
                    VoiceLanguage.TELUGU -> "TE"
                    VoiceLanguage.ENGLISH -> "EN"
                    VoiceLanguage.HINDI -> "HI"
                }

                Surface(
                    onClick = {
                        onLanguageSelected(lang)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC).bg(),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0).bd()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(VoiceEmerald.bg().copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = badgeCode,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoiceEmerald.fg()
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = lang.nativeName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = VoicePrimaryNavy.fg()
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "(${lang.englishName})",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B).fg()
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8).fg(),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
