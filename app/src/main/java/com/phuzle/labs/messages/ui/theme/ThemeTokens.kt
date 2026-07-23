package com.phuzle.labs.messages.ui.theme

import androidx.compose.ui.graphics.Color

/** 1:1 port of the prototype's per-render `c` color-token object. */
data class ThemeTokens(
    val bg: Color,
    val barBorder: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val chipActiveBg: Color,
    val chipActiveText: Color,
    val chipInactiveBorder: Color,
    val danger: Color,
    val overlayBg: Color,
    val modalBg: Color,
    val modalText: Color,
    val inputBg: Color,
    val switchTrackOff: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentText: Color,
)

/** The 4 selectable accent choices, each with a light- and dark-mode variant, per ACCENT_MAP. */
data class AccentOption(val hex: String, val light: Color, val dark: Color)

val ACCENT_OPTIONS = listOf(
    AccentOption("#3E6DF2", Color(0xFF3E6DF2), Color(0xFF7DA1FF)),
    AccentOption("#1FAA7A", Color(0xFF1FAA7A), Color(0xFF4FD3A0)),
    AccentOption("#E0503C", Color(0xFFE0503C), Color(0xFFFF8A76)),
    AccentOption("#0F172A", Color(0xFF0F172A), Color(0xFFF1F5F9)),
)

enum class ThemeMode(val key: String, val label: String) {
    System("system", "System"),
    Light("light", "Light"),
    Dark("dark", "Dark"),
    Amoled("amoled", "Midnight"),
    Sepia("sepia", "Sepia");

    companion object {
        fun fromKey(key: String): ThemeMode = entries.firstOrNull { it.key == key } ?: System
    }
}

/** Mirrors resolveDark(mode): amoled forces dark, sepia forces light, system defers to [systemDark]. */
fun resolveDark(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
    ThemeMode.Dark, ThemeMode.Amoled -> true
    ThemeMode.Light, ThemeMode.Sepia -> false
    ThemeMode.System -> systemDark
}

fun buildTheme(dark: Boolean, accentHex: String): ThemeTokens {
    val option = ACCENT_OPTIONS.firstOrNull { it.hex == accentHex } ?: ACCENT_OPTIONS[0]
    val accent = if (dark) option.dark else option.light
    val accentSoft = accent.copy(alpha = 0.16f)
    val accentText = if (dark) Color(0xFF0B0F17) else Color(0xFFFFFFFF)

    return if (!dark) {
        ThemeTokens(
            bg = Color(0xFFF8FAFC),
            barBorder = Color(0x140F172A),
            surface = Color(0xFFFFFFFF),
            surfaceAlt = Color(0xFFF1F5F9),
            border = Color(0xFFE2E8F0),
            textPrimary = Color(0xFF0F172A),
            textSecondary = Color(0xFF64748B),
            textTertiary = Color(0xFF94A3B8),
            chipActiveBg = Color(0xFF0F172A),
            chipActiveText = Color(0xFFFFFFFF),
            chipInactiveBorder = Color(0xFFE2E8F0),
            danger = Color(0xFFDC2626),
            overlayBg = Color(0x660F172A),
            modalBg = Color(0xFF0F172A),
            modalText = Color(0xFFFFFFFF),
            inputBg = Color(0x0D0F172A),
            switchTrackOff = Color(0xFFE2E8F0),
            accent = accent, accentSoft = accentSoft, accentText = accentText,
        )
    } else {
        ThemeTokens(
            bg = Color(0xFF0B0F17),
            barBorder = Color(0x14FFFFFF),
            surface = Color(0xFF131826),
            surfaceAlt = Color(0xFF1A2030),
            border = Color(0x17FFFFFF),
            textPrimary = Color(0xFFF1F5F9),
            textSecondary = Color(0xFF94A3B8),
            textTertiary = Color(0xFF64748B),
            chipActiveBg = Color(0xFFF1F5F9),
            chipActiveText = Color(0xFF0F172A),
            chipInactiveBorder = Color(0x24FFFFFF),
            danger = Color(0xFFF87171),
            overlayBg = Color(0x99000000),
            modalBg = Color(0xFFF1F5F9),
            modalText = Color(0xFF0F172A),
            inputBg = Color(0x0FFFFFFF),
            switchTrackOff = Color(0x29FFFFFF),
            accent = accent, accentSoft = accentSoft, accentText = accentText,
        )
    }
}

/** Mirrors applyThemeOverrides(mode, c): amoled/sepia layer extra overrides on top of the base palette. */
fun applyThemeOverrides(mode: ThemeMode, tokens: ThemeTokens): ThemeTokens = when (mode) {
    ThemeMode.Amoled -> tokens.copy(
        bg = Color(0xFF000000),
        surface = Color(0xFF0A0A0A),
        surfaceAlt = Color(0xFF121212),
    )
    ThemeMode.Sepia -> tokens.copy(
        bg = Color(0xFFF6EFE1),
        surface = Color(0xFFFFFBF3),
        surfaceAlt = Color(0xFFEFE6D3),
        border = Color(0xFFE3D6BA),
        textPrimary = Color(0xFF2B2013),
        textSecondary = Color(0xFF6B5D45),
        textTertiary = Color(0xFF9A8C6E),
    )
    else -> tokens
}

fun resolveTheme(mode: ThemeMode, accentHex: String, systemDark: Boolean): Pair<ThemeTokens, Boolean> {
    val dark = resolveDark(mode, systemDark)
    val tokens = applyThemeOverrides(mode, buildTheme(dark, accentHex))
    return tokens to dark
}
