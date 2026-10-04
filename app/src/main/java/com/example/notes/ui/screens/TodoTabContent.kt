package com.example.notes.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notes.data.local.TodoEntity
import com.example.notes.ui.components.CircleIconButton
import com.example.notes.ui.components.isAppInDarkTheme
import com.example.notes.ui.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

// ─────────────────────────────────────────────────────────────────────────────
// Sticky Notes Pinboard ("Today's Notes")
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TodoTabContent(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onAdd: () -> Unit = {}
) {
    val context = LocalContext.current
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    val isDark = isAppInDarkTheme()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var todoToDelete by remember { mutableStateOf<TodoEntity?>(null) }

    // Stable order (newest first). The DB sorts completed tasks to the bottom, which made
    // cards jump between columns on every tap, so the user would hit the wrong note.
    val filteredTodos = remember(todos, searchQuery) {
        val base = todos.sortedByDescending { it.id }
        if (searchQuery.isBlank()) base
        else base.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            (it.locationName?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    val completedCount = remember(todos) { todos.count { it.isCompleted } }
    val totalCount = todos.size

    val boardBg = if (isDark) Color(0xFF13151D) else Color(0xFFFBFBFC)
    val headerTextColor = if (isDark) Color.White else Color(0xFF1E2028)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(boardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
        ) {
            // ── Top Header matching the mockup ────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp)
            ) {
                // Circular back button [<]
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E222D) else Color.White)
                        .border(
                            1.dp,
                            if (isDark) Color(0xFF2C3242) else Color(0xFFE5E7EB),
                            CircleShape
                        )
                        .clickable { onBack() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Notes",
                        tint = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E2028),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Circular search button with yellow/golden ring (from mockup)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E222D) else Color.White)
                            .border(
                                1.5.dp,
                                Color(0xFFFACC15), // Distinctive golden-yellow outline
                                CircleShape
                            )
                            .clickable {
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) searchQuery = ""
                            }
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isDark) Color(0xFFFACC15) else Color(0xFFCA8A04),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Add sticky task button [+]
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .shadow(4.dp, CircleShape)
                            .clickable {
                                onAdd()
                                showAddTaskDialog = true
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Sticky Note",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ── Inline Search Input (Expands when search icon tapped) ───────
            AnimatedVisibility(
                visible = isSearchActive,
                enter = fadeIn(tween(180)) + expandVertically(tween(200)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(180))
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search sticky notes...", color = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) Color(0xFF1E222D) else Color.White,
                        unfocusedContainerColor = if (isDark) Color(0xFF1E222D) else Color.White,
                        focusedBorderColor = Color(0xFFFACC15),
                        unfocusedBorderColor = if (isDark) Color(0xFF2C3242) else Color(0xFFE5E7EB)
                    ),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Clear", tint = Color.Gray)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )
            }

            // ── Title: "Today's Notes" ──────────────────────────────────────
            Column(modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)) {
                Text(
                    text = "Today's Notes",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = headerTextColor
                )
                if (totalCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (completedCount == totalCount) Color(0xFF10B981).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (completedCount == totalCount) "All $totalCount completed! 🎉"
                                else "$completedCount of $totalCount completed",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = if (completedCount == totalCount) Color(0xFF059669)
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // ── Pinboard Canvas (Organic Sticky Notes) ─────────────────────
            if (filteredTodos.isEmpty()) {
                // Empty Pinboard State
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStickyNote(isDark = isDark, onAdd = {
                        onAdd()
                        showAddTaskDialog = true
                    })
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalItemSpacing = 18.dp,
                    contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(filteredTodos, key = { _, todo -> todo.id }) { index, todo ->
                        StickyNoteCard(
                            todo = todo,
                            index = index,
                            isDark = isDark,
                            onToggleComplete = { viewModel.toggleTodoCompletion(todo) },
                            onDelete = { todoToDelete = todo }
                        )
                    }
                }
            }
        }

        // Add task dialog triggered locally
        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismiss = { showAddTaskDialog = false },
                onSave = { title, dueDate, selectedAddress, lat, lng ->
                    val newTodo = TodoEntity(
                        title = title,
                        dueDate = dueDate,
                        locationName = selectedAddress,
                        latitude = lat,
                        longitude = lng
                    )
                    viewModel.saveTodo(newTodo)
                    showAddTaskDialog = false
                },
                viewModel = viewModel
            )
        }

        // Delete confirmation dialog
        todoToDelete?.let { todo ->
            AlertDialog(
                onDismissRequest = { todoToDelete = null },
                title = { Text("Delete Sticky Note?") },
                text = { Text("Are you sure you want to remove \"${todo.title}\" from your board?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteTodo(todo)
                            todoToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { todoToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sticky Note Card (Matches organic reference image)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StickyNoteCard(
    todo: TodoEntity,
    index: Int,
    isDark: Boolean,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val idNum = todo.id.toInt().absoluteValue
    // Style is derived ONLY from the id so it never changes when the list re-orders
    val palette = StickyPalettes[idNum % StickyPalettes.size]
    val fastener = FastenerType.entries[(idNum * 3 + 1) % FastenerType.entries.size]
    val angle = StickyAngles[(idNum * 5 + 2) % StickyAngles.size]
    val font = getStickyFont(idNum.toLong())

    val paperColor = if (isDark) palette.bgDark else palette.bgLight
    val textColor = if (isDark) palette.textDark else palette.textLight

    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "stickyScale"
    )

    // ── Floppy paper physics ─────────────────────────────────────────
    // Notes hang from their fastener, so rotate around that point.
    val pivot = when (fastener) {
        FastenerType.PAPERCLIP -> TransformOrigin(0.82f, 0f)
        FastenerType.CORNER_TAPE -> TransformOrigin(0.95f, 0f)
        FastenerType.BINDER_CLIP -> TransformOrigin(0.5f, 0f)
        else -> TransformOrigin(0.5f, 0.02f)
    }
    val swayAmp = if (todo.isCompleted) 0.6f else 1.6f
    val infinite = rememberInfiniteTransition(label = "stickyFlop")
    val sway by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000 + (idNum % 5) * 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset((idNum % 7) * 230)
        ),
        label = "stickySway"
    )
    val wobble = remember { Animatable(0f) }
    val drop = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Entry: note drops in and flops on its pin until it settles
    LaunchedEffect(todo.id) {
        delay((index % 8) * 70L)
        launch { drop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
        wobble.snapTo(if (idNum % 2 == 0) 12f else -12f)
        wobble.animateTo(0f, spring(dampingRatio = 0.22f, stiffness = 90f))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                transformOrigin = pivot
                cameraDistance = 14f * density
                scaleX = scale
                scaleY = scale
                rotationZ = angle + sway * swayAmp + wobble.value
                rotationX = sway * 2.5f + wobble.value * 0.9f
                translationY = (1f - drop.value) * -60.dp.toPx()
                alpha = drop.value.coerceIn(0f, 1f)
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    scope.launch {
                        wobble.snapTo(if (sway > 0f) -9f else 9f)
                        wobble.animateTo(0f, spring(dampingRatio = 0.2f, stiffness = 110f))
                    }
                    onToggleComplete()
                }
            )
            .padding(top = 10.dp) // Room for push pin / washi tape overflow
    ) {
        // ── Sticky Paper Card ─────────────────────────────────────────
        Card(
            shape = StickyPaperShape,
            colors = CardDefaults.cardColors(containerColor = paperColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (todo.isCompleted) 2.dp else 7.dp,
                    shape = StickyPaperShape,
                    ambientColor = Color.Black.copy(alpha = if (isDark) 0.45f else 0.12f),
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.55f else 0.2f)
                )
                .drawWithContent {
                    drawContent()
                    // Soft lift shading along the curled bottom edge
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.78f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.07f)
                        )
                    )
                    // Curled bottom-right corner highlight + shadow
                    val c = size.minDimension * 0.22f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(size.width, size.height),
                            radius = c
                        ),
                        radius = c,
                        center = Offset(size.width, size.height)
                    )
                    // Paper sheen from the top light source
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.18f),
                            0.25f to Color.Transparent
                        )
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .padding(top = 6.dp)
            ) {
                // Top row with options/delete icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(24.dp)
                            .offset(x = 4.dp, y = (-4).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = textColor.copy(alpha = 0.35f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Title of sticky note (with organic casual / handwriting font)
                Text(
                    text = todo.title,
                    fontFamily = font,
                    fontWeight = if (font == FontFamily.Cursive) FontWeight.Normal else FontWeight.SemiBold,
                    fontSize = if (font == FontFamily.Cursive) 19.sp else 16.sp,
                    lineHeight = if (font == FontFamily.Cursive) 22.sp else 20.sp,
                    color = if (todo.isCompleted) textColor.copy(alpha = 0.45f) else textColor,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Time / Date indicator (matches "06:30 PM", "09:30 PM" from mockup)
                if (todo.dueDate != null) {
                    val dateObj = Date(todo.dueDate)
                    val isToday = isSameDay(dateObj, Date())
                    val formatted = if (isToday) timeFormat.format(dateObj) else "${dateFormat.format(dateObj)}, ${timeFormat.format(dateObj)}"

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = "Due time",
                            tint = palette.accent.copy(alpha = 0.8f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formatted,
                            fontFamily = font,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.accent.copy(alpha = 0.9f)
                        )
                    }
                }

                // Location badge if exists
                todo.locationName?.let { loc ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Location",
                            tint = palette.accent.copy(alpha = 0.75f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = loc,
                            fontFamily = font,
                            fontSize = 11.sp,
                            color = palette.accent.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // ── Completed Stamp ───────────────────────────────────────
                if (todo.isCompleted) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .graphicsLayer { rotationZ = -12f }
                            .border(1.5.dp, Color(0xFFDC2626).copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DONE ✓",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }
        }

        // ── Fasteners Overlay (Pins, Tape, Clips, Magnets) ────────────
        val pinColor = PinColors[(idNum * 7) % PinColors.size]
        when (fastener) {
            FastenerType.PUSH_PIN -> {
                PushPinFastener(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-10).dp),
                    pinColor = pinColor
                )
            }
            FastenerType.THUMBTACK -> {
                ThumbtackFastener(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-6).dp)
                )
            }
            FastenerType.BINDER_CLIP -> {
                BinderClipFastener(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-14).dp)
                )
            }
            FastenerType.MAGNET -> {
                MagnetFastener(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-8).dp),
                    color = pinColor
                )
            }
            FastenerType.HEART_PIN -> {
                HeartPinFastener(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-10).dp)
                )
            }
            FastenerType.WASHI_TAPE -> {
                WashiTapeFastener(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-6).dp),
                    tapeColor = if (idNum % 2 == 0) Color(0xCCF87171) else Color(0xCCFB923C),
                    rotation = if (idNum % 2 == 0) -2.5f else 3f
                )
            }
            FastenerType.PAPERCLIP -> {
                PaperclipFastener(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-16).dp, y = (-8).dp),
                    clipColor = if (idNum % 2 == 0) Color(0xFF8B5CF6) else Color(0xFFEF4444)
                )
            }
            FastenerType.CORNER_TAPE -> {
                WashiTapeFastener(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp),
                    tapeColor = Color(0xCC34D399),
                    rotation = 35f
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fastener Components (Push Pin, Washi Tape, Paperclip)
// ─────────────────────────────────────────────────────────────────────────────

enum class FastenerType {
    PUSH_PIN,
    WASHI_TAPE,
    THUMBTACK,
    PAPERCLIP,
    MAGNET,
    CORNER_TAPE,
    BINDER_CLIP,
    HEART_PIN
}

val PinColors = listOf(
    Color(0xFFEF4444), // red
    Color(0xFF3B82F6), // blue
    Color(0xFF22C55E), // green
    Color(0xFFF59E0B), // amber
    Color(0xFF8B5CF6), // violet
    Color(0xFFEC4899)  // pink
)

/** Paper shape with rounded corners, slightly bowed sides and a lifted, curled bottom edge. */
val StickyPaperShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val r = minOf(w, h) * 0.08f
    val curl = minOf(w, h) * 0.18f
    moveTo(r, 0f)
    lineTo(w - r, 0f)
    quadraticBezierTo(w, 0f, w, r)
    quadraticBezierTo(w + w * 0.012f, h * 0.5f, w, h - curl)
    quadraticBezierTo(w * 0.99f, h * 0.99f, w - curl, h)
    quadraticBezierTo(w * 0.5f, h * 0.975f, r, h)
    quadraticBezierTo(0f, h, 0f, h - r)
    quadraticBezierTo(-w * 0.012f, h * 0.5f, 0f, r)
    quadraticBezierTo(0f, 0f, r, 0f)
    close()
}

