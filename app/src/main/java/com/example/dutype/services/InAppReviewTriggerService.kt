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
 * - After first job application ⭐
 * - After first job completion
 * - After profile completion
 * 
 * EMPLOYER TRIGGERS:
 * - After first job posting ⭐
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
                Timber.i("⭐ IN-APP REVIEW: onWorkerJobApplication() called")
                
                // Check stats for logging
                val stats = reviewManager.getReviewStats()
                Timber.d("📊 Review stats: hasRated=${stats.hasRated}, dismissCount=${stats.dismissCount}")
                
                // Trigger review immediately (no threshold check)
                // This works for both new users and old users who applied to many jobs
                reviewManager.requestInAppReview(activity)
                
                Timber.d("📝 Worker applied to job - review triggered")
            } catch (e: Exception) {
                Timber.e(e, "❌ Error requesting review after job application")
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
                Timber.d("✅ Worker profile completed - positive action tracked")
            } catch (e: Exception) {
                Timber.e(e, "Error tracking profile completion")
            }
        }
    }
    
    /**
     * Trigger after worker's job is marked as completed
     */
    fun onWorkerJobCompleted(activity: Activity) {
        scope.launch {
            try {
                reviewManager.trackPositiveAction()
                reviewManager.requestInAppReview(activity)
                Timber.d("🎉 Worker job completed - requesting review")
            } catch (e: Exception) {
                Timber.e(e, "Error requesting review after job completion")
            }
        }
    }
    
    /**
     * Trigger after employer posts a job
     * Shows review for ALL employers (new and old) when they post
     * No threshold - triggers immediately if conditions are met
     */
    fun onEmployerJobPosted(activity: Activity) {
        scope.launch {
            try {
                Timber.i("⭐ IN-APP REVIEW: onEmployerJobPosted() called")
                
                // Check stats for logging
                val stats = reviewManager.getReviewStats()
                Timber.d("📊 Review stats: hasRated=${stats.hasRated}, dismissCount=${stats.dismissCount}")
                
                // Trigger review immediately (no threshold check)
                // This works for both new employers and old employers who posted many jobs
                reviewManager.requestInAppReview(activity)
                
                Timber.d("💼 Employer posted job - review triggered")
            } catch (e: Exception) {
                Timber.e(e, "❌ Error requesting review after job posting")
            }
        }
    }
    
    /**
     * Trigger after employer hires a worker (application accepted)
     */
    fun onEmployerHiredWorker(activity: Activity) {
        scope.launch {
            try {
                reviewManager.trackPositiveAction()
                reviewManager.requestInAppReview(activity)
                Timber.d("🤝 Employer hired worker - requesting review")
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
                reviewManager.trackPositiveAction()
                reviewManager.requestInAppReview(activity)
                Timber.d("✅ Employer verified work - requesting review")
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
                Timber.d("🎁 Successful referral - positive action tracked")
            } catch (e: Exception) {
                Timber.e(e, "Error tracking referral")
            }
        }
    }
}
