package com.phuzle.labs.messages.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

val LocalThemeTokens = compositionLocalOf {
    buildTheme(dark = false, accentHex = ACCENT_OPTIONS[0].hex)
}
val LocalIsDarkTheme = compositionLocalOf { false }

/** Reading `MessagesTheme.tokens`/`MessagesTheme.isDark` inside any composable under [MessagesTheme]. */
object MessagesTheme {
    val tokens: ThemeTokens
        @Composable get() = LocalThemeTokens.current
    val isDark: Boolean
        @Composable get() = LocalIsDarkTheme.current
}

@Composable
fun MessagesTheme(
    themeMode: ThemeMode,
    accentHex: String,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val (tokens, dark) = remember(themeMode, accentHex, systemDark) {
        resolveTheme(themeMode, accentHex, systemDark)
    }

    val colorScheme = remember(tokens, dark) {
        val base = if (dark) darkColorScheme() else lightColorScheme()
        base.copy(
            primary = tokens.accent,
            onPrimary = tokens.accentText,
            background = tokens.bg,
            onBackground = tokens.textPrimary,
            surface = tokens.surface,
            onSurface = tokens.textPrimary,
            surfaceVariant = tokens.surfaceAlt,
            outline = tokens.border,
            error = tokens.danger,
        )
    }

    CompositionLocalProvider(LocalThemeTokens provides tokens, LocalIsDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MessagesTypography,
            shapes = MessagesShapes,
            content = content,
        )
    }
}