@Composable
fun ThumbtackFastener(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val rad = size.minDimension / 2.4f
        drawCircle(Color.Black.copy(alpha = 0.22f), rad, c + Offset(2.dp.toPx(), 2.5.dp.toPx()))
        drawCircle(
            brush = Brush.linearGradient(
                listOf(Color(0xFFF1F5F9), Color(0xFF94A3B8), Color(0xFFE2E8F0), Color(0xFF64748B)),
                start = Offset.Zero, end = Offset(size.width, size.height)
            ),
            radius = rad, center = c
        )
        drawCircle(Color(0xFF475569), rad * 0.62f, c, style = Stroke(1.dp.toPx()))
        drawCircle(Color(0xFF334155), rad * 0.18f, c)
        drawCircle(Color.White.copy(alpha = 0.8f), rad * 0.2f, c - Offset(rad * 0.45f, rad * 0.45f))
    }
}

@Composable
fun BinderClipFastener(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(40.dp, 30.dp)) {
        val w = size.width
        val h = size.height
        val metal = Color(0xFFCBD5E1)
        val wire = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
        // Silver wire handles
        drawLine(metal, Offset(w * 0.28f, h * 0.55f), Offset(w * 0.18f, 0f), wire.width, StrokeCap.Round)
        drawLine(metal, Offset(w * 0.72f, h * 0.55f), Offset(w * 0.82f, 0f), wire.width, StrokeCap.Round)
        drawLine(metal, Offset(w * 0.18f, 0f), Offset(w * 0.82f, 0f), wire.width, StrokeCap.Round)
        // Shadow
        drawRoundRect(
            Color.Black.copy(alpha = 0.2f),
            topLeft = Offset(w * 0.12f + 1.5.dp.toPx(), h * 0.45f + 2.dp.toPx()),
            size = Size(w * 0.76f, h * 0.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
        )
        // Black clip body (trapezoid)
        val body = Path().apply {
            moveTo(w * 0.2f, h * 0.45f)
            lineTo(w * 0.8f, h * 0.45f)
            lineTo(w * 0.88f, h * 0.95f)
            lineTo(w * 0.12f, h * 0.95f)
            close()
        }
        drawPath(body, Brush.verticalGradient(listOf(Color(0xFF4B5563), Color(0xFF111827))))
        drawLine(Color.White.copy(alpha = 0.25f), Offset(w * 0.22f, h * 0.5f), Offset(w * 0.78f, h * 0.5f), 1.dp.toPx())
    }
}

