package com.example.dutype.repositories

import com.example.dutype.models.Plan
import com.example.dutype.models.QrCode
import com.example.dutype.models.PaymentRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class SubscriptionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Real-time listener to get subscription plans from Firestore configuration.
     * Uses fallback plans if document doesn't exist or is empty.
     */
    fun getPlans(): Flow<List<Plan>> = callbackFlow {
        val listener = firestore.collection("config").document("subscription_plans")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(getFallbackPlans())
                    return@addSnapshotListener
                }
                
                val plansList = snapshot?.get("plans") as? List<Map<String, Any?>>
                if (plansList != null && plansList.isNotEmpty()) {
                    val parsedPlans = plansList.mapNotNull { data ->
                        val id = (data["id"] as? String) ?: return@mapNotNull null
                        Plan.fromMap(id, data)
                    }
                    trySend(parsedPlans)
                } else {
                    trySend(getFallbackPlans())
                }
            }
        awaitClose { listener.remove() }
    }
    
    private fun getFallbackPlans(): List<Plan> = listOf(
        Plan("single_49", "Single Job", 49.0, 1, 0, "1 Credit to post a new job or extend an existing one", "Perfect for Quick Trial", ""),
        Plan("starter_99", "Starter Plan", 99.0, 3, 5, "Ideal for micro-employers with few active needs", "Most Popular", ""),
        Plan("growth_149", "Growth Plan", 149.0, 6, 15, "Best value! 5 + 1 Free job posts with extensions", "🔥 Best Value", "+ 1 Free Job Post"),
        Plan("premium_299", "Premium Plan", 299.0, 12, 40, "Enterprise plan with 10 + 2 Free posts", "Pro Recruiter", "+ 2 Free Job Posts")
    )

    /**
     * Real-time listener to get all active QR codes from Firestore.
     */
    fun getActiveQrCodes(): Flow<List<QrCode>> = callbackFlow {
        val listener = firestore.collection("active_qr_codes")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val qrs = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    QrCode.fromMap(doc.id, data)
                }.orEmpty()
                trySend(qrs)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Submit a payment request reference to Firestore for verification.
     */
    suspend fun submitPaymentRequest(request: PaymentRequest): Result<Unit> {
        return try {
            val docRef = firestore.collection("subscription_payment_requests").document()
            val requestWithId = request.copy(id = docRef.id)
            val data = mapOf(
                "id" to requestWithId.id,
                "employerId" to requestWithId.employerId,
                "employerPhone" to requestWithId.employerPhone,
                "planId" to requestWithId.planId,
                "amount" to requestWithId.amount,
                "upiIdUsed" to requestWithId.upiIdUsed,
                "utrNumber" to requestWithId.utrNumber,
                "screenshotUrl" to requestWithId.screenshotUrl,
                "status" to requestWithId.status,
                "requestTimestamp" to requestWithId.requestTimestamp,
                "verifiedTimestamp" to requestWithId.verifiedTimestamp,
                "expiryTimestamp" to requestWithId.expiryTimestamp,
                "rejectionReason" to requestWithId.rejectionReason
            )
            docRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time listener to get payment submissions history for an employer.
     */
    fun getPaymentRequests(employerId: String): Flow<List<PaymentRequest>> = callbackFlow {
        val listener = firestore.collection("subscription_payment_requests")
            .whereEqualTo("employerId", employerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    PaymentRequest(
                        id = doc.id,
                        employerId = (data["employerId"] as? String) ?: "",
                        employerPhone = (data["employerPhone"] as? String) ?: "",
                        planId = (data["planId"] as? String) ?: "",
                        amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                        upiIdUsed = (data["upiIdUsed"] as? String) ?: "",
                        utrNumber = (data["utrNumber"] as? String) ?: "",
                        screenshotUrl = (data["screenshotUrl"] as? String) ?: "",
                        status = (data["status"] as? String) ?: "",
                        requestTimestamp = (data["requestTimestamp"] as? Number)?.toLong() ?: 0L,
                        verifiedTimestamp = (data["verifiedTimestamp"] as? Number)?.toLong(),
                        expiryTimestamp = (data["expiryTimestamp"] as? Number)?.toLong(),
                        rejectionReason = data["rejectionReason"] as? String
                    )
                }.orEmpty().sortedByDescending { it.requestTimestamp }
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }
}
