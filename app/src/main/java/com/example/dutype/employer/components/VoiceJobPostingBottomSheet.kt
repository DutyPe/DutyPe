package com.example.dutype.employer.components

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
    employerPhone: String = "",
    defaultAddress: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val manager = remember {
        VoiceJobPostingManager(context, scope)
    }

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
            manager.startListening()
        }
    }

    LaunchedEffect(Unit) {
        if (hasAudioPermission) {
            manager.startListening()
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
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFCBD5E1))
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
                        text = "Speak to Post (बोलकर पोस्ट करें)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoicePrimaryNavy
                    )
                    Text(
                        text = "Hindi, Telugu or English",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
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
                        tint = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!hasAudioPermission) {
                // Audio permission needed card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = VoiceSurfaceBg),
                    border = BorderStroke(1.dp, VoiceBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Microphone Permission Required",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = VoicePrimaryNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To speak and post urgent jobs, DutyPe needs microphone access.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VoicePrimaryNavy),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Grant Permission")
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
                                .background(if (isListening) VoiceEmerald.copy(alpha = 0.15f) else VoiceSurfaceBg)
                                .clickable {
                                    if (isListening) manager.stopListening() else manager.startListening()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(if (isListening) VoiceEmerald else VoicePrimaryNavy),
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

                        Text(
                            text = if (isListening) "Listening... Bolna shuru karein" else "Tap mic to speak",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isListening) VoiceEmerald else VoicePrimaryNavy
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "e.g. \"Kal subah 9 baje 2 helper chahiye, 700 rupaye denge\"",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )

                        if (transcript.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = VoiceSurfaceBg),
                                border = BorderStroke(1.dp, VoiceBorder)
                            ) {
                                Text(
                                    text = "\"$transcript\"",
                                    fontSize = 14.sp,
                                    color = VoicePrimaryNavy,
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
                                .background(VoiceSurfaceBg),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = VoicePrimaryNavy,
                                strokeWidth = 3.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Understanding job details...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VoicePrimaryNavy
                        )
                        Text(
                            text = "Finding role, workers and wage",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    is VoicePostingState.Clarifying -> {
                        // Clarification Question (e.g. missing wage)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = VoiceSurfaceBg),
                            border = BorderStroke(1.dp, VoiceBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = VoiceEmerald.copy(alpha = 0.12f),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Text(
                                            text = "✓ ${current.parsedData.category}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = VoiceEmerald,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = VoiceEmerald.copy(alpha = 0.12f),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Text(
                                            text = "✓ ${current.parsedData.workersNeeded} Workers",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = VoiceEmerald,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = current.question.ifBlank { "Aap per day kitna payment denge?" },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VoicePrimaryNavy
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Quick-select wage chips
                                Text(
                                    text = "Select wage or tap mic to speak:",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(500.0, 700.0, 800.0, 1000.0).forEach { amount ->
                                        OutlinedButton(
                                            onClick = { manager.updateWageDirectly(amount) },
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = "₹${amount.toInt()}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VoicePrimaryNavy
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Mic button to answer
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = { manager.startListening() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(VoicePrimaryNavy)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Speak Wage",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "Tap mic to speak answer",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    is VoicePostingState.ReadyToPost -> {
                        val parsed = current.parsedData

                        // Summary Confirmation Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = VoiceSurfaceBg),
                            border = BorderStroke(1.dp, VoiceBorder)
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
                                        color = VoicePrimaryNavy
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFDC2626).copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = "⚡ Urgent Job",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626),
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
                                        Text(text = "Workers", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                        Text(
                                            text = "${parsed.workersNeeded} Workers",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = VoicePrimaryNavy
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Daily Wage", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                        Text(
                                            text = parsed.budgetText.ifBlank { "₹${parsed.perPersonPayment.toInt()} / Day" },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = VoiceEmerald
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Duration", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                        Text(
                                            text = parsed.durationText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = VoicePrimaryNavy
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
                            colors = ButtonDefaults.buttonColors(containerColor = VoicePrimaryNavy),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                text = "Post Urgent Job Now",
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
                                text = "Edit Details Manually",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    is VoicePostingState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            border = BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = current.message,
                                    fontSize = 13.sp,
                                    color = Color(0xFF991B1B),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { manager.startListening() },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VoicePrimaryNavy)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Try Speaking Again")
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, VoiceBorder),
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
                    .background(VoiceEmerald.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Speak to post",
                    tint = VoiceEmerald,
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
                        text = "Bol Kar Post Karein",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoicePrimaryNavy
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = VoiceEmerald.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "AI Voice",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VoiceEmerald,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Speak in Hindi, Telugu or English · e.g. \"2 helper kal subah, ₹700\"",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = VoicePrimaryNavy,
                modifier = Modifier.padding(start = 6.dp)
            ) {
                Text(
                    text = "Speak",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
