package com.example.dutype.services

import com.example.dutype.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ============================================
 * ENTERPRISE-GRADE REFERRAL SERVICE
 * ============================================
 * 
 * Architecture inspired by:
 * - Dropbox (two-sided rewards, viral growth)
 * - PayPal ($20 referral program that grew to 100M users)
 * - Uber (location-based fraud detection)
 * - Stripe (atomic transactions, idempotency)
 * 
 * Key Features:
 * 1. Server-Side Processing - All reward calculations via Cloud Functions
 * 2. Real-Time Updates - Firestore listeners for instant UI updates
 * 3. Fraud Prevention - Device fingerprinting, rate limiting
 * 4. Scalable Design - O(1) lookups, denormalized data
 * 5. Idempotency - Prevents duplicate rewards
 * 
 * @author DutyPe Engineering Team
 * @version 2.0.0 - Enterprise Edition
 */
@Singleton
class ReferralService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
    private val deviceFingerprintService: DeviceFingerprintService,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    companion object {
        private const val COLLECTION_REFERRAL_CODES = "referral_codes"
        private const val COLLECTION_REFERRAL_STATS = "referral_stats"
        private const val COLLECTION_REFERRALS = "referrals"
        private const val COLLECTION_WITHDRAWALS = "withdrawal_requests"
    }

    // ============================================
    // REAL-TIME STATS LISTENER
    // ============================================
    
    /**
     * Get real-time updates for user's referral stats
     * Uses Firestore snapshot listener for instant UI updates
     */
    fun getReferralStatsFlow(userId: String): Flow<ReferralStats?> = callbackFlow {
        val listenerRegistration = firestore.collection(COLLECTION_REFERRAL_STATS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "🎁 REFERRAL: Error listening to stats")
                    trySend(null)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val stats = snapshot.data?.let { ReferralStats.fromMap(it) }
                    Timber.d("🎁 REFERRAL: Stats updated - ${stats?.successfulReferrals} successful")
                    trySend(stats)
                } else {
                    trySend(null)
                }
            }
        
        awaitClose { listenerRegistration.remove() }
    }

    /**
     * Get real-time updates for current user's referral stats
     */
    fun getCurrentUserReferralStatsFlow(): Flow<ReferralStats?> {
        val userId = auth.currentUser?.uid ?: return callbackFlow { 
            trySend(null)
            awaitClose { }
        }
        return getReferralStatsFlow(userId)
    }


    // ============================================
    // REFERRAL CODE VALIDATION (O(1) Lookup)
    // ============================================

    /**
     * Validate referral code - O(1) lookup using code as document ID
     * Returns validation info including referrer details
     */
    suspend fun validateReferralCode(code: String): ReferralValidationInfo {
        val trimmedCode = code.trim().uppercase()
        
        // Local format validation first
        if (!isValidReferralCode(trimmedCode)) {
            return ReferralValidationInfo(
                isValid = false,
                errorMessage = "Invalid referral code format"
            )
        }

        return try {
            // O(1) lookup - code is document ID
            val codeDoc = firestore.collection(COLLECTION_REFERRAL_CODES)
                .document(trimmedCode)
                .get()
                .await()

            if (!codeDoc.exists()) {
                Timber.d("🎁 REFERRAL: Code $trimmedCode not found")
                return ReferralValidationInfo(
                    isValid = false,
                    errorMessage = "Referral code not found"
                )
            }

            val codeData = ReferralCodeLookup.fromMap(codeDoc.data ?: emptyMap())

            // Check if code is active
            if (!codeData.isActive) {
                return ReferralValidationInfo(
                    isValid = false,
                    errorMessage = "This referral code is no longer active",
                    isBlocked = true
                )
            }

            // Check self-referral
            val currentUserId = auth.currentUser?.uid
            if (codeData.userId == currentUserId) {
                return ReferralValidationInfo(
                    isValid = false,
                    errorMessage = "You cannot use your own referral code"
                )
            }

            Timber.d("🎁 REFERRAL: Code $trimmedCode valid - belongs to ${codeData.userName}")
            ReferralValidationInfo(
                isValid = true,
                referrerUserId = codeData.userId,
                referrerRole = codeData.userRole,
                referrerName = codeData.userName
            )

        } catch (e: Exception) {
            Timber.e(e, "🎁 REFERRAL: Error validating code $trimmedCode")
            ReferralValidationInfo(
                isValid = false,
                errorMessage = "Failed to validate code. Please try again."
            )
        }
    }

    // ============================================
    // APPLY REFERRAL CODE (via Cloud Function)
    // ============================================

    /**
     * Apply referral code during signup - calls Cloud Function
     * Creates PENDING referral record with fraud detection
     */
    suspend fun applyReferralCode(
        referralCode: String,
        userRole: String,
        userName: String,
        userPhone: String
    ): Result<ApplyReferralResult> {
        val trimmedCode = referralCode.trim().uppercase()
        
        return try {
            // Get device fingerprint for fraud detection
            val deviceFingerprint = deviceFingerprintService.getDeviceFingerprint(context)

            val data = hashMapOf(
                "referralCode" to trimmedCode,
                "userRole" to userRole,
                "userName" to userName,
                "userPhone" to userPhone,
                "deviceFingerprint" to deviceFingerprint
            )

            Timber.d("🎁 REFERRAL: Applying code $trimmedCode via Cloud Function")

            val result = functions
                .getHttpsCallable("applyReferralCode")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val response = result.data as? Map<String, Any?> ?: emptyMap()
            
            val success = response["success"] as? Boolean ?: false
            
            if (success) {
                Timber.d("🎁 REFERRAL: ✅ Code applied successfully")
                Result.success(ApplyReferralResult(
                    success = true,
                    referralId = response["referralId"] as? String,
                    referrerName = response["referrerName"] as? String,
                    referrerRole = response["referrerRole"] as? String,
                    message = response["message"] as? String
                ))
            } else {
                val error = response["error"] as? String ?: "Failed to apply referral code"
                Timber.w("🎁 REFERRAL: ❌ Code application failed: $error")
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Timber.e(e, "🎁 REFERRAL: Error applying code")
            Result.failure(e)
        }
    }

    // ============================================
    // GET REFERRAL STATS
    // ============================================

    /**
     * Get referral stats for current user (one-time fetch)
     */
    suspend fun getReferralStats(): ReferralStats? {
        val userId = auth.currentUser?.uid ?: return null
        
        return try {
            val statsDoc = firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(userId)
                .get()
                .await()

            if (statsDoc.exists()) {
                ReferralStats.fromMap(statsDoc.data ?: emptyMap())
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "🎁 REFERRAL: Error getting stats")
            null
        }
    }

    /**
     * Get current user's referral stats as Result (for ViewModel compatibility)
     */
    suspend fun getCurrentUserReferralStats(): Result<ReferralStats> {
        return try {
            val stats = getReferralStats()
            if (stats != null) {
                Result.success(stats)
            } else {
                Result.failure(Exception("No referral stats found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "🎁 REFERRAL: Error getting current user stats")
            Result.failure(e)
        }
    }

    /**
     * Get current user's referral history as Result (for ViewModel compatibility)
     */
    suspend fun getCurrentUserReferralHistory(): Result<List<Referral>> {
        return try {
            val history = getReferralHistory()
            Result.success(history)
        } catch (e: Exception) {
            Timber.e(e, "🎁 REFERRAL: Error getting current user history")
            Result.failure(e)
        }
    }

    /**
     * Get user's referral code
     */
    suspend fun getUserReferralCode(): String? {
        return getReferralStats()?.referralCode
    }

    /**
     * Check free job postings for employer
     */
    suspend fun checkFreeJobPostings(userId: String): Result<Pair<Int, Long?>> {
        return try {
            val statsDoc = firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(userId)
                .get()
                .await()

            if (statsDoc.exists()) {
                val freePostings = statsDoc.getLong("freeJobPostings")?.toInt() ?: 0
                val expiry = statsDoc.getLong("freeJobPostingsExpiry")
                Result.success(Pair(freePostings, expiry))
            } else {
                Result.success(Pair(0, null))
            }
        } catch (e: Exception) {
            Timber.e(e, "🎁 REFERRAL: Error checking free job postings")
            Result.failure(e)
        }
    }

    /**
     * Use a free job posting (decrement count)
     */
    suspend fun useFreeJobPosting(userId: String): Result<Boolean> {
        return try {
            val statsDoc = firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(userId)
                .get()
                .await()

            if (!statsDoc.exists()) {
                return Result.success(false)
            }

            val freePostings = statsDoc.getLong("freeJobPostings")?.toInt() ?: 0
            val expiry = statsDoc.getLong("freeJobPostingsExpiry")
            
            // Check if expired
            if (expiry != null && System.currentTimeMillis() > expiry) {
                return Result.success(false)
            }

            if (freePostings <= 0) {
                return Result.success(false)
            }

            // Decrement free postings
            firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(userId)
                .update(
                    "freeJobPostings", com.google.firebase.firestore.FieldValue.increment(-1),
                    "lastUpdated", System.currentTimeMillis()
                )
                .await()

            Timber.d("🎁 REFERRAL: Used free job posting for $userId, remaining: ${freePostings - 1}")
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "🎁 REFERRAL: Error using free job posting")
            Result.failure(e)
        }
    }

    /**
     * Get top referrers for leaderboard
     */
    suspend fun getTopReferrers(role: String?, limit: Int = 10): Result<List<ReferralStats>> {
        return try {
            var query = firestore.collection(COLLECTION_REFERRAL_STATS)
                .whereEqualTo("isBlocked", false)
                .orderBy("successfulReferrals", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            if (role != null) {
                query = firestore.collection(COLLECTION_REFERRAL_STATS)
                    .whereEqualTo("userRole", role)
                    .whereEqualTo("isBlocked", false)
                    .orderBy("successfulReferrals", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
            }

            val snapshot = query.get().await()
            val topReferrers = snapshot.documents.mapNotNull { doc ->
                try {
                    ReferralStats.fromMap(doc.data ?: emptyMap())
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(topReferrers)
        } catch (e: Exception) {
            Timber.e(e, "🎁 REFERRAL: Error getting top referrers")
            Result.failure(e)
        }
    }

    // ============================================
    // REFERRAL HISTORY
    // ============================================

    /**
     * Get referral history for current user
     */
    suspend fun getReferralHistory(limit: Int = 20): List<Referral> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        
        return try {
            val referrals = firestore.collection(COLLECTION_REFERRALS)
                .whereEqualTo("referrerUserId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            referrals.documents.mapNotNull { doc ->
                try {
                    Referral.fromMap(doc.data ?: emptyMap())
                } catch (e: Exception) {
                    Timber.e(e, "🎁 REFERRAL: Error parsing referral ${doc.id}")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "🎁 REFERRAL: Error getting history")
            emptyList()
        }
    }

    /**
     * Get referral history as Flow for real-time updates
     */
    fun getReferralHistoryFlow(limit: Int = 20): Flow<List<Referral>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection(COLLECTION_REFERRALS)
            .whereEqualTo("referrerUserId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "🎁 REFERRAL: Error listening to history")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val referrals = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Referral.fromMap(doc.data ?: emptyMap())
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(referrals)
            }

        awaitClose { listenerRegistration.remove() }
    }

    // ============================================
    // WITHDRAWAL REQUESTS
    // ============================================

    /**
     * Request withdrawal - calls Cloud Function
     */
    suspend fun requestWithdrawal(
        amount: Double,
        paymentMethod: PaymentMethod,
        upiId: String? = null,
        bankDetails: BankDetails? = null
    ): Result<WithdrawalResult> {
        return try {
            val data = hashMapOf<String, Any?>(
                "amount" to amount,
                "paymentMethod" to paymentMethod.name,
                "upiId" to upiId,
                "bankDetails" to bankDetails?.let {
                    hashMapOf(
                        "accountNumber" to it.accountNumber,
                        "ifscCode" to it.ifscCode,
                        "accountHolderName" to it.accountHolderName
                    )
                }
            )

            Timber.d("🎁 REFERRAL: Requesting withdrawal of ₹$amount")

            val result = functions
                .getHttpsCallable("requestWithdrawal")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val response = result.data as? Map<String, Any?> ?: emptyMap()
            
            val success = response["success"] as? Boolean ?: false
            
            if (success) {
                Timber.d("🎁 REFERRAL: ✅ Withdrawal request created")
                Result.success(WithdrawalResult(
                    success = true,
                    withdrawalId = response["withdrawalId"] as? String
                ))
            } else {
                val error = response["error"] as? String ?: "Failed to create withdrawal request"
                Timber.w("🎁 REFERRAL: ❌ Withdrawal failed: $error")
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Timber.e(e, "🎁 REFERRAL: Error requesting withdrawal")
            Result.failure(e)
        }
    }

    /**
     * Get withdrawal history
     */
    suspend fun getWithdrawalHistory(limit: Int = 20): List<WithdrawalRequest> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        
        return try {
            val withdrawals = firestore.collection(COLLECTION_WITHDRAWALS)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            withdrawals.documents.mapNotNull { doc ->
                try {
                    WithdrawalRequest.fromMap(doc.data ?: emptyMap())
                } catch (e: Exception) {
                    Timber.e(e, "🎁 REFERRAL: Error parsing withdrawal ${doc.id}")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "🎁 REFERRAL: Error getting withdrawal history")
            emptyList()
        }
    }

    // ============================================
    // LEADERBOARD
    // ============================================

    /**
     * Get referral leaderboard
     */
    suspend fun getLeaderboard(role: String? = null, limit: Int = 10): List<LeaderboardEntry> {
        return try {
            val data = hashMapOf<String, Any?>(
                "role" to role,
                "limit" to limit
            )

            val result = functions
                .getHttpsCallable("getReferralLeaderboard")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val response = result.data as? Map<String, Any?> ?: emptyMap()
            
            @Suppress("UNCHECKED_CAST")
            val leaderboardData = response["leaderboard"] as? List<Map<String, Any?>> ?: emptyList()
            
            leaderboardData.map { entry ->
                LeaderboardEntry(
                    rank = (entry["rank"] as? Number)?.toInt() ?: 0,
                    userId = entry["userId"] as? String ?: "",
                    userRole = entry["userRole"] as? String ?: "",
                    referralCode = entry["referralCode"] as? String ?: "",
                    successfulReferrals = (entry["successfulReferrals"] as? Number)?.toInt() ?: 0,
                    totalEarnings = (entry["totalEarnings"] as? Number)?.toDouble() ?: 0.0,
                    currentTier = entry["currentTier"] as? String ?: "BRONZE"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "🎁 REFERRAL: Error getting leaderboard")
            emptyList()
        }
    }

    // ============================================
    // SHARE REFERRAL CODE
    // ============================================

    /**
     * Generate share message for referral code
     */
    fun generateShareMessage(referralCode: String, userName: String): String {
        return """
🎉 Join DutyPe and earn ₹10!

$userName has invited you to DutyPe - India's #1 job platform for daily workers!

✅ Find jobs near you
✅ Get paid daily
✅ Verified employers

Use my referral code: $referralCode

Download now: https://play.google.com/store/apps/details?id=com.example.dutype

#DutyPe #Jobs #Earn
        """.trimIndent()
    }
}

// ============================================
// RESULT DATA CLASSES
// ============================================

data class ApplyReferralResult(
    val success: Boolean,
    val referralId: String? = null,
    val referrerName: String? = null,
    val referrerRole: String? = null,
    val message: String? = null
)

data class WithdrawalResult(
    val success: Boolean,
    val withdrawalId: String? = null
)

data class BankDetails(
    val accountNumber: String,
    val ifscCode: String,
    val accountHolderName: String
)

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val userRole: String,
    val referralCode: String,
    val successfulReferrals: Int,
    val totalEarnings: Double,
    val currentTier: String
)
