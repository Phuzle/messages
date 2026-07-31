package com.phuzle.labs.messages.ui.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.phuzle.labs.messages.domain.model.Category

enum class DashboardTab { Messages, Passbook, Reminders }

enum class PushedScreen { Thread, ThreadInfo, Compose, Settings, RecycleBin, Archived, PrivateChats, Drafts, AccountDetail, BackupList, ScheduledMessages }

enum class SettingsSub(val title: String) {
    Notifications("Notifications"),
    Appearance("Appearance"),
    Privacy("Privacy & Security"),
    Chats("Chats"),
    Backup("Backup & Restore"),
    Storage("Storage & Data"),
    About("About"),
}

enum class SwipeAction(val key: String, val label: String) {
    Archive("archive", "Archive"),
    Delete("delete", "Delete"),
    ToggleRead("toggleRead", "Toggle read"),
    None("none", "None");

    companion object {
        fun fromKey(key: String): SwipeAction = entries.firstOrNull { it.key == key } ?: Archive
    }
}

// Note: none of these carry resolved Color values for theme-dependent styling (chip fills, swipe
// panels, credit/debit tint, bubble colors...) — those depend on the live theme tokens, which are
// only available inside a @Composable via MessagesTheme.tokens, not in the ViewModel. Only
// genuinely theme-independent colors (the fixed avatar palette) are carried as Color here; the
// rest is exposed as plain enums/booleans/keys for the composables to resolve at render time.

data class CategoryChipUi(
    val category: Category,
    val label: String,
    val active: Boolean,
)

data class ThreadUi(
    val id: String,
    val sender: String,
    val displayName: String,
    val category: Category,
    val isBusiness: Boolean,
    val avatarColor: Color,
    val photoUri: String? = null,
    val initials: String,
    val preview: String,
    /** True when [preview] was sent by us — the row prefixes it with "You: ", same as every other
     * SMS app does for a conversation's own last message. */
    val previewIsOutgoing: Boolean = false,
    val timeLabel: String,
    val unread: Boolean,
    /** Real per-message unread count when known; falls back to 1 when [unread] is true but no
     * individual message is flagged unread (e.g. after a manual "mark as unread" swipe, which
     * has no specific message to un-read) so the badge still shows *something* rather than a
     * confusing blank. See AppViewModel's threadsSnapshot for how this is computed. */
    val unreadCount: Int,
    val nameWeight: FontWeight,
    /** Character indices within [displayName]/[preview] that matched the active fuzzy search
     * query — empty when there's no active search or the match came from elsewhere in the
     * thread's full history (nothing to visibly bold in that case). See FuzzyMatcher. */
    val displayNameMatch: Set<Int> = emptySet(),
    val previewMatch: Set<Int> = emptySet(),
)

data class MessageUi(
    val id: Long,
    val text: String,
    val timeLabel: String,
    val timestamp: Long,
    val isMine: Boolean,
    val isScheduled: Boolean,
    val detectedEntities: List<com.phuzle.labs.messages.domain.text.DetectedEntity> = emptyList(),
)

data class CurrentThreadUi(
    val id: String,
    val sender: String,
    val displayName: String,
    val category: Category,
    val isBusiness: Boolean,
    val avatarColor: Color,
    val photoUri: String? = null,
    val initials: String,
    val kindLabel: String,
    val channelName: String,
    val infoTitle: String,
    val isReplyable: Boolean,
    val isBlocked: Boolean,
    val firstContactLabel: String?,
)

data class AccountUi(
    val last4: String,
    val transactionCount: Int,
    val netLabel: String,
    val netIsCredit: Boolean,
    val selected: Boolean,
)

data class TransactionUi(
    val id: String,
    val merchant: String,
    val accountLabel: String,
    val timeLabel: String,
    val amountLabel: String,
    val isCredit: Boolean,
)

data class ReminderUi(val id: String, val title: String, val detail: String, val timeLabel: String)

data class DeletedThreadUi(
    val id: String,
    val sender: String,
    val initials: String,
    val avatarColor: Color,
    val isBusiness: Boolean,
    val category: Category,
    val preview: String,
)

data class BlockedNumberUi(val number: String)

data class PillOptionUi(val key: String, val label: String, val active: Boolean)

data class StorageOverviewUi(val chatCount: Int, val senderCount: Int, val messageCount: Int, val storageBytes: Long)

data class LocalBackupUi(val fileName: String, val timestampMillis: Long)
data class DriveBackupUi(val id: String, val name: String, val createdTime: String)
data class BackupListUiState(
    val loading: Boolean = false,
    val local: List<LocalBackupUi> = emptyList(),
    val drive: List<DriveBackupUi> = emptyList(),
    val driveConnected: Boolean = false,
    /** Key of whichever backup is currently being restored ("local:<fileName>" or "drive:<id>"),
     * so that one row can show a spinner and every row can be disabled meanwhile — restoring two
     * backups at once would race on the same live database. */
    val restoringKey: String? = null,
)

data class ContactSuggestionUi(val name: String, val number: String, val photoUri: String? = null)
data class DraftUi(val id: String, val to: String, val bodyPreview: String, val timeLabel: String)
data class MessageActionTargetUi(val id: Long, val text: String)

/** A single row in the Scheduled Messages hub — see AppViewModel.scheduledMessages. */
data class ScheduledMessageUi(
    val id: Long,
    val threadId: String,
    val threadDisplayName: String,
    val avatarColor: Color,
    val category: Category,
    val isBusiness: Boolean,
    val photoUri: String?,
    val body: String,
    val scheduledFor: Long,
    val scheduleLabel: String,
)

/** Which scheduled message the restricted (Edit/Delete-only) action sheet is currently showing
 * for — reachable from either the thread view's own long-press or the hub's, see
 * AppViewModel.openScheduledMessageActions. */
data class ScheduledMessageActionTargetUi(val id: Long, val threadId: String)

/** The in-flight edit of a scheduled message's body/time — non-null while EditScheduledMessageDialog
 * is showing. [body] is a live copy the dialog edits locally before Save commits it. */
data class ScheduledMessageEditUi(val messageId: Long, val threadId: String, val body: String, val scheduledFor: Long)
