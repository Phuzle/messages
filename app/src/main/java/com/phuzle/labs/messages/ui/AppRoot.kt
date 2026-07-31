package com.phuzle.labs.messages.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.archived.ArchivedScreen
import com.phuzle.labs.messages.ui.compose.ComposeScreen
import com.phuzle.labs.messages.ui.components.ActionSheet
import com.phuzle.labs.messages.ui.components.DrawerIconType
import com.phuzle.labs.messages.ui.components.DrawerItem
import com.phuzle.labs.messages.ui.components.MenuItem
import com.phuzle.labs.messages.ui.components.NavDrawer
import com.phuzle.labs.messages.ui.components.OtpModal
import com.phuzle.labs.messages.ui.components.OverflowMenu
import com.phuzle.labs.messages.ui.components.UndoBar
import com.phuzle.labs.messages.ui.components.UpdateAvailableDialog
import com.phuzle.labs.messages.ui.dashboard.DashboardScreen
import com.phuzle.labs.messages.ui.drafts.DraftsScreen
import com.phuzle.labs.messages.ui.model.PushedScreen
import com.phuzle.labs.messages.ui.model.SettingsSub
import com.phuzle.labs.messages.ui.passbook.AccountDetailScreen
import com.phuzle.labs.messages.ui.privatechats.PrivateChatsScreen
import com.phuzle.labs.messages.ui.recyclebin.RecycleBinScreen
import com.phuzle.labs.messages.ui.settings.SettingsScreen
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.thread.ThreadInfoScreen
import com.phuzle.labs.messages.ui.thread.ThreadScreen

