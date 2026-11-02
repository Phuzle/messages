package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** [barContentHeight] (the bar's own visual height/margin) plus however tall the status bar is. */
@Composable
fun topBarContentPadding(barContentHeight: Dp): Dp =
    barContentHeight + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

/** Height of the 3-button navigation bar's reserved strip, or 0 with gesture navigation. The
 * tappable-element inset is the part of the nav bar that actually holds tappable buttons — zero
 * for the gesture pill, which content may safely draw under. */
@Composable
fun buttonNavBarHeight(): Dp = WindowInsets.tappableElement.asPaddingValues().calculateBottomPadding()

/** The part of the nav-bar inset that AppRoot has already given up to a solid strip (see
 * [LocalConsumedNavBarInset]) — screens must not pad for it a second time. */
val LocalConsumedNavBarInset = compositionLocalOf { 0.dp }

/** Nav-bar inset a screen still has to pad for itself: all of it with gesture navigation (content
 * draws under the pill), none of it once AppRoot's solid button-nav strip owns that space. */
@Composable
fun remainingNavBarInset(): Dp =
    (WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() - LocalConsumedNavBarInset.current).coerceAtLeast(0.dp)

/** [barContentHeight] (the bar's own visual height/margin) plus however tall the navigation bar is. */
@Composable
fun bottomBarContentPadding(barContentHeight: Dp): Dp = barContentHeight + remainingNavBarInset()
