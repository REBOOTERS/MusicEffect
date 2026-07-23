package com.sodamusic.player.audio.effects

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Biquad IIR filter supporting low-pass, high-pass, band-pass, peaking EQ,
 * low-shelf and high-shelf types. Coefficients computed via the bilinear transform.
 */
class BiquadFilter(private val sampleRate: Int) {

    enum class Type {
        LOW_PASS, HIGH_PASS, BAND_PASS, PEAKING, LOW_SHELF, HIGH_SHELF
    }

    private var b0 = 1f; private var b1 = 0f; private var b2 = 0f
    private var a1 = 0f; private var a2 = 0f
    private var x1 = 0f; private var x2 = 0f
    private var y1 = 0f; private var y2 = 0f

    fun setPeaking(freqHz: Float, q: Float, gainDb: Float) {
        val a = sqrt(Math.pow(10.0, gainDb / 40.0)).toFloat()
        val w0 = 2f * PI.toFloat() * freqHz / sampleRate
        val alpha = sin(w0) / (2f * q)
        val cosW = cos(w0)
        val nb0 = 1f + alpha * a; val nb1 = -2f * cosW; val nb2 = 1f - alpha * a
        val a0 = 1f + alpha / a; val na1 = -2f * cosW; val na2 = 1f - alpha / a
        apply(nb0, nb1, nb2, na1, na2, a0)
    }

    fun setLowShelf(freqHz: Float, gainDb: Float) {
        val a = sqrt(Math.pow(10.0, gainDb / 40.0)).toFloat()
        val w0 = 2f * PI.toFloat() * freqHz / sampleRate
        val s = 1f
        val alpha = sin(w0) / 2f * sqrt((a + 1f / a) * (1f / s - 1f) + 2f)
        val cosW = cos(w0)
        val ap1 = a + 1f; val am1 = a - 1f
        val nb0 = a * (ap1 - am1 * cosW + 2f * sqrt(a) * alpha)
        val nb1 = 2f * a * (am1 - ap1 * cosW)
        val nb2 = a * (ap1 - am1 * cosW - 2f * sqrt(a) * alpha)
        val a0 = ap1 + am1 * cosW + 2f * sqrt(a) * alpha
        val na1 = -2f * (am1 + ap1 * cosW)
        val na2 = ap1 + am1 * cosW - 2f * sqrt(a) * alpha
        apply(nb0, nb1, nb2, na1, na2, a0)
    }

    fun setHighShelf(freqHz: Float, gainDb: Float) {
        val a = sqrt(Math.pow(10.0, gainDb / 40.0)).toFloat()
        val w0 = 2f * PI.toFloat() * freqHz / sampleRate
        val s = 1f
        val alpha = sin(w0) / 2f * sqrt((a + 1f / a) * (1f / s - 1f) + 2f)
        val cosW = cos(w0)
        val ap1 = a + 1f; val am1 = a - 1f
        val nb0 = a * (ap1 + am1 * cosW + 2f * sqrt(a) * alpha)
        val nb1 = -2f * a * (am1 + ap1 * cosW)
        val nb2 = a * (ap1 + am1 * cosW - 2f * sqrt(a) * alpha)
        val a0 = ap1 - am1 * cosW + 2f * sqrt(a) * alpha
        val na1 = 2f * (am1 - ap1 * cosW)
        val na2 = ap1 - am1 * cosW - 2f * sqrt(a) * alpha
        apply(nb0, nb1, nb2, na1, na2, a0)
    }

    fun setBandPass(freqHz: Float, q: Float) {
        val w0 = 2f * PI.toFloat() * freqHz / sampleRate
        val alpha = sin(w0) / (2f * q)
        val cosW = cos(w0)
        val nb0 = alpha; val nb1 = 0f; val nb2 = -alpha
        val a0 = 1f + alpha; val na1 = -2f * cosW; val na2 = 1f - alpha
        apply(nb0, nb1, nb2, na1, na2, a0)
    }

    fun setLowPass(freqHz: Float, q: Float) {
        val w0 = 2f * PI.toFloat() * freqHz / sampleRate
        val alpha = sin(w0) / (2f * q)
        val cosW = cos(w0)
        val nb0 = (1f - cosW) / 2f; val nb1 = 1f - cosW; val nb2 = (1f - cosW) / 2f
        val a0 = 1f + alpha; val na1 = -2f * cosW; val na2 = 1f - alpha
        apply(nb0, nb1, nb2, na1, na2, a0)
    }

    fun setHighPass(freqHz: Float, q: Float) {
        val w0 = 2f * PI.toFloat() * freqHz / sampleRate
        val alpha = sin(w0) / (2f * q)
        val cosW = cos(w0)
        val nb0 = (1f + cosW) / 2f; val nb1 = -(1f + cosW); val nb2 = (1f + cosW) / 2f
        val a0 = 1f + alpha; val na1 = -2f * cosW; val na2 = 1f - alpha
        apply(nb0, nb1, nb2, na1, na2, a0)
    }

    private fun apply(nb0: Float, nb1: Float, nb2: Float, na1: Float, na2: Float, a0: Float) {
        b0 = nb0 / a0; b1 = nb1 / a0; b2 = nb2 / a0
        a1 = na1 / a0; a2 = na2 / a0
    }

    fun processSample(x: Float): Float {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = x
        y2 = y1; y1 = y
        return y
    }

    fun processBlock(input: FloatArray, output: FloatArray, offset: Int, count: Int) {
        for (i in offset until min(offset + count, input.size)) {
            val x = input[i]
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x
            y2 = y1; y1 = y
            output[i] = y
        }
    }

    fun reset() { x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f }
}
