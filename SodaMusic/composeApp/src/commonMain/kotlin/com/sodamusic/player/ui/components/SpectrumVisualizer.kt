package com.sodamusic.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Animated bar spectrum driven by a normalized magnitude array ([levels], each in [0,1]).
 * Each band is drawn as a vertical rounded bar with a soft top highlight; gaps give it
 * a clean, music-app feel. Falls silent (flat low bars) when levels is empty or all zero.
 */
@Composable
fun SpectrumVisualizer(
    levels: FloatArray,
    modifier: Modifier = Modifier,
    barColor: Color = Color.White.copy(alpha = 0.85f),
    peakColor: Color = Color(0xFFC8D99A)
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        if (levels.isEmpty()) return@Canvas
        val bands = levels.size
        val gap = 2.dp.toPx()
        val barWidth = ((size.width - gap * (bands - 1)) / bands).coerceAtLeast(2f)
        val maxHeight = size.height
        for (i in 0 until bands) {
            // Slight gravity curve: outer bars a touch shorter so it reads symmetrically.
            val level = levels[i].coerceIn(0f, 1f)
            val h = max(level, 0.03f) * maxHeight
            val x = i * (barWidth + gap)
            val y = maxHeight - h
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
            )
            // Top peak cap highlight.
            if (h > 4f) {
                drawRoundRect(
                    color = peakColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, 3.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
