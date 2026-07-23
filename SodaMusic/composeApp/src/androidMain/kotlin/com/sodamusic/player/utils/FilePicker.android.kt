package com.sodamusic.player.utils

import com.sodamusic.player.AndroidFilePicker

internal actual val hasNativeFilePickerImpl: Boolean = true

internal actual suspend fun openAudioFilePickerImpl(): String? {
    // Suspends until the user picks a file or cancels. Requires MainActivity.attach().
    return AndroidFilePicker.pick()
}
