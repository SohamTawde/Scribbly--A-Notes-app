package com.example.notes.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.*

// ── Palette data matching Image 5 ─────────────────────────────────────────────

val GrayscaleRow = listOf(
    Color(0xFFFFFFFF), Color(0xFFE5E5EA), Color(0xFFD1D1D6), Color(0xFFAEAEB2), Color(0xFF8E8E93),
    Color(0xFF636366), Color(0xFF48484A), Color(0xFF3A3A3C), Color(0xFF2C2C2E), Color(0xFF000000)
)

val SpectrumColumns = listOf(
    // Col 0: Blue shades
    listOf(Color(0xFF0D1B2A), Color(0xFF1B4965), Color(0xFF2A6F97), Color(0xFF468FAF), Color(0xFF61A5C2), Color(0xFF89C2D9), Color(0xFFA9D6E5)),
    // Col 1: Indigo shades
    listOf(Color(0xFF10002B), Color(0xFF240046), Color(0xFF3C096C), Color(0xFF5A189A), Color(0xFF7B2CBF), Color(0xFF9D4EDD), Color(0xFFC77DFF)),
    // Col 2: Violet / Purple shades
    listOf(Color(0xFF2B0938), Color(0xFF4A1259), Color(0xFF6B1772), Color(0xFF861B85), Color(0xFFA4249B), Color(0xFFC743BD), Color(0xFFE879E0)),
    // Col 3: Magenta / Crimson
    listOf(Color(0xFF3B0018), Color(0xFF5E0027), Color(0xFF850638), Color(0xFFA61048), Color(0xFFC91A5B), Color(0xFFE63973), Color(0xFFF7729F)),
    // Col 4: Red / Vermilion
    listOf(Color(0xFF4A0505), Color(0xFF730B0B), Color(0xFF9E1010), Color(0xFFC91A1A), Color(0xFFE53935), Color(0xFFEF5350), Color(0xFFE57373)),
    // Col 5: Orange / Coral
    listOf(Color(0xFF4E2000), Color(0xFF783200), Color(0xFFA34400), Color(0xFFD35400), Color(0xFFF3722C), Color(0xFFF8961E), Color(0xFFF9C74F)),
    // Col 6: Amber / Gold
    listOf(Color(0xFF473B00), Color(0xFF6B5800), Color(0xFF8F7600), Color(0xFFB89600), Color(0xFFE0B500), Color(0xFFFFD129), Color(0xFFFFE66D)),
    // Col 7: Lime / Chartreuse
    listOf(Color(0xFF273800), Color(0xFF3E5400), Color(0xFF577300), Color(0xFF739400), Color(0xFF90B800), Color(0xFFAACC00), Color(0xFFD0F040)),
    // Col 8: Green / Mint
    listOf(Color(0xFF083318), Color(0xFF104F26), Color(0xFF1A6B35), Color(0xFF278A48), Color(0xFF38B064), Color(0xFF51CF66), Color(0xFF8CE99A)),
    // Col 9: Teal / Cyan
    listOf(Color(0xFF003033), Color(0xFF034A4F), Color(0xFF06666D), Color(0xFF0A858E), Color(0xFF10A6B2), Color(0xFF22CCE2), Color(0xFF6FE9F7))
)

val DefaultSwatches = listOf(
    Color(0xFFFA5252), Color(0xFFFF922B), Color(0xFFFFD43B), Color(0xFF51CF66), Color(0xFF339AF0),
    Color(0xFF9D4EDD), Color(0xFF000000), Color(0xFF495057), Color(0xFFFFFFFF)
)

enum class PickerMode(val label: String) {
    GRID("Grid"), RING("Ring")
}

// ── Color Picker Card (Matches Image 5) ────────────────────────────────────────

