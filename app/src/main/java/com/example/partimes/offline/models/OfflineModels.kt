package com.example.partimes.offline.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Offline data models for caching and sync
 */

/**
 * Cached job data
 */
@Entity(tableName = "cached_jobs")
data class CachedJob(
    @PrimaryKey
    val jobId: String,
    val title: String,
    val description: String,
    val employerName: String,
    val location: String,
    val salary: String?,
    val employmentType: String,
    val experienceLevel: String,
    val skills: String, // JSON string of skills
    val tags: String, // JSON string of tags
    val postedAt: Long,
    val expiresAt: Long,
    val isRemote: Boolean,
    val companySize: String,
    val industry: String,
    val benefits: String?,
    val requirements: String,
    val responsibilities: String,
    val applicationDeadline: Long?,
    val viewCount: Int,
    val applicationCount: Int,
    val isActive: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Cached user profile data
 */
@Entity(tableName = "cached_user_profile")
data class CachedUserProfile(
    @PrimaryKey
    val userId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val profilePicture: String?,
    val title: String?,
    val summary: String?,
    val location: String?,
    val skills: String, // JSON string of skills
    val experience: String, // JSON string of work experience
    val education: String, // JSON string of education
    val preferences: String, // JSON string of preferences
    val isVerified: Boolean,
    val profileCompletion: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Cached application data
 */
@Entity(tableName = "cached_applications")
data class CachedApplication(
    @PrimaryKey
    val applicationId: String,
    val userId: String,
    val jobId: String,
    val status: String,
    val appliedAt: Long,
    val personalDetails: String, // JSON string
    val experience: String, // JSON string
    val skills: String, // JSON string
    val coverLetter: String?,
    val documents: String, // JSON string of documents
    val notes: String?,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Cached notification data
 */
@Entity(tableName = "cached_notifications")
data class CachedNotification(
    @PrimaryKey
    val notificationId: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String,
    val priority: String,
    val isRead: Boolean,
    val isArchived: Boolean,
    val createdAt: Long,
    val readAt: Long?,
    val archivedAt: Long?,
    val data: String?, // JSON string of additional data
    val isSynced: Boolean = false
)

/**
 * Cached search data
 */
@Entity(tableName = "cached_searches")
data class CachedSearch(
    @PrimaryKey
    val searchId: String,
    val userId: String,
    val query: String,
    val filters: String, // JSON string of filters
    val results: String, // JSON string of job IDs
    val resultCount: Int,
    val searchedAt: Long,
    val isSaved: Boolean,
    val isSynced: Boolean = false
)

/**
 * Cached saved jobs
 */
@Entity(tableName = "cached_saved_jobs")
data class CachedSavedJob(
    @PrimaryKey
    val savedJobId: String,
    val userId: String,
    val jobId: String,
    val savedAt: Long,
    val notes: String?,
    val isSynced: Boolean = false
)

/**
 * Cached recently viewed jobs
 */
@Entity(tableName = "cached_recently_viewed")
data class CachedRecentlyViewed(
    @PrimaryKey
    val viewId: String,
    val userId: String,
    val jobId: String,
    val viewedAt: Long,
    val viewDuration: Long,
    val source: String,
    val isSynced: Boolean = false
)

/**
 * Sync status tracking
 */
@Entity(tableName = "sync_status")
data class SyncStatus(
    @PrimaryKey
    val entityType: String, // jobs, applications, notifications, etc.
    val lastSyncTime: Long,
    val syncStatus: String, // SUCCESS, FAILED, IN_PROGRESS
    val errorMessage: String?,
    val recordCount: Int,
    val syncedCount: Int,
    val failedCount: Int
)

/**
 * Offline queue for pending operations
 */
@Entity(tableName = "offline_queue")
data class OfflineQueueItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val operationType: String, // CREATE, UPDATE, DELETE
    val entityType: String, // application, saved_job, etc.
    val entityId: String,
    val data: String, // JSON string of data
    val priority: Int, // Higher number = higher priority
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val lastRetryAt: Long? = null,
    val errorMessage: String? = null
)

/**
 * Cache metadata
 */
@Entity(tableName = "cache_metadata")
data class CacheMetadata(
    @PrimaryKey
    val key: String,
    val value: String,
    val expiresAt: Long,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Offline sync configuration
 */
data class OfflineSyncConfig(
    val enableAutoSync: Boolean = true,
    val syncIntervalMinutes: Long = 30,
    val maxRetryAttempts: Int = 3,
    val retryDelayMinutes: Long = 5,
    val cacheExpirationHours: Long = 24,
    val maxCacheSizeMB: Long = 100,
    val syncOnWifiOnly: Boolean = false,
    val syncOnChargingOnly: Boolean = false
)

/**
 * Sync result
 */
data class SyncResult(
    val isSuccess: Boolean,
    val syncedCount: Int,
    val failedCount: Int,
    val errorMessage: String?,
    val syncDuration: Long,
    val entityType: String
)

/**
 * Offline status
 */
data class OfflineStatus(
    val isOnline: Boolean,
    val lastSyncTime: Long?,
    val pendingOperations: Int,
    val cacheSize: Long,
    val syncInProgress: Boolean,
    val nextSyncTime: Long?
)

/**
 * Cache statistics
 */
data class CacheStats(
    val totalJobs: Int,
    val totalApplications: Int,
    val totalNotifications: Int,
    val totalSearches: Int,
    val totalSavedJobs: Int,
    val totalRecentlyViewed: Int,
    val cacheSize: Long,
    val lastCleanup: Long?,
    val expiredItems: Int
)
