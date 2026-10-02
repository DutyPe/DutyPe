package com.example.dutype.employer.ai

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import kotlinx.coroutines.tasks.await
import androidx.compose.animation.core.animateFloat
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Brush
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import java.util.Locale
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
                    // The server's own message (e.g. "Pay can be at most ₹50,000") is clear; else a plain one.
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

private val Purple = Color(0xFF6D28D9)
private val PurpleSoft = Color(0xFFF5F3FF)
private val PurpleLine = Color(0xFFE9E5FB)
private val Ink = Color(0xFF0F172A)
private val Muted = Color(0xFF64748B)
private val Line = Color(0xFFE5E7EB)
private val AiGradient = Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF4F46E5)))

/**
 * DutyPe AI: talk (or type) to post jobs, hear how many applied and who is hired, hire or close
 * jobs. Replies are spoken in the app language; every action waits for the employer's "Yes".
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
    val greeting = stringResource(R.string.dutype_ai_greeting)

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var voiceOk by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Telugu / Hindi voices are not on every phone: try xx-IN, then xx; else stay silent
                // rather than reading Telugu text with an English voice.
                engine?.let { e ->
                    voiceOk = listOf(Locale(lang, "IN"), Locale(lang)).any { loc ->
                        e.setLanguage(loc) >= TextToSpeech.LANG_AVAILABLE
                    }
                    tts = e
                }
            }
        }
        onDispose { engine?.stop(); engine?.shutdown() }
    }
    val speak: (String) -> Unit = { text -> if (!muted && voiceOk && text.isNotBlank()) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dutype_ai") }
    // Spoken welcome once per open, in the app language: "Hello Sita, how can I help you today?"
    var spokeHello by rememberSaveable { mutableStateOf(false) }
    val helloPlain = stringResource(R.string.dutype_ai_voice_hello)
    LaunchedEffect(tts, startListening) {
        if (tts == null || spokeHello || startListening) return@LaunchedEffect
        spokeHello = true
        // Give the profile a moment to load so the greeting can use the name.
        kotlinx.coroutines.delay(300)
        val first = viewModel.employer.value?.ownerName?.trim()?.substringBefore(' ').orEmpty()
        speak(if (first.isNotBlank()) context.getString(R.string.dutype_ai_voice_hello_name, first) else helloPlain)
    }
    LaunchedEffect(Unit) {
        viewModel.greet(greeting)
        DutyPeAiShortcut.register(context)
    }

    val listen = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { viewModel.send(it, speak) }
        }
    }
    val startMic = {
        tts?.stop()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "$lang-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.dutype_ai_listening))
        }
        runCatching { listen.launch(intent) }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.bg())
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
    ) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Ink.fg()) }
            AiAvatar(size = 34.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("DutyPe AI", color = Ink.fg(), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                val trialLeft = (10 - (employer?.aiTrialUsed ?: 0)).coerceAtLeast(0)
                Text(
                    when {
                        state.needsLogin -> stringResource(R.string.dutype_ai_login_title)
                        subscription.hasAi -> stringResource(R.string.dutype_ai_plan_included)
                        trialLeft > 0 -> stringResource(R.string.dutype_ai_trial_left, trialLeft)
                        else -> stringResource(R.string.dutype_ai_plan_needed)
                    },
                    color = Muted.fg(), fontSize = 12.sp, maxLines = 1
                )
            }
            IconButton(onClick = { muted = !muted; if (muted) tts?.stop() }) {
                Icon(if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Ink.fg())
            }
            IconButton(onClick = {
                val ok = DutyPeAiShortcut.pin(context)
                android.widget.Toast.makeText(context, context.getString(if (ok) R.string.dutype_ai_pin_ok else R.string.dutype_ai_pin_unsupported), android.widget.Toast.LENGTH_SHORT).show()
            }) { Icon(Icons.Filled.AddToHomeScreen, contentDescription = stringResource(R.string.dutype_ai_pin), tint = Ink.fg()) }
        }
        androidx.compose.material3.HorizontalDivider(color = Line.bd())

        state.stats?.let { StatsStrip(it) }

        when {
            state.needsLogin -> LoginGate(modifier = Modifier.weight(1f), onLogin = { showLogin = true })
            !conversationStarted -> Welcome(
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
                items(state.messages.drop(1)) { m -> Bubble(m) }
                item {
                    state.pending?.let { action -> ActionCard(action, onYes = { viewModel.confirm(speak) }, onNo = viewModel::cancel) }
                    if (state.locked == "upgrade" && state.pending == null) UpgradeCard { runCatching { navController.navigate(com.example.dutype.navigation.Routes.EMPLOYER_SUBSCRIPTION) } }
                }
                item { if (state.thinking) TypingBubble() }
            }
        }

        if (!state.needsLogin) {
            if (conversationStarted) {
                val chips = listOf(
                    stringResource(R.string.dutype_ai_chip_applied),
                    stringResource(R.string.dutype_ai_chip_hired),
                    stringResource(R.string.dutype_ai_chip_post),
                    stringResource(R.string.dutype_ai_chip_best)
                )
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    items(chips) { chip ->
                        Text(
                            chip,
                            color = Ink.fg(), fontSize = 13.sp,
                            modifier = Modifier
                                .border(1.dp, Line.bd(), RoundedCornerShape(999.dp))
                                .clickable(enabled = !state.thinking) { viewModel.send(chip, speak) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
            Composer(
                typed = typed,
                onTyped = { typed = it },
                thinking = state.thinking,
                onSend = { viewModel.send(typed, speak); typed = "" },
                onMic = { startMic() }
            )
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
private fun AiAvatar(size: androidx.compose.ui.unit.Dp) {
    Box(modifier = Modifier.size(size).background(AiGradient, CircleShape), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
    }
}

/** Today's numbers, refreshed with every answer. */
@Composable
private fun StatsStrip(stats: AiStats) {
    Row(
        modifier = Modifier.fillMaxWidth().background(PurpleSoft.bg()).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatCell(stats.appliedToday, stringResource(R.string.dutype_ai_stat_applied))
        StatCell(stats.waiting, stringResource(R.string.dutype_ai_stat_waiting))
        StatCell(stats.openJobs, stringResource(R.string.dutype_ai_stat_open))
        StatCell(stats.hired, stringResource(R.string.dutype_ai_stat_hired))
    }
}

