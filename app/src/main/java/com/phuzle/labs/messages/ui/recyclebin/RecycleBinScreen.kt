package com.phuzle.labs.messages.ui.recyclebin
import com.phuzle.labs.messages.ui.components.topBarContentPadding

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.BackBarScaffold
import com.phuzle.labs.messages.ui.components.EmptyState
import com.phuzle.labs.messages.ui.components.ListCountHeader
import com.phuzle.labs.messages.ui.components.SimpleThreadRow
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.theme.MessagesTheme

@Composable
fun RecycleBinScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    var showEmptyConfirm by remember { mutableStateOf(false) }

    BackBarScaffold(title = "Recycle Bin", onBack = viewModel::goBack) {
        if (state.deletedThreads.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Delete,
                title = "Recycle bin is empty",
                detail = "Deleted chats stay here for 30 days before they're purged for good.",
                modifier = Modifier.padding(top = topBarContentPadding(68.dp)),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topBarContentPadding(68.dp), bottom = 24.dp),
            ) {
                item {
                    ListCountHeader(
                        count = state.deletedThreads.size,
                        noun = if (state.deletedThreads.size == 1) "chat deleted" else "chats deleted",
                        actionLabel = "Restore all",
                        onAction = viewModel::restoreAllDeleted,
                        secondaryActionLabel = "Empty bin",
                        secondaryActionDanger = true,
                        onSecondaryAction = { showEmptyConfirm = true },
                    )
                    Text(
                        "Deleted threads are purged automatically after 30 days.",
                        color = tokens.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                items(state.deletedThreads, key = { it.id }) { item ->
                    SimpleThreadRow(item = item, actionLabel = "Restore", onAction = { viewModel.restoreThread(item.id) })
                }
            }
        }
    }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { Text("Empty recycle bin?") },
            text = { Text("This permanently deletes ${state.deletedThreads.size} chat(s) and can't be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.emptyRecycleBin(); showEmptyConfirm = false }) {
                    Text("Empty bin", color = tokens.danger)
                }
            },
            dismissButton = { TextButton(onClick = { showEmptyConfirm = false }) { Text("Cancel") } },
        )
    }
}
