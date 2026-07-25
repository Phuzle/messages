package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** A leading icon inside a tinted circle — see DESIGN.md's Icon badges section. Used on the SMS
 * disclosure rows, Passbook account avatars, and transaction credit/debit icons. Defaults to a
 * neutral accent-tinted badge; pass [tint]/[iconColor] for a semantic one (e.g. success/danger at
 * low alpha for a credit/debit icon). */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = size * 0.45f,
    tint: Color = MessagesTheme.tokens.accentSoft,
    iconColor: Color = MessagesTheme.tokens.accent,
) {
    Box(
        modifier.size(size).background(tint, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(iconSize))
    }
}
