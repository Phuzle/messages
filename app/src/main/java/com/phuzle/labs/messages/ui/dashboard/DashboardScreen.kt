package com.phuzle.labs.messages.ui.dashboard

import com.phuzle.labs.messages.ui.theme.softElevation
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import com.phuzle.labs.messages.ui.model.SearchResultUi
import com.phuzle.labs.messages.ui.components.SearchSectionHeader
import com.phuzle.labs.messages.ui.components.SearchMessageRow
import com.phuzle.labs.messages.ui.components.SearchFiltersSheet
import com.phuzle.labs.messages.ui.components.PillButton
import com.phuzle.labs.messages.domain.search.SearchSort
import com.phuzle.labs.messages.domain.search.SearchRange
import com.phuzle.labs.messages.domain.search.SearchFilters
import com.phuzle.labs.messages.domain.search.SearchDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.input.pointer.pointerInput
import com.phuzle.labs.messages.domain.model.Category
import com.phuzle.labs.messages.domain.model.FeatureFlags
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat as ChatOutlined
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountBalanceWallet as AccountBalanceWalletOutlined
import androidx.compose.material.icons.outlined.MarkEmailUnread as MarkEmailUnreadOutlined
import androidx.compose.material.icons.outlined.Notifications as NotificationsOutlined
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.BarInset
import com.phuzle.labs.messages.ui.components.CategoryChip
import com.phuzle.labs.messages.ui.components.FlatTextField
import com.phuzle.labs.messages.ui.components.EmptyState
import com.phuzle.labs.messages.ui.components.GlassBar
import com.phuzle.labs.messages.ui.components.SelectionOverflowMenu
import com.phuzle.labs.messages.ui.components.SelectionTopBar
import com.phuzle.labs.messages.ui.components.ThreadRow
import com.phuzle.labs.messages.ui.components.roundClickable
import com.phuzle.labs.messages.ui.model.AccountUi
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.model.DashboardTab
import com.phuzle.labs.messages.ui.model.ReminderUi
import androidx.compose.ui.draw.shadow
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium
import com.phuzle.labs.messages.ui.theme.ShapePill
import com.phuzle.labs.messages.ui.theme.ShapeSmall
import com.phuzle.labs.messages.ui.theme.barElevation

private const val BOTTOM_BAR_HEIGHT = 60

/** Compose-FAB geometry, shared by the FAB itself (near the bottom of this file) and by the list
 * padding that has to scroll clear of it — see [bottomContentPadding]'s use of these. */
private const val FAB_SIZE = 56
private const val FAB_MARGIN = 16

