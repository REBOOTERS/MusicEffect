package com.sodamusic.player

import androidx.compose.ui.window.ComposeUIViewController
import com.sodamusic.player.ui.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
