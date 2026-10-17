package com.example.dutype.employer.voice

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-performance, natural human-like voice synthesizer engine for DutyPe AI (Liri).
 *
 * Provides expressive, conversational speech synthesis matching modern AI voice assistants (Gemini / ChatGPT).
 * - Single-Voice Guarantee: Exactly ONE voice audio pipeline. Zero dual-voice overlapping or sequential fallback clash.
 * - Google Neural Voices: Explicitly prioritizes Google's high-fidelity DeepMind neural network voices
 *   (e.g., `te-in-x-*-network`, `hi-in-x-*-network`, `en-in-x-*-network`) via Google Speech Services.
 * - Conversational Prosody: Natural human cadence, optimal 0.98f speech rate, 1.0f pitch, and assistant audio attributes.
 * - Markdown & Emoji Sanitization: Strips markdown asterisks, hashtags, bullets, and emojis so speech flows naturally.
 */
class NaturalVoiceEngine(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val appContext: Context = context.applicationContext
    private var textToSpeech: TextToSpeech? = null
    private val isTtsReady = AtomicBoolean(false)
    private var pendingSpeech: PendingUtterance? = null
    private var currentOnStart: (() -> Unit)? = null
    private var currentOnDone: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var isStopped = false

    private data class PendingUtterance(
        val text: String,
        val onStart: (() -> Unit)?,
        val onDone: (() -> Unit)?
    )

    init {
        initEngine()
    }

    private fun initEngine() {
        val googleTtsPackage = "com.google.android.tts"

        // Check if Google Speech Services engine is available
        val initListener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady.set(true)
                configureAudioAttributes()
                Timber.i("NaturalVoiceEngine: Google Neural TTS initialized successfully")

                // If a speech request arrived before initialization finished, speak it now
                mainHandler.post {
                    pendingSpeech?.let { pending ->
                        pendingSpeech = null
                        if (!isStopped) {
                            speak(pending.text, pending.onStart, pending.onDone)
                        }
                    }
                }
            } else {
                Timber.w("NaturalVoiceEngine: TTS engine init failed with status $status")
            }
        }

        try {
            // First try to instantiate using Google's Neural TTS Engine explicitly
            textToSpeech = TextToSpeech(appContext, initListener, googleTtsPackage)
        } catch (e: Exception) {
            Timber.w(e, "NaturalVoiceEngine: Could not bind com.google.android.tts, trying default system TTS")
            try {
                textToSpeech = TextToSpeech(appContext, initListener)
            } catch (fallbackEx: Exception) {
                Timber.e(fallbackEx, "NaturalVoiceEngine: Fatal TTS initialization failure")
            }
        }

        setupUtteranceListener()
    }

    private fun configureAudioAttributes() {
        textToSpeech?.let { tts ->
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .build()
                tts.setAudioAttributes(audioAttributes)
                tts.setSpeechRate(0.98f) // Relaxed, fluent human conversational tempo
                tts.setPitch(1.0f)       // Natural human voice pitch
            } catch (e: Exception) {
                Timber.w(e, "NaturalVoiceEngine: Failed to apply audio attributes")
            }
        }
    }

    private fun setupUtteranceListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post {
                    if (!isStopped) {
                        currentOnStart?.invoke()
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    if (!isStopped) {
                        currentOnDone?.invoke()
                    }
                    currentOnStart = null
                    currentOnDone = null
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    if (!isStopped) {
                        currentOnDone?.invoke()
                    }
                    currentOnStart = null
                    currentOnDone = null
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Timber.w("NaturalVoiceEngine utterance error: code=$errorCode")
                mainHandler.post {
                    if (!isStopped) {
                        currentOnDone?.invoke()
                    }
                    currentOnStart = null
                    currentOnDone = null
                }
            }
        })
    }

    /**
     * Speaks the provided text using high-fidelity natural neural speech.
     * Guaranteed single voice, zero robotic duplicate speaking.
     */
    fun speak(
        text: String,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null
    ) {
        stop()

        val cleanText = sanitizeForSpeech(text)
        if (cleanText.isBlank()) {
            onDone?.invoke()
            return
        }

        isStopped = false
        currentOnStart = onStart
        currentOnDone = onDone

        val tts = textToSpeech
        if (tts == null || !isTtsReady.get()) {
            // Queue pending utterance until TTS engine completes async binding
            pendingSpeech = PendingUtterance(cleanText, onStart, onDone)
            return
        }

        try {
            val targetLocale = detectLocale(cleanText)

            // Select language
            val langResult = tts.setLanguage(targetLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to English if target language pack is missing
                tts.setLanguage(Locale("en", "IN"))
            }

            // Select highest quality Neural Network voice for the target language
            selectBestNeuralVoice(tts, targetLocale)

            val utteranceId = "liri_speech_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }

            val speakResult = tts.speak(
                cleanText,
                TextToSpeech.QUEUE_FLUSH,
                params,
                utteranceId
            )

            if (speakResult != TextToSpeech.SUCCESS) {
                Timber.w("NaturalVoiceEngine: speak failed with code $speakResult")
                onDone?.invoke()
            }
        } catch (e: Exception) {
            Timber.w(e, "NaturalVoiceEngine: Exception during speak")
            onDone?.invoke()
        }
    }

    /**
     * Selects Google's natural neural network voice for the locale.
     * Prioritizes neural network models (e.g. `te-in-x-*-network`, `hi-in-x-*-network`) which produce
     * Gemini/ChatGPT quality audio instead of mechanical diphone synthesis.
     */
    private fun selectBestNeuralVoice(tts: TextToSpeech, targetLocale: Locale) {
        try {
            val voices = tts.voices ?: return
            val matchingVoices = voices.filter { v ->
                v.locale.language.equals(targetLocale.language, ignoreCase = true)
            }

            if (matchingVoices.isEmpty()) return

            // Sort to find the highest-quality neural voice
            val bestVoice = matchingVoices.maxWithOrNull(
                compareBy<Voice> { voice ->
                    val name = voice.name.lowercase(Locale.ROOT)
                    when {
                        // Highest priority: Google network neural voice
                        name.contains("network") -> 100
                        name.contains("neural") -> 90
                        name.contains("natural") -> 80
                        else -> 10
                    }
                }.thenBy { voice ->
                    when (voice.quality) {
                        Voice.QUALITY_VERY_HIGH -> 50
                        Voice.QUALITY_HIGH -> 40
                        Voice.QUALITY_NORMAL -> 30
                        else -> 10
                    }
                }.thenBy { voice ->
                    // Penalize voices requiring extra download if not installed
                    if (voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true) -100 else 10
                }
            )

            if (bestVoice != null) {
                tts.voice = bestVoice
                Timber.d("NaturalVoiceEngine: Selected voice=${bestVoice.name}, quality=${bestVoice.quality}")
            }
        } catch (e: Exception) {
            Timber.d(e, "NaturalVoiceEngine: Voice selection fallback")
        }
    }

    /**
     * Immediately silences speech and flushes audio queue.
     */
    fun stop() {
        isStopped = true
        pendingSpeech = null
        currentOnStart = null
        currentOnDone = null

        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
    }

    /**
     * Shuts down the engine and releases audio resources.
     */
    fun shutdown() {
        stop()
        try {
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (_: Exception) {}
        scope.cancel()
    }

    companion object {
        /**
         * Detects the target locale based on unicode script analysis.
         */
        fun detectLocale(text: String): Locale {
            return when {
                text.any { it in '\u0C00'..'\u0C7F' } -> Locale("te", "IN") // Telugu
                text.any { it in '\u0900'..'\u097F' } -> Locale("hi", "IN") // Hindi
                else -> Locale("en", "IN")                                  // Indian English
            }
        }

        /**
         * Detects language code string ("te", "hi", "en-IN").
         */
        fun detectLanguageCode(text: String): String {
            return when {
                text.any { it in '\u0C00'..'\u0C7F' } -> "te"
                text.any { it in '\u0900'..'\u097F' } -> "hi"
                else -> "en-IN"
            }
        }

        /**
         * Preprocesses and sanitizes text for natural conversational speech:
         * - Strips markdown asterisks, hashes, backticks, bullet symbols
         * - Strips emojis so the TTS engine doesn't read out bizarre emoji names
         * - Normalizes conversational spacing and pauses
         */
        fun sanitizeForSpeech(text: String): String {
            return text
                // Remove Markdown bold/italic: **text** or *text*
                .replace(Regex("\\*+([^*]+)\\*+"), "$1")
                // Remove Markdown headers: # Header
                .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "")
                // Remove code blocks and inline backticks
                .replace("`", "")
                // Remove bullet point markers
                .replace(Regex("^[•\\-*]\\s+", RegexOption.MULTILINE), "")
                // Remove Markdown links: [label](url) -> label
                .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
                // Remove emojis (Unicode surrogate pairs and standard emoji ranges)
                .replace(Regex("[\\p{So}\\p{Cn}\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"), "")
                // Clean up duplicate spaces and newlines
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        /**
         * Backward compatibility helper for phrase splitting if needed.
         */
        fun splitIntoPhrases(text: String, maxPhraseLen: Int = 130): List<String> {
            val clean = sanitizeForSpeech(text)
            if (clean.length <= maxPhraseLen) return listOf(clean)

            val sentences = clean.split(Regex("(?<=[.!?,;।\n])\\s+"))
            val result = mutableListOf<String>()
            var current = StringBuilder()

            for (part in sentences) {
                val trimmed = part.trim()
                if (trimmed.isEmpty()) continue

                if (current.isEmpty()) {
                    current.append(trimmed)
                } else if (current.length + 1 + trimmed.length <= maxPhraseLen) {
                    current.append(" ").append(trimmed)
                } else {
                    result.add(current.toString())
                    current = StringBuilder(trimmed)
                }
            }

            if (current.isNotEmpty()) {
                result.add(current.toString())
            }

            return if (result.isEmpty() && text.isNotBlank()) listOf(text.take(maxPhraseLen)) else result
        }
    }
}
