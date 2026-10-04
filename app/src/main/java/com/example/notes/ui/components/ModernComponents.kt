package com.example.notes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notes.theme.DarkBackground
import com.example.notes.theme.NoteColors
import com.example.notes.theme.getNoteBackgroundColor

// ─────────────────────────────────────────────────────────────────────────────
// Design helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun isAppInDarkTheme(): Boolean = MaterialTheme.colorScheme.background == DarkBackground

/** Card / surface fill that respects the optional glassy UI mode. */
@Composable
fun cardSurfaceColor(glassy: Boolean): Color {
    val dark = isAppInDarkTheme()
    return if (glassy) {
        if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.60f)
    } else MaterialTheme.colorScheme.surface
}

/** Content color that respects glassy UI mode. */
@Composable
fun appContentColor(glassy: Boolean): Color {
    val dark = isAppInDarkTheme()
    return if (glassy) {
        if (dark) Color.White else Color(0xFF111827)
    } else MaterialTheme.colorScheme.onBackground
}

// ─────────────────────────────────────────────────────────────────────────────
// Circle icon button (used in top bars)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    containerColor: Color? = null,
    tint: Color? = null,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "circleBtnScale"
    )
    val bg = containerColor ?: MaterialTheme.colorScheme.surface
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (containerColor == null) 2.dp else 0.dp, CircleShape, clip = false,
                ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f))
            .clip(CircleShape)
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(),
                enabled = enabled,
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(size * 0.46f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick action card (dashboard 2x2 grid)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    glassy: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "quickActionScale"
    )
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(24.dp)
    val bg: Brush = if (highlighted) {
        Brush.linearGradient(listOf(primary.copy(alpha = 0.92f), primary))
    } else {
        Brush.linearGradient(listOf(cardSurfaceColor(glassy), cardSurfaceColor(glassy)))
    }
    val contentColor = if (highlighted) Color.White else MaterialTheme.colorScheme.onSurface
    val decoColor = if (highlighted) Color.White.copy(alpha = 0.12f) else accent.copy(alpha = 0.07f)

    Box(
        modifier = modifier
            .height(140.dp)
            .graphicsLayer {
                scaleX = scale; scaleY = scale
                rotationZ = if (highlighted) 2.5f else 0f
            }
            .shadow(
                elevation = if (highlighted) 14.dp else 0.dp,
                shape = shape,
                ambientColor = primary.copy(alpha = 0.4f),
                spotColor = primary.copy(alpha = 0.5f)
            )
            .clip(shape)
            .background(bg)
            .drawBehind {
                // Decorative soft circles in the corner
                drawCircle(decoColor, radius = size.width * 0.34f, center = Offset(size.width * 0.98f, size.height * 0.15f))
                drawCircle(decoColor, radius = size.width * 0.20f, center = Offset(size.width * 0.80f, size.height * 0.62f))
            }
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), onClick = onClick)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (highlighted) Color.White.copy(alpha = 0.22f) else accent.copy(alpha = 0.14f))
                    .then(
                        if (highlighted) Modifier.border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        else Modifier
                    )
            ) {
                Icon(icon, contentDescription = null, tint = if (highlighted) Color.White else accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = if (highlighted) 0.85f else 0.55f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Floating pill navigation bar
// ─────────────────────────────────────────────────────────────────────────────

data class PillNavItem(
    val icon: ImageVector,
    val label: String,
    val selected: Boolean = false,
    val isAction: Boolean = false,
    val tint: Color? = null,
    val onClick: () -> Unit
)

@Composable
fun FloatingPillNavBar(
    items: List<PillNavItem>,
    modifier: Modifier = Modifier,
    scrollPosition: Float? = null
) {
    val dark = isAppInDarkTheme()
    val pillColor = if (dark) Color(0xFF1E2433) else Color(0xFF1C2333)
    val tabItems = items.filter { !it.isAction }
    val actionItems = items.filter { it.isAction }

    val selectedIndex = tabItems.indexOfFirst { it.selected }.coerceAtLeast(0)
    val currentPos = scrollPosition ?: selectedIndex.toFloat()

    val itemSize = 48.dp
    val spacing = 6.dp
    val step = itemSize + spacing

    // Continuous scroll tracking with liquid stretch physics
    val pos = currentPos.coerceIn(0f, (tabItems.size - 1).coerceAtLeast(0).toFloat())
    val fraction = (pos - pos.toInt())
    val stretch = kotlin.math.sin(fraction * Math.PI).toFloat()
    val extraWidth = (stretch * 20).dp
    val blobWidth = itemSize + extraWidth
    val blobHeight = itemSize * (1f - stretch * 0.12f)
    val blobOffsetX = (pos * (step.value)).dp - (extraWidth / 2f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .shadow(
                20.dp,
                CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = Color.Black.copy(alpha = 0.45f)
            )
            .clip(CircleShape)
            .background(pillColor)
            .border(1.dp, Color.White.copy(alpha = if (dark) 0.12f else 0.08f), CircleShape)
            .padding(horizontal = 7.dp, vertical = 6.dp)
    ) {
        // Tab items area with liquid blob sliding underneath
        Box(
            modifier = Modifier
                .height(itemSize)
                .width((step * tabItems.size) - spacing)
        ) {
            // Liquidy blob indicator
            Box(
                modifier = Modifier
                    .offset(x = blobOffsetX)
                    .align(Alignment.CenterStart)
                    .width(blobWidth)
                    .height(blobHeight)
                    .shadow(
                        8.dp,
                        RoundedCornerShape(24.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                Color(0xFF8B5CF6)
                            )
                        )
                    )
            )

            // Tab icons on top of liquid blob
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                tabItems.forEachIndexed { index, item ->
                    val dist = kotlin.math.abs(pos - index)
                    val isUnderBlob = dist < 0.5f
                    val iconScale = 1f + ((1f - dist.coerceIn(0f, 1f)) * 0.12f)
                    val iconAlpha = 0.65f + ((1f - dist.coerceIn(0f, 1f)) * 0.35f)

                    val interaction = remember { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val buttonScale by animateFloatAsState(
                        targetValue = if (pressed) 0.88f else iconScale,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "pillTabScale"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(itemSize)
                            .graphicsLayer {
                                scaleX = buttonScale
                                scaleY = buttonScale
                            }
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = interaction,
                                indication = androidx.compose.material3.ripple(bounded = false, radius = 24.dp),
                                onClick = item.onClick
                            )
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isUnderBlob) Color.White else Color.White.copy(alpha = iconAlpha),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Action button (+) if present
        if (actionItems.isNotEmpty()) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(Color.White.copy(alpha = 0.14f))
            )
            Spacer(Modifier.width(6.dp))

            actionItems.forEach { actionItem ->
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (pressed) 0.86f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "pillActionScale"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(itemSize)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .shadow(6.dp, CircleShape, spotColor = Color.White.copy(alpha = 0.3f))
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(
                            interactionSource = interaction,
                            indication = androidx.compose.material3.ripple(bounded = false, radius = 24.dp),
                            onClick = actionItem.onClick
                        )
                ) {
                    Icon(
                        imageVector = actionItem.icon,
                        contentDescription = actionItem.label,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small action chip ("Add Tag", "Colour", ...)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SoftActionChip(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    glassy: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(if (active) accent.copy(alpha = 0.16f) else cardSurfaceColor(glassy))
            .border(1.dp, if (active) accent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page skins (lines / grid / dots)
// ─────────────────────────────────────────────────────────────────────────────

object NoteSkins {
    const val PLAIN = 0
    const val LINES = 1
    const val GRID = 2
    const val DOTS = 3
}

/** Draws a ruled / grid / dotted paper pattern behind content. */
fun Modifier.notePattern(
    skin: Int,
    color: Color,
    spacing: Dp = 28.dp,
    topOffset: Dp = 0.dp
): Modifier = if (skin == NoteSkins.PLAIN) this else this.drawBehind {
    val step = spacing.toPx()
    val start = topOffset.toPx()
    val stroke = 1.dp.toPx()
    when (skin) {
        NoteSkins.LINES -> {
            var y = start + step
            while (y < size.height) {
                drawLine(color, Offset(0f, y), Offset(size.width, y), stroke)
                y += step
            }
        }
        NoteSkins.GRID -> {
            var y = start
            while (y < size.height) {
                drawLine(color, Offset(0f, y), Offset(size.width, y), stroke)
                y += step
            }
            var x = 0f
            while (x < size.width) {
                drawLine(color, Offset(x, 0f), Offset(x, size.height), stroke)
                x += step
            }
        }
        NoteSkins.DOTS -> {
            val r = 1.6.dp.toPx()
            var y = start + step / 2
            while (y < size.height) {
                var x = step / 2
                while (x < size.width) {
                    drawCircle(color, r, Offset(x, y))
                    x += step
                }
                y += step
            }
        }
    }
}

/** "Skins" bottom sheet: note colours + lines and grids. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinsBottomSheet(
    selectedColor: Int,
    selectedSkin: Int,
    onColorSelected: (Int) -> Unit,
    onSkinSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)) }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Text(
                    "Done",
                    style = MaterialTheme.typography.titleMedium,
                    color = primary,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss)
                        .padding(6.dp)
                )
                Text("Skins", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.Center))
            }
            Spacer(Modifier.height(20.dp))
            SheetSectionLabel("Colours")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(NoteColors) { (index, _) ->
                    val selected = index == selectedColor
                    SkinTile(selected = selected, onClick = { onColorSelected(index) }, background = getNoteBackgroundColor(index)) {
                        // Three "text lines" preview
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(14.dp)) {
                            val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                            listOf(1f, 0.8f, 0.55f).forEach { w ->
                                Box(Modifier.fillMaxWidth(w).height(3.dp).clip(CircleShape).background(lineColor))
                            }
                        }
                        if (selected) {
                            Icon(
                                Icons.Default.Check, contentDescription = "Selected",
                                tint = primary,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(16.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            SheetSectionLabel("Lines and grids")
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                listOf(
                    NoteSkins.PLAIN to "Plain",
                    NoteSkins.LINES to "Lines",
                    NoteSkins.GRID to "Grid",
                    NoteSkins.DOTS to "Dots"
                ).forEach { (skin, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SkinTile(
                            selected = skin == selectedSkin,
                            onClick = { onSkinSelected(skin) },
                            background = getNoteBackgroundColor(selectedColor),
                            patternSkin = skin
                        ) {}
                        Spacer(Modifier.height(6.dp))
                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, bottom = 10.dp)
    )
}

@Composable
private fun SkinTile(
    selected: Boolean,
    onClick: () -> Unit,
    background: Color,
    patternSkin: Int = NoteSkins.PLAIN,
    content: @Composable BoxScope.() -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val scale by animateFloatAsState(if (selected) 1.05f else 1f, spring(dampingRatio = 0.55f), label = "skinScale")
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(background)
            .notePattern(patternSkin, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f), spacing = 11.dp)
            .border(
                BorderStroke(
                    if (selected) 2.5.dp else 1.dp,
                    if (selected) primary else MaterialTheme.colorScheme.outlineVariant
                ),
                shape
            )
            .clickable(onClick = onClick),
        content = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Photo source popup ("Take Photo" / "Photos" / "Scan text")
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PhotoSourceMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onScanText: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        offset = androidx.compose.ui.unit.DpOffset(0.dp, (-8).dp)
    ) {
        PhotoMenuRow(Icons.Outlined.PhotoCamera, "Take Photo") { onDismiss(); onTakePhoto() }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        PhotoMenuRow(Icons.Outlined.PhotoLibrary, "Photos") { onDismiss(); onPickPhoto() }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        PhotoMenuRow(Icons.Outlined.DocumentScanner, "Scan Text") { onDismiss(); onScanText() }
    }
}

@Composable
private fun PhotoMenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        modifier = Modifier.widthIn(min = 200.dp)
    )
}
