package com.example.dutype.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Shared helpers for the small Preferences-DataStores in this package.
 *
 * Centralises the (try / withContext(IO) / Gson) boilerplate that
 * [ApplicationFormDataStore] and [JobDraftDataStore] previously duplicated
 * 6+ times each.
 */

internal val sharedGson: Gson = Gson()

/** Write a single preferences value on the IO dispatcher, swallowing + logging errors. */
internal suspend fun <T> DataStore<Preferences>.editIo(
    tag: String,
    block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        edit { prefs -> block(prefs) }
    } catch (e: Exception) {
        Timber.e(e, "❌ %s: edit failed", tag)
    }
}

/** Read the latest snapshot on the IO dispatcher, returning [default] on error. */
internal suspend fun <T> DataStore<Preferences>.readIo(
    tag: String,
    default: T,
    block: (Preferences) -> T
): T = withContext(Dispatchers.IO) {
    try {
        data.map(block).first()
    } catch (e: Exception) {
        Timber.e(e, "❌ %s: read failed", tag)
        default
    }
}

/** JSON-encode [value] to a String key. Errors are logged; preference is left unchanged. */
internal suspend inline fun <reified T> DataStore<Preferences>.saveJson(
    tag: String,
    key: androidx.datastore.preferences.core.Preferences.Key<String>,
    value: T
) = editIo<Unit>(tag) { prefs -> prefs[key] = sharedGson.toJson(value) }

/** Decode a JSON String key into [T]; returns [default] when missing or unparseable. */
internal suspend inline fun <reified T> DataStore<Preferences>.getJson(
    tag: String,
    key: androidx.datastore.preferences.core.Preferences.Key<String>,
    default: T
): T = readIo(tag, default) { prefs ->
    val json = prefs[key] ?: return@readIo default
    try {
        val type = object : TypeToken<T>() {}.type
        sharedGson.fromJson<T>(json, type) ?: default
    } catch (e: Exception) {
        Timber.e(e, "❌ %s: parse failed for %s", tag, key.name)
        default
    }
}
