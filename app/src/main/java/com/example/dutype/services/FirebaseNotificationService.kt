package com.example.dutype.services

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for sending Firebase Cloud Messaging (FCM) notifications
 * This service handles sending push notifications to users
 */
@Singleton
class FirebaseNotificationService @Inject constructor() {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseMessaging = FirebaseMessaging.getInstance()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Send a notification to a specific user
     */
    fun sendNotificationToUser(
        userId: String,
        title: String,
        message: String,
        action: String = "",
        jobId: String = "",
        applicationId: String = ""
    ) {
        coroutineScope.launch {
            try {
                // Get user's FCM token from Firestore
                val userDoc = firestore.collection("users").document(userId).get().await()
                val fcmToken = userDoc.getString("fcmToken")
                
                if (fcmToken != null) {
                    // Send notification via FCM
                    sendFCMNotification(
                        fcmToken = fcmToken,
                        title = title,
                        message = message,
                        action = action,
                        jobId = jobId,
                        applicationId = applicationId,
                        userId = userId
                    )
                } else {
                    Log.w("FirebaseNotificationService", "No FCM token found for user: $userId")
                }
            } catch (e: Exception) {
                Log.e("FirebaseNotificationService", "Error sending notification to user $userId", e)
            }
        }
    }
    
    /**
     * Send FCM notification using HTTP API
     * Note: In production, you would use your server to send FCM messages
     * This is a simplified version for demo purposes
     */
    private suspend fun sendFCMNotification(
        fcmToken: String,
        title: String,
        message: String,
        action: String,
        jobId: String,
        applicationId: String,
        userId: String
    ) {
        try {
            // Create notification payload
            val notificationPayload = mapOf(
                "to" to fcmToken,
                "notification" to mapOf(
                    "title" to title,
                    "body" to message,
                    "icon" to "ic_notification",
                    "sound" to "default"
                ),
                "data" to mapOf(
                    "title" to title,
                    "message" to message,
                    "action" to action,
                    "jobId" to jobId,
                    "applicationId" to applicationId,
                    "userId" to userId,
                    "notificationId" to System.currentTimeMillis().toString()
                )
            )
            
            // In a real app, you would send this to your server
            // which would then send the FCM message
            // For now, we'll just log it
            Log.d("FirebaseNotificationService", "Would send FCM notification: $notificationPayload")
            
        } catch (e: Exception) {
            Log.e("FirebaseNotificationService", "Error sending FCM notification", e)
        }
    }
    
    /**
     * Save FCM token for current user
     */
    fun saveFCMToken(userId: String) {
        coroutineScope.launch {
            try {
                firebaseMessaging.token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        Log.d("FirebaseNotificationService", "FCM Token: $token")
                        
                        // Save token to Firestore
                        firestore.collection("users").document(userId)
                            .update("fcmToken", token)
                            .addOnSuccessListener {
                                Log.d("FirebaseNotificationService", "FCM token saved for user: $userId")
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirebaseNotificationService", "Error saving FCM token", e)
                            }
                    } else {
                        Log.e("FirebaseNotificationService", "Error getting FCM token", task.exception)
                    }
                }
            } catch (e: Exception) {
                Log.e("FirebaseNotificationService", "Error saving FCM token for user $userId", e)
            }
        }
    }
    
    /**
     * Send notification when worker applies for job
     */
    fun sendJobApplicationNotification(
        employerId: String,
        workerName: String,
        jobTitle: String,
        jobId: String,
        applicationId: String
    ) {
        sendNotificationToUser(
            userId = employerId,
            title = "New Job Application",
            message = "$workerName applied for your job: $jobTitle",
            action = "view_application",
            jobId = jobId,
            applicationId = applicationId
        )
    }
    
    /**
     * Send notification when application status changes
     */
    fun sendApplicationStatusNotification(
        workerId: String,
        jobTitle: String,
        status: String,
        jobId: String,
        applicationId: String
    ) {
        val title = when (status.lowercase()) {
            "shortlisted" -> "Application Shortlisted"
            "rejected" -> "Application Update"
            "accepted" -> "Application Accepted"
            "interview_scheduled" -> "Interview Scheduled"
            else -> "Application Update"
        }
        
        val message = when (status.lowercase()) {
            "shortlisted" -> "Congratulations! Your application for $jobTitle has been shortlisted"
            "rejected" -> "Your application for $jobTitle was not selected this time"
            "accepted" -> "Great news! Your application for $jobTitle has been accepted"
            "interview_scheduled" -> "An interview has been scheduled for your application to $jobTitle"
            else -> "Your application for $jobTitle status has been updated to $status"
        }
        
        sendNotificationToUser(
            userId = workerId,
            title = title,
            message = message,
            action = "view_application",
            jobId = jobId,
            applicationId = applicationId
        )
    }
    
    /**
     * Send notification when new job is posted
     */
    fun sendNewJobNotification(
        workerId: String,
        jobTitle: String,
        companyName: String,
        jobId: String
    ) {
        sendNotificationToUser(
            userId = workerId,
            title = "New Job Alert",
            message = "New job posted by $companyName: $jobTitle",
            action = "view_job",
            jobId = jobId
        )
    }
    
    /**
     * Send notification when job is paused
     */
    fun sendJobPausedNotification(
        workerId: String,
        jobTitle: String,
        jobId: String
    ) {
        sendNotificationToUser(
            userId = workerId,
            title = "Job Paused",
            message = "The job '$jobTitle' has been paused by the employer",
            action = "view_job",
            jobId = jobId
        )
    }
}
