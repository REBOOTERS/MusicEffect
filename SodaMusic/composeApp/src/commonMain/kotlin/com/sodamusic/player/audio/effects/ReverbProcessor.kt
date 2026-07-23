package com.sodamusic.player.audio.effects

import kotlin.math.sin

class ReverbProcessor(sampleRate: Int) {

    private class Comb(delaySamples: Int, private val feedback: Float) {
        private val buf = FloatArray(delaySamples)
        private var pos = 0
        fun process(x: Float): Float {
            val y = buf[pos]
            buf[pos] = x + y * feedback
            pos = (pos + 1) % buf.size
            return y
        }
        fun reset() { buf.fill(0f); pos = 0 }
    }

    private class AllPass(delaySamples: Int, private val gain: Float) {
        private val buf = FloatArray(delaySamples)
        private var pos = 0
        fun process(x: Float): Float {
            val delayed = buf[pos]
            val y = delayed - gain * x
            buf[pos] = x + gain * delayed
            pos = (pos + 1) % buf.size
            return y
        }
        fun reset() { buf.fill(0f); pos = 0 }
    }

    private val combs: Array<Comb>
    private val allpasses: Array<AllPass>
    private var wet = 0.3f
    private var dry = 0.7f

    init {
        val scale = sampleRate.toFloat() / 44100f
        combs = arrayOf(
            Comb((1557 * scale).toInt(), 0.805f),
            Comb((1617 * scale).toInt(), 0.827f),
            Comb((1491 * scale).toInt(), 0.783f),
            Comb((1422 * scale).toInt(), 0.764f)
        )
        allpasses = arrayOf(
            AllPass((525 * scale).toInt(), 0.5f),
            AllPass((556 * scale).toInt(), 0.5f)
        )
    }

    fun setMix(wetAmount: Float) {
        wet = wetAmount.coerceIn(0f, 1f)
        dry = 1f - wet
    }

    fun processSample(x: Float): Float {
        var reverb = 0f
        for (c in combs) reverb += c.process(x)
        reverb *= 0.25f
        for (ap in allpasses) reverb = ap.process(reverb)
        return x * dry + reverb * wet
    }

    fun reset() { combs.forEach { it.reset() }; allpasses.forEach { it.reset() } }
}
