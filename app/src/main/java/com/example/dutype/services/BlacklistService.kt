package com.example.dutype.services

import com.example.dutype.utils.PhoneUtils
import com.example.dutype.utils.SecureLogger
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Blacklist Service - P0 FIX #4 & #5
 * 
 * Handles phone and device blacklist checking to prevent:
 * - Banned scammers from re-registering with same phone
 * - Banned users from creating new accounts on same device
 * 
 * REFACTORED: Now receives FirebaseFirestore via constructor injection
 * NOTE: Device ID retrieval moved to DeviceFingerprintService (canonical implementation)
 * NOTE: Phone normalization uses shared PhoneUtils
 * 
 * @author DutyPe Engineering Team
 * @since 2.0.0
 */
@Singleton
class BlacklistService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        private const val COLLECTION_BLACKLISTS = "blacklists"
        
        // Blacklist types
        const val TYPE_PHONE = "PHONE"
        const val TYPE_DEVICE = "DEVICE"
        const val TYPE_IP = "IP"
        const val TYPE_EMAIL = "EMAIL"
    }
    
    /**
     * Blacklist entry data class
     */
    data class BlacklistEntry(
        val id: String = "",
        val type: String = "",           // PHONE, DEVICE, IP, EMAIL
        val value: String = "",          // The actual phone/device ID/IP/email
        val reason: String = "",         // Why they were banned
        val reportedBy: List<String> = emptyList(), // User IDs who reported
        val reportCount: Int = 0,
        val addedBy: String = "",        // Admin who added
        val addedAt: Long = 0,
        val expiresAt: Long? = null,     // null = permanent
        val isActive: Boolean = true
    )
    
    /**
     * Check result data class
     */
    data class BlacklistCheckResult(
        val isBlacklisted: Boolean,
        val reason: String? = null,
        val blacklistType: String? = null,
        val canAppeal: Boolean = true
    )
    
    // ==========================================
    // PHONE BLACKLIST (P0 FIX #4)
    // ==========================================
    
    /**
     * Check if a phone number is blacklisted
     * Call this BEFORE sending OTP during registration
     * 
     * @param phone Phone number (with or without +91)
     * @return BlacklistCheckResult
     */
    suspend fun isPhoneBlacklisted(phone: String): BlacklistCheckResult {
        return try {
            // Use canonical PhoneUtils for phone normalization
            val normalizedPhone = PhoneUtils.normalizePhone(phone)
            
            // P0 FIX: Use SecureLogger to mask phone number
            SecureLogger.d("BlacklistService", "Checking phone blacklist", 
                "phone" to normalizedPhone
            )
            
            val snapshot = firestore.collection(COLLECTION_BLACKLISTS)
                .whereEqualTo("type", TYPE_PHONE)
                .whereEqualTo("value", normalizedPhone)
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                Timber.d("🛡️ BLACKLIST: Phone $normalizedPhone is NOT blacklisted")
                return BlacklistCheckResult(isBlacklisted = false)
            }
            
            val entry = snapshot.documents.first()
            val reason = entry.getString("reason") ?: "Violation of terms"
            val expiresAt = entry.getLong("expiresAt")
            
            // Check if ban has expired
            if (expiresAt != null && expiresAt < System.currentTimeMillis()) {
                Timber.d("🛡️ BLACKLIST: Phone $normalizedPhone ban has EXPIRED")
                // Optionally: Mark as inactive
                entry.reference.update("isActive", false)
                return BlacklistCheckResult(isBlacklisted = false)
            }
            
            Timber.w("🛡️ BLACKLIST: ⛔ Phone $normalizedPhone is BLACKLISTED - Reason: $reason")
            BlacklistCheckResult(
                isBlacklisted = true,
                reason = reason,
                blacklistType = TYPE_PHONE,
                canAppeal = true
            )
            
        } catch (e: Exception) {
            Timber.e(e, "🛡️ BLACKLIST: Error checking phone blacklist")
            // On error, allow access (fail open) but log for investigation
            BlacklistCheckResult(isBlacklisted = false)
        }
    }
    
    // ==========================================
    // DEVICE BLACKLIST (P0 FIX #5)
    // ==========================================
    
    /**
     * Check if a device is blacklisted
     * Call this on app launch in MainActivity
     * 
     * @param deviceId Android ID or device fingerprint
     * @return BlacklistCheckResult
     */
    suspend fun isDeviceBlacklisted(deviceId: String): BlacklistCheckResult {
        return try {
            if (deviceId.isBlank()) {
                Timber.w("🛡️ BLACKLIST: Empty device ID provided")
                return BlacklistCheckResult(isBlacklisted = false)
            }
            
            // P0 FIX: Use SecureLogger to prevent logging full device ID
            SecureLogger.d("BlacklistService", "Checking device blacklist", 
                "deviceId" to deviceId
            )
            
            val snapshot = firestore.collection(COLLECTION_BLACKLISTS)
                .whereEqualTo("type", TYPE_DEVICE)
                .whereEqualTo("value", deviceId)
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                Timber.d("🛡️ BLACKLIST: Device is NOT blacklisted")
                return BlacklistCheckResult(isBlacklisted = false)
            }
            
            val entry = snapshot.documents.first()
            val reason = entry.getString("reason") ?: "Violation of terms"
            val expiresAt = entry.getLong("expiresAt")
            
            // Check if ban has expired
            if (expiresAt != null && expiresAt < System.currentTimeMillis()) {
                Timber.d("🛡️ BLACKLIST: Device ban has EXPIRED")
                entry.reference.update("isActive", false)
                return BlacklistCheckResult(isBlacklisted = false)
            }
            
            Timber.w("🛡️ BLACKLIST: ⛔ Device is BLACKLISTED - Reason: $reason")
            BlacklistCheckResult(
                isBlacklisted = true,
                reason = reason,
                blacklistType = TYPE_DEVICE,
                canAppeal = true
            )
            
        } catch (e: Exception) {
            Timber.e(e, "🛡️ BLACKLIST: Error checking device blacklist")
            BlacklistCheckResult(isBlacklisted = false)
        }
    }
    
    // ==========================================
    // COMBINED CHECK
    // ==========================================
    
    /**
     * Check both phone and device blacklist
     * Returns the first match found
     */
    suspend fun checkBlacklist(
        phone: String? = null,
        deviceId: String? = null
    ): BlacklistCheckResult {
        // Check phone first
        if (!phone.isNullOrBlank()) {
            val phoneResult = isPhoneBlacklisted(phone)
            if (phoneResult.isBlacklisted) {
                return phoneResult
            }
        }
        
        // Check device
        if (!deviceId.isNullOrBlank()) {
            val deviceResult = isDeviceBlacklisted(deviceId)
            if (deviceResult.isBlacklisted) {
                return deviceResult
            }
        }
        
        return BlacklistCheckResult(isBlacklisted = false)
    }
    
    // ==========================================
    // REPORTING (For future use)
    // ==========================================
    
    /**
     * Report a user for potential blacklisting
     * After 3 reports, user is auto-flagged for review
     * 
     * @param reportedValue Phone/Device/Email to report
     * @param type Type of blacklist entry
     * @param reporterId User ID of reporter
     * @param reason Reason for report
     */
    suspend fun reportForBlacklist(
        reportedValue: String,
        type: String,
        reporterId: String,
        reason: String
    ): Result<Unit> {
        return try {
            val normalizedValue = when (type) {
                TYPE_PHONE -> PhoneUtils.normalizePhone(reportedValue)
                else -> reportedValue
            }
            
            // P0 FIX: Use SecureLogger to mask sensitive data
            SecureLogger.d("BlacklistService", "Reporting for blacklist", 
                "type" to type,
                "reportedValue" to normalizedValue,
                "reporterId" to reporterId
            )
            
            // Check if entry already exists
            val existingSnapshot = firestore.collection(COLLECTION_BLACKLISTS)
                .whereEqualTo("type", type)
                .whereEqualTo("value", normalizedValue)
                .limit(1)
                .get()
                .await()
            
            if (existingSnapshot.isEmpty) {
                // Create new report entry (not yet blacklisted, just reported)
                val reportEntry = hashMapOf(
                    "type" to type,
                    "value" to normalizedValue,
                    "reason" to reason,
                    "reportedBy" to listOf(reporterId),
                    "reportCount" to 1,
                    "addedBy" to "", // Will be set by admin when confirmed
                    "addedAt" to System.currentTimeMillis(),
                    "expiresAt" to null,
                    "isActive" to false, // Not active until admin confirms
                    "isPendingReview" to true
                )
                
                firestore.collection(COLLECTION_BLACKLISTS)
                    .add(reportEntry)
                    .await()
                
                Timber.i("🛡️ BLACKLIST: New report created for $type: $normalizedValue")
            } else {
                // Update existing entry
                val doc = existingSnapshot.documents.first()
                val currentReporters = doc.get("reportedBy") as? List<String> ?: emptyList()
                
                if (!currentReporters.contains(reporterId)) {
                    val newReporters = currentReporters + reporterId
                    val newCount = newReporters.size
                    
                    val updates = hashMapOf<String, Any>(
                        "reportedBy" to newReporters,
                        "reportCount" to newCount
                    )
                    
                    // Auto-activate if 3+ reports
                    if (newCount >= 3) {
                        updates["isActive"] = true
                        updates["reason"] = "Auto-blacklisted: $newCount reports"
                        Timber.w("🛡️ BLACKLIST: ⛔ Auto-blacklisted $type: $normalizedValue (3+ reports)")
                    }
                    
                    doc.reference.update(updates).await()
                    Timber.i("🛡️ BLACKLIST: Report added for $type: $normalizedValue (total: $newCount)")
                }
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "🛡️ BLACKLIST: Error reporting for blacklist")
            Result.failure(e)
        }
    }
    
    // NOTE: normalizePhone() REMOVED - Use PhoneUtils.normalizePhone() instead
    // NOTE: getDeviceId() REMOVED - Use DeviceFingerprintService.getAndroidId() instead
    // This eliminates duplicate utility functions across the codebase
}
