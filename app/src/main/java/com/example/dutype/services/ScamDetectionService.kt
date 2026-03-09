package com.example.dutype.services

import com.example.dutype.utils.SecureLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI Scam Detection Service
 * Auto-flags suspicious jobs based on multiple risk factors
 */
@Singleton
class ScamDetectionService @Inject constructor() {
    
    // Banned keywords that indicate potential scams
    private val bannedKeywords = listOf(
        // WFH/Online scams
        "work from home", "wfh", "online job", "online work", "data entry",
        "typing job", "form filling", "pdf conversion", "copy paste",
        "sms sending", "email sending", "ad posting", "survey job",
        
        // Investment scams
        "investment", "forex", "crypto", "bitcoin", "trading",
        "mlm", "network marketing", "pyramid", "referral income",
        
        // Too good to be true
        "earn lakhs", "earn crores", "guaranteed income", "no experience needed",
        "work anytime", "flexible hours unlimited", "part time wfh",
        
        // Suspicious requirements
        "laptop required", "computer required", "internet required only",
        "registration fee", "joining fee", "security deposit required",
        
        // Common scam phrases
        "easy money", "quick money", "passive income", "side income from home",
        "telecalling from home", "customer support from home"
    )
    
    // Suspicious patterns
    private val suspiciousPatterns = listOf(
        Regex("earn\\s*₹?\\s*\\d{5,}\\s*(per|/)?\\s*(day|daily)", RegexOption.IGNORE_CASE),
        Regex("₹?\\s*\\d{5,}\\s*-\\s*₹?\\s*\\d{6,}\\s*(per|/)?\\s*(month|monthly)", RegexOption.IGNORE_CASE),
        Regex("no\\s*(experience|qualification|skill)\\s*(required|needed)", RegexOption.IGNORE_CASE),
        Regex("100%\\s*(job|placement|guarantee)", RegexOption.IGNORE_CASE),
        Regex("whatsapp\\s*(only|number|contact)", RegexOption.IGNORE_CASE)
    )
    
    // Category-specific pay limits (per day)
    private val categoryPayLimits = mapOf(
        "DELIVERY" to Pair(300, 2000),
        "DRIVER" to Pair(400, 3000),
        "HELPER" to Pair(300, 1500),
        "COOK" to Pair(400, 2000),
        "MAID" to Pair(300, 1500),
        "SECURITY" to Pair(400, 1500),
        "WAITER" to Pair(300, 1500),
        "ELECTRICIAN" to Pair(500, 3000),
        "PLUMBER" to Pair(500, 3000),
        "CARPENTER" to Pair(500, 3000),
        "PAINTER" to Pair(400, 2500),
        "GARDENER" to Pair(300, 1500),
        "CARETAKER" to Pair(400, 2000),
        "RECEPTIONIST" to Pair(400, 1500),
        "CASHIER" to Pair(400, 1500),
        "PACKER" to Pair(300, 1200)
    )
    
    data class ScamAnalysisResult(
        val isScam: Boolean,
        val riskScore: Int, // 0-100
        val riskLevel: RiskLevel,
        val flags: List<ScamFlag>,
        val recommendation: String
    )
    
    data class ScamFlag(
        val type: FlagType,
        val description: String,
        val severity: Severity
    )
    
    enum class RiskLevel {
        LOW,      // 0-25: Safe
        MEDIUM,   // 26-50: Review recommended
        HIGH,     // 51-75: Likely scam
        CRITICAL  // 76-100: Definite scam
    }
    
    enum class FlagType {
        BANNED_KEYWORD,
        SUSPICIOUS_PATTERN,
        UNREALISTIC_PAY,
        LOCATION_MISMATCH,
        MULTI_CATEGORY_POSTER,
        NEW_ACCOUNT,
        NO_CONTACT_INFO,
        SUSPICIOUS_DESCRIPTION
    }
    
