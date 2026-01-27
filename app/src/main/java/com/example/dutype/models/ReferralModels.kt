package com.example.dutype.models

import androidx.annotation.Keep
import java.security.SecureRandom

/**
 * Referral System Models - SCALABLE FOR 10 LAKH+ USERS
 * 
 * Architecture:
 * - referral_stats: One document per user (userId as document ID)
 * - referral_codes: Separate collection for O(1) code lookup (code as document ID)
 * - referrals: Individual referral records with composite indexes
 * - withdrawal_requests: Withdrawal tracking with status
 * 
 * Reward Structure:
 * - Worker/Employer: ₹10 per successful referral
 *   - 5 referrals: +₹50 bonus, can withdraw
 *   - 10 referrals: +₹100 bonus, can withdraw
 *   - 15 referrals: +₹150 bonus, can withdraw anytime after
 * 
 * - Employer Extra: Free job postings
 *   - 5 referrals: 5 free job postings for 15 days
 *   - 10 referrals: 10 free job postings for 1 month
 * 
 * Scalability Features:
 * - Unique 9-character codes (WRK/EMP + 6 alphanumeric) = 2.1 billion combinations
 * - Separate code lookup collection for O(1) validation
 * - Denormalized data to minimize reads
 * - Batch operations for stats updates
 * - Fraud prevention with rate limiting and duplicate checks
 */

/**
 * Referral code lookup document (stored in referral_codes collection)
 * Document ID = referralCode (e.g., "WRK1A2B3C")
 * This enables O(1) code validation without querying
 */
@Keep
data class ReferralCodeLookup(
    val code: String = "",                  // The referral code (also document ID)
    val userId: String = "",                // Owner of this code
    val userRole: String = "",              // WORKER or EMPLOYER
    val userName: String = "",              // Display name for validation feedback
    val isActive: Boolean = true,           // Can be deactivated for fraud
    val createdAt: Long = System.currentTimeMillis(),
    val totalUsed: Int = 0                  // How many times this code was used
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "code" to code,
        "userId" to userId,
        "userRole" to userRole,
        "userName" to userName,
        "isActive" to isActive,
        "createdAt" to createdAt,
        "totalUsed" to totalUsed
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>): ReferralCodeLookup {
            return ReferralCodeLookup(
                code = map["code"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                userRole = map["userRole"] as? String ?: "",
                userName = map["userName"] as? String ?: "",
                isActive = map["isActive"] as? Boolean ?: true,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                totalUsed = (map["totalUsed"] as? Number)?.toInt() ?: 0
            )
        }
    }
}

/**
 * Referral data stored in Firestore (referrals collection)
 * Tracks individual referral relationships
 */
@Keep
data class Referral(
    val id: String = "",
    val referrerUserId: String = "",        // User who shared the code
    val referrerRole: String = "",          // WORKER or EMPLOYER
    val referredUserId: String = "",        // New user who used the code
    val referredRole: String = "",          // WORKER or EMPLOYER
    val referralCode: String = "",          // The code used
    val status: ReferralStatus = ReferralStatus.PENDING,
    val rewardAmount: Double = 0.0,         // Amount earned (₹10 per referral)
    val bonusAmount: Double = 0.0,          // Milestone bonus (if any)
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,          // When referral was marked successful
    val expiresAt: Long = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L), // 30 days expiry
    val referredUserName: String = "",      // Name of referred user for display
    val referredUserPhone: String = "",     // Phone of referred user (masked)
    val deviceFingerprint: String? = null,  // For fraud detection
    val ipAddress: String? = null           // For fraud detection
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "referrerUserId" to referrerUserId,
        "referrerRole" to referrerRole,
        "referredUserId" to referredUserId,
        "referredRole" to referredRole,
        "referralCode" to referralCode,
        "status" to status.name,
        "rewardAmount" to rewardAmount,
        "bonusAmount" to bonusAmount,
        "createdAt" to createdAt,
        "completedAt" to completedAt,
        "expiresAt" to expiresAt,
        "referredUserName" to referredUserName,
        "referredUserPhone" to referredUserPhone,
        "deviceFingerprint" to deviceFingerprint,
        "ipAddress" to ipAddress
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>): Referral {
            return Referral(
                id = map["id"] as? String ?: "",
                referrerUserId = map["referrerUserId"] as? String ?: "",
                referrerRole = map["referrerRole"] as? String ?: "",
                referredUserId = map["referredUserId"] as? String ?: "",
                referredRole = map["referredRole"] as? String ?: "",
                referralCode = map["referralCode"] as? String ?: "",
                status = try { 
                    ReferralStatus.valueOf(map["status"] as? String ?: "PENDING") 
                } catch (e: Exception) { 
                    ReferralStatus.PENDING 
                },
                rewardAmount = (map["rewardAmount"] as? Number)?.toDouble() ?: 0.0,
                bonusAmount = (map["bonusAmount"] as? Number)?.toDouble() ?: 0.0,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                completedAt = (map["completedAt"] as? Number)?.toLong(),
                expiresAt = (map["expiresAt"] as? Number)?.toLong() ?: (System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L),
                referredUserName = map["referredUserName"] as? String ?: "",
                referredUserPhone = map["referredUserPhone"] as? String ?: "",
                deviceFingerprint = map["deviceFingerprint"] as? String,
                ipAddress = map["ipAddress"] as? String
            )
        }
    }
}

