package com.example.dutype.urgent

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Enterprise audio & vibration alert manager for urgent / instant jobs.
 *
 * Requirements:
 * 1. Plays system ringtone/alert + distinct vibration when urgent jobs arrive in worker's area.
 * 2. Works for online workers, including newly registered or freshly visiting workers.
 * 3. Once ignored or dismissed, it will NEVER make sound again for that urgent request.
 * 4. Can be stopped immediately when worker accepts, skips, or leaves the screen.
 */
object UrgentSoundAlertManager {
    private const val PREFS_NAME = "dutype_urgent_alerts"
    private const val KEY_ALERTED_IDS = "alerted_ids"
    private const val KEY_IGNORED_IDS = "ignored_ids"

    private var activeRingtone: Ringtone? = null
    private var stopJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val alertedInMemory = mutableSetOf<String>()
    private val ignoredInMemory = mutableSetOf<String>()

    @Synchronized
    fun isIgnored(context: Context, requestId: String): Boolean {
        if (requestId.isBlank()) return true
        if (ignoredInMemory.contains(requestId)) return true
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val set = prefs.getStringSet(KEY_IGNORED_IDS, emptySet()) ?: emptySet()
            if (set.contains(requestId)) {
                ignoredInMemory.add(requestId)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    @Synchronized
    fun hasAlerted(context: Context, requestId: String): Boolean {
        if (requestId.isBlank()) return true
        if (alertedInMemory.contains(requestId)) return true
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val set = prefs.getStringSet(KEY_ALERTED_IDS, emptySet()) ?: emptySet()
            if (set.contains(requestId)) {
                alertedInMemory.add(requestId)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    @Synchronized
    fun ignoreRequest(context: Context, requestId: String) {
        if (requestId.isBlank()) return
        stopSound()
        ignoredInMemory.add(requestId)
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = prefs.getStringSet(KEY_IGNORED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
            current.add(requestId)
            prefs.edit().putStringSet(KEY_IGNORED_IDS, current).apply()
        } catch (e: Exception) {
            Timber.e(e, "Error recording ignored urgent request")
        }
    }

    @Synchronized
    fun markAlerted(context: Context, requestId: String) {
        if (requestId.isBlank()) return
        alertedInMemory.add(requestId)
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = prefs.getStringSet(KEY_ALERTED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
            current.add(requestId)
            prefs.edit().putStringSet(KEY_ALERTED_IDS, current).apply()
        } catch (e: Exception) {
            Timber.e(e, "Error recording alerted urgent request")
        }
    }

    fun playAlertIfNew(context: Context, requestId: String, durationMs: Long = 4000L): Boolean {
        if (requestId.isBlank()) return false
        if (isIgnored(context, requestId) || hasAlerted(context, requestId)) {
            return false
        }
        markAlerted(context, requestId)
        playSoundAndVibration(context, durationMs)
        return true
    }

    fun playAlertForAnyNew(context: Context, requestIds: List<String>, durationMs: Long = 4000L): Boolean {
        val newId = requestIds.firstOrNull { it.isNotBlank() && !isIgnored(context, it) && !hasAlerted(context, it) }
        return if (newId != null) {
            playAlertIfNew(context, newId, durationMs)
        } else {
            false
        }
    }

    @Synchronized
    fun playSoundAndVibration(context: Context, durationMs: Long = 4000L) {
        stopSound()
        val appContext = context.applicationContext
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: Settings.System.DEFAULT_RINGTONE_URI

            val ringtone = RingtoneManager.getRingtone(appContext, alertUri)
            ringtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone?.play()
            activeRingtone = ringtone

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 300, 600), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 600, 300, 600), -1)
            }

            stopJob = scope.launch {
                delay(durationMs)
                stopSound()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to play urgent alert sound")
        }
    }

    @Synchronized
    fun stopSound() {
        stopJob?.cancel()
        stopJob = null
        try {
            activeRingtone?.stop()
        } catch (e: Exception) {
            Timber.e(e, "Error stopping ringtone")
        }
        activeRingtone = null
    }
}
