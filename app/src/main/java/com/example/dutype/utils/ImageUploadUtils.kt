package com.example.dutype.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream

/**
 * ImageUploadUtils - Utility for image compression and upload with retry
 * 
 * P1 FIX: Adds retry with exponential backoff for image uploads.
 * Also includes image compression to reduce bandwidth usage.
 * 
 * Features:
 * - Image compression (max 2MB file size while maintaining quality)
 * - Retry with exponential backoff (max 3 retries)
 * - Progress tracking
 * - Automatic cleanup on failure
 * 
 * @author DutyPe Engineering Team
 * @since 2.3.0
 */
object ImageUploadUtils {
    
    // #5 fix: aggressive defaults so users on poor networks (the common case)
    // get a usable upload time. The previous 1200px / 2MB / 3-retry / 1s→10s
    // backoff configuration combined with Firebase Storage's internal
    // exponential backoff (≈30s) was producing minute-long stalls on Wi-Fi
    // hiccups and blocking the post-job flow.
    private const val MAX_WIDTH = 800
    private const val JPEG_QUALITY = 75
    private const val MAX_FILE_SIZE_BYTES = 400 * 1024  // 400KB max
    private const val MAX_RETRIES = 2
    private const val INITIAL_DELAY_MS = 600L
    private const val MAX_DELAY_MS = 3_000L
    // Per-attempt hard ceiling. Firebase's own retry chain can run ≈30s on a
    // single attempt; we cap it so the user can react instead of watching a
    // spinner forever.
    private const val PER_ATTEMPT_TIMEOUT_MS = 25_000L

    
    /**
     * Upload result sealed class
     */
    sealed class UploadResult {
        data class Success(val downloadUrl: String) : UploadResult()
        data class Failure(val error: String, val exception: Exception? = null) : UploadResult()
        data class Progress(val progress: Int) : UploadResult()
    }
    
    /**
     * Compress image before upload
     * Ensures image is under 2MB while maintaining acceptable quality
     * Uses adaptive quality reduction if needed
     */
    fun compressImage(
        context: Context,
        uri: Uri,
        maxWidth: Int = MAX_WIDTH,
        quality: Int = JPEG_QUALITY
    ): ByteArray? {
        return try {
            // Load bitmap bounds first
            val inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()
            
            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            
            Timber.d("📸 COMPRESS: Original size: ${originalWidth}x${originalHeight}")
            
            // Calculate sample size for efficient memory usage
            var sampleSize = 1
            if (originalWidth > maxWidth) {
                sampleSize = (originalWidth.toFloat() / maxWidth).toInt()
            }
            
            // Decode with sample size
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val newInputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(newInputStream, null, decodeOptions)
            newInputStream?.close()
            
            if (bitmap == null) {
                Timber.w("📸 COMPRESS: Failed to decode bitmap")
                return null
            }
            
            // Scale if still too large
            val scaledBitmap = if (bitmap.width > maxWidth) {
                val ratio = maxWidth.toFloat() / bitmap.width
                val newHeight = (bitmap.height * ratio).toInt()
                Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true).also {
                    if (it != bitmap) bitmap.recycle()
                }
            } else {
                bitmap
            }
            
            // Compress to JPEG with adaptive quality to ensure under 2MB
            var currentQuality = quality
            var compressedBytes: ByteArray
            val outputStream = ByteArrayOutputStream()
            
            do {
                outputStream.reset()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, outputStream)
                compressedBytes = outputStream.toByteArray()
                
                val sizeKB = compressedBytes.size / 1024
                val sizeMB = sizeKB / 1024.0
                
                Timber.d("📸 COMPRESS: Quality $currentQuality -> ${sizeKB}KB (${String.format("%.2f", sizeMB)}MB)")
                
                // If still over 2MB and quality can be reduced, try again
                if (compressedBytes.size > MAX_FILE_SIZE_BYTES && currentQuality > 50) {
                    currentQuality -= 10
                    Timber.d("📸 COMPRESS: File too large, reducing quality to $currentQuality")
                } else {
                    break
                }
            } while (compressedBytes.size > MAX_FILE_SIZE_BYTES && currentQuality >= 50)
            
            // Cleanup
            scaledBitmap.recycle()
            outputStream.close()
            
