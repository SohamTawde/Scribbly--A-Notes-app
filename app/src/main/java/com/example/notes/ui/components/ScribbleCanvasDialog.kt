package com.example.notes.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.sqrt

// ── Data models ────────────────────────────────────────────────────────────────

data class ScribbleLine(
    val points: List<Offset>,
    val widths: List<Float>,          // per-point widths (pressure simulation)
    val color: Color,
    val opacity: Float,
    val brushType: BrushType,
    val isEraser: Boolean = false
)

data class CanvasPhoto(
    val bitmap: ImageBitmap,
    val offset: Offset,
    val size: Size,
    val isBackground: Boolean
)

enum class CanvasTool { BRUSH, ERASER, PHOTO_MOVE }

// ── ScribbleCanvasDialog ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScribbleCanvasDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    initialScribblePath: String? = null
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val canvasBgColor = if (isDark) Color(0xFF1C1C1E) else Color.White
    val studioBgColor = if (isDark) Color(0xFF141416) else MaterialTheme.colorScheme.background
    val surfaceBarColor = if (isDark) Color(0xFF18181A) else MaterialTheme.colorScheme.surface
    val contentTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val iconActiveTint = if (isDark) Color(0xFFD1D1D6) else MaterialTheme.colorScheme.onSurfaceVariant
    val iconDisabledTint = if (isDark) Color(0xFF48484A) else MaterialTheme.colorScheme.outlineVariant
    val dividerColor = if (isDark) Color(0xFF2C2C2E) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val lines = remember { mutableStateListOf<ScribbleLine>() }
    val photos = remember { mutableStateListOf<CanvasPhoto>() }
    val currentPoints = remember { mutableStateListOf<Offset>() }
    val currentWidths = remember { mutableStateListOf<Float>() }

    var selectedColor by remember { mutableStateOf(if (isDark) Color.White else Color.Black) }
    var brushOpacity by remember { mutableFloatStateOf(1f) }
    var selectedBrush by remember { mutableStateOf(BrushType.PEN) }
    var strokeWidth by remember { mutableFloatStateOf(BrushType.PEN.defaultStrokeWidth) }
    var currentTool by remember { mutableStateOf(CanvasTool.BRUSH) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showBrushPanel by remember { mutableStateOf(false) }
    var showPhotoSheet by remember { mutableStateOf(false) }
    var draggingPhotoIndex by remember { mutableIntStateOf(-1) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var photoStartOffset by remember { mutableStateOf(Offset.Zero) }

    // Velocity tracking for pressure
    var lastPointerPos by remember { mutableStateOf(Offset.Zero) }
    var smoothedWidth by remember { mutableFloatStateOf(strokeWidth) }

    // Toolbar stagger animation
    var toolbarVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { toolbarVisible = true }

    // Photo launchers
    val bgPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { loadBitmapFromUri(context, it)?.let { bmp ->
            photos.removeAll { p -> p.isBackground }
            photos.add(0, CanvasPhoto(
                bmp.asImageBitmap(), Offset.Zero,
                Size(canvasSize.width.toFloat().coerceAtLeast(100f), canvasSize.height.toFloat().coerceAtLeast(100f)),
                isBackground = true
            ))
        }}
    }

    val floatPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { loadBitmapFromUri(context, it)?.let { bmp ->
            val aspect = bmp.width.toFloat() / bmp.height.toFloat()
            val w = (canvasSize.width * 0.5f).coerceAtLeast(120f)
            photos.add(CanvasPhoto(
                bmp.asImageBitmap(),
                Offset(canvasSize.width * 0.15f, canvasSize.height * 0.15f),
                Size(w, w / aspect),
                isBackground = false
            ))
        }}
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        bmp?.let {
            val aspect = it.width.toFloat() / it.height.toFloat()
            val w = (canvasSize.width * 0.5f).coerceAtLeast(120f)
            photos.add(CanvasPhoto(
                it.asImageBitmap(),
                Offset(canvasSize.width * 0.15f, canvasSize.height * 0.15f),
                Size(w, w / aspect),
                isBackground = false
            ))
        }
    }


    // Photo source bottom sheet
    if (showPhotoSheet) {
        ModalBottomSheet(onDismissRequest = { showPhotoSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Add Photo",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                // Stagger animation for sheet items
                val items = listOf(
                    Triple(Icons.Default.PhotoCamera, "Take Photo", {
                        showPhotoSheet = false; cameraLauncher.launch(null)
                    }),
                    Triple(Icons.Default.PhotoLibrary, "Choose from Album", {
                        showPhotoSheet = false
                        floatPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }),
                    Triple(Icons.Default.Wallpaper, "Set as Background", {
                        showPhotoSheet = false
                        bgPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }),
                )
                items.forEachIndexed { index, (icon, label, action) ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(index * 80L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(200)) + slideInHorizontally(tween(200)) { -40 }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { action() }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    if (index < items.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = studioBgColor) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // ── Top Bar (Theme adaptive) ──────────────────────────────
                    Surface(
                        color = surfaceBarColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.ArrowBack, "Close", tint = contentTextColor)
                            }

                            Text(
                                "Canvas",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = contentTextColor,
                                modifier = Modifier.padding(start = 4.dp)
                            )

                            Spacer(Modifier.weight(1f))

                            // Move photos (shown when floating photos exist)
                            if (photos.any { !it.isBackground }) {
                                IconButton(onClick = {
                                    currentTool = if (currentTool == CanvasTool.PHOTO_MOVE) CanvasTool.BRUSH else CanvasTool.PHOTO_MOVE
                                }) {
                                    Icon(
                                        Icons.Default.OpenWith, "Move photo",
                                        tint = if (currentTool == CanvasTool.PHOTO_MOVE) MaterialTheme.colorScheme.primary
                                        else iconActiveTint
                                    )
                                }
                            }

                            // Add photo button
                            IconButton(onClick = { showPhotoSheet = true }) {
                                Icon(Icons.Default.AddPhotoAlternate, "Photo", tint = iconActiveTint)
                            }

                            // Clear canvas button
                            IconButton(
                                onClick = {
                                    lines.clear()
                                },
                                enabled = lines.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.Refresh, "Clear",
                                    tint = if (lines.isNotEmpty()) iconActiveTint
                                    else iconDisabledTint
                                )
                            }

                            // Undo button
                            IconButton(
                                onClick = { if (lines.isNotEmpty()) lines.removeAt(lines.size - 1) },
                                enabled = lines.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.Undo, "Undo",
                                    tint = if (lines.isNotEmpty()) iconActiveTint
                                    else iconDisabledTint
                                )
                            }

                            Spacer(Modifier.width(4.dp))

                            // Save checkmark
                            IconButton(
                                onClick = {
                                    val path = saveCanvasToBitmap(
                                        context, lines, photos,
                                        canvasSize.width.coerceAtLeast(1080),
                                        canvasSize.height.coerceAtLeast(1440),
                                        backgroundColor = canvasBgColor.toArgb()
                                    )
                                    if (path != null) onSave(path)
                                },
                                enabled = lines.isNotEmpty() || photos.isNotEmpty()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (lines.isNotEmpty() || photos.isNotEmpty()) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Done, "Save", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                    // ── Canvas drawing area ──────────────────────────────────
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(canvasBgColor)
                            .onSizeChanged { canvasSize = it }
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(currentTool, selectedBrush) {
                                    when (currentTool) {
                                        CanvasTool.BRUSH, CanvasTool.ERASER -> {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    currentPoints.clear()
                                                    currentWidths.clear()
                                                    currentPoints.add(offset)
                                                    currentWidths.add(strokeWidth)
                                                    lastPointerPos = offset
                                                    smoothedWidth = strokeWidth
                                                },
                                                onDragEnd = {
                                                    if (currentPoints.isNotEmpty()) {
                                                        lines.add(ScribbleLine(
                                                            points = currentPoints.toList(),
                                                            widths = currentWidths.toList(),
                                                            color = if (currentTool == CanvasTool.ERASER) canvasBgColor else selectedColor,
                                                            opacity = if (currentTool == CanvasTool.ERASER) 1f else brushOpacity,
                                                            brushType = selectedBrush,
                                                            isEraser = currentTool == CanvasTool.ERASER
                                                        ))
                                                        currentPoints.clear()
                                                        currentWidths.clear()
                                                    }
                                                },
                                                onDrag = { change, _ ->
                                                    change.consume()
                                                    val curr = change.position
                                                    val velocity = sqrt(
                                                        (curr.x - lastPointerPos.x) * (curr.x - lastPointerPos.x) +
                                                        (curr.y - lastPointerPos.y) * (curr.y - lastPointerPos.y)
                                                    )
                                                    val targetWidth = when {
                                                        currentTool == CanvasTool.ERASER -> strokeWidth * 3f
                                                        selectedBrush == BrushType.CALLIGRAPHY -> {
                                                            val dx = kotlin.math.abs(curr.x - lastPointerPos.x)
                                                            val dy = kotlin.math.abs(curr.y - lastPointerPos.y)
                                                            if (dx > dy) strokeWidth * 1.8f else strokeWidth * 0.5f
                                                        }
                                                        else -> (strokeWidth * (1f - (velocity / 80f).coerceIn(0f, 0.6f)))
                                                            .coerceIn(strokeWidth * 0.4f, strokeWidth * 1.4f)
                                                    }
                                                    smoothedWidth = smoothedWidth * 0.7f + targetWidth * 0.3f
                                                    currentPoints.add(curr)
                                                    currentWidths.add(smoothedWidth)
                                                    lastPointerPos = curr
                                                }
                                            )
                                        }
                                        CanvasTool.PHOTO_MOVE -> {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    val idx = photos.indexOfLast { p ->
                                                        !p.isBackground &&
                                                        offset.x >= p.offset.x && offset.x <= p.offset.x + p.size.width &&
                                                        offset.y >= p.offset.y && offset.y <= p.offset.y + p.size.height
                                                    }
                                                    draggingPhotoIndex = idx
                                                    if (idx >= 0) { dragStartOffset = offset; photoStartOffset = photos[idx].offset }
                                                },
                                                onDragEnd = { draggingPhotoIndex = -1 },
                                                onDrag = { change, _ ->
                                                    change.consume()
                                                    if (draggingPhotoIndex >= 0) {
                                                        val delta = change.position - dragStartOffset
                                                        photos[draggingPhotoIndex] = photos[draggingPhotoIndex].copy(
                                                            offset = Offset(
                                                                (photoStartOffset.x + delta.x).coerceIn(0f, canvasSize.width - photos[draggingPhotoIndex].size.width),
                                                                (photoStartOffset.y + delta.y).coerceIn(0f, canvasSize.height - photos[draggingPhotoIndex].size.height)
                                                            )
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                        ) {
                            photos.firstOrNull { it.isBackground }?.let { photo ->
                                drawIntoCanvas { c ->
                                    c.drawImageRect(photo.bitmap,
                                        dstOffset = androidx.compose.ui.unit.IntOffset.Zero,
                                        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
                                        paint = androidx.compose.ui.graphics.Paint())
                                }
                            }
                            photos.filter { !it.isBackground }.forEach { photo ->
                                drawIntoCanvas { c ->
                                    c.drawImageRect(photo.bitmap,
                                        dstOffset = androidx.compose.ui.unit.IntOffset(photo.offset.x.toInt(), photo.offset.y.toInt()),
                                        dstSize = androidx.compose.ui.unit.IntSize(photo.size.width.toInt(), photo.size.height.toInt()),
                                        paint = androidx.compose.ui.graphics.Paint())
                                }
                            }
                            lines.forEach { drawBrushStroke(it) }
                            if (currentPoints.isNotEmpty()) {
                                drawBrushStroke(ScribbleLine(
                                    points = currentPoints.toList(),
                                    widths = currentWidths.toList(),
                                    color = if (currentTool == CanvasTool.ERASER) canvasBgColor else selectedColor,
                                    opacity = if (currentTool == CanvasTool.ERASER) 1f else brushOpacity,
                                    brushType = selectedBrush,
                                    isEraser = currentTool == CanvasTool.ERASER
                                ))
                            }
                        }

                        if (currentTool == CanvasTool.PHOTO_MOVE) {
                            photos.filter { !it.isBackground }.forEach { photo ->
                                Box(modifier = Modifier
                                    .offset(photo.offset.x.dp, photo.offset.y.dp)
                                    .size(photo.size.width.dp, photo.size.height.dp)
                                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(4.dp)))
                            }
                        }
                    }

                    // ── Subtle Stroke Size Slider Bar ────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0xFF1E1E20) else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Size",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = iconActiveTint
                        )
                        Spacer(Modifier.width(10.dp))
                        Slider(
                            value = strokeWidth,
                            onValueChange = { strokeWidth = it },
                            valueRange = 2f..48f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = if (isDark) Color.White else MaterialTheme.colorScheme.primary,
                                activeTrackColor = selectedColor.copy(alpha = 0.8f),
                                inactiveTrackColor = if (isDark) Color(0xFF38383B) else MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .size(strokeWidth.coerceIn(4f, 32f).dp)
                                .clip(CircleShape)
                                .background(
                                    if (currentTool == CanvasTool.ERASER) canvasBgColor
                                    else selectedColor.copy(alpha = brushOpacity)
                                )
                                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.25f), CircleShape)
                        )
                    }

                    // Divider before pen dock
                    HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                    // ── Realistic Stationery Pen Dock (Matches Image 1) ─────
                    Surface(
                        color = surfaceBarColor,
                        tonalElevation = 10.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(105.dp)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            // 1. Close button [✕] (Image 1)
                            CircleIconButton(
                                icon = Icons.Default.Close,
                                contentDescription = "Close canvas",
                                onClick = onDismiss,
                                containerColor = if (isDark) Color(0xFF2E2E32) else MaterialTheme.colorScheme.surfaceVariant,
                                tint = contentTextColor,
                                size = 40.dp,
                                modifier = Modifier.padding(bottom = 14.dp)
                            )

                            // 2. Rainbow Color Swatch Circle (Image 1)
                            RainbowColorSwatch(
                                selectedColor = selectedColor,
                                opacity = brushOpacity,
                                onClick = { showColorPicker = !showColorPicker },
                                modifier = Modifier.padding(bottom = 14.dp)
                            )

                            // 3. Ballpoint Pen (PEN)
                            RealisticPenItem(
                                pen = PenInstrument.BALLPOINT,
                                isSelected = (currentTool == CanvasTool.BRUSH && selectedBrush == BrushType.PEN),
                                tipColor = selectedColor,
                                onClick = {
                                    selectedBrush = BrushType.PEN
                                    strokeWidth = BrushType.PEN.defaultStrokeWidth
                                    brushOpacity = BrushType.PEN.defaultOpacity
                                    currentTool = CanvasTool.BRUSH
                                }
                            )

                            // 4. Fountain Pen (CALLIGRAPHY)
                            RealisticPenItem(
                                pen = PenInstrument.FOUNTAIN,
                                isSelected = (currentTool == CanvasTool.BRUSH && selectedBrush == BrushType.CALLIGRAPHY),
                                tipColor = selectedColor,
                                onClick = {
                                    selectedBrush = BrushType.CALLIGRAPHY
                                    strokeWidth = BrushType.CALLIGRAPHY.defaultStrokeWidth
                                    brushOpacity = BrushType.CALLIGRAPHY.defaultOpacity
                                    currentTool = CanvasTool.BRUSH
                                }
                            )

                            // 5. Wooden Pencil (PENCIL)
                            RealisticPenItem(
                                pen = PenInstrument.PENCIL,
                                isSelected = (currentTool == CanvasTool.BRUSH && selectedBrush == BrushType.PENCIL),
                                tipColor = selectedColor,
                                onClick = {
                                    selectedBrush = BrushType.PENCIL
                                    strokeWidth = BrushType.PENCIL.defaultStrokeWidth
                                    brushOpacity = BrushType.PENCIL.defaultOpacity
                                    currentTool = CanvasTool.BRUSH
                                }
                            )

                            // 6. Highlighter Marker (MARKER)
                            RealisticPenItem(
                                pen = PenInstrument.HIGHLIGHTER,
                                isSelected = (currentTool == CanvasTool.BRUSH && selectedBrush == BrushType.MARKER),
                                tipColor = selectedColor,
                                onClick = {
                                    selectedBrush = BrushType.MARKER
                                    strokeWidth = BrushType.MARKER.defaultStrokeWidth
                                    brushOpacity = BrushType.MARKER.defaultOpacity
                                    currentTool = CanvasTool.BRUSH
                                }
                            )

                            // 7. Eraser Tool (ERASER)
                            RealisticPenItem(
                                pen = PenInstrument.ERASER,
                                isSelected = (currentTool == CanvasTool.ERASER),
                                tipColor = if (isDark) Color(0xFF3A3A3C) else Color.White,
                                onClick = {
                                    currentTool = CanvasTool.ERASER
                                    strokeWidth = 24f
                                }
                            )
                        }
                    }
                }

                // ── Color Picker Card Overlay (Floating above pen dock, Image 5) ──
                AnimatedVisibility(
                    visible = showColorPicker,
                    enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { it / 2 },
                    exit = fadeOut(tween(180)) + slideOutVertically(tween(200)) { it / 2 },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null,
                                onClick = { showColorPicker = false }
                            ),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 115.dp)
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null,
                                    onClick = {}
                                )
                        ) {
                            RingColorPickerCard(
                                initialColor = selectedColor,
                                initialOpacity = brushOpacity,
                                onColorSelected = { color, opacity ->
                                    selectedColor = color
                                    brushOpacity = opacity
                                    showColorPicker = false
                                },
                                onDismiss = { showColorPicker = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Realistic Stationery Pen Types (Image 1) ──────────────────────────────────

enum class PenInstrument {
    BALLPOINT,
    FOUNTAIN,
    PENCIL,
    HIGHLIGHTER,
    ERASER
}

@Composable
private fun RealisticPenItem(
    pen: PenInstrument,
    isSelected: Boolean,
    tipColor: Color,
    onClick: () -> Unit
) {
    val offsetY by animateDpAsState(
        targetValue = if (isSelected) (-8).dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "pen_offset"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset(y = offsetY)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Canvas(
            modifier = Modifier
                .width(when (pen) {
                    PenInstrument.HIGHLIGHTER -> 34.dp
                    PenInstrument.BALLPOINT, PenInstrument.FOUNTAIN -> 26.dp
                    PenInstrument.PENCIL -> 22.dp
                    PenInstrument.ERASER -> 24.dp
                })
                .height(86.dp)
        ) {
            val w = size.width
            val h = size.height

            when (pen) {
                PenInstrument.BALLPOINT -> {
                    // Needle ballpoint pen (matches Image 1 Pen 1)
                    val barrelTop = h * 0.32f
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF32363C), Color(0xFF454A53), Color(0xFF26282D))
                        ),
                        topLeft = Offset(0f, barrelTop),
                        size = Size(w, h - barrelTop),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    // White vertical slit button on barrel
                    val slitW = 3.5.dp.toPx()
                    val slitH = 18.dp.toPx()
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset((w - slitW) / 2f, barrelTop + 14.dp.toPx()),
                        size = Size(slitW, slitH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    // Silver metallic tapered cone
                    val conePath = Path().apply {
                        moveTo(w * 0.12f, barrelTop)
                        lineTo(w * 0.36f, h * 0.12f)
                        lineTo(w * 0.64f, h * 0.12f)
                        lineTo(w * 0.88f, barrelTop)
                        close()
                    }
                    drawPath(
                        conePath,
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFB0BEC5), Color(0xFFECEFF1), Color(0xFF78909C))
                        )
                    )
                    // Fine needle ballpoint tip
                    val tipW = 2.5.dp.toPx()
                    drawRoundRect(
                        color = Color(0xFF263238),
                        topLeft = Offset((w - tipW) / 2f, 2.dp.toPx()),
                        size = Size(tipW, h * 0.12f - 2.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                    // Ball point shiny reflection
                    drawCircle(
                        color = Color.White,
                        radius = 1.2.dp.toPx(),
                        center = Offset(w / 2f, 2.dp.toPx())
                    )
                }

                PenInstrument.FOUNTAIN -> {
                    // Fountain pen with split metallic nib (matches Image 1 Pen 2)
                    val barrelTop = h * 0.36f
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF2C2F34), Color(0xFF3F444C), Color(0xFF232529))
                        ),
                        topLeft = Offset(0f, barrelTop),
                        size = Size(w, h - barrelTop),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    // White vertical accent slit
                    val slitW = 3.5.dp.toPx()
                    val slitH = 16.dp.toPx()
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset((w - slitW) / 2f, barrelTop + 14.dp.toPx()),
                        size = Size(slitW, slitH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    // Silver collar ring
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF90A4AE), Color(0xFFECEFF1), Color(0xFF78909C))
                        ),
                        topLeft = Offset(w * 0.08f, barrelTop - 4.dp.toPx()),
                        size = Size(w * 0.84f, 4.dp.toPx())
                    )
                    // Metallic triangular fountain nib with curved shoulders
                    val nibPath = Path().apply {
                        moveTo(w * 0.16f, barrelTop - 4.dp.toPx())
                        cubicTo(w * 0.12f, h * 0.22f, w * 0.34f, h * 0.10f, w * 0.5f, 3.dp.toPx())
                        cubicTo(w * 0.66f, h * 0.10f, w * 0.88f, h * 0.22f, w * 0.84f, barrelTop - 4.dp.toPx())
                        close()
                    }
                    drawPath(
                        nibPath,
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFB0BEC5), Color(0xFFFFFFFF), Color(0xFF78909C))
                        )
                    )
                    // Nib center slit line
                    drawLine(
                        color = Color(0xFF37474F),
                        start = Offset(w / 2f, h * 0.19f),
                        end = Offset(w / 2f, 3.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                    // Breather hole
                    drawCircle(
                        color = Color(0xFF37474F),
                        radius = 1.5.dp.toPx(),
                        center = Offset(w / 2f, h * 0.19f)
                    )
                }

                PenInstrument.PENCIL -> {
                    // Wooden pencil (matches Image 1 Pen 3)
                    val barrelTop = h * 0.38f
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF26292E), Color(0xFF383C42), Color(0xFF212327))
                        ),
                        topLeft = Offset(0f, barrelTop),
                        size = Size(w, h - barrelTop),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    // Natural wood sharpened cone collar
                    val woodPath = Path().apply {
                        moveTo(0f, barrelTop)
                        lineTo(w * 0.34f, h * 0.14f)
                        lineTo(w * 0.66f, h * 0.14f)
                        lineTo(w, barrelTop)
                        close()
                    }
                    drawPath(
                        woodPath,
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFD4B595), Color(0xFFE8CFB5), Color(0xFFC7A583))
                        )
                    )
                    // White/Graphite lead tip cone
                    val leadPath = Path().apply {
                        moveTo(w * 0.34f, h * 0.14f)
                        lineTo(w * 0.5f, 2.dp.toPx())
                        lineTo(w * 0.66f, h * 0.14f)
                        close()
                    }
                    drawPath(leadPath, Color(0xFFF0F0F2))
                }

                PenInstrument.HIGHLIGHTER -> {
                    // Chunky chisel tip marker (matches Image 1 Pen 4)
                    val barrelTop = h * 0.32f
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF32353B), Color(0xFF454952), Color(0xFF26282D))
                        ),
                        topLeft = Offset(0f, barrelTop),
                        size = Size(w, h - barrelTop),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    // Stepped neck
                    val neckW = w * 0.62f
                    val neckTop = h * 0.22f
                    drawRoundRect(
                        color = Color(0xFF26282D),
                        topLeft = Offset((w - neckW) / 2f, neckTop),
                        size = Size(neckW, barrelTop - neckTop),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                    // Slanted chisel felt tip (bright yellow)
                    val tipPath = Path().apply {
                        moveTo(w * 0.22f, neckTop)
                        lineTo(w * 0.24f, h * 0.10f)
                        lineTo(w * 0.76f, 3.dp.toPx())
                        lineTo(w * 0.78f, neckTop)
                        close()
                    }
                    drawPath(
                        tipPath,
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFFFD500), Color(0xFFFFEB3B), Color(0xFFFFC107))
                        )
                    )
                }

                PenInstrument.ERASER -> {
                    // Eraser stylus tool (matches Image 1 Pen 5)
                    val barrelTop = h * 0.34f
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF2D3035), Color(0xFF3C4148), Color(0xFF222428))
                        ),
                        topLeft = Offset(0f, barrelTop),
                        size = Size(w, h - barrelTop),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                    // Dashed selection loop at tip
                    drawCircle(
                        color = Color(0xFFB0BEC5),
                        radius = 8.dp.toPx(),
                        center = Offset(w / 2f, h * 0.18f),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Active indicator dot
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                )
        )
    }
}

