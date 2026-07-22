package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.phuzle.labs.messages.ui.model.SettingsSub
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** Small hand-drawn glyphs matching the prototype's own CSS-shape icons, one per settings section. */
@Composable
fun SettingsCategoryIcon(section: SettingsSub, modifier: Modifier = Modifier) {
    val tokens = MessagesTheme.tokens
    Box(
        modifier = modifier
            .size(36.dp)
            .background(tokens.inputBg, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(16.dp)) {
            val stroke = Stroke(width = 1.6.dp.toPx())
            when (section) {
                SettingsSub.Notifications -> {
                    drawArc(
                        color = tokens.textSecondary, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                        topLeft = Offset(size.width * 0.15f, 0f),
                        size = Size(size.width * 0.7f, size.height * 0.7f),
                        style = stroke,
                    )
                    drawCircle(tokens.textSecondary, radius = 1.4.dp.toPx(), center = Offset(size.width / 2, size.height * 0.85f))
                }
                SettingsSub.Appearance -> {
                    drawArc(tokens.textSecondary, 90f, 180f, useCenter = true, size = size)
                    drawArc(tokens.textSecondary, 270f, 180f, useCenter = false, size = size, style = stroke)
                }
                SettingsSub.Privacy -> {
                    drawRoundRect(
                        color = tokens.textSecondary,
                        topLeft = Offset(size.width * 0.1f, size.height * 0.45f),
                        size = Size(size.width * 0.8f, size.height * 0.5f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                    )
                    drawArc(
                        color = tokens.textSecondary, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                        topLeft = Offset(size.width * 0.25f, 0f),
                        size = Size(size.width * 0.5f, size.height * 0.55f),
                        style = stroke,
                    )
                }
                SettingsSub.Chats -> {
                    drawRoundRect(
                        color = tokens.textSecondary,
                        size = Size(size.width, size.height * 0.8f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    )
                }
                SettingsSub.Backup -> {
                    drawArc(
                        color = tokens.textSecondary, startAngle = -40f, sweepAngle = 300f, useCenter = false,
                        style = stroke,
                    )
                }
                SettingsSub.Storage -> {
                    val barHeight = 2.5.dp.toPx()
                    val gap = size.height / 3
                    for (i in 0..2) {
                        drawRoundRect(
                            color = tokens.textSecondary,
                            topLeft = Offset(0f, i * gap),
                            size = Size(size.width * 0.9f, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
                        )
                    }
                }
            }
        }
    }
}
