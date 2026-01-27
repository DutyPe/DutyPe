package com.example.dutype.utils

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.result.ActivityResultLauncher
import java.util.Locale

/**
 * Voice Search Helper - Integrates Google Speech Recognition
 * 
 * Supports multi-language voice input for job search
 * Uses Android's built-in speech recognition service
 */
object VoiceSearchHelper {
    
    const val REQUEST_CODE_SPEECH_INPUT = 1001
    
    /**
     * Start voice recognition for job search
     * Supports any language configured on the device
     * Default: English (India) - "en-IN"
     */
    fun startVoiceRecognition(
        activity: Activity,
        launcher: ActivityResultLauncher<Intent>,
        languageCode: String = "en-IN" // English (India) as default
    ) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            // Use free form speech recognition
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            
            // Set language to English (India) for better Indian accent recognition
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
            
            // Prompt text
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Say job title, location, or category..."
            )
            
            // Get multiple results for better accuracy
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            
            // Partial results for real-time feedback
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        try {
            launcher.launch(intent)
        } catch (e: Exception) {
            // Speech recognition not available
            timber.log.Timber.e(e, "Voice recognition not available")
        }
    }
    
    /**
     * Extract search query from speech recognition result
     */
    fun extractSearchQuery(data: Intent?): String? {
        val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        return results?.firstOrNull()
    }
    
    /**
     * Check if speech recognition is available on device
     */
    fun isSpeechRecognitionAvailable(activity: Activity): Boolean {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        val packageManager = activity.packageManager
        return intent.resolveActivity(packageManager) != null
    }
}
