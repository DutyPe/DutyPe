package com.example.dutype.urgent

import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.dutype.app.R
import com.example.dutype.models.InstantRequest
import com.example.dutype.utils.LocaleHelper
import com.example.dutype.utils.findActivity
import com.example.dutype.di.rememberInAppReviewTriggerService
import java.util.Locale

private val Green = Color(0xFF16A34A)
private val Ink = Color(0xFF0F172A)
private val Muted = Color(0xFF64748B)
private val Urgent = Color(0xFFDC2626)
private val Page = Color(0xFFF8FAFC)

/**
 * The urgent offer, made for workers who may not read well: the job is read aloud in the app
 * language, the pay is the biggest thing on screen, and there are two big buttons.
 */
@Composable
fun UrgentOfferScreen(requestId: String, navController: NavController) {
    val viewModel: UrgentOfferViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(requestId) { viewModel.load(requestId) }
    val context = LocalContext.current
    val reviewTriggerService = rememberInAppReviewTriggerService()
    LaunchedEffect(Unit) { UrgentSoundAlertManager.stopSound() }
    val speaker = rememberSpeaker()
    val close: () -> Unit = { if (!navController.popBackStack()) navController.navigate(com.example.dutype.navigation.WorkerBottomRoutes.HOME) }
    val onSkip: () -> Unit = {
        UrgentSoundAlertManager.ignoreRequest(context, requestId)
        close()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Page.bg())
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        when (val s = state) {
            UrgentOfferState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Green.fg())
            is UrgentOfferState.Offer -> OfferContent(s, speaker, onAccept = viewModel::accept, onSkip = onSkip)
            is UrgentOfferState.Accepted -> {
                LaunchedEffect(Unit) {
                    context.findActivity()?.let { act -> reviewTriggerService.onWorkerJobApplication(act) }
                }
                AcceptedContent(s, speaker, onDone = close)
            }
            is UrgentOfferState.Unavailable -> MessageContent(s.reason, onDone = close)
            is UrgentOfferState.Failed -> FailedContent(s.message, onRetry = { viewModel.load(requestId) }, onDone = close)
        }
    }
}

/** Text-to-speech in the app language; silently does nothing when the phone has no such voice. */
private class Speaker(val tts: TextToSpeech?, val ready: Boolean) {
    fun say(text: String) {
        if (ready) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "urgent_offer")
    }
}

@Composable
private fun rememberSpeaker(): Speaker {
    val context = LocalContext.current
    var speaker by remember { mutableStateOf(Speaker(null, false)) }
    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            val tts = engine ?: return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) return@TextToSpeech
            val lang = LocaleHelper.getLanguage(context)
            val result = tts.setLanguage(Locale(lang, "IN"))
            val ok = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            speaker = Speaker(tts, ok)
        }
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }
    return speaker
}

private fun kmText(km: Double?): String? = km?.let { if (it < 1.0) "${(it * 1000).toInt()} m" else String.format(Locale.US, "%.1f km", it) }

