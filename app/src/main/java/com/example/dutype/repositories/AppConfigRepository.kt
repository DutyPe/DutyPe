package com.example.dutype.repositories

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Admin-editable referral configuration.
 * Mirrors the /app_config/referral Firestore document. If the document is
 * missing or unreadable we fall back to the same defaults used by the CF
 * layer so the UI never shows ₹0.
 */
data class ReferralConfig(
    val rewardPerReferral: Double = 25.0,
    val signupBonus: Double = 25.0,
    val employerSignupBonus: Double = 10.0,
    val employerSignupBonusEnabled: Boolean = true,
    val employerUnlimitedJobPostingEnabled: Boolean = true,
    val welcomeBonusCampaignId: String = "welcome_bonus_v1",
    val minWithdrawal: Double = 100.0,
    val maxWithdrawalPerDay: Double = 1000.0,
    val milestones: Map<Int, Double> = mapOf(
        5 to 50.0, 10 to 100.0, 15 to 150.0,
        25 to 250.0, 50 to 500.0, 100 to 1000.0
    ),
    val withdrawalMilestones: List<Int> = listOf(5, 10, 15)
)

data class AppUpdateConfig(
    val enabled: Boolean = false,
    val latestVersionCode: Long = 0L,
    val title: String = "",
    val titleTe: String = "",
    val message: String = "",
    val messageTe: String = "",
    val buttonText: String = "",
    val buttonTextTe: String = "",
    val playStoreUrl: String = "",
    val targetRoles: List<String> = listOf("WORKER", "EMPLOYER")
) {
    val dismissalKey: String
        get() = listOf(
            latestVersionCode,
            title,
            titleTe,
            message,
            messageTe,
            buttonText,
            buttonTextTe
        ).joinToString("|")

    fun appliesToRole(role: String): Boolean {
        val normalizedRole = role.trim().uppercase()
        return targetRoles.isEmpty() ||
            targetRoles.any { target ->
                val normalizedTarget = target.trim().uppercase()
                normalizedTarget == "ALL" || normalizedTarget == normalizedRole
            }
    }

    fun shouldPromptFor(currentVersionCode: Long, role: String): Boolean {
        if (!enabled || !appliesToRole(role)) return false
        return latestVersionCode <= 0L || currentVersionCode < latestVersionCode
    }

    fun isRequiredFor(currentVersionCode: Long): Boolean {
        return false
    }
}

data class DynamicFeaturesConfig(
    val isDirectCallEnabled: Boolean = true,
    val isUrgentJobsEnabled: Boolean = true,
    val activeLauncherIcon: String = "default",
    val promoBannerUrl: String = "",
    val employerPromoBannerUrl: String = "",
    val lottieLoadingUrl: String = "",
    val primaryColor: String = "#1E3A8A",
    val employerPrimaryColor: String = "#0F0F0F",
    val headerLottieUrl: String = "",
    val launchPromoEnabled: Boolean = false,
    val launchPromoMediaType: String = "IMAGE",
    val launchPromoBannerUrl: String = "",
    val launchPromoAnimationUrl: String = "",
    val supportEmail: String = "support@dutype.com",
    val headerTextColor: String = "#FFFFFF",
    val isRemoteLoaded: Boolean = false
)

/**
 * Realtime source of truth for app configuration that admins edit via the
 * web admin UI. One snapshot listener is shared across every consumer.
 */
