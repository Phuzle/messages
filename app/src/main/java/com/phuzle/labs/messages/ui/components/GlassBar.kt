package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/**
 * The prototype's frosted top/bottom bar. True backdrop blur-through of scrolling content isn't
 * achievable with plain Jetpack Compose (no first-party equivalent to CSS `backdrop-filter`
 * without a third-party compositing library) — this renders the same translucent tint + hairline
 * border the design specifies, without the blur-through of what's underneath.
 */
@Composable
fun GlassBar(
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    hairlineAtBottom: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = MessagesTheme.tokens
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
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
            },
        contentAlignment = Alignment.CenterStart,
        content = content,
    )
}
