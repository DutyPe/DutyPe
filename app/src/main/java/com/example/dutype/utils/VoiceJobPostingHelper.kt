package com.example.dutype.utils

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.result.ActivityResultLauncher
import timber.log.Timber
import java.util.Locale

/**
 * Voice Job Posting Helper
 * Handles voice recognition and text-to-speech for conversational job posting
 * 
 * Features:
 * - Multi-language support (English, Hindi, Telugu)
 * - Text-to-speech for questions
 * - Speech-to-text for answers
 * - Conversational flow management
 */
class VoiceJobPostingHelper(
    private val activity: Activity,
    private val onInitialized: (Boolean) -> Unit
) {
    
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    
    companion object {
        const val REQUEST_CODE_VOICE_INPUT = 2001
    }
    
    init {
        initializeTextToSpeech()
    }
    
    /**
     * Initialize Text-to-Speech engine
     */
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(activity) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale("en", "IN"))
                isInitialized = result != TextToSpeech.LANG_MISSING_DATA && 
                               result != TextToSpeech.LANG_NOT_SUPPORTED
                onInitialized(isInitialized)
                Timber.d("🎤 TTS initialized: $isInitialized")
            } else {
                isInitialized = false
                onInitialized(false)
                Timber.e("🎤 TTS initialization failed")
            }
        }
    }
    
    /**
     * Speak a question to the user
     */
    fun speak(text: String, languageCode: String = "en-IN") {
        if (!isInitialized) {
            Timber.w("🎤 TTS not initialized")
            return
        }
        
        // Set language based on code
        val locale = when (languageCode) {
            "hi-IN" -> Locale("hi", "IN")
            "te-IN" -> Locale("te", "IN")
            else -> Locale("en", "IN")
        }
        
        textToSpeech?.language = locale
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "question")
        Timber.d("🎤 Speaking: $text")
    }
    
    /**
     * Stop speaking
     */
    fun stopSpeaking() {
        textToSpeech?.stop()
    }
    
    /**
     * Start voice recognition for user's answer
     */
    fun startVoiceRecognition(
        launcher: ActivityResultLauncher<Intent>,
        prompt: String = "Speak your answer...",
        languageCode: String = "en-IN"
    ) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        try {
            launcher.launch(intent)
            Timber.d("🎤 Voice recognition started")
        } catch (e: Exception) {
            Timber.e(e, "🎤 Voice recognition failed")
        }
    }
    
    /**
     * Extract text from speech recognition result
     */
    fun extractText(data: Intent?): String? {
        val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        return results?.firstOrNull()
    }
    
    /**
     * Check if voice recognition is available
     */
    fun isVoiceRecognitionAvailable(): Boolean {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        return intent.resolveActivity(activity.packageManager) != null
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        Timber.d("🎤 Voice helper cleaned up")
    }
}
