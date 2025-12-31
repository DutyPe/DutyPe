package com.example.dutype.utils

import com.example.dutype.employer.models.enums.JobCategory
import com.example.dutype.employer.models.enums.PayType

/**
 * Job Validation Utilities
 * 
 * Contains anti-fraud validation logic:
 * 1. No-Data-Entry Firewall - Blocks WFH/Online scam keywords
 * 2. Pay Rate Guardrails - Min/max salary validation per category
 */
object JobValidationUtils {

    // ==========================================
    // NO-DATA-ENTRY FIREWALL (Anti-Scam)
    // ==========================================
    
    /**
     * Blocked keywords that indicate potential scam jobs
     * These are commonly used in WFH/Online fraud schemes
     * 
     * P0 FIX #3: Added 50+ new scam keywords for comprehensive coverage
     * Last Updated: December 31, 2025
     */
    private val BLOCKED_KEYWORDS = listOf(
        // ==========================================
        // WORK FROM HOME SCAMS
        // ==========================================
        "work from home", "wfh", "work at home", "home based",
        "online job", "online work", "online earning", "online income",
        "typing job", "data entry", "form filling", "copy paste",
        "ad posting", "ad clicking", "click ads", "survey job",
        "captcha", "captcha typing", "captcha entry",
        "home job", "ghar baithe", "ghar se kaam", "घर बैठे",
        
        // ==========================================
        // INCOME SCAMS (NEW)
        // ==========================================
        "part time income", "extra income", "side income", "second income",
        "passive earning", "daily income", "weekly income", "monthly income",
        "guaranteed earning", "fixed income", "assured income",
        "income guarantee", "earning guarantee", "money guarantee",
        
        // ==========================================
        // MOBILE/SMS SCAMS (NEW)
        // ==========================================
        "mobile job", "phone job", "sms job", "missed call job",
        "recharge job", "mobile recharge", "paytm job", "phonepe job",
        "gpay job", "upi job", "call job", "calling job from home",
        
        // ==========================================
        // E-COMMERCE SCAMS (NEW)
        // ==========================================
        "amazon job", "flipkart job", "meesho job", "reselling job",
        "product listing", "review job", "rating job", "5 star review",
        "product review", "app review", "play store review",
        "online selling", "reseller", "dropshipping",
        
        // ==========================================
        // AFFILIATE/MLM SCAMS (NEW)
        // ==========================================
        "affiliate", "commission based", "referral based", "chain marketing",
        "pyramid", "downline", "upline", "multi level", "mlm",
        "network marketing", "direct selling scheme",
        "referral income", "passive income", "easy money",
        "earn lakhs", "earn crores", "guaranteed income",
        "daily earning", "weekly payout", "instant money",
        
        // ==========================================
        // QUALIFICATION BAIT SCAMS (NEW)
        // ==========================================
        "no qualification", "10th pass", "12th pass", "any qualification",
        "fresher welcome high salary", "no experience high salary",
        "no degree required", "no education required",
        "8th pass", "5th pass", "illiterate welcome",
        
        // ==========================================
        // TARGET DEMOGRAPHIC SCAMS (NEW)
        // ==========================================
        "housewife job", "student job", "retired person job",
        "senior citizen", "handicapped job", "disabled job",
        "ladies only work from home", "women only online",
        "college student earning", "pocket money job",
        
        // ==========================================
        // GOVERNMENT SCHEME SCAMS (NEW)
        // ==========================================
        "government scheme", "pm scheme", "yojana", "sarkari",
        "government approved", "rbi approved", "sebi approved",
        "govt job without exam", "sarkari naukri ghar baithe",
        
        // ==========================================
        // FINANCIAL SCAMS (NEW)
        // ==========================================
        "loan job", "insurance job", "credit card job", "emi job",
        "finance job work from home", "bank job no interview",
        "dsp job", "lic agent", "mutual fund agent home",
        
        // ==========================================
        // APP-BASED SCAMS (NEW)
        // ==========================================
        "app download", "install app", "play store review",
        "app testing job", "beta testing job", "app install",
        "download and earn", "install and earn", "watch and earn",
        "video watching job", "youtube watching job",
        
        // ==========================================
        // CRYPTO/TRADING SCAMS
        // ==========================================
        "crypto", "bitcoin", "trading", "forex", "binary options",
        "nft job", "web3 job", "defi", "staking", "mining job",
        "cryptocurrency", "ethereum", "dogecoin",
        
        // ==========================================
        // SUSPICIOUS PAYMENT TERMS
        // ==========================================
        "registration fee", "joining fee", "security deposit required",
        "pay first", "advance payment", "upfront payment",
        "send money", "transfer money", "pay to join",
        "refundable deposit", "training fee", "kit fee",
        
        // ==========================================
        // SUSPICIOUS CONTACT METHODS
        // ==========================================
        "telegram only", "whatsapp only", "dm for details",
        "contact on telegram", "message on whatsapp",
        "join telegram", "join whatsapp group", "secret group",
        
        // ==========================================
        // RED FLAG PHRASES (NEW)
        // ==========================================
        "limited seats", "hurry up", "last date today",
        "interview tomorrow", "joining today", "start immediately earn",
        "no rejection", "100% selection", "direct joining",
        "spot offer", "walk in today", "urgent hiring today only",
        
        // ==========================================
        // TOO GOOD TO BE TRUE
        // ==========================================
        "no experience needed high salary", "earn without working",
        "unlimited earning", "no interview", "instant hiring",
        "100% genuine", "100% real", "not fake", "trust me",
        "lakhs per month", "crores per year", "become rich",
        "millionaire", "crorepati", "lakhpati"
    )
    
