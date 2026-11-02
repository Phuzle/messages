package com.phuzle.labs.messages.ui.theme

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * The app's single elevation level: a soft shadow on Light/Sepia, and a 1dp hairline on Dark/Midnight
 * where a shadow would be invisible. Used for unread rows, received bubbles, grouped cards and tiles.
 */
@Composable
fun Modifier.softElevation(shape: Shape): Modifier {
    val tokens = MessagesTheme.tokens
    return if (MessagesTheme.isDark) {
        this.border(1.dp, tokens.border, shape)
    } else {
        this.shadow(
            elevation = 6.dp,
            shape = shape,
            ambientColor = tokens.textPrimary.copy(alpha = 0.10f),
            spotColor = tokens.textPrimary.copy(alpha = 0.18f),
        )
    }
}

/**
 * Bar-level elevation shared by [com.phuzle.labs.messages.ui.components.GlassBar] and the chip strip
 * under the dashboard's top bar: a soft shadow on Light/Sepia, a 1dp hairline on Dark/Midnight.
 * [atTop] puts the hairline on the bar's top edge instead of its bottom (for bottom bars).
 */
@Composable
fun Modifier.barElevation(atTop: Boolean = false): Modifier {
    val tokens = MessagesTheme.tokens
    return if (MessagesTheme.isDark) {
        this.drawBehind {
            val y = if (atTop) 0f else size.height
            drawLine(tokens.border, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
    } else {
        this.shadow(
            elevation = 6.dp,
            ambientColor = tokens.textPrimary.copy(alpha = 0.10f),
            spotColor = tokens.textPrimary.copy(alpha = 0.16f),
        )
    }
}