/**
 * Referral status enum
 */
enum class ReferralStatus {
    PENDING,        // User signed up but hasn't completed profile
    COMPLETED,      // User completed profile - reward credited
    EXPIRED,        // Referral expired (user didn't complete in 30 days)
    CANCELLED,      // Referral cancelled (fraud detection)
    REJECTED        // Referral rejected (same device/IP fraud)
}

/**
 * User's referral statistics (referral_stats collection)
 * Document ID = userId for O(1) lookup
 */
@Keep
data class ReferralStats(
    val userId: String = "",
    val userRole: String = "",
    val referralCode: String = "",          // Unique code for this user
    val totalReferrals: Int = 0,            // Total people who used code
    val successfulReferrals: Int = 0,       // Completed referrals
    val pendingReferrals: Int = 0,          // Pending referrals
    val expiredReferrals: Int = 0,          // Expired referrals
    val rejectedReferrals: Int = 0,         // Rejected (fraud) referrals
    val totalEarnings: Double = 0.0,        // Total earned from referrals
    val pendingEarnings: Double = 0.0,      // Pending earnings
    val withdrawnAmount: Double = 0.0,      // Amount already withdrawn
    val availableBalance: Double = 0.0,     // Available for withdrawal
    val canWithdraw: Boolean = false,       // Based on milestone rules
    val nextMilestone: Int = 5,             // Next milestone to reach
    val currentTier: ReferralTier = ReferralTier.BRONZE, // Gamification tier
    val freeJobPostings: Int = 0,           // For employers only
    val freeJobPostingsExpiry: Long? = null,// Expiry date for free postings
    val lastUpdated: Long = System.currentTimeMillis(),
    val referredByCode: String? = null,     // Code used when this user signed up
    val referredByUserId: String? = null,   // Who referred this user
    val lastWithdrawalAt: Long? = null,     // Last withdrawal timestamp
    val totalWithdrawals: Int = 0,          // Number of withdrawals made
    val isBlocked: Boolean = false,         // Blocked for fraud
    val blockReason: String? = null         // Reason for blocking
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "userRole" to userRole,
        "referralCode" to referralCode,
        "totalReferrals" to totalReferrals,
        "successfulReferrals" to successfulReferrals,
        "pendingReferrals" to pendingReferrals,
        "expiredReferrals" to expiredReferrals,
        "rejectedReferrals" to rejectedReferrals,
        "totalEarnings" to totalEarnings,
        "pendingEarnings" to pendingEarnings,
        "withdrawnAmount" to withdrawnAmount,
        "availableBalance" to availableBalance,
        "canWithdraw" to canWithdraw,
        "nextMilestone" to nextMilestone,
        "currentTier" to currentTier.name,
        "freeJobPostings" to freeJobPostings,
        "freeJobPostingsExpiry" to freeJobPostingsExpiry,
        "lastUpdated" to lastUpdated,
        "referredByCode" to referredByCode,
        "referredByUserId" to referredByUserId,
        "lastWithdrawalAt" to lastWithdrawalAt,
        "totalWithdrawals" to totalWithdrawals,
        "isBlocked" to isBlocked,
        "blockReason" to blockReason
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>): ReferralStats {
            return ReferralStats(
                userId = map["userId"] as? String ?: "",
                userRole = map["userRole"] as? String ?: "",
                referralCode = map["referralCode"] as? String ?: "",
                totalReferrals = (map["totalReferrals"] as? Number)?.toInt() ?: 0,
                successfulReferrals = (map["successfulReferrals"] as? Number)?.toInt() ?: 0,
                pendingReferrals = (map["pendingReferrals"] as? Number)?.toInt() ?: 0,
                expiredReferrals = (map["expiredReferrals"] as? Number)?.toInt() ?: 0,
                rejectedReferrals = (map["rejectedReferrals"] as? Number)?.toInt() ?: 0,
                totalEarnings = (map["totalEarnings"] as? Number)?.toDouble() ?: 0.0,
                pendingEarnings = (map["pendingEarnings"] as? Number)?.toDouble() ?: 0.0,
                withdrawnAmount = (map["withdrawnAmount"] as? Number)?.toDouble() ?: 0.0,
                availableBalance = (map["availableBalance"] as? Number)?.toDouble() ?: 0.0,
                canWithdraw = map["canWithdraw"] as? Boolean ?: false,
                nextMilestone = (map["nextMilestone"] as? Number)?.toInt() ?: 5,
                currentTier = try {
                    ReferralTier.valueOf(map["currentTier"] as? String ?: "BRONZE")
                } catch (e: Exception) {
                    ReferralTier.BRONZE
                },
                freeJobPostings = (map["freeJobPostings"] as? Number)?.toInt() ?: 0,
                freeJobPostingsExpiry = (map["freeJobPostingsExpiry"] as? Number)?.toLong(),
                lastUpdated = (map["lastUpdated"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                referredByCode = map["referredByCode"] as? String,
                referredByUserId = map["referredByUserId"] as? String,
                lastWithdrawalAt = (map["lastWithdrawalAt"] as? Number)?.toLong(),
                totalWithdrawals = (map["totalWithdrawals"] as? Number)?.toInt() ?: 0,
                isBlocked = map["isBlocked"] as? Boolean ?: false,
                blockReason = map["blockReason"] as? String
            )
        }
    }
}

