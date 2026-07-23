package com.sodamusic.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.sodamusic.player.ui.components.CoverArt
import com.sodamusic.player.ui.components.EffectsDrawer
import com.sodamusic.player.ui.components.PlaybackControls
import com.sodamusic.player.ui.components.ProgressBar
import com.sodamusic.player.ui.components.TrackPicker
import com.sodamusic.player.utils.getStartupTrackPath
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
            Track("demo1", "Sunset Drive", "Chillwave", "Demo", 180000, TrackSource.Online("demo://soda/1"), coverColor = 0xFF8AA86F),
            Track("demo2", "Neon Nights", "Synthwave", "Demo", 210000, TrackSource.Online("demo://soda/2"), coverColor = 0xFF6F8A9F),
            Track("demo3", "Midnight Jazz", "Smooth Jazz", "Demo", 240000, TrackSource.Online("demo://soda/3"), coverColor = 0xFFA08B6B),
            Track("demo4", "Electric Dreams", "EDM", "Demo", 195000, TrackSource.Online("demo://soda/4"), coverColor = 0xFFB08A6F),
            Track("demo5", "Acoustic Breeze", "Acoustic", "Demo", 200000, TrackSource.Online("demo://soda/5"), coverColor = 0xFF88AA77),
            Track("demo6", "Bass Drop", "Dubstep", "Demo", 170000, TrackSource.Online("demo://soda/6"), coverColor = 0xFF6B7A8A)
        )
    }

    val durationMs = player.durationMs.coerceAtLeast(currentTrack?.durationMs ?: 0L)
    val isPlaying = playState == PlayState.PLAYING

    fun pickAndPlayLocal(picked: String) {
        val isContentUri = picked.startsWith("content://")
        val displayName = if (isContentUri) {
            // content://com.android.externalstorage/.../foo.mp3 -> take last path segment.
            picked.substringAfterLast('%').substringAfterLast('/').substringBeforeLast('.')
                .ifBlank { picked.substringAfterLast('/').substringBeforeLast('.').ifBlank { "音频文件" } }
        } else {
            picked.substringAfterLast('/').substringAfterLast('\\')
                .substringBeforeLast('.').ifBlank { picked }
        }
        val source = if (isContentUri) TrackSource.ContentUri(picked) else TrackSource.Local(picked)
        val track = Track(
            id = "local_$picked",
            title = displayName,
            artist = "本地文件",
            source = source
        )
        player.setQueue(demoTracks + track)
        player.play(track)
    }

    // Auto-play on first launch:
    //  - Desktop: if caee.mp3 is available at the well-known path, play it directly.
    //  - Android (and other native-picker platforms): open the system file picker so the
    //    user chooses a real audio file.
    //  - Otherwise open the in-app TrackPicker (which lists demos + local/online tabs).
    var autoOpened by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (autoOpened || currentTrack != null) return@LaunchedEffect
        autoOpened = true
        val startupPath = getStartupTrackPath()
        when {
            startupPath != null -> pickAndPlayLocal(startupPath)
            hasNativeFilePicker -> {
                val picked = withContext(Dispatchers.IO) { openAudioFilePicker() }
                if (!picked.isNullOrBlank()) {
                    pickAndPlayLocal(picked.trim())
                } else {
                    // User cancelled the system picker — fall back to in-app dialog so
                    // they can at least pick a demo track.
                    showPicker = true
                }
            }
            else -> showPicker = true
        }
    }

    val bg = MaterialTheme.colorScheme.background
    val backgroundGradient = Brush.verticalGradient(
        0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.08f).compositeOver(bg),
        0.5f to MaterialTheme.colorScheme.surface.copy(alpha = 0.6f).compositeOver(bg),
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
                            fontWeight = FontWeight.Medium,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    actions = {
                        // Equalizer / effects toggle
                        IconButton(onClick = { showEffects = true }) {
                            Icon(
                                Icons.Default.Equalizer,
                                contentDescription = "音效",
                                tint = if (currentEffect.displayName != "原声")
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))

                CoverArt(track = currentTrack, isPlaying = isPlaying)

                Spacer(Modifier.height(32.dp))

                currentTrack?.let { track ->
                    Text(
                        track.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        track.artist,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Spacer(Modifier.height(28.dp))
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
                    Spacer(Modifier.height(28.dp))
                }

                ProgressBar(
                    positionMs = position,
                    durationMs = durationMs,
                    onSeek = { player.seekTo(it) }
                )

                Spacer(Modifier.height(8.dp))

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

                Spacer(Modifier.height(32.dp))
            }
        }

        // Effects drawer overlays the bottom of the screen with a scrim.
        if (showEffects) {
            val dismissInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
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
