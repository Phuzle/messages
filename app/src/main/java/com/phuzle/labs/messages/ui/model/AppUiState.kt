package com.phuzle.labs.messages.ui.model

import com.phuzle.labs.messages.data.prefs.AppSettings
import com.phuzle.labs.messages.domain.model.Category
import com.phuzle.labs.messages.ui.theme.ThemeMode

data class OtpModalUi(val senderLabel: String, val code: String, val copied: Boolean)
data class ActionSheetUi(val threadId: String, val sender: String, val markReadLabel: String, val privateLabel: String)
data class UpdateInfoUi(val message: String)

/** The single source of render truth for the whole app — the Compose analogue of the prototype's `state`. */
data class AppUiState(
    val settings: AppSettings = AppSettings(),
    val themeMode: ThemeMode = ThemeMode.System,

    val pushedScreen: PushedScreen? = null,
    val settingsSub: SettingsSub? = null,
    val activeTab: DashboardTab = DashboardTab.Messages,

    val searchQuery: String = "",
    val activeCategory: Category = Category.All,
    val categories: List<CategoryChipUi> = emptyList(),
    val threads: List<ThreadUi> = emptyList(),
    val hasUnread: Boolean = false,

    val accounts: List<AccountUi> = emptyList(),
    val transactions: List<TransactionUi> = emptyList(),
    val reminders: List<ReminderUi> = emptyList(),
    val selectedAccountLast4: String? = null,

    val currentThread: CurrentThreadUi? = null,
    val currentThreadMessages: List<MessageUi> = emptyList(),
    val isLoadingOlderMessages: Boolean = false,
    val hasMoreOlderMessages: Boolean = true,
    val threadInput: String = "",
    val threadOtpCopied: Boolean = false,

    val composeTo: String = "",
    val composeBody: String = "",
    val composeScheduleKey: String? = null,
    val scheduleOptions: List<ScheduleOptionUi> = emptyList(),
    val composeToSuggestions: List<ContactSuggestionUi> = emptyList(),
    val drafts: List<DraftUi> = emptyList(),

    val deletedThreads: List<DeletedThreadUi> = emptyList(),
    val archivedThreads: List<DeletedThreadUi> = emptyList(),
    val privateThreads: List<DeletedThreadUi> = emptyList(),
    val privateChatsUnlockedThisSession: Boolean = false,

    val blockedList: List<BlockedNumberUi> = emptyList(),

    val showDrawer: Boolean = false,
    val overflowMenuOpen: Boolean = false,
    val actionSheet: ActionSheetUi? = null,
    val otpModal: OtpModalUi? = null,
    val updateInfo: UpdateInfoUi? = null,

    val isDefaultSmsApp: Boolean = true,
)
