package com.example.partimes.offline.services

import com.example.partimes.offline.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing offline functionality
 * Handles caching, sync, and offline operations
 */
@Singleton
class OfflineService @Inject constructor() {
    
    // In-memory storage for offline data (in production, use Room database)
    private val cachedJobs = mutableMapOf<String, CachedJob>()
    private val cachedUserProfile = mutableMapOf<String, CachedUserProfile>()
    private val cachedApplications = mutableMapOf<String, CachedApplication>()
    private val cachedNotifications = mutableMapOf<String, CachedNotification>()
    private val cachedSearches = mutableMapOf<String, CachedSearch>()
    private val cachedSavedJobs = mutableMapOf<String, CachedSavedJob>()
    private val cachedRecentlyViewed = mutableMapOf<String, CachedRecentlyViewed>()
    private val offlineQueue = mutableListOf<OfflineQueueItem>()
    private val syncStatus = mutableMapOf<String, SyncStatus>()
    
    private val _offlineStatus = MutableStateFlow(OfflineStatus(
        isOnline = true,
        lastSyncTime = null,
        pendingOperations = 0,
        cacheSize = 0,
        syncInProgress = false,
        nextSyncTime = null
    ))
    val offlineStatus: StateFlow<OfflineStatus> = _offlineStatus.asStateFlow()
    
    private val syncConfig = OfflineSyncConfig()
    
