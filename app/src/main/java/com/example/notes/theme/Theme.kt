package com.example.notes.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF5B9BFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A6B),
    onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = SecondaryColor,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3B2F66),
    onSecondaryContainer = Color(0xFFEDE7FF),
    tertiary = TertiaryColor,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF0F4D3A),
    onTertiaryContainer = Color(0xFFD1FAE5),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onSurfaceVariant = Color(0xFF9AA6BA),
    outline = Color(0xFF55627A),
    outlineVariant = Color(0xFF2A3549),
    error = Color(0xFFF87171),
    surfaceContainerHighest = Color(0xFF2A3549),
    surfaceContainerHigh = Color(0xFF212B3E),
    surfaceContainer = Color(0xFF1A2335),
    surfaceContainerLow = Color(0xFF151D2E),
    surfaceContainerLowest = Color(0xFF0B1120)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF0F2D66),
    secondary = SecondaryColor,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE7FF),
    onSecondaryContainer = Color(0xFF3B2F66),
    tertiary = TertiaryColor,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF0F4D3A),
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFF9CA3AF),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFEF4444),
    surfaceContainerHighest = Color(0xFFE2E8F2),
    surfaceContainerHigh = Color(0xFFEDF2F9),
    surfaceContainer = Color(0xFFF3F6FB),
    surfaceContainerLow = Color(0xFFF8FAFD),
    surfaceContainerLowest = Color.White
)

@Composable
fun NotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamicColor to enforce our curated premium colors!
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