// ── Rainbow Color Swatch (Matches Image 1) ────────────────────────────────────

@Composable
private fun RainbowColorSwatch(
    selectedColor: Color,
    opacity: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Rainbow sweep gradient ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rainbowColors = listOf(
                Color(0xFFFF0000), Color(0xFFFF8800), Color(0xFFFFFF00),
                Color(0xFF00FF00), Color(0xFF00FFFF), Color(0xFF0000FF),
                Color(0xFFFF00FF), Color(0xFFFF0000)
            )
            drawCircle(
                brush = Brush.sweepGradient(rainbowColors),
                radius = size.minDimension / 2f - 1.dp.toPx(),
                style = Stroke(width = 3.5.dp.toPx())
            )
        }
        // Inner selected color circle
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(selectedColor.copy(alpha = opacity))
                .border(1.dp, if (isAppInDarkTheme()) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.25f), CircleShape)
        )
    }
}

// ── Stagger animation wrapper ──────────────────────────────────────────────────

@Composable
private fun StaggeredToolbarItem(
    visible: Boolean,
    delayMs: Int,
    content: @Composable () -> Unit
) {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(delayMs.toLong())
            show = true
        }
    }
    AnimatedVisibility(
        visible = show,
        enter = fadeIn(tween(200)) + scaleIn(tween(200, easing = FastOutSlowInEasing), initialScale = 0.7f)
    ) {
        content()
    }
}

