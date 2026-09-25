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
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-App Update Manager
 *
 * Implements Google Play In-App Updates API:
 * - IMMEDIATE updates: For critical updates (security, major bug fixes)
 * - FLEXIBLE updates: For background downloads (user can continue using app)
 */
private val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(name = "in_app_update")

@Singleton
class InAppUpdateManager @Inject constructor(
    private val context: Context
) {
    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(context)
    }

    companion object {
        private val KEY_LAST_UPDATE_CHECK = longPreferencesKey("last_update_check_time")
        private val KEY_UPDATE_DISMISSED_COUNT = longPreferencesKey("update_dismissed_count")

        const val UPDATE_TYPE_FLEXIBLE = AppUpdateType.FLEXIBLE
        const val UPDATE_TYPE_IMMEDIATE = AppUpdateType.IMMEDIATE

        // Update staleness thresholds (in days)
        private const val DAYS_FOR_FLEXIBLE_UPDATE = 7
        private const val DAYS_FOR_IMMEDIATE_UPDATE = 3

        // Priority thresholds
        private const val PRIORITY_CRITICAL = 5
        private const val PRIORITY_HIGH = 4
        private const val PRIORITY_MEDIUM = 3

        // Minimum time between checks (in hours) - 0 checks on every resume
        private const val MIN_HOURS_BETWEEN_CHECKS = 0
    }

    private var installStateListener: InstallStateUpdatedListener? = null

    /**
     * Check if update is available on Google Play Store and initiate update flow.
     */
    suspend fun checkForUpdate(
        activity: Activity,
        activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>,
        onUpdateAvailable: (AppUpdateInfo, Int) -> Unit = { _, _ -> },
        onNoUpdate: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        try {
            Timber.i("🔄 IN-APP UPDATE: checkForUpdate() called")

            if (!shouldCheckForUpdate()) {
                Timber.d("🔄 IN-APP UPDATE: Skipping check - checked recently")
                onNoUpdate()
                return
            }

            updateLastCheckTime()

            val appUpdateInfoTask = appUpdateManager.appUpdateInfo

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                when (appUpdateInfo.updateAvailability()) {
                    UpdateAvailability.UPDATE_AVAILABLE -> {
                        val updateType = determineUpdateType(appUpdateInfo)
                        if (updateType == null) {
                            Timber.w("🔄 IN-APP UPDATE: Update available but no allowed type from Play Core")
                            onNoUpdate()
                            return@addOnSuccessListener
                        }
                        Timber.i("🔄 IN-APP UPDATE: Update available! Type: ${if (updateType == AppUpdateType.IMMEDIATE) "IMMEDIATE" else "FLEXIBLE"}")
                        Timber.i("🔄 IN-APP UPDATE: Priority: ${appUpdateInfo.updatePriority()}, Staleness: ${appUpdateInfo.clientVersionStalenessDays()} days")

                        onUpdateAvailable(appUpdateInfo, updateType)

                        // Auto-start update based on type
                        startUpdate(activity, appUpdateInfo, updateType, activityResultLauncher)
                    }
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        Timber.i("🔄 IN-APP UPDATE: Update in progress - resuming")
                        startUpdate(activity, appUpdateInfo, AppUpdateType.IMMEDIATE, activityResultLauncher)
                    }
                    UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                        Timber.d("✅ IN-APP UPDATE: App is up to date on Google Play")
                        onNoUpdate()
                    }
                    else -> {
                        Timber.d("🔄 IN-APP UPDATE: Update availability code: ${appUpdateInfo.updateAvailability()}")
                        onNoUpdate()
                    }
                }
            }.addOnFailureListener { exception ->
                val errorMessage = exception.message ?: ""
                if (errorMessage.contains("-10") || errorMessage.contains("ERROR_APP_NOT_OWNED")) {
                    Timber.d("ℹ️ IN-APP UPDATE: Skipped (app not installed from Play Store or debug build)")
                } else {
                    Timber.w(exception, "⚠️ IN-APP UPDATE: Check failed")
                }
                onError(exception)
            }
        } catch (e: Exception) {
            Timber.w(e, "⚠️ IN-APP UPDATE: Error during check")
            onError(e)
        }
    }

    /**
     * Determine update type based on priority and staleness
     */
    private fun determineUpdateType(appUpdateInfo: AppUpdateInfo): Int? {
        val priority = appUpdateInfo.updatePriority()
        val stalenessDays = appUpdateInfo.clientVersionStalenessDays() ?: 0
        val immediateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
        val flexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

        Timber.d("🔄 IN-APP UPDATE: Allowed types - immediate=$immediateAllowed, flexible=$flexibleAllowed")

        if (!immediateAllowed && !flexibleAllowed) {
            return null
        }

        return when {
            // Critical priority - always immediate
            priority >= PRIORITY_CRITICAL && immediateAllowed -> AppUpdateType.IMMEDIATE
            // High priority - immediate after 3 days
            priority >= PRIORITY_HIGH && stalenessDays >= DAYS_FOR_IMMEDIATE_UPDATE && immediateAllowed -> AppUpdateType.IMMEDIATE
            // Default to flexible if allowed
            flexibleAllowed -> AppUpdateType.FLEXIBLE
            // Fallback to immediate only when allowed
            immediateAllowed -> AppUpdateType.IMMEDIATE
            else -> null
        }
    }

    /**
     * Start the Google Play in-app update flow
     */
    fun startUpdate(
        activity: Activity,
        appUpdateInfo: AppUpdateInfo,
        updateType: Int,
        activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        try {
            if (!appUpdateInfo.isUpdateTypeAllowed(updateType)) {
                Timber.w("🔄 IN-APP UPDATE: Requested type $updateType is not allowed by Play Core")
                return
            }

            val updateOptions = AppUpdateOptions.newBuilder(updateType)
                .setAllowAssetPackDeletion(true)
                .build()

            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activityResultLauncher,
                updateOptions
            )

            Timber.i("🔄 Started ${if (updateType == AppUpdateType.IMMEDIATE) "IMMEDIATE" else "FLEXIBLE"} update flow")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to start update flow")
        }
    }

    /**
     * Register listener for flexible update progress
     */
    fun registerFlexibleUpdateListener(
        onDownloading: (bytesDownloaded: Long, totalBytes: Long) -> Unit = { _, _ -> },
        onDownloaded: () -> Unit = {},
        onInstalling: () -> Unit = {},
        onInstalled: () -> Unit = {},
        onFailed: (errorCode: Int) -> Unit = {},
        onCanceled: () -> Unit = {}
    ) {
        installStateListener = InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADING -> {
                    val bytesDownloaded = state.bytesDownloaded()
                    val totalBytes = state.totalBytesToDownload()
                    val progress = if (totalBytes > 0) (bytesDownloaded * 100 / totalBytes).toInt() else 0
                    Timber.d("📥 Downloading update: $progress% ($bytesDownloaded / $totalBytes bytes)")
                    onDownloading(bytesDownloaded, totalBytes)
                }
                InstallStatus.DOWNLOADED -> {
                    Timber.i("✅ Update downloaded - ready to install")
                    onDownloaded()
                }
                InstallStatus.INSTALLING -> {
                    Timber.d("⚙️ Installing update...")
                    onInstalling()
                }
                InstallStatus.INSTALLED -> {
                    Timber.i("✅ Update installed successfully")
                    onInstalled()
                    unregisterFlexibleUpdateListener()
                }
                InstallStatus.FAILED -> {
                    Timber.e("❌ Update failed with error code: ${state.installErrorCode()}")
                    onFailed(state.installErrorCode())
                    unregisterFlexibleUpdateListener()
                }
                InstallStatus.CANCELED -> {
                    Timber.w("⚠️ Update canceled by user")
                    onCanceled()
                    unregisterFlexibleUpdateListener()
                }
                InstallStatus.PENDING -> {
                    Timber.d("⏳ Update pending...")
                }
                else -> {
                    Timber.d("🔄 Update status: ${state.installStatus()}")
                }
            }
        }

        appUpdateManager.registerListener(installStateListener!!)
        Timber.d("📝 Registered flexible update listener")
    }

    /**
     * Unregister flexible update listener
     */
    fun unregisterFlexibleUpdateListener() {
        installStateListener?.let {
            appUpdateManager.unregisterListener(it)
            installStateListener = null
            Timber.d("📝 Unregistered flexible update listener")
        }
    }

    /**
     * Complete flexible update (restart app)
     */
    fun completeFlexibleUpdate() {
        appUpdateManager.completeUpdate()
        Timber.i("🔄 Completing flexible update - app will restart")
    }

    /**
     * Check if downloaded update is waiting to be installed
     */
    suspend fun checkForPendingUpdate(onPendingUpdate: () -> Unit = {}) {
        try {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    Timber.i("⏳ Downloaded update is waiting to be installed - auto-completing")
                    onPendingUpdate()
                    completeFlexibleUpdate()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error checking for pending update")
        }
    }

    private suspend fun shouldCheckForUpdate(): Boolean {
        if (MIN_HOURS_BETWEEN_CHECKS <= 0) return true
        val prefs = context.updateDataStore.data.first()
        val lastCheck = prefs[KEY_LAST_UPDATE_CHECK] ?: 0
        val hoursSinceLastCheck = (System.currentTimeMillis() - lastCheck) / (1000 * 60 * 60)
        return hoursSinceLastCheck >= MIN_HOURS_BETWEEN_CHECKS
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
