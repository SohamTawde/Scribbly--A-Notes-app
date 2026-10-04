package com.example.notes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notes.ui.viewmodel.NoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NoteViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val glassyUiEnabled by viewModel.glassyUiEnabled.collectAsState()
    val fontSizeScale by viewModel.fontSizeScale.collectAsState()
    val tags by viewModel.tags.collectAsState(initial = emptyList())
    val isDark = MaterialTheme.colorScheme.background == com.example.notes.theme.DarkBackground
    val glassyContentColor = if (isDark) Color.White else Color(0xFF1E1B4B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = if (glassyUiEnabled) glassyContentColor else LocalContentColor.current
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = if (glassyUiEnabled) glassyContentColor else LocalContentColor.current
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier.background(
            if (glassyUiEnabled) Color.Transparent else MaterialTheme.colorScheme.background
        )
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Mode Section Card
            SettingsCard(glassyUiEnabled = glassyUiEnabled, isDark = isDark) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brightness4,
                            contentDescription = "Theme Icon",
                            tint = if (glassyUiEnabled) {
                                if (isDark) Color(0xFF93C5FD) else Color(0xFF4338CA)
                            } else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Theme Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (glassyUiEnabled) glassyContentColor else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (mode, label) ->
                            val isSelected = themeMode == mode
                            val containerColor = if (isSelected) {
                                if (glassyUiEnabled) {
                                    if (isDark) Color.White.copy(alpha = 0.20f) else Color(0xFF4338CA).copy(alpha = 0.15f)
                                } else MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            }
                            val labelColor = if (isSelected) {
                                if (glassyUiEnabled) glassyContentColor else MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                if (glassyUiEnabled) glassyContentColor.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(containerColor)
                                    .clickable { viewModel.setThemeMode(mode) }
                            ) {
                                Text(
                                    text = label,
                                    color = labelColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // Glassy UI Toggle Card
            SettingsCard(glassyUiEnabled = glassyUiEnabled, isDark = isDark) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Glassy UI Icon",
                        tint = if (glassyUiEnabled) {
                            if (isDark) Color(0xFFC4B5FD) else Color(0xFF4338CA)
                        } else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Glassy UI (Glassmorphism)",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (glassyUiEnabled) glassyContentColor else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Frosted glass panels and mesh gradients",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (glassyUiEnabled) glassyContentColor.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = glassyUiEnabled,
                        onCheckedChange = { viewModel.setGlassyUiEnabled(it) },
                        colors = if (glassyUiEnabled) {
                            SwitchDefaults.colors(
                                checkedThumbColor = if (isDark) Color.White else Color(0xFF4338CA),
                                checkedTrackColor = if (isDark) Color.White.copy(alpha = 0.25f) else Color(0xFF4338CA).copy(alpha = 0.20f)
                            )
                        } else {
                            SwitchDefaults.colors()
                        }
                    )
                }
            }

            // Font Size Card
            SettingsCard(glassyUiEnabled = glassyUiEnabled, isDark = isDark) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Font Size",
                            tint = if (glassyUiEnabled) {
                                if (isDark) Color(0xFF93C5FD) else Color(0xFF4338CA)
                            } else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Font Size",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (glassyUiEnabled) glassyContentColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.85f to "Small", 1.0f to "Medium", 1.18f to "Large").forEach { (scale, label) ->
                            val isSelected = fontSizeScale == scale
                            val containerColor = if (isSelected) {
                                if (glassyUiEnabled) {
                                    if (isDark) Color.White.copy(alpha = 0.20f) else Color(0xFF4338CA).copy(alpha = 0.15f)
                                } else MaterialTheme.colorScheme.primaryContainer
                            } else Color.Transparent
                            val labelColor = if (isSelected) {
                                if (glassyUiEnabled) glassyContentColor else MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                if (glassyUiEnabled) glassyContentColor.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(containerColor)
                                    .clickable { viewModel.setFontSizeScale(scale) }
                            ) {
                                Text(
                                    text = label,
                                    color = labelColor,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Manage Tags Card
            SettingsCard(glassyUiEnabled = glassyUiEnabled, isDark = isDark) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = "Tags",
                            tint = if (glassyUiEnabled) {
                                if (isDark) Color(0xFFC4B5FD) else Color(0xFF4338CA)
                            } else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Manage Tags",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (glassyUiEnabled) glassyContentColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (tags.isEmpty()) {
                        Text(
                            text = "No tags yet. Create one from the note editor.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (glassyUiEnabled) glassyContentColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        tags.forEach { tag ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (tag.color != 0) Color(tag.color).copy(alpha = 0.20f)
                                           else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "#${tag.name}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = if (tag.color != 0) Color(tag.color)
                                               else (if (glassyUiEnabled) glassyContentColor else MaterialTheme.colorScheme.onSurface),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { viewModel.deleteTag(tag.name) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete tag ${tag.name}",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (tags.last() != tag) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }

            // Information Card
            SettingsCard(glassyUiEnabled = glassyUiEnabled, isDark = isDark) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info Icon",
                        tint = if (glassyUiEnabled) {
                            if (isDark) Color(0xFF93C5FD) else Color(0xFF4338CA)
                        } else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "App Version",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (glassyUiEnabled) glassyContentColor else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "v2.0 (Material You & Glassy)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (glassyUiEnabled) glassyContentColor.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    glassyUiEnabled: Boolean,
    isDark: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    if (glassyUiEnabled) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.50f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    } else {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}
