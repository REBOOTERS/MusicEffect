package com.sodamusic.player.audio.effects

import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin

/**
 * Minimal radix-2 Cooley-Tukey FFT for power-of-two real input.
 * Used to drive the on-screen spectrum visualizer from decoded PCM.
 * Window size must be a power of two (256 is a good default).
 */
object FFT {
    private const val MIN_BANDS = 32

    /**
     * @param mono mono samples in [-1, 1], length must be a power of two.
     * @param bands number of output frequency bands (log-spaced magnitude).
     * @return normalized [0,1] per-band magnitudes of length [bands].
     */
    fun computeBands(mono: FloatArray, bands: Int = MIN_BANDS): FloatArray {
        val n = mono.size
        if (n < 2) return FloatArray(bands)
        val re = FloatArray(n)
        val im = FloatArray(n)
        // Hann window to reduce spectral leakage.
        for (i in 0 until n) {
            val w = 0.5f - 0.5f * cos(2f * Math.PI.toFloat() * i / (n - 1))
            re[i] = mono[i] * w
        }
        fft(re, im)

        val half = n / 2
        val out = FloatArray(bands)
        // Log-spaced bin mapping.
        val logHalf = ln(half.toFloat())
        var prevBin = 0
        var maxVal = 1e-9f
        val mags = FloatArray(half)
        for (k in 1 until half) {
            val mag = Math.sqrt((re[k] * re[k] + im[k] * im[k]).toDouble()).toFloat()
            mags[k] = mag
            if (mag > maxVal) maxVal = mag
        }
        for (b in 0 until bands) {
            val lo = ln((prevBin + 1).toFloat()).coerceAtLeast(0f)
            val hi = (logHalf * (b + 1) / bands).coerceIn(lo, logHalf)
            val binHi = Math.exp(hi.toDouble()).toInt().coerceIn(prevBin + 1, half)
            var sum = 0f
            var count = 0
            for (k in (prevBin + 1)..binHi) {
                if (k in 1 until half) { sum += mags[k]; count++ }
            }
            val avg = if (count > 0) sum / count else 0f
            // Normalize + perceptual curve.
            out[b] = ((avg / maxVal).toDouble().pow(0.6)).toFloat().coerceIn(0f, 1f)
            prevBin = binHi
        }
        return out
    }

    /** In-place radix-2 FFT on [re] / [im], length must be a power of two. */
    private fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        // Bit reversal permutation.
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j or bit
            if (i < j) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }
        // Butterfly.
        var len = 2
        while (len <= n) {
            val ang = -2.0 * Math.PI / len
            val wre = cos(ang).toFloat()
            val wim = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var wRe = 1f; var wIm = 0f
                val half = len / 2
                for (k in 0 until half) {
                    val aRe = re[i + k]; val aIm = im[i + k]
                    val bRe = re[i + k + half] * wRe - im[i + k + half] * wIm
                    val bIm = re[i + k + half] * wIm + im[i + k + half] * wRe
                    re[i + k] = aRe + bRe; im[i + k] = aIm + bIm
                    re[i + k + half] = aRe - bRe; im[i + k + half] = aIm - bIm
                    val nextRe = wRe * wre - wIm * wim
                    wIm = wRe * wim + wIm * wre
                    wRe = nextRe
                }
                i += len
            }
            len *= 2
        }
    }

    private fun Double.pow(e: Double) = Math.pow(this, e)
}
