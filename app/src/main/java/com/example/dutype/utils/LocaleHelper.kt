package com.example.dutype.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Locale

/**
 * LocaleHelper - Language Selector Utility
 * 
 * Supports Telugu (te) and English (en) languages.
 * Persists language preference and applies it app-wide.
 * 
 * @author DutyPe Engineering Team
 * @since 2.2.0
 */
object LocaleHelper {
    
    private const val PREF_NAME = "dutype_language_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    
    // Supported languages
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_TELUGU = "te"
    // Legacy stored value normalized to English on read/write
    private const val LEGACY_LANGUAGE_HINDI = "hi"
    
    /**
     * Get the currently selected language
     */
    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val selected = prefs.getString(KEY_LANGUAGE, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
        return if (selected == LEGACY_LANGUAGE_HINDI) LANGUAGE_ENGLISH else selected
    }
    
    /**
     * Save the selected language preference.
     *
     * Also writes the language to the user's Firestore profile (best-effort,
     * fire-and-forget) so server-side notification fan-out can localise pushes,
     * and re-subscribes the device to the matching language-specific FCM topics
     * so admin broadcasts deliver the right copy.
     */
    fun saveLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val normalized = if (language == LEGACY_LANGUAGE_HINDI) LANGUAGE_ENGLISH else language
        val previous = prefs.getString(KEY_LANGUAGE, null)
            ?.let { if (it == LEGACY_LANGUAGE_HINDI) LANGUAGE_ENGLISH else it }
        prefs.edit().putString(KEY_LANGUAGE, normalized).apply()

        if (previous == normalized) return

        // Best-effort sync to Firestore so cloud functions can read users/{uid}.language.
        // Silent failure is acceptable — local SharedPreferences remains the source of truth
        // for the UI; server-side localization just falls back to English.
        runCatching {
            FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("language", normalized)
            }
        }

        // Resubscribe FCM topics to the new language so admin broadcasts to e.g.
        // workers_te / employers_te / all_users_te / app_updates_te are delivered.
        runCatching {
            val messaging = FirebaseMessaging.getInstance()
            if (previous != null) {
                FCM_BASE_TOPICS.forEach { base ->
                    messaging.unsubscribeFromTopic("${base}_$previous")
                }
            }
            FCM_BASE_TOPICS.forEach { base ->
                messaging.subscribeToTopic("${base}_$normalized")
            }
        }
    }

    /**
     * Base topic names that have language-suffixed variants (e.g. workers_te).
     * Mirrors the broadcast topics used by the admin notifications API.
     */
    private val FCM_BASE_TOPICS = listOf("all_users", "workers", "employers", "app_updates", "guest_users")
    
    /**
     * Set the app locale and return the updated context
     * Call this in Application.attachBaseContext() and Activity.attachBaseContext()
     */
    fun setLocale(context: Context, language: String? = null): Context {
        val selected = language ?: getLanguage(context)
        val lang = if (selected == LEGACY_LANGUAGE_HINDI) LANGUAGE_ENGLISH else selected
        val locale = Locale(lang)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * Update the locale for an existing context
     */
    fun updateLocale(context: Context, language: String): Context {
        saveLanguage(context, language)
        return setLocale(context, language)
    }
    
    /**
     * Check if current language is Telugu
     */
    fun isTelugu(context: Context): Boolean {
        return getLanguage(context) == LANGUAGE_TELUGU
    }
    
    /**
     * Check if current language is English
     */
    fun isEnglish(context: Context): Boolean {
        return getLanguage(context) == LANGUAGE_ENGLISH
    }

    /**
     * Get display name for a language code (in native script)
     */
    fun getLanguageDisplayName(languageCode: String): String {
        return when (languageCode) {
            LANGUAGE_TELUGU -> "తెలుగు"
            LANGUAGE_ENGLISH -> "English"
            else -> "English"
        }
    }
    
    /**
     * Get display name for a language code (in English)
     */
    fun getLanguageEnglishName(languageCode: String): String {
        return when (languageCode) {
            LANGUAGE_TELUGU -> "Telugu"
            LANGUAGE_ENGLISH -> "English"
            else -> "English"
        }
    }
    
    /**
     * Get flag emoji for a language code
     */
    fun getLanguageEmoji(languageCode: String): String {
        return when (languageCode) {
            LANGUAGE_TELUGU -> "🇮🇳"
            LANGUAGE_ENGLISH -> "🇬🇧"
            else -> "🇬🇧"
        }
    }
    
    /**
     * Get all supported languages
     */
    fun getSupportedLanguages(): List<Pair<String, String>> {
        return listOf(
            LANGUAGE_ENGLISH to "English",
            LANGUAGE_TELUGU to "తెలుగు (Telugu)"
        )
    }
    
    /**
     * Get localized text based on current language
     * Useful for inline translations
     */
    fun getLocalizedText(context: Context, englishText: String, teluguText: String): String {
        return if (isTelugu(context)) teluguText else englishText
    }
}
