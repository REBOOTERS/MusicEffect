package com.sodamusic.player.utils

actual fun resolveDisplayName(source: String): String? {
    val name = source.substringAfterLast('/').substringBeforeLast('.')
    return name.ifBlank { null }
}
