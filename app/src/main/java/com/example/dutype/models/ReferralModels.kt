package com.example.dutype.models

import androidx.annotation.Keep

/**
 * Referral - MINIMAL MODEL (10 fields)
 * Based on Dropbox/PayPal/Airbnb patterns
 */
@Keep
data class Referral(
    val id: String = "",
    val referrerUserId: String = "",
    val referredUserId: String = "",
    val referralCode: String = "",
    val status: ReferralStatus = ReferralStatus.PENDING,
    val rewardAmount: Double = 25.0,
    val bonusAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val deviceFingerprint: String? = null,
    // Denormalized for display
    val referredUserName: String = "",
    val referredUserRole: String = ""
) {
    fun getExpiresAt(): Long = createdAt + (30 * 24 * 60 * 60 * 1000L)
    fun isExpired(): Boolean = System.currentTimeMillis() > getExpiresAt()
    
    companion object {
        fun fromMap(data: Map<String, Any>): Referral {
            return Referral(
                id = data["id"] as? String ?: "",
                referrerUserId = data["referrerUserId"] as? String ?: "",
                referredUserId = data["referredUserId"] as? String ?: "",
                referralCode = data["referralCode"] as? String ?: "",
                status = try {
                    ReferralStatus.valueOf(data["status"] as? String ?: "PENDING")
                } catch (e: Exception) {
                    ReferralStatus.PENDING
                },
                rewardAmount = (data["rewardAmount"] as? Number)?.toDouble() ?: 25.0,
                bonusAmount = (data["bonusAmount"] as? Number)?.toDouble() ?: 0.0,
                createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                completedAt = data["completedAt"] as? Long,
                deviceFingerprint = data["deviceFingerprint"] as? String,
                referredUserName = data["referredUserName"] as? String ?: "",
                referredUserRole = data["referredUserRole"] as? String ?: ""
            )
        }
    }
}

enum class ReferralStatus {
    PENDING,
    COMPLETED,
    EXPIRED,
    CANCELLED,
    REJECTED
}

/**
 * ReferralCodeLookup - O(1) code validation (code as document ID)
 */
@Keep
data class ReferralCodeLookup(
    val code: String = "",
    val userId: String = "",
    val userRole: String = "",
    val userName: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromMap(data: Map<String, Any>): ReferralCodeLookup {
            return ReferralCodeLookup(
                code = data["code"] as? String ?: "",
                userId = data["userId"] as? String ?: "",
                userRole = data["userRole"] as? String ?: "",
                userName = data["userName"] as? String ?: "",
                isActive = data["isActive"] as? Boolean ?: true,
                createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis()
            )
        }
    }
}

/**
 * WithdrawalRequest - Payout requests
 */
@Keep
data class WithdrawalRequest(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val status: WithdrawalStatus = WithdrawalStatus.PENDING,
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val upiId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null,
    val transactionId: String? = null
) {
    companion object {
        fun fromMap(data: Map<String, Any>): WithdrawalRequest {
            return WithdrawalRequest(
                id = data["id"] as? String ?: "",
                userId = data["userId"] as? String ?: "",
                amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                status = try {
                    WithdrawalStatus.valueOf(data["status"] as? String ?: "PENDING")
                } catch (e: Exception) {
                    WithdrawalStatus.PENDING
                },
                paymentMethod = try {
                    PaymentMethod.valueOf(data["paymentMethod"] as? String ?: "UPI")
                } catch (e: Exception) {
                    PaymentMethod.UPI
                },
                upiId = data["upiId"] as? String,
                createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                processedAt = data["processedAt"] as? Long,
                transactionId = data["transactionId"] as? String
            )
        }
    }
}

enum class WithdrawalStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class PaymentMethod {
    UPI,
    BANK_TRANSFER,
    PAYTM,
    PHONEPE,
    GPAY
}

object ReferralRewards {
    const val REWARD_PER_REFERRAL = 25.0
    const val MIN_WITHDRAWAL_AMOUNT = 50.0
    
    val MILESTONES = mapOf(
        5 to 50.0,
        10 to 100.0,
        15 to 150.0
    )
    
    fun canWithdraw(successfulReferrals: Int): Boolean = successfulReferrals >= 5
    
    fun getTierDisplayName(tier: ReferralTier): String = when (tier) {
        ReferralTier.BRONZE -> "Bronze"
        ReferralTier.SILVER -> "Silver"
        ReferralTier.GOLD -> "Gold"
        ReferralTier.PLATINUM -> "Platinum"
        ReferralTier.DIAMOND -> "Diamond"
    }
    
    fun getMilestoneBonus(milestone: Int): Double = MILESTONES[milestone] ?: 0.0
    
    fun getEmployerFreePostings(milestone: Int): Int = when (milestone) {
        5 -> 1
        10 -> 2
        15 -> 3
        else -> 0
    }
}

