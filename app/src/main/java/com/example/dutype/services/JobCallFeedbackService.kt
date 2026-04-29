package com.example.dutype.services

import com.example.dutype.firestore.FirestoreCollections
import com.example.dutype.models.JobListing
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

enum class JobAvailabilityFeedback(val displayName: String) {
    STILL_AVAILABLE("Still available"),
    FILLED("Job filled"),
    NOT_SURE("Not sure")
}

@Singleton
class JobCallFeedbackService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun submitCallFeedback(
        job: JobListing,
        spokeWithEmployer: Boolean,
        availability: JobAvailabilityFeedback
    ): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Please login to submit feedback"))
            val jobId = job.id.ifBlank { job.jobId }
            if (jobId.isBlank()) return Result.failure(Exception("Job not found"))

            val feedbackRef = firestore.collection(FirestoreCollections.JOB_CALL_FEEDBACK)
                .document(buildFeedbackId(userId, jobId))
            val existing = feedbackRef.get().await()
            val now = Timestamp.now()

            val feedbackData = mutableMapOf<String, Any>(
                "jobId" to jobId,
                "workerId" to userId,
                "spokeWithEmployer" to spokeWithEmployer,
                "jobAvailability" to availability.name,
                "source" to "JOB_DESCRIPTION_CALL_RETURN",
                "updatedAt" to now
            )
            if (job.employerId.isNotBlank()) {
                feedbackData["employerId"] = job.employerId
            }
            if (!existing.exists()) {
                feedbackData["createdAt"] = now
                feedbackRef.set(feedbackData).await()
            } else {
                feedbackRef.update(feedbackData).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to submit job call feedback")
            Result.failure(e)
        }
    }

    private fun buildFeedbackId(userId: String, jobId: String): String = "${userId}_${jobId}"
}