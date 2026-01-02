package com.example.dutype.services

import com.example.dutype.models.VerificationStatus
import com.example.dutype.models.WorkVerification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
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
        private const val COLLECTION_VERIFICATIONS = "work_verifications"
        private const val COLLECTION_APPLICATIONS = "job_applications"
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
        return try {
            Timber.d("🔐 WORK VERIFICATION: Generating verification for job $jobId, worker $workerId")
            
            // Generate unique verification code - ensure it doesn't exist
            var verificationCode = WorkVerification.generateVerificationCode()
            var attempts = 0
            while (attempts < 5) {
                val existingCode = firestore.collection(COLLECTION_VERIFICATIONS)
                    .whereEqualTo("verificationCode", verificationCode)
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
            
            // Save to Firestore
            firestore.collection(COLLECTION_VERIFICATIONS)
                .document(verificationId)
                .set(verification.toMap())
                .await()
            
            // Update application with verification ID
            firestore.collection(COLLECTION_APPLICATIONS)
                .document(applicationId)
                .update(
                    mapOf(
                        "verificationId" to verificationId,
                        "verificationCode" to verificationCode,
                        "verificationStatus" to VerificationStatus.PENDING.name
                    )
                )
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
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<WorkVerification> {
        return try {
            Timber.d("🔐 WORK VERIFICATION: Verifying code $verificationCode by employer $employerId")
            
            // Validate code format (DTP-XXXXXX = 10 characters)
            val normalizedCode = verificationCode.uppercase().trim()
            if (!normalizedCode.matches(Regex("^DTP-[A-Z0-9]{6}$"))) {
                Timber.w("🔐 WORK VERIFICATION: Invalid code format: $normalizedCode (expected DTP-XXXXXX)")
                return Result.failure(Exception("Invalid code format. Code should be DTP- followed by 6 characters (e.g., DTP-7X9K2M)"))
            }
            
            // First, check if code exists at all (regardless of status)
            val allCodesSnapshot = firestore.collection(COLLECTION_VERIFICATIONS)
                .whereEqualTo("verificationCode", normalizedCode)
                .get()
                .await()
            
            if (allCodesSnapshot.isEmpty) {
                Timber.w("🔐 WORK VERIFICATION: Code does not exist in database: $normalizedCode")
                return Result.failure(Exception("Verification code not found. Please check the code and try again."))
            }
            
            // Code exists, check its status
            val existingDoc = allCodesSnapshot.documents.first()
            val existingStatus = existingDoc.getString("status")
            
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
            val querySnapshot = firestore.collection(COLLECTION_VERIFICATIONS)
                .whereEqualTo("verificationCode", normalizedCode)
                .whereEqualTo("status", VerificationStatus.PENDING.name)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                Timber.w("🔐 WORK VERIFICATION: Code not in PENDING status: $normalizedCode")
                return Result.failure(Exception("Invalid or expired verification code. Ask the worker to generate a new code."))
            }
            
            val doc = querySnapshot.documents.first()
            val verification = WorkVerification.fromMap(doc.data ?: emptyMap())
            
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
                firestore.collection(COLLECTION_VERIFICATIONS)
                    .document(verification.verificationId)
                    .update("status", VerificationStatus.EXPIRED.name)
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
        
        // Update verification document
        firestore.collection(COLLECTION_VERIFICATIONS)
            .document(verification.verificationId)
            .update(
                mapOf(
                    "status" to VerificationStatus.VERIFIED.name,
                    "verifiedAt" to now,
                    "verifiedByEmployerId" to verifiedByEmployerId,
                    "verifiedLocation" to location
                )
            )
            .await()
        
        // Update application status to IN_PROGRESS
        firestore.collection(COLLECTION_APPLICATIONS)
            .document(verification.applicationId)
            .update(
                mapOf(
                    "verificationStatus" to VerificationStatus.VERIFIED.name,
                    "workStartedAt" to now,
                    "status" to "IN_PROGRESS"
                )
            )
            .await()
        
        // Update job status
        firestore.collection(COLLECTION_JOBS)
            .document(verification.jobId)
            .update(
                mapOf(
                    "hasActiveWorker" to true,
                    "lastWorkerStartedAt" to now
                )
            )
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
     */
    suspend fun getVerificationForWorker(
        workerId: String,
        jobId: String
    ): Result<WorkVerification?> {
        return try {
            val querySnapshot = firestore.collection(COLLECTION_VERIFICATIONS)
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("jobId", jobId)
                .orderBy("generatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                Result.success(null)
            } else {
                val verification = WorkVerification.fromMap(querySnapshot.documents.first().data ?: emptyMap())
                Result.success(verification)
            }
        } catch (e: Exception) {
            Timber.e(e, "🔐 WORK VERIFICATION: Failed to get verification")
            Result.failure(e)
        }
    }
    
    /**
     * Get all pending verifications for an employer
     */
    suspend fun getPendingVerificationsForEmployer(employerId: String): Result<List<WorkVerification>> {
        return try {
            val querySnapshot = firestore.collection(COLLECTION_VERIFICATIONS)
                .whereEqualTo("employerId", employerId)
                .whereEqualTo("status", VerificationStatus.PENDING.name)
                .get()
                .await()
            
            val verifications = querySnapshot.documents.mapNotNull { doc ->
                doc.data?.let { WorkVerification.fromMap(it) }
            }.filter { !it.isExpired() }
            
            Result.success(verifications)
        } catch (e: Exception) {
            Timber.e(e, "🔐 WORK VERIFICATION: Failed to get pending verifications")
            Result.failure(e)
        }
    }
    
    /**
     * Regenerate verification code (if expired or lost)
     */
    suspend fun regenerateVerification(verificationId: String): Result<WorkVerification> {
        return try {
            val doc = firestore.collection(COLLECTION_VERIFICATIONS)
                .document(verificationId)
                .get()
                .await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("Verification not found"))
            }
            
            val oldVerification = WorkVerification.fromMap(doc.data ?: emptyMap())
            
            // Generate new code
            val newCode = WorkVerification.generateVerificationCode()
            val newQRData = WorkVerification.createQRCodeData(
                verificationId = verificationId,
                jobId = oldVerification.jobId,
                workerId = oldVerification.workerId,
                verificationCode = newCode
            )
            
            // Update with new code and extended expiry
            val updates = mapOf(
                "verificationCode" to newCode,
                "qrCodeData" to newQRData,
                "status" to VerificationStatus.PENDING.name,
                "expiresAt" to System.currentTimeMillis() + (2 * 60 * 60 * 1000)
            )
            
            firestore.collection(COLLECTION_VERIFICATIONS)
                .document(verificationId)
                .update(updates)
                .await()
            
            // Update application
            firestore.collection(COLLECTION_APPLICATIONS)
                .document(oldVerification.applicationId)
                .update(
                    mapOf(
                        "verificationCode" to newCode,
                        "verificationStatus" to VerificationStatus.PENDING.name
                    )
                )
                .await()
            
            val newVerification = oldVerification.copy(
                verificationCode = newCode,
                qrCodeData = newQRData,
                status = VerificationStatus.PENDING.name,
                expiresAt = System.currentTimeMillis() + (2 * 60 * 60 * 1000)
            )
            
            Timber.i("🔐 WORK VERIFICATION: ✅ Regenerated code $newCode for verification $verificationId")
            Result.success(newVerification)
            
        } catch (e: Exception) {
            Timber.e(e, "🔐 WORK VERIFICATION: Failed to regenerate verification")
            Result.failure(e)
        }
    }
}