/**
 * Referral tier for gamification
 */
enum class ReferralTier {
    BRONZE,     // 0-4 referrals
    SILVER,     // 5-9 referrals
    GOLD,       // 10-24 referrals
    PLATINUM,   // 25-49 referrals
    DIAMOND     // 50+ referrals
}

/**
 * Withdrawal request
 */
@Keep
data class WithdrawalRequest(
    val id: String = "",
    val userId: String = "",
    val userRole: String = "",
    val amount: Double = 0.0,
    val status: WithdrawalStatus = WithdrawalStatus.PENDING,
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val upiId: String? = null,
    val bankAccountNumber: String? = null,
    val ifscCode: String? = null,
    val accountHolderName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null,
    val transactionId: String? = null,
    val remarks: String? = null,
    val adminNotes: String? = null,
    val processedBy: String? = null         // Admin who processed
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "userRole" to userRole,
        "amount" to amount,
        "status" to status.name,
        "paymentMethod" to paymentMethod.name,
        "upiId" to upiId,
        "bankAccountNumber" to bankAccountNumber,
        "ifscCode" to ifscCode,
        "accountHolderName" to accountHolderName,
        "createdAt" to createdAt,
        "processedAt" to processedAt,
        "transactionId" to transactionId,
        "remarks" to remarks,
        "adminNotes" to adminNotes,
        "processedBy" to processedBy
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>): WithdrawalRequest {
            return WithdrawalRequest(
                id = map["id"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                userRole = map["userRole"] as? String ?: "",
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                status = try {
                    WithdrawalStatus.valueOf(map["status"] as? String ?: "PENDING")
                } catch (e: Exception) {
                    WithdrawalStatus.PENDING
                },
                paymentMethod = try {
                    PaymentMethod.valueOf(map["paymentMethod"] as? String ?: "UPI")
                } catch (e: Exception) {
                    PaymentMethod.UPI
                },
                upiId = map["upiId"] as? String,
                bankAccountNumber = map["bankAccountNumber"] as? String,
                ifscCode = map["ifscCode"] as? String,
                accountHolderName = map["accountHolderName"] as? String,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                processedAt = (map["processedAt"] as? Number)?.toLong(),
                transactionId = map["transactionId"] as? String,
                remarks = map["remarks"] as? String,
                adminNotes = map["adminNotes"] as? String,
                processedBy = map["processedBy"] as? String
            )
        }
    }
}

