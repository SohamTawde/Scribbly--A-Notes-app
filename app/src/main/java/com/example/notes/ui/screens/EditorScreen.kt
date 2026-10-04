/*
 * Copyright (c) 2026 Soham Tawde. All rights reserved.
 * Scribbly - Notes & Tasks for Android
 */

package com.example.notes.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.notes.data.local.NoteEntity
import com.example.notes.theme.*
import com.example.notes.ui.components.*
import com.example.notes.ui.viewmodel.NoteViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteId: Long,
    viewModel: NoteViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    startAction: String = ""
) {
    val context = LocalContext.current

    // Load note for editing
    LaunchedEffect(noteId) {
        viewModel.loadNoteForEditing(noteId)
    }

    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val noteTags by viewModel.noteTags.collectAsStateWithLifecycle()
    val fontSizeScale by viewModel.fontSizeScale.collectAsStateWithLifecycle()
    val glassyUiEnabled by viewModel.glassyUiEnabled.collectAsStateWithLifecycle()

    var showSkinsSheet by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showPhotoMenu by remember { mutableStateOf(false) }
    var showScribbleDialog by remember { mutableStateOf(false) }
    var showOcrDialog by remember { mutableStateOf(false) }
    var showSpeechDialog by remember { mutableStateOf(false) }
    var showVoiceRecorder by remember { mutableStateOf(false) }
    var isPreviewMode by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var showFindBar by remember { mutableStateOf(false) }

    // If opened directly as a Sketch Note from dashboard quick action:
    // Render only the canvas with matching theme background — zero micro-frames of text note!
    if (startAction == "draw") {
        val isDirectDark = isAppInDarkTheme()
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(if (isDirectDark) Color(0xFF141416) else MaterialTheme.colorScheme.background)
        ) {
            ScribbleCanvasDialog(
                onDismiss = onNavigateBack,
                onSave = { path ->
                    viewModel.noteScribblePath = path
                    if (viewModel.noteTitle.isBlank()) {
                        viewModel.noteTitle = "Sketch Note"
                    }
                    viewModel.saveEditingNote()
                    onNavigateBack()
                },
                initialScribblePath = viewModel.noteScribblePath
            )
        }
        return
    }

    // Quick action trigger from dashboard (voice, image)
    LaunchedEffect(startAction) {
        when (startAction) {
            "voice" -> showVoiceRecorder = true
            "image" -> showPhotoMenu = true
        }
    }

    // Audio player state
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    // FileProvider image capture setup
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraFile != null) {
            viewModel.noteImageUri = pendingCameraFile!!.absolutePath
        }
    }

    val cameraPermissionLauncherForPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val (uri, file) = createCameraImageUri(context)
                pendingCameraFile = file
                takePhotoLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCameraCapture() {
        val status = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (status == PackageManager.PERMISSION_GRANTED) {
            try {
                val (uri, file) = createCameraImageUri(context)
                pendingCameraFile = file
                takePhotoLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncherForPhoto.launch(Manifest.permission.CAMERA)
        }
    }

    // Gallery picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val internalPath = copyUriToInternalStorage(context, uri)
            if (internalPath != null) {
                viewModel.noteImageUri = internalPath
            } else {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // OCR permission launcher
    val cameraPermissionLauncherForOcr = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showOcrDialog = true
        else Toast.makeText(context, "Camera permission is required for scan", Toast.LENGTH_SHORT).show()
    }

    // Audio permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showVoiceRecorder = true
        else Toast.makeText(context, "Audio permission is required to record", Toast.LENGTH_SHORT).show()
    }

    // Theme & colors
    val isDark = isAppInDarkTheme()
    val noteBgColor = getNoteBackgroundColor(viewModel.noteColor)
    val contentColor = if (glassyUiEnabled) appContentColor(true) else getNoteOnColor(viewModel.noteColor)
    val cardColor = if (glassyUiEnabled) {
        if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.65f)
    } else {
        noteBgColor
    }

    val wordCount = remember(viewModel.noteContent) {
        if (viewModel.noteContent.isBlank()) 0
        else viewModel.noteContent.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }

    val formattedDate = remember(viewModel.noteCreatedAt) {
        val sdf = SimpleDateFormat("dd/MM/yyyy, h:mm a", Locale.getDefault())
        sdf.format(Date(viewModel.noteCreatedAt))
    }

    // Rich text field state tracking cursor & selection for markdown toggles
    var contentValue by remember { mutableStateOf(TextFieldValue(viewModel.noteContent)) }
    var isBoldToggledOn by remember { mutableStateOf(false) }
    var isItalicToggledOn by remember { mutableStateOf(false) }

    // Sync contentValue whenever noteContent is modified from outside (AI, OCR, voice, initial load)
    LaunchedEffect(viewModel.noteContent) {
        if (viewModel.noteContent != contentValue.text) {
            contentValue = TextFieldValue(
                text = viewModel.noteContent,
                selection = TextRange(viewModel.noteContent.length)
            )
        }
    }

    fun toggleBold() {
        val current = contentValue
        val text = current.text
        val sel = current.selection
        val start = minOf(sel.start, sel.end)
        val end = maxOf(sel.start, sel.end)

        if (start != end) {
            val selectedText = text.substring(start, end)
            if (selectedText.startsWith("**") && selectedText.endsWith("**") && selectedText.length >= 4) {
                val unwrapped = selectedText.substring(2, selectedText.length - 2)
                val newText = text.substring(0, start) + unwrapped + text.substring(end)
                contentValue = TextFieldValue(newText, TextRange(start, start + unwrapped.length))
                viewModel.noteContent = newText
            } else if (start >= 2 && end + 2 <= text.length &&
                text.substring(start - 2, start) == "**" && text.substring(end, end + 2) == "**") {
                val newText = text.substring(0, start - 2) + selectedText + text.substring(end + 2)
                contentValue = TextFieldValue(newText, TextRange(start - 2, end - 2))
                viewModel.noteContent = newText
            } else {
                val newText = text.substring(0, start) + "**" + selectedText + "**" + text.substring(end)
                contentValue = TextFieldValue(newText, TextRange(start, end + 4))
                viewModel.noteContent = newText
            }
        } else {
            val pos = start
            val (wStart, wEnd) = findWordBounds(text, pos)
            if (wStart < wEnd) {
                val word = text.substring(wStart, wEnd)
                if (wStart >= 2 && wEnd + 2 <= text.length &&
                    text.substring(wStart - 2, wStart) == "**" && text.substring(wEnd, wEnd + 2) == "**") {
                    val newText = text.substring(0, wStart - 2) + word + text.substring(wEnd + 2)
                    contentValue = TextFieldValue(newText, TextRange((pos - 2).coerceIn(0, newText.length)))
                    viewModel.noteContent = newText
                } else {
                    val newText = text.substring(0, wStart) + "**" + word + "**" + text.substring(wEnd)
                    contentValue = TextFieldValue(newText, TextRange((pos + 2).coerceIn(0, newText.length)))
                    viewModel.noteContent = newText
                }
            } else {
                isBoldToggledOn = !isBoldToggledOn
            }
        }
    }

    fun toggleItalic() {
        val current = contentValue
        val text = current.text
        val sel = current.selection
        val start = minOf(sel.start, sel.end)
        val end = maxOf(sel.start, sel.end)

        if (start != end) {
            val selectedText = text.substring(start, end)
            if (selectedText.startsWith("*") && !selectedText.startsWith("**") &&
                selectedText.endsWith("*") && !selectedText.endsWith("**") && selectedText.length >= 2) {
                val unwrapped = selectedText.substring(1, selectedText.length - 1)
                val newText = text.substring(0, start) + unwrapped + text.substring(end)
                contentValue = TextFieldValue(newText, TextRange(start, start + unwrapped.length))
                viewModel.noteContent = newText
            } else if (start >= 1 && end + 1 <= text.length &&
                text[start - 1] == '*' && text.getOrNull(start - 2) != '*' &&
                text[end] == '*' && text.getOrNull(end + 1) != '*') {
                val newText = text.substring(0, start - 1) + selectedText + text.substring(end + 1)
                contentValue = TextFieldValue(newText, TextRange(start - 1, end - 1))
                viewModel.noteContent = newText
            } else {
                val newText = text.substring(0, start) + "*" + selectedText + "*" + text.substring(end)
                contentValue = TextFieldValue(newText, TextRange(start, end + 2))
                viewModel.noteContent = newText
            }
        } else {
            val pos = start
            val (wStart, wEnd) = findWordBounds(text, pos)
            if (wStart < wEnd) {
                val word = text.substring(wStart, wEnd)
                if (wStart >= 1 && wEnd + 1 <= text.length &&
                    text[wStart - 1] == '*' && text.getOrNull(wStart - 2) != '*' &&
                    text[wEnd] == '*' && text.getOrNull(wEnd + 1) != '*') {
                    val newText = text.substring(0, wStart - 1) + word + text.substring(wEnd + 1)
                    contentValue = TextFieldValue(newText, TextRange((pos - 1).coerceIn(0, newText.length)))
                    viewModel.noteContent = newText
                } else {
                    val newText = text.substring(0, wStart) + "*" + word + "*" + text.substring(wEnd)
                    contentValue = TextFieldValue(newText, TextRange((pos + 1).coerceIn(0, newText.length)))
                    viewModel.noteContent = newText
                }
            } else {
                isItalicToggledOn = !isItalicToggledOn
            }
        }
    }

    fun toggleHeading() {
        val current = contentValue
        val text = current.text
        val pos = current.selection.start
        val lineStart = text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', pos).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        val (newLine, diff) = when {
            line.startsWith("### ") -> Pair(line.removePrefix("### "), -4)
            line.startsWith("## ") -> Pair("### " + line.removePrefix("## "), 1)
            line.startsWith("# ") -> Pair("## " + line.removePrefix("# "), 1)
            else -> Pair("# $line", 2)
        }

        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        contentValue = TextFieldValue(newText, TextRange((pos + diff).coerceIn(0, newText.length)))
        viewModel.noteContent = newText
    }

    fun toggleChecklist() {
        val current = contentValue
        val text = current.text
        val pos = current.selection.start
        val lineStart = text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', pos).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        val (newLine, diff) = when {
            line.startsWith("- [ ] ") -> Pair(line.removePrefix("- [ ] "), -6)
            line.startsWith("- [x] ") -> Pair(line.removePrefix("- [x] "), -6)
            line.startsWith("- ") -> Pair("- [ ] " + line.removePrefix("- "), 4)
            else -> Pair("- [ ] $line", 6)
        }

        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        contentValue = TextFieldValue(newText, TextRange((pos + diff).coerceIn(0, newText.length)))
        viewModel.noteContent = newText
    }

    fun saveAndExit() {
        viewModel.saveEditingNote()
        onNavigateBack()
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Scaffold(
            topBar = {
                EditorTopAppBar(
                    isNewNote = (noteId == 0L),
                    isPinned = viewModel.notePinned,
                    isLocked = viewModel.noteIsLocked,
                    onBackClick = { saveAndExit() },
                    onPinClick = {
                        viewModel.setEditingNotePinned(!viewModel.notePinned)
                    },
                    onLockClick = {
                        if (viewModel.noteIsLocked) {
                            authenticateBiometricInEditor(
                                context = context,
                                title = "Confirm Unlock",
                                subtitle = "Verify your identity to unlock this note",
                                onSuccess = { viewModel.noteIsLocked = false }
                            )
                        } else {
                            viewModel.noteIsLocked = true
                        }
                    },
                    onShareClick = {
                        val shareText = buildString {
                            if (viewModel.noteTitle.isNotBlank()) appendLine(viewModel.noteTitle)
                            if (viewModel.noteContent.isNotBlank()) append(viewModel.noteContent)
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, viewModel.noteTitle)
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share note"))
                    },
                    onSaveClick = { saveAndExit() },
                    contentColor = contentColor
                )
            },
            bottomBar = {
                EditorBottomBar(
                    isPreviewMode = isPreviewMode,
                    showFindBar = showFindBar,
                    isBold = isBoldAtCursor(contentValue) || isBoldToggledOn,
                    isItalic = isItalicAtCursor(contentValue) || isItalicToggledOn,
                    onTogglePreview = { isPreviewMode = !isPreviewMode },
                    onToggleFind = { showFindBar = !showFindBar },
                    onBold = { toggleBold() },
                    onItalic = { toggleItalic() },
                    onHeading = { toggleHeading() },
                    onChecklist = { toggleChecklist() },
                    onStrikethrough = {
                        val current = contentValue
                        val sel = current.selection
                        val start = minOf(sel.start, sel.end)
                        val end = maxOf(sel.start, sel.end)
                        if (start != end) {
                            val sub = current.text.substring(start, end)
                            val newText = current.text.substring(0, start) + "~~$sub~~" + current.text.substring(end)
                            contentValue = TextFieldValue(newText, TextRange(start + 2, end + 2))
                            viewModel.noteContent = newText
                        } else {
                            val newText = current.text.substring(0, start) + "~~~~" + current.text.substring(start)
                            contentValue = TextFieldValue(newText, TextRange(start + 2))
                            viewModel.noteContent = newText
                        }
                    },
                    onCode = {
                        val current = contentValue
                        val pos = current.selection.start
                        val newText = current.text.substring(0, pos) + "\n```\n\n```\n" + current.text.substring(pos)
                        contentValue = TextFieldValue(newText, TextRange(pos + 5))
                        viewModel.noteContent = newText
                    },
                    onSketch = { showScribbleDialog = true },
                    onSpeech = {
                        val status = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        if (status == PackageManager.PERMISSION_GRANTED) showSpeechDialog = true
                        else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onCameraMenu = { showPhotoMenu = true },
                    contentColor = contentColor,
                    glassy = glassyUiEnabled
                )
            },
            containerColor = if (glassyUiEnabled) Color.Transparent else MaterialTheme.colorScheme.background,
            modifier = modifier
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // ── Editor Note Card (Matches Image 1 Create Note screen) ────
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(
                        1.dp,
                        if (glassyUiEnabled) Color.White.copy(alpha = if (isDark) 0.15f else 0.5f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (viewModel.noteColor == 0) 0.7f else 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Attached Image Preview
                        viewModel.noteImageUri?.let { path ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .padding(bottom = 14.dp)
                            ) {
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = "Note image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(20.dp))
                                )
                                CircleIconButton(
                                    icon = Icons.Default.Close,
                                    contentDescription = "Remove image",
                                    onClick = { viewModel.noteImageUri = null },
                                    containerColor = Color.Black.copy(alpha = 0.55f),
                                    tint = Color.White,
                                    size = 36.dp,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                )
                            }
                        }

                        // Attached Scribble Preview
                        viewModel.noteScribblePath?.let { path ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .padding(bottom = 14.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isDark) Color(0xFF1C1C1E) else Color.White)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                            ) {
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = "Note sketch",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                )
                                CircleIconButton(
                                    icon = Icons.Default.Close,
                                    contentDescription = "Remove sketch",
                                    onClick = { viewModel.noteScribblePath = null },
                                    containerColor = Color.Black.copy(alpha = 0.55f),
                                    tint = Color.White,
                                    size = 36.dp,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                )
                            }
                        }

                        // Attached Voice Note banner
                        viewModel.noteAudioPath?.let { audioPath ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    CircleIconButton(
                                        icon = if (isAudioPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Play voice note",
                                        onClick = {
                                            if (isAudioPlaying) {
                                                mediaPlayer?.stop()
                                                mediaPlayer?.release()
                                                mediaPlayer = null
                                                isAudioPlaying = false
                                            } else {
                                                try {
                                                    mediaPlayer = MediaPlayer().apply {
                                                        setDataSource(audioPath)
                                                        prepare()
                                                        start()
                                                        setOnCompletionListener {
                                                            isAudioPlaying = false
                                                            release()
                                                            mediaPlayer = null
                                                        }
                                                    }
                                                    isAudioPlaying = true
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Failed to play audio", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        tint = Color.White,
                                        size = 40.dp
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Voice Note",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            if (isAudioPlaying) "Playing…" else "Tap to listen",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = {
                                        mediaPlayer?.stop()
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                        isAudioPlaying = false
                                        viewModel.noteAudioPath = null
                                    }) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete voice note",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        // Title row with smart AI title / lightbulb button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (viewModel.noteTitle.isEmpty()) {
                                    Text(
                                        text = "Write Title…",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp
                                        ),
                                        color = contentColor.copy(alpha = 0.35f)
                                    )
                                }
                                BasicTextField(
                                    value = viewModel.noteTitle,
                                    onValueChange = { viewModel.noteTitle = it },
                                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp,
                                        color = contentColor
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            // Lightbulb / quick generate title icon
                            IconButton(
                                onClick = {
                                    val suggested = generateSmartTitle(viewModel.noteContent)
                                    if (suggested.isNotBlank()) {
                                        viewModel.noteTitle = suggested
                                        Toast.makeText(context, "Title generated ✨", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Write some notes first to generate a title", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Lightbulb,
                                    contentDescription = "Generate Title",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Metadata line matching Image 2: "03/10/2026, 5:43 pm | 0 | Uncategorised"
                        val categoryText = if (noteTags.isEmpty()) "Uncategorised" else noteTags.joinToString(", ")
                        Text(
                            text = "$formattedDate | $wordCount words | $categoryText",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = contentColor.copy(alpha = 0.45f),
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        HorizontalDivider(
                            color = contentColor.copy(alpha = 0.08f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Content input area with ruled / grid / dot paper pattern support
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 220.dp)
                                .notePattern(
                                    skin = viewModel.noteSkin,
                                    color = contentColor.copy(alpha = 0.12f),
                                    spacing = (26 * fontSizeScale).dp
                                )
                        ) {
                            if (isPreviewMode) {
                                val highlighted = viewModel.noteContent.ifBlank { "*No content yet.*" }
                                MarkdownText(
                                    text = highlighted,
                                    color = contentColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                if (viewModel.noteContent.isEmpty()) {
                                    Text(
                                        text = "Write your notes…",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = (16 * fontSizeScale).sp,
                                            lineHeight = (26 * fontSizeScale).sp
                                        ),
                                        color = contentColor.copy(alpha = 0.35f)
                                    )
                                }
                                BasicTextField(
                                    value = contentValue,
                                    onValueChange = { newFieldValue ->
                                        var nextVal = newFieldValue
                                        val oldText = contentValue.text
                                        val newText = newFieldValue.text

                                        if (newText.length > oldText.length) {
                                            val diff = newText.length - oldText.length
                                            val insertPos = (newFieldValue.selection.start - diff).coerceAtLeast(0)
                                            val typedChunk = newText.substring(insertPos, (insertPos + diff).coerceAtMost(newText.length))
                                            if (isBoldToggledOn && !typedChunk.startsWith("**")) {
                                                val replaced = newText.substring(0, insertPos) + "**$typedChunk**" + newText.substring((insertPos + diff).coerceAtMost(newText.length))
                                                nextVal = TextFieldValue(replaced, TextRange((insertPos + 2 + diff).coerceIn(0, replaced.length)))
                                                isBoldToggledOn = false
                                            } else if (isItalicToggledOn && !typedChunk.startsWith("*")) {
                                                val replaced = newText.substring(0, insertPos) + "*$typedChunk*" + newText.substring((insertPos + diff).coerceAtMost(newText.length))
                                                nextVal = TextFieldValue(replaced, TextRange((insertPos + 1 + diff).coerceIn(0, replaced.length)))
                                                isItalicToggledOn = false
                                            }
                                        }

                                        contentValue = nextVal
                                        viewModel.noteContent = nextVal.text
                                    },
                                    visualTransformation = remember { MarkdownVisualTransformation() },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = (16 * fontSizeScale).sp,
                                        lineHeight = (26 * fontSizeScale).sp,
                                        color = contentColor
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // ── Action Chips Row (Matches Image 1 Create Note screen) ────
                Spacer(Modifier.height(14.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        SoftActionChip(
                            icon = Icons.Outlined.Label,
                            label = if (noteTags.isEmpty()) "Add Tag" else "${noteTags.size} Tags",
                            accent = AccentRose,
                            active = noteTags.isNotEmpty(),
                            glassy = glassyUiEnabled,
                            onClick = { showTagDialog = true }
                        )
                    }
                    item {
                        SoftActionChip(
                            icon = Icons.Outlined.Palette,
                            label = "Skins",
                            accent = AccentMint,
                            active = (viewModel.noteColor != 0 || viewModel.noteSkin != 0),
                            glassy = glassyUiEnabled,
                            onClick = { showSkinsSheet = true }
                        )
                    }
                    item {
                        Box {
                            SoftActionChip(
                                icon = Icons.Outlined.PhotoCamera,
                                label = "Image",
                                accent = MaterialTheme.colorScheme.primary,
                                active = (viewModel.noteImageUri != null),
                                glassy = glassyUiEnabled,
                                onClick = { showPhotoMenu = true }
                            )
                            PhotoSourceMenu(
                                expanded = showPhotoMenu,
                                onDismiss = { showPhotoMenu = false },
                                onTakePhoto = { launchCameraCapture() },
                                onPickPhoto = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onScanText = {
                                    val status = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                    if (status == PackageManager.PERMISSION_GRANTED) showOcrDialog = true
                                    else cameraPermissionLauncherForOcr.launch(Manifest.permission.CAMERA)
                                }
                            )
                        }
                    }
                    item {
                        SoftActionChip(
                            icon = Icons.Outlined.GraphicEq,
                            label = "Voice Note",
                            accent = AccentViolet,
                            active = (viewModel.noteAudioPath != null),
                            glassy = glassyUiEnabled,
                            onClick = {
                                val status = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                if (status == PackageManager.PERMISSION_GRANTED) showVoiceRecorder = true
                                else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        )
                    }
                    item {
                        SoftActionChip(
                            icon = Icons.Outlined.Draw,
                            label = "Draw",
                            accent = AccentAmber,
                            active = (viewModel.noteScribblePath != null),
                            glassy = glassyUiEnabled,
                            onClick = { showScribbleDialog = true }
                        )
                    }
                    item {
                        SoftActionChip(
                            icon = Icons.Outlined.DocumentScanner,
                            label = "Scan",
                            accent = TertiaryColor,
                            glassy = glassyUiEnabled,
                            onClick = {
                                val status = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                if (status == PackageManager.PERMISSION_GRANTED) showOcrDialog = true
                                else cameraPermissionLauncherForOcr.launch(Manifest.permission.CAMERA)
                            }
                        )
                    }
                }

                // ── AI Assist Section (Matches Image 1 Create Note screen) ───
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "AI Assist",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = contentColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Smart Tools",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AiAssistCard(
                            title = "Summarize Note",
                            icon = Icons.Outlined.AutoAwesome,
                            iconColor = AccentMint,
                            glassy = glassyUiEnabled,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (viewModel.noteContent.isBlank()) {
                                    Toast.makeText(context, "Write some notes first to summarize", Toast.LENGTH_SHORT).show()
                                } else {
                                    val summary = summarizeText(viewModel.noteContent)
                                    viewModel.noteContent += "\n\n📌 **Summary:**\n$summary"
                                    Toast.makeText(context, "Summary added ✨", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        AiAssistCard(
                            title = "Rewrite for Clarity",
                            icon = Icons.Outlined.Psychology,
                            iconColor = AccentViolet,
                            glassy = glassyUiEnabled,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (viewModel.noteContent.isBlank()) {
                                    Toast.makeText(context, "Write some notes first to polish", Toast.LENGTH_SHORT).show()
                                } else {
                                    val polished = polishTextForClarity(viewModel.noteContent)
                                    viewModel.noteContent = polished
                                    Toast.makeText(context, "Polished for clarity ✨", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AiAssistCard(
                            title = "Convert to Bullets",
                            icon = Icons.Outlined.FormatListBulleted,
                            iconColor = AccentRose,
                            glassy = glassyUiEnabled,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (viewModel.noteContent.isBlank()) {
                                    Toast.makeText(context, "Write some notes first", Toast.LENGTH_SHORT).show()
                                } else {
                                    val bullets = convertTextToBullets(viewModel.noteContent)
                                    viewModel.noteContent = bullets
                                    Toast.makeText(context, "Converted to checklist bullets ✨", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        AiAssistCard(
                            title = "Generate Title",
                            icon = Icons.Outlined.TextFields,
                            iconColor = AccentAmber,
                            glassy = glassyUiEnabled,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val suggested = generateSmartTitle(viewModel.noteContent)
                                if (suggested.isNotBlank()) {
                                    viewModel.noteTitle = suggested
                                    Toast.makeText(context, "Title generated ✨", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Write some notes first to generate a title", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // ── Dialogs & Bottom Sheets ──────────────────────────────────────────────

    // Voice recorder screen (Image 1 Screen 3)
    if (showVoiceRecorder) {
        VoiceRecorderScreen(
            onDismiss = { showVoiceRecorder = false },
            onSaved = { path ->
                viewModel.noteAudioPath = path
                showVoiceRecorder = false
                Toast.makeText(context, "Voice note attached", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Skins Bottom Sheet (Image 3)
    if (showSkinsSheet) {
        SkinsBottomSheet(
            selectedColor = viewModel.noteColor,
            selectedSkin = viewModel.noteSkin,
            onColorSelected = { viewModel.noteColor = it },
            onSkinSelected = { viewModel.noteSkin = it },
            onDismiss = { showSkinsSheet = false }
        )
    }

    // Scribble canvas dialog (Images 4 & 5)
    if (showScribbleDialog) {
        ScribbleCanvasDialog(
            onDismiss = {
                showScribbleDialog = false
                if (startAction == "draw") {
                    onNavigateBack()
                }
            },
            onSave = { path ->
                viewModel.noteScribblePath = path
                showScribbleDialog = false
                if (startAction == "draw") {
                    if (viewModel.noteTitle.isBlank()) {
                        viewModel.noteTitle = "Sketch Note"
                    }
                    viewModel.saveEditingNote()
                    onNavigateBack()
                }
            },
            initialScribblePath = viewModel.noteScribblePath
        )
    }

    // OCR scanner dialog
    if (showOcrDialog) {
        OcrScannerDialog(
            onDismiss = { showOcrDialog = false },
            onTextScanned = { scanned ->
                viewModel.noteContent += (if (viewModel.noteContent.isEmpty()) "" else "\n") + scanned
                showOcrDialog = false
            }
        )
    }

    // Speech recognition dialog
    if (showSpeechDialog) {
        SpeechRecognizerDialog(
            onDismiss = { showSpeechDialog = false },
            onResult = { text ->
                viewModel.noteContent += (if (viewModel.noteContent.isEmpty()) "" else " ") + text
            }
        )
    }

    // Tags dialog
    if (showTagDialog) {
        TagPickerEditorDialog(
            tags = tags,
            selectedTags = noteTags,
            onToggleTag = { viewModel.toggleTagForEditingNote(it) },
            onCreateTag = { name, color -> viewModel.createNewTag(name, color) },
            onDismiss = { showTagDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top App Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EditorTopAppBar(
    isNewNote: Boolean,
    isPinned: Boolean,
    isLocked: Boolean,
    onBackClick: () -> Unit,
    onPinClick: () -> Unit,
    onLockClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        CircleIconButton(
            icon = Icons.Default.ArrowBack,
            contentDescription = "Back",
            onClick = onBackClick,
            size = 40.dp
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (isNewNote) "Create Note" else "Edit Note",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = onLockClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Outlined.LockOpen,
                    contentDescription = "Lock Note",
                    tint = if (isLocked) AccentViolet else contentColor.copy(alpha = 0.65f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onPinClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "Pin Note",
                    tint = if (isPinned) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.65f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onShareClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share",
                    tint = contentColor.copy(alpha = 0.65f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            CircleIconButton(
                icon = Icons.Default.Done,
                contentDescription = "Save",
                onClick = onSaveClick,
                containerColor = MaterialTheme.colorScheme.primary,
                tint = Color.White,
                size = 40.dp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom Formatting Toolbar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EditorBottomBar(
    isPreviewMode: Boolean,
    showFindBar: Boolean,
    isBold: Boolean,
    isItalic: Boolean,
    onTogglePreview: () -> Unit,
    onToggleFind: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onHeading: () -> Unit,
    onChecklist: () -> Unit,
    onStrikethrough: () -> Unit,
    onCode: () -> Unit,
    onSketch: () -> Unit,
    onSpeech: () -> Unit,
    onCameraMenu: () -> Unit,
    contentColor: Color,
    glassy: Boolean
) {
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = cardSurfaceColor(glassy),
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            // Camera / Image menu button
            IconButton(onClick = onCameraMenu, modifier = Modifier.size(38.dp)) {
                Icon(
                    Icons.Outlined.PhotoCamera,
                    contentDescription = "Camera options",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            // Checklist
            IconButton(onClick = onChecklist, modifier = Modifier.size(38.dp)) {
                Icon(
                    Icons.Outlined.CheckBox,
                    contentDescription = "Checklist",
                    tint = contentColor.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp)
                )
            }
            // Heading Aa
            IconButton(onClick = onHeading, modifier = Modifier.size(38.dp)) {
                Text(
                    "Aa",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
            // Bold B (with active toggle indicator)
            IconButton(
                onClick = onBold,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isBold) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent)
            ) {
                Text(
                    "B",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 17.sp),
                    color = if (isBold) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.8f)
                )
            }
            // Italic I (with active toggle indicator)
            IconButton(
                onClick = onItalic,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isItalic) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent)
            ) {
                Text(
                    "I",
                    style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 17.sp),
                    color = if (isItalic) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.8f)
                )
            }
            // Sketch / Draw (User request: add sketch option in normal notes tool bar)
            IconButton(onClick = onSketch, modifier = Modifier.size(38.dp)) {
                Icon(
                    Icons.Outlined.Draw,
                    contentDescription = "Sketch canvas",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            // Speech to text
            IconButton(onClick = onSpeech, modifier = Modifier.size(38.dp)) {
                Icon(
                    Icons.Outlined.Mic,
                    contentDescription = "Speech to text",
                    tint = contentColor.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp)
                )
            }
            // Markdown Preview
            IconButton(onClick = onTogglePreview, modifier = Modifier.size(38.dp)) {
                Icon(
                    imageVector = if (isPreviewMode) Icons.Default.Edit else Icons.Outlined.Visibility,
                    contentDescription = "Toggle Preview",
                    tint = if (isPreviewMode) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AI Assist Card (Image 1 Screen 2)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AiAssistCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    glassy: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardSurfaceColor(glassy)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.14f))
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tag Picker Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TagPickerEditorDialog(
    tags: List<com.example.notes.data.local.TagEntity>,
    selectedTags: Set<String>,
    onToggleTag: (String) -> Unit,
    onCreateTag: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var newTagName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Note Tags") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        placeholder = { Text("New tag…") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    CircleIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Add tag",
                        containerColor = MaterialTheme.colorScheme.primary,
                        tint = Color.White,
                        size = 46.dp,
                        onClick = {
                            if (newTagName.isNotBlank()) {
                                val tagColors = listOf(0xFFFCA5A5, 0xFFFCD34D, 0xFF86EFAC, 0xFF93C5FD, 0xFFC4B5FD, 0xFFF9A8D4)
                                onCreateTag(newTagName.trim(), tagColors.random().toInt())
                                newTagName = ""
                            }
                        }
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    tags.forEach { tag ->
                        val isChecked = selectedTags.contains(tag.name)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onToggleTag(tag.name) }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            Checkbox(checked = isChecked, onCheckedChange = { onToggleTag(tag.name) })
                            Spacer(Modifier.width(8.dp))
                            Text(tag.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers: Smart Text Tools & Camera URI
// ─────────────────────────────────────────────────────────────────────────────

private fun generateSmartTitle(content: String): String {
    if (content.isBlank()) return ""
    val firstMeaningful = content.lines()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() && !it.startsWith("```") } ?: return ""
    val cleaned = firstMeaningful
        .replace(Regex("^#{1,6}\\s*"), "")
        .replace(Regex("^[-*>]\\s+"), "")
        .replace(Regex("^[-*\\[\\] xX]+\\s*"), "")
        .replace(Regex("[*~`_]"), "")
        .trim()
    val words = cleaned.split(Regex("\\s+")).take(5)
    return words.joinToString(" ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun summarizeText(content: String): String {
    val sentences = content.split(Regex("(?<=[.!?])\\s+"))
        .map { it.trim() }
        .filter { it.length > 10 }
    if (sentences.isEmpty()) return "• " + content.take(120)
    return sentences.take(3).joinToString("\n") { "• $it" }
}

private fun polishTextForClarity(content: String): String {
    return content.lines().joinToString("\n") { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty()) ""
        else {
            val capitalized = trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            if (!capitalized.endsWith(".") && !capitalized.endsWith("!") && !capitalized.endsWith("?") && !capitalized.startsWith("#") && !capitalized.startsWith("-")) {
                "$capitalized."
            } else capitalized
        }
    }
}

private fun convertTextToBullets(content: String): String {
    return content.lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString("\n") { line ->
            if (line.startsWith("- [ ]") || line.startsWith("- [x]")) line
            else "- [ ] " + line.removePrefix("-").removePrefix("•").trim()
        }
}

private fun createCameraImageUri(context: Context): Pair<Uri, File> {
    val dir = File(context.filesDir, "images").apply { mkdirs() }
    val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)
    return uri to file
}

private fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val dir = File(context.filesDir, "images").apply { mkdirs() }
        val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun authenticateBiometricInEditor(
    context: Context,
    title: String = "Vault Verification",
    subtitle: String = "Confirm your identity",
    onSuccess: () -> Unit,
    onError: (String) -> Unit = {}
) {
    val executor = ContextCompat.getMainExecutor(context)
    val activity = context as? androidx.fragment.app.FragmentActivity ?: run { onSuccess(); return }

    val prompt = androidx.biometric.BiometricPrompt(
        activity, executor,
        object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }
            override fun onAuthenticationFailed() {
                onError("Authentication failed")
            }
        }
    )
    val info = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setNegativeButtonText("Cancel")
        .build()
    prompt.authenticate(info)
}

@Composable
fun SpeechRecognizerDialog(
    onDismiss: () -> Unit,
    onResult: (String) -> Unit
) {
    val context = LocalContext.current
    var speechText by remember { mutableStateOf("Say something...") }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    
    DisposableEffect(Unit) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                speechText = "Listening..."
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                speechText = "Processing..."
            }
            override fun onError(error: Int) {
                speechText = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    else -> "Speech recognition error"
                }
            }
            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onResult(matches[0])
                }
                onDismiss()
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    speechText = matches[0]
                }
            }
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        }
        
        speechRecognizer.setRecognitionListener(listener)
        speechRecognizer.startListening(intent)
        
        onDispose {
            speechRecognizer.destroy()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Speech-to-Text") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = speechText, style = MaterialTheme.typography.bodyLarge)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun findWordBounds(text: String, pos: Int): Pair<Int, Int> {
    if (text.isEmpty()) return Pair(0, 0)
    val p = pos.coerceIn(0, text.length)
    var start = p
    while (start > 0 && text[start - 1].isLetterOrDigit()) {
        start--
    }
    var end = p
    while (end < text.length && text[end].isLetterOrDigit()) {
        end++
    }
    return Pair(start, end)
}

private fun isBoldAtCursor(value: TextFieldValue): Boolean {
    val text = value.text
    val sel = value.selection
    val start = minOf(sel.start, sel.end)
    val end = maxOf(sel.start, sel.end)
    if (start != end) {
        val s = text.substring(start, end)
        if (s.startsWith("**") && s.endsWith("**") && s.length >= 4) return true
        if (start >= 2 && end + 2 <= text.length && text.substring(start - 2, start) == "**" && text.substring(end, end + 2) == "**") return true
        return false
    }
    val (wStart, wEnd) = findWordBounds(text, start)
    if (wStart >= 2 && wEnd + 2 <= text.length && text.substring(wStart - 2, wStart) == "**" && text.substring(wEnd, wEnd + 2) == "**") return true
    val prevOpen = text.lastIndexOf("**", (start - 1).coerceAtLeast(0))
    if (prevOpen != -1) {
        val nextClose = text.indexOf("**", prevOpen + 2)
        if (nextClose != -1 && start in (prevOpen + 2)..nextClose) {
            return true
        }
    }
    return false
}

private fun isItalicAtCursor(value: TextFieldValue): Boolean {
    val text = value.text
    val sel = value.selection
    val start = minOf(sel.start, sel.end)
    val end = maxOf(sel.start, sel.end)
    if (start != end) {
        val s = text.substring(start, end)
        if (s.startsWith("*") && !s.startsWith("**") && s.endsWith("*") && !s.endsWith("**") && s.length >= 2) return true
        if (start >= 1 && end + 1 <= text.length && text[start - 1] == '*' && text.getOrNull(start - 2) != '*' && text[end] == '*' && text.getOrNull(end + 1) != '*') return true
        return false
    }
    val (wStart, wEnd) = findWordBounds(text, start)
    if (wStart >= 1 && wEnd + 1 <= text.length && text[wStart - 1] == '*' && text.getOrNull(wStart - 2) != '*' && text[wEnd] == '*' && text.getOrNull(wEnd + 1) != '*') return true
    val prevOpen = text.lastIndexOf("*", (start - 1).coerceAtLeast(0))
    if (prevOpen != -1 && text.getOrNull(prevOpen - 1) != '*' && text.getOrNull(prevOpen + 1) != '*') {
        val nextClose = text.indexOf("*", prevOpen + 1)
        if (nextClose != -1 && text.getOrNull(nextClose + 1) != '*' && start in (prevOpen + 1)..nextClose) {
            return true
        }
    }
    return false
}

