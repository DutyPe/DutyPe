package com.example.dutype.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity for saved/bookmarked jobs
 * 
 * Stores user's saved jobs for offline access.
 */
@Entity(tableName = "saved_jobs")
data class SavedJobEntity(
    @PrimaryKey
    val id: String,
    val workerId: String,
    val jobId: String,
    val savedAt: Long,
    val isActive: Boolean = true,
    // Cache metadata
    val cachedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true,
    val pendingAction: String? = null // "SAVE", "UNSAVE"
)
