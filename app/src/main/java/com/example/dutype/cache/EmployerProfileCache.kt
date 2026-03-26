package com.example.dutype.cache

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EmployerProfileCache - In-memory cache for employer profile data
 * 
 * P1 FIX: Prevents fetching employer profile on every job post.
 * Profile data is cached with 5-minute TTL and refreshed on demand.
 * 
 * Features:
 * - Cache-first approach for profile data
 * - 5-minute TTL for freshness
 * - Thread-safe operations with Mutex
 * - Automatic refresh on cache miss
 * 
 * @author DutyPe Engineering Team
 * @since 2.3.0
 */
@Singleton
class EmployerProfileCache @Inject constructor() {
    
    companion object {
        // Cache TTL: 5 minutes (profile data doesn't change frequently)
        private const val CACHE_TTL_MS = 5 * 60 * 1000L
    }
    
    private val mutex = Mutex()
    private val firestore = FirebaseFirestore.getInstance()
    
    // Cached profile data per employer
    private val profileCache = mutableMapOf<String, CachedProfile>()
    
    /**
     * Cached employer profile data
     */
    data class CachedProfile(
        val employerId: String,
        val companyName: String,
        val employerName: String,
        val contactPhone: String,
        val trustTier: String,
        val profileImageUrl: String,
        val timestamp: Long
    ) {
        fun isValid(): Boolean = System.currentTimeMillis() - timestamp < CACHE_TTL_MS
    }
    
    /**
     * Get employer profile with cache-first approach
     * 
     * @param employerId Employer's user ID
     * @param forceRefresh Force fetch from Firestore even if cached
     * @return CachedProfile or null if not found
     */
    suspend fun getProfile(
        employerId: String,
        forceRefresh: Boolean = false
    ): CachedProfile? = mutex.withLock {
        // Check cache first
        val cached = profileCache[employerId]
        if (!forceRefresh && cached != null && cached.isValid()) {
            Timber.d("📦 EMPLOYER_CACHE: HIT for $employerId (age: ${System.currentTimeMillis() - cached.timestamp}ms)")
            return@withLock cached
        }
        
        Timber.d("📦 EMPLOYER_CACHE: MISS for $employerId, fetching from Firestore...")
        
        // Fetch from Firestore
        return@withLock try {
            withContext(Dispatchers.IO) {
                val userDoc = firestore.collection(com.example.dutype.firestore.FirestoreCollections.USERS)
                    .document(employerId)
                    .get()
                    .await()
                val employerProfileDoc = firestore.collection(com.example.dutype.firestore.FirestoreCollections.EMPLOYER_PROFILES)
                    .document(employerId)
                    .get()
                    .await()
                
                if (userDoc.exists() || employerProfileDoc.exists()) {
                    val profile = CachedProfile(
                        employerId = employerId,
                        companyName = employerProfileDoc.getString("companyName") ?: "",
                        employerName = userDoc.getString("fullName") ?: "",
                        contactPhone = userDoc.getString("phone") ?: "",
                        trustTier = "VERIFIED",
                        profileImageUrl = userDoc.getString("profileImageUrl") ?: "",
                        timestamp = System.currentTimeMillis()
                    )
                    
                    // Store in cache
                    profileCache[employerId] = profile
                    Timber.d("📦 EMPLOYER_CACHE: Cached profile for $employerId")
                    Timber.d("📦   - Company: ${profile.companyName}")
                    Timber.d("📦   - Name: ${profile.employerName}")
                    Timber.d("📦   - Trust Tier: ${profile.trustTier}")
                    
                    profile
                } else {
                    Timber.w("📦 EMPLOYER_CACHE: User document not found for $employerId")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "📦 EMPLOYER_CACHE: Failed to fetch profile for $employerId")
            // Return stale cache if available
            cached?.also {
                Timber.d("📦 EMPLOYER_CACHE: Returning stale cache due to error")
            }
        }
    }
    
    /**
     * Get company name with cache-first approach
     */
    suspend fun getCompanyName(employerId: String): String {
        return getProfile(employerId)?.companyName ?: ""
    }
    
    /**
     * Get employer name with cache-first approach
     */
    suspend fun getEmployerName(employerId: String): String {
        return getProfile(employerId)?.employerName ?: ""
    }
    
    /**
     * Get contact phone with cache-first approach
     */
    suspend fun getContactPhone(employerId: String): String {
        return getProfile(employerId)?.contactPhone ?: ""
    }
    
    /**
     * Get trust tier with cache-first approach
     */
    suspend fun getTrustTier(employerId: String): String {
        return getProfile(employerId)?.trustTier ?: "VERIFIED"
    }
    
    /**
     * Update cache after profile edit
     */
    suspend fun updateCache(
        employerId: String,
        companyName: String? = null,
        employerName: String? = null,
        contactPhone: String? = null,
        trustTier: String? = null
    ) = mutex.withLock {
        val existing = profileCache[employerId]
        if (existing != null) {
            profileCache[employerId] = existing.copy(
                companyName = companyName ?: existing.companyName,
                employerName = employerName ?: existing.employerName,
                contactPhone = contactPhone ?: existing.contactPhone,
                trustTier = trustTier ?: existing.trustTier,
                timestamp = System.currentTimeMillis()
            )
            Timber.d("📦 EMPLOYER_CACHE: Updated cache for $employerId")
        }
    }
    
    /**
     * Invalidate cache for an employer
     */
    suspend fun invalidate(employerId: String) = mutex.withLock {
        profileCache.remove(employerId)
        Timber.d("📦 EMPLOYER_CACHE: Invalidated cache for $employerId")
    }
    
    /**
     * Clear all cached profiles
     */
    suspend fun clearAll() = mutex.withLock {
        profileCache.clear()
        Timber.d("📦 EMPLOYER_CACHE: Cleared all cached profiles")
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            totalCached = profileCache.size,
            validCount = profileCache.values.count { it.isValid() },
            staleCount = profileCache.values.count { !it.isValid() }
        )
    }
    
    data class CacheStats(
        val totalCached: Int,
        val validCount: Int,
        val staleCount: Int
    )
}
