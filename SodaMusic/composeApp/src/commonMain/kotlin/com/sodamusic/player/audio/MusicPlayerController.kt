package com.sodamusic.player.audio

import com.sodamusic.player.audio.effects.AudioEffect
import com.sodamusic.player.audio.effects.EffectProcessor
import com.sodamusic.player.model.PlayState
import com.sodamusic.player.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Platform audio player interface. Each target implements this via
 * a platform-specific factory function createNativeAudioPlayer().
 */
interface NativeAudioPlayer {
    val sampleRate: Int
    val positionMs: Long
    val durationMs: Long
    val playState: StateFlow<PlayState>
    val currentPosition: StateFlow<Long>
    fun play(track: Track, effectProcessor: EffectProcessor)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    fun release()
}

expect fun createNativeAudioPlayer(): NativeAudioPlayer

class MusicPlayerController {
    private val player: NativeAudioPlayer = createNativeAudioPlayer()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()
    val playState: StateFlow<PlayState> = player.playState
    val position: StateFlow<Long> = player.currentPosition

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentEffect = MutableStateFlow(AudioEffect.NONE)
    val currentEffect: StateFlow<AudioEffect> = _currentEffect.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val effectProcessor = EffectProcessor(player.sampleRate)

    val durationMs: Long get() = player.durationMs
    val availableEffects: List<AudioEffect> = AudioEffect.all

    fun setQueue(tracks: List<Track>) { _queue.value = tracks }

    fun play(track: Track) {
        _currentTrack.value = track
        effectProcessor.configure(_currentEffect.value)
        player.play(track, effectProcessor)
    }

    fun togglePlayPause() {
        when (_playStateValue()) {
            PlayState.PLAYING, PlayState.BUFFERING -> player.pause()
            PlayState.PAUSED -> player.resume()
            else -> _currentTrack.value?.let { play(it) }
        }
    }
    private fun _playStateValue() = playState.value

    fun next() {
        val list = _queue.value
        val current = _currentTrack.value ?: return
        val idx = list.indexOf(current)
        if (idx >= 0 && idx < list.lastIndex) play(list[idx + 1])
        else if (list.isNotEmpty()) play(list[0])
    }

    fun previous() {
        val list = _queue.value
        val current = _currentTrack.value ?: return
        val idx = list.indexOf(current)
        if (idx > 0) play(list[idx - 1])
        else if (list.isNotEmpty()) play(list.last())
    }

    fun setEffect(effect: AudioEffect) {
        _currentEffect.value = effect
        effectProcessor.configure(effect)
    }

    fun seekTo(ms: Long) = player.seekTo(ms)
    fun setVolume(v: Float) { _volume.value = v; player.setVolume(v) }
    fun toggleShuffle() { _isShuffle.value = !_isShuffle.value }
    fun cycleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
    }
    fun release() = player.release()
}

enum class RepeatMode { NONE, ALL, ONE }
