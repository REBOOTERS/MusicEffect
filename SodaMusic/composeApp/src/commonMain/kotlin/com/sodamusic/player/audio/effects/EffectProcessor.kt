package com.sodamusic.player.audio.effects

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Chains all DSP blocks: EQ (bank of biquads) -> compressor ->
 * stereo widener -> delay -> reverb. configure() applies a preset.
 * Processes interleaved stereo float blocks [-1, 1].
 */
class EffectProcessor(private val sampleRate: Int) {

    // EQ bank: 5 parametric bands
    private val eqLowShelf = BiquadFilter(sampleRate)
    private val eqLowMid = BiquadFilter(sampleRate)
    private val eqMid = BiquadFilter(sampleRate)
    private val eqHighMid = BiquadFilter(sampleRate)
    private val eqHighShelf = BiquadFilter(sampleRate)

    private val compressor = Compressor(sampleRate)
    private val widener = StereoWidener()
    private val delay = DelayProcessor(sampleRate)
    private val reverb = ReverbProcessor(sampleRate)

    // Vintage effect: soft saturation + wow/flutter
    private var saturation = 0f
    private var wowAmount = 0f
    private var noiseAmount = 0f
    private var phase = 0f

    var currentEffect: AudioEffect = AudioEffect.NONE
        private set

    init { configure(AudioEffect.NONE) }

    fun configure(effect: AudioEffect) {
        currentEffect = effect
        // Reset all to neutral first
        eqLowShelf.setLowShelf(200f, 0f)
        eqLowMid.setPeaking(500f, 1f, 0f)
        eqMid.setPeaking(1500f, 1f, 0f)
        eqHighMid.setPeaking(4000f, 1f, 0f)
        eqHighShelf.setHighShelf(6000f, 0f)
        compressor.setParameters(0f, 1f, 10f, 100f, 0f)
        widener.setWidth(1f)
        delay.setDelay(0f, 0f, 0f, 0f)
        reverb.setMix(0f)
        saturation = 0f; wowAmount = 0f; noiseAmount = 0f

        when (effect) {
            AudioEffect.NONE -> { /* neutral */ }

            AudioEffect.SMART -> {
                // Gentle enhancement: slight bass lift, presence boost
                eqLowShelf.setLowShelf(150f, 3f)
                eqHighShelf.setHighShelf(8000f, 2f)
                eqMid.setPeaking(3000f, 1.5f, 2f)
                compressor.setParameters(-12f, 2f, 10f, 200f, 1f)
                widener.setWidth(1.2f)
            }

            AudioEffect.SURROUND_360 -> {
                widener.setWidth(3.2f)
                delay.setDelay(12f, 25f, 0.2f, 0.35f)
                reverb.setMix(0.25f)
                eqHighShelf.setHighShelf(8000f, 1.5f)
            }

            AudioEffect.SUPER_BASS -> {
                eqLowShelf.setLowShelf(80f, 12f)
                eqLowMid.setPeaking(200f, 0.8f, 4f)
                compressor.setParameters(-14f, 3f, 8f, 150f, 2f)
                widener.setWidth(1.1f)
            }

            AudioEffect.CLEAR_VOCALS -> {
                eqLowShelf.setLowShelf(250f, -4f)
                eqMid.setPeaking(2500f, 1.2f, 5f)
                eqHighMid.setPeaking(5000f, 0.8f, 3f)
                eqHighShelf.setHighShelf(8000f, 2f)
                compressor.setParameters(-10f, 2f, 5f, 100f, 1f)
                widener.setWidth(0.85f)
            }

            AudioEffect.AUDIO_3D -> {
                widener.setWidth(2.0f)
                delay.setDelay(7f, 18f, 0.15f, 0.3f)
                reverb.setMix(0.15f)
                eqLowShelf.setLowShelf(200f, -2f)
            }

            AudioEffect.HIFI_LIVE -> {
                reverb.setMix(0.35f)
                widener.setWidth(1.4f)
                eqHighShelf.setHighShelf(7000f, 3f)
                eqLowShelf.setLowShelf(120f, 2f)
                compressor.setParameters(-8f, 2.5f, 15f, 300f, 2f)
            }

            AudioEffect.DYNAMIC_EDM -> {
                eqLowShelf.setLowShelf(100f, 8f)
                eqHighShelf.setHighShelf(9000f, 6f)
                eqLowMid.setPeaking(300f, 2f, -3f)
                compressor.setParameters(-10f, 6f, 3f, 50f, 4f)
                widener.setWidth(1.6f)
            }

            AudioEffect.ROCK -> {
                eqLowShelf.setLowShelf(100f, 5f)
                eqHighMid.setPeaking(3000f, 1f, 3f)
                eqHighShelf.setHighShelf(6000f, 4f)
                eqLowMid.setPeaking(400f, 1.5f, -3f)
                compressor.setParameters(-8f, 5f, 5f, 80f, 3f)
                saturation = 0.15f
                widener.setWidth(1.3f)
            }

            AudioEffect.VINTAGE -> {
                eqLowShelf.setLowShelf(100f, -8f)
                eqHighShelf.setHighShelf(5000f, -6f)
                eqMid.setPeaking(1500f, 0.7f, 2f)
                saturation = 0.25f
                wowAmount = 0.003f
                noiseAmount = 0.0008f
                reverb.setMix(0.08f)
            }
        }
    }

