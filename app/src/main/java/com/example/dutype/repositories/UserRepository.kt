package com.example.dutype.repositories

import com.example.dutype.cache.JobCacheManager
import com.example.dutype.models.User
import com.example.dutype.models.UserSummary
import com.example.dutype.performance.MainThreadChecker
import com.example.dutype.services.FirestoreService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * UserRepository - Handles user data with lazy loading support
 * 
 * PERFORMANCE OPTIMIZATION:
 * - Uses UserSummary for list views (application lists, chat lists)
 * - Full User data fetched only for detail screens
 * - Caches user summaries to reduce Firestore reads
 */
@Singleton
class UserRepository @Inject constructor(
    private val firestoreService: FirestoreService,
    private val auth: FirebaseAuth,
    private val cacheManager: JobCacheManager
) {
    
    /**
     * Get user summary for list views (lightweight)
     * Uses cache when available
     */
    fun getUserSummary(userId: String): Flow<Result<UserSummary?>> = flow {
        MainThreadChecker.assertBackgroundThread("UserRepository.getUserSummary")
        
        // Check cache first
        val cachedSummary = cacheManager.getUserSummaryCached(userId)
        if (cachedSummary != null) {
            Timber.d("✅ Returning user summary from cache: $userId")
            emit(Result.success(cachedSummary))
            return@flow
        }
        
        try {
            val result = firestoreService.getUserSummary(userId)
            result.fold(
                onSuccess = { summaryData ->
                    if (summaryData != null) {
                        val summary = UserSummary.fromMap(summaryData as Map<String, Any>)
                        // Cache the summary
                        cacheManager.cacheUserSummary(summary)
                        emit(Result.success(summary))
                    } else {
                        emit(Result.success(null))
                    }
                },
                onFailure = { exception ->
                    emit(Result.failure(exception))
                }
            )
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get multiple user summaries (batch operation)
     * Optimized for application lists where we need multiple user summaries
     */
    fun getUserSummaries(userIds: List<String>): Flow<Result<List<UserSummary>>> = flow {
        MainThreadChecker.assertBackgroundThread("UserRepository.getUserSummaries")
        
        if (userIds.isEmpty()) {
            emit(Result.success(emptyList()))
            return@flow
        }
        
        // Check cache first
        val cachedSummaries = cacheManager.getUserSummariesCached(userIds)
        val missingIds = userIds.filter { it !in cachedSummaries.keys }
        
        if (missingIds.isEmpty()) {
            // All summaries are cached
            Timber.d("✅ All ${userIds.size} user summaries from cache")
            emit(Result.success(cachedSummaries.values.toList()))
            return@flow
        }
        
        try {
            // Fetch missing summaries from Firestore
            val result = firestoreService.getUserSummaries(missingIds)
            result.fold(
                onSuccess = { summariesData ->
                    val fetchedSummaries = summariesData.map { UserSummary.fromMap(it as Map<String, Any>) }
                    
                    // Cache the fetched summaries
                    cacheManager.cacheUserSummaries(fetchedSummaries)
                    
                    // Combine cached and fetched summaries
                    val allSummaries = cachedSummaries.values.toList() + fetchedSummaries
                    
                    Timber.d("📦 Loaded ${fetchedSummaries.size} user summaries (${cachedSummaries.size} from cache)")
                    emit(Result.success(allSummaries))
                },
                onFailure = { exception ->
                    // Return cached summaries even if fetch fails
                    if (cachedSummaries.isNotEmpty()) {
                        emit(Result.success(cachedSummaries.values.toList()))
                    } else {
                        emit(Result.failure(exception))
                    }
                }
            )
        } catch (e: Exception) {
            // Return cached summaries even if fetch fails
            if (cachedSummaries.isNotEmpty()) {
                emit(Result.success(cachedSummaries.values.toList()))
            } else {
                emit(Result.failure(e))
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get full user data for detail screens
     */
    fun getFullUser(userId: String): Flow<Result<User?>> = flow {
        try {
            val result = firestoreService.getUserById(userId)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get current user's full data
     */
    fun getCurrentUser(): Flow<Result<User?>> = flow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            emit(Result.success(null))
            return@flow
        }
        
        try {
            val result = firestoreService.getUserById(currentUserId)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Clear user summaries cache
     */
    suspend fun clearUserCache() {
        cacheManager.clearUserSummariesCache()
    }
}
