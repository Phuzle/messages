package com.phuzle.labs.messages.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phuzle.labs.messages.AppContainer
import com.phuzle.labs.messages.data.db.entity.BankAccountEntity
import com.phuzle.labs.messages.data.db.entity.BlockedNumberEntity
import com.phuzle.labs.messages.data.db.entity.MessageEntity
import com.phuzle.labs.messages.data.db.entity.ReminderEntity
import com.phuzle.labs.messages.data.db.entity.ThreadEntity
import com.phuzle.labs.messages.data.db.entity.TransactionEntity
import com.phuzle.labs.messages.data.prefs.AppSettings
import com.phuzle.labs.messages.domain.model.Category
import com.phuzle.labs.messages.domain.model.NotificationChannelIds
import com.phuzle.labs.messages.domain.model.initialsFor
import com.phuzle.labs.messages.ui.format.formatCentsPlain
import com.phuzle.labs.messages.ui.format.formatCentsSigned
import com.phuzle.labs.messages.ui.format.formatDueRelative
import com.phuzle.labs.messages.ui.format.formatMessageTime
import com.phuzle.labs.messages.ui.format.formatThreadListTime
import com.phuzle.labs.messages.ui.format.formatTransactionTime
import com.phuzle.labs.messages.ui.model.AccountUi
import com.phuzle.labs.messages.ui.model.ActionSheetUi
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.model.BlockedNumberUi
import com.phuzle.labs.messages.ui.model.CategoryChipUi
import com.phuzle.labs.messages.ui.model.CurrentThreadUi
import com.phuzle.labs.messages.ui.model.DashboardTab
import com.phuzle.labs.messages.ui.model.DeletedThreadUi
import com.phuzle.labs.messages.ui.model.MessageUi
import com.phuzle.labs.messages.ui.model.OtpModalUi
import com.phuzle.labs.messages.ui.model.PushedScreen
import com.phuzle.labs.messages.ui.model.ReminderUi
import com.phuzle.labs.messages.ui.model.ScheduleOptionUi
import com.phuzle.labs.messages.ui.model.SettingsSub
import com.phuzle.labs.messages.ui.model.TransactionUi
import com.phuzle.labs.messages.ui.theme.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private data class Ephemeral(
    val pushedScreen: PushedScreen? = null,
    val settingsSub: SettingsSub? = null,
    val activeTab: DashboardTab = DashboardTab.Messages,
    val activeThreadId: String? = null,
    val searchQuery: String = "",
    val activeCategory: Category = Category.All,
    val showDrawer: Boolean = false,
    val overflowMenuOpen: Boolean = false,
    val actionSheetThreadId: String? = null,
    val threadInput: String = "",
    val threadOtpCopied: Boolean = false,
    val composeTo: String = "",
    val composeBody: String = "",
    val composeScheduleKey: String? = null,
    val privateChatsUnlockedThisSession: Boolean = false,
    val otpModal: OtpModalUi? = null,
    val isDefaultSmsApp: Boolean = true,
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
    val accounts: List<BankAccountEntity>,
    val transactions: List<TransactionEntity>,
    val reminders: List<ReminderEntity>,
)

