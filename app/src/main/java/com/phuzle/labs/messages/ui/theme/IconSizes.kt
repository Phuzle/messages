package com.phuzle.labs.messages.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

/**
 * Standard icon sizes, expressed in `sp` and converted to `dp` at the call site instead of being
 * fixed `dp` literals — a slew of icons added across Settings ended up hand-picked per screen
 * (13.dp here, 18.dp there, 20.dp somewhere else) with no shared scale, several of which read as
 * noticeably undersized next to their own label text. Sizing off `sp` fixes both: every icon that
 * asks for the same [IconSizes] token now actually renders at the same size, and — since `sp`
 * scales with the user's system font size setting the same way the label text next to it does —
 * an icon stays proportionate to its text instead of the text growing while the icon stays fixed.
 */
object IconSizes {
    /** Small glyphs inside pills/chips/inline actions (was a mix of 13-14.dp). */
    val Small: Dp @Composable @ReadOnlyComposable get() = with(LocalDensity.current) { 16.sp.toDp() }

    /** The default for a row's leading icon, menu items, and most standalone icon buttons (was a
     * mix of 18-20.dp — this is the one most of "notification > categories" / "privacy > app
     * lock" fell into, and the main source of the "too small" complaint). */
    val Medium: Dp @Composable @ReadOnlyComposable get() = with(LocalDensity.current) { 22.sp.toDp() }

    /** Emphasis icons — empty states, confirmation dialogs, update prompts. */
    val Large: Dp @Composable @ReadOnlyComposable get() = with(LocalDensity.current) { 30.sp.toDp() }
}
