package com.example.dutype.database.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Provides a persistent, device-bound passphrase for the encrypted Room
 * database. The passphrase is generated on first launch and stored in
 * EncryptedSharedPreferences (AES-256-GCM, key wrapped by Android Keystore).
 */
object DatabasePassphraseProvider {
    private const val PREFS_NAME = "dutype_db_secrets"
    private const val KEY_DB_PASSPHRASE = "db_passphrase_v1"
    private const val PASSPHRASE_BYTES = 32

    fun getPassphrase(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
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
}
