package com.sodamusic.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sodamusic.player.audio.MusicPlayerController
import com.sodamusic.player.model.PlayState
import com.sodamusic.player.model.Track
import com.sodamusic.player.model.TrackSource
import com.sodamusic.player.ui.LocalPlayer
import com.sodamusic.player.ui.components.AlbumArt
import com.sodamusic.player.ui.components.EffectsDrawer
import com.sodamusic.player.ui.components.PlaybackControls
import com.sodamusic.player.ui.components.ProgressBar
import com.sodamusic.player.ui.components.TrackPicker
import com.sodamusic.player.utils.hasNativeFilePicker
import com.sodamusic.player.utils.openAudioFilePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var showPicker by remember { mutableStateOf(!hasNativeFilePicker) }
    var showEffects by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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

    fun pickAndPlayLocal(path: String) {
        val track = Track(
            id = "local_$path",
            title = path.substringAfterLast('/').substringAfterLast('\\')
                .substringBeforeLast('.').ifBlank { path },
            artist = "本地文件",
            source = TrackSource.Local(path)
        )
        player.setQueue(demoTracks + track)
        player.play(track)
    }

    // Desktop: auto-open the native file picker once on first launch when there's no current track.
    var autoOpened by remember { mutableStateOf(false) }
    LaunchedEffect(hasNativeFilePicker) {
        if (hasNativeFilePicker && !autoOpened && currentTrack == null) {
            autoOpened = true
            val picked = withContext(Dispatchers.IO) { openAudioFilePicker() }
            if (!picked.isNullOrBlank()) {
                pickAndPlayLocal(picked.trim())
            }
        }
    }

    val bg = MaterialTheme.colorScheme.background
    val backgroundGradient = Brush.verticalGradient(
        0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.22f).compositeOver(bg),
        0.4f to MaterialTheme.colorScheme.primary.copy(alpha = 0.07f).compositeOver(bg),
        1f to bg
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "SodaMusic",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    },
                    actions = {
                        // Effects button — opens the bottom drawer.
                        IconButton(onClick = { showEffects = true }) {
                            Icon(
                                Icons.Default.Equalizer,
                                contentDescription = "音效",
                                tint = if (currentEffect.displayName != "原声") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        // Library / open file button.
                        IconButton(
                            onClick = {
                                if (hasNativeFilePicker) {
                                    scope.launch {
                                        val picked = withContext(Dispatchers.IO) { openAudioFilePicker() }
                                        if (!picked.isNullOrBlank()) pickAndPlayLocal(picked.trim())
                                    }
                                } else {
                                    showPicker = true
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.LibraryMusic,
                                contentDescription = "打开音频文件",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            PlayerContent(
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                positionMs = position,
                durationMs = durationMs,
                shuffle = shuffle,
                repeat = repeat,
                player = player,
                modifier = Modifier.padding(innerPadding)
            )
        }

        // Effects drawer overlays the bottom of the screen with a scrim.
        if (showEffects) {
            val dismissInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .align(Alignment.BottomCenter)
                    .clickable(
                        interactionSource = dismissInteraction,
                        indication = null,
                        onClick = { showEffects = false }
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Consume taps on the drawer itself so they don't also dismiss it.
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent
                ) {
                    EffectsDrawer(
                        currentEffect = currentEffect,
                        onSelect = { player.setEffect(it) },
                        onClose = { showEffects = false }
                    )
                }
            }
        }
    }

    if (showPicker) {
        TrackPicker(
            onPickLocal = { path ->
                pickAndPlayLocal(path)
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

@Composable
private fun PlayerContent(
    currentTrack: Track?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffle: Boolean,
    repeat: com.sodamusic.player.audio.RepeatMode,
    player: MusicPlayerController,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        // Vinyl album art — the visual centerpiece.
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
        } ?: run {
            Text(
                "未选择音频",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "点击右上角音符图标选择音频文件",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(20.dp))
        }

        ProgressBar(
            positionMs = positionMs,
            durationMs = durationMs,
            onSeek = { player.seekTo(it) }
        )

        Spacer(Modifier.height(12.dp))

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
    }
}
