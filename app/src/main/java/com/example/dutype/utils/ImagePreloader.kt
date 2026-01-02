package com.example.dutype.utils

import android.content.Context
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.CachePolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ImagePreloader - Preloads images for smoother scrolling
 * 
 * PERFORMANCE OPTIMIZATION:
 * - Preloads job images before they appear on screen
 * - Uses Coil's disk cache for persistence
 * - Runs on IO dispatcher to avoid blocking UI
 * 
 * Usage:
 * - Call preloadJobImages() when job list is loaded
 * - Images will be cached and ready when user scrolls
 */
@Singleton
class ImagePreloader @Inject constructor() {
    
    private var imageLoader: ImageLoader? = null
    
    /**
     * Initialize with context (call once in Application or Activity)
     */
    fun initialize(context: Context) {
        if (imageLoader == null) {
            imageLoader = ImageLoader.Builder(context)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
        }
    }
    
    /**
     * Preload a list of image URLs
     */
    fun preloadImages(context: Context, imageUrls: List<String>, scope: CoroutineScope) {
        if (imageUrls.isEmpty()) return
        
        val loader = imageLoader ?: ImageLoader.Builder(context)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build().also { imageLoader = it }
        
        scope.launch(Dispatchers.IO) {
            val validUrls = imageUrls.filter { it.isNotBlank() }.take(20) // Limit to 20 images
            Timber.d("🖼️ Preloading ${validUrls.size} images")
            
            validUrls.forEach { url ->
                try {
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build()
                    
                    loader.enqueue(request)
                } catch (e: Exception) {
                    // Silently ignore preload failures
                }
            }
        }
    }
    
    /**
     * Preload job images from job summaries
     */
    fun preloadJobImages(
        context: Context,
        jobImageUrls: List<String>,
        scope: CoroutineScope
    ) {
        preloadImages(context, jobImageUrls, scope)
    }
    
    /**
     * Preload profile images
     */
    fun preloadProfileImages(
        context: Context,
        profileImageUrls: List<String>,
        scope: CoroutineScope
    ) {
        preloadImages(context, profileImageUrls, scope)
    }
    
    /**
     * Clear image cache (call on logout or low memory)
     */
    fun clearCache() {
        imageLoader?.memoryCache?.clear()
        Timber.d("🖼️ Image cache cleared")
    }
}
