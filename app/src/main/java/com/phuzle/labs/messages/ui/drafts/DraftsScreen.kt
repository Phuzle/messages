package com.phuzle.labs.messages.ui.drafts

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.BackBarScaffold
import com.phuzle.labs.messages.ui.components.EmptyState
import com.phuzle.labs.messages.ui.components.roundClickable
import com.phuzle.labs.messages.ui.components.topBarContentPadding
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.model.DraftUi
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

@Composable
fun DraftsScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    BackBarScaffold(title = "Drafts", onBack = viewModel::goBack) {
        if (state.drafts.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Drafts,
                title = "No drafts",
                detail = "Unsent messages you back out of will be saved here.",
                modifier = Modifier.padding(top = topBarContentPadding(68.dp)),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topBarContentPadding(68.dp), start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.drafts, key = { it.id }) { draft -> DraftRow(draft, viewModel) }
            }
        }
    }
}

@Composable
private fun DraftRow(draft: DraftUi, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.surface, ShapeMedium)
            .border(1.dp, tokens.border, ShapeMedium)
            .clickable(onClick = { viewModel.openDraft(draft.id) })
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(draft.to, color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                draft.bodyPreview.ifBlank { "No content" }, color = tokens.textTertiary, fontSize = 12.5.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
            )
            Text(draft.timeLabel, color = tokens.textTertiary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Box(
            Modifier.size(32.dp).roundClickable(onClick = { viewModel.deleteDraft(draft.id) }),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete draft", tint = tokens.danger, modifier = Modifier.size(18.dp))
        }
    }
}
