package com.phuzle.labs.messages.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phuzle.labs.messages.AppContainer
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
import com.phuzle.labs.messages.ui.model.SettingsSub
import com.phuzle.labs.messages.ui.model.TransactionUi
import com.phuzle.labs.messages.ui.model.UpdateInfoUi
import com.phuzle.labs.messages.ui.theme.ThemeMode
import com.phuzle.labs.messages.BuildConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    val actionSheetThreadId: String? = null,
    val threadInput: String = "",
    val threadOtpCopied: Boolean = false,
    val composeTo: String = "",
    val composeBody: String = "",
    val composeRecipients: List<ContactSuggestionUi> = emptyList(),
    val composeCustomScheduleMillis: Long? = null,
    val composeDraftId: String? = null,
    /** Set when Compose was opened from the Drafts list, so closing it (see closeCompose) returns
     * there instead of falling back to the dashboard. */
    val composeOpenedFromDrafts: Boolean = false,
    val composeToSuggestions: List<ContactSuggestionUi> = emptyList(),
    val privateChatsUnlockedThisSession: Boolean = false,
    val otpModal: OtpModalUi? = null,
    val isDefaultSmsApp: Boolean = true,
    val updateInfo: UpdateInfoUi? = null,
    val selectedAccountLast4: String? = null,
    val isImportingHistory: Boolean = false,
    val importDone: Int = 0,
    val importTotal: Int = 0,
    val threadOverflowMenuOpen: Boolean = false,
    val threadSearchActive: Boolean = false,
    val threadSearchQuery: String = "",
    /** First-launch-only prompt (see checkFirstLaunchDriveRestore) offering to restore/merge a
     * Google Drive backup found via a silent, no-UI sign-in. */
    val driveRestoreAvailable: Boolean = false,
    /** Fetched once when Thread Info opens (see openThreadInfo) — not worth a continuous reactive
     * flow just for a "first contact" date that never changes after the fact. */
    val threadInfoFirstContactAt: Long? = null,
    val messageActionTarget: MessageActionTargetUi? = null,
)

