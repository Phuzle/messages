package com.phuzle.labs.messages.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import com.phuzle.labs.messages.ui.archived.ArchivedScreen
import com.phuzle.labs.messages.ui.compose.ComposeScreen
import com.phuzle.labs.messages.ui.components.ActionSheet
import com.phuzle.labs.messages.ui.components.DrawerIconType
import com.phuzle.labs.messages.ui.components.DrawerItem
import com.phuzle.labs.messages.ui.components.MenuItem
import com.phuzle.labs.messages.ui.components.NavDrawer
import com.phuzle.labs.messages.ui.components.OtpModal
import com.phuzle.labs.messages.ui.components.OverflowMenu
import com.phuzle.labs.messages.ui.components.UpdateAvailableDialog
import android.net.Uri
import com.phuzle.labs.messages.ui.dashboard.DashboardScreen
import com.phuzle.labs.messages.ui.model.PushedScreen
import com.phuzle.labs.messages.ui.model.SettingsSub
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

    MessagesTheme(themeMode = state.themeMode, accentHex = state.settings.accentHex) {
        val tokens = MessagesTheme.tokens

        BackHandler(enabled = state.updateInfo != null || state.actionSheet != null || state.overflowMenuOpen || state.showDrawer || state.pushedScreen != null) {
            when {
                state.updateInfo != null -> viewModel.dismissUpdate()
                state.actionSheet != null -> viewModel.closeActionSheet()
                state.overflowMenuOpen -> viewModel.closeOverflowMenu()
                state.showDrawer -> viewModel.closeDrawer()
                else -> viewModel.goBack()
            }
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
            }

            NavDrawer(
                visible = state.showDrawer,
                onDismiss = viewModel::closeDrawer,
                items = listOf(
                    DrawerItem("Inbox", DrawerIconType.Inbox, viewModel::openMessagesTab),
                    DrawerItem("Archived", DrawerIconType.Archived, viewModel::openArchivedScreen),
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
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Messages — a smart SMS app with automatic categorization, OTP quick-copy, and a built-in passbook.",
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
                    MenuItem("Mark all as read", viewModel::markAllAsRead),
                    MenuItem("Simulate incoming OTP", viewModel::simulateOtp),
                    MenuItem("Settings", viewModel::openSettings),
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
        }
    }
}
