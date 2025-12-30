package com.example.dutype.services

import android.app.Activity
import com.example.dutype.models.PaymentTransaction
import com.example.dutype.models.SubscriptionPlan
import com.example.dutype.models.SubscriptionPlanType
import com.example.dutype.models.SubscriptionUsage
import com.example.dutype.models.UserSubscription
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.razorpay.Checkout
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Razorpay Payment Service
 * Handles subscription payments and management with robust error handling
 */
@Singleton
class RazorpayService @Inject constructor() {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        // Razorpay API keys - Test mode
        const val RAZORPAY_KEY_ID = "rzp_test_Rx5NGq0pYaKrLc"
        // For production, use: const val RAZORPAY_KEY_ID = "rzp_live_xxxxxxxxxxxxx"
        
        private const val COLLECTION_SUBSCRIPTIONS = "subscriptions"
        private const val COLLECTION_TRANSACTIONS = "payment_transactions"
        private const val COLLECTION_USAGE = "subscription_usage"
        
        // Retry configuration
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }
    
    /**
     * Initialize Razorpay checkout - call this before starting payment
     */
    fun initializeRazorpay(activity: Activity) {
        try {
            Checkout.preload(activity.applicationContext)
            Timber.d("💳 Razorpay SDK preloaded successfully")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to preload Razorpay SDK")
        }
    }
    
    /**
     * Validate plan before payment
     */
    private fun validatePlan(plan: SubscriptionPlan, isYearly: Boolean): Result<Unit> {
        if (plan.id.isBlank()) {
            return Result.failure(Exception("Invalid plan: Plan ID is empty"))
        }
        
        val amount = if (isYearly) plan.priceYearly else plan.priceMonthly
        if (amount <= 0 && plan.type != SubscriptionPlanType.FREE) {
            return Result.failure(Exception("Invalid plan: Price must be greater than 0"))
        }
        
        if (plan.type == SubscriptionPlanType.FREE) {
            return Result.failure(Exception("Free plan doesn't require payment"))
        }
        
        return Result.success(Unit)
    }
    
    /**
     * Start payment for a subscription plan
     */
    fun startPayment(
        activity: Activity,
        plan: SubscriptionPlan,
        isYearly: Boolean,
        userEmail: String,
        userPhone: String,
        userName: String,
        onSuccess: (paymentId: String, orderId: String, signature: String) -> Unit,
        onError: (errorCode: Int, errorMessage: String) -> Unit
    ) {
        // Validate inputs
        val validationResult = validatePlan(plan, isYearly)
        if (validationResult.isFailure) {
            onError(-1, validationResult.exceptionOrNull()?.message ?: "Validation failed")
            return
        }
        
        if (auth.currentUser == null) {
            onError(-2, "User not authenticated. Please login again.")
            return
        }
        
        try {
            val checkout = Checkout()
            checkout.setKeyID(RAZORPAY_KEY_ID)
            
            val amount = if (isYearly) plan.priceYearly else plan.priceMonthly
            val billingCycle = if (isYearly) "Yearly" else "Monthly"
            val description = "${plan.name} Plan - $billingCycle Subscription"
            
            // Set up callbacks via PaymentResultHolder
            com.example.dutype.PaymentResultHolder.setCallbacks(
                onSuccess = { paymentId, orderId, signature ->
                    Timber.d("💳 Payment callback received: $paymentId")
                    onSuccess(paymentId, orderId ?: "", signature ?: "")
                },
                onError = { code, message ->
                    Timber.e("💳 Payment error callback: $code - $message")
                    onError(code, message)
                }
            )
            
            val options = JSONObject().apply {
                put("name", "DutyPe")
                put("description", description)
                put("currency", "INR")
                put("amount", amount) // Amount in paise
                
                // Prefill user details
                put("prefill", JSONObject().apply {
                    if (userEmail.isNotBlank()) put("email", userEmail)
                    if (userPhone.isNotBlank()) put("contact", userPhone)
                    if (userName.isNotBlank()) put("name", userName)
                })
                
                // Theme customization
                put("theme", JSONObject().apply {
                    put("color", "#3B82F6")
                    put("backdrop_color", "#ffffff")
                })
                
                // Payment notes for tracking
                put("notes", JSONObject().apply {
                    put("plan_id", plan.id)
                    put("plan_name", plan.name)
                    put("plan_type", plan.type.name)
                    put("billing_cycle", if (isYearly) "YEARLY" else "MONTHLY")
                    put("user_id", auth.currentUser?.uid ?: "")
                    put("timestamp", System.currentTimeMillis().toString())
                })
                
                // Retry configuration
                put("retry", JSONObject().apply {
                    put("enabled", true)
                    put("max_count", 3)
                })
                
                // Send SMS hash for auto-read OTP
                put("send_sms_hash", true)
            }
            
            Timber.d("💳 Opening Razorpay checkout for ${plan.name} plan - ₹${amount/100}")
            checkout.open(activity, options)
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Error starting Razorpay payment")
            onError(-1, e.message ?: "Payment initialization failed. Please try again.")
        }
    }
    
    /**
     * Handle successful payment with retry logic
     */
    suspend fun handlePaymentSuccess(
        paymentId: String,
        orderId: String?,
        signature: String?,
        plan: SubscriptionPlan,
        isYearly: Boolean
    ): Result<UserSubscription> {
        val userId = auth.currentUser?.uid 
            ?: return Result.failure(Exception("User not authenticated"))
        
        if (paymentId.isBlank()) {
            return Result.failure(Exception("Invalid payment ID"))
        }
        
        return try {
            Timber.d("💳 Processing payment success: $paymentId")
            
            // Check for duplicate transaction
            val existingTransaction = checkDuplicateTransaction(paymentId)
            if (existingTransaction != null) {
                Timber.w("⚠️ Duplicate transaction detected: $paymentId")
                // Return existing subscription if available
                val existingSub = getCurrentSubscription(userId)
                if (existingSub != null) {
                    return Result.success(existingSub)
                }
            }
            
            // Create payment transaction record
            val transaction = PaymentTransaction(
                id = paymentId,
                userId = userId,
                subscriptionId = "", // Will be updated after subscription creation
                razorpayPaymentId = paymentId,
                razorpayOrderId = orderId ?: "",
                razorpaySignature = signature ?: "",
                amount = if (isYearly) plan.priceYearly else plan.priceMonthly,
                currency = "INR",
                status = "SUCCESS",
                paymentMethod = "razorpay",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Save transaction with retry
            saveTransactionWithRetry(transaction)
            
            // Cancel any existing active subscription
            cancelExistingSubscriptions(userId)
            
            // Create new subscription
            val subscription = createSubscription(userId, plan, isYearly, paymentId)
            
            // Update transaction with subscription ID
            firestore.collection(COLLECTION_TRANSACTIONS)
                .document(paymentId)
                .update("subscriptionId", subscription.id)
                .await()
            
            Timber.i("✅ Payment successful: $paymentId, Subscription: ${subscription.id}")
            Result.success(subscription)
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Error handling payment success")
            
            // Log failed transaction for manual review
            logFailedTransaction(paymentId, userId, e.message ?: "Unknown error")
            
            Result.failure(Exception("Payment recorded but subscription activation failed. Please contact support with Payment ID: $paymentId"))
        }
    }
    
    /**
     * Check for duplicate transaction
     */
    private suspend fun checkDuplicateTransaction(paymentId: String): PaymentTransaction? {
        return try {
            val doc = firestore.collection(COLLECTION_TRANSACTIONS)
                .document(paymentId)
                .get()
                .await()
            
            if (doc.exists()) {
                doc.toObject(PaymentTransaction::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking duplicate transaction")
            null
        }
    }
    
    /**
     * Save transaction with retry logic
     */
    private suspend fun saveTransactionWithRetry(transaction: PaymentTransaction) {
        var lastException: Exception? = null
        
        repeat(MAX_RETRIES) { attempt ->
            try {
                firestore.collection(COLLECTION_TRANSACTIONS)
                    .document(transaction.id)
                    .set(transaction)
                    .await()
                Timber.d("✅ Transaction saved on attempt ${attempt + 1}")
                return
            } catch (e: Exception) {
                lastException = e
                Timber.w("⚠️ Transaction save attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < MAX_RETRIES - 1) {
                    kotlinx.coroutines.delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        
        throw lastException ?: Exception("Failed to save transaction after $MAX_RETRIES attempts")
    }
    
    /**
     * Cancel existing active subscriptions before creating new one
     */
    private suspend fun cancelExistingSubscriptions(userId: String) {
        try {
            val activeSubscriptions = firestore.collection(COLLECTION_SUBSCRIPTIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .await()
            
            for (doc in activeSubscriptions.documents) {
                doc.reference.update(mapOf(
                    "status" to "SUPERSEDED",
                    "updatedAt" to System.currentTimeMillis()
                )).await()
                Timber.d("📝 Superseded old subscription: ${doc.id}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error cancelling existing subscriptions")
            // Continue anyway - new subscription should still be created
        }
    }
    
    /**
     * Log failed transaction for manual review
     */
    private suspend fun logFailedTransaction(paymentId: String, userId: String, error: String) {
        try {
            firestore.collection("failed_transactions")
                .document(paymentId)
                .set(mapOf(
                    "paymentId" to paymentId,
                    "userId" to userId,
                    "error" to error,
                    "timestamp" to System.currentTimeMillis(),
                    "resolved" to false
                ))
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to log failed transaction")
        }
    }
    
    /**
     * Create subscription after successful payment
     */
    private suspend fun createSubscription(
        userId: String,
        plan: SubscriptionPlan,
        isYearly: Boolean,
        paymentId: String
    ): UserSubscription {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        // Calculate end date
        if (isYearly) {
            calendar.add(Calendar.YEAR, 1)
        } else {
            calendar.add(Calendar.MONTH, 1)
        }
        val endDate = calendar.timeInMillis
        
        val subscriptionId = firestore.collection(COLLECTION_SUBSCRIPTIONS).document().id
        
        val subscription = UserSubscription(
            id = subscriptionId,
            userId = userId,
            planId = plan.id,
            planType = plan.type.name,
            status = "ACTIVE",
            billingCycle = if (isYearly) "YEARLY" else "MONTHLY",
            startDate = now,
            endDate = endDate,
            nextBillingDate = endDate,
            lastPaymentId = paymentId,
            lastPaymentDate = now,
            lastPaymentAmount = if (isYearly) plan.priceYearly else plan.priceMonthly,
            autoRenew = true,
            createdAt = now,
            updatedAt = now,
            jobPostsPerMonth = plan.jobPostsPerMonth,
            applicationsViewLimit = plan.applicationsViewLimit,
            isFeaturedListings = plan.isFeaturedListings,
            hasAnalytics = plan.hasAnalytics,
            hasVerifiedBadge = plan.hasVerifiedBadge
        )
        
        // Save subscription
        firestore.collection(COLLECTION_SUBSCRIPTIONS)
            .document(subscriptionId)
            .set(subscription)
            .await()
        
        // Update user document with subscription info
        firestore.collection("users")
            .document(userId)
            .update(mapOf(
                "subscriptionId" to subscriptionId,
                "subscriptionPlan" to plan.type.name,
                "subscriptionStatus" to "ACTIVE",
                "subscriptionEndDate" to endDate,
                "hasVerifiedBadge" to plan.hasVerifiedBadge,
                "updatedAt" to now
            ))
            .await()
        
        Timber.d("✅ Subscription created: $subscriptionId for plan ${plan.name}")
        return subscription
    }
    
    /**
     * Get user's current active subscription
     */
    suspend fun getCurrentSubscription(userId: String? = null): UserSubscription? {
        val uid = userId ?: auth.currentUser?.uid ?: return null
        
        return try {
            val snapshot = firestore.collection(COLLECTION_SUBSCRIPTIONS)
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", "ACTIVE")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            if (snapshot.documents.isNotEmpty()) {
                val subscription = snapshot.documents[0].toObject(UserSubscription::class.java)
                
                // Check if subscription has expired
                if (subscription != null && subscription.endDate < System.currentTimeMillis()) {
                    // Mark as expired
                    firestore.collection(COLLECTION_SUBSCRIPTIONS)
                        .document(subscription.id)
                        .update(mapOf(
                            "status" to "EXPIRED",
                            "updatedAt" to System.currentTimeMillis()
                        ))
                        .await()
                    
                    // Update user document
                    firestore.collection("users")
                        .document(uid)
                        .update(mapOf(
                            "subscriptionStatus" to "EXPIRED",
                            "hasVerifiedBadge" to false
                        ))
                        .await()
                    
                    Timber.d("📝 Subscription expired: ${subscription.id}")
                    return null
                }
                
                subscription
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting current subscription")
            null
        }
    }
    
    /**
     * Check if user can post a job based on subscription limits
     */
    suspend fun canPostJob(userId: String? = null): Pair<Boolean, String> {
        val uid = userId ?: auth.currentUser?.uid 
            ?: return Pair(false, "Please login to post jobs")
        
        try {
            val subscription = getCurrentSubscription(uid)
            val limit = subscription?.jobPostsPerMonth ?: 1 // Free plan: 1 post/month
            
            // Unlimited posts
            if (limit == -1) {
                return Pair(true, "Unlimited job posts")
            }
            
            // Get current month usage
            val monthYear = getCurrentMonthYear()
            val usage = getUsage(uid, monthYear)
            val remaining = limit - usage.jobPostsUsed
            
            return if (remaining > 0) {
                Pair(true, "$remaining post${if (remaining > 1) "s" else ""} remaining this month")
            } else {
                Pair(false, "Monthly limit reached. Upgrade your plan for more posts.")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking job post limit")
            return Pair(true, "Unable to verify limit") // Allow posting on error
        }
    }
    
    /**
     * Increment job post usage counter
     */
    suspend fun incrementJobPostUsage(userId: String? = null) {
        val uid = userId ?: auth.currentUser?.uid ?: return
        val monthYear = getCurrentMonthYear()
        
        try {
            val usageRef = firestore.collection(COLLECTION_USAGE)
                .document("${uid}_$monthYear")
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(usageRef)
                val currentUsage = snapshot.getLong("jobPostsUsed") ?: 0
                
                if (snapshot.exists()) {
                    transaction.update(usageRef, mapOf(
                        "jobPostsUsed" to currentUsage + 1,
                        "lastUpdated" to System.currentTimeMillis()
                    ))
                } else {
                    transaction.set(usageRef, mapOf(
                        "userId" to uid,
                        "monthYear" to monthYear,
                        "jobPostsUsed" to 1,
                        "applicationsViewed" to 0,
                        "lastUpdated" to System.currentTimeMillis()
                    ))
                }
            }.await()
            
            Timber.d("📝 Job post usage incremented for $uid")
        } catch (e: Exception) {
            Timber.e(e, "Error incrementing job post usage")
        }
    }
    
    /**
     * Get usage for a specific month
     */
    private suspend fun getUsage(userId: String, monthYear: String): SubscriptionUsage {
        return try {
            val doc = firestore.collection(COLLECTION_USAGE)
                .document("${userId}_$monthYear")
                .get()
                .await()
            
            doc.toObject(SubscriptionUsage::class.java) 
                ?: SubscriptionUsage(userId = userId, monthYear = monthYear)
        } catch (e: Exception) {
            Timber.e(e, "Error getting usage")
            SubscriptionUsage(userId = userId, monthYear = monthYear)
        }
    }
    
    private fun getCurrentMonthYear(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${String.format("%02d", calendar.get(Calendar.MONTH) + 1)}"
    }
    
    /**
     * Cancel subscription (disable auto-renew)
     */
    suspend fun cancelSubscription(subscriptionId: String): Result<Unit> {
        if (subscriptionId.isBlank()) {
            return Result.failure(Exception("Invalid subscription ID"))
        }
        
        return try {
            firestore.collection(COLLECTION_SUBSCRIPTIONS)
                .document(subscriptionId)
                .update(mapOf(
                    "autoRenew" to false,
                    "status" to "CANCELLED",
                    "updatedAt" to System.currentTimeMillis()
                ))
                .await()
            
            // Update user document
            val subscription = firestore.collection(COLLECTION_SUBSCRIPTIONS)
                .document(subscriptionId)
                .get()
                .await()
                .toObject(UserSubscription::class.java)
            
            subscription?.let {
                firestore.collection("users")
                    .document(it.userId)
                    .update(mapOf(
                        "subscriptionStatus" to "CANCELLED"
                    ))
                    .await()
            }
            
            Timber.d("✅ Subscription cancelled: $subscriptionId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error cancelling subscription")
            Result.failure(e)
        }
    }
    
    /**
     * Get subscription plan features for display
     */
    fun getPlanFeatures(planType: SubscriptionPlanType): Map<String, Any> {
        return when (planType) {
            SubscriptionPlanType.FREE -> mapOf(
                "jobPostsPerMonth" to 1,
                "applicationsViewLimit" to 5,
                "featuredListings" to false,
                "analytics" to false,
                "verifiedBadge" to false
            )
            SubscriptionPlanType.BASIC -> mapOf(
                "jobPostsPerMonth" to 5,
                "applicationsViewLimit" to -1,
                "featuredListings" to false,
                "analytics" to false,
                "verifiedBadge" to false
            )
            SubscriptionPlanType.PRO -> mapOf(
                "jobPostsPerMonth" to -1,
                "applicationsViewLimit" to -1,
                "featuredListings" to true,
                "analytics" to true,
                "verifiedBadge" to true
            )
            SubscriptionPlanType.ENTERPRISE -> mapOf(
                "jobPostsPerMonth" to -1,
                "applicationsViewLimit" to -1,
                "featuredListings" to true,
                "analytics" to true,
                "verifiedBadge" to true
            )
        }
    }
}
