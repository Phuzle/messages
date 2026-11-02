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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.model.DeletedThreadUi
import com.phuzle.labs.messages.ui.theme.IconSizes
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium
import com.phuzle.labs.messages.ui.theme.ShapePill

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
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(ShapeMedium)
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
                    .size(48.dp)
                    .background(if (selected) tokens.accentDeep else tokens.surfaceAlt, CircleShape)
                    .border(1.dp, if (selected) tokens.accentDeep else tokens.border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                // Always shows a checkmark, muted when not selected — see ThreadRow's same choice.
                Icon(
                    Icons.Filled.Check,
                    contentDescription = if (selected) "Selected" else "Not selected",
                    tint = if (selected) tokens.onAccentDeep else tokens.textTertiary,
                    modifier = Modifier.size(IconSizes.Small),
                )
            }
        } else {
            AvatarBubble(item.category, item.avatarColor, item.isBusiness, size = 48.dp)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(item.sender, color = tokens.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(item.preview, color = tokens.textSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!selectionMode && actionLabel != null && onAction != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                // 40dp tonal pill; the clickable wraps an extra 4dp above/below for a 48dp target.
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onAction)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .height(40.dp)
                    .background(tokens.accentSoft, ShapePill)
                    .padding(horizontal = 16.dp),
            ) {
                if (actionIcon != null) {
                    Icon(actionIcon, contentDescription = null, tint = tokens.accentLabel, modifier = Modifier.size(IconSizes.Small))
                }
                Text(
                    actionLabel,
                    color = tokens.accentLabel,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = if (actionIcon != null) Modifier.padding(start = 5.dp) else Modifier,
                )
            }
        }
    }
}
