package com.phuzle.labs.messages.ui.scheduled

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.AvatarBubble
import com.phuzle.labs.messages.ui.components.BackBarScaffold
import com.phuzle.labs.messages.ui.components.EmptyState
import com.phuzle.labs.messages.ui.components.ListCountHeader
import com.phuzle.labs.messages.ui.components.bottomBarContentPadding
import com.phuzle.labs.messages.ui.components.topBarContentPadding
import com.phuzle.labs.messages.ui.model.ScheduledMessageUi
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** Nav-drawer-reachable list of every message still waiting to send (see
 * AppViewModel.scheduledMessages) — long-press a row for the same Edit/Delete sheet the thread
 * view's own scheduled bubbles use (see ScheduledMessageActionSheet), rendered as a shared overlay
 * in AppRoot so it works identically from either screen. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduledMessagesScreen(viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    val messages by viewModel.scheduledMessages.collectAsStateWithLifecycle()

    BackBarScaffold(title = "Scheduled Messages", onBack = viewModel::goBack) {
        if (messages.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Schedule,
                title = "No scheduled messages",
                detail = "Messages you schedule to send later show up here until they're sent.",
                modifier = Modifier.padding(top = topBarContentPadding(68.dp)),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topBarContentPadding(68.dp), bottom = bottomBarContentPadding(24.dp)),
            ) {
                item {
                    ListCountHeader(
                        count = messages.size,
                        noun = if (messages.size == 1) "message scheduled" else "messages scheduled",
                    )
                }
                items(messages, key = { it.id }) { message ->
                    ScheduledMessageRow(
                        message = message,
                        onClick = { viewModel.openThreadById(message.threadId) },
                        onLongPress = { viewModel.openScheduledMessageActions(message.id, message.threadId) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduledMessageRow(message: ScheduledMessageUi, onClick: () -> Unit, onLongPress: () -> Unit) {
    val tokens = MessagesTheme.tokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AvatarBubble(
            category = message.category, color = message.avatarColor, isBusiness = message.isBusiness,
            size = 44.dp, photoUri = message.photoUri,
        )
        Column(Modifier.weight(1f)) {
            Text(message.threadDisplayName, color = tokens.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                message.body, color = tokens.textSecondary, fontSize = 13.sp, maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                "Scheduled for ${message.scheduleLabel}",
                color = tokens.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
