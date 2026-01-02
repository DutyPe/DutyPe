package com.example.dutype.metadata

import android.content.Context
import com.example.dutype.cache.JobCacheManager
import com.example.dutype.models.JobListing
import com.example.dutype.models.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MetadataManager - Central coordinator for all metadata in the app
 * 
 * This is the single entry point for:
 * 1. AppMetadata - App version, feature flags, platform stats
 * 2. JobMetadata - Job statistics, trending data
 * 3. UserMetadata - User stats, limits, achievements
 * 4. CacheMetadata - Cache statistics
 * 
 * Usage:
 * - Initialize on app startup
 * - Refresh periodically or on user action
 * - Access individual metadata via properties
 * 
 * Example:
 * ```kotlin
 * @Inject lateinit var metadataManager: MetadataManager
 * 
 * // Initialize on app start
 * metadataManager.initialize(context)
 * 
 * // After user login
 * metadataManager.initializeUserMetadata(UserRole.WORKER)
 * 
 * // Access metadata
 * val jobCount = metadataManager.jobMetadata.getJobCountForCategory("COOK")
 * val canApply = metadataManager.userMetadata.canApplyForJob()
 * ```
 */
@Singleton
class MetadataManager @Inject constructor(
    val appMetadata: AppMetadata,
    val jobMetadata: JobMetadata,
    val userMetadata: UserMetadata,
    private val cacheManager: JobCacheManager
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // ==========================================
    // INITIALIZATION STATE
    // ==========================================
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _lastRefreshed = MutableStateFlow(0L)
    val lastRefreshed: StateFlow<Long> = _lastRefreshed.asStateFlow()
    
    // ==========================================
    // CACHE STATS
    // ==========================================
    
    private val _cacheStats = MutableStateFlow(CacheStats())
    val cacheStats: StateFlow<CacheStats> = _cacheStats.asStateFlow()
    
    /**
     * Initialize all metadata - call on app startup
     */
    suspend fun initialize(context: Context) {
        if (_isInitialized.value) {
            Timber.d("📊 MetadataManager already initialized")
            return
        }
        
        Timber.d("📊 Initializing MetadataManager...")
        _isLoading.value = true
        
        try {
            // Initialize app metadata (version, feature flags, platform stats)
            appMetadata.initialize(context)
            
            // Initialize job metadata (category stats, trending)
            jobMetadata.initialize()
            
            _isInitialized.value = true
            _lastRefreshed.value = System.currentTimeMillis()
            
            Timber.d("📊 MetadataManager initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "📊 Failed to initialize MetadataManager")
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Initialize user-specific metadata - call after user login
     */
    suspend fun initializeUserMetadata(userRole: UserRole) {
        Timber.d("📊 Initializing user metadata for role: $userRole")
        userMetadata.initialize(userRole)
    }
    
    /**
     * Clear user metadata - call on logout
     */
    fun clearUserMetadata() {
        userMetadata.clear()
        Timber.d("📊 User metadata cleared")
    }
    
    /**
     * Refresh all metadata
     */
    suspend fun refreshAll(context: Context, userRole: UserRole? = null) {
        Timber.d("📊 Refreshing all metadata...")
        _isLoading.value = true
        
        try {
            appMetadata.refresh()
            jobMetadata.refresh()
            userRole?.let { userMetadata.refresh(it) }
            updateCacheStats()
            
            _lastRefreshed.value = System.currentTimeMillis()
            Timber.d("📊 All metadata refreshed")
        } catch (e: Exception) {
            Timber.e(e, "📊 Failed to refresh metadata")
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Update job metadata from loaded jobs
     * Call this after jobs are loaded to calculate local stats
     */
    fun updateJobMetadataFromJobs(jobs: List<JobListing>) {
        scope.launch {
            jobMetadata.calculateFromJobs(jobs)
            updateCacheStats()
        }
    }
    
    /**
     * Update cache statistics
     */
    fun updateCacheStats() {
        val stats = cacheManager.getCacheStats()
        _cacheStats.value = CacheStats(
            allJobsCount = stats.allJobsCount,
            allJobsCacheAgeMs = stats.allJobsCacheAge,
            categoryCount = stats.categoryCount,
            locationCount = stats.locationCount,
            searchCount = stats.searchCount,
            singleJobCount = stats.singleJobCount,
            savedJobIdsCount = stats.savedJobIdsCount,
            isCacheValid = stats.allJobsCacheAge in 0..300000 // Valid if less than 5 minutes
        )
    }
    
    /**
     * Get summary of all metadata for debugging
     */
    fun getMetadataSummary(): MetadataSummary {
        return MetadataSummary(
            appVersion = appMetadata.appVersion,
            isDebug = appMetadata.isDebug,
            platformStats = appMetadata.platformStats.value,
            featureFlags = appMetadata.featureFlags.value,
            categoryCount = jobMetadata.categoryStats.value.size,
            trendingCategoriesCount = jobMetadata.trendingCategories.value.size,
            userStatsLoaded = userMetadata.userStats.value.userId.isNotEmpty(),
            cacheStats = _cacheStats.value,
            lastRefreshed = _lastRefreshed.value
        )
    }
    
    // ==========================================
    // CONVENIENCE METHODS
    // ==========================================
    
    /**
     * Check if app is in maintenance mode
     */
    fun isMaintenanceMode(): Boolean = appMetadata.isInMaintenanceMode()
    
    /**
     * Check if force update is required
     */
    fun needsForceUpdate(): Boolean = appMetadata.needsForceUpdate()
    
    /**
     * Get job count badge for a category
     */
    fun getCategoryBadge(category: String): String? = jobMetadata.getCategoryBadgeText(category)
    
    /**
     * Check if user can apply for jobs
     */
    fun canUserApply(): Boolean = userMetadata.canApplyForJob()
    
    /**
     * Check if employer can post jobs
     */
    fun canEmployerPost(): Boolean = userMetadata.canPostJob()
    
    /**
     * Check if a feature is enabled
     */
    fun isFeatureEnabled(feature: Feature): Boolean {
        val flags = appMetadata.featureFlags.value
        return when (feature) {
            Feature.CHAT -> flags.isChatEnabled
            Feature.MAP_VIEW -> flags.isMapViewEnabled
            Feature.SUBSCRIPTION -> flags.isSubscriptionEnabled
            Feature.REFERRAL -> flags.isReferralEnabled
            Feature.WORK_VERIFICATION -> flags.isWorkVerificationEnabled
            Feature.RATING -> flags.isRatingEnabled
            Feature.WHATSAPP_APPLY -> flags.isWhatsAppApplyEnabled
            Feature.DIGITAL_CARD -> flags.isDigitalCardEnabled
        }
    }
}

/**
 * Cache statistics
 */
data class CacheStats(
    val allJobsCount: Int = 0,
    val allJobsCacheAgeMs: Long = -1,
    val categoryCount: Int = 0,
    val locationCount: Int = 0,
    val searchCount: Int = 0,
    val singleJobCount: Int = 0,
    val savedJobIdsCount: Int = 0,
    val isCacheValid: Boolean = false
) {
    val allJobsCacheAgeSeconds: Long
        get() = if (allJobsCacheAgeMs > 0) allJobsCacheAgeMs / 1000 else -1
    
    val cacheStatusText: String
        get() = when {
            allJobsCacheAgeMs < 0 -> "No cache"
            isCacheValid -> "Fresh (${allJobsCacheAgeSeconds}s ago)"
            else -> "Stale (${allJobsCacheAgeSeconds}s ago)"
        }
}

/**
 * Summary of all metadata for debugging
 */
data class MetadataSummary(
    val appVersion: String,
    val isDebug: Boolean,
    val platformStats: PlatformStats,
    val featureFlags: FeatureFlags,
    val categoryCount: Int,
    val trendingCategoriesCount: Int,
    val userStatsLoaded: Boolean,
    val cacheStats: CacheStats,
    val lastRefreshed: Long
)

/**
 * Feature flags enum for type-safe feature checking
 */
enum class Feature {
    CHAT,
    MAP_VIEW,
    SUBSCRIPTION,
    REFERRAL,
    WORK_VERIFICATION,
    RATING,
    WHATSAPP_APPLY,
    DIGITAL_CARD
}