@Composable
fun MagnetFastener(modifier: Modifier = Modifier, color: Color = Color(0xFF3B82F6)) {
    Canvas(modifier = modifier.size(30.dp, 20.dp)) {
        val cr = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx())
        drawRoundRect(Color.Black.copy(alpha = 0.22f), topLeft = Offset(1.5.dp.toPx(), 2.5.dp.toPx()), size = size, cornerRadius = cr)
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(color.copy(alpha = 1f), color.copy(red = color.red * 0.7f, green = color.green * 0.7f, blue = color.blue * 0.7f))),
            size = size, cornerRadius = cr
        )
        drawRoundRect(
            Color.White.copy(alpha = 0.35f),
            topLeft = Offset(size.width * 0.12f, size.height * 0.14f),
            size = Size(size.width * 0.55f, size.height * 0.22f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
    }
}

@Composable
fun HeartPinFastener(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp, 24.dp)) {
        val w = size.width
        val h = size.height
        fun heart(dx: Float, dy: Float) = Path().apply {
            moveTo(w * 0.5f + dx, h * 0.85f + dy)
            cubicTo(w * 0.05f + dx, h * 0.55f + dy, w * 0.1f + dx, h * 0.1f + dy, w * 0.5f + dx, h * 0.3f + dy)
            cubicTo(w * 0.9f + dx, h * 0.1f + dy, w * 0.95f + dx, h * 0.55f + dy, w * 0.5f + dx, h * 0.85f + dy)
            close()
        }
        drawPath(heart(2.dp.toPx(), 2.5.dp.toPx()), Color.Black.copy(alpha = 0.22f))
        drawPath(
            heart(0f, 0f),
            Brush.radialGradient(
                listOf(Color(0xFFFDA4AF), Color(0xFFF43F5E), Color(0xFF9F1239)),
                center = Offset(w * 0.35f, h * 0.3f), radius = w * 0.7f
            )
        )
        drawCircle(Color.White.copy(alpha = 0.8f), 1.6.dp.toPx(), Offset(w * 0.32f, h * 0.33f))
    }
}