    /**
     * Blocked patterns (regex) for more sophisticated detection
     * P0 FIX #3: Added 15+ new patterns for comprehensive scam detection
     */
    private val BLOCKED_PATTERNS = listOf(
        // Earning patterns
        Regex("earn\\s*₹?\\s*\\d+k?\\s*(per|/)?\\s*(day|hour)", RegexOption.IGNORE_CASE),
        Regex("₹\\s*\\d{4,}\\s*(daily|hourly|per\\s*day)", RegexOption.IGNORE_CASE),
        Regex("\\d+k?\\s*(per|/)\\s*(day|hour|week)\\s*(income|earning)", RegexOption.IGNORE_CASE),
        Regex("(income|earning|salary)\\s*₹?\\s*\\d{5,}", RegexOption.IGNORE_CASE),
        
        // Work from home patterns
        Regex("(work|job)\\s*(from|at)\\s*home", RegexOption.IGNORE_CASE),
        Regex("ghar\\s*(se|baithe)\\s*(kaam|job|earning)", RegexOption.IGNORE_CASE),
        Regex("home\\s*based\\s*(job|work|income)", RegexOption.IGNORE_CASE),
        
        // Online job patterns
        Regex("online\\s*(job|work|earning|income)", RegexOption.IGNORE_CASE),
        Regex("(typing|data\\s*entry)\\s*job", RegexOption.IGNORE_CASE),
        Regex("(copy|paste)\\s*(job|work)", RegexOption.IGNORE_CASE),
        
        // Fee patterns
        Regex("(registration|joining|security)\\s*(fee|deposit)", RegexOption.IGNORE_CASE),
        Regex("pay\\s*₹?\\s*\\d+\\s*(to|for)\\s*(join|register|start)", RegexOption.IGNORE_CASE),
        
        // Contact patterns
        Regex("whatsapp\\s*(only|number|no\\.?|:)", RegexOption.IGNORE_CASE),
        Regex("telegram\\s*(only|id|channel|:)", RegexOption.IGNORE_CASE),
        Regex("(call|contact|msg)\\s*(on)?\\s*(whatsapp|telegram)", RegexOption.IGNORE_CASE),
        
        // Urgency patterns (NEW)
        Regex("(limited|only)\\s*\\d+\\s*(seats|vacancy|opening)", RegexOption.IGNORE_CASE),
        Regex("(last|final)\\s*(date|day|chance)\\s*(today|tomorrow)", RegexOption.IGNORE_CASE),
        Regex("(hurry|urgent|immediate)\\s*(join|apply|hiring)", RegexOption.IGNORE_CASE),
        
        // Guarantee patterns (NEW)
        Regex("(100|hundred)\\s*%\\s*(genuine|real|guaranteed)", RegexOption.IGNORE_CASE),
        Regex("(guaranteed|assured|fixed)\\s*(income|earning|salary)", RegexOption.IGNORE_CASE),
        Regex("no\\s*(rejection|interview)\\s*(direct|immediate)?\\s*(joining|hiring)", RegexOption.IGNORE_CASE),
        
        // Phone number in description (suspicious)
        Regex("(call|whatsapp|contact)\\s*:?\\s*\\+?91?\\s*\\d{10}", RegexOption.IGNORE_CASE),
        
        // Investment patterns (NEW)
        Regex("(invest|deposit)\\s*₹?\\s*\\d+\\s*(and|to)\\s*(earn|get)", RegexOption.IGNORE_CASE),
        Regex("(daily|weekly|monthly)\\s*(return|profit|income)\\s*₹?\\s*\\d+", RegexOption.IGNORE_CASE)
    )
    
