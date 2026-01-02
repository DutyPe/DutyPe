package com.example.dutype.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity for cached job applications
 * 
 * Stores user's job applications for offline access.
 */
@Entity(tableName = "applications")
data class ApplicationEntity(
    @PrimaryKey
    val applicationId: String,
    val jobId: String,
    val workerId: String,
    val employerId: String,
    val status: String,
    val workerName: String,
    val workerEmail: String,
    val workerPhone: String?,
    val workerProfileImageUrl: String?,
    val jobTitle: String,
    val companyName: String,
    val jobLocation: String,
    val jobType: String,
    val payInfo: String,
    val coverLetter: String,
    val appliedAt: Long,
    val updatedAt: Long,
    val isActive: Boolean,
    val isFilled: Boolean,
    // Cache metadata
    val cachedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true,
    val pendingAction: String? = null // "WITHDRAW", "UPDATE_STATUS", etc.
)