@Composable
fun DashboardScreen(state: AppUiState, viewModel: AppViewModel, scroll: DashboardScrollState) {
    val tokens = MessagesTheme.tokens
    val context = androidx.compose.ui.platform.LocalContext.current
    val isMessages = state.activeTab == DashboardTab.Messages
    val filters = state.searchFilters
    val filtersRestrict = filters.category != Category.All ||
        filters.range != com.phuzle.labs.messages.domain.search.SearchRange.Any ||
        filters.direction != com.phuzle.labs.messages.domain.search.SearchDirection.Any
    // Search mode with something to look for: the list switches from one-row-per-conversation to
    // one-row-per-matching-message.
    val showResults = isMessages && state.searchActive && (state.searchQuery.isNotBlank() || filtersRestrict)
    val navBarInset = com.phuzle.labs.messages.ui.components.remainingNavBarInset()
    // Closed-testing-only — see AppViewModel.dismissFeedbackBanner for the "comes back tomorrow"
    // behavior. A real first item in the list below (scrolls with everything else, shown for
    // every category filter), not a fixed overlay — so unlike the header, it needs no special
    // handling here at all.
    val showFeedbackBanner = isMessages &&
        state.settings.feedbackBannerDismissedDay != com.phuzle.labs.messages.ui.format.currentLocalEpochDay()
    // Measured from the actual rendered header (top bar + category chips) instead of a hand-typed
    // estimate — a mismatched guess here (this used to be a flat 104.dp/68.dp constant) is exactly
    // what let list content render partially behind the header instead of just below it, most
    // visibly for a thread restored via undo animating straight to the wrong spot.
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val topContentPadding = with(density) { headerHeightPx.toDp() }
    // Has to clear the compose FAB as well as the nav bar and the optional bottom bar. The FAB is a
    // fixed overlay in the same Box as these lists, so leaving its height out of here (this used to
    // be a flat 26.dp of breathing room) meant the final row of every list could never be scrolled
    // out from under it — the last thread's timestamp and unread badge sat permanently behind the
    // FAB with no scroll position that would reveal them.
    val bottomContentPadding = (if (FeatureFlags.PASSBOOK_AND_REMINDERS_ENABLED) BOTTOM_BAR_HEIGHT else 0).dp +
        (FAB_MARGIN + FAB_SIZE + 16).dp + navBarInset
    // Hoisted out of GlassBar's content (not just declared inline in the selection bar below) so
    // SelectionOverflowMenu can be rendered as a sibling of GlassBar instead of nested inside it —
    // GlassBar's shadow clips its content by default, which would make the popup invisible and
    // untouchable the moment it's taller than the bar itself. See SelectionTopBar.kt.
    var showSelectionMenu by remember { mutableStateOf(false) }
    // Hoisted out of the selection bar: an overlay nested inside GlassBar would be clipped to it.
    var showBulkArchiveConfirm by remember { mutableStateOf(false) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        when (state.activeTab) {
            DashboardTab.Messages -> if (showResults) {
                SearchResults(
                    state = state,
                    viewModel = viewModel,
                    listState = scroll.results,
                    topPadding = topContentPadding,
                    bottomPadding = bottomContentPadding,
                )
            } else if (state.threads.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(top = topContentPadding, bottom = bottomContentPadding)) {
                    if (showFeedbackBanner) {
                        FeedbackBanner(
                            onSendFeedback = {
                                com.phuzle.labs.messages.core.util.openSupportEmail(context, subject = "Messages closed-testing feedback")
                            },
                            onDismiss = viewModel::dismissFeedbackBanner,
                            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp),
                        )
                    }
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = if (state.searchQuery.isNotBlank() || state.activeCategory != Category.All || state.unreadOnly) "No matching messages" else "No messages yet",
                        detail = if (state.searchQuery.isNotBlank() || state.activeCategory != Category.All || state.unreadOnly) {
                            "Try a different search, category, or filter."
                        } else {
                            "Incoming texts will show up here automatically."
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                val listState = scroll.inbox
                // Undoing an archive/delete brings a thread back near the top of a recency-sorted
                // list, but LazyColumn's key-based scroll anchoring keeps whatever was already on
                // screen pinned in place — so the restored thread can reappear scrolled out of
                // view above the top edge instead of visibly popping back in. Scroll back to the
                // top whenever that happens (see AppViewModel.confirmUndo).
                LaunchedEffect(Unit) {
                    viewModel.scrollToTopEvents.collect { listState.animateScrollToItem(0) }
                }
                // Switching category chips or the unread-only filter swaps out state.threads
                // entirely, but LazyColumn's key-based scroll anchoring has no idea the new list
                // is conceptually a different view — it just keeps whatever scroll offset was
                // already set, which can land you mid-list in the new filter (e.g. back on "All"
                // after "Personal" scrolled you partway down, still showing wherever Personal's
                // items happened to leave the offset). A filter change should always read from
                // the top, like a fresh list.
                // Skipped on the very first composition, and on every re-entry to this screen (coming
                // back from a thread): the list state now outlives the screen, so "composed again"
                // is not "the filter changed" — only an actual change from the last seen value is.
                LaunchedEffect(state.activeCategory, state.unreadOnly) {
                    val key = state.activeCategory to state.unreadOnly
                    if (scroll.lastFilter != null && scroll.lastFilter != key) listState.scrollToItem(0)
                    scroll.lastFilter = key
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = topContentPadding + 10.dp, bottom = bottomContentPadding),
                ) {
                    // The first real item in the list, not a fixed overlay — scrolls with
                    // everything else and shows for every category filter (not just All), unless
                    // dismissed for the day.
                    if (showFeedbackBanner) {
                        item(key = "feedback-banner", contentType = "feedback-banner") {
                            FeedbackBanner(
                                onSendFeedback = {
                                    com.phuzle.labs.messages.core.util.openSupportEmail(context, subject = "Messages closed-testing feedback")
                                },
                                onDismiss = viewModel::dismissFeedbackBanner,
                                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                            )
                        }
                    }
                    items(state.threads, key = { it.id }) { thread ->
                        ThreadRow(
                            thread = thread,
                            swipeRightActionKey = state.settings.swipeRightAction,
                            swipeLeftActionKey = state.settings.swipeLeftAction,
                            onOpen = { viewModel.openThreadById(thread.id) },
                            onLongPress = { viewModel.openActionSheet(thread.id) },
                            onSwipeRight = { viewModel.onSwipeRight(thread.id) },
                            onSwipeLeft = { viewModel.onSwipeLeft(thread.id) },
                            modifier = Modifier.animateItem(),
                            selectionMode = state.multiSelectThreadIds.isNotEmpty(),
                            selected = thread.id in state.multiSelectThreadIds,
                            onToggleSelect = { viewModel.toggleThreadSelection(thread.id) },
                            onAvatarClick = { viewModel.openThreadInfoById(thread.id) },
                            onAvatarLongPress = { viewModel.startMultiSelect(thread.id) },
                        )
                    }
                }
            }
            DashboardTab.Passbook -> if (state.accounts.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "No transactions yet",
                    detail = "Bank and card messages are captured automatically as they arrive — nothing to show until then.",
                    modifier = Modifier.padding(top = topContentPadding, bottom = bottomContentPadding),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = topContentPadding, bottom = bottomContentPadding),
                ) {
                    items(state.accounts, key = { it.last4 }) { account ->
                        AccountCard(account = account, onClick = { viewModel.openAccountDetail(account.last4) }, modifier = Modifier.animateItem())
                    }
                }
            }
            DashboardTab.Reminders -> if (state.reminders.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.NotificationsNone,
                    title = "No reminders yet",
                    detail = "Reminders will appear here once we can reliably detect due dates and follow-ups from your messages.",
                    modifier = Modifier.padding(top = topContentPadding, bottom = bottomContentPadding),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = topContentPadding, bottom = bottomContentPadding, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.reminders, key = { it.id }) { reminder ->
                        ReminderCard(reminder, onDismiss = { viewModel.dismissReminder(reminder.id) }, modifier = Modifier.animateItem())
                    }
                }
            }
        }

        Column(
            Modifier.align(Alignment.TopCenter).onGloballyPositioned { headerHeightPx = it.size.height },
        ) {
            GlassBar(height = 64.dp, inset = BarInset.Top) {
                val barMode = when {
                    state.multiSelectThreadIds.isNotEmpty() -> DashboardBarMode.Selection
                    isMessages && state.searchActive -> DashboardBarMode.Search
                    else -> DashboardBarMode.Browse
                }
                // Kept through the bar's fade-out, once the selection itself is already empty.
                val selectedCount = com.phuzle.labs.messages.ui.components.rememberLast(state.multiSelectThreadIds.size.takeIf { it > 0 }) ?: 0
                androidx.compose.animation.AnimatedContent(
                    targetState = barMode,
                    modifier = Modifier.fillMaxWidth(),
                    transitionSpec = {
                        val intoSearch = targetState == DashboardBarMode.Search
                        val outOfSearch = initialState == DashboardBarMode.Search
                        val enterShift: (Int) -> Int = { width -> if (intoSearch) width / 10 else if (outOfSearch) -width / 10 else 0 }
                        (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220, delayMillis = 70)) +
                            androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(260), enterShift))
                            .togetherWith(androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)))
                    },
                    label = "dashboardBar",
                ) { mode ->
                when (mode) {
                DashboardBarMode.Selection -> {
                    SelectionTopBar(
                        count = selectedCount,
                        onClose = viewModel::exitMultiSelect,
                        onMenuClick = { showSelectionMenu = true },
                    ) {
                        Box(
                            Modifier.size(48.dp).roundClickable(onClick = viewModel::shareSelectedContacts),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Share, contentDescription = "Share contacts", tint = tokens.textPrimary, modifier = Modifier.size(20.dp)) }
                        Box(
                            Modifier.size(48.dp).roundClickable(onClick = viewModel::bulkMarkReadSelected),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.MarkEmailRead, contentDescription = "Mark as read", tint = tokens.textPrimary, modifier = Modifier.size(20.dp)) }
                        Box(
                            Modifier.size(48.dp).roundClickable(onClick = { showBulkArchiveConfirm = true }),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Archive, contentDescription = "Archive", tint = tokens.textPrimary, modifier = Modifier.size(20.dp)) }
                        Box(
                            Modifier.size(48.dp).roundClickable(onClick = { showBulkDeleteConfirm = true }),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = tokens.danger, modifier = Modifier.size(20.dp)) }
                    }
                }
                DashboardBarMode.Search -> SearchModeBar(state, viewModel)
                DashboardBarMode.Browse -> Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).roundClickable(onClick = viewModel::toggleDrawer),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = tokens.textPrimary, modifier = Modifier.size(22.dp)) }

                    when (state.activeTab) {
                        DashboardTab.Messages -> Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(ShapePill)
                                .background(tokens.inputBg, ShapePill)
                                .clickable(onClick = viewModel::enterSearch)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = tokens.textTertiary, modifier = Modifier.size(20.dp))
                            Text("Search messages", color = tokens.textTertiary, fontSize = 15.sp, maxLines = 1)
                        }
                        DashboardTab.Passbook -> Text("Passbook", color = tokens.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        DashboardTab.Reminders -> Text("Reminders", color = tokens.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    }

                    if (isMessages) {
                        Box(
                            Modifier.size(48.dp).roundClickable(onClick = viewModel::toggleUnreadOnly),
                            contentAlignment = Alignment.Center,
                        ) {
                            // An envelope-with-a-dot reads as "unread messages" on sight — the
                            // generic funnel this replaced looked like it filtered by category,
                            // which is what the chips above it already do.
                            Icon(
                                if (state.unreadOnly) Icons.Filled.MarkEmailUnread else Icons.Outlined.MarkEmailUnreadOutlined,
                                contentDescription = if (state.unreadOnly) "Showing unread only" else "Show unread only",
                                tint = if (state.unreadOnly) tokens.accentLabel else tokens.textPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Box(
                        Modifier.size(48.dp).roundClickable(onClick = viewModel::toggleOverflowMenu),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = tokens.textPrimary, modifier = Modifier.size(22.dp)) }
                }
                }
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = isMessages && state.searchActive && filters.activeCount > 0,
                enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(200)) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)),
                exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(160)) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)),
            ) {
                SearchFilterSummary(filters, viewModel)
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = isMessages && !state.searchActive && state.categories.isNotEmpty(),
                enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(220)) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)),
                exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(180)) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)),
            ) {
                val categoryScrollState = rememberScrollState()
                val coroutineScope = rememberCoroutineScope()
                var rowWidth by remember { mutableIntStateOf(0) }
                val chipBounds = remember { mutableStateMapOf<Category, Pair<Int, Int>>() }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .barElevation()
                        .background(tokens.surface)
                        .onGloballyPositioned { rowWidth = it.size.width }
                        .horizontalScroll(categoryScrollState)
                        .padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.categories.forEach { chip ->
                        CategoryChip(
                            chip = chip,
                            modifier = Modifier.onGloballyPositioned { coords ->
                                chipBounds[chip.category] = coords.positionInParent().x.toInt() to coords.size.width
                            },
                            onClick = {
                                viewModel.setCategory(chip.category)
                                chipBounds[chip.category]?.let { (x, width) ->
                                    val target = (x + width / 2) - rowWidth / 2
                                    coroutineScope.launch {
                                        categoryScrollState.animateScrollTo(target.coerceIn(0, categoryScrollState.maxValue))
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }

        if (FeatureFlags.PASSBOOK_AND_REMINDERS_ENABLED) {
            GlassBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                height = BOTTOM_BAR_HEIGHT.dp,
                inset = BarInset.Bottom,
            ) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    BottomTabButton(
                        label = "Messages",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        iconOutlined = Icons.AutoMirrored.Outlined.ChatOutlined,
                        active = isMessages,
                        hasUnreadDot = state.hasUnread,
                        onClick = viewModel::openMessagesTab,
                        modifier = Modifier.weight(1f),
                    )
                    BottomTabButton(
                        label = "Passbook",
                        icon = Icons.Filled.AccountBalanceWallet,
                        iconOutlined = Icons.Outlined.AccountBalanceWalletOutlined,
                        active = state.activeTab == DashboardTab.Passbook,
                        hasUnreadDot = false,
                        onClick = viewModel::openPassbookTab,
                        modifier = Modifier.weight(1f),
                    )
                    BottomTabButton(
                        label = "Reminders",
                        icon = Icons.Filled.Notifications,
                        iconOutlined = Icons.Outlined.NotificationsOutlined,
                        active = state.activeTab == DashboardTab.Reminders,
                        hasUnreadDot = false,
                        onClick = viewModel::openRemindersTab,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (!state.searchActive) Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = FAB_MARGIN.dp,
                    bottom = (if (FeatureFlags.PASSBOOK_AND_REMINDERS_ENABLED) BOTTOM_BAR_HEIGHT + FAB_MARGIN else FAB_MARGIN).dp + navBarInset,
                )
                .size(FAB_SIZE.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = tokens.accentDeep.copy(alpha = 0.25f),
                    spotColor = tokens.accentDeep.copy(alpha = 0.55f),
                )
                .background(tokens.accentDeep, CircleShape)
                .roundClickable(onClick = viewModel::openCompose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Compose", tint = tokens.onAccentDeep, modifier = Modifier.size(26.dp))
        }

        com.phuzle.labs.messages.ui.components.AppDialog(
            visible = showBulkArchiveConfirm,
            onDismiss = { showBulkArchiveConfirm = false },
            title = "Archive ${state.multiSelectThreadIds.size} chats?",
            message = "They move to Archived and come back if a new message arrives.",
            confirmLabel = "Archive",
            onConfirm = { viewModel.bulkArchiveSelected(); showBulkArchiveConfirm = false },
        )
        com.phuzle.labs.messages.ui.components.AppDialog(
            visible = showBulkDeleteConfirm,
            onDismiss = { showBulkDeleteConfirm = false },
            title = "Delete ${state.multiSelectThreadIds.size} chats?",
            message = "Deleted chats move to the Recycle Bin for 30 days before they're purged for good.",
            confirmLabel = "Delete",
            onConfirm = { viewModel.bulkDeleteSelected(); showBulkDeleteConfirm = false },
            destructive = true,
        )

        SelectionOverflowMenu(
            visible = showSelectionMenu,
            onDismiss = { showSelectionMenu = false },
            onSelectAll = { viewModel.selectAllThreads(state.threads.map { it.id }) },
            onInvertSelection = { viewModel.invertThreadSelection(state.threads.map { it.id }) },
        )

        SearchFiltersSheet(
            visible = state.searchFiltersOpen,
            filters = filters,
            categories = state.categories.map { it.category },
            onChange = viewModel::setSearchFilters,
            onDismiss = viewModel::closeSearchFilters,
        )
    }
}

/** Hoisted above the screen (see AppRoot) so scroll positions survive opening a thread and coming back. */
class DashboardScrollState(val inbox: LazyListState, val results: LazyListState) {
    var lastFilter: Pair<Category, Boolean>? = null
}

private enum class DashboardBarMode { Selection, Search, Browse }

@Composable
private fun SearchModeBar(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Only for a fresh search — coming back from an opened result must not re-raise the keyboard.
    LaunchedEffect(Unit) {
        if (state.searchQuery.isEmpty()) {
            focus.requestFocus()
            keyboard?.show()
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).roundClickable(onClick = viewModel::exitSearch), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search", tint = tokens.textPrimary, modifier = Modifier.size(22.dp))
        }
        Row(
            modifier = Modifier.weight(1f).height(48.dp).background(tokens.inputBg, ShapePill).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = tokens.textTertiary, modifier = Modifier.size(20.dp))
            FlatTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchChange,
                placeholder = "Search messages",
                modifier = Modifier.weight(1f).padding(start = 10.dp),
                focusRequester = focus,
            )
            if (state.searchQuery.isNotEmpty()) {
                Box(Modifier.size(40.dp).roundClickable(onClick = viewModel::clearSearchText), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = tokens.textSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }
        Box(Modifier.size(48.dp).roundClickable(onClick = viewModel::openSearchFilters), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.Tune, contentDescription = "Search filters",
                tint = if (state.searchFilters.activeCount > 0) tokens.accentLabel else tokens.textPrimary,
                modifier = Modifier.size(22.dp),
            )
            if (state.searchFilters.activeCount > 0) {
                Box(Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp).size(9.dp).background(tokens.accentDeep, CircleShape))
            }
        }
    }
}

