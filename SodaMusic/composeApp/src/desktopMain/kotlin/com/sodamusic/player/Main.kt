package com.sodamusic.player

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.sodamusic.player.audio.effects.EffectProcessor
import com.sodamusic.player.audio.decode.Mp3Decoder
import com.sodamusic.player.model.PlayState
import com.sodamusic.player.model.Track
import com.sodamusic.player.model.TrackSource
import com.sodamusic.player.ui.App
import java.awt.Dimension
import java.awt.Toolkit
import kotlin.io.path.Path
import kotlin.math.max

fun main(args: Array<String>) {
    // Headless test mode: --play-mp3 <path> plays 5 seconds and reports timing.
    if (args.size >= 2 && args[0] == "--play-mp3") {
        playMp3Test(args[1])
        return
    }

    application {
        // Reasonable desktop default: 60% screen width × 85% height, min 720×640, resizable.
        val screenSize: Dimension = Toolkit.getDefaultToolkit().screenSize
        val defaultW = (screenSize.width * 0.42).toInt().coerceAtLeast(520).dp
        val defaultH = (screenSize.height * 0.85).toInt().coerceAtLeast(720).dp

        Window(
            onCloseRequest = ::exitApplication,
            title = "SodaMusic",
            state = rememberWindowState(size = DpSize(defaultW, defaultH)),
        ) {
            window.minimumSize = Dimension(420, 620)
            App()
        }
    }
}

private fun playMp3Test(path: String) {
    println("play-mp3 test: $path")
    val res = Mp3Decoder.decode(Path(path))
    if (res == null) { println("FAIL: decode returned null"); kotlin.system.exitProcess(2) }
    println("Decoded: sr=${res.sampleRate} ch=${res.channels} samples=${res.samples.size} durMs=${res.durationMs}")

    val player = DesktopAudioPlayer()
    val track = Track(
        id = "test",
        title = path.substringAfterLast('/').substringBeforeLast('.'),
        artist = "test",
        source = TrackSource.Local(path)
    )
    val effectProcessor = EffectProcessor(44100)
    player.play(track, effectProcessor)

    val start = System.currentTimeMillis()
    var lastState: PlayState = PlayState.IDLE
    // Run up to 8 seconds; expect state BUFFERING -> PLAYING and position advancing.
    while (System.currentTimeMillis() - start < 8000) {
        Thread.sleep(250)
        val p = player.positionMs
        val d = player.durationMs
        val state = player.playState.value
        if (state != lastState) {
            println("state -> $state after ${System.currentTimeMillis() - start}ms  pos=$p dur=$d")
            lastState = state
        } else {
            print(String.format("\rpos=%-6d dur=%-6d", p, d))
            System.out.flush()
        }
        if (state == PlayState.STOPPED || state == PlayState.ERROR) break
    }
    println()
    player.stop()
    player.release()
    val elapsed = System.currentTimeMillis() - start
    println("Done after ${elapsed}ms; final pos=${player.positionMs} dur=${player.durationMs} state=${player.playState.value}")
    kotlin.system.exitProcess(0)
}
