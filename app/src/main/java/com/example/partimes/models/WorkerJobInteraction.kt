package com.example.partimes.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Model for worker-specific interactions with jobs
 * This is separate from the job posting data and handles:
 * - Bookmarks
 * - Applications
 * - Personal notes
 * - Application status
 */
@Entity(tableName = "worker_interactions")
data class WorkerJobInteraction(
    @PrimaryKey
    val id: String = "",
    val jobId: String = "",
    val workerId: String = "",
    val isBookmarked: Boolean = false,
    val isApplied: Boolean = false,
    val applicationStatus: String = "PENDING", // PENDING, VIEWED, SHORTLISTED, SELECTED, REJECTED
    val appliedAt: Long = 0L,
    val bookmarkedAt: Long = 0L,
    val personalNotes: String = "",
    val applicationNotes: String = ""
)

// ApplicationStatus enum is defined in JobApplication.kt
