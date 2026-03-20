package com.example.dutype.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication

/**
 * Room Entity for cached job applications (OPTIMIZED)
 *
 * Firestore schema fields: applicationId, jobId, workerId, employerId, status, createdAt
 * Runtime-enriched display fields: jobTitle, jobLocation, companyName, workerName
 * User-provided content: coverLetter
 * Cache metadata: cachedAt, isSynced, isPendingSubmission
 */
@Entity(tableName = "applications")
data class ApplicationEntity(
    @PrimaryKey
    val applicationId: String,
    val jobId: String,
    val workerId: String,
    val employerId: String,
    val status: String,
    val appliedAt: Long,
    val updatedAt: Long,
    // Enriched display fields (fetched from jobs/users, cached locally)
    val jobTitle: String,
    val jobLocation: String,
    val companyName: String,
    val workerName: String,
    val coverLetter: String?,
    // Cache metadata
    val cachedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true,
    val isPendingSubmission: Boolean = false
) {
    
    fun toJobApplication(): JobApplication {
        return JobApplication(
            id = applicationId,
            jobId = jobId,
            workerId = workerId,
            employerId = employerId,
            status = try {
                ApplicationStatus.valueOf(status)
            } catch (e: Exception) {
                ApplicationStatus.PENDING
            },
            createdAt = appliedAt,
            jobTitle = jobTitle,
            jobLocation = jobLocation,
            companyName = companyName,
            workerName = workerName
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
        fun fromJobApplication(app: JobApplication): ApplicationEntity {
            return ApplicationEntity(
                applicationId = app.id,
                jobId = app.jobId,
                workerId = app.workerId,
                employerId = app.employerId,
                status = app.status.name,
                appliedAt = app.appliedAt,
                updatedAt = System.currentTimeMillis(),
                jobTitle = app.jobTitle,
                jobLocation = app.jobLocation,
                companyName = app.companyName,
                workerName = app.workerName,
                coverLetter = null
            )
        }
        
        fun createPendingApplication(
            applicationId: String,
            jobId: String,
            workerId: String,
            employerId: String,
            workerName: String,
            jobTitle: String,
            companyName: String,
            jobLocation: String
        ): ApplicationEntity {
            val now = System.currentTimeMillis()
            return ApplicationEntity(
                applicationId = applicationId,
                jobId = jobId,
                workerId = workerId,
                employerId = employerId,
                status = ApplicationStatus.PENDING.name,
                appliedAt = now,
                updatedAt = now,
                jobTitle = jobTitle,
                jobLocation = jobLocation,
                companyName = companyName,
                workerName = workerName,
                coverLetter = null,
                cachedAt = now,
                isSynced = false,
                isPendingSubmission = true
            )
        }
    }
}