private data class ThreadsSnapshot(
    val inbox: List<ThreadEntity>,
    val allActive: List<ThreadEntity>,
    val archived: List<ThreadEntity>,
    val deleted: List<ThreadEntity>,
    val privateList: List<ThreadEntity>,
    val blocked: List<BlockedNumberEntity>,
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
        ThreadsSnapshot(lists[0], lists[1], lists[2], lists[3], lists[4], blocked)
    }

    /** Real search — fuzzy-matches (see FuzzyMatcher) against a thread's sender name *or any
     * message in its full history*, not just the cached last-message preview. Null means "no
     * active search, don't filter". */
    private val searchMatchingIds: Flow<Set<String>?> = ephemeral
        .map { it.searchQuery.trim() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                flowOf(null)
            } else {
                container.threadRepository.observeSearchCandidates().map { rows ->
                    rows.mapNotNull { row ->
                        val nameMatch = FuzzyMatcher.match(query, row.displayName)
                        val bodyMatch = row.body?.let { FuzzyMatcher.match(query, it) }
                        row.threadId.takeIf { nameMatch != null || bodyMatch != null }
                    }.toSet()
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
            state.copy(drafts = drafts.map {
                DraftUi(
                    id = it.id,
                    to = it.to.ifBlank { "No recipient" },
                    bodyPreview = it.body.take(60),
                    timeLabel = formatThreadListTime(it.updatedAt),
                )
            })
        }
        .combine(undoState) { state, undo -> state.copy(undoMessage = undo?.message) }
        .combine(searchMatchingIds) { state, matches ->
            if (matches == null) state else state.copy(threads = state.threads.filter { matches.contains(it.id) })
        }
        .combine(ephemeral.map { it.searchQuery.trim() }.distinctUntilChanged()) { state, query ->
            // Bolding is purely cosmetic and only ever applies to the two fields actually shown in
            // the row (displayName/preview) — a match found elsewhere in a thread's full history
            // still includes the thread (see searchMatchingIds) but has nothing visible to bold.
            if (query.isEmpty()) {
                state
            } else {
                state.copy(threads = state.threads.map { t ->
                    t.copy(
                        displayNameMatch = FuzzyMatcher.match(query, t.displayName)?.matchedIndices ?: emptySet(),
                        previewMatch = FuzzyMatcher.match(query, t.preview)?.matchedIndices ?: emptySet(),
                    )
                })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init {
        viewModelScope.launch {
            val update = container.updateChecker.checkForUpdate(BuildConfig.VERSION_CODE.toLong())
            if (update != null) {
                ephemeral.update { it.copy(updateInfo = UpdateInfoUi(update.message)) }
            }
        }
    }

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
        }.map { it.toThreadUi() }

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
            val latestIncoming = messages.lastOrNull { !it.outgoing }
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
                isOtp = category == Category.Otp,
                isBlocked = threads.blocked.any { it.number == thread.sender },
                latestOtpCode = latestIncoming?.let { container.regexRules.extractCode(it.body) },
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
            )
        }

        fun toDeleted(t: ThreadEntity) = DeletedThreadUi(
            t.id, t.displayName, initialsFor(t.displayName), androidx.compose.ui.graphics.Color(t.avatarColor), t.isBusiness, t.lastMessagePreview,
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
            threadOtpCopied = eph.threadOtpCopied,
            composeTo = eph.composeTo,
            composeBody = eph.composeBody,
            composeRecipients = eph.composeRecipients,
            composeCustomScheduleMillis = eph.composeCustomScheduleMillis,
            composeToSuggestions = eph.composeToSuggestions,
            deletedThreads = threads.deleted.map(::toDeleted),
            archivedThreads = threads.archived.map(::toDeleted),
            privateThreads = threads.privateList.map(::toDeleted),
            privateChatsUnlockedThisSession = eph.privateChatsUnlockedThisSession,
            blockedList = threads.blocked.map { BlockedNumberUi(it.number) },
            showDrawer = eph.showDrawer,
            overflowMenuOpen = eph.overflowMenuOpen,
            actionSheet = actionSheet,
            otpModal = eph.otpModal,
            isDefaultSmsApp = eph.isDefaultSmsApp,
            updateInfo = eph.updateInfo,
            threadOverflowMenuOpen = eph.threadOverflowMenuOpen,
            threadSearchActive = eph.threadSearchActive,
            threadSearchQuery = eph.threadSearchQuery,
            driveRestoreAvailable = eph.driveRestoreAvailable,
            messageActionTarget = eph.messageActionTarget,
            isImportingHistory = eph.isImportingHistory,
            importDone = eph.importDone,
            importTotal = eph.importTotal,
        )
    }

    private fun ThreadEntity.toThreadUi(): com.phuzle.labs.messages.ui.model.ThreadUi = com.phuzle.labs.messages.ui.model.ThreadUi(
        id = id,
        sender = sender,
        displayName = displayName,
        category = Category.fromStoredName(category),
        isBusiness = isBusiness,
        avatarColor = androidx.compose.ui.graphics.Color(avatarColor),
        photoUri = photoUri,
        initials = initialsFor(displayName),
        preview = lastMessagePreview,
        timeLabel = formatThreadListTime(lastMessageTime),
        unread = unread,
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
        container.draftRepository.save(eph.composeDraftId, to, body)
        toast("Saved to drafts")
    }

    fun openDraftsScreen() = ephemeral.update { it.copy(pushedScreen = PushedScreen.Drafts, showDrawer = false, overflowMenuOpen = false) }

    fun openDraft(id: String) = viewModelScope.launch {
        val draft = container.draftRepository.findById(id) ?: return@launch
        val recipients = draft.to.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { ContactSuggestionUi(it, it) }
        ephemeral.update {
            it.copy(
                pushedScreen = PushedScreen.Compose,
                composeTo = "",
                composeBody = draft.body,
                composeRecipients = recipients,
                composeDraftId = draft.id,
                composeCustomScheduleMillis = null,
                composeToSuggestions = emptyList(),
                composeOpenedFromDrafts = true,
            )
        }
    }

    fun deleteDraft(id: String) = viewModelScope.launch {
        val draft = container.draftRepository.findById(id) ?: return@launch
        container.draftRepository.delete(id)
        offerUndo("Draft deleted") { container.draftRepository.save(draft.id, draft.to, draft.body) }
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
    fun openPrivateChatsScreen() {
        settingsAwareNav(PushedScreen.PrivateChats)
        ephemeral.update { it.copy(privateChatsUnlockedThisSession = false) }
    }
    fun unlockPrivateChats() = ephemeral.update { it.copy(privateChatsUnlockedThisSession = true) }

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

    fun setCategory(category: Category) = ephemeral.update { it.copy(activeCategory = category) }
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
        // viewing the thread.
        viewModelScope.launch {
            val thread = container.threadRepository.getThread(id) ?: return@launch
            if (thread.unread) container.threadRepository.toggleRead(id, true)
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

    fun markAllAsRead() = viewModelScope.launch {
        container.threadRepository.markAllRead()
        ephemeral.update { it.copy(overflowMenuOpen = false) }
    }

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

    /** Adds a contact-search hit as a recipient chip; a number typed with no match can also be added this way. */
    fun selectComposeContact(contact: ContactSuggestionUi) = ephemeral.update {
        if (it.composeRecipients.any { r -> r.number == contact.number }) {
            it.copy(composeTo = "", composeToSuggestions = emptyList())
        } else {
            it.copy(composeRecipients = it.composeRecipients + contact, composeTo = "", composeToSuggestions = emptyList())
        }
    }

    /** Adds whatever's currently typed in "To" as a raw recipient (no contact match required). */
    fun addTypedComposeRecipient() {
        val number = ephemeral.value.composeTo.trim()
        if (number.isEmpty()) return
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

        data class Sent(val threadId: String, val messageId: Long, val number: String)
        val sent = mutableListOf<Sent>()
        var lastThreadId: String? = null
        for (recipient in recipients) {
            val (thread, message) = container.threadRepository.composeOutgoingThread(
                to = recipient.number, body = body, scheduledFor = scheduledFor, scheduleLabel = scheduleLabel, nowMillis = now,
                displayName = recipient.name, photoUri = recipient.photoUri,
            )
            sent += Sent(thread.id, message.id, recipient.number)
            lastThreadId = thread.id
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
                olderMessages = emptyList(), hasMoreOlderMessages = true, isLoadingOlderMessages = false,
            )
        }

        if (scheduledFor == null) {
            // Held back for UNDO_WINDOW_MS so "undo send" actually prevents the SMS from going out,
            // not just the local row — undo cancels this job before it ever calls SmsSender.
            val sendJob = viewModelScope.launch {
                delay(UNDO_WINDOW_MS)
                sent.forEach { runCatching { container.smsSender.send(it.number, body) } }
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

    fun sendThreadMessage() = viewModelScope.launch {
        val eph = ephemeral.value
        val threadId = eph.activeThreadId ?: return@launch
        var text = eph.threadInput.trim()
        if (text.isEmpty()) return@launch
        val signature = uiState.value.settings.signature.trim()
        if (signature.isNotEmpty()) text = "$text\n$signature"

        val thread = container.threadRepository.getThread(threadId) ?: return@launch
        val message = container.threadRepository.appendOutgoingMessage(threadId, text, null, null, System.currentTimeMillis())
        ephemeral.update { it.copy(threadInput = "") }

        val sendJob = viewModelScope.launch {
            delay(UNDO_WINDOW_MS)
            runCatching { container.smsSender.send(thread.sender, text) }
        }
        offerUndo("Message sent") {
            sendJob.cancel()
            container.threadRepository.deleteMessage(threadId, message.id)
        }
    }

    fun copyThreadCode() = viewModelScope.launch {
        val code = uiState.value.currentThread?.latestOtpCode ?: return@launch
        container.copyToClipboard("OTP code", code)
        ephemeral.update { it.copy(threadOtpCopied = true) }
        delay(1500)
        ephemeral.update { it.copy(threadOtpCopied = false) }
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

    // endregion

    // region ---- OTP hot-swap ----

    /** Called from Activity.onResume: shows the modal if an OTP arrived in the last 30 seconds. */
    fun checkOtpHotSwap() = viewModelScope.launch {
        val latest = container.threadRepository.latestIncomingOtpMessage() ?: return@launch
        val age = System.currentTimeMillis() - latest.timestamp
        if (age !in 0..30_000) return@launch
        val code = container.regexRules.extractCode(latest.body) ?: return@launch
        val thread = container.threadRepository.getThread(latest.threadId) ?: return@launch
        ephemeral.update { it.copy(otpModal = OtpModalUi(thread.displayName, code, copied = false)) }
    }

    fun closeOtpModal() = ephemeral.update { it.copy(otpModal = null) }

    fun copyOtpCode() = viewModelScope.launch {
        val modal = ephemeral.value.otpModal ?: return@launch
        container.copyToClipboard("OTP code", modal.code)
        ephemeral.update { it.copy(otpModal = it.otpModal?.copy(copied = true)) }
        delay(2000)
        ephemeral.update { it.copy(otpModal = null) }
    }

    fun setDefaultSmsAppStatus(isDefault: Boolean) {
        ephemeral.update { it.copy(isDefaultSmsApp = isDefault) }
        if (isDefault) importHistoryOnce()
    }

    /** Backfills pre-existing on-device SMS the first time we gain the default-SMS-app role. */
    private fun importHistoryOnce() = viewModelScope.launch {
        if (container.settingsRepository.settingsFlow.first().historyImported) return@launch
        ephemeral.update { it.copy(isImportingHistory = true, importDone = 0, importTotal = 0) }
        container.smsHistoryImporter.importAll { done, total ->
            ephemeral.update { it.copy(importDone = done, importTotal = total) }
        }
        container.settingsRepository.setHistoryImported(true)
        ephemeral.update { it.copy(isImportingHistory = false) }
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

    fun backupNow() = viewModelScope.launch {
        container.backupManager.backupNow(container.database)
        container.settingsRepository.setLastLocalBackupAt(System.currentTimeMillis())
        toast("Backed up locally")
    }

    fun restoreNow() = viewModelScope.launch {
        if (container.backupManager.restoreNow()) {
            container.settingsRepository.setLastLocalRestoreAt(System.currentTimeMillis())
            toast("Restored from local backup")
        }
    }

    // endregion

    // region ---- Google Drive backup ----

    private val _driveSignInRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val driveSignInRequests: SharedFlow<Unit> = _driveSignInRequests.asSharedFlow()

    /** MainActivity collects this and calls startActivityForResult with
     * container.driveBackupManager.signInIntent() — the intent itself needs an Activity, which the
     * ViewModel doesn't have, so this just signals "please launch it" the same way toastEvents
     * signals "please show this". */
    fun requestDriveSignIn() = _driveSignInRequests.tryEmit(Unit)

    fun handleDriveSignInResult(data: Intent?) = viewModelScope.launch {
        val account = container.driveBackupManager.handleSignInResult(data)
        val email = account?.email
        if (account == null || email == null) {
            toast("Google sign-in was cancelled or failed")
            return@launch
        }
        // A successful sign-in only proves an account was picked — it does NOT prove Drive access
        // was granted (that's a separate consent step Play Services can skip or the user can deny
        // independently of picking an account). Treating any successful sign-in as "connected"
        // without this check was the exact bug: the app would proceed to back up to Drive with no
        // real consent for that access at all.
        if (!container.driveBackupManager.hasDriveScope(account)) {
            toast("Signed in as $email, but Drive access wasn't granted — tap Connect again and allow access to Google Drive")
            return@launch
        }
        container.settingsRepository.setGoogleAccountEmail(email)
        container.settingsRepository.setCloudBackupConnected(true)
        toast("Connected to Google Drive as $email")
    }

    fun disconnectGoogleDrive() = viewModelScope.launch {
        container.driveBackupManager.signOut {}
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

    fun driveBackupNow() = viewModelScope.launch {
        val settings = uiState.value.settings
        if (settings.driveWifiOnly && !container.isOnWifi()) {
            toast("Waiting for Wi-Fi — this device only backs up to Drive on Wi-Fi (see Backup & Restore)")
            return@launch
        }
        val account = container.driveBackupManager.lastSignedInAccount()
        if (account == null) {
            toast("Not connected to Google Drive")
            return@launch
        }
        val token = container.driveBackupManager.accessToken(account)
        if (token == null) {
            toast("Couldn't reach Google Drive — check your connection and try again")
            return@launch
        }
        val gzipped = container.backupManager.gzipDatabaseSnapshot(container.database)
        val fileId = container.driveBackupManager.uploadBackup(token, "messages-${System.currentTimeMillis()}.bak", gzipped)
        if (fileId == null) {
            toast("Drive backup failed — see BackupSettingsScreen's doc comment for the Google Cloud setup this needs")
            return@launch
        }
        container.driveBackupManager.pruneOldBackups(token)
        container.settingsRepository.setLastDriveBackupAt(System.currentTimeMillis())
        toast("Backed up to Google Drive")
    }

    /** Always a merge, never a destructive overwrite — unlike local Restore, which is explicitly a
     * clean overwrite of an already-local, already-understood backup. Drive restores can happen at
     * any time (not just first launch), potentially alongside real local data already on this
     * device, so silently discarding it would be a bad surprise. See DriveBackupMerger. */
    fun driveRestoreNow() = viewModelScope.launch {
        val account = container.driveBackupManager.lastSignedInAccount()
        if (account == null) {
            toast("Not connected to Google Drive")
            return@launch
        }
        val token = container.driveBackupManager.accessToken(account) ?: run {
            toast("Couldn't reach Google Drive — check your connection and try again")
            return@launch
        }
        val latest = container.driveBackupManager.listBackups(token).firstOrNull() ?: run {
            toast("No Drive backup found")
            return@launch
        }
        val gzipped = container.driveBackupManager.downloadBackup(token, latest.id) ?: run {
            toast("Couldn't download the Drive backup")
            return@launch
        }
        val raw = container.backupManager.gunzipDriveSnapshot(gzipped)
        container.driveBackupMerger.merge(raw)
        container.settingsRepository.setLastDriveRestoreAt(System.currentTimeMillis())
        toast("Merged in messages from your Google Drive backup")
    }

    // region ---- Backup list (pick-a-file restore, see BackupListScreen) ----

    private val _backupListState = MutableStateFlow(com.phuzle.labs.messages.ui.model.BackupListUiState())
    val backupListState: StateFlow<com.phuzle.labs.messages.ui.model.BackupListUiState> = _backupListState

    /** Loads every local snapshot plus every Drive snapshot (if connected) — not just the newest of
     * each, so a device migrating in from another install can see and pick an older one. */
    fun loadBackupLists() = viewModelScope.launch {
        _backupListState.update { it.copy(loading = true) }
        val local = container.backupManager.listBackups()
            .map { com.phuzle.labs.messages.ui.model.LocalBackupUi(it.fileName, it.timestampMillis) }

        val account = container.driveBackupManager.lastSignedInAccount()
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
    fun restoreLocalBackup(fileName: String) = viewModelScope.launch {
        if (container.backupManager.restore(fileName)) {
            container.settingsRepository.setLastLocalRestoreAt(System.currentTimeMillis())
            toast("Restored from backup")
        } else {
            toast("Couldn't restore that backup")
        }
    }

    /** Always a merge (see driveRestoreNow/DriveBackupMerger) — safe to run without a confirmation
     * dialog since nothing local is ever discarded. */
    fun restoreDriveBackup(fileId: String) = viewModelScope.launch {
        val account = container.driveBackupManager.lastSignedInAccount() ?: run {
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
    }

    // endregion

    /** Called once at startup (see MainActivity/AppRoot). Only ever prompts once per install
     * (driveRestorePromptShown) and only when there's nothing local to lose yet — an existing
     * inbox with real messages should never get an unsolicited "want to restore?" popup. Uses a
     * *silent* sign-in (no UI) — Google Play Services remembers this app's consent at the account
     * level even across a reinstall, so this can genuinely detect "you've done this before on this
     * device" without asking the user anything until there's actually a backup to offer. */
    fun checkFirstLaunchDriveRestore() = viewModelScope.launch {
        val settings = container.settingsRepository.settingsFlow.first()
        if (settings.driveRestorePromptShown) return@launch
        if (uiState.value.threads.isNotEmpty()) {
            container.settingsRepository.setDriveRestorePromptShown(true)
            return@launch
        }
        val account = container.driveBackupManager.silentSignIn()
        if (account?.email == null) {
            container.settingsRepository.setDriveRestorePromptShown(true)
            return@launch
        }
        val token = container.driveBackupManager.accessToken(account)
        if (token == null) {
            container.settingsRepository.setDriveRestorePromptShown(true)
            return@launch
        }
        if (container.driveBackupManager.listBackups(token).isEmpty()) {
            container.settingsRepository.setDriveRestorePromptShown(true)
            return@launch
        }
        container.settingsRepository.setGoogleAccountEmail(account.email)
        ephemeral.update { it.copy(driveRestoreAvailable = true) }
    }

    fun confirmDriveRestore() {
        ephemeral.update { it.copy(driveRestoreAvailable = false) }
        viewModelScope.launch { container.settingsRepository.setDriveRestorePromptShown(true) }
        driveRestoreNow()
    }

    fun dismissDriveRestorePrompt() = viewModelScope.launch {
        ephemeral.update { it.copy(driveRestoreAvailable = false) }
        container.settingsRepository.setDriveRestorePromptShown(true)
    }

    // endregion
}

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(container) as T
}