            val finalSizeKB = compressedBytes.size / 1024
            val finalSizeMB = finalSizeKB / 1024.0
            Timber.d("📸 COMPRESS: Final: ${originalWidth}x${originalHeight} -> ${finalSizeKB}KB (${String.format("%.2f", finalSizeMB)}MB) at quality $currentQuality")
            
            if (compressedBytes.size > MAX_FILE_SIZE_BYTES) {
                Timber.w("📸 COMPRESS: Warning - Image still over 2MB after compression (${String.format("%.2f", finalSizeMB)}MB)")
            }
            
            compressedBytes
        } catch (e: Exception) {
            Timber.e(e, "📸 COMPRESS: Failed to compress image")
            null
        }
    }

    
    /**
     * Upload image with retry and exponential backoff
     * 
     * @param context Application context
     * @param uri Image URI to upload
     * @param storagePath Firebase Storage path
     * @param onProgress Progress callback (0-100)
     * @return UploadResult with download URL or error
     */
    suspend fun uploadWithRetry(
        context: Context,
        uri: Uri,
        storagePath: String,
        onProgress: ((Int) -> Unit)? = null
    ): UploadResult = withContext(Dispatchers.IO) {
        var currentDelay = INITIAL_DELAY_MS
        var lastException: Exception? = null
        
        // Compress image first
        val compressedBytes = compressImage(context, uri)
        
        repeat(MAX_RETRIES) { attempt ->
            try {
                Timber.d("📸 UPLOAD: Attempt ${attempt + 1}/$MAX_RETRIES for $storagePath")
                
                val storage = FirebaseStorage.getInstance().apply {
                    // Trim Firebase's internal retry/operation budgets so a
                    // failing attempt doesn't wedge for 30s+ before our own
                    // retry kicks in.
                    maxOperationRetryTimeMillis = 8_000L
                    maxUploadRetryTimeMillis = 8_000L
                }
                val storageRef = storage.reference.child(storagePath)
                
                kotlinx.coroutines.withTimeout(PER_ATTEMPT_TIMEOUT_MS) {
                    val uploadTask = if (compressedBytes != null) {
                        Timber.d("📸 UPLOAD: Uploading compressed image (${compressedBytes.size / 1024}KB)")
                        storageRef.putBytes(compressedBytes)
                    } else {
                        Timber.d("📸 UPLOAD: Uploading original file (compression failed)")
                        storageRef.putFile(uri)
                    }

                    uploadTask.addOnProgressListener { snapshot ->
                        val progress = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount).toInt()
                        onProgress?.invoke(progress)
                    }

                    uploadTask.await()
                }

                val downloadUrl = kotlinx.coroutines.withTimeout(8_000L) {
                    FirebaseStorage.getInstance().reference.child(storagePath)
                        .downloadUrl.await().toString()
                }
                
                Timber.i("📸 UPLOAD: ✅ Success! URL: $downloadUrl")
                return@withContext UploadResult.Success(downloadUrl)
                
            } catch (e: Exception) {
                lastException = e
                Timber.w(e, "📸 UPLOAD: Attempt ${attempt + 1} failed")
                
                // Don't delay on last attempt
                if (attempt < MAX_RETRIES - 1) {
                    Timber.d("📸 UPLOAD: Waiting ${currentDelay}ms before retry")
                    kotlinx.coroutines.delay(currentDelay)
                    currentDelay = (currentDelay * 2).coerceAtMost(MAX_DELAY_MS)
                }
            }
        }
        
        // All retries exhausted
        Timber.e(lastException, "📸 UPLOAD: ❌ All $MAX_RETRIES attempts failed")
        UploadResult.Failure(
            error = lastException?.message ?: "Upload failed after $MAX_RETRIES attempts",
            exception = lastException
        )
    }
    
    /**
     * Delete uploaded image (cleanup on failure)
     */
    suspend fun deleteImage(storagePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val storage = FirebaseStorage.getInstance()
            storage.reference.child(storagePath).delete().await()
            Timber.d("📸 DELETE: Deleted image at $storagePath")
            true
        } catch (e: Exception) {
            Timber.w(e, "📸 DELETE: Failed to delete image at $storagePath")
            false
        }
    }
}
