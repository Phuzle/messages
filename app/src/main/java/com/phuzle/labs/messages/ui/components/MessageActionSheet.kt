package com.phuzle.labs.messages.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** The bottom sheet triggered by a long-press on a message bubble: copy/reply/forward/share/select/delete.
 * Same icon-plus-label rows as the dashboard thread-row long-press (see ActionSheet). */
@Composable
fun MessageActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onReply: () -> Unit,
    onForward: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MessagesTheme.tokens
    AppBottomSheet(visible = visible, onDismiss = onDismiss, modifier = modifier) {
        SheetActionRow("Copy", Icons.Filled.ContentCopy, onCopy)
        SheetActionRow("Reply", Icons.AutoMirrored.Filled.Reply, onReply)
        SheetActionRow("Forward", Icons.AutoMirrored.Filled.Send, onForward)
        SheetActionRow("Share", Icons.Filled.Share, onShare)
        SheetActionRow("Select", Icons.Outlined.CheckCircleOutline, onSelect)
        SheetActionRow("Delete", Icons.Filled.Delete, onDelete, color = tokens.danger)
    }
}
