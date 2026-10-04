package com.example.notes.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.notes.data.local.NoteWithTags
import com.example.notes.ui.components.NoteCard
import com.example.notes.ui.viewmodel.NoteViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: NoteViewModel,
    onNavigateToEditor: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val archivedNotes by viewModel.archivedNotes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val glassyUiEnabled by viewModel.glassyUiEnabled.collectAsStateWithLifecycle()
    val isDark = MaterialTheme.colorScheme.background == com.example.notes.theme.DarkBackground

    var selectedNoteForActions by remember { mutableStateOf<NoteWithTags?>(null) }
    var showActionsBottomSheet by remember { mutableStateOf(false) }

    val backgroundModifier = if (glassyUiEnabled) {
        Modifier.background(Color.Transparent)
    } else {
        Modifier.background(MaterialTheme.colorScheme.background)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archive", color = if (glassyUiEnabled) Color.White else LocalContentColor.current) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = if (glassyUiEnabled) Color.White else LocalContentColor.current
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier.then(backgroundModifier)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Search field
            SearchBarField(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (archivedNotes.isEmpty()) {
                val animScale = remember { Animatable(0.6f) }
                val animAlpha = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    launch {
                        animScale.animateTo(
                            1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    animAlpha.animateTo(1f, animationSpec = tween(500))
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(40.dp)
                            .graphicsLayer {
                                scaleX = animScale.value
                                scaleY = animScale.value
                                alpha = animAlpha.value
                            }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = if (glassyUiEnabled) {
                                if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.5f)
                            } else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(96.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = if (glassyUiEnabled) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Archive is empty",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = if (glassyUiEnabled) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Notes you archive will appear here.\nSwipe right on a note to archive it.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = if (glassyUiEnabled) Color.White.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(archivedNotes, key = { it.note.id }) { noteWithTags ->
                        NoteCard(
                            noteWithTags = noteWithTags,
                            onClick = { onNavigateToEditor(noteWithTags.note.id) },
                            onLongClick = {
                                selectedNoteForActions = noteWithTags
                                showActionsBottomSheet = true
                            },
                            modifier = Modifier.animateItem(),
                            glassyUiEnabled = glassyUiEnabled
                        )
                    }
                }
            }
        }
    }

    // Modal Actions Sheet for Archived Notes
    if (showActionsBottomSheet && selectedNoteForActions != null) {
        val note = selectedNoteForActions!!.note
        ModalBottomSheet(
            onDismissRequest = { showActionsBottomSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = note.title.ifBlank { "Note Actions" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Unarchive Action
                BottomSheetItem(
                    icon = Icons.Default.Unarchive,
                    text = "Unarchive note",
                    onClick = {
                        viewModel.setNoteArchived(note.id, false)
                        showActionsBottomSheet = false
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Delete Action (Destructive)
                BottomSheetItem(
                    icon = Icons.Default.DeleteForever,
                    text = "Delete permanently",
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