    /**
     * Process interleaved stereo buffer [L,R,L,R,...] in place.
     * Frame count = buffer.size / 2.
     */
    fun processInterleaved(buffer: FloatArray, offset: Int, frameCount: Int) {
        // Deinterleave to temporary L/R
        val left = FloatArray(frameCount)
        val right = FloatArray(frameCount)
        for (i in 0 until frameCount) {
            left[i] = buffer[offset + i * 2]
            right[i] = buffer[offset + i * 2 + 1]
        }

        // EQ chain (per-channel)
        applyEq(left)
        applyEq(right)

        // Compression (per-channel)
        for (i in 0 until frameCount) {
            left[i] = compressor.processSample(left[i])
            right[i] = compressor.processSample(right[i])
        }
        compressor.reset()

        // Saturation
        if (saturation > 0f) {
            for (i in 0 until frameCount) {
                left[i] = softClip(left[i], saturation)
                right[i] = softClip(right[i], saturation)
            }
        }

        // Vintage wow/flutter + noise
        if (wowAmount > 0f || noiseAmount > 0f) {
            for (i in 0 until frameCount) {
                phase += 2f * PI.toFloat() * 0.5f / sampleRate
                val wow = sin(phase) * wowAmount
                left[i] = left[i] * (1f + wow) + (Random.nextFloat() - 0.5f) * noiseAmount
                right[i] = right[i] * (1f + wow) + (Random.nextFloat() - 0.5f) * noiseAmount
            }
        }

        // Stereo widener
        for (i in 0 until frameCount) {
            val (l, r) = widener.process(left[i], right[i])
            left[i] = l; right[i] = r
        }

        // Delay + Reverb
        val tmpL = FloatArray(frameCount)
        val tmpR = FloatArray(frameCount)
        for (i in 0 until frameCount) {
            val (l, r) = delay.processStereo(left[i], right[i])
            tmpL[i] = l; tmpR[i] = r
        }
        for (i in 0 until frameCount) {
            left[i] = reverb.processSample(tmpL[i])
            right[i] = reverb.processSample(tmpR[i])
        }

        // Re-interleave
        for (i in 0 until frameCount) {
            buffer[offset + i * 2] = left[i].coerceIn(-1f, 1f)
            buffer[offset + i * 2 + 1] = right[i].coerceIn(-1f, 1f)
        }
    }

    private fun applyEq(channel: FloatArray) {
        eqLowShelf.processBlock(channel, channel, 0, channel.size)
        eqLowMid.processBlock(channel, channel, 0, channel.size)
        eqMid.processBlock(channel, channel, 0, channel.size)
        eqHighMid.processBlock(channel, channel, 0, channel.size)
        eqHighShelf.processBlock(channel, channel, 0, channel.size)
    }

    private fun softClip(x: Float, drive: Float): Float {
        val d = 1f + drive * 4f
        val y = x * d
        // tanh-like soft clipper
        return if (y > 1f) 1f - 1f / (y * y + 1f)
        else if (y < -1f) -1f + 1f / (y * y + 1f)
        else y - y * y * y / 3f
    }

    fun reset() {
        eqLowShelf.reset(); eqLowMid.reset(); eqMid.reset(); eqHighMid.reset(); eqHighShelf.reset()
        compressor.reset(); widener.setWidth(1f); delay.reset(); reverb.reset()
    }
}

