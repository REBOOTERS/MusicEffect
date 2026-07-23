package com.sodamusic.player.utils

import java.io.File
import java.util.concurrent.atomic.AtomicReference
import java.util.prefs.Preferences
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

actual val hasNativeFilePicker: Boolean = true

private const val PREF_KEY_LAST_DIR = "lastAudioDir"

actual fun openAudioFilePicker(): String? {
    val prefs = Preferences.userNodeForPackage(DesktopAudioPlayerPrefs::class.java)
    val result = AtomicReference<String?>(null)
    SwingUtilities.invokeAndWait {
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
            // Resume from the last directory the user picked from.
            val lastDir = prefs.get(PREF_KEY_LAST_DIR, null)
            if (lastDir != null) {
                val f = File(lastDir)
                if (f.exists() && f.isDirectory) currentDirectory = f
            }
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            val selected = chooser.selectedFile
            result.set(selected.absolutePath)
            // Remember the parent directory for next time.
            selected.parentFile?.let { parent ->
                try {
                    prefs.put(PREF_KEY_LAST_DIR, parent.absolutePath)
                    prefs.flush()
                } catch (_: Exception) { /* best effort */ }
            }
        }
    }
    return result.get()
}

private class DesktopAudioPlayerPrefs private constructor()
