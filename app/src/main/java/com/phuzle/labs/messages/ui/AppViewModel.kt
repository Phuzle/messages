package com.phuzle.labs.messages.ui

import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phuzle.labs.messages.AppContainer
import com.phuzle.labs.messages.core.sms.SubscriptionHelper
import com.phuzle.labs.messages.data.db.entity.BlockedNumberEntity
import com.phuzle.labs.messages.data.db.entity.MessageEntity
import com.phuzle.labs.messages.data.db.entity.ReminderEntity
import com.phuzle.labs.messages.data.db.entity.ThreadEntity
import com.phuzle.labs.messages.data.db.entity.TransactionEntity
import com.phuzle.labs.messages.data.prefs.AppSettings
import com.phuzle.labs.messages.domain.model.Category
import com.phuzle.labs.messages.domain.model.NotificationChannelIds
import com.phuzle.labs.messages.domain.model.initialsFor
import com.phuzle.labs.messages.domain.search.FuzzyMatcher
import com.phuzle.labs.messages.ui.format.currentLocalEpochDay
import com.phuzle.labs.messages.ui.format.formatCentsSigned
import com.phuzle.labs.messages.ui.format.formatDueRelative
import com.phuzle.labs.messages.ui.format.formatMessageTime
import com.phuzle.labs.messages.ui.format.formatScheduleTime
import com.phuzle.labs.messages.ui.format.formatThreadListTime
import com.phuzle.labs.messages.ui.format.formatTransactionTime
import com.phuzle.labs.messages.ui.model.AccountUi
import com.phuzle.labs.messages.ui.model.ActionSheetUi
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.model.BlockedNumberUi
import com.phuzle.labs.messages.ui.model.CategoryChipUi
import com.phuzle.labs.messages.ui.model.ContactSuggestionUi
import com.phuzle.labs.messages.ui.model.CurrentThreadUi
import com.phuzle.labs.messages.ui.model.DashboardTab
import com.phuzle.labs.messages.ui.model.DeletedThreadUi
import com.phuzle.labs.messages.ui.model.DraftUi
import com.phuzle.labs.messages.ui.model.MessageActionTargetUi
import com.phuzle.labs.messages.ui.model.MessageUi
import com.phuzle.labs.messages.ui.model.OtpModalUi
import com.phuzle.labs.messages.ui.model.PushedScreen
import com.phuzle.labs.messages.ui.model.ReminderUi
import com.phuzle.labs.messages.ui.model.ScheduledMessageActionTargetUi
import com.phuzle.labs.messages.ui.model.ScheduledMessageEditUi
import com.phuzle.labs.messages.ui.model.ScheduledMessageUi
import com.phuzle.labs.messages.ui.model.SettingsSub
import com.phuzle.labs.messages.ui.model.ThreadUi
import com.phuzle.labs.messages.ui.model.SimOptionUi
import com.phuzle.labs.messages.ui.model.TransactionUi
import com.phuzle.labs.messages.ui.model.UpdateInfoUi
import com.phuzle.labs.messages.ui.theme.ThemeMode
import com.phuzle.labs.messages.BuildConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch

private data class Ephemeral(
    val pushedScreen: PushedScreen? = null,
    val settingsSub: SettingsSub? = null,
    /** Set when RecycleBin/Archived/PrivateChats is opened from inside a settings sub-page (e.g.
     * Storage's "Recycle Bin" row) instead of the drawer, so [AppViewModel.goBack] can return to
     * that sub-page instead of falling all the way back to the dashboard. */
    val returnToSettingsSub: SettingsSub? = null,
    val activeTab: DashboardTab = DashboardTab.Messages,
    val activeThreadId: String? = null,
    val olderMessages: List<MessageEntity> = emptyList(),
    val hasMoreOlderMessages: Boolean = true,
    val isLoadingOlderMessages: Boolean = false,
    val searchQuery: String = "",
    val activeCategory: Category = Category.All,
    val unreadOnly: Boolean = false,
    val showDrawer: Boolean = false,
    val overflowMenuOpen: Boolean = false,
    /** Non-null while confirming the overflow menu's "Mark all as read" — the exact thread ids to
     * touch, captured at the moment the menu item was tapped rather than re-derived at confirm
     * time, so the dialog's own displayed count can never drift from what actually gets mutated.
     * Affects potentially many threads at once and writes through to the system SMS provider, with
     * no practical way to undo it afterward — a confirmation dialog, not an undo bar, matches how
     * every other multi-item destructive action here is handled (see RecycleBinScreen's "Empty
     * recycle bin?"). */
    val markAllReadConfirmThreadIds: List<String>? = null,
    val actionSheetThreadId: String? = null,
    val threadInput: String = "",
    /** A "send later" schedule chosen for the *next* reply in the currently open thread — the
     * in-thread mirror of composeCustomScheduleMillis, added because the reply bar previously had
     * no way to schedule a reply at all, only Compose did. Cleared once that reply is actually
     * sent/scheduled or the thread is left. */
    val threadCustomScheduleMillis: Long? = null,
    val composeTo: String = "",
    val composeBody: String = "",
    val composeRecipients: List<ContactSuggestionUi> = emptyList(),
    val composeCustomScheduleMillis: Long? = null,
    val composeDraftId: String? = null,
    /** Set when Compose was opened from the Drafts list, so closing it (see closeCompose) returns
     * there instead of falling back to the dashboard. */
    val composeOpenedFromDrafts: Boolean = false,
    val composeToSuggestions: List<ContactSuggestionUi> = emptyList(),
    /** Refreshed on init and whenever we (re)gain the default-SMS-app role — see refreshAvailableSims. */
    val availableSims: List<SimOptionUi> = emptyList(),
    val composeSelectedSubscriptionId: Int? = null,
    val privateChatsUnlockedThisSession: Boolean = false,
    /** Raw "has the biometric gate succeeded this session" flag — false until [AppViewModel.unlockApp]
     * fires. Whether the app is actually *shown* locked also depends on settings.appLockEnabled
     * (see the uiState combine below), which Ephemeral doesn't know about. */
    val appUnlockedThisSession: Boolean = false,
    val otpModal: OtpModalUi? = null,
    val isDefaultSmsApp: Boolean = true,
    val updateInfo: UpdateInfoUi? = null,
    val selectedAccountLast4: String? = null,
    val isImportingHistory: Boolean = false,
    val importDone: Int = 0,
    val importTotal: Int = 0,
    /** Brief "done!" beat shown (still under isImportingHistory) once the first-run history
     * import actually finishes, before StartupFlowScreen moves on to the next step — see
     * importHistoryOnce. Without this the progress screen just vanished the instant sync
     * finished, with nothing telling the user it actually succeeded. */
    val historySyncSuccess: Boolean = false,
    val threadOverflowMenuOpen: Boolean = false,
    val threadSearchActive: Boolean = false,
    val threadSearchQuery: String = "",
    /** First-launch-only prompt (see checkFirstLaunchDriveRestore) offering to restore/merge a
     * Google Drive backup found via a silent, no-UI sign-in. */
    val driveRestoreAvailable: Boolean = false,
    /** See AppUiState.driveSignInNeededForRestore — silent sign-in couldn't tell either way, so
     * this offers an explicit interactive sign-in (still with Skip) instead of just giving up. */
    val driveSignInNeededForRestore: Boolean = false,
    /** True for the whole span of the startup-triggered restore-and-merge (see confirmDriveRestore)
     * — keeps StartupFlowScreen showing a loader instead of revealing the dashboard mid-merge. */
    val driveRestoreInProgress: Boolean = false,
    /** True while checkFirstLaunchDriveRestore's sign-in + backup-listing round trip is in flight.
     * Purely so StartupFlowScreen can name that step honestly ("Checking Google Drive") instead of
     * falling through to its generic "Setting up Messages" gap-filler — this is a real network
     * call that can take seconds, not one of the sub-frame gaps that fallback exists for. */
    val driveCheckInProgress: Boolean = false,
    /** Set when a silent sign-in resolved a Google account with zero interaction but that account
     * had no Drive backup — see checkDriveBackupsAndOffer. Without this the user was never told a
     * check happened at all for this branch: the app would just silently accept whichever account
     * Play Services happened to cache and move on, with no chance to try a different one even
     * though they were never actually asked which account to check in the first place. */
    val driveNoBackupFoundEmail: String? = null,
    /** Fetched once when Thread Info opens (see openThreadInfo) — not worth a continuous reactive
     * flow just for a "first contact" date that never changes after the fact. */
    val threadInfoFirstContactAt: Long? = null,
    val messageActionTarget: MessageActionTargetUi? = null,
    /** See AppViewModel's fields of the same name — the restricted (Edit/Delete-only) action
     * sheet and edit dialog shared by the thread view's scheduled-message bubbles and the
     * Scheduled Messages hub. */
    val scheduledMessageActionTarget: ScheduledMessageActionTargetUi? = null,
    val scheduledMessageEdit: ScheduledMessageEditUi? = null,
    /** Non-empty means multi-select is active (started by long-pressing a chat's avatar — see
     * ThreadRow.onAvatarLongPress). Reaching empty via individual toggles exits select mode the
     * same way an explicit "close" would, matching most inbox apps' behavior. */
    val multiSelectThreadIds: Set<String> = emptySet(),
)

private data class ThreadsSnapshot(
    val inbox: List<ThreadEntity>,
    val allActive: List<ThreadEntity>,
    val archived: List<ThreadEntity>,
    val deleted: List<ThreadEntity>,
    val privateList: List<ThreadEntity>,
    val blocked: List<BlockedNumberEntity>,
    val unreadCounts: Map<String, Int>,
)

private data class PassbookSnapshot(
    val transactions: List<TransactionEntity>,
    val reminders: List<ReminderEntity>,
)

private const val MESSAGE_PAGE_SIZE = 40
private const val OLDER_MESSAGE_PAGE_SIZE = 30
/** Once loaded-older messages fall this far behind the visible/live area, drop them to bound memory. */
private const val TRIM_OLDER_MESSAGES_SLACK = 15
/** How long a destructive action or an outgoing send stays undoable before it's committed for real. */
private const val UNDO_WINDOW_MS = 6000L

private data class PendingUndo(val message: String, val undo: suspend () -> Unit)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(private val container: AppContainer) : ViewModel() {

    private val ephemeral = MutableStateFlow(Ephemeral())

    private val undoState = MutableStateFlow<PendingUndo?>(null)
    private var undoToken = 0

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    /** Undoing an archive/delete brings a thread back near the top of the list (by recency), but
     * LazyColumn's key-based scroll anchoring keeps whatever was already on screen pinned in
     * place — so a restore that lands above the current scroll position (most commonly: it was
     * the most-recent thread, i.e. the very first row) reappears scrolled out of view above the
     * top edge instead of visibly popping back in. DashboardScreen collects this to scroll back
     * to the top on every undo, which is always safe here since undo is single-thread-at-a-time
     * and restored threads are never far from the top of a recency-sorted list. */
    private val _scrollToTopEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollToTopEvents: SharedFlow<Unit> = _scrollToTopEvents.asSharedFlow()

    private fun toast(message: String) {
        _toastEvents.tryEmit(message)
    }

    /** Schedules [action] as reversible for [UNDO_WINDOW_MS]; a newer call supersedes any pending one. */
    private fun offerUndo(message: String, action: suspend () -> Unit) {
        val token = ++undoToken
        undoState.value = PendingUndo(message, action)
        viewModelScope.launch {
            delay(UNDO_WINDOW_MS)
            if (undoToken == token) undoState.value = null
        }
    }

    fun confirmUndo() = viewModelScope.launch {
        val pending = undoState.value ?: return@launch
        undoState.value = null
        pending.undo()
        _scrollToTopEvents.tryEmit(Unit)
    }

    fun dismissUndo() {
        undoState.value = null
    }

    private val threadsSnapshot: Flow<ThreadsSnapshot> = combine(
        container.threadRepository.observeInbox(),
        container.threadRepository.observeAllActive(),
        container.threadRepository.observeArchived(),
        container.threadRepository.observeDeleted(),
        container.threadRepository.observePrivate(),
    ) { inbox, allActive, archived, deleted, private ->
        listOf(inbox, allActive, archived, deleted, private)
    }.combine(container.threadRepository.observeBlockedNumbers()) { lists, blocked ->
        lists to blocked
    }.combine(container.threadRepository.observeUnreadCounts()) { (lists, blocked), counts ->
        ThreadsSnapshot(lists[0], lists[1], lists[2], lists[3], lists[4], blocked, counts.associate { it.threadId to it.count })
    }

