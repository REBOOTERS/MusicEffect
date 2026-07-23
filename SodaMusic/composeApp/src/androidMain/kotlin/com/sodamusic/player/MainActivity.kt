package com.sodamusic.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sodamusic.player.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidFilePicker.attach(this)
        enableEdgeToEdge()
        setContent { App() }
    }

    override fun onDestroy() {
        AndroidFilePicker.detach()
        super.onDestroy()
    }
}