/**
 * ReferralStats - Embedded in User document (OPTIMIZED)
 * Moved from separate collection to users.referralStats field
 */
@Keep
data class ReferralStats(
    val userId: String = "",
    val userRole: String = "",
    val referralCode: String = "",
    val totalReferrals: Int = 0,
    val successfulReferrals: Int = 0,
    val pendingReferrals: Int = 0,
    val expiredReferrals: Int = 0,
    val rejectedReferrals: Int = 0,
    val totalEarnings: Double = 0.0,
    val pendingEarnings: Double = 0.0,
    val withdrawnAmount: Double = 0.0,
    val availableBalance: Double = 0.0,
    val canWithdraw: Boolean = false,
    val nextMilestone: Int = 5,
    val currentTier: ReferralTier = ReferralTier.BRONZE,
    val freeJobPostings: Int = 0,
    val freeJobPostingsExpiry: Long? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val referredByCode: String? = null,
    val referredByUserId: String? = null,
    val lastWithdrawalAt: Long? = null,
    val totalWithdrawals: Int = 0,
    val isBlocked: Boolean = false,
    val blockReason: String? = null
)

/**
 * Referral tier for gamification
 */
enum class ReferralTier {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM,
    DIAMOND
}

// Success stories for motivation
@Keep
data class ReferralSuccessStory(
    val userId: String = "",
    val userName: String = "",
    val city: String = "",
    val totalEarnings: Double = 0.0,
    val successfulReferrals: Int = 0,
    val month: String = ""
) {
    companion object {
        fun fromMap(data: Map<String, Any?>): ReferralSuccessStory {
            return ReferralSuccessStory(
                userId = data["userId"] as? String ?: "",
                userName = data["userName"] as? String ?: "",
                city = data["city"] as? String ?: "",
                totalEarnings = (data["totalEarnings"] as? Number)?.toDouble() ?: 0.0,
                successfulReferrals = (data["successfulReferrals"] as? Number)?.toInt() ?: 0,
                month = data["month"] as? String ?: ""
            )
        }
    }
}

// Validation result
data class ReferralValidationInfo(
    val isValid: Boolean,
    val referrerUserId: String? = null,
    val referrerName: String? = null,
    val errorMessage: String? = null
)

/**
 * Referral Analytics for tracking performance
 */
@Keep
data class ReferralAnalytics(
    val conversionRate: Float = 0f,
    val totalClicks: Int = 0,
    val rankOverall: Int = 0,
    val percentile: Float = 0f,
    val projectedMonthlyEarnings: Int = 0
) {
    companion object {
        fun fromMap(data: Map<String, Any?>): ReferralAnalytics {
            return ReferralAnalytics(
                conversionRate = (data["conversionRate"] as? Number)?.toFloat() ?: 0f,
                totalClicks = (data["totalClicks"] as? Number)?.toInt() ?: 0,
                rankOverall = (data["rankOverall"] as? Number)?.toInt() ?: 0,
                percentile = (data["percentile"] as? Number)?.toFloat() ?: 0f,
                projectedMonthlyEarnings = (data["projectedMonthlyEarnings"] as? Number)?.toInt() ?: 0
            )
        }
    }
}

/**
 * Share channel enum
 */
enum class ShareChannel {
    WHATSAPP,
    SMS,
    EMAIL,
    FACEBOOK,
    TWITTER,
    INSTAGRAM,
    LINKEDIN,
    COPY_LINK,
    OTHER
}

/**
 * Share messages for different channels
 */
object ShareMessages {
    fun getShareMessage(
        userRole: String,
        channel: ShareChannel,
        referralCode: String,
        deepLink: String,
        userName: String
    ): String {
        val roleText = if (userRole == "WORKER") "worker" else "employer"
        return when (channel) {
            ShareChannel.WHATSAPP -> "🎉 Join DutyPe as a $roleText! Use my referral code: $referralCode\n\n$deepLink\n\n- $userName"
            ShareChannel.SMS -> "Join DutyPe with code $referralCode: $deepLink"
            ShareChannel.EMAIL -> "Hi! I'm using DutyPe and thought you might be interested. Use my referral code $referralCode to get started: $deepLink"
            ShareChannel.FACEBOOK, ShareChannel.TWITTER, ShareChannel.INSTAGRAM, ShareChannel.LINKEDIN -> "Join DutyPe with my referral code: $referralCode\n$deepLink"
            ShareChannel.COPY_LINK -> deepLink
            ShareChannel.OTHER -> "Join DutyPe with my referral code: $referralCode\n$deepLink"
            else -> "Join DutyPe with my referral code: $referralCode\n$deepLink"
        }
    }
    
    fun getSharePrompt(context: String): String {
        return when (context) {
            "profile" -> "Share your referral code with friends!"
            "earnings" -> "Earn more by referring friends!"
            "success" -> "Great! Now share with more friends!"
            else -> "Share DutyPe with your network!"
        }
    }
}
