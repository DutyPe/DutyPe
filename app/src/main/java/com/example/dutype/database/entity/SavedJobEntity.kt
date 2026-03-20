package com.example.dutype.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity for saved/bookmarked jobs
 *
 * Mirrors Firestore saved_jobs: { id, userId, jobId, createdAt }
 * Cache metadata fields are Room-only (never written to Firestore).
 */
@Entity(tableName = "saved_jobs")
data class SavedJobEntity(
    @PrimaryKey
    val id: String,
    val workerId: String,
    val jobId: String,
    val savedAt: Long,
    // Cache metadata (Room-only)
    val cachedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true,
    val pendingAction: String? = null // "SAVE", "UNSAVE" — offline queue
)