/** The non-default filters as removable chips, so it is always visible what is narrowing the results. */
@Composable
private fun SearchFilterSummary(filters: SearchFilters, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .barElevation()
            .background(tokens.surface)
            .horizontalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (filters.sort != SearchSort.BestMatch) PillButton("Sort: ${filters.sort.label}", true, { viewModel.setSearchFilters(filters.copy(sort = SearchSort.BestMatch)) }, icon = Icons.Filled.Close)
        if (filters.category != Category.All) PillButton(filters.category.label, true, { viewModel.setSearchFilters(filters.copy(category = Category.All)) }, icon = Icons.Filled.Close)
        if (filters.range != SearchRange.Any) PillButton(filters.range.label, true, { viewModel.setSearchFilters(filters.copy(range = SearchRange.Any)) }, icon = Icons.Filled.Close)
        if (filters.direction != SearchDirection.Any) PillButton(filters.direction.label, true, { viewModel.setSearchFilters(filters.copy(direction = SearchDirection.Any)) }, icon = Icons.Filled.Close)
    }
}

@Composable
private fun SearchResults(
    state: AppUiState,
    viewModel: AppViewModel,
    listState: LazyListState,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    // A new query or filter set starts reading from the top; re-entering after opening a result
    // (same query/filters) keeps the position.
    val key = Triple(state.searchQuery.trim(), state.searchFilters, state.searchLoaded)
    val lastKey = remember { androidx.compose.runtime.mutableStateOf<Any?>(null) }
    LaunchedEffect(key) {
        if (lastKey.value != null && lastKey.value != key) listState.scrollToItem(0)
        lastKey.value = key
    }
    val contacts = remember(state.searchResults) { state.searchResults.filterIsInstance<SearchResultUi.Contact>() }
    val messages = remember(state.searchResults) { state.searchResults.filterIsInstance<SearchResultUi.Message>() }
    when {
        !state.searchLoaded -> Box(Modifier.fillMaxSize().padding(top = topPadding))
        state.searchResults.isEmpty() -> EmptyState(
            icon = Icons.Filled.Search,
            title = "No results",
            detail = if (state.searchQuery.isNotBlank()) "Nothing matches “${state.searchQuery.trim()}”. Try fewer words, or loosen the filters." else "Nothing matches these filters.",
            modifier = Modifier.padding(top = topPadding, bottom = bottomPadding),
        )
        else -> LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
        ) {
            if (contacts.isNotEmpty()) {
                item(key = "h-contacts", contentType = "header") { SearchSectionHeader("Contacts") }
                items(contacts, key = { it.key }, contentType = { "contact" }) { result ->
                    ThreadRow(
                        thread = result.thread,
                        swipeRightActionKey = "none",
                        swipeLeftActionKey = "none",
                        onOpen = { viewModel.openThreadById(result.thread.id) },
                        onLongPress = {},
                        onSwipeRight = {},
                        onSwipeLeft = {},
                    )
                }
            }
            if (messages.isNotEmpty()) {
                item(key = "h-messages", contentType = "header") {
                    SearchSectionHeader(
                        if (state.searchTruncated) "Messages · newest ${state.searchMessageCount}" else "Messages · ${state.searchMessageCount}",
                    )
                }
                items(messages, key = { it.key }, contentType = { "message" }) { result ->
                    SearchMessageRow(result, onClick = { viewModel.openThreadAtMessage(result.threadId, result.messageId) })
                }
            }
        }
    }
}

