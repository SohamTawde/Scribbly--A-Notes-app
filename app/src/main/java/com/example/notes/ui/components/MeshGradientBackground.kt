package com.example.notes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun MeshGradientBackground(isDark: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        if (isDark) {
            // Deep space base
            drawRect(color = Color(0xFF080818))

            // Large indigo core glow (center-left)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF4338CA).copy(alpha = 0.40f), Color.Transparent),
                    center = Offset(w * 0.20f, h * 0.30f),
                    radius = w * 1.0f
                ),
                center = Offset(w * 0.20f, h * 0.30f),
                radius = w * 1.0f
            )

            // Vibrant magenta/pink glow (top-right)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFD946EF).copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.12f),
                    radius = w * 0.75f
                ),
                center = Offset(w * 0.85f, h * 0.12f),
                radius = w * 0.75f
            )

            // Rose/coral accent (right-center)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE11D48).copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(w * 0.92f, h * 0.55f),
                    radius = w * 0.65f
                ),
                center = Offset(w * 0.92f, h * 0.55f),
                radius = w * 0.65f
            )

            // Teal/cyan glow (bottom-left)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF06B6D4).copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.82f),
                    radius = w * 0.70f
                ),
                center = Offset(w * 0.15f, h * 0.82f),
                radius = w * 0.70f
            )

            // Deep violet accent (bottom-right)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF7C3AED).copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(w * 0.75f, h * 0.90f),
                    radius = w * 0.55f
                ),
                center = Offset(w * 0.75f, h * 0.90f),
                radius = w * 0.55f
            )
        } else {
            // Warm cream/peach base for light mode
            drawRect(color = Color(0xFFFFF7ED))

            // Soft pink/rose glow (top-left)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFBCFE8).copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(w * 0.10f, h * 0.15f),
                    radius = w * 0.85f
                ),
                center = Offset(w * 0.10f, h * 0.15f),
                radius = w * 0.85f
            )

            // Soft lavender glow (top-right)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE9D5FF).copy(alpha = 0.50f), Color.Transparent),
                    center = Offset(w * 0.88f, h * 0.20f),
                    radius = w * 0.75f
                ),
                center = Offset(w * 0.88f, h * 0.20f),
                radius = w * 0.75f
            )

            // Soft sky blue glow (center-right)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFBAE6FD).copy(alpha = 0.40f), Color.Transparent),
                    center = Offset(w * 0.80f, h * 0.55f),
                    radius = w * 0.70f
                ),
                center = Offset(w * 0.80f, h * 0.55f),
                radius = w * 0.70f
            )

            // Soft peach glow (bottom-left)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFED7AA).copy(alpha = 0.50f), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.78f),
                    radius = w * 0.75f
                ),
                center = Offset(w * 0.15f, h * 0.78f),
                radius = w * 0.75f
            )

            // Soft mint glow (bottom-right)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFA7F3D0).copy(alpha = 0.40f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.85f),
                    radius = w * 0.55f
                ),
                center = Offset(w * 0.70f, h * 0.85f),
                radius = w * 0.55f
            )
        }
    }
}
