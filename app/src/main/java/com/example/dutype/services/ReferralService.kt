package com.example.dutype.services

import com.example.dutype.firestore.FirestoreSchema.ReferralCodes
import com.example.dutype.firestore.FirestoreSchema.Referrals
import com.example.dutype.firestore.FirestoreSchema.WalletLedger
import com.example.dutype.firestore.FirestoreSchema.Wallets
import com.example.dutype.firestore.FirestoreSchema.Withdrawals
import com.example.dutype.models.LeaderboardEntry
import com.example.dutype.models.LedgerEntry
import com.example.dutype.models.Referral
import com.example.dutype.models.ReferralValidationInfo
import com.example.dutype.models.Wallet
import com.example.dutype.models.WithdrawalRequest
import com.example.dutype.models.normalizeReferralCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The signed-in user's wallet, money history, referrals and withdrawals. Everything that moves
 * money runs in Cloud Functions; the app only reads its own documents.
 */
@Singleton
class ReferralService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) {
    private fun uid(): String? = auth.currentUser?.takeUnless { it.isAnonymous }?.uid

    /** Live wallet. Asks the server to create it (and the user's own code) when missing. */
    fun wallet(): Flow<Wallet?> = listenDoc { uid ->
        firestore.collection(Wallets.COLLECTION).document(uid)
    }.let { flow ->
        callbackFlow {
            var requested = false
            val job = launch {
                flow.collect { wallet ->
                    if (wallet == null && !requested) {
                        requested = true
                        runCatching { functions.getHttpsCallable("ensureWalletCallable").call().await() }
                    }
                    trySend(wallet)
                }
            }
            awaitClose { job.cancel() }
        }
    }

    /** Money history, newest first. */
    fun ledger(limit: Long = 100): Flow<List<LedgerEntry>> = listenQuery { uid ->
        firestore.collection(WalletLedger.COLLECTION)
            .whereEqualTo(WalletLedger.UID, uid)
            .orderBy(WalletLedger.CREATED_AT, Query.Direction.DESCENDING)
            .limit(limit)
    }.let { flow -> flow.toTyped { id, d -> LedgerEntry.from(id, d) } }

    /** People who joined with my code, with the reward each one paid me (from the ledger). */
    fun myReferrals(): Flow<List<Referral>> {
        val referrals = listenQuery { uid ->
            firestore.collection(Referrals.COLLECTION)
                .whereEqualTo(Referrals.REFERRER_UID, uid)
                .orderBy(Referrals.CREATED_AT, Query.Direction.DESCENDING)
                .limit(100)
        }.toTyped { id, d -> Referral.from(id, d) }
        return combine(referrals, ledger()) { list, entries ->
            val rewards = entries.filter { it.id.startsWith("referral_") && it.id.endsWith("_referrer") }
                .associate { it.refId to it.amountPaise }
            list.map { it.copy(rewardPaise = rewards[it.refereeUid] ?: 0L) }
        }
    }

    /** The referral I joined with, if any. */
    suspend fun myReferrer(): Referral? {
        val uid = uid() ?: return null
        val doc = runCatching { firestore.collection(Referrals.COLLECTION).document(uid).get().await() }.getOrNull()
        val fromReferral = doc?.data?.let { Referral.from(doc.id, it) }
        if (fromReferral != null) return fromReferral

        // Fallback: check users/{uid} in case referral code was entered during profile setup / onboarding
        val userDoc = runCatching { firestore.collection("users").document(uid).get().await() }.getOrNull()
        if (userDoc != null && (userDoc.getBoolean("hasUsedReferralCode") == true || !userDoc.getString("referredByCode").isNullOrBlank())) {
            val code = userDoc.getString("referredByCode").orEmpty()
            val referrerUid = userDoc.getString("referredBy").orEmpty()
            return Referral(
                refereeUid = uid,
                referrerUid = referrerUid,
                code = code,
                status = com.example.dutype.models.ReferralStatus.COMPLETED
            )
        }
        return null
    }

    fun withdrawals(): Flow<List<WithdrawalRequest>> = listenQuery { uid ->
        firestore.collection(Withdrawals.COLLECTION)
            .whereEqualTo(Withdrawals.UID, uid)
            .orderBy(Withdrawals.CREATED_AT, Query.Direction.DESCENDING)
            .limit(50)
    }.toTyped { id, d -> WithdrawalRequest.from(id, d) }

    /** Cashes out the whole balance to [upiId]. Returns the amount sent (paise). Supports persistent idempotencyKey across retries. */
    suspend fun requestWithdrawal(upiId: String, idempotencyKey: String? = null): Result<Long> = runCatching {
        @Suppress("UNCHECKED_CAST")
        val reqId = idempotencyKey?.trim()?.takeIf { it.isNotEmpty() } ?: UUID.randomUUID().toString()
        val data = functions.getHttpsCallable("requestWithdrawal")
            .call(mapOf("upiId" to upiId.trim(), "requestId" to reqId))
            .await().data as? Map<String, Any?> ?: emptyMap()
        (data["amountPaise"] as? Number)?.toLong() ?: 0L
    }

