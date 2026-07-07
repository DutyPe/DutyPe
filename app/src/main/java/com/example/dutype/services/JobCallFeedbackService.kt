package com.example.dutype.services

import com.example.dutype.firestore.FirestoreCollections
import com.example.dutype.models.JobApplication
import com.example.dutype.models.JobListing
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
    suspend fun hasSubmittedFeedback(jobId: String): Boolean {
        val workerId = auth.currentUser?.uid ?: return false
        if (jobId.isBlank()) return false

        return try {
            firestore.collection(FirestoreCollections.JOB_CALL_FEEDBACK)
                .document(buildFeedbackId(workerId, jobId))
                .get()
                .await()
                .exists()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun submitCallFeedback(
        application: JobApplication,
        spokeWithEmployer: Boolean,
        availability: JobAvailabilityFeedback?,
        jobOfferAccepted: Boolean?
    ): Result<Unit> {
        return submitCallFeedback(
            jobId = application.jobId,
            employerId = application.employerId,
            jobTitle = application.jobTitle,
            companyName = application.companyName,
            spokeWithEmployer = spokeWithEmployer,
            availability = availability,
            jobOfferAccepted = jobOfferAccepted
        )
    }

    suspend fun submitCallFeedback(
        job: JobListing,
        spokeWithEmployer: Boolean,
        availability: JobAvailabilityFeedback?,
        jobOfferAccepted: Boolean?
    ): Result<Unit> {
        return submitCallFeedback(
            jobId = job.id.ifBlank { job.jobId },
            employerId = job.employerId,
            jobTitle = job.title,
            companyName = job.companyName,
            spokeWithEmployer = spokeWithEmployer,
            availability = availability,
            jobOfferAccepted = jobOfferAccepted
        )
    }

    private suspend fun submitCallFeedback(
        jobId: String,
        employerId: String,
        jobTitle: String,
        companyName: String,
        spokeWithEmployer: Boolean,
        availability: JobAvailabilityFeedback?,
        jobOfferAccepted: Boolean?
    ): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Please login to submit feedback"))
            if (jobId.isBlank()) return Result.failure(Exception("Job not found"))

            val feedbackRef = firestore.collection(FirestoreCollections.JOB_CALL_FEEDBACK)
                .document(buildFeedbackId(userId, jobId))
            val existing = feedbackRef.get().await()
            val now = Timestamp.now()

            val feedbackData = mutableMapOf<String, Any>(
                "jobId" to jobId,
                "workerId" to userId,
                "spokeWithEmployer" to spokeWithEmployer,
                "source" to "JOB_DESCRIPTION_CALL_TAP",
                "feedbackSubmittedAt" to now,
                "updatedAt" to now
            )
            if (availability != null) {
                feedbackData["jobAvailability"] = availability.name
            }
            if (jobOfferAccepted != null) {
                feedbackData["jobOfferAccepted"] = jobOfferAccepted
            }
            if (employerId.isNotBlank()) {
                feedbackData["employerId"] = employerId
            }
            if (jobTitle.isNotBlank()) {
                feedbackData["jobTitle"] = jobTitle
            }
            if (companyName.isNotBlank()) {
                feedbackData["companyName"] = companyName
            }
            if (!existing.exists()) {
                feedbackData["createdAt"] = now
                feedbackRef.set(feedbackData).await()
            } else {
                feedbackRef.set(feedbackData, SetOptions.merge()).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to submit job call feedback")
            Result.failure(e)
        }
    }

    private fun buildFeedbackId(userId: String, jobId: String): String = "${userId}_${jobId}"
}