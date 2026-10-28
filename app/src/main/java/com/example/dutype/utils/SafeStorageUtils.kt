package com.example.dutype.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.UserManager
import timber.log.Timber

/**
 * Enterprise Safe Storage & Direct Boot Utility
 *
 * Addresses Android 7.0+ through Android 16/17 File-Based Encryption (FBE) and strict OEM
 * battery/storage policies (such as Vivo Funtouch OS / OriginOS).
 *
 * CRITICAL DIRECT BOOT CONTEXT:
 * When an Android device is locked or in low-power sleep:
 * 1. Default Credential-Protected (CE) storage is strictly encrypted and inaccessible.
 * 2. Accessing standard SharedPreferences or database/file paths while locked throws
 *    [IllegalStateException] or blocks the binder/main looper waiting on user credentials.
 * 3. On Vivo devices, aggressive power management kills blocked background threads or
 *    triggers `android.os.MessageQueue.nativePollOnce` ANRs.
 *
 * This utility ensures background push notifications (FCM), BroadcastReceivers, and
 * background Workers always use Device-Protected (DE) Storage and never crash or hang.
 */
object SafeStorageUtils {

    private const val PREFS_DE_AUTH = "dutype_de_auth_state"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    /**
     * Checks whether the user has unlocked the device since boot.
     * On Android N (API 24)+, queries [UserManager.isUserUnlocked].
     */
    fun isUserUnlocked(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
                userManager?.isUserUnlocked ?: true
            } catch (e: Exception) {
                Timber.w(e, "SafeStorageUtils: Error querying UserManager.isUserUnlocked")
                true
            }
        } else {
            true
        }
    }

    /**
     * Returns a Device-Protected (DE) storage Context that is safe to use even when
     * the phone is locked.
     */
    fun getDeviceProtectedContext(context: Context): Context {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                if (context.isDeviceProtectedStorage) {
                    context
                } else {
                    context.createDeviceProtectedStorageContext()
                }
            } catch (e: Exception) {
                Timber.w(e, "SafeStorageUtils: createDeviceProtectedStorageContext failed, falling back")
                context
            }
        } else {
            context
        }
    }

    /**
     * Returns a safe Context: the standard context if unlocked, or the Device-Protected
     * context if locked.
     */
    fun getStorageContext(context: Context): Context {
        return if (isUserUnlocked(context)) {
            context
        } else {
            getDeviceProtectedContext(context)
        }
    }

    /**
     * Safely retrieves [SharedPreferences] with multi-tier fallback:
     * 1. Uses Device-Protected Context if [forceDeviceProtected] is true or if user is locked.
     * 2. Catches [IllegalStateException], [SecurityException], and [Exception].
     * 3. Falls back gracefully to DE storage.
     * 4. If all filesystem access fails (OEM deep sleep lockout), falls back to [InMemorySharedPreferences].
     */
    fun getSafeSharedPreferences(
        context: Context,
        name: String,
        mode: Int = Context.MODE_PRIVATE,
        forceDeviceProtected: Boolean = false
    ): SharedPreferences {
        val targetContext = if (forceDeviceProtected || !isUserUnlocked(context)) {
            getDeviceProtectedContext(context)
        } else {
            context
        }

        return try {
            targetContext.getSharedPreferences(name, mode)
        } catch (e: Exception) {
            Timber.w(e, "SafeStorageUtils: Direct SharedPreferences access failed for '$name', trying DE fallback")
            try {
                getDeviceProtectedContext(context).getSharedPreferences(name, mode)
            } catch (fallbackError: Exception) {
                Timber.e(fallbackError, "SafeStorageUtils: DE SharedPreferences access also failed for '$name', using in-memory store")
                InMemorySharedPreferences()
            }
        }
    }

    /**
     * Shortcut to explicitly obtain Device-Protected SharedPreferences.
     */
    fun getDeviceProtectedSharedPreferences(
        context: Context,
        name: String,
        mode: Int = Context.MODE_PRIVATE
    ): SharedPreferences = getSafeSharedPreferences(context, name, mode, forceDeviceProtected = true)

    /**
     * Migrates a SharedPreferences file from Credential-Protected storage to Device-Protected
     * storage if the user is unlocked. Safe and idempotent.
     */
    fun migrateSharedPreferencesToDeviceProtected(context: Context, name: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return try {
                if (isUserUnlocked(context)) {
                    val deContext = getDeviceProtectedContext(context)
                    deContext.moveSharedPreferencesFrom(context, name)
                } else {
                    false
                }
            } catch (e: Exception) {
                Timber.w(e, "SafeStorageUtils: Failed to migrate SharedPreferences '$name' to DE storage")
                false
            }
        }
        return false
    }

    /**
     * Records authentication state into Device-Protected storage.
     * This allows background push services (FCM) to check whether the user is logged in
     * without attempting to query FirebaseAuth (which accesses encrypted CE storage).
     */
    fun setDeviceProtectedUserLoggedIn(context: Context, isLoggedIn: Boolean) {
        try {
            getDeviceProtectedSharedPreferences(context, PREFS_DE_AUTH)
                .edit()
                .putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
                .apply()
        } catch (e: Exception) {
            Timber.w(e, "SafeStorageUtils: Failed to record DE user login state")
        }
    }

    /**
     * Reads the cached authentication state from Device-Protected storage.
     * Safe to call while the device is locked.
     */
    fun isDeviceProtectedUserLoggedIn(context: Context): Boolean {
        return try {
            getDeviceProtectedSharedPreferences(context, PREFS_DE_AUTH)
                .getBoolean(KEY_IS_LOGGED_IN, false)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * In-memory fallback SharedPreferences to prevent unhandled crashes on extreme OEM
     * power-saving states where the disk filesystem is temporarily read-only or blocked.
     */
    class InMemorySharedPreferences : SharedPreferences {
        private val memory = mutableMapOf<String, Any?>()
        private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

        override fun getAll(): MutableMap<String, *> = synchronized(memory) { HashMap(memory) }
        override fun getString(key: String?, defValue: String?): String? = synchronized(memory) { memory[key] as? String ?: defValue }
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            synchronized(memory) {
                @Suppress("UNCHECKED_CAST")
                (memory[key] as? Set<String>)?.toMutableSet() ?: defValues
            }
        override fun getInt(key: String?, defValue: Int): Int = synchronized(memory) { memory[key] as? Int ?: defValue }
        override fun getLong(key: String?, defValue: Long): Long = synchronized(memory) { memory[key] as? Long ?: defValue }
        override fun getFloat(key: String?, defValue: Float): Float = synchronized(memory) { memory[key] as? Float ?: defValue }
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = synchronized(memory) { memory[key] as? Boolean ?: defValue }
        override fun contains(key: String?): Boolean = synchronized(memory) { memory.containsKey(key) }
        override fun edit(): SharedPreferences.Editor = InMemoryEditor()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            listener?.let { synchronized(listeners) { listeners.add(it) } }
        }
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            listener?.let { synchronized(listeners) { listeners.remove(it) } }
        }

        private inner class InMemoryEditor : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clearRequested = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { pending[key.orEmpty()] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply { pending[key.orEmpty()] = values?.toSet() }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { pending[key.orEmpty()] = value }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { pending[key.orEmpty()] = value }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { pending[key.orEmpty()] = value }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { pending[key.orEmpty()] = value }
            override fun remove(key: String?): SharedPreferences.Editor = apply { pending[key.orEmpty()] = this }
            override fun clear(): SharedPreferences.Editor = apply { clearRequested = true }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                val modifiedKeys = mutableListOf<String>()
                synchronized(memory) {
                    if (clearRequested) {
                        modifiedKeys.addAll(memory.keys)
                        memory.clear()
                        clearRequested = false
                    }
                    for ((k, v) in pending) {
                        if (v === this) {
                            memory.remove(k)
                        } else {
                            memory[k] = v
                        }
                        modifiedKeys.add(k)
                    }
                    pending.clear()
                }
                synchronized(listeners) {
                    for (listener in listeners) {
                        for (key in modifiedKeys) {
                            listener.onSharedPreferenceChanged(this@InMemorySharedPreferences, key)
                        }
                    }
                }
            }
        }
    }
}
