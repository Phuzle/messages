package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapePill
import com.phuzle.labs.messages.ui.theme.pillOptionColors

/** The soft-accent-tinted pill used across Settings for theme/accent/swipe/schedule/app-lock choices. */
@Composable
fun PillButton(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = pillOptionColors(active, MessagesTheme.tokens)
    Text(
        text = label,
        color = colors.content,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(ShapePill)
            .background(colors.background, ShapePill)
            .border(1.dp, colors.border, ShapePill)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
    )
}
