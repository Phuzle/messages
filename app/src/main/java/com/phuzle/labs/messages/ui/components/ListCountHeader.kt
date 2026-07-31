package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** "5 drafts" / "10 archived" header row with up to two bulk-action links (e.g. "Restore all" / "Empty bin"). */
@Composable
fun ListCountHeader(
    count: Int,
    noun: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    actionDanger: Boolean = false,
    onAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    secondaryActionIcon: ImageVector? = null,
    secondaryActionDanger: Boolean = false,
    onSecondaryAction: (() -> Unit)? = null,
) {
    val tokens = MessagesTheme.tokens
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$count $noun", color = tokens.textSecondary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (actionLabel != null && onAction != null) {
                HeaderAction(actionLabel, actionIcon, if (actionDanger) tokens.danger else tokens.accent, onAction)
            }
            if (secondaryActionLabel != null && onSecondaryAction != null) {
                HeaderAction(secondaryActionLabel, secondaryActionIcon, if (secondaryActionDanger) tokens.danger else tokens.accent, onSecondaryAction)
            }
        }
    }
}

@Composable
private fun HeaderAction(label: String, icon: ImageVector?, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onClick)) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        }
        Text(
            label,
            color = color,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = if (icon != null) Modifier.padding(start = 4.dp) else Modifier,
        )
    }
}
