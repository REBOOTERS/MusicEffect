package com.sodamusic.player.utils

import com.sodamusic.player.model.Track

/**
 * Scans the device for local audio files and returns them as playable tracks.
 * - Android: queries MediaStore.Audio.Media (requires READ_MEDIA_AUDIO; the implementation
 *   requests it at runtime). Returns one [Track] per song with a `content://` source.
 * - Desktop / iOS: returns an empty list (no system media library to scan).
 *
 * Must be called off the main thread.
 */
expect suspend fun scanLocalTracks(): List<Track>
