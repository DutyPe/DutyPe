package com.example.dutype.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.models.ApplicationSource

/**
 * Room Entity for cached job applications (OPTIMIZED - 14 fields)
 * Matches optimized Firestore schema
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
    val jobTitle: String,
    val jobLocation: String,
    val companyName: String,
    val workerName: String,
    val workerPhone: String,
    val coverLetter: String?,
    val source: String = "MOBILE_APP",
    
    // Cache metadata
    val cachedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true,
    val isPendingSubmission: Boolean = false // True if queued offline
) {
    
    /**
     * Convert to JobApplication model (optimized schema - 14 fields)
     */
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
            appliedAt = appliedAt,
            updatedAt = updatedAt,
            jobTitle = jobTitle,
            jobLocation = jobLocation,
            companyName = companyName,
            workerName = workerName,
            coverLetter = coverLetter ?: "",
            source = try {
                ApplicationSource.valueOf(source)
            } catch (e: Exception) {
                ApplicationSource.MOBILE_APP
            }
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
                applicationId = app.id,
                jobId = app.jobId,
                workerId = app.workerId,
                employerId = app.employerId,
                status = app.status.name,
                appliedAt = app.appliedAt,
                updatedAt = app.updatedAt,
                jobTitle = app.jobTitle,
                jobLocation = app.jobLocation,
                companyName = app.companyName,
                workerName = app.workerName,
                workerPhone = "", // Not in optimized model, will be fetched from user profile if needed
                coverLetter = app.coverLetter,
                source = app.source.name
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
            workerPhone: String,
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
                appliedAt = now,
                updatedAt = now,
                jobTitle = jobTitle,
                jobLocation = jobLocation,
                companyName = companyName,
                workerName = workerName,
                workerPhone = workerPhone,
                coverLetter = coverLetter,
                source = ApplicationSource.MOBILE_APP.name,
                cachedAt = now,
                isSynced = false,
                isPendingSubmission = true
            )
        }
    }
}
