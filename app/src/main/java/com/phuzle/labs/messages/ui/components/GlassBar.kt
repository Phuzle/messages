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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** Which system bar this bar sits flush against, so it can pad its content clear of it. */
enum class BarInset { None, Top, Bottom }

/**
 * The prototype's frosted top/bottom bar. True backdrop blur-through of scrolling content isn't
 * achievable with plain Jetpack Compose (no first-party equivalent to CSS `backdrop-filter`
 * without a third-party compositing library) — this renders the same translucent tint + hairline
 * border the design specifies, without the blur-through of what's underneath.
 *
 * The tint+background always extends into the system bar area (edge-to-edge is mandatory as of
 * API 35); [inset] pads the *content* clear of the status/navigation bar without shrinking the
 * painted background, so the glass tint still shows behind the status/nav bar.
 */
@Composable
fun GlassBar(
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    hairlineAtBottom: Boolean = true,
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
            .background(tokens.barBg)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val y = if (hairlineAtBottom) size.height - strokeWidth / 2 else strokeWidth / 2
                drawLine(
                    color = tokens.barBorder,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = strokeWidth,
                )
            }
            .then(insetModifier)
            .height(height),
        contentAlignment = Alignment.CenterStart,
        content = content,
    )
}