@Composable
fun PushPinFastener(
    modifier: Modifier = Modifier,
    pinColor: Color = Color(0xFFEF4444)
) {
    Canvas(modifier = modifier.size(24.dp, 28.dp)) {
        val cx = size.width / 2f
        val cy = size.height * 0.42f

        // Cast shadow on the paper (slanted down-right)
        drawOval(
            color = Color.Black.copy(alpha = 0.26f),
            topLeft = Offset(cx - 2.dp.toPx(), cy + 4.dp.toPx()),
            size = Size(13.dp.toPx(), 8.dp.toPx())
        )

        // Steel pin needle entry point
        drawCircle(
            color = Color(0xFF78909C),
            radius = 1.6.dp.toPx(),
            center = Offset(cx, size.height - 2.dp.toPx())
        )

        // Plastic conical collar / base
        val collarPath = Path().apply {
            moveTo(cx - 3.5.dp.toPx(), cy + 2.dp.toPx())
            lineTo(cx + 3.5.dp.toPx(), cy + 2.dp.toPx())
            lineTo(cx + 2.dp.toPx(), cy + 7.dp.toPx())
            lineTo(cx - 2.dp.toPx(), cy + 7.dp.toPx())
            close()
        }
        drawPath(collarPath, Color(0xFFB71C1C))

        // Glossy spherical pin head (3D radial gradient)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF8A80),
                    pinColor,
                    Color(0xFFB71C1C),
                    Color(0xFF450A0A)
                ),
                center = Offset(cx - 2.2.dp.toPx(), cy - 2.2.dp.toPx()),
                radius = 7.dp.toPx()
            ),
            radius = 6.2.dp.toPx(),
            center = Offset(cx, cy)
        )

        // Specular highlight spot (Glassy shine)
        drawCircle(
            color = Color.White.copy(alpha = 0.88f),
            radius = 1.7.dp.toPx(),
            center = Offset(cx - 2.2.dp.toPx(), cy - 2.2.dp.toPx())
        )
    }
}

