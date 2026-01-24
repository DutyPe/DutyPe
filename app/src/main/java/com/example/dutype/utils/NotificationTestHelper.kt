package com.example.dutype.utils

import com.example.dutype.models.JobListing
import com.example.dutype.services.SmartNotificationManager
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification Test Helper
 * 
 * Utility class for testing smart notifications in development/staging
 * Provides methods to trigger all notification types manually
 * 
 * Usage: Inject this class and call test methods from debug screens
 */
@Singleton
class NotificationTestHelper @Inject constructor(
    private val smartNotificationManager: SmartNotificationManager
) {
    
    /**
     * Test location-based job alert
     */
    fun testLocationBasedAlert(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val testJob = JobListing(
                    id = "test_job_123",
                    jobId = "test_job_123",
                    employerId = "test_employer",
                    title = "Test Plumber Job",
                    location = "Delhi, India",
                    latitude = 28.6139,
                    longitude = 77.2090,
                    payAmount = "500",
                    payType = "per day"
                )
                
                smartNotificationManager.notifyNearbyWorkersAboutNewJob(testJob)
                Timber.i("🧪 TEST: Location-based alert triggered")
            } catch (e: Exception) {
                Timber.e(e, "🧪 TEST: Failed to trigger location-based alert")
            }
        }
    }
    
    /**
     * Test job expiry reminder
     */
    fun testJobExpiryReminder(jobId: String, employerId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                smartNotificationManager.notifyJobExpiringSoon(jobId, employerId)
                Timber.i("🧪 TEST: Job expiry reminder triggered")
            } catch (e: Exception) {
                Timber.e(e, "🧪 TEST: Failed to trigger job expiry reminder")
            }
        }
    }
    
    /**
     * Test pending applications reminder
     */
    fun testPendingApplicationsReminder(employerId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                smartNotificationManager.notifyPendingApplications(employerId)
                Timber.i("🧪 TEST: Pending applications reminder triggered")
            } catch (e: Exception) {
                Timber.e(e, "🧪 TEST: Failed to trigger pending applications reminder")
            }
        }
    }
    
    /**
     * Test inactive worker re-engagement
     */
    fun testInactiveWorkerReEngagement(workerId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                smartNotificationManager.reEngageInactiveWorker(workerId)
                Timber.i("🧪 TEST: Inactive worker re-engagement triggered")
            } catch (e: Exception) {
                Timber.e(e, "🧪 TEST: Failed to trigger inactive worker re-engagement")
            }
        }
    }
    
    /**
     * Test inactive employer re-engagement
     */
    fun testInactiveEmployerReEngagement(employerId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                smartNotificationManager.reEngageInactiveEmployer(employerId)
                Timber.i("🧪 TEST: Inactive employer re-engagement triggered")
            } catch (e: Exception) {
                Timber.e(e, "🧪 TEST: Failed to trigger inactive employer re-engagement")
            }
        }
    }
    
    /**
     * Test profile milestone notification
     */
    fun testProfileMilestone(userId: String, percentage: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                smartNotificationManager.notifyProfileMilestone(userId, percentage)
                Timber.i("🧪 TEST: Profile milestone $percentage% triggered")
            } catch (e: Exception) {
                Timber.e(e, "🧪 TEST: Failed to trigger profile milestone")
            }
        }
    }
    
    /**
     * Test referral milestone notification
     */
    fun testReferralMilestone(userId: String, referralCount: Int, rewardAmount: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                smartNotificationManager.notifyReferralMilestone(userId, referralCount, rewardAmount)
                Timber.i("🧪 TEST: Referral milestone $referralCount triggered")
            } catch (e: Exception) {
                Timber.e(e, "🧪 TEST: Failed to trigger referral milestone")
            }
        }
    }
    
    /**
     * Test all notification types for current user
     */
    fun testAllNotifications(userId: String, userRole: String) {
        Timber.i("🧪 TEST: Testing all notification types for user $userId")
        
        // Test profile milestones
        testProfileMilestone(userId, 25)
        
        // Test referral milestone
        testReferralMilestone(userId, 5, 50)
        
        // Role-specific tests
        when (userRole) {
            "WORKER" -> {
                testLocationBasedAlert(userId)
                testInactiveWorkerReEngagement(userId)
            }
            "EMPLOYER" -> {
                testPendingApplicationsReminder(userId)
                testInactiveEmployerReEngagement(userId)
            }
        }
        
        Timber.i("🧪 TEST: All notification tests triggered")
    }
}
