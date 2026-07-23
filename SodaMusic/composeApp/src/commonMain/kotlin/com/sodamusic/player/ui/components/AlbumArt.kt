package com.sodamusic.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sodamusic.player.model.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun AlbumArt(track: Track?, isPlaying: Boolean, modifier: Modifier = Modifier) {
    var rotation by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val startTime = System.nanoTime()
            val startRotation = rotation
            while (isActive) {
                val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
                rotation = (startRotation + elapsedMs / 12000f * 360f) % 360f
                delay(16)
            }
        }
    }

    // The vinyl face is an intentionally dark surface; text/icons on it stay
    // white so the metaphor reads correctly even if a light theme is ever enabled.
    Box(modifier = modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringGradient = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF818CF8),
                    Color(0xFFC084FC),
                    Color(0xFFF472B6),
                    Color(0xFF818CF8)
                )
            )
            drawCircle(brush = ringGradient, radius = size.minDimension / 2, style = Stroke(width = 8f))
        }

        Box(
            modifier = Modifier
                .size(240.dp)
                .graphicsLayer { rotationZ = rotation }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        0f to Color(track?.coverColor ?: 0xFF818CF8),
                        0.45f to Color(track?.coverColor ?: 0xFF818CF8).copy(alpha = 0.35f),
                        1f to Color(0xFF0F0F1A)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = track?.title ?: "SodaMusic",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = track?.artist ?: "选择音乐开始播放",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            // Polished-metal spindle: subtle top sheen over a dark disc with a faint ring.
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.18f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A2E))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                )
            }
        }
    }
}
