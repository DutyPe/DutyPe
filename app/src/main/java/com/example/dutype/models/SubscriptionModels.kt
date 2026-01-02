package com.example.dutype.models

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

/**
 * Subscription plan types
 */
enum class SubscriptionPlanType {
    FREE,
    BASIC,
    PRO,
    ENTERPRISE
}

/**
 * Subscription status
 */
enum class SubscriptionStatus {
    ACTIVE,
    EXPIRED,
    CANCELLED,
    PENDING,
    FAILED
}

/**
 * Subscription plan details
 */
@Keep
data class SubscriptionPlan(
    val id: String = "",
    val name: String = "",
    val type: SubscriptionPlanType = SubscriptionPlanType.FREE,
    val priceMonthly: Int = 0, // Price in paise (₹299 = 29900)
    val priceYearly: Int = 0,  // Price in paise
    val displayPriceMonthly: String = "₹0",
    val displayPriceYearly: String = "₹0",
    val jobPostsPerMonth: Int = 1,
    val applicationsViewLimit: Int = 5, // -1 for unlimited
    val features: List<String> = emptyList(),
    val isFeaturedListings: Boolean = false,
    val hasAnalytics: Boolean = false,
    val hasVerifiedBadge: Boolean = false,
    val hasPrioritySupport: Boolean = false,
    val hasDedicatedSupport: Boolean = false,
    val hasBulkHiringTools: Boolean = false,
    val razorpayPlanIdMonthly: String = "", // Razorpay plan ID for monthly
    val razorpayPlanIdYearly: String = "",  // Razorpay plan ID for yearly
    val isPopular: Boolean = false,
    val sortOrder: Int = 0
)

/**
 * User's active subscription
 */
@Keep
data class UserSubscription(
    val id: String = "",
    val userId: String = "",
    val planId: String = "",
    val planType: String = "FREE", // FREE, BASIC, PRO, ENTERPRISE
    val status: String = "ACTIVE", // ACTIVE, EXPIRED, CANCELLED, PENDING, FAILED
    val billingCycle: String = "MONTHLY", // MONTHLY, YEARLY
    val startDate: Long = 0,
    val endDate: Long = 0,
    val nextBillingDate: Long = 0,
    val razorpaySubscriptionId: String = "",
    val razorpayCustomerId: String = "",
    val lastPaymentId: String = "",
    val lastPaymentDate: Long = 0,
    val lastPaymentAmount: Int = 0,
    val autoRenew: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Plan limits (cached from plan for quick access)
    val jobPostsPerMonth: Int = 1,
    val applicationsViewLimit: Int = 5,
    @get:PropertyName("isFeaturedListings")
    @set:PropertyName("isFeaturedListings")
    var isFeaturedListings: Boolean = false,
    val hasAnalytics: Boolean = false,
    val hasVerifiedBadge: Boolean = false
)

/**
 * Payment transaction record
 */
@Keep
data class PaymentTransaction(
    val id: String = "",
    val userId: String = "",
    val subscriptionId: String = "",
    val razorpayPaymentId: String = "",
    val razorpayOrderId: String = "",
    val razorpaySignature: String = "",
    val amount: Int = 0, // Amount in paise
    val currency: String = "INR",
    val status: String = "PENDING", // PENDING, SUCCESS, FAILED, REFUNDED
    val paymentMethod: String = "", // upi, card, netbanking, wallet
    val errorCode: String = "",
    val errorDescription: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Usage tracking for subscription limits
 */
@Keep
data class SubscriptionUsage(
    val userId: String = "",
    val monthYear: String = "", // Format: "2025-12"
    val jobPostsUsed: Int = 0,
    val applicationsViewed: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Predefined subscription plans
 */
object SubscriptionPlans {
    val FREE = SubscriptionPlan(
        id = "free",
        name = "Free",
        type = SubscriptionPlanType.FREE,
        priceMonthly = 0,
        priceYearly = 0,
        displayPriceMonthly = "₹0",
        displayPriceYearly = "₹0",
        jobPostsPerMonth = 1,
        applicationsViewLimit = 5,
        features = listOf(
            "1 job post per month",
            "View up to 5 applications",
            "Basic support"
        ),
        sortOrder = 0
    )
    
    val BASIC = SubscriptionPlan(
        id = "basic",
        name = "Basic",
        type = SubscriptionPlanType.BASIC,
        priceMonthly = 29900, // ₹299
        priceYearly = 299900, // ₹2999 (save ₹589)
        displayPriceMonthly = "₹299",
        displayPriceYearly = "₹2,999",
        jobPostsPerMonth = 5,
        applicationsViewLimit = -1, // Unlimited
        features = listOf(
            "5 job posts per month",
            "Unlimited application views",
            "Priority support",
            "Email notifications"
        ),
        hasPrioritySupport = true,
        sortOrder = 1
    )
    
    val PRO = SubscriptionPlan(
        id = "pro",
        name = "Pro",
        type = SubscriptionPlanType.PRO,
        priceMonthly = 79900, // ₹799
        priceYearly = 799900, // ₹7999 (save ₹1589)
        displayPriceMonthly = "₹799",
        displayPriceYearly = "₹7,999",
        jobPostsPerMonth = -1, // Unlimited
        applicationsViewLimit = -1,
        features = listOf(
            "Unlimited job posts",
            "Unlimited application views",
            "Featured job listings",
            "Hiring analytics dashboard",
            "Verified employer badge",
            "Priority support"
        ),
        isFeaturedListings = true,
        hasAnalytics = true,
        hasVerifiedBadge = true,
        hasPrioritySupport = true,
        isPopular = true,
        sortOrder = 2
    )
    
    val ENTERPRISE = SubscriptionPlan(
        id = "enterprise",
        name = "Enterprise",
        type = SubscriptionPlanType.ENTERPRISE,
        priceMonthly = 199900, // ₹1999
        priceYearly = 1999900, // ₹19999 (save ₹3989)
        displayPriceMonthly = "₹1,999",
        displayPriceYearly = "₹19,999",
        jobPostsPerMonth = -1,
        applicationsViewLimit = -1,
        features = listOf(
            "Everything in Pro",
            "Dedicated account manager",
            "Bulk hiring tools",
            "Custom branding",
            "API access",
            "Advanced analytics",
            "24/7 phone support"
        ),
        isFeaturedListings = true,
        hasAnalytics = true,
        hasVerifiedBadge = true,
        hasPrioritySupport = true,
        hasDedicatedSupport = true,
        hasBulkHiringTools = true,
        sortOrder = 3
    )
    
    val ALL_PLANS = listOf(FREE, BASIC, PRO, ENTERPRISE)
    
    fun getPlanById(id: String): SubscriptionPlan {
        return ALL_PLANS.find { it.id == id } ?: FREE
    }
    
    fun getPlanByType(type: SubscriptionPlanType): SubscriptionPlan {
        return ALL_PLANS.find { it.type == type } ?: FREE
    }
}
