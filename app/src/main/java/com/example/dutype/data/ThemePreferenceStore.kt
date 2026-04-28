package com.example.dutype.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * User preference for app theme. SYSTEM follows the OS, LIGHT and DARK
 * force the app palette regardless of the device setting.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromStorage(value: String?): ThemeMode = when (value) {
            LIGHT.name -> LIGHT
            DARK.name -> DARK
            else -> SYSTEM
        }
    }
}

/**
 * Persists the user's chosen theme mode (System / Light / Dark) in DataStore.
 * Backed by its own preferences file so it does not collide with other
 * feature stores.
 */
@Singleton
class ThemePreferenceStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
            name = "theme_preferences"
        )

        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        ThemeMode.fromStorage(prefs[THEME_MODE_KEY])
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }
}
