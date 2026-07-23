package com.sodamusic.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Soda Music inspired palette: deep forest green background, warm off-white text,
// soft lime/olive accents. Low saturation — no neon purple/pink.
private val SodaDarkColorScheme = darkColorScheme(
    primary = Color(0xFFC8D99A),          // soft lime text/icons
    onPrimary = Color(0xFF1A2418),
    primaryContainer = Color(0xFF3D4B33),
    onPrimaryContainer = Color(0xFFE4EED1),
    secondary = Color(0xFFB5C2A0),
    onSecondary = Color(0xFF222E1F),
    tertiary = Color(0xFFD6B98B),         // warm tan
    onTertiary = Color(0xFF2E2518),
    background = Color(0xFF1E2A1F),       // deep forest green
    onBackground = Color(0xFFE8EDE3),
    surface = Color(0xFF273428),          // slightly lighter panel
    onSurface = Color(0xFFE8EDE3),
    surfaceVariant = Color(0xFF314033),
    onSurfaceVariant = Color(0xFFB5BFB1),
    outline = Color(0xFF5A6B56),
    outlineVariant = Color(0xFF3E4E3B)
)

// Reserved for future light-mode work; not used while ForceDarkTheme = true.
private val SodaLightColorScheme = lightColorScheme(
    primary = Color(0xFF557A3E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E7BC),
    onPrimaryContainer = Color(0xFF1A2418),
    secondary = Color(0xFF606D50),
    onSecondary = Color.White,
    tertiary = Color(0xFF8A6E3F),
    background = Color(0xFFF5F7F0),
    onBackground = Color(0xFF1A2418),
    surface = Color.White,
    onSurface = Color(0xFF1A2418),
    surfaceVariant = Color(0xFFE6EBD8),
    onSurfaceVariant = Color(0xFF566451),
    outline = Color(0xFF8C9988),
    outlineVariant = Color(0xFFC3CDBC)
)

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
