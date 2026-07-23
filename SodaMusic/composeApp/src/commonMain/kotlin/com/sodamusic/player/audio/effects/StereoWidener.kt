package com.sodamusic.player.audio.effects

import kotlin.math.max
import kotlin.math.min

/**
 * Mid-side stereo widener. Converts L/R to M/S, applies gain
 * to the side signal, converts back. +width widens, negative narrows.
 */
class StereoWidener {
    private var sideGain = 1f // 1.0 = unchanged, >1 wider, <1 narrower

    fun setWidth(width: Float) {
        sideGain = width.coerceIn(0f, 4f)
    }

    fun process(left: Float, right: Float): Pair<Float, Float> {
        val mid = (left + right) * 0.5f
        val side = (left - right) * 0.5f
        val s = side * sideGain
        return (mid + s) to (mid - s)
    }
}
