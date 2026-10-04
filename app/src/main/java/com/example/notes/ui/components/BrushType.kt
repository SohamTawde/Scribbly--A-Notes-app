package com.example.notes.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector

enum class BrushType(
    val label: String,
    val icon: ImageVector,
    val defaultStrokeWidth: Float,
    val defaultOpacity: Float,
    val strokeCap: StrokeCap,
    val strokeJoin: StrokeJoin
) {
    PEN(
        label = "Pen",
        icon = Icons.Default.Edit,
        defaultStrokeWidth = 6f,
        defaultOpacity = 1f,
        strokeCap = StrokeCap.Round,
        strokeJoin = StrokeJoin.Round
    ),
    MARKER(
        label = "Marker",
        icon = Icons.Default.BorderColor,
        defaultStrokeWidth = 18f,
        defaultOpacity = 0.6f,
        strokeCap = StrokeCap.Square,
        strokeJoin = StrokeJoin.Bevel
    ),
    WATERCOLOR(
        label = "Watercolor",
        icon = Icons.Default.Brush,
        defaultStrokeWidth = 28f,
        defaultOpacity = 0.28f,
        strokeCap = StrokeCap.Round,
        strokeJoin = StrokeJoin.Round
    ),
    PENCIL(
        label = "Pencil",
        icon = Icons.Default.Create,
        defaultStrokeWidth = 4f,
        defaultOpacity = 0.75f,
        strokeCap = StrokeCap.Butt,
        strokeJoin = StrokeJoin.Miter
    ),
    CALLIGRAPHY(
        label = "Calligraphy",
        icon = Icons.Default.AutoFixHigh,
        defaultStrokeWidth = 12f,
        defaultOpacity = 1f,
        strokeCap = StrokeCap.Round,
        strokeJoin = StrokeJoin.Round
    )
}
