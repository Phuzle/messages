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
import com.phuzle.labs.messages.ui.components.SyncingScreen
import com.phuzle.labs.messages.ui.components.UndoBar
import com.phuzle.labs.messages.ui.components.DriveRestoreDialog
import com.phuzle.labs.messages.ui.components.UpdateAvailableDialog
import android.net.Uri
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

        BackHandler(enabled = state.undoMessage != null || state.updateInfo != null || state.driveRestoreAvailable || state.actionSheet != null || state.overflowMenuOpen || state.showDrawer || state.multiSelectThreadIds.isNotEmpty() || state.pushedScreen != null) {
            when {
                state.undoMessage != null -> viewModel.dismissUndo()
                state.updateInfo != null -> viewModel.dismissUpdate()
                state.driveRestoreAvailable -> viewModel.dismissDriveRestorePrompt()
                state.actionSheet != null -> viewModel.closeActionSheet()
                state.overflowMenuOpen -> viewModel.closeOverflowMenu()
                state.showDrawer -> viewModel.closeDrawer()
                state.multiSelectThreadIds.isNotEmpty() -> viewModel.exitMultiSelect()
                else -> viewModel.goBack()
            }
        }

        if (!state.settings.smsDisclosureAcknowledged) {
            com.phuzle.labs.messages.ui.onboarding.SmsDisclosureScreen(onContinue = viewModel::acknowledgeSmsDisclosure)
            return@MessagesTheme
        }

        if (state.isImportingHistory) {
            SyncingScreen(done = state.importDone, total = state.importTotal)
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
            }

            NavDrawer(
                visible = state.showDrawer,
                onDismiss = viewModel::closeDrawer,
                items = listOf(
                    DrawerItem("Inbox", DrawerIconType.Inbox, viewModel::openMessagesTab),
                    DrawerItem("Archived", DrawerIconType.Archived, viewModel::openArchivedScreen),
                    DrawerItem("Drafts", DrawerIconType.Drafts, viewModel::openDraftsScreen),
                    DrawerItem("Passbook", DrawerIconType.Passbook, viewModel::openPassbookTab),
                    DrawerItem("Reminders", DrawerIconType.Reminders, viewModel::openRemindersTab),
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
                                    "Messages — a smart SMS app with automatic categorization, OTP quick-copy, and a built-in passbook.\n\n$playStoreUrl",
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
                    MenuItem("Mark all as read", onClick = viewModel::markAllAsRead),
                    MenuItem(if (state.unreadOnly) "Show all messages" else "Show unread only", onClick = viewModel::toggleUnreadOnly),
                    MenuItem("Settings", onClick = viewModel::openSettings),
                ),
            )

            ActionSheet(
                sheet = state.actionSheet,
                onDismiss = viewModel::closeActionSheet,
                onMarkRead = viewModel::sheetMarkRead,
                onArchive = viewModel::sheetArchive,
                onTogglePrivate = viewModel::sheetTogglePrivate,
                onDelete = viewModel::sheetDelete,
            )

            OtpModal(otp = state.otpModal, onCopy = viewModel::copyOtpCode, onDismiss = viewModel::closeOtpModal)

            UpdateAvailableDialog(
                update = state.updateInfo,
                onUpdate = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).apply {
                        setPackage("com.android.vending")
                    }
                    context.startActivity(intent)
                    viewModel.dismissUpdate()
                },
                onDismiss = viewModel::dismissUpdate,
            )

            DriveRestoreDialog(
                visible = state.driveRestoreAvailable,
                onRestore = viewModel::confirmDriveRestore,
                onDismiss = viewModel::dismissDriveRestorePrompt,
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