enum class WithdrawalStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    ON_HOLD       // For fraud review
}

enum class PaymentMethod {
    UPI,
    BANK_TRANSFER,
    PAYTM,
    PHONEPE,
    GPAY
}

/**
 * Referral reward constants and calculations
 */
object ReferralRewards {
    const val REWARD_PER_REFERRAL = 10.0  // ₹10 per successful referral
    const val MIN_WITHDRAWAL_AMOUNT = 50.0 // Minimum ₹50 to withdraw
    const val MAX_WITHDRAWAL_PER_DAY = 1000.0 // Max ₹1000 per day
    const val REFERRAL_EXPIRY_DAYS = 30   // Referral expires in 30 days
    
    // Milestone bonuses
    val MILESTONES = mapOf(
        5 to 50.0,    // 5 referrals = ₹50 bonus
        10 to 100.0,  // 10 referrals = ₹100 bonus
        15 to 150.0,  // 15 referrals = ₹150 bonus
        25 to 250.0,  // 25 referrals = ₹250 bonus
        50 to 500.0   // 50 referrals = ₹500 bonus
    )
    
    // Withdrawal thresholds (can withdraw at these milestones)
    val WITHDRAWAL_MILESTONES = listOf(5, 10, 15)
    
    // Employer free job posting rewards
    val EMPLOYER_FREE_POSTINGS = mapOf(
        5 to Pair(5, 15),    // 5 referrals = 5 free postings for 15 days
        10 to Pair(10, 30),  // 10 referrals = 10 free postings for 30 days
        25 to Pair(25, 60)   // 25 referrals = 25 free postings for 60 days
    )
    
    // Tier thresholds
    val TIER_THRESHOLDS = mapOf(
        ReferralTier.BRONZE to 0,
        ReferralTier.SILVER to 5,
        ReferralTier.GOLD to 10,
        ReferralTier.PLATINUM to 25,
        ReferralTier.DIAMOND to 50
    )
    
    /**
     * Calculate total reward for a given number of successful referrals
     */
    fun calculateTotalReward(successfulReferrals: Int): Double {
        var total = successfulReferrals * REWARD_PER_REFERRAL
        
        // Add milestone bonuses
        MILESTONES.forEach { (milestone, bonus) ->
            if (successfulReferrals >= milestone) {
                total += bonus
            }
        }
        
        return total
    }
    
    /**
     * Check if user can withdraw based on referral count
     */
    fun canWithdraw(successfulReferrals: Int): Boolean {
        // Can withdraw at 5, 10, 15 referrals, and anytime after 15
        return successfulReferrals >= 15 || WITHDRAWAL_MILESTONES.contains(successfulReferrals)
    }
    
    /**
     * Get next milestone for user
     */
    fun getNextMilestone(successfulReferrals: Int): Int {
        return when {
            successfulReferrals < 5 -> 5
            successfulReferrals < 10 -> 10
            successfulReferrals < 15 -> 15
            successfulReferrals < 25 -> 25
            successfulReferrals < 50 -> 50
            else -> successfulReferrals + 10  // After 50, every 10
        }
    }
    
    /**
     * Get bonus for reaching a milestone
     */
    fun getMilestoneBonus(milestone: Int): Double {
        return MILESTONES[milestone] ?: 0.0
    }
    
    /**
     * Get free job postings for employer at milestone
     */
    fun getEmployerFreePostings(successfulReferrals: Int): Pair<Int, Int>? {
        return when {
            successfulReferrals >= 25 -> EMPLOYER_FREE_POSTINGS[25]
            successfulReferrals >= 10 -> EMPLOYER_FREE_POSTINGS[10]
            successfulReferrals >= 5 -> EMPLOYER_FREE_POSTINGS[5]
            else -> null
        }
    }
    
    /**
     * Get tier for referral count
     */
    fun getTier(successfulReferrals: Int): ReferralTier {
        return when {
            successfulReferrals >= 50 -> ReferralTier.DIAMOND
            successfulReferrals >= 25 -> ReferralTier.PLATINUM
            successfulReferrals >= 10 -> ReferralTier.GOLD
            successfulReferrals >= 5 -> ReferralTier.SILVER
            else -> ReferralTier.BRONZE
        }
    }
    