    /**
     * Validates job title and description against scam keywords
     * @return ValidationResult with isValid flag and error message if invalid
     */
    fun validateAgainstScamKeywords(title: String, description: String): ValidationResult {
        val combinedText = "$title $description".lowercase()
        
        // Check blocked keywords
        for (keyword in BLOCKED_KEYWORDS) {
            if (combinedText.contains(keyword.lowercase())) {
                return ValidationResult(
                    isValid = false,
                    errorMessage = "This job posting contains suspicious content. DutyPe only allows genuine local jobs. Keywords like '$keyword' are not permitted.",
                    blockedKeyword = keyword
                )
            }
        }
        
        // Check blocked patterns
        for (pattern in BLOCKED_PATTERNS) {
            if (pattern.containsMatchIn(combinedText)) {
                return ValidationResult(
                    isValid = false,
                    errorMessage = "This job posting appears to be a work-from-home or online job. DutyPe is for local, in-person jobs only.",
                    blockedKeyword = pattern.pattern
                )
            }
        }
        
        return ValidationResult(isValid = true)
    }
    
    // ==========================================
    // PAY RATE GUARDRAILS
    // ==========================================
    
    /**
     * Pay rate ranges per category (in INR)
     * Format: Category -> PayType -> (minRate, maxRate)
     */
    private val PAY_RATE_RANGES: Map<JobCategory, Map<PayType, Pair<Int, Int>>> = mapOf(
        // Cook: ₹300-800/day, ₹50-150/hour, ₹8000-25000/month
        JobCategory.COOK to mapOf(
            PayType.DAILY to Pair(300, 800),
            PayType.HOURLY to Pair(50, 150),
            PayType.MONTHLY to Pair(8000, 25000),
            PayType.TASK to Pair(200, 1000)
        ),
        
        // Maid: ₹250-600/day, ₹40-100/hour, ₹5000-15000/month
        JobCategory.MAID to mapOf(
            PayType.DAILY to Pair(250, 600),
            PayType.HOURLY to Pair(40, 100),
            PayType.MONTHLY to Pair(5000, 15000),
            PayType.TASK to Pair(100, 500)
        ),
        
        // Driver: ₹400-1000/day, ₹60-200/hour, ₹12000-35000/month
        JobCategory.DRIVER to mapOf(
            PayType.DAILY to Pair(400, 1000),
            PayType.HOURLY to Pair(60, 200),
            PayType.MONTHLY to Pair(12000, 35000),
            PayType.TASK to Pair(200, 2000)
        ),
        
        // Helper: ₹250-500/day, ₹40-80/hour, ₹6000-15000/month
        JobCategory.HELPER to mapOf(
            PayType.DAILY to Pair(250, 500),
            PayType.HOURLY to Pair(40, 80),
            PayType.MONTHLY to Pair(6000, 15000),
            PayType.TASK to Pair(100, 500)
        ),
        
        // Security: ₹350-700/day, ₹50-100/hour, ₹10000-20000/month
        JobCategory.SECURITY to mapOf(
            PayType.DAILY to Pair(350, 700),
            PayType.HOURLY to Pair(50, 100),
            PayType.MONTHLY to Pair(10000, 20000),
            PayType.TASK to Pair(300, 1000)
        ),
        
        // Gardener: ₹300-600/day, ₹50-100/hour, ₹7000-15000/month
        JobCategory.GARDENER to mapOf(
            PayType.DAILY to Pair(300, 600),
            PayType.HOURLY to Pair(50, 100),
            PayType.MONTHLY to Pair(7000, 15000),
            PayType.TASK to Pair(200, 800)
        ),
        
        // Caretaker: ₹400-800/day, ₹60-150/hour, ₹10000-25000/month
        JobCategory.CARETAKER to mapOf(
            PayType.DAILY to Pair(400, 800),
            PayType.HOURLY to Pair(60, 150),
            PayType.MONTHLY to Pair(10000, 25000),
            PayType.TASK to Pair(300, 1500)
        ),
        
        // Delivery: ₹300-700/day, ₹40-100/hour, ₹8000-20000/month
        JobCategory.DELIVERY to mapOf(
            PayType.DAILY to Pair(300, 700),
            PayType.HOURLY to Pair(40, 100),
            PayType.MONTHLY to Pair(8000, 20000),
            PayType.TASK to Pair(30, 200) // Per delivery
        ),
        
        // Waiter: ₹300-600/day, ₹50-100/hour, ₹8000-18000/month
        JobCategory.WAITER to mapOf(
            PayType.DAILY to Pair(300, 600),
            PayType.HOURLY to Pair(50, 100),
            PayType.MONTHLY to Pair(8000, 18000),
            PayType.TASK to Pair(200, 800)
        ),
        
        // Electrician: ₹500-1200/day, ₹80-200/hour, ₹15000-35000/month
        JobCategory.ELECTRICIAN to mapOf(
            PayType.DAILY to Pair(500, 1200),
            PayType.HOURLY to Pair(80, 200),
            PayType.MONTHLY to Pair(15000, 35000),
            PayType.TASK to Pair(300, 3000)
        ),
        
        // Plumber: ₹500-1200/day, ₹80-200/hour, ₹15000-35000/month
        JobCategory.PLUMBER to mapOf(
            PayType.DAILY to Pair(500, 1200),
            PayType.HOURLY to Pair(80, 200),
            PayType.MONTHLY to Pair(15000, 35000),
            PayType.TASK to Pair(300, 3000)
        ),
        
        // Painter: ₹400-1000/day, ₹60-150/hour, ₹12000-30000/month
        JobCategory.PAINTER to mapOf(
            PayType.DAILY to Pair(400, 1000),
            PayType.HOURLY to Pair(60, 150),
            PayType.MONTHLY to Pair(12000, 30000),
            PayType.TASK to Pair(500, 5000)
        ),
        
        // Carpenter: ₹500-1200/day, ₹80-200/hour, ₹15000-35000/month
        JobCategory.CARPENTER to mapOf(
            PayType.DAILY to Pair(500, 1200),
            PayType.HOURLY to Pair(80, 200),
            PayType.MONTHLY to Pair(15000, 35000),
            PayType.TASK to Pair(500, 5000)
        ),
        
        // Receptionist: ₹400-700/day, ₹60-120/hour, ₹10000-25000/month
        JobCategory.RECEPTIONIST to mapOf(
            PayType.DAILY to Pair(400, 700),
            PayType.HOURLY to Pair(60, 120),
            PayType.MONTHLY to Pair(10000, 25000),
            PayType.TASK to Pair(300, 1000)
        ),
        
        // Cashier: ₹350-600/day, ₹50-100/hour, ₹8000-20000/month
        JobCategory.CASHIER to mapOf(
            PayType.DAILY to Pair(350, 600),
            PayType.HOURLY to Pair(50, 100),
            PayType.MONTHLY to Pair(8000, 20000),
            PayType.TASK to Pair(200, 800)
        ),
        
        // Packer: ₹300-500/day, ₹40-80/hour, ₹7000-15000/month
        JobCategory.PACKER to mapOf(
            PayType.DAILY to Pair(300, 500),
            PayType.HOURLY to Pair(40, 80),
            PayType.MONTHLY to Pair(7000, 15000),
            PayType.TASK to Pair(50, 300)
        ),
        
        // Other: Generic ranges
        JobCategory.OTHER to mapOf(
            PayType.DAILY to Pair(250, 1500),
            PayType.HOURLY to Pair(40, 250),
            PayType.MONTHLY to Pair(5000, 50000),
            PayType.TASK to Pair(50, 5000)
        )
    )
    
