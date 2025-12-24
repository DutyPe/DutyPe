package com.example.dutype.services

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing device fingerprints for fraud prevention
 * Stores device identifiers and phone numbers for potential account suspension
 */
@Singleton
class DeviceFingerprintService @Inject constructor() {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    companion object {
        private const val COLLECTION_DEVICE_FINGERPRINTS = "device_fingerprints"
        private const val COLLECTION_SUSPENDED_DEVICES = "suspended_devices"
        private const val COLLECTION_USERS = "users"
    }
    
    /**
     * Get Android ID - unique per device per app signing key
     */
    fun getAndroidId(context: Context): String {
        return try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown"
        } catch (e: Exception) {
            Timber.e(e, "Failed to get Android ID")
            "unknown"
        }
    }
    
    /**
     * Get device model info
     */
    fun getDeviceModel(): String {
        return "${Build.MANUFACTURER}_${Build.MODEL}".replace(" ", "_")
    }
    
    /**
     * Get device fingerprint hash combining multiple identifiers
     */
    fun getDeviceFingerprint(context: Context): String {
        val androidId = getAndroidId(context)
        val deviceModel = getDeviceModel()
        val buildId = Build.ID
        
        // Create a combined fingerprint
        val combined = "$androidId|$deviceModel|$buildId"
        return combined.hashCode().toString(16).uppercase()
    }
    
    /**
     * Get comprehensive device info for storage
     */
    fun getDeviceInfo(context: Context): Map<String, Any> {
        return mapOf(
            "androidId" to getAndroidId(context),
            "deviceFingerprint" to getDeviceFingerprint(context),
            "deviceModel" to getDeviceModel(),
            "manufacturer" to Build.MANUFACTURER,
            "brand" to Build.BRAND,
            "device" to Build.DEVICE,
            "product" to Build.PRODUCT,
            "sdkVersion" to Build.VERSION.SDK_INT,
            "androidVersion" to Build.VERSION.RELEASE,
            "timestamp" to System.currentTimeMillis()
        )
    }
    
    /**
     * Store device fingerprint during user registration
     * Links device to user account and phone number for fraud tracking
     */
    suspend fun storeDeviceFingerprint(
        context: Context,
        userId: String,
        phoneNumber: String,
        userRole: String
    ): Result<Unit> {
        return try {
            val deviceInfo = getDeviceInfo(context)
            val fingerprint = getDeviceFingerprint(context)
            
            val fingerprintData = deviceInfo.toMutableMap().apply {
                put("userId", userId)
                put("phoneNumber", phoneNumber)
                put("userRole", userRole)
                put("registeredAt", System.currentTimeMillis())
                put("lastSeenAt", System.currentTimeMillis())
                put("isSuspended", false)
                put("suspensionReason", "")
                put("developerModeViolations", 0)
            }
            
            // Store in device_fingerprints collection (indexed by fingerprint)
            firestore.collection(COLLECTION_DEVICE_FINGERPRINTS)
                .document(fingerprint)
                .set(fingerprintData, SetOptions.merge())
                .await()
            
            // Also update user document with device info
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .set(
                    mapOf(
                        "deviceFingerprint" to fingerprint,
                        "deviceInfo" to deviceInfo,
                        "lastDeviceUpdate" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                .await()
            
            Timber.d("✅ Device fingerprint stored: $fingerprint for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to store device fingerprint")
            Result.failure(e)
        }
    }
    
    /**
     * Update last seen timestamp for device
     */
    suspend fun updateLastSeen(context: Context, userId: String): Result<Unit> {
        return try {
            val fingerprint = getDeviceFingerprint(context)
            
            firestore.collection(COLLECTION_DEVICE_FINGERPRINTS)
                .document(fingerprint)
                .update(
                    mapOf(
                        "lastSeenAt" to System.currentTimeMillis(),
                        "userId" to userId
                    )
                )
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update last seen")
            Result.failure(e)
        }
    }
    
    /**
     * Record a developer mode violation
     * Increments violation count for potential suspension
     */
    suspend fun recordDeveloperModeViolation(
        context: Context,
        userId: String
    ): Result<Int> {
        return try {
            val fingerprint = getDeviceFingerprint(context)
            
            // Get current violation count
            val doc = firestore.collection(COLLECTION_DEVICE_FINGERPRINTS)
                .document(fingerprint)
                .get()
                .await()
            
            val currentViolations = doc.getLong("developerModeViolations")?.toInt() ?: 0
            val newViolations = currentViolations + 1
            
            // Update violation count
            firestore.collection(COLLECTION_DEVICE_FINGERPRINTS)
                .document(fingerprint)
                .update(
                    mapOf(
                        "developerModeViolations" to newViolations,
                        "lastViolationAt" to System.currentTimeMillis(),
                        "userId" to userId
                    )
                )
                .await()
            
            Timber.w("⚠️ Developer mode violation recorded: $newViolations for device: $fingerprint")
            
            // Auto-suspend after 3 violations
            if (newViolations >= 3) {
                suspendDevice(context, userId, "Multiple developer mode violations")
            }
            
            Result.success(newViolations)
        } catch (e: Exception) {
            Timber.e(e, "Failed to record violation")
            Result.failure(e)
        }
    }
    
    /**
     * Suspend a device and associated accounts
     */
    suspend fun suspendDevice(
        context: Context,
        userId: String,
        reason: String
    ): Result<Unit> {
        return try {
            val fingerprint = getDeviceFingerprint(context)
            val androidId = getAndroidId(context)
            
            // Mark device as suspended
            firestore.collection(COLLECTION_DEVICE_FINGERPRINTS)
                .document(fingerprint)
                .update(
                    mapOf(
                        "isSuspended" to true,
                        "suspensionReason" to reason,
                        "suspendedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            
            // Add to suspended devices collection for quick lookup
            firestore.collection(COLLECTION_SUSPENDED_DEVICES)
                .document(fingerprint)
                .set(
                    mapOf(
                        "fingerprint" to fingerprint,
                        "androidId" to androidId,
                        "userId" to userId,
                        "reason" to reason,
                        "suspendedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            
            // Also suspend the user account
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .update(
                    mapOf(
                        "isSuspended" to true,
                        "suspensionReason" to reason,
                        "suspendedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            
            Timber.w("🚫 Device suspended: $fingerprint, User: $userId, Reason: $reason")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to suspend device")
            Result.failure(e)
        }
    }
    
    /**
     * Check if current device is suspended
     */
    suspend fun isDeviceSuspended(context: Context): Result<Boolean> {
        return try {
            val fingerprint = getDeviceFingerprint(context)
            
            val doc = firestore.collection(COLLECTION_SUSPENDED_DEVICES)
                .document(fingerprint)
                .get()
                .await()
            
            val isSuspended = doc.exists()
            
            if (isSuspended) {
                Timber.w("🚫 Device is suspended: $fingerprint")
            }
            
            Result.success(isSuspended)
        } catch (e: Exception) {
            Timber.e(e, "Failed to check device suspension")
            Result.failure(e)
        }
    }
    
    /**
     * Check if a phone number is associated with a suspended device
     */
    suspend fun isPhoneNumberSuspended(phoneNumber: String): Result<Boolean> {
        return try {
            val query = firestore.collection(COLLECTION_DEVICE_FINGERPRINTS)
                .whereEqualTo("phoneNumber", phoneNumber)
                .whereEqualTo("isSuspended", true)
                .get()
                .await()
            
            val isSuspended = !query.isEmpty
            
            if (isSuspended) {
                Timber.w("🚫 Phone number is suspended: $phoneNumber")
            }
            
            Result.success(isSuspended)
        } catch (e: Exception) {
            Timber.e(e, "Failed to check phone suspension")
            Result.failure(e)
        }
    }
    
    /**
     * Get suspension details for a device
     */
    suspend fun getSuspensionDetails(context: Context): Result<Map<String, Any>?> {
        return try {
            val fingerprint = getDeviceFingerprint(context)
            
            val doc = firestore.collection(COLLECTION_SUSPENDED_DEVICES)
                .document(fingerprint)
                .get()
                .await()
            
            if (doc.exists()) {
                Result.success(doc.data)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get suspension details")
            Result.failure(e)
        }
    }
}
