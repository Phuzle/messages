package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/** [barContentHeight] (the bar's own visual height/margin) plus however tall the status bar is. */
@Composable
fun topBarContentPadding(barContentHeight: Dp): Dp =
    barContentHeight + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

/** [barContentHeight] (the bar's own visual height/margin) plus however tall the navigation bar is. */
@Composable
fun bottomBarContentPadding(barContentHeight: Dp): Dp =
    barContentHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