    enum class Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    /**
     * Analyze a job posting for potential scam indicators
     */
    fun analyzeJob(
        title: String,
        description: String,
        category: String,
        payAmount: String,
        payType: String,
        location: String,
        employerJobCount: Int = 0,
        employerCategoryCount: Int = 1,
        hasContactNumber: Boolean = true,
        employerAccountAgeDays: Int = 30
    ): ScamAnalysisResult {
        val flags = mutableListOf<ScamFlag>()
        var riskScore = 0
        
        val combinedText = "$title $description".lowercase()
        
        // Check 1: Banned keywords (Critical)
        bannedKeywords.forEach { keyword ->
            if (combinedText.contains(keyword.lowercase())) {
                flags.add(ScamFlag(
                    type = FlagType.BANNED_KEYWORD,
                    description = "Contains banned keyword: '$keyword'",
                    severity = Severity.CRITICAL
                ))
                riskScore += 25
            }
        }
        
        // Check 2: Suspicious patterns (High)
        suspiciousPatterns.forEach { pattern ->
            if (pattern.containsMatchIn(combinedText)) {
                flags.add(ScamFlag(
                    type = FlagType.SUSPICIOUS_PATTERN,
                    description = "Matches suspicious pattern",
                    severity = Severity.HIGH
                ))
                riskScore += 15
            }
        }
        
        // Check 3: Unrealistic pay (High)
        val payAmountNum = payAmount.replace(",", "").replace("₹", "").toIntOrNull() ?: 0
        val categoryLimits = categoryPayLimits[category.uppercase()]
        if (categoryLimits != null && payAmountNum > 0) {
            val dailyPay = when {
                payType.contains("hour", true) -> payAmountNum * 8
                payType.contains("month", true) -> payAmountNum / 26
                else -> payAmountNum
            }
            
            if (dailyPay > categoryLimits.second * 2) {
                flags.add(ScamFlag(
                    type = FlagType.UNREALISTIC_PAY,
                    description = "Pay ₹$dailyPay/day is unrealistically high for $category",
                    severity = Severity.HIGH
                ))
                riskScore += 20
            }
        }
        
        // Check 4: Multi-category poster (Medium) - Consultancy detection
        if (employerCategoryCount >= 5) {
            flags.add(ScamFlag(
                type = FlagType.MULTI_CATEGORY_POSTER,
                description = "Employer posts in $employerCategoryCount different categories",
                severity = Severity.MEDIUM
            ))
            riskScore += 15
        }
        
        // Check 5: New account (Low)
        if (employerAccountAgeDays < 7) {
            flags.add(ScamFlag(
                type = FlagType.NEW_ACCOUNT,
                description = "Account is only $employerAccountAgeDays days old",
                severity = Severity.LOW
            ))
            riskScore += 10
        }
        
        // Check 6: No contact info (Medium)
        if (!hasContactNumber) {
            flags.add(ScamFlag(
                type = FlagType.NO_CONTACT_INFO,
                description = "No contact number provided",
                severity = Severity.MEDIUM
            ))
            riskScore += 10
        }
        
        // Check 7: Suspicious description length (Low)
        if (description.length < 20) {
            flags.add(ScamFlag(
                type = FlagType.SUSPICIOUS_DESCRIPTION,
                description = "Description is too short",
                severity = Severity.LOW
            ))
            riskScore += 5
        }
        
        // Cap risk score at 100
        riskScore = riskScore.coerceAtMost(100)
        
        val riskLevel = when {
            riskScore >= 76 -> RiskLevel.CRITICAL
            riskScore >= 51 -> RiskLevel.HIGH
            riskScore >= 26 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
        
        val recommendation = when (riskLevel) {
            RiskLevel.CRITICAL -> "This job appears to be a scam. Do not apply."
            RiskLevel.HIGH -> "This job has multiple red flags. Proceed with extreme caution."
            RiskLevel.MEDIUM -> "This job has some suspicious elements. Verify before applying."
            RiskLevel.LOW -> "This job appears legitimate."
        }
        
        return ScamAnalysisResult(
            isScam = riskScore >= 51,
            riskScore = riskScore,
            riskLevel = riskLevel,
            flags = flags,
            recommendation = recommendation
        )
    }
    
    /**
     * Quick check if job should be auto-blocked
     */
    fun shouldAutoBlock(title: String, description: String): Boolean {
        val combinedText = "$title $description".lowercase()
        
        // Critical keywords that should auto-block
        val criticalKeywords = listOf(
            "work from home", "wfh", "data entry", "typing job",
            "online job", "form filling", "registration fee",
            "joining fee", "investment required"
        )
        
        return criticalKeywords.any { combinedText.contains(it) }
    }
}
