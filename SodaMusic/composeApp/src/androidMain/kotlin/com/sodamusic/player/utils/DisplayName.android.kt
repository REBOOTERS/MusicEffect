package com.sodamusic.player.utils

import android.net.Uri
import android.provider.OpenableColumns
import com.sodamusic.player.AndroidAudioPlayer

actual fun resolveDisplayName(source: String): String? {
    if (!source.startsWith("content://")) {
        // Treat as a filesystem path.
        val name = source.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
        return name.ifBlank { null }
    }
    return try {
        val resolver = AndroidAudioPlayer.appContext.contentResolver
        val uri = Uri.parse(source)
        var displayName: String? = null
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) displayName = c.getString(idx)
            }
        }
        displayName?.substringBeforeLast('.')?.ifBlank { null }
    } catch (_: Exception) {
        null
    }
}
