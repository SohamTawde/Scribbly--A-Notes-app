package com.example.notes.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    val lines = text.split("\n")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            when {
                line.startsWith("# ") -> {
                    Text(
                        text = line.substring(2),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.substring(3),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                }
                line.startsWith("### ") -> {
                    Text(
                        text = line.substring(4),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                }
                line.startsWith("- [ ] ") -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Checkbox(checked = false, onCheckedChange = {}, enabled = false)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = parseMarkdown(line.substring(6)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = color
                        )
                    }
                }
                line.startsWith("- [x] ") -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Checkbox(checked = true, onCheckedChange = {}, enabled = false)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = parseMarkdown(line.substring(6)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = color,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    }
                }
                line.startsWith("- ") -> {
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(text = "•", style = MaterialTheme.typography.bodyLarge, color = color)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseMarkdown(line.substring(2)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = color
                        )
                    }
                }
                else -> {
                    Text(
                        text = parseMarkdown(line),
                        style = MaterialTheme.typography.bodyLarge,
                        color = color
                    )
                }
            }
        }
    }
}

fun parseMarkdown(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    while (i < text.length) {
        if (text.startsWith("**", i)) {
            val end = text.indexOf("**", i + 2)
            if (end != -1) {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                builder.append(text.substring(i + 2, end))
                builder.pop()
                i = end + 2
                continue
            }
        } else if (text.startsWith("*", i)) {
            val end = text.indexOf("*", i + 1)
            if (end != -1) {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                builder.append(text.substring(i + 1, end))
                builder.pop()
                i = end + 1
                continue
            }
        }
        builder.append(text[i])
        i++
    }
    return builder.toAnnotatedString()
}
