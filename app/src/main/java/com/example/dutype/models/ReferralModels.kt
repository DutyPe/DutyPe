package com.example.dutype.models

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.example.dutype.firestore.FirestoreSchema.Referrals
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.firestore.FirestoreSchema.WalletLedger
import com.example.dutype.firestore.FirestoreSchema.Wallets
import com.example.dutype.firestore.FirestoreSchema.Withdrawals
import com.example.dutype.utils.epochMillis

private val REFERRAL_CODE_REGEX = Regex("^[A-Z0-9]{6,10}$")

fun normalizeReferralCode(rawCode: String): String =
    rawCode.filter { it.isLetterOrDigit() }.uppercase().take(10)

fun isValidNormalizedReferralCode(rawCode: String): Boolean =
    REFERRAL_CODE_REGEX.matches(normalizeReferralCode(rawCode))

/** Whole paise → "₹123" / "₹123.50". */
fun formatPaise(paise: Long): String =
    if (paise % 100 == 0L) "₹${paise / 100}" else "₹${"%.2f".format(paise / 100.0)}"

/** `referral_stats/{uid}` — the wallet (server-written; the owner can read it). */
@Keep
@Immutable
data class Wallet(
    val referralCode: String = "",
    val balancePaise: Long = 0L,
    val lifetimeEarnedPaise: Long = 0L,
    val withdrawnPaise: Long = 0L,
    val successfulReferrals: Int = 0,
    val awardedMilestones: List<Int> = emptyList(),
    val blocked: Boolean = false
) {
    val tier: ReferralTier get() = ReferralTier.forCount(successfulReferrals)

    companion object {
        fun from(d: Map<String, Any?>): Wallet = Wallet(
            referralCode = d[Wallets.REFERRAL_CODE] as? String ?: "",
            balancePaise = (d[Wallets.BALANCE_PAISE] as? Number)?.toLong() ?: 0L,
            lifetimeEarnedPaise = (d[Wallets.LIFETIME_EARNED_PAISE] as? Number)?.toLong() ?: 0L,
            withdrawnPaise = (d[Wallets.WITHDRAWN_PAISE] as? Number)?.toLong() ?: 0L,
            successfulReferrals = (d[Wallets.SUCCESSFUL_REFERRALS] as? Number)?.toInt() ?: 0,
            awardedMilestones = (d[Wallets.AWARDED_MILESTONES] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }.orEmpty(),
            blocked = d[Wallets.BLOCKED] as? Boolean ?: false
        )
    }
}

/** `wallet_ledger/{eventId}` — one money movement. */
@Keep
@Immutable
data class LedgerEntry(
    val id: String = "",
    val type: String = "",
    val amountPaise: Long = 0L,
    val refId: String = "",
    val balanceAfterPaise: Long = 0L,
    val createdAt: Long = 0L
) {
    val isCredit: Boolean get() = amountPaise > 0

    val label: String get() = when (type) {
        Values.LedgerType.REFERRAL_REWARD -> "Referral reward"
        Values.LedgerType.SIGNUP_BONUS -> "Joining bonus"
        Values.LedgerType.WELCOME_BONUS -> "Welcome bonus"
        Values.LedgerType.MILESTONE -> "Milestone bonus"
        Values.LedgerType.WITHDRAWAL -> "Withdrawal"
        Values.LedgerType.REFUND -> "Withdrawal refund"
        else -> "Adjustment"
    }

    companion object {
        fun from(id: String, d: Map<String, Any?>): LedgerEntry = LedgerEntry(
            id = id,
            type = d[WalletLedger.TYPE] as? String ?: "",
            amountPaise = (d[WalletLedger.AMOUNT_PAISE] as? Number)?.toLong() ?: 0L,
            refId = d[WalletLedger.REF_ID] as? String ?: "",
            balanceAfterPaise = (d[WalletLedger.BALANCE_AFTER_PAISE] as? Number)?.toLong() ?: 0L,
            createdAt = d[WalletLedger.CREATED_AT].epochMillis()
        )
    }
}

/** `referrals/{refereeUid}` — someone who joined with a code. [rewardPaise] is joined from the ledger. */
@Keep
@Immutable
data class Referral(
    val refereeUid: String = "",
    val referrerUid: String = "",
    val code: String = "",
    val status: ReferralStatus = ReferralStatus.PENDING,
    val createdAt: Long = 0L,
    val completedAt: Long = 0L,
    val rewardPaise: Long = 0L
) {
    companion object {
        fun from(id: String, d: Map<String, Any?>): Referral = Referral(
            refereeUid = id,
            referrerUid = d[Referrals.REFERRER_UID] as? String ?: "",
            code = d[Referrals.CODE] as? String ?: "",
            status = ReferralStatus.from(d[Referrals.STATUS] as? String),
            createdAt = d[Referrals.CREATED_AT].epochMillis(),
            completedAt = d[Referrals.COMPLETED_AT].epochMillis()
        )
    }
}

enum class ReferralStatus {
    PENDING, COMPLETED, REJECTED, EXPIRED;

    companion object {
        fun from(value: String?): ReferralStatus = entries.firstOrNull { it.name == value } ?: PENDING
    }
}

/** `withdrawal_requests/{id}`. */
@Keep
@Immutable
data class WithdrawalRequest(
    val id: String = "",
    val amountPaise: Long = 0L,
    val upiId: String = "",
    val status: WithdrawalStatus = WithdrawalStatus.PENDING,
    val txnRef: String = "",
    val failureReason: String = "",
    val createdAt: Long = 0L,
    val processedAt: Long = 0L
) {
    companion object {
        fun from(id: String, d: Map<String, Any?>): WithdrawalRequest = WithdrawalRequest(
            id = id,
            amountPaise = (d[Withdrawals.AMOUNT_PAISE] as? Number)?.toLong() ?: 0L,
            upiId = d[Withdrawals.UPI_ID] as? String ?: "",
            status = WithdrawalStatus.from(d[Withdrawals.STATUS] as? String),
            txnRef = d[Withdrawals.TXN_REF] as? String ?: "",
            failureReason = d[Withdrawals.FAILURE_REASON] as? String ?: "",
            createdAt = d[Withdrawals.CREATED_AT].epochMillis(),
            processedAt = d[Withdrawals.PROCESSED_AT].epochMillis()
        )
    }
}

enum class WithdrawalStatus {
    PENDING, PROCESSING, COMPLETED, FAILED;

    companion object {
        fun from(value: String?): WithdrawalStatus = entries.firstOrNull { it.name == value } ?: PENDING
    }
}

@Keep
data class LeaderboardEntry(val rank: Int, val name: String, val successfulReferrals: Int)

enum class ReferralTier(val minReferrals: Int) {
    BRONZE(0), SILVER(5), GOLD(10), PLATINUM(25), DIAMOND(50);

    companion object {
        fun forCount(count: Int): ReferralTier = entries.last { count >= it.minReferrals }
    }
}

/** Result of checking a code someone typed (before registering). */
data class ReferralValidationInfo(
    val isValid: Boolean,
    val referrerRole: String? = null,
    val errorMessage: String? = null
) {
    /** Who shared the code, without exposing their identity before sign-up. */
    val referrerName: String get() = if (referrerRole == Values.Role.EMPLOYER) "an employer" else "a DutyPe user"
}
