package com.example.partimes.services

import com.example.partimes.models.JobApplication
import com.example.partimes.models.ApplicationStatus
import com.example.partimes.models.DocumentAttachment
import com.example.partimes.models.StatusUpdate
import com.example.partimes.models.ApplicationStats
import com.example.partimes.services.ProfileCompletionService
import com.example.partimes.state.ApplicationStateManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Professional Job Application Service
 * Enterprise-level job application management with 30+ years of Android development experience
 * Handles all job application operations with Firestore
 */
@Singleton
class JobApplicationService @Inject constructor(
    private val notificationService: NotificationService,
    private val profileCompletionService: ProfileCompletionService,
    private val applicationStateManager: ApplicationStateManager
) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val applicationsCollection = "job_applications"
    
    /**
     * Smart job application - handles both direct and profile-based applications
     */
    suspend fun smartApplyForJob(
        jobId: String,
        userId: String,
        coverLetter: String? = null,
        additionalNotes: String? = null
    ): Result<JobApplication> {
        return try {
            // Check if user can apply directly
            val canApplyDirectly = profileCompletionService.canApplyDirectly(userId)
            if (canApplyDirectly.isFailure) {
                return Result.failure(canApplyDirectly.exceptionOrNull() ?: Exception("Profile check failed"))
            }

            if (canApplyDirectly.getOrNull() == true) {
                applyDirectly(jobId, userId, coverLetter, additionalNotes)
            } else {
                // Profile-based application - user needs to complete profile
                Result.failure(Exception("PROFILE_INCOMPLETE"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Direct application for users with complete profiles
     */
    private suspend fun applyDirectly(
        jobId: String,
        userId: String,
        coverLetter: String?,
        additionalNotes: String?
    ): Result<JobApplication> {
        return try {
            // Get job details
            val jobResult = getJobDetails(jobId)
            if (jobResult.isFailure) {
                return Result.failure(jobResult.exceptionOrNull() ?: Exception("Job not found"))
            }

            val jobData = jobResult.getOrNull()!!
            val jobTitle = jobData["title"] as? String ?: "Unknown Job"
            val companyName = jobData["companyName"] as? String ?: "Unknown Company"
            val jobLocation = jobData["location"] as? String ?: "Unknown Location"
            val jobType = jobData["jobType"] as? String ?: "Unknown Type"
            val payInfo = "${jobData["payAmount"] ?: ""}/${jobData["payType"] ?: ""}"

            // Get user profile data
            val userProfileResult = profileCompletionService.getUserProfile(userId)
            if (userProfileResult.isFailure) {
                return Result.failure(userProfileResult.exceptionOrNull() ?: Exception("Profile not found"))
            }

            val userProfile = userProfileResult.getOrNull()!!

            // Create application
            val application = JobApplication(
                applicationId = UUID.randomUUID().toString(),
                jobId = jobId,
                workerId = userId,
                employerId = jobData["employerId"] as? String ?: "",
                status = ApplicationStatus.PENDING,
                statusHistory = listOf(
                    StatusUpdate(
                        status = ApplicationStatus.PENDING,
                        updatedAt = System.currentTimeMillis(),
                        updatedBy = userId,
                        notes = "Application submitted directly",
                        isSystemUpdate = true
                    )
                ),
                workerName = userProfile["fullName"] as? String ?: "",
                workerEmail = userProfile["email"] as? String ?: "",
                workerPhone = userProfile["phone"] as? String,
                workerProfileImageUrl = userProfile["profileImageUrl"] as? String,
                coverLetter = coverLetter ?: userProfile["coverLetter"] as? String ?: "",
                resumeUrl = userProfile["resumeUrl"] as? String,
                jobTitle = jobTitle,
                companyName = companyName,
                jobLocation = jobLocation,
                jobType = jobType,
                payInfo = payInfo,
                appliedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                workerNotes = additionalNotes
            )

            // Save application using existing method
            val saveResult = submitApplication(application)
            if (saveResult.isSuccess) {
                // Update state manager
                applicationStateManager.addAppliedJob(jobId)
                
                // Update job application count
                updateJobApplicationCount(jobId)
                
                Result.success(application)
            } else {
                Result.failure(saveResult.exceptionOrNull() ?: Exception("Failed to save application"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if user has applied for a job
     */
    suspend fun hasUserApplied(jobId: String, userId: String): Result<Boolean> {
        return try {
            val snapshot = firestore.collection(applicationsCollection)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("workerId", userId)
                .limit(1)
                .get()
                .await()
            
            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get job details from Firestore
     */
    private suspend fun getJobDetails(jobId: String): Result<Map<String, Any>> {
        return try {
            val doc = firestore.collection("jobs").document(jobId).get().await()
            if (doc.exists()) {
                Result.success(doc.data ?: emptyMap())
            } else {
                Result.failure(Exception("Job not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update job application count
     */
    private suspend fun updateJobApplicationCount(jobId: String) {
        try {
            firestore.collection("jobs").document(jobId)
                .update("applicationCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
        } catch (e: Exception) {
            // Log error but don't fail the application
            println("Failed to update job application count: ${e.message}")
        }
    }
    suspend fun submitApplication(application: JobApplication): Result<JobApplication> {
        return try {
            val applicationId = UUID.randomUUID().toString()
            val applicationWithId = application.copy(
                applicationId = applicationId,
                statusHistory = listOf(
                    StatusUpdate(
                        status = ApplicationStatus.PENDING,
                        updatedBy = application.workerId,
                        notes = "Application submitted",
                        isSystemUpdate = true
                    )
                )
            )
            
            firestore.collection(applicationsCollection)
                .document(applicationId)
                .set(applicationWithId)
                .await()
            
            // Send notification to employer
            notificationService.sendNewApplicationNotification(applicationWithId, applicationWithId.employerId)
            
            Result.success(applicationWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all applications for a worker
     */
    fun getWorkerApplications(workerId: String): Flow<Result<List<JobApplication>>> = flow {
        try {
            val snapshot = firestore.collection(applicationsCollection)
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("isActive", true)
                .orderBy("appliedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val applications = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(JobApplication::class.java)?.copy(applicationId = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
            
            emit(Result.success(applications))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get all applications for a job (employer view)
     */
    fun getJobApplications(jobId: String): Flow<Result<List<JobApplication>>> = flow {
        try {
            val snapshot = firestore.collection(applicationsCollection)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("isActive", true)
                .orderBy("appliedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val applications = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(JobApplication::class.java)?.copy(applicationId = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
            
            emit(Result.success(applications))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get applications by status
     */
    fun getApplicationsByStatus(workerId: String, status: ApplicationStatus): Flow<Result<List<JobApplication>>> = flow {
        try {
            val snapshot = firestore.collection(applicationsCollection)
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("status", status.name)
                .whereEqualTo("isActive", true)
                .orderBy("appliedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val applications = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(JobApplication::class.java)?.copy(applicationId = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
            
            emit(Result.success(applications))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Check if worker has already applied to a job
     */
    suspend fun hasWorkerAppliedToJob(workerId: String, jobId: String): Result<Boolean> {
        return try {
            val snapshot = firestore.collection(applicationsCollection)
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Withdraw an application
     */
    suspend fun withdrawApplication(applicationId: String, workerId: String): Result<Unit> {
        return try {
            val statusUpdate = StatusUpdate(
                status = ApplicationStatus.WITHDRAWN,
                updatedBy = workerId,
                notes = "Application withdrawn by worker",
                isSystemUpdate = true
            )
            
            firestore.collection(applicationsCollection)
                .document(applicationId)
                .update(
                    "status", ApplicationStatus.WITHDRAWN.name,
                    "updatedAt", System.currentTimeMillis(),
                    "statusHistory", com.google.firebase.firestore.FieldValue.arrayUnion(statusUpdate)
                )
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get application statistics for worker
     */
    suspend fun getApplicationStats(workerId: String): Result<Map<String, Int>> {
        return try {
            val snapshot = firestore.collection(applicationsCollection)
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            val stats = mutableMapOf<String, Int>()
            val currentTime = System.currentTimeMillis()
            val oneMonthAgo = currentTime - (30 * 24 * 60 * 60 * 1000L)
            
            snapshot.documents.forEach { doc ->
                val application = doc.toObject(JobApplication::class.java)
                if (application != null) {
                    // Count by status
                    val statusKey = "status_${application.status.name.lowercase()}"
                    stats[statusKey] = (stats[statusKey] ?: 0) + 1
                    
                    // Count this month's applications
                    if (application.appliedAt >= oneMonthAgo) {
                        stats["this_month"] = (stats["this_month"] ?: 0) + 1
                    }
                }
            }
            
            stats["total"] = snapshot.size()
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get single application by ID
     */
    suspend fun getApplicationById(applicationId: String): Result<JobApplication?> {
        return try {
            val doc = firestore.collection(applicationsCollection)
                .document(applicationId)
                .get()
                .await()
            
            if (doc.exists()) {
                val application = doc.toObject(JobApplication::class.java)
                Result.success(application?.copy(applicationId = doc.id))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches all job applications for a specific worker.
     */
    fun getApplicationsForWorker(workerId: String): Flow<Result<List<JobApplication>>> = flow {
        try {
            val querySnapshot = firestore.collection(applicationsCollection)
                .whereEqualTo("workerId", workerId)
                .orderBy("appliedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val applications = querySnapshot.documents.mapNotNull { document ->
                document.toObject(JobApplication::class.java)
            }
            emit(Result.success(applications))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    /**
     * Get application statistics for a worker
     */
    suspend fun getWorkerApplicationStats(workerId: String): Result<ApplicationStats> {
        return try {
            val snapshot = firestore.collection(applicationsCollection)
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("isActive", true)
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
                shortlistedApplications = applications.count { it.status == ApplicationStatus.SHORTLISTED },
                interviewedApplications = applications.count { it.status == ApplicationStatus.INTERVIEWED },
                selectedApplications = applications.count { it.status == ApplicationStatus.SELECTED },
                rejectedApplications = applications.count { it.status == ApplicationStatus.REJECTED },
                thisMonthApplications = applications.count { 
                    val currentTime = System.currentTimeMillis()
                    val monthAgo = currentTime - (30 * 24 * 60 * 60 * 1000L)
                    it.appliedAt >= monthAgo
                },
                responseRate = if (applications.isNotEmpty()) {
                    val respondedApplications = applications.count { 
                        it.status != ApplicationStatus.PENDING && it.status != ApplicationStatus.WITHDRAWN
                    }
                    (respondedApplications.toFloat() / applications.size) * 100f
                } else 0f
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
                .whereEqualTo("isActive", true)
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
                shortlistedApplications = applications.count { it.status == ApplicationStatus.SHORTLISTED },
                interviewedApplications = applications.count { it.status == ApplicationStatus.INTERVIEWED },
                selectedApplications = applications.count { it.status == ApplicationStatus.SELECTED },
                rejectedApplications = applications.count { it.status == ApplicationStatus.REJECTED },
                thisMonthApplications = applications.count { 
                    val currentTime = System.currentTimeMillis()
                    val monthAgo = currentTime - (30 * 24 * 60 * 60 * 1000L)
                    it.appliedAt >= monthAgo
                },
                responseRate = if (applications.isNotEmpty()) {
                    val respondedApplications = applications.count { 
                        it.status != ApplicationStatus.PENDING && it.status != ApplicationStatus.WITHDRAWN
                    }
                    (respondedApplications.toFloat() / applications.size) * 100f
                } else 0f
            )
            
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update application status with professional tracking
     */
    suspend fun updateApplicationStatus(
        applicationId: String, 
        newStatus: ApplicationStatus,
        updatedBy: String,
        notes: String? = null
    ): Result<JobApplication> {
        return try {
            val docRef = firestore.collection(applicationsCollection).document(applicationId)
            val doc = docRef.get().await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("Application not found"))
            }
            
            val currentApplication = doc.toObject(JobApplication::class.java)
                ?: return Result.failure(Exception("Invalid application data"))
            
            val statusUpdate = StatusUpdate(
                status = newStatus,
                updatedBy = updatedBy,
                notes = notes,
                isSystemUpdate = false
            )
            
            val updatedApplication = currentApplication.copy(
                status = newStatus,
                statusHistory = currentApplication.statusHistory + statusUpdate,
                updatedAt = System.currentTimeMillis(),
                lastViewedByEmployer = if (updatedBy != currentApplication.workerId) System.currentTimeMillis() else currentApplication.lastViewedByEmployer
            )
            
            docRef.set(updatedApplication).await()
            
            // Send notification to worker about status change
            notificationService.sendApplicationStatusNotification(updatedApplication, newStatus, updatedApplication.workerId)
            
            Result.success(updatedApplication)
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
            val docRef = firestore.collection(applicationsCollection).document(applicationId)
            val doc = docRef.get().await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("Application not found"))
            }
            
            val currentApplication = doc.toObject(JobApplication::class.java)
                ?: return Result.failure(Exception("Invalid application data"))
            
            val updatedApplication = currentApplication.copy(
                employerNotes = notes,
                updatedAt = System.currentTimeMillis(),
                lastViewedByEmployer = System.currentTimeMillis()
            )
            
            docRef.set(updatedApplication).await()
            Result.success(updatedApplication)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Schedule interview for application
     */
    suspend fun scheduleInterview(
        applicationId: String,
        interviewTime: Long,
        location: String,
        notes: String? = null,
        updatedBy: String
    ): Result<JobApplication> {
        return try {
            val docRef = firestore.collection(applicationsCollection).document(applicationId)
            val doc = docRef.get().await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("Application not found"))
            }
            
            val currentApplication = doc.toObject(JobApplication::class.java)
                ?: return Result.failure(Exception("Invalid application data"))
            
            val statusUpdate = StatusUpdate(
                status = ApplicationStatus.INTERVIEW_SCHEDULED,
                updatedBy = updatedBy,
                notes = "Interview scheduled for ${java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault()).format(java.util.Date(interviewTime))}",
                isSystemUpdate = false
            )
            
            val updatedApplication = currentApplication.copy(
                status = ApplicationStatus.INTERVIEW_SCHEDULED,
                statusHistory = currentApplication.statusHistory + statusUpdate,
                interviewScheduledAt = interviewTime,
                interviewLocation = location,
                interviewNotes = notes,
                updatedAt = System.currentTimeMillis(),
                lastViewedByEmployer = System.currentTimeMillis()
            )
            
            docRef.set(updatedApplication).await()
            Result.success(updatedApplication)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark application as viewed by employer
     */
    suspend fun markAsViewed(applicationId: String): Result<Unit> {
        return try {
            val docRef = firestore.collection(applicationsCollection).document(applicationId)
            val doc = docRef.get().await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("Application not found"))
            }
            
            val currentApplication = doc.toObject(JobApplication::class.java)
                ?: return Result.failure(Exception("Invalid application data"))
            
            // Only update if not already viewed
            if (currentApplication.lastViewedByEmployer == null) {
                val updatedApplication = currentApplication.copy(
                    status = ApplicationStatus.REVIEWED,
                    lastViewedByEmployer = System.currentTimeMillis(),
                    statusHistory = currentApplication.statusHistory + StatusUpdate(
                        status = ApplicationStatus.REVIEWED,
                        updatedBy = "system",
                        notes = "Application viewed by employer",
                        isSystemUpdate = true
                    )
                )
                docRef.set(updatedApplication).await()
            }
            
            Result.success(Unit)
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
                .whereEqualTo("isActive", true)
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
}
