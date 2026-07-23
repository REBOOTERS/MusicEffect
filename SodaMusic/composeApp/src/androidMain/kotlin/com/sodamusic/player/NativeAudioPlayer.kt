package com.sodamusic.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.sodamusic.player.audio.NativeAudioPlayer
import com.sodamusic.player.audio.effects.EffectProcessor
import com.sodamusic.player.model.PlayState
import com.sodamusic.player.model.Track
import com.sodamusic.player.model.TrackSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Android player built on [MediaPlayer].
 *
 * MediaPlayer is the standard, reliable Android audio API: it handles `content://` URIs
 * (SAF picks) and file paths natively, decodes every common format, and exposes duration
 * and seeking out of the box. The custom DSP/EffectProcessor chain is not applied here
 * (it stays a desktop-only feature); on Android, audio effects should use the platform
 * AudioFx framework, which is a separate concern. Reliability of playback comes first.
 */
class AndroidAudioPlayer : NativeAudioPlayer {

    override val sampleRate: Int = 44100

    private var mediaPlayer: MediaPlayer? = null
    private var posJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _playState = MutableStateFlow(PlayState.IDLE)
    override val playState: StateFlow<PlayState> = _playState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    @Volatile
    private var totalDurationMs: Long = 0
    override val positionMs: Long get() = runCatching { mediaPlayer?.currentPosition?.toLong() }.getOrNull() ?: _currentPosition.value
    override val durationMs: Long get() = totalDurationMs

    override fun play(track: Track, effectProcessor: EffectProcessor) {
        stop()
        _playState.value = PlayState.BUFFERING
        _currentPosition.value = 0

        val ctx = appContext
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            when (val src = track.source) {
                is TrackSource.Local -> mp.setDataSource(src.filePath)
                is TrackSource.ContentUri -> mp.setDataSource(ctx, Uri.parse(src.uri))
                is TrackSource.Online -> mp.setDataSource(src.streamUrl)
            }
            mp.setOnPreparedListener { player ->
                totalDurationMs = runCatching { player.duration.toLong() }.getOrDefault(0L)
                player.start()
                _playState.value = PlayState.PLAYING
                startPositionTracking()
            }
            mp.setOnCompletionListener {
                _playState.value = PlayState.STOPPED
                stopPositionTracking()
            }
            mp.setOnErrorListener { _, _, _ ->
                _playState.value = PlayState.ERROR
                stopPositionTracking()
                true
            }
            mp.prepareAsync()
            mediaPlayer = mp
        } catch (e: Exception) {
            e.printStackTrace()
            try { mp.release() } catch (_: Exception) {}
            _playState.value = PlayState.ERROR
        }
    }

    private fun startPositionTracking() {
        stopPositionTracking()
        posJob = scope.launch {
            while (isActive) {
                val mp = mediaPlayer ?: break
                if (_playState.value == PlayState.PLAYING) {
                    _currentPosition.value = runCatching { mp.currentPosition.toLong() }.getOrDefault(_currentPosition.value)
                }
                delay(200)
            }
        }
    }

    private fun stopPositionTracking() {
        posJob?.cancel()
        posJob = null
    }

    override fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) it.pause()
        }
        _playState.value = PlayState.PAUSED
    }

    override fun resume() {
        mediaPlayer?.start()
        _playState.value = PlayState.PLAYING
    }

    override fun stop() {
        _playState.value = PlayState.STOPPED
        stopPositionTracking()
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        totalDurationMs = 0
    }

    override fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            _currentPosition.value = positionMs
        } catch (_: Exception) {}
    }

    override fun setVolume(volume: Float) {
        try { mediaPlayer?.setVolume(volume, volume) } catch (_: Exception) {}
    }

    override fun release() {
        stop()
        scope.coroutineContext[Job]?.cancel()
    }

    companion object {
        lateinit var appContext: Context
    }
}
