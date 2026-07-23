package com.sodamusic.player

import com.sodamusic.player.audio.NativeAudioPlayer
import com.sodamusic.player.audio.effects.EffectProcessor
import com.sodamusic.player.audio.effects.Resampler
import com.sodamusic.player.audio.decode.Mp3Decoder
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

    // We output to SourceDataLine at whatever sample rate the source is.
    // sampleRate property stays 44100 for the EffectProcessor (which is tuned to 44.1k);
    // when an effect is active we run DSP at 44100 then resample up to the source rate.
    override val sampleRate: Int = 44100

    private var line: SourceDataLine? = null
    private var playJob: Job? = null
    private var lineFormat: AudioFormat? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _playState = MutableStateFlow(PlayState.IDLE)
    override val playState: StateFlow<PlayState> = _playState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    @Volatile
    private var paused = false

    @Volatile
    private var totalDurationMs: Long = 0

    @Volatile
    private var currentSampleRate: Int = 44100
    override val positionMs: Long get() = _currentPosition.value
    override val durationMs: Long get() = totalDurationMs

    override fun play(track: Track, effectProcessor: EffectProcessor) {
        stop()
        _playState.value = PlayState.BUFFERING
        _currentPosition.value = 0L

        playJob = scope.launch {
            val decoded = decodeToPcm(track)
            if (decoded == null) {
                _playState.value = PlayState.ERROR
                return@launch
            }
            val (srcSamples, srcSr, srcChannels) = decoded
            totalDurationMs = (srcSamples.size / srcChannels) * 1000L / srcSr
            currentSampleRate = srcSr

            // Final PCM that will be sent to the sound card (interleaved stereo s16 @ outSr).
            val outSamples: ShortArray
            val outSr: Int
            val useEffects = effectProcessor.currentEffect != com.sodamusic.player.audio.effects.AudioEffect.NONE

            if (useEffects) {
                // DSP is tuned for 44100 Hz. Resample source -> 44100 if needed, run the chain,
                // then the output stays at 44100 (the OS mixer will handle the last mile).
                val dspRate = 44100
                val dspStereo = if (srcSr != dspRate) {
                    Resampler.resampleStereo(srcSamples, srcSr, dspRate)
                } else if (srcChannels == 1) {
                    monoToStereo(srcSamples)
                } else srcSamples

                val floatBuf = FloatArray(dspStereo.size)
                for (i in dspStereo.indices) floatBuf[i] = dspStereo[i] / 32768f

                var processed = 0
                val blockSize = 2048 // frames per processInterleaved call
                while (processed < dspStereo.size / 2) {
                    val frames = minOf(blockSize, dspStereo.size / 2 - processed)
                    effectProcessor.processInterleaved(floatBuf, processed * 2, frames)
                    processed += frames
                }
                outSamples = Resampler.resampleFloatToS16(floatBuf, dspRate, dspRate) // stays at 44100
                outSr = dspRate
            } else {
                // Bypass all DSP — deliver the decoded PCM directly.
                outSamples = if (srcChannels == 1) monoToStereo(srcSamples) else srcSamples
                outSr = srcSr
            }

            // Open the line at the actual output sample rate (zero resampling for bypass path).
            val outFormat = AudioFormat(outSr.toFloat(), 16, 2, true, false)
            val dataLine = try {
                val dl = AudioSystem.getSourceDataLine(outFormat)
                dl.open(outFormat, 65536) // 64 KB buffer ≈ 340 ms @ 48k stereo s16
                dl
            } catch (e: Exception) {
                // Fallback to 48000 if the native rate isn't supported.
                val fallbackFormat = AudioFormat(48000f, 16, 2, true, false)
                val dl = AudioSystem.getSourceDataLine(fallbackFormat)
                dl.open(fallbackFormat, 65536)
                dl
            }
            dataLine.start()
            line = dataLine
            lineFormat = dataLine.format
            val lineSr = dataLine.format.sampleRate.toInt()
            val finalSamples = if (lineSr != outSr) {
                Resampler.resampleStereo(outSamples, outSr, lineSr)
            } else outSamples
            val actualOutSr = lineSr
            totalDurationMs = (finalSamples.size / 2) * 1000L / actualOutSr

            _playState.value = PlayState.PLAYING
            try {
                val frameSize = 4
                val chunkFrames = 4096
                val byteBuf = ByteArray(chunkFrames * frameSize)
                var offset = 0
                val startNs = System.nanoTime()

                while (isActive && offset < finalSamples.size && _playState.value != PlayState.STOPPED) {
                    if (paused) {
                        kotlinx.coroutines.delay(20)
                        continue
                    }

                    val framesToWrite = minOf(chunkFrames, (finalSamples.size - offset) / 2)
                    if (framesToWrite <= 0) break

                    for (i in 0 until framesToWrite * 2) {
                        val s = finalSamples[offset + i].toInt()
                        val bi = i * 2
                        byteBuf[bi] = (s and 0xFF).toByte()
                        byteBuf[bi + 1] = ((s shr 8) and 0xFF).toByte()
                    }
                    dataLine.write(byteBuf, 0, framesToWrite * frameSize)
                    offset += framesToWrite * 2
                    _currentPosition.value = (offset / 2L) * 1000L / actualOutSr

                    // Light throttling: don't get more than ~250 ms ahead of real time.
                    val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
                    val audioMs = _currentPosition.value
                    val aheadMs = audioMs - elapsedMs
                    if (aheadMs > 250) kotlinx.coroutines.delay(aheadMs - 200)
                }
                dataLine.drain()
                _playState.value = PlayState.STOPPED
            } catch (e: Exception) {
                e.printStackTrace()
                _playState.value = PlayState.ERROR
            } finally {
                try { dataLine.close() } catch (_: Exception) {}
                line = null
                lineFormat = null
            }
        }
    }

    private fun monoToStereo(mono: ShortArray): ShortArray {
        val out = ShortArray(mono.size * 2)
        for (i in mono.indices) { out[i * 2] = mono[i]; out[i * 2 + 1] = mono[i] }
        return out
    }

    private data class Decoded(val samples: ShortArray, val sampleRate: Int, val channels: Int)

    private fun decodeToPcm(track: Track): Decoded? {
        return when (val s = track.source) {
            is TrackSource.Local -> {
                val file = File(s.filePath)
                if (!file.exists()) return decodeDemoTone(track)
                val lower = s.filePath.lowercase()
                when {
                    lower.endsWith(".wav") -> {
                        val wav = readWav(file.toPath()) ?: return decodeDemoTone(track)
                        Decoded(wav.first, wav.second, wav.third)
                    }
                    lower.endsWith(".mp3") -> {
                        val mp3 = Mp3Decoder.decode(file.toPath()) ?: return decodeDemoTone(track)
                        Decoded(mp3.samples, mp3.sampleRate, mp3.channels)
                    }
                    else -> decodeDemoTone(track)
                }
            }
            is TrackSource.Online -> {
                if (s.streamUrl.startsWith("demo://")) {
                    val num = s.streamUrl.substringAfterLast("/").toIntOrNull() ?: 1
                    val baseFreq = 220f + (num - 1) * 55f
                    decodeDemoTone(track, baseFreq)
                } else decodeDemoTone(track)
            }
        }
    }

    private fun decodeDemoTone(track: Track, baseFreq: Float = 440f): Decoded {
        val durMs = track.durationMs.coerceAtLeast(60_000)
        val totalFrames = (44100 * durMs / 1000).toInt()
        val out = ShortArray(totalFrames * 2)
        for (i in 0 until totalFrames) {
            val t = i.toFloat() / 44100f
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
        return Decoded(out, 44100, 2)
    }

    private fun readWav(path: Path): Triple<ShortArray, Int, Int>? {
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
                Triple(raw, sr, channels)
            }
        } catch (_: Exception) { null }
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
        lineFormat = null
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
