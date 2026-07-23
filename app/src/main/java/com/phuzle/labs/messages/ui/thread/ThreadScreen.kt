package com.phuzle.labs.messages.ui.thread

import android.content.Intent
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import com.phuzle.labs.messages.ui.components.highlightedText
import com.phuzle.labs.messages.ui.components.roundClickable
import com.phuzle.labs.messages.ui.components.topBarContentPadding
import com.phuzle.labs.messages.domain.search.FuzzyMatcher
import com.phuzle.labs.messages.ui.format.formatDateSeparator
import com.phuzle.labs.messages.ui.format.isDifferentDay
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
    val context = androidx.compose.ui.platform.LocalContext.current

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

    val inAppBrowser = state.settings.inAppBrowser
    val onOpenUrl: (String) -> Unit = { url -> com.phuzle.labs.messages.core.util.openUrl(context, inAppBrowser, url) }
    val onCall: (String) -> Unit = { number ->
        val launched = runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$number"))) }.isSuccess
        if (!launched) android.widget.Toast.makeText(context, "No dialer app found", android.widget.Toast.LENGTH_SHORT).show()
    }
    val onEmail: (String) -> Unit = { email ->
        val launched = runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:$email"))) }.isSuccess
        if (!launched) android.widget.Toast.makeText(context, "No email app found", android.widget.Toast.LENGTH_SHORT).show()
    }

    // Opening a conversation should land on the most recent message, like any other chat app —
    // not at the top of whatever's currently loaded. Messages arrive asynchronously (a Flow, not
    // available the instant this composable enters), so this fires once real content shows up
    // for this thread rather than immediately on thread.id changing. Guarded by
    // scrolledToBottomFor so it only happens on the initial open, never again when an older page
    // loads in from scrolling up, or a new message quietly extends the list while already reading.
    var scrolledToBottomFor by remember { mutableStateOf<String?>(null) }

    val canSend = state.threadInput.isNotBlank()
    val listEntries = remember(state.currentThreadMessages, state.threadSearchQuery) {
        val query = state.threadSearchQuery.trim()
        if (query.isEmpty()) {
            buildThreadListEntries(state.currentThreadMessages)
        } else {
            // Searching within the conversation filters to just the matches — date headers would
            // just be clutter across a sparse, non-contiguous result set — with matched characters
            // bolded the same way the dashboard's thread-list search does (see FuzzyMatcher).
            val matches = state.currentThreadMessages.mapNotNull { m -> FuzzyMatcher.match(query, m.text)?.let { m to it.matchedIndices } }
            buildThreadListEntries(matches.map { it.first }, matches.associate { it.first.id to it.second }, showDateHeaders = false)
        }
    }

    LaunchedEffect(thread.id, listEntries.size) {
        if (listEntries.isNotEmpty() && scrolledToBottomFor != thread.id) {
            listState.scrollToItem(listEntries.size - 1)
            scrolledToBottomFor = thread.id
        }
    }

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
            items(
                listEntries,
                key = { entry -> when (entry) { is ThreadListEntry.DateHeader -> "date-${entry.label}"; is ThreadListEntry.Msg -> entry.message.id } },
                contentType = { entry -> entry is ThreadListEntry.DateHeader },
            ) { entry ->
                when (entry) {
                    is ThreadListEntry.DateHeader -> DateSeparator(entry.label)
                    is ThreadListEntry.Msg -> MessageBubble(
                        entry.message,
                        matchedIndices = entry.matchedIndices,
                        onLongPress = { viewModel.openMessageActions(MessageActionTargetUi(entry.message.id, entry.message.text)) },
                        onOpenUrl = onOpenUrl,
                        onCall = onCall,
                        onEmail = onEmail,
                        onCopyEntity = viewModel::copyDetectedText,
                    )
                }
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
                if (state.threadSearchActive) {
                    Box(Modifier.size(36.dp).roundClickable(onClick = viewModel::closeThreadSearch), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search", tint = tokens.textPrimary, modifier = Modifier.size(20.dp))
                    }
                    FlatTextField(
                        value = state.threadSearchQuery,
                        onValueChange = viewModel::onThreadSearchChange,
                        placeholder = "Search in conversation",
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                    )
                } else {
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
        }

        OverflowMenu(
            visible = state.threadOverflowMenuOpen,
            onDismiss = viewModel::closeThreadOverflowMenu,
            items = listOf(
                MenuItem("Search in conversation", onClick = viewModel::openThreadSearch),
                MenuItem("View contact info") { viewModel.closeThreadOverflowMenu(); viewModel.openThreadInfo() },
                MenuItem(if (thread.isBlocked) "Unblock" else "Block") { viewModel.closeThreadOverflowMenu(); viewModel.toggleBlockCurrent() },
                MenuItem("Archive", onClick = viewModel::archiveCurrentThread),
                MenuItem("Delete conversation", danger = true, onClick = viewModel::deleteCurrentThread),
            ),
        )

        if (thread.isReplyable && !thread.isBlocked) {
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(tokens.surface).navigationBarsPadding()) {
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
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(tokens.surface).navigationBarsPadding().padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (thread.isBlocked) "You've blocked this sender" else "This sender doesn't accept replies",
                    color = tokens.textTertiary, fontSize = 12.5.sp,
                )
            }
        }

        // Composed last so it draws above the reply bar / "doesn't accept replies" banner above —
        // it used to be composed first, which put that banner's Box on top of this bottom sheet.
        MessageActionSheet(
            visible = state.messageActionTarget != null,
            onDismiss = viewModel::closeMessageActions,
            onCopy = viewModel::copySelectedMessage,
            onReply = viewModel::replyQuotingSelectedMessage,
            onForward = viewModel::forwardSelectedMessage,
            onDelete = viewModel::deleteSelectedMessage,
        )
    }
}

