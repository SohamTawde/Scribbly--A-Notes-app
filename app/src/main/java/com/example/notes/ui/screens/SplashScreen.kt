package com.example.notes.ui.screens

import android.graphics.Paint
import android.graphics.PathMeasure
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// --- Easing Mathematics ---
private fun easeOutBounce(x: Float): Float {
    val n1 = 7.5625f
    val d1 = 2.75f
    var v = x
    return when {
        v < 1f / d1 -> n1 * v * v
        v < 2f / d1 -> {
            v -= 1.5f / d1
            n1 * v * v + 0.75f
        }
        v < 2.5f / d1 -> {
            v -= 2.25f / d1
            n1 * v * v + 0.9375f
        }
        else -> {
            v -= 2.625f / d1
            n1 * v * v + 0.984375f
        }
    }
}

private fun easeOutBack(x: Float, c1: Float = 1.70158f): Float {
    val c3 = c1 + 1f
    val t = x - 1f
    return 1f + c3 * t * t * t + c1 * t * t
}

private fun easeOutQuad(x: Float): Float = 1f - (1f - x) * (1f - x)

private data class SplashBlob(
    val xPercent: Float,
    val yPercent: Float,
    val maxRadius: Float,
    val color: Color,
    val delayMs: Float
)

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Single deterministic timeline clock (0ms to 5000ms)
    val timelineMs = remember { Animatable(0f) }

    // Idle floating animation that takes over once timeline completes
    val infiniteTransition = rememberInfiniteTransition(label = "idleFloating")
    val idleBobbing by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleBobbing"
    )
    val idleBubblePulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleBubblePulse"
    )

    LaunchedEffect(Unit) {
        // Runs once: 0ms -> 5000ms. When complete, does NOT re-run from start!
        timelineMs.animateTo(
            targetValue = 5000f,
            animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
        )
    }

    val t = timelineMs.value
    val isIdle = t >= 4500f

    // Background color transition (pastel lilac -> vivid purple)
    val bgPastel = Color(0xFFEDE8FB)
    val bgPurpleSolid = Color(0xFF7C3AED)
    val currentBgColor = when {
        t < 2500f -> bgPastel
        t < 2900f -> Color(0xFF8B5CF6)
        else -> bgPurpleSolid
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(currentBgColor)
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        // Reference design width: 390px
        val scale = (screenWidthPx / 390f).coerceAtMost(screenHeightPx / 800f)

        // Center stage position for the notepad
        val centerStageX = screenWidthPx / 2f
        val notepadBaseY = screenHeightPx * 0.40f

        // Notepad landing animation (Frame 2: 0.5s - 1.0s)
        val notepadY = when {
            t < 500f -> -300f * scale
            t <= 1000f -> {
                val dropProg = ((t - 500f) / 320f).coerceIn(0f, 1f)
                val startY = -300f * scale
                startY + (notepadBaseY - startY) * easeOutBounce(dropProg)
            }
            else -> {
                val bob = if (isIdle) idleBobbing * scale else 0f
                notepadBaseY + bob
            }
        }

        // Notepad ground shadow
        val shadowAlpha = when {
            t < 500f -> 0f
            t <= 1000f -> ((t - 500f) / 300f).coerceIn(0f, 0.7f)
            else -> 0.7f
        }
        val shadowRx = when {
            t < 500f -> 30f * scale
            t <= 1000f -> {
                val dropProg = ((t - 500f) / 320f).coerceIn(0f, 1f)
                (30f + dropProg * 65f) * scale
            }
            else -> 95f * scale
        }
        val shadowColor = if (t >= 2700f) Color(0xFF581C87) else Color(0xFFD3C7F5)

        // Frame 6: Purple splash explosion blobs
        val splashBlobs = remember {
            listOf(
                SplashBlob(0.50f, 0.45f, 550f, Color(0xFF7E22CE), 0f),
                SplashBlob(0.35f, 0.35f, 420f, Color(0xFF9333EA), 60f),
                SplashBlob(0.70f, 0.55f, 460f, Color(0xFF8B5CF6), 110f),
                SplashBlob(0.25f, 0.65f, 380f, Color(0xFF7C3AED), 150f),
                SplashBlob(0.80f, 0.25f, 360f, Color(0xFFA855F7), 180f)
            )
        }

        // Path of the scribble doodle in notepad local coordinates
        val doodleAndroidPath = remember(scale) {
            android.graphics.Path().apply {
                moveTo(-15f * scale, 0f)
                cubicTo(-40f * scale, -25f * scale, -60f * scale, 10f * scale, -45f * scale, 30f * scale)
                cubicTo(-30f * scale, 50f * scale, -5f * scale, 40f * scale, -10f * scale, 5f * scale)
                cubicTo(-15f * scale, -25f * scale, -55f * scale, -15f * scale, -70f * scale, 15f * scale)
                cubicTo(-85f * scale, 45f * scale, -75f * scale, 85f * scale, -45f * scale, 95f * scale)
                cubicTo(-15f * scale, 105f * scale, 15f * scale, 60f * scale, 45f * scale, 70f * scale)
                cubicTo(65f * scale, 78f * scale, 75f * scale, 65f * scale, 82f * scale, 55f * scale)
            }
        }
        val pathMeasure = remember(doodleAndroidPath) {
            PathMeasure(doodleAndroidPath, false)
        }
        val totalDoodleLength = pathMeasure.length

        // Paint for the continuous ink line
        val doodlePaint = remember(scale) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#18171E")
                strokeWidth = 5.5f * scale
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
        }

        // Calculate pen position & line reveal
        val drawProgress = when {
            t < 1500f -> 0f
            t <= 2500f -> ((t - 1500f) / 1000f).coerceIn(0f, 1f)
            else -> 1f
        }
        val currentDoodleLength = drawProgress * totalDoodleLength

        val nibPos = remember { FloatArray(2) }
        pathMeasure.getPosTan(currentDoodleLength, nibPos, null)

        // Convert local nib point to stage coordinates (tilted by -3.5 deg)
        val rad = Math.toRadians(-3.5)
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val rotNibX = nibPos[0] * cosA - nibPos[1] * sinA
        val rotNibY = nibPos[0] * sinA + nibPos[1] * cosA
        val doodleStartStage = Offset(centerStageX + rotNibX, notepadY + rotNibY)

        // Pen stage position
        val penStagePos: Offset = when {
            t < 1000f -> Offset(screenWidthPx + 150f, -100f)
            t < 1500f -> {
                val swoopProg = ((t - 1000f) / 450f).coerceIn(0f, 1f)
                val easedSwoop = easeOutBack(swoopProg)
                val startX = screenWidthPx + 150f
                val startY = -100f
                Offset(
                    startX + (doodleStartStage.x - startX) * easedSwoop,
                    startY + (doodleStartStage.y - startY) * easedSwoop
                )
            }
            t <= 2500f -> {
                doodleStartStage
            }
            else -> {
                val liftProg = ((t - 2500f) / 300f).coerceIn(0f, 1f)
                val liftY = easeOutQuad(liftProg) * -18f * scale
                val liftX = easeOutQuad(liftProg) * 12f * scale
                val bob = if (isIdle) idleBobbing * 0.8f * scale else 0f
                Offset(doodleStartStage.x + liftX, doodleStartStage.y + liftY + bob)
            }
        }

        // Pen rotation angle
        val penRotation = when {
            t < 1500f -> -44f
            t <= 2500f -> -44f + (sin(t * 0.05f) * 2f) // subtle writing wiggle
            else -> -42f
        }

        // Main Graphic Canvas: Background splashes, Notepad, Doodle, Ground Shadow, Pen
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Draw splash explosion blobs (Frame 6)
            if (t >= 2500f) {
                splashBlobs.forEach { blob ->
                    val blobT = ((t - (2500f + blob.delayMs)) / 600f).coerceIn(0f, 1f)
                    if (blobT > 0f) {
                        val radius = blob.maxRadius * scale * easeOutQuad(blobT)
                        drawCircle(
                            color = blob.color,
                            radius = radius,
                            center = Offset(screenWidthPx * blob.xPercent, screenHeightPx * blob.yPercent)
                        )
                    }
                }
            }

            // 2. Ambient floating bubbles (Frame 6-9)
            if (t >= 2800f) {
                val bubbleAlpha = if (isIdle) idleBubblePulse * 0.35f else 0.35f
                val bubbleColor = Color(0xFFA855F7).copy(alpha = bubbleAlpha)
                drawCircle(bubbleColor, radius = 70f * scale, center = Offset(screenWidthPx * 0.9f, screenHeightPx * 0.15f))
                drawCircle(bubbleColor, radius = 50f * scale, center = Offset(screenWidthPx * 0.1f, screenHeightPx * 0.40f))
                drawCircle(bubbleColor, radius = 80f * scale, center = Offset(screenWidthPx * 0.95f, screenHeightPx * 0.80f))
                drawCircle(bubbleColor, radius = 60f * scale, center = Offset(screenWidthPx * 0.08f, screenHeightPx * 0.72f))
            }

            // 3. Ground Shadow underneath Notepad
            if (shadowAlpha > 0f) {
                drawOval(
                    color = shadowColor.copy(alpha = shadowAlpha),
                    topLeft = Offset(centerStageX - shadowRx, notepadY + 140f * scale - 18f * scale),
                    size = Size(shadowRx * 2f, 36f * scale)
                )
            }

            // 4. Notepad landing burst sparks (Frame 2: 750ms - 980ms)
            if (t in 750f..980f) {
                val burstProg = ((t - 750f) / 230f).coerceIn(0f, 1f)
                val sparkAlpha = 1f - burstProg
                val sparkStroke = Stroke(width = 4.5f * scale, cap = StrokeCap.Round)
                val sparkColor = Color(0xFFA855F7).copy(alpha = sparkAlpha)
                val burstScale = 1f + burstProg * 0.3f

                withTransform({
                    scale(burstScale, burstScale, pivot = Offset(centerStageX, notepadY))
                }) {
                    // Top-Left Sparks
                    drawLine(sparkColor, Offset(centerStageX - 115f * scale, notepadY - 150f * scale), Offset(centerStageX - 135f * scale, notepadY - 165f * scale), strokeWidth = sparkStroke.width, cap = StrokeCap.Round)
                    drawLine(sparkColor, Offset(centerStageX - 105f * scale, notepadY - 180f * scale), Offset(centerStageX - 115f * scale, notepadY - 200f * scale), strokeWidth = sparkStroke.width, cap = StrokeCap.Round)
                    // Top-Right Sparks
                    drawLine(sparkColor, Offset(centerStageX + 95f * scale, notepadY - 160f * scale), Offset(centerStageX + 115f * scale, notepadY - 175f * scale), strokeWidth = sparkStroke.width, cap = StrokeCap.Round)
                    drawLine(sparkColor, Offset(centerStageX + 105f * scale, notepadY - 135f * scale), Offset(centerStageX + 125f * scale, notepadY - 135f * scale), strokeWidth = sparkStroke.width, cap = StrokeCap.Round)
                    // Bottom-Right Spark
                    drawLine(sparkColor, Offset(centerStageX + 100f * scale, notepadY + 100f * scale), Offset(centerStageX + 120f * scale, notepadY + 115f * scale), strokeWidth = sparkStroke.width, cap = StrokeCap.Round)
                    // Bottom-Left Spark
                    drawLine(sparkColor, Offset(centerStageX - 100f * scale, notepadY + 115f * scale), Offset(centerStageX - 115f * scale, notepadY + 135f * scale), strokeWidth = sparkStroke.width, cap = StrokeCap.Round)
                }
            }

            // 5. THE NOTEPAD (Tilted -3.5 deg, 3D Back Cover, White Page, Spiral Rings, Doodle)
            withTransform({
                translate(centerStageX, notepadY)
                rotate(-3.5f, pivot = Offset.Zero)
            }) {
                // A. 3D Dark Charcoal Back Cover (creates thick notebook depth)
                val backCoverPath = Path().apply {
                    moveTo(-75f * scale, -130f * scale)
                    lineTo(80f * scale, -120f * scale)
                    lineTo(110f * scale, 140f * scale)
                    lineTo(95f * scale, 175f * scale)
                    lineTo(-60f * scale, 160f * scale)
                    close()
                }
                drawPath(backCoverPath, color = Color(0xFF201E26))
                drawPath(backCoverPath, color = Color(0xFF121117), style = Stroke(width = 5f * scale, join = StrokeJoin.Round))

                // B. Front White Sheet (With page flare curve at bottom)
                val frontPagePath = Path().apply {
                    moveTo(-90f * scale, -120f * scale)
                    cubicTo(-30f * scale, -125f * scale, 40f * scale, -122f * scale, 90f * scale, -112f * scale)
                    cubicTo(88f * scale, -20f * scale, 85f * scale, 80f * scale, 75f * scale, 160f * scale)
                    cubicTo(20f * scale, 155f * scale, -40f * scale, 168f * scale, -80f * scale, 145f * scale)
                    cubicTo(-85f * scale, 60f * scale, -88f * scale, -30f * scale, -90f * scale, -120f * scale)
                    close()
                }
                drawPath(frontPagePath, color = Color.White)
                drawPath(frontPagePath, color = Color(0xFF18171E), style = Stroke(width = 5f * scale, join = StrokeJoin.Round, cap = StrokeCap.Round))

                // C. Top Dark Binding Strip
                val bindingPath = Path().apply {
                    moveTo(-90f * scale, -120f * scale)
                    cubicTo(-30f * scale, -125f * scale, 40f * scale, -122f * scale, 90f * scale, -112f * scale)
                    lineTo(87f * scale, -88f * scale)
                    cubicTo(35f * scale, -96f * scale, -35f * scale, -98f * scale, -89f * scale, -94f * scale)
                    close()
                }
                drawPath(bindingPath, color = Color(0xFF18171E))

                // D. Three Ring Holes (White Cutouts)
                drawCircle(Color.White, radius = 5f * scale, center = Offset(-55f * scale, -98f * scale))
                drawCircle(Color.White, radius = 5f * scale, center = Offset(-2f * scale, -97f * scale))
                drawCircle(Color.White, radius = 5f * scale, center = Offset(50f * scale, -95f * scale))

                // E. Three Thick Wire Rings
                val ringStroke = Stroke(width = 5f * scale, cap = StrokeCap.Round)
                val ring1 = Path().apply {
                    moveTo(-55f * scale, -98f * scale)
                    cubicTo(-68f * scale, -135f * scale, -42f * scale, -145f * scale, -48f * scale, -100f * scale)
                }
                val ring2 = Path().apply {
                    moveTo(-2f * scale, -97f * scale)
                    cubicTo(-15f * scale, -135f * scale, 11f * scale, -144f * scale, 5f * scale, -99f * scale)
                }
                val ring3 = Path().apply {
                    moveTo(50f * scale, -95f * scale)
                    cubicTo(37f * scale, -132f * scale, 63f * scale, -142f * scale, 57f * scale, -97f * scale)
                }
                drawPath(ring1, color = Color(0xFF18171E), style = ringStroke)
                drawPath(ring2, color = Color(0xFF18171E), style = ringStroke)
                drawPath(ring3, color = Color(0xFF18171E), style = ringStroke)

                // F. The Continuous Ink Scribble Doodle
                if (currentDoodleLength > 0f) {
                    val subPath = android.graphics.Path()
                    pathMeasure.getSegment(0f, currentDoodleLength, subPath, true)
                    drawContext.canvas.nativeCanvas.drawPath(subPath, doodlePaint)
                }
            }

            // 6. Pen Speed Streak (Frame 3: 1.0s - 1.5s)
            if (t in 1050f..1450f) {
                val streakAlpha = 0.8f
                drawLine(
                    Color(0xFFC4B5FD).copy(alpha = streakAlpha),
                    Offset(penStagePos.x + 40f * scale, penStagePos.y - 70f * scale),
                    Offset(penStagePos.x + 90f * scale, penStagePos.y - 140f * scale),
                    strokeWidth = 4f * scale,
                    cap = StrokeCap.Round
                )
            }

            // 7. THE STYLUS PEN (Dual-tone cyan/azure, teal grip, graphite nib)
            if (t >= 1000f) {
                withTransform({
                    translate(penStagePos.x, penStagePos.y)
                    rotate(penRotation, pivot = Offset.Zero)
                }) {
                    drawStylus(scale)
                }
            }
        }

        // Overlay: Badges and "Get Started" Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // "All your ideas" Cyan Card + "in one place" Pill Badge
            val badgeScale = when {
                t < 3000f -> 0f
                t <= 3500f -> {
                    val prog = ((t - 3000f) / 350f).coerceIn(0f, 1f)
                    easeOutBack(prog)
                }
                else -> 1f
            }

            if (badgeScale > 0f) {
                val badgeBob = if (isIdle) -idleBobbing * 0.5f else 0f
                Box(
                    modifier = Modifier
                        .offset(y = badgeBob.dp)
                        .scale(badgeScale)
                        .rotate(-4f)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = Color(0xFF0284C7),
                            ambientColor = Color(0xFF0284C7)
                        )
                ) {
                    // 3D bottom bevel backing
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0284C7),
                        modifier = Modifier
                            .offset(y = 5.dp)
                            .matchParentSize()
                    ) {}

                    // Main Cyan Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF38BDF8),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 26.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "All your ideas",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                letterSpacing = (-0.5).sp
                            )

                            // Frame 8: "in one place" White Pill Badge
                            val pillScale = when {
                                t < 3500f -> 0f
                                t <= 4000f -> {
                                    val pillProg = ((t - 3500f) / 300f).coerceIn(0f, 1f)
                                    easeOutBack(pillProg)
                                }
                                else -> 1f
                            }

                            if (pillScale > 0f) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    shadowElevation = 6.dp,
                                    modifier = Modifier
                                        .scale(pillScale)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "in one place",
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp,
                                        letterSpacing = (-0.3).sp,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Frame 9: "Get Started" Button (Pops up at 4.0s - 4.5s)
            val btnScale = when {
                t < 4000f -> 0f
                t <= 4500f -> {
                    val prog = ((t - 4000f) / 320f).coerceIn(0f, 1f)
                    easeOutBack(prog)
                }
                else -> 1f
            }

            if (btnScale > 0f) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF18181B),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .scale(btnScale)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSplashComplete() }
                        )
                ) {
                    Text(
                        text = "Get Started",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = (-0.2).sp,
                        modifier = Modifier.padding(horizontal = 52.dp, vertical = 15.dp)
                    )
                }
            }
        }
    }
}

