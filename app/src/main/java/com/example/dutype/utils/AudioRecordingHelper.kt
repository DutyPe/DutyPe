package com.example.dutype.utils

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Helper to record 15-second voice intros for blue-collar workers.
 * Uses AAC/MPEG-4 encoding to produce lightweight audio files (~30-50KB)
 * that load quickly even on 2G/3G networks in Tier-2 and Tier-3 areas.
 */
class AudioRecordingHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime: Long = 0L
    private var isCurrentlyRecording: Boolean = false

    var onMaxDurationReached: (() -> Unit)? = null

    val isRecording: Boolean
        get() = isCurrentlyRecording

    val outputFile: File?
        get() = currentOutputFile

    /**
     * Start recording. Maximum duration is strictly capped at 15 seconds.
     */
    fun startRecording(): Result<File> {
        return try {
            stopRecording() // Clean up any existing recorder

            val cacheDir = context.cacheDir
            val audioDir = File(cacheDir, "audio_intros").apply { if (!exists()) mkdirs() }
            val outputFile = File(audioDir, "intro_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(22050)
                setOutputFile(outputFile.absolutePath)
                setMaxDuration(MAX_DURATION_MS)

                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        Timber.d("Voice intro max duration (15s) reached")
                        isCurrentlyRecording = false
                        onMaxDurationReached?.invoke()
                    }
                }

                prepare()
                start()
            }

            mediaRecorder = recorder
            recordingStartTime = System.currentTimeMillis()
            isCurrentlyRecording = true
            Result.success(outputFile)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start audio recording")
            cleanup()
            Result.failure(e)
        }
    }

    /**
     * Stop recording and return the recorded file and duration in seconds.
     */
    fun stopRecording(): Result<Pair<File, Int>> {
        if (!isCurrentlyRecording && currentOutputFile == null) {
            return Result.failure(IllegalStateException("No active recording"))
        }

        return try {
            val durationSec = ((System.currentTimeMillis() - recordingStartTime) / 1000L).toInt().coerceIn(1, 15)
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: RuntimeException) {
                    Timber.w(e, "MediaRecorder stop failed (possibly too short)")
                }
                release()
            }
            mediaRecorder = null
            isCurrentlyRecording = false

            val file = currentOutputFile
            if (file != null && file.exists() && file.length() > 0) {
                Result.success(Pair(file, durationSec))
            } else {
                Result.failure(IOException("Recorded audio file is empty or missing"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop audio recording")
            cleanup()
            Result.failure(e)
        }
    }

    /**
     * Discard the current recording and clean up temporary files.
     */
    fun cleanup() {
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {}
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null
        isCurrentlyRecording = false

        currentOutputFile?.let {
            if (it.exists()) {
                it.delete()
            }
        }
        currentOutputFile = null
    }

    companion object {
        const val MAX_DURATION_MS = 15000 // 15 seconds

        /**
         * Upload recorded audio file to Firebase Storage.
         * Returns Pair(downloadUrl, durationSeconds).
         */
        suspend fun uploadToFirebaseStorage(
            file: File,
            userId: String,
            durationSec: Int
        ): Result<Pair<String, Int>> = withContext(Dispatchers.IO) {
            try {
                if (!file.exists() || file.length() == 0L) {
                    return@withContext Result.failure(IllegalArgumentException("Audio file is empty"))
                }

                val storageRef = FirebaseStorage.getInstance().reference
                val audioRef = storageRef.child("audio_intros/$userId/intro_${System.currentTimeMillis()}.m4a")

                val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                    .setContentType("audio/mp4")
                    .setCustomMetadata("durationSec", durationSec.toString())
                    .build()

                audioRef.putFile(Uri.fromFile(file), metadata).await()
                val downloadUrl = audioRef.downloadUrl.await().toString()
                Result.success(Pair(downloadUrl, durationSec))
            } catch (e: Exception) {
                Timber.e(e, "Failed to upload audio intro to Firebase Storage")
                Result.failure(e)
            }
        }
    }
}
