package com.example.dutype.database.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.dutype.database.DutyPeDatabase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.SecureRandom
import java.security.UnrecoverableKeyException

/**
 * Provides a persistent, device-bound passphrase for the encrypted Room
 * database. The passphrase is generated on first launch and stored in
 * EncryptedSharedPreferences (AES-256-GCM, key wrapped by Android Keystore).
 *
 * SELF-HEALING ARCHITECTURE:
 * Handles UnrecoverableKeyException, KeyPermanentlyInvalidatedException,
 * KeyStoreException, and Tink initialization failures caused by lock screen
 * changes, biometric updates, or OEM Keystore firmware bugs.
 *
 * On failure:
 * 1. Logs non-fatal event to Crashlytics / Timber for observability
 * 2. Deletes corrupted EncryptedSharedPreferences XML files
 * 3. Deletes corrupted MasterKey aliases from AndroidKeyStore
 * 4. Cleans up orphaned unopenable database files
 * 5. Re-initializes fresh encryption key or secondary private storage
 */
object DatabasePassphraseProvider {
    private const val PREFS_NAME = "dutype_db_secrets"
    private const val FALLBACK_PREFS_NAME = "dutype_db_secrets_fallback"
    private const val KEY_DB_PASSPHRASE = "db_passphrase_v1"
    private const val PASSPHRASE_BYTES = 32

    fun getPassphrase(context: Context): ByteArray {
        return try {
            getOrGenerateEncryptedPassphrase(context)
        } catch (e: UnrecoverableKeyException) {
            handleKeystoreFailure(context, "UnrecoverableKeyException", e)
        } catch (e: KeyPermanentlyInvalidatedException) {
            handleKeystoreFailure(context, "KeyPermanentlyInvalidatedException", e)
        } catch (e: KeyStoreException) {
            handleKeystoreFailure(context, "KeyStoreException", e)
        } catch (e: GeneralSecurityException) {
            handleKeystoreFailure(context, "GeneralSecurityException", e)
        } catch (e: IOException) {
            handleKeystoreFailure(context, "IOException", e)
        } catch (e: NullPointerException) {
            handleKeystoreFailure(context, "NullPointerException_TinkKeyset", e)
        } catch (e: IllegalStateException) {
            handleKeystoreFailure(context, "IllegalStateException_Keystore", e)
        } catch (e: Throwable) {
            handleKeystoreFailure(context, "UnexpectedKeystoreError", e)
        }
    }

    private fun handleKeystoreFailure(context: Context, errorType: String, error: Throwable): ByteArray {
        Timber.e(error, "[DatabasePassphraseProvider] $errorType encountered; executing self-healing recovery")
        recordKeystoreRecoveryEvent(errorType, error)
        return recoverPassphrase(context, error)
    }

    private fun getOrGenerateEncryptedPassphrase(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (existing != null) {
            return android.util.Base64.decode(existing, android.util.Base64.NO_WRAP)
        }

        val bytes = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_DB_PASSPHRASE, android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
            .apply()
        return bytes
    }

    private fun recoverPassphrase(context: Context, originalError: Throwable): ByteArray {
        // Step 1: Wipe corrupted SharedPreferences file
        deleteEncryptedPreferencesFile(context, PREFS_NAME)

        // Step 2: Delete corrupted MasterKey alias from AndroidKeyStore
        deleteAndroidKeyStoreEntries()

        // Step 3: Remove orphaned encrypted database files since old ciphertext is unreadable
        deleteDatabaseFiles(context)

        // Step 4: Invalidate cached auth/session if needed
        clearAuthPreferences(context)

        // Step 5: Attempt re-initialization with a fresh MasterKey and EncryptedSharedPreferences
        try {
            return getOrGenerateEncryptedPassphrase(context)
        } catch (recoveryError: Throwable) {
            Timber.e(recoveryError, "[DatabasePassphraseProvider] Fresh MasterKey creation failed; engaging secondary fallback")
            recordKeystoreRecoveryEvent("Fallback_StandardPrefs", recoveryError)
        }

        // Step 6: Secondary fallback — private preferences with cryptographic entropy
        return getOrGenerateFallbackPassphrase(context)
    }

    private fun getOrGenerateFallbackPassphrase(context: Context): ByteArray {
        val fallbackPrefs = context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        val existingFallback = fallbackPrefs.getString(KEY_DB_PASSPHRASE, null)
        if (existingFallback != null) {
            return android.util.Base64.decode(existingFallback, android.util.Base64.NO_WRAP)
        }

        val bytes = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        fallbackPrefs.edit()
            .putString(KEY_DB_PASSPHRASE, android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
            .apply()
        return bytes
    }

    fun deleteEncryptedPreferencesFile(context: Context, prefsName: String = PREFS_NAME) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(prefsName)
            } else {
                val sharedPrefsFile = File(context.filesDir.parent, "shared_prefs/$prefsName.xml")
                if (sharedPrefsFile.exists()) sharedPrefsFile.delete()
                val backupFile = File(context.filesDir.parent, "shared_prefs/$prefsName.bak")
                if (backupFile.exists()) backupFile.delete()
            }
            Timber.i("[DatabasePassphraseProvider] Cleared SharedPreferences file: $prefsName")
        } catch (e: Exception) {
            Timber.w(e, "[DatabasePassphraseProvider] Failed to delete SharedPreferences file: $prefsName")
        }
    }

    fun deleteAndroidKeyStoreEntries() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            if (keyStore.containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
                keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                Timber.i("[DatabasePassphraseProvider] Deleted default master key alias")
            }

            // Clean any matching androidx or dutype security aliases
            val aliases = keyStore.aliases()
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement()
                if (alias.startsWith("_androidx_security_") || alias.contains("dutype")) {
                    try {
                        keyStore.deleteEntry(alias)
                        Timber.i("[DatabasePassphraseProvider] Deleted Keystore alias: $alias")
                    } catch (e: Exception) {
                        Timber.w(e, "[DatabasePassphraseProvider] Failed to delete alias: $alias")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "[DatabasePassphraseProvider] Failed to clean AndroidKeyStore entries")
        }
    }

    private fun deleteDatabaseFiles(context: Context) {
        try {
            val databaseName = DutyPeDatabase.DATABASE_NAME
            val databaseFile = context.getDatabasePath(databaseName)
            context.deleteDatabase(databaseName)
            listOf(
                databaseFile,
                File("${databaseFile.path}-journal"),
                File("${databaseFile.path}-shm"),
                File("${databaseFile.path}-wal")
            ).forEach { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
            Timber.i("[DatabasePassphraseProvider] Cleaned database files on disk")
        } catch (e: Exception) {
            Timber.w(e, "[DatabasePassphraseProvider] Failed to delete database files")
        }
    }

    private fun clearAuthPreferences(context: Context) {
        try {
            context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        } catch (e: Exception) {
            Timber.w(e, "[DatabasePassphraseProvider] Failed to clear auth/session preferences")
        }
    }

    private fun recordKeystoreRecoveryEvent(errorType: String, error: Throwable) {
        try {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("keystore_error_type", errorType)
                setCustomKey("keystore_recovery_triggered", true)
                log("[DatabasePassphraseProvider] Keystore recovery triggered due to $errorType: ${error.message}")
                recordException(error)
            }
        } catch (_: Exception) {
            // Crashlytics not initialized or disabled in debug
        }
    }
}
