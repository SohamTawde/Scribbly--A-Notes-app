package com.example.notes.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.notes.theme.DarkSurface
import com.example.notes.theme.LightSurface
import com.example.notes.theme.NoteColors
import com.example.notes.theme.getNoteBackgroundColor

@Composable
fun ColorPicker(
    selectedColorIndex: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == com.example.notes.theme.DarkBackground
    
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        items(NoteColors) { (index, baseColor) ->
            val isSelected = selectedColorIndex == index
            
            // Map index to the dark/light specific color
            val backgroundColor = getNoteBackgroundColor(index)
            
            // Selection scale animation
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.15f else 1.0f,
                animationSpec = spring(dampingRatio = 0.6f),
                label = "colorSwatchScale"
            )
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .border(
                        border = BorderStroke(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            }
                        ),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(index) }
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = if (index == 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            if (isDark) Color.White else Color.Black.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
