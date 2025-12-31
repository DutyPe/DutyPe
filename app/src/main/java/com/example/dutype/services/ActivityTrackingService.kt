package com.example.dutype.services

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Activity Tracking Service - P1 FIX #7
 * 
 * Logs user activities with IP address for fraud detection.
 * Calls Cloud Function to track and analyze IP patterns.
 */
@Singleton
class ActivityTrackingService @Inject constructor() {
    
    private val functions = FirebaseFunctions.getInstance()
    
    // ==========================================
    // ACTIVITY TYPES
    // ==========================================
    
    enum class ActivityType {
        LOGIN,
        REGISTRATION,
        JOB_POST,
        JOB_VIEW,
        APPLICATION_SUBMIT,
        PROFILE_UPDATE,
        CHAT_MESSAGE,
        REPORT_SUBMIT
    }
    
    // ==========================================
    // TRACKING METHODS
    // ==========================================
    
    /**
     * Log a user activity with IP tracking
     * Called from various parts of the app
     */
    suspend fun logActivity(
        action: ActivityType,
        metadata: Map<String, Any> = emptyMap()
    ): Result<Unit> {
        return try {
            Timber.d("📍 ACTIVITY: Logging ${action.name}")
            
            val data = hashMapOf(
                "action" to action.name,
                "metadata" to metadata
            )
            
            functions
                .getHttpsCallable("logUserActivity")
                .call(data)
                .await()
            
            Timber.d("📍 ACTIVITY: ✅ Logged ${action.name}")
            Result.success(Unit)
            
        } catch (e: Exception) {
            // Don't fail the main operation if tracking fails
            Timber.e(e, "📍 ACTIVITY: Error logging activity (non-fatal)")
            Result.failure(e)
        }
    }
    
    // ==========================================
    // CONVENIENCE METHODS
    // ==========================================
    
    /**
     * Track login event
     */
    suspend fun trackLogin(loginMethod: String = "PHONE") {
        logActivity(
            ActivityType.LOGIN,
            mapOf("method" to loginMethod)
        )
    }
    
    /**
     * Track registration event
     */
    suspend fun trackRegistration(role: String) {
        logActivity(
            ActivityType.REGISTRATION,
            mapOf("role" to role)
        )
    }
    
    /**
     * Track job posting
     */
    suspend fun trackJobPost(jobId: String, category: String) {
        logActivity(
            ActivityType.JOB_POST,
            mapOf(
                "jobId" to jobId,
                "category" to category
            )
        )
    }
    
    /**
     * Track job view
     */
    suspend fun trackJobView(jobId: String) {
        logActivity(
            ActivityType.JOB_VIEW,
            mapOf("jobId" to jobId)
        )
    }
    
    /**
     * Track application submission
     */
    suspend fun trackApplicationSubmit(jobId: String, applicationId: String) {
        logActivity(
            ActivityType.APPLICATION_SUBMIT,
            mapOf(
                "jobId" to jobId,
                "applicationId" to applicationId
            )
        )
    }
    
    /**
     * Track profile update
     */
    suspend fun trackProfileUpdate(fieldsUpdated: List<String>) {
        logActivity(
            ActivityType.PROFILE_UPDATE,
            mapOf("fields" to fieldsUpdated)
        )
    }
    
    /**
     * Track chat message sent
     */
    suspend fun trackChatMessage(conversationId: String) {
        logActivity(
            ActivityType.CHAT_MESSAGE,
            mapOf("conversationId" to conversationId)
        )
    }
    
    /**
     * Track report submission
     */
    suspend fun trackReportSubmit(reportType: String, targetId: String) {
        logActivity(
            ActivityType.REPORT_SUBMIT,
            mapOf(
                "reportType" to reportType,
                "targetId" to targetId
            )
        )
    }
}
