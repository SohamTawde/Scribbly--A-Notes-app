package com.example.notes.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration

/**
 * VisualTransformation that renders rich formatting (Bold, Italic, Strikethrough)
 * in the editor while hiding the raw markdown delimiters (**, *, ~~) from the user.
 * Provides a verified monotonic OffsetMapping to ensure seamless cursor movement and typing.
 */
class MarkdownVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val spans = mutableListOf<FormatSpan>()

        // 1. Bold + Italic: ***text***
        val boldItalicRegex = Regex("\\*\\*\\*(.+?)\\*\\*\\*")
        boldItalicRegex.findAll(raw).forEach { match ->
            val g = match.groups[1]
            if (g != null && g.value.isNotEmpty()) {
                spans.add(
                    FormatSpan(
                        openStart = match.range.first,
                        openEnd = match.range.first + 3,
                        closeStart = match.range.last - 2,
                        closeEnd = match.range.last + 1,
                        style = SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                    )
                )
            }
        }

        // 2. Bold: **text** (excluding ***)
        val boldRegex = Regex("(?<!\\*)\\*\\*(?!\\*)(.+?)(?<!\\*)\\*\\*(?!\\*)")
        boldRegex.findAll(raw).forEach { match ->
            val g = match.groups[1]
            if (g != null && g.value.isNotEmpty()) {
                val start = match.range.first
                val end = match.range.last + 1
                if (spans.none { it.overlaps(start, end) }) {
                    spans.add(
                        FormatSpan(
                            openStart = start,
                            openEnd = start + 2,
                            closeStart = end - 2,
                            closeEnd = end,
                            style = SpanStyle(fontWeight = FontWeight.Bold)
                        )
                    )
                }
            }
        }

        // 3. Italic: *text* (excluding ** and ***)
        val italicRegex = Regex("(?<!\\*)\\*(?!\\*)([^*\\n]+?)(?<!\\*)\\*(?!\\*)")
        italicRegex.findAll(raw).forEach { match ->
            val g = match.groups[1]
            if (g != null && g.value.isNotEmpty()) {
                val start = match.range.first
                val end = match.range.last + 1
                if (spans.none { it.overlaps(start, end) }) {
                    spans.add(
                        FormatSpan(
                            openStart = start,
                            openEnd = start + 1,
                            closeStart = end - 1,
                            closeEnd = end,
                            style = SpanStyle(fontStyle = FontStyle.Italic)
                        )
                    )
                }
            }
        }

        // 4. Strikethrough: ~~text~~
        val strikeRegex = Regex("~~([^~\\n]+?)~~")
        strikeRegex.findAll(raw).forEach { match ->
            val g = match.groups[1]
            if (g != null && g.value.isNotEmpty()) {
                val start = match.range.first
                val end = match.range.last + 1
                if (spans.none { it.overlaps(start, end) }) {
                    spans.add(
                        FormatSpan(
                            openStart = start,
                            openEnd = start + 2,
                            closeStart = end - 2,
                            closeEnd = end,
                            style = SpanStyle(textDecoration = TextDecoration.LineThrough)
                        )
                    )
                }
            }
        }

        if (spans.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        // Mark delimiter characters
        val isDelim = BooleanArray(raw.length)
        for (span in spans) {
            for (i in span.openStart until span.openEnd.coerceAtMost(raw.length)) {
                isDelim[i] = true
            }
            for (i in span.closeStart.coerceAtLeast(0) until span.closeEnd.coerceAtMost(raw.length)) {
                isDelim[i] = true
            }
        }

        val transformedBuilder = AnnotatedString.Builder()
        val origToTrans = IntArray(raw.length + 1)
        val nonDelimCount = isDelim.count { !it }
        val transToOrig = IntArray(nonDelimCount + 1)

        var transIdx = 0
        for (origIdx in raw.indices) {
            origToTrans[origIdx] = transIdx
            if (!isDelim[origIdx]) {
                transToOrig[transIdx] = origIdx
                transformedBuilder.append(raw[origIdx])
                transIdx++
            }
        }
        origToTrans[raw.length] = transIdx
        transToOrig[transIdx] = raw.length

        // Apply styles to the transformed annotated string
        for (span in spans) {
            val tStart = origToTrans[span.openEnd.coerceIn(0, raw.length)]
            val tEnd = origToTrans[span.closeStart.coerceIn(0, raw.length)]
            if (tStart < tEnd) {
                transformedBuilder.addStyle(span.style, tStart, tEnd)
            }
        }

        val finalTransformed = transformedBuilder.toAnnotatedString()
        val finalTransLength = transIdx

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                return origToTrans[clamped].coerceIn(0, finalTransLength)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, finalTransLength)
                return transToOrig[clamped].coerceIn(0, raw.length)
            }
        }

        return TransformedText(finalTransformed, offsetMapping)
    }

    private data class FormatSpan(
        val openStart: Int,
        val openEnd: Int,
        val closeStart: Int,
        val closeEnd: Int,
        val style: SpanStyle
    ) {
        fun overlaps(start: Int, end: Int): Boolean {
            return !(end <= openStart || start >= closeEnd)
        }
    }
}