    /**
     * Get tier display name
     */
    fun getTierDisplayName(tier: ReferralTier): String {
        return when (tier) {
            ReferralTier.BRONZE -> "🥉 Bronze"
            ReferralTier.SILVER -> "🥈 Silver"
            ReferralTier.GOLD -> "🥇 Gold"
            ReferralTier.PLATINUM -> "💎 Platinum"
            ReferralTier.DIAMOND -> "👑 Diamond"
        }
    }
    
    /**
     * Get tier color
     */
    fun getTierColor(tier: ReferralTier): Long {
        return when (tier) {
            ReferralTier.BRONZE -> 0xFFCD7F32
            ReferralTier.SILVER -> 0xFFC0C0C0
            ReferralTier.GOLD -> 0xFFFFD700
            ReferralTier.PLATINUM -> 0xFFE5E4E2
            ReferralTier.DIAMOND -> 0xFFB9F2FF
        }
    }
}

/**
 * Generate unique referral code for user
 * Format: WRK/EMP + 6 alphanumeric characters
 * Total combinations: 36^6 = 2.1 billion (enough for 10L+ users)
 */
fun generateReferralCode(userId: String, role: String): String {
    val prefix = if (role.uppercase() == "EMPLOYER") "EMP" else "WRK"
    
    // Use first 6 chars of userId (already unique from Firebase)
    // Convert to uppercase alphanumeric
    val uniquePart = userId.take(6).uppercase().filter { it.isLetterOrDigit() }
    
    // If userId doesn't have enough chars, pad with random
    val finalPart = if (uniquePart.length >= 6) {
        uniquePart.take(6)
    } else {
        val random = SecureRandom()
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val padding = (1..(6 - uniquePart.length)).map { chars[random.nextInt(chars.length)] }.joinToString("")
        uniquePart + padding
    }
    
    return "$prefix$finalPart"
}

/**
 * Validate referral code format
 */
fun isValidReferralCode(code: String): Boolean {
    if (code.isBlank()) return false
    val trimmed = code.trim().uppercase()
    
    // Must start with WRK or EMP
    if (!trimmed.startsWith("WRK") && !trimmed.startsWith("EMP")) return false
    
    // Must be exactly 9 characters (3 prefix + 6 unique)
    if (trimmed.length != 9) return false
    
    // Last 6 chars must be alphanumeric
    val uniquePart = trimmed.substring(3)
    return uniquePart.all { it.isLetterOrDigit() }
}

/**
 * Get role from referral code
 */
fun getRoleFromReferralCode(code: String): String? {
    val trimmed = code.trim().uppercase()
    return when {
        trimmed.startsWith("WRK") -> "WORKER"
        trimmed.startsWith("EMP") -> "EMPLOYER"
        else -> null
    }
}

/**
 * Mask phone number for privacy
 */
fun maskPhoneNumber(phone: String): String {
    if (phone.length < 4) return "****"
    return "****${phone.takeLast(4)}"
}

/**
 * Referral validation result
 */
data class ReferralValidationInfo(
    val isValid: Boolean,
    val referrerUserId: String? = null,
    val referrerRole: String? = null,
    val referrerName: String? = null,
    val errorMessage: String? = null,
    val isBlocked: Boolean = false
)

/**
 * Payout Request Status - For UI display of withdrawal status
 */
@Keep
data class PayoutRequestStatus(
    val requestId: String = "",
    val status: WithdrawalStatus = WithdrawalStatus.PENDING,
    val amount: Double = 0.0,
    val requestedAt: Long = System.currentTimeMillis(),
    val estimatedCompletionDate: Long? = null,
    val transactionId: String? = null,
    val rejectionReason: String? = null
) {
    companion object {
        fun fromWithdrawalRequest(request: WithdrawalRequest): PayoutRequestStatus {
            return PayoutRequestStatus(
                requestId = request.id,
                status = request.status,
                amount = request.amount,
                requestedAt = request.createdAt,
                estimatedCompletionDate = when (request.status) {
                    WithdrawalStatus.PENDING -> request.createdAt + (3 * 24 * 60 * 60 * 1000L) // 3 days
                    WithdrawalStatus.PROCESSING -> request.createdAt + (1 * 24 * 60 * 60 * 1000L) // 1 day
                    else -> null
                },
                transactionId = request.transactionId,
                rejectionReason = request.remarks
            )
        }
    }
}

