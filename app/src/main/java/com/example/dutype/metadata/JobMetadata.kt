package com.example.dutype.metadata

import com.example.dutype.models.JobListing
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JobMetadata - Metadata and statistics for jobs
 * 
 * This provides:
 * 1. Job category statistics
 * 2. Trending jobs and categories
 * 3. Location-based job counts
 * 4. Pay range statistics
 * 5. Application statistics
 * 
 * REFACTORED: Now receives FirebaseFirestore via constructor injection
 * 
 * Usage:
 * - Use for showing "X jobs available" badges
 * - Use for trending/popular sections
 * - Use for analytics and insights
 * 
 * @author DutyPe Engineering Team
 * @since 2.0.0
 */
@Singleton
class JobMetadata @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    // ==========================================
    // CATEGORY STATS
    // ==========================================
    
    private val _categoryStats = MutableStateFlow<Map<String, CategoryStats>>(emptyMap())
    val categoryStats: StateFlow<Map<String, CategoryStats>> = _categoryStats.asStateFlow()
    
    // ==========================================
    // LOCATION STATS
    // ==========================================
    
    private val _locationStats = MutableStateFlow<Map<String, LocationStats>>(emptyMap())
    val locationStats: StateFlow<Map<String, LocationStats>> = _locationStats.asStateFlow()
    
    // ==========================================
    // TRENDING DATA
    // ==========================================
    
    private val _trendingCategories = MutableStateFlow<List<TrendingCategory>>(emptyList())
    val trendingCategories: StateFlow<List<TrendingCategory>> = _trendingCategories.asStateFlow()
    
    private val _trendingLocations = MutableStateFlow<List<String>>(emptyList())
    val trendingLocations: StateFlow<List<String>> = _trendingLocations.asStateFlow()
    
    // ==========================================
    // PAY RANGE STATS
    // ==========================================
    
    private val _payRangeStats = MutableStateFlow(PayRangeStats())
    val payRangeStats: StateFlow<PayRangeStats> = _payRangeStats.asStateFlow()
    
    // ==========================================
    // LOADING STATE
    // ==========================================
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _lastUpdated = MutableStateFlow(0L)
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()
    
    /**
     * Initialize job metadata - call on app startup
     * Note: Firestore metadata requires authentication, so we skip Firestore calls here
     * and defer them until user is authenticated
     */
    suspend fun initialize() {
        Timber.d("📊 Initializing JobMetadata...")
        _isLoading.value = true
        
        try {
            // Note: All Firestore calls require authentication
            // They will be loaded when initializeWithAuth() is called after login
            
            _lastUpdated.value = System.currentTimeMillis()
            Timber.d("📊 JobMetadata initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "📊 Failed to initialize JobMetadata")
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Initialize Firestore-dependent metadata - call after user authentication
     */
    suspend fun initializeWithAuth() {
        Timber.d("📊 Loading authenticated JobMetadata...")
        _isLoading.value = true
        
        try {
            loadStatsFromJobsCollection()
            
            _lastUpdated.value = System.currentTimeMillis()
            Timber.d("📊 JobMetadata loaded from jobs collection")
        } catch (e: Exception) {
            Timber.w(e, "📊 Failed to load JobMetadata from jobs collection (using defaults)")
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Refresh job metadata
     */
    suspend fun refresh() {
        initializeWithAuth()
    }
    
    /**
     * Calculate metadata from a list of jobs (local calculation)
     * Use this when you have jobs loaded and want quick stats
     */
    fun calculateFromJobs(jobs: List<JobListing>) {
        Timber.d("📊 Calculating metadata from ${jobs.size} jobs...")
        
        // Category stats
        val categoryMap = mutableMapOf<String, CategoryStats>()
        jobs.groupBy { it.getCategory() }.forEach { (category, categoryJobs) ->
            if (category.isNotEmpty()) {
                val avgPay = categoryJobs.mapNotNull {
                    com.example.dutype.utils.SalaryFormatter.lowerBound(it.salary).takeIf { v -> v > 0 }
                }.average().takeIf { !it.isNaN() } ?: 0.0
                
                categoryMap[category] = CategoryStats(
                    category = category,
                    totalJobs = categoryJobs.size,
                    activeJobs = categoryJobs.count { it.status == "open" && !it.isExpired() },
                    averagePay = avgPay,
                    payTypes = categoryJobs.map { it.salaryType }.distinct()
                )
            }
        }
        _categoryStats.value = categoryMap
        
        // Location stats
        val locationMap = mutableMapOf<String, LocationStats>()
        jobs.groupBy { it.addressText.ifBlank { it.location }.ifEmpty { "Unknown" } }.forEach { (location, locationJobs) ->
            if (location.isNotEmpty() && location != "Unknown") {
                locationMap[location] = LocationStats(
                    location = location,
                    totalJobs = locationJobs.size,
                    activeJobs = locationJobs.count { it.status == "open" && !it.isExpired() },
                    topCategories = locationJobs.groupBy { it.getCategory() }
                        .entries.sortedByDescending { it.value.size }
                        .take(5)
                        .map { it.key }
                )
            }
        }
        _locationStats.value = locationMap
        
        // Trending categories (by job count)
        _trendingCategories.value = categoryMap.entries
            .sortedByDescending { it.value.activeJobs }
            .take(10)
            .map { TrendingCategory(it.key, it.value.activeJobs, getTrendDirection(it.key)) }
        
        // Trending locations
        _trendingLocations.value = locationMap.entries
            .sortedByDescending { it.value.activeJobs }
            .take(10)
            .map { it.key }
        
        // Pay range stats
        val allPays = jobs.mapNotNull {
            com.example.dutype.utils.SalaryFormatter.lowerBound(it.salary).takeIf { v -> v > 0 }
        }
        if (allPays.isNotEmpty()) {
            _payRangeStats.value = PayRangeStats(
                minPay = allPays.minOrNull() ?: 0.0,
                maxPay = allPays.maxOrNull() ?: 0.0,
                averagePay = allPays.average(),
                medianPay = allPays.sorted().let { sorted ->
                    if (sorted.size % 2 == 0) {
                        (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
                    } else {
                        sorted[sorted.size / 2]
                    }
                }
            )
        }
        
        _lastUpdated.value = System.currentTimeMillis()
        Timber.d("📊 Metadata calculated: ${categoryMap.size} categories, ${locationMap.size} locations")
    }
    
    /**
     * Get most recent job IDs directly from jobs collection.
     */
    suspend fun getRecentJobIdsFromMetadata(): List<String> {
        return try {
            val snapshot = firestore.collection(com.example.dutype.firestore.FirestoreCollections.JOBS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()
            val jobIds = snapshot.documents.map { it.id }
            Timber.d("📊 Loaded ${jobIds.size} recent job IDs from jobs collection")
            jobIds
        } catch (e: Exception) {
            Timber.e(e, "📊 Failed to load recent job IDs from jobs collection")
            emptyList()
        }
    }
    
    /**
     * Get job count for a specific category
     */
    fun getJobCountForCategory(category: String): Int {
        return _categoryStats.value[category]?.activeJobs ?: 0
    }
    
    /**
     * Get job count for a specific location
     */
    fun getJobCountForLocation(location: String): Int {
        return _locationStats.value[location]?.activeJobs ?: 0
    }
    
    /**
     * Get average pay for a category
     */
    fun getAveragePayForCategory(category: String): Double {
        return _categoryStats.value[category]?.averagePay ?: 0.0
    }
    
    /**
     * Check if a category is trending
     */
    fun isCategoryTrending(category: String): Boolean {
        return _trendingCategories.value.any { it.category == category }
    }
    
    /**
     * Get category badge text (e.g., "25 jobs", "Trending")
     */
    fun getCategoryBadgeText(category: String): String? {
        val stats = _categoryStats.value[category] ?: return null
        return when {
            stats.activeJobs == 0 -> null
            isCategoryTrending(category) -> "🔥 ${stats.activeJobs} jobs"
            stats.activeJobs > 50 -> "${stats.activeJobs}+ jobs"
            else -> "${stats.activeJobs} jobs"
        }
    }
    
    // ==========================================
    // PRIVATE METHODS
    // ==========================================

    private suspend fun loadStatsFromJobsCollection() {
        val snapshot = firestore.collection(com.example.dutype.firestore.FirestoreCollections.JOBS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(400)
            .get()
            .await()

        if (snapshot.isEmpty) {
            _categoryStats.value = emptyMap()
            _locationStats.value = emptyMap()
            _trendingCategories.value = emptyList()
            _trendingLocations.value = emptyList()
            _payRangeStats.value = PayRangeStats()
            return
        }

        val categoryMap = mutableMapOf<String, CategoryStats>()
        val locationMap = mutableMapOf<String, LocationStats>()
        val allPays = mutableListOf<Double>()

        val categoryBuckets = mutableMapOf<String, MutableList<Map<String, Any>>>()
        val locationBuckets = mutableMapOf<String, MutableList<Map<String, Any>>>()

        snapshot.documents.forEach { doc ->
            val data = doc.data ?: return@forEach
            val status = (data["status"] as? String)?.lowercase()
            val active = status == "open"

            val category = (data["jobType"] as? String ?: "OTHER").ifBlank { "OTHER" }
            categoryBuckets.getOrPut(category) { mutableListOf() }.add(data)

            val locationLabel = when (val rawLocation = data["location"]) {
                is String -> rawLocation
                else -> {
                    val geohash = data["geohash"] as? String
                    if (geohash.isNullOrBlank()) "Unknown" else "Geo-${geohash.take(5)}"
                }
            }.ifBlank { "Unknown" }
            locationBuckets.getOrPut(locationLabel) { mutableListOf() }.add(data)

            val pay = when (val rawPay = data["salary"]) {
                is Number -> rawPay.toDouble()
                is String -> rawPay.replace(",", "").toDoubleOrNull()
                else -> null
            }
            if (pay != null) allPays.add(pay)

            if (!active) return@forEach
        }

        categoryBuckets.forEach { (category, jobs) ->
            val activeJobs = jobs.count {
                val status = (it["status"] as? String)?.lowercase()
                status == "open"
            }
            val pays = jobs.mapNotNull {
                when (val rawPay = it["salary"]) {
                    is Number -> rawPay.toDouble()
                    is String -> rawPay.replace(",", "").toDoubleOrNull()
                    else -> null
                }
            }
            categoryMap[category] = CategoryStats(
                category = category,
                totalJobs = jobs.size,
                activeJobs = activeJobs,
                averagePay = pays.average().takeIf { !it.isNaN() } ?: 0.0,
                payTypes = jobs.mapNotNull { it["salaryType"] as? String }.distinct()
            )
        }

        locationBuckets.forEach { (location, jobs) ->
            val groupedCategories = jobs.groupBy {
                ((it["jobType"] as? String) ?: "OTHER").ifBlank { "OTHER" }
            }
            locationMap[location] = LocationStats(
                location = location,
                totalJobs = jobs.size,
                activeJobs = jobs.count {
                    val status = (it["status"] as? String)?.lowercase()
                    status == "open"
                },
                topCategories = groupedCategories.entries
                    .sortedByDescending { it.value.size }
                    .take(5)
                    .map { it.key }
            )
        }

        _categoryStats.value = categoryMap
        _locationStats.value = locationMap
        _trendingCategories.value = categoryMap.entries
            .sortedByDescending { it.value.activeJobs }
            .take(10)
            .map { TrendingCategory(it.key, it.value.activeJobs, TrendDirection.STABLE) }
        _trendingLocations.value = locationMap.entries
            .sortedByDescending { it.value.activeJobs }
            .take(10)
            .map { it.key }

        _payRangeStats.value = if (allPays.isNotEmpty()) {
            val sorted = allPays.sorted()
            PayRangeStats(
                minPay = sorted.first(),
                maxPay = sorted.last(),
                averagePay = sorted.average(),
                medianPay = if (sorted.size % 2 == 0) {
                    (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
                } else {
                    sorted[sorted.size / 2]
                }
            )
        } else {
            PayRangeStats()
        }
    }
    
    private fun getTrendDirection(category: String): TrendDirection {
        // Simple logic - can be enhanced with historical data
        return TrendDirection.STABLE
    }
}

/**
 * Statistics for a job category
 */
data class CategoryStats(
    val category: String = "",
    val totalJobs: Int = 0,
    val activeJobs: Int = 0,
    val averagePay: Double = 0.0,
    val payTypes: List<String> = emptyList()
)

/**
 * Statistics for a location
 */
data class LocationStats(
    val location: String = "",
    val totalJobs: Int = 0,
    val activeJobs: Int = 0,
    val topCategories: List<String> = emptyList()
)

/**
 * Trending category with trend direction
 */
data class TrendingCategory(
    val category: String,
    val jobCount: Int,
    val trend: TrendDirection = TrendDirection.STABLE
)

/**
 * Trend direction for categories
 */
enum class TrendDirection {
    UP,      // More jobs than before
    DOWN,    // Fewer jobs than before
    STABLE   // About the same
}

/**
 * Pay range statistics
 */
data class PayRangeStats(
    val minPay: Double = 0.0,
    val maxPay: Double = 0.0,
    val averagePay: Double = 0.0,
    val medianPay: Double = 0.0
)
