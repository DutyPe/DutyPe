package com.example.dutype.core.cache

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Enterprise Multi-Level Cache Manager
 * 
 * 3-tier caching strategy:
 * - L1: Memory cache (fastest, volatile)
 * - L2: Disk cache (persistent, slower)
 * - L3: Network (slowest, always fresh)
 * 
 * Features:
 * - TTL (Time To Live) per entry
 * - LRU eviction for memory cache
 * - Stale-while-revalidate pattern
 * - Cache warming
 * - Automatic invalidation
 * 
 * @author DutyPe Engineering Team
 * @since 3.0.0
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cache_store")
    
    // L1: Memory cache (fast, volatile)
    private val memoryCache = ConcurrentHashMap<String, CacheEntry<*>>()
    private val memoryCacheMutex = Mutex()
    
    // Memory cache size limit (entries)
    private val maxMemoryCacheSize = 100
    
    /**
     * Get value from cache with fallback to fetcher
     * Implements stale-while-revalidate pattern
     */
    suspend fun <T> getOrFetch(
        key: String,
        ttl: Duration = 5.minutes,
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
        fetcher: suspend () -> T
    ): T {
        return when (strategy) {
            CacheStrategy.CACHE_FIRST -> getCacheFirst(key, ttl, fetcher)
            CacheStrategy.NETWORK_FIRST -> getNetworkFirst(key, ttl, fetcher)
            CacheStrategy.CACHE_ONLY -> getCacheOnly(key)
            CacheStrategy.NETWORK_ONLY -> getNetworkOnly(key, ttl, fetcher)
            CacheStrategy.STALE_WHILE_REVALIDATE -> getStaleWhileRevalidate(key, ttl, fetcher)
        }
    }
    
    /**
     * Cache-first strategy: Return cached if valid, else fetch
     */
    private suspend fun <T> getCacheFirst(
        key: String,
        ttl: Duration,
        fetcher: suspend () -> T
    ): T {
        // Try L1 (memory)
        val memoryValue = getFromMemory<T>(key)
        if (memoryValue != null && !isExpired(memoryValue, ttl)) {
            Timber.d("💾 Cache HIT (L1 Memory): $key")
            return memoryValue.data
        }
        
        // Try L2 (disk)
        val diskValue = getFromDisk<T>(key)
        if (diskValue != null && !isExpired(diskValue, ttl)) {
            Timber.d("💾 Cache HIT (L2 Disk): $key")
            // Promote to L1
            putToMemory(key, diskValue)
            return diskValue.data
        }
        
        // L3: Fetch from network
        Timber.d("🌐 Cache MISS: Fetching from network: $key")
        val freshData = fetcher()
        
        // Store in both caches
        val entry = CacheEntry(freshData, System.currentTimeMillis())
        putToMemory(key, entry)
        putToDisk(key, entry)
        
        return freshData
    }
    
    /**
     * Network-first strategy: Always fetch, use cache as fallback
     */
    private suspend fun <T> getNetworkFirst(
        key: String,
        ttl: Duration,
        fetcher: suspend () -> T
    ): T {
        return try {
            val freshData = fetcher()
            val entry = CacheEntry(freshData, System.currentTimeMillis())
            putToMemory(key, entry)
            putToDisk(key, entry)
            freshData
        } catch (e: Exception) {
            Timber.w("Network fetch failed, falling back to cache: $key")
            // Fallback to cache (even if stale)
            getFromMemory<T>(key)?.data
                ?: getFromDisk<T>(key)?.data
                ?: throw e
        }
    }
    
    /**
     * Cache-only strategy: Return cached or throw
     */
    private suspend fun <T> getCacheOnly(key: String): T {
        return getFromMemory<T>(key)?.data
            ?: getFromDisk<T>(key)?.data
            ?: throw CacheException("No cached value for key: $key")
    }
    
    /**
     * Network-only strategy: Always fetch, update cache
     */
    private suspend fun <T> getNetworkOnly(
        key: String,
        ttl: Duration,
        fetcher: suspend () -> T
    ): T {
        val freshData = fetcher()
        val entry = CacheEntry(freshData, System.currentTimeMillis())
        putToMemory(key, entry)
        putToDisk(key, entry)
        return freshData
    }
    
    /**
     * Stale-while-revalidate: Return stale immediately, fetch in background
     */
    private suspend fun <T> getStaleWhileRevalidate(
        key: String,
        ttl: Duration,
        fetcher: suspend () -> T
    ): T {
        // Return stale cache immediately
        val cachedValue = getFromMemory<T>(key)?.data
            ?: getFromDisk<T>(key)?.data
        
        if (cachedValue != null) {
            // Revalidate in background (fire and forget)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val freshData = fetcher()
                    val entry = CacheEntry(freshData, System.currentTimeMillis())
                    putToMemory(key, entry)
                    putToDisk(key, entry)
                } catch (e: Exception) {
                    Timber.w("Background revalidation failed: $key")
                }
            }
            return cachedValue
        }
        
        // No cache, fetch synchronously
        return getCacheFirst(key, ttl, fetcher)
    }
    
    /**
     * Put value in cache
     */
    suspend fun <T> put(key: String, value: T) {
        val entry = CacheEntry(value, System.currentTimeMillis())
        putToMemory(key, entry)
        putToDisk(key, entry)
    }
    
    /**
     * Invalidate cache entry
     */
    suspend fun invalidate(key: String) {
        memoryCache.remove(key)
        context.dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey(key))
        }
        Timber.d("🗑️ Cache invalidated: $key")
    }
    
    /**
     * Invalidate all entries matching pattern
     */
    suspend fun invalidatePattern(pattern: String) {
        val regex = pattern.toRegex()
        
        // Invalidate memory cache
        memoryCache.keys.filter { it.matches(regex) }.forEach { key ->
            memoryCache.remove(key)
        }
        
        // Invalidate disk cache
        context.dataStore.edit { prefs ->
            prefs.asMap().keys
                .filter { it.name.matches(regex) }
                .forEach { key -> prefs.remove(key) }
        }
        
        Timber.d("🗑️ Cache invalidated (pattern): $pattern")
    }
    
    /**
     * Clear all caches
     */
    suspend fun clearAll() {
        memoryCache.clear()
        context.dataStore.edit { it.clear() }
        Timber.d("🗑️ All caches cleared")
    }
    
    /**
     * Warm cache with data
     */
    suspend fun <T> warm(key: String, value: T) {
        put(key, value)
        Timber.d("🔥 Cache warmed: $key")
    }
    
    // ==================== PRIVATE METHODS ====================
    
    private fun <T> getFromMemory(key: String): CacheEntry<T>? {
        @Suppress("UNCHECKED_CAST")
        return memoryCache[key] as? CacheEntry<T>
    }
    
    private suspend fun <T> putToMemory(key: String, entry: CacheEntry<T>) {
        memoryCacheMutex.withLock {
            // LRU eviction if cache is full
            if (memoryCache.size >= maxMemoryCacheSize) {
                val oldestKey = memoryCache.entries
                    .minByOrNull { (it.value as CacheEntry<*>).timestamp }
                    ?.key
                
                oldestKey?.let { memoryCache.remove(it) }
            }
            
            memoryCache[key] = entry
        }
    }
    
    private suspend fun <T> getFromDisk(key: String): CacheEntry<T>? {
        return try {
            val json = context.dataStore.data.map { prefs ->
                prefs[stringPreferencesKey(key)]
            }.first()
            
            if (json != null) {
                @Suppress("UNCHECKED_CAST")
                gson.fromJson(json, CacheEntry::class.java) as? CacheEntry<T>
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.w("Failed to read from disk cache: $key")
            null
        }
    }
    
    private suspend fun <T> putToDisk(key: String, entry: CacheEntry<T>) {
        try {
            val json = gson.toJson(entry)
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(key)] = json
            }
        } catch (e: Exception) {
            Timber.w("Failed to write to disk cache: $key")
        }
    }
    
    private fun <T> isExpired(entry: CacheEntry<T>, ttl: Duration): Boolean {
        val age = System.currentTimeMillis() - entry.timestamp
        return age > ttl.inWholeMilliseconds
    }
}

/**
 * Cache entry with timestamp
 */
data class CacheEntry<T>(
    val data: T,
    val timestamp: Long
)

/**
 * Cache strategies
 */
enum class CacheStrategy {
    CACHE_FIRST,              // Return cache if valid, else fetch
    NETWORK_FIRST,            // Always fetch, use cache as fallback
    CACHE_ONLY,               // Return cache or throw
    NETWORK_ONLY,             // Always fetch, update cache
    STALE_WHILE_REVALIDATE    // Return stale, fetch in background
}

/**
 * Cache exception
 */
class CacheException(message: String) : Exception(message)
