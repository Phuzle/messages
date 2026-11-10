package com.phuzle.labs.messages.ui.archived
import com.phuzle.labs.messages.ui.components.bottomBarContentPadding
import com.phuzle.labs.messages.ui.components.topBarContentPadding

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.BackBarScaffold
import com.phuzle.labs.messages.ui.components.EmptyState
import com.phuzle.labs.messages.ui.components.ListCountHeader
import com.phuzle.labs.messages.ui.components.SelectionOverflowMenu
import com.phuzle.labs.messages.ui.components.SelectionTopBar
import com.phuzle.labs.messages.ui.components.SimpleThreadRow
import com.phuzle.labs.messages.ui.components.roundClickable
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.theme.IconSizes
import com.phuzle.labs.messages.ui.theme.MessagesTheme

@Composable
fun ArchivedScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    // Hoisted here (not inside topBarContent) so the popup can be rendered as a sibling of
    // GlassBar instead of nested inside it — see SelectionTopBar.kt for why nesting it there
    // makes the popup invisible and untouchable.
    var showSelectionMenu by remember { mutableStateOf(false) }
    val selection = state.multiSelectArchivedIds

    BackBarScaffold(
        title = "Archived",
        onBack = viewModel::goBack,
        topBarContent = if (selection.isNotEmpty()) {
            {
                SelectionTopBar(
                    count = selection.size,
                    onClose = viewModel::exitArchivedSelect,
                    onMenuClick = { showSelectionMenu = true },
                ) {
                    Box(Modifier.size(48.dp).roundClickable(onClick = viewModel::bulkUnarchiveSelected), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Unarchive, contentDescription = "Unarchive", tint = tokens.textPrimary, modifier = Modifier.size(IconSizes.Medium))
                    }
                    Box(Modifier.size(48.dp).roundClickable(onClick = { showBulkDeleteConfirm = true }), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = tokens.danger, modifier = Modifier.size(IconSizes.Medium))
                    }
                }
            }
        } else null,
    ) {
        if (state.archivedThreads.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Archive,
                title = "No archived chats",
                detail = "Chats you archive from the inbox will show up here.",
                modifier = Modifier.padding(top = topBarContentPadding(76.dp)),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // bottomBarContentPadding, not a flat 24.dp: on a gesture-nav device the last row
                // otherwise scrolls to a resting position underneath the navigation bar.
                contentPadding = PaddingValues(top = topBarContentPadding(76.dp), bottom = bottomBarContentPadding(24.dp)),
            ) {
                item {
                    ListCountHeader(
                        count = state.archivedThreads.size,
                        noun = if (state.archivedThreads.size == 1) "chat archived" else "chats archived",
                        actionLabel = "Unarchive all",
                        actionIcon = Icons.Filled.Unarchive,
                        onAction = viewModel::unarchiveAll,
                    )
                }
                items(state.archivedThreads, key = { it.id }) { item ->
                    SimpleThreadRow(
                        item = item,
                        actionLabel = "Unarchive",
                        actionIcon = Icons.Filled.Unarchive,
                        onAction = { viewModel.unarchiveThread(item.id) },
                        selectionMode = selection.isNotEmpty(),
                        selected = item.id in selection,
                        onToggleSelect = { viewModel.toggleArchivedSelection(item.id) },
                        onLongPress = { viewModel.startArchivedSelect(item.id) },
                    )
                }
            }
        }

        SelectionOverflowMenu(
            visible = showSelectionMenu,
            onDismiss = { showSelectionMenu = false },
            onSelectAll = { viewModel.selectAllArchived(state.archivedThreads.map { it.id }) },
            onInvertSelection = { viewModel.invertArchivedSelection(state.archivedThreads.map { it.id }) },
        )
    }

    com.phuzle.labs.messages.ui.components.AppDialog(
        visible = showBulkDeleteConfirm,
        onDismiss = { showBulkDeleteConfirm = false },
        title = "Delete ${selection.size} chats?",
        message = "Deleted chats move to the Recycle Bin for 30 days before they're purged for good.",
        confirmLabel = "Delete",
        onConfirm = { viewModel.bulkDeleteArchivedSelected(); showBulkDeleteConfirm = false },
        destructive = true,
    )
}
