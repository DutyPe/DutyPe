package com.example.dutype.cache

import com.example.dutype.models.UserRole
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RoleCacheManager - Manages role-specific caches during role switching
 * 
 * Cache Strategy:
 * - Worker caches: Job listings, job categories, search queries, filters
 * - Employer caches: Posted jobs, applications, employer-specific data
 * - Shared caches: User profile, referral data, notifications (preserved)
 * 
 * Inspired by Airbnb's cache segregation strategy
 */
@Singleton
class RoleCacheManager @Inject constructor(
    private val jobCacheManager: JobCacheManager,
    private val employerProfileCache: EmployerProfileCache
) {
    
    /**
     * Clear all caches specific to a role
     */
    fun clearRoleSpecificCache(role: UserRole) {
        when (role) {
            UserRole.WORKER -> clearWorkerCache()
            UserRole.EMPLOYER -> clearEmployerCache()
            else -> Timber.w("⚠️ CACHE_MANAGER: Unknown role $role")
        }
    }
    
    /**
     * Clear Worker-specific caches
     * 
     * Worker caches:
     * - Job listings cache
     * - Job categories cache
     * - Search queries
     * - Filters and sort options
     * - Scroll positions
     * - Application drafts
     */
    private fun clearWorkerCache() {
        Timber.d("🧹 CACHE_MANAGER: Clearing Worker caches")
        
        try {
            // Clear job cache
            kotlinx.coroutines.runBlocking {
                jobCacheManager.clearAllCaches()
            }
            Timber.d("  ✓ Job listings cache cleared")
            
            // Additional worker-specific cache clearing can be added here
            // For example:
            // - searchQueryCache.clear()
            // - filterCache.clear()
            // - scrollPositionCache.clear()
            
        } catch (e: Exception) {
            Timber.e(e, "❌ CACHE_MANAGER: Error clearing worker cache")
        }
    }
    
    /**
     * Clear Employer-specific caches
     * 
     * Employer caches:
     * - Posted jobs cache
     * - Applications cache
     * - Employer profile cache
     * - Job posting drafts
     */
    private fun clearEmployerCache() {
        Timber.d("🧹 CACHE_MANAGER: Clearing Employer caches")
        
        try {
            // Clear employer profile cache
            kotlinx.coroutines.runBlocking {
                employerProfileCache.clearAll()
            }
            Timber.d("  ✓ Employer profile cache cleared")
            
            // Clear job cache (employer's posted jobs)
            kotlinx.coroutines.runBlocking {
                jobCacheManager.clearAllCaches()
            }
            Timber.d("  ✓ Posted jobs cache cleared")
            
            // Additional employer-specific cache clearing can be added here
            // For example:
            // - applicationCache.clear()
            // - jobDraftCache.clear()
            
        } catch (e: Exception) {
            Timber.e(e, "❌ CACHE_MANAGER: Error clearing employer cache")
        }
    }
    
    /**
     * Clear all caches (both worker and employer)
     * Use this for logout or complete reset
     */
    fun clearAllCaches() {
        Timber.d("🧹 CACHE_MANAGER: Clearing ALL caches")
        clearWorkerCache()
        clearEmployerCache()
    }
    
    /**
     * Shared caches that should NOT be cleared during role switch:
     * - User profile cache (shared across roles)
     * - Referral data cache (shared across roles)
     * - Notification preferences (shared across roles)
     * - Authentication tokens (shared across roles)
     * 
     * These caches are only cleared on logout.
     */
}
