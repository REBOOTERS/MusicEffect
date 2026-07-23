package com.sodamusic.player

import com.sodamusic.player.audio.MusicPlayerController
import com.sodamusic.player.ui.App
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SodaMusic",
        state = rememberWindowState(width = 420.dp, height = 780.dp)
    ) {
        App()
    }
}
