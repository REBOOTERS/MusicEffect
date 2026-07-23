package com.sodamusic.player.utils

import java.io.File

actual fun getStartupTrackPath(): String? {
    // The guide.txt asks us to validate against caee.mp3 placed at the project root.
    // Try a few well-known locations so the user doesn't need to pick the file manually.
    val candidates = listOf(
        "D:/workspace/agent-proj/caee.mp3",
        System.getProperty("user.dir") + "/caee.mp3",
        System.getProperty("user.dir") + "/../caee.mp3",
        System.getProperty("user.home") + "/caee.mp3"
    )
    return candidates.firstOrNull { File(it).exists() }
}
