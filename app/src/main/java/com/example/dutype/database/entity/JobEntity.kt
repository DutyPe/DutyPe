package com.example.dutype.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.dutype.models.JobListing

/**
 * Room Entity for cached jobs
 * 
 * Stores job data for offline access.
 * Maps to/from JobListing and JobListingSummary models.
 * 
 * P1 PERFORMANCE FIX: Added database indexes for frequently queried columns
 * - category + isActive + postedAt: Category browsing with sorting
 * - isActive + postedAt: All jobs listing with sorting
 * - employerId: Employer's posted jobs
 * - payType + isActive: Job type filtering
 * - jobType + isActive: Full-time/Part-time filtering
 * 
 * @author DutyPe Engineering Team
 * @since 2.4.0
 */
@Entity(
    tableName = "jobs",
    indices = [
        Index(value = ["category", "isActive", "postedAt"]),
        Index(value = ["isActive", "postedAt"]),
        Index(value = ["employerId"]),
        Index(value = ["payType", "isActive"]),
        Index(value = ["jobType", "isActive"]),
        Index(value = ["isFilled", "isActive"]),
        Index(value = ["cachedAt"])
    ]
)
data class JobEntity(
    @PrimaryKey
    val jobId: String,
    val employerId: String,
    val title: String,
    val companyName: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val payAmount: String,
    val payType: String,
    val category: String,
    val jobType: String,
    val vacancies: Int,
    val description: String,
    val requirements: String, // Stored as comma-separated string
    val contactNumber: String,
    val urgency: String,
    // REMOVED: employerTrustTier - no longer in JobListing model
    val jobImageUrl: String,
    val isActive: Boolean,
    val isFilled: Boolean,
    val applicationCount: Long,
    val postedAt: Long,
    // REMOVED: expiresAt - calculated from postedAt + expiryDays
    // Cache metadata
    val cachedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true
) {
    
    /**
     * Convert to JobListing model
     */
    fun toJobListing(): JobListing {
        return JobListing(
            jobId = jobId,
            employerId = employerId,
            title = title,
            companyName = companyName,
            location = location,
            latitude = latitude,
            longitude = longitude,
            payAmount = payAmount,
            payType = payType,
            jobType = jobType,
            vacancies = vacancies,
            description = description,
            requirements = requirements.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            contactNumber = contactNumber,
            urgency = urgency,
            jobImageUrl = jobImageUrl,
            isActive = isActive,
            isFilled = isFilled,
            applicationCount = applicationCount,
            postedAt = postedAt
        )
    }

    companion object {
        /**
         * Create from JobListing model
         */
        fun fromJobListing(job: JobListing): JobEntity {
            return JobEntity(
                jobId = job.jobId,
                employerId = job.employerId,
                title = job.title,
                companyName = job.companyName,
                location = job.location,
                latitude = job.latitude,
                longitude = job.longitude,
                payAmount = job.payAmount,
                payType = job.payType,
                category = job.getCategory(),
                jobType = job.jobType,
                vacancies = job.vacancies,
                description = job.description,
                requirements = job.requirements.joinToString(","),
                contactNumber = job.contactNumber,
                urgency = job.urgency,
                jobImageUrl = job.jobImageUrl,
                isActive = job.isActive,
                isFilled = job.isFilled,
                applicationCount = job.applicationCount,
                postedAt = job.postedAt
            )
        }

    }
}