@Composable
private fun BottomTabButton(
    label: String,
    icon: ImageVector,
    iconOutlined: ImageVector,
    active: Boolean,
    hasUnreadDot: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MessagesTheme.tokens
    val color = if (active) tokens.accentLabel else tokens.textTertiary
    Box(modifier.fillMaxSize().clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(
                modifier = Modifier
                    .background(if (active) tokens.accentSoft else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                    .padding(horizontal = 18.dp, vertical = 4.dp),
            ) {
                Icon(if (active) icon else iconOutlined, contentDescription = label, tint = color, modifier = Modifier.size(23.dp))
                if (hasUnreadDot) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(7.dp)
                            .background(tokens.danger, CircleShape),
                    )
                }
            }
            Text(label, color = color, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AccountCard(account: AccountUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = MessagesTheme.tokens
    val amountColor = if (account.netIsCredit) tokens.success else tokens.danger
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (account.selected) tokens.accentSoft else tokens.bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).background(tokens.accentSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = tokens.accentLabel, modifier = Modifier.size(19.dp)) }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("•• ${account.last4}", color = tokens.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(account.netLabel, color = amountColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                "${account.transactionCount} transaction${if (account.transactionCount == 1) "" else "s"}",
                color = tokens.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Closed-testing-only nudge to send feedback — closable, but only for the rest of today (see
 * AppViewModel.dismissFeedbackBanner); it comes back on its own the next day rather than being
 * gone for good after one tap, since the build stays in closed testing regardless. */
@Composable
private fun FeedbackBanner(onSendFeedback: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    // Same flat-row shape as ThreadRow/AccountCard — no card background, no border, no rounding
    // (see DESIGN.md) — just a tinted background standing in for the card look this used to have.
    val tokens = MessagesTheme.tokens
    Row(
        modifier
            .fillMaxWidth()
            // This row sits directly above swipeable ThreadRows in the same list, so users land
            // on it while swiping to archive/delete the row below or above it — a plain
            // `clickable` here still fires on a horizontal drag (nothing claims the gesture the
            // way ThreadRow's own swipe handling does), which used to launch the feedback email
            // client on what was meant as a swipe. Only fire on an actual tap-in-place.
            .pointerInput(onSendFeedback) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val up = waitForUpOrCancellation()
                    if (up != null && (up.position - down.position).getDistance() < viewConfiguration.touchSlop) onSendFeedback()
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.phuzle.labs.messages.ui.components.IconBadge(icon = Icons.Filled.Campaign, size = 40.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("You're testing an early build", color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Tap to email us your feedback", color = tokens.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Box(Modifier.size(32.dp).roundClickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Close, contentDescription = "Dismiss for today", tint = tokens.textSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ReminderCard(reminder: ReminderUi, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = MessagesTheme.tokens
    Row(
        modifier.fillMaxWidth().softElevation(ShapeMedium).background(tokens.surface, ShapeMedium)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(reminder.title, color = tokens.textPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
            Text(reminder.detail, color = tokens.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Text(
                reminder.timeLabel.uppercase(), color = tokens.accentLabel, fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp, modifier = Modifier.padding(top = 6.dp),
            )
        }
        Box(
            Modifier.size(28.dp).roundClickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = "Dismiss reminder", tint = tokens.textTertiary, modifier = Modifier.size(18.dp))
        }
    }
}
