package com.sodamusic.player

import com.sodamusic.player.audio.NativeAudioPlayer
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
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
import java.io.IOException
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

class AndroidAudioPlayer : NativeAudioPlayer {

    override val sampleRate: Int = 44100

    private var audioTrack: AudioTrack? = null
    private var extractor: MediaExtractor? = null
    private var decoder: MediaCodec? = null
    private var playJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _playState = MutableStateFlow(PlayState.IDLE)
    override val playState: StateFlow<PlayState> = _playState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    @Volatile
    private var paused = false
    private var trackDurationUs: Long = 0L

    override val positionMs: Long get() = _currentPosition.value
    override val durationMs: Long get() = trackDurationUs / 1000

    override fun play(track: Track, effectProcessor: EffectProcessor) {
        stop()
        _playState.value = PlayState.BUFFERING
        _currentPosition.value = 0

        val isDemo = (track.source as? TrackSource.Online)?.streamUrl?.startsWith("demo://") == true

        if (isDemo) {
            trackDurationUs = track.durationMs * 1000
            val num = (track.source as TrackSource.Online).streamUrl
                .substringAfterLast("/").toIntOrNull() ?: 1
            val baseFreq = 220f + (num - 1) * 55f
            playDemoTone(effectProcessor, baseFreq, track.durationMs)
            return
        }

        playDecoded(track, effectProcessor)
    }

