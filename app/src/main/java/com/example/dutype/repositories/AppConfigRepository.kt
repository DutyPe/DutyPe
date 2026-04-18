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
    val minWithdrawal: Double = 50.0,
    val maxWithdrawalPerDay: Double = 1000.0,
    val milestones: Map<Int, Double> = mapOf(
        5 to 50.0, 10 to 100.0, 15 to 150.0,
        25 to 250.0, 50 to 500.0, 100 to 1000.0
    ),
    val withdrawalMilestones: List<Int> = listOf(5, 10, 15)
)

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
                    minWithdrawal = (data["minWithdrawal"] as? Number)?.toDouble() ?: 50.0,
                    maxWithdrawalPerDay = (data["maxWithdrawalPerDay"] as? Number)?.toDouble() ?: 1000.0,
                    milestones = milestoneMap.ifEmpty { ReferralConfig().milestones },
                    withdrawalMilestones = withdrawalList.ifEmpty { ReferralConfig().withdrawalMilestones }
                )
            )
        }
        awaitClose { registration.remove() }
    }.stateIn(scope, SharingStarted.WhileSubscribed(60_000), ReferralConfig())

    fun referralConfigFlow(): Flow<ReferralConfig> = referralConfig
}