@Composable
private fun OfferContent(s: UrgentOfferState.Offer, speaker: Speaker, onAccept: () -> Unit, onSkip: () -> Unit) {
    val r = s.request
    val pay = r.perPersonPayment.toInt()
    val left = (r.workersNeeded - r.selectedWorkerIds.size).coerceAtLeast(1)
    val distance = kmText(s.distanceKm)
    val spoken = stringResource(
        R.string.urgent_offer_spoken, r.title, pay, r.addressText,
        distance ?: stringResource(R.string.urgent_offer_nearby)
    )
    LaunchedEffect(speaker.ready, r.requestId) { speaker.say(spoken) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(Urgent.bg(), RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(stringResource(R.string.urgent_offer_badge), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { speaker.say(spoken) }, enabled = speaker.ready) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Green.fg())
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.urgent_offer_listen), color = Green.fg(), fontWeight = FontWeight.SemiBold)
                }
            }
            Text(r.title, color = Ink.fg(), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 32.sp)
            if (r.employerName.isNotBlank()) Text(r.employerName, color = Muted.fg(), fontSize = 15.sp)
            Column {
                Text("₹$pay", color = Green.fg(), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.urgent_offer_per_person), color = Muted.fg(), fontSize = 15.sp)
            }
            InfoRow(Icons.Filled.LocationOn, listOfNotNull(distance?.let { stringResource(R.string.urgent_offer_away, it) }, r.addressText).joinToString(" · "))
            InfoRow(Icons.Filled.Groups, stringResource(R.string.urgent_offer_places_left, left, r.workersNeeded))
            val until = r.expiresAt.takeIf { it > 0 }?.let {
                java.text.SimpleDateFormat("h:mm a, d MMM", Locale.ENGLISH).format(java.util.Date(it))
            }
            if (until != null) InfoRow(Icons.Filled.Schedule, stringResource(R.string.urgent_offer_until, until))
            if (r.durationText.isNotBlank()) InfoRow(Icons.Filled.Schedule, r.durationText)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.bg())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onAccept,
                enabled = !s.accepting,
                colors = ButtonDefaults.buttonColors(containerColor = Green.bg(), contentColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                if (s.accepting) CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(26.dp))
                else Text(stringResource(R.string.urgent_offer_accept_big), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
            OutlinedButton(
                onClick = onSkip,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(stringResource(R.string.urgent_offer_skip), fontSize = 17.sp, color = Muted.fg(), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    if (text.isBlank()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Muted.fg(), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.size(10.dp))
        Text(text, color = Ink.fg(), fontSize = 17.sp)
    }
}

@Composable
private fun AcceptedContent(s: UrgentOfferState.Accepted, speaker: Speaker, onDone: () -> Unit) {
    val context = LocalContext.current
    val spoken = stringResource(R.string.urgent_offer_yours_spoken, s.result.addressText)
    LaunchedEffect(speaker.ready) { speaker.say(spoken) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.size(96.dp).background(Color(0xFFDCFCE7).bg(), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Green.fg(), modifier = Modifier.size(64.dp))
        }
        Text(stringResource(R.string.urgent_offer_yours_title), color = Ink.fg(), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text(s.result.title, color = Muted.fg(), fontSize = 17.sp, textAlign = TextAlign.Center)
        if (s.result.addressText.isNotBlank()) {
            InfoRow(Icons.Filled.LocationOn, s.result.addressText)
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (s.result.contactNumber.isNotBlank()) {
            Button(
                onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+91${s.result.contactNumber.takeLast(10)}"))) } },
                colors = ButtonDefaults.buttonColors(containerColor = Green.bg(), contentColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.size(10.dp))
                Text(stringResource(R.string.urgent_offer_call_employer), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        if (com.example.dutype.utils.GeoUtils.hasValidCoordinates(s.result.lat, s.result.lng)) {
            OutlinedButton(
                onClick = {
                    val uri = Uri.parse("geo:${s.result.lat},${s.result.lng}?q=${s.result.lat},${s.result.lng}(${Uri.encode(s.result.title)})")
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Filled.Directions, contentDescription = null, tint = Ink.fg())
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.urgent_offer_directions), fontSize = 17.sp, color = Ink.fg(), fontWeight = FontWeight.SemiBold)
            }
        }
        TextButton(onClick = onDone) { Text(stringResource(R.string.urgent_offer_done), fontSize = 16.sp, color = Muted.fg()) }
    }
}

@Composable
private fun MessageContent(reason: UrgentOffers.Result, onDone: () -> Unit) {
    val (title, body) = when (reason) {
        UrgentOffers.Result.Filled -> stringResource(R.string.urgent_offer_filled_title) to stringResource(R.string.urgent_offer_filled_body)
        UrgentOffers.Result.Busy -> stringResource(R.string.urgent_offer_busy_title) to stringResource(R.string.urgent_offer_busy_body)
        else -> stringResource(R.string.urgent_offer_closed_title) to stringResource(R.string.urgent_offer_closed_body)
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text(title, color = Ink.fg(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text(body, color = Muted.fg(), fontSize = 17.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = Ink.bg(), contentColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text(stringResource(R.string.urgent_offer_done), fontSize = 17.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun FailedContent(message: String, onRetry: () -> Unit, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text(message, color = Ink.fg(), fontSize = 18.sp, textAlign = TextAlign.Center)
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Green.bg(), contentColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text(stringResource(R.string.urgent_offer_try_again), fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        TextButton(onClick = onDone) { Text(stringResource(R.string.urgent_offer_done), color = Muted.fg()) }
    }
}