    /** Best match quality found for a thread across all its candidate rows — [nameQuality] is
     * kept in its own band from [bodyQuality] so a contact-name match always outranks a
     * message-body match regardless of either's absolute score (see the sort in [uiState]):
     * searching a saved contact's name should surface that person's thread first, not whatever
     * promotional blast happens to mention the same word in its body and was merely received more
     * recently. [bestBodySnippet]/[bodyMatchIndices] remember *which* message actually produced
     * [bodyQuality] — a match can come from anywhere in the thread's history (see
     * observeSearchCandidates), not just its current last-message preview, so the row can show
     * that message instead of the preview; otherwise the match has nothing visible backing it. */
    private data class SearchRank(
        val nameQuality: Int,
        val bodyQuality: Int,
        val nameMatchIndices: Set<Int> = emptySet(),
        val bestBodySnippet: String? = null,
        val bodyMatchIndices: Set<Int> = emptySet(),
        /** Whether [bestBodySnippet] was sent by us — the swapped-in snippet needs its own "You:"
         * prefix decision, since it isn't necessarily the thread's actual last message anymore. */
        val bestBodyOutgoing: Boolean = false,
    )

    /** Real search — fuzzy-matches (see FuzzyMatcher) against a thread's sender name *or any
     * message in its full history*, not just the cached last-message preview. Null means "no
     * active search, don't filter". */
    private val searchRanking: Flow<Map<String, SearchRank>?> = ephemeral
        .map { it.searchQuery.trim() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                flowOf(null)
            } else {
                container.threadRepository.observeSearchCandidates().map { rows ->
                    val ranks = mutableMapOf<String, SearchRank>()
                    for (row in rows) {
                        val nameMatch = FuzzyMatcher.match(query, row.displayName)
                        val bodyMatch = row.body?.let { FuzzyMatcher.match(query, it) }
                        if (nameMatch == null && bodyMatch == null) continue
                        val existing = ranks[row.threadId] ?: SearchRank(0, 0)
                        val betterName = (nameMatch?.quality ?: 0) > existing.nameQuality
                        val betterBody = (bodyMatch?.quality ?: 0) > existing.bodyQuality
                        ranks[row.threadId] = SearchRank(
                            nameQuality = maxOf(existing.nameQuality, nameMatch?.quality ?: 0),
                            bodyQuality = maxOf(existing.bodyQuality, bodyMatch?.quality ?: 0),
                            nameMatchIndices = if (betterName) nameMatch!!.matchedIndices else existing.nameMatchIndices,
                            bestBodySnippet = if (betterBody) row.body else existing.bestBodySnippet,
                            bodyMatchIndices = if (betterBody) bodyMatch!!.matchedIndices else existing.bodyMatchIndices,
                            bestBodyOutgoing = if (betterBody) row.outgoing else existing.bestBodyOutgoing,
                        )
                    }
                    ranks
                }
            }
        }

    private val passbookSnapshot: Flow<PassbookSnapshot> = combine(
        container.passbookRepository.observeTransactions(),
        container.passbookRepository.observeReminders(),
    ) { transactions, reminders -> PassbookSnapshot(transactions, reminders) }

    /** The reactive "live window" — only the most recent [MESSAGE_PAGE_SIZE] rows, so an old
     * thread with thousands of messages doesn't get pulled into memory on open. */
    private val recentThreadMessages: Flow<List<MessageEntity>> = ephemeral
        .map { it.activeThreadId }
        .distinctUntilChanged()
        .flatMapLatest { id -> id?.let { container.threadRepository.observeRecentMessages(it, MESSAGE_PAGE_SIZE) } ?: flowOf(emptyList()) }

    /** Older pages loaded on-demand (see [loadOlderMessages]) are prepended in front of the live window. */
    private val activeThreadMessages: Flow<List<MessageEntity>> = combine(recentThreadMessages, ephemeral) { recent, eph ->
        (eph.olderMessages + recent.sortedBy { it.timestamp }).distinctBy { it.id }
    }

    private val baseUiState: Flow<AppUiState> = combine(
        threadsSnapshot,
        passbookSnapshot,
        activeThreadMessages,
        container.settingsRepository.settingsFlow,
        ephemeral,
    ) { threads, passbook, messages, settings, eph -> buildUiState(threads, passbook, messages, settings, eph) }

    val uiState: StateFlow<AppUiState> = baseUiState
        .combine(container.draftRepository.observeAll()) { state, drafts ->
            state.copy(
                drafts = drafts.map {
                    DraftUi(
                        id = it.id,
                        // Drafts only ever store raw numbers (see saveDraftIfNeeded) — show the
                        // saved contact's name here too, not just on reopening the draft itself.
                        to = withContext(Dispatchers.IO) { resolveDraftRecipientsLabel(it.to) },
                        bodyPreview = it.body.take(60),
                        timeLabel = formatThreadListTime(it.updatedAt),
                    )
                },
            )
        }
        .combine(undoState) { state, undo -> state.copy(undoMessage = undo?.message) }
        .combine(searchRanking) { state, ranks ->
            if (ranks == null) {
                state
            } else {
                // Name matches sort as a whole tier above body-only matches (a contact-name hit
                // always wins, whatever its fuzzy score, over a promo blast that merely mentions
                // the same word), then within a tier by match quality, then stable on the
                // existing recency order for ties.
                val ordered = state.threads
                    .filter { ranks.containsKey(it.id) }
                    .sortedWith(compareByDescending<ThreadUi> { ranks.getValue(it.id).nameQuality }
                        .thenByDescending { ranks.getValue(it.id).bodyQuality })
                    .map { t ->
                        val rank = ranks.getValue(t.id)
                        // A body match can come from any message in the thread's history, not
                        // just its current last-message preview (see observeSearchCandidates) —
                        // show the message that actually matched instead of the preview, or the
                        // row displays unrelated text with no visible reason it matched at all.
                        t.copy(
                            displayNameMatch = rank.nameMatchIndices,
                            preview = rank.bestBodySnippet ?: t.preview,
                            previewMatch = rank.bodyMatchIndices,
                            // Only swap in the matched message's own "You:" state when the
                            // preview itself was swapped — otherwise this would incorrectly
                            // override a name-only match's untouched (and already correct)
                            // last-message preview with whatever bestBodyOutgoing defaults to.
                            previewIsOutgoing = if (rank.bestBodySnippet != null) rank.bestBodyOutgoing else t.previewIsOutgoing,
                        )
                    }
                state.copy(threads = ordered)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init {
        observeScheduledMessages()
        viewModelScope.launch {
            val update = container.updateChecker.checkForUpdate(BuildConfig.VERSION_CODE.toLong())
            if (update != null) {
                ephemeral.update { it.copy(updateInfo = UpdateInfoUi(update.message)) }
            }
        }
        // Restores whichever category chip (Personal, Transactions, ...) was active when the app
        // was last closed — picking a filter and reopening the app used to always land back on
        // All, silently discarding it.
        viewModelScope.launch {
            val stored = container.settingsRepository.settingsFlow.first().lastActiveCategory
            val restored = Category.entries.firstOrNull { it.name == stored } ?: Category.All
            if (restored != Category.All) ephemeral.update { it.copy(activeCategory = restored) }
        }
        refreshAvailableSims()

        // A message arriving *while its thread is already open* used to still leave the thread
        // marked unread on the dashboard once the user backed out — openThreadById only marks
        // read at the moment of opening, and recordIncomingMessage unconditionally sets
        // unread=true on every new message with no notion of "the user is already looking at
        // this". Whenever the currently-open thread flips back to unread — genuinely being read
        // in real time, not just opened once — immediately re-mark it read and clear its
        // notification, the same as first opening it does.
        viewModelScope.launch {
            ephemeral.map { it.activeThreadId }.distinctUntilChanged()
                .flatMapLatest { id ->
                    if (id == null) flowOf(null) else threadsSnapshot.map { snapshot -> snapshot.allActive.find { it.id == id } }
                }
                .collect { thread ->
                    if (thread != null && thread.unread) {
                        container.threadRepository.toggleRead(thread.id, true)
                        container.messageNotifier.cancelForThread(thread.id)
                    }
                }
        }
    }

    /** Re-reads the device's active SIMs — a no-op (empty list) on single-SIM devices or without
     * READ_PHONE_STATE. Called at startup and again once we (re)gain the default-SMS-app role,
     * since that permission is requested in the same batch as contacts/notifications right after. */
    private fun refreshAvailableSims() = viewModelScope.launch(Dispatchers.IO) {
        val sims = container.activeSims().map { SimOptionUi(it.subscriptionId, it.label) }
        ephemeral.update { it.copy(availableSims = sims) }
    }

    fun selectComposeSim(subscriptionId: Int) = ephemeral.update { it.copy(composeSelectedSubscriptionId = subscriptionId) }

    fun dismissUpdate() = ephemeral.update { it.copy(updateInfo = null) }

    // region ---- derived state ----

    private fun buildUiState(
        threads: ThreadsSnapshot,
        passbook: PassbookSnapshot,
        messages: List<MessageEntity>,
        settings: AppSettings,
        eph: Ephemeral,
    ): AppUiState {
        val categories = Category.entries.map { CategoryChipUi(it, it.label, it == eph.activeCategory) }

        // Real text search (against full message history, not just the cached preview) is applied
        // as a separate layer over this state — see searchMatchingIds — so only category/unread
        // filtering happens here.
        val filteredThreads = threads.inbox.filter { t ->
            (eph.activeCategory == Category.All || t.category == eph.activeCategory.name) &&
                (!eph.unreadOnly || t.unread)
        }.map { it.toThreadUi(threads.unreadCounts[it.id] ?: 0) }

        val hasUnread = threads.inbox.any { it.unread }

        // "Accounts" aren't separately stored — grouping the real transaction feed by last-4 is
        // the whole Layer-1-only story here (see PassbookRepository's doc comment).
        val accounts = passbook.transactions
            .filter { it.accountLast4.isNotBlank() }
            .groupBy { it.accountLast4 }
            .map { (last4, txs) ->
                val net = txs.sumOf { it.amountCents }
                AccountUi(
                    last4 = last4,
                    transactionCount = txs.size,
                    netLabel = formatCentsSigned(net),
                    netIsCredit = net >= 0,
                    selected = last4 == eph.selectedAccountLast4,
                )
            }
            .sortedByDescending { it.transactionCount }
        val transactions = passbook.transactions
            .filter { eph.selectedAccountLast4 == null || it.accountLast4 == eph.selectedAccountLast4 }
            .map {
                TransactionUi(it.id, it.merchant, "•• ${it.accountLast4}", formatTransactionTime(it.time), formatCentsSigned(it.amountCents), it.isCredit)
            }
        val reminders = passbook.reminders.map { ReminderUi(it.id, it.title, it.detail, formatDueRelative(it.dueAt)) }

        val activeThread = threads.allActive.firstOrNull { it.id == eph.activeThreadId }
        val currentThread = activeThread?.let { thread ->
            val category = Category.fromStoredName(thread.category)
            CurrentThreadUi(
                id = thread.id,
                sender = thread.sender,
                displayName = thread.displayName,
                category = category,
                isBusiness = thread.isBusiness,
                avatarColor = androidx.compose.ui.graphics.Color(thread.avatarColor),
                photoUri = thread.photoUri,
                initials = initialsFor(thread.displayName),
                kindLabel = if (thread.isBusiness) "Business sender" else "Personal contact",
                channelName = channelNameFor(category),
                infoTitle = if (thread.isBusiness) "Sender info" else "Contact info",
                isReplyable = category.isReplyable,
                isBlocked = threads.blocked.any { it.number == thread.sender },
                firstContactLabel = eph.threadInfoFirstContactAt?.let { formatThreadListTime(it) },
            )
        }
        val currentThreadMessages = messages.map {
            MessageUi(
                id = it.id,
                text = it.body,
                timeLabel = if (it.scheduledFor != null && !it.sent) "Scheduled · ${it.scheduleLabel}" else formatMessageTime(it.timestamp),
                timestamp = it.timestamp,
                isMine = it.outgoing,
                isScheduled = it.scheduledFor != null && !it.sent,
                detectedEntities = com.phuzle.labs.messages.domain.text.MessageEntityDetector.detect(
                    it.body, container.regexRules.otpKeywords, container.regexRules.otpCodePattern,
                ),
            )
        }

        fun toDeleted(t: ThreadEntity) = DeletedThreadUi(
            t.id, t.displayName, initialsFor(t.displayName), androidx.compose.ui.graphics.Color(t.avatarColor), t.isBusiness,
            Category.fromStoredName(t.category), t.lastMessagePreview,
        )

        val actionSheet = eph.actionSheetThreadId?.let { id ->
            threads.allActive.firstOrNull { it.id == id }?.let { t ->
                ActionSheetUi(
                    threadId = t.id,
                    sender = t.displayName,
                    markReadLabel = if (t.unread) "Mark as read" else "Mark as unread",
                    privateLabel = if (t.isPrivate) "Remove from Private" else "Move to Private",
                )
            }
        }

        return AppUiState(
            settings = settings,
            themeMode = ThemeMode.fromKey(settings.themeMode),
            pushedScreen = eph.pushedScreen,
            settingsSub = eph.settingsSub,
            activeTab = eph.activeTab,
            searchQuery = eph.searchQuery,
            activeCategory = eph.activeCategory,
            unreadOnly = eph.unreadOnly,
            categories = categories,
            threads = filteredThreads,
            hasUnread = hasUnread,
            accounts = accounts,
            transactions = transactions,
            reminders = reminders,
            selectedAccountLast4 = eph.selectedAccountLast4,
            currentThread = currentThread,
            currentThreadMessages = currentThreadMessages,
            isLoadingOlderMessages = eph.isLoadingOlderMessages,
            hasMoreOlderMessages = eph.hasMoreOlderMessages,
            threadInput = eph.threadInput,
            threadCustomScheduleMillis = eph.threadCustomScheduleMillis,
            composeTo = eph.composeTo,
            composeBody = eph.composeBody,
            composeRecipients = eph.composeRecipients,
            composeCustomScheduleMillis = eph.composeCustomScheduleMillis,
            composeToSuggestions = eph.composeToSuggestions,
            availableSims = eph.availableSims,
            composeSelectedSubscriptionId = eph.composeSelectedSubscriptionId,
            deletedThreads = threads.deleted.map(::toDeleted),
            archivedThreads = threads.archived.map(::toDeleted),
            privateThreads = threads.privateList.map(::toDeleted),
            privateChatsUnlockedThisSession = eph.privateChatsUnlockedThisSession,
            appUnlockedThisSession = !settings.appLockEnabled || eph.appUnlockedThisSession,
            multiSelectThreadIds = eph.multiSelectThreadIds,
            blockedList = threads.blocked.map { BlockedNumberUi(it.number) },
            showDrawer = eph.showDrawer,
            overflowMenuOpen = eph.overflowMenuOpen,
            markAllReadConfirmThreadIds = eph.markAllReadConfirmThreadIds,
            actionSheet = actionSheet,
            otpModal = eph.otpModal,
            isDefaultSmsApp = eph.isDefaultSmsApp,
            updateInfo = eph.updateInfo,
            threadOverflowMenuOpen = eph.threadOverflowMenuOpen,
            threadSearchActive = eph.threadSearchActive,
            threadSearchQuery = eph.threadSearchQuery,
            driveRestoreAvailable = eph.driveRestoreAvailable,
            driveSignInNeededForRestore = eph.driveSignInNeededForRestore,
            driveRestoreInProgress = eph.driveRestoreInProgress,
            driveCheckInProgress = eph.driveCheckInProgress,
            driveNoBackupFoundEmail = eph.driveNoBackupFoundEmail,
            messageActionTarget = eph.messageActionTarget,
            scheduledMessageActionTarget = eph.scheduledMessageActionTarget,
            scheduledMessageEdit = eph.scheduledMessageEdit,
            isImportingHistory = eph.isImportingHistory,
            importDone = eph.importDone,
            importTotal = eph.importTotal,
            historySyncSuccess = eph.historySyncSuccess,
        )
    }

    /** Draft "to" is stored as raw comma-joined numbers (see saveDraftIfNeeded) — show whatever
     * name is on file for each number, falling back to the number itself, same as everywhere else
     * a bare number gets a contact-name upgrade. */
    private fun resolveDraftRecipientsLabel(to: String): String {
        val numbers = to.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (numbers.isEmpty()) return "No recipient"
        return numbers.joinToString(", ") { container.contactLookup.displayNameFor(it) ?: it }
    }

    private fun ThreadEntity.toThreadUi(realUnreadCount: Int): com.phuzle.labs.messages.ui.model.ThreadUi = com.phuzle.labs.messages.ui.model.ThreadUi(
        id = id,
        sender = sender,
        displayName = displayName,
        category = Category.fromStoredName(category),
        isBusiness = isBusiness,
        avatarColor = androidx.compose.ui.graphics.Color(avatarColor),
        photoUri = photoUri,
        initials = initialsFor(displayName),
        preview = lastMessagePreview,
        previewIsOutgoing = lastMessageOutgoing,
        timeLabel = formatThreadListTime(lastMessageTime),
        unread = unread,
        unreadCount = if (realUnreadCount > 0) realUnreadCount else if (unread) 1 else 0,
        nameWeight = if (unread) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
    )

    private fun channelNameFor(category: Category): String = when (category) {
        Category.Personal -> "Direct Messages"
        Category.Otp -> "Authentication"
        Category.Transactions -> "Transactions"
        else -> "Promotional"
    }

    // endregion

    // region ---- navigation ----

    fun toggleDrawer() = ephemeral.update { it.copy(showDrawer = !it.showDrawer) }
    fun closeDrawer() = ephemeral.update { it.copy(showDrawer = false) }
    fun toggleOverflowMenu() = ephemeral.update { it.copy(overflowMenuOpen = !it.overflowMenuOpen) }
    fun closeOverflowMenu() = ephemeral.update { it.copy(overflowMenuOpen = false) }

    fun openMessagesTab() = ephemeral.update { it.copy(activeTab = DashboardTab.Messages, pushedScreen = null, showDrawer = false) }
    fun openPassbookTab() = ephemeral.update { it.copy(activeTab = DashboardTab.Passbook, pushedScreen = null, showDrawer = false) }
    fun openRemindersTab() = ephemeral.update { it.copy(activeTab = DashboardTab.Reminders, pushedScreen = null, showDrawer = false) }

    fun dismissReminder(id: String) = viewModelScope.launch {
        val reminder = container.passbookRepository.findReminder(id) ?: return@launch
        container.passbookRepository.deleteReminder(id)
        offerUndo("Reminder dismissed") { container.passbookRepository.restoreReminder(reminder) }
    }

    /** Opens the account's own detail page (recent activity lives there now, not inline on the Passbook tab). */
    fun openAccountDetail(last4: String) = ephemeral.update {
        it.copy(selectedAccountLast4 = last4, pushedScreen = PushedScreen.AccountDetail)
    }

    fun openSettings() = ephemeral.update { it.copy(pushedScreen = PushedScreen.Settings, settingsSub = null, overflowMenuOpen = false, showDrawer = false) }
    fun openAbout() = ephemeral.update { it.copy(pushedScreen = PushedScreen.Settings, settingsSub = SettingsSub.About, overflowMenuOpen = false, showDrawer = false) }
    fun openSettingsSub(sub: SettingsSub) = ephemeral.update { it.copy(settingsSub = sub) }
    fun openCompose() = ephemeral.update {
        it.copy(
            pushedScreen = PushedScreen.Compose, composeTo = "", composeBody = "", composeRecipients = emptyList(),
            composeCustomScheduleMillis = null, composeDraftId = null, composeToSuggestions = emptyList(),
            composeOpenedFromDrafts = false,
        )
    }

    /** Closing Compose (X icon or system back, see [goBack]) saves an unsent, non-empty draft, and
     * returns to the Drafts list if that's where Compose was opened from instead of the dashboard. */
    fun closeCompose() = viewModelScope.launch {
        val openedFromDrafts = ephemeral.value.composeOpenedFromDrafts
        saveDraftIfNeeded()
        ephemeral.update {
            it.copy(
                pushedScreen = if (openedFromDrafts) PushedScreen.Drafts else null,
                composeToSuggestions = emptyList(),
                composeOpenedFromDrafts = false,
            )
        }
    }

    private suspend fun saveDraftIfNeeded() {
        val eph = ephemeral.value
        val body = eph.composeBody.trim()
        if (body.isEmpty()) {
            eph.composeDraftId?.let { container.draftRepository.delete(it) }
            return
        }
        // Recipients live as chips (composeRecipients), not the "To" text buffer — persist both
        // so a draft with an already-added recipient doesn't come back showing "No recipient".
        val typed = eph.composeTo.trim()
        val to = (eph.composeRecipients.map { it.number } + listOfNotNull(typed.takeIf { it.isNotEmpty() })).joinToString(",")
        container.draftRepository.save(eph.composeDraftId, to, body, eph.composeCustomScheduleMillis)
        toast("Saved to drafts")
    }

    fun openDraftsScreen() = ephemeral.update { it.copy(pushedScreen = PushedScreen.Drafts, showDrawer = false, overflowMenuOpen = false) }

    fun openDraft(id: String) = viewModelScope.launch {
        val draft = container.draftRepository.findById(id) ?: return@launch
        val numbers = draft.to.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        // Drafts only ever stored the raw number (see saveDraftIfNeeded) — resolve the contact
        // name fresh each time instead of showing the number as its own "name", which used to
        // make a draft to a saved contact silently lose that contact's name on reopen.
        val recipients = withContext(Dispatchers.IO) {
            numbers.map { number ->
                ContactSuggestionUi(container.contactLookup.displayNameFor(number) ?: number, number, container.contactLookup.photoUriFor(number))
            }
        }
        ephemeral.update {
            it.copy(
                pushedScreen = PushedScreen.Compose,
                composeTo = "",
                composeBody = draft.body,
                composeRecipients = recipients,
                composeDraftId = draft.id,
                composeCustomScheduleMillis = draft.scheduledFor,
                composeToSuggestions = emptyList(),
                composeOpenedFromDrafts = true,
            )
        }
    }

    fun deleteDraft(id: String) = viewModelScope.launch {
        val draft = container.draftRepository.findById(id) ?: return@launch
        container.draftRepository.delete(id)
        offerUndo("Draft deleted") { container.draftRepository.save(draft.id, draft.to, draft.body, draft.scheduledFor) }
    }

    /** Recycle Bin/Archived/Private Chats can be reached either from the drawer (top level) or
     * from a row inside a settings sub-page (Storage's "Recycle Bin"/"Archived", Privacy's
     * "Private Chats"); in the latter case, remember the sub-page so [goBack] returns there
     * instead of falling all the way back to the dashboard. */
    private fun settingsAwareNav(target: PushedScreen) = ephemeral.update {
        it.copy(
            pushedScreen = target,
            returnToSettingsSub = if (it.pushedScreen == PushedScreen.Settings) it.settingsSub else null,
            showDrawer = false,
            overflowMenuOpen = false,
        )
    }

    fun openRecycleBin() = settingsAwareNav(PushedScreen.RecycleBin)
    fun openBackupList() {
        settingsAwareNav(PushedScreen.BackupList)
        loadBackupLists()
    }
    fun openArchivedScreen() = settingsAwareNav(PushedScreen.Archived)
    fun openThreadInfo() {
        ephemeral.update { it.copy(pushedScreen = PushedScreen.ThreadInfo, threadInfoFirstContactAt = null) }
        viewModelScope.launch {
            val id = ephemeral.value.activeThreadId ?: return@launch
            val first = container.threadRepository.firstMessageTime(id)
            ephemeral.update { it.copy(threadInfoFirstContactAt = first) }
        }
    }

    /** Tapping a chat's avatar in the Messages list — jumps straight to its profile/info page
     * without opening the conversation first (unlike [openThreadById], this does not mark the
     * thread read — viewing someone's profile card isn't "reading" their messages). */
    fun openThreadInfoById(id: String) {
        ephemeral.update { it.copy(activeThreadId = id, pushedScreen = PushedScreen.ThreadInfo, threadInfoFirstContactAt = null) }
        viewModelScope.launch {
            val first = container.threadRepository.firstMessageTime(id)
            ephemeral.update { it.copy(threadInfoFirstContactAt = first) }
        }
    }

    // region ---- Multi-select (started by long-pressing a chat's avatar) ----

    fun startMultiSelect(threadId: String) = ephemeral.update { it.copy(multiSelectThreadIds = setOf(threadId)) }

    fun toggleThreadSelection(threadId: String) = ephemeral.update {
        val current = it.multiSelectThreadIds
        it.copy(multiSelectThreadIds = if (threadId in current) current - threadId else current + threadId)
    }

    fun exitMultiSelect() = ephemeral.update { it.copy(multiSelectThreadIds = emptySet()) }

    /** Multiple destructive actions ask for confirmation instead of offering undo (the dialog
     * lives in DashboardScreen) — unlike a single archive/delete, which offers undo. */
    fun bulkArchiveSelected() = viewModelScope.launch {
        val ids = ephemeral.value.multiSelectThreadIds
        ids.forEach { container.threadRepository.archive(it) }
        exitMultiSelect()
        toast("${ids.size} ${if (ids.size == 1) "chat" else "chats"} archived")
    }

    fun bulkDeleteSelected() = viewModelScope.launch {
        val ids = ephemeral.value.multiSelectThreadIds
        val now = System.currentTimeMillis()
        ids.forEach { container.threadRepository.softDelete(it, now) }
        exitMultiSelect()
        toast("${ids.size} ${if (ids.size == 1) "chat" else "chats"} deleted")
    }

    fun bulkMarkReadSelected() = viewModelScope.launch {
        val ids = ephemeral.value.multiSelectThreadIds
        ids.forEach { container.threadRepository.toggleRead(it, true) }
        exitMultiSelect()
    }

    // endregion
    fun openPrivateChatsScreen() {
        settingsAwareNav(PushedScreen.PrivateChats)
        ephemeral.update { it.copy(privateChatsUnlockedThisSession = false) }
    }
    fun unlockPrivateChats() = ephemeral.update { it.copy(privateChatsUnlockedThisSession = true) }

    /** Whole-app equivalent of [unlockPrivateChats] — see BiometricGate in AppRoot. */
    fun unlockApp() = ephemeral.update { it.copy(appUnlockedThisSession = true) }

    fun goBack() {
        val eph = ephemeral.value
        when {
            eph.pushedScreen == PushedScreen.Compose -> closeCompose()
            eph.pushedScreen == PushedScreen.ThreadInfo -> ephemeral.update { it.copy(pushedScreen = PushedScreen.Thread) }
            eph.pushedScreen == PushedScreen.Settings && eph.settingsSub != null -> ephemeral.update { it.copy(settingsSub = null) }
            eph.returnToSettingsSub != null -> ephemeral.update {
                it.copy(pushedScreen = PushedScreen.Settings, settingsSub = it.returnToSettingsSub, returnToSettingsSub = null)
            }
            else -> ephemeral.update { it.copy(pushedScreen = null, activeThreadId = null, settingsSub = null) }
        }
    }

    fun setCategory(category: Category) {
        ephemeral.update { it.copy(activeCategory = category) }
        viewModelScope.launch { container.settingsRepository.setLastActiveCategory(category.name) }
    }

    /** Hides the closed-testing feedback banner for the rest of today (see
     * currentLocalEpochDay) — it comes back on its own tomorrow rather than being gone for good
     * after one tap, since this build being in closed testing doesn't stop being true. */
    fun dismissFeedbackBanner() = viewModelScope.launch {
        container.settingsRepository.setFeedbackBannerDismissedDay(currentLocalEpochDay())
    }
    fun onSearchChange(query: String) = ephemeral.update { it.copy(searchQuery = query) }

    // endregion

    // region ---- threads ----

    fun openThreadById(id: String) {
        ephemeral.update {
            it.copy(
                pushedScreen = PushedScreen.Thread, activeThreadId = id, threadInput = "",
                olderMessages = emptyList(), hasMoreOlderMessages = true, isLoadingOlderMessages = false,
            )
        }
        // Opening the conversation is itself "reading" it — previously only the explicit
        // action-sheet "Mark as read" toggled this, so the unread dot stuck around after simply
        // viewing the thread. Its notification (if any) is stale the moment that happens too —
        // autoCancel only clears it on an actual tap, not on being read some other way.
        viewModelScope.launch {
            val thread = container.threadRepository.getThread(id) ?: return@launch
            if (thread.unread) container.threadRepository.toggleRead(id, true)
            container.messageNotifier.cancelForThread(id)
        }
    }

    /** Loads one more page of history above the live window; called when the list scrolls near the top. */
    fun loadOlderMessages() = viewModelScope.launch {
        val eph = ephemeral.value
        val threadId = eph.activeThreadId ?: return@launch
        if (eph.isLoadingOlderMessages || !eph.hasMoreOlderMessages) return@launch

        ephemeral.update { it.copy(isLoadingOlderMessages = true) }
        val oldestLoadedTimestamp = activeThreadMessages.first().firstOrNull()?.timestamp
        if (oldestLoadedTimestamp == null) {
            ephemeral.update { it.copy(isLoadingOlderMessages = false) }
            return@launch
        }
        val page = container.threadRepository.olderMessagesThan(threadId, oldestLoadedTimestamp, OLDER_MESSAGE_PAGE_SIZE)
        ephemeral.update {
            it.copy(
                olderMessages = (page.sortedBy { m -> m.timestamp } + it.olderMessages).distinctBy { m -> m.id },
                hasMoreOlderMessages = page.size == OLDER_MESSAGE_PAGE_SIZE,
                isLoadingOlderMessages = false,
            )
        }
    }

    /** Releases loaded-older pages once the user has scrolled well back into the live window. */
    fun trimOlderMessages() = ephemeral.update {
        if (it.olderMessages.isEmpty()) it else it.copy(olderMessages = emptyList(), hasMoreOlderMessages = true)
    }

    /** Overflow menu's "Mark all as read" — captures which threads are affected (whatever the
     * current category/unread-only filter shows, see uiState.value.threads) and asks for
     * confirmation before touching anything. Used to call ThreadRepository.markAllRead()
     * unconditionally, which silently marked *every* thread in the whole inbox read regardless of
     * which category tab was open — e.g. tapping this while filtered to OTP marked Personal,
     * Transactions, and Promotions read too, with no way to tell it had happened until those
     * threads' unread state was already gone. */
    fun requestMarkAllAsRead() {
        val threadIds = uiState.value.threads.map { it.id }
        ephemeral.update {
            it.copy(overflowMenuOpen = false, markAllReadConfirmThreadIds = threadIds.ifEmpty { null })
        }
    }

    fun confirmMarkAllAsRead() = viewModelScope.launch {
        val threadIds = ephemeral.value.markAllReadConfirmThreadIds ?: return@launch
        ephemeral.update { it.copy(markAllReadConfirmThreadIds = null) }
        container.threadRepository.markThreadsRead(threadIds)
    }

    fun dismissMarkAllAsReadConfirm() = ephemeral.update { it.copy(markAllReadConfirmThreadIds = null) }

    fun toggleUnreadOnly() = ephemeral.update { it.copy(unreadOnly = !it.unreadOnly, overflowMenuOpen = false) }

    fun onSwipeRight(threadId: String) = performThreadAction(uiState.value.settings.swipeRightAction, threadId)
    fun onSwipeLeft(threadId: String) = performThreadAction(uiState.value.settings.swipeLeftAction, threadId)

    private fun performThreadAction(action: String, threadId: String) = viewModelScope.launch {
        when (action) {
            "archive" -> {
                container.threadRepository.archive(threadId)
                offerUndo("Archived") { container.threadRepository.unarchive(threadId) }
            }
            "delete" -> {
                container.threadRepository.softDelete(threadId, System.currentTimeMillis())
                offerUndo("Chat deleted") { container.threadRepository.restore(threadId) }
            }
            "toggleRead" -> {
                val thread = container.threadRepository.getThread(threadId) ?: return@launch
                container.threadRepository.toggleRead(threadId, thread.unread)
            }
        }
    }

    fun openActionSheet(threadId: String) = ephemeral.update { it.copy(actionSheetThreadId = threadId) }
    fun closeActionSheet() = ephemeral.update { it.copy(actionSheetThreadId = null) }

    fun sheetMarkRead() = viewModelScope.launch {
        val id = ephemeral.value.actionSheetThreadId ?: return@launch
        val thread = container.threadRepository.getThread(id) ?: return@launch
        container.threadRepository.toggleRead(id, thread.unread)
        closeActionSheet()
    }

    fun sheetArchive() = viewModelScope.launch {
        val id = ephemeral.value.actionSheetThreadId ?: return@launch
        container.threadRepository.archive(id)
        closeActionSheet()
        offerUndo("Archived") { container.threadRepository.unarchive(id) }
    }

    fun sheetTogglePrivate() = viewModelScope.launch {
        val id = ephemeral.value.actionSheetThreadId ?: return@launch
        val thread = container.threadRepository.getThread(id) ?: return@launch
        container.threadRepository.setPrivate(id, !thread.isPrivate)
        closeActionSheet()
    }

    fun sheetDelete() = viewModelScope.launch {
        val id = ephemeral.value.actionSheetThreadId ?: return@launch
        container.threadRepository.softDelete(id, System.currentTimeMillis())
        closeActionSheet()
        offerUndo("Chat deleted") { container.threadRepository.restore(id) }
    }

    fun restoreThread(id: String) = viewModelScope.launch {
        container.threadRepository.restore(id)
        offerUndo("Restored") { container.threadRepository.softDelete(id, System.currentTimeMillis()) }
    }

    fun unarchiveThread(id: String) = viewModelScope.launch {
        container.threadRepository.unarchive(id)
        offerUndo("Unarchived") { container.threadRepository.archive(id) }
    }

    fun unarchiveAll() = viewModelScope.launch {
        val ids = uiState.value.archivedThreads.map { it.id }
        if (ids.isEmpty()) return@launch
        ids.forEach { container.threadRepository.unarchive(it) }
        offerUndo("${ids.size} ${if (ids.size == 1) "chat" else "chats"} unarchived") { ids.forEach { container.threadRepository.archive(it) } }
    }

    fun restoreAllDeleted() = viewModelScope.launch {
        val ids = uiState.value.deletedThreads.map { it.id }
        if (ids.isEmpty()) return@launch
        val now = System.currentTimeMillis()
        ids.forEach { container.threadRepository.restore(it) }
        offerUndo("${ids.size} ${if (ids.size == 1) "chat" else "chats"} restored") { ids.forEach { container.threadRepository.softDelete(it, now) } }
    }

    /** Permanent — not reversible via undo, so callers should confirm with the user first. */
    fun emptyRecycleBin() = viewModelScope.launch {
        val ids = uiState.value.deletedThreads.map { it.id }
        if (ids.isEmpty()) return@launch
        ids.forEach { container.threadRepository.hardDelete(it) }
        toast("Recycle bin emptied")
    }

    fun deleteAllDrafts() = viewModelScope.launch {
        val drafts = container.draftRepository.observeAll().first()
        if (drafts.isEmpty()) return@launch
        drafts.forEach { container.draftRepository.delete(it.id) }
        offerUndo("${drafts.size} ${if (drafts.size == 1) "draft" else "drafts"} deleted") {
            drafts.forEach { container.draftRepository.save(it.id, it.to, it.body) }
        }
    }

    // endregion

    // region ---- compose & replying ----

    private var contactSearchJob: Job? = null

    fun onComposeToChange(value: String) {
        ephemeral.update { it.copy(composeTo = value) }
        contactSearchJob?.cancel()
        if (value.isBlank()) {
            ephemeral.update { it.copy(composeToSuggestions = emptyList()) }
            return
        }
        contactSearchJob = viewModelScope.launch {
            delay(150)
            val matches = withContext(Dispatchers.IO) {
                container.contactLookup.searchContacts(value).map { ContactSuggestionUi(it.name, it.number, it.photoUri) }
            }
            ephemeral.update { it.copy(composeToSuggestions = matches) }
        }
    }

    /** Closes the full-screen "Suggested contacts" picker without picking anyone or losing what
     * was typed — the picker otherwise only ever goes away by selecting a suggestion or clearing
     * the "To" field yourself (see ComposeScreen), which left no way back to the message body if
     * none of the suggestions were who you meant to text. Wired to the system back button. */
    fun dismissComposeToSuggestions() = ephemeral.update { it.copy(composeToSuggestions = emptyList()) }

    /** Adds a contact-search hit as a recipient chip; a number typed with no match can also be added this way. */
    fun selectComposeContact(contact: ContactSuggestionUi) = ephemeral.update {
        if (it.composeRecipients.any { r -> r.number == contact.number }) {
            it.copy(composeTo = "", composeToSuggestions = emptyList())
        } else {
            it.copy(composeRecipients = it.composeRecipients + contact, composeTo = "", composeToSuggestions = emptyList())
        }
    }

    /** Adds whatever's currently typed in "To" as a raw recipient — no contact match required,
     * since texting a number that isn't saved is completely ordinary. What it must not accept is
     * something that was never a phone number to begin with: [PhoneNumberUtils.isWellFormedSmsAddress]
     * is the same check Android's own Messages-style clients use to gate an SMS destination, and
     * rejects plain text like "dddd" that would otherwise sit in the recipient list looking
     * legitimate right up until SmsManager.sendTextMessage fails on it at actual send time. */
    fun addTypedComposeRecipient() {
        val number = ephemeral.value.composeTo.trim()
        if (number.isEmpty()) return
        if (!android.telephony.PhoneNumberUtils.isWellFormedSmsAddress(number)) {
            toast("Enter a valid phone number")
            return
        }
        selectComposeContact(ContactSuggestionUi(name = number, number = number))
    }

    fun removeComposeRecipient(number: String) = ephemeral.update {
        it.copy(composeRecipients = it.composeRecipients.filterNot { r -> r.number == number })
    }

    fun onComposeBodyChange(value: String) = ephemeral.update { it.copy(composeBody = value) }
    fun setComposeCustomSchedule(epochMillis: Long?) = ephemeral.update { it.copy(composeCustomScheduleMillis = epochMillis) }

    /** Every recipient gets its own individual SMS/thread — this is not group MMS. */
    fun sendCompose() = viewModelScope.launch {
        val eph = ephemeral.value
        var body = eph.composeBody.trim()
        if (body.isEmpty()) return@launch
        val typed = eph.composeTo.trim()
        val recipients = (eph.composeRecipients + if (typed.isNotEmpty()) listOf(ContactSuggestionUi(typed, typed)) else emptyList())
            .distinctBy { it.number }
        if (recipients.isEmpty()) return@launch

        val signature = uiState.value.settings.signature.trim()
        if (signature.isNotEmpty()) body = "$body\n$signature"

        val scheduledFor = eph.composeCustomScheduleMillis
        val scheduleLabel = scheduledFor?.let { formatScheduleTime(it) }
        val now = System.currentTimeMillis()
        // Explicit picker choice wins; otherwise fall back to the system's default SMS SIM (a
        // brand-new conversation has no prior message to infer a SIM from).
        val subscriptionId = eph.composeSelectedSubscriptionId ?: SubscriptionHelper.defaultSmsSubscriptionId()

        data class Sent(val threadId: String, val messageId: Long, val number: String)
        val sent = mutableListOf<Sent>()
        var lastThreadId: String? = null
        for (recipient in recipients) {
            val (thread, message) = container.threadRepository.composeOutgoingThread(
                to = recipient.number, body = body, scheduledFor = scheduledFor, scheduleLabel = scheduleLabel, nowMillis = now,
                displayName = recipient.name, photoUri = recipient.photoUri, subscriptionId = subscriptionId,
            )
            sent += Sent(thread.id, message.id, recipient.number)
            lastThreadId = thread.id
            if (scheduledFor != null) container.scheduledMessageAlarmScheduler.schedule(message.id, scheduledFor)
        }
        eph.composeDraftId?.let { container.draftRepository.delete(it) }

        val singleRecipient = recipients.size == 1
        ephemeral.update {
            it.copy(
                pushedScreen = if (singleRecipient) PushedScreen.Thread else null,
                activeThreadId = if (singleRecipient) lastThreadId else null,
                activeTab = DashboardTab.Messages,
                composeTo = "", composeBody = "", composeRecipients = emptyList(),
                composeCustomScheduleMillis = null, composeDraftId = null, composeToSuggestions = emptyList(),
                composeSelectedSubscriptionId = null,
                olderMessages = emptyList(), hasMoreOlderMessages = true, isLoadingOlderMessages = false,
            )
        }

        if (scheduledFor == null) {
            // Held back for UNDO_WINDOW_MS so "undo send" actually prevents the SMS from going out,
            // not just the local row — undo cancels this job before it ever calls SmsSender.
            val sendJob = viewModelScope.launch {
                delay(UNDO_WINDOW_MS)
                sent.forEach {
                    runCatching { container.smsSender.send(it.number, body, subscriptionId) }
                        .getOrNull()?.let { systemSmsId -> container.threadRepository.setSystemSmsId(it.messageId, systemSmsId) }
                }
            }
            offerUndo(if (sent.size == 1) "Message sent" else "${sent.size} messages sent") {
                sendJob.cancel()
                sent.forEach { container.threadRepository.deleteMessage(it.threadId, it.messageId) }
            }
        } else {
            toast("Scheduled for $scheduleLabel")
        }
    }

    fun onThreadInputChange(value: String) = ephemeral.update { it.copy(threadInput = value) }

    fun setThreadCustomSchedule(epochMillis: Long?) = ephemeral.update { it.copy(threadCustomScheduleMillis = epochMillis) }

    fun sendThreadMessage() = viewModelScope.launch {
        val eph = ephemeral.value
        val threadId = eph.activeThreadId ?: return@launch
        var text = eph.threadInput.trim()
        if (text.isEmpty()) return@launch
        val signature = uiState.value.settings.signature.trim()
        if (signature.isNotEmpty()) text = "$text\n$signature"

        val thread = container.threadRepository.getThread(threadId) ?: return@launch
        // Reply on whichever SIM this conversation has been happening on; only a thread with no
        // incoming message yet (composed by us first) has none, in which case the system default
        // SIM is exactly what "no preference" should mean.
        val subscriptionId = thread.preferredSubscriptionId ?: SubscriptionHelper.defaultSmsSubscriptionId()

        val scheduledFor = eph.threadCustomScheduleMillis
        if (scheduledFor != null) {
            // Same "queue it, don't send" path Compose uses (see sendCompose) — the reply bar
            // previously had no way to reach this at all.
            val scheduleLabel = formatScheduleTime(scheduledFor)
            val message = container.threadRepository.appendOutgoingMessage(
                threadId, text, scheduledFor, scheduleLabel, System.currentTimeMillis(), subscriptionId,
            )
            container.scheduledMessageAlarmScheduler.schedule(message.id, scheduledFor)
            ephemeral.update { it.copy(threadInput = "", threadCustomScheduleMillis = null) }
            toast("Scheduled for $scheduleLabel")
            return@launch
        }

        val message = container.threadRepository.appendOutgoingMessage(
            threadId, text, null, null, System.currentTimeMillis(), subscriptionId,
        )
        ephemeral.update { it.copy(threadInput = "") }

        val sendJob = viewModelScope.launch {
            delay(UNDO_WINDOW_MS)
            runCatching { container.smsSender.send(thread.sender, text, subscriptionId) }
                .getOrNull()?.let { systemSmsId -> container.threadRepository.setSystemSmsId(message.id, systemSmsId) }
        }
        offerUndo("Message sent") {
            sendJob.cancel()
            container.threadRepository.deleteMessage(threadId, message.id)
        }
    }

    // endregion

    // region ---- thread info / blocking ----

    fun toggleBlockCurrent() = viewModelScope.launch {
        val thread = uiState.value.currentThread ?: return@launch
        if (thread.isBlocked) {
            container.threadRepository.unblock(thread.sender)
            toast("Unblocked ${thread.displayName}")
        } else {
            container.threadRepository.block(thread.sender)
            toast("Blocked ${thread.displayName}")
        }
    }

    fun unblockNumber(number: String) = viewModelScope.launch {
        container.threadRepository.unblock(number)
        toast("Unblocked $number")
    }

    fun toggleThreadOverflowMenu() = ephemeral.update { it.copy(threadOverflowMenuOpen = !it.threadOverflowMenuOpen) }
    fun closeThreadOverflowMenu() = ephemeral.update { it.copy(threadOverflowMenuOpen = false) }

    fun openThreadSearch() = ephemeral.update { it.copy(threadSearchActive = true, threadOverflowMenuOpen = false) }
    fun closeThreadSearch() = ephemeral.update { it.copy(threadSearchActive = false, threadSearchQuery = "") }
    fun onThreadSearchChange(value: String) = ephemeral.update { it.copy(threadSearchQuery = value) }

    fun archiveCurrentThread() = viewModelScope.launch {
        val id = ephemeral.value.activeThreadId ?: return@launch
        container.threadRepository.archive(id)
        closeThreadOverflowMenu()
        goBack()
        offerUndo("Archived") { container.threadRepository.unarchive(id) }
    }

    fun deleteCurrentThread() = viewModelScope.launch {
        val id = ephemeral.value.activeThreadId ?: return@launch
        container.threadRepository.softDelete(id, System.currentTimeMillis())
        closeThreadOverflowMenu()
        goBack()
        offerUndo("Chat deleted") { container.threadRepository.restore(id) }
    }

    /** Contact info's "Clear conversation" — wipes every message in this thread but keeps the
     * thread (and its settings/category/block-state) around, unlike Delete which removes the
     * whole conversation to the recycle bin. */
    fun clearCurrentConversation() = viewModelScope.launch {
        val id = ephemeral.value.activeThreadId ?: return@launch
        val deleted = container.threadRepository.clearConversation(id)
        offerUndo("Conversation cleared") { container.threadRepository.restoreMessages(deleted) }
    }

    // endregion

    // region ---- per-message long-press actions ----

    fun openMessageActions(target: MessageActionTargetUi) = ephemeral.update { it.copy(messageActionTarget = target) }
    fun closeMessageActions() = ephemeral.update { it.copy(messageActionTarget = null) }

    fun deleteSelectedMessage() = viewModelScope.launch {
        val target = ephemeral.value.messageActionTarget ?: return@launch
        val threadId = ephemeral.value.activeThreadId ?: return@launch
        closeMessageActions()
        val deleted = container.threadRepository.deleteMessage(threadId, target.id)
        offerUndo("Message deleted") { deleted?.let { container.threadRepository.restoreMessage(it) } }
    }

    /** No threaded-quote UI exists in this design, so "reply" prefills the input with a quoted snippet. */
    fun replyQuotingSelectedMessage() {
        val target = ephemeral.value.messageActionTarget ?: return
        val quote = if (target.text.length > 80) "${target.text.take(80)}…" else target.text
        ephemeral.update { it.copy(threadInput = "> $quote\n", messageActionTarget = null) }
    }

    fun forwardSelectedMessage() {
        val target = ephemeral.value.messageActionTarget ?: return
        ephemeral.update {
            it.copy(
                pushedScreen = PushedScreen.Compose,
                composeRecipients = emptyList(),
                composeTo = "",
                composeBody = target.text,
                composeCustomScheduleMillis = null,
                composeDraftId = null,
                composeToSuggestions = emptyList(),
                composeOpenedFromDrafts = false,
                messageActionTarget = null,
            )
        }
    }

    fun copySelectedMessage() {
        val target = ephemeral.value.messageActionTarget ?: return
        container.copyToClipboard("Message", target.text)
        closeMessageActions()
        toast("Message copied")
    }

    fun copyNumber(number: String) {
        container.copyToClipboard("Number", number)
        toast("Number copied")
    }

    /** Backs the Copy chip under a detected phone/URL/email/code inside a message body (see
     * MessageEntityDetector) — Open/Call/Email chips instead launch an intent directly from the
     * composable, since that only needs a Context, not anything the ViewModel holds. */
    fun copyDetectedText(value: String) {
        container.copyToClipboard("Copied text", value)
        toast("Copied")
    }

    // endregion

    // region ---- scheduled messages (thread long-press + Scheduled Messages hub) ----

    /** Reactive backing for the Scheduled Messages hub — a plain collected StateFlow rather than
     * threading a new source into the big uiState combine (see AppViewModel.storageOverview for
     * the same lightweight pattern used for Settings > Storage). Rebuilt from scratch on every
     * emission: this list is realistically tiny (how many messages does anyone have queued up at
     * once?), so there's no real cost to simplicity here. */
    private val _scheduledMessages = MutableStateFlow<List<ScheduledMessageUi>>(emptyList())
    val scheduledMessages: StateFlow<List<ScheduledMessageUi>> = _scheduledMessages.asStateFlow()

    private fun observeScheduledMessages() = viewModelScope.launch {
        container.threadRepository.observePendingScheduledMessages().collect { pending ->
            _scheduledMessages.value = pending.mapNotNull { message ->
                val scheduledFor = message.scheduledFor ?: return@mapNotNull null
                val thread = container.threadRepository.getThread(message.threadId) ?: return@mapNotNull null
                ScheduledMessageUi(
                    id = message.id,
                    threadId = thread.id,
                    threadDisplayName = thread.displayName,
                    avatarColor = Color(thread.avatarColor),
                    category = Category.fromStoredName(thread.category),
                    isBusiness = thread.isBusiness,
                    photoUri = thread.photoUri,
                    body = message.body,
                    scheduledFor = scheduledFor,
                    scheduleLabel = message.scheduleLabel ?: formatScheduleTime(scheduledFor),
                )
            }
        }
    }

    fun openScheduledMessagesScreen() = ephemeral.update {
        it.copy(pushedScreen = PushedScreen.ScheduledMessages, showDrawer = false)
    }

    /** Long-press on a scheduled message bubble (thread view) or hub row — both routes land here,
     * one shared restricted (Edit/Delete-only) sheet regardless of which screen triggered it. */
    fun openScheduledMessageActions(id: Long, threadId: String) =
        ephemeral.update { it.copy(scheduledMessageActionTarget = ScheduledMessageActionTargetUi(id, threadId)) }

    fun closeScheduledMessageActions() = ephemeral.update { it.copy(scheduledMessageActionTarget = null) }

    fun beginEditScheduledMessage() = viewModelScope.launch {
        val target = ephemeral.value.scheduledMessageActionTarget ?: return@launch
        val message = container.threadRepository.getMessage(target.id) ?: return@launch
        val scheduledFor = message.scheduledFor ?: return@launch
        ephemeral.update {
            it.copy(
                scheduledMessageActionTarget = null,
                scheduledMessageEdit = ScheduledMessageEditUi(message.id, target.threadId, message.body, scheduledFor),
            )
        }
    }

    fun updateScheduledMessageEditBody(text: String) = ephemeral.update {
        it.copy(scheduledMessageEdit = it.scheduledMessageEdit?.copy(body = text))
    }

    fun updateScheduledMessageEditTime(epochMillis: Long) = ephemeral.update {
        it.copy(scheduledMessageEdit = it.scheduledMessageEdit?.copy(scheduledFor = epochMillis))
    }

    fun dismissScheduledMessageEdit() = ephemeral.update { it.copy(scheduledMessageEdit = null) }

    fun confirmScheduledMessageEdit() = viewModelScope.launch {
        val edit = ephemeral.value.scheduledMessageEdit ?: return@launch
        val body = edit.body.trim()
        if (body.isEmpty()) return@launch
        val scheduleLabel = formatScheduleTime(edit.scheduledFor)
        container.threadRepository.editScheduledMessage(edit.messageId, body, edit.scheduledFor, scheduleLabel)
        // Re-arms unconditionally rather than only when the time actually changed — cancel+
        // schedule on an id that was never armed (or already fired) is a harmless no-op, and this
        // avoids having to track "did the time change" as its own bit of state.
        container.scheduledMessageAlarmScheduler.cancel(edit.messageId)
        container.scheduledMessageAlarmScheduler.schedule(edit.messageId, edit.scheduledFor)
        ephemeral.update { it.copy(scheduledMessageEdit = null) }
        toast("Scheduled message updated")
    }

    /** Delete from either the thread view's restricted sheet or the hub — cancels the exact alarm
     * so a message the user just deleted can't still go out moments later, then offers the same
     * single-item undo every other message delete in this app gets (see deleteSelectedMessage). */
    fun deleteScheduledMessage() = viewModelScope.launch {
        val target = ephemeral.value.scheduledMessageActionTarget ?: return@launch
        closeScheduledMessageActions()
        container.scheduledMessageAlarmScheduler.cancel(target.id)
        val deleted = container.threadRepository.deleteMessage(target.threadId, target.id)
        offerUndo("Scheduled message deleted") {
            deleted?.let {
                container.threadRepository.restoreMessage(it)
                it.scheduledFor?.let { at -> container.scheduledMessageAlarmScheduler.schedule(it.id, at) }
            }
        }
    }

    // endregion

    // region ---- OTP hot-swap ----

    /** Called from Activity.onResume: shows the modal if an OTP arrived in the last 30 seconds.
     * The modal is only ever meant to live for the rest of that 30-second window from when the
     * OTP itself arrived (not 30 more seconds from whenever the user happens to open the app) —
     * this schedules the matching auto-dismiss so the overlay doesn't just sit there forever if
     * the user never taps Copy or Dismiss themselves. */
    fun checkOtpHotSwap() = viewModelScope.launch {
        val latest = container.threadRepository.latestIncomingOtpMessage() ?: return@launch
        val age = System.currentTimeMillis() - latest.timestamp
        if (age !in 0..30_000) return@launch
        val code = container.regexRules.extractCode(latest.body) ?: return@launch
        val thread = container.threadRepository.getThread(latest.threadId) ?: return@launch
        val expiresAt = latest.timestamp + 30_000
        ephemeral.update { it.copy(otpModal = OtpModalUi(thread.displayName, code, copied = false, expiresAtMillis = expiresAt)) }
        delay((expiresAt - System.currentTimeMillis()).coerceAtLeast(0))
        ephemeral.update { if (it.otpModal?.expiresAtMillis == expiresAt) it.copy(otpModal = null) else it }
    }

    fun closeOtpModal() = ephemeral.update { it.copy(otpModal = null) }

    fun copyOtpCode() = viewModelScope.launch {
        val modal = ephemeral.value.otpModal ?: return@launch
        container.copyToClipboard("OTP code", modal.code)
        ephemeral.update { it.copy(otpModal = it.otpModal?.copy(copied = true)) }
        delay(2000)
        ephemeral.update { it.copy(otpModal = null) }
    }

    /** [startWork] = false records the role state *without* starting the first-run history import,
     * SIM refresh, or provider reconciliation. MainActivity passes false in exactly two places —
     * the pre-composition seed in onCreate, and the moment the role is granted but the
     * contacts/notifications dialogs haven't been answered yet — so the import doesn't run against
     * an ungranted READ_CONTACTS (see MainActivity.requestRuntimePermissions for what that broke).
     * The UI still moves off the disclosure screen immediately either way, since it keys off
     * isDefaultSmsApp alone. */
    fun setDefaultSmsAppStatus(isDefault: Boolean, startWork: Boolean = true) {
        ephemeral.update { it.copy(isDefaultSmsApp = isDefault) }
        if (isDefault && startWork) {
            importHistoryOnce()
            refreshAvailableSims()
            reconcileWithSystemProvider()
        }
    }

    /** Read state and deletes only ever write through to the system SMS provider one way — this
     * app to the provider (see ThreadRepository.toggleRead/deleteMessage/etc). While some *other*
     * app held the default-SMS-app role, it could have changed that shared table on its own
     * (marked something read, deleted a message/conversation) with zero way for this app to know,
     * since it isn't running and holds no observer on it. This is the other half: called every
     * time we (re)gain the default role, it diffs every message we've previously linked to a
     * system-provider row (MessageEntity.systemSmsId) against a fresh snapshot of that table —
     * anything now missing was deleted elsewhere and gets removed here too; any inbound message
     * whose system READ flag no longer matches ours gets its local read state corrected to match.
     * Messages that never got a systemSmsId (a prior provider write failed) have nothing to
     * diff against and are skipped, same as if this pass never ran for them. */
    private fun reconcileWithSystemProvider() = viewModelScope.launch(Dispatchers.IO) {
        val linked = container.threadRepository.messagesLinkedToSystemProvider()
        if (linked.isEmpty()) return@launch
        val systemSnapshot = container.smsProviderSync.allRowsSnapshot()
        val affectedThreads = mutableSetOf<String>()
        for (row in linked) {
            val systemRead = systemSnapshot[row.systemSmsId]
            if (systemRead == null) {
                container.threadRepository.deleteReconciledMessage(row.threadId, row.id)
                affectedThreads += row.threadId
            } else if (!row.outgoing && row.read != systemRead) {
                container.threadRepository.setMessageReadState(row.id, systemRead)
                affectedThreads += row.threadId
            }
        }
        affectedThreads.forEach { container.threadRepository.refreshThreadUnreadFlag(it) }
    }

    /** setDefaultSmsAppStatus(true) fires from more than one place within milliseconds of each
     * other right after the role is granted — MainActivity's roleRequestLauncher callback and
     * the onResume() that follows it practically immediately. Without this lock, both calls read
     * historyImported=false before either had a chance to write it back true, so the whole
     * import ran twice concurrently and duplicated every message. The mutex makes the second
     * caller wait for the first to actually finish (and persist historyImported) before it gets
     * to check that flag itself, so it correctly no-ops instead of re-running the import. */
    private val historyImportMutex = Mutex()

    /** Backfills pre-existing on-device SMS the first time we gain the default-SMS-app role. */
    private fun importHistoryOnce() = viewModelScope.launch {
        // Captured inside the mutex, at the moment historyImported itself is read — this is what
        // tells apart "this install genuinely already finished its one-time setup" from "Android's
        // Auto Backup silently restored the *record* of that decision from a previous install,
        // without restoring the data it was a decision about". allowBackup/dataExtractionRules
        // deliberately excludes messages.db from that cloud backup (see res/xml/backup_rules.xml)
        // so real messages are never restored outside the app's own explicit Drive-backup opt-in —
        // but the DataStore file holding historyImported/driveRestorePromptShown is NOT excluded,
        // since losing the user's theme/accent/other preferences on every reinstall would be its
        // own regression. Confirmed on a real device signed into a Google account: uninstall +
        // reinstall correctly wipes the local database, but the restored settings still say
        // historyImported=true and driveRestorePromptShown=true from the previous install, so the
        // app trusted them completely and skipped straight past both the sync screen and the Drive
        // check to a blank dashboard — silently and permanently, since nothing here ever re-checked
        // either flag against reality on its own.
        //
        // container.freshInstallMarker is the actual signal (see its own doc comment for why a
        // no-backup-directory marker file, not a database row count, is what correctly answers
        // "was this specific installation ever set up" independent of whether the user's inbox
        // happens to be empty for entirely unrelated reasons). wasFreshAtEntry has to be read here,
        // inside the lock, rather than before it: setDefaultSmsAppStatus(true) fires from two
        // places within milliseconds of each other (see historyImportMutex's own doc comment), and
        // reading the marker before acquiring the lock would let the second caller see the same
        // "still fresh" snapshot the first caller already acted on and is about to resolve —
        // reading it after the lock is held guarantees the second caller sees the marker the first
        // caller just wrote, the same protection the mutex already gives historyImported itself.
        var wasFreshAtEntry = false
        historyImportMutex.withLock {
            val settings = container.settingsRepository.settingsFlow.first()
            wasFreshAtEntry = container.freshInstallMarker.isFreshInstall()
            val alreadyImported = settings.historyImported && !wasFreshAtEntry
            if (!alreadyImported) {
                ephemeral.update { it.copy(isImportingHistory = true, importDone = 0, importTotal = 0, historySyncSuccess = false) }
                try {
                    container.smsHistoryImporter.importAll { done, total ->
                        ephemeral.update { it.copy(importDone = done, importTotal = total) }
                    }
                    container.settingsRepository.setHistoryImported(true)
                    container.freshInstallMarker.markComplete()
                    // A brief "done!" beat instead of jumping straight to whatever's next — the
                    // progress screen otherwise just vanishes the instant sync finishes, with
                    // nothing telling the user it actually succeeded.
                    ephemeral.update { it.copy(historySyncSuccess = true) }
                    delay(900)
                } catch (e: Exception) {
                    // Deliberately not rethrown: historyImported stays false so this retries next
                    // launch, but a transient failure here (e.g. a Room I/O hiccup) must never
                    // crash the app outright and strand the user on the syncing screen forever.
                    toast("Couldn't finish importing your existing messages — will retry next time you open the app")
                } finally {
                    ephemeral.update { it.copy(isImportingHistory = false, historySyncSuccess = false) }
                }
            }
        }
        // Third startup step, and only checked here — after local sync has definitively finished
        // (or been confirmed already done) — so "is there already something here worth not
        // overwriting" reflects the post-import state, matching the intended sequence: disclosure,
        // then sync, then (only if still empty) offer to restore from Drive. checkFirstLaunchDriveRestore
        // itself is cheap to call redundantly on every later launch — it no-ops immediately once
        // driveRestorePromptShown is set.
        //
        // forceRecheck carries wasFreshAtEntry forward rather than re-reading the marker here: the
        // import above may have just written it (markComplete(), right after a successful import),
        // so re-checking now would get the wrong answer for exactly the restored-settings case this
        // is meant to catch — the whole point is "was this fresh at the *start* of this sequence".
        checkFirstLaunchDriveRestore(forceRecheck = wasFreshAtEntry)
    }

    // endregion

    // region ---- settings ----

    fun setThemeMode(mode: String) = viewModelScope.launch { container.settingsRepository.setThemeMode(mode) }
    fun setAccent(hex: String) = viewModelScope.launch { container.settingsRepository.setAccentHex(hex) }
    fun setSwipeAction(left: Boolean, action: String) = viewModelScope.launch {
        if (left) container.settingsRepository.setSwipeLeftAction(action) else container.settingsRepository.setSwipeRightAction(action)
    }
    fun toggleChannel(channelId: String) = viewModelScope.launch {
        val settings = uiState.value.settings
        val current = when (channelId) {
            NotificationChannelIds.PERSONAL -> settings.channelPersonalEnabled
            NotificationChannelIds.OTP -> settings.channelOtpEnabled
            NotificationChannelIds.TRANSACTIONS -> settings.channelTransactEnabled
            else -> settings.channelPromoEnabled
        }
        container.settingsRepository.setChannelEnabled(channelId, !current)
    }
    fun toggleNotificationsAllowed() = viewModelScope.launch {
        container.settingsRepository.setNotificationsAllowed(!uiState.value.settings.notificationsAllowed)
    }
    fun toggleWakeScreen() = viewModelScope.launch {
        container.settingsRepository.setWakeScreen(!uiState.value.settings.wakeScreenForHighPriority)
    }
    fun toggleQuickActions() = viewModelScope.launch {
        container.settingsRepository.setQuickActions(!uiState.value.settings.quickActionButtons)
    }
    fun setLockScreenVisibility(mode: String) = viewModelScope.launch { container.settingsRepository.setLockScreenVisibility(mode) }
    fun toggleAppLock() = viewModelScope.launch { container.settingsRepository.setAppLockEnabled(!uiState.value.settings.appLockEnabled) }
    fun setAppLockMethod(method: String) = viewModelScope.launch { container.settingsRepository.setAppLockMethod(method) }
    fun onSignatureChange(value: String) = viewModelScope.launch { container.settingsRepository.setSignature(value) }
    fun toggleCharCount() = viewModelScope.launch { container.settingsRepository.setShowCharCount(!uiState.value.settings.showCharCount) }
    fun toggleInAppBrowser() = viewModelScope.launch { container.settingsRepository.setInAppBrowser(!uiState.value.settings.inAppBrowser) }
    fun toggleCloudFallback() = viewModelScope.launch { container.settingsRepository.setCloudFallbackEnabled(!uiState.value.settings.cloudFallbackEnabled) }
    fun onServerUrlChange(value: String) = viewModelScope.launch { container.settingsRepository.setServerBaseUrl(value) }
    fun setBackupFrequency(frequency: String) = viewModelScope.launch { container.settingsRepository.setBackupFrequency(frequency) }
    fun toggleOtpEviction() = viewModelScope.launch { container.settingsRepository.setOtpEvictionEnabled(!uiState.value.settings.otpEvictionEnabled) }

    /** Backup/restore/disconnect are all async network-or-disk I/O with no immediate visible
     * effect — without this, tapping "Backup now" (or any of these) several times in a row queued
     * up that many redundant runs, since nothing disabled the button while one was in flight.
     * Shared across every action on BackupSettingsScreen: a real intent isn't to run two of these
     * at once anyway (e.g. backing up locally while also restoring from Drive). */
    private val _backupBusy = MutableStateFlow(false)
    val backupBusy: StateFlow<Boolean> = _backupBusy

    private fun runBackupAction(block: suspend () -> Unit) {
        if (_backupBusy.value) return
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                block()
            } finally {
                _backupBusy.value = false
            }
        }
    }

    /** The single "Backup now" action (see BackupSettingsScreen) — always backs up locally, and
     * also to Drive when that's enabled, instead of the two separate buttons this used to be
     * split across. Local always runs first and unconditionally; Drive is best-effort on top of
     * it, so a Drive failure (offline, waiting for Wi-Fi, revoked access) never prevents the local
     * snapshot — which is the one thing every device can always fall back to — from happening. */
    fun backupNow() = runBackupAction {
        container.backupManager.backupNow(container.database)
        container.settingsRepository.setLastLocalBackupAt(System.currentTimeMillis())

        val settings = uiState.value.settings
        if (!settings.cloudBackupConnected) {
            toast("Backed up locally")
            return@runBackupAction
        }
        toast(if (performDriveBackup()) "Backed up locally and to Google Drive" else "Backed up locally — Drive backup didn't go through")
    }

    /** The Drive half of [backupNow] — also reused by nothing else right now, but kept separate
     * from the local half so a Drive-specific failure reason (offline, no Wi-Fi, disconnected)
     * stays easy to reason about on its own. Returns whether it actually completed. */
    private suspend fun performDriveBackup(): Boolean {
        val settings = uiState.value.settings
        if (settings.driveWifiOnly && !container.isOnWifi()) return false
        val account = container.driveBackupManager.resolveConnectedAccount() ?: return false
        val token = container.driveBackupManager.accessToken(account) ?: return false
        val gzipped = container.backupManager.gzipDatabaseSnapshot(container.database)
        container.driveBackupManager.uploadBackup(token, "messages-${System.currentTimeMillis()}.bak", gzipped) ?: return false
        container.driveBackupManager.pruneOldBackups(token)
        container.settingsRepository.setLastDriveBackupAt(System.currentTimeMillis())
        return true
    }

    fun restoreNow() = runBackupAction {
        if (container.backupManager.restoreNow()) {
            container.settingsRepository.setLastLocalRestoreAt(System.currentTimeMillis())
            toast("Restored from local backup")
        }
    }

    // endregion

    // region ---- SMS prominent disclosure (Play Store policy requirement) ----

    private val _smsPermissionRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** MainActivity collects this and actually fires the SMS/default-handler permission prompts
     * — kept out of the ViewModel since requesting permissions needs an Activity.
     *
     * AppRoot gates its entire UI behind `!state.isDefaultSmsApp` and shows SmsDisclosureScreen
     * whenever that's true — on a fresh install (nothing granted yet), and again any time the
     * user switches to a different default SMS app later, since the app can't do anything
     * useful without that role. That single condition covers "first ever launch" and "lost the
     * role since" identically, so there's no separate persisted "have they seen this before"
     * flag to fall out of sync with reality: the gate simply reflects the role's actual current
     * state rather than a snapshot of whether it was once explained. This function is what the
     * gate's Continue/"Set as default" button calls — deliberately user-triggered, never fired
     * automatically on cold start, so we're never popping a system permission dialog the user
     * didn't just ask for. */
    val smsPermissionRequests: SharedFlow<Unit> = _smsPermissionRequests.asSharedFlow()

    fun requestBecomeDefaultSmsApp() = _smsPermissionRequests.tryEmit(Unit)

    // endregion

    // region ---- Google Drive backup ----

    private val _driveSignInRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val driveSignInRequests: SharedFlow<Unit> = _driveSignInRequests.asSharedFlow()

    /** MainActivity collects this and calls startActivityForResult with
     * container.driveBackupManager.signInIntent() — the intent itself needs an Activity, which the
     * ViewModel doesn't have, so this just signals "please launch it" the same way toastEvents
     * signals "please show this". */
    fun requestDriveSignIn() = _driveSignInRequests.tryEmit(Unit)

    /** Shared by two callers: Settings' "Connect to Google Drive" button, and the startup
     * DriveSignInPromptScreen's "Sign in" button (see requestDriveSignInForRestore) — told apart by
     * driveSignInNeededForRestore, since only the startup path should go on to check for backups
     * and offer to restore afterward. */
    fun handleDriveSignInResult(data: Intent?) = viewModelScope.launch {
        val fromStartupRestoreCheck = ephemeral.value.driveSignInNeededForRestore
        val account = container.driveBackupManager.handleSignInResult(data)
        val email = account?.email
        if (account == null || email == null) {
            toast("Google sign-in was cancelled or failed")
            if (fromStartupRestoreCheck) skipDriveSignInForRestore()
            return@launch
        }
        // A successful sign-in only proves an account was picked — it does NOT prove Drive access
        // was granted (that's a separate consent step Play Services can skip or the user can deny
        // independently of picking an account). Treating any successful sign-in as "connected"
        // without this check was the exact bug: the app would proceed to back up to Drive with no
        // real consent for that access at all.
        if (!container.driveBackupManager.hasDriveScope(account)) {
            toast("Signed in as $email, but Drive access wasn't granted — tap Connect again and allow access to Google Drive")
            if (fromStartupRestoreCheck) skipDriveSignInForRestore()
            return@launch
        }
        container.settingsRepository.setGoogleAccountEmail(email)
        container.settingsRepository.setCloudBackupConnected(true)
        if (fromStartupRestoreCheck) {
            checkDriveBackupsAndOffer(account)
        } else {
            toast("Connected to Google Drive as $email")
        }
    }

    fun disconnectGoogleDrive() = runBackupAction {
        container.driveBackupManager.signOut()
        container.settingsRepository.setGoogleAccountEmail(null)
        container.settingsRepository.setCloudBackupConnected(false)
        toast("Disconnected from Google Drive")
    }

    fun toggleDriveEnabled() = viewModelScope.launch {
        container.settingsRepository.setCloudBackupConnected(!uiState.value.settings.cloudBackupConnected)
    }

    fun toggleDriveWifiOnly() = viewModelScope.launch {
        container.settingsRepository.setDriveWifiOnly(!uiState.value.settings.driveWifiOnly)
    }

    /** Always a merge, never a destructive overwrite — unlike local Restore, which is explicitly a
     * clean overwrite of an already-local, already-understood backup. Drive restores can happen at
     * any time (not just first launch), potentially alongside real local data already on this
     * device, so silently discarding it would be a bad surprise. See DriveBackupMerger.
     *
     * The actual work is the private suspend function below, shared with confirmDriveRestore's
     * startup path — that path needs to *await* the merge (to keep StartupFlowScreen's loader up
     * until it genuinely finishes) rather than fire-and-forget into runBackupAction's own coroutine. */
    fun driveRestoreNow() = runBackupAction { performDriveRestore() }

    private suspend fun performDriveRestore() {
        val account = container.driveBackupManager.resolveConnectedAccount()
        if (account == null) {
            toast("Not connected to Google Drive")
            return
        }
        val token = container.driveBackupManager.accessToken(account) ?: run {
            toast("Couldn't reach Google Drive — check your connection and try again")
            return
        }
        val latest = container.driveBackupManager.listBackups(token).firstOrNull() ?: run {
            toast("No Drive backup found")
            return
        }
        val gzipped = container.driveBackupManager.downloadBackup(token, latest.id) ?: run {
            toast("Couldn't download the Drive backup")
            return
        }
        val raw = container.backupManager.gunzipDriveSnapshot(gzipped)
        container.driveBackupMerger.merge(raw)
        container.settingsRepository.setLastDriveRestoreAt(System.currentTimeMillis())
        toast("Merged in messages from your Google Drive backup")
    }

    // region ---- Storage & Data overview ----

    private val _storageOverview = MutableStateFlow<com.phuzle.labs.messages.ui.model.StorageOverviewUi?>(null)
    val storageOverview: StateFlow<com.phuzle.labs.messages.ui.model.StorageOverviewUi?> = _storageOverview

    /** One-shot load when Storage & Data opens — not worth a continuous reactive flow for a
     * summary that only needs to be roughly current (matches the threadInfoFirstContactAt pattern). */
    fun loadStorageOverview() = viewModelScope.launch {
        val counts = container.threadRepository.storageOverview()
        val bytes = container.backupManager.totalStorageBytes()
        _storageOverview.value = com.phuzle.labs.messages.ui.model.StorageOverviewUi(counts.chatCount, counts.senderCount, counts.messageCount, bytes)
    }

    // endregion

    // region ---- Backup list (pick-a-file restore, see BackupListScreen) ----

    private val _backupListState = MutableStateFlow(com.phuzle.labs.messages.ui.model.BackupListUiState())
    val backupListState: StateFlow<com.phuzle.labs.messages.ui.model.BackupListUiState> = _backupListState

    /** Loads every local snapshot plus every Drive snapshot (if connected) — not just the newest of
     * each, so a device migrating in from another install can see and pick an older one. */
    fun loadBackupLists() = viewModelScope.launch {
        _backupListState.update { it.copy(loading = true) }
        val local = container.backupManager.listBackups()
            .map { com.phuzle.labs.messages.ui.model.LocalBackupUi(it.fileName, it.timestampMillis) }

        val account = container.driveBackupManager.resolveConnectedAccount()
        val drive = if (account == null) {
            emptyList()
        } else {
            val token = container.driveBackupManager.accessToken(account)
            if (token == null) emptyList() else container.driveBackupManager.listBackups(token)
                .map { com.phuzle.labs.messages.ui.model.DriveBackupUi(it.id, it.name, it.createdTime) }
        }
        _backupListState.update { it.copy(loading = false, local = local, drive = drive, driveConnected = account != null) }
    }

    /** Destructive — overwrites the live database, unlike a Drive restore (always a merge). The
     * confirmation dialog lives in BackupListScreen, matching the destructive-action rule the rest
     * of the app follows (archive/delete/disconnect all confirm or offer undo). */
    fun restoreLocalBackup(fileName: String) {
        if (_backupListState.value.restoringKey != null) return
        viewModelScope.launch {
            _backupListState.update { it.copy(restoringKey = "local:$fileName") }
            try {
                if (container.backupManager.restore(fileName)) {
                    container.settingsRepository.setLastLocalRestoreAt(System.currentTimeMillis())
                    toast("Restored from backup")
                } else {
                    toast("Couldn't restore that backup")
                }
            } finally {
                _backupListState.update { it.copy(restoringKey = null) }
            }
        }
    }

    /** Always a merge (see driveRestoreNow/DriveBackupMerger) — safe to run without a confirmation
     * dialog since nothing local is ever discarded. */
    fun restoreDriveBackup(fileId: String) {
        if (_backupListState.value.restoringKey != null) return
        viewModelScope.launch {
            _backupListState.update { it.copy(restoringKey = "drive:$fileId") }
            try {
                val account = container.driveBackupManager.resolveConnectedAccount() ?: run {
                    toast("Not connected to Google Drive")
                    return@launch
                }
                val token = container.driveBackupManager.accessToken(account) ?: run {
                    toast("Couldn't reach Google Drive — check your connection and try again")
                    return@launch
                }
                val gzipped = container.driveBackupManager.downloadBackup(token, fileId) ?: run {
                    toast("Couldn't download that Drive backup")
                    return@launch
                }
                val raw = container.backupManager.gunzipDriveSnapshot(gzipped)
                container.driveBackupMerger.merge(raw)
                container.settingsRepository.setLastDriveRestoreAt(System.currentTimeMillis())
                toast("Merged in messages from that Drive backup")
            } finally {
                _backupListState.update { it.copy(restoringKey = null) }
            }
        }
    }

    // region ---- Export / restore-from-file (moving a backup between devices by hand) ----

    private val _exportBackupRequests = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val exportBackupRequests: SharedFlow<String> = _exportBackupRequests.asSharedFlow()

    private val _restoreFromFileRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val restoreFromFileRequests: SharedFlow<Unit> = _restoreFromFileRequests.asSharedFlow()

    /** Local snapshots live app-private (see LocalBackupManager's class doc) and can't be browsed
     * to or moved by hand — this is the only way to get a backup file onto, say, a USB drive or
     * cloud folder to carry to another device. MainActivity collects this and launches
     * ActivityResultContracts.CreateDocument, the same "please launch this for me" pattern as
     * requestDriveSignIn. Exported as gzip only (no device-bound AES — see gzipDatabaseSnapshot),
     * the same portable format Drive backups already use, so it can actually be restored elsewhere. */
    fun requestExportBackup() = _exportBackupRequests.tryEmit("messages-backup-${System.currentTimeMillis()}.bak")

    fun handleExportBackupResult(uri: android.net.Uri?) = viewModelScope.launch {
        if (uri == null) return@launch
        val gzipped = withContext(Dispatchers.IO) { container.backupManager.gzipDatabaseSnapshot(container.database) }
        val ok = withContext(Dispatchers.IO) { container.writeBytesToUri(uri, gzipped) }
        toast(if (ok) "Backup saved" else "Couldn't save the backup file")
    }

    fun requestRestoreFromFile() = _restoreFromFileRequests.tryEmit(Unit)

    /** Always a merge, same as a Drive restore — a file handed over from another device is exactly
     * that "migrating in, alongside data already here" case, so overwriting would be wrong. */
    fun handleRestoreFromFileResult(uri: android.net.Uri?) = viewModelScope.launch {
        if (uri == null) return@launch
        val bytes = withContext(Dispatchers.IO) { container.readBytesFromUri(uri) }
        if (bytes == null) {
            toast("Couldn't read that file")
            return@launch
        }
        val raw = runCatching { container.backupManager.gunzipDriveSnapshot(bytes) }.getOrNull()
        if (raw == null) {
            toast("That doesn't look like a Messages backup file")
            return@launch
        }
        container.driveBackupMerger.merge(raw)
        container.settingsRepository.setLastLocalRestoreAt(System.currentTimeMillis())
        toast("Merged in messages from that file")
    }

    // endregion

    // endregion

    /** Called once at startup (see MainActivity). A future release that ships smarter
     * classification rules (see RegexRules.CURRENT_VERSION) would otherwise silently do nothing
     * for senders the app already has a thread for — categories are only ever decided once, when
     * a thread is first created. Cheap no-op when the version hasn't changed since the last run. */
    fun reclassifyThreadsIfNeeded() = viewModelScope.launch {
        val settings = container.settingsRepository.settingsFlow.first()
        if (settings.appliedClassifierVersion >= com.phuzle.labs.messages.domain.categorization.RegexRules.CURRENT_VERSION) return@launch
        val result = runCatching {
            container.threadRepository.reclassifyAllThreads { sender, body -> container.classifier.classify(sender, body) }
            // A thread whose category just flipped to Transactions under the new rules (e.g. a
            // language/pattern the classifier didn't recognize before) never had its messages run
            // through TransactionExtractor at receive time, so it'd otherwise sit in the Passbook
            // tab's underlying data as a thread with no transactions at all. Reusing the same
            // backfill pass extraction logic here (see backfillTransactionsForTransactionThreads)
            // catches those retroactively — recordTransaction's dedup makes re-processing an
            // already-recorded message a safe no-op, so this is fine to run on every version bump,
            // not just once per install.
            backfillTransactionsForTransactionThreads()
        }
        // Only recorded on success — a failed pass (e.g. a transient DB error) should retry on the
        // next launch rather than being silently skipped forever.
        if (result.isSuccess) container.settingsRepository.setAppliedClassifierVersion(com.phuzle.labs.messages.domain.categorization.RegexRules.CURRENT_VERSION)
    }

    /** Called once at startup (see MainActivity). One-time correction for installs that ran
     * SmsHistoryImporter before it started extracting Passbook transactions from backfilled
     * history (see that class's doc comment) — without this, anyone who did their SMS import
     * before that fix shipped would have a permanently empty Passbook despite a full inbox of
     * bank/card texts. Re-scans already-imported messages (not the system SMS provider) so it
     * can't duplicate any message row; recordTransaction's own dedup check means re-processing a
     * message the live receiver already recorded is a safe no-op, not a duplicate entry. */
    fun backfillPassbookIfNeeded() = viewModelScope.launch {
        val settings = container.settingsRepository.settingsFlow.first()
        if (settings.passbookBackfilled) return@launch
        val succeeded = runCatching { backfillTransactionsForTransactionThreads() }.isSuccess
        if (succeeded) container.settingsRepository.setPassbookBackfilled(true)
    }

    private suspend fun backfillTransactionsForTransactionThreads() {
        container.threadRepository.transactionCandidateMessages().forEach { (thread, message) ->
            com.phuzle.labs.messages.domain.categorization.TransactionExtractor
                .extract(message.body, container.regexRules.amountPattern, fallbackMerchant = thread.displayName)
                ?.let { tx ->
                    container.passbookRepository.recordTransaction(
                        merchant = tx.merchant,
                        accountLast4 = tx.accountLast4,
                        amountCents = tx.amountCents,
                        isCredit = tx.isCredit,
                        timestampMillis = message.timestamp,
                    )
                }
        }
    }

    /** Called once at startup (see MainActivity/AppRoot). Only ever prompts once per install
     * (driveRestorePromptShown) — deliberately NOT gated on whether local threads already exist.
     * That used to be the condition (skip the Drive check if storageOverview().chatCount > 0),
     * on the theory that an established inbox shouldn't get an unsolicited "want to restore?"
     * popup. In practice that made the whole feature unreachable: this function is called at the
     * end of importHistoryOnce(), *after* the system SMS backfill already ran — and on any real
     * phone with actual texting history, that backfill alone makes chatCount > 0 before this line
     * ever executes, so the Drive check silently never fired, on every device, every time. The
     * "don't surprise an established inbox" concern is instead handled by the dialog itself always
     * offering Skip (see DriveRestorePromptScreen) and by DriveBackupMerger always merging, never
     * overwriting — so showing this is safe regardless of what's already local.
     *
     * Attempts a *silent* sign-in (no UI) first — when it works, Google Play Services can resolve
     * "this account already granted this app Drive access" with zero interaction, which is worth
     * trying since it's free. But silentSignIn() is NOT guaranteed to succeed just because the
     * account consented before: for a scoped/sensitive permission like Drive (as opposed to basic
     * profile/email), Play Services commonly returns ApiException(4) SIGN_IN_REQUIRED — "I can't
     * tell silently, an interactive sign-in is needed" — and this is routine, not a bug, especially
     * right after this app's own data was cleared (which wipes whatever local session state let a
     * *previous* silent attempt short-circuit). Treating that failure as "no backup" (the previous
     * version of this function) was itself the bug this fixes: it meant the Drive-restore step
     * silently never appeared for real accounts with real backups. Now a silent-sign-in failure
     * instead offers an explicit interactive "Sign in to check" step (driveSignInNeededForRestore),
     * still with Skip — see handleDriveSignInResult for what happens after that sign-in returns. */
    /** [forceRecheck] — see importHistoryOnce's wasFreshAtEntry: a stale driveRestorePromptShown
     * restored by Android's Auto Backup onto a fresh install is exactly as untrustworthy as a
     * stale historyImported would be, and for the same reason — it describes a decision made on a
     * previous installation, not this one. */
    fun checkFirstLaunchDriveRestore(forceRecheck: Boolean = false) = viewModelScope.launch {
        // Already showing one of the two prompts (the user hasn't decided yet) — importHistoryOnce
        // calls this again on every onResume, not just the first, so this avoids redundant sign-in/
        // Drive API calls for as long as either gate sits there waiting on a tap.
        if (ephemeral.value.driveRestoreAvailable || ephemeral.value.driveSignInNeededForRestore) return@launch
        // Same reason, for the in-flight case: this is the only startup step that makes network
        // calls, so it's also the only one slow enough for a second onResume to land mid-flight and
        // start a duplicate sign-in + backup listing.
        if (ephemeral.value.driveCheckInProgress) return@launch
        val settings = container.settingsRepository.settingsFlow.first()
        if (settings.driveRestorePromptShown && !forceRecheck) return@launch

        ephemeral.update { it.copy(driveCheckInProgress = true) }
        try {
            // Bounded, and treated as "couldn't tell" on expiry rather than retried forever. Both
            // halves of this check can hang indefinitely on their own: silentSignIn wraps a Play
            // Services Task in suspendCancellableCoroutine, and on a device with absent or broken
            // Play Services neither the success nor the failure listener is ever called, so the
            // coroutine simply never resumes. Since StartupFlowScreen is a hard gate over the whole
            // app, "never resumes" meant the user sat on a loader forever with no way to reach
            // their messages — a permanently bricked launch, not a slow one.
            val account = withTimeoutOrNull(DRIVE_STARTUP_CHECK_TIMEOUT_MS) {
                container.driveBackupManager.silentSignIn()
            }
            if (account?.email == null) {
                ephemeral.update { it.copy(driveSignInNeededForRestore = true) }
                return@launch
            }
            withTimeoutOrNull(DRIVE_STARTUP_CHECK_TIMEOUT_MS) { checkDriveBackupsAndOffer(account) }
                // Timed out mid-listing: fall back to the interactive step rather than leaving
                // every flag false, which would drop the user onto the generic loader again.
                ?: ephemeral.update { it.copy(driveSignInNeededForRestore = true) }
        } catch (e: Exception) {
            // Anything unexpected from Play Services or the Drive API (the individual manager
            // methods runCatching internally, but constructing the sign-in client itself can throw
            // on a device without Play Services) must not strand the user on the startup loader.
            // Mark the step decided and move on — Settings' "Connect to Google Drive" is still
            // there if they want to restore later.
            container.settingsRepository.setDriveRestorePromptShown(true)
            ephemeral.update { it.copy(driveSignInNeededForRestore = false, driveRestoreAvailable = false) }
        } finally {
            ephemeral.update { it.copy(driveCheckInProgress = false) }
        }
    }

    /** Shared tail end of both the silent path above and the interactive path below — resolves an
     * already-signed-in [account] to either "found a backup, offer to restore" or "nothing there,
     * don't ask again this install". */
    // TODO: migrate GoogleSignInAccount → Credential Manager (Google Identity Services) once the
    //  full sign-in flow in GoogleDriveBackupManager is updated.
    @Suppress("DEPRECATION")
    private suspend fun checkDriveBackupsAndOffer(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        val token = container.driveBackupManager.accessToken(account)
        if (token == null) {
            container.settingsRepository.setDriveRestorePromptShown(true)
            ephemeral.update { it.copy(driveSignInNeededForRestore = false) }
            return
        }
        if (container.driveBackupManager.listBackups(token).isEmpty()) {
            container.settingsRepository.setDriveRestorePromptShown(true)
            // Surfaced instead of silently continuing to the dashboard: a *silent* sign-in resolved
            // this account with zero interaction from the user, so this is the only moment they
            // find out which account got checked — and their only chance to try a different one
            // before this startup step is gone for the rest of the install (checkFirstLaunchDriveRestore
            // no-ops on every later launch once driveRestorePromptShown is set).
            ephemeral.update { it.copy(driveSignInNeededForRestore = false, driveNoBackupFoundEmail = account.email) }
            return
        }
        container.settingsRepository.setGoogleAccountEmail(account.email!!)
        ephemeral.update { it.copy(driveRestoreAvailable = true, driveSignInNeededForRestore = false) }
    }

    /** The startup screen's "Sign in" button (see DriveSignInPromptScreen) — reuses the exact same
     * interactive sign-in flow as Settings' "Connect to Google Drive", just requested from a
     * different place. handleDriveSignInResult tells the two apart via driveSignInNeededForRestore. */
    fun requestDriveSignInForRestore() = requestDriveSignIn()

    fun skipDriveSignInForRestore() = viewModelScope.launch {
        ephemeral.update { it.copy(driveSignInNeededForRestore = false) }
        container.settingsRepository.setDriveRestorePromptShown(true)
    }

    /** "Not you?" action from the restore-offer screen or the no-backup-found screen — lets the
     * user pick a different Google account through the same interactive chooser Settings' "Connect
     * to Google Drive" uses, instead of silently accepting whichever account a background sign-in
     * happened to resolve. Routes back through driveSignInNeededForRestore, the exact branch
     * handleDriveSignInResult already uses for the rest of the startup flow, so picking a different
     * account re-runs the backup check against it rather than being treated as a Settings-initiated
     * connect. */
    fun switchDriveAccountForRestore() {
        ephemeral.update { it.copy(driveRestoreAvailable = false, driveNoBackupFoundEmail = null, driveSignInNeededForRestore = true) }
        requestDriveSignIn()
    }

    fun dismissNoBackupFound() = viewModelScope.launch {
        ephemeral.update { it.copy(driveNoBackupFoundEmail = null) }
        container.settingsRepository.setDriveRestorePromptShown(true)
    }

    /** Awaits the merge directly (unlike driveRestoreNow's fire-and-forget) so driveRestoreInProgress
     * stays true — and StartupFlowScreen keeps its loader up — for the merge's real duration,
     * instead of dropping to the dashboard the instant the button is tapped while it's still
     * downloading/merging in the background. */
    fun confirmDriveRestore() = viewModelScope.launch {
        ephemeral.update { it.copy(driveRestoreAvailable = false, driveRestoreInProgress = true) }
        container.settingsRepository.setDriveRestorePromptShown(true)
        performDriveRestore()
        ephemeral.update { it.copy(driveRestoreInProgress = false) }
    }

    fun dismissDriveRestorePrompt() = viewModelScope.launch {
        ephemeral.update { it.copy(driveRestoreAvailable = false) }
        container.settingsRepository.setDriveRestorePromptShown(true)
    }

    // endregion

    private companion object {
        /** Per-leg budget for the startup Drive check. Generous enough for a cold Play Services
         * sign-in plus one Drive listing on a slow connection, but bounded — this check gates the
         * entire app behind a loader, so "slow" has to be allowed and "never" must not be. */
        const val DRIVE_STARTUP_CHECK_TIMEOUT_MS = 15_000L
    }
}

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(container) as T
}