private val SCHEDULE_OPTIONS = listOf(null to "Send now", "1h" to "In 1 hour", "tom9" to "Tomorrow, 9 AM")

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(private val container: AppContainer) : ViewModel() {

    private val ephemeral = MutableStateFlow(Ephemeral())

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

    private val passbookSnapshot: Flow<PassbookSnapshot> = combine(
        container.passbookRepository.observeAccounts(),
        container.passbookRepository.observeTransactions(),
        container.passbookRepository.observeReminders(),
    ) { accounts, transactions, reminders -> PassbookSnapshot(accounts, transactions, reminders) }

    private val activeThreadMessages: Flow<List<MessageEntity>> = ephemeral
        .map { it.activeThreadId }
        .distinctUntilChanged()
        .flatMapLatest { id -> id?.let { container.threadRepository.observeMessages(it) } ?: flowOf(emptyList()) }

    val uiState: StateFlow<AppUiState> = combine(
        threadsSnapshot,
        passbookSnapshot,
        activeThreadMessages,
        container.settingsRepository.settingsFlow,
        ephemeral,
    ) { threads, passbook, messages, settings, eph -> buildUiState(threads, passbook, messages, settings, eph) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init {
        viewModelScope.launch {
            container.passbookRepository.seedIfEmpty()
        }
    }

    // region ---- derived state ----

    private fun buildUiState(
        threads: ThreadsSnapshot,
        passbook: PassbookSnapshot,
        messages: List<MessageEntity>,
        settings: AppSettings,
        eph: Ephemeral,
    ): AppUiState {
        val categories = Category.entries.map { CategoryChipUi(it, it.label, it == eph.activeCategory) }

        val query = eph.searchQuery.trim().lowercase()
        val filteredThreads = threads.inbox.filter { t ->
            val categoryOk = eph.activeCategory == Category.All || t.category == eph.activeCategory.name
            val searchOk = query.isEmpty() || t.displayName.lowercase().contains(query) || t.lastMessagePreview.lowercase().contains(query)
            categoryOk && searchOk
        }.map { it.toThreadUi() }

        val hasUnread = threads.inbox.any { it.unread }

        val accounts = passbook.accounts.map { AccountUi(it.id, it.name, it.last4, it.type, formatCentsPlain(it.balanceCents)) }
        val transactions = passbook.transactions.map {
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
                initials = initialsFor(thread.displayName),
                kindLabel = if (thread.isBusiness) "Business sender" else "Personal contact",
                channelName = channelNameFor(category),
                infoTitle = if (thread.isBusiness) "Sender info" else "Contact info",
                isReplyable = category.isReplyable,
                isOtp = category == Category.Otp,
                isBlocked = threads.blocked.any { it.number == thread.sender },
                latestOtpCode = latestIncoming?.let { container.regexRules.extractCode(it.body) },
            )
        }
        val currentThreadMessages = messages.map {
            MessageUi(
                id = it.id,
                text = it.body,
                timeLabel = if (it.scheduledFor != null && !it.sent) "Scheduled · ${it.scheduleLabel}" else formatMessageTime(it.timestamp),
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

        val scheduleOptions = SCHEDULE_OPTIONS.map { (key, label) -> ScheduleOptionUi(key, label, key == eph.composeScheduleKey) }

        return AppUiState(
            settings = settings,
            themeMode = ThemeMode.fromKey(settings.themeMode),
            pushedScreen = eph.pushedScreen,
            settingsSub = eph.settingsSub,
            activeTab = eph.activeTab,
            searchQuery = eph.searchQuery,
            activeCategory = eph.activeCategory,
            categories = categories,
            threads = filteredThreads,
            hasUnread = hasUnread,
            accounts = accounts,
            transactions = transactions,
            reminders = reminders,
            currentThread = currentThread,
            currentThreadMessages = currentThreadMessages,
            threadInput = eph.threadInput,
            threadOtpCopied = eph.threadOtpCopied,
            composeTo = eph.composeTo,
            composeBody = eph.composeBody,
            composeScheduleKey = eph.composeScheduleKey,
            scheduleOptions = scheduleOptions,
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
        )
    }

    private fun ThreadEntity.toThreadUi(): com.phuzle.labs.messages.ui.model.ThreadUi = com.phuzle.labs.messages.ui.model.ThreadUi(
        id = id,
        sender = sender,
        displayName = displayName,
        category = Category.fromStoredName(category),
        isBusiness = isBusiness,
        avatarColor = androidx.compose.ui.graphics.Color(avatarColor),
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

    fun openSettings() = ephemeral.update { it.copy(pushedScreen = PushedScreen.Settings, settingsSub = null, overflowMenuOpen = false, showDrawer = false) }
    fun openSettingsSub(sub: SettingsSub) = ephemeral.update { it.copy(settingsSub = sub) }
    fun openCompose() = ephemeral.update { it.copy(pushedScreen = PushedScreen.Compose, composeTo = "", composeBody = "", composeScheduleKey = null) }
    fun closeCompose() = ephemeral.update { it.copy(pushedScreen = null) }
    fun openRecycleBin() = ephemeral.update { it.copy(pushedScreen = PushedScreen.RecycleBin, showDrawer = false, overflowMenuOpen = false) }
    fun openArchivedScreen() = ephemeral.update { it.copy(pushedScreen = PushedScreen.Archived, showDrawer = false, overflowMenuOpen = false) }
    fun openThreadInfo() = ephemeral.update { it.copy(pushedScreen = PushedScreen.ThreadInfo) }
    fun openPrivateChatsScreen() = ephemeral.update { it.copy(pushedScreen = PushedScreen.PrivateChats, privateChatsUnlockedThisSession = false) }
    fun unlockPrivateChats() = ephemeral.update { it.copy(privateChatsUnlockedThisSession = true) }

    fun goBack() = ephemeral.update {
        when {
            it.pushedScreen == PushedScreen.ThreadInfo -> it.copy(pushedScreen = PushedScreen.Thread)
            it.pushedScreen == PushedScreen.Settings && it.settingsSub != null -> it.copy(settingsSub = null)
            else -> it.copy(pushedScreen = null, activeThreadId = null, settingsSub = null)
        }
    }

    fun setCategory(category: Category) = ephemeral.update { it.copy(activeCategory = category) }
    fun onSearchChange(query: String) = ephemeral.update { it.copy(searchQuery = query) }

    // endregion

    // region ---- threads ----

    fun openThreadById(id: String) = ephemeral.update { it.copy(pushedScreen = PushedScreen.Thread, activeThreadId = id, threadInput = "") }

    fun markAllAsRead() = viewModelScope.launch {
        container.threadRepository.markAllRead()
        ephemeral.update { it.copy(overflowMenuOpen = false) }
    }

    fun onSwipeRight(threadId: String) = performThreadAction(uiState.value.settings.swipeRightAction, threadId)
    fun onSwipeLeft(threadId: String) = performThreadAction(uiState.value.settings.swipeLeftAction, threadId)

    private fun performThreadAction(action: String, threadId: String) = viewModelScope.launch {
        when (action) {
            "archive" -> container.threadRepository.archive(threadId)
            "delete" -> container.threadRepository.softDelete(threadId, System.currentTimeMillis())
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
    }

    fun restoreThread(id: String) = viewModelScope.launch { container.threadRepository.restore(id) }
    fun unarchiveThread(id: String) = viewModelScope.launch { container.threadRepository.unarchive(id) }

    // endregion

    // region ---- compose & replying ----

    fun onComposeToChange(value: String) = ephemeral.update { it.copy(composeTo = value) }
    fun onComposeBodyChange(value: String) = ephemeral.update { it.copy(composeBody = value) }
    fun setComposeSchedule(key: String?) = ephemeral.update { it.copy(composeScheduleKey = key) }

    fun sendCompose() = viewModelScope.launch {
        val eph = ephemeral.value
        var body = eph.composeBody.trim()
        if (body.isEmpty()) return@launch
        val to = eph.composeTo.trim().ifEmpty { "New Contact" }
        val signature = uiState.value.settings.signature.trim()
        if (signature.isNotEmpty()) body = "$body\n$signature"

        val scheduleLabel = SCHEDULE_OPTIONS.firstOrNull { it.first == eph.composeScheduleKey }?.second
        val scheduledFor = when (eph.composeScheduleKey) {
            "1h" -> System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
            "tom9" -> nextNineAm()
            else -> null
        }

        val (thread, _) = container.threadRepository.composeOutgoingThread(to, body, scheduledFor, scheduleLabel, System.currentTimeMillis())
        if (scheduledFor == null) {
            runCatching { container.smsSender.send(to, body) }
        }
        ephemeral.update { it.copy(pushedScreen = PushedScreen.Thread, activeThreadId = thread.id, composeTo = "", composeBody = "", composeScheduleKey = null) }
    }

    private fun nextNineAm(): Long {
        val zone = java.time.ZoneId.systemDefault()
        val now = java.time.ZonedDateTime.now(zone)
        var next = now.withHour(9).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.toInstant().toEpochMilli()
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
        container.threadRepository.appendOutgoingMessage(threadId, text, null, null, System.currentTimeMillis())
        runCatching { container.smsSender.send(thread.sender, text) }
        ephemeral.update { it.copy(threadInput = "") }
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
        if (thread.isBlocked) container.threadRepository.unblock(thread.sender) else container.threadRepository.block(thread.sender)
    }

    fun unblockNumber(number: String) = viewModelScope.launch { container.threadRepository.unblock(number) }

    // endregion

    // region ---- OTP demo + hot-swap ----

    /** The overflow menu's "Simulate incoming OTP" — exercises the full classify → store → notify path locally. */
    fun simulateOtp() = viewModelScope.launch {
        val code = (100000..999999).random().toString()
        val body = "Your Northgate Bank verification code is $code. Do not share this code."
        val (thread, message) = container.threadRepository.recordIncomingMessage(
            sender = "Northgate Bank",
            displayName = "Northgate Bank",
            isBusiness = true,
            category = Category.Otp,
            body = body,
            timestampMillis = System.currentTimeMillis(),
        )
        container.messageNotifier.notifyIncoming(thread, message, Category.Otp, code)
        ephemeral.update { it.copy(overflowMenuOpen = false) }
        checkOtpHotSwap()
    }

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

    fun setDefaultSmsAppStatus(isDefault: Boolean) = ephemeral.update { it.copy(isDefaultSmsApp = isDefault) }

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
    fun setBackupFrequency(frequency: String) = viewModelScope.launch { container.settingsRepository.setBackupFrequency(frequency) }
    fun toggleCloud() = viewModelScope.launch { container.settingsRepository.setCloudBackupConnected(!uiState.value.settings.cloudBackupConnected) }
    fun toggleOtpEviction() = viewModelScope.launch { container.settingsRepository.setOtpEvictionEnabled(!uiState.value.settings.otpEvictionEnabled) }

    fun backupNow() = viewModelScope.launch {
        container.backupManager.backupNow(container.database)
        container.settingsRepository.setLastLocalBackupAt(System.currentTimeMillis())
    }

    fun restoreNow() = viewModelScope.launch {
        if (container.backupManager.restoreNow()) {
            container.settingsRepository.setLastLocalRestoreAt(System.currentTimeMillis())
        }
    }

    // endregion
}

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(container) as T
}
