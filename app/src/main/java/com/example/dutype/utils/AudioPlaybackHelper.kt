package com.example.dutype.utils

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * Lightweight helper to play 15-second audio intro previews
 * using Android's native MediaPlayer.
 */
class AudioPlaybackHelper(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var currentPlayingSource: String? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentPositionSec = MutableStateFlow(0)
    val currentPositionSec: StateFlow<Int> = _currentPositionSec.asStateFlow()

    private val _durationSec = MutableStateFlow(0)
    val durationSec: StateFlow<Int> = _durationSec.asStateFlow()

    val currentSource: String?
        get() = currentPlayingSource

    fun play(source: String, onCompletion: (() -> Unit)? = null) {
        if (currentPlayingSource == source && mediaPlayer != null) {
            if (_isPlaying.value) {
                pause()
            } else {
                resume()
            }
            return
        }

        stop()

        try {
            currentPlayingSource = source
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(source)
                setOnPreparedListener { mp ->
                    val totalDuration = (mp.duration / 1000).coerceAtLeast(1)
                    _durationSec.value = totalDuration
                    mp.start()
                    _isPlaying.value = true
                    startProgressTracker()
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    _progress.value = 0f
                    _currentPositionSec.value = 0
                    stopProgressTracker()
                    onCompletion?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Timber.e("MediaPlayer error: what=$what, extra=$extra")
                    stop()
                    false
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Timber.e(e, "Failed to start audio playback for $source")
            stop()
        }
    }

    fun playFile(file: File, onCompletion: (() -> Unit)? = null) {
        play(file.absolutePath, onCompletion)
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _isPlaying.value = false
                    stopProgressTracker()
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error pausing playback")
        }
    }

    fun resume() {
        try {
            mediaPlayer?.let {
                it.start()
                _isPlaying.value = true
                startProgressTracker()
            }
        } catch (e: Exception) {
            Timber.w(e, "Error resuming playback")
        }
    }

    fun stop() {
        stopProgressTracker()
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            Timber.w(e, "Error stopping MediaPlayer")
        }
        mediaPlayer = null
        currentPlayingSource = null
        _isPlaying.value = false
        _progress.value = 0f
        _currentPositionSec.value = 0
    }

    fun release() {
        stop()
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { mp ->
                    try {
                        if (mp.isPlaying) {
                            val current = mp.currentPosition
                            val total = mp.duration.coerceAtLeast(1)
                            _progress.value = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                            _currentPositionSec.value = (current / 1000).coerceAtLeast(0)
                        }
                    } catch (_: Exception) {}
                }
                delay(100)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }
}
