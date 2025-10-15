package com.example.dutype.smart.repository

import com.example.dutype.smart.models.*
import com.example.dutype.smart.services.*
import com.example.dutype.worker.models.JobCardModel
import com.example.dutype.profile.models.AdvancedProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.List


/**
 * Repository for coordinating smart features
 * Acts as a central hub for all intelligent job matching and recommendation services
 */
@Singleton
class SmartFeaturesRepository @Inject constructor(
    private val jobRecommendationService: JobRecommendationService,
    private val enhancedFiltersService: EnhancedFiltersService,
    private val smartSearchService: SmartSearchService,
    private val recentlyViewedService: RecentlyViewedService,
    private val savedSearchesService: SavedSearchesService,
    private val jobMatchingService: JobMatchingService
) {
    
    /**
     * Get comprehensive job recommendations
     */
    fun getComprehensiveRecommendations(
        userId: String,
        userProfile: AdvancedProfile,
        availableJobs: List<JobCardModel>
    ): Flow<ComprehensiveRecommendations> = flow {
        // Get different types of recommendations
        val skillBasedRecs = jobRecommendationService.getJobRecommendations(userId, userProfile, availableJobs, 10).first()
        val jobMatches = jobMatchingService.getTopJobMatches(userId, 10).first()
        val recentlyViewed = recentlyViewedService.getRecentlyViewedJobs(userId, 5).first()
        
        // Combine recommendations
        val recommendations = ComprehensiveRecommendations(
            skillBasedRecommendations = skillBasedRecs,
            jobMatches = jobMatches,
            recentlyViewed = recentlyViewed,
            totalRecommendations = skillBasedRecs.size + jobMatches.size + recentlyViewed.size
        )
        
        emit(recommendations)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Perform smart search with filters
     */
    fun performSmartSearchWithFilters(
        userId: String,
        query: String,
        filters: JobFilters,
        availableJobs: List<JobCardModel>
    ): Flow<SmartSearchWithFiltersResult> = flow {
        // Apply filters first
        val filteredJobs = enhancedFiltersService.filterJobs(availableJobs, filters).first()
        
        // Perform smart search on filtered results
        val searchResult = smartSearchService.performSmartSearch(userId, query, filters, filteredJobs).first()
        
        // Get related searches
        val relatedSearches = smartSearchService.getSearchSuggestions(query, 5).first()
        
        val result = SmartSearchWithFiltersResult(
            searchResult = searchResult.results,
            filteredJobs = filteredJobs,
            relatedSearches = relatedSearches,
            totalResults = filteredJobs.size
        )
        
        emit(result)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get personalized job feed
     */
    fun getPersonalizedJobFeed(
        userId: String,
        userProfile: AdvancedProfile,
        availableJobs: List<JobCardModel>
    ): Flow<PersonalizedJobFeed> = flow {
        // Calculate job matches
        val jobMatches = jobMatchingService.calculateJobMatches(userId, userProfile, availableJobs).first()
        // Get recommendations
        val recommendations = jobRecommendationService.getJobRecommendations(userId, userProfile, availableJobs, 15).first()
        
        // Get recently viewed
        val recentlyViewed = recentlyViewedService.getRecentlyViewedJobsWithDetails(userId, availableJobs, 5).first()
        
        // Get trending jobs
        val trendingJobs = getTrendingJobs(availableJobs)
        
        val feed = PersonalizedJobFeed(
            recommendedJobs = recommendations,
            matchedJobs = jobMatches,
            recentlyViewed = recentlyViewed,
            trendingJobs = trendingJobs,
            totalJobs = availableJobs.size
        )
        
        emit(feed)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Track job interaction
     */
    suspend fun trackJobInteraction(
        userId: String,
        jobId: String,
        interactionType: JobInteractionType,
        source: ViewSource = ViewSource.DIRECT
    ): Result<Unit> {
        return try {
            when (interactionType) {
                JobInteractionType.VIEW -> {
                    recentlyViewedService.trackJobView(userId, jobId, source)
                }
                JobInteractionType.APPLY -> {
                    // Track application
                    Result.success(Unit)
                }
                JobInteractionType.SAVE -> {
                    // Track save
                    Result.success(Unit)
                }
                JobInteractionType.SHARE -> {
                    // Track share
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get smart insights
     */
    fun getSmartInsights(userId: String): Flow<SmartInsights> = flow {
        // Get various analytics
        val searchAnalytics = smartSearchService.getSearchAnalytics(userId).first()
        val viewingAnalytics = recentlyViewedService.getViewingAnalytics(userId).first()
        val savedSearchAnalytics = savedSearchesService.getSavedSearchAnalytics(userId).first()
        val recommendationStats = jobRecommendationService.getRecommendationStats(userId)
        
        val insights = SmartInsights(
            searchAnalytics = searchAnalytics,
            viewingAnalytics = viewingAnalytics,
            savedSearchAnalytics = savedSearchAnalytics,
            recommendationStats = recommendationStats,
            insights = generateInsights(searchAnalytics, viewingAnalytics, savedSearchAnalytics)
        )
        
        emit(insights)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get filter suggestions
     */
    fun getFilterSuggestions(availableJobs: List<JobCardModel>): Flow<FilterSuggestions> = flow {
        enhancedFiltersService.getFilterSuggestions(availableJobs).collect { suggestions ->
            emit(suggestions)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get popular filters
     */
    fun getPopularFilters(): Flow<List<PopularFilter>> = flow {
        enhancedFiltersService.getPopularFilters().collect { filters ->
            emit(filters)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Save search
     */
    suspend fun saveSearch(
        userId: String,
        name: String,
        query: String,
        filters: JobFilters,
        notificationEnabled: Boolean = true
    ): Result<SavedSearch> {
        return savedSearchesService.saveSearch(userId, name, query, filters, notificationEnabled)
    }
    
    /**
     * Get saved searches
     */
    fun getSavedSearches(userId: String): Flow<List<SavedSearch>> = flow {
        savedSearchesService.getSavedSearches(userId).collect { searches ->
            emit(searches)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get search suggestions
     */
    fun getSearchSuggestions(query: String, limit: Int = 10): Flow<List<SearchSuggestion>> = flow {
        smartSearchService.getSearchSuggestions(query, limit).collect { suggestions ->
            emit(suggestions)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get trending jobs
     */
    private fun getTrendingJobs(availableJobs: List<JobCardModel>): List<JobCardModel> {
        // Sort by trending score (simplified)
        return availableJobs.sortedByDescending { job ->
            when {
                job.title.contains("Senior", ignoreCase = true) -> 0.9
                job.title.contains("Lead", ignoreCase = true) -> 0.8
                job.title.contains("Manager", ignoreCase = true) -> 0.7
                else -> 0.5
            }
        }.take(10)
    }
    
    /**
     * Generate insights from analytics
     */
    private fun generateInsights(
        searchAnalytics: SearchAnalytics,
        viewingAnalytics: ViewingAnalytics,
        savedSearchAnalytics: SavedSearchAnalytics
    ): List<String> {
        val insights = mutableListOf<String>()
        
        // Search insights
        if (searchAnalytics.totalSearches > 0) {
            insights.add("You've performed ${searchAnalytics.totalSearches} searches with ${searchAnalytics.uniqueQueries} unique queries.")
        }
        
        // Viewing insights
        if (viewingAnalytics.totalViews > 0) {
            insights.add("You've viewed ${viewingAnalytics.totalViews} jobs with an average viewing time of ${(viewingAnalytics.averageViewDuration / 1000).toInt()} seconds.")
        }
        
        // Saved search insights
        if (savedSearchAnalytics.totalSearches > 0) {
            insights.add("You have ${savedSearchAnalytics.totalSearches} saved searches, with ${savedSearchAnalytics.notificationEnabledSearches} notifications enabled.")
        }
        
        // Recommendations
        if (searchAnalytics.totalSearches > 10) {
            insights.add("Consider using saved searches to get notified about new jobs matching your criteria.")
        }
        
        if (viewingAnalytics.averageViewDuration < 10000) { // Less than 10 seconds
            insights.add("Try spending more time reviewing job details to find better matches.")
        }
        
        return insights
    }
}

/**
 * Comprehensive recommendations
 */
data class ComprehensiveRecommendations(
    val skillBasedRecommendations: List<JobRecommendation>,
    val jobMatches: List<JobMatchScore>,
    val recentlyViewed: List<RecentlyViewedJob>,
    val totalRecommendations: Int
)


/**
 * Personalized job feed
 */
data class PersonalizedJobFeed(
    val recommendedJobs: List<JobRecommendation>,
    val matchedJobs: List<JobMatchScore>,
    val recentlyViewed: List<RecentlyViewedJobWithDetails>,
    val trendingJobs: List<JobCardModel>,
    val totalJobs: Int
)

/**
 * Job interaction types
 */
enum class JobInteractionType {
    VIEW,
    APPLY,
    SAVE,
    SHARE
}

/**
 * Smart insights
 */
data class SmartInsights(
    val searchAnalytics: SearchAnalytics,
    val viewingAnalytics: ViewingAnalytics,
    val savedSearchAnalytics: SavedSearchAnalytics,
    val recommendationStats: RecommendationStats,
    val insights: List<String>
)

/**
 * Smart search with filters result
 */
data class SmartSearchWithFiltersResult(
    val searchResult: List<JobCardModel>,
    val filteredJobs: List<JobCardModel>,
    val relatedSearches: List<SearchSuggestion>,
    val totalResults: Int
)