// Vector Stylus drawing helper
private fun DrawScope.drawStylus(scale: Float) {
    val penStroke = Stroke(width = 3.5f * scale, join = StrokeJoin.Round)

    // 1. Left Highlight Facet (Bright Cyan)
    val leftBody = Path().apply {
        moveTo(-11f * scale, -38f * scale)
        lineTo(0f, -38f * scale)
        lineTo(0f, -200f * scale)
        lineTo(-11f * scale, -195f * scale)
        close()
    }
    drawPath(leftBody, color = Color(0xFF38BDF8))
    drawPath(leftBody, color = Color(0xFF18171E), style = penStroke)

    // 2. Right Shadow Facet (Deep Azure)
    val rightBody = Path().apply {
        moveTo(0f, -38f * scale)
        lineTo(11f * scale, -38f * scale)
        lineTo(11f * scale, -195f * scale)
        lineTo(0f, -200f * scale)
        close()
    }
    drawPath(rightBody, color = Color(0xFF0284C7))
    drawPath(rightBody, color = Color(0xFF18171E), style = penStroke)

    // 3. Top Cap
    val topCap = Path().apply {
        moveTo(-11f * scale, -195f * scale)
        cubicTo(
            -11f * scale, -212f * scale,
            11f * scale, -212f * scale,
            11f * scale, -195f * scale
        )
        close()
    }
    drawPath(topCap, color = Color(0xFF38BDF8))
    drawPath(topCap, color = Color(0xFF18171E), style = penStroke)

    // 4. Teal / Aqua Grip Collar
    val grip = Path().apply {
        moveTo(-11f * scale, -38f * scale)
        lineTo(11f * scale, -38f * scale)
        lineTo(7f * scale, -14f * scale)
        lineTo(-7f * scale, -14f * scale)
        close()
    }
    drawPath(grip, color = Color(0xFF14B8A6))
    drawPath(grip, color = Color(0xFF18171E), style = penStroke)

    // 5. Graphite Black Nib (Tapering to (0,0))
    val nib = Path().apply {
        moveTo(-7f * scale, -14f * scale)
        lineTo(7f * scale, -14f * scale)
        lineTo(0f, 0f)
        close()
    }
    drawPath(nib, color = Color(0xFF18171E))

    // 6. Pen Highlight reflection line
    drawLine(
        color = Color.White.copy(alpha = 0.45f),
        start = Offset(6f * scale, -140f * scale),
        end = Offset(6f * scale, -45f * scale),
        strokeWidth = 3f * scale,
        cap = StrokeCap.Round
    )
}