/** WhatsApp-style grouping: a date header before the first message of each calendar day (skipped
 * during in-conversation search, see buildThreadListEntries's showDateHeaders). */
private sealed class ThreadListEntry {
    data class DateHeader(val label: String) : ThreadListEntry()
    data class Msg(val message: MessageUi, val matchedIndices: Set<Int> = emptySet()) : ThreadListEntry()
}

private fun buildThreadListEntries(
    messages: List<MessageUi>,
    matchIndices: Map<Long, Set<Int>> = emptyMap(),
    showDateHeaders: Boolean = true,
): List<ThreadListEntry> {
    val entries = mutableListOf<ThreadListEntry>()
    var previousTimestamp: Long? = null
    for (message in messages) {
        if (showDateHeaders && (previousTimestamp == null || isDifferentDay(message.timestamp, previousTimestamp))) {
            entries += ThreadListEntry.DateHeader(formatDateSeparator(message.timestamp))
        }
        entries += ThreadListEntry.Msg(message, matchIndices[message.id] ?: emptySet())
        previousTimestamp = message.timestamp
    }
    return entries
}

@Composable
private fun DateSeparator(label: String) {
    val tokens = MessagesTheme.tokens
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(
            label,
            color = tokens.textTertiary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .background(tokens.surfaceAlt, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: MessageUi,
    onLongPress: () -> Unit,
    matchedIndices: Set<Int> = emptySet(),
    onOpenUrl: (String) -> Unit = {},
    onCall: (String) -> Unit = {},
    onEmail: (String) -> Unit = {},
    onCopyEntity: (String) -> Unit = {},
) {
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
            Text(highlightedText(message.text, matchedIndices), color = fg, fontSize = 14.sp, lineHeight = 19.sp)
            if (message.detectedEntities.isNotEmpty()) {
                com.phuzle.labs.messages.ui.components.MessageEntityChips(
                    entities = message.detectedEntities,
                    contentColor = fg,
                    onOpenUrl = onOpenUrl,
                    onCall = onCall,
                    onEmail = onEmail,
                    onCopy = onCopyEntity,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Text(message.timeLabel, color = fg.copy(alpha = 0.65f), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
