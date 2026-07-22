package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class DrawerIconType { Inbox, Archived, Passbook, Reminders, Settings, RecycleBin, AboutUs, Share }

/** Small hand-drawn glyphs for the nav drawer's item icons — same technique as [SettingsCategoryIcon]. */
@Composable
fun DrawerIcon(type: DrawerIconType, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(18.dp)) {
        val stroke = Stroke(width = 1.6.dp.toPx())
        when (type) {
            DrawerIconType.Inbox -> drawRoundRect(
                color = color,
                size = Size(size.width, size.height * 0.78f),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
            DrawerIconType.Archived -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, size.height * 0.32f),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
                drawRect(
                    color = color,
                    topLeft = Offset(size.width * 0.12f, size.height * 0.46f),
                    size = Size(size.width * 0.76f, size.height * 0.5f),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.5f, size.height * 0.58f),
                    end = Offset(size.width * 0.5f, size.height * 0.86f),
                    strokeWidth = stroke.width,
                )
            }
            DrawerIconType.Passbook -> {
                drawRoundRect(
                    color = color,
                    size = Size(size.width, size.height * 0.7f),
                    topLeft = Offset(0f, size.height * 0.12f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.08f, size.height * 0.38f),
                    end = Offset(size.width * 0.92f, size.height * 0.38f),
                    strokeWidth = stroke.width,
                )
            }
            DrawerIconType.Reminders -> {
                drawArc(
                    color = color, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(size.width * 0.15f, 0f),
                    size = Size(size.width * 0.7f, size.height * 0.68f),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.15f, size.height * 0.68f),
                    end = Offset(size.width * 0.85f, size.height * 0.68f),
                    strokeWidth = stroke.width,
                )
                drawCircle(color, radius = 1.6.dp.toPx(), center = Offset(size.width / 2, size.height * 0.86f))
            }
            DrawerIconType.Settings -> {
                drawCircle(color = color, radius = size.width * 0.4f, center = center, style = stroke)
                drawCircle(color = color, radius = size.width * 0.14f, center = center)
            }
            DrawerIconType.RecycleBin -> {
                drawRect(
                    color = color,
                    topLeft = Offset(size.width * 0.22f, size.height * 0.32f),
                    size = Size(size.width * 0.56f, size.height * 0.58f),
                    style = stroke,
                )
                drawLine(color, Offset(size.width * 0.12f, size.height * 0.28f), Offset(size.width * 0.88f, size.height * 0.28f), stroke.width)
                drawLine(color, Offset(size.width * 0.4f, size.height * 0.12f), Offset(size.width * 0.6f, size.height * 0.12f), stroke.width)
            }
            DrawerIconType.AboutUs -> {
                drawCircle(color = color, radius = size.width * 0.42f, center = center, style = stroke)
                drawCircle(color = color, radius = 1.4.dp.toPx(), center = Offset(size.width / 2, size.height * 0.32f))
                drawLine(color, Offset(size.width / 2, size.height * 0.46f), Offset(size.width / 2, size.height * 0.72f), stroke.width)
            }
            DrawerIconType.Share -> {
                drawCircle(color, radius = 1.8.dp.toPx(), center = Offset(size.width * 0.2f, size.height * 0.5f))
                drawCircle(color, radius = 1.8.dp.toPx(), center = Offset(size.width * 0.85f, size.height * 0.18f))
                drawCircle(color, radius = 1.8.dp.toPx(), center = Offset(size.width * 0.85f, size.height * 0.82f))
                drawLine(color, Offset(size.width * 0.2f, size.height * 0.5f), Offset(size.width * 0.85f, size.height * 0.18f), stroke.width)
                drawLine(color, Offset(size.width * 0.2f, size.height * 0.5f), Offset(size.width * 0.85f, size.height * 0.82f), stroke.width)
            }
        }
    }
}
