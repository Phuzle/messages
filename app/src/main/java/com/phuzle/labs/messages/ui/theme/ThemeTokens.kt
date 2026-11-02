package com.phuzle.labs.messages.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

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
    /** Passbook credit green — fixed semantic color independent of the user's chosen accent, same as [danger]. */
    val success: Color,
    val overlayBg: Color,
    val modalBg: Color,
    val modalText: Color,
    val inputBg: Color,
    val switchTrackOff: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentText: Color,
    /**
     * Calmer fill derived from [accent] for large surfaces (sent bubble, FAB, badges, switches, send
     * buttons). The raw [accent] is unchanged and still drives swatches and the Appearance picker.
     */
    val accentDeep: Color = Color.Unspecified,
    /** Content color to draw on [accentDeep] (white or near-black, whichever contrasts more). */
    val onAccentDeep: Color = Color.Unspecified,
    /** Accent-coloured text and tonal-chip content: [accentDeep] in light themes, [accent] in dark ones. */
    val accentLabel: Color = Color.Unspecified,
    /** Primary button fill on the inverse-surface modal (OTP), taken from the opposite theme's accent. */
    val modalAccent: Color = Color.Unspecified,
    val onModalAccent: Color = Color.Unspecified,
)

private val NearBlackOnAccent = Color(0xFF14161F)

private fun contrast(a: Color, b: Color): Float {
    val x = a.luminance()
    val y = b.luminance()
    return (maxOf(x, y) + 0.05f) / (minOf(x, y) + 0.05f)
}

private fun deepFrom(accent: Color, tokens: ThemeTokens, dark: Boolean): Color =
    if (dark) lerp(accent, tokens.bg, 0.60f) else lerp(accent, tokens.textPrimary, 0.40f)

private fun onDeepFor(deep: Color): Color =
    if (contrast(Color.White, deep) >= contrast(NearBlackOnAccent, deep)) Color.White else NearBlackOnAccent

/**
 * Derives the calm accent tokens from the chosen accent. Runs after [applyThemeOverrides] so Midnight
 * and Sepia mix against their own bg/text colors.
 */
fun withDerivedAccent(tokens: ThemeTokens, dark: Boolean, option: AccentOption): ThemeTokens {
    val deep = deepFrom(tokens.accent, tokens, dark)
    // The modal is an inverse surface, so its button borrows the opposite theme's accent.
    val modalAccent: Color
    val onModalAccent: Color
    if (dark) {
        val lightDeep = lerp(option.light, Color(0xFF1C2133), 0.40f)
        modalAccent = lightDeep
        onModalAccent = onDeepFor(lightDeep)
    } else {
        modalAccent = option.dark
        onModalAccent = Color(0xFF0B0F17)
    }
    return tokens.copy(
        accentDeep = deep,
        onAccentDeep = onDeepFor(deep),
        accentLabel = if (dark) tokens.accent else deep,
        modalAccent = modalAccent,
        onModalAccent = onModalAccent,
    )
}

/** The 4 selectable accent choices, each with a light- and dark-mode variant, per ACCENT_MAP. */
data class AccentOption(val hex: String, val light: Color, val dark: Color)

val ACCENT_OPTIONS = listOf(
    AccentOption("#3E6DF2", Color(0xFF3E6DF2), Color(0xFF7DA1FF)),
    AccentOption("#1FAA7A", Color(0xFF1FAA7A), Color(0xFF4FD3A0)),
    AccentOption("#E0503C", Color(0xFFE0503C), Color(0xFFFF8A76)),
    AccentOption("#7C3AED", Color(0xFF7C3AED), Color(0xFFB794F6)),
    AccentOption("#DB2777", Color(0xFFDB2777), Color(0xFFF783AC)),
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

    val base = if (!dark) {
        ThemeTokens(
            bg = Color(0xFFF3F4F8),
            barBorder = Color(0x141C2133),
            surface = Color(0xFFFFFFFF),
            surfaceAlt = Color(0xFFE8EAF1),
            border = Color(0xFFE4E7EF),
            textPrimary = Color(0xFF1C2133),
            textSecondary = Color(0xFF565D74),
            textTertiary = Color(0xFF666D85),
            chipActiveBg = Color(0xFF0F172A),
            chipActiveText = Color(0xFFFFFFFF),
            chipInactiveBorder = Color(0xFFE2E8F0),
            danger = Color(0xFFDC2626),
            success = Color(0xFF16A34A),
            overlayBg = Color(0x660F172A),
            modalBg = Color(0xFF0F172A),
            modalText = Color(0xFFFFFFFF),
            inputBg = Color(0x0D0F172A),
            switchTrackOff = Color(0xFFD3D8E3),
            accent = accent, accentSoft = accentSoft, accentText = accentText,
        )
    } else {
        ThemeTokens(
            bg = Color(0xFF0E111A),
            barBorder = Color(0x12FFFFFF),
            surface = Color(0xFF161A27),
            surfaceAlt = Color(0xFF1E2332),
            border = Color(0x12FFFFFF),
            textPrimary = Color(0xFFE8EAF3),
            textSecondary = Color(0xFFA3AAC0),
            textTertiary = Color(0xFF8087A0),
            chipActiveBg = Color(0xFFF1F5F9),
            chipActiveText = Color(0xFF0F172A),
            chipInactiveBorder = Color(0x24FFFFFF),
            danger = Color(0xFFF87171),
            success = Color(0xFF4ADE80),
            overlayBg = Color(0x99000000),
            modalBg = Color(0xFFF1F5F9),
            modalText = Color(0xFF0F172A),
            inputBg = Color(0x0FFFFFFF),
            switchTrackOff = Color(0x2EFFFFFF),
            accent = accent, accentSoft = accentSoft, accentText = accentText,
        )
    }
    return withDerivedAccent(base, dark, option)
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
        textTertiary = Color(0xFF75674A),
        switchTrackOff = Color(0xFFE3D6BA),
    )
    else -> tokens
}

fun resolveTheme(mode: ThemeMode, accentHex: String, systemDark: Boolean): Pair<ThemeTokens, Boolean> {
    val dark = resolveDark(mode, systemDark)
    val option = ACCENT_OPTIONS.firstOrNull { it.hex == accentHex } ?: ACCENT_OPTIONS[0]
    val overridden = applyThemeOverrides(mode, buildTheme(dark, accentHex))
    // Inputs sit on bars and cards, so they take the solid alt surface rather than a translucent tint.
    val tokens = withDerivedAccent(overridden.copy(inputBg = overridden.surfaceAlt), dark, option)
    return tokens to dark
}
