package com.phuzle.labs.messages.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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

    // Status/nav bar icons are drawn by the OS, not this Compose tree, and default to light
    // (made for a dark backdrop) — with our own bars now transparent/edge-to-edge, the Light and
    // Sepia themes left them invisible against a light background. Kept in sync with whichever
    // theme actually resolved (System/Light/Dark/Midnight/Sepia all collapse to this one `dark`
    // flag), not just the raw system setting.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
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
