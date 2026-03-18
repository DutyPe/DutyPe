package com.example.dutype.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.dutype.models.JobListing

/**
 * Room Entity for cached jobs — aligned to target Firestore schema.
 * Migration: version bump required (see AppDatabase).
 */
@Entity(
    tableName = "jobs",
    indices = [
        Index(value = ["status", "createdAt"]),
        Index(value = ["employerId"]),
        Index(value = ["jobType", "status"]),
        Index(value = ["geohash"]),
        Index(value = ["isSynced"])
    ]
)
data class JobEntity(
    @PrimaryKey
    val id: String,
    val employerId: String,
    val title: String,
    val jobType: String,
    val salary: Double,
    val salaryType: String,
    val lat: Double,
    val lng: Double,
    val geohash: String,
    val urgency: String,
    val status: String,          // "open" | "closed" | "expired"
    val createdAt: Long,
    val expiresAt: Long,
    // job_details fields (cached for offline)
    val description: String,
    val contactNumber: String,
    val addressText: String,
    // local-only
    val isSynced: Boolean = true,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toJobListing(): JobListing {
        return JobListing(
            id = id,
            employerId = employerId,
            title = title,
            jobType = jobType,
            salary = salary,
            salaryType = salaryType,
            lat = lat,
            lng = lng,
            geohash = geohash,
            urgency = urgency,
            status = status,
            createdAt = createdAt,
            expiresAt = expiresAt,
            description = description,
            contactNumber = contactNumber,
            addressText = addressText,
            location = addressText
        )
    }

    companion object {
        fun fromJobListing(job: JobListing): JobEntity {
            return JobEntity(
                id = job.id,
                employerId = job.employerId,
                title = job.title,
                jobType = job.jobType,
                salary = job.salary,
                salaryType = job.salaryType,
                lat = if (job.lat != 0.0 || job.lng != 0.0) job.lat else job.latitude,
                lng = if (job.lat != 0.0 || job.lng != 0.0) job.lng else job.longitude,
                geohash = job.geohash,
                urgency = job.urgency,
                status = job.status,
                createdAt = job.createdAt,
                expiresAt = job.expiresAt,
                description = job.description,
                contactNumber = job.contactNumber,
                addressText = if (job.addressText.isNotBlank()) job.addressText else job.location,
                isSynced = true
            )
        }
    }
}