@Composable
fun RingColorPickerCard(
    initialColor: Color,
    initialOpacity: Float,
    onColorSelected: (Color, Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(PickerMode.GRID) }

    val initHsv = remember(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        hsv
    }
    var hue by remember { mutableFloatStateOf(initHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initHsv[1]) }
    var value by remember { mutableFloatStateOf(initHsv[2]) }
    var opacity by remember { mutableFloatStateOf(initialOpacity) }

    val customSwatches = remember { mutableStateListOf(*DefaultSwatches.toTypedArray()) }

    val currentColor by remember(hue, saturation, value) {
        derivedStateOf { Color.hsv(hue, saturation, value) }
    }

    val isDark = isAppInDarkTheme()
    val cardBg = if (isDark) Color(0xFF28282B) else MaterialTheme.colorScheme.surface
    val cardBorder = if (isDark) Color(0xFF3E3E42) else MaterialTheme.colorScheme.outlineVariant
    val pillBg = if (isDark) Color(0xFF3A3A3C) else MaterialTheme.colorScheme.surfaceVariant
    val iconTint = if (isDark) Color(0xFFD1D1D6) else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        tonalElevation = 12.dp,
        modifier = modifier
            .widthIn(max = 350.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Bar: Eyedropper + Segmented [Grid] [Ring] Pill ────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Eyedropper icon
                IconButton(
                    onClick = {
                        // Eyedropper action toggle
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Colorize,
                        contentDescription = "Eyedropper",
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Segmented control pill: [Grid]  [Ring]
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(pillBg)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    PickerMode.entries.forEach { m ->
                        val isSelected = (mode == m)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) (if (isDark) Color.White else MaterialTheme.colorScheme.primary) else Color.Transparent)
                                .clickable { mode = m }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = m.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isSelected) (if (isDark) Color.Black else MaterialTheme.colorScheme.onPrimary) else (if (isDark) Color(0xFFB0B0B5) else MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            )
                        }
                    }
                }

                // Spacer to balance layout
                Spacer(Modifier.size(36.dp))
            }

            Spacer(Modifier.height(16.dp))

            // ── Center Content: Matrix Grid vs Hue Ring ──────────────
            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                label = "color_picker_mode"
            ) { currentMode ->
                when (currentMode) {
                    PickerMode.GRID -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp)),
                            verticalArrangement = Arrangement.spacedBy(1.5.dp)
                        ) {
                            // Row 0: Grayscale spectrum
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp),
                                horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                            ) {
                                GrayscaleRow.forEach { color ->
                                    val isColorSelected = (currentColor.toArgb() == color.toArgb())
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(color)
                                            .then(
                                                if (isColorSelected) {
                                                    Modifier.border(2.5.dp, Color.White, RoundedCornerShape(2.dp))
                                                } else Modifier
                                            )
                                            .clickable {
                                                val hsv = FloatArray(3)
                                                android.graphics.Color.colorToHSV(color.toArgb(), hsv)
                                                hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
                                            }
                                    )
                                }
                            }

                            // Rows 1-7: Full color spectrum matrix
                            for (rowIndex in 0..6) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(23.dp),
                                    horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                                ) {
                                    for (colIndex in 0..9) {
                                        val color = SpectrumColumns[colIndex][rowIndex]
                                        val isColorSelected = (currentColor.toArgb() == color.toArgb())
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(color)
                                                .then(
                                                    if (isColorSelected) {
                                                        Modifier.border(2.5.dp, Color.White, RoundedCornerShape(2.dp))
                                                    } else Modifier
                                                )
                                                .clickable {
                                                    val hsv = FloatArray(3)
                                                    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
                                                    hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    PickerMode.RING -> {
                        HueSaturationRing(
                            hue = hue,
                            saturation = saturation,
                            value = value,
                            onHueChange = { hue = it },
                            onSVChange = { s, v -> saturation = s; value = v }
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Checkerboard Opacity Slider (Matches Image 5) ────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                // Checkerboard background
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCheckerboard(tileSize = 12f)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(currentColor.copy(alpha = 0f), currentColor.copy(alpha = 1f))
                        ),
                        topLeft = Offset.Zero,
                        size = size
                    )
                }

                // Interactive slider
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = (-11).dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    )
                )
            }

            Spacer(Modifier.height(18.dp))

            // ── Bottom Preview Box + Swatches Row (Matches Image 5) ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Large color preview box
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCheckerboard(tileSize = 10f)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(currentColor.copy(alpha = opacity))
                            .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                    )
                }

                Spacer(Modifier.width(14.dp))

                // 2 Rows of Palette Swatches with [+] button
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Row 1 (first 5 swatches)
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        customSwatches.take(5).forEach { swatch ->
                            SwatchCircle(
                                color = swatch,
                                isSelected = (currentColor.toArgb() == swatch.toArgb()),
                                onClick = {
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(swatch.toArgb(), hsv)
                                    hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
                                }
                            )
                        }
                    }

                    // Row 2 (next 4 swatches + Add button)
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        customSwatches.drop(5).take(4).forEach { swatch ->
                            SwatchCircle(
                                color = swatch,
                                isSelected = (currentColor.toArgb() == swatch.toArgb()),
                                onClick = {
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(swatch.toArgb(), hsv)
                                    hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
                                }
                            )
                        }

                        // Add current color to swatches (+) button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(pillBg)
                                .border(1.dp, cardBorder, CircleShape)
                                .clickable {
                                    if (!customSwatches.contains(currentColor)) {
                                        customSwatches.add(currentColor)
                                    }
                                }
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add swatch",
                                tint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Apply button
            Button(
                onClick = { onColorSelected(currentColor, opacity) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = currentColor.copy(alpha = 1f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = "Apply Color",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (value > 0.65f && saturation < 0.45f) Color.Black else Color.White
                    )
                )
            }
        }
    }
}

// ── Swatch Circle Component ───────────────────────────────────────────────────

