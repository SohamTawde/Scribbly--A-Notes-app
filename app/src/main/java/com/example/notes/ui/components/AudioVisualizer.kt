package com.example.notes.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

@Composable
fun AudioVisualizer(
    isRecording: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 15
    val transition = rememberInfiniteTransition(label = "audioWave")
    
    // Create animations for bars. If not active, keep static low heights.
    val animations = (0 until barCount).map { index ->
        if (isRecording || isPlaying) {
            val duration = 400 + (index % 3) * 150
            val delay = (index % 4) * 100
            transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, delayMillis = delay, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "barHeight_$index"
            )
        } else {
            // Static flat line
            rememberUpdatedState(0.15f)
        }
    }
    
    val activeColor = if (isRecording) {
        MaterialTheme.colorScheme.error // Red for recording
    } else {
        MaterialTheme.colorScheme.secondary // Pink/secondary for playing
    }
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        val width = size.width
        val height = size.height
        val barWidth = 8.dp.toPx()
        val spacing = 6.dp.toPx()
        val totalBarWidth = barWidth + spacing
        
        // Center the wave horizontally
        val startX = (width - (barCount * totalBarWidth - spacing)) / 2
        
        for (i in 0 until barCount) {
            val scale = animations[i].value
            val barHeight = height * scale
            val x = startX + i * totalBarWidth
            val y = (height - barHeight) / 2 // Center vertically
            
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
