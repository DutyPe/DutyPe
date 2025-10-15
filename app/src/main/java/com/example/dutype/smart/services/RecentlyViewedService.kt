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
 * Service for tracking recently viewed jobs
 * Provides insights and recommendations based on viewing behavior
 */
@Singleton
class RecentlyViewedService @Inject constructor() {
    
    // In-memory storage for recently viewed jobs (in production, use Room database)
    private val recentlyViewedJobs = mutableMapOf<String, MutableList<RecentlyViewedJob>>()
    private val userBehavior = mutableMapOf<String, UserBehavior>()
    
    private val _recentlyViewed = MutableStateFlow<List<RecentlyViewedJob>>(emptyList())
    val recentlyViewed: StateFlow<List<RecentlyViewedJob>> = _recentlyViewed.asStateFlow()
    
    /**
     * Track job view
     */
    suspend fun trackJobView(
        userId: String,
        jobId: String,
        source: ViewSource = ViewSource.DIRECT,
        duration: Long = 0
    ): Result<Unit> {
        return try {
            val recentlyViewed = RecentlyViewedJob(
                id = UUID.randomUUID().toString(),
                jobId = jobId,
                userId = userId,
                viewedAt = System.currentTimeMillis(),
                viewDuration = duration
            )
            
            val userViews = recentlyViewedJobs.getOrPut(userId) { mutableListOf() }
            
            // Remove existing entry for same job to avoid duplicates
            userViews.removeAll { it.jobId == jobId }
            
            // Add new entry at the beginning
            userViews.add(0, recentlyViewed)
            
            // Keep only last 50 viewed jobs
            if (userViews.size > 50) {
                userViews.removeAt(userViews.size - 1)
            }
            
            _recentlyViewed.value = userViews.toList()
            
            // Update user behavior
            updateUserBehavior(userId, jobId, source, duration)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get recently viewed jobs for user
     */
    fun getRecentlyViewedJobs(userId: String, limit: Int = 20): Flow<List<RecentlyViewedJob>> = flow {
        val views = recentlyViewedJobs[userId]?.take(limit) ?: emptyList()
        emit(views)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get recently viewed jobs with job details
     */
    fun getRecentlyViewedJobsWithDetails(
        userId: String,
        availableJobs: List<JobCardModel>,
        limit: Int = 20
    ): Flow<List<RecentlyViewedJobWithDetails>> = flow {
        val views = recentlyViewedJobs[userId]?.take(limit) ?: emptyList()
        
        val jobsWithDetails = views.mapNotNull { view ->
            val job = availableJobs.find { it.jobId == view.jobId }
            if (job != null) {
                RecentlyViewedJobWithDetails(
                    recentlyViewed = view,
                    job = job
                )
            } else null
        }
        
        emit(jobsWithDetails)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Clear recently viewed jobs
     */
    suspend fun clearRecentlyViewedJobs(userId: String): Result<Unit> {
        return try {
            recentlyViewedJobs[userId]?.clear()
            _recentlyViewed.value = emptyList()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Remove specific job from recently viewed
     */
    suspend fun removeFromRecentlyViewed(userId: String, jobId: String): Result<Unit> {
        return try {
            val userViews = recentlyViewedJobs[userId] ?: return Result.failure(Exception("No views found"))
            val removed = userViews.removeAll { it.jobId == jobId }
            
            if (removed) {
                _recentlyViewed.value = userViews.toList()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Job not found in recently viewed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get viewing analytics
     */
    fun getViewingAnalytics(userId: String): Flow<ViewingAnalytics> = flow {
        val views = recentlyViewedJobs[userId] ?: emptyList()
        val behavior = userBehavior[userId] ?: UserBehavior(userId = userId)
        
        val totalViews = views.size
        val uniqueJobs = views.map { it.jobId }.distinct().size
        val averageViewDuration = if (views.isNotEmpty()) {
            views.map { it.viewDuration }.average()
        } else 0.0
        
        val viewsBySource = views.groupingBy { it.viewDuration }.eachCount()
        val viewsByDay = views.groupingBy { 
            it.viewedAt / (24 * 60 * 60 * 1000) 
        }.eachCount()
        
        val mostViewedJobs = views
            .groupingBy { it.jobId }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
        
        val analytics = ViewingAnalytics(
            totalViews = totalViews,
            uniqueJobs = uniqueJobs,
            averageViewDuration = averageViewDuration,
            viewsBySource = viewsBySource,
            viewsByDay = viewsByDay,
            mostViewedJobs = mostViewedJobs,
            lastViewedAt = views.maxOfOrNull { it.viewedAt }
        )
        
        emit(analytics)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get similar jobs based on viewing history
     */
    fun getSimilarJobsBasedOnHistory(
        userId: String,
        availableJobs: List<JobCardModel>,
        limit: Int = 10
    ): Flow<List<JobCardModel>> = flow {
        val views = recentlyViewedJobs[userId] ?: emptyList()
        
        if (views.isEmpty()) {
            emit(emptyList())
            return@flow
        }
        
        // Get recently viewed job IDs
        val viewedJobIds = views.take(10).map { it.jobId }
        val viewedJobs = availableJobs.filter { it.jobId in viewedJobIds }
        
        // Find similar jobs based on viewed jobs
        val similarJobs = findSimilarJobs(viewedJobs, availableJobs, limit)
        
        emit(similarJobs)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get viewing insights
     */
    fun getViewingInsights(userId: String): Flow<List<String>> = flow {
        val views = recentlyViewedJobs[userId] ?: emptyList()
        val insights = mutableListOf<String>()
        
        if (views.isEmpty()) {
            insights.add("Start browsing jobs to see your viewing history here.")
            insights.add("Your viewing patterns will help us recommend better jobs for you.")
        } else {
            val totalViews = views.size
            val uniqueJobs = views.map { it.jobId }.distinct().size
            
            insights.add("You've viewed $totalViews jobs, with $uniqueJobs unique positions.")
            
            val averageDuration = views.map { it.viewDuration }.average()
            if (averageDuration > 30000) { // 30 seconds
                insights.add("You spend good time reviewing job details - great for finding the right fit!")
            } else {
                insights.add("Consider spending more time reviewing job details for better matches.")
            }
            
            val recentViews = views.take(5)
            val companies = recentViews.mapNotNull { view ->
                // In a real app, get company name from job data
                "Company ${view.jobId.takeLast(2)}"
            }.distinct()
            
            if (companies.isNotEmpty()) {
                insights.add("Recently viewed companies: ${companies.joinToString(", ")}")
            }
            
            val todayViews = views.count { 
                System.currentTimeMillis() - it.viewedAt < 24 * 60 * 60 * 1000 
            }
            if (todayViews > 0) {
                insights.add("You viewed $todayViews jobs today - active job searching!")
            }
        }
        
        emit(insights)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Update user behavior tracking
     */
    private fun updateUserBehavior(userId: String, jobId: String, source: ViewSource, duration: Long) {
        val behavior = userBehavior.getOrPut(userId) { UserBehavior(userId = userId) }
        
        val jobView = JobView(
            jobId = jobId,
            viewedAt = System.currentTimeMillis(),
            duration = duration,
            source = source
        )
        
        behavior.jobViews = behavior.jobViews + jobView
        behavior.lastActive = System.currentTimeMillis()
        
        // Keep only last 100 views
        if (behavior.jobViews.size > 100) {
            behavior.jobViews = behavior.jobViews.takeLast(100)
        }
    }
    
    /**
     * Find similar jobs based on viewed jobs
     */
    private fun findSimilarJobs(
        viewedJobs: List<JobCardModel>,
        availableJobs: List<JobCardModel>,
        limit: Int
    ): List<JobCardModel> {
        if (viewedJobs.isEmpty()) return emptyList()
        
        val similarJobs = mutableListOf<ScoredJob>()
        
        availableJobs.forEach { job ->
            if (viewedJobs.any { it.jobId == job.jobId }) return@forEach // Skip already viewed
            
            var score = 0.0
            
            // Check for similar titles
            viewedJobs.forEach { viewedJob ->
                if (job.title.lowercase().contains(viewedJob.title.lowercase().split(" ").first())) {
                    score += 5.0
                }
            }
            
            // Check for same company
            viewedJobs.forEach { viewedJob ->
                if (job.employerName == viewedJob.employerName) {
                    score += 10.0
                }
            }
            
            // Check for similar location
            viewedJobs.forEach { viewedJob ->
                if (job.location.getDisplayText() == viewedJob.location.getDisplayText()) {
                    score += 3.0
                }
            }
            
            // Check for similar tags
            viewedJobs.forEach { viewedJob ->
                val commonTags = job.tags.intersect(viewedJob.tags.toSet()).size
                score += commonTags * 2.0
            }
            
            if (score > 0) {
                similarJobs.add(ScoredJob(job, score))
            }
        }
        
        return similarJobs
            .sortedByDescending { it.score }
            .take(limit)
            .map { it.job }
    }
}

/**
 * Recently viewed job with details
 */
data class RecentlyViewedJobWithDetails(
    val recentlyViewed: RecentlyViewedJob,
    val job: JobCardModel
)

/**
 * Viewing analytics
 */
data class ViewingAnalytics(
    val totalViews: Int = 0,
    val uniqueJobs: Int = 0,
    val averageViewDuration: Double = 0.0,
    val viewsBySource: Map<Long, Int> = emptyMap(),
    val viewsByDay: Map<Long, Int> = emptyMap(),
    val mostViewedJobs: List<String> = emptyList(),
    val lastViewedAt: Long? = null
)

