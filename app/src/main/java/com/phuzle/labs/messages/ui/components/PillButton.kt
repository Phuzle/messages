package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapePill
import com.phuzle.labs.messages.ui.theme.pillOptionColors

/** The soft-accent-tinted pill used across Settings for theme/accent/swipe/schedule/app-lock
 * choices. [icon] is optional — most of these choices (accent colors, swipe actions) read fine as
 * label-only, but a few (app-lock method, backup frequency) are clearer with a leading glyph. */
@Composable
fun PillButton(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val colors = pillOptionColors(active, MessagesTheme.tokens)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(ShapePill)
            .background(colors.background, ShapePill)
            .border(1.dp, colors.border, ShapePill)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = colors.content, modifier = Modifier.size(14.dp))
        }
        Text(
            text = label,
            color = colors.content,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = if (icon != null) Modifier.padding(start = 6.dp) else Modifier,
        )
    }
}
