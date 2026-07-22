package com.phuzle.labs.messages.ui.theme

import androidx.compose.ui.graphics.Color

data class TriColor(val background: Color, val content: Color, val border: Color)

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

/** Swipe-reveal panel label + color for a configured action, mirrors `actionMeta(action, unreadNow, c)`. */
fun swipeActionMeta(actionKey: String, unreadNow: Boolean, tokens: ThemeTokens): Pair<String, Color> = when (actionKey) {
    "archive" -> "Archive" to Color(0xFF64748B)
    "delete" -> "Delete" to tokens.danger
    "toggleRead" -> (if (unreadNow) "Mark read" else "Mark unread") to tokens.accent
    else -> "—" to tokens.switchTrackOff
}
