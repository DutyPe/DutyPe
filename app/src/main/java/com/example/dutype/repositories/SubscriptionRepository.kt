package com.example.dutype.repositories

import androidx.annotation.Keep
import com.example.dutype.firestore.FirestoreSchema.AppConfig
import com.example.dutype.firestore.FirestoreSchema.PaymentQrCodes
import com.example.dutype.firestore.FirestoreSchema.SubscriptionPayments
import com.example.dutype.models.Plan
import com.example.dutype.models.QrCode
import com.example.dutype.utils.epochMillis
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** `subscription_payment_requests/{id}` — an employer's UPI payment proof, verified by an admin. */
@Keep
data class PaymentRequest(
    val id: String = "",
    val employerId: String = "",
    val planId: String = "",
    val amountPaise: Long = 0L,
    val upiIdUsed: String = "",
    val utrNumber: String = "",
    val screenshotUrl: String = "",
    val status: String = STATUS_PENDING,
    val rejectionReason: String = "",
    val createdAt: Long = 0L,
    val verifiedAt: Long = 0L
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_VERIFIED = "VERIFIED"
        const val STATUS_REJECTED = "REJECTED"

        fun from(id: String, d: Map<String, Any?>): PaymentRequest = PaymentRequest(
            id = id,
            employerId = d[SubscriptionPayments.EMPLOYER_ID] as? String ?: "",
            planId = d[SubscriptionPayments.PLAN_ID] as? String ?: "",
            amountPaise = (d[SubscriptionPayments.AMOUNT_PAISE] as? Number)?.toLong() ?: 0L,
            upiIdUsed = d[SubscriptionPayments.UPI_ID_USED] as? String ?: "",
            utrNumber = d[SubscriptionPayments.UTR_NUMBER] as? String ?: "",
            screenshotUrl = d[SubscriptionPayments.SCREENSHOT_URL] as? String ?: "",
            status = d[SubscriptionPayments.STATUS] as? String ?: STATUS_PENDING,
            rejectionReason = d[SubscriptionPayments.REJECTION_REASON] as? String ?: "",
            createdAt = d[SubscriptionPayments.CREATED_AT].epochMillis(),
            verifiedAt = d[SubscriptionPayments.VERIFIED_AT].epochMillis()
        )
    }
}

/**
 * Plans (app_config/subscription_plans), payment QR codes, and the employer's payment requests.
 * Credits are granted only by the server when an admin verifies a payment.
 */
@Singleton
class SubscriptionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    /** Plans from app_config; built-in defaults when the doc is missing or unreadable. */
    suspend fun getPlans(): List<Plan> = runCatching {
        @Suppress("UNCHECKED_CAST")
        val rows = firestore.collection(AppConfig.COLLECTION).document(AppConfig.DOC_SUBSCRIPTION_PLANS)
            .get().await().get("plans") as? List<Map<String, Any?>>
        rows.orEmpty().mapNotNull { row -> (row["id"] as? String)?.let { Plan.fromMap(it, row) } }.filterNot { it.legacy }
    }.getOrElse {
        Timber.w(it, "subscription plans read failed")
        emptyList()
    }.ifEmpty { DEFAULT_PLANS }

    suspend fun getActiveQrCodes(): List<QrCode> = runCatching {
        firestore.collection(PaymentQrCodes.COLLECTION)
            .whereEqualTo(PaymentQrCodes.ACTIVE, true)
            .limit(5)
            .get().await().documents
            .mapNotNull { doc -> doc.data?.let { QrCode.fromMap(doc.id, it) } }
    }.getOrElse { emptyList() }

    suspend fun submitPaymentRequest(
        planId: String,
        amountPaise: Long,
        upiIdUsed: String,
        utrNumber: String,
        screenshotUrl: String
    ): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val ref = firestore.collection(SubscriptionPayments.COLLECTION).document()
        ref.set(
            mapOf(
                SubscriptionPayments.EMPLOYER_ID to uid,
                SubscriptionPayments.PLAN_ID to planId,
                SubscriptionPayments.AMOUNT_PAISE to amountPaise,
                SubscriptionPayments.UPI_ID_USED to upiIdUsed,
                SubscriptionPayments.UTR_NUMBER to utrNumber.trim(),
                SubscriptionPayments.SCREENSHOT_URL to screenshotUrl,
                SubscriptionPayments.STATUS to PaymentRequest.STATUS_PENDING,
                SubscriptionPayments.CREATED_AT to Timestamp.now()
            )
        ).await()
        ref.id
    }

    /** The employer's payment history, newest first, live. */
    fun getPaymentRequests(employerId: String): Flow<List<PaymentRequest>> = callbackFlow {
        if (employerId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val registration = firestore.collection(SubscriptionPayments.COLLECTION)
            .whereEqualTo(SubscriptionPayments.EMPLOYER_ID, employerId)
            .orderBy(SubscriptionPayments.CREATED_AT, Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.w(error, "payment requests listener failed")
                    trySend(emptyList())
                } else {
                    trySend(snapshot?.documents?.mapNotNull { doc -> doc.data?.let { PaymentRequest.from(doc.id, it) } }.orEmpty())
                }
            }
        awaitClose { registration.remove() }
    }

    private companion object {
        /** Same as the server's DEFAULT_PLANS (functions/src/subscriptions.ts): ₹99 without AI, ₹199 / ₹299 with DutyPe AI. */
        val DEFAULT_PLANS = listOf(
            Plan("basic_99", "Basic", 9_900, 3, 5, 30, "3 job posts and 5 urgent posts", "Starter"),
            Plan("pro_ai_199", "Pro + DutyPe AI", 19_900, 6, 15, 30, "Hire by talking: AI posts jobs and picks the best workers", "Most popular", ai = true, aiPerDay = 100),
            Plan("max_ai_299", "Max + DutyPe AI", 29_900, 12, 40, 30, "For busy employers: more posts and more AI every day", "Best value", ai = true, aiPerDay = 300)
        )
    }
}
