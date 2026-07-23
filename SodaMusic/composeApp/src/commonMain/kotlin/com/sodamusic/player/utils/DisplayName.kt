package com.sodamusic.player.utils

/**
 * Resolves a human-readable display name for a picked audio source.
 * - Android `content://` URI: queries the SAF provider for DISPLAY_NAME.
 * - File path / desktop: derives the file name from the path.
 *
 * @return display name without extension, or null if it can't be determined.
 */
expect fun resolveDisplayName(source: String): String?
