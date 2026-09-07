package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.model.DeletedThreadUi
import com.phuzle.labs.messages.ui.theme.IconSizes
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** The same flush, flat list-row look as the main inbox's ThreadRow — used by Recycle Bin
 * (Restore), Archived (Unarchive), and Private Chats (tap to open) so every list in the app reads
 * as one consistent design, not a mix of flat rows here and bordered cards there. Also mirrors
 * ThreadRow's long-press-to-multi-select gesture ([selectionMode]/[selected]/[onToggleSelect]/
 * [onLongPress]) — the dashboard's inbox was the only list that supported it, which made every
 * other list feel like a different, less capable app. Swiping is deliberately not part of this
 * (that stays specific to the dashboard's ThreadRow); everything else about multi-select is shared. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SimpleThreadRow(
    item: DeletedThreadUi,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongPress: () -> Unit = {},
) {
    val tokens = MessagesTheme.tokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (selected) tokens.accentSoft else tokens.bg)
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelect() else onClick?.invoke() },
                onLongClick = if (selectionMode) null else withLongPressHaptic(onLongPress),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (selected) tokens.accent else tokens.surfaceAlt, CircleShape)
                    .border(1.dp, if (selected) tokens.accent else tokens.border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                // Always shows a checkmark, muted when not selected — see ThreadRow's same choice.
                Icon(
                    Icons.Filled.Check,
                    contentDescription = if (selected) "Selected" else "Not selected",
                    tint = if (selected) tokens.accentText else tokens.textTertiary,
                    modifier = Modifier.size(IconSizes.Small),
                )
            }
        } else {
            AvatarBubble(item.category, item.avatarColor, item.isBusiness, size = 40.dp)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(item.sender, color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(item.preview, color = tokens.textTertiary, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!selectionMode && actionLabel != null && onAction != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .border(1.dp, tokens.border, RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                if (actionIcon != null) {
                    Icon(actionIcon, contentDescription = null, tint = tokens.accent, modifier = Modifier.size(IconSizes.Small))
                }
                Text(
                    actionLabel,
                    color = tokens.accent,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = if (actionIcon != null) Modifier.padding(start = 5.dp) else Modifier,
                )
            }
        }
    }
}
