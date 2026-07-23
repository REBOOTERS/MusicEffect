package com.sodamusic.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sodamusic.player.model.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Square rounded-corner album cover with a subtle pulse while playing.
 * Gradient uses the track's coverColor when available; otherwise falls back
 * to a neutral olive gradient matching the Soda Music dark-green theme.
 */
@Composable
fun CoverArt(track: Track?, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val coverColor = Color(track?.coverColor ?: 0xFF557A3E)
    val pulse by rememberInfiniteTransition(label = "cover-pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .size(300.dp)
            .graphicsLayer {
                val s = if (isPlaying) pulse else 1f
                scaleX = s; scaleY = s
            }
            .clip(shape)
            .background(
                Brush.radialGradient(
                    0f to coverColor.copy(alpha = 0.85f),
                    0.55f to coverColor.copy(alpha = 0.25f),
                    1f to Color(0xFF1A2418)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (track == null) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(88.dp)
            )
        } else {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 28.dp)
            )
        }
    }
}
