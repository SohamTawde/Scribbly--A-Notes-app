/*
 * Copyright (c) 2026 Soham Tawde. All rights reserved.
 * Scribbly - Notes & Tasks for Android
 */

package com.example.notes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notes.ui.components.NoteCard
import com.example.notes.ui.viewmodel.NoteViewModel
import com.example.notes.theme.DarkBackground

// ─────────────────────────────────────────────────────────────────
// Theme-aware design tokens for the Vault
// ─────────────────────────────────────────────────────────────────
private data class VaultTheme(
    val background: Brush,
    val headerText: Color,
    val accentPurple: Color,
    val gold: Color,
    val chipBg: Color,
    val chipBorder: Color,
    val chipText: Color,
    val glowColor: Color,
    val cardGlassy: Boolean
)

@Composable
private fun rememberVaultTheme(
    isDark: Boolean,
    isGlassy: Boolean
): VaultTheme {
    return when {
        isGlassy && isDark -> VaultTheme(
            background = Brush.verticalGradient(listOf(Color(0xFF0A0A1A), Color(0xFF130D2B), Color(0xFF0C0C1F))),
            headerText = Color.White,
            accentPurple = Color(0xFFB39DDB),
            gold = Color(0xFFFFD700),
            chipBg = Color.White.copy(alpha = 0.08f),
            chipBorder = Color(0xFFFFD700).copy(alpha = 0.3f),
            chipText = Color.White.copy(alpha = 0.75f),
            glowColor = Color(0xFF7C3AED),
            cardGlassy = true
        )
        isGlassy && !isDark -> VaultTheme(
            background = Brush.verticalGradient(listOf(Color(0xFFEDE9FE), Color(0xFFF5F3FF), Color(0xFFEDE9FE))),
            headerText = Color(0xFF1E1B4B),
            accentPurple = Color(0xFF7C3AED),
            gold = Color(0xFFB8860B),
            chipBg = Color.White.copy(alpha = 0.65f),
            chipBorder = Color(0xFF7C3AED).copy(alpha = 0.3f),
            chipText = Color(0xFF4C1D95),
            glowColor = Color(0xFFA855F7),
            cardGlassy = true
        )
        isDark -> VaultTheme(
            background = Brush.verticalGradient(listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))),
            headerText = Color.White,
            accentPurple = Color(0xFF9F7AEA),
            gold = Color(0xFFD4AF37),
            chipBg = Color.White.copy(alpha = 0.08f),
            chipBorder = Color(0xFFD4AF37).copy(alpha = 0.3f),
            chipText = Color.White.copy(alpha = 0.7f),
            glowColor = Color(0xFF6C3FC5),
            cardGlassy = true
        )
        else -> VaultTheme(
            // Light + non-glassy: clean lavender-tinted white
            background = Brush.verticalGradient(listOf(Color(0xFFF5F3FF), Color(0xFFEDE9FE), Color(0xFFF5F3FF))),
            headerText = Color(0xFF1E1B4B),
            accentPurple = Color(0xFF7C3AED),
            gold = Color(0xFFB8860B),
            chipBg = Color(0xFF7C3AED).copy(alpha = 0.08f),
            chipBorder = Color(0xFF7C3AED).copy(alpha = 0.2f),
            chipText = Color(0xFF4C1D95),
            glowColor = Color(0xFFA855F7),
            cardGlassy = false
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// VaultScreen
// ─────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: NoteViewModel,
    onNavigateToEditor: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lockedNotes by viewModel.lockedNotes.collectAsStateWithLifecycle()
    val glassyUiEnabled by viewModel.glassyUiEnabled.collectAsStateWithLifecycle()
    val isDark = MaterialTheme.colorScheme.background == DarkBackground

    val vault = rememberVaultTheme(isDark = isDark, isGlassy = glassyUiEnabled)

    // Pulsing animations
    val infiniteTransition = rememberInfiniteTransition(label = "vaultAnim")
    val lockScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "lockScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f, targetValue = 0.52f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(vault.background)
    ) {
        // Ambient glow blobs (decoration)
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = (-60).dp, y = 60.dp)
                .blur(110.dp)
                .background(vault.glowColor.copy(alpha = 0.35f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = (-80).dp)
                .blur(90.dp)
                .background(vault.accentPurple.copy(alpha = 0.2f), CircleShape)
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Glowing lock badge
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(vault.accentPurple, vault.glowColor)
                                        )
                                    )
                                    .border(1.dp, vault.gold.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Private Vault",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = vault.headerText
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = vault.headerText)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            if (lockedNotes.isEmpty()) {
                // ── Empty State ──────────────────────────────────────────────
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 2 }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .padding(36.dp)
                    ) {
                        // Pulsing glowing lock
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .scale(lockScale)
                                    .blur(40.dp)
                                    .background(vault.glowColor.copy(alpha = glowAlpha), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(vault.accentPurple, vault.glowColor)
                                        )
                                    )
                                    .border(2.dp, vault.gold.copy(alpha = 0.55f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        Text(
                            "Your vault is empty",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = vault.headerText
                            )
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Long-press any note → \"Lock note\" to move it here, protected behind your biometric.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = vault.headerText.copy(alpha = 0.5f),
                                lineHeight = 22.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(32.dp))

                        // Decorative divider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Divider(
                                modifier = Modifier.weight(1f),
                                color = vault.accentPurple.copy(alpha = 0.25f)
                            )
                            Text(
                                "  ✦  ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = vault.gold.copy(alpha = 0.6f)
                                )
                            )
                            Divider(
                                modifier = Modifier.weight(1f),
                                color = vault.accentPurple.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            } else {
                // ── Notes Grid ───────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    // Status chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(vault.chipBg)
                            .border(1.dp, vault.chipBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = vault.gold,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${lockedNotes.size} protected note${if (lockedNotes.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = vault.chipText,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(lockedNotes, key = { it.note.id }) { noteWithTags ->
                            Box(modifier = Modifier.animateItem()) {
                                NoteCard(
                                    noteWithTags = noteWithTags,
                                    onClick = { onNavigateToEditor(noteWithTags.note.id) },
                                    onLongClick = {},
                                    glassyUiEnabled = vault.cardGlassy
                                )
                                // Lock badge on each card
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(vault.accentPurple, vault.glowColor)
                                            )
                                        )
                                        .border(1.dp, vault.gold.copy(alpha = 0.55f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