// ── Draw brush stroke ──────────────────────────────────────────────────────────

private fun DrawScope.drawBrushStroke(line: ScribbleLine) {
    if (line.points.size < 2) return
    val paintAlpha = line.opacity

    when {
        line.isEraser -> {
            val path = buildSmoothPath(line.points)
            drawPath(path, line.color, style = Stroke(
                width = (line.widths.average().toFloat()) * 3f,
                cap = StrokeCap.Round, join = StrokeJoin.Round
            ))
        }
        line.brushType == BrushType.WATERCOLOR -> {
            // Multiple overlapping translucent strokes for wet look
            repeat(3) { pass ->
                val path = buildSmoothPath(line.points)
                drawPath(
                    path, line.color.copy(alpha = paintAlpha * 0.35f),
                    style = Stroke(
                        width = line.widths.average().toFloat() * (1f + pass * 0.3f),
                        cap = StrokeCap.Round, join = StrokeJoin.Round
                    )
                )
            }
        }
        line.brushType == BrushType.PENCIL -> {
            // Draw with slightly jittery, thin paths for pencil texture
            for (i in 1 until line.points.size) {
                val prev = line.points[i - 1]
                val curr = line.points[i]
                val w = line.widths.getOrElse(i) { line.widths.lastOrNull() ?: 4f }
                // Main line
                drawLine(
                    line.color.copy(alpha = paintAlpha * 0.85f),
                    prev, curr, strokeWidth = w,
                    cap = StrokeCap.Butt
                )
                // Texture parallel line (offset slightly)
                if (i % 3 == 0) {
                    drawLine(
                        line.color.copy(alpha = paintAlpha * 0.25f),
                        Offset(prev.x + 1.5f, prev.y + 0.5f),
                        Offset(curr.x + 1.5f, curr.y + 0.5f),
                        strokeWidth = w * 0.4f, cap = StrokeCap.Butt
                    )
                }
            }
        }
        else -> {
            // Pen, Marker, Calligraphy — variable-width strokes
            for (i in 1 until line.points.size) {
                val prev = line.points[i - 1]
                val curr = line.points[i]
                val w = line.widths.getOrElse(i) { line.widths.lastOrNull() ?: line.brushType.defaultStrokeWidth }
                drawLine(
                    line.color.copy(alpha = paintAlpha),
                    prev, curr,
                    strokeWidth = w,
                    cap = line.brushType.strokeCap
                )
            }
        }
    }
}

