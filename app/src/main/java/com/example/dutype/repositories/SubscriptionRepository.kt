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
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Keep
data class RazorpayOrderResult(
    val orderId: String = "",
    val amountPaise: Long = 0L,
    val currency: String = "INR",
    val keyId: String = "rzp_test_Tuw8VzuHNLVm1y",
    val name: String = "DutyPe",
    val description: String = ""
)

@Keep
data class RazorpayVerifyResult(
    val ok: Boolean = false,
    val status: String = "",
    val planId: String = "",
    val orderId: String = "",
    val paymentId: String = ""
)

/** `subscription_payment_requests/{id}` — an employer's UPI payment proof, verified by an admin. */
@Keep
data class PaymentRequest(
    val id: String = "",
    val employerId: String = "",
    val employerName: String = "",
    val employerPhone: String = "",
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
            employerName = d["employerName"] as? String ?: "",
            employerPhone = d["employerPhone"] as? String ?: "",
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
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) {
    /** Plans from app_config; built-in defaults when the doc is missing or unreadable. */
    suspend fun getPlans(): List<Plan> = runCatching {
        @Suppress("UNCHECKED_CAST")
        val rows = firestore.collection(AppConfig.COLLECTION).document(AppConfig.DOC_SUBSCRIPTION_PLANS)
            .get().await().get("plans") as? List<Map<String, Any?>>
        val fetched = rows.orEmpty().mapNotNull { row -> (row["id"] as? String)?.let { Plan.fromMap(it, row) } }.filterNot { it.legacy }
        
        // Ensure all 4 standard plans (₹49, ₹99, ₹199, ₹499) are ALWAYS present in the exact order
        val defaultMap = DEFAULT_PLANS.associateBy { it.id }
        val mergedStandard = DEFAULT_PLANS.map { def ->
            fetched.find { it.id == def.id } ?: def
        }
        val customOthers = fetched.filter { it.id !in defaultMap.keys }
        mergedStandard + customOthers
    }.getOrElse {
        Timber.w(it, "subscription plans read failed")
        DEFAULT_PLANS
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
        screenshotUrl: String,
        employerName: String = "",
        employerPhone: String = ""
    ): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val phone = employerPhone.ifBlank { auth.currentUser?.phoneNumber.orEmpty() }
        val name = employerName.ifBlank { auth.currentUser?.displayName.orEmpty() }
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
                SubscriptionPayments.CREATED_AT to Timestamp.now(),
                "employerName" to name,
                "employerPhone" to phone,
                "payeeName" to "DutyPe",
                "paymentMethod" to "UPI_INTENT"
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

    /**
     * Creates an authorized Razorpay Order via Cloud Functions.
     */
    suspend fun createRazorpayOrder(
        planId: String? = null,
        type: String = "SUBSCRIPTION",
        amountRupees: Int? = null
    ): Result<RazorpayOrderResult> = runCatching {
        val payload = mutableMapOf<String, Any?>("type" to type)
        if (planId != null) payload["planId"] = planId
        if (amountRupees != null) payload["amountRupees"] = amountRupees

        @Suppress("UNCHECKED_CAST")
        val res = functions.getHttpsCallable("createRazorpayOrder")
            .call(payload)
            .await().data as? Map<String, Any?> ?: error("Empty response from server")

        RazorpayOrderResult(
            orderId = res["orderId"] as? String ?: error("Missing orderId"),
            amountPaise = (res["amountPaise"] as? Number)?.toLong() ?: 0L,
            currency = res["currency"] as? String ?: "INR",
            keyId = res["keyId"] as? String ?: "rzp_live_TvEYRD4s7DgLuj",
            name = res["name"] as? String ?: "DutyPe",
            description = res["description"] as? String ?: "DutyPe Payment"
        )
    }

    /**
     * Verifies the cryptographic HMAC-SHA256 signature and activates the subscription instantly.
     */
    suspend fun verifyRazorpayPayment(
        orderId: String,
        paymentId: String,
        signature: String,
        planId: String? = null,
        type: String = "SUBSCRIPTION"
    ): Result<RazorpayVerifyResult> = runCatching {
        val payload = mutableMapOf<String, Any?>(
            "type" to type,
            "orderId" to orderId,
            "paymentId" to paymentId,
            "signature" to signature
        )
        if (planId != null) payload["planId"] = planId

        @Suppress("UNCHECKED_CAST")
        val res = functions.getHttpsCallable("verifyRazorpayPayment")
            .call(payload)
            .await().data as? Map<String, Any?> ?: error("Empty response from server")

        RazorpayVerifyResult(
            ok = res["ok"] as? Boolean ?: false,
            status = res["status"] as? String ?: "",
            planId = res["planId"] as? String ?: (planId ?: ""),
            orderId = res["orderId"] as? String ?: orderId,
            paymentId = res["paymentId"] as? String ?: paymentId
        )
    }

    private companion object {
        /** Same as the server's DEFAULT_PLANS (functions/src/subscriptions.ts): ₹99 without AI, ₹199 / ₹299 with DutyPe AI. */
        val DEFAULT_PLANS = listOf(
            Plan("single_job_49", "Single Job", 4_900, 1, 10, 30, "1 job post & 10 worker contact unlocks • DutyPe AI", "Instant Hire", ai = true, aiPerDay = 10),
            Plan("basic_99", "Starter Basic", 9_900, 3, 25, 30, "3 job posts & 25 worker contact unlocks • DutyPe AI", "Value Pack", ai = true, aiPerDay = 25),
            Plan("launch_pass_199", "Launch Pass", 19_900, 5, 50, 45, "5 verified hires + 50 worker contact unlocks • 45 Days • Liri AI", "Most Popular", ai = true, aiPerDay = 50),
            Plan("business_pro_499", "Business Pro", 49_900, 20, 100, 45, "20 job posts + 100 unlocks • 45 Days • Liri AI Pro", "Best Value", ai = true, aiPerDay = 500)
        )
    }
}
