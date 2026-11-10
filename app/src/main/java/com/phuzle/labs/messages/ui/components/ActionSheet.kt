package com.phuzle.labs.messages.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.phuzle.labs.messages.ui.model.ActionSheetUi
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** The bottom sheet triggered by a long-press on a thread row: read/archive/private/delete. */
@Composable
fun ActionSheet(
    sheet: ActionSheetUi?,
    onDismiss: () -> Unit,
    onMarkRead: () -> Unit,
    onArchive: () -> Unit,
    onTogglePrivate: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MessagesTheme.tokens
    val shown = rememberLast(sheet)
    AppBottomSheet(visible = sheet != null, onDismiss = onDismiss, modifier = modifier, title = shown?.sender) {
        shown?.let { s ->
            SheetActionRow(s.markReadLabel, if (s.markReadLabel == "Mark as read") Icons.Filled.MarkEmailRead else Icons.Filled.MarkEmailUnread, onMarkRead)
            SheetActionRow("Share contact", Icons.Filled.Share, onShare)
            SheetActionRow("Archive", Icons.Filled.Archive, onArchive)
            SheetActionRow(s.privateLabel, if (s.privateLabel == "Move to Private") Icons.Filled.Lock else Icons.Filled.LockOpen, onTogglePrivate)
            SheetActionRow("Delete", Icons.Filled.Delete, onDelete, color = tokens.danger)
        }
    }
}
