package com.phuzle.labs.messages.ui.privatechats
import com.phuzle.labs.messages.ui.components.bottomBarContentPadding
import com.phuzle.labs.messages.ui.components.topBarContentPadding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.BackBarScaffold
import com.phuzle.labs.messages.ui.components.BiometricGate
import com.phuzle.labs.messages.ui.components.EmptyState
import com.phuzle.labs.messages.ui.components.SelectionOverflowMenu
import com.phuzle.labs.messages.ui.components.SelectionTopBar
import com.phuzle.labs.messages.ui.components.SimpleThreadRow
import com.phuzle.labs.messages.ui.components.roundClickable
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.theme.IconSizes
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

@Composable
fun PrivateChatsScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    // Hoisted here (not inside topBarContent) so the popup can be rendered as a sibling of
    // GlassBar instead of nested inside it — see SelectionTopBar.kt for why nesting it there
    // makes the popup invisible and untouchable.
    var showSelectionMenu by remember { mutableStateOf(false) }
    val selection = state.multiSelectPrivateIds

    BackBarScaffold(
        title = "Private Chats",
        onBack = viewModel::goBack,
        topBarContent = if (selection.isNotEmpty()) {
            {
                SelectionTopBar(
                    count = selection.size,
                    onClose = viewModel::exitPrivateSelect,
                    onMenuClick = { showSelectionMenu = true },
                ) {
                    Box(Modifier.size(40.dp).roundClickable(onClick = viewModel::bulkRemoveFromPrivateSelected), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.LockOpen, contentDescription = "Remove from Private", tint = tokens.textPrimary, modifier = Modifier.size(IconSizes.Medium))
                    }
                    Box(Modifier.size(40.dp).roundClickable(onClick = { showBulkDeleteConfirm = true }), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = tokens.danger, modifier = Modifier.size(IconSizes.Medium))
                    }
                }
            }
        } else null,
    ) {
        if (state.privateChatsUnlockedThisSession) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // See ArchivedScreen — a flat bottom padding rests the last row under the nav bar.
                contentPadding = PaddingValues(top = topBarContentPadding(68.dp), start = 16.dp, end = 16.dp, bottom = bottomBarContentPadding(24.dp)),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.privateThreads.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.LockOpen,
                            title = "No private chats yet",
                            detail = "Use a chat's long-press menu to move it here.",
                            modifier = Modifier.fillMaxSize().padding(top = 20.dp),
                        )
                    }
                } else {
                    items(state.privateThreads, key = { it.id }) { item ->
                        SimpleThreadRow(
                            item = item,
                            onClick = { viewModel.openThreadById(item.id) },
                            selectionMode = selection.isNotEmpty(),
                            selected = item.id in selection,
                            onToggleSelect = { viewModel.togglePrivateSelection(item.id) },
                            onLongPress = { viewModel.startPrivateSelect(item.id) },
                        )
                    }
                }
            }
        } else {
            BiometricGate(
                key = "private_chats",
                title = "Unlock private chats",
                subtitle = "Confirm it's you to view your private chats",
                onUnlocked = viewModel::unlockPrivateChats,
            ) { retry ->
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            Modifier.size(48.dp).background(tokens.surfaceAlt, RoundedCornerShape(14.dp)).border(1.dp, tokens.border, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = tokens.textSecondary, modifier = Modifier.size(22.dp))
                        }
                        Text("Private chats are locked", color = tokens.textSecondary, fontSize = 14.sp)
                        Text(
                            "Unlock",
                            color = tokens.accentText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .background(tokens.accent, ShapeMedium)
                                .clickable(onClick = retry)
                                .padding(horizontal = 20.dp, vertical = 11.dp),
                        )
                    }
                }
            }
        }

        SelectionOverflowMenu(
            visible = showSelectionMenu,
            onDismiss = { showSelectionMenu = false },
            onSelectAll = { viewModel.selectAllPrivate(state.privateThreads.map { it.id }) },
            onInvertSelection = { viewModel.invertPrivateSelection(state.privateThreads.map { it.id }) },
        )
    }

    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("Delete ${selection.size} chats?") },
            text = { Text("Deleted chats move to the Recycle Bin for 30 days before they're purged for good.") },
            confirmButton = {
                TextButton(onClick = { viewModel.bulkDeletePrivateSelected(); showBulkDeleteConfirm = false }) {
                    Text("Delete", color = tokens.danger)
                }
            },
            dismissButton = { TextButton(onClick = { showBulkDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}
