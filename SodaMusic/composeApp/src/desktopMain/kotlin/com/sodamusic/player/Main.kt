package com.sodamusic.player

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.sodamusic.player.ui.App
import java.awt.Dimension
import java.awt.Toolkit

fun main() = application {
    // Desktop window: 42% screen width × 85% screen height by default, resizable
    // down to a minimum of 420×640. The UI uses vertical scroll so narrower sizes work.
    val screenSize: Dimension = Toolkit.getDefaultToolkit().screenSize
    val defaultW = (screenSize.width * 0.42).toInt().coerceAtLeast(480).dp
    val defaultH = (screenSize.height * 0.85).toInt().coerceAtLeast(720).dp

    Window(
        onCloseRequest = ::exitApplication,
        title = "SodaMusic",
        state = rememberWindowState(size = DpSize(defaultW, defaultH)),
    ) {
        window.minimumSize = Dimension(420, 640)
        App()
    }
}
