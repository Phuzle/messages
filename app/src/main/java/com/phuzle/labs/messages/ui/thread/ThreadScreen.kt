package com.phuzle.labs.messages.ui.thread

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.AvatarBubble
import com.phuzle.labs.messages.ui.theme.barElevation
import com.phuzle.labs.messages.ui.theme.bubbleShape
import com.phuzle.labs.messages.ui.theme.softElevation
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
import com.phuzle.labs.messages.ui.components.withLongPressHaptic
import com.phuzle.labs.messages.domain.search.SearchMatcher
import com.phuzle.labs.messages.ui.format.formatDateSeparator
import com.phuzle.labs.messages.ui.format.isDifferentDay
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.model.MessageActionTargetUi
import com.phuzle.labs.messages.ui.model.MessageUi
import com.phuzle.labs.messages.ui.theme.MessagesTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThreadScreen(state: AppUiState, viewModel: AppViewModel, scrollStore: ThreadScrollStore) {
    val tokens = MessagesTheme.tokens
    val thread = state.currentThread ?: return
    // Coming back from Contact info re-creates this screen; its list state (and the fact that the
    // initial scroll-to-latest already happened) must survive that or the reader is thrown to the
    // bottom of the conversation.
    val returningToThread = remember(thread.id) { scrollStore.has(thread.id) }
    val listState = scrollStore.stateFor(thread.id)
    val latestState = rememberUpdatedState(state)
    val context = androidx.compose.ui.platform.LocalContext.current
    val threadSearchFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(state.threadSearchActive) {
        if (state.threadSearchActive) {
            threadSearchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Bounded message buffer: scrolling near the top fetches one more page of history; scrolling
    // back down well clear of it releases those pages again so long threads don't sit fully in memory.
    LaunchedEffect(thread.id, listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
            val s = latestState.value
            // Older pages stay loaded once fetched (they are small): dropping them as soon as the
            // reader moved a screen away both shifted the list under them and re-fetched the same
            // pages when they scrolled back up.
            if (index <= 3 && s.hasMoreOlderMessages && !s.isLoadingOlderMessages) viewModel.loadOlderMessages()
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
    // scrolledToBottomFor so it only happens once per thread open, not on every later message.
    var scrolledToBottomFor by remember { mutableStateOf<String?>(if (returningToThread) thread.id else null) }
    // Separately, after that initial landing, a *new* message extending the list should pull the
    // view down to it too — but only when the reader was already at (or basically at) the
    // bottom. Someone scrolled up rereading older history shouldn't get yanked down to the
    // newest message the instant one arrives; this mirrors how any other chat app behaves.
    var lastKnownEntryCount by remember { mutableIntStateOf(0) }
    var skipNextCountCheck by remember { mutableStateOf(returningToThread) }

    val canSend = state.threadInput.isNotBlank()
    val scheduleState = com.phuzle.labs.messages.ui.components.rememberScheduleSendState()
    val listEntries = remember(state.currentThreadMessages, state.threadSearchQuery, state.threadSearchResults) {
        val query = state.threadSearchQuery.trim()
        val parsed = SearchMatcher.parse(query)
        if (parsed == null) {
            buildThreadListEntries(state.currentThreadMessages)
        } else {
            // Searching within the conversation filters to just the matches — date headers would
            // just be clutter across a sparse, non-contiguous result set — with matched characters
            // bolded. Hits come from the whole conversation (see AppViewModel.threadSearchOutput),
            // not only the pages that happen to be loaded.
            val matches = state.threadSearchResults.mapNotNull { m -> SearchMatcher.match(parsed, m.text)?.let { m to it.indices } }
            buildThreadListEntries(matches.map { it.first }, matches.associate { it.first.id to it.second }, showDateHeaders = false)
        }
    }

    // A search result was opened: bring that message into view and flash it. Handled before (and
    // instead of) the "land on the latest message" logic below, which must then stay out of the way.
    LaunchedEffect(state.pendingScrollToMessageId, listEntries) {
        val target = state.pendingScrollToMessageId ?: return@LaunchedEffect
        val index = listEntries.indexOfFirst { it is ThreadListEntry.Msg && it.message.id == target }
        if (index < 0) return@LaunchedEffect
        // A couple of rows of context above the hit rather than pinning it to the very top edge.
        listState.scrollToItem((index - 2).coerceAtLeast(0))
        scrolledToBottomFor = thread.id
        lastKnownEntryCount = listEntries.size
        viewModel.onScrolledToMessage(target)
    }

    LaunchedEffect(thread.id, listEntries.size) {
        if (listEntries.isEmpty()) return@LaunchedEffect
        if (state.pendingScrollToMessageId != null) return@LaunchedEffect
        if (skipNextCountCheck) {
            skipNextCountCheck = false
            lastKnownEntryCount = listEntries.size
            return@LaunchedEffect
        }
        if (scrolledToBottomFor != thread.id) {
            listState.scrollToItem(listEntries.size - 1)
            scrolledToBottomFor = thread.id
        } else if (state.threadSearchQuery.isBlank() && listEntries.size > lastKnownEntryCount) {
            // Not a search-driven count change (see listEntries above) — a genuinely new message.
            // LazyColumn doesn't reflow existing items when the list grows, so whatever was the
            // last *visible* index right now still reflects where the reader was relative to the
            // list's *old* end, before this item was appended.
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val wasAtBottom = lastVisibleIndex >= lastKnownEntryCount - 2
            if (wasAtBottom) listState.animateScrollToItem(listEntries.size - 1)
        }
        lastKnownEntryCount = listEntries.size
    }

    // Height of whatever occupies the bottom edge (reply bar, or the "can't reply" banner), so the
    // list reserves exactly that much instead of a fixed guess that left a dead gap above the
    // slimmer banner.
    val bottomDensity = androidx.compose.ui.platform.LocalDensity.current
    var bottomBarPx by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val bottomBarHeight = with(bottomDensity) { bottomBarPx.toDp() }

    Box(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            state = listState,
            // The top inset lives here, on the LazyColumn's own bounds, rather than in
            // contentPadding — stickyHeader pins to this Composable's top edge and ignores
            // contentPadding entirely (confirmed via uiautomator: the pinned header's bounds sat
            // at y≈13px, i.e. the true top of the screen). With the inset in contentPadding, the
            // header pins there too, landing completely hidden behind the opaque GlassBar drawn
            // over it — it IS sticking, just invisibly. Shrinking the LazyColumn itself instead
            // makes its top edge (and thus the sticky pin point) land exactly where the header
            // should visually rest, just below the bar.
            modifier = Modifier.fillMaxSize().padding(top = topBarContentPadding(78.dp), bottom = bottomBarHeight + 8.dp, start = 16.dp, end = 16.dp),
            // Spacing is per-bubble (3dp inside a run, 12dp between runs), see MessageBubble.
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            if (state.isLoadingOlderMessages) {
                item { MessageSkeletonRow(alignEnd = false) }
                item { MessageSkeletonRow(alignEnd = true) }
            }
            // Each date's header is a stickyHeader (not a plain item) so it pins to the top while
            // that day's messages scroll past underneath — otherwise, opening a day with enough
            // messages to fill the screen means the only way to tell which day you're looking at
            // is to scroll all the way back up to its header.
            listEntries.forEachIndexed { index, entry ->
                when (entry) {
                    is ThreadListEntry.DateHeader -> {
                        stickyHeader(key = "date-${entry.label}", contentType = "date-header") {
                            DateSeparator(entry.label)
                        }
                    }
                    is ThreadListEntry.Msg -> {
                        // Run grouping is derived purely from the entries already on screen (same sender,
                        // same day, within a few minutes): the last bubble of a run carries the tail and
                        // timestamp, and the first gets the larger gap above it.
                        val previous = (listEntries.getOrNull(index - 1) as? ThreadListEntry.Msg)?.message
                        val next = (listEntries.getOrNull(index + 1) as? ThreadListEntry.Msg)?.message
                        val startsRun = previous == null || !continuesRun(previous, entry.message)
                        val endsRun = next == null || !continuesRun(entry.message, next)
                        item(key = entry.message.id, contentType = "message") {
                            val selectionMode = state.selectedMessageIds.isNotEmpty()
                            MessageBubble(
                                entry.message,
                                startsRun = startsRun,
                                endsRun = endsRun,
                                matchedIndices = entry.matchedIndices,
                                selected = entry.message.id in state.selectedMessageIds,
                                highlighted = state.highlightMessageId == entry.message.id,
                                onClick = {
                                    when {
                                        selectionMode -> viewModel.toggleMessageSelection(entry.message.id)
                                        // A hit in the conversation-search list: leave search and show it in context.
                                        state.threadSearchQuery.isNotBlank() -> viewModel.openThreadAtMessage(thread.id, entry.message.id)
                                    }
                                },
                                onLongPress = {
                                    // A message still waiting to send gets the restricted
                                    // Edit/Delete sheet (see ScheduledMessageActionSheet) instead
                                    // of the normal Copy/Reply/Forward/Delete set — "reply" and
                                    // "forward" don't mean anything for something that hasn't
                                    // actually gone out yet.
                                    if (selectionMode) {
                                        viewModel.toggleMessageSelection(entry.message.id)
                                    } else if (entry.message.isScheduled) {
                                        viewModel.openScheduledMessageActions(entry.message.id, thread.id)
                                    } else {
                                        viewModel.openMessageActions(MessageActionTargetUi(entry.message.id, entry.message.text))
                                    }
                                },
                                onOpenUrl = onOpenUrl,
                                onCall = onCall,
                                onEmail = onEmail,
                                onCopyEntity = viewModel::copyDetectedText,
                            )
                        }
                    }
                }
            }
        }

        GlassBar(modifier = Modifier.align(Alignment.TopCenter), height = 64.dp, inset = BarInset.Top) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.selectedMessageIds.isNotEmpty()) {
                    Box(Modifier.size(48.dp).roundClickable(onClick = viewModel::exitMessageSelection), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel selection", tint = tokens.textPrimary, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        "${state.selectedMessageIds.size} selected", color = tokens.textPrimary, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 4.dp),
                    )
                    Box(Modifier.size(48.dp).roundClickable(onClick = viewModel::selectAllMessages), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.SelectAll, contentDescription = "Select all", tint = tokens.textPrimary, modifier = Modifier.size(20.dp))
                    }
                    Box(Modifier.size(48.dp).roundClickable(onClick = viewModel::copySelectedMessages), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = tokens.textPrimary, modifier = Modifier.size(20.dp))
                    }
                    Box(Modifier.size(48.dp).roundClickable(onClick = viewModel::shareSelectedMessages), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = tokens.textPrimary, modifier = Modifier.size(20.dp))
                    }
                } else if (state.threadSearchActive) {
                    Box(Modifier.size(48.dp).roundClickable(onClick = viewModel::closeThreadSearch), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search", tint = tokens.textPrimary, modifier = Modifier.size(20.dp))
                    }
                    FlatTextField(
                        value = state.threadSearchQuery,
                        onValueChange = viewModel::onThreadSearchChange,
                        placeholder = "Search in conversation",
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                        focusRequester = threadSearchFocusRequester,
                    )
                } else {
                    Box(Modifier.size(48.dp).roundClickable(onClick = viewModel::goBack), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = tokens.textPrimary, modifier = Modifier.size(20.dp))
                    }
                    Row(
                        modifier = Modifier.weight(1f).clickable(onClick = viewModel::openThreadInfo).padding(start = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AvatarBubble(thread.category, thread.avatarColor, thread.isBusiness, size = 40.dp, photoUri = thread.photoUri)
                        Column(Modifier.weight(1f)) {
                            Text(
                                thread.displayName, color = tokens.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                            Text(thread.kindLabel, color = tokens.textTertiary, fontSize = 12.5.sp, maxLines = 1)
                        }
                    }
                    Box(Modifier.size(48.dp).roundClickable(onClick = viewModel::toggleThreadOverflowMenu), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = tokens.textPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        OverflowMenu(
            visible = state.threadOverflowMenuOpen,
            onDismiss = viewModel::closeThreadOverflowMenu,
            items = listOf(
                MenuItem("Search in conversation", icon = Icons.Filled.Search, onClick = viewModel::openThreadSearch),
                MenuItem("View contact info", icon = Icons.Filled.Info, onClick = { viewModel.closeThreadOverflowMenu(); viewModel.openThreadInfo() }),
                MenuItem("Share contact", icon = Icons.Filled.Share, onClick = { viewModel.closeThreadOverflowMenu(); viewModel.shareCurrentContact() }),
                MenuItem(
                    if (thread.isBlocked) "Unblock" else "Block",
                    icon = if (thread.isBlocked) Icons.Filled.LockOpen else Icons.Filled.Block,
                    onClick = { viewModel.closeThreadOverflowMenu(); viewModel.toggleBlockCurrent() },
                ),
                MenuItem("Archive", icon = Icons.Filled.Archive, onClick = viewModel::archiveCurrentThread),
                MenuItem("Delete conversation", icon = Icons.Filled.Delete, danger = true, onClick = viewModel::deleteCurrentThread),
            ),
        )

        if (thread.isReplyable && !thread.isBlocked) {
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().onSizeChanged { bottomBarPx = it.height }.barElevation(atTop = true).background(tokens.surface).navigationBarsPadding()) {
                if (state.settings.showCharCount) {
                    Text(
                        "${state.threadInput.length} characters", color = tokens.textTertiary, fontSize = 11.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 6.dp),
                    )
                }
                state.threadCustomScheduleMillis?.let { millis ->
                    Row(
                        modifier = Modifier
                            .padding(start = 16.dp, top = 8.dp)
                            .clip(com.phuzle.labs.messages.ui.theme.ShapePill)
                            .background(tokens.accentSoft, com.phuzle.labs.messages.ui.theme.ShapePill)
                            .clickable(onClick = { viewModel.setThreadCustomSchedule(null) })
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Same accentSoft-background/accent-foreground pairing as Compose's own
                        // schedule chip — accentText only reads correctly on the solid accent
                        // background, not this translucent tint (see ComposeScreen).
                        Text(
                            com.phuzle.labs.messages.ui.format.formatScheduleTime(millis),
                            color = tokens.accentLabel, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
                        )
                        Icon(Icons.Filled.Close, contentDescription = "Clear schedule", tint = tokens.accentLabel, modifier = Modifier.size(12.dp))
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier.size(48.dp).background(tokens.surfaceAlt, CircleShape).roundClickable(onClick = scheduleState::start),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Schedule send", tint = tokens.textSecondary, modifier = Modifier.size(20.dp))
                    }
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
                            .size(48.dp)
                            .background(if (canSend) tokens.accentDeep else tokens.surfaceAlt, CircleShape)
                            .let { if (canSend) it.roundClickable(onClick = viewModel::sendThreadMessage) else it },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (state.threadCustomScheduleMillis != null) Icons.Filled.Schedule else Icons.AutoMirrored.Filled.Send,
                            contentDescription = if (state.threadCustomScheduleMillis != null) "Schedule" else "Send",
                            tint = if (canSend) tokens.onAccentDeep else tokens.textTertiary, modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        } else {
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().onSizeChanged { bottomBarPx = it.height }.background(tokens.surface).navigationBarsPadding().padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (thread.isBlocked) "You've blocked this sender" else "This sender doesn't accept replies",
                    color = tokens.textTertiary, fontSize = 13.5.sp,
                )
            }
        }

        // Only worth surfacing once there's actually somewhere to jump to — canScrollForward means
        // more (newer) content exists below the current viewport, i.e. we're not already at the
        // latest message.
        val showScrollToLatest by remember { derivedStateOf { listState.canScrollForward } }
        val scrollToLatestScope = rememberCoroutineScope()
        androidx.compose.animation.AnimatedVisibility(
            visible = showScrollToLatest,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = bottomBarHeight + 12.dp),
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
        ) {
            Box(
                Modifier.size(48.dp).background(tokens.surface, CircleShape).border(1.dp, tokens.border, CircleShape)
                    .roundClickable(onClick = {
                        scrollToLatestScope.launch { listState.animateScrollToItem((listEntries.size - 1).coerceAtLeast(0)) }
                    }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Scroll to latest", tint = tokens.textPrimary, modifier = Modifier.size(22.dp))
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
            onShare = viewModel::shareSelectedMessage,
            onSelect = { state.messageActionTarget?.let { viewModel.startMessageSelection(it.id) } },
        )
    }

    com.phuzle.labs.messages.ui.components.ScheduleSendDialogs(scheduleState, onScheduled = viewModel::setThreadCustomSchedule)
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
    // Solid full-width band, not just a transparent-surround pill: while pinned, this renders
    // directly on top of whatever message is currently scrolled to the front — see the
    // LazyColumn's modifier comment above — so it needs its own opaque backing (matching the
    // screen background) to fully mask that message instead of letting it show through around
    // the pill.
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            label,
            color = tokens.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .background(tokens.surfaceAlt, com.phuzle.labs.messages.ui.theme.ShapePill)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

/** Two bubbles belong to one run when they share a sender and sit within a few minutes of each other. */
private const val RUN_GAP_MILLIS = 5 * 60 * 1000L

private fun continuesRun(earlier: MessageUi, later: MessageUi): Boolean =
    earlier.isMine == later.isMine &&
        !isDifferentDay(later.timestamp, earlier.timestamp) &&
        later.timestamp - earlier.timestamp in 0..RUN_GAP_MILLIS

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: MessageUi,
    startsRun: Boolean,
    endsRun: Boolean,
    onLongPress: () -> Unit,
    matchedIndices: Set<Int> = emptySet(),
    selected: Boolean = false,
    highlighted: Boolean = false,
    onClick: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    onCall: (String) -> Unit = {},
    onEmail: (String) -> Unit = {},
    onCopyEntity: (String) -> Unit = {},
) {
    val tokens = MessagesTheme.tokens
    val dark = MessagesTheme.isDark
    val shape = bubbleShape(isMine = message.isMine, hasTail = endsRun)
    // A message still waiting to send is an outlined placeholder (dashed, accent-labelled, with a
    // clock) so it reads as "not sent yet" at a glance instead of looking like a received message.
    val scheduled = message.isScheduled
    val bg = when {
        scheduled -> androidx.compose.ui.graphics.Color.Transparent
        message.isMine -> tokens.accentDeep
        dark -> tokens.surfaceAlt
        else -> tokens.surface
    }
    val fg = when {
        scheduled -> tokens.textSecondary
        message.isMine -> tokens.onAccentDeep
        else -> tokens.textPrimary
    }
    val maxBubbleWidth = (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp * 0.78f).dp
    val dashColor = tokens.accentLabel
    val density = androidx.compose.ui.platform.LocalDensity.current
    // Flashes a message reached from a search result: a ring that fades out on its own.
    val flash by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (highlighted) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(if (highlighted) 150 else 900),
        label = "flash",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = if (startsRun) 12.dp else 3.dp)
            .then(if (selected) Modifier.background(tokens.accentSoft) else Modifier),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .then(if (!scheduled && !message.isMine) Modifier.softElevation(shape) else Modifier)
                .background(bg, shape)
                .then(
                    if (scheduled) {
                        Modifier.drawBehind {
                            drawOutline(
                                outline = shape.createOutline(size, layoutDirection, density),
                                color = dashColor,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                                ),
                            )
                        }
                    } else {
                        Modifier
                    },
                )
                .then(if (flash > 0f) Modifier.border(2.dp, tokens.accentLabel.copy(alpha = flash), shape) else Modifier)
                .combinedClickable(onClick = onClick, onLongClick = withLongPressHaptic(onLongPress))
                .padding(horizontal = 16.dp, vertical = 11.dp),
        ) {
            Text(com.phuzle.labs.messages.ui.components.rememberHighlightedText(message.text, matchedIndices), color = fg, fontSize = 15.sp, lineHeight = 22.sp)
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
            // Right-aligned regardless of which side the bubble itself is on — the timestamp
            // reads as metadata about the message, not body text. Shown on the last bubble of a
            // run only (and always on a scheduled one, where it says when the message will send).
            if (scheduled) {
                Row(
                    Modifier.align(Alignment.End).padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = dashColor, modifier = Modifier.size(13.dp))
                    Text(message.timeLabel, color = dashColor, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }
            } else if (endsRun) {
                Text(
                    message.timeLabel, color = fg.copy(alpha = 0.7f), fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                )
            }
        }
    }
}