private fun buildSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        path.cubicTo(
            prev.x + (curr.x - prev.x) / 3f, prev.y + (curr.y - prev.y) / 3f,
            curr.x - (curr.x - prev.x) / 3f, curr.y - (curr.y - prev.y) / 3f,
            curr.x, curr.y
        )
    }
    return path
}

// ── Bitmap helpers ─────────────────────────────────────────────────────────────

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) { null }
}

private fun saveCanvasToBitmap(
    context: Context,
    lines: List<ScribbleLine>,
    photos: List<CanvasPhoto>,
    canvasWidth: Int,
    canvasHeight: Int,
    backgroundColor: Int = android.graphics.Color.WHITE
): String? {
    return try {
        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(backgroundColor)

        // Draw background
        photos.firstOrNull { it.isBackground }?.let { p ->
            canvas.drawBitmap(p.bitmap.asAndroidBitmap(),
                android.graphics.Rect(0, 0, p.bitmap.width, p.bitmap.height),
                android.graphics.RectF(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat()), null)
        }
        // Draw floating photos
        photos.filter { !it.isBackground }.forEach { p ->
            canvas.drawBitmap(p.bitmap.asAndroidBitmap(),
                android.graphics.Rect(0, 0, p.bitmap.width, p.bitmap.height),
                android.graphics.RectF(p.offset.x, p.offset.y, p.offset.x + p.size.width, p.offset.y + p.size.height), null)
        }
        // Draw strokes
        val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        lines.forEach { line ->
            paint.color = if (line.isEraser) backgroundColor else line.color.copy(alpha = line.opacity).toArgb()
            paint.xfermode = null
            val path = android.graphics.Path()
            if (line.points.isNotEmpty()) {
                path.moveTo(line.points.first().x, line.points.first().y)
                for (i in 1 until line.points.size) {
                    paint.strokeWidth = (line.widths.getOrElse(i) { line.brushType.defaultStrokeWidth }) * 2f
                    path.lineTo(line.points[i].x, line.points[i].y)
                }
                canvas.drawPath(path, paint)
            }
        }

        val dir = File(context.filesDir, "scribbles").also { if (!it.exists()) it.mkdirs() }
        val file = File(dir, "scribble_${UUID.randomUUID()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        file.absolutePath
    } catch (e: Exception) { e.printStackTrace(); null }
}