@Composable
private fun StatCell(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = Ink.fg(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Muted.fg(), fontSize = 11.sp, maxLines = 1)
    }
}

/** First open: a short hello and four things to ask, one tap each. */
@Composable
private fun Welcome(name: String, modifier: Modifier, onAsk: (String) -> Unit) {
    val suggestions = listOf(
        Triple(Icons.Filled.Work, stringResource(R.string.dutype_ai_chip_post), stringResource(R.string.dutype_ai_card_post)),
        Triple(Icons.Filled.Bolt, stringResource(R.string.dutype_ai_chip_urgent), stringResource(R.string.dutype_ai_card_urgent)),
        Triple(Icons.Filled.Groups, stringResource(R.string.dutype_ai_chip_applied), stringResource(R.string.dutype_ai_card_applied)),
        Triple(Icons.Filled.HowToReg, stringResource(R.string.dutype_ai_chip_hired), stringResource(R.string.dutype_ai_card_hired)),
        Triple(Icons.Filled.HomeRepairService, stringResource(R.string.dutype_ai_chip_service), stringResource(R.string.dutype_ai_card_service)),
        Triple(Icons.Filled.CardGiftcard, stringResource(R.string.dutype_ai_chip_plan), stringResource(R.string.dutype_ai_card_plan))
    )
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            AiAvatar(size = 64.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                if (name.isNotBlank()) stringResource(R.string.dutype_ai_hello_name, name) else stringResource(R.string.dutype_ai_hello),
                color = Ink.fg(), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                stringResource(R.string.dutype_ai_hero_sub),
                color = Muted.fg(), fontSize = 14.sp, lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        items(suggestions.chunked(2)) { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (icon, ask, hint) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp)
                            .border(1.dp, Line.bd(), RoundedCornerShape(16.dp))
                            .clickable { onAsk(ask) }
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.size(32.dp).background(PurpleSoft.bg(), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = Purple.fg(), modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(ask, color = Ink.fg(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 18.sp)
                            Text(hint, color = Muted.fg(), fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginGate(modifier: Modifier, onLogin: () -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AiAvatar(size = 64.dp)
        Spacer(modifier = Modifier.height(18.dp))
        Text(stringResource(R.string.dutype_ai_login_title), color = Ink.fg(), fontSize = 20.sp, fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(6.dp))
        Text(stringResource(R.string.dutype_ai_login_body), color = Muted.fg(), fontSize = 14.sp, lineHeight = 20.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(22.dp))
        Button(
            onClick = onLogin,
            colors = ButtonDefaults.buttonColors(containerColor = Ink.bg(), contentColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(46.dp)
        ) { Text(stringResource(R.string.dutype_ai_login_button), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp)) }
    }
}

@Composable
private fun Composer(typed: String, onTyped: (String) -> Unit, thinking: Boolean, onSend: () -> Unit, onMic: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = typed,
            onValueChange = onTyped,
            placeholder = { Text(stringResource(R.string.dutype_ai_type_hint), color = Muted.fg()) },
            trailingIcon = {
                if (typed.isNotBlank()) IconButton(onClick = onSend, enabled = !thinking) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Purple.fg())
                }
            },
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Line.bd(), focusedBorderColor = Purple.bd()
            ),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(if (thinking) Brush.linearGradient(listOf(Color(0xFFC4B5FD).bg(), Color(0xFFC4B5FD).bg())) else AiGradient, CircleShape)
                .clickable(enabled = !thinking) { onMic() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.ai_post_tap_to_speak), tint = Color.White, modifier = Modifier.size(26.dp)) }
    }
}

