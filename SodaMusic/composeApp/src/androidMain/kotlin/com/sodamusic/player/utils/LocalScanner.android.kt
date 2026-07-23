package com.sodamusic.player.utils

import android.content.ContentUris
import android.provider.MediaStore
import com.sodamusic.player.AndroidAudioPlayer
import com.sodamusic.player.AndroidFilePicker
import com.sodamusic.player.model.Track
import com.sodamusic.player.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun scanLocalTracks(): List<Track> = withContext(Dispatchers.IO) {
    // No permission -> return empty so the UI can fall back to the SAF picker.
    if (!AndroidFilePicker.ensureAudioPermission()) return@withContext emptyList()
    val ctx = AndroidAudioPlayer.appContext
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ALBUM_ID
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

    val result = mutableListOf<Track>()
    try {
        ctx.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val title = c.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "未知曲目"
                val artist = c.getString(artistCol)?.takeIf { it.isNotBlank() } ?: "未知艺术家"
                val duration = c.getLong(durCol).coerceAtLeast(0L)
                result.add(
                    Track(
                        id = "media_$id",
                        title = title,
                        artist = artist,
                        album = c.getString(albumCol).orEmpty(),
                        durationMs = duration,
                        source = TrackSource.ContentUri(uri.toString()),
                        coverColor = colorForTitle(title)
                    )
                )
            }
        }
    } catch (_: Exception) {
        return@withContext emptyList()
    }
    result
}

// Derive a stable cover color from the title so each row's tile differs.
private fun colorForTitle(title: String): Long {
    val palette = longArrayOf(
        0xFF8AA86F, 0xFF6F8A9F, 0xFFA08B6B, 0xFFB08A6F,
        0xFF88AA77, 0xFF6B7A8A, 0xFF9F8AA8, 0xFF7A9F8A
    )
    val h = (title.hashCode() and Int.MAX_VALUE)
    return palette[h % palette.size]
}
