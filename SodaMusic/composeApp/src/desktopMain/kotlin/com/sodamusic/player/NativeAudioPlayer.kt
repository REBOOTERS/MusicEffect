package com.sodamusic.player

import com.sodamusic.player.audio.NativeAudioPlayer
import com.sodamusic.player.audio.decode.Mp3Decoder
import com.sodamusic.player.audio.effects.EffectProcessor
import com.sodamusic.player.model.PlayState
import com.sodamusic.player.model.Track
import com.sodamusic.player.model.TrackSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Path
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine
import kotlin.io.path.Path
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

class DesktopAudioPlayer : NativeAudioPlayer {

    override val sampleRate: Int = 44100
    private val outputFormat = AudioFormat(44100f, 16, 2, true, false)

    private var line: SourceDataLine? = null
    private var playJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _playState = MutableStateFlow(PlayState.IDLE)
    override val playState: StateFlow<PlayState> = _playState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    @Volatile
    private var paused = false

    @Volatile
    private var totalDurationMs: Long = 0
    override val positionMs: Long get() = _currentPosition.value
    override val durationMs: Long get() = totalDurationMs

    override fun play(track: Track, effectProcessor: EffectProcessor) {
        stop()
        _playState.value = PlayState.BUFFERING
        _currentPosition.value = 0L

        val dataLine = AudioSystem.getSourceDataLine(outputFormat)
        dataLine.open(outputFormat, 16384)
        dataLine.start()
        line = dataLine

        playJob = scope.launch {
            // Decode first so we know the real duration before publishing PLAYING.
            val loaded = loadPcm(track)
            if (loaded == null) {
                _playState.value = PlayState.ERROR
                try { dataLine.close() } catch (_: Exception) {}
                line = null
                return@launch
            }
            val (samples, durMs) = loaded
            totalDurationMs = durMs
            _playState.value = PlayState.PLAYING

            try {
                var offset = 0
                val frameSize = 4 // stereo * 16-bit
                val buffer = ByteArray(4096)
                val floatBuf = FloatArray(4096 / 2)
                val startNs = System.nanoTime()

                while (isActive && offset < samples.size && _playState.value != PlayState.STOPPED) {
                    if (paused) {
                        kotlinx.coroutines.delay(20)
                        continue
                    }

                    val framesToProcess = minOf(buffer.size / frameSize, (samples.size - offset) / 2)
                    if (framesToProcess <= 0) break

                    for (i in 0 until framesToProcess * 2) {
                        floatBuf[i] = samples[offset + i] / 32768f
                    }
                    effectProcessor.processInterleaved(floatBuf, 0, framesToProcess)

                    for (i in 0 until framesToProcess * 2) {
                        val s = (floatBuf[i] * 32767f).toInt().coerceIn(-32768, 32767)
                        val bi = i * 2
                        buffer[bi] = (s and 0xFF).toByte()
                        buffer[bi + 1] = ((s shr 8) and 0xFF).toByte()
                    }
                    dataLine.write(buffer, 0, framesToProcess * frameSize)
                    offset += framesToProcess * 2
                    _currentPosition.value = (offset / 2L) * 1000L / sampleRate

                    val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
                    val audioMs = _currentPosition.value
                    val waitMs = audioMs - elapsedMs
                    if (waitMs > 5) kotlinx.coroutines.delay(waitMs)
                }
                dataLine.drain()
                _playState.value = PlayState.STOPPED
            } catch (e: Exception) {
                e.printStackTrace()
                _playState.value = PlayState.ERROR
            } finally {
                try { dataLine.close() } catch (_: Exception) {}
                line = null
            }
        }
    }

    /** @return (PCM samples at 44100 Hz stereo s16, duration in ms). Null if nothing can be played. */
    private fun loadPcm(track: Track): Pair<ShortArray, Long>? {
        return when (val s = track.source) {
            is TrackSource.Local -> {
                val file = File(s.filePath)
                if (!file.exists()) return generateDemoTone(track.durationMs.coerceAtLeast(60_000), 440f)
                val lower = s.filePath.lowercase()
                when {
                    lower.endsWith(".wav") -> readWav(file.toPath()) ?: generateDemoTone(60_000, 440f)
                    lower.endsWith(".mp3") -> readMp3(file.toPath()) ?: generateDemoTone(60_000, 440f)
                    else -> {
                        // Fallback to a short demo tone so the player doesn't error out.
                        generateDemoTone(60_000, 440f)
                    }
                }
            }
            is TrackSource.Online -> {
                if (s.streamUrl.startsWith("demo://")) {
                    val num = s.streamUrl.substringAfterLast("/").toIntOrNull() ?: 1
                    val baseFreq = 220f + (num - 1) * 55f
                    generateDemoTone(track.durationMs.coerceAtLeast(60_000), baseFreq)
                } else {
                    generateDemoTone(track.durationMs.coerceAtLeast(60_000), 440f)
                }
            }
        }
    }