@Composable
private fun SwatchCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) Color.White else Color(0xFF555558),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

// ── Checkerboard drawing helper ───────────────────────────────────────────────

private fun DrawScope.drawCheckerboard(tileSize: Float = 12f) {
    val cols = ceil(size.width / tileSize).toInt()
    val rows = ceil(size.height / tileSize).toInt()
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val cellColor = if ((r + c) % 2 == 0) Color(0xFF454548) else Color(0xFF2C2C2E)
            drawRect(
                color = cellColor,
                topLeft = Offset(c * tileSize, r * tileSize),
                size = Size(tileSize, tileSize)
            )
        }
    }
}

// ── Full Dialog implementation ────────────────────────────────────────────────

@Composable
fun RingColorPickerDialog(
    initialColor: Color,
    initialOpacity: Float,
    onColorSelected: (Color, Float) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
            ) {
                RingColorPickerCard(
                    initialColor = initialColor,
                    initialOpacity = initialOpacity,
                    onColorSelected = onColorSelected,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

// ── Hue ring + SV square implementation ───────────────────────────────────────

@Composable
private fun HueSaturationRing(
    hue: Float,
    saturation: Float,
    value: Float,
    onHueChange: (Float) -> Unit,
    onSVChange: (Float, Float) -> Unit
) {
    val ringThickness = 22.dp

    Box(
        modifier = Modifier
            .size(220.dp)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = change.position.x - center.x
                            val dy = change.position.y - center.y
                            val radius = sqrt(dx * dx + dy * dy)
                            val outerRadius = size.width / 2f
                            val ringPx = ringThickness.toPx()
                            val innerRadius = outerRadius - ringPx

                            if (radius >= innerRadius) {
                                val angle = (atan2(dy, dx) * (180f / PI.toFloat()) + 360f) % 360f
                                onHueChange(angle)
                            } else {
                                val sqSize = innerRadius * 2f / sqrt(2f)
                                val sqLeft = center.x - sqSize / 2f
                                val sqTop = center.y - sqSize / 2f
                                val s = ((change.position.x - sqLeft) / sqSize).coerceIn(0f, 1f)
                                val v = (1f - (change.position.y - sqTop) / sqSize).coerceIn(0f, 1f)
                                onSVChange(s, v)
                            }
                        }
                    )
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f - 2.dp.toPx()
            val ringPx = ringThickness.toPx()
            val innerRadius = outerRadius - ringPx

            // Draw hue ring
            drawHueRing(center, outerRadius, innerRadius)

            // Draw SV square
            val sqSize = innerRadius * 2f / sqrt(2f)
            drawSVSquare(center, sqSize, hue)

            // Draw hue thumb on ring
            val hueAngleRad = hue * PI.toFloat() / 180f
            val thumbX = center.x + cos(hueAngleRad) * (innerRadius + ringPx / 2f)
            val thumbY = center.y + sin(hueAngleRad) * (innerRadius + ringPx / 2f)
            drawCircle(Color.White, radius = ringPx / 2f - 2.dp.toPx(), center = Offset(thumbX, thumbY))
            drawCircle(Color.hsv(hue, 1f, 1f), radius = ringPx / 2f - 5.dp.toPx(), center = Offset(thumbX, thumbY))

            // Draw SV thumb
            val sqLeft = center.x - sqSize / 2f
            val sqTop = center.y - sqSize / 2f
            val svX = sqLeft + saturation * sqSize
            val svY = sqTop + (1f - value) * sqSize
            drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(svX, svY))
            drawCircle(Color.hsv(hue, saturation, value), radius = 4.dp.toPx(), center = Offset(svX, svY))
        }
    }
}

private fun DrawScope.drawHueRing(center: Offset, outerRadius: Float, innerRadius: Float) {
    val steps = 360
    for (i in 0 until steps) {
        val startAngle = i.toFloat()
        val hueColor = Color.hsv(startAngle, 1f, 1f)
        drawArc(
            color = hueColor,
            startAngle = startAngle,
            sweepAngle = 1.5f,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = outerRadius - innerRadius)
        )
    }
}

private fun DrawScope.drawSVSquare(center: Offset, size: Float, hue: Float) {
    val steps = 32
    val left = center.x - size / 2f
    val top = center.y - size / 2f
    val cellW = size / steps

    for (col in 0 until steps) {
        val s = col.toFloat() / steps
        val brush = Brush.verticalGradient(
            colors = listOf(Color.hsv(hue, s, 1f), Color.hsv(hue, s, 0f)),
            startY = top,
            endY = top + size
        )
        drawRect(
            brush = brush,
            topLeft = Offset(left + col * cellW, top),
            size = Size(cellW + 0.5f, size)
        )
    }
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.White, Color.Transparent),
            startX = left, endX = left + size
        ),
        topLeft = Offset(left, top),
        size = Size(size, size)
    )
}