@Composable
private fun Bubble(m: AiMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (m.fromAi) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        if (m.fromAi) {
            AiAvatar(size = 26.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            m.text,
            color = if (m.fromAi) Ink.fg() else Color.White,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier
                .widthIn(max = 290.dp)
                .background(
                    if (m.fromAi) Color(0xFFF8FAFC).bg() else Ink.bg(),
                    RoundedCornerShape(topStart = if (m.fromAi) 4.dp else 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = if (m.fromAi) 18.dp else 4.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

/** Three dots while DutyPe AI is thinking. */
@Composable
private fun TypingBubble() {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.CenterVertically) {
        AiAvatar(size = 26.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            modifier = Modifier.background(Color(0xFFF8FAFC).bg(), RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(3) { i ->
                val alpha by transition.animateFloat(
                    initialValue = 0.25f, targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                        androidx.compose.animation.core.tween(500, delayMillis = i * 150),
                        androidx.compose.animation.core.RepeatMode.Reverse
                    ),
                    label = "dot$i"
                )
                Box(modifier = Modifier.size(7.dp).background(Muted.bg().copy(alpha = alpha), CircleShape))
            }
        }
    }
}

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
        modifier = Modifier.fillMaxWidth().padding(start = 34.dp).border(1.dp, PurpleLine.bd(), shape).background(Color.White.bg(), shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(34.dp).background(PurpleSoft.bg(), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Purple.fg(), modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(stringResource(R.string.dutype_ai_confirm_title), color = Muted.fg(), fontSize = 12.sp)
                Text(action.summary, color = Ink.fg(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onNo, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(44.dp)) {
                Text(stringResource(R.string.dutype_ai_no), color = Ink.fg())
            }
            Button(
                onClick = onYes,
                colors = ButtonDefaults.buttonColors(containerColor = Ink.bg(), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(44.dp)
            ) { Text(stringResource(R.string.dutype_ai_yes), fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun UpgradeCard(onPlans: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 34.dp).background(PurpleSoft.bg(), shape).border(1.dp, PurpleLine.bd(), shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.dutype_ai_upgrade_title), color = Ink.fg(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(stringResource(R.string.dutype_ai_upgrade_body), color = Muted.fg(), fontSize = 13.sp, lineHeight = 18.sp)
        Button(onClick = onPlans, colors = ButtonDefaults.buttonColors(containerColor = Ink.bg()), shape = RoundedCornerShape(12.dp), modifier = Modifier.height(42.dp)) {
            Text(stringResource(R.string.dutype_ai_see_plans), fontWeight = FontWeight.SemiBold)
        }
    }
}
