package com.sodamusic.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.sodamusic.player.audio.MusicPlayerController
import com.sodamusic.player.ui.screens.PlayerScreen
import com.sodamusic.player.ui.theme.SodaMusicTheme

val LocalPlayer = staticCompositionLocalOf<MusicPlayerController> { error("No player") }

@Composable
fun App() {
    val player = remember { MusicPlayerController() }

    CompositionLocalProvider(LocalPlayer provides player) {
        SodaMusicTheme {
            PlayerScreen()
        }
    }
}
