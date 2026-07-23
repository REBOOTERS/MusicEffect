package com.sodamusic.player.utils

/**
 * Opens a platform-native file chooser for audio files. Suspends until the user selects
 * a file or cancels.
 *
 * - Desktop: modal JFileChooser filtered to common audio extensions. Returns absolute path.
 * - Android: launches the system Storage Access Framework (SAF) audio picker; returns a
 *   `content://...` URI wrapped as a string (the Android MediaExtractor can read it directly).
 * - iOS: returns null (not yet implemented).
 *
 * @return absolute file path (desktop) / content URI (android), or null on cancel.
 */
suspend fun openAudioFilePicker(): String? = openAudioFilePickerImpl()

/** True when the platform provides a native modal audio picker (Desktop + Android). */
val hasNativeFilePicker: Boolean = hasNativeFilePickerImpl

// Internal expect/actual bridge so the public suspend API is clean.
internal expect suspend fun openAudioFilePickerImpl(): String?
internal expect val hasNativeFilePickerImpl: Boolean