    private fun toOutputStereo(raw: ShortArray, inputSr: Int, channels: Int): ShortArray {
        val stereo = if (channels == 1) {
            ShortArray(raw.size * 2).also { out ->
                for (i in raw.indices) { out[i * 2] = raw[i]; out[i * 2 + 1] = raw[i] }
            }
        } else raw
        return if (inputSr != sampleRate) resample(stereo, inputSr, 2) else stereo
    }

    private fun readWav(path: Path): Pair<ShortArray, Long>? {
        return try {
            RandomAccessFile(path.toFile(), "r").use { raf ->
                val riff = ByteArray(4); raf.read(riff)
                raf.readInt()
                val wave = ByteArray(4); raf.read(wave)
                var sr = 44100; var channels = 2; var bits = 16; var dataSize = 0L
                while (raf.filePointer < raf.length()) {
                    val id = ByteArray(4); raf.read(id)
                    val size = raf.readInt().toLong() and 0xFFFFFFFFL
                    when (String(id)) {
                        "fmt " -> {
                            raf.readShort()
                            channels = raf.readShort().toInt() and 0xFFFF
                            sr = raf.readInt()
                            raf.readInt()
                            raf.readShort()
                            bits = raf.readShort().toInt() and 0xFFFF
                            if (size > 16) raf.skipBytes((size - 16).toInt())
                        }
                        "data" -> { dataSize = size; break }
                        else -> raf.skipBytes(size.toInt())
                    }
                }
                if (dataSize == 0L || bits != 16) return null
                val totalSamples = (dataSize / 2).toInt()
                val raw = ShortArray(totalSamples)
                val bb = ByteArray(dataSize.toInt())
                raf.readFully(bb)
                for (i in 0 until totalSamples) {
                    raw[i] = ((bb[i * 2 + 1].toInt() shl 8) or (bb[i * 2].toInt() and 0xFF)).toShort()
                }
                val out = toOutputStereo(raw, sr, channels)
                val durationMs = out.size / 2L * 1000L / sampleRate
                out to durationMs
            }
        } catch (_: Exception) { null }
    }

    private fun readMp3(path: Path): Pair<ShortArray, Long>? {
        val res = Mp3Decoder.decode(path) ?: return null
        val out = toOutputStereo(res.samples, res.sampleRate, res.channels)
        // After resampling, recompute duration based on output sample rate.
        val durationMs = out.size / 2L * 1000L / sampleRate
        return out to durationMs
    }

    private fun resample(input: ShortArray, inputSr: Int, channels: Int): ShortArray {
        val ratio = inputSr.toDouble() / sampleRate
        val outFrames = (input.size / channels / ratio).toInt()
        val out = ShortArray(outFrames * channels)
        for (i in 0 until outFrames) {
            val srcIdx = minOf((i * ratio).toInt() * channels, input.size - channels)
            for (c in 0 until channels) {
                out[i * channels + c] = input[srcIdx + c]
            }
        }
        return out
    }

    private fun generateDemoTone(durationMs: Long, baseFreq: Float): Pair<ShortArray, Long> {
        val durMs = durationMs.coerceAtLeast(60_000)
        val totalFrames = (sampleRate * durMs / 1000).toInt()
        val out = ShortArray(totalFrames * 2)
        for (i in 0 until totalFrames) {
            val t = i.toFloat() / sampleRate
            val env = (sin(PI.toFloat() * t / (durMs / 1000f)) * 0.5f + 0.5f)
            val sample = (
                sin(2f * PI.toFloat() * baseFreq * t) * 0.25f +
                sin(2f * PI.toFloat() * (baseFreq * 1.25f) * t) * 0.20f +
                sin(2f * PI.toFloat() * (baseFreq * 1.5f) * t) * 0.20f +
                sin(2f * PI.toFloat() * (baseFreq * 2f) * t) * 0.15f
            ) * env * 0.7f
            val s = (sample * 32767f).toInt().toShort()
            out[i * 2] = s; out[i * 2 + 1] = s
        }
        return out to durMs
    }

    override fun pause() {
        paused = true
        _playState.value = PlayState.PAUSED
        line?.stop()
    }

    override fun resume() {
        paused = false
        _playState.value = PlayState.PLAYING
        line?.start()
    }

    override fun stop() {
        paused = false
        _playState.value = PlayState.STOPPED
        playJob?.cancel()
        playJob = null
        try { line?.stop(); line?.close() } catch (_: Exception) {}
        line = null
    }

    override fun seekTo(positionMs: Long) { _currentPosition.value = positionMs }

    override fun setVolume(volume: Float) {
        val dl = line ?: return
        try {
            val gain = dl.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl
            gain?.value = 20f * kotlin.math.log10(volume.coerceAtLeast(0.001f).toDouble()).toFloat()
        } catch (_: Exception) {}
    }

    override fun release() {
        stop()
        scope.coroutineContext[Job]?.cancel()
    }
}