@Singleton
class AppConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dynamicFeaturesPrefs by lazy {
        context.getSharedPreferences(DYNAMIC_FEATURES_PREFS, Context.MODE_PRIVATE)
    }
    private val initialDynamicFeaturesConfig: DynamicFeaturesConfig by lazy {
        readCachedDynamicFeatures() ?: DynamicFeaturesConfig()
    }

    val referralConfig: StateFlow<ReferralConfig> = callbackFlow {
        val ref = firestore.collection(com.example.dutype.firestore.FirestoreCollections.APP_CONFIG).document("referral")
        trySend(ReferralConfig()) // seed defaults immediately
        val registration = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                Timber.w(error, "AppConfigRepository: referral listener error; using defaults")
                trySend(ReferralConfig())
                return@addSnapshotListener
            }
            if (snap == null || !snap.exists()) {
                trySend(ReferralConfig())
                return@addSnapshotListener
            }
            val data = snap.data.orEmpty()
            val milestoneMap = (data["milestones"] as? Map<*, *>).orEmpty().mapNotNull { (k, v) ->
                val key = (k?.toString()?.toIntOrNull()) ?: return@mapNotNull null
                val value = (v as? Number)?.toDouble() ?: return@mapNotNull null
                key to value
            }.toMap()
            val withdrawalList = (data["withdrawalMilestones"] as? List<*>).orEmpty()
                .mapNotNull { (it as? Number)?.toInt() }

            trySend(
                ReferralConfig(
                    rewardPerReferral = (data["rewardPerReferral"] as? Number)?.toDouble() ?: 25.0,
                    signupBonus = (data["signupBonus"] as? Number)?.toDouble() ?: 25.0,
                    employerSignupBonus = (data["employerSignupBonus"] as? Number)?.toDouble() ?: 10.0,
                    employerSignupBonusEnabled = (data["employerSignupBonusEnabled"] as? Boolean) ?: true,
                    employerUnlimitedJobPostingEnabled = (data["employerUnlimitedJobPostingEnabled"] as? Boolean) ?: true,
                    welcomeBonusCampaignId = (data["welcomeBonusCampaignId"] as? String)?.trim()?.takeIf { it.isNotBlank() }
                        ?: "welcome_bonus_v1",
                    minWithdrawal = ((data["minWithdrawal"] as? Number)?.toDouble() ?: 100.0).coerceAtLeast(100.0),
                    maxWithdrawalPerDay = (data["maxWithdrawalPerDay"] as? Number)?.toDouble() ?: 1000.0,
                    milestones = milestoneMap.ifEmpty { ReferralConfig().milestones },
                    withdrawalMilestones = withdrawalList.ifEmpty { ReferralConfig().withdrawalMilestones }
                )
            )
        }
        awaitClose { registration.remove() }
    }.stateIn(scope, SharingStarted.WhileSubscribed(60_000), ReferralConfig())

    val appUpdateConfig: StateFlow<AppUpdateConfig> = callbackFlow {
        val ref = firestore.collection(com.example.dutype.firestore.FirestoreCollections.APP_CONFIG).document("app_update")
        trySend(AppUpdateConfig())
        val registration = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                Timber.w(error, "AppConfigRepository: app_update listener error; using defaults")
                trySend(AppUpdateConfig())
                return@addSnapshotListener
            }
            if (snap == null || !snap.exists()) {
                trySend(AppUpdateConfig())
                return@addSnapshotListener
            }
            val data = snap.data.orEmpty()
            trySend(
                AppUpdateConfig(
                    enabled = (data["enabled"] as? Boolean) ?: false,
                    latestVersionCode = (data["latestVersionCode"] as? Number)?.toLong() ?: 0L,
                    title = (data["title"] as? String)?.trim().orEmpty(),
                    titleTe = (data["titleTe"] as? String)?.trim().orEmpty(),
                    message = (data["message"] as? String)?.trim().orEmpty(),
                    messageTe = (data["messageTe"] as? String)?.trim().orEmpty(),
                    buttonText = (data["buttonText"] as? String)?.trim().orEmpty(),
                    buttonTextTe = (data["buttonTextTe"] as? String)?.trim().orEmpty(),
                    playStoreUrl = (data["playStoreUrl"] as? String)?.trim().orEmpty(),
                    targetRoles = parseTargetRoles(data["targetRoles"])
                )
            )
        }
        awaitClose { registration.remove() }
    }.stateIn(scope, SharingStarted.WhileSubscribed(60_000), AppUpdateConfig())

    val dynamicFeaturesConfig: StateFlow<DynamicFeaturesConfig> = callbackFlow {
        val ref = firestore.collection(com.example.dutype.firestore.FirestoreCollections.APP_CONFIG).document("dynamic_features")
        trySend(initialDynamicFeaturesConfig)
        val registration = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                Timber.w(error, "AppConfigRepository: dynamic_features listener error; using cached/defaults")
                return@addSnapshotListener
            }
            if (snap == null || !snap.exists()) {
                return@addSnapshotListener
            }
            val data = snap.data.orEmpty()
            val config = DynamicFeaturesConfig(
                isDirectCallEnabled = (data["isDirectCallEnabled"] as? Boolean) ?: true,
                isUrgentJobsEnabled = (data["isUrgentJobsEnabled"] as? Boolean) ?: true,
                activeLauncherIcon = (data["activeLauncherIcon"] as? String) ?: "default",
                promoBannerUrl = (data["promoBannerUrl"] as? String).orEmpty(),
                employerPromoBannerUrl = (data["employerPromoBannerUrl"] as? String).orEmpty(),
                lottieLoadingUrl = (data["lottieLoadingUrl"] as? String).orEmpty(),
                primaryColor = (data["primaryColor"] as? String) ?: "#1E3A8A",
                employerPrimaryColor = (data["employerPrimaryColor"] as? String) ?: "#0F0F0F",
                headerLottieUrl = (data["headerLottieUrl"] as? String).orEmpty(),
                launchPromoEnabled = (data["launchPromoEnabled"] as? Boolean) ?: false,
                launchPromoMediaType = (data["launchPromoMediaType"] as? String)?.trim()?.uppercase()
                    ?.takeIf { it == "IMAGE" || it == "ANIMATION" } ?: "IMAGE",
                launchPromoBannerUrl = (data["launchPromoBannerUrl"] as? String) ?: "",
                launchPromoAnimationUrl = (data["launchPromoAnimationUrl"] as? String) ?: "",
                supportEmail = (data["supportEmail"] as? String) ?: "support@dutype.com",
                headerTextColor = (data["headerTextColor"] as? String) ?: "#FFFFFF",
                isRemoteLoaded = true
            )
            saveCachedDynamicFeatures(config)
            trySend(config)
        }
        awaitClose { registration.remove() }
    }.stateIn(scope, SharingStarted.WhileSubscribed(60_000), initialDynamicFeaturesConfig)

    fun referralConfigFlow(): Flow<ReferralConfig> = referralConfig

    private fun readCachedDynamicFeatures(): DynamicFeaturesConfig? {
        val raw = dynamicFeaturesPrefs.getString(DYNAMIC_FEATURES_CACHE_KEY, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            DynamicFeaturesConfig(
                isDirectCallEnabled = json.optBoolean("isDirectCallEnabled", true),
                isUrgentJobsEnabled = json.optBoolean("isUrgentJobsEnabled", true),
                activeLauncherIcon = json.optString("activeLauncherIcon", "default"),
                promoBannerUrl = json.optString("promoBannerUrl", ""),
                employerPromoBannerUrl = json.optString("employerPromoBannerUrl", ""),
                lottieLoadingUrl = json.optString("lottieLoadingUrl", ""),
                primaryColor = json.optString("primaryColor", "#1E3A8A"),
                employerPrimaryColor = json.optString("employerPrimaryColor", "#0F0F0F"),
                headerLottieUrl = json.optString("headerLottieUrl", ""),
                launchPromoEnabled = json.optBoolean("launchPromoEnabled", false),
                launchPromoMediaType = json.optString("launchPromoMediaType", "IMAGE").trim().uppercase()
                    .takeIf { it == "IMAGE" || it == "ANIMATION" } ?: "IMAGE",
                launchPromoBannerUrl = json.optString("launchPromoBannerUrl", ""),
                launchPromoAnimationUrl = json.optString("launchPromoAnimationUrl", ""),
                supportEmail = json.optString("supportEmail", "support@dutype.com"),
                headerTextColor = json.optString("headerTextColor", "#FFFFFF"),
                isRemoteLoaded = json.optBoolean("isRemoteLoaded", true)
            )
        }.getOrNull()
    }

    private fun saveCachedDynamicFeatures(config: DynamicFeaturesConfig) {
        dynamicFeaturesPrefs.edit()
            .putString(DYNAMIC_FEATURES_CACHE_KEY, config.toJson().toString())
            .apply()
    }

    private fun DynamicFeaturesConfig.toJson(): JSONObject = JSONObject().apply {
        put("isDirectCallEnabled", isDirectCallEnabled)
        put("isUrgentJobsEnabled", isUrgentJobsEnabled)
        put("activeLauncherIcon", activeLauncherIcon)
        put("promoBannerUrl", promoBannerUrl)
        put("employerPromoBannerUrl", employerPromoBannerUrl)
        put("lottieLoadingUrl", lottieLoadingUrl)
        put("primaryColor", primaryColor)
        put("employerPrimaryColor", employerPrimaryColor)
        put("headerLottieUrl", headerLottieUrl)
        put("launchPromoEnabled", launchPromoEnabled)
        put("launchPromoMediaType", launchPromoMediaType)
        put("launchPromoBannerUrl", launchPromoBannerUrl)
        put("launchPromoAnimationUrl", launchPromoAnimationUrl)
        put("supportEmail", supportEmail)
        put("headerTextColor", headerTextColor)
        put("isRemoteLoaded", isRemoteLoaded)
    }

    private fun parseTargetRoles(raw: Any?): List<String> {
        val roles = when (raw) {
            is List<*> -> raw.mapNotNull { it?.toString() }
            is String -> listOf(raw)
            else -> emptyList()
        }.map { it.trim().uppercase() }
            .filter { it == "ALL" || it == "WORKER" || it == "EMPLOYER" }

        return roles.ifEmpty { listOf("WORKER", "EMPLOYER") }
    }

    private companion object {
        const val DYNAMIC_FEATURES_PREFS = "app_config_cache"
        const val DYNAMIC_FEATURES_CACHE_KEY = "dynamic_features"
    }
}
