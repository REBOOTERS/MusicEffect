package com.sodamusic.player

import android.content.Context
import android.Manifest
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.net.Uri
import com.sodamusic.player.audio.NativeAudioPlayer
import com.sodamusic.player.audio.effects.AudioEffect
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
    private var audioFx: SodaAudioFx? = null
    private var visualizer: Visualizer? = null
    private var pendingEffect: AudioEffect = AudioEffect.NONE
    private var posJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _playState = MutableStateFlow(PlayState.IDLE)
    override val playState: StateFlow<PlayState> = _playState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _spectrum = MutableStateFlow(FloatArray(SPECTRUM_BANDS))
    override val spectrum: StateFlow<FloatArray> = _spectrum.asStateFlow()

    @Volatile
    private var totalDurationMs: Long = 0
    override val positionMs: Long get() = runCatching { mediaPlayer?.currentPosition?.toLong() }.getOrNull() ?: _currentPosition.value
    override val durationMs: Long get() = totalDurationMs

    override fun play(track: Track, effectProcessor: EffectProcessor) {
        stop()
        _playState.value = PlayState.BUFFERING
        _currentPosition.value = 0
        pendingEffect = effectProcessor.currentEffect

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
                // Attach the native AudioFx chain to this session and apply the current preset.
                audioFx = SodaAudioFx(player.audioSessionId).also { it.configure(pendingEffect) }
                attachVisualizer(player.audioSessionId)
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

    private fun attachVisualizer(sessionId: Int) {
        // Visualizer requires RECORD_AUDIO at runtime; request it before creating one.
        scope.launch {
            val granted = AndroidFilePicker.requestPermission(Manifest.permission.RECORD_AUDIO)
            if (granted) attachVisualizerInternal(sessionId)
        }
    }

    private fun attachVisualizerInternal(sessionId: Int) {
        detachVisualizer()
        try {
            val viz = Visualizer(sessionId)
            val captureSize = Visualizer.getCaptureSizeRange().let {
                it.getOrElse(1) { 256 }
            }
            viz.captureSize = captureSize.coerceAtLeast(64)
            viz.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        val data = fft ?: return
                        _spectrum.value = fftToBands(data)
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                false,
                true
            )
            viz.enabled = true
            visualizer = viz
        } catch (_: Exception) {
            visualizer = null
        }
    }

    private fun detachVisualizer() {
        try { visualizer?.enabled = false; visualizer?.release() } catch (_: Exception) {}
        visualizer = null
        _spectrum.value = FloatArray(SPECTRUM_BANDS)
    }

    /** Convert JLayer/Android FFT bytes (interleaved re/im, scaled -128..127) to log-spaced bands. */
    private fun fftToBands(fft: ByteArray): FloatArray {
        val n = fft.size / 2
        if (n <= 1) return FloatArray(SPECTRUM_BANDS)
        val mags = FloatArray(n)
        var maxVal = 1e-9f
        for (k in 1 until n) {
            val re = fft[k * 2].toFloat()
            val im = fft[k * 2 + 1].toFloat()
            val mag = Math.sqrt((re * re + im * im).toDouble()).toFloat()
            mags[k] = mag
            if (mag > maxVal) maxVal = mag
        }
        val out = FloatArray(SPECTRUM_BANDS)
        val logN = kotlin.math.ln(n.toFloat())
        var prevBin = 0
        for (b in 0 until SPECTRUM_BANDS) {
            val hi = (logN * (b + 1) / SPECTRUM_BANDS).coerceIn(0f, logN)
            val binHi = kotlin.math.exp(hi.toDouble()).toInt().coerceIn(prevBin + 1, n)
            var sum = 0f; var count = 0
            for (k in (prevBin + 1) until binHi) {
                if (k in 1 until n) { sum += mags[k]; count++ }
            }
            val avg = if (count > 0) sum / count else 0f
            out[b] = ((avg / maxVal).toDouble().pow(0.6)).toFloat().coerceIn(0f, 1f)
            prevBin = binHi
        }
        return out
    }

    private fun Double.pow(e: Double) = Math.pow(this, e)

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
        detachVisualizer()
        try { audioFx?.release() } catch (_: Exception) {}
        audioFx = null
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

    override fun applyEffect(effect: AudioEffect) {
        pendingEffect = effect
        audioFx?.configure(effect)
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
        private const val SPECTRUM_BANDS = 32
    }
}
