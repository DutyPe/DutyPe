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
        sampleHint = "ఉదా: రేపు ఉదయం 2 గుమస్తాలు కావాలి, ₹800 ఇస్తాము",
        locale = Locale("te", "IN")
    ),
    ENGLISH(
        code = "en-IN",
        nativeName = "English",
        englishName = "English",
        sampleHint = "e.g. Need 2 warehouse helpers tomorrow, 800 rupees each",
        locale = Locale.ENGLISH
    ),
    HINDI(
        code = "hi-IN",
        nativeName = "हिंदी",
        englishName = "Hindi",
        sampleHint = "जैसे: कल 2 हेल्पर चाहिए, ₹700 देंगे",
        locale = Locale("hi", "IN")
    )
}

data class VoiceJobParsedData(
    val title: String = "",
    val category: String = "Loading Helper",
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
    data class Clarifying(
        val question: String,
        val parsedData: VoiceJobParsedData,
        val isListening: Boolean = false,
        val isSpeakingQuestion: Boolean = false,
        val hintMessage: String = ""
    ) : VoicePostingState()
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

    private fun startListeningInternal() {
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
            // Ensure recognizer waits for user to finish speaking without cutting off prematurely
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2500L)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Timber.e(e, "Error starting speech recognition")
            val current = _state.value
            if (current is VoicePostingState.Clarifying) {
                _state.value = current.copy(isListening = false, isSpeakingQuestion = false)
            } else {
                _state.value = VoicePostingState.Error(e.message ?: "Could not start voice recognition")
            }
        }
    }

    fun startListening(language: VoiceLanguage? = null) {
        if (language != null) {
            setLanguage(language)
        }
        val current = _state.value
        if (current is VoicePostingState.Clarifying) {
            _state.value = current.copy(isListening = true, isSpeakingQuestion = false, hintMessage = "")
            startListeningInternal()
            return
        }
        _state.value = VoicePostingState.Listening
        startListeningInternal()
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        val current = _state.value
        if (current is VoicePostingState.Clarifying) {
            _state.value = current.copy(isListening = false)
        }
    }

    fun speak(text: String, onDone: () -> Unit = {}) {
        if (!isTtsReady || text.isBlank()) {
            onDone()
            return
        }

        try {
            stopListening()
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
        } catch (e: Exception) {
            Timber.w(e, "TTS speak error")
            onDone()
        }
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
    }

    fun updateWageDirectly(amount: Double) {
        stopSpeaking()
        stopListening()
        val updated = currentAccumulatedInput.copy(
            perPersonPayment = amount,
            budgetText = "₹${amount.toInt()} / Day",
            isComplete = amount > 0,
            missingFields = emptyList()
        )
        currentAccumulatedInput = updated
        _state.value = VoicePostingState.ReadyToPost(updated)
        val readyMsg = when (currentLanguage) {
            VoiceLanguage.TELUGU -> "మీ అర్జెంట్ జాబ్ రెడీగా ఉంది. Confirm చేసి పోస్ట్ చేయండి."
            VoiceLanguage.HINDI -> "Aapka urgent job ready hai. Confirm karke post karein."
            VoiceLanguage.ENGLISH -> "Your urgent job is ready. Confirm and post now."
        }
        speak(readyMsg)
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
        val current = _state.value
        if (current is VoicePostingState.Clarifying) {
            _state.value = current.copy(isListening = true, isSpeakingQuestion = false)
        } else {
            _state.value = VoicePostingState.Listening
        }
    }

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {
        _soundLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _soundLevel.value = 0f
        val current = _state.value
        if (current is VoicePostingState.Clarifying) {
            _state.value = current.copy(isListening = false)
        } else {
            _state.value = VoicePostingState.Processing
        }
    }

    override fun onError(error: Int) {
        _soundLevel.value = 0f
        Timber.w("Voice recognition onError code: $error")
        val current = _state.value
        if (current is VoicePostingState.Clarifying) {
            val hint = when (error) {
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_NO_MATCH ->
                    "Didn't catch that. Tap mic or pick wage below."
                else -> "Tap mic to speak again or pick wage below."
            }
            _state.value = current.copy(isListening = false, isSpeakingQuestion = false, hintMessage = hint)
            return
        }
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
            val current = _state.value
            if (current is VoicePostingState.Clarifying) {
                _state.value = current.copy(isListening = false, hintMessage = "Didn't catch that. Tap mic or pick wage below.")
            } else {
                _state.value = VoicePostingState.Error("Koi awaaz capture nahi hui. Dobara boliye.")
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        matches?.firstOrNull()?.let {
            _partialTranscript.value = it
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    // ─────────────────────────────── Processing Logic ───────────────────────────────

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

                if (result != null && (result["success"] as? Boolean) == true) {
                    val extractedCat = (result["category"] as? String)?.takeIf { it != "Other Work" && it.isNotBlank() }
                    val category = extractedCat ?: currentAccumulatedInput.category.ifBlank { "Loading Helper" }
                    val workers = (result["workersNeeded"] as? Number)?.toInt()?.takeIf { it > 0 }
                        ?: currentAccumulatedInput.workersNeeded.coerceAtLeast(1)
                    val pay = (result["perPersonPayment"] as? Number)?.toDouble()?.takeIf { it > 0 }
                        ?: currentAccumulatedInput.perPersonPayment

                    val isComplete = pay > 0
                    val missing = if (isComplete) emptyList() else listOf("wage")
                    val question = (result["clarificationQuestion"] as? String).orEmpty()
                    val summary = (result["summaryText"] as? String).orEmpty()

                    val parsed = VoiceJobParsedData(
                        title = (result["title"] as? String) ?: "${workers} ${category} Needed",
                        category = category,
                        workersNeeded = workers,
                        perPersonPayment = pay,
                        budgetText = if (pay > 0) "₹${pay.toInt()} / Day" else "",
                        durationText = (result["durationText"] as? String) ?: "Full Day (8 hrs)",
                        addressText = (result["addressText"] as? String).orEmpty().ifBlank { currentAccumulatedInput.addressText },
                        isComplete = isComplete,
                        missingFields = missing,
                        clarificationQuestion = question,
                        summaryText = summary.ifBlank { "${workers} ${category} · ${if (pay > 0) "₹${pay.toInt()}/day" else "Wage pending"}" }
                    )

                    currentAccumulatedInput = parsed

                    if (isComplete) {
                        _state.value = VoicePostingState.ReadyToPost(parsed)
                        val readyMsg = when (currentLanguage) {
                            VoiceLanguage.TELUGU -> "మీ అర్జెంట్ జాబ్ రెడీగా ఉంది. Confirm చేసి పోస్ట్ చేయండి."
                            VoiceLanguage.HINDI -> "Aapka urgent job ready hai. Confirm karke post karein."
                            VoiceLanguage.ENGLISH -> "Your urgent job is ready. Confirm and post now."
                        }
                        speak(readyMsg)
                    } else {
                        val clarQ = question.ifBlank {
                            when (currentLanguage) {
                                VoiceLanguage.TELUGU -> "పని అర్థమైంది. రోజుకు ఎంత పేమెంట్ ఇస్తారు?"
                                VoiceLanguage.HINDI -> "Kaam samajh gaya. Aap per day kitna payment denge?"
                                VoiceLanguage.ENGLISH -> "Understood. How much can you pay per worker per day?"
                            }
                        }
                        _state.value = VoicePostingState.Clarifying(
                            question = clarQ,
                            parsedData = parsed,
                            isListening = false,
                            isSpeakingQuestion = true
                        )
                        speak(clarQ) {
                            _state.value = VoicePostingState.Clarifying(
                                question = clarQ,
                                parsedData = parsed,
                                isListening = true,
                                isSpeakingQuestion = false
                            )
                            startListeningInternal()
                        }
                    }
                } else {
                    fallbackClientSideParsing(transcript)
                }
            } catch (e: Exception) {
                Timber.w(e, "Cloud function parseVoiceJobDetails failed, using fallback")
                fallbackClientSideParsing(transcript)
            }
        }
    }

    private fun fallbackClientSideParsing(transcript: String) {
        val detectedWage = extractWageFromText(transcript)
        val wage = if (detectedWage > 0) detectedWage else currentAccumulatedInput.perPersonPayment

        val category = extractCategoryFromText(transcript, currentAccumulatedInput.category)
        val workers = extractWorkersFromText(transcript, currentAccumulatedInput.workersNeeded)

        val isComplete = wage > 0
        val question = when (currentLanguage) {
            VoiceLanguage.TELUGU -> "పని అర్థమైంది. రోజుకు ఎంత పేమెంట్ ఇస్తారు?"
            VoiceLanguage.HINDI -> "Kaam samajh gaya. Aap per day kitna payment denge?"
            VoiceLanguage.ENGLISH -> "Understood. How much can you pay per worker per day?"
        }

        val parsed = VoiceJobParsedData(
            title = "${workers} ${category} Needed",
            category = category,
            workersNeeded = workers,
            perPersonPayment = wage,
            budgetText = if (wage > 0) "₹${wage.toInt()} / Day" else "",
            durationText = currentAccumulatedInput.durationText.ifBlank { "Full Day (8 hrs)" },
            addressText = currentAccumulatedInput.addressText,
            isComplete = isComplete,
            missingFields = if (isComplete) emptyList() else listOf("wage"),
            clarificationQuestion = if (isComplete) "" else question,
            summaryText = "${workers} ${category} · ${if (wage > 0) "₹${wage.toInt()}/day" else "Wage pending"}"
        )
        currentAccumulatedInput = parsed

        if (isComplete) {
            _state.value = VoicePostingState.ReadyToPost(parsed)
            val readyMsg = when (currentLanguage) {
                VoiceLanguage.TELUGU -> "మీ అర్జెంట్ జాబ్ రెడీగా ఉంది. Confirm చేసి పోస్ట్ చేయండి."
                VoiceLanguage.HINDI -> "Aapka urgent job ready hai. Confirm karke post karein."
                VoiceLanguage.ENGLISH -> "Your urgent job is ready. Confirm and post now."
            }
            speak(readyMsg)
        } else {
            _state.value = VoicePostingState.Clarifying(
                question = question,
                parsedData = parsed,
                isListening = false,
                isSpeakingQuestion = true
            )
            speak(question) {
                _state.value = VoicePostingState.Clarifying(
                    question = question,
                    parsedData = parsed,
                    isListening = true,
                    isSpeakingQuestion = false
                )
                startListeningInternal()
            }
        }
    }

    private fun extractWageFromText(text: String): Double {
        val lower = text.lowercase(Locale.ROOT)

        // 1. Direct 3 to 5 digit numbers (e.g. 500, 600, 700, 800, 1000, 1500)
        val digitMatch = Regex("""\b(\d{3,5})\b""").find(lower)
        if (digitMatch != null) {
            val amount = digitMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            if (amount in 200.0..25000.0) {
                return amount
            }
        }

        // 2. English number words
        if (lower.contains("five hundred") || lower.contains("5 hundred")) return 500.0
        if (lower.contains("six hundred") || lower.contains("6 hundred")) return 600.0
        if (lower.contains("seven hundred") || lower.contains("7 hundred")) return 700.0
        if (lower.contains("eight hundred") || lower.contains("8 hundred")) return 800.0
        if (lower.contains("nine hundred") || lower.contains("9 hundred")) return 900.0
        if (lower.contains("one thousand") || lower.contains("thousand") || lower.contains("1 thousand")) return 1000.0
        if (lower.contains("twelve hundred") || lower.contains("12 hundred")) return 1200.0
        if (lower.contains("fifteen hundred") || lower.contains("15 hundred")) return 1500.0
        if (lower.contains("two thousand") || lower.contains("2 thousand")) return 2000.0

        // 3. Hindi number words
        if (lower.contains("paanch sau") || lower.contains("panch sau") || lower.contains("panch so")) return 500.0
        if (lower.contains("che sau") || lower.contains("chhah sau") || lower.contains("chhe sau") || lower.contains("che so")) return 600.0
        if (lower.contains("saat sau") || lower.contains("sat sau") || lower.contains("saat so")) return 700.0
        if (lower.contains("aath sau") || lower.contains("ath sau") || lower.contains("aath so")) return 800.0
        if (lower.contains("nau sau") || lower.contains("no sau") || lower.contains("nau so")) return 900.0
        if (lower.contains("ek hazaar") || lower.contains("hazaar") || lower.contains("hazar") || lower.contains("hazara")) return 1000.0
        if (lower.contains("barah sau") || lower.contains("gyarah sau")) return 1200.0
        if (lower.contains("pandrah sau")) return 1500.0
        if (lower.contains("do hazaar") || lower.contains("do hazar")) return 2000.0

        // 4. Telugu number words
        if (lower.contains("aidu vandalu") || lower.contains("aidhu vandalu") || lower.contains("5 vandalu")) return 500.0
        if (lower.contains("aaru vandalu") || lower.contains("6 vandalu")) return 600.0
        if (lower.contains("yeedu vandalu") || lower.contains("yedu vandalu") || lower.contains("edu vandalu") || lower.contains("7 vandalu")) return 700.0
        if (lower.contains("enimidi vandalu") || lower.contains("enimidhi vandalu") || lower.contains("8 vandalu")) return 800.0
        if (lower.contains("tommidi vandalu") || lower.contains("9 vandalu")) return 900.0
        if (lower.contains("veyi") || lower.contains("veyyi") || lower.contains("oka veyi") || lower.contains("oka veyyi") || lower.contains("vei")) return 1000.0
        if (lower.contains("padihenu vandalu") || lower.contains("padaharu vandalu")) return 1500.0
        if (lower.contains("rendu velu") || lower.contains("rendu veylu")) return 2000.0

        return 0.0
    }

    private fun extractCategoryFromText(text: String, existingCategory: String = ""): String {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("electric") || lower.contains("wiring") || lower.contains("current") -> "Electrician"
            lower.contains("plumb") || lower.contains("pipe") || lower.contains("tap") || lower.contains("motor") -> "Plumber"
            lower.contains("cook") || lower.contains("rasoi") || lower.contains("khana") || lower.contains("vantam") || lower.contains("vanta") -> "Cook"
            lower.contains("driver") || lower.contains("gaadi") || lower.contains("auto") || lower.contains("car") -> "Driver"
            lower.contains("clean") || lower.contains("safai") || lower.contains("maid") || lower.contains("jhadu") || lower.contains("sweeper") -> "Cleaner / Maid"
            lower.contains("security") || lower.contains("guard") || lower.contains("watchman") -> "Security"
            lower.contains("paint") || lower.contains("rang") || lower.contains("sunnam") -> "Painter"
            lower.contains("carpenter") || lower.contains("wood") || lower.contains("badhai") -> "Carpenter"
            lower.contains("deliver") || lower.contains("parcel") -> "Delivery"
            lower.contains("helper") || lower.contains("labour") || lower.contains("labor") || lower.contains("loading") || lower.contains("godown") || lower.contains("coolie") || lower.contains("worker") || lower.contains("manishi") || lower.contains("hamali") -> "Loading Helper"
            existingCategory.isNotBlank() && existingCategory != "Other Work" -> existingCategory
            else -> "Loading Helper"
        }
    }

    private fun extractWorkersFromText(text: String, existingWorkers: Int = 1): Int {
        val lower = text.lowercase(Locale.ROOT)
        val countMatch = Regex("""(\d+)\s*(helper|worker|person|man|people|members|mandhi|mandi|log|hamali)""").find(lower)
        if (countMatch != null) {
            val count = countMatch.groupValues[1].toIntOrNull()
            if (count != null && count in 1..20) return count
        }
        val standaloneSmall = Regex("""\b([1-9]|10)\b""").find(lower)
        if (standaloneSmall != null && (lower.contains("worker") || lower.contains("helper") || lower.contains("man") || lower.contains("log") || lower.contains("kavali") || lower.contains("chahiye"))) {
            val count = standaloneSmall.groupValues[1].toIntOrNull()
            if (count != null && count in 1..20) return count
        }
        if (lower.contains("rendu ") || lower.contains("do ") || lower.contains("two ")) return 2
        if (lower.contains("moodu ") || lower.contains("teen ") || lower.contains("three ")) return 3
        if (lower.contains("naalugu ") || lower.contains("chaar ") || lower.contains("four ")) return 4
        if (lower.contains("aidu ") || lower.contains("paanch ") || lower.contains("five ")) return 5
        if (lower.contains("oka ") || lower.contains("ek ") || lower.contains("one ")) return 1
        return if (existingWorkers > 0) existingWorkers else 1
    }
}
