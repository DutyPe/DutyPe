package com.example.dutype.employer.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.dutype.models.QuickUrgentNeedInput
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale

enum class VoiceLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val sampleHint: String,
    val locale: Locale
) {
    TELUGU(
        code = "te-IN",
        nativeName = "తెలుగు",
        englishName = "Telugu",
        sampleHint = "ఉదా: రేపు ఉదయం 9 గంటలకు 2 గుమస్తాలు కావాలి, ₹800 ఇస్తాము",
        locale = Locale("te", "IN")
    ),
    ENGLISH(
        code = "en-IN",
        nativeName = "English",
        englishName = "English",
        sampleHint = "e.g. Need 2 warehouse helpers tomorrow 9 AM, 800 rupees each",
        locale = Locale.ENGLISH
    ),
    HINDI(
        code = "hi-IN",
        nativeName = "हिंदी",
        englishName = "Hindi",
        sampleHint = "जैसे: कल सुबह 9 बजे 2 हेल्पर चाहिए, ₹700 देंगे",
        locale = Locale("hi", "IN")
    )
}

data class VoiceJobParsedData(
    val title: String = "",
    val category: String = "Other Work",
    val workersNeeded: Int = 1,
    val perPersonPayment: Double = 0.0,
    val budgetText: String = "",
    val durationText: String = "Full Day (8 hrs)",
    val addressText: String = "",
    val isComplete: Boolean = false,
    val missingFields: List<String> = emptyList(),
    val clarificationQuestion: String = "",
    val summaryText: String = ""
)

sealed class VoicePostingState {
    object Idle : VoicePostingState()
    object Listening : VoicePostingState()
    object Processing : VoicePostingState()
    data class Clarifying(val question: String, val parsedData: VoiceJobParsedData) : VoicePostingState()
    data class ReadyToPost(val parsedData: VoiceJobParsedData) : VoicePostingState()
    data class Error(val message: String) : VoicePostingState()
}

