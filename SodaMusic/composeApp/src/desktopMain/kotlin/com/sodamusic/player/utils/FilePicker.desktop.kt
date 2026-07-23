package com.sodamusic.player.utils

import kotlinx.coroutines.future.await
import java.io.File
import java.util.prefs.Preferences
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

internal actual val hasNativeFilePickerImpl: Boolean = true

private const val PREF_KEY_LAST_DIR = "lastAudioDir"

internal actual suspend fun openAudioFilePickerImpl(): String? {
    val prefs = Preferences.userNodeForPackage(DesktopAudioPlayerPrefs::class.java)
    // Swing dialogs must be opened on the AWT Event Dispatch Thread.
    val future = java.util.concurrent.CompletableFuture<String?>()
    SwingUtilities.invokeLater {
        try {
            val chooser = JFileChooser().apply {
                dialogTitle = "选择音频文件"
                fileSelectionMode = JFileChooser.FILES_ONLY
                isAcceptAllFileFilterUsed = true
                addChoosableFileFilter(
                    FileNameExtensionFilter(
                        "音频文件 (mp3/wav/m4a/flac/ogg/aac)",
                        "mp3", "wav", "m4a", "flac", "ogg", "aac", "aiff", "wma"
                    )
                )
                val lastDir = prefs.get(PREF_KEY_LAST_DIR, null)
                if (lastDir != null) {
                    val f = File(lastDir)
                    if (f.exists() && f.isDirectory) currentDirectory = f
                }
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                val selected = chooser.selectedFile
                selected.parentFile?.let { parent ->
                    try {
                        prefs.put(PREF_KEY_LAST_DIR, parent.absolutePath)
                        prefs.flush()
                    } catch (_: Exception) { /* best effort */ }
                }
                future.complete(selected.absolutePath)
            } else {
                future.complete(null)
            }
        } catch (t: Throwable) {
            future.completeExceptionally(t)
        }
    }
    return try { future.await() } catch (_: Throwable) { null }
}

private class DesktopAudioPlayerPrefs private constructor()