    /**
     * Validates pay rate against category-specific guardrails
     * @return ValidationResult with isValid flag and suggested range if invalid
     */
    fun validatePayRate(
        category: JobCategory,
        payType: PayType,
        payAmount: String
    ): PayRateValidationResult {
        val amount = payAmount.replace(",", "").replace("₹", "").trim().toIntOrNull()
            ?: return PayRateValidationResult(
                isValid = false,
                errorMessage = "Please enter a valid pay amount",
                suggestedMin = null,
                suggestedMax = null
            )
        
        val ranges = PAY_RATE_RANGES[category] ?: PAY_RATE_RANGES[JobCategory.OTHER]!!
        val (minRate, maxRate) = ranges[payType] ?: Pair(0, Int.MAX_VALUE)
        
        return when {
            amount < minRate -> PayRateValidationResult(
                isValid = false,
                errorMessage = "Pay rate seems too low for ${category.displayName}. Market rate is ₹$minRate-₹$maxRate ${payType.displayName.lowercase()}.",
                suggestedMin = minRate,
                suggestedMax = maxRate,
                isTooLow = true
            )
            amount > maxRate -> PayRateValidationResult(
                isValid = false,
                errorMessage = "Pay rate seems unusually high for ${category.displayName}. Market rate is ₹$minRate-₹$maxRate ${payType.displayName.lowercase()}.",
                suggestedMin = minRate,
                suggestedMax = maxRate,
                isTooHigh = true
            )
            else -> PayRateValidationResult(
                isValid = true,
                suggestedMin = minRate,
                suggestedMax = maxRate
            )
        }
    }
    
    /**
     * Gets the suggested pay rate range for a category and pay type
     */
    fun getSuggestedPayRange(category: JobCategory, payType: PayType): Pair<Int, Int> {
        val ranges = PAY_RATE_RANGES[category] ?: PAY_RATE_RANGES[JobCategory.OTHER]!!
        return ranges[payType] ?: Pair(0, 0)
    }
    
    /**
     * Formats the suggested pay range as a display string
     */
    fun formatSuggestedRange(category: JobCategory, payType: PayType): String {
        val (min, max) = getSuggestedPayRange(category, payType)
        val suffix = when (payType) {
            PayType.DAILY -> "/day"
            PayType.HOURLY -> "/hour"
            PayType.MONTHLY -> "/month"
            PayType.TASK -> "/task"
        }
        return "₹$min - ₹$max$suffix"
    }
}

/**
 * Result of scam keyword validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val blockedKeyword: String? = null
)

/**
 * Result of pay rate validation
 */
data class PayRateValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val suggestedMin: Int? = null,
    val suggestedMax: Int? = null,
    val isTooLow: Boolean = false,
    val isTooHigh: Boolean = false
)