@Composable
fun WashiTapeFastener(
    modifier: Modifier = Modifier,
    tapeColor: Color = Color(0xCCF87171),
    rotation: Float = -2f
) {
    Canvas(
        modifier = modifier
            .size(46.dp, 16.dp)
            .graphicsLayer { rotationZ = rotation }
    ) {
        val w = size.width
        val h = size.height

        // Subtle drop shadow under tape
        drawRect(
            color = Color.Black.copy(alpha = 0.08f),
            topLeft = Offset(1.dp.toPx(), 2.dp.toPx()),
            size = Size(w, h)
        )

        // Tape body with jagged / torn edges on ends
        val tapePath = Path().apply {
            moveTo(0f, 0f)
            lineTo(2.dp.toPx(), h * 0.25f)
            lineTo(0f, h * 0.5f)
            lineTo(2.dp.toPx(), h * 0.75f)
            lineTo(0f, h)

            lineTo(w, h)

            lineTo(w - 2.dp.toPx(), h * 0.75f)
            lineTo(w, h * 0.5f)
            lineTo(w - 2.dp.toPx(), h * 0.25f)
            lineTo(w, 0f)
            close()
        }

        drawPath(tapePath, tapeColor)

        // Subtle translucency & surface grain
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.08f)
                )
            ),
            topLeft = Offset.Zero,
            size = size
        )
    }
}

