package com.example.dutype.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication

/**
 * Room Entity for cached job applications
 * 
 * Supports offline-first architecture:
 * - Cache applications for offline viewing
 * - Queue new applications when offline
 * - Track sync status for background sync
 * 
 * @author DutyPe Engineering Team
 * @since 2.2.0
 */
@Entity(tableName = "applications")
data class ApplicationEntity(
    @PrimaryKey
    val applicationId: String,
    val jobId: String,
    val workerId: String,
    val employerId: String,
    val status: String, // ApplicationStatus enum name
    
    // Worker info snapshot
    val workerName: String,
    val workerEmail: String,
    val workerPhone: String?,
    val workerProfileImageUrl: String?,
    val workerLocation: String?,
    
    // Job info snapshot
    val jobTitle: String,
    val companyName: String,
    val jobLocation: String,
    val jobType: String,
    val payInfo: String,
    
    // Application content
    val coverLetter: String?,
    val resumeUrl: String?,
    val workerNotes: String?,
    
    // Timestamps
    val appliedAt: Long,
    val updatedAt: Long,
    val viewedAt: Long?,
    
    // Cache metadata
    val cachedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true,
    val isPendingSubmission: Boolean = false // True if queued offline
) {
    
    /**
     * Convert to JobApplication model
     */
    fun toJobApplication(): JobApplication {
        return JobApplication(
            applicationId = applicationId,
            jobId = jobId,
            workerId = workerId,
            employerId = employerId,
            status = try { 
                ApplicationStatus.valueOf(status) 
            } catch (e: Exception) { 
                ApplicationStatus.PENDING 
            },
            workerName = workerName,
            workerEmail = workerEmail,
            workerPhone = workerPhone,
            workerProfileImageUrl = workerProfileImageUrl,
            workerLocation = workerLocation,
            jobTitle = jobTitle,
            companyName = companyName,
            jobLocation = jobLocation,
            jobType = jobType,
            payInfo = payInfo,
            coverLetter = coverLetter ?: "",
            resumeUrl = resumeUrl,
            workerNotes = workerNotes,
            appliedAt = appliedAt,
            updatedAt = updatedAt,
            lastViewedByEmployer = viewedAt
        )
    }
    
    /**
     * Get status as enum
     */
    fun getStatusEnum(): ApplicationStatus {
        return try {
            ApplicationStatus.valueOf(status)
        } catch (e: Exception) {
            ApplicationStatus.PENDING
        }
    }
    
    companion object {
        /**
         * Create from JobApplication model
         */
        fun fromJobApplication(app: JobApplication): ApplicationEntity {
            return ApplicationEntity(
                applicationId = app.applicationId,
                jobId = app.jobId,
                workerId = app.workerId,
                employerId = app.employerId,
                status = app.status.name,
                workerName = app.workerName,
                workerEmail = app.workerEmail,
                workerPhone = app.workerPhone,
                workerProfileImageUrl = app.workerProfileImageUrl,
                workerLocation = app.workerLocation,
                jobTitle = app.jobTitle,
                companyName = app.companyName,
                jobLocation = app.jobLocation,
                jobType = app.jobType,
                payInfo = app.payInfo,
                coverLetter = app.coverLetter,
                resumeUrl = app.resumeUrl,
                workerNotes = app.workerNotes,
                appliedAt = app.appliedAt,
                updatedAt = app.updatedAt,
                viewedAt = app.lastViewedByEmployer
            )
        }
        
        /**
         * Create a new application for offline submission
         */
        fun createPendingApplication(
            applicationId: String,
            jobId: String,
            workerId: String,
            employerId: String,
            workerName: String,
            workerEmail: String,
            workerPhone: String?,
            jobTitle: String,
            companyName: String,
            jobLocation: String,
            coverLetter: String?
        ): ApplicationEntity {
            val now = System.currentTimeMillis()
            return ApplicationEntity(
                applicationId = applicationId,
                jobId = jobId,
                workerId = workerId,
                employerId = employerId,
                status = ApplicationStatus.PENDING.name,
                workerName = workerName,
                workerEmail = workerEmail,
                workerPhone = workerPhone,
                workerProfileImageUrl = null,
                workerLocation = null,
                jobTitle = jobTitle,
                companyName = companyName,
                jobLocation = jobLocation,
                jobType = "",
                payInfo = "",
                coverLetter = coverLetter,
                resumeUrl = null,
                workerNotes = null,
                appliedAt = now,
                updatedAt = now,
                viewedAt = null,
                cachedAt = now,
                isSynced = false,
                isPendingSubmission = true
            )
        }
    }
}
