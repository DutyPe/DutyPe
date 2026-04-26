package com.example.dutype.services

import com.example.dutype.models.*
import com.example.dutype.components.isValidReferralCode
import com.example.dutype.firestore.FirestoreCollections
import com.example.dutype.utils.PhoneNumberUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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
        private const val COLLECTION_REFERRAL_STATS = FirestoreCollections.REFERRAL_STATS
        private const val COLLECTION_WORKER_PROFILES = FirestoreCollections.WORKER_PROFILES
        private const val COLLECTION_EMPLOYER_PROFILES = FirestoreCollections.EMPLOYER_PROFILES
        private const val COLLECTION_PHONE_ROLES = FirestoreCollections.PHONE_ROLES
        private const val SUBCOLLECTION_WITHDRAWALS = FirestoreCollections.WITHDRAWALS
        private const val FIELD_REFERRER_ID = "referrerId"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_STATUS = "status"
        private const val STATUS_COMPLETED = "COMPLETED"

        // #14 fix: in-process cache TTL for the heavy bootstrap reads on the
        // Refer & Earn screen. Each open used to re-execute referral_stats +
        // referral_codes + users reads serially. With a short TTL the screen
        // is instant on every re-entry within the same session while still
        // letting the realtime snapshot listener eventually overwrite stale
        // data when the user actually earns a new reward.
        private const val STATS_CACHE_TTL_MS = 60_000L
    }

    @Volatile private var cachedStatsKey: String? = null
    @Volatile private var cachedStats: ReferralStats? = null
    @Volatile private var cachedStatsAt: Long = 0L
    @Volatile private var cachedReferrerKey: String? = null
    @Volatile private var cachedReferrer: ReferrerInfo? = null
    @Volatile private var cachedReferrerAt: Long = 0L

    private data class ReferralProfile(
        val role: String,
        val data: Map<String, Any?>
    )

    /** Invalidate caches after a write that may change the snapshot. */
    private fun invalidateReferralCaches() {
        cachedStats = null
        cachedStatsAt = 0L
        cachedReferrer = null
        cachedReferrerAt = 0L
    }

    private suspend fun findReferralCodeDocument(rawCode: String): DocumentSnapshot? {
        val normalizedCode = normalizeReferralCode(rawCode)
        if (normalizedCode.isBlank()) return null

        val candidates = linkedSetOf(
            normalizedCode,
            normalizedCode.lowercase()
        )

        for (candidate in candidates) {
            try {
                val snapshot = firestore.collection(COLLECTION_REFERRAL_CODES)
                    .document(candidate)
                    .get()
                    .await()
                if (snapshot.exists()) {
                    return snapshot
                }
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Timber.w("🎁 REFERRAL: Lookup denied for %s, trying next candidate", candidate)
                    continue
                }
                throw e
            }
        }

        return null
    }

    private suspend fun ensureReferralCodeForUser(
        userId: String,
        userRole: String,
        userName: String,
        existingUserCode: String
    ): String {
        val normalizedExistingCode = normalizeReferralCode(existingUserCode)
        if (normalizedExistingCode.isNotBlank()) {
            return normalizedExistingCode
        }

        return callEnsureUserReferralCode(
            userRole = userRole,
            userName = userName
        )
    }

    private fun profileCollectionForRole(role: String): String {
        return if (role.uppercase() == "EMPLOYER") COLLECTION_EMPLOYER_PROFILES else COLLECTION_WORKER_PROFILES
    }

    private suspend fun currentPhoneRoleData(userId: String): Map<String, Any?> {
        if (auth.currentUser?.uid != userId) return emptyMap()
        val normalizedPhone = auth.currentUser?.phoneNumber?.let(PhoneNumberUtils::normalize).orEmpty()
        if (normalizedPhone.isBlank()) return emptyMap()
        return firestore.collection(COLLECTION_PHONE_ROLES)
            .document(normalizedPhone)
            .get()
            .await()
            .data
            .orEmpty()
    }

    private suspend fun loadReferralProfile(userId: String, preferredRole: String? = null): ReferralProfile {
        val phoneRoleData = currentPhoneRoleData(userId)
        val roles = linkedSetOf<String>()
        preferredRole?.takeIf { it.isNotBlank() }?.let { roles += it.uppercase() }
        @Suppress("UNCHECKED_CAST")
        val phoneRoles = phoneRoleData["roles"] as? List<*>
        phoneRoles.orEmpty()
            .mapNotNull { it as? String }
            .map { it.uppercase() }
            .forEach { roles += it }
        roles += "WORKER"
        roles += "EMPLOYER"

        for (role in roles) {
            val snapshot = firestore.collection(profileCollectionForRole(role))
                .document(userId)
                .get()
                .await()
            if (snapshot.exists()) {
                val merged = snapshot.data.orEmpty() + phoneRoleData
                return ReferralProfile(role, merged)
            }
        }

        return ReferralProfile(preferredRole?.uppercase() ?: "WORKER", phoneRoleData)
    }

    private fun referralCodeFromProfile(profile: ReferralProfile): String {
        return normalizeReferralCode(profile.data["referralCode"] as? String ?: "")
    }

    private fun displayNameFromProfile(profile: ReferralProfile): String {
        return profile.data["fullName"] as? String
            ?: profile.data["companyName"] as? String
            ?: profile.data["name"] as? String
            ?: ""
    }

    private suspend fun findExistingReferralCodeForUser(userId: String): String {
        return try {
            val statsDoc = firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(userId)
                .get()
                .await()

            val statsCode = normalizeReferralCode(statsDoc.getString("referralCode") ?: "")
            if (statsCode.isNotBlank()) {
                return statsCode
            }

            val profile = loadReferralProfile(
                userId = userId,
                preferredRole = statsDoc.getString("userRole")
            )
            val profileCode = referralCodeFromProfile(profile)
            if (profileCode.isNotBlank()) {
                return profileCode
            }

            callEnsureUserReferralCode(
                userRole = profile.role,
                userName = displayNameFromProfile(profile)
            )
        } catch (e: Exception) {
            Timber.w(e, "🎁 REFERRAL: Unable to resolve referral code for user $userId")
            ""
        }
    }

    private suspend fun callEnsureUserReferralCode(
        userRole: String,
        userName: String
    ): String {
        return try {
            val payload = hashMapOf(
                "userRole" to userRole,
                "userName" to userName
            )

            val result = functions
                .getHttpsCallable("ensureUserReferralCode")
                .call(payload)
                .await()

            @Suppress("UNCHECKED_CAST")
            val response = result.data as? Map<String, Any?> ?: emptyMap()
            val success = response["success"] as? Boolean ?: false
            if (!success) {
                return ""
            }

            normalizeReferralCode(response["referralCode"]?.toString().orEmpty())
        } catch (e: Exception) {
            Timber.w(e, "🎁 REFERRAL: ensureUserReferralCode callable failed")
            ""
        }
    }

    private fun mapReferralStats(
        userId: String,
        userRole: String,
        referralCode: String,
        statsMap: Map<String, Any?>
    ): ReferralStats {
        return ReferralStats(
            userId = userId,
            userRole = userRole,
            referralCode = referralCode,
            totalReferrals = (statsMap["totalReferrals"] as? Number)?.toInt() ?: 0,
            successfulReferrals = (statsMap["successfulReferrals"] as? Number)?.toInt() ?: 0,
            totalEarnings = (statsMap["totalEarnings"] as? Number)?.toDouble() ?: 0.0,
            availableBalance = (statsMap["availableBalance"] as? Number)?.toDouble() ?: 0.0,
            canWithdraw = ReferralRewards.canWithdraw(
                (statsMap["availableBalance"] as? Number)?.toDouble() ?: 0.0
            ),
            currentTier = try {
                ReferralTier.valueOf(statsMap["currentTier"] as? String ?: "BRONZE")
            } catch (e: Exception) {
                ReferralTier.BRONZE
            }
        )
    }

    suspend fun getCurrentUserReferrerInfo(): Result<ReferrerInfo> {
        val userId = auth.currentUser?.uid ?: return Result.success(ReferrerInfo())

        // #14 fix: referrer info almost never changes after signup; serve
        // from cache so re-opening Refer & Earn skips two users/* reads.
        val now = System.currentTimeMillis()
        cachedReferrer?.let { snap ->
            if (cachedReferrerKey == userId && (now - cachedReferrerAt) < STATS_CACHE_TTL_MS) {
                return Result.success(snap)
            }
        }

        return try {
            val profile = loadReferralProfile(userId)
            val referredByCode = normalizeReferralCode(profile.data["referredByCode"] as? String ?: "")
            val referredByUserId = profile.data["referredByUserId"] as? String ?: ""
            var referrerName = ""
            var referrerRole = ""

            if (referredByUserId.isNotBlank()) {
                val referrerProfile = loadReferralProfile(referredByUserId)
                referrerName = displayNameFromProfile(referrerProfile)
                referrerRole = referrerProfile.role
            }

            if (referrerName.isBlank() && referredByCode.isNotBlank()) {
                val codeDoc = findReferralCodeDocument(referredByCode)
                if (codeDoc != null && codeDoc.exists()) {
                    referrerName = codeDoc.getString("userName") ?: ""
                    referrerRole = codeDoc.getString("userRole") ?: referrerRole
                }
            }

            val info = ReferrerInfo(
                referredByCode = referredByCode,
                referredByUserId = referredByUserId,
                referrerName = referrerName,
                referrerRole = referrerRole
            )
            cachedReferrer = info
            cachedReferrerKey = userId
            cachedReferrerAt = now
            Result.success(info)
        } catch (e: Exception) {
            Timber.e(e, "🎁 REFERRAL: Error getting referrer info")
            Result.failure(e)
        }
    }

    // ============================================
    // REAL-TIME STATS LISTENER
    // ============================================
    
    /**
     * Get real-time updates for user's referral stats
     * Reads referralCode from referral_codes/{userId} and stats from referral_stats/{userId}
     * 🔔 SMART NOTIFICATION: Checks for milestone achievements
     */
    fun getReferralStatsFlow(userId: String): Flow<ReferralStats?> = callbackFlow {
        var lastNotifiedCount = -1
        
        val listenerRegistration = firestore.collection(COLLECTION_REFERRAL_STATS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "🎁 REFERRAL: Error listening to user stats")
                    trySend(null)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val rawReferralCode = snapshot.getString("referralCode") ?: ""
                    val userRole = snapshot.getString("userRole") ?: "WORKER"

                    @Suppress("UNCHECKED_CAST")
                    val statsMap = snapshot.data as? Map<String, Any?> ?: emptyMap()

                    CoroutineScope(Dispatchers.IO).launch {
                        val resolvedCode = if (rawReferralCode.isNotBlank()) {
                            normalizeReferralCode(rawReferralCode)
                        } else {
                            findExistingReferralCodeForUser(userId)
                        }

                        val stats = mapReferralStats(
                            userId = userId,
                            userRole = userRole,
                            referralCode = resolvedCode,
                            statsMap = statsMap
                        )

                        Timber.d("🎁 REFERRAL: Stats updated from Firestore - Code: ${stats.referralCode}, Total: ${stats.totalReferrals}, Successful: ${stats.successfulReferrals}, Earnings: ₹${stats.totalEarnings}, Balance: ₹${stats.availableBalance}, Tier: ${stats.currentTier}")

                        // 🔔 SMART NOTIFICATION: Check for referral milestones
                        val currentCount = stats.successfulReferrals
                        val milestones = listOf(5, 10, 15, 25, 50, 100)

                        if (currentCount > lastNotifiedCount) {
                            val newMilestone = milestones.firstOrNull { milestone ->
                                currentCount >= milestone && lastNotifiedCount < milestone
                            }

                            if (newMilestone != null) {
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
                                lastNotifiedCount = currentCount
                            }
                        }

                        trySend(stats)
                    }
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val profile = loadReferralProfile(userId)
                            val profileCode = referralCodeFromProfile(profile)
                            val resolvedCode = if (profileCode.isNotBlank()) {
                                profileCode
                            } else {
                                findExistingReferralCodeForUser(userId)
                            }

                            val fallbackStats = ReferralStats(
                                userId = userId,
                                userRole = profile.role,
                                referralCode = resolvedCode,
                                totalReferrals = 0,
                                successfulReferrals = 0,
                                totalEarnings = 0.0,
                                availableBalance = 0.0,
                                canWithdraw = false,
                                currentTier = ReferralTier.BRONZE
                            )
                            trySend(fallbackStats)
                        } catch (e: Exception) {
                            Timber.w(e, "🎁 REFERRAL: Failed fallback stats generation")
                            trySend(null)
                        }
                    }
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
                invalidateReferralCaches()
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
     * Reads from referral_stats/{userId} collection.
     * Returns empty stats if not found (instead of null)
     */
    suspend fun getReferralStats(): ReferralStats? {
        val userId = auth.currentUser?.uid ?: return null

        // #14 fix: serve from cache if fresh; this is the call that fires on
        // every open of Refer & Earn and used to issue 1–3 sequential
        // network reads.
        val now = System.currentTimeMillis()
        cachedStats?.let { snap ->
            if (cachedStatsKey == userId && (now - cachedStatsAt) < STATS_CACHE_TTL_MS) {
                return snap
            }
        }

        return try {
            val statsDoc = firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(userId)
                .get()
                .await()

            if (statsDoc.exists()) {
                val userRole = statsDoc.getString("userRole") ?: "WORKER"
                
                @Suppress("UNCHECKED_CAST")
                val statsMap = statsDoc.data as? Map<String, Any?> ?: emptyMap()

                var referralCode = normalizeReferralCode(statsDoc.getString("referralCode") ?: "")
                if (referralCode.isBlank()) {
                    referralCode = findExistingReferralCodeForUser(userId)
                }

                if (referralCode.isBlank()) {
                    val profile = loadReferralProfile(userId, userRole)
                    referralCode = ensureReferralCodeForUser(
                        userId = userId,
                        userRole = profile.role,
                        userName = displayNameFromProfile(profile),
                        existingUserCode = referralCodeFromProfile(profile)
                    )
                }
                
                // If no stats yet, return default empty stats
                if (statsMap.isEmpty() && referralCode.isEmpty()) {
                    Timber.d("🎁 REFERRAL: User has no referral_stats yet, returning empty stats")
                    return ReferralStats(
                        userId = userId,
                        userRole = userRole,
                        referralCode = "",
                        totalReferrals = 0,
                        successfulReferrals = 0,
                        totalEarnings = 0.0,
                        availableBalance = 0.0,
                        canWithdraw = false,
                        currentTier = ReferralTier.BRONZE
                    )
                }
                
                val stats = mapReferralStats(
                    userId = userId,
                    userRole = userRole,
                    referralCode = referralCode,
                    statsMap = statsMap
                )
                
                Timber.d("🎁 REFERRAL: getReferralStats() - Code: ${stats.referralCode}, Total: ${stats.totalReferrals}")
                cachedStats = stats
                cachedStatsKey = userId
                cachedStatsAt = now
                stats
            } else {
                val profile = loadReferralProfile(userId)
                val userRole = profile.role
                val fallbackCode = ensureReferralCodeForUser(
                    userId = userId,
                    userRole = userRole,
                    userName = displayNameFromProfile(profile),
                    existingUserCode = referralCodeFromProfile(profile)
                )

                Timber.d("🎁 REFERRAL: No referral_stats doc for user $userId, returning empty stats fallback")
                val fallback = ReferralStats(
                    userId = userId,
                    userRole = userRole,
                    referralCode = fallbackCode,
                    totalReferrals = 0,
                    successfulReferrals = 0,
                    totalEarnings = 0.0,
                    availableBalance = 0.0,
                    canWithdraw = false,
                    currentTier = ReferralTier.BRONZE
                )
                cachedStats = fallback
                cachedStatsKey = userId
                cachedStatsAt = now
                fallback
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
                Result.failure(Exception("Unable to load referral stats"))
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
                            "DIAMOND", "ELITE" -> ReferralTier.DIAMOND
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
                .whereEqualTo(FIELD_REFERRER_ID, userId)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
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
            .whereEqualTo(FIELD_REFERRER_ID, userId)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
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
                invalidateReferralCaches()
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
            val withdrawals = firestore.collection(COLLECTION_REFERRAL_STATS)
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

            // Compute analytics from canonical referral_stats collection only.
            val statsDoc = firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(userId)
                .get()
                .await()

            val statsMap = statsDoc.data ?: emptyMap<String, Any?>()
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
                .whereEqualTo(FIELD_STATUS, STATUS_COMPLETED)
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            // Group by referrer and build stories
            val referrerMap = mutableMapOf<String, MutableList<Double>>()
            val referrerNames = mutableMapOf<String, String>()
            for (doc in topReferrers.documents) {
                val referrerId = doc.getString(FIELD_REFERRER_ID) ?: continue
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
     * Checks referral_stats/{userId} and referral_codes collections.
     * Returns the referral code if found, null otherwise (backend generates it).
     */
    suspend fun ensureReferralCodeExists(): String? {
        val userId = auth.currentUser?.uid ?: return null
        return try {
            // Check referral_stats first
            val statsDoc = firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(userId)
                .get()
                .await()

            val existingCode = normalizeReferralCode(statsDoc.getString("referralCode") ?: "")
            if (existingCode.isNotBlank()) return existingCode

            val fallbackCode = findExistingReferralCodeForUser(userId)
            if (fallbackCode.isNotBlank()) {
                return fallbackCode
            }

            val profile = loadReferralProfile(userId)
            val userRole = profile.role
            val userName = displayNameFromProfile(profile)
            val userCode = referralCodeFromProfile(profile)

            val ensuredCode = ensureReferralCodeForUser(
                userId = userId,
                userRole = userRole,
                userName = userName,
                existingUserCode = userCode
            )

            if (ensuredCode.isNotBlank()) {
                return ensuredCode
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