    suspend fun leaderboard(): Result<List<LeaderboardEntry>> = runCatching {
        @Suppress("UNCHECKED_CAST")
        val data = functions.getHttpsCallable("getReferralLeaderboard").call().await().data as? Map<String, Any?>
        (data?.get("leaders") as? List<*>).orEmpty().mapNotNull { row ->
            (row as? Map<*, *>)?.let {
                LeaderboardEntry(
                    rank = (it["rank"] as? Number)?.toInt() ?: 0,
                    name = it["name"] as? String ?: "",
                    successfulReferrals = (it["successfulReferrals"] as? Number)?.toInt() ?: 0
                )
            }
        }
    }

    /** Checks a code typed at sign-up (one read of referral_codes/{CODE}). */
    suspend fun validateReferralCode(rawCode: String): ReferralValidationInfo {
        val code = normalizeReferralCode(rawCode)
        if (code.length < 3) return ReferralValidationInfo(false, errorMessage = "Enter a valid referral code (min 3 characters)")
        val currentUid = uid()
        // Check if user already used a code
        if (!currentUid.isNullOrBlank()) {
            val existing = runCatching {
                firestore.collection(Referrals.COLLECTION).document(currentUid).get().await()
            }.getOrNull()
            if (existing != null && existing.exists()) {
                return ReferralValidationInfo(false, errorMessage = "A referral code was already used for this account")
            }
        }
        return runCatching {
            var doc = firestore.collection(ReferralCodes.COLLECTION).document(code).get().await()
            if (!doc.exists()) {
                val lowerDoc = firestore.collection(ReferralCodes.COLLECTION).document(code.lowercase()).get().await()
                if (lowerDoc.exists()) {
                    doc = lowerDoc
                }
            }
            when {
                !doc.exists() ->
                    ReferralValidationInfo(false, errorMessage = "Referral code not found")
                doc.getBoolean(ReferralCodes.ACTIVE) != true ->
                    ReferralValidationInfo(false, errorMessage = "This referral code is disabled")
                doc.getString(ReferralCodes.UID) == currentUid ->
                    ReferralValidationInfo(false, errorMessage = "You cannot use your own referral code")
                else -> {
                    val referrerName = doc.getString("name")?.trim()
                    val referrerRole = doc.getString(ReferralCodes.ROLE)
                    ReferralValidationInfo(
                        isValid = true,
                        referrerRole = referrerRole,
                        referrerNameText = referrerName
                    )
                }
            }
        }.getOrElse { ReferralValidationInfo(false, errorMessage = "Could not check the code. Please try again.") }
    }

    /** Uses a code after sign-up (profile setup; first 7 days). Throws with the server's user-friendly reason. */
    suspend fun applyReferralCode(
        rawCode: String,
        name: String? = null,
        phone: String? = null
    ): Result<Unit> = runCatching {
        val payload = mutableMapOf<String, Any>(
            "code" to normalizeReferralCode(rawCode)
        )
        if (!name.isNullOrBlank()) payload["name"] = name.trim()
        if (!phone.isNullOrBlank()) payload["phone"] = phone.trim()

        try {
            functions.getHttpsCallable("applyReferralCode").call(payload).await()
            Unit
        } catch (e: Exception) {
            val rawMsg = e.message ?: "Failed to apply referral code"
            val cleanMsg = rawMsg.substringAfter(": ").trim()
            throw Exception(if (cleanMsg.isNotBlank()) cleanMsg else rawMsg, e)
        }
    }

    /** Refreshes or creates the user's personal referral code derived from their verified real name. */
    suspend fun refreshReferralCode(name: String): Result<String> = runCatching {
        @Suppress("UNCHECKED_CAST")
        val data = functions.getHttpsCallable("refreshReferralCode")
            .call(mapOf("name" to name.trim()))
            .await().data as? Map<String, Any?> ?: emptyMap()
        data["referralCode"] as? String ?: ""
    }

    /** Whether I already joined with someone's code (one read of referrals/{uid}). */
    suspend fun hasUsedReferralCode(): Boolean = myReferrer() != null

    fun shareMessage(referralCode: String, userName: String): String {
        val link = com.example.dutype.utils.DeepLinkHandler.generateReferralWebLink(referralCode)
        return """
            🎉 Join DutyPe — find jobs near you and get hired fast!

            $userName invited you. Use my referral code: $referralCode

            👉 $link
        """.trimIndent()
    }

    // ─────────────────────────────── internals ───────────────────────────────

    private fun listenDoc(ref: (String) -> com.google.firebase.firestore.DocumentReference): Flow<Wallet?> {
        val uid = uid() ?: return flowOf(null)
        return callbackFlow {
            val registration = ref(uid).addSnapshotListener { snap, error ->
                if (error == null) trySend(snap?.data?.let { Wallet.from(it) })
            }
            awaitClose { registration.remove() }
        }
    }

    private fun listenQuery(query: (String) -> Query): Flow<List<Pair<String, Map<String, Any?>>>> {
        val uid = uid() ?: return flowOf(emptyList())
        return callbackFlow {
            val registration = query(uid).addSnapshotListener { snap, error ->
                if (error == null && snap != null) trySend(snap.documents.mapNotNull { d -> d.data?.let { d.id to it } })
            }
            awaitClose { registration.remove() }
        }
    }

    private fun <T> Flow<List<Pair<String, Map<String, Any?>>>>.toTyped(map: (String, Map<String, Any?>) -> T): Flow<List<T>> =
        this.map { rows -> rows.map { (id, d) -> map(id, d) } }
}