    /**
     * Cache job data
     */
    suspend fun cacheJob(job: CachedJob): Result<Unit> {
        return try {
            cachedJobs[job.jobId] = job
            updateCacheSize()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get cached job
     */
    fun getCachedJob(jobId: String): Flow<CachedJob?> = flow {
        val job = cachedJobs[jobId]
        if (job != null && job.expiresAt > System.currentTimeMillis()) {
            emit(job)
        } else {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get all cached jobs
     */
    fun getAllCachedJobs(): Flow<List<CachedJob>> = flow {
        val currentTime = System.currentTimeMillis()
        val validJobs = cachedJobs.values.filter { it.expiresAt > currentTime }
        emit(validJobs)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Cache user profile
     */
    suspend fun cacheUserProfile(profile: CachedUserProfile): Result<Unit> {
        return try {
            cachedUserProfile[profile.userId] = profile
            updateCacheSize()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get cached user profile
     */
    fun getCachedUserProfile(userId: String): Flow<CachedUserProfile?> = flow {
        emit(cachedUserProfile[userId])
    }.flowOn(Dispatchers.IO)
    
    /**
     * Cache application
     */
    suspend fun cacheApplication(application: CachedApplication): Result<Unit> {
        return try {
            cachedApplications[application.applicationId] = application
            updateCacheSize()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get cached applications
     */
    fun getCachedApplications(userId: String): Flow<List<CachedApplication>> = flow {
        val userApplications = cachedApplications.values.filter { it.userId == userId }
        emit(userApplications)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Cache notification
     */
    suspend fun cacheNotification(notification: CachedNotification): Result<Unit> {
        return try {
            cachedNotifications[notification.notificationId] = notification
            updateCacheSize()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get cached notifications
     */
    fun getCachedNotifications(userId: String): Flow<List<CachedNotification>> = flow {
        val userNotifications = cachedNotifications.values.filter { it.userId == userId }
        emit(userNotifications)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Cache search
     */
    suspend fun cacheSearch(search: CachedSearch): Result<Unit> {
        return try {
            cachedSearches[search.searchId] = search
            updateCacheSize()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get cached searches
     */
    fun getCachedSearches(userId: String): Flow<List<CachedSearch>> = flow {
        val userSearches = cachedSearches.values.filter { it.userId == userId }
        emit(userSearches)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Cache saved job
     */
    suspend fun cacheSavedJob(savedJob: CachedSavedJob): Result<Unit> {
        return try {
            cachedSavedJobs[savedJob.savedJobId] = savedJob
            updateCacheSize()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get cached saved jobs
     */
    fun getCachedSavedJobs(userId: String): Flow<List<CachedSavedJob>> = flow {
        val userSavedJobs = cachedSavedJobs.values.filter { it.userId == userId }
        emit(userSavedJobs)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Cache recently viewed job
     */
    suspend fun cacheRecentlyViewed(recentlyViewed: CachedRecentlyViewed): Result<Unit> {
        return try {
            cachedRecentlyViewed[recentlyViewed.viewId] = recentlyViewed
            updateCacheSize()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get cached recently viewed jobs
     */
    fun getCachedRecentlyViewed(userId: String): Flow<List<CachedRecentlyViewed>> = flow {
        val userRecentlyViewed = cachedRecentlyViewed.values
            .filter { it.userId == userId }
            .sortedByDescending { it.viewedAt }
        emit(userRecentlyViewed)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Add operation to offline queue
     */
    suspend fun addToOfflineQueue(
        operationType: String,
        entityType: String,
        entityId: String,
        data: String,
        priority: Int = 1
    ): Result<Unit> {
        return try {
            val queueItem = OfflineQueueItem(
                operationType = operationType,
                entityType = entityType,
                entityId = entityId,
                data = data,
                priority = priority,
                createdAt = System.currentTimeMillis()
            )
            
            offlineQueue.add(queueItem)
            offlineQueue.sortByDescending { it.priority }
            
            updateOfflineStatus()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get offline queue
     */
    fun getOfflineQueue(): Flow<List<OfflineQueueItem>> = flow {
        emit(offlineQueue.toList())
    }.flowOn(Dispatchers.IO)
    
    /**
     * Process offline queue
     */
    suspend fun processOfflineQueue(): Result<SyncResult> {
        return try {
            if (offlineQueue.isEmpty()) {
                return Result.success(SyncResult(
                    isSuccess = true,
                    syncedCount = 0,
                    failedCount = 0,
                    errorMessage = null,
                    syncDuration = 0,
                    entityType = "queue"
                ))
            }
            
            val startTime = System.currentTimeMillis()
            var syncedCount = 0
            var failedCount = 0
            val failedItems = mutableListOf<OfflineQueueItem>()
            
            for (item in offlineQueue.toList()) {
                try {
                    // Simulate processing the item
                    processQueueItem(item)
                    syncedCount++
                } catch (e: Exception) {
                    failedCount++
                    val updatedItem = item.copy(
                        retryCount = item.retryCount + 1,
                        lastRetryAt = System.currentTimeMillis(),
                        errorMessage = e.message
                    )
                    
                    if (updatedItem.retryCount < updatedItem.maxRetries) {
                        failedItems.add(updatedItem)
                    }
                }
            }
            
            // Remove processed items
            offlineQueue.clear()
            offlineQueue.addAll(failedItems)
            
            val syncDuration = System.currentTimeMillis() - startTime
            val result = SyncResult(
                isSuccess = failedCount == 0,
                syncedCount = syncedCount,
                failedCount = failedCount,
                errorMessage = if (failedCount > 0) "Some operations failed" else null,
                syncDuration = syncDuration,
                entityType = "queue"
            )
            
            updateOfflineStatus()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Process individual queue item
     */
    private suspend fun processQueueItem(item: OfflineQueueItem) {
        // In a real app, this would make actual API calls
        // For demo, just simulate processing
        when (item.entityType) {
            "application" -> {
                // Process application
            }
            "saved_job" -> {
                // Process saved job
            }
            "notification" -> {
                // Process notification
            }
        }
    }
    
    /**
     * Clear expired cache
     */
    suspend fun clearExpiredCache(): Result<Int> {
        return try {
            val currentTime = System.currentTimeMillis()
            var clearedCount = 0
            
            // Clear expired jobs
            val expiredJobs = cachedJobs.values.filter { it.expiresAt <= currentTime }
            expiredJobs.forEach { cachedJobs.remove(it.jobId) }
            clearedCount += expiredJobs.size
            
            // Clear old searches (older than 7 days)
            val oldSearches = cachedSearches.values.filter { 
                System.currentTimeMillis() - it.searchedAt > 7 * 24 * 60 * 60 * 1000L 
            }
            oldSearches.forEach { cachedSearches.remove(it.searchId) }
            clearedCount += oldSearches.size
            
            // Clear old recently viewed (older than 30 days)
            val oldRecentlyViewed = cachedRecentlyViewed.values.filter { 
                System.currentTimeMillis() - it.viewedAt > 30 * 24 * 60 * 60 * 1000L 
            }
            oldRecentlyViewed.forEach { cachedRecentlyViewed.remove(it.viewId) }
            clearedCount += oldRecentlyViewed.size
            
            updateCacheSize()
            Result.success(clearedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): Flow<CacheStats> = flow {
        val stats = CacheStats(
            totalJobs = cachedJobs.size,
            totalApplications = cachedApplications.size,
            totalNotifications = cachedNotifications.size,
            totalSearches = cachedSearches.size,
            totalSavedJobs = cachedSavedJobs.size,
            totalRecentlyViewed = cachedRecentlyViewed.size,
            cacheSize = calculateCacheSize(),
            lastCleanup = null,
            expiredItems = countExpiredItems()
        )
        emit(stats)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Update offline status
     */
    private fun updateOfflineStatus() {
        _offlineStatus.value = OfflineStatus(
            isOnline = true, // In a real app, check network connectivity
            lastSyncTime = syncStatus.values.maxOfOrNull { it.lastSyncTime },
            pendingOperations = offlineQueue.size,
            cacheSize = calculateCacheSize(),
            syncInProgress = false,
            nextSyncTime = null
        )
    }
    
    /**
     * Update cache size
     */
    private fun updateCacheSize() {
        val currentStatus = _offlineStatus.value
        _offlineStatus.value = currentStatus.copy(
            cacheSize = calculateCacheSize(),
            pendingOperations = offlineQueue.size
        )
    }
    
    /**
     * Calculate cache size
     */
    private fun calculateCacheSize(): Long {
        // Simplified calculation - in real app, calculate actual size
        return (cachedJobs.size + cachedApplications.size + cachedNotifications.size + 
                cachedSearches.size + cachedSavedJobs.size + cachedRecentlyViewed.size).toLong()
    }
    
    /**
     * Count expired items
     */
    private fun countExpiredItems(): Int {
        val currentTime = System.currentTimeMillis()
        return cachedJobs.values.count { it.expiresAt <= currentTime }
    }
    
    /**
     * Set online status
     */
    fun setOnlineStatus(isOnline: Boolean) {
        val currentStatus = _offlineStatus.value
        _offlineStatus.value = currentStatus.copy(isOnline = isOnline)
    }
    
    /**
     * Get sync status
     */
    fun getSyncStatus(entityType: String): Flow<SyncStatus?> = flow {
        emit(syncStatus[entityType])
    }.flowOn(Dispatchers.IO)
    
    /**
     * Update sync status
     */
    suspend fun updateSyncStatus(
        entityType: String,
        status: String,
        errorMessage: String? = null,
        recordCount: Int = 0,
        syncedCount: Int = 0,
        failedCount: Int = 0
    ): Result<Unit> {
        return try {
            syncStatus[entityType] = SyncStatus(
                entityType = entityType,
                lastSyncTime = System.currentTimeMillis(),
                syncStatus = status,
                errorMessage = errorMessage,
                recordCount = recordCount,
                syncedCount = syncedCount,
                failedCount = failedCount
            )
            updateOfflineStatus()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
