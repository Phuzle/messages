package com.phuzle.labs.messages.ui.recyclebin
import com.phuzle.labs.messages.ui.components.topBarContentPadding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.BackBarScaffold
import com.phuzle.labs.messages.ui.components.SimpleThreadRow
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.theme.MessagesTheme

@Composable
fun RecycleBinScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    BackBarScaffold(title = "Recycle Bin", onBack = viewModel::goBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topBarContentPadding(68.dp), start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.deletedThreads.isNotEmpty()) {
                item {
                    Text(
                        "Deleted threads are purged automatically after 30 days.",
                        color = tokens.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
            items(state.deletedThreads, key = { it.id }) { item ->
                SimpleThreadRow(item = item, actionLabel = "Restore", onAction = { viewModel.restoreThread(item.id) })
            }
            if (state.deletedThreads.isEmpty()) {
                item {
                    Text(
                        "Empty — deleted threads will appear here.",
                        color = tokens.textTertiary, fontSize = 13.5.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    )
                }
            }
        }
    }
}
