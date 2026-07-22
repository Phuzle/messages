package com.phuzle.labs.messages.ui.archived
import com.phuzle.labs.messages.ui.components.topBarContentPadding

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.BackBarScaffold
import com.phuzle.labs.messages.ui.components.EmptyState
import com.phuzle.labs.messages.ui.components.ListCountHeader
import com.phuzle.labs.messages.ui.components.SimpleThreadRow
import com.phuzle.labs.messages.ui.model.AppUiState

@Composable
fun ArchivedScreen(state: AppUiState, viewModel: AppViewModel) {
    BackBarScaffold(title = "Archived", onBack = viewModel::goBack) {
        if (state.archivedThreads.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Archive,
                title = "No archived chats",
                detail = "Chats you archive from the inbox will show up here.",
                modifier = Modifier.padding(top = topBarContentPadding(68.dp)),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topBarContentPadding(68.dp), bottom = 24.dp),
            ) {
                item {
                    ListCountHeader(
                        count = state.archivedThreads.size,
                        noun = if (state.archivedThreads.size == 1) "chat archived" else "chats archived",
                        actionLabel = "Unarchive all",
                        onAction = viewModel::unarchiveAll,
                    )
                }
                items(state.archivedThreads, key = { it.id }) { item ->
                    SimpleThreadRow(item = item, actionLabel = "Unarchive", onAction = { viewModel.unarchiveThread(item.id) })
                }
            }
        }
    }
}
