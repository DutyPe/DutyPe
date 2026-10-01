package com.example.dutype.employer.ai

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dutype.app.R
import com.example.dutype.utils.LocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class AiPostUiState(
    val draft: AiJobDraft = AiJobDraft(),
    val heard: List<String> = emptyList(),
    val question: String = "",
    val summary: String = "",
    val ready: Boolean = false,
    val thinking: Boolean = false,
    val error: String? = null,
    /** Available workers with the job's skill within 5 / 10 km (null until known). */
    val nearby: Pair<Int, Int>? = null
)

@HiltViewModel
class AiPostViewModel @Inject constructor(private val service: AiHiringService) : ViewModel() {
    private val _state = MutableStateFlow(AiPostUiState())
    val state: StateFlow<AiPostUiState> = _state.asStateFlow()
    private var nearbyKey = ""

    fun start(firstQuestion: String) = _state.update { AiPostUiState(question = firstQuestion, nearby = it.nearby) }

    /** One thing the employer said (or typed): merged into the draft by the server. */
    fun said(text: String, onQuestion: (String) -> Unit) {
        val clean = text.trim()
        if (clean.isEmpty() || _state.value.thinking) return
        _state.update { it.copy(heard = it.heard + clean, thinking = true, error = null) }
        viewModelScope.launch {
            service.assistant(clean, _state.value.draft).fold(
                onSuccess = { turn ->
                    _state.update { it.copy(draft = turn.draft, question = turn.question, summary = turn.summary, ready = turn.ready, thinking = false) }
                    onQuestion(turn.question)
                },
                onFailure = { e -> _state.update { it.copy(thinking = false, error = e.message) } }
            )
        }
    }

    fun loadNearby(lat: Double, lng: Double, category: String) {
        if (lat == 0.0 && lng == 0.0) return
        val key = "%.3f|%.3f|%s".format(lat, lng, category)
        if (key == nearbyKey) return
        nearbyKey = key
        viewModelScope.launch {
            service.nearbyWorkers(lat, lng, category).onSuccess { n -> _state.update { it.copy(nearby = n) } }
        }
    }
}

private val Purple = Color(0xFF6D28D9)
private val Ink = Color(0xFF0F172A)
private val Muted = Color(0xFF64748B)

/** Top of the regular job form: talk-to-post entry and how many workers are available nearby. */
@Composable
fun AiPostBanner(lat: Double, lng: Double, category: String, onTalk: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: AiPostViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(lat, lng, category) { viewModel.loadNearby(lat, lng, category) }
    val shape = RoundedCornerShape(14.dp)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAF5FF).bg(), shape)
                .border(1.dp, Color(0xFFE9D5FF).bd(), shape)
                .clickable(onClick = onTalk)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(40.dp).background(Purple.bg(), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Mic, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.ai_post_banner_title), color = Ink.fg(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(stringResource(R.string.ai_post_banner_body), color = Muted.fg(), fontSize = 12.sp)
            }
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Purple.fg())
        }
        state.nearby?.let { (within5, within10) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Groups, contentDescription = null, tint = Color(0xFF16A34A).fg(), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (within10 > 0) stringResource(R.string.ai_nearby_workers, within5, within10) else stringResource(R.string.ai_nearby_none),
                    color = Ink.fg(), fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Talk-to-post: speak (or type) the job; the AI fills a draft, reads its next question aloud and
 * shows what it understood. "Fill the form" hands the draft to the normal form — the employer
 * checks it there and taps Post, so nothing is posted without them seeing it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPostAssistantSheet(onDismiss: () -> Unit, onApply: (AiJobDraft) -> Unit) {
    val context = LocalContext.current
    val viewModel: AiPostViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lang = LocaleHelper.getLanguage(context)
    val firstQuestion = stringResource(R.string.ai_post_first_question)
    var typed by remember { mutableStateOf("") }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale(lang, "IN")
                tts = engine
            }
        }
        onDispose { engine?.stop(); engine?.shutdown() }
    }
    val speak: (String) -> Unit = { text -> if (text.isNotBlank()) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ai_post") }
    LaunchedEffect(Unit) { viewModel.start(firstQuestion) }
    LaunchedEffect(tts) { if (tts != null && state.heard.isEmpty()) speak(firstQuestion) }

    // The phone's own speech recogniser (no microphone permission needed by the app).
    val listen = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { heard ->
                viewModel.said(heard, speak)
            }
        }
    }
    val startListening = {
        tts?.stop()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "$lang-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, state.question)
        }
        runCatching { listen.launch(intent) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White.bg()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Purple.fg())
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.ai_post_sheet_title), color = Ink.fg(), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(state.question, color = Purple.fg(), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            state.heard.takeLast(3).forEach { Text("“$it”", color = Muted.fg(), fontSize = 14.sp) }
            if (state.thinking) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Purple.fg(), strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.ai_post_thinking), color = Muted.fg(), fontSize = 13.sp)
                }
            }
            state.error?.let { Text(stringResource(R.string.ai_post_error), color = Color(0xFFDC2626).fg(), fontSize = 13.sp) }
            if (state.draft.title.isNotBlank()) DraftCard(state.draft, state.summary)

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(if (state.thinking) Color(0xFFC4B5FD).bg() else Purple.bg(), CircleShape)
                        .clickable(enabled = !state.thinking) { startListening() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.ai_post_tap_to_speak), tint = Color.White, modifier = Modifier.size(36.dp)) }
            }
            Text(stringResource(R.string.ai_post_tap_to_speak), color = Muted.fg(), fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it },
                placeholder = { Text(stringResource(R.string.ai_post_type_hint)) },
                trailingIcon = {
                    IconButton(onClick = { viewModel.said(typed, speak); typed = "" }, enabled = typed.isNotBlank() && !state.thinking) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Purple.fg())
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onApply(state.draft) },
                enabled = state.ready && !state.thinking,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A).bg(), contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text(stringResource(R.string.ai_post_fill_form), fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            Text(stringResource(R.string.ai_post_check_note), color = Muted.fg(), fontSize = 12.sp)
        }
    }
}

@Composable
private fun DraftCard(d: AiJobDraft, summary: String) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC).bg(), shape)
            .border(1.dp, Color(0xFFE2E8F0).bd(), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(summary, color = Ink.fg(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        val rows = listOf(
            d.category.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
            d.employmentType.replace('_', ' ').lowercase(),
            d.shift.lowercase() + " shift",
            d.experience,
            d.education,
            d.gender.takeIf { it != "Both" }.orEmpty(),
            d.perks.joinToString(", ")
        ).filter { it.isNotBlank() }
        Text(rows.joinToString(" · "), color = Muted.fg(), fontSize = 12.sp)
        if (d.description.isNotBlank()) Text(d.description, color = Ink.fg(), fontSize = 13.sp)
    }
}