@Composable
fun AppRoot(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }

    MessagesTheme(themeMode = state.themeMode, accentHex = state.settings.accentHex) {
        val tokens = MessagesTheme.tokens

        BackHandler(enabled = state.undoMessage != null || state.updateInfo != null || state.driveRestoreAvailable || state.driveSignInNeededForRestore || state.driveNoBackupFoundEmail != null || state.markAllReadConfirmThreadIds != null || state.scheduledMessageActionTarget != null || state.scheduledMessageEdit != null || state.actionSheet != null || state.overflowMenuOpen || state.showDrawer || state.multiSelectThreadIds.isNotEmpty() || state.threadSearchActive || state.composeToSuggestions.isNotEmpty() || state.pushedScreen != null || state.searchQuery.isNotEmpty()) {
            when {
                state.undoMessage != null -> viewModel.dismissUndo()
                state.updateInfo != null -> viewModel.dismissUpdate()
                state.driveRestoreAvailable -> viewModel.dismissDriveRestorePrompt()
                state.driveSignInNeededForRestore -> viewModel.skipDriveSignInForRestore()
                state.driveNoBackupFoundEmail != null -> viewModel.dismissNoBackupFound()
                state.markAllReadConfirmThreadIds != null -> viewModel.dismissMarkAllAsReadConfirm()
                state.scheduledMessageEdit != null -> viewModel.dismissScheduledMessageEdit()
                state.scheduledMessageActionTarget != null -> viewModel.closeScheduledMessageActions()
                state.actionSheet != null -> viewModel.closeActionSheet()
                state.overflowMenuOpen -> viewModel.closeOverflowMenu()
                state.showDrawer -> viewModel.closeDrawer()
                state.multiSelectThreadIds.isNotEmpty() -> viewModel.exitMultiSelect()
                // Checked before pushedScreen since thread search lives inside the (pushed)
                // Thread screen — back should close search there first, not pop the thread.
                state.threadSearchActive -> viewModel.closeThreadSearch()
                // The full-screen "Suggested contacts" picker on Compose (see ComposeScreen)
                // otherwise only goes away by picking a suggestion or clearing "To" by hand.
                state.composeToSuggestions.isNotEmpty() -> viewModel.dismissComposeToSuggestions()
                state.pushedScreen != null -> viewModel.goBack()
                // Dashboard search has no pushedScreen of its own (it's inline on the root
                // screen), so it needs its own fallback — otherwise back exits the app straight
                // from a search instead of just clearing it first, like any other list search.
                else -> viewModel.onSearchChange("")
            }
        }

        // Single parent for the whole startup sequence (disclosure -> sync -> Drive sign-in/
        // restore offer) — see StartupFlowScreen's doc comment for why this used to be four
        // separate gates that flickered through the dashboard in the gaps between them.
        // !settings.driveRestorePromptShown is what bridges those gaps: it stays false for the
        // entire span from launch until the Drive decision is truly finalized (found nothing,
        // skipped, or restored), covering every moment where none of the other conditions happen
        // to be true yet.
        val startupActive = !state.isDefaultSmsApp ||
            state.isImportingHistory ||
            state.driveSignInNeededForRestore ||
            state.driveRestoreAvailable ||
            state.driveRestoreInProgress ||
            state.driveNoBackupFoundEmail != null ||
            !state.settings.driveRestorePromptShown
        if (startupActive) {
            com.phuzle.labs.messages.ui.components.StartupFlowScreen(state = state, viewModel = viewModel)
            return@MessagesTheme
        }

        if (!state.appUnlockedThisSession) {
            com.phuzle.labs.messages.ui.components.BiometricGate(
                key = "app_lock",
                title = "Unlock Messages",
                subtitle = "Confirm it's you to open the app",
                onUnlocked = viewModel::unlockApp,
            ) { retry ->
                Box(Modifier.fillMaxSize().background(tokens.bg).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)) {
                        com.phuzle.labs.messages.ui.components.AppLogo(size = 56.dp)
                        Text("Messages is locked", color = tokens.textSecondary, fontSize = 14.sp)
                        Text(
                            "Unlock", color = tokens.accentText, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            modifier = Modifier
                                .background(tokens.accent, com.phuzle.labs.messages.ui.theme.ShapeMedium)
                                .clickable(onClick = retry)
                                .padding(horizontal = 20.dp, vertical = 11.dp),
                        )
                    }
                }
            }
            return@MessagesTheme
        }

        Box(Modifier.fillMaxSize().background(tokens.bg)) {
            when (state.pushedScreen) {
                null -> DashboardScreen(state, viewModel)
                PushedScreen.Thread -> ThreadScreen(state, viewModel)
                PushedScreen.ThreadInfo -> ThreadInfoScreen(state, viewModel)
                PushedScreen.Compose -> ComposeScreen(state, viewModel)
                PushedScreen.Settings -> SettingsScreen(state, viewModel)
                PushedScreen.RecycleBin -> RecycleBinScreen(state, viewModel)
                PushedScreen.Archived -> ArchivedScreen(state, viewModel)
                PushedScreen.PrivateChats -> PrivateChatsScreen(state, viewModel)
                PushedScreen.Drafts -> DraftsScreen(state, viewModel)
                PushedScreen.AccountDetail -> AccountDetailScreen(state, viewModel)
                PushedScreen.BackupList -> com.phuzle.labs.messages.ui.settings.BackupListScreen(viewModel)
                PushedScreen.ScheduledMessages -> com.phuzle.labs.messages.ui.scheduled.ScheduledMessagesScreen(viewModel)
            }

            NavDrawer(
                visible = state.showDrawer,
                onDismiss = viewModel::closeDrawer,
                items = listOfNotNull(
                    DrawerItem("Inbox", DrawerIconType.Inbox, viewModel::openMessagesTab),
                    DrawerItem("Archived", DrawerIconType.Archived, viewModel::openArchivedScreen),
                    DrawerItem("Drafts", DrawerIconType.Drafts, viewModel::openDraftsScreen),
                    if (com.phuzle.labs.messages.domain.model.FeatureFlags.PASSBOOK_AND_REMINDERS_ENABLED) {
                        DrawerItem("Passbook", DrawerIconType.Passbook, viewModel::openPassbookTab)
                    } else null,
                    if (com.phuzle.labs.messages.domain.model.FeatureFlags.PASSBOOK_AND_REMINDERS_ENABLED) {
                        DrawerItem("Reminders", DrawerIconType.Reminders, viewModel::openRemindersTab)
                    } else null,
                    DrawerItem("Scheduled Messages", DrawerIconType.ScheduledMessages, viewModel::openScheduledMessagesScreen),
                    DrawerItem("Private Chats", DrawerIconType.PrivateChats, viewModel::openPrivateChatsScreen),
                    DrawerItem("Settings", DrawerIconType.Settings, viewModel::openSettings),
                    DrawerItem("Recycle Bin", DrawerIconType.RecycleBin, viewModel::openRecycleBin),
                ),
                secondaryItems = listOf(
                    DrawerItem("About Us", DrawerIconType.AboutUs, viewModel::openAbout),
                    DrawerItem(
                        "Share",
                        DrawerIconType.Share,
                        {
                            // A plain description with no link left whoever received it with no
                            // way to actually get the app. Sharing the real Play Store listing
                            // means a tap on the shared message takes them straight there.
                            val playStoreUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Messages — a smart SMS app with automatic categorization and instant OTP quick-copy.\n\n$playStoreUrl",
                                )
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Messages"))
                        },
                    ),
                ),
            )

            OverflowMenu(
                visible = state.overflowMenuOpen,
                onDismiss = viewModel::closeOverflowMenu,
                items = listOf(
                    MenuItem("Mark all as read", icon = Icons.Filled.DoneAll, onClick = viewModel::requestMarkAllAsRead),
                    MenuItem(
                        if (state.unreadOnly) "Show all messages" else "Show unread only",
                        icon = if (state.unreadOnly) Icons.Filled.FilterAltOff else Icons.Filled.FilterAlt,
                        onClick = viewModel::toggleUnreadOnly,
                    ),
                    MenuItem("Settings", icon = Icons.Filled.Settings, onClick = viewModel::openSettings),
                ),
            )

            state.markAllReadConfirmThreadIds?.let { threadIds ->
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = viewModel::dismissMarkAllAsReadConfirm,
                    title = { Text("Mark all as read?") },
                    text = {
                        Text(
                            if (state.activeCategory == com.phuzle.labs.messages.domain.model.Category.All) {
                                "This marks all ${threadIds.size} conversation(s) in your inbox as read."
                            } else {
                                "This marks ${threadIds.size} conversation(s) in ${state.activeCategory.label} as read."
                            },
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = viewModel::confirmMarkAllAsRead) { Text("Mark as read") }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = viewModel::dismissMarkAllAsReadConfirm) { Text("Cancel") }
                    },
                )
            }

            ActionSheet(
                sheet = state.actionSheet,
                onDismiss = viewModel::closeActionSheet,
                onMarkRead = viewModel::sheetMarkRead,
                onArchive = viewModel::sheetArchive,
                onTogglePrivate = viewModel::sheetTogglePrivate,
                onDelete = viewModel::sheetDelete,
            )

            OtpModal(otp = state.otpModal, onCopy = viewModel::copyOtpCode, onDismiss = viewModel::closeOtpModal)

            // Shared by the thread view's scheduled-message bubbles and the Scheduled Messages
            // hub — same overlay, same ViewModel state, regardless of which screen is currently
            // pushed, so a long-press behaves identically from either place.
            com.phuzle.labs.messages.ui.components.ScheduledMessageActionSheet(
                visible = state.scheduledMessageActionTarget != null,
                onDismiss = viewModel::closeScheduledMessageActions,
                onEdit = viewModel::beginEditScheduledMessage,
                onDelete = viewModel::deleteScheduledMessage,
            )

            state.scheduledMessageEdit?.let { edit ->
                com.phuzle.labs.messages.ui.components.EditScheduledMessageDialog(
                    edit = edit,
                    onBodyChange = viewModel::updateScheduledMessageEditBody,
                    onTimeChange = viewModel::updateScheduledMessageEditTime,
                    onSave = viewModel::confirmScheduledMessageEdit,
                    onDismiss = viewModel::dismissScheduledMessageEdit,
                )
            }

            UpdateAvailableDialog(
                update = state.updateInfo,
                onUpdate = viewModel::completeInAppUpdate,
                onDismiss = viewModel::dismissUpdate,
            )

            UndoBar(
                message = state.undoMessage,
                onUndo = viewModel::confirmUndo,
                onDismiss = viewModel::dismissUndo,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
