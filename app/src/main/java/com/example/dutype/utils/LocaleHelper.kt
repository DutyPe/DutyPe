package com.example.dutype.utils

import timber.log.Timber

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Locale

/**
 * LocaleHelper - Language Selector Utility
 *
 * Supports English (en), Telugu (te) and Hindi (hi).
 * Persists language preference and applies it app-wide.
 *
 * Any language added here must also be listed in `resourceConfigurations`
 * in app/build.gradle.kts, or its resources are stripped from the build.
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
    const val LANGUAGE_HINDI = "hi"
    
    /**
     * Get the currently selected language
     * Always uses Device-Protected storage to guarantee access even when device is locked (Direct Boot).
     */
    fun getLanguage(context: Context): String {
        return try {
            val dePrefs = SafeStorageUtils.getSafeSharedPreferences(context, PREF_NAME, forceDeviceProtected = true)
            var lang = dePrefs.getString(KEY_LANGUAGE, null)
            if (lang == null && SafeStorageUtils.isUserUnlocked(context)) {
                // If not yet in DE storage, opportunistically read from CE storage and migrate
                val cePrefs = SafeStorageUtils.getSafeSharedPreferences(context, PREF_NAME, forceDeviceProtected = false)
                lang = cePrefs.getString(KEY_LANGUAGE, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
                dePrefs.edit().putString(KEY_LANGUAGE, lang).apply()
            }
            lang ?: LANGUAGE_ENGLISH
        } catch (_: Exception) {
            LANGUAGE_ENGLISH
        }
    }
    
    /**
     * Save the selected language preference.
     *
     * Persists to both Device-Protected (DE) storage (for background FCM/Direct Boot)
     * and Credential-Protected (CE) storage.
     *
     * Also writes the language to the user's Firestore profile (best-effort,
     * fire-and-forget) so server-side notification fan-out can localise pushes,
     * and re-subscribes the device to the matching language-specific FCM topics
     * so admin broadcasts deliver the right copy.
     */
    fun saveLanguage(context: Context, language: String) {
        val previous = getLanguage(context)
        
        // 1. Save to Device-Protected storage (Direct Boot & lock screen safe)
        runCatching {
            SafeStorageUtils.getSafeSharedPreferences(context, PREF_NAME, forceDeviceProtected = true)
                .edit()
                .putString(KEY_LANGUAGE, language)
                .apply()
        }

        // 2. Save to standard storage if user unlocked
        runCatching {
            if (SafeStorageUtils.isUserUnlocked(context)) {
                SafeStorageUtils.getSafeSharedPreferences(context, PREF_NAME, forceDeviceProtected = false)
                    .edit()
                    .putString(KEY_LANGUAGE, language)
                    .apply()
            }
        }

        if (previous == language) return

        // Best-effort sync to Firestore so cloud functions can read user_tokens/{uid}.language.
        // Silent failure is acceptable — local SharedPreferences remains the source of truth
        // for the UI; server-side localization just falls back to English.
        if (SafeStorageUtils.isUserUnlocked(context)) {
            runCatching {
                FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                    FirebaseFirestore.getInstance()
                        .collection(com.example.dutype.firestore.FirestoreSchema.UserTokens.COLLECTION)
                        .document(uid)
                        .set(
                            mapOf(
                                com.example.dutype.firestore.FirestoreSchema.UserTokens.LANGUAGE to language,
                                com.example.dutype.firestore.FirestoreSchema.UserTokens.UPDATED_AT to Timestamp.now()
                            ),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                }
            }
        }

        // Resubscribe FCM topics to the new language so admin broadcasts to e.g.
        // workers_hi / employers_hi / all_users_hi / app_updates_hi are delivered.
        runCatching {
            val messaging = FirebaseMessaging.getInstance()
            if (previous.isNotBlank()) {
                FCM_BASE_TOPICS.forEach { base ->
                    val cleanOld = "${base}_$previous".replace(Regex("[^a-zA-Z0-9-_.~%]"), "_").trim('_')
                    if (cleanOld.isNotBlank()) messaging.unsubscribeFromTopic(cleanOld)
                }
            }
            FCM_BASE_TOPICS.forEach { base ->
                val cleanNew = "${base}_$language".replace(Regex("[^a-zA-Z0-9-_.~%]"), "_").trim('_')
                if (cleanNew.isNotBlank()) messaging.subscribeToTopic(cleanNew)
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
        return try {
            val lang = language ?: getLanguage(context)
            val locale = Locale(lang)
            Locale.setDefault(locale)
            
            val config = Configuration(context.resources.configuration)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocales(LocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                config.locale = locale
            }

            @Suppress("DEPRECATION")
            runCatching { context.resources.updateConfiguration(config, context.resources.displayMetrics) }
            runCatching {
                @Suppress("DEPRECATION")
                context.applicationContext?.resources?.updateConfiguration(config, context.applicationContext.resources.displayMetrics)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.createConfigurationContext(config)
            } else {
                context
            }
        } catch (t: Throwable) {
            Timber.w(t, "LocaleHelper.setLocale fallback to original context")
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
     * Get a string resource in the user's currently selected language, regardless
     * of the system or activity context locale. Useful for background services and
     * notification builders where the host context may not have the right locale.
     */
    fun getLocalizedString(context: Context, @androidx.annotation.StringRes resId: Int, vararg formatArgs: Any): String {
        val localized = setLocale(context.applicationContext)
        return if (formatArgs.isEmpty()) {
            localized.getString(resId)
        } else {
            localized.getString(resId, *formatArgs)
        }
    }
    
    /**
     * Check if current language is Telugu
     */
    fun isTelugu(context: Context): Boolean {
        return getLanguage(context) == LANGUAGE_TELUGU
    }

    /**
     * Check if current language is Hindi
     */
    fun isHindi(context: Context): Boolean {
        return getLanguage(context) == LANGUAGE_HINDI
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
            LANGUAGE_HINDI -> "हिन्दी"
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
            LANGUAGE_HINDI -> "Hindi"
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
            LANGUAGE_HINDI -> "🇮🇳"
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
            LANGUAGE_HINDI to "हिन्दी (Hindi)",
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