class VoiceJobPostingManager(
    private val context: Context,
    private val scope: CoroutineScope
) : RecognitionListener, TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    var currentLanguage: VoiceLanguage = VoiceLanguage.TELUGU
        private set

    private val _state = MutableStateFlow<VoicePostingState>(VoicePostingState.Idle)
    val state: StateFlow<VoicePostingState> = _state.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private var currentAccumulatedInput = VoiceJobParsedData()
    private val functions = FirebaseFunctions.getInstance("asia-south1")

    init {
        initSpeechRecognizer()
        initTextToSpeech()
    }

    fun setLanguage(lang: VoiceLanguage) {
        currentLanguage = lang
        if (isTtsReady) {
            try {
                if (textToSpeech?.isLanguageAvailable(lang.locale) ?: TextToSpeech.LANG_NOT_SUPPORTED >= TextToSpeech.LANG_AVAILABLE) {
                    textToSpeech?.language = lang.locale
                }
            } catch (e: Exception) {
                Timber.w(e, "Could not set TTS language to ${lang.code}")
            }
        }
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoiceJobPostingManager)
            }
        }
    }

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                if (textToSpeech?.isLanguageAvailable(currentLanguage.locale) ?: TextToSpeech.LANG_NOT_SUPPORTED >= TextToSpeech.LANG_AVAILABLE) {
                    textToSpeech?.language = currentLanguage.locale
                } else {
                    textToSpeech?.language = Locale.ENGLISH
                }
            } catch (_: Exception) {
                textToSpeech?.language = Locale.ENGLISH
            }
            isTtsReady = true
        }
    }

    fun startListening(language: VoiceLanguage? = null) {
        if (language != null) {
            currentLanguage = language
        }
        stopSpeaking()
        _partialTranscript.value = ""
        _soundLevel.value = 0f

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = VoicePostingState.Error("Speech recognition is not available on this device")
            return
        }

        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }

        val additionalLangs = when (currentLanguage) {
            VoiceLanguage.TELUGU -> arrayOf("en-IN", "hi-IN")
            VoiceLanguage.HINDI -> arrayOf("en-IN", "te-IN")
            VoiceLanguage.ENGLISH -> arrayOf("te-IN", "hi-IN")
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage.code)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", additionalLangs)
        }

        try {
            speechRecognizer?.startListening(intent)
            _state.value = VoicePostingState.Listening
        } catch (e: Exception) {
            _state.value = VoicePostingState.Error(e.message ?: "Could not start voice recognition")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
    }

    fun speak(text: String, onDone: () -> Unit = {}) {
        if (!isTtsReady || text.isBlank()) {
            onDone()
            return
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                scope.launch(Dispatchers.Main) { onDone() }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                scope.launch(Dispatchers.Main) { onDone() }
            }
        })

        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_job_utterance_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
    }

    fun updateWageDirectly(amount: Double) {
        val updated = currentAccumulatedInput.copy(
            perPersonPayment = amount,
            budgetText = "₹${amount.toInt()} / Day",
            isComplete = amount > 0,
            missingFields = emptyList()
        )
        currentAccumulatedInput = updated
        _state.value = VoicePostingState.ReadyToPost(updated)
    }

    fun toQuickUrgentNeedInput(employerPhone: String = "", defaultAddress: String = ""): QuickUrgentNeedInput {
        val data = currentAccumulatedInput
        val workers = data.workersNeeded.coerceAtLeast(1)
        val pay = data.perPersonPayment.coerceAtLeast(0.0)
        return QuickUrgentNeedInput(
            title = data.title.ifBlank { "${workers} ${data.category} Needed" },
            description = data.summaryText.ifBlank { "${workers} workers needed urgently" },
            category = data.category,
            workersNeeded = workers,
            needType = "urgent_now",
            urgencyType = "right_now",
            budgetText = if (pay > 0) "₹${pay.toInt()} / Day" else "",
            perPersonPayment = pay,
            totalPayment = pay * workers,
            durationText = data.durationText.ifBlank { "Full Day (8 hrs)" },
            addressText = data.addressText.ifBlank { defaultAddress },
            contactNumber = employerPhone,
            radiusKm = 10.0
        )
    }

    fun reset() {
        stopSpeaking()
        stopListening()
        currentAccumulatedInput = VoiceJobParsedData()
        _partialTranscript.value = ""
        _soundLevel.value = 0f
        _state.value = VoicePostingState.Idle
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (_: Exception) {}
    }

    // ─────────────────────────────── RecognitionListener ───────────────────────────────

    override fun onReadyForSpeech(params: Bundle?) {
        _state.value = VoicePostingState.Listening
    }

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {
        _soundLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _soundLevel.value = 0f
        _state.value = VoicePostingState.Processing
    }

    override fun onError(error: Int) {
        _soundLevel.value = 0f
        val msg = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "Kuch sunai nahi diya. Dobara mic dabakar boliye."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout. Dobara mic dabakar boliye."
            SpeechRecognizer.ERROR_NETWORK -> "Network issue. Internet check karein."
            else -> "Voice recognition error ($error). Dobara try karein."
        }
        _state.value = VoicePostingState.Error(msg)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull().orEmpty()
        if (text.isNotBlank()) {
            _partialTranscript.value = text
            processTranscript(text)
        } else {
            _state.value = VoicePostingState.Error("Koi awaaz capture nahi hui. Dobara boliye.")
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        matches?.firstOrNull()?.let {
            _partialTranscript.value = it
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun processTranscript(transcript: String) {
        _state.value = VoicePostingState.Processing
        scope.launch {
            try {
                val payload = mapOf(
                    "transcript" to transcript,
                    "currentInput" to mapOf(
                        "title" to currentAccumulatedInput.title,
                        "category" to currentAccumulatedInput.category,
                        "workersNeeded" to currentAccumulatedInput.workersNeeded,
                        "perPersonPayment" to currentAccumulatedInput.perPersonPayment,
                        "budgetText" to currentAccumulatedInput.budgetText,
                        "durationText" to currentAccumulatedInput.durationText,
                        "addressText" to currentAccumulatedInput.addressText
                    )
                )

                val result = withContext(Dispatchers.IO) {
                    val resp = functions.getHttpsCallable("parseVoiceJobDetails").call(payload).await()
                    resp.data as? Map<*, *>
                }

                if (result != null) {
                    val category = (result["category"] as? String) ?: "Loading Helper"
                    val workers = (result["workersNeeded"] as? Number)?.toInt() ?: 1
                    val pay = (result["perPersonPayment"] as? Number)?.toDouble() ?: 0.0
                    val isComplete = (result["isComplete"] as? Boolean) ?: (pay > 0)
                    val missing = (result["missingFields"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    val question = (result["clarificationQuestion"] as? String).orEmpty()
                    val summary = (result["summaryText"] as? String).orEmpty()

                    val parsed = VoiceJobParsedData(
                        title = (result["title"] as? String) ?: "${workers} ${category} Needed",
                        category = category,
                        workersNeeded = workers,
                        perPersonPayment = pay,
                        budgetText = if (pay > 0) "₹${pay.toInt()} / Day" else "",
                        durationText = (result["durationText"] as? String) ?: "Full Day (8 hrs)",
                        addressText = (result["addressText"] as? String).orEmpty(),
                        isComplete = isComplete,
                        missingFields = missing,
                        clarificationQuestion = question,
                        summaryText = summary.ifBlank { "${workers} ${category} · ₹${pay.toInt()}/day" }
                    )

                    currentAccumulatedInput = parsed

                    if (isComplete) {
                        _state.value = VoicePostingState.ReadyToPost(parsed)
                        speak("Aapka urgent job ready hai. Confirm karke post karein.")
                    } else {
                        _state.value = VoicePostingState.Clarifying(question, parsed)
                        if (question.isNotBlank()) {
                            speak(question)
                        }
                    }
                } else {
                    fallbackClientSideParsing(transcript)
                }
            } catch (e: Exception) {
                fallbackClientSideParsing(transcript)
            }
        }
    }

    private fun fallbackClientSideParsing(transcript: String) {
        val lower = transcript.toLowerCase(Locale.ROOT)
        var category = "Loading Helper"
        if (lower.contains("electric") || lower.contains("wiring")) category = "Electrician"
        else if (lower.contains("plumb") || lower.contains("pipe")) category = "Plumber"
        else if (lower.contains("cook") || lower.contains("rasoi") || lower.contains("food")) category = "Cook"
        else if (lower.contains("driver") || lower.contains("car")) category = "Driver"
        else if (lower.contains("clean") || lower.contains("maid")) category = "Cleaner / Maid"
        else if (lower.contains("security") || lower.contains("guard")) category = "Security"

        var workers = 1
        val numMatch = Regex("(\\d+)\\s*(helper|worker|person|man|people|log)").find(lower)
        if (numMatch != null) {
            workers = numMatch.groupValues[1].toIntOrNull()?.coerceIn(1, 20) ?: 1
        } else if (lower.contains("do ") || lower.contains("rendu ")) {
            workers = 2
        }

        var pay = 0.0
        val payMatch = Regex("(\\d{3,4})\\s*(rupaye|rs|inr|per|daily|isthanu|denge)?").find(lower)
        if (payMatch != null) {
            pay = payMatch.groupValues[1].toDoubleOrNull() ?: 0.0
        }

        val isComplete = pay > 0
        val parsed = VoiceJobParsedData(
            title = "${workers} ${category} Needed",
            category = category,
            workersNeeded = workers,
            perPersonPayment = pay,
            budgetText = if (pay > 0) "₹${pay.toInt()} / Day" else "",
            durationText = "Full Day (8 hrs)",
            isComplete = isComplete,
            missingFields = if (isComplete) emptyList() else listOf("wage"),
            clarificationQuestion = if (isComplete) "" else "Kaam samajh gaya. Aap per day kitna payment denge?",
            summaryText = "${workers} ${category} · ₹${pay.toInt()}/day"
        )
        currentAccumulatedInput = parsed

        if (isComplete) {
            _state.value = VoicePostingState.ReadyToPost(parsed)
            speak("Aapka urgent job ready hai. Confirm karke post karein.")
        } else {
            _state.value = VoicePostingState.Clarifying("Aap per day kitna payment denge?", parsed)
            speak("Kaam samajh gaya. Aap per day kitna payment denge?")
        }
    }
}
