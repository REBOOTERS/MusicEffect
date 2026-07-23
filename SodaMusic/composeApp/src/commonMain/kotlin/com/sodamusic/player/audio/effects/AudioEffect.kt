package com.sodamusic.player.audio.effects

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Enum matching the vip.jpg effect grid.
 * Each preset configures the full DSP chain in EffectProcessor.
 */
enum class AudioEffect(val displayName: String, val description: String, val isPremium: Boolean = true) {
    NONE("原声", "无音效处理", false),
    SMART("智能音效", "跟随曲风智能适配"),
    SURROUND_360("360环绕", "多角度超大声场体验"),
    SUPER_BASS("超重低音", "澎湃低音带来更多震撼"),
    CLEAR_VOCALS("清澈人声", "更具穿透力的人声体验"),
    AUDIO_3D("3D音效", "足不出户享受现场"),
    HIFI_LIVE("HIFI现场", "亲临最high音乐现场"),
    DYNAMIC_EDM("动感电音", "独具一格电子音乐风格"),
    ROCK("摇滚音效", "再现饱含激情音乐节奏"),
    VINTAGE("复古唱片", "复古怀旧年代感来袭");

    companion object {
        val all: List<AudioEffect> = entries
    }
}
