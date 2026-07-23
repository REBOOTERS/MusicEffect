package com.sodamusic.player.audio

import com.sodamusic.player.IOSAudioPlayer

actual fun createNativeAudioPlayer(): NativeAudioPlayer = IOSAudioPlayer()
