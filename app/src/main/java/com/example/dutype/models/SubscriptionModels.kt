package com.example.dutype.models

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.example.dutype.firestore.FirestoreSchema.EmployerProfiles.Subscription
import com.example.dutype.firestore.FirestoreSchema.PaymentQrCodes
import com.example.dutype.utils.epochMillis

/** `employer_profiles/{uid}.subscription` (server-written). */
@Keep
@Immutable
data class EmployerSubscription(
    val planId: String = "",
    val status: String = STATUS_NONE,
    val startAt: Long = 0L,
    val expiresAt: Long = 0L,
    val normalCredits: Int = 0,
    val instantCredits: Int = 0,
    /** The plan includes DutyPe AI. */
    val ai: Boolean = false,
    val aiPerDay: Int = 0
) {
    val isActive: Boolean get() = status == STATUS_ACTIVE && (expiresAt == 0L || expiresAt > System.currentTimeMillis())

    /** DutyPe AI included right now (an active AI plan, or the launch campaign). */
    val hasAi: Boolean get() = isActive && (ai || planId == PLAN_UNLIMITED_CAMPAIGN)

    /** Referral / launch campaign: unlimited job posts while active. */
    val isUnlimitedCampaign: Boolean get() = isActive && planId == PLAN_UNLIMITED_CAMPAIGN

    companion object {
        const val STATUS_NONE = "NONE"
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_EXPIRED = "EXPIRED"
        const val PLAN_UNLIMITED_CAMPAIGN = "UNLIMITED_CAMPAIGN"

        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>?): EmployerSubscription {
            if (map == null) return EmployerSubscription()
            val credits = map[Subscription.CREDITS] as? Map<String, Any?>
            return EmployerSubscription(
                planId = map[Subscription.PLAN_ID] as? String ?: "",
                status = map[Subscription.STATUS] as? String ?: STATUS_NONE,
                startAt = map[Subscription.START_AT].epochMillis(),
                expiresAt = map[Subscription.EXPIRES_AT].epochMillis(),
                normalCredits = (credits?.get(Subscription.CREDITS_NORMAL) as? Number)?.toInt() ?: 0,
                instantCredits = (credits?.get(Subscription.CREDITS_INSTANT) as? Number)?.toInt() ?: 0,
                ai = map[Subscription.AI] == true,
                aiPerDay = (map[Subscription.AI_PER_DAY] as? Number)?.toInt() ?: 0
            )
        }
    }
}

/** `active_qr_codes/{id}` — UPI QR shown on the subscription screen. */
@Keep
@Immutable
data class QrCode(
    val id: String = "",
    val imageUrl: String = "",
    val label: String = "",
    val active: Boolean = false,
    val createdAt: Long = 0L
) {
    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): QrCode = QrCode(
            id = id,
            imageUrl = map[PaymentQrCodes.IMAGE_URL] as? String ?: "",
            label = map[PaymentQrCodes.LABEL] as? String ?: "",
            active = map[PaymentQrCodes.ACTIVE] as? Boolean ?: false,
            createdAt = map[PaymentQrCodes.CREATED_AT].epochMillis()
        )
    }
}

/** A subscription plan from `app_config/subscription_plans` (admin-editable). */
@Keep
@Immutable
data class Plan(
    val id: String = "",
    val name: String = "",
    /** Price in whole paise. */
    val pricePaise: Long = 0L,
    val jobs: Int = 0,
    val instantUnlocks: Int = 0,
    val validityDays: Int = 30,
    val description: String = "",
    val tag: String = "",
    /** Includes DutyPe AI. */
    val ai: Boolean = false,
    /** DutyPe AI actions per day. */
    val aiPerDay: Int = 0,
    /** Older plan, not offered any more. */
    val legacy: Boolean = false
) {
    val priceRupees: Long get() = pricePaise / 100

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Plan = Plan(
            id = id,
            name = map["name"] as? String ?: "",
            pricePaise = (map["pricePaise"] as? Number)?.toLong() ?: 0L,
            jobs = (map["jobs"] as? Number)?.toInt() ?: 0,
            instantUnlocks = (map["instantUnlocks"] as? Number)?.toInt() ?: 0,
            validityDays = (map["validityDays"] as? Number)?.toInt() ?: 30,
            description = map["description"] as? String ?: "",
            tag = map["tag"] as? String ?: "",
            ai = map["ai"] == true,
            aiPerDay = (map["aiPerDay"] as? Number)?.toInt() ?: 0,
            legacy = map["legacy"] == true
        )
    }
}
