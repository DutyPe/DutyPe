package com.example.dutype.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.dutype.models.JobListing

/**
 * Room Entity for cached jobs (OPTIMIZED - 18 fields)
 * Matches optimized Firestore schema
 */
@Entity(
    tableName = "jobs",
    indices = [
        Index(value = ["isActive", "postedAt"]),
        Index(value = ["employerId"]),
        Index(value = ["isFilled", "isActive"]),
        Index(value = ["category"]),
        Index(value = ["isSynced"])
    ]
)
data class JobEntity(
    @PrimaryKey
    val id: String,
    val employerId: String,
    val title: String,
    val companyName: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val payAmount: String,
    val payType: String,
    val shiftTiming: String,
    val isActive: Boolean,
    val isFilled: Boolean,
    val postedAt: Long,
    val contactNumber: String,
    val vacancies: Int,
    val jobType: String,
    val gender: String,
    val description: String,
    val category: String = "OTHER",
    val applicationCount: Int = 0,
    val isSynced: Boolean = true,
    val cachedAt: Long = System.currentTimeMillis()
) {
    
    /**
     * Convert to JobListing model (optimized schema - 18 fields)
     */
    fun toJobListing(): JobListing {
        return JobListing(
            id = id,
            employerId = employerId,
            title = title,
            companyName = companyName,
            location = location,
            latitude = latitude,
            longitude = longitude,
            payAmount = payAmount,
            payType = payType,
            shiftTiming = shiftTiming,
            isActive = isActive,
            isFilled = isFilled,
            postedAt = postedAt,
            contactNumber = contactNumber,
            vacancies = vacancies,
            jobType = jobType,
            gender = gender,
            description = description
        )
    }

    companion object {
        /**
         * Create from JobListing model
         */
        fun fromJobListing(job: JobListing, category: String = "OTHER"): JobEntity {
            return JobEntity(
                id = job.id,
                employerId = job.employerId,
                title = job.title,
                companyName = job.companyName,
                location = job.location,
                latitude = job.latitude,
                longitude = job.longitude,
                payAmount = job.payAmount,
                payType = job.payType,
                shiftTiming = job.shiftTiming,
                isActive = job.isActive,
                isFilled = job.isFilled,
                postedAt = job.postedAt,
                contactNumber = job.contactNumber,
                vacancies = job.vacancies,
                jobType = job.jobType,
                gender = job.gender,
                description = job.description,
                category = category,
                applicationCount = 0,
                isSynced = true
            )
        }
    }
}
