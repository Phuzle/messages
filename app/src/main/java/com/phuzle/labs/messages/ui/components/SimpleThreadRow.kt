package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.model.DeletedThreadUi
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

/** The flat card row shared by Recycle Bin (Restore), Archived (Unarchive), and Private Chats (tap to open). */
@Composable
fun SimpleThreadRow(
    item: DeletedThreadUi,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val tokens = MessagesTheme.tokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(tokens.surface, ShapeMedium)
            .border(1.dp, tokens.border, ShapeMedium)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarBubble(item.initials, item.avatarColor, item.isBusiness, size = 40.dp)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(item.sender, color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(item.preview, color = tokens.textTertiary, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                color = tokens.accent,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .border(1.dp, tokens.border, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}
