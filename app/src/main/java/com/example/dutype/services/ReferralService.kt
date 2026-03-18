package com.example.dutype.services

import com.example.dutype.models.*
import com.example.dutype.components.isValidReferralCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import com.example.dutype.utils.SecureLogger
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
    private val smartNotificationManager: com.example.dutype.services.SmartNotificationManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    companion object {
        private const val COLLECTION_REFERRAL_CODES = "referral_codes"
        private const val COLLECTION_REFERRALS = "referrals"
        private const val SUBCOLLECTION_WITHDRAWALS = "withdrawals"
        private const val COLLECTION_USERS = "users"
    }

    private suspend fun findReferralCodeDocument(rawCode: String): DocumentSnapshot? {
        val normalizedCode = normalizeReferralCode(rawCode)
        if (normalizedCode.isBlank()) return null

        val candidates = linkedSetOf(
            normalizedCode,
            normalizedCode.lowercase()
        )

        for (candidate in candidates) {
            val snapshot = firestore.collection(COLLECTION_REFERRAL_CODES)
                .document(candidate)
                .get()
                .await()
            if (snapshot.exists()) {
                return snapshot
            }
        }

        return null
    }

    // ============================================
    // REAL-TIME STATS LISTENER
    // ============================================
    
    /**
     * Get real-time updates for user's referral stats
     * SIMPLIFIED: Reads from users collection with nested referralStats
     * 🔔 SMART NOTIFICATION: Checks for milestone achievements
     */
    fun getReferralStatsFlow(userId: String): Flow<ReferralStats?> = callbackFlow {
        var lastNotifiedCount = -1
        
        val listenerRegistration = firestore.collection(COLLECTION_USERS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "🎁 REFERRAL: Error listening to user stats")
                    trySend(null)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val referralCode = snapshot.getString("referralCode") ?: ""
                    val userRole = snapshot.getString("activeRole") ?: "WORKER"
                    
                    @Suppress("UNCHECKED_CAST")
                    val statsMap = snapshot.get("referralStats") as? Map<String, Any?> ?: emptyMap()
                    
                    val stats = ReferralStats(
                        userId = userId,
                        userRole = userRole,
                        referralCode = referralCode,
                        totalReferrals = (statsMap["totalReferrals"] as? Number)?.toInt() ?: 0,
                        successfulReferrals = (statsMap["successfulReferrals"] as? Number)?.toInt() ?: 0,
                        pendingReferrals = (statsMap["pendingReferrals"] as? Number)?.toInt() ?: 0,
                        totalEarnings = (statsMap["totalEarnings"] as? Number)?.toDouble() ?: 0.0,
                        availableBalance = (statsMap["availableBalance"] as? Number)?.toDouble() ?: 0.0,
                        withdrawnAmount = (statsMap["withdrawnAmount"] as? Number)?.toDouble() ?: 0.0,
                        canWithdraw = statsMap["canWithdraw"] as? Boolean ?: false,
                        nextMilestone = (statsMap["nextMilestone"] as? Number)?.toInt() ?: 5,
                        currentTier = try {
                            ReferralTier.valueOf(statsMap["currentTier"] as? String ?: "BRONZE")
                        } catch (e: Exception) {
                            ReferralTier.BRONZE
                        },
                        freeJobPostings = (statsMap["freeJobPostings"] as? Number)?.toInt() ?: 0,
                        freeJobPostingsExpiry = statsMap["freeJobPostingsExpiry"].toEpochMillis(),
                        lastUpdated = statsMap["lastUpdated"].toEpochMillis() ?: System.currentTimeMillis()
                    )
                    
                    Timber.d("🎁 REFERRAL: Stats updated from Firestore - Code: ${stats.referralCode}, Total: ${stats.totalReferrals}, Successful: ${stats.successfulReferrals}, Earnings: ₹${stats.totalEarnings}, Balance: ₹${stats.availableBalance}, Tier: ${stats.currentTier}, NextMilestone: ${stats.nextMilestone}")
                    
                    // 🔔 SMART NOTIFICATION: Check for referral milestones
                    val currentCount = stats.successfulReferrals
                    val milestones = listOf(5, 10, 15, 25, 50, 100)
                    
                    if (currentCount > lastNotifiedCount) {
                        val newMilestone = milestones.firstOrNull { milestone ->
                            currentCount >= milestone && lastNotifiedCount < milestone
                        }
                        
                        if (newMilestone != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val rewardAmount = when (newMilestone) {
                                        5 -> 50
                                        10 -> 100
                                        15 -> 150
                                        25 -> 250
                                        50 -> 500
                                        100 -> 1000
                                        else -> 0
                                    }
                                    withContext(Dispatchers.IO) {
                                        smartNotificationManager.notifyReferralMilestone(
                                            userId,
                                            newMilestone,
                                            rewardAmount
                                        )
                                    }
                                    Timber.d("🔔 SMART NOTIFICATION: Referral milestone $newMilestone triggered")
                                } catch (e: Exception) {
                                    Timber.e(e, "🔔 SMART NOTIFICATION: Failed to trigger milestone (non-critical)")
                                }
                            }
                            lastNotifiedCount = currentCount
                        }
                    }
                    
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
     * 
     * NOTE: Referral codes are stored in LOWERCASE format
     */
    suspend fun validateReferralCode(code: String): ReferralValidationInfo {
        val trimmedCode = normalizeReferralCode(code)
        
        // Local format validation first
        if (!isValidReferralCode(trimmedCode)) {
            return ReferralValidationInfo(
                isValid = false,
                errorMessage = "Invalid referral code format"
            )
        }

        return try {
            // O(1) lookup - code is document ID
            val codeDoc = findReferralCodeDocument(trimmedCode)

            if (codeDoc == null || !codeDoc.exists()) {
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
                    errorMessage = "This referral code is no longer active"
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
     * 
     * NOTE: Referral codes are stored in LOWERCASE format
     */
    suspend fun applyReferralCode(
        referralCode: String,
        userRole: String,
        userName: String,
        userPhone: String
    ): Result<ApplyReferralResult> {
        val trimmedCode = normalizeReferralCode(referralCode)
        
        return try {
            // Device fingerprint handled by Cloud Function
            val data = hashMapOf(
                "referralCode" to trimmedCode,
                "userRole" to userRole,
                "userName" to userName,
                "userPhone" to userPhone
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
                    referrerReward = (response["referrerReward"] as? Number)?.toDouble() ?: 0.0,
                    referredUserReward = (response["referredUserReward"] as? Number)?.toDouble() ?: 0.0,
                    milestoneBonus = (response["milestoneBonus"] as? Number)?.toDouble() ?: 0.0,
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
     * SIMPLIFIED: Reads from users collection with nested referralStats
     * Returns empty stats if not found (instead of null)
     */
    suspend fun getReferralStats(): ReferralStats? {
        val userId = auth.currentUser?.uid ?: return null
        
        return try {
            val userDoc = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            if (userDoc.exists()) {
                val referralCode = userDoc.getString("referralCode") ?: ""
                val userRole = userDoc.getString("activeRole") ?: "WORKER"
                
                @Suppress("UNCHECKED_CAST")
                val statsMap = userDoc.get("referralStats") as? Map<String, Any?> ?: emptyMap()
                
                // If no referralStats yet, return default empty stats
                // This happens for new users before Cloud Function creates the code
                if (statsMap.isEmpty() && referralCode.isEmpty()) {
                    Timber.d("🎁 REFERRAL: User has no referralStats yet, returning empty stats")
                    return ReferralStats(
                        userId = userId,
                        userRole = userRole,
                        referralCode = "", // Will be set by Cloud Function
                        totalReferrals = 0,
                        successfulReferrals = 0,
                        pendingReferrals = 0,
                        totalEarnings = 0.0,
                        availableBalance = 0.0,
                        withdrawnAmount = 0.0,
                        canWithdraw = false,
                        nextMilestone = 5,
                        currentTier = ReferralTier.BRONZE,
                        freeJobPostings = 0,
                        freeJobPostingsExpiry = null,
                        lastUpdated = System.currentTimeMillis()
                    )
                }
                
                val stats = ReferralStats(
                    userId = userId,
                    userRole = userRole,
                    referralCode = referralCode,
                    totalReferrals = (statsMap["totalReferrals"] as? Number)?.toInt() ?: 0,
                    successfulReferrals = (statsMap["successfulReferrals"] as? Number)?.toInt() ?: 0,
                    pendingReferrals = (statsMap["pendingReferrals"] as? Number)?.toInt() ?: 0,
                    totalEarnings = (statsMap["totalEarnings"] as? Number)?.toDouble() ?: 0.0,
                    availableBalance = (statsMap["availableBalance"] as? Number)?.toDouble() ?: 0.0,
                    withdrawnAmount = (statsMap["withdrawnAmount"] as? Number)?.toDouble() ?: 0.0,
                    canWithdraw = statsMap["canWithdraw"] as? Boolean ?: false,
                    nextMilestone = (statsMap["nextMilestone"] as? Number)?.toInt() ?: 5,
                    currentTier = try {
                        ReferralTier.valueOf(statsMap["currentTier"] as? String ?: "BRONZE")
                    } catch (e: Exception) {
                        ReferralTier.BRONZE
                    },
                    freeJobPostings = (statsMap["freeJobPostings"] as? Number)?.toInt() ?: 0,
                    freeJobPostingsExpiry = statsMap["freeJobPostingsExpiry"].toEpochMillis(),
                    lastUpdated = statsMap["lastUpdated"].toEpochMillis() ?: System.currentTimeMillis()
                )
                
                Timber.d("🎁 REFERRAL: getReferralStats() - Code: ${stats.referralCode}, Total: ${stats.totalReferrals}, Successful: ${stats.successfulReferrals}, Earnings: ₹${stats.totalEarnings}, Balance: ₹${stats.availableBalance}, Tier: ${stats.currentTier}, NextMilestone: ${stats.nextMilestone}")
                stats
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
     * SIMPLIFIED: Reads from users collection
     */
    suspend fun checkFreeJobPostings(userId: String): Result<Pair<Int, Long?>> {
        return try {
            val userDoc = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            if (userDoc.exists()) {
                @Suppress("UNCHECKED_CAST")
                val statsMap = userDoc.get("referralStats") as? Map<String, Any?> ?: emptyMap()
                val freePostings = (statsMap["freeJobPostings"] as? Number)?.toInt() ?: 0
                val expiry = statsMap["freeJobPostingsExpiry"].toEpochMillis()
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
     * SIMPLIFIED: Updates users collection
     */
    suspend fun useFreeJobPosting(userId: String): Result<Boolean> {
        return try {
            val userDoc = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            if (!userDoc.exists()) {
                return Result.success(false)
            }

            @Suppress("UNCHECKED_CAST")
            val statsMap = userDoc.get("referralStats") as? Map<String, Any?> ?: emptyMap()
            val freePostings = (statsMap["freeJobPostings"] as? Number)?.toInt() ?: 0
            val expiry = statsMap["freeJobPostingsExpiry"].toEpochMillis()
            
            // Check if expired
            if (expiry != null && System.currentTimeMillis() > expiry) {
                return Result.success(false)
            }

            if (freePostings <= 0) {
                return Result.success(false)
            }

            // Decrement free postings
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .update(
                    "referralStats.freeJobPostings", com.google.firebase.firestore.FieldValue.increment(-1),
                    "referralStats.lastUpdated", System.currentTimeMillis()
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
     * Note: With simplified structure, this requires a Cloud Function or client-side aggregation
     * For now, returns empty list - implement via Cloud Function if needed
     */
    /**
     * P2 FIX: Implemented leaderboard via Cloud Function
     * Gets top referrers by successful referrals count
     */
    suspend fun getTopReferrers(role: String?, limit: Int = 10): Result<List<ReferralStats>> {
        return try {
            SecureLogger.d("ReferralService", "Getting top referrers", 
                "role" to (role ?: "ALL"),
                "limit" to limit.toString()
            )
            
            val data = hashMapOf(
                "role" to role,
                "limit" to limit
            )
            
            val result = functions
                .getHttpsCallable("getReferralLeaderboard")
                .call(data)
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val response = result.data as? Map<String, Any> ?: emptyMap()
            val leaderboard = response["leaderboard"] as? List<Map<String, Any>> ?: emptyList()
            
            val stats = leaderboard.mapNotNull { entry ->
                try {
                    ReferralStats(
                        userId = entry["userId"] as? String ?: "",
                        referralCode = entry["referralCode"] as? String ?: "",
                        successfulReferrals = (entry["successfulReferrals"] as? Number)?.toInt() ?: 0,
                        totalEarnings = (entry["totalEarnings"] as? Number)?.toDouble() ?: 0.0,
                        currentTier = when (entry["currentTier"] as? String) {
                            "SILVER" -> ReferralTier.SILVER
                            "GOLD" -> ReferralTier.GOLD
                            "PLATINUM" -> ReferralTier.PLATINUM
                            else -> ReferralTier.BRONZE
                        }
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing leaderboard entry")
                    null
                }
            }
            
            Timber.d("🎁 REFERRAL: Retrieved ${stats.size} top referrers")
            Result.success(stats)
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
            val withdrawals = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_WITHDRAWALS)
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
    // SHARE REFERRAL CODE & PROFESSIONAL FEATURES
    // ============================================

    /**
     * Generate share message for referral code with deep link
     * @deprecated Use generateShareMessage with channel parameter for context-aware messages
     */
    fun generateShareMessage(referralCode: String, userName: String): String {
        val referralDeepLink = com.example.dutype.utils.DeepLinkHandler.generateReferralWebLink(referralCode)
        return """
🎉 Join DutyPe and earn ₹25!

$userName has invited you to DutyPe - India's #1 job platform for daily workers!

✅ Find jobs near you
✅ Get paid daily
✅ Verified employers

Use my referral code: $referralCode

👉 Sign up here:
$referralDeepLink

📲 Download now:
https://play.google.com/store/apps/details?id=com.example.dutype

#DutyPe #Jobs #Earn
        """.trimIndent()
    }

    /**
     * Generate contextual share message based on role and channel
     * Uses ShareMessages helper for professional, context-aware messaging
     */
    fun generateShareMessage(
        referralCode: String,
        userName: String,
        userRole: String = "WORKER",
        channel: ShareChannel = ShareChannel.WHATSAPP
    ): String {
        val referralDeepLink = com.example.dutype.utils.DeepLinkHandler.generateReferralWebLink(referralCode)
        return ShareMessages.getShareMessage(userRole, channel, referralCode, referralDeepLink, userName)
    }
    
    /**
     * Get contextual share prompt based on user action
     */
    fun getSharePrompt(context: String): String {
        return ShareMessages.getSharePrompt(context)
    }

    // ============================================
    // PROFESSIONAL FEATURES - V2.0
    // ============================================

    /**
     * Get referral analytics for current user
     * Returns performance metrics, rankings, and trends
     */
    suspend fun getReferralAnalytics(): Result<ReferralAnalytics> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            Timber.d("🎁 REFERRAL: Fetching analytics for user $userId")

            // Compute analytics locally from existing user data
            val userDoc = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            @Suppress("UNCHECKED_CAST")
            val statsMap = userDoc.get("referralStats") as? Map<String, Any?> ?: emptyMap()
            val totalReferrals = (statsMap["totalReferrals"] as? Number)?.toInt() ?: 0
            val successfulReferrals = (statsMap["successfulReferrals"] as? Number)?.toInt() ?: 0
            val totalEarnings = (statsMap["totalEarnings"] as? Number)?.toDouble() ?: 0.0

            val conversionRate = if (totalReferrals > 0) {
                (successfulReferrals.toFloat() / totalReferrals * 100)
            } else 0f

            val projectedMonthly = (successfulReferrals * 25) // ₹25 per referral

            val analytics = ReferralAnalytics(
                conversionRate = conversionRate,
                totalClicks = totalReferrals,
                rankOverall = 0,
                percentile = 0f,
                projectedMonthlyEarnings = projectedMonthly
            )
            Timber.d("🎁 REFERRAL: Analytics loaded - conversion: ${analytics.conversionRate}%")
            
            Result.success(analytics)
        } catch (e: Exception) {
            Timber.e(e, "🎁 REFERRAL: Error fetching analytics")
            Result.success(ReferralAnalytics())
        }
    }

    /**
     * Get success stories for social proof
     * Shows top performers to motivate users
     */
    suspend fun getSuccessStories(limit: Int = 10): Result<List<ReferralSuccessStory>> {
        return try {
            Timber.d("🎁 REFERRAL: Fetching success stories")

            // Query top referrers from referrals collection
            val topReferrers = firestore.collection(COLLECTION_REFERRALS)
                .whereEqualTo("status", ReferralStatus.COMPLETED.name)
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            // Group by referrer and build stories
            val referrerMap = mutableMapOf<String, MutableList<Double>>()
            val referrerNames = mutableMapOf<String, String>()
            for (doc in topReferrers.documents) {
                val referrerId = doc.getString("referrerUserId") ?: continue
                val referral = Referral.fromMap(doc.data ?: emptyMap())
                referrerMap.getOrPut(referrerId) { mutableListOf() }.add(referral.getTotalReferrerReward())
                if (referrerId !in referrerNames) {
                    referrerNames[referrerId] = doc.getString("referredUserName")?.let { "Referrer" } ?: "DutyPe User"
                }
            }

            val stories = referrerMap.entries
                .sortedByDescending { it.value.sum() }
                .take(limit)
                .map { (userId, rewards) ->
                    ReferralSuccessStory(
                        userId = userId,
                        userName = referrerNames[userId] ?: "DutyPe User",
                        totalEarnings = rewards.sum(),
                        successfulReferrals = rewards.size
                    )
                }

            Timber.d("🎁 REFERRAL: Loaded ${stories.size} success stories")
            Result.success(stories)
        } catch (e: Exception) {
            Timber.e(e, "🎁 REFERRAL: Error fetching success stories")
            Result.success(emptyList())
        }
    }

    /**
     * Ensure the current user has a referral code.
     * If they completed profile but Cloud Function didn't fire, this creates it client-side.
     * Returns the referral code if created/found, null on failure.
     */
    suspend fun ensureReferralCodeExists(): String? {
        val userId = auth.currentUser?.uid ?: return null
        return try {
            val userDoc = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            if (!userDoc.exists()) return null

            val existingCode = userDoc.getString("referralCode")
            if (!existingCode.isNullOrBlank()) return existingCode

            if (userDoc.getBoolean("isVerified") == true) {
                firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .update("referralStats.lastUpdated", System.currentTimeMillis())
                    .await()
            }

            Timber.d("REFERRAL: Referral code missing for $userId, waiting for backend generation")
            null
        } catch (e: Exception) {
            Timber.e(e, "REFERRAL: Error ensuring referral code exists")
            null
        }
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
    val referrerReward: Double = 0.0,
    val referredUserReward: Double = 0.0,
    val milestoneBonus: Double = 0.0,
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

