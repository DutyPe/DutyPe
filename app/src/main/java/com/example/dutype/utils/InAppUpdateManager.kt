package com.example.dutype.utils

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(name = "in_app_update")

@Singleton
class InAppUpdateManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private val KEY_LAST_UPDATE_CHECK = longPreferencesKey("last_update_check_time")
        private val KEY_UPDATE_DISMISSED_COUNT = longPreferencesKey("update_dismissed_count")

        const val UPDATE_TYPE_FLEXIBLE = 0
        const val UPDATE_TYPE_IMMEDIATE = 1
    }

    suspend fun checkForUpdate(
        activity: Activity,
        activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>,
        onUpdateAvailable: (Any, Int) -> Unit = { _, _ -> },
        onNoUpdate: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        updateLastCheckTime()
        Timber.i("Native Play in-app updates disabled to keep release dex below 7 MB")
        onNoUpdate()
    }

    fun startUpdate(
        activity: Activity,
        appUpdateInfo: Any,
        updateType: Int,
        activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        Timber.d("Native Play in-app update flow disabled")
    }

    fun registerFlexibleUpdateListener(
        onDownloading: (bytesDownloaded: Long, totalBytes: Long) -> Unit = { _, _ -> },
        onDownloaded: () -> Unit = {},
        onInstalling: () -> Unit = {},
        onInstalled: () -> Unit = {},
        onFailed: (errorCode: Int) -> Unit = {},
        onCanceled: () -> Unit = {}
    ) {
        Timber.d("Native Play flexible update listener disabled")
    }

    fun unregisterFlexibleUpdateListener() {
        Timber.d("Native Play flexible update listener disabled")
    }

    fun completeFlexibleUpdate() {
        Timber.d("Native Play flexible update completion disabled")
    }

    suspend fun checkForPendingUpdate(onPendingUpdate: () -> Unit = {}) {
        Timber.d("Native Play pending update check disabled")
    }

    private suspend fun updateLastCheckTime() {
        context.updateDataStore.edit { prefs ->
            prefs[KEY_LAST_UPDATE_CHECK] = System.currentTimeMillis()
        }
    }

    suspend fun trackUpdateDismissal() {
        context.updateDataStore.edit { prefs ->
            val current = prefs[KEY_UPDATE_DISMISSED_COUNT] ?: 0
            prefs[KEY_UPDATE_DISMISSED_COUNT] = current + 1
        }
        Timber.d("Update dismissed")
    }

    suspend fun getUpdateStats(): UpdateStats {
        val prefs = context.updateDataStore.data.first()
        return UpdateStats(
            lastCheckTime = prefs[KEY_LAST_UPDATE_CHECK] ?: 0,
            dismissCount = prefs[KEY_UPDATE_DISMISSED_COUNT] ?: 0
        )
    }
}

data class UpdateStats(
    val lastCheckTime: Long,
    val dismissCount: Long
)
