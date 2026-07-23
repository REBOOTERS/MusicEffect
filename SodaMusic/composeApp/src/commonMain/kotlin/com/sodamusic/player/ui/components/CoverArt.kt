package com.sodamusic.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sodamusic.player.model.Track
import kotlin.math.max

/**
 * Square rounded-corner album cover. The live spectrum is rendered as an overlay
 * along the bottom third of the cover so the animation lives inside the cover block
 * (no extra vertical space / no page scroll).
 */
@Composable
fun CoverArt(
    track: Track?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    spectrum: FloatArray = FloatArray(0)
) {
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
            .size(280.dp)
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
                modifier = Modifier.size(72.dp)
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

        // Spectrum overlay along the bottom of the cover.
        if (spectrum.isNotEmpty()) {
            SpectrumOverlay(
                levels = spectrum,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(1f)
            )
        }
    }
}

@Composable
private fun SpectrumOverlay(levels: FloatArray, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val bands = levels.size
        if (bands == 0) return@Canvas
        val gap = 2.dp.toPx()
        val barWidth = ((size.width - gap * (bands - 1)) / bands).coerceAtLeast(2f)
        // Bars occupy the bottom ~45% of the cover; rising from the bottom edge.
        val maxHeight = size.height * 0.45f
        val baseY = size.height
        for (i in 0 until bands) {
            val level = levels[i].coerceIn(0f, 1f)
            val h = max(level, 0.04f) * maxHeight
            val x = i * (barWidth + gap)
            val y = baseY - h
            drawRoundRect(
                color = Color.White.copy(alpha = 0.78f),
                topLeft = Offset(x, y),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
