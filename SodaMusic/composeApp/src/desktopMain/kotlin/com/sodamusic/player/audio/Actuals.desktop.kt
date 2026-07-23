package com.sodamusic.player.audio

import com.sodamusic.player.DesktopAudioPlayer

actual fun createNativeAudioPlayer(): NativeAudioPlayer = DesktopAudioPlayer()
