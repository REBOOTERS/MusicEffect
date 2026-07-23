package com.sodamusic.player.utils

/**
 * Absolute path to a local audio file that should be auto-played on app start,
 * or null to skip auto-play (mobile / no file configured).
 */
expect fun getStartupTrackPath(): String?
