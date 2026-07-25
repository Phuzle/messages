package com.phuzle.labs.messages.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

/** Bolds the characters at [matchedIndices] — fuzzy search hit positions from FuzzyMatcher —
 * leaving everything else at its normal weight. Shared by the thread list (ThreadRow) and the
 * in-conversation search (ThreadScreen's MessageBubble).
 *
 * [matchedIndices] are per-UTF-16-code-unit (FuzzyMatcher compares Chars, not code points), so an
 * emoji or other character outside the BMP — a surrogate pair, two Chars wide — can end up with
 * both of its halves matched as separate indices. Adding one SpanStyle per index individually
 * would then put a span *boundary* right in the middle of that surrogate pair; Android's text
 * shaping can't render a glyph split across two separately-styled spans and falls back to tofu —
 * literally two '?' glyphs where the one emoji should be. Merging adjacent matched indices into a
 * single contiguous span keeps any multi-char character whole. */
fun highlightedText(text: String, matchedIndices: Set<Int>): AnnotatedString {
    if (matchedIndices.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        val sorted = matchedIndices.filter { it in text.indices }.sorted()
        var runStart = -2
        var runEnd = -2
        for (i in sorted) {
            if (i == runEnd + 1) {
                runEnd = i
            } else {
                if (runStart >= 0) addStyle(SpanStyle(fontWeight = FontWeight.Bold), runStart, runEnd + 1)
                runStart = i
                runEnd = i
            }
        }
        if (runStart >= 0) addStyle(SpanStyle(fontWeight = FontWeight.Bold), runStart, runEnd + 1)
    }
}
