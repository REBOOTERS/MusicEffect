package com.sodamusic.player.utils

/**
 * Opens a platform-native file chooser for audio files.
 * - Desktop: shows a modal JFileChooser filtered to common audio extensions.
 * - Android / iOS: currently returns null (no native picker wired up yet);
 *   callers should fall back to manual path entry.
 *
 * @return absolute path of the selected file, or `null` if the user cancelled.
 */
expect fun openAudioFilePicker(): String?
