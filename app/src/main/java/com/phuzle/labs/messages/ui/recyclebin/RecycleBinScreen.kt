package com.phuzle.labs.messages.ui.recyclebin
import com.phuzle.labs.messages.ui.components.bottomBarContentPadding
import com.phuzle.labs.messages.ui.components.topBarContentPadding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.BackBarScaffold
import com.phuzle.labs.messages.ui.components.EmptyState
import com.phuzle.labs.messages.ui.components.ListCountHeader
import com.phuzle.labs.messages.ui.components.SectionLabel
import com.phuzle.labs.messages.ui.components.SelectionOverflowMenu
import com.phuzle.labs.messages.ui.components.SelectionTopBar
import com.phuzle.labs.messages.ui.components.SimpleThreadRow
import com.phuzle.labs.messages.ui.components.roundClickable
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.model.DeletedOtpMessageUi
import com.phuzle.labs.messages.ui.theme.IconSizes
import com.phuzle.labs.messages.ui.theme.MessagesTheme

@Composable
fun RecycleBinScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    var showEmptyConfirm by remember { mutableStateOf(false) }
    var showBulkDeleteForeverConfirm by remember { mutableStateOf(false) }
    // Hoisted here (not inside topBarContent) so the popup can be rendered as a sibling of
    // GlassBar instead of nested inside it — see SelectionTopBar.kt for why nesting it there
    // makes the popup invisible and untouchable.
    var showSelectionMenu by remember { mutableStateOf(false) }
    val totalCount = state.deletedThreads.size + state.deletedOtpMessages.size
    // Multi-select only covers the "Chats" section — deleted OTP messages are a different entity
    // (MessageEntity, not ThreadEntity) with their own single restore action, and mixing the two
    // into one selection would make "select all" mean two different things at once.
    val selection = state.multiSelectRecycleBinIds

    BackBarScaffold(
        title = "Recycle Bin",
        onBack = viewModel::goBack,
        topBarContent = if (selection.isNotEmpty()) {
            {
                SelectionTopBar(
                    count = selection.size,
                    onClose = viewModel::exitRecycleBinSelect,
                    onMenuClick = { showSelectionMenu = true },
                ) {
                    Box(Modifier.size(40.dp).roundClickable(onClick = viewModel::bulkRestoreSelected), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Restore, contentDescription = "Restore", tint = tokens.textPrimary, modifier = Modifier.size(IconSizes.Medium))
                    }
                    Box(Modifier.size(40.dp).roundClickable(onClick = { showBulkDeleteForeverConfirm = true }), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = "Delete forever", tint = tokens.danger, modifier = Modifier.size(IconSizes.Medium))
                    }
                }
            }
        } else null,
    ) {
        if (totalCount == 0) {
            EmptyState(
                icon = Icons.Filled.Delete,
                title = "Recycle bin is empty",
                detail = "Deleted chats and OTP codes stay here for 30 days before they're purged for good.",
                modifier = Modifier.padding(top = topBarContentPadding(68.dp)),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // See ArchivedScreen — a flat bottom padding rests the last row under the nav bar.
                contentPadding = PaddingValues(top = topBarContentPadding(68.dp), bottom = bottomBarContentPadding(24.dp)),
            ) {
                item {
                    ListCountHeader(
                        count = totalCount,
                        noun = if (totalCount == 1) "item deleted" else "items deleted",
                        actionLabel = "Restore all",
                        actionIcon = Icons.Filled.Restore,
                        onAction = viewModel::restoreAllDeleted,
                        secondaryActionLabel = "Empty bin",
                        secondaryActionIcon = Icons.Filled.DeleteForever,
                        secondaryActionDanger = true,
                        onSecondaryAction = { showEmptyConfirm = true },
                    )
                    Text(
                        "Deleted chats and OTP codes are purged automatically after 30 days.",
                        color = tokens.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                if (state.deletedThreads.isNotEmpty()) {
                    item { SectionLabel("Chats", Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
                    items(state.deletedThreads, key = { "thread-${it.id}" }) { item ->
                        SimpleThreadRow(
                            item = item,
                            actionLabel = "Restore",
                            actionIcon = Icons.Filled.Restore,
                            onAction = { viewModel.restoreThread(item.id) },
                            selectionMode = selection.isNotEmpty(),
                            selected = item.id in selection,
                            onToggleSelect = { viewModel.toggleRecycleBinSelection(item.id) },
                            onLongPress = { viewModel.startRecycleBinSelect(item.id) },
                        )
                    }
                }
                if (state.deletedOtpMessages.isNotEmpty()) {
                    item { SectionLabel("OTP codes", Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
                    items(state.deletedOtpMessages, key = { "otp-${it.id}" }) { item ->
                        DeletedOtpMessageRow(item = item, onRestore = { viewModel.restoreDeletedOtpMessage(item.id) })
                    }
                }
            }
        }

        SelectionOverflowMenu(
            visible = showSelectionMenu,
            onDismiss = { showSelectionMenu = false },
            onSelectAll = { viewModel.selectAllRecycleBin(state.deletedThreads.map { it.id }) },
            onInvertSelection = { viewModel.invertRecycleBinSelection(state.deletedThreads.map { it.id }) },
        )
    }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { Text("Empty recycle bin?") },
            text = { Text("This permanently deletes $totalCount item(s) and can't be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.emptyRecycleBin(); showEmptyConfirm = false }) {
                    Text("Empty bin", color = tokens.danger)
                }
            },
            dismissButton = { TextButton(onClick = { showEmptyConfirm = false }) { Text("Cancel") } },
        )
    }

    if (showBulkDeleteForeverConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteForeverConfirm = false },
            title = { Text("Delete ${selection.size} chats forever?") },
            text = { Text("This permanently deletes them and can't be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.bulkDeleteForeverSelected(); showBulkDeleteForeverConfirm = false }) {
                    Text("Delete forever", color = tokens.danger)
                }
            },
            dismissButton = { TextButton(onClick = { showBulkDeleteForeverConfirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DeletedOtpMessageRow(item: DeletedOtpMessageUi, onRestore: () -> Unit) {
    val tokens = MessagesTheme.tokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.senderName, color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
                Text(item.timeLabel, color = tokens.textTertiary, fontSize = 11.5.sp, modifier = Modifier.padding(start = 8.dp))
            }
            Text(item.body, color = tokens.textTertiary, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .border(1.dp, tokens.border, RoundedCornerShape(8.dp))
                .clickable(onClick = onRestore)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            Icon(Icons.Filled.Restore, contentDescription = null, tint = tokens.accent, modifier = Modifier.size(14.dp))
            Text("Restore", color = tokens.accent, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 5.dp))
        }
    }
}
