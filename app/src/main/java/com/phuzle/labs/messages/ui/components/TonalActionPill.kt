package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.IconSizes
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapePill

/**
 * The small inline action next to a row or inside a card (Restore, Unblock, Export...): a 40dp tonal
 * accent pill inside a 48dp tap target. Replaces the old outlined 8-9dp-radius mini buttons.
 */
@Composable
fun TonalActionPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val tokens = MessagesTheme.tokens
    val content = if (enabled) tokens.accentLabel else tokens.textTertiary
    Box(
        modifier = modifier.heightIn(min = 48.dp).clip(ShapePill).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.height(40.dp).background(if (enabled) tokens.accentSoft else tokens.surfaceAlt, ShapePill).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(IconSizes.Small))
            }
            Text(
                label,
                color = content,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = if (icon != null) Modifier.padding(start = 6.dp) else Modifier,
            )
        }
    }
}
