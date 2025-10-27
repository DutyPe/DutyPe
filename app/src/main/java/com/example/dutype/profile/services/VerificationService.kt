package com.example.dutype.profile.services

import com.example.dutype.profile.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling user verification processes
 * Manages email, phone, ID proof, and other verification types
 */
@Singleton
class VerificationService @Inject constructor() {
    
    // In-memory storage for verifications (in production, use Room database)
    private val userVerifications = mutableMapOf<String, MutableList<Verification>>()
    private val verificationCodes = mutableMapOf<String, VerificationCode>()
    
    private val _verifications = MutableStateFlow<List<Verification>>(emptyList())
    val verifications: StateFlow<List<Verification>> = _verifications.asStateFlow()
    
    /**
     * Get all verifications for a user
     */
    fun getUserVerifications(userId: String): Flow<List<Verification>> = flow {
        val verifications = userVerifications[userId] ?: emptyList()
        emit(verifications)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Start email verification process
     */
    suspend fun startEmailVerification(userId: String, email: String): Result<VerificationCode> {
        return try {
            val code = generateVerificationCode()
            val verificationCode = VerificationCode(
                id = UUID.randomUUID().toString(),
                userId = userId,
                type = VerificationType.EMAIL,
                code = code,
                target = email,
                expiresAt = System.currentTimeMillis() + (15 * 60 * 1000), // 15 minutes
                createdAt = System.currentTimeMillis()
            )
            
            verificationCodes[verificationCode.id] = verificationCode
            
            // In a real app, send email here
            // emailService.sendVerificationEmail(email, code)
            
            Result.success(verificationCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Start phone verification process
     */
    suspend fun startPhoneVerification(userId: String, phone: String): Result<VerificationCode> {
        return try {
            val code = generateVerificationCode()
            val verificationCode = VerificationCode(
                id = UUID.randomUUID().toString(),
                userId = userId,
                type = VerificationType.PHONE,
                code = code,
                target = phone,
                expiresAt = System.currentTimeMillis() + (10 * 60 * 1000), // 10 minutes
                createdAt = System.currentTimeMillis()
            )
            
            verificationCodes[verificationCode.id] = verificationCode
            
            // In a real app, send SMS here
            // smsService.sendVerificationSMS(phone, code)
            
            Result.success(verificationCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Verify email with code
     */
    suspend fun verifyEmail(userId: String, code: String): Result<Verification> {
        return try {
            val verificationCode = verificationCodes.values.find { 
                it.userId == userId && it.type == VerificationType.EMAIL && it.code == code 
            }
            
            if (verificationCode == null) {
                return Result.failure(Exception("Invalid verification code"))
            }
            
            if (System.currentTimeMillis() > verificationCode.expiresAt) {
                return Result.failure(Exception("Verification code expired"))
            }
            
            val verification = Verification(
                id = UUID.randomUUID().toString(),
                type = VerificationType.EMAIL,
                status = VerificationStatus.VERIFIED,
                verifiedBy = "system",
                verifiedAt = System.currentTimeMillis()
            )
            
            addVerification(userId, verification)
            verificationCodes.remove(verificationCode.id)
            
            Result.success(verification)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Verify phone with code
     */
    suspend fun verifyPhone(userId: String, code: String): Result<Verification> {
        return try {
            val verificationCode = verificationCodes.values.find { 
                it.userId == userId && it.type == VerificationType.PHONE && it.code == code 
            }
            
            if (verificationCode == null) {
                return Result.failure(Exception("Invalid verification code"))
            }
            
            if (System.currentTimeMillis() > verificationCode.expiresAt) {
                return Result.failure(Exception("Verification code expired"))
            }
            
            val verification = Verification(
                id = UUID.randomUUID().toString(),
                type = VerificationType.PHONE,
                status = VerificationStatus.VERIFIED,
                verifiedBy = "system",
                verifiedAt = System.currentTimeMillis()
            )
            
            addVerification(userId, verification)
            verificationCodes.remove(verificationCode.id)
            
            Result.success(verification)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Submit ID proof for verification
     */
    suspend fun submitIdProof(
        userId: String,
        documentUrl: String,
        documentType: String
    ): Result<Verification> {
        return try {
            val verification = Verification(
                id = UUID.randomUUID().toString(),
                type = VerificationType.ID_PROOF,
                status = VerificationStatus.PENDING,
                documentUrl = documentUrl,
                submittedAt = System.currentTimeMillis()
            )
            
            addVerification(userId, verification)
            
            // In a real app, this would trigger manual review process
            // adminService.notifyIdProofSubmission(userId, verification.id)
            
            Result.success(verification)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Submit address proof for verification
     */
    suspend fun submitAddressProof(
        userId: String,
        documentUrl: String,
        documentType: String
    ): Result<Verification> {
        return try {
            val verification = Verification(
                id = UUID.randomUUID().toString(),
                type = VerificationType.ADDRESS,
                status = VerificationStatus.PENDING,
                documentUrl = documentUrl,
                submittedAt = System.currentTimeMillis()
            )
            
            addVerification(userId, verification)
            
            Result.success(verification)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Submit education verification
     */
    suspend fun submitEducationVerification(
        userId: String,
        educationId: String,
        documentUrl: String
    ): Result<Verification> {
        return try {
            val verification = Verification(
                id = UUID.randomUUID().toString(),
                type = VerificationType.EDUCATION,
                status = VerificationStatus.PENDING,
                documentUrl = documentUrl,
                submittedAt = System.currentTimeMillis()
            )
            
            addVerification(userId, verification)
            
            Result.success(verification)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Submit employment verification
     */
    suspend fun submitEmploymentVerification(
        userId: String,
        experienceId: String,
        documentUrl: String
    ): Result<Verification> {
        return try {
            val verification = Verification(
                id = UUID.randomUUID().toString(),
                type = VerificationType.EMPLOYMENT,
                status = VerificationStatus.PENDING,
                documentUrl = documentUrl,
                submittedAt = System.currentTimeMillis()
            )
            
            addVerification(userId, verification)
            
            Result.success(verification)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get verification status for a specific type
     */
    fun getVerificationStatus(userId: String, type: VerificationType): VerificationStatus {
        val verification = userVerifications[userId]?.find { it.type == type }
        return verification?.status ?: VerificationStatus.NOT_VERIFIED
    }
    
    /**
     * Get verification statistics
     */
    fun getVerificationStats(userId: String): VerificationStats {
        val verifications = userVerifications[userId] ?: emptyList()
        
        val byStatus = verifications.groupingBy { it.status }.eachCount()
        val byType = verifications.groupingBy { it.type }.eachCount()
        val verifiedCount = verifications.count { it.status == VerificationStatus.VERIFIED }
        val pendingCount = verifications.count { it.status == VerificationStatus.PENDING }
        
        return VerificationStats(
            totalVerifications = verifications.size,
            verifiedCount = verifiedCount,
            pendingCount = pendingCount,
            rejectedCount = verifications.count { it.status == VerificationStatus.REJECTED },
            byStatus = byStatus,
            byType = byType,
            verificationScore = calculateVerificationScore(verifications)
        )
    }
    
    /**
     * Calculate verification score (0-100)
     */
    private fun calculateVerificationScore(verifications: List<Verification>): Int {
        val verifiedTypes = verifications.filter { it.status == VerificationStatus.VERIFIED }
        val totalTypes = VerificationType.values().size
        return (verifiedTypes.size * 100) / totalTypes
    }
    
    /**
     * Add verification to user's list
     */
    private fun addVerification(userId: String, verification: Verification) {
        val verificationList = userVerifications.getOrPut(userId) { mutableListOf() }
        
        // Remove existing verification of same type
        verificationList.removeAll { it.type == verification.type }
        
        verificationList.add(verification)
        _verifications.value = verificationList.toList()
    }
    
    /**
     * Generate verification code
     */
    private fun generateVerificationCode(): String {
        return (100000..999999).random().toString()
    }
    
    /**
     * Resend verification code
     */
    suspend fun resendVerificationCode(userId: String, type: VerificationType): Result<VerificationCode> {
        return try {
            val existingCode = verificationCodes.values.find { 
                it.userId == userId && it.type == type 
            }
            
            if (existingCode != null) {
                verificationCodes.remove(existingCode.id)
            }
            
            when (type) {
                VerificationType.EMAIL -> {
                    // Get email from user profile
                    startEmailVerification(userId, "dutypein@gmail.com")
                }
                VerificationType.PHONE -> {
                    // Get phone from user profile
                    startPhoneVerification(userId, "+1234567890")
                }
                else -> {
                    Result.failure(Exception("Cannot resend code for this verification type"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Verification code model
 */
data class VerificationCode(
    val id: String,
    val userId: String,
    val type: VerificationType,
    val code: String,
    val target: String, // email or phone number
    val expiresAt: Long,
    val createdAt: Long
)

/**
 * Verification statistics
 */
data class VerificationStats(
    val totalVerifications: Int = 0,
    val verifiedCount: Int = 0,
    val pendingCount: Int = 0,
    val rejectedCount: Int = 0,
    val byStatus: Map<VerificationStatus, Int> = emptyMap(),
    val byType: Map<VerificationType, Int> = emptyMap(),
    val verificationScore: Int = 0
)
