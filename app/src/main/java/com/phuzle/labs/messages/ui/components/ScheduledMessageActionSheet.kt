package com.phuzle.labs.messages.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** Long-press sheet for a message still waiting to send — deliberately just Edit/Delete, not the
 * full Copy/Reply/Forward/Delete set MessageActionSheet offers for real messages: a scheduled
 * message hasn't gone out yet, so "reply" or "forward" don't mean anything for it, and "copy"
 * duplicates what editing the body already lets you do. Shared by the thread view's scheduled
 * bubbles and the Scheduled Messages hub — same sheet, same two actions, regardless of which
 * screen triggered it. */
@Composable
fun ScheduledMessageActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MessagesTheme.tokens
    AppBottomSheet(visible = visible, onDismiss = onDismiss, modifier = modifier) {
        SheetActionRow("Edit", Icons.Filled.Edit, onEdit)
        SheetActionRow("Delete", Icons.Filled.Delete, onDelete, color = tokens.danger)
    }
}
