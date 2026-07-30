package com.phuzle.labs.messages.ui.model

import com.phuzle.labs.messages.data.prefs.AppSettings
import com.phuzle.labs.messages.domain.model.Category
import com.phuzle.labs.messages.ui.theme.ThemeMode

data class SimOptionUi(val subscriptionId: Int, val label: String)
data class OtpModalUi(val senderLabel: String, val code: String, val copied: Boolean, val expiresAtMillis: Long)
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
    val unreadOnly: Boolean = false,
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

    val composeTo: String = "",
    val composeBody: String = "",
    val composeRecipients: List<ContactSuggestionUi> = emptyList(),
    val composeCustomScheduleMillis: Long? = null,
    val composeToSuggestions: List<ContactSuggestionUi> = emptyList(),
    /** Non-empty only on multi-SIM devices with READ_PHONE_STATE granted — drives Compose's SIM
     * picker (see ComposeScreen). Single-SIM devices never see this, matching how they never saw
     * any SIM-related UI before this feature existed. */
    val availableSims: List<SimOptionUi> = emptyList(),
    val composeSelectedSubscriptionId: Int? = null,
    val drafts: List<DraftUi> = emptyList(),
    val undoMessage: String? = null,

    val deletedThreads: List<DeletedThreadUi> = emptyList(),
    val archivedThreads: List<DeletedThreadUi> = emptyList(),
    val privateThreads: List<DeletedThreadUi> = emptyList(),
    val privateChatsUnlockedThisSession: Boolean = false,
    val appUnlockedThisSession: Boolean = true,

    val blockedList: List<BlockedNumberUi> = emptyList(),

    val showDrawer: Boolean = false,
    val overflowMenuOpen: Boolean = false,
    val actionSheet: ActionSheetUi? = null,
    val otpModal: OtpModalUi? = null,
    val updateInfo: UpdateInfoUi? = null,
    val threadOverflowMenuOpen: Boolean = false,
    val threadSearchActive: Boolean = false,
    val threadSearchQuery: String = "",
    val driveRestoreAvailable: Boolean = false,
    /** Silent sign-in couldn't determine whether a Drive backup exists (commonly a real Google
     * Play Services ApiException 4 / SIGN_IN_REQUIRED, not a bug — silent resolution for a scoped
     * permission like Drive is not guaranteed even for an account that consented before, especially
     * right after app data is cleared) — see AppViewModel.checkFirstLaunchDriveRestore. This offers
     * an explicit interactive sign-in instead of silently giving up, still with a Skip option. */
    val driveSignInNeededForRestore: Boolean = false,
    /** See AppViewModel's field of the same name — the actual restore-and-merge is in flight. */
    val driveRestoreInProgress: Boolean = false,
    /** See AppViewModel's field of the same name — the startup "is there a Drive backup?" round
     * trip is in flight, so StartupFlowScreen can name that step instead of showing its generic
     * gap-filler loader through what can be several seconds of network work. */
    val driveCheckInProgress: Boolean = false,
    val messageActionTarget: MessageActionTargetUi? = null,
    val multiSelectThreadIds: Set<String> = emptySet(),

    val isDefaultSmsApp: Boolean = true,
    val isImportingHistory: Boolean = false,
    val importDone: Int = 0,
    val importTotal: Int = 0,
    val historySyncSuccess: Boolean = false,
)
