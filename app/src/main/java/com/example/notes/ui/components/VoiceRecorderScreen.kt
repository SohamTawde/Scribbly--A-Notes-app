package com.example.notes.ui.components

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.sqrt

private enum class RecState { IDLE, RECORDING, PAUSED }

/**
 * Full-screen voice note recorder with live waveform, timer and save / discard controls.
 * Caller must ensure RECORD_AUDIO permission is granted before showing this.
 */
@Composable
fun VoiceRecorderScreen(
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val dark = isAppInDarkTheme()

    var state by remember { mutableStateOf(RecState.IDLE) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var saved by remember { mutableStateOf(false) }
    val barCount = 42
    val amplitudes = remember { mutableStateListOf<Float>().apply { repeat(barCount) { add(0.06f) } } }

    fun start() {
        try {
            val dir = File(context.filesDir, "audio").apply { mkdirs() }
            val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
            val rec = createRecorder(context).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = rec
            outputFile = file
            state = RecState.RECORDING
        } catch (e: Exception) {
            Toast.makeText(context, "Recording failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun togglePause() {
        val rec = recorder ?: return start()
        try {
            if (state == RecState.RECORDING) { rec.pause(); state = RecState.PAUSED }
            else if (state == RecState.PAUSED) { rec.resume(); state = RecState.RECORDING }
        } catch (_: Exception) { }
    }

    fun stopRecorder() {
        try { recorder?.stop() } catch (_: Exception) { }
        recorder?.release()
        recorder = null
    }

    fun save() {
        if (recorder == null) { onDismiss(); return }
        stopRecorder()
        saved = true
        outputFile?.let { onSaved(it.absolutePath) }
    }

    fun discard() {
        stopRecorder()
        outputFile?.delete()
        onDismiss()
    }

    // Auto-start recording when opened
    LaunchedEffect(Unit) { start() }

    // Poll amplitude + timer
    LaunchedEffect(state) {
        while (state == RecState.RECORDING) {
            val amp = try { recorder?.maxAmplitude ?: 0 } catch (_: Exception) { 0 }
            val normalized = sqrt((amp / 32767f).coerceIn(0f, 1f)).coerceAtLeast(0.06f)
            amplitudes.removeAt(0)
            amplitudes.add(normalized)
            delay(80)
            elapsedMs += 80
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (recorder != null) {
                try { recorder?.stop() } catch (_: Exception) { }
                recorder?.release()
                if (!saved) outputFile?.delete()
            }
        }
    }

    Dialog(
        onDismissRequest = { discard() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        if (dark) listOf(Color(0xFF0B1120), Color(0xFF13203A))
                        else listOf(Color(0xFFEAF0F8), Color(0xFFDCE7F7))
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                // Top bar
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    CircleIconButton(Icons.Default.ArrowBack, "Back", onClick = { discard() }, modifier = Modifier.align(Alignment.CenterStart))
                    Text("Voice Note", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.Center))
                    RecordingBadge(state == RecState.RECORDING, modifier = Modifier.align(Alignment.CenterEnd))
                }

                Spacer(Modifier.height(48.dp))
                Text(
                    text = when (state) {
                        RecState.IDLE -> "Preparing microphone…"
                        RecState.RECORDING -> "Listening…\nSpeak clearly, your voice is being captured."
                        RecState.PAUSED -> "Paused\nTap the mic to continue recording."
                    },
                    style = MaterialTheme.typography.titleMedium.copy(lineHeight = 26.sp, fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)
                )

                Spacer(Modifier.weight(1f))
                LiveWaveform(amplitudes, active = state == RecState.RECORDING, color = primary)
                Spacer(Modifier.weight(1f))

                // Bottom curved panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(ArcTopShape)
                            .background(MaterialTheme.colorScheme.surface)
                    )
                    // Mic button
                    PulsingMicButton(
                        recording = state == RecState.RECORDING,
                        onClick = { togglePause() },
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 36.dp)
                    ) {
                        CircleIconButton(
                            Icons.Default.Check, "Save recording",
                            onClick = { save() },
                            containerColor = primary.copy(alpha = 0.12f),
                            tint = primary,
                            size = 52.dp
                        )
                        Text(
                            text = formatElapsed(elapsedMs),
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        CircleIconButton(
                            Icons.Default.Close, "Discard recording",
                            onClick = { discard() },
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            tint = MaterialTheme.colorScheme.error,
                            size = 52.dp
                        )
                    }
                }
            }
        }
    }
}

private val ArcTopShape = GenericShape { size, _ ->
    val top = size.height * 0.30f
    moveTo(0f, top)
    quadraticTo(size.width / 2f, -size.height * 0.10f, size.width, top)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

@Suppress("DEPRECATION")
private fun createRecorder(context: Context): MediaRecorder =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()

private fun formatElapsed(ms: Long): String {
    val total = ms / 1000
    return "%02d:%02d:%02d".format(total / 3600, (total % 3600) / 60, total % 60)
}

@Composable
private fun RecordingBadge(active: Boolean, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "recBadge")
    val alpha by t.animateFloat(1f, 0.25f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "recDot")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            Modifier.size(8.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = if (active) alpha else 0.3f))
        )
        Spacer(Modifier.width(6.dp))
        Text(if (active) "REC" else "PAUSED", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LiveWaveform(values: List<Float>, active: Boolean, color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 16.dp)
    ) {
        val n = values.size
        val gap = 4.dp.toPx()
        val barW = ((size.width - gap * (n - 1)) / n).coerceAtLeast(2f)
        val mid = size.height / 2
        values.forEachIndexed { i, v ->
            // Fade bars near the edges, like the reference design
            val edge = minOf(i, n - 1 - i).toFloat() / (n / 4f)
            val edgeFade = edge.coerceIn(0.15f, 1f)
            val h = if (edgeFade < 0.3f) barW else (size.height * v * edgeFade).coerceAtLeast(barW)
            val x = i * (barW + gap)
            drawRoundRect(
                color = color.copy(alpha = (if (active) 1f else 0.45f) * edgeFade.coerceAtLeast(0.35f)),
                topLeft = Offset(x, mid - h / 2),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2, barW / 2)
            )
        }
    }
}

@Composable
private fun PulsingMicButton(recording: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val t = rememberInfiniteTransition(label = "micPulse")
    val pulse by t.animateFloat(1f, 1.35f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "pulse")
    val pulseAlpha by t.animateFloat(0.35f, 0f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "pulseA")
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(110.dp)) {
        if (recording) {
            Box(Modifier.size(78.dp).scale(pulse).clip(CircleShape).background(primary.copy(alpha = pulseAlpha)))
        }
        Box(Modifier.size(92.dp).clip(CircleShape).background(primary.copy(alpha = 0.15f)))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .shadow(12.dp, CircleShape, spotColor = primary, ambientColor = primary)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(primary.copy(alpha = 0.85f), primary)))
                .clickable(onClick = onClick)
        ) {
            Icon(
                if (recording) Icons.Default.Pause else Icons.Default.Mic,
                contentDescription = if (recording) "Pause" else "Record",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
