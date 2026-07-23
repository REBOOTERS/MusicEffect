package com.sodamusic.player

import com.sodamusic.player.audio.NativeAudioPlayer
import com.sodamusic.player.audio.effects.EffectProcessor
import com.sodamusic.player.model.PlayState
import com.sodamusic.player.model.Track
import com.sodamusic.player.model.TrackSource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVFAudio.*
import platform.Foundation.*
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalForeignApi::class)
class IOSAudioPlayer : NativeAudioPlayer {

    override val sampleRate: Int = 44100

    private var engine: AVAudioEngine? = null
    private var playerNode: AVAudioPlayerNode? = null
    private var playJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _playState = MutableStateFlow(PlayState.IDLE)
    override val playState: StateFlow<PlayState> = _playState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private var totalDuration: Long = 0

    override val positionMs: Long get() = _currentPosition.value
    override val durationMs: Long get() = totalDuration

    override fun play(track: Track, effectProcessor: EffectProcessor) {
        stop()
        _playState.value = PlayState.BUFFERING
        totalDuration = track.durationMs

        val isDemo = (track.source as? TrackSource.Online)?.streamUrl?.startsWith("demo://") == true
        val pcm: FloatArray = if (isDemo) {
            val num = (track.source as TrackSource.Online).streamUrl
                .substringAfterLast("/").toIntOrNull() ?: 1
            val baseFreq = 220f + (num - 1) * 55f
            generateDemoFloats(track.durationMs.coerceAtLeast(60_000), baseFreq, effectProcessor)
        } else {
            loadPcmFloats(track, effectProcessor)
        }

        val eng = AVAudioEngine()
        val node = AVAudioPlayerNode()
        eng.attachNode(node)
        val format = AVAudioFormat(
            commonFormat = AVAudioPCMFormatFloat32,
            sampleRate = sampleRate.toDouble(),
            channels = 2u,
            interleaved = true
        )!!
        eng.connect(node, to = eng.mainMixerNode, format = format)
        if (!eng.startAndReturnError(null)) {
            _playState.value = PlayState.ERROR
            return
        }
        engine = eng
        playerNode = node

        val frameCount = pcm.size / 2
        val buffer = AVAudioPCMBuffer(pcmFormat = format, frameCapacity = frameCount.toUInt())!!
        buffer.frameLength = frameCount.toUInt()
        val dst = buffer.floatChannelData!![0]!!
        for (i in 0 until (frameCount * 2)) dst[i] = pcm[i]

        node.scheduleBuffer(buffer, atTime = null, options = 0u) {
            if (_playState.value == PlayState.PLAYING) {
                _playState.value = PlayState.STOPPED
            }
        }
        node.play()
        _playState.value = PlayState.PLAYING

        playJob = scope.launch {
            val startTime = NSDate.date().timeIntervalSince1970
            while (isActive && _playState.value == PlayState.PLAYING) {
                val nodeTime = node.lastRenderTime
                if (nodeTime != null) {
                    val playerTime = node.playerTimeForNodeTime(nodeTime)
                    if (playerTime != null) {
                        _currentPosition.value = playerTime.sampleTime.toLong() * 1000L / sampleRate
                    }
                } else {
                    _currentPosition.value = ((NSDate.date().timeIntervalSince1970 - startTime) * 1000).toLong()
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }

    private fun loadPcmFloats(track: Track, effectProcessor: EffectProcessor): FloatArray {
        val url = when (val s = track.source) {
            is TrackSource.Local -> NSURL.fileURLWithPath(s.filePath)
            is TrackSource.Online -> NSURL.URLWithString(s.streamUrl)
        } ?: return generateDemoFloats(60_000, 440f, effectProcessor)

        val audioFile = try { AVAudioFile(forReading = url, error = null) } catch (_: Exception) { null }
            ?: return generateDemoFloats(60_000, 440f, effectProcessor)

        totalDuration = (audioFile.length.toLong() * 1000.0 / audioFile.processingFormat.sampleRate).toLong()
        val srcFormat = audioFile.processingFormat
        val frameCount = audioFile.length.toInt()
        val buffer = AVAudioPCMBuffer(pcmFormat = srcFormat, frameCapacity = audioFile.length)!!
        audioFile.readIntoBuffer(buffer, error = null)

        val interleaved = FloatArray(frameCount * 2)
        val channels = srcFormat.channelCount.toInt()
        val isInterleaved = srcFormat.isInterleaved

        if (srcFormat.commonFormat == AVAudioPCMFormatFloat32) {
            val chData = buffer.floatChannelData!!
            if (isInterleaved && channels == 2) {
                val data = chData[0]!!
                for (i in 0 until frameCount * 2) interleaved[i] = data[i]
            } else if (!isInterleaved) {
                val l = chData[0]!!
                val r = if (channels >= 2) chData[1]!! else l
                for (i in 0 until frameCount) {
                    interleaved[i * 2] = l[i]; interleaved[i * 2 + 1] = r[i]
                }
            }
        } else if (srcFormat.commonFormat == AVAudioPCMFormatInt16) {
            val chData = buffer.int16ChannelData!!
            if (isInterleaved) {
                val data = chData[0]!!
                for (i in 0 until frameCount * 2) interleaved[i] = data[i] / 32768f
            } else {
                val l = chData[0]!!
                val r = if (channels >= 2) chData[1]!! else l
                for (i in 0 until frameCount) {
                    interleaved[i * 2] = l[i] / 32768f; interleaved[i * 2 + 1] = r[i] / 32768f
                }
            }
        } else {
            return generateDemoFloats(60_000, 440f, effectProcessor)
        }
        effectProcessor.processInterleaved(interleaved, 0, frameCount)
        return interleaved
    }

    private fun generateDemoFloats(durationMs: Long, baseFreq: Float, effectProcessor: EffectProcessor): FloatArray {
        val dur = durationMs.coerceAtLeast(60_000)
        val frames = sampleRate * dur / 1000
        val out = FloatArray(frames.toInt() * 2)
        for (i in 0 until frames.toInt()) {
            val t = i.toFloat() / sampleRate
            val env = (sin(PI.toFloat() * t / (dur / 1000f)) * 0.5f + 0.5f)
            val s = (
                sin(2f * PI.toFloat() * baseFreq * t) * 0.25f +
                sin(2f * PI.toFloat() * (baseFreq * 1.25f) * t) * 0.20f +
                sin(2f * PI.toFloat() * (baseFreq * 1.5f) * t) * 0.20f +
                sin(2f * PI.toFloat() * (baseFreq * 2f) * t) * 0.15f
            ) * env * 0.7f
            out[i * 2] = s; out[i * 2 + 1] = s
        }
        effectProcessor.processInterleaved(out, 0, frames.toInt())
        totalDuration = dur
        return out
    }

    override fun pause() { playerNode?.pause(); _playState.value = PlayState.PAUSED }
    override fun resume() { playerNode?.play(); _playState.value = PlayState.PLAYING }
    override fun stop() {
        _playState.value = PlayState.STOPPED
        playJob?.cancel(); playJob = null
        try { playerNode?.stop() } catch (_: Exception) {}
        try { engine?.stop() } catch (_: Exception) {}
        engine = null; playerNode = null; totalDuration = 0
    }
    override fun seekTo(positionMs: Long) { _currentPosition.value = positionMs }
    override fun setVolume(volume: Float) { engine?.mainMixerNode?.volume = volume }
    override fun release() { stop(); scope.coroutineContext[Job]?.cancel() }
}