@Composable
fun PaperclipFastener(
    modifier: Modifier = Modifier,
    clipColor: Color = Color(0xFF7C3AED)
) {
    Canvas(modifier = modifier.size(18.dp, 34.dp)) {
        val w = size.width
        val h = size.height
        val strokePx = 2.2.dp.toPx()

        // Shadow behind wire
        val shadowPath = Path().apply {
            moveTo(w * 0.35f + 1.dp.toPx(), 2.dp.toPx() + 2.dp.toPx())
            lineTo(w * 0.35f + 1.dp.toPx(), h * 0.78f + 2.dp.toPx())
            arcTo(
                Rect(
                    w * 0.35f + 1.dp.toPx(), h * 0.68f + 2.dp.toPx(),
                    w * 0.85f + 1.dp.toPx(), h * 0.96f + 2.dp.toPx()
                ),
                180f, -180f, false
            )
            lineTo(w * 0.85f + 1.dp.toPx(), h * 0.22f + 2.dp.toPx())
            arcTo(
                Rect(
                    w * 0.15f + 1.dp.toPx(), 2.dp.toPx() + 2.dp.toPx(),
                    w * 0.85f + 1.dp.toPx(), h * 0.32f + 2.dp.toPx()
                ),
                0f, -180f, false
            )
            lineTo(w * 0.15f + 1.dp.toPx(), h * 0.68f + 2.dp.toPx())
        }
        drawPath(
            shadowPath,
            color = Color.Black.copy(alpha = 0.16f),
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        // Clip wire loops
        val clipPath = Path().apply {
            moveTo(w * 0.35f, 2.dp.toPx())
            lineTo(w * 0.35f, h * 0.78f)
            arcTo(
                Rect(
                    w * 0.35f, h * 0.68f,
                    w * 0.85f, h * 0.96f
                ),
                180f, -180f, false
            )
            lineTo(w * 0.85f, h * 0.22f)
            arcTo(
                Rect(
                    w * 0.15f, 2.dp.toPx(),
                    w * 0.85f, h * 0.32f
                ),
                0f, -180f, false
            )
            lineTo(w * 0.15f, h * 0.68f)
        }
        drawPath(
            clipPath,
            color = clipColor,
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// ── Empty State Sticky Note ──────────────────────────────────────────────────

@Composable
private fun EmptyStickyNote(isDark: Boolean, onAdd: () -> Unit) {
    Box(
        modifier = Modifier
            .width(260.dp)
            .graphicsLayer { rotationZ = -2.5f }
            .clickable { onAdd() }
    ) {
        Card(
            shape = RoundedCornerShape(4.dp, 4.dp, 8.dp, 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF322C19) else Color(0xFFFEF9C3)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Your Pinboard is Empty",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Cursive,
                        fontSize = 22.sp
                    ),
                    color = if (isDark) Color(0xFFFEF3C7) else Color(0xFF713F12)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tap here or the [+] button above to pin your first sticky note or reminder!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = if (isDark) Color(0xFFFEF3C7).copy(alpha = 0.7f) else Color(0xFF854D0E),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        PushPinFastener(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-4).dp),
            pinColor = Color(0xFFEF4444)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sticky Palettes & Formatting Helpers
// ─────────────────────────────────────────────────────────────────────────────

data class StickyPalette(
    val bgLight: Color,
    val bgDark: Color,
    val textLight: Color,
    val textDark: Color,
    val accent: Color
)

val StickyPalettes = listOf(
    // 0. Mint Green (Image top left)
    StickyPalette(
        bgLight = Color(0xFFD4F5E2),
        bgDark = Color(0xFF1E3A2F),
        textLight = Color(0xFF1B4332),
        textDark = Color(0xFFE8FDF5),
        accent = Color(0xFF2D6A4F)
    ),
    // 1. Peach / Warm Orange (Image top right)
    StickyPalette(
        bgLight = Color(0xFFFFDFBA),
        bgDark = Color(0xFF3F2718),
        textLight = Color(0xFF5D2E0C),
        textDark = Color(0xFFFFECE0),
        accent = Color(0xFFC05621)
    ),
    // 2. Lavender / Soft Violet (Image mid left)
    StickyPalette(
        bgLight = Color(0xFFE5DDFF),
        bgDark = Color(0xFF2E244E),
        textLight = Color(0xFF372661),
        textDark = Color(0xFFF3EFFF),
        accent = Color(0xFF6D28D9)
    ),
    // 3. Teal / Turquoise (Image mid right)
    StickyPalette(
        bgLight = Color(0xFFB5EAD7),
        bgDark = Color(0xFF1A3D36),
        textLight = Color(0xFF14473F),
        textDark = Color(0xFFE6FAF3),
        accent = Color(0xFF0F766E)
    ),
    // 4. Sky Blue (Image bottom left)
    StickyPalette(
        bgLight = Color(0xFFCBE8FF),
        bgDark = Color(0xFF1B354B),
        textLight = Color(0xFF123D5F),
        textDark = Color(0xFFE3F2FD),
        accent = Color(0xFF0284C7)
    ),
    // 5. Warm Banana Yellow (Image bottom right)
    StickyPalette(
        bgLight = Color(0xFFFEF3C7),
        bgDark = Color(0xFF3E3618),
        textLight = Color(0xFF524410),
        textDark = Color(0xFFFEF9C3),
        accent = Color(0xFFD97706)
    ),
    // 6. Rose Pink
    StickyPalette(
        bgLight = Color(0xFFFFD8E1),
        bgDark = Color(0xFF412028),
        textLight = Color(0xFF5B1B2A),
        textDark = Color(0xFFFFEBF0),
        accent = Color(0xFFE11D48)
    )
)

val StickyAngles = listOf(2.8f, -2.4f, -3.8f, 3.5f, -1.8f, 2.0f)

fun getStickyFont(id: Long): FontFamily {
    return when ((id.absoluteValue % 3L).toInt()) {
        0 -> FontFamily.SansSerif
        1 -> FontFamily.Cursive
        else -> FontFamily.Serif
    }
}

private fun isSameDay(d1: Date, d2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = d1 }
    val cal2 = Calendar.getInstance().apply { time = d2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

// ─────────────────────────────────────────────────────────────────────────────
// Add Task Dialog (Sticky Note Creator)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (String, Long?, String?, Double?, Double?) -> Unit,
    viewModel: NoteViewModel
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var locationQuery by remember { mutableStateOf("") }

    val geocodingState = viewModel.geocodingState
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    var requestBackgroundLocation by remember { mutableStateOf(false) }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                Toast.makeText(context, "Background geofencing enabled!", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fineGranted || coarseGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && requestBackgroundLocation) {
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
        }
    )

    LaunchedEffect(geocodingState) {
        if (geocodingState is NoteViewModel.GeocodingResult.Success) {
            selectedAddress = geocodingState.address
            latitude = geocodingState.latitude
            longitude = geocodingState.longitude
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PushPinFastener(modifier = Modifier.size(20.dp, 24.dp))
                Text(
                    text = "Pin New Sticky Note",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Task / Note Name
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What's on your mind?") },
                    placeholder = { Text("e.g. Call Boss 06:30 PM, Lunch time...") },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Date & Time Picker trigger
                OutlinedCard(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val pickedDate = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                }
                                // Follow up with TimePicker
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        pickedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        pickedDate.set(Calendar.MINUTE, minute)
                                        dueDate = pickedDate.timeInMillis
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (dueDate == null) "Set Time / Due Date (Optional)" else {
                                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(dueDate!!))
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (dueDate == null) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                        )
                        Icon(Icons.Outlined.AccessTime, contentDescription = "Pick Date & Time", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Geolocation input
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = locationQuery,
                            onValueChange = { locationQuery = it },
                            label = { Text("Location Reminder") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = {
                                val fineLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                                val coarseLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                                if (fineLoc != PackageManager.PERMISSION_GRANTED && coarseLoc != PackageManager.PERMISSION_GRANTED) {
                                    requestBackgroundLocation = true
                                    locationPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                } else {
                                    viewModel.geocodeAddress(locationQuery)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Find")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    when (geocodingState) {
                        is NoteViewModel.GeocodingResult.Loading -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Searching address...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        is NoteViewModel.GeocodingResult.Success -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Success",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = geocodingState.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        is NoteViewModel.GeocodingResult.Error -> {
                            Text(
                                text = geocodingState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        else -> {}
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, dueDate, selectedAddress, latitude, longitude)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Pin Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
