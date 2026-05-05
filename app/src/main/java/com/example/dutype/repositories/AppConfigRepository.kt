package com.example.dutype.repositories

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

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
    val minSupportedVersionCode: Long = 0L,
    val latestVersionName: String = "",
    val forceUpdate: Boolean = false,
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
            minSupportedVersionCode,
            latestVersionName,
            forceUpdate,
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
        val hasVersionGate = latestVersionCode > 0L || minSupportedVersionCode > 0L
        return !hasVersionGate || maxOf(latestVersionCode, minSupportedVersionCode) > currentVersionCode
    }

    fun isRequiredFor(currentVersionCode: Long): Boolean {
        return forceUpdate || (minSupportedVersionCode > 0L && currentVersionCode < minSupportedVersionCode)
    }
}

/**
 * Realtime source of truth for app configuration that admins edit via the
 * web admin UI. One snapshot listener is shared across every consumer.
 */
@Singleton
class AppConfigRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val referralConfig: StateFlow<ReferralConfig> = callbackFlow {
        val ref = firestore.collection(com.example.dutype.firestore.FirestoreCollections.APP_CONFIG).document("referral")
        trySend(ReferralConfig()) // seed defaults immediately
        val registration = ref.addSnapshotListener { snap, _ ->
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
        val registration = ref.addSnapshotListener { snap, _ ->
            if (snap == null || !snap.exists()) {
                trySend(AppUpdateConfig())
                return@addSnapshotListener
            }
            val data = snap.data.orEmpty()
            trySend(
                AppUpdateConfig(
                    enabled = (data["enabled"] as? Boolean) ?: false,
                    latestVersionCode = (data["latestVersionCode"] as? Number)?.toLong() ?: 0L,
                    minSupportedVersionCode = (data["minSupportedVersionCode"] as? Number)?.toLong() ?: 0L,
                    latestVersionName = (data["latestVersionName"] as? String)?.trim().orEmpty(),
                    forceUpdate = (data["forceUpdate"] as? Boolean) ?: false,
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

    fun referralConfigFlow(): Flow<ReferralConfig> = referralConfig

    private fun parseTargetRoles(raw: Any?): List<String> {
        val roles = when (raw) {
            is List<*> -> raw.mapNotNull { it?.toString() }
            is String -> listOf(raw)
            else -> emptyList()
        }.map { it.trim().uppercase() }
            .filter { it == "ALL" || it == "WORKER" || it == "EMPLOYER" }

        return roles.ifEmpty { listOf("WORKER", "EMPLOYER") }
    }
}
