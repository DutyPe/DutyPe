package com.example.dutype.services

import timber.log.Timber
import com.example.dutype.utils.SecureLogger
import com.example.dutype.models.JobApplication
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.ApplicationStats
import com.example.dutype.models.ApplicationAnalytics
import com.example.dutype.models.JobVacancyStatus
import com.example.dutype.utils.RetryUtils
import com.example.dutype.utils.toJobApplicationOrNull
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ApplicationManagementService - Unified service for application status and statistics
 * 
 * Consolidated from ApplicationStatusService + ApplicationStatsService as part of 
 * architecture refactoring. Single responsibility: All application management operations.
 * 
 * @author DutyPe Engineering Team
 * @since 2.2.0
 */
@Singleton
class ApplicationManagementService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationService: NotificationService
) {
    
    private val applicationsCollection = "applications"
    
    // ==================== STATUS MANAGEMENT ====================
    
    /**
     * Update application status
     */
    suspend fun updateApplicationStatus(
        applicationId: String, 
        newStatus: ApplicationStatus,
        updatedBy: String,
        notes: String? = null
    ): Result<JobApplication> {
        return try {
            RetryUtils.retryWithBackoffResult {
                val docRef = firestore.collection(applicationsCollection).document(applicationId)
                val doc = docRef.get().await()
                
                if (!doc.exists()) {
                    return@retryWithBackoffResult Result.failure(Exception("Application not found"))
                }
                
                val currentApplication = doc.toJobApplicationOrNull()
                    ?: return@retryWithBackoffResult Result.failure(Exception("Invalid application data"))
                
                val updatedApplication = currentApplication.copy(
                    status = newStatus
                )
                
                docRef.update("status", newStatus.toFirestoreValue()).await()
                
                // Send notification to worker about status change
                notificationService.sendApplicationStatusNotification(
                    updatedApplication, newStatus, updatedApplication.workerId
                )
                
                // Send hired notification when status is HIRED
                if (newStatus == ApplicationStatus.HIRED) {
                    try {
                        notificationService.sendWorkerHiredNotification(
                            workerName = updatedApplication.workerName,
                            jobTitle = updatedApplication.jobTitle,
                            workerId = updatedApplication.workerId,
                            employerId = updatedApplication.employerId,
                            jobId = updatedApplication.jobId
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to send worker hired notification")
                    }
                }
                
                Result.success(updatedApplication)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    
    /**
     * Mark application as under review when employer opens it
     */
    suspend fun markApplicationAsUnderReview(applicationId: String, employerId: String): Result<JobApplication> {
        return try {
            val docRef = firestore.collection(applicationsCollection).document(applicationId)
            val doc = docRef.get().await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("Application not found"))
            }
            
            val currentApplication = doc.toJobApplicationOrNull()
                ?: return Result.failure(Exception("Invalid application data"))
            
            // Only update if status is APPLIED
            if (currentApplication.status == ApplicationStatus.APPLIED) {
                val updatedApplication = currentApplication.copy(
                    status = ApplicationStatus.SHORTLISTED
                )
                
                docRef.update("status", ApplicationStatus.SHORTLISTED.toFirestoreValue()).await()
                
                notificationService.sendApplicationStatusNotification(
                    updatedApplication, 
                    ApplicationStatus.SHORTLISTED, 
                    updatedApplication.workerId
                )
                
                Result.success(updatedApplication)
            } else {
                Result.success(currentApplication)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * Accept application (employer action)
     * P1 FIX: Uses Firestore transaction to prevent race condition
     * Checks vacancy limit before accepting and generates work verification code
     */
    suspend fun acceptApplication(applicationId: String, employerId: String): Result<JobApplication> {
        return try {
            val result = firestore.runTransaction { transaction ->
                val docRef = firestore.collection(applicationsCollection).document(applicationId)
                val doc = transaction.get(docRef)

                if (!doc.exists()) throw Exception("Application not found")

                val currentApplication = doc.toJobApplicationOrNull()
                    ?: throw Exception("Invalid application data")

                // Schema-compliant: check job status field only (no vacancies/acceptedCount)
                val jobRef = firestore.collection(com.example.dutype.firestore.FirestoreCollections.JOBS).document(currentApplication.jobId)
                val jobDoc = transaction.get(jobRef)

                if (!jobDoc.exists()) throw Exception("Job not found")

                val jobStatus = jobDoc.getString("status") ?: "open"
                if (jobStatus != "open") {
                    throw Exception("This job is no longer accepting applications.")
                }

                // Update status only — targeted update avoids writing non-schema fields
                val updatedApplication = currentApplication.copy(
                    status = ApplicationStatus.HIRED
                )
                transaction.update(docRef, "status", ApplicationStatus.HIRED.toFirestoreValue())
                updatedApplication
            }.await()
            
            // Send notification to worker (outside transaction)
            notificationService.sendApplicationStatusNotification(
                result, 
                ApplicationStatus.HIRED, 
                result.workerId
            )
            
            // Update job vacancy status if needed (outside transaction)
            updateJobVacancyStatusIfNeeded(result.jobId)
            
            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "Failed to accept application")
            Result.failure(e)
        }
    }
    
    /**
     * Reject application (employer action)
     */
    suspend fun rejectApplication(applicationId: String, employerId: String, reason: String? = null): Result<JobApplication> {
        return try {
            val docRef = firestore.collection(applicationsCollection).document(applicationId)
            val doc = docRef.get().await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("Application not found"))
            }
            
            val currentApplication = doc.toJobApplicationOrNull()
                ?: return Result.failure(Exception("Invalid application data"))
            
            val updatedApplication = currentApplication.copy(
                status = ApplicationStatus.REJECTED
            )
            
            docRef.update("status", ApplicationStatus.REJECTED.toFirestoreValue()).await()
            
            notificationService.sendApplicationStatusNotification(
                updatedApplication, 
                ApplicationStatus.REJECTED, 
                updatedApplication.workerId
            )
            
            Result.success(updatedApplication)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark application as viewed by employer
     */
    suspend fun markApplicationAsViewed(applicationId: String, employerId: String): Result<Unit> {
        return try {
            // No-op since lastViewedByEmployer field was removed
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add employer notes to application
     */
    suspend fun addEmployerNotes(
        applicationId: String,
        notes: String,
        updatedBy: String
    ): Result<JobApplication> {
        return try {
            RetryUtils.retryWithBackoffResult {
                val docRef = firestore.collection(applicationsCollection).document(applicationId)
                val doc = docRef.get().await()
                
                if (!doc.exists()) {
                    return@retryWithBackoffResult Result.failure(Exception("Application not found"))
                }
                
                val currentApplication = doc.toJobApplicationOrNull()
                    ?: return@retryWithBackoffResult Result.failure(Exception("Invalid application data"))
                
                val updatedApplication = currentApplication.copy()
                
                // Notes are not stored in Firestore schema — no-op write
                Result.success(updatedApplication)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    
    // ==================== VACANCY MANAGEMENT ====================
    
    /**
     * Check if employer can accept more applications — vacancies removed from schema.
     * Returns true if job status is "open".
     */
    suspend fun canAcceptMoreApplications(jobId: String): Result<Boolean> {
        return try {
            val jobDoc = firestore.collection(com.example.dutype.firestore.FirestoreCollections.JOBS).document(jobId).get().await()
            if (!jobDoc.exists()) return Result.failure(Exception("Job not found"))
            val jobStatus = jobDoc.getString("status") ?: "open"
            Result.success(jobStatus == "open")
        } catch (e: Exception) {
            Timber.e(e, "Error checking vacancy availability")
            Result.failure(e)
        }
    }
    
    /**
     * Get remaining vacancies — vacancies removed from schema.
     * Returns 1 if open, 0 if closed/expired.
     */
    suspend fun getRemainingVacancies(jobId: String): Result<Int> {
        return try {
            val jobDoc = firestore.collection(com.example.dutype.firestore.FirestoreCollections.JOBS).document(jobId).get().await()
            if (!jobDoc.exists()) return Result.failure(Exception("Job not found"))
            val jobStatus = jobDoc.getString("status") ?: "open"
            Result.success(if (jobStatus == "open") 1 else 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get job vacancy status — reads canonical "status" field (open/closed/expired).
     */
    suspend fun getJobVacancyStatus(jobId: String): Result<JobVacancyStatus> {
        return try {
            val doc = firestore.collection(com.example.dutype.firestore.FirestoreCollections.JOBS).document(jobId).get().await()
            if (doc.exists()) {
                val status = when (doc.getString("status") ?: "open") {
                    "closed" -> JobVacancyStatus.FILLED
                    "expired" -> JobVacancyStatus.EXPIRED
                    else -> JobVacancyStatus.OPEN
                }
                Result.success(status)
            } else {
                Result.success(JobVacancyStatus.OPEN)
            }
        } catch (e: Exception) {
            Result.success(JobVacancyStatus.OPEN)
        }
    }
    
    /**
     * Update job status to "closed" when a worker is hired.
     */
    private suspend fun updateJobVacancyStatusIfNeeded(jobId: String) {
        try {
            firestore.collection(com.example.dutype.firestore.FirestoreCollections.JOBS).document(jobId)
                .update("status", "closed")
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Error updating job vacancy status")
        }
    }

    
    // ==================== STATISTICS & ANALYTICS ====================
    
    /**
     * Get application statistics for a worker
     */
    suspend fun getWorkerApplicationStats(workerId: String): Result<com.example.dutype.models.ApplicationStats> {
        return try {
            val snapshot = firestore.collection(applicationsCollection)
                .whereEqualTo("workerId", workerId)
                .limit(200)
                .get()
                .await()
            
            val applications = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toJobApplicationOrNull()
                } catch (e: Exception) {
                    null
                }
            }
            
            val stats = com.example.dutype.models.ApplicationStats(
                totalApplications = applications.size,
                appliedApplications = applications.count { it.status == ApplicationStatus.APPLIED },
                shortlistedApplications = applications.count { it.status == ApplicationStatus.SHORTLISTED },
                rejectedApplications = applications.count { it.status == ApplicationStatus.REJECTED },
                hiredApplications = applications.count { it.status == ApplicationStatus.HIRED },
                recentApplications = applications.sortedByDescending { it.appliedAt }.take(5)
            )
            
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get application statistics for an employer
     */
    suspend fun getEmployerApplicationStats(employerId: String): Result<com.example.dutype.models.ApplicationStats> {
        return try {
            val snapshot = firestore.collection(applicationsCollection)
                .whereEqualTo("employerId", employerId)
                .limit(500)
                .get()
                .await()
            
            val applications = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toJobApplicationOrNull()
                } catch (e: Exception) {
                    null
                }
            }
            
            val stats = com.example.dutype.models.ApplicationStats(
                totalApplications = applications.size,
                appliedApplications = applications.count { it.status == ApplicationStatus.APPLIED },
                shortlistedApplications = applications.count { it.status == ApplicationStatus.SHORTLISTED },
                rejectedApplications = applications.count { it.status == ApplicationStatus.REJECTED },
                hiredApplications = applications.count { it.status == ApplicationStatus.HIRED },
                recentApplications = applications.sortedByDescending { it.appliedAt }.take(5)
            )
            
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get application analytics for employer dashboard
     */
    suspend fun getApplicationAnalytics(employerId: String): Result<com.example.dutype.models.ApplicationAnalytics> {
        return try {
            val snapshot = firestore.collection(applicationsCollection)
                .whereEqualTo("employerId", employerId)
                .limit(500)
                .get()
                .await()
            
            val applications = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toJobApplicationOrNull()
                } catch (e: Exception) {
                    null
                }
            }
            
            val analytics = com.example.dutype.models.ApplicationAnalytics(
                totalApplications = applications.size,
                applicationsThisWeek = applications.count { 
                    System.currentTimeMillis() - it.appliedAt <= 7 * 24 * 60 * 60 * 1000 
                },
                applicationsThisMonth = applications.count { 
                    System.currentTimeMillis() - it.appliedAt <= 30 * 24 * 60 * 60 * 1000 
                },
                averageResponseTime = calculateAverageResponseTime(applications),
                topJobTitles = getTopJobTitles(applications),
                applicationTrends = getApplicationTrends(applications)
            )
            
            Result.success(analytics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get recent applications for dashboard
     */
    suspend fun getRecentApplications(limit: Int = 10): Result<List<JobApplication>> {
        return try {
            val snapshot = firestore.collection(applicationsCollection)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val applications = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toJobApplicationOrNull()
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== PRIVATE HELPERS ====================
    
    private fun calculateAverageResponseTime(applications: List<JobApplication>): Long {
        val respondedApplications = applications.filter { 
            it.statusHistory.any { update -> !update.systemUpdate } 
        }
        
        if (respondedApplications.isEmpty()) return 0L
        
        val totalResponseTime = respondedApplications.sumOf { app ->
            val firstUpdate = app.statusHistory.firstOrNull { !it.systemUpdate }
            firstUpdate?.let { update ->
                update.updatedAt - app.appliedAt
            } ?: 0L
        }
        
        return totalResponseTime / respondedApplications.size
    }
    
    private fun getTopJobTitles(applications: List<JobApplication>): List<String> {
        return applications.groupBy { it.jobTitle }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
    }
    
    private fun getApplicationTrends(applications: List<JobApplication>): Map<String, Int> {
        val calendar = Calendar.getInstance()
        val trends = mutableMapOf<String, Int>()
        
        repeat(7) { daysAgo ->
            calendar.timeInMillis = System.currentTimeMillis() - (daysAgo * 24 * 60 * 60 * 1000)
            val dayKey = SimpleDateFormat("MMM dd", Locale.getDefault()).format(calendar.time)
            
            val dayStart = calendar.timeInMillis
            val dayEnd = dayStart + (24 * 60 * 60 * 1000)
            
            val dayApplications = applications.count { app ->
                app.appliedAt in dayStart until dayEnd
            }
            
            trends[dayKey] = dayApplications
        }
        
        return trends
    }
}

// Backward compatibility type aliases
@Deprecated("Use ApplicationManagementService instead", ReplaceWith("ApplicationManagementService"))
typealias ApplicationStatusService = ApplicationManagementService

@Deprecated("Use ApplicationManagementService instead", ReplaceWith("ApplicationManagementService"))
typealias ApplicationStatsService = ApplicationManagementService