/**
 * Referral Transaction - For transaction history display
 */
@Keep
data class ReferralTransaction(
    val id: String = "",
    val userId: String = "",
    val type: TransactionType = TransactionType.REFERRAL_EARNED,
    val amount: Double = 0.0,
    val description: String = "",
    val referralId: String? = null,
    val withdrawalId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val balanceBefore: Double = 0.0,
    val balanceAfter: Double = 0.0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "type" to type.name,
        "amount" to amount,
        "description" to description,
        "referralId" to referralId,
        "withdrawalId" to withdrawalId,
        "timestamp" to timestamp,
        "balanceBefore" to balanceBefore,
        "balanceAfter" to balanceAfter
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>): ReferralTransaction {
            return ReferralTransaction(
                id = map["id"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                type = try {
                    TransactionType.valueOf(map["type"] as? String ?: "REFERRAL_EARNED")
                } catch (e: Exception) {
                    TransactionType.REFERRAL_EARNED
                },
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                description = map["description"] as? String ?: "",
                referralId = map["referralId"] as? String,
                withdrawalId = map["withdrawalId"] as? String,
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                balanceBefore = (map["balanceBefore"] as? Number)?.toDouble() ?: 0.0,
                balanceAfter = (map["balanceAfter"] as? Number)?.toDouble() ?: 0.0
            )
        }
        
        /**
         * Create transaction from referral completion
         */
        fun fromReferralCompletion(
            userId: String,
            referral: Referral,
            balanceBefore: Double
        ): ReferralTransaction {
            val totalAmount = referral.rewardAmount + referral.bonusAmount
            return ReferralTransaction(
                id = "txn_${System.currentTimeMillis()}_${userId.take(6)}",
                userId = userId,
                type = if (referral.bonusAmount > 0) TransactionType.MILESTONE_BONUS else TransactionType.REFERRAL_EARNED,
                amount = totalAmount,
                description = if (referral.bonusAmount > 0) {
                    "Referral reward + milestone bonus"
                } else {
                    "Referral reward from ${referral.referredUserName}"
                },
                referralId = referral.id,
                timestamp = referral.completedAt ?: System.currentTimeMillis(),
                balanceBefore = balanceBefore,
                balanceAfter = balanceBefore + totalAmount
            )
        }
        
        /**
         * Create transaction from withdrawal
         */
        fun fromWithdrawal(
            userId: String,
            withdrawal: WithdrawalRequest,
            balanceBefore: Double
        ): ReferralTransaction {
            return ReferralTransaction(
                id = "txn_${System.currentTimeMillis()}_${userId.take(6)}",
                userId = userId,
                type = when (withdrawal.status) {
                    WithdrawalStatus.COMPLETED -> TransactionType.WITHDRAWAL
                    WithdrawalStatus.FAILED, WithdrawalStatus.CANCELLED -> TransactionType.WITHDRAWAL_REVERSED
                    else -> TransactionType.WITHDRAWAL
                },
                amount = if (withdrawal.status == WithdrawalStatus.COMPLETED) -withdrawal.amount else withdrawal.amount,
                description = when (withdrawal.status) {
                    WithdrawalStatus.COMPLETED -> "Withdrawal to ${withdrawal.paymentMethod.name}"
                    WithdrawalStatus.FAILED -> "Withdrawal failed - amount refunded"
                    WithdrawalStatus.CANCELLED -> "Withdrawal cancelled - amount refunded"
                    else -> "Withdrawal processing"
                },
                withdrawalId = withdrawal.id,
                timestamp = withdrawal.processedAt ?: withdrawal.createdAt,
                balanceBefore = balanceBefore,
                balanceAfter = if (withdrawal.status == WithdrawalStatus.COMPLETED) {
                    balanceBefore - withdrawal.amount
                } else {
                    balanceBefore
                }
            )
        }
    }
}

/**
 * Transaction type enum
 */
enum class TransactionType {
    REFERRAL_EARNED,        // Earned from successful referral
    MILESTONE_BONUS,        // Bonus from reaching milestone
    SIGNUP_BONUS,           // Bonus for using referral code
    WITHDRAWAL,             // Money withdrawn
    WITHDRAWAL_REVERSED,    // Withdrawal failed/cancelled
    ADJUSTMENT              // Admin adjustment
}
