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
                ApplicationStatus.APPLIED
            },
            createdAt = appliedAt,
            jobTitle = jobTitle,
            jobLocation = jobLocation,
            companyName = companyName,
            workerName = workerName,
            coverLetter = coverLetter.orEmpty()
        )
    }
    
    /**
     * Get status as enum
     */
    fun getStatusEnum(): ApplicationStatus {
        return try {
            ApplicationStatus.valueOf(status)
        } catch (e: Exception) {
            ApplicationStatus.APPLIED
        }
    }
    
    companion object {
        fun fromJobApplication(app: JobApplication): ApplicationEntity {
            val canonicalId = app.id.ifBlank { "${app.jobId}_${app.workerId}" }
            return ApplicationEntity(
                applicationId = canonicalId,
                jobId = app.jobId,
                workerId = app.workerId,
                employerId = app.employerId,
                status = app.status.name,
                appliedAt = app.createdAt,
                updatedAt = System.currentTimeMillis(),
                jobTitle = app.jobTitle,
                jobLocation = app.jobLocation,
                companyName = app.companyName,
                workerName = app.workerName,
                coverLetter = app.coverLetter.takeIf { it.isNotBlank() }
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
            jobLocation: String,
            coverLetter: String? = null
        ): ApplicationEntity {
            val now = System.currentTimeMillis()
            return ApplicationEntity(
                applicationId = applicationId,
                jobId = jobId,
                workerId = workerId,
                employerId = employerId,
                status = ApplicationStatus.APPLIED.name,
                appliedAt = now,
                updatedAt = now,
                jobTitle = jobTitle,
                jobLocation = jobLocation,
                companyName = companyName,
                workerName = workerName,
                coverLetter = coverLetter,
                cachedAt = now,
                isSynced = false,
                isPendingSubmission = true
            )
        }
    }
}
