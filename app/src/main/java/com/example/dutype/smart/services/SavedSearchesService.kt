package com.example.dutype.smart.services

import com.example.dutype.smart.models.*
import com.example.dutype.worker.models.JobCardModel
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
 * Service for managing saved searches
 * Provides functionality to save, manage, and execute saved searches
 */
@Singleton
class SavedSearchesService @Inject constructor() {
    
    // In-memory storage for saved searches (in production, use Room database)
    private val savedSearchesMap = mutableMapOf<String, MutableList<SavedSearch>>()
    private val searchNotifications = mutableMapOf<String, MutableList<SearchNotification>>()
    
    private val _savedSearches = MutableStateFlow<List<SavedSearch>>(emptyList())
    val savedSearches: StateFlow<List<SavedSearch>> = _savedSearches.asStateFlow()
    
    /**
     * Save a search
     */
    suspend fun saveSearch(
        userId: String,
        name: String,
        query: String,
        filters: JobFilters,
        notificationEnabled: Boolean = true
    ): Result<SavedSearch> {
        return try {
            val savedSearch = SavedSearch(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                query = query,
                filters = filters,
                isActive = true,
                notificationEnabled = notificationEnabled,
                createdAt = System.currentTimeMillis(),
                lastUsed = System.currentTimeMillis()
            )
            
            val userSearches = savedSearchesMap.getOrPut(userId) { mutableListOf() }
            userSearches.add(savedSearch)
            _savedSearches.value = savedSearchesMap.values.flatten().toList()
            
            Result.success(savedSearch)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get saved searches for user
     */
    fun getSavedSearches(userId: String): Flow<List<SavedSearch>> = flow {
        val searches = savedSearchesMap[userId] ?: emptyList()
        emit(searches)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Update saved search
     */
    suspend fun updateSavedSearch(
        userId: String,
        searchId: String,
        name: String? = null,
        filters: JobFilters? = null,
        notificationEnabled: Boolean? = null
    ): Result<SavedSearch> {
        return try {
            val userSearches = savedSearchesMap[userId] ?: return Result.failure(Exception("No saved searches found"))
            val index = userSearches.indexOfFirst { it.id == searchId }
            
            if (index == -1) {
                return Result.failure(Exception("Saved search not found"))
            }
            
            val existingSearch = userSearches[index]
            val updatedSearch = existingSearch.copy(
                name = name ?: existingSearch.name,
                filters = filters ?: existingSearch.filters,
                notificationEnabled = notificationEnabled ?: existingSearch.notificationEnabled,
                lastUsed = System.currentTimeMillis()
            )
            
            userSearches[index] = updatedSearch
            _savedSearches.value = savedSearchesMap.values.flatten().toList()
            
            Result.success(updatedSearch)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete saved search
     */
    suspend fun deleteSavedSearch(userId: String, searchId: String): Result<Unit> {
        return try {
            val userSearches = savedSearchesMap[userId] ?: return Result.failure(Exception("No saved searches found"))
            val removed = userSearches.removeAll { it.id == searchId }
            
            if (removed) {
                _savedSearches.value = savedSearchesMap.values.flatten().toList()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Saved search not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Execute saved search
     */
    fun executeSavedSearch(
        userId: String,
        searchId: String,
        availableJobs: List<JobCardModel>
    ): Flow<SavedSearchResult> = flow {
        val userSearches = savedSearchesMap[userId] ?: emptyList()
        val savedSearch = userSearches.find { it.id == searchId }
        
        if (savedSearch == null) {
            emit(SavedSearchResult(
                search = null,
                results = emptyList(),
                newJobsCount = 0,
                error = "Saved search not found"
            ))
            return@flow
        }
        
        // Update last used time
        updateLastUsed(userId, searchId)
        
        // Apply filters to get results
        val filteredJobs = applyFilters(availableJobs, savedSearch.filters)
        
        // Count new jobs since last search
        val newJobsCount = countNewJobs(filteredJobs, savedSearch.lastUsed)
        
        val result = SavedSearchResult(
            search = savedSearch,
            results = filteredJobs,
            newJobsCount = newJobsCount,
            error = null
        )
        
        emit(result)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get search notifications
     */
    fun getSearchNotifications(userId: String): Flow<List<SearchNotification>> = flow {
        val notifications = searchNotifications[userId] ?: emptyList()
        emit(notifications)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Check for new jobs in saved searches
     */
    suspend fun checkForNewJobs(
        userId: String,
        availableJobs: List<JobCardModel>
    ): Result<List<SearchNotification>> {
        return try {
            val userSearches = savedSearchesMap[userId] ?: emptyList()
            val activeSearches = userSearches.filter { it.isActive && it.notificationEnabled }
            val notifications = mutableListOf<SearchNotification>()
            
            activeSearches.forEach { search ->
                val filteredJobs = applyFilters(availableJobs, search.filters)
                val newJobsCount = countNewJobs(filteredJobs, search.lastUsed)
                
                if (newJobsCount > 0) {
                    val notification = SearchNotification(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        searchId = search.id,
                        searchName = search.name,
                        newJobsCount = newJobsCount,
                        createdAt = System.currentTimeMillis(),
                        isRead = false
                    )
                    
                    notifications.add(notification)
                    
                    // Add to user's notifications
                    val userNotifications = searchNotifications.getOrPut(userId) { mutableListOf() }
                    userNotifications.add(notification)
                }
            }
            
            Result.success(notifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark search notification as read
     */
    suspend fun markNotificationAsRead(userId: String, notificationId: String): Result<Unit> {
        return try {
            val userNotifications = searchNotifications[userId] ?: return Result.failure(Exception("No notifications found"))
            val index = userNotifications.indexOfFirst { it.id == notificationId }
            
            if (index != -1) {
                userNotifications[index] = userNotifications[index].copy(isRead = true)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Notification not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get saved search analytics
     */
    fun getSavedSearchAnalytics(userId: String): Flow<SavedSearchAnalytics> = flow {
        val searches = savedSearchesMap[userId] ?: emptyList()
        val notifications = searchNotifications[userId] ?: emptyList()
        
        val totalSearches = searches.size
        val activeSearches = searches.count { it.isActive }
        val notificationEnabledSearches = searches.count { it.notificationEnabled }
        val totalNotifications = notifications.size
        val unreadNotifications = notifications.count { !it.isRead }
        
        val mostUsedSearches = searches
            .sortedByDescending { it.lastUsed }
            .take(5)
            .map { it.name }
        
        val searchFrequency = searches
            .groupingBy { it.createdAt / (7 * 24 * 60 * 60 * 1000) } // Group by week
            .eachCount()
        
        val analytics = SavedSearchAnalytics(
            totalSearches = totalSearches,
            activeSearches = activeSearches,
            notificationEnabledSearches = notificationEnabledSearches,
            totalNotifications = totalNotifications,
            unreadNotifications = unreadNotifications,
            mostUsedSearches = mostUsedSearches,
            searchFrequency = searchFrequency
        )
        
        emit(analytics)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get search suggestions based on saved searches
     */
    fun getSearchSuggestionsFromSaved(userId: String): Flow<List<String>> = flow {
        val searches = savedSearchesMap[userId] ?: emptyList()
        val suggestions = searches
            .filter { it.filters.searchQuery.isNotEmpty() }
            .map { it.filters.searchQuery }
            .distinct()
            .take(10)
        
        emit(suggestions)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Apply filters to job list
     */
    private fun applyFilters(jobs: List<JobCardModel>, filters: JobFilters): List<JobCardModel> {
        var filteredJobs = jobs
        
        // Apply search query filter
        if (filters.searchQuery.isNotEmpty()) {
            filteredJobs = filteredJobs.filter { job ->
                job.title.contains(filters.searchQuery, ignoreCase = true) ||
                job.employerName.contains(filters.searchQuery, ignoreCase = true) ||
                job.description.contains(filters.searchQuery, ignoreCase = true)
            }
        }
        
        // Apply location filter
        if (filters.location.isNotEmpty()) {
            filteredJobs = filteredJobs.filter { job ->
                job.location.getDisplayText().contains(filters.location, ignoreCase = true)
            }
        }
        
        // Apply other filters as needed
        // (Implementation similar to EnhancedFiltersService)
        
        return filteredJobs
    }
    
    /**
     * Count new jobs since last search
     */
    private fun countNewJobs(jobs: List<JobCardModel>, lastUsed: Long): Int {
        return jobs.count { job ->
            job.postedAt > lastUsed
        }
    }
    
    /**
     * Update last used time for saved search
     */
    private suspend fun updateLastUsed(userId: String, searchId: String) {
        val userSearches = savedSearchesMap[userId] ?: return
        val index = userSearches.indexOfFirst { it.id == searchId }
        
        if (index != -1) {
            userSearches[index] = userSearches[index].copy(lastUsed = System.currentTimeMillis())
        }
    }
}

/**
 * Saved search result
 */
data class SavedSearchResult(
    val search: SavedSearch?,
    val results: List<JobCardModel>,
    val newJobsCount: Int,
    val error: String?
)

/**
 * Search notification
 */
data class SearchNotification(
    val id: String,
    val userId: String,
    val searchId: String,
    val searchName: String,
    val newJobsCount: Int,
    val createdAt: Long,
    val isRead: Boolean = false
)

/**
 * Saved search analytics
 */
data class SavedSearchAnalytics(
    val totalSearches: Int = 0,
    val activeSearches: Int = 0,
    val notificationEnabledSearches: Int = 0,
    val totalNotifications: Int = 0,
    val unreadNotifications: Int = 0,
    val mostUsedSearches: List<String> = emptyList(),
    val searchFrequency: Map<Long, Int> = emptyMap()
)
