package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** Which system bar this bar sits flush against, so it can pad its content clear of it. */
enum class BarInset { None, Top, Bottom }

/**
 * The app's solid, gently-elevated top/bottom bar — a One UI-style opaque surface with a soft
 * drop shadow, not a translucent glass tint (the earlier frosted-glass look read as messy/see-
 * through against scrolling content, especially on the bottom nav).
 *
 * The background always extends into the system bar area (edge-to-edge is mandatory as of API 35);
 * [inset] pads the *content* clear of the status/navigation bar without shrinking the painted
 * background, so the surface still shows behind the status/nav bar.
 */
@Composable
fun GlassBar(
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    inset: BarInset = BarInset.None,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = MessagesTheme.tokens
    val insetModifier = when (inset) {
        BarInset.Top -> Modifier.statusBarsPadding()
        BarInset.Bottom -> Modifier.navigationBarsPadding()
        BarInset.None -> Modifier
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp)
            .background(tokens.surface)
            .then(insetModifier)
            .height(height),
        contentAlignment = Alignment.CenterStart,
        content = content,
    )
}
