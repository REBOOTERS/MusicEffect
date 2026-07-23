package com.sodamusic.player.audio.effects

import kotlin.math.ceil
import kotlin.math.floor

/**
 * Zero-alloc linear interpolation resampler for interleaved-stereo 16-bit PCM.
 *
 * Linear interpolation is sufficient for both up- and down-sampling at the small
 * ratio differences we encounter (44.1k <-> 48k is ~1.09x). For much larger down
 * ratios a proper anti-alias LPF would be required, but that's not a target case here.
 */
object Resampler {

    /**
     * Resample [input] (interleaved stereo shorts) from [inputSr] to [outputSr].
     * Returns a new ShortArray at the output sample rate.
     */
    fun resampleStereo(input: ShortArray, inputSr: Int, outputSr: Int): ShortArray {
        if (inputSr == outputSr) return input.copyOf()
        if (input.isEmpty()) return ShortArray(0)

        val channels = 2
        val inFrames = input.size / channels
        val ratio = inputSr.toDouble() / outputSr // > 1 for downsample, < 1 for upsample
        val outFrames = ceil(inFrames / ratio).toInt()
        val out = ShortArray(outFrames * channels)

        for (i in 0 until outFrames) {
            val srcFrame = i * ratio
            val f0 = floor(srcFrame).toInt()
            val frac = (srcFrame - f0).toFloat()
            val f1 = minOf(f0 + 1, inFrames - 1)
            val i0 = f0 * channels
            val i1 = f1 * channels
            val o = i * channels
            for (c in 0 until channels) {
                val s0 = input[i0 + c].toInt()
                val s1 = input[i1 + c].toInt()
                val v = s0 + (s1 - s0) * frac
                out[o + c] = v.toInt().coerceIn(-32768, 32767).toShort()
            }
        }
        return out
    }

    /**
     * Convert [samples] (interleaved stereo floats in [-1, 1]) at [inputSr] to interleaved
     * stereo s16 PCM at [outputSr], with linear interpolation resampling.
     */
    fun resampleFloatToS16(samples: FloatArray, inputSr: Int, outputSr: Int): ShortArray {
        if (inputSr == outputSr) {
            val out = ShortArray(samples.size)
            for (i in samples.indices) {
                out[i] = (samples[i] * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            }
            return out
        }
        val channels = 2
        val inFrames = samples.size / channels
        val ratio = inputSr.toDouble() / outputSr
        val outFrames = ceil(inFrames / ratio).toInt()
        val out = ShortArray(outFrames * channels)
        for (i in 0 until outFrames) {
            val srcFrame = i * ratio
            val f0 = floor(srcFrame).toInt()
            val frac = (srcFrame - f0).toFloat()
            val f1 = minOf(f0 + 1, inFrames - 1)
            val i0 = f0 * channels
            val i1 = f1 * channels
            val o = i * channels
            for (c in 0 until channels) {
                val s0 = samples[i0 + c]
                val s1 = samples[i1 + c]
                val v = (s0 + (s1 - s0) * frac) * 32767f
                out[o + c] = v.toInt().coerceIn(-32768, 32767).toShort()
            }
        }
        return out
    }
}
