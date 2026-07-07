package com.example.dutype.services

import com.example.dutype.firestore.FirestoreCollections
import com.example.dutype.models.JobApplication
import com.example.dutype.models.JobListing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobInteractionService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    /**
     * Logs when a worker taps the call button for a specific job.
     * This creates a dedicated call-session document so call taps do not
     * collide with real application records.
     *
     * @param job The job being called.
     * @return A Result containing the created call-session document ID.
     */
    suspend fun logCallButtonTap(job: JobListing): Result<String> {
        return logCallButtonTap(
            jobId = job.id.ifBlank { job.jobId },
            employerId = job.employerId,
            jobTitle = job.title,
            companyName = job.companyName,
            contactNumber = job.contactNumber
        )
    }

    suspend fun logCallButtonTap(application: JobApplication): Result<String> {
        return logCallButtonTap(
            jobId = application.jobId,
            employerId = application.employerId,
            jobTitle = application.jobTitle,
            companyName = application.companyName,
            contactNumber = application.employerPhone.orEmpty()
        )
    }

    suspend fun logCallButtonTap(
        jobId: String,
        employerId: String? = null,
        jobTitle: String? = null,
        companyName: String? = null,
        contactNumber: String? = null
    ): Result<String> {
        val workerId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        if (jobId.isBlank()) return Result.failure(IllegalArgumentException("Job ID cannot be blank"))

        val callSessionRef = firestore.collection(FirestoreCollections.JOB_CALL_SESSIONS).document()

        return try {
            val now = Timestamp.now()
            val callData = mapOf(
                "jobId" to jobId,
                "workerId" to workerId,
                "callButtonTaps" to 1,
                "callStartedAt" to now,
                "createdAt" to now,
                "updatedAt" to now,
                "source" to "JOB_DESCRIPTION_CALL_TAP"
            ).toMutableMap().apply {
                if (!employerId.isNullOrBlank()) put("employerId", employerId)
                if (!jobTitle.isNullOrBlank()) put("jobTitle", jobTitle)
                if (!companyName.isNullOrBlank()) put("companyName", companyName)
                if (!contactNumber.isNullOrBlank()) put("contactNumber", contactNumber)
            }

            callSessionRef.set(callData).await()
            Result.success(callSessionRef.id)
        } catch (e: Exception) {
            Timber.e(e, "Failed to log call button tap for job $jobId")
            Result.failure(e)
        }
    }
}