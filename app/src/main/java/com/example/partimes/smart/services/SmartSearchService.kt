package com.example.partimes.smart.services

import com.example.partimes.smart.models.*
import com.example.partimes.jobseeker.models.JobCardModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first // Make sure to import thisimport javax.inject.Inject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for intelligent search with autocomplete and suggestions
 * Provides smart search capabilities with learning from user behavior
 */
@Singleton
class SmartSearchService @Inject constructor() {
    
    // In-memory storage for search data (in production, use Room database)
    private val searchHistory = mutableMapOf<String, MutableList<SearchQuery>>()
    private val searchSuggestions = mutableListOf<SearchSuggestion>()
    private val popularSearches = mutableMapOf<String, Int>()
    
    init {
        initializeSearchSuggestions()
    }
    
    /**
     * Get search suggestions based on query
     */
    fun getSearchSuggestions(query: String, limit: Int = 10): Flow<List<SearchSuggestion>> = flow {
        if (query.isEmpty()) {
            // Return popular searches when query is empty
            val popular = searchSuggestions
                .sortedByDescending { it.popularity }
                .take(limit)
            emit(popular)
        } else {
            // Filter suggestions based on query
            val filtered = searchSuggestions
                .filter { suggestion ->
                    suggestion.text.contains(query, ignoreCase = true)
                }
                .sortedWith(compareByDescending<SearchSuggestion> { it.popularity }
                    .thenBy { it.text.length })
                .take(limit)
            emit(filtered)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get autocomplete suggestions
     */
    fun getAutocompleteSuggestions(query: String, limit: Int = 5): Flow<List<String>> = flow {
        val suggestions = searchSuggestions
            .filter { suggestion ->
                suggestion.text.startsWith(query, ignoreCase = true)
            }
            .sortedByDescending { it.popularity }
            .map { it.text }
            .distinct()
            .take(limit)
        
        emit(suggestions)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Perform smart search
     */
    fun performSmartSearch(
        userId: String,
        query: String,
        filters: JobFilters,
        availableJobs: List<JobCardModel>
    ): Flow<SmartSearchResult> = flow {
        val startTime = System.currentTimeMillis()
        
        // Record search query
        recordSearchQuery(userId, query, filters, availableJobs.size)
        
        // Perform search with ranking
        val searchResults = performSearchWithRanking(query, filters, availableJobs)
        
        // Get related searches
        val relatedSearches = getRelatedSearches(query)
        
        // Get search insights
        val insights = generateSearchInsights(query, searchResults)
        val searchSuggestionsList = getSearchSuggestions(query,5).first() // Collect the list from the Flow
        val result = SmartSearchResult(
            query = query,
            results = searchResults,
            totalCount = searchResults.size,
            relatedSearches = relatedSearches,
            insights = insights,
            searchTime = System.currentTimeMillis() - startTime,
            suggestions = searchSuggestionsList
        )
        
        emit(result)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get search history for user
     */
    fun getSearchHistory(userId: String, limit: Int = 20): Flow<List<SearchQuery>> = flow {
        val history = searchHistory[userId]?.takeLast(limit)?.reversed() ?: emptyList()
        emit(history)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Clear search history
     */
    suspend fun clearSearchHistory(userId: String): Result<Unit> {
        return try {
            searchHistory[userId]?.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get trending searches
     */
    fun getTrendingSearches(limit: Int = 10): Flow<List<SearchSuggestion>> = flow {
        val trending = searchSuggestions
            .sortedByDescending { it.popularity }
            .take(limit)
        emit(trending)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get search analytics
     */
    fun getSearchAnalytics(userId: String): Flow<SearchAnalytics> = flow {
        val userHistory = searchHistory[userId] ?: emptyList()
        
        val totalSearches = userHistory.size
        val uniqueQueries = userHistory.map { it.query }.distinct().size
        val averageResults = if (userHistory.isNotEmpty()) {
            userHistory.map { it.resultCount }.average()
        } else 0.0
        
        val mostSearchedTerms = userHistory
            .groupingBy { it.query }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
        
        val searchFrequency = userHistory
            .groupingBy { it.searchedAt / (24 * 60 * 60 * 1000) } // Group by day
            .eachCount()
        
        val analytics = SearchAnalytics(
            totalSearches = totalSearches,
            uniqueQueries = uniqueQueries,
            averageResults = averageResults,
            mostSearchedTerms = mostSearchedTerms,
            searchFrequency = searchFrequency,
            lastSearchAt = userHistory.maxOfOrNull { it.searchedAt }
        )
        
        emit(analytics)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Perform search with intelligent ranking
     */
    private fun performSearchWithRanking(
        query: String,
        filters: JobFilters,
        availableJobs: List<JobCardModel>
    ): List<JobCardModel> {
        val scoredJobs = availableJobs.map { job ->
            val score = calculateSearchScore(query, job)
            ScoredJob(job, score)
        }
        
        return scoredJobs
            .sortedByDescending { it.score }
            .map { it.job }
    }
    
    /**
     * Calculate search score for a job
     */
    private fun calculateSearchScore(query: String, job: JobCardModel): Double {
        var score = 0.0
        val queryLower = query.lowercase()
        
        // Title match (highest weight)
        if (job.title.lowercase().contains(queryLower)) {
            score += 10.0
        }
        
        // Company name match
        if (job.employerName.lowercase().contains(queryLower)) {
            score += 8.0
        }
        
        // Description match
        if (job.description.lowercase().contains(queryLower)) {
            score += 5.0
        }
        
        // Tags match
        val tagMatches = job.tags.count { tag ->
            tag.text.lowercase().contains(queryLower)
        }
        score += tagMatches * 3.0
        
        // Location match
        if (job.location.getDisplayText().lowercase().contains(queryLower)) {
            score += 2.0
        }
        
        // Boost score for exact matches
        if (job.title.equals(query, ignoreCase = true)) {
            score += 15.0
        }
        
        // Boost score for popular jobs
        score += job.viewCount * 0.1
        
        return score
    }
    
    /**
     * Record search query
     */
    private fun recordSearchQuery(
        userId: String,
        query: String,
        filters: JobFilters,
        resultCount: Int
    ) {
        val searchQuery = SearchQuery(
            query = query,
            filters = filters,
            resultCount = resultCount,
            searchedAt = System.currentTimeMillis()
        )
        
        val userHistory = searchHistory.getOrPut(userId) { mutableListOf() }
        userHistory.add(searchQuery)
        
        // Update popular searches
        popularSearches[query] = (popularSearches[query] ?: 0) + 1
        
        // Update suggestion popularity
        searchSuggestions.find { it.text.equals(query, ignoreCase = true) }?.let { suggestion ->
            suggestion.copy(popularity = suggestion.popularity + 1)
        }
    }
    
    /**
     * Get related searches
     */
    private fun getRelatedSearches(query: String): List<String> {
        val queryLower = query.lowercase()
        
        return searchSuggestions
            .filter { suggestion ->
                suggestion.text.lowercase().contains(queryLower) ||
                queryLower.contains(suggestion.text.lowercase())
            }
            .sortedByDescending { it.popularity }
            .map { it.text }
            .distinct()
            .take(5)
    }
    
    /**
     * Generate search insights
     */
    private fun generateSearchInsights(query: String, results: List<JobCardModel>): List<String> {
        val insights = mutableListOf<String>()
        
        if (results.isEmpty()) {
            insights.add("No jobs found matching your search. Try different keywords.")
            insights.add("Consider broadening your search terms.")
        } else {
            insights.add("Found ${results.size} jobs matching your search.")
            
            val topCompanies = results.take(5).map { it.employerName }.distinct()
            if (topCompanies.isNotEmpty()) {
                insights.add("Top companies: ${topCompanies.joinToString(", ")}")
            }
            
            val topLocations = results.take(5).map { it.location.getDisplayText() }.distinct()
            if (topLocations.isNotEmpty()) {
                insights.add("Popular locations: ${topLocations.joinToString(", ")}")
            }
            
            if (results.size > 10) {
                insights.add("Try adding more specific filters to narrow down results.")
            }
        }
        
        return insights
    }
    
    /**
     * Initialize search suggestions
     */
    private fun initializeSearchSuggestions() {
        val suggestions = listOf(
            // Job Titles
            SearchSuggestion("Software Engineer", SuggestionType.JOB_TITLE, 95, "Technology"),
            SearchSuggestion("Data Scientist", SuggestionType.JOB_TITLE, 88, "Technology"),
            SearchSuggestion("Product Manager", SuggestionType.JOB_TITLE, 82, "Business"),
            SearchSuggestion("UX Designer", SuggestionType.JOB_TITLE, 78, "Design"),
            SearchSuggestion("Marketing Manager", SuggestionType.JOB_TITLE, 75, "Marketing"),
            SearchSuggestion("Sales Representative", SuggestionType.JOB_TITLE, 72, "Sales"),
            SearchSuggestion("DevOps Engineer", SuggestionType.JOB_TITLE, 70, "Technology"),
            SearchSuggestion("Business Analyst", SuggestionType.JOB_TITLE, 68, "Business"),
            
            // Companies
            SearchSuggestion("Google", SuggestionType.COMPANY, 90, "Technology"),
            SearchSuggestion("Microsoft", SuggestionType.COMPANY, 85, "Technology"),
            SearchSuggestion("Amazon", SuggestionType.COMPANY, 80, "Technology"),
            SearchSuggestion("Apple", SuggestionType.COMPANY, 78, "Technology"),
            SearchSuggestion("Meta", SuggestionType.COMPANY, 75, "Technology"),
            SearchSuggestion("Netflix", SuggestionType.COMPANY, 70, "Entertainment"),
            
            // Skills
            SearchSuggestion("Python", SuggestionType.SKILL, 85, "Programming"),
            SearchSuggestion("JavaScript", SuggestionType.SKILL, 82, "Programming"),
            SearchSuggestion("React", SuggestionType.SKILL, 80, "Frontend"),
            SearchSuggestion("AWS", SuggestionType.SKILL, 78, "Cloud"),
            SearchSuggestion("Docker", SuggestionType.SKILL, 75, "DevOps"),
            SearchSuggestion("Machine Learning", SuggestionType.SKILL, 72, "AI"),
            
            // Locations
            SearchSuggestion("San Francisco", SuggestionType.LOCATION, 88, "California"),
            SearchSuggestion("New York", SuggestionType.LOCATION, 85, "New York"),
            SearchSuggestion("Seattle", SuggestionType.LOCATION, 80, "Washington"),
            SearchSuggestion("Austin", SuggestionType.LOCATION, 75, "Texas"),
            SearchSuggestion("Boston", SuggestionType.LOCATION, 70, "Massachusetts"),
            SearchSuggestion("Remote", SuggestionType.LOCATION, 90, "Work Type"),
            
            // Industries
            SearchSuggestion("Technology", SuggestionType.INDUSTRY, 90, "Industry"),
            SearchSuggestion("Healthcare", SuggestionType.INDUSTRY, 80, "Industry"),
            SearchSuggestion("Finance", SuggestionType.INDUSTRY, 75, "Industry"),
            SearchSuggestion("Education", SuggestionType.INDUSTRY, 70, "Industry"),
            SearchSuggestion("E-commerce", SuggestionType.INDUSTRY, 65, "Industry")
        )
        
        searchSuggestions.addAll(suggestions)
    }
}

/**
 * Smart search result
 */
data class SmartSearchResult(
    val query: String,
    val results: List<JobCardModel>,
    val totalCount: Int,
    val relatedSearches: List<String>,
    val insights: List<String>,
    val searchTime: Long,
    val suggestions: List<SearchSuggestion>
)

/**
 * Scored job for ranking
 */
data class ScoredJob(
    val job: JobCardModel,
    val score: Double
)

/**
 * Search analytics
 */
data class SearchAnalytics(
    val totalSearches: Int = 0,
    val uniqueQueries: Int = 0,
    val averageResults: Double = 0.0,
    val mostSearchedTerms: List<String> = emptyList(),
    val searchFrequency: Map<Long, Int> = emptyMap(),
    val lastSearchAt: Long? = null
)
