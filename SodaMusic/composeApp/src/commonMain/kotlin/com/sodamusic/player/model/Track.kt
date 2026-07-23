package com.sodamusic.player.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val source: TrackSource,
    val coverUrl: String? = null,
    val coverColor: Long = 0xFF6366F1
)

sealed class TrackSource {
    data class Local(val filePath: String) : TrackSource()
    data class Online(val streamUrl: String, val headers: Map<String, String> = emptyMap()) : TrackSource()
}