    private fun playDemoTone(effectProcessor: EffectProcessor, baseFreq: Float, durationMs: Long) {
        val channelMask = AudioFormat.CHANNEL_OUT_STEREO
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(8192)

        val trackOut = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(channelMask)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = trackOut
        trackOut.play()
        _playState.value = PlayState.PLAYING

        playJob = scope.launch {
            try {
                val totalFrames = sampleRate * durationMs / 1000
                val chunkFrames = 2048
                val floatBuf = FloatArray(chunkFrames * 2)
                var processedFrames = 0L
                val startNs = System.nanoTime()

                while (isActive && processedFrames < totalFrames && _playState.value != PlayState.STOPPED) {
                    if (paused) { kotlinx.coroutines.delay(20); continue }

                    val frames = min(chunkFrames.toLong(), totalFrames - processedFrames).toInt()
                    for (i in 0 until frames) {
                        val t = (processedFrames + i).toFloat() / sampleRate
                        val env = (sin(PI.toFloat() * t / (durationMs / 1000f)) * 0.5f + 0.5f)
                        val s = (
                            sin(2f * PI.toFloat() * baseFreq * t) * 0.25f +
                            sin(2f * PI.toFloat() * (baseFreq * 1.25f) * t) * 0.20f +
                            sin(2f * PI.toFloat() * (baseFreq * 1.5f) * t) * 0.20f +
                            sin(2f * PI.toFloat() * (baseFreq * 2f) * t) * 0.15f
                        ) * env * 0.7f
                        floatBuf[i * 2] = s
                        floatBuf[i * 2 + 1] = s
                    }
                    effectProcessor.processInterleaved(floatBuf, 0, frames)
                    val out = ShortArray(frames * 2)
                    for (i in out.indices) {
                        out[i] = (floatBuf[i] * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                    }
                    trackOut.write(out, 0, out.size)
                    processedFrames += frames
                    _currentPosition.value = processedFrames * 1000L / sampleRate

                    val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
                    val audioMs = _currentPosition.value
                    val waitMs = audioMs - elapsedMs
                    if (waitMs > 5) kotlinx.coroutines.delay(waitMs)
                }
                trackOut.stop()
                _playState.value = PlayState.STOPPED
            } catch (e: Exception) {
                e.printStackTrace()
                _playState.value = PlayState.ERROR
            }
        }
    }

    private fun playDecoded(track: Track, effectProcessor: EffectProcessor) {
        playJob = scope.launch {
            try {
                setupDecoder(track)
                val ext = extractor ?: run { _playState.value = PlayState.ERROR; return@launch }

                val format = ext.getTrackFormat(ext.sampleTrackIndex)
                val sr = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                if (format.containsKey(MediaFormat.KEY_DURATION)) {
                    trackDurationUs = format.getLong(MediaFormat.KEY_DURATION)
                }

                val channelMask = AudioFormat.CHANNEL_OUT_STEREO
                val minBuf = AudioTrack.getMinBufferSize(
                    sr, channelMask, AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(8192)

                val trackOut = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sr)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(channelMask)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = trackOut
                trackOut.play()
                _playState.value = PlayState.PLAYING

                val codec = decoder ?: return@launch
                val bufferInfo = MediaCodec.BufferInfo()

                while (isActive && _playState.value != PlayState.STOPPED) {
                    if (paused) { kotlinx.coroutines.delay(30); continue }

                    val inputIdx = codec.dequeueInputBuffer(10_000)
                    if (inputIdx >= 0) {
                        val inputBuf = codec.getInputBuffer(inputIdx)
                        if (inputBuf != null) {
                            inputBuf.clear()
                            val sampleSize = ext.readSampleData(inputBuf, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inputIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            } else {
                                codec.queueInputBuffer(inputIdx, 0, sampleSize, ext.sampleTime, 0)
                                ext.advance()
                            }
                        }
                    }

                    val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                    when {
                        outIdx >= 0 -> {
                            val outBuf = codec.getOutputBuffer(outIdx)
                            if (outBuf != null) {
                                outBuf.position(bufferInfo.offset)
                                outBuf.limit(bufferInfo.offset + bufferInfo.size)
                                val pcm = ShortArray(bufferInfo.size / 2)
                                outBuf.asShortBuffer().get(pcm)

                                val inChannels = channelCount.coerceAtLeast(1)
                                val inFrames = pcm.size / inChannels
                                val floatBuf = FloatArray(inFrames * 2)
                                for (f in 0 until inFrames) {
                                    val l = if (inChannels >= 1) pcm[f * inChannels] / 32768f else 0f
                                    val r = if (inChannels >= 2) pcm[f * inChannels + 1] / 32768f else l
                                    floatBuf[f * 2] = l
                                    floatBuf[f * 2 + 1] = r
                                }

                                if (sr == sampleRate) {
                                    effectProcessor.processInterleaved(floatBuf, 0, inFrames)
                                }

                                val out = ShortArray(inFrames * 2)
                                for (i in out.indices) {
                                    out[i] = (floatBuf[i] * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                                }
                                trackOut.write(out, 0, out.size)
                                _currentPosition.value = bufferInfo.presentationTimeUs / 1000
                            }
                            codec.releaseOutputBuffer(outIdx, false)

                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                _playState.value = PlayState.STOPPED
                                break
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                _playState.value = PlayState.ERROR
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                _playState.value = PlayState.ERROR
            }
        }
    }

    private val MediaExtractor.sampleTrackIndex: Int
        get() {
            for (i in 0 until trackCount) {
                val fmt = getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) return i
            }
            return 0
        }

    private fun setupDecoder(track: Track) {
        val ext = MediaExtractor()
        when (val src = track.source) {
            is TrackSource.Local -> ext.setDataSource(src.filePath)
            is TrackSource.Online -> ext.setDataSource(src.streamUrl, src.headers)
        }
        val trackIdx = ext.sampleTrackIndex
        ext.selectTrack(trackIdx)
        extractor = ext
        val format = ext.getTrackFormat(trackIdx)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return
        decoder = MediaCodec.createDecoderByType(mime).apply {
            configure(format, null, null, 0)
            start()
        }
    }

    override fun pause() {
        paused = true
        _playState.value = PlayState.PAUSED
        audioTrack?.pause()
    }

    override fun resume() {
        paused = false
        _playState.value = PlayState.PLAYING
        audioTrack?.play()
    }

    override fun stop() {
        paused = false
        _playState.value = PlayState.STOPPED
        playJob?.cancel()
        playJob = null
        try { decoder?.stop(); decoder?.release() } catch (_: Exception) {}
        try { extractor?.release() } catch (_: Exception) {}
        try { audioTrack?.stop(); audioTrack?.release() } catch (_: Exception) {}
        decoder = null; extractor = null; audioTrack = null
        trackDurationUs = 0
    }

    override fun seekTo(positionMs: Long) {
        extractor?.seekTo(positionMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        _currentPosition.value = positionMs
    }

    override fun setVolume(volume: Float) { audioTrack?.setVolume(volume) }

    override fun release() {
        stop()
        scope.coroutineContext[Job]?.cancel()
    }
}



