package com.sodamusic.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import com.sodamusic.player.audio.effects.AudioEffect
import kotlin.math.abs

/**
 * Drives the platform AudioFx chain (Equalizer + BassBoost + Virtualizer + PresetReverb)
 * attached to a MediaPlayer audio session. Each [AudioEffect] preset maps to a combination
 * of these native effects so switching produces an audible change on Android.
 *
 * Band levels are in millibels (mB). Typical device EQ has 5 bands (~60/230/910/3600/14000 Hz).
 * We boost/cut by mapping each preset to target gains on low / low-mid / mid / high-mid / high.
 */
class SodaAudioFx(private val audioSessionId: Int) {

    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var reverb: PresetReverb? = null

    // Cached band centers (Hz) so we can address low/mid/high generically.
    private var bandCount = 0
    private var bandFreqs = IntArray(0)
    private var bandLevelRange = ShortArray(2)

    init {
        try {
            val e = Equalizer(0, audioSessionId)
            e.enabled = true
            eq = e
            bandCount = e.numberOfBands.toInt()
            bandFreqs = IntArray(bandCount)
            for (i in 0 until bandCount) {
                bandFreqs[i] = e.getCenterFreq(i.toShort()).toInt() // milliHz
            }
            val range = e.bandLevelRange
            if (range != null && range.size >= 2) bandLevelRange = range
        } catch (_: Exception) { eq = null }
        try {
            val b = BassBoost(0, audioSessionId)
            b.enabled = false
            bass = b
        } catch (_: Exception) { bass = null }
        try {
            val v = Virtualizer(0, audioSessionId)
            v.enabled = false
            virtualizer = v
        } catch (_: Exception) { virtualizer = null }
        try {
            val r = PresetReverb(0, audioSessionId)
            r.enabled = false
            reverb = r
        } catch (_: Exception) { reverb = null }
    }

    fun configure(effect: AudioEffect) {
        // Defaults: everything off / flat.
        setBassBoost(0)
        setVirtualizer(0)
        setReverb(PresetReverb.PRESET_NONE)
        // Flat EQ (0 mB on all bands).
        for (i in 0 until bandCount) setBand(i, 0)

        when (effect) {
            AudioEffect.NONE -> Unit

            AudioEffect.SMART -> {
                // Gentle lift: warm low end + airy top.
                setBandByFreq(120, 300)
                setBandByFreq(250, 200)
                setBandByFreq(3000, 200)
                setBandByFreq(8000, 300)
            }

            AudioEffect.SURROUND_360 -> {
                setVirtualizer(1000)
                setReverb(PresetReverb.PRESET_MEDIUMROOM)
                setBandByFreq(8000, 300)
            }

            AudioEffect.SUPER_BASS -> {
                setBassBoost(900)
                setBandByFreq(60, 600)
                setBandByFreq(150, 400)
            }

            AudioEffect.CLEAR_VOCALS -> {
                setBandByFreq(120, -400)   // trim muddy lows
                setBandByFreq(2500, 500)   // vocal presence
                setBandByFreq(5000, 400)
                setBandByFreq(8000, 300)
            }

            AudioEffect.AUDIO_3D -> {
                setVirtualizer(800)
                setBandByFreq(2000, 200)
            }

            AudioEffect.HIFI_LIVE -> {
                setReverb(PresetReverb.PRESET_LARGEHALL)
                setBandByFreq(7000, 400)
                setBandByFreq(120, 200)
            }

            AudioEffect.DYNAMIC_EDM -> {
                setBassBoost(800)
                setBandByFreq(60, 500)
                setBandByFreq(9000, 500)
                setBandByFreq(300, -200)
            }

            AudioEffect.ROCK -> {
                // Classic V-shape.
                setBandByFreq(80, 500)
                setBandByFreq(300, -200)
                setBandByFreq(1000, -100)
                setBandByFreq(3000, 300)
                setBandByFreq(8000, 500)
            }

            AudioEffect.VINTAGE -> {
                setReverb(PresetReverb.PRESET_PLATE)
                setBandByFreq(120, -300)
                setBandByFreq(1500, 300)
                setBandByFreq(8000, -300)
            }
        }
    }

    fun release() {
        try { eq?.enabled = false; eq?.release() } catch (_: Exception) {}
        try { bass?.enabled = false; bass?.release() } catch (_: Exception) {}
        try { virtualizer?.enabled = false; virtualizer?.release() } catch (_: Exception) {}
        try { reverb?.enabled = false; reverb?.release() } catch (_: Exception) {}
        eq = null; bass = null; virtualizer = null; reverb = null
    }

    private fun setBand(index: Int, levelMb: Int) {
        if (index < 0 || index >= bandCount) return
        val clamped = levelMb.coerceIn(bandLevelRange[0].toInt(), bandLevelRange[1].toInt())
        try { eq?.setBandLevel(index.toShort(), clamped.toShort()) } catch (_: Exception) {}
    }

    /** Boost/cut the band whose center frequency is closest to [targetHz]. */
    private fun setBandByFreq(targetHz: Int, levelMb: Int) {
        if (bandCount == 0) return
        var bestIdx = 0
        var bestDist = Int.MAX_VALUE
        for (i in 0 until bandCount) {
            // bandFreqs is in milliHz per Android docs; convert to Hz.
            val hz = bandFreqs[i] / 1000
            val d = abs(hz - targetHz)
            if (d < bestDist) { bestDist = d; bestIdx = i }
        }
        setBand(bestIdx, levelMb)
    }

    private fun setBassBoost(strength: Int) {
        val b = bass ?: return
        try {
            b.setStrength(strength.coerceIn(0, 1000).toShort())
            b.enabled = strength > 0
        } catch (_: Exception) {}
    }

    private fun setVirtualizer(strength: Int) {
        val v = virtualizer ?: return
        try {
            v.setStrength(strength.coerceIn(0, 1000).toShort())
            v.enabled = strength > 0
        } catch (_: Exception) {}
    }

    private fun setReverb(preset: Short) {
        val r = reverb ?: return
        try {
            r.preset = preset
            r.enabled = preset != PresetReverb.PRESET_NONE
        } catch (_: Exception) {}
    }
}
