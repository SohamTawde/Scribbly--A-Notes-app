package com.example.notes.ui.components

import android.text.format.DateUtils
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.notes.data.local.NoteWithTags
import com.example.notes.theme.getNoteBackgroundColor
import com.example.notes.theme.getNoteOnColor
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    noteWithTags: NoteWithTags,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    glassyUiEnabled: Boolean = false,
    onMoreClick: (() -> Unit)? = null
) {
    val note = noteWithTags.note
    val tags = noteWithTags.tags
    val isDark = isAppInDarkTheme()

    val cardColor = when {
        glassyUiEnabled && note.color == 0 -> cardSurfaceColor(true)
        glassyUiEnabled -> getNoteBackgroundColor(note.color).copy(alpha = if (isDark) 0.35f else 0.65f)
        else -> getNoteBackgroundColor(note.color)
    }
    val border = when {
        glassyUiEnabled -> BorderStroke(1.dp, Color.White.copy(alpha = if (isDark) 0.14f else 0.6f))
        isDark && note.color == 0 -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        else -> null
    }
    val onColor = if (glassyUiEnabled) appContentColor(true) else getNoteOnColor(note.color)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "cardScale"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        CompositionLocalProvider(LocalContentColor provides onColor) {
            if (note.isLocked) {
                LockedNoteBody()
                return@CompositionLocalProvider
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                // Image / scribble thumbnail
                val thumb = note.imageUri ?: note.scribblePath
                if (thumb != null) {
                    AsyncImage(
                        model = File(thumb),
                        contentDescription = "Note image",
                        contentScale = if (note.imageUri != null) ContentScale.Crop else ContentScale.Fit,
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (note.imageUri != null) Color.Transparent else Color.White)
                    )
                }

                Column(modifier = Modifier.padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 14.dp)) {
                    // Title row + more button
                    Row(verticalAlignment = Alignment.Top) {
                        if (note.isPinned) {
                            Icon(
                                Icons.Default.PushPin, contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 3.dp, end = 4.dp).size(14.dp)
                            )
                        }
                        Text(
                            text = note.title.ifBlank { "Untitled" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(top = 1.dp)
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(onColor.copy(alpha = 0.06f))
                                .clickable { (onMoreClick ?: onLongClick)() }
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(16.dp))
                        }
                    }

                    // Content preview with checklist rendering
                    val previewLines = remember(note.content) { buildPreview(note.content) }
                    if (previewLines.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            previewLines.forEach { line ->
                                when (line) {
                                    is PreviewLine.Task -> Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (line.done) Icons.Default.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                            contentDescription = null,
                                            tint = if (line.done) MaterialTheme.colorScheme.primary else onColor.copy(alpha = 0.6f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            line.text,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 13.sp,
                                                textDecoration = if (line.done) TextDecoration.LineThrough else null
                                            ),
                                            color = onColor.copy(alpha = 0.72f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    is PreviewLine.Text -> Text(
                                        line.text,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                                        color = onColor.copy(alpha = 0.65f),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Tags
                    if (tags.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            tags.take(3).forEach { tag ->
                                val tagColor = if (tag.color != 0) Color(tag.color) else onColor.copy(alpha = 0.2f)
                                Text(
                                    text = tag.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = onColor.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (note.color == 0 && !glassyUiEnabled) tagColor.copy(alpha = if (isDark) 0.22f else 0.35f)
                                            else Color.White.copy(alpha = if (isDark) 0.12f else 0.7f)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Footer: media + date
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 6.dp)) {
                        if (note.audioFilePath != null) MediaDot(Icons.Outlined.GraphicEq, onColor)
                        if (note.scribblePath != null && note.imageUri != null) MediaDot(Icons.Outlined.Draw, onColor)
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Outlined.CalendarToday, contentDescription = null,
                            tint = onColor.copy(alpha = 0.45f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = relativeTime(note.updatedAt),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                            color = onColor.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LockedNoteBody() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
        ) {
            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text("Locked note", style = MaterialTheme.typography.titleSmall)
        Text(
            "Tap to unlock",
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun MediaDot(icon: ImageVector, onColor: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(end = 6.dp)
            .size(24.dp)
            .clip(CircleShape)
            .background(onColor.copy(alpha = 0.07f))
    ) {
        Icon(icon, contentDescription = null, tint = onColor.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
    }
}

private sealed class PreviewLine {
    data class Text(val text: String) : PreviewLine()
    data class Task(val text: String, val done: Boolean) : PreviewLine()
}

/** Turns markdown content into up to 4 clean preview lines (checklists rendered as tasks). */
private fun buildPreview(content: String): List<PreviewLine> {
    val result = mutableListOf<PreviewLine>()
    val textBuffer = StringBuilder()
    fun flush() {
        if (textBuffer.isNotBlank()) result += PreviewLine.Text(textBuffer.toString().trim())
        textBuffer.clear()
    }
    for (raw in content.lines()) {
        if (result.size >= 4) break
        val line = raw.trim()
        when {
            line.startsWith("- [ ]") -> { flush(); result += PreviewLine.Task(line.removePrefix("- [ ]").trim().ifBlank { "Task" }, false) }
            line.startsWith("- [x]", ignoreCase = true) -> { flush(); result += PreviewLine.Task(line.substring(5).trim().ifBlank { "Task" }, true) }
            line.startsWith("```") -> Unit
            line.isBlank() -> flush()
            else -> {
                val clean = line
                    .replace(Regex("^#{1,6}\\s*"), "")
                    .replace(Regex("^[-*>]\\s+"), "• ")
                    .replace("**", "").replace("~~", "").replace("`", "")
                    .replace(Regex("(?<!\\*)\\*(?!\\*)"), "")
                if (textBuffer.isNotEmpty()) textBuffer.append(' ')
                textBuffer.append(clean)
            }
        }
    }
    flush()
    return result.take(4)
}

private fun relativeTime(time: Long): String {
    val now = System.currentTimeMillis()
    return if (now - time < DateUtils.MINUTE_IN_MILLIS) "Just now"
    else DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
}
