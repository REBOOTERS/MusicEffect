package com.sodamusic.player.audio

import com.sodamusic.player.AndroidAudioPlayer

actual fun createNativeAudioPlayer(): NativeAudioPlayer = AndroidAudioPlayer()
