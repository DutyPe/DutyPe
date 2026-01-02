package com.example.dutype.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
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
    
    /**
     * Get the currently selected language
     */
    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
    }
    
    /**
     * Save the selected language preference
     */
    fun saveLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }
    
    /**
     * Set the app locale and return the updated context
     * Call this in Application.attachBaseContext() and Activity.attachBaseContext()
     */
    fun setLocale(context: Context, language: String? = null): Context {
        val lang = language ?: getLanguage(context)
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
