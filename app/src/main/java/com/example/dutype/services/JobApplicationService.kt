package com.example.dutype.services

import android.util.Log
import com.example.dutype.models.JobApplication
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.StatusUpdate
import com.example.dutype.models.ApplicationStats
import com.example.dutype.models.WorkExperience
import com.example.dutype.models.Education
import com.example.dutype.models.DocumentAttachment
import com.example.dutype.models.DocumentType
import com.example.dutype.state.ApplicationStateManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
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

            // Create application with comprehensive worker profile data
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
                        systemUpdate = true
                    )
                ),
                // Basic worker information
                workerName = userProfile["fullName"] as? String ?: "",
                workerEmail = userProfile["email"] as? String ?: "",
                workerPhone = userProfile["phone"] as? String,
                workerProfileImageUrl = userProfile["profileImageUrl"] as? String,
                workerLocation = userProfile["location"] as? String,
                workerDateOfBirth = userProfile["dateOfBirth"] as? String,
                workerGender = userProfile["gender"] as? String,
                
                // Professional information
                workExperience = (userProfile["experience"] as? List<Map<String, Any>>)?.map { exp ->
                    WorkExperience(
                        id = exp["id"] as? String ?: "",
                        company = exp["company"] as? String ?: "",
                        position = exp["position"] as? String ?: "",
                        startDate = exp["startDate"] as? String ?: "",
                        endDate = exp["endDate"] as? String,
                        description = exp["description"] as? String ?: "",
                        isCurrent = exp["isCurrent"] as? Boolean ?: false,
                        location = exp["location"] as? String,
                        salary = exp["salary"] as? String,
                        achievements = exp["achievements"] as? List<String> ?: emptyList()
                    )
                } ?: emptyList(),
                
                skills = userProfile["skills"] as? List<String> ?: emptyList(),
                education = (userProfile["education"] as? List<Map<String, Any>>)?.map { edu ->
                    Education(
                        id = edu["id"] as? String ?: "",
                        institution = edu["institution"] as? String ?: "",
                        degree = edu["degree"] as? String ?: "",
                        fieldOfStudy = edu["fieldOfStudy"] as? String,
                        startDate = edu["startDate"] as? String ?: "",
                        endDate = edu["endDate"] as? String,
                        gpa = edu["gpa"] as? String,
                        description = edu["description"] as? String,
                        isCurrent = edu["isCurrent"] as? Boolean ?: false
                    )
                } ?: emptyList(),
                
                certifications = userProfile["certifications"] as? List<String> ?: emptyList(),
                languages = userProfile["languages"] as? List<String> ?: emptyList(),
                availability = userProfile["availability"] as? String,
                expectedSalary = userProfile["expectedSalary"] as? String,
                
                // Application content
                coverLetter = coverLetter ?: userProfile["coverLetter"] as? String ?: "",
                resumeUrl = userProfile["resumeUrl"] as? String,
                additionalDocuments = (userProfile["documents"] as? List<Map<String, Any>>)?.map { doc ->
                    DocumentAttachment(
                        documentId = doc["id"] as? String ?: "",
                        fileName = doc["name"] as? String ?: "",
                        fileUrl = doc["url"] as? String ?: "",
                        fileType = DocumentType.valueOf(doc["type"] as? String ?: "OTHER"),
                        fileSize = doc["size"] as? Long ?: 0L,
                        uploadedAt = doc["uploadedAt"] as? Long ?: System.currentTimeMillis(),
                        isRequired = doc["isRequired"] as? Boolean ?: false
                    )
                } ?: emptyList(),
                
                // Portfolio & Links (Removed - dutype doesn't need external profiles)
                
                // Job information snapshot
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
                // Ensure new documents always have active=true 
                active = true,
                statusHistory = listOf(
                    StatusUpdate(
                        status = ApplicationStatus.PENDING,
                        updatedBy = application.workerId,
                        notes = "Application submitted",
                        systemUpdate = true
                    )
                )
            )
            
            firestore.collection(applicationsCollection)
                .document(applicationId)
                .set(applicationWithId)
                .await()
            
            // Send notification to employer
            notificationService.sendNewApplicationNotification(applicationWithId, applicationWithId.employerId)
            
            // Also notify the worker that the application was submitted successfully
            notificationService.sendApplicationStatusNotification(
                applicationWithId,
                ApplicationStatus.PENDING,
                applicationWithId.workerId
            )
            
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
            // Use active=true to match actual Firestore field
            val base = firestore.collection(applicationsCollection)
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("active", true)
                .orderBy("appliedAt", Query.Direction.DESCENDING)

            val snapshot = base.get().await()
            println("[Applications] workerId=$workerId using active field -> ${snapshot.size()} docs")

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
     * Get all applications for a job (employer view) - Enhanced with real-time updates
     */
    fun getJobApplications(jobId: String): Flow<Result<List<JobApplication>>> = flow {
        try {
            val base = firestore.collection(applicationsCollection)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("active", true)
                .orderBy("appliedAt", Query.Direction.DESCENDING)

            val snapshot = base.get().await()
            println("[Applications] jobId=$jobId using active field -> ${snapshot.size()} docs")

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
     * Get all applications for an employer (across all their jobs) - Enterprise feature
     */
    fun getEmployerApplications(employerId: String): Flow<Result<List<JobApplication>>> = flow {
        try {
            println("[Applications] DEBUG: Starting getEmployerApplications for employerId=$employerId")
            
            // First try simple query without ordering to avoid index issues
            val simpleSnapshot = try {
                val query = firestore.collection(applicationsCollection)
                    .whereEqualTo("employerId", employerId)
                println("[Applications] DEBUG: Executing simple query for employerId=$employerId")
                val result = query.get().await()
                println("[Applications] DEBUG: Simple query completed with ${result.size()} documents")
                result
            } catch (e: Exception) {
                println("[Applications] Simple query failed: ${e.message}")
                e.printStackTrace()
                null
            }
            
            if (simpleSnapshot != null && !simpleSnapshot.isEmpty) {
                println("[Applications] employerId=$employerId using simple query -> ${simpleSnapshot.size()} docs")
                val applications = simpleSnapshot.documents.mapNotNull { doc ->
                    try {
                        println("[Applications] DEBUG: Processing document ${doc.id}")
                        println("[Applications] DEBUG: Document data: ${doc.data}")
                        val app = doc.toObject(JobApplication::class.java)?.copy(applicationId = doc.id)
                        println("[Applications] DEBUG: Parsed application: ${app?.applicationId}")
                        app
                    } catch (e: Exception) {
                        println("[Applications] Failed to parse document ${doc.id}: ${e.message}")
                        e.printStackTrace()
                        null
                    }
                }
                
                println("[Applications] DEBUG: Successfully parsed ${applications.size} applications")
                // Sort in memory by appliedAt descending
                val sortedApplications = applications.sortedByDescending { it.appliedAt }
                emit(Result.success(sortedApplications))
                return@flow
            } else {
                println("[Applications] DEBUG: Simple query returned empty or null")
            }

            // Fallback to complex queries if simple query returns nothing
            val base = firestore.collection(applicationsCollection)
                .whereEqualTo("employerId", employerId)
                .orderBy("appliedAt", Query.Direction.DESCENDING)

            val snapshot = try {
                base.whereEqualTo("active", true).get().await()
            } catch (e: Exception) {
                println("[Applications] active query failed: ${e.message}")
                null
            }

            val legacySnapshot = try {
                base.whereEqualTo("active", true).get().await()
            } catch (e: Exception) {
                println("[Applications] active query failed: ${e.message}")
                null
            }
            
            val documents = when {
                snapshot != null && !snapshot.isEmpty -> {
                    println("[Applications] employerId=$employerId using active path -> ${snapshot.size()} docs")
                    snapshot.documents
                }
                legacySnapshot != null && !legacySnapshot.isEmpty -> {
                    println("[Applications] employerId=$employerId using legacy active path -> ${legacySnapshot.size()} docs")
                    legacySnapshot.documents
                }
                else -> {
                    println("[Applications] employerId=$employerId no results on both paths")
                    emptyList()
                }
            }

            val applications = documents.mapNotNull { doc ->
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
     * Get real-time application statistics for employer dashboard
     */
    suspend fun getEmployerApplicationStats(employerId: String): Result<ApplicationStats> {
        return try {
            val applications = getEmployerApplications(employerId).first().getOrNull() ?: emptyList()
            
            val stats = ApplicationStats(
                totalApplications = applications.size,
                pendingApplications = applications.count { it.status == ApplicationStatus.PENDING },
                reviewedApplications = applications.count { it.status == ApplicationStatus.UNDER_REVIEW },
                shortlistedApplications = applications.count { it.status == ApplicationStatus.SHORTLISTED },
                rejectedApplications = applications.count { it.status == ApplicationStatus.REJECTED },
                hiredApplications = applications.count { it.status == ApplicationStatus.HIRED },
                recentApplications = applications.take(5) // Last 5 applications
            )
            
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get applications by status
     */
    fun getApplicationsByStatus(workerId: String, status: ApplicationStatus): Flow<Result<List<JobApplication>>> = flow {
        try {
            val base = firestore.collection(applicationsCollection)
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("status", status.name)
                .orderBy("appliedAt", Query.Direction.DESCENDING)

            val snapshot = try {
                base.whereEqualTo("active", true).get().await()
            } catch (e: Exception) {
                null
            }

            val legacySnapshot = try {
                base.whereEqualTo("active", true).get().await()
            } catch (e: Exception) {
                null
            }
            
            val documents = when {
                snapshot != null && !snapshot.isEmpty -> {
                    println("[Applications] status filter using active path -> ${snapshot.size()} docs")
                    snapshot.documents
                }
                legacySnapshot != null && !legacySnapshot.isEmpty -> {
                    println("[Applications] status filter using legacy active path -> ${legacySnapshot.size()} docs")
                    legacySnapshot.documents
                }
                else -> {
                    println("[Applications] status filter no results on both paths")
                    emptyList()
                }
            }

            val applications = documents.mapNotNull { doc ->
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
                .whereEqualTo("active", true)
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
                systemUpdate = true
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
                .whereEqualTo("active", true)
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
                shortlistedApplications = applications.count { it.status == ApplicationStatus.SHORTLISTED },
                rejectedApplications = applications.count { it.status == ApplicationStatus.REJECTED },
                hiredApplications = applications.count { it.status == ApplicationStatus.HIRED },
                recentApplications = applications.take(5)
            )
            
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark application as viewed by employer (for analytics)
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
     * Get application analytics for employer dashboard
     */
    suspend fun getApplicationAnalytics(employerId: String): Result<com.example.dutype.models.ApplicationAnalytics> {
        return try {
            val applications = getEmployerApplications(employerId).first().getOrNull() ?: emptyList()
            
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
        val calendar = java.util.Calendar.getInstance()
        val trends = mutableMapOf<String, Int>()
        
        // Get last 7 days
        repeat(7) { daysAgo ->
            calendar.timeInMillis = System.currentTimeMillis() - (daysAgo * 24 * 60 * 60 * 1000)
            val dayKey = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(calendar.time)
            
            val dayStart = calendar.timeInMillis
            val dayEnd = dayStart + (24 * 60 * 60 * 1000)
            
            val dayApplications = applications.count { app ->
                app.appliedAt in dayStart until dayEnd
            }
            
            trends[dayKey] = dayApplications
        }
        
        return trends
    }
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
                systemUpdate = false
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
                systemUpdate = false
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
                        systemUpdate = true
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

    /**
     * Debug function to check all applications data
     */
    suspend fun debugApplicationData(employerId: String) {
        try {
            Log.d("JobApplicationService", "=== DEBUG APPLICATION DATA ===")
            Log.d("JobApplicationService", "Looking for applications for employer: $employerId")
            
            // Get all documents from job_applications collection
            val allDocuments = firestore.collection("job_applications").get().await()
            Log.d("JobApplicationService", "Total documents in job_applications: ${allDocuments.documents.size}")
            
            allDocuments.documents.forEach { doc ->
                Log.d("JobApplicationService", "Document ID: ${doc.id}")
                Log.d("JobApplicationService", "Document data: ${doc.data}")
                val docEmployerId = doc.getString("employerId")
                Log.d("JobApplicationService", "Document employerId: $docEmployerId")
                Log.d("JobApplicationService", "Matches target employerId: ${docEmployerId == employerId}")
                Log.d("JobApplicationService", "---")
            }
            
        } catch (e: Exception) {
            Log.e("JobApplicationService", "Error in debugApplicationData", e)
        }
    }

    /**
     * Debug function to check job data and verify employerId correlation
     */
    suspend fun debugJobData(employerId: String) {
        try {
            Log.d("JobApplicationService", "=== DEBUG JOB DATA ===")
            Log.d("JobApplicationService", "Looking for jobs for employer: $employerId")
            
            // Get all documents from jobs collection for this employer
            val employerJobs = firestore.collection("jobs")
                .whereEqualTo("employerId", employerId)
                .get()
                .await()
            
            Log.d("JobApplicationService", "Total jobs for employer: ${employerJobs.documents.size}")
            
            employerJobs.documents.forEach { doc ->
                Log.d("JobApplicationService", "Job ID: ${doc.id}")
                val jobEmployerId = doc.getString("employerId")
                val jobTitle = doc.getString("title")
                Log.d("JobApplicationService", "Job Title: $jobTitle")
                Log.d("JobApplicationService", "Job employerId: $jobEmployerId")
                Log.d("JobApplicationService", "---")
                
                // Now check applications for this specific job
                Log.d("JobApplicationService", "Checking applications for job: ${doc.id}")
                val jobApplications = firestore.collection("job_applications")
                    .whereEqualTo("jobId", doc.id)
                    .get()
                    .await()
                
                Log.d("JobApplicationService", "Applications for job ${doc.id}: ${jobApplications.documents.size}")
                jobApplications.documents.forEach { appDoc ->
                    val appEmployerId = appDoc.getString("employerId")
                    Log.d("JobApplicationService", "  App employerId: $appEmployerId")
                    Log.d("JobApplicationService", "  App jobId: ${appDoc.getString("jobId")}")
                    Log.d("JobApplicationService", "  App workerId: ${appDoc.getString("workerId")}")
                }
            }
            
        } catch (e: Exception) {
            Log.e("JobApplicationService", "Error in debugJobData", e)
        }
    }
}
