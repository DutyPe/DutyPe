package com.example.dutype.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Enterprise Storage & Cache Manager
 * Prevents local storage bloating by pruning stale temporary files and bounded image caches.
 */
object StorageCacheManager {

    suspend fun pruneStaleCache(context: Context, maxAgeDays: Int = 7) = withContext(Dispatchers.IO) {
        runCatching {
            val cacheDir = context.cacheDir
            val now = System.currentTimeMillis()
            val maxAgeMs = maxAgeDays * 24 * 60 * 60 * 1000L

            var totalBytesDeleted = 0L
            var filesDeletedCount = 0

            cacheDir.walkBottomUp().forEach { file ->
                if (file.isFile && (now - file.lastModified()) > maxAgeMs) {
                    val size = file.length()
                    if (file.delete()) {
                        totalBytesDeleted += size
                        filesDeletedCount++
                    }
                }
            }

            if (filesDeletedCount > 0) {
                Timber.i("🧹 StorageCacheManager: Pruned $filesDeletedCount stale cache files (${totalBytesDeleted / (1024 * 1024)} MB freed)")
            }
        }.onFailure { error ->
            Timber.w(error, "StorageCacheManager: Pruning stale cache encountered an error")
        }
    }
}
