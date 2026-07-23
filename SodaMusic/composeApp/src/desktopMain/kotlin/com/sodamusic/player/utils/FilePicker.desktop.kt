package com.sodamusic.player.utils

import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

actual fun openAudioFilePicker(): String? {
    val result = AtomicReference<String?>(null)
    SwingUtilities.invokeAndWait {
        val chooser = JFileChooser().apply {
            dialogTitle = "选择音频文件"
            fileSelectionMode = JFileChooser.FILES_ONLY
            isAcceptAllFileFilterUsed = true
            addChoosableFileFilter(
                FileNameExtensionFilter(
                    "音频文件 (wav/mp3/m4a/flac/ogg/aac)",
                    "wav", "mp3", "m4a", "flac", "ogg", "aac", "aiff", "wma"
                )
            )
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            result.set(chooser.selectedFile.absolutePath)
        }
    }
    return result.get()
}
