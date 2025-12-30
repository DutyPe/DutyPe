package com.example.dutype.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

/**
 * Work Start Verification Models
 * Used for QR code or unique code verification when starting work
 */

/**
 * Work Verification Status
 */
enum class VerificationStatus {
    PENDING,    // Code generated, waiting for verification
    VERIFIED,   // Successfully verified
    EXPIRED,    // Code expired (after 2 hours)
    CANCELLED   // Job cancelled before verification
}

/**
 * Work Verification Data
 * Stores the verification code and status for a job
 */
data class WorkVerification(
    val verificationId: String = "",
    val jobId: String = "",
    val applicationId: String = "",
    val workerId: String = "",
    val employerId: String = "",
    val verificationCode: String = "", // e.g., "DTP-7X9K"
    val qrCodeData: String = "", // Encoded job + worker info for QR
    val status: String = VerificationStatus.PENDING.name,
    val generatedAt: Long = System.currentTimeMillis(),
    val verifiedAt: Long? = null,
    val verifiedByEmployerId: String? = null,
    val verifiedLocation: Map<String, Double>? = null, // lat, lng
    val expiresAt: Long = System.currentTimeMillis() + (2 * 60 * 60 * 1000), // 2 hours from now
    val workerName: String = "",
    val jobTitle: String = "",
    val employerName: String = ""
) {
    /**
     * Check if verification code is expired
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiresAt
    }
    
    /**
     * Check if verification is still pending
     */
    fun isPending(): Boolean {
        return status == VerificationStatus.PENDING.name && !isExpired()
    }
    
    /**
     * Check if verification is complete
     */
    fun isVerified(): Boolean {
        return status == VerificationStatus.VERIFIED.name
    }
    
    /**
     * Get time remaining until expiry in minutes
     */
    fun getMinutesUntilExpiry(): Int {
        val remaining = expiresAt - System.currentTimeMillis()
        return if (remaining > 0) (remaining / (60 * 1000)).toInt() else 0
    }
    
    /**
     * Get formatted expiry time
     */
    fun getExpiryText(): String {
        val minutes = getMinutesUntilExpiry()
        return when {
            minutes <= 0 -> "Expired"
            minutes < 60 -> "$minutes min remaining"
            else -> "${minutes / 60}h ${minutes % 60}m remaining"
        }
    }
    
    /**
     * Convert to Firestore map
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "verificationId" to verificationId,
            "jobId" to jobId,
            "applicationId" to applicationId,
            "workerId" to workerId,
            "employerId" to employerId,
            "verificationCode" to verificationCode,
            "qrCodeData" to qrCodeData,
            "status" to status,
            "generatedAt" to generatedAt,
            "verifiedAt" to verifiedAt,
            "verifiedByEmployerId" to verifiedByEmployerId,
            "verifiedLocation" to verifiedLocation,
            "expiresAt" to expiresAt,
            "workerName" to workerName,
            "jobTitle" to jobTitle,
            "employerName" to employerName
        )
    }
    
    companion object {
        /**
         * Generate a unique verification code
         * Format: DTP-XXXXXX (e.g., DTP-7X9K2M)
         * Uses only non-confusing characters (no 0,O,1,I,L)
         * 6 characters = ~729 million combinations for strong uniqueness
         */
        fun generateVerificationCode(): String {
            val chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
            val code = (1..6).map { chars.random() }.joinToString("")
            return "DTP-$code"
        }
        
        /**
         * Create QR code data payload
         */
        fun createQRCodeData(
            verificationId: String,
            jobId: String,
            workerId: String,
            verificationCode: String
        ): String {
            // Simple JSON-like format for QR code
            return "DUTYPE_VERIFY|$verificationId|$jobId|$workerId|$verificationCode|${System.currentTimeMillis()}"
        }
        
        /**
         * Parse QR code data
         */
        fun parseQRCodeData(qrData: String): QRCodePayload? {
            return try {
                val parts = qrData.split("|")
                if (parts.size >= 5 && parts[0] == "DUTYPE_VERIFY") {
                    QRCodePayload(
                        verificationId = parts[1],
                        jobId = parts[2],
                        workerId = parts[3],
                        verificationCode = parts[4],
                        timestamp = parts.getOrNull(5)?.toLongOrNull() ?: 0L
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * Create from Firestore document
         */
        fun fromMap(map: Map<String, Any?>): WorkVerification {
            return WorkVerification(
                verificationId = map["verificationId"] as? String ?: "",
                jobId = map["jobId"] as? String ?: "",
                applicationId = map["applicationId"] as? String ?: "",
                workerId = map["workerId"] as? String ?: "",
                employerId = map["employerId"] as? String ?: "",
                verificationCode = map["verificationCode"] as? String ?: "",
                qrCodeData = map["qrCodeData"] as? String ?: "",
                status = map["status"] as? String ?: VerificationStatus.PENDING.name,
                generatedAt = (map["generatedAt"] as? Long) ?: System.currentTimeMillis(),
                verifiedAt = map["verifiedAt"] as? Long,
                verifiedByEmployerId = map["verifiedByEmployerId"] as? String,
                verifiedLocation = map["verifiedLocation"] as? Map<String, Double>,
                expiresAt = (map["expiresAt"] as? Long) ?: System.currentTimeMillis(),
                workerName = map["workerName"] as? String ?: "",
                jobTitle = map["jobTitle"] as? String ?: "",
                employerName = map["employerName"] as? String ?: ""
            )
        }
    }
}

/**
 * QR Code Payload - parsed from QR code scan
 */
data class QRCodePayload(
    val verificationId: String,
    val jobId: String,
    val workerId: String,
    val verificationCode: String,
    val timestamp: Long
)
