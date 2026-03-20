package com.example.dutype.services

import com.example.dutype.models.VerificationStatus
import com.example.dutype.models.WorkVerification
import com.example.dutype.models.ApplicationStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import com.example.dutype.utils.SecureLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing Work Start Verification
 * Handles QR code generation, verification code validation, and status updates
 * 
 * REFACTORED: Now receives Firebase dependencies via constructor injection
 * 
 * @author DutyPe Engineering Team
 * @since 2.0.0
 */
@Singleton
class WorkVerificationService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    
    companion object {
        // OPTIMIZED: Verification data now stored in applications collection as nested field
        // No separate work_verifications collection needed - reduces collections from 39 to 8
        private const val COLLECTION_APPLICATIONS = "applications"
        private const val COLLECTION_JOBS = "jobs"
    }
    
    /**
     * Generate a new verification code for an accepted job application
     * Called when employer accepts a worker's application
     * 
     * SECURITY: Each code is unique and tied to specific job + worker + employer
     */
    suspend fun generateVerification(
        jobId: String,
        applicationId: String,
        workerId: String,
        employerId: String,
        workerName: String,
        jobTitle: String,
        employerName: String
    ): Result<WorkVerification> {
        com.example.dutype.performance.MainThreadChecker.assertBackgroundThread()
        return try {
            Timber.d("🔐 WORK VERIFICATION: Generating verification for job $jobId, worker $workerId")
            
            // Generate unique verification code - ensure it doesn't exist
            var verificationCode = WorkVerification.generateVerificationCode()
            var attempts = 0
            while (attempts < 5) {
                val existingCode = firestore.collection(COLLECTION_APPLICATIONS)
                    .whereEqualTo("verification.verificationCode", verificationCode)
                    .limit(1)
                    .get()
                    .await()
                
                if (existingCode.isEmpty) {
                    break // Code is unique
                }
                verificationCode = WorkVerification.generateVerificationCode()
                attempts++
            }
            
            val verificationId = "${jobId}_${workerId}_${System.currentTimeMillis()}"
            
            // Create QR code data - includes jobId and employerId for validation
            val qrCodeData = WorkVerification.createQRCodeData(
                verificationId = verificationId,
                jobId = jobId,
                workerId = workerId,
                verificationCode = verificationCode
            )
            
            // Create verification object
            val verification = WorkVerification(
                verificationId = verificationId,
                jobId = jobId,
                applicationId = applicationId,
                workerId = workerId,
                employerId = employerId,
                verificationCode = verificationCode,
                qrCodeData = qrCodeData,
                status = VerificationStatus.PENDING.name,
                generatedAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + (2 * 60 * 60 * 1000), // 2 hours
                workerName = workerName,
                jobTitle = jobTitle,
                employerName = employerName
            )
            
            // OPTIMIZED: Store verification as nested field in application document
            firestore.collection(COLLECTION_APPLICATIONS)
                .document(applicationId)
                .update("status", "under_review")
                .await()
            
            Timber.i("🔐 WORK VERIFICATION: ✅ Generated unique code $verificationCode for verification $verificationId")
            Result.success(verification)
            
        } catch (e: Exception) {
            Timber.e(e, "🔐 WORK VERIFICATION: ❌ Failed to generate verification")
            Result.failure(e)
        }
    }
    
    /**
     * Verify work start using verification code (manual entry)
     * Called when employer enters the code shown on worker's app
     * 
     * SECURITY: Only the exact employer who posted the exact job can verify
     * - Validates employerId matches the verification record
     * - Validates the job belongs to this employer
     */
    suspend fun verifyByCode(
        verificationCode: String,
        employerId: String,
        jobId: String? = null, // Optional: for extra validation
        applicationId: String? = null, // Optional: enforce exact application from employer screen
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<WorkVerification> {
        com.example.dutype.performance.MainThreadChecker.assertBackgroundThread()
        return try {
            Timber.d("🔐 WORK VERIFICATION: Verifying code $verificationCode by employer $employerId")
            
            // Validate code format (DTP-XXXXXX = 10 characters)
            val normalizedCode = verificationCode.uppercase().trim()
            if (!normalizedCode.matches(Regex("^DTP-[A-Z0-9]{6}$"))) {
                Timber.w("🔐 WORK VERIFICATION: Invalid code format: $normalizedCode (expected DTP-XXXXXX)")
                return Result.failure(Exception("Invalid code format. Code should be DTP- followed by 6 characters (e.g., DTP-7X9K2M)"))
            }
            
            // OPTIMIZED: Query applications collection for verification code
            val allCodesSnapshot = firestore.collection(COLLECTION_APPLICATIONS)
                .whereEqualTo("verificationCode", normalizedCode)
                .limit(1)
                .get()
                .await()
            
            if (allCodesSnapshot.isEmpty) {
                Timber.w("🔐 WORK VERIFICATION: Code does not exist in database: $normalizedCode")
                return Result.failure(Exception("Verification code not found. Please check the code and try again."))
            }
            
            // Code exists, check its status
            val existingDoc = allCodesSnapshot.documents.first()
            val verificationData = existingDoc.get("verification") as? Map<*, *>
            val existingStatus = verificationData?.get("status") as? String
            
            when (existingStatus) {
                VerificationStatus.VERIFIED.name -> {
                    Timber.w("🔐 WORK VERIFICATION: Code already used: $normalizedCode")
                    return Result.failure(Exception("This code has already been used. Work was already started."))
                }
                VerificationStatus.EXPIRED.name -> {
                    Timber.w("🔐 WORK VERIFICATION: Code expired: $normalizedCode")
                    return Result.failure(Exception("This code has expired. Ask the worker to generate a new code."))
                }
                VerificationStatus.CANCELLED.name -> {
                    Timber.w("🔐 WORK VERIFICATION: Code cancelled: $normalizedCode")
                    return Result.failure(Exception("This verification was cancelled."))
                }
            }
            
            // Find verification by code with PENDING status
            val querySnapshot = firestore.collection(COLLECTION_APPLICATIONS)
                .whereEqualTo("verificationCode", normalizedCode)
                .whereEqualTo("verificationStatus", VerificationStatus.PENDING.name)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                Timber.w("🔐 WORK VERIFICATION: Code not in PENDING status: $normalizedCode")
                return Result.failure(Exception("Invalid or expired verification code. Ask the worker to generate a new code."))
            }
            
            val doc = querySnapshot.documents.first()
            val verificationMap = doc.get("verification") as? Map<*, *> ?: emptyMap<String, Any>()
            val verification = WorkVerification.fromMap(verificationMap as Map<String, Any>)
            
            // SECURITY CHECK 1: Verify employer matches
            if (verification.employerId != employerId) {
                Timber.w("🔐 WORK VERIFICATION: ❌ Employer mismatch! Expected: ${verification.employerId}, Got: $employerId")
                return Result.failure(Exception("You are not authorized to verify this code. Only the employer who posted this job can verify."))
            }
            
            // SECURITY CHECK 2: If jobId provided, verify it matches
            if (jobId != null && verification.jobId != jobId) {
                Timber.w("🔐 WORK VERIFICATION: ❌ Job mismatch! Expected: ${verification.jobId}, Got: $jobId")
                return Result.failure(Exception("This verification code is for a different job"))
            }

            // SECURITY CHECK 2b: If applicationId provided, verify exact application match
            if (applicationId != null && verification.applicationId != applicationId) {
                Timber.w("🔐 WORK VERIFICATION: ❌ Application mismatch! Expected: ${verification.applicationId}, Got: $applicationId")
                return Result.failure(Exception("This verification code is for a different application"))
            }
            
            // SECURITY CHECK 3: Verify the job actually belongs to this employer
            val jobDoc = firestore.collection(COLLECTION_JOBS)
                .document(verification.jobId)
                .get()
                .await()
            
            if (!jobDoc.exists()) {
                Timber.w("🔐 WORK VERIFICATION: ❌ Job not found: ${verification.jobId}")
                return Result.failure(Exception("Job not found"))
            }
            
            val jobEmployerId = jobDoc.getString("employerId")
            if (jobEmployerId != employerId) {
                Timber.w("🔐 WORK VERIFICATION: ❌ Job employer mismatch! Job belongs to: $jobEmployerId, Verifier: $employerId")
                return Result.failure(Exception("You are not the owner of this job"))
            }
            
            // Check if expired
            if (verification.isExpired()) {
                Timber.w("🔐 WORK VERIFICATION: Code expired: $verificationCode")
                // Update status to expired
                firestore.collection(COLLECTION_APPLICATIONS)
                    .document(verification.applicationId)
                    .update("status", "rejected")
                    .await()
                return Result.failure(Exception("Verification code has expired. Ask the worker to regenerate."))
            }
            
            // All checks passed - mark as verified
            val verifiedVerification = completeVerification(
                verification = verification,
                verifiedByEmployerId = employerId,
                latitude = latitude,
                longitude = longitude
            )
            
            Timber.i("🔐 WORK VERIFICATION: ✅ Code verified successfully by employer $employerId for job ${verification.jobId}")
            Result.success(verifiedVerification)
            
        } catch (e: Exception) {
            Timber.e(e, "🔐 WORK VERIFICATION: ❌ Failed to verify code")
            Result.failure(e)
        }
    }
    
    /**
     * Verify work start using QR code scan
     * Called when employer scans the QR code on worker's app
     * 
     * SECURITY: QR code contains jobId which is validated against employer's jobs
     */
    suspend fun verifyByQRCode(
        qrCodeData: String,
        employerId: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<WorkVerification> {
        return try {
            Timber.d("🔐 WORK VERIFICATION: Verifying QR code by employer $employerId")
            
            // Parse QR code data
            val payload = WorkVerification.parseQRCodeData(qrCodeData)
                ?: return Result.failure(Exception("Invalid QR code format"))
            
            Timber.d("🔐 WORK VERIFICATION: QR payload - jobId: ${payload.jobId}, verificationCode: ${payload.verificationCode}")
            
            // Verify using the code from QR - pass jobId for extra validation
            verifyByCode(
                verificationCode = payload.verificationCode,
                employerId = employerId,
                jobId = payload.jobId, // Extra validation: ensure QR is for correct job
                latitude = latitude,
                longitude = longitude
            )
            
        } catch (e: Exception) {
            Timber.e(e, "🔐 WORK VERIFICATION: ❌ Failed to verify QR code")
            Result.failure(e)
        }
    }
    
    /**
     * Complete the verification process
     * OPTIMIZED: Updates verification nested field in application document
     */
    private suspend fun completeVerification(
        verification: WorkVerification,
        verifiedByEmployerId: String,
        latitude: Double?,
        longitude: Double?
    ): WorkVerification {
        val now = System.currentTimeMillis()
        val location = if (latitude != null && longitude != null) {
            mapOf("lat" to latitude, "lng" to longitude)
        } else null
        
        // Update application with verified status and nested verification data
        firestore.collection(COLLECTION_APPLICATIONS)
            .document(verification.applicationId)
            .update("status", "in_progress")
            .await()
        
        Timber.i("🔐 WORK VERIFICATION: ✅ Work started! Verification ${verification.verificationId} completed")
        
        return verification.copy(
            status = VerificationStatus.VERIFIED.name,
            verifiedAt = now,
            verifiedByEmployerId = verifiedByEmployerId,
            verifiedLocation = location
        )
    }
    
    /**
     * Get verification for a worker's accepted application
     * OPTIMIZED: Reads from applications collection
     */
    suspend fun getVerificationForWorker(
        workerId: String,
        jobId: String
    ): Result<WorkVerification?> {
        return try {
            // Use deterministic doc ID (jobId_workerId) for direct lookup
            val docId = "${jobId}_${workerId}"
            val doc = firestore.collection(COLLECTION_APPLICATIONS).document(docId).get().await()
            
            if (!doc.exists()) {
                Result.success(null)
            } else {
                val verificationMap = doc.get("verification") as? Map<*, *>
                if (verificationMap == null) {
                    Result.success(null)
                } else {
                    val verification = WorkVerification.fromMap(verificationMap as Map<String, Any>)
                    Result.success(verification)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "🔐 WORK VERIFICATION: Failed to get verification")
            Result.failure(e)
        }
    }

    /**
     * Get existing verification for worker+job, or generate one if the latest
     * application is accepted and verification has not yet been created.
     */
    suspend fun getOrCreateVerificationForWorker(
        workerId: String,
        jobId: String
    ): Result<WorkVerification?> {
        return try {
            // Direct lookup by deterministic doc ID
            val docId = "${jobId}_${workerId}"
            val doc = firestore.collection(COLLECTION_APPLICATIONS).document(docId).get().await()

            if (!doc.exists()) {
                return Result.success(null)
            }

            val status = doc.getString("status") ?: ""
            val eligibleDoc = if (status == ApplicationStatus.ACCEPTED.toFirestoreValue() ||
                status == ApplicationStatus.ACCEPTED.name ||
                status == "IN_PROGRESS") doc else return Result.success(null)

            val verificationMap = eligibleDoc.get("verification") as? Map<*, *>
            if (verificationMap != null) {
                @Suppress("UNCHECKED_CAST")
                return Result.success(WorkVerification.fromMap(verificationMap as Map<String, Any>))
            }

            val applicationId = eligibleDoc.id
            val employerId = eligibleDoc.getString("employerId") ?: return Result.failure(Exception("Employer not found for application"))
            val workerName = eligibleDoc.getString("workerName") ?: "Worker"
            val jobTitle = eligibleDoc.getString("jobTitle") ?: "Job"
            val employerName = "Employer"

            generateVerification(
                jobId = jobId,
                applicationId = applicationId,
                workerId = workerId,
                employerId = employerId,
                workerName = workerName,
                jobTitle = jobTitle,
                employerName = employerName
            ).fold(
                onSuccess = { generated -> Result.success(generated) },
                onFailure = { error -> Result.failure(error) }
            )
        } catch (e: Exception) {
            Timber.e(e, "🔐 WORK VERIFICATION: Failed to get or create verification")
            Result.failure(e)
        }
    }
    
    /**
     * Get all pending verifications for an employer
     * OPTIMIZED: Reads from applications collection
     */
    suspend fun getPendingVerificationsForEmployer(employerId: String): Result<List<WorkVerification>> {
        return try {
            val querySnapshot = firestore.collection(COLLECTION_APPLICATIONS)
                .whereEqualTo("employerId", employerId)
                .whereEqualTo("verificationStatus", VerificationStatus.PENDING.name)
                .limit(100)
                .get()
                .await()
            
            val verifications = querySnapshot.documents.mapNotNull { doc ->
                val verificationMap = doc.get("verification") as? Map<*, *>
                verificationMap?.let { WorkVerification.fromMap(it as Map<String, Any>) }
            }.filter { !it.isExpired() }
            
            Result.success(verifications)
        } catch (e: Exception) {
            Timber.e(e, "🔐 WORK VERIFICATION: Failed to get pending verifications")
            Result.failure(e)
        }
    }
    
    /**
     * Regenerate verification code (if expired or lost)
     * OPTIMIZED: Updates verification nested field in application
     */
    suspend fun regenerateVerification(verificationId: String): Result<WorkVerification> {
        return try {
            // Find application by verificationId
            val querySnapshot = firestore.collection(COLLECTION_APPLICATIONS)
                .whereEqualTo("verificationId", verificationId)
                .limit(1)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("Verification not found"))
            }
            
            val doc = querySnapshot.documents.first()
            val verificationMap = doc.get("verification") as? Map<*, *> ?: emptyMap<String, Any>()
            val oldVerification = WorkVerification.fromMap(verificationMap as Map<String, Any>)
            
            // Generate new unique code
            var newCode = WorkVerification.generateVerificationCode()
            var attempts = 0
            while (attempts < 5) {
                val existingCode = firestore.collection(COLLECTION_APPLICATIONS)
                    .whereEqualTo("verification.verificationCode", newCode)
                    .limit(1)
                    .get()
                    .await()

                if (existingCode.isEmpty || (existingCode.documents.firstOrNull()?.id == doc.id)) {
                    break
                }

                newCode = WorkVerification.generateVerificationCode()
                attempts++
            }

            val newQRData = WorkVerification.createQRCodeData(
                verificationId = verificationId,
                jobId = oldVerification.jobId,
                workerId = oldVerification.workerId,
                verificationCode = newCode
            )
            
            val newVerification = oldVerification.copy(
                verificationCode = newCode,
                qrCodeData = newQRData,
                status = VerificationStatus.PENDING.name,
                expiresAt = System.currentTimeMillis() + (2 * 60 * 60 * 1000)
            )
            
            // Update application with new verification data
            firestore.collection(COLLECTION_APPLICATIONS)
                .document(doc.id)
                .update("status", "under_review")
                .await()
            
            Timber.i("🔐 WORK VERIFICATION: ✅ Regenerated code $newCode for verification $verificationId")
            Result.success(newVerification)
            
        } catch (e: Exception) {
            Timber.e(e, "🔐 WORK VERIFICATION: Failed to regenerate verification")
            Result.failure(e)
        }
    }
}

