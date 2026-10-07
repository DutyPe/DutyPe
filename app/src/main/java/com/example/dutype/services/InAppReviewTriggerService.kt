package com.example.dutype.services

import android.app.Activity
import com.example.dutype.utils.InAppReviewManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-App Review Trigger Service
 * 
 * Intelligently triggers review prompts at optimal moments:
 * 
 * WORKER TRIGGERS:
 * - After first job application
 * - After first job completion
 * - After profile completion
 * 
 * EMPLOYER TRIGGERS:
 * - After first job posting
 * - After first successful hire
 * - After first work verification
 * 
 * Follows Google's best practices:
 * - Only after positive experiences
 * - Not too frequently
 * - Native Play Store dialog
 */
@Singleton
class InAppReviewTriggerService @Inject constructor(
    private val reviewManager: InAppReviewManager,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    // P1 FIX: Single reusable scope instead of creating new CoroutineScope per method call
    private val scope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Main)
    
    /**
     * Trigger after worker applies to a job
     */
    fun onWorkerJobApplication(activity: Activity) {
        scope.launch {
            try {
                Timber.i("IN-APP REVIEW: onWorkerJobApplication() called")
                reviewManager.trackPositiveAction()

                // Trigger forcefully on job application
                reviewManager.triggerRatingPrompt(activity, force = true)
                Timber.d("Worker applied to job - review triggered forcefully")
            } catch (e: Exception) {
                Timber.e(e, "Error requesting review after job application")
            }
        }
    }

    fun onWorkerDirectContact(activity: Activity) {
        scope.launch {
            try {
                Timber.i("IN-APP REVIEW: onWorkerDirectContact() called")
                reviewManager.trackPositiveAction()
                reviewManager.triggerRatingPrompt(activity, force = false)
                Timber.d("Worker contacted employer directly - review triggered")
            } catch (e: Exception) {
                Timber.e(e, "Error requesting review after direct contact")
            }
        }
    }

    /**
     * Trigger after worker completes profile
     */
    fun onWorkerProfileCompleted(activity: Activity) {
        scope.launch {
            try {
                reviewManager.trackPositiveAction()
                Timber.d("Worker profile completed - positive action tracked")
            } catch (e: Exception) {
                Timber.e(e, "Error tracking profile completion")
            }
        }
    }

    /**
     * Trigger after employer completes profile
     */
    fun onEmployerProfileCompleted(activity: Activity) {
        scope.launch {
            try {
                reviewManager.trackPositiveAction()
                Timber.d("Employer profile completed - positive action tracked")
            } catch (e: Exception) {
                Timber.e(e, "Error requesting review after employer profile completion")
            }
        }
    }
    
    /**
     * Trigger after customer books a home service (cleaning, plumbing, etc.)
     */
    fun onServiceBooked(activity: Activity) {
        scope.launch {
            try {
                Timber.i("IN-APP REVIEW: onServiceBooked() called")
                reviewManager.trackPositiveAction()
                reviewManager.triggerRatingPrompt(activity, force = true)
                Timber.d("Home service booked - review triggered forcefully")
            } catch (e: Exception) {
                Timber.e(e, "Error requesting review after service booking")
            }
        }
    }

    /**
     * Trigger after a service is successfully completed or rated by the customer
     */
    fun onServiceCompleted(activity: Activity, stars: Int = 5) {
        scope.launch {
            try {
                Timber.i("IN-APP REVIEW: onServiceCompleted() called (stars=$stars)")
                reviewManager.trackPositiveAction()
                reviewManager.triggerRatingPrompt(activity, force = (stars >= 4))
                Timber.d("Service completed - review triggered")
            } catch (e: Exception) {
                Timber.e(e, "Error requesting review after service completion")
            }
        }
    }

    /**
     * Trigger after worker's job or service is marked as completed
     */
    fun onWorkerJobCompleted(activity: Activity) {
        scope.launch {
            try {
                Timber.i("IN-APP REVIEW: onWorkerJobCompleted() called")
                reviewManager.trackPositiveAction()
                reviewManager.triggerRatingPrompt(activity, force = true)
                Timber.d("Worker job completed - requesting review forcefully")
            } catch (e: Exception) {
                Timber.e(e, "Error requesting review after job completion")
            }
        }
    }
    
    /**
     * Trigger after employer posts a job
     * Shows review for employers when they post
     */
    fun onEmployerJobPosted(activity: Activity) {
        scope.launch {
            try {
                Timber.i("IN-APP REVIEW: onEmployerJobPosted() called")
                reviewManager.trackPositiveAction()

                // Trigger forcefully on job posting
                reviewManager.triggerRatingPrompt(activity, force = true)
                Timber.d("Employer posted job - review triggered forcefully")
            } catch (e: Exception) {
                Timber.e(e, "Error requesting review after job posting")
            }
        }
    }

    /**
     * Trigger after user has been actively using the app for some time
     */
    fun onAppUsedForSomeTime(activity: Activity) {
        scope.launch {
            try {
                Timber.i("IN-APP REVIEW: onAppUsedForSomeTime() called")
                val stats = reviewManager.getReviewStats()
                if (!stats.hasRated && reviewManager.shouldShowReviewPrompt()) {
                    reviewManager.triggerRatingPrompt(activity, force = false)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in onAppUsedForSomeTime")
            }
        }
    }
    
    /**
     * Trigger after employer hires a worker (application accepted)
     */
    fun onEmployerHiredWorker(activity: Activity) {
        scope.launch {
            try {
                Timber.i("IN-APP REVIEW: onEmployerHiredWorker() called")
                reviewManager.trackPositiveAction()
                reviewManager.triggerRatingPrompt(activity, force = true)
                Timber.d("Employer hired worker - requesting review forcefully")
            } catch (e: Exception) {
                Timber.e(e, "Error requesting review after hiring")
            }
        }
    }
    
    /**
     * Trigger after employer verifies work completion
     */
    fun onEmployerVerifiedWork(activity: Activity) {
        scope.launch {
            try {
                Timber.i("IN-APP REVIEW: onEmployerVerifiedWork() called")
                reviewManager.trackPositiveAction()
                reviewManager.triggerRatingPrompt(activity, force = true)
                Timber.d("Employer verified work - requesting review forcefully")
            } catch (e: Exception) {
                Timber.e(e, "Error requesting review after work verification")
            }
        }
    }
    
    /**
     * Trigger after successful referral
     */
    fun onSuccessfulReferral(activity: Activity) {
        scope.launch {
            try {
                reviewManager.trackPositiveAction()
                Timber.d("Successful referral - positive action tracked")
            } catch (e: Exception) {
                Timber.e(e, "Error tracking referral")
            }
        }
    }

    /**
     * Trigger after user shares referral code
     */
    fun onReferralCodeShared(activity: Activity) {
        scope.launch {
            try {
                reviewManager.trackPositiveAction()
                Timber.d("Referral code shared - positive action tracked")
            } catch (e: Exception) {
                Timber.e(e, "Error requesting review after referral share")
            }
        }
    }

    /**
     * Trigger after withdrawal is successfully requested
     */
    fun onReferralWithdrawalSuccess(activity: Activity) {
        scope.launch {
            try {
                reviewManager.trackPositiveAction()
                Timber.d("Referral withdrawal successful - positive action tracked")
            } catch (e: Exception) {
                Timber.e(e, "Error requesting review after referral withdrawal success")
            }
        }
    }

    /**
     * Trigger after employer unlocks worker contact details
     */
    fun onEmployerContactUnlocked(activity: Activity) {
        scope.launch {
            try {
                reviewManager.trackPositiveAction()
                reviewManager.triggerRatingPrompt(activity, force = false)
                Timber.d("Employer contact unlocked - requesting review")
            } catch (e: Exception) {
                Timber.e(e, "Error requesting review after contact unlock")
            }
        }
    }

    private suspend fun isFirstWorkerApplication(): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val snapshot = firestore.collection(com.example.dutype.firestore.FirestoreSchema.Applications.COLLECTION)
            .whereEqualTo("workerId", userId)
            .limit(2)
            .get()
            .await()
        return snapshot.size() == 1
    }

    private suspend fun isFirstEmployerPostedJob(): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val snapshot = firestore.collection(com.example.dutype.firestore.FirestoreSchema.Jobs.COLLECTION)
            .whereEqualTo("employerId", userId)
            .limit(2)
            .get()
            .await()
        return snapshot.size() == 1
    }
}
