package com.example.dutype.utils

import android.content.ComponentCallbacks2
import coil.ImageLoader
import com.example.dutype.cache.EmployerProfileCache
import com.example.dutype.jobs.JobRepository
import com.example.dutype.worker.screens.map.MapMarkerIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enterprise Memory Trim & Lifecycle Manager for Android 17 / Play Store Compliance.
 *
 * Implements Google's Android 17 memory efficiency mandates:
 * - Sheds dirty memory pages on [ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN] to survive in the background.
 * - Evicts image caches, bitmap pools, and repository caches under system memory pressure.
 * - Prevents LMKD (Low Memory Killer Daemon) termination without stack traces.
 */
@Singleton
class MemoryTrimManager @Inject constructor(
    private val imageLoader: dagger.Lazy<ImageLoader>,
    private val jobRepository: dagger.Lazy<JobRepository>,
    private val employerProfileCache: dagger.Lazy<EmployerProfileCache>
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun onTrimMemory(level: Int) {
        Timber.d("🧹 MemoryTrimManager: onTrimMemory level = $level")
        try {
            when (level) {
                ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                    // App moved to background: clear UI bitmap caches immediately
                    imageLoader.get().memoryCache?.clear()
                    MapMarkerIcons.clearCache()
                    Timber.d("🧹 MemoryTrimManager: TRIM_MEMORY_UI_HIDDEN - Cleared Coil & Map marker bitmap caches")
                }

                ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                    // App is running but system is under severe memory pressure
                    imageLoader.get().memoryCache?.trimMemory(level)
                    Timber.d("🧹 MemoryTrimManager: Trimmed image memory cache (level=$level)")
                }

                ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
                ComponentCallbacks2.TRIM_MEMORY_MODERATE,
                ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                    // App is deep in LRU list or memory is critical: aggressively drop all volatile caches
                    imageLoader.get().memoryCache?.clear()
                    MapMarkerIcons.clearCache()
                    jobRepository.get().clearCaches()
                    scope.launch {
                        runCatching { employerProfileCache.get().clearAll() }
                    }
                    if (level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
                        System.gc()
                    }
                    Timber.d("🧹 MemoryTrimManager: Evicted all in-memory caches (level=$level)")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "MemoryTrimManager: Error during onTrimMemory")
        }
    }

    fun onLowMemory() {
        Timber.w("🚨 MemoryTrimManager: System reported onLowMemory - performing full eviction")
        onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }
}
