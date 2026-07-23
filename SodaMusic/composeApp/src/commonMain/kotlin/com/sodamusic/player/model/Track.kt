package com.sodamusic.player.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val source: TrackSource,
    val coverUrl: String? = null,
    val coverColor: Long = 0xFF557A3E
)

sealed class TrackSource {
    /** Absolute filesystem path (desktop or known android file path). */
    data class Local(val filePath: String) : TrackSource()
    /** Android content:// URI picked via the system SAF picker. */
    data class ContentUri(val uri: String) : TrackSource()
    /** Online HTTP/HTTPS stream. */
    data class Online(val streamUrl: String, val headers: Map<String, String> = emptyMap()) : TrackSource()
}
