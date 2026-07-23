package com.sodamusic.player.utils

/**
 * Opens a platform-native file chooser for audio files.
 * - Desktop: shows a modal JFileChooser filtered to common audio extensions.
 * - Android / iOS: currently returns null (no native picker wired up yet);
 *   callers should fall back to manual path entry.
 *
 * @return absolute path of the selected file, or `null` if the user cancelled
 *         or the platform has no native picker.
 */
expect fun openAudioFilePicker(): String?

/**
 * True when the platform provides a native modal file picker (Desktop/JVM).
 * Used to decide whether to skip the in-app TrackPicker dialog and drive
 * the flow straight from the native chooser.
 */
expect val hasNativeFilePicker: Boolean
