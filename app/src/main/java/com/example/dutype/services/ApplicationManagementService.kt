package com.example.dutype.services

import timber.log.Timber
import com.example.dutype.models.JobApplication
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.StatusUpdate
import com.example.dutype.models.JobVacancyStatus
import com.example.dutype.models.ApplicationStats
import com.example.dutype.models.ApplicationAnalytics
import com.example.dutype.utils.RetryUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
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
    private val notificationService: NotificationService,
    private val workVerificationService: WorkVerificationService
) {
    
    private val applicationsCollection = "job_applications"
    
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
                
                val currentApplication = doc.toObject(JobApplication::class.java)
                    ?: return@retryWithBackoffResult Result.failure(Exception("Invalid application data"))
                
                val statusUpdate = StatusUpdate(
                    status = newStatus,
                    updatedBy = updatedBy,
                    notes = notes,
                    systemUpdate = false
                )
                
                val updatedApplication = currentApplication.copy(
                    status = newStatus,
                    statusHistory = currentApplication.statusHistory + statusUpdate,
                    updatedAt = System.currentTimeMillis(),
                    lastViewedByEmployer = if (updatedBy != currentApplication.workerId) 
                        System.currentTimeMillis() else currentApplication.lastViewedByEmployer
                )
                
                docRef.set(updatedApplication).await()
                
                // Send notification to worker about status change
                notificationService.sendApplicationStatusNotification(
                    updatedApplication, newStatus, updatedApplication.workerId
                )
                
                // Send hired notification when status is ACCEPTED
                if (newStatus == ApplicationStatus.ACCEPTED) {
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
            
            val currentApplication = doc.toObject(JobApplication::class.java)
                ?: return Result.failure(Exception("Invalid application data"))
            
            // Only update if status is PENDING
            if (currentApplication.status == ApplicationStatus.PENDING) {
                val statusUpdate = StatusUpdate(
                    status = ApplicationStatus.UNDER_REVIEW,
                    updatedBy = employerId,
                    notes = "Application opened by employer",
                    systemUpdate = false
                )
                
                val updatedApplication = currentApplication.copy(
                    status = ApplicationStatus.UNDER_REVIEW,
                    statusHistory = currentApplication.statusHistory + statusUpdate,
                    updatedAt = System.currentTimeMillis(),
                    lastViewedByEmployer = System.currentTimeMillis()
                )
                
                docRef.set(updatedApplication).await()
                
                notificationService.sendApplicationStatusNotification(
                    updatedApplication, 
                    ApplicationStatus.UNDER_REVIEW, 
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
     * Checks vacancy limit before accepting and generates work verification code
     */
    suspend fun acceptApplication(applicationId: String, employerId: String): Result<JobApplication> {
        return try {
            val docRef = firestore.collection(applicationsCollection).document(applicationId)
            val doc = docRef.get().await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("Application not found"))
            }
            
            val currentApplication = doc.toObject(JobApplication::class.java)
                ?: return Result.failure(Exception("Invalid application data"))
            
            // Check if vacancies are still available
            val canAcceptResult = canAcceptMoreApplications(currentApplication.jobId)
            if (canAcceptResult.isFailure) {
                return Result.failure(canAcceptResult.exceptionOrNull() ?: Exception("Failed to check vacancy status"))
            }
            
            if (canAcceptResult.getOrNull() != true) {
                return Result.failure(Exception("All vacancies for this job have been filled. Cannot accept more applications."))
            }
            
            val statusUpdate = StatusUpdate(
                status = ApplicationStatus.ACCEPTED,
                updatedBy = employerId,
                notes = "Application accepted by employer",
                systemUpdate = false
            )
            
            val updatedApplication = currentApplication.copy(
                status = ApplicationStatus.ACCEPTED,
                statusHistory = currentApplication.statusHistory + statusUpdate,
                updatedAt = System.currentTimeMillis(),
                lastViewedByEmployer = System.currentTimeMillis()
            )
            
            docRef.set(updatedApplication).await()
            
            // Generate Work Start Verification Code
            try {
                workVerificationService.generateVerification(
                    jobId = currentApplication.jobId,
                    applicationId = applicationId,
                    workerId = currentApplication.workerId,
                    employerId = employerId,
                    workerName = currentApplication.workerName,
                    jobTitle = currentApplication.jobTitle,
                    employerName = currentApplication.companyName
                )
                Timber.i("🔐 WORK VERIFICATION: Generated verification code for application $applicationId")
            } catch (e: Exception) {
                Timber.e(e, "🔐 WORK VERIFICATION: Failed to generate verification code")
            }
            
            // Send notification to worker
            notificationService.sendApplicationStatusNotification(
                updatedApplication, 
                ApplicationStatus.ACCEPTED, 
                updatedApplication.workerId
            )
            
            // Update job vacancy status if needed
            updateJobVacancyStatusIfNeeded(currentApplication.jobId)
            
            Result.success(updatedApplication)
        } catch (e: Exception) {
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
            
            val currentApplication = doc.toObject(JobApplication::class.java)
                ?: return Result.failure(Exception("Invalid application data"))
            
            val statusUpdate = StatusUpdate(
                status = ApplicationStatus.REJECTED,
                updatedBy = employerId,
                notes = reason ?: "Application rejected by employer",
                systemUpdate = false
            )
            
            val updatedApplication = currentApplication.copy(
                status = ApplicationStatus.REJECTED,
                statusHistory = currentApplication.statusHistory + statusUpdate,
                updatedAt = System.currentTimeMillis(),
                lastViewedByEmployer = System.currentTimeMillis()
            )
            
            docRef.set(updatedApplication).await()
            
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
            val applicationRef = firestore.collection(applicationsCollection).document(applicationId)
            applicationRef.update("lastViewedByEmployer", System.currentTimeMillis()).await()
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
                
                val currentApplication = doc.toObject(JobApplication::class.java)
                    ?: return@retryWithBackoffResult Result.failure(Exception("Invalid application data"))
                
                val updatedApplication = currentApplication.copy(
                    employerNotes = notes,
                    updatedAt = System.currentTimeMillis(),
                    lastViewedByEmployer = System.currentTimeMillis()
                )
                
                docRef.set(updatedApplication).await()
                Result.success(updatedApplication)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    
    // ==================== VACANCY MANAGEMENT ====================
    
    /**
     * Check if employer can accept more applications for a job
     */
    suspend fun canAcceptMoreApplications(jobId: String): Result<Boolean> {
        return try {
            val jobDoc = firestore.collection("jobs").document(jobId).get().await()
            if (!jobDoc.exists()) {
                return Result.failure(Exception("Job not found"))
            }
            
            val jobData = jobDoc.data ?: return Result.failure(Exception("Invalid job data"))
            val requiredVacancies = (jobData["vacancies"] as? Long)?.toInt() ?: 1
            
            val vacancyStatus = jobData["vacancyStatus"] as? String
            if (vacancyStatus == JobVacancyStatus.FILLED.name) {
                return Result.success(false)
            }
            
            val acceptedApplications = firestore.collection(applicationsCollection)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("status", ApplicationStatus.ACCEPTED.name)
                .get()
                .await()
            
            val acceptedCount = acceptedApplications.size()
            Timber.d("📊 canAcceptMoreApplications - jobId: $jobId, vacancies: $requiredVacancies, accepted: $acceptedCount")
            
            Result.success(acceptedCount < requiredVacancies)
        } catch (e: Exception) {
            Timber.e(e, "Error checking vacancy availability")
            Result.failure(e)
        }
    }
    
    /**
     * Get remaining vacancies for a job
     */
    suspend fun getRemainingVacancies(jobId: String): Result<Int> {
        return try {
            val jobDoc = firestore.collection("jobs").document(jobId).get().await()
            if (!jobDoc.exists()) {
                return Result.failure(Exception("Job not found"))
            }
            
            val jobData = jobDoc.data ?: return Result.failure(Exception("Invalid job data"))
            val requiredVacancies = (jobData["vacancies"] as? Long)?.toInt() ?: 1
            
            val acceptedApplications = firestore.collection(applicationsCollection)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("status", ApplicationStatus.ACCEPTED.name)
                .get()
                .await()
            
            val acceptedCount = acceptedApplications.size()
            val remaining = (requiredVacancies - acceptedCount).coerceAtLeast(0)
            
            Result.success(remaining)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get job vacancy status
     */
    suspend fun getJobVacancyStatus(jobId: String): Result<JobVacancyStatus> {
        return try {
            val doc = firestore.collection("jobs").document(jobId).get().await()
            if (doc.exists()) {
                val statusString = doc.getString("vacancyStatus") ?: JobVacancyStatus.OPEN.name
                val status = try {
                    JobVacancyStatus.valueOf(statusString)
                } catch (e: Exception) {
                    JobVacancyStatus.OPEN
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
     * Update job vacancy status based on accepted applications
     */
    private suspend fun updateJobVacancyStatusIfNeeded(jobId: String) {
        try {
            val jobDoc = firestore.collection("jobs").document(jobId).get().await()
            if (!jobDoc.exists()) return
            
            val jobData = jobDoc.data ?: return
            val requiredVacancies = jobData["vacancies"] as? Long ?: 1L
            
            val acceptedApplications = firestore.collection(applicationsCollection)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("status", ApplicationStatus.ACCEPTED.name)
                .whereEqualTo("active", true)
                .get()
                .await()
            
            val acceptedCount = acceptedApplications.size()
            
            if (acceptedCount >= requiredVacancies) {
                firestore.collection("jobs")
                    .document(jobId)
                    .update(
                        "vacancyStatus", JobVacancyStatus.FILLED.name,
                        "updatedAt", System.currentTimeMillis()
                    )
                    .await()
                
                val allApplications = firestore.collection(applicationsCollection)
                    .whereEqualTo("jobId", jobId)
                    .whereEqualTo("active", true)
                    .get()
                    .await()
                
                allApplications.documents.forEach { doc ->
                    doc.reference.update("isFilled", true).await()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating job vacancy status")
        }
    }

    
    // ==================== STATISTICS & ANALYTICS ====================
    
    /**
     * Get application statistics for a worker
     */
    suspend fun getWorkerApplicationStats(workerId: String): Result<ApplicationStats> {
        return try {
            val snapshot = firestore.collection(applicationsCollection)
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("active", true)
                .get()
                .await()
            
            val applications = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(JobApplication::class.java)?.copy(applicationId = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
            
            val stats = ApplicationStats(
                totalApplications = applications.size,
                pendingApplications = applications.count { it.status == ApplicationStatus.PENDING },
                reviewedApplications = applications.count { it.status == ApplicationStatus.UNDER_REVIEW },
                shortlistedApplications = applications.count { it.status == ApplicationStatus.ACCEPTED },
                rejectedApplications = applications.count { it.status == ApplicationStatus.REJECTED },
                hiredApplications = applications.count { it.status == ApplicationStatus.ACCEPTED },
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
    suspend fun getEmployerApplicationStats(employerId: String): Result<ApplicationStats> {
        return try {
            val snapshot = firestore.collection(applicationsCollection)
                .whereEqualTo("employerId", employerId)
                .get()
                .await()
            
            val applications = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(JobApplication::class.java)?.copy(applicationId = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
            
            val stats = ApplicationStats(
                totalApplications = applications.size,
                pendingApplications = applications.count { it.status == ApplicationStatus.PENDING },
                reviewedApplications = applications.count { it.status == ApplicationStatus.UNDER_REVIEW },
                shortlistedApplications = applications.count { it.status == ApplicationStatus.ACCEPTED },
                rejectedApplications = applications.count { it.status == ApplicationStatus.REJECTED },
                hiredApplications = applications.count { it.status == ApplicationStatus.ACCEPTED },
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
    suspend fun getApplicationAnalytics(employerId: String): Result<ApplicationAnalytics> {
        return try {
            val snapshot = firestore.collection(applicationsCollection)
                .whereEqualTo("employerId", employerId)
                .get()
                .await()
            
            val applications = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(JobApplication::class.java)?.copy(applicationId = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
            
            val analytics = ApplicationAnalytics(
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
                .whereEqualTo("active", true)
                .orderBy("appliedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val applications = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(JobApplication::class.java)?.copy(applicationId = doc.id)
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
