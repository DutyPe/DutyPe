package com.example.dutype.services

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Smart Notification Worker
 * 
 * Runs daily to send time-based and behavior-based notifications:
 * - Job expiry reminders (24 hours before)
 * - Pending application reminders (48 hours)
 * - Inactive user re-engagement (3 days for workers, 15 days for employers)
 * 
 * Scheduled to run once per day at optimal time (8 AM)
 */
@HiltWorker
class SmartNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val smartNotificationManager: SmartNotificationManager,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            Timber.i("🔔 SmartNotificationWorker: Starting daily notification check")
            
            // 1. Check jobs expiring in 24 hours
            checkExpiringJobs()
            
            // 2. Check pending applications (48+ hours)
            checkPendingApplications()
            
            // 3. Re-engage inactive workers (7+ days)
            reEngageInactiveWorkers()
            
            // 4. Re-engage inactive employers (30+ days)
            reEngageInactiveEmployers()
            
            Timber.i("🔔 SmartNotificationWorker: Completed successfully")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "🔔 SmartNotificationWorker: Error")
            Result.retry()
        }
    }
    
    /**
     * Check jobs expiring in next 24 hours and notify employers
     */
    private suspend fun checkExpiringJobs() {
        try {
            val now = System.currentTimeMillis()
            val tomorrow = now + (24 * 60 * 60 * 1000) // 24 hours from now
            
            val snapshot = firestore.collection("jobs")
                .whereEqualTo("status", "ACTIVE")
                .whereLessThan("expiresAt", tomorrow)
                .whereGreaterThan("expiresAt", now)
                .get()
                .await()
            
            Timber.d("🔔 Found ${snapshot.size()} jobs expiring in 24 hours")
            
            snapshot.documents.forEach { doc ->
                val jobId = doc.id
                val employerId = doc.getString("employerId") ?: return@forEach
                
                smartNotificationManager.notifyJobExpiringSoon(jobId, employerId)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking expiring jobs")
        }
    }
    
    /**
     * Check applications pending for 48+ hours and remind employers
     */
    private suspend fun checkPendingApplications() {
        try {
            val twoDaysAgo = System.currentTimeMillis() - (48 * 60 * 60 * 1000)
            
            // Get employers with pending applications
            val snapshot = firestore.collection("applications")
                .whereEqualTo("status", "PENDING")
                .whereLessThan("appliedAt", twoDaysAgo)
                .get()
                .await()
            
            // Group by employer
            val employerIds = snapshot.documents
                .mapNotNull { it.getString("employerId") }
                .distinct()
            
            Timber.d("🔔 Found ${employerIds.size} employers with pending applications")
            
            employerIds.forEach { employerId ->
                smartNotificationManager.notifyPendingApplications(employerId)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking pending applications")
        }
    }
    
    /**
     * Re-engage workers who haven't applied in 3+ days
     */
    private suspend fun reEngageInactiveWorkers() {
        try {
            val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000)
            
            // Get all workers
            val workersSnapshot = firestore.collection("users")
                .whereEqualTo("role", "WORKER")
                .get()
                .await()
            
            var reEngagedCount = 0
            
            workersSnapshot.documents.forEach { doc ->
                val workerId = doc.id
                
                // Check last application
                val lastAppSnapshot = firestore.collection("applications")
                    .whereEqualTo("workerId", workerId)
                    .orderBy("appliedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .await()
                
                if (lastAppSnapshot.isEmpty) {
                    // Never applied - re-engage
                    smartNotificationManager.reEngageInactiveWorker(workerId)
                    reEngagedCount++
                } else {
                    val lastAppliedAt = lastAppSnapshot.documents[0].getLong("appliedAt") ?: 0
                    if (lastAppliedAt < threeDaysAgo) {
                        // Inactive for 3+ days - re-engage
                        smartNotificationManager.reEngageInactiveWorker(workerId)
                        reEngagedCount++
                    }
                }
            }
            
            Timber.d("🔔 Re-engaged $reEngagedCount inactive workers")
        } catch (e: Exception) {
            Timber.e(e, "Error re-engaging inactive workers")
        }
    }
    
    /**
     * Re-engage employers who haven't posted in 15+ days
     */
    private suspend fun reEngageInactiveEmployers() {
        try {
            val fifteenDaysAgo = System.currentTimeMillis() - (15 * 24 * 60 * 60 * 1000)
            
            // Get all employers
            val employersSnapshot = firestore.collection("users")
                .whereEqualTo("role", "EMPLOYER")
                .get()
                .await()
            
            var reEngagedCount = 0
            
            employersSnapshot.documents.forEach { doc ->
                val employerId = doc.id
                
                // Check last job post
                val lastJobSnapshot = firestore.collection("jobs")
                    .whereEqualTo("employerId", employerId)
                    .orderBy("postedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .await()
                
                if (lastJobSnapshot.isEmpty) {
                    // Never posted - re-engage
                    smartNotificationManager.reEngageInactiveEmployer(employerId)
                    reEngagedCount++
                } else {
                    val lastPostedAt = lastJobSnapshot.documents[0].getLong("postedAt") ?: 0
                    if (lastPostedAt < fifteenDaysAgo) {
                        // Inactive for 15+ days - re-engage
                        smartNotificationManager.reEngageInactiveEmployer(employerId)
                        reEngagedCount++
                    }
                }
            }
            
            Timber.d("🔔 Re-engaged $reEngagedCount inactive employers")
        } catch (e: Exception) {
            Timber.e(e, "Error re-engaging inactive employers")
        }
    }
}
