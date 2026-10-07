package com.example.dutype.employer.ai

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import kotlinx.coroutines.tasks.await
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.ui.graphics.Brush
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.applications.ApplicationRepository
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.jobs.JobForm
import com.example.dutype.jobs.JobRepository
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.QuickUrgentNeedInput
import com.example.dutype.profile.CurrentProfileStore
import com.example.dutype.services.InstantHelpService
import com.example.dutype.utils.AreaText
import com.example.dutype.utils.LocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiMessage(val fromAi: Boolean, val text: String)

data class DutyPeAiUiState(
    val messages: List<AiMessage> = emptyList(),
    val pending: AiAction? = null,
    val thinking: Boolean = false,
    val locked: String? = null,
    /** Set after a confirmed "open job": the screen navigates and clears it. */
    val openJobId: String? = null,
    /** Set after a confirmed "book a home service" / "open booking": a root route to open. */
    val openRoute: String? = null,
    val stats: AiStats? = null,
    /** Not signed in: the screen shows the log-in card instead of the chat. */
    val needsLogin: Boolean = false
)

@HiltViewModel
class DutyPeAiViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val service: AiHiringService,
    private val jobs: JobRepository,
    private val applications: ApplicationRepository,
    private val urgent: InstantHelpService,
    private val profileStore: CurrentProfileStore
) : ViewModel() {
    private val _state = MutableStateFlow(DutyPeAiUiState())
    val state: StateFlow<DutyPeAiUiState> = _state.asStateFlow()
    val subscription = profileStore.subscription
    val employer = profileStore.employer

    init {
        profileStore.start(Values.Role.EMPLOYER)
        refreshLogin()
    }

    /** Call on open and after logging in from the screen. */
    fun refreshLogin() = _state.update { it.copy(needsLogin = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) }

    /** One call; on "not an employer" the login token may predate the role claim, so refresh it once and retry. */
    private suspend fun ask(text: String, history: List<Pair<Boolean, String>>): Result<DutyPeAiTurn> {
        val first = service.dutypeAi(text, history)
        val code = (first.exceptionOrNull() as? com.google.firebase.functions.FirebaseFunctionsException)?.code
        if (code != com.google.firebase.functions.FirebaseFunctionsException.Code.PERMISSION_DENIED) return first
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return first
        runCatching { user.getIdToken(true).await() }
        return service.dutypeAi(text, history)
    }

    /** What went wrong, in words the employer can act on. */
    private fun messageFor(error: Throwable): String {
        val code = (error as? com.google.firebase.functions.FirebaseFunctionsException)?.code
        return when (code) {
            com.google.firebase.functions.FirebaseFunctionsException.Code.UNAUTHENTICATED -> {
                _state.update { it.copy(needsLogin = true) }
                context.getString(R.string.dutype_ai_login_body)
            }
            com.google.firebase.functions.FirebaseFunctionsException.Code.PERMISSION_DENIED ->
                context.getString(R.string.dutype_ai_employer_only)
            com.google.firebase.functions.FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                context.getString(R.string.auth_app_not_verified)
            com.google.firebase.functions.FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                context.getString(R.string.dutype_ai_busy)
            else -> context.getString(R.string.dutype_ai_error)
        }
    }

    fun greet(text: String) {
        if (_state.value.messages.isEmpty()) _state.update { it.copy(messages = listOf(AiMessage(true, text))) }
    }

    fun send(text: String, speak: (String) -> Unit) {
        val clean = text.trim()
        if (clean.isEmpty() || _state.value.thinking) return
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
            _state.update { it.copy(needsLogin = true) }
            return
        }
        val history = _state.value.messages.map { it.fromAi to it.text }
        _state.update { it.copy(messages = it.messages + AiMessage(false, clean), pending = null, thinking = true) }
        viewModelScope.launch {
            ask(clean, history).fold(
                onSuccess = { turn ->
                    _state.update {
                        it.copy(
                            messages = it.messages + AiMessage(true, turn.reply), pending = turn.action, locked = turn.locked,
                            thinking = false, stats = turn.stats ?: it.stats
                        )
                    }
                    speak(turn.reply)
                },
                onFailure = { error ->
                    timber.log.Timber.w(error, "DutyPe AI call failed")
                    val msg = messageFor(error)
                    _state.update { s -> s.copy(messages = s.messages + AiMessage(true, msg), thinking = false) }
                    speak(msg)
                }
            )
        }
    }

    fun cancel() = _state.update { it.copy(pending = null, messages = it.messages + AiMessage(true, context.getString(R.string.dutype_ai_cancelled))) }

    fun openedJob() = _state.update { it.copy(openJobId = null) }

    fun openedRoute() = _state.update { it.copy(openRoute = null) }

    /** The employer said yes: run the action through the normal secured calls. */
    fun confirm(speak: (String) -> Unit) {
        val action = _state.value.pending ?: return
        _state.update { it.copy(pending = null, thinking = true) }
        viewModelScope.launch {
            val result: Result<String> = runCatching { run(action) }
            val text = result.getOrElse { e ->
                if (e is com.google.firebase.functions.FirebaseFunctionsException) {
                    // The server's own message (e.g. "Pay can be at most â‚¹50,000") is clear; else a plain one.
                    e.message?.takeIf { it.isNotBlank() && e.code == com.google.firebase.functions.FirebaseFunctionsException.Code.INVALID_ARGUMENT
                        || e.code == com.google.firebase.functions.FirebaseFunctionsException.Code.FAILED_PRECONDITION } ?: messageFor(e)
                } else e.message ?: context.getString(R.string.dutype_ai_error)
            }
            _state.update { it.copy(messages = it.messages + AiMessage(true, text), thinking = false) }
            speak(text)
        }
    }

    private fun arg(a: AiAction, key: String): String = a.args[key]?.toString().orEmpty()
    private fun num(a: AiAction, key: String): Number = (a.args[key] as? Number) ?: 0

    private suspend fun run(a: AiAction): String = when (a.type) {
        "post_job" -> {
            val p = employer.value ?: error(context.getString(R.string.dutype_ai_need_profile))
            if (p.lat == 0.0 && p.lng == 0.0) error(context.getString(R.string.dutype_ai_need_location))
            val form = JobForm(
                title = arg(a, "title"),
                category = arg(a, "category"),
                employmentType = arg(a, "employmentType"),
                payAmount = num(a, "payAmount").toLong(),
                payType = arg(a, "payType"),
                vacancies = num(a, "vacancies").toInt().coerceIn(1, 50),
                urgency = Values.Urgency.NORMAL,
                shift = arg(a, "shift"),
                area = p.area.ifBlank { AreaText.from(p.address) },
                lat = p.lat,
                lng = p.lng,
                photoUrl = null,
                description = arg(a, "description").ifBlank { arg(a, "title") },
                addressText = p.address.ifBlank { p.area },
                contactNumber = p.phone.filter(Char::isDigit).takeLast(10),
                gender = when (arg(a, "gender")) { "Male" -> "MALE"; "Female" -> "FEMALE"; else -> "ANY" },
                experienceRequired = arg(a, "experience"),
                educationRequired = arg(a, "education"),
                benefits = (a.args["perks"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
            )
            jobs.postJob(form).getOrThrow()
            context.getString(R.string.dutype_ai_done_posted)
        }
        "post_urgent" -> {
            val p = employer.value ?: error(context.getString(R.string.dutype_ai_need_profile))
            val input = QuickUrgentNeedInput(
                title = arg(a, "title"),
                category = arg(a, "category"),
                workersNeeded = num(a, "workersNeeded").toInt(),
                perPersonPayment = num(a, "payPerPerson").toDouble(),
                urgencyType = arg(a, "window").ifBlank { "right_now" },
                durationText = arg(a, "durationText"),
                addressText = p.address,
                contactNumber = p.phone
            )
            urgent.createUrgentNeed(input, p).getOrThrow()
            context.getString(R.string.dutype_ai_done_urgent)
        }
        "hire" -> {
            applications.setStatus(arg(a, "applicationId"), ApplicationStatus.HIRED).getOrThrow()
            context.getString(R.string.dutype_ai_done_hired)
        }
        "reject" -> {
            applications.setStatus(arg(a, "applicationId"), ApplicationStatus.REJECTED).getOrThrow()
            context.getString(R.string.dutype_ai_done_rejected)
        }
        "close_job" -> {
            jobs.setStatus(arg(a, "jobId"), "CLOSED").getOrThrow()
            context.getString(R.string.dutype_ai_done_closed)
        }
        "renew_job" -> {
            jobs.renewJob(arg(a, "jobId")).getOrThrow()
            context.getString(R.string.dutype_ai_done_renewed)
        }
        "open_job" -> {
            _state.update { it.copy(openJobId = arg(a, "jobId")) }
            context.getString(R.string.dutype_ai_done_opening)
        }
        "urgent_mark_filled" -> {
            urgent.markFilledById(arg(a, "requestId")).getOrThrow()
            context.getString(R.string.dutype_ai_done_urgent_filled)
        }
        "urgent_select_worker" -> {
            urgent.setResponseStatusById(arg(a, "requestId"), arg(a, "workerId"), "accepted").getOrThrow()
            context.getString(R.string.dutype_ai_done_hired)
        }
        "urgent_remove_worker" -> {
            urgent.setResponseStatusById(arg(a, "requestId"), arg(a, "workerId"), "rejected", arg(a, "reason")).getOrThrow()
            context.getString(R.string.dutype_ai_done_urgent_removed)
        }
        "book_service" -> {
            _state.update { it.copy(openRoute = com.example.dutype.navigation.Routes.servicesBookRoute(arg(a, "serviceId"))) }
            context.getString(R.string.dutype_ai_done_service)
        }
        "open_service_booking" -> {
            _state.update { it.copy(openRoute = com.example.dutype.navigation.Routes.servicesBookingRoute(arg(a, "bookingId"))) }
            context.getString(R.string.dutype_ai_done_opening)
        }
        else -> error(context.getString(R.string.dutype_ai_error))
    }
}

/** "DutyPe AI" on the app icon (long-press) and, on request, pinned to the home screen. */
object DutyPeAiShortcut {
    private const val ID = "dutype_ai"
    const val DEEP_LINK = "dutype://ai?listen=true"

    private fun info(context: Context): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(context, ID)
            .setShortLabel("DutyPe AI")
            .setLongLabel(context.getString(R.string.dutype_ai_shortcut_long))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(Intent(Intent.ACTION_VIEW, Uri.parse(DEEP_LINK)).setPackage(context.packageName))
            .build()

    /** Employers only: adds the long-press shortcut (no-op if already there). */
    fun register(context: Context) {
        runCatching { ShortcutManagerCompat.pushDynamicShortcut(context, info(context)) }
    }

    fun pin(context: Context): Boolean =
        ShortcutManagerCompat.isRequestPinShortcutSupported(context) &&
            runCatching { ShortcutManagerCompat.requestPinShortcut(context, info(context), null) }.getOrDefault(false)
}


// ──────────────────────────── DutyPe AI light theme colour palette ────────────────────────────
private val AiBg         = Color.White
private val AiSurface    = Color(0xFFF8FAFC)
private val AiSurfaceAlt = Color(0xFFF1F5F9)
private val AiAccent     = Color(0xFF7C3AED)          // vivid violet
private val AiAccentAlt  = Color(0xFF4F46E5)          // indigo
private val AiOnDark     = Color(0xFF0F172A)          // main dark text
private val AiMuted      = Color(0xFF64748B)          // muted blue-slate
private val AiChipBg     = Color(0xFFF1F5F9)          // chip surface
private val AiChipBorder = Color(0xFFE2E8F0)          // chip border

// Kept for action-card section (light colours used on white surface)
private val Purple     = Color(0xFF6D28D9)
private val PurpleSoft = Color(0xFFF5F3FF)
private val PurpleLine = Color(0xFFE9E5FB)
private val Ink        = Color(0xFF0F172A)
private val Muted      = Color(0xFF64748B)
private val Line       = Color(0xFFE5E7EB)

private val AiGradient      = Brush.linearGradient(listOf(AiAccent, AiAccentAlt))
private val AiScreenGradient = Brush.verticalGradient(listOf(Color.White, Color(0xFFF8FAFC), Color.White))
private val AiMicRingGradient = Brush.sweepGradient(
    listOf(
        Color(0xFFFFD700), Color(0xFF00E5FF), Color(0xFFFFFFFF),
        Color(0xFF7C3AED), Color(0xFFFFD700)
    )
)

/**
 * DutyPe AI: full-screen dark assistant. Talks + types to post jobs, book services,
 * hear stats, hire / close. Replies spoken in app language; every action waits for "Yes".
 */
@Composable
fun DutyPeAiScreen(navController: NavController, startListening: Boolean = false, rootNavController: NavController? = null) {
    val context = LocalContext.current
    val viewModel: DutyPeAiViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val subscription by viewModel.subscription.collectAsStateWithLifecycle()
    val employer by viewModel.employer.collectAsStateWithLifecycle()
    val lang = LocaleHelper.getLanguage(context)
    var typed by rememberSaveable { mutableStateOf("") }
    var muted by rememberSaveable { mutableStateOf(false) }
    var autoListened by rememberSaveable { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }
    var showKeyboard by remember { mutableStateOf(false) }
    val greeting = stringResource(R.string.dutype_ai_greeting)

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.let { e ->
                    e.setSpeechRate(0.92f)
                    e.setPitch(1.02f)
                    tts = e
                }
            }
        }
        onDispose { engine?.stop(); engine?.shutdown() }
    }

    val speak: (String) -> Unit = { text ->
        if (!muted && text.isNotBlank()) {
            tts?.let { engine ->
                val targetLocale = when {
                    text.any { it in '\u0C00'..'\u0C7F' } -> Locale("te", "IN")
                    text.any { it in '\u0900'..'\u097F' } -> Locale("hi", "IN")
                    else -> Locale("en", "IN")
                }
                runCatching {
                    if (engine.isLanguageAvailable(targetLocale) >= TextToSpeech.LANG_AVAILABLE) {
                        engine.language = targetLocale
                    }
                    engine.setSpeechRate(0.92f)
                    engine.setPitch(1.02f)
                    engine.voices?.filter { v -> v.locale.language == targetLocale.language }
                        ?.maxByOrNull { v -> if (v.quality >= Voice.QUALITY_HIGH) 2 else 1 }
                        ?.let { bestVoice -> engine.voice = bestVoice }
                }
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dutype_ai_${System.currentTimeMillis()}")
            }
        }
    }

    var spokeHello by rememberSaveable { mutableStateOf(false) }
    val helloPlain = stringResource(R.string.dutype_ai_voice_hello)
    LaunchedEffect(tts, startListening) {
        if (tts == null || spokeHello || startListening) return@LaunchedEffect
        spokeHello = true
        kotlinx.coroutines.delay(300)
        val first = viewModel.employer.value?.ownerName?.trim()?.substringBefore(' ').orEmpty()
        speak(if (first.isNotBlank()) context.getString(R.string.dutype_ai_voice_hello_name, first) else helloPlain)
    }
    LaunchedEffect(Unit) {
        viewModel.greet(greeting)
        DutyPeAiShortcut.register(context)
    }

    var isListening by rememberSaveable { mutableStateOf(false) }

    // Fallback activity launcher if in-app recognizer is unavailable
    val listen = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { viewModel.send(it, speak) }
        }
    }

    // In-app SpeechRecognizer: stays directly on screen without opening external dialog
    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
        } else null
    }

    DisposableEffect(speechRecognizer) {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = matches?.firstOrNull()?.trim()
                if (!spoken.isNullOrBlank()) {
                    viewModel.send(spoken, speak)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose {
            runCatching {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            }
        }
    }

    val startMic: () -> Unit = {
        tts?.stop()
        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "$lang-IN")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("te-IN", "hi-IN", "en-IN"))
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
        }
        if (speechRecognizer != null) {
            val started = runCatching { speechRecognizer.startListening(intent) }.isSuccess
            if (!started) {
                val launched = runCatching { listen.launch(intent) }.isSuccess
                if (!launched) isListening = false
            }
        } else {
            val launched = runCatching { listen.launch(intent) }.isSuccess
            if (!launched) isListening = false
        }
    }

    val stopMic: () -> Unit = {
        isListening = false
        runCatching { speechRecognizer?.stopListening() }
        tts?.stop()
    }
    LaunchedEffect(startListening, state.needsLogin) {
        if (startListening && !autoListened && !state.needsLogin) {
            autoListened = true
            startMic()
        }
    }
    LaunchedEffect(state.openRoute) {
        state.openRoute?.let { route ->
            viewModel.openedRoute()
            runCatching { (rootNavController ?: navController).navigate(route) }
        }
    }
    LaunchedEffect(state.openJobId) {
        state.openJobId?.let { jobId ->
            viewModel.openedJob()
            runCatching { navController.navigate(com.example.dutype.navigation.Routes.employerApplicationsJobRoute(jobId)) }
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.pending, state.thinking) {
        if (state.messages.size > 1) listState.animateScrollToItem((state.messages.size + 1).coerceAtLeast(0))
    }
    val conversationStarted = state.messages.size > 1
    val firstName = employer?.ownerName?.trim()?.substringBefore(' ').orEmpty()

    // Status bar: dark icons on white background
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.SideEffect {
        val window = (view.context as? android.app.Activity)?.window
        window?.let {
            androidx.core.view.WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AiBg)
    ) {
        // Ambient horizon glow at bottom edge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0x0A7C3AED),
                            Color(0x187C3AED)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
        ) {
            // ── Top bar: Clean centered "DutyPe AI Assistant" pill ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "DutyPe AI Assistant",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            }

            // Stats strip (always dark)
            state.stats?.let { AiDarkStatsStrip(it) }

            // ── Main content (Listening vs Idle vs Chat) ─────────────────────────
            when {
                state.needsLogin -> AiDarkLoginGate(
                    modifier = Modifier.weight(1f),
                    onLogin = { showLogin = true }
                )
                isListening -> AiDarkListeningView(modifier = Modifier.weight(1f))
                !conversationStarted -> AiDarkWelcome(
                    name = firstName,
                    modifier = Modifier.weight(1f),
                    onAsk = { viewModel.send(it, speak) }
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.messages.drop(1)) { m -> AiDarkBubble(m) }
                    item {
                        state.pending?.let { action ->
                            ActionCard(action, onYes = { viewModel.confirm(speak) }, onNo = viewModel::cancel)
                        }
                        if (state.locked == "upgrade" && state.pending == null) {
                            AiDarkUpgradeCard {
                                runCatching { navController.navigate(com.example.dutype.navigation.Routes.EMPLOYER_SUBSCRIPTION) }
                            }
                        }
                    }
                    item { if (state.thinking) AiDarkTypingBubble() }
                }
            }

            // â”€â”€ Quick suggestion chips (after first message) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (!state.needsLogin && conversationStarted) {
                val chips = listOf(
                    stringResource(R.string.dutype_ai_chip_applied),
                    stringResource(R.string.dutype_ai_chip_hired),
                    stringResource(R.string.dutype_ai_chip_post),
                    stringResource(R.string.dutype_ai_chip_best)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chips.forEach { chip ->
                        Text(
                            chip,
                            color = AiOnDark,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .border(1.dp, AiChipBorder, RoundedCornerShape(999.dp))
                                .background(AiChipBg, RoundedCornerShape(999.dp))
                                .clickable(enabled = !state.thinking) { viewModel.send(chip, speak) }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // â”€â”€ Bottom bar: keyboard | animated mic | Ã— close â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (!state.needsLogin) {
                // Hint text above bar
                val hintText = when {
                    state.thinking -> "Thinking…"
                    isListening -> "Tap anytime to stop"
                    else -> "Tap to talk"
                }
                Text(
                    text = hintText,
                    color = AiMuted,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                if (showKeyboard) {
                    // Keyboard input row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .drawBehind {
                                drawLine(
                                    color = Color(0xFFE2E8F0),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = typed,
                            onValueChange = { typed = it },
                            placeholder = { Text(stringResource(R.string.dutype_ai_type_hint), color = Color(0xFF94A3B8)) },
                            trailingIcon = {
                                if (typed.isNotBlank()) IconButton(onClick = { viewModel.send(typed, speak); typed = "" }, enabled = !state.thinking) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = AiAccent)
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedBorderColor = AiAccent,
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedTextColor = Color(0xFF0F172A),
                                cursorColor = AiAccent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFFF1F5F9), CircleShape)
                                .clickable { showKeyboard = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close keyboard", tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
                        }
                    }
                } else {
                    // Voice bottom bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .drawBehind {
                                drawLine(
                                    color = Color(0xFFE2E8F0),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .padding(horizontal = 32.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Keyboard toggle
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(Color(0xFFF1F5F9), CircleShape)
                                .clickable { showKeyboard = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Keyboard, contentDescription = "Type", tint = Color(0xFF475569), modifier = Modifier.size(22.dp))
                        }

                        // Animated gradient-border button (waveform when listening, mic when idle)
                        AiAnimatedMicButton(
                            thinking = state.thinking,
                            isListening = isListening,
                            onClick = {
                                if (isListening) {
                                    stopMic()
                                } else {
                                    startMic()
                                }
                            }
                        )

                        // × close / go back
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(Color(0xFFF1F5F9), CircleShape)
                                .clickable {
                                    tts?.stop()
                                    navController.popBackStack()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF475569), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }

    com.example.dutype.components.LoginBottomSheet(
        isVisible = showLogin,
        onDismiss = { showLogin = false },
        onLoginSuccess = {
            showLogin = false
            viewModel.refreshLogin()
        },
        role = com.example.dutype.models.UserRole.EMPLOYER,
        title = stringResource(R.string.dutype_ai_login_title),
        subtitle = stringResource(R.string.dutype_ai_login_body),
        navController = navController
    )
}

@Composable
private fun AiAnimatedMicButton(
    thinking: Boolean,
    isListening: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micRing")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "micRotation"
    )
    Box(
        modifier = Modifier
            .size(72.dp)
            .drawBehind {
                rotate(rotation) {
                    drawCircle(
                        brush = AiMicRingGradient,
                        radius = size.minDimension / 2f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                }
            }
            .padding(5.dp)
            .background(
                if (thinking) Brush.linearGradient(listOf(Color(0xFF4B5563), Color(0xFF374151)))
                else if (isListening) Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1B4B)))
                else AiGradient,
                CircleShape
            )
            .clickable(enabled = !thinking) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isListening) {
            AudioWaveBars()
        } else {
            Icon(
                Icons.Filled.Mic,
                contentDescription = stringResource(R.string.ai_post_tap_to_speak),
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

/** 3 vertical pulsing audio waveform bars (matches media_1791350652254.jpg) */
@Composable
private fun AudioWaveBars() {
    val transition = rememberInfiniteTransition(label = "wave")
    val h1 by transition.animateFloat(10f, 26f, infiniteRepeatable(tween(380), RepeatMode.Reverse), label = "h1")
    val h2 by transition.animateFloat(18f, 32f, infiniteRepeatable(tween(320, delayMillis = 60), RepeatMode.Reverse), label = "h2")
    val h3 by transition.animateFloat(12f, 24f, infiniteRepeatable(tween(420, delayMillis = 120), RepeatMode.Reverse), label = "h3")
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(3.5.dp).height(h1.dp).background(Color.White, RoundedCornerShape(2.dp)))
        Box(Modifier.width(3.5.dp).height(h2.dp).background(Color.White, RoundedCornerShape(2.dp)))
        Box(Modifier.width(3.5.dp).height(h3.dp).background(Color.White, RoundedCornerShape(2.dp)))
    }
}

/** Centered Listening.... state (matches media_1791350652254.jpg) */
@Composable
private fun AiDarkListeningView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val transition = rememberInfiniteTransition(label = "listeningDots")
        val alpha by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
            label = "dotsAlpha"
        )
        Text(
            text = "Listening....",
            color = Color(0xFF0F172A).copy(alpha = alpha),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Dark avatar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AiAvatar(size: androidx.compose.ui.unit.Dp) {
    Box(modifier = Modifier.size(size).background(AiGradient, CircleShape), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Stats strip (dark) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AiDarkStatsStrip(stats: AiStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC))
            .drawBehind {
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        AiDarkStatCell(stats.appliedToday, stringResource(R.string.dutype_ai_stat_applied))
        AiDarkStatCell(stats.waiting, stringResource(R.string.dutype_ai_stat_waiting))
        AiDarkStatCell(stats.openJobs, stringResource(R.string.dutype_ai_stat_open))
        AiDarkStatCell(stats.hired, stringResource(R.string.dutype_ai_stat_hired))
    }
}

@Composable
private fun AiDarkStatCell(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = AiOnDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = AiMuted, fontSize = 11.sp, maxLines = 1)
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Welcome / idle screen (dark) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AiDarkWelcome(name: String, modifier: Modifier, onAsk: (String) -> Unit) {
    // Suggestion chips — two scrollable rows (matching media_1791350659354.jpg)
    val chipsRow1 = listOf("Post a job", "Book AC service", "Find a plumber", "Hire a Cook", "Daily Helper")
    val chipsRow2 = listOf("Track my booking", "Check worker applicants", "Urgent need today", "Check earnings")

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // Glowing avatar
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(
                        Brush.radialGradient(listOf(AiAccent.copy(alpha = 0.35f), Color.Transparent)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                AiAvatar(size = 60.dp)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (name.isNotBlank()) "Hi $name 👋" else "DutyPe AI Assistant",
                color = AiMuted,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "What can I help\nyou with?",
                color = AiOnDark,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tap a suggestion, use your mic, or type to explore.",
                color = AiMuted,
                fontSize = 13.5.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(24.dp))
        }

        // Row 1 chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                chipsRow1.forEach { chip ->
                    Text(
                        chip,
                        color = AiOnDark,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .border(1.dp, AiChipBorder, RoundedCornerShape(999.dp))
                            .background(AiChipBg, RoundedCornerShape(999.dp))
                            .clickable { onAsk(chip) }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // Row 2 chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                chipsRow2.forEach { chip ->
                    Text(
                        chip,
                        color = AiOnDark,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .border(1.dp, AiChipBorder, RoundedCornerShape(999.dp))
                            .background(AiChipBg, RoundedCornerShape(999.dp))
                            .clickable { onAsk(chip) }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Login gate (dark) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AiDarkLoginGate(modifier: Modifier, onLogin: () -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AiAvatar(size = 64.dp)
        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(R.string.dutype_ai_login_title),
            color = AiOnDark, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.dutype_ai_login_body),
            color = AiMuted, fontSize = 14.sp, lineHeight = 20.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onLogin,
            colors = ButtonDefaults.buttonColors(containerColor = AiAccent, contentColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(46.dp)
        ) {
            Text(
                stringResource(R.string.dutype_ai_login_button),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Chat bubble (dark) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AiDarkBubble(m: AiMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (m.fromAi) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        if (m.fromAi) {
            AiAvatar(size = 26.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            m.text,
            color = if (m.fromAi) AiOnDark else Color.White,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier
                .widthIn(max = 290.dp)
                .background(
                    if (m.fromAi) Brush.linearGradient(listOf(AiChipBg, AiChipBg)) else AiGradient,
                    RoundedCornerShape(
                        topStart = if (m.fromAi) 4.dp else 18.dp,
                        topEnd = 18.dp, bottomStart = 18.dp,
                        bottomEnd = if (m.fromAi) 18.dp else 4.dp
                    )
                )
                .border(1.dp, if (m.fromAi) AiChipBorder else Color.Transparent,
                    RoundedCornerShape(topStart = if (m.fromAi) 4.dp else 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = if (m.fromAi) 18.dp else 4.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Typing indicator (dark, three dots) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AiDarkTypingBubble() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.CenterVertically) {
        AiAvatar(size = 26.dp)
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .background(AiChipBg, RoundedCornerShape(18.dp))
                .border(1.dp, AiChipBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(3) { i ->
                val alpha by transition.animateFloat(
                    initialValue = 0.25f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(500, delayMillis = i * 150),
                        RepeatMode.Reverse
                    ),
                    label = "dot$i"
                )
                Box(modifier = Modifier.size(7.dp).background(AiMuted.copy(alpha = alpha), CircleShape))
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Action confirm card (light surface) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// (kept on a light surface card so the confirm/cancel buttons remain legible)

@Composable
private fun ActionCard(action: AiAction, onYes: () -> Unit, onNo: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val icon = when (action.type) {
        "post_job" -> Icons.Filled.Work
        "post_urgent" -> Icons.Filled.Bolt
        "hire" -> Icons.Filled.HowToReg
        else -> Icons.Filled.AutoAwesome
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 34.dp)
            .border(1.dp, Color(0xFFE2E8F0), shape)
            .background(Color(0xFFF8FAFC), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(34.dp).background(Color(0xFFEDE9FE), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = AiAccent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(stringResource(R.string.dutype_ai_confirm_title), color = AiMuted, fontSize = 12.sp)
                Text(action.summary, color = AiOnDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onNo,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(44.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AiChipBorder)
            ) { Text(stringResource(R.string.dutype_ai_no), color = AiMuted) }
            Button(
                onClick = onYes,
                colors = ButtonDefaults.buttonColors(containerColor = AiAccent, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(44.dp)
            ) { Text(stringResource(R.string.dutype_ai_yes), fontWeight = FontWeight.SemiBold) }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Upgrade card (dark) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AiDarkUpgradeCard(onPlans: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 34.dp)
            .background(Color(0xFFF5F3FF), shape)
            .border(1.dp, Color(0xFFDDD6FE), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.dutype_ai_upgrade_title), color = AiOnDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(stringResource(R.string.dutype_ai_upgrade_body), color = AiMuted, fontSize = 13.sp, lineHeight = 18.sp)
        Button(
            onClick = onPlans,
            colors = ButtonDefaults.buttonColors(containerColor = AiAccent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(42.dp)
        ) { Text(stringResource(R.string.dutype_ai_see_plans), fontWeight = FontWeight.SemiBold) }
    }
}

