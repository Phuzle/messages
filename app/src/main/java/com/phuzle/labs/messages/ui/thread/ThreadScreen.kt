package com.phuzle.labs.messages.ui.thread

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.AvatarBubble
import com.phuzle.labs.messages.ui.components.BarInset
import com.phuzle.labs.messages.ui.components.FlatTextField
import com.phuzle.labs.messages.ui.components.GlassBar
import com.phuzle.labs.messages.ui.components.MessageActionSheet
import com.phuzle.labs.messages.ui.components.MessageSkeletonRow
import com.phuzle.labs.messages.ui.components.MenuItem
import com.phuzle.labs.messages.ui.components.OverflowMenu
import com.phuzle.labs.messages.ui.components.roundClickable
import com.phuzle.labs.messages.ui.components.topBarContentPadding
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.model.MessageActionTargetUi
import com.phuzle.labs.messages.ui.model.MessageUi
import com.phuzle.labs.messages.ui.theme.MessagesTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThreadScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    val thread = state.currentThread ?: return
    val listState = rememberLazyListState()
    val latestState = rememberUpdatedState(state)

    // Bounded message buffer: scrolling near the top fetches one more page of history; scrolling
    // back down well clear of it releases those pages again so long threads don't sit fully in memory.
    LaunchedEffect(thread.id, listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
            val s = latestState.value
            when {
                index <= 3 && s.hasMoreOlderMessages && !s.isLoadingOlderMessages -> viewModel.loadOlderMessages()
                index > 20 -> viewModel.trimOlderMessages()
            }
        }
    }

    val canSend = state.threadInput.isNotBlank()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topBarContentPadding(70.dp), bottom = 84.dp, start = 14.dp, end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.isLoadingOlderMessages) {
                item { MessageSkeletonRow(alignEnd = false) }
                item { MessageSkeletonRow(alignEnd = true) }
            }
            items(state.currentThreadMessages, key = { it.id }) { message ->
                MessageBubble(message, onLongPress = { viewModel.openMessageActions(MessageActionTargetUi(message.id, message.text)) })
            }
            if (thread.isOtp && thread.latestOtpCode != null) {
                item {
                    Text(
                        if (state.threadOtpCopied) "Copied ✓" else "Copy Code",
                        color = tokens.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(tokens.accentSoft, RoundedCornerShape(9.dp))
                            .clickable(onClick = viewModel::copyThreadCode)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        GlassBar(modifier = Modifier.align(Alignment.TopCenter), height = 56.dp, inset = BarInset.Top) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).roundClickable(onClick = viewModel::goBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = tokens.textPrimary, modifier = Modifier.size(20.dp))
                }
                Row(
                    modifier = Modifier.weight(1f).clickable(onClick = viewModel::openThreadInfo).padding(start = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AvatarBubble(thread.initials, thread.avatarColor, thread.isBusiness, size = 34.dp, photoUri = thread.photoUri)
                    Text(thread.displayName, color = tokens.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(Modifier.size(36.dp).roundClickable(onClick = viewModel::toggleThreadOverflowMenu), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = tokens.textPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }

        OverflowMenu(
            visible = state.threadOverflowMenuOpen,
            onDismiss = viewModel::closeThreadOverflowMenu,
            items = listOf(
                MenuItem("View contact info") { viewModel.closeThreadOverflowMenu(); viewModel.openThreadInfo() },
                MenuItem(if (thread.isBlocked) "Unblock" else "Block") { viewModel.closeThreadOverflowMenu(); viewModel.toggleBlockCurrent() },
                MenuItem("Archive", viewModel::archiveCurrentThread),
                MenuItem("Delete conversation", viewModel::deleteCurrentThread),
            ),
        )

        MessageActionSheet(
            visible = state.messageActionTarget != null,
            onDismiss = viewModel::closeMessageActions,
            onReply = viewModel::replyQuotingSelectedMessage,
            onForward = viewModel::forwardSelectedMessage,
            onDelete = viewModel::deleteSelectedMessage,
        )

        if (thread.isReplyable) {
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(tokens.barBg).navigationBarsPadding()) {
                if (state.settings.showCharCount) {
                    Text(
                        "${state.threadInput.length} characters", color = tokens.textTertiary, fontSize = 11.sp,
                        modifier = Modifier.padding(start = 14.dp, top = 4.dp),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FlatTextField(
                        value = state.threadInput,
                        onValueChange = viewModel::onThreadInputChange,
                        placeholder = "Message",
                        filled = true,
                        singleLine = false,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(if (canSend) tokens.accent else tokens.surfaceAlt, CircleShape)
                            .let { if (canSend) it.roundClickable(onClick = viewModel::sendThreadMessage) else it },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send, contentDescription = "Send",
                            tint = if (canSend) tokens.accentText else tokens.textTertiary, modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        } else {
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(tokens.barBg).navigationBarsPadding().padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("This sender doesn't accept replies", color = tokens.textTertiary, fontSize = 12.5.sp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(message: MessageUi, onLongPress: () -> Unit) {
    val tokens = MessagesTheme.tokens
    val bg = if (message.isScheduled) tokens.surfaceAlt else if (message.isMine) tokens.accent else tokens.surfaceAlt
    val fg = if (message.isScheduled) tokens.textSecondary else if (message.isMine) tokens.accentText else tokens.textPrimary
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bg, RoundedCornerShape(16.dp))
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(message.text, color = fg, fontSize = 14.sp, lineHeight = 19.sp)
            Text(message.timeLabel, color = fg.copy(alpha = 0.65f), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
