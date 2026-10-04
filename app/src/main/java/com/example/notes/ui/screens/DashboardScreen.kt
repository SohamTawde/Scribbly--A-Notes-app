/*
 * Copyright (c) 2026 Soham Tawde. All rights reserved.
 * Scribbly - Notes & Tasks for Android
 */

package com.example.notes.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notes.R
import com.example.notes.data.local.NoteWithTags
import com.example.notes.data.local.TagEntity
import com.example.notes.theme.AccentAmber
import com.example.notes.theme.AccentMint
import com.example.notes.theme.AccentRose
import com.example.notes.theme.AccentViolet
import com.example.notes.ui.components.CircleIconButton
import com.example.notes.ui.components.FloatingPillNavBar
import com.example.notes.ui.components.NoteCard
import com.example.notes.ui.components.PillNavItem
import com.example.notes.ui.components.QuickActionCard
import com.example.notes.ui.components.appContentColor
import com.example.notes.ui.components.cardSurfaceColor
import com.example.notes.ui.viewmodel.NoteViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: NoteViewModel,
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToVault: () -> Unit,
    modifier: Modifier = Modifier,
    onQuickAction: (String) -> Unit = { onNavigateToEditor(0) }
) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsStateWithLifecycle()
    val isGridLayout by viewModel.isGridLayout.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val glassyUiEnabled by viewModel.glassyUiEnabled.collectAsStateWithLifecycle()
    val todos by viewModel.todos.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    var showAddTaskDialog by remember { mutableStateOf(false) }

    var selectedNoteForActions by remember { mutableStateOf<NoteWithTags?>(null) }
    var showActionsBottomSheet by remember { mutableStateOf(false) }

    // Secret long-press vault access state
    var showSecretLockReveal by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "vaultGlow")
    val secretLockScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "secretLockScale"
    )
    val secretGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "secretGlowAlpha"
    )

    val isDark = MaterialTheme.colorScheme.background == com.example.notes.theme.DarkBackground
    val contentColor = appContentColor(glassyUiEnabled)

    // Voice search via the system speech recogniser
    val voiceSearchLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) viewModel.onSearchQueryChanged(spoken)
    }

    fun openVault() {
        authenticateBiometric(
            context = context,
            onSuccess = { onNavigateToVault() },
            onError = { err -> android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show() }
        )
    }

    fun openNote(noteWithTags: NoteWithTags) {
        if (noteWithTags.note.isLocked) {
            authenticateBiometric(
                context = context,
                onSuccess = { onNavigateToEditor(noteWithTags.note.id) },
                onError = { err -> android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show() }
            )
        } else onNavigateToEditor(noteWithTags.note.id)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (glassyUiEnabled) Color.Transparent else MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                        val absOffset = pageOffset.absoluteValue
                        alpha = 1f - (absOffset * 1.2f).coerceIn(0f, 1f)
                        val scale = 1f - (absOffset * 0.06f).coerceIn(0f, 0.06f)
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                if (page == 0) {
                    val pinnedNotes = notes.filter { it.note.isPinned }
                    val regularNotes = notes.filter { !it.note.isPinned }

                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(if (isGridLayout) 2 else 1),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 130.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        // ── Header ────────────────────────────────────────
                        item(span = StaggeredGridItemSpan.FullLine, key = "header") {
                            DashboardHeader(
                                themeMode = themeMode,
                                contentColor = contentColor,
                                glassy = glassyUiEnabled,
                                onThemeClick = {
                                    val nextTheme = when (themeMode) {
                                        "system" -> "light"
                                        "light" -> "dark"
                                        else -> "system"
                                    }
                                    viewModel.setThemeMode(nextTheme)
                                },
                                onSettingsClick = onNavigateToSettings,
                                onSecretLongPress = { showSecretLockReveal = true }
                            )
                        }

                        // ── Search ────────────────────────────────────────
                        item(span = StaggeredGridItemSpan.FullLine, key = "search") {
                            SearchBarField(
                                query = searchQuery,
                                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                                glassyUiEnabled = glassyUiEnabled,
                                isDark = isDark,
                                onMicClick = {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Search your notes")
                                    }
                                    try { voiceSearchLauncher.launch(intent) } catch (_: ActivityNotFoundException) {
                                        android.widget.Toast.makeText(context, "Voice search not available", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // ── Quick actions (2x2) ───────────────────────────
                        if (searchQuery.isBlank()) {
                            item(span = StaggeredGridItemSpan.FullLine, key = "quick") {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        QuickActionCard(
                                            title = "Text Note",
                                            subtitle = "Write and save your thoughts",
                                            icon = Icons.Outlined.TextFields,
                                            accent = MaterialTheme.colorScheme.primary,
                                            onClick = { onQuickAction("") },
                                            glassy = glassyUiEnabled,
                                            modifier = Modifier.weight(1f)
                                        )
                                        QuickActionCard(
                                            title = "Voice Note",
                                            subtitle = "Record and save your voice",
                                            icon = Icons.Outlined.GraphicEq,
                                            accent = MaterialTheme.colorScheme.primary,
                                            highlighted = true,
                                            onClick = { onQuickAction("voice") },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        QuickActionCard(
                                            title = "Image Note",
                                            subtitle = "Capture notes from images",
                                            icon = Icons.Outlined.Image,
                                            accent = AccentRose,
                                            onClick = { onQuickAction("image") },
                                            glassy = glassyUiEnabled,
                                            modifier = Modifier.weight(1f)
                                        )
                                        QuickActionCard(
                                            title = "Sketch Note",
                                            subtitle = "Draw and doodle freely",
                                            icon = Icons.Outlined.Draw,
                                            accent = AccentViolet,
                                            onClick = { onQuickAction("draw") },
                                            glassy = glassyUiEnabled,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // ── Recent notes header ───────────────────────────
                        item(span = StaggeredGridItemSpan.FullLine, key = "recentHeader") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 14.dp)
                            ) {
                                Text(
                                    if (searchQuery.isBlank()) "Recent Notes" else "Results",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = contentColor
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${notes.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                        .padding(horizontal = 9.dp, vertical = 2.dp)
                                )
                                Spacer(Modifier.weight(1f))
                                CircleIconButton(
                                    icon = if (isGridLayout) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                    contentDescription = "Toggle layout",
                                    onClick = { viewModel.toggleLayout() },
                                    size = 38.dp,
                                    containerColor = cardSurfaceColor(glassyUiEnabled)
                                )
                            }
                        }

                        // ── Tag filters ───────────────────────────────────
                        if (tags.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine, key = "tags") {
                                TagFiltersRow(
                                    tags = tags,
                                    selectedTag = selectedTagFilter,
                                    onTagSelected = { viewModel.onTagFilterSelected(it) },
                                    glassyUiEnabled = glassyUiEnabled,
                                    isDark = isDark
                                )
                            }
                        }

                        if (notes.isEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine, key = "empty") {
                                EmptyStatePlaceholder(
                                    isSearching = searchQuery.isNotBlank() || selectedTagFilter != null,
                                    glassyUiEnabled = glassyUiEnabled,
                                    isDark = isDark,
                                    modifier = Modifier.height(280.dp)
                                )
                            }
                        }

                        if (pinnedNotes.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine, key = "pinnedHeader") {
                                SectionHeader(text = "Pinned", icon = Icons.Default.PushPin)
                            }
                            items(pinnedNotes, key = { it.note.id }) { noteWithTags ->
                                NoteListItem(
                                    noteWithTags = noteWithTags,
                                    isGrid = isGridLayout,
                                    glassy = glassyUiEnabled,
                                    onClick = { openNote(noteWithTags) },
                                    onLongClick = { selectedNoteForActions = noteWithTags; showActionsBottomSheet = true },
                                    onArchive = { viewModel.setNoteArchived(noteWithTags.note.id, true) },
                                    onDelete = { viewModel.deleteNote(noteWithTags.note) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                        if (regularNotes.isNotEmpty()) {
                            if (pinnedNotes.isNotEmpty()) {
                                item(span = StaggeredGridItemSpan.FullLine, key = "othersHeader") {
                                    SectionHeader(text = "Others", icon = null)
                                }
                            }
                            items(regularNotes, key = { it.note.id }) { noteWithTags ->
                                NoteListItem(
                                    noteWithTags = noteWithTags,
                                    isGrid = isGridLayout,
                                    glassy = glassyUiEnabled,
                                    onClick = { openNote(noteWithTags) },
                                    onLongClick = { selectedNoteForActions = noteWithTags; showActionsBottomSheet = true },
                                    onArchive = { viewModel.setNoteArchived(noteWithTags.note.id, true) },
                                    onDelete = { viewModel.deleteNote(noteWithTags.note) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                } else if (page == 1) {
                    TodoTabContent(
                        viewModel = viewModel,
                        onBack = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        onAdd = { showAddTaskDialog = true },
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(bottom = 80.dp)
                    )
                } else {
                    val archivedNotes by viewModel.archivedNotes.collectAsStateWithLifecycle()
                    ArchiveTabContent(
                        archivedNotes = archivedNotes,
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.onSearchQueryChanged(it) },
                        isGridLayout = isGridLayout,
                        glassy = glassyUiEnabled,
                        isDark = isDark,
                        contentColor = contentColor,
                        onNoteClick = { openNote(it) },
                        onNoteLongClick = { selectedNoteForActions = it; showActionsBottomSheet = true },
                        onUnarchive = { viewModel.setNoteArchived(it.note.id, false) },
                        onDelete = { viewModel.deleteNote(it.note) }
                    )
                }
            }
        }

        // ── Floating pill navigation with liquid blob indicator ─────────
        FloatingPillNavBar(
            scrollPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction,
            items = listOf(
                PillNavItem(Icons.Outlined.Home, "Notes", selected = pagerState.targetPage == 0) {
                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                },
                PillNavItem(Icons.Outlined.TaskAlt, "To-dos", selected = pagerState.targetPage == 1) {
                    coroutineScope.launch { pagerState.animateScrollToPage(1) }
                },
                PillNavItem(Icons.Outlined.Archive, "Archive", selected = pagerState.targetPage == 2) {
                    coroutineScope.launch { pagerState.animateScrollToPage(2) }
                },
                PillNavItem(Icons.Default.Add, if (pagerState.targetPage == 1) "New task" else "New note", isAction = true) {
                    if (pagerState.targetPage == 1) showAddTaskDialog = true else onNavigateToEditor(0)
                }
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }

    if (showAddTaskDialog) {
        com.example.notes.ui.screens.AddTaskDialog(
            onDismiss = {
                viewModel.clearGeocodingState()
                showAddTaskDialog = false
            },
            onSave = { title, date, locName, lat, lng ->
                val newTodo = com.example.notes.data.local.TodoEntity(
                    title = title,
                    dueDate = date,
                    locationName = locName,
                    latitude = lat,
                    longitude = lng
                )
                viewModel.saveTodo(newTodo)
                showAddTaskDialog = false
                viewModel.clearGeocodingState()
            },
            viewModel = viewModel
        )
    }

    // Secret long-press vault reveal overlay
    if (showSecretLockReveal) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showSecretLockReveal = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E3A6B).copy(alpha = 0.97f),
                                Color(0xFF0B1120).copy(alpha = 0.97f)
                            )
                        )
                    )
                    .clickable { showSecretLockReveal = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .scale(secretLockScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF5B9BFF).copy(alpha = secretGlowAlpha),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF5B9BFF), Color(0xFF2F6FE0))
                                    )
                                )
                                .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                .clickable {
                                    showSecretLockReveal = false
                                    openVault()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Open Vault",
                                tint = Color.White,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(
                        "Private Vault",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap the lock to authenticate",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }

    // Long press actions Bottom Sheet
    if (showActionsBottomSheet && selectedNoteForActions != null) {
        val note = selectedNoteForActions!!.note
        ModalBottomSheet(
            onDismissRequest = { showActionsBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Choose an action",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                BottomSheetItem(
                    icon = Icons.Default.PushPin,
                    text = if (note.isPinned) "Unpin note" else "Pin note",
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        viewModel.setNotePinned(note.id, !note.isPinned)
                        showActionsBottomSheet = false
                    }
                )

                // Lock / Unlock Action — unlocking requires biometric verification
                BottomSheetItem(
                    icon = if (note.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    text = if (note.isLocked) "Unlock note" else "Lock note",
                    iconColor = AccentViolet,
                    onClick = {
                        if (note.isLocked) {
                            authenticateBiometric(
                                context = context,
                                title = "Confirm Unlock",
                                subtitle = "Verify your identity to move this note back to your main list",
                                onSuccess = {
                                    viewModel.setNoteLocked(note.id, false)
                                    showActionsBottomSheet = false
                                },
                                onError = { err ->
                                    android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            viewModel.setNoteLocked(note.id, true)
                            showActionsBottomSheet = false
                        }
                    }
                )

                BottomSheetItem(
                    icon = Icons.Default.Archive,
                    text = if (note.isArchived) "Unarchive note" else "Archive note",
                    iconColor = AccentAmber,
                    onClick = {
                        viewModel.setNoteArchived(note.id, !note.isArchived)
                        showActionsBottomSheet = false
                    }
                )

                BottomSheetItem(
                    icon = Icons.Default.Delete,
                    text = "Delete note",
                    textColor = MaterialTheme.colorScheme.error,
                    iconColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        viewModel.deleteNote(note)
                        showActionsBottomSheet = false
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header / sections
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardHeader(
    themeMode: String,
    contentColor: Color,
    glassy: Boolean,
    onThemeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSecretLongPress: () -> Unit
) {
    val greeting = remember {
        when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Hello, night owl"
        }
    }
    val today = remember {
        java.text.SimpleDateFormat("EEEE, d MMM", java.util.Locale.getDefault()).format(java.util.Date())
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .pointerInput(Unit) { detectTapGestures(onLongPress = { onSecretLongPress() }) }
        ) {
            Text(
                "$greeting 👋",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
            Text(
                today,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.55f)
            )
        }
        CircleIconButton(
            icon = when (themeMode) {
                "light" -> Icons.Default.LightMode
                "dark" -> Icons.Default.DarkMode
                else -> Icons.Default.BrightnessAuto
            },
            contentDescription = "Switch theme",
            onClick = onThemeClick,
            containerColor = cardSurfaceColor(glassy)
        )
        Spacer(Modifier.width(10.dp))
        CircleIconButton(
            icon = Icons.Outlined.Settings,
            contentDescription = "Settings",
            onClick = onSettingsClick,
            containerColor = cardSurfaceColor(glassy)
        )
    }
}

@Composable
private fun TodoHeader(pending: Int, total: Int, contentColor: Color, onAdd: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text("My Tasks", style = MaterialTheme.typography.headlineMedium, color = contentColor)
            Text(
                if (total == 0) "Nothing planned yet" else "$pending of $total pending",
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.55f)
            )
        }
        CircleIconButton(
            icon = Icons.Default.Add,
            contentDescription = "Add task",
            onClick = onAdd,
            containerColor = MaterialTheme.colorScheme.primary,
            tint = Color.White
        )
    }
    if (total > 0) {
        LinearProgressIndicator(
            progress = { (total - pending).toFloat() / total },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .height(6.dp)
        )
    }
}

@Composable
private fun ArchiveTabContent(
    archivedNotes: List<NoteWithTags>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isGridLayout: Boolean,
    glassy: Boolean,
    isDark: Boolean,
    contentColor: Color,
    onNoteClick: (NoteWithTags) -> Unit,
    onNoteLongClick: (NoteWithTags) -> Unit,
    onUnarchive: (NoteWithTags) -> Unit,
    onDelete: (NoteWithTags) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Archive Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Archive",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = contentColor
                )
                Text(
                    text = if (archivedNotes.isEmpty()) "No archived notes" else "${archivedNotes.size} notes archived",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.55f)
                )
            }
        }

        // Search bar
        SearchBarField(
            query = searchQuery,
            onQueryChange = onSearchChange,
            glassyUiEnabled = glassy,
            isDark = isDark,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        Spacer(Modifier.height(8.dp))

        if (archivedNotes.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 80.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = if (glassy) {
                            if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.5f)
                        } else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(90.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Archive is empty",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = contentColor
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Notes you archive will appear here.\nSwipe right on a note to archive it.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = contentColor.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(if (isGridLayout) 2 else 1),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 130.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                items(archivedNotes, key = { it.note.id }) { noteWithTags ->
                    NoteListItem(
                        noteWithTags = noteWithTags,
                        isGrid = isGridLayout,
                        glassy = glassy,
                        onClick = { onNoteClick(noteWithTags) },
                        onLongClick = { onNoteLongClick(noteWithTags) },
                        onArchive = { onUnarchive(noteWithTags) },
                        onDelete = { onDelete(noteWithTags) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteListItem(
    noteWithTags: NoteWithTags,
    isGrid: Boolean,
    glassy: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isGrid) {
        NoteCard(
            noteWithTags = noteWithTags,
            onClick = onClick,
            onLongClick = onLongClick,
            glassyUiEnabled = glassy,
            modifier = modifier
        )
    } else {
        Box(modifier) {
            SwipeableNoteCard(
                noteWithTags = noteWithTags,
                onClick = onClick,
                onLongClick = onLongClick,
                onSwipeArchive = onArchive,
                onSwipeDelete = onDelete,
                glassyUiEnabled = glassy
            )
        }
    }
}

private fun authenticateBiometric(
    context: Context,
    title: String = "Private Vault Verification",
    subtitle: String = "Confirm your biometric credentials",
    onSuccess: () -> Unit,
    onError: (String) -> Unit = {}
) {
    val executor = ContextCompat.getMainExecutor(context)
    val activity = context as? androidx.fragment.app.FragmentActivity
    if (activity == null) {
        onSuccess()
        return
    }

    val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Authentication failed")
            }
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setNegativeButtonText("Cancel")
        .build()

    biometricPrompt.authenticate(promptInfo)
}

/** Rounded pill search field with optional mic button. */
@Composable
fun SearchBarField(
    query: String,
    onQueryChange: (String) -> Unit,
    glassyUiEnabled: Boolean = false,
    isDark: Boolean = false,
    modifier: Modifier = Modifier,
    onMicClick: (() -> Unit)? = null
) {
    val content = appContentColor(glassyUiEnabled)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(CircleShape)
            .background(cardSurfaceColor(glassyUiEnabled))
            .then(
                if (glassyUiEnabled || isDark) Modifier.border(1.dp, content.copy(alpha = 0.08f), CircleShape) else Modifier
            )
            .padding(start = 18.dp, end = 6.dp)
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = "Search",
            tint = content.copy(alpha = 0.45f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    "Search notes, tags…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = content.copy(alpha = 0.4f)
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = content),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = content.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            }
        } else if (onMicClick != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable(onClick = onMicClick)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Voice search", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun TagFiltersRow(
    tags: List<TagEntity>,
    selectedTag: TagEntity?,
    onTagSelected: (TagEntity?) -> Unit,
    glassyUiEnabled: Boolean = false,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        item {
            TagPill(label = "All", selected = selectedTag == null, dotColor = null, glassy = glassyUiEnabled) { onTagSelected(null) }
        }
        items(tags) { tag ->
            val isSelected = selectedTag?.name == tag.name
            TagPill(
                label = tag.name,
                selected = isSelected,
                dotColor = if (tag.color != 0) Color(tag.color) else null,
                glassy = glassyUiEnabled
            ) { onTagSelected(if (isSelected) null else tag) }
        }
    }
}

@Composable
private fun TagPill(label: String, selected: Boolean, dotColor: Color?, glassy: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val bg by animateColorAsState(if (selected) primary else cardSurfaceColor(glassy), label = "tagBg")
    val fg = if (selected) Color.White else appContentColor(glassy).copy(alpha = 0.75f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        if (dotColor != null) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White else dotColor)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
fun EmptyStatePlaceholder(
    isSearching: Boolean,
    glassyUiEnabled: Boolean = false,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val content = appContentColor(glassyUiEnabled)
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
        ) {
            Icon(
                imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.EditNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isSearching) "No matches found" else "No notes yet",
            style = MaterialTheme.typography.titleMedium,
            color = content.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isSearching) "Try a different keyword or tag." else "Tap a card above or the + button to capture your first idea.",
            style = MaterialTheme.typography.bodyMedium,
            color = content.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BottomSheetItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconColor.copy(alpha = 0.12f))
        ) {
            Icon(imageVector = icon, contentDescription = text, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = textColor)
    }
}

/** Section header label with optional leading icon */
@Composable
fun SectionHeader(
    text: String,
    icon: ImageVector?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
        )
    }
}

/** A NoteCard wrapped in SwipeToDismissBox: swipe right = archive, swipe left = delete */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableNoteCard(
    noteWithTags: NoteWithTags,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeArchive: () -> Unit,
    onSwipeDelete: () -> Unit,
    glassyUiEnabled: Boolean,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onSwipeArchive(); true }
                SwipeToDismissBoxValue.EndToStart -> { onSwipeDelete(); true }
                else -> false
            }
        },
        positionalThreshold = { it * 0.40f }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isArchive = direction == SwipeToDismissBoxValue.StartToEnd
            val bgColor by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> AccentMint
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    else -> Color.Transparent
                },
                label = "swipeBg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(bgColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (isArchive) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = if (isArchive) Icons.Default.Archive else Icons.Default.Delete,
                    contentDescription = if (isArchive) "Archive" else "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        modifier = Modifier
    ) {
        NoteCard(
            noteWithTags = noteWithTags,
            onClick = onClick,
            onLongClick = onLongClick,
            glassyUiEnabled = glassyUiEnabled,
        )
    }
}
