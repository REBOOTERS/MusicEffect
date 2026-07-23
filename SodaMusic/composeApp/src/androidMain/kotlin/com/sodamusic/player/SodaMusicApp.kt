package com.sodamusic.player

import android.app.Application

class SodaMusicApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAudioPlayer.appContext = applicationContext
    }
}
