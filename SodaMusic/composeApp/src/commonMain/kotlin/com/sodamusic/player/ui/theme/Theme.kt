package com.sodamusic.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SodaDarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF0F0F1A),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFC7D2FE),
    secondary = Color(0xFFC084FC),
    onSecondary = Color(0xFF1A1A2E),
    secondaryContainer = Color(0xFF581C87),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = Color(0xFFF472B6),
    onTertiary = Color(0xFF1A1A2E),
    tertiaryContainer = Color(0xFF9D174D),
    onTertiaryContainer = Color(0xFFFBCFE8),
    background = Color(0xFF0F0F1A),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1A1A2E),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF252542),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155)
)

// Fully-specified light scheme kept for future use; not active by default.
private val SodaLightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF3730A3),
    secondary = Color(0xFF9333EA),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF6B21A8),
    tertiary = Color(0xFFDB2777),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCE7F3),
    onTertiaryContainer = Color(0xFF9D174D),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1)
)

// Music players (Spotify / Apple Music / 汽水音乐) ship a dark player by default;
// the vinyl disc metaphor only glows against a dark surface. Set to false and
// the scheme will follow the system setting.
private const val ForceDarkTheme = true

@Composable
fun SodaMusicTheme(content: @Composable () -> Unit) {
    val scheme = when {
        ForceDarkTheme -> SodaDarkColorScheme
        isSystemInDarkTheme() -> SodaDarkColorScheme
        else -> SodaLightColorScheme
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
