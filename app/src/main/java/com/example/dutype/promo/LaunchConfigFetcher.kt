package com.example.dutype.promo

import android.content.Context
import android.net.ConnectivityManager
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.dutype.firestore.FirestoreCollections
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

/**
 * One cheap, background-only fetch of `app_config/launch` (a single document read), followed by
 * an image pre-warm into Coil's disk cache. Never called from the startup path or awaited by UI.
 *
 * Document formats accepted (both are equivalent):
 *  - a string field `config` holding the JSON text (easiest to paste in the console), or
 *  - native fields matching the JSON schema.
 */
object LaunchConfigFetcher {
    const val DOC_ID = "launch"
    private const val TAG = "PromoConfig"
    private const val MAX_IMAGE_BYTES_UNMETERED = 4L * 1024L * 1024L
    private const val MAX_IMAGE_BYTES_METERED = 1L * 1024L * 1024L
    private const val MAX_PREFETCH = 3

    /** @return true when the fetch itself succeeded (even if the payload was rejected). */
    suspend fun refresh(context: Context): Boolean {
        val ctx = context.applicationContext
        try {
            val snap = FirebaseFirestore.getInstance()
                .collection(FirestoreCollections.APP_CONFIG)
                .document(DOC_ID)
                .get(Source.SERVER)
                .await()

            val raw: String = if (!snap.exists()) {
                // The server confirmed there is no config: treat as "feature off" so a removed
                // campaign disappears and the icon reverts to default.
                Timber.tag(TAG).d("app_config/launch missing on server -> feature disabled")
                "{\"version\":0,\"enabled\":false}"
            } else {
                val text = snap.getString("config")
                if (!text.isNullOrBlank()) text else JSONObject(snap.data.orEmpty()).toString()
            }

            val accepted = LaunchConfigStore.save(ctx, raw)
            LaunchConfigStore.markFetched(ctx)
            if (accepted) {
                LaunchConfigStore.current(ctx)?.let { prefetchImages(ctx, it) }
            }
            return true
        } catch (t: Throwable) {
            // Offline / permission / anything else: keep the last good config.
            Timber.tag(TAG).d("Fetch failed (%s); keeping last good config", t.javaClass.simpleName)
            return false
        }
    }

    /** True only when the image bytes are already in Coil's disk cache. Cheap; safe on IO. */
    fun isImageCached(context: Context, url: String): Boolean {
        return try {
            context.applicationContext.imageLoader.diskCache?.openSnapshot(url)?.use { true } == true
        } catch (_: Throwable) {
            false
        }
    }

    private suspend fun prefetchImages(ctx: Context, config: LaunchConfig) {
        if (!config.enabled) return
        val now = System.currentTimeMillis()
        val urls = config.promos
            .filter { it.imageUrl.isNotEmpty() && (it.endAt <= 0L || it.endAt > now) }
            .sortedByDescending { it.priority }
            .map { it.imageUrl }
            .distinct()
            .take(MAX_PREFETCH)

        for (url in urls) {
            if (isImageCached(ctx, url)) continue
            val limit = if (isMetered(ctx)) MAX_IMAGE_BYTES_METERED else MAX_IMAGE_BYTES_UNMETERED
            val size = contentLength(url)
            // Unknown size on a metered network is skipped; known size above the limit is skipped.
            if (size > limit || (size < 0L && isMetered(ctx))) {
                Timber.tag(TAG).d("Skip prefetch (size=%d, limit=%d): %s", size, limit, url)
                continue
            }
            try {
                val request = ImageRequest.Builder(ctx)
                    .data(url)
                    .diskCacheKey(url)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    // The disk cache stores the original bytes; decode tiny so no big bitmap is built.
                    .size(64, 64)
                    .build()
                ctx.imageLoader.execute(request)
                Timber.tag(TAG).d("Prefetched promo image cached=%s url=%s", isImageCached(ctx, url), url)
            } catch (t: Throwable) {
                Timber.tag(TAG).d("Prefetch failed: %s", t.javaClass.simpleName)
            }
        }
    }

    private fun isMetered(ctx: Context): Boolean = try {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.isActiveNetworkMetered
    } catch (_: Throwable) {
        true
    }

    /** HEAD request for Content-Length; -1 when unknown. */
    private suspend fun contentLength(url: String): Long = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 5_000
                readTimeout = 5_000
                instanceFollowRedirects = true
            }
            if (conn.responseCode in 200..299) conn.contentLengthLong else -1L
        } catch (_: Throwable) {
            -1L
        } finally {
            try { conn?.disconnect() } catch (_: Throwable) { }
        }
    }
}
