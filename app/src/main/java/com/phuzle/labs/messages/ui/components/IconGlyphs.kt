package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** The design's 3-bar hamburger glyph (hand-drawn divs in the source), ported to a tiny Canvas. */
@Composable
fun HamburgerIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 18.dp, height = 13.dp)) {
        val barHeight = 2.dp.toPx()
        val gap = (size.height - barHeight) / 2
        for (i in 0..2) {
            drawRect(color = color, topLeft = Offset(0f, i * gap), size = androidx.compose.ui.geometry.Size(size.width, barHeight))
        }
    }
}

/** The search bar's magnifying-glass glyph. */
@Composable
fun SearchGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(13.dp)) {
        val stroke = Stroke(width = 1.6.dp.toPx())
        val radius = size.width * 0.35f
        drawCircle(color = color, radius = radius, center = Offset(radius, radius), style = stroke)
        drawLine(
            color = color,
            start = Offset(radius * 1.7f, radius * 1.7f),
            end = Offset(size.width, size.height),
            strokeWidth = stroke.width,
        )
    }
}
