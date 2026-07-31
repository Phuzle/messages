package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

data class MenuItem(val label: String, val icon: ImageVector, val danger: Boolean = false, val onClick: () -> Unit)

@Composable
fun OverflowMenu(visible: Boolean, onDismiss: () -> Unit, items: List<MenuItem>, modifier: Modifier = Modifier) {
    if (!visible) return
    val tokens = MessagesTheme.tokens
    Box(modifier.fillMaxSize().statusBarsPadding()) {
        Box(Modifier.fillMaxSize().clickable(onClick = onDismiss))
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 10.dp)
                .widthIn(min = 180.dp, max = 240.dp)
                // A floating popover reads as a distinct layer above content in a way an inline
                // list row doesn't — same deliberate exception to this app's zero-elevation rule
                // as GlassBar's top/bottom bars (same 6.dp), not a new one-off.
                .shadow(elevation = 6.dp, shape = ShapeMedium)
                .background(tokens.surface, ShapeMedium)
                .border(1.dp, tokens.border, ShapeMedium),
        ) {
            items.forEachIndexed { index, item ->
                if (index > 0) SettingsRowDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = item.onClick)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = if (item.danger) tokens.danger else tokens.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        item.label,
                        color = if (item.danger) tokens.danger else tokens.textPrimary,
                        fontSize = 13.5.sp,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}
