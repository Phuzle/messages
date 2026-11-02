package com.phuzle.labs.messages.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class TriColor(val background: Color, val content: Color, val border: Color)

data class SwipeActionMeta(val label: String, val color: Color, val icon: ImageVector?)

/** Category chips: tonal accent fill when active, quiet alt-surface fill when not. No borders. */
fun categoryChipColors(active: Boolean, tokens: ThemeTokens): TriColor = if (active) {
    TriColor(tokens.accentSoft, tokens.accentLabel, Color.Transparent)
} else {
    TriColor(tokens.surfaceAlt, tokens.textSecondary, Color.Transparent)
}

/** Settings pill options (theme, swipe actions, schedule, app-lock method): mirrors `pillStyle(active, c)`. */
fun pillOptionColors(active: Boolean, tokens: ThemeTokens): TriColor = if (active) {
    TriColor(tokens.accentSoft, tokens.accentLabel, tokens.accentLabel)
} else {
    TriColor(Color.Transparent, tokens.textSecondary, tokens.border)
}

/** Swipe-reveal panel label/color/icon for a configured action, mirrors `actionMeta(action, unreadNow, c)`. */
fun swipeActionMeta(actionKey: String, unreadNow: Boolean, tokens: ThemeTokens): SwipeActionMeta = when (actionKey) {
    "archive" -> SwipeActionMeta("Archive", Color(0xFF64748B), Icons.Filled.Archive)
    "delete" -> SwipeActionMeta("Delete", tokens.danger, Icons.Filled.Delete)
    "toggleRead" -> SwipeActionMeta(
        if (unreadNow) "Mark read" else "Mark unread",
        tokens.accentDeep,
        if (unreadNow) Icons.Filled.MarkEmailRead else Icons.Filled.MarkEmailUnread,
    )
    else -> SwipeActionMeta("—", tokens.switchTrackOff, null)
}
