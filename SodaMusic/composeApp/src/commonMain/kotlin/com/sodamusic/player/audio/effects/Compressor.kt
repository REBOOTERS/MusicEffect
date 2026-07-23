package com.sodamusic.player.audio.effects

import kotlin.math.abs

/**
 * Simple feed-forward compressor/limiter. Applies gain reduction
 * when signal exceeds threshold. Uses smooth envelope tracking.
 */
class Compressor(private val sampleRate: Int) {
    private var threshold = -6f   // dB
    private var ratio = 4f
    private var attackMs = 5f
    private var releaseMs = 100f
    private var makeupGain = 1f
    private var envelope = 0f

    fun setParameters(thresholdDb: Float, ratioN: Float, attack: Float, release: Float, makeupDb: Float) {
        threshold = thresholdDb
        ratio = ratioN.coerceIn(1f, 20f)
        attackMs = attack
        releaseMs = release
        makeupGain = dbToLinear(makeupDb)
    }

    fun processSample(x: Float): Float {
        val level = abs(x)
        val levelDb = linearToDb(level)
        val overDb = levelDb - threshold
        val targetReduction = if (overDb > 0f) overDb * (1f - 1f / ratio) else 0f

        val coeff = if (targetReduction > envelope) {
            timeCoeff(attackMs)
        } else {
            timeCoeff(releaseMs)
        }
        envelope = envelope + coeff * (targetReduction - envelope)
        val gain = dbToLinear(-envelope) * makeupGain
        return x * gain
    }

    private fun timeCoeff(ms: Float): Float {
        val samples = ms * sampleRate / 1000f
        return (1f / samples).coerceIn(0f, 1f)
    }

    private fun dbToLinear(db: Float) = Math.pow(10.0, db / 20.0).toFloat()
    private fun linearToDb(lin: Float): Float {
        val safe = lin.coerceAtLeast(1e-10f)
        return (20.0 * kotlin.math.ln(safe.toDouble()) / kotlin.math.ln(10.0)).toFloat()
    }

    fun reset() { envelope = 0f }
}
