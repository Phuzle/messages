package com.phuzle.labs.messages.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

/** Bolds the characters at [matchedIndices] — fuzzy search hit positions from FuzzyMatcher —
 * leaving everything else at its normal weight. Shared by the thread list (ThreadRow) and the
 * in-conversation search (ThreadScreen's MessageBubble). */
fun highlightedText(text: String, matchedIndices: Set<Int>): AnnotatedString {
    if (matchedIndices.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        matchedIndices.forEach { i -> if (i in text.indices) addStyle(SpanStyle(fontWeight = FontWeight.Bold), i, i + 1) }
    }
}
