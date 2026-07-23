package com.sodamusic.player.audio.effects

/**
 * Simple delay line with feedback. Used for spatial/3D effects.
 * Supports stereo offset for Haas-effect widening.
 */
class DelayProcessor(private val sampleRate: Int) {
    private val bufL = FloatArray(sampleRate * 2)
    private val bufR = FloatArray(sampleRate * 2)
    private var posL = 0; private var posR = 0
    private var feedback = 0f
    private var wet = 0f
    private var delayL = 0; private var delayR = 0

    fun setDelay(leftMs: Float, rightMs: Float, feedbackAmt: Float, wetAmt: Float) {
        delayL = (leftMs * sampleRate / 1000f).toInt().coerceIn(0, bufL.size - 1)
        delayR = (rightMs * sampleRate / 1000f).toInt().coerceIn(0, bufR.size - 1)
        feedback = feedbackAmt.coerceIn(0f, 0.95f)
        wet = wetAmt.coerceIn(0f, 1f)
    }

    fun processStereo(left: Float, right: Float): Pair<Float, Float> {
        val idxL = (posL - delayL + bufL.size) % bufL.size
        val idxR = (posR - delayR + bufR.size) % bufR.size
        val dl = bufL[idxL]; val dr = bufR[idxR]
        bufL[posL] = left + dl * feedback
        bufR[posR] = right + dr * feedback
        posL = (posL + 1) % bufL.size
        posR = (posR + 1) % bufR.size
        val outL = left * (1f - wet) + dl * wet
        val outR = right * (1f - wet) + dr * wet
        return outL to outR
    }

    fun reset() { bufL.fill(0f); bufR.fill(0f); posL = 0; posR = 0 }
}
