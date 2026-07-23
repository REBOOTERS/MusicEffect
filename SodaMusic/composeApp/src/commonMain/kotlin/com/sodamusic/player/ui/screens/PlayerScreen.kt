package com.sodamusic.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sodamusic.player.audio.MusicPlayerController
import com.sodamusic.player.audio.RepeatMode
import com.sodamusic.player.model.PlayState
import com.sodamusic.player.model.Track
import com.sodamusic.player.model.TrackSource
import com.sodamusic.player.ui.LocalPlayer
import com.sodamusic.player.ui.components.AlbumArt
import com.sodamusic.player.ui.components.EffectsPanel
import com.sodamusic.player.ui.components.PlaybackControls
import com.sodamusic.player.ui.components.ProgressBar
import com.sodamusic.player.ui.components.TrackPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen() {
    val player = LocalPlayer.current
    val currentTrack by player.currentTrack.collectAsState()
    val playState by player.playState.collectAsState()
    val position by player.position.collectAsState()
    val currentEffect by player.currentEffect.collectAsState()
    val shuffle by player.isShuffle.collectAsState()
    val repeat by player.repeatMode.collectAsState()
    var showPicker by remember { mutableStateOf(true) }

    val demoTracks = remember {
        listOf(
            Track("demo1", "Sunset Drive", "Chillwave", "Demo", 180000, TrackSource.Online("demo://soda/1"), coverColor = 0xFFFF6B6B),
            Track("demo2", "Neon Nights", "Synthwave", "Demo", 210000, TrackSource.Online("demo://soda/2"), coverColor = 0xFF4ECDC4),
            Track("demo3", "Midnight Jazz", "Smooth Jazz", "Demo", 240000, TrackSource.Online("demo://soda/3"), coverColor = 0xFF45B7D1),
            Track("demo4", "Electric Dreams", "EDM", "Demo", 195000, TrackSource.Online("demo://soda/4"), coverColor = 0xFFF7DC6F),
            Track("demo5", "Acoustic Breeze", "Acoustic", "Demo", 200000, TrackSource.Online("demo://soda/5"), coverColor = 0xFF82E0AA),
            Track("demo6", "Bass Drop", "Dubstep", "Demo", 170000, TrackSource.Online("demo://soda/6"), coverColor = 0xFFBB8FCE)
        )
    }

    val durationMs = player.durationMs.coerceAtLeast(currentTrack?.durationMs ?: 0L)
    val isPlaying = playState == PlayState.PLAYING

    val backgroundGradient = Brush.verticalGradient(
        0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        0.4f to MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
        1f to MaterialTheme.colorScheme.background
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopAppBar(
                title = { Text("SodaMusic", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                actions = {
                    IconButton(onClick = { showPicker = true }) {
                        Icon(
                            Icons.Default.LibraryMusic,
                            contentDescription = "Library",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )

            Spacer(Modifier.height(20.dp))

            AlbumArt(track = currentTrack, isPlaying = isPlaying)

            Spacer(Modifier.height(28.dp))

            currentTrack?.let { track ->
                Text(
                    track.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    track.artist,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(20.dp))
            }

            ProgressBar(
                positionMs = position,
                durationMs = durationMs,
                onSeek = { player.seekTo(it) }
            )

            Spacer(Modifier.height(16.dp))

            PlaybackControls(
                isPlaying = isPlaying,
                isShuffle = shuffle,
                repeatMode = repeat,
                onPlayPause = { player.togglePlayPause() },
                onNext = { player.next() },
                onPrevious = { player.previous() },
                onShuffle = { player.toggleShuffle() },
                onRepeat = { player.cycleRepeat() }
            )

            Spacer(Modifier.height(24.dp))

            EffectsPanel(
                currentEffect = currentEffect,
                onSelect = { player.setEffect(it) }
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPicker) {
        TrackPicker(
            onPickLocal = { path ->
                val track = Track(
                    id = "local_$path",
                    title = path.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.'),
                    artist = "本地文件",
                    source = TrackSource.Local(path)
                )
                player.setQueue(demoTracks + track)
                player.play(track)
                showPicker = false
            },
            onPickOnline = { url ->
                val track = Track(
                    id = "online_$url",
                    title = url.substringAfterLast('/').substringBeforeLast('?').ifBlank { "在线音频" },
                    artist = "在线播放",
                    source = TrackSource.Online(url)
                )
                player.setQueue(demoTracks + track)
                player.play(track)
                showPicker = false
            },
            demoTracks = demoTracks,
            onSelectTrack = { track ->
                player.setQueue(demoTracks)
                player.play(track)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}
