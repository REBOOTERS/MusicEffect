package com.sodamusic.player.utils

/**
 * iOS: native document picker not yet wired through expect/actual.
 * Caller falls back to manual path entry.
 */
actual val hasNativeFilePicker: Boolean = false

actual fun openAudioFilePicker(): String? = null
