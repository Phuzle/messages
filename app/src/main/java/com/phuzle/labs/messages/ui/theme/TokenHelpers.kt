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

/** Category chips: shadcn-style flat toggle — filled dark/light when active, per the prototype. */
fun categoryChipColors(active: Boolean, tokens: ThemeTokens): TriColor = if (active) {
    TriColor(tokens.chipActiveBg, tokens.chipActiveText, tokens.chipActiveBg)
} else {
    TriColor(Color.Transparent, tokens.textSecondary, tokens.chipInactiveBorder)
}

/** Settings pill options (theme, swipe actions, schedule, app-lock method): mirrors `pillStyle(active, c)`. */
fun pillOptionColors(active: Boolean, tokens: ThemeTokens): TriColor = if (active) {
    TriColor(tokens.accentSoft, tokens.accent, tokens.accent)
} else {
    TriColor(Color.Transparent, tokens.textSecondary, tokens.border)
}

/** Swipe-reveal panel label/color/icon for a configured action, mirrors `actionMeta(action, unreadNow, c)`. */
fun swipeActionMeta(actionKey: String, unreadNow: Boolean, tokens: ThemeTokens): SwipeActionMeta = when (actionKey) {
    "archive" -> SwipeActionMeta("Archive", Color(0xFF64748B), Icons.Filled.Archive)
    "delete" -> SwipeActionMeta("Delete", tokens.danger, Icons.Filled.Delete)
    "toggleRead" -> SwipeActionMeta(
        if (unreadNow) "Mark read" else "Mark unread",
        tokens.accent,
        if (unreadNow) Icons.Filled.MarkEmailRead else Icons.Filled.MarkEmailUnread,
    )
    else -> SwipeActionMeta("—", tokens.switchTrackOff, null)
}
