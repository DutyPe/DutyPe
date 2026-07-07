package com.example.dutype.models

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class EmployerSubscription(
    val status: String = "NONE", // NONE, TRIAL, ACTIVE
    val planId: String = "",
    val startDate: Long = 0L,
    val expiryDate: Long = 0L,
    val normalCredits: Int = 0,
    val instantCredits: Int = 0,
    val trialJobsUsed: Int = 0,
    val trialInstantJobsUsed: Int = 0
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>?): EmployerSubscription {
            if (map == null) return EmployerSubscription()
            val creditsMap = map["credits"] as? Map<String, Any?>
            return EmployerSubscription(
                status = (map["status"] as? String) ?: "NONE",
                planId = (map["planId"] as? String) ?: "",
                startDate = (map["startDate"] as? Number)?.toLong() ?: 0L,
                expiryDate = (map["expiryDate"] as? Number)?.toLong() ?: 0L,
                normalCredits = (creditsMap?.get("normal") as? Number)?.toInt() ?: 0,
                instantCredits = (creditsMap?.get("instant") as? Number)?.toInt() ?: 0,
                trialJobsUsed = (map["trialJobsUsed"] as? Number)?.toInt() ?: 0,
                trialInstantJobsUsed = (map["trialInstantJobsUsed"] as? Number)?.toInt() ?: 0
            )
        }
    }
}

@Keep
@Immutable
data class QrCode(
    val id: String = "",
    val imageUrl: String = "",
    val label: String = "",
    val isActive: Boolean = false,
    val createdAt: Long = 0L
) {
    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): QrCode {
            return QrCode(
                id = id,
                imageUrl = (map["imageUrl"] as? String) ?: "",
                label = (map["label"] as? String) ?: "",
                isActive = (map["isActive"] as? Boolean) ?: false,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}

@Keep
@Immutable
data class PaymentRequest(
    val id: String = "",
    val employerId: String = "",
    val employerPhone: String = "",
    val planId: String = "",
    val amount: Double = 0.0,
    val upiIdUsed: String = "dutypeindia@ybl",
    val utrNumber: String = "",
    val screenshotUrl: String = "",
    val status: String = "PENDING", // PENDING, VERIFIED, REJECTED
    val requestTimestamp: Long = System.currentTimeMillis(),
    val verifiedTimestamp: Long? = null,
    val expiryTimestamp: Long? = null,
    val rejectionReason: String? = null
)
