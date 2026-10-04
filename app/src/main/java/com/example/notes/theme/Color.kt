package com.example.notes.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

import androidx.compose.material3.MaterialTheme

// Modern soft-blue design system
val PrimaryColor = Color(0xFF4A8DF8) // Soft Azure Blue
val PrimaryDeep = Color(0xFF2F6FE0)
val SecondaryColor = Color(0xFFA78BFA) // Lavender
val TertiaryColor = Color(0xFF34D399) // Mint

// Accent tints used for quick-action icons
val AccentRose = Color(0xFFF87171)
val AccentAmber = Color(0xFFFBBF24)
val AccentMint = Color(0xFF34D399)
val AccentViolet = Color(0xFFA78BFA)

// Deep navy colors for Dark Mode
val DarkBackground = Color(0xFF0B1120)
val DarkSurface = Color(0xFF151D2E)
val DarkSurfaceVariant = Color(0xFF1F2A3F)
val DarkOnBackground = Color(0xFFF1F5FB)
val DarkOnSurface = Color(0xFFE6ECF5)

// Airy blue-grey colors for Light Mode
val LightBackground = Color(0xFFEAF0F8)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF3F6FB)
val LightOnBackground = Color(0xFF111827)
val LightOnSurface = Color(0xFF1F2937)

// Harmony-curated Note Background Colors (Index to ARGB Color) - Soft Material You Pastels!
val NoteColors = listOf(
    0 to Color.Transparent, // Default
    1 to Color(0xFFFFDAD6), // Soft Rose/Red
    2 to Color(0xFFFFDCC8), // Soft Peach/Orange
    3 to Color(0xFFFBE08F), // Soft Warm Yellow
    4 to Color(0xFFE2F3CD), // Soft Sage Green
    5 to Color(0xFFD1F2F2), // Soft Sky/Teal
    6 to Color(0xFFDDE2F9), // Soft Lavender/Blue
    7 to Color(0xFFE8DDFF), // Soft Periwinkle
    8 to Color(0xFFFCDDFF), // Soft Violet/Purple
    9 to Color(0xFFFFD9E2), // Soft Rose Pink
    10 to Color(0xFFFAE3C6), // Soft Amber
    11 to Color(0xFFE1E2EC)  // Soft Cool Grey
)

@Composable
fun getNoteOnColor(colorIndex: Int): Color {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    return if (isDark) {
        Color(0xFFE6E1E5) // Standard light text on dark background
    } else {
        Color(0xFF1C1B1F) // Standard dark text on light background
    }
}

@Composable
fun getNoteBackgroundColor(colorIndex: Int): Color {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val baseColor = NoteColors.firstOrNull { it.first == colorIndex }?.second ?: Color.Transparent
    if (baseColor == Color.Transparent) {
        return if (isDark) DarkSurface else LightSurface
    }
    
    return if (isDark) {
        when (colorIndex) {
            1 -> Color(0xFF5A1A1A)
            2 -> Color(0xFF552200)
            3 -> Color(0xFF4A3E00)
            4 -> Color(0xFF1E3A1E)
            5 -> Color(0xFF113D3D)
            6 -> Color(0xFF1C2C5A)
            7 -> Color(0xFF2D1F5B)
            8 -> Color(0xFF3F1948)
            9 -> Color(0xFF4E1624)
            10 -> Color(0xFF4A3412)
            11 -> Color(0xFF2B2D38)
            else -> DarkSurface
        }
    } else {
        baseColor
    }
}

