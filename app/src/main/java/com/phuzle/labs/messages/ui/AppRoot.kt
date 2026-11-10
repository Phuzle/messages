package com.phuzle.labs.messages.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.lazy.rememberLazyListState
import com.phuzle.labs.messages.ui.dashboard.DashboardScrollState
import com.phuzle.labs.messages.ui.thread.ThreadScrollStore
import androidx.compose.runtime.remember
import com.phuzle.labs.messages.ui.components.LocalConsumedNavBarInset
import com.phuzle.labs.messages.ui.components.buttonNavBarHeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
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
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.model.PushedScreen
import com.phuzle.labs.messages.ui.model.SettingsSub
import com.phuzle.labs.messages.ui.passbook.AccountDetailScreen
import com.phuzle.labs.messages.ui.privatechats.PrivateChatsScreen
import com.phuzle.labs.messages.ui.recyclebin.RecycleBinScreen
import com.phuzle.labs.messages.ui.settings.SettingsScreen
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.thread.ThreadInfoScreen
import com.phuzle.labs.messages.ui.thread.ThreadScreen

private const val NAV_ENTER_MS = 240
private const val NAV_EXIT_MS = 160

/** How deep a screen sits in the push stack, so a transition knows whether it is going forward
 * (deeper) or back. Info is one level below the thread it describes. */
private fun navDepth(screen: PushedScreen?): Int = when (screen) {
    null -> 0
    PushedScreen.ThreadInfo -> 2
    else -> 1
}

@Composable
fun AppRoot(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }
    LaunchedEffect(Unit) {
        viewModel.shareEvents.collect { text ->
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            runCatching { context.startActivity(Intent.createChooser(send, null)) }
                .onFailure { Toast.makeText(context, "Nothing to share with", Toast.LENGTH_SHORT).show() }
        }
    }

    MessagesTheme(themeMode = state.themeMode, accentHex = state.settings.accentHex) {
        val tokens = MessagesTheme.tokens

        BackHandler(enabled = state.undoMessage != null || state.updateInfo != null || state.driveRestoreAvailable || state.driveSignInNeededForRestore || state.driveNoBackupFoundEmail != null || state.markAllReadConfirmThreadIds != null || state.scheduledMessageActionTarget != null || state.scheduledMessageEdit != null || state.actionSheet != null || state.overflowMenuOpen || state.searchFiltersOpen || state.selectedMessageIds.isNotEmpty() || state.showDrawer || state.multiSelectThreadIds.isNotEmpty() || state.threadSearchActive || state.composeToSuggestions.isNotEmpty() || state.pushedScreen != null || state.searchActive) {
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
                state.searchFiltersOpen -> viewModel.closeSearchFilters()
                state.selectedMessageIds.isNotEmpty() -> viewModel.exitMessageSelection()
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
                else -> viewModel.exitSearch()
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
                            "Unlock", color = tokens.onAccentDeep, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            modifier = Modifier
                                .background(tokens.accentDeep, com.phuzle.labs.messages.ui.theme.ShapeMedium)
                                .clickable(onClick = retry)
                                .padding(horizontal = 20.dp, vertical = 11.dp),
                        )
                    }
                }
            }
            return@MessagesTheme
        }

        // With 3-button navigation the nav bar's buttons sit directly over whatever is drawn
        // behind them, so scrolling content ran underneath them and the last rows could not be
        // reached. Reserve that strip as a solid surface and lay the screens out above it;
        // gesture navigation reports no tappable inset, so it keeps drawing edge-to-edge.
        val buttonNavHeight = buttonNavBarHeight()
        // Last state each screen was shown with — a screen that is animating out must keep
        // rendering what it showed (e.g. the thread whose id was just cleared by Back).
        val lastStateFor = remember { HashMap<PushedScreen?, AppUiState>() }
        // Scroll positions that must outlive their screen: a screen is disposed once something is
        // pushed over it, so state kept inside it was lost — opening a message from deep in the
        // list and coming back landed at the top; backing out of Contact info dropped you to the
        // bottom of the conversation instead of where you were reading.
        val inboxListState = rememberLazyListState()
        val resultsListState = rememberLazyListState()
        val dashboardScroll = remember { DashboardScrollState(inboxListState, resultsListState) }
        val threadScroll = remember { ThreadScrollStore() }
        LaunchedEffect(state.pushedScreen) {
            if (state.pushedScreen != PushedScreen.Thread && state.pushedScreen != PushedScreen.ThreadInfo) threadScroll.clear()
        }

        Box(Modifier.fillMaxSize().background(tokens.bg)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = buttonNavHeight)
                    .consumeWindowInsets(PaddingValues(bottom = buttonNavHeight)),
            ) {
                CompositionLocalProvider(LocalConsumedNavBarInset provides buttonNavHeight) {
                    AnimatedContent(
                        targetState = state.pushedScreen,
                        transitionSpec = {
                            val forward = navDepth(targetState) > navDepth(initialState)
                            val backward = navDepth(targetState) < navDepth(initialState)
                            // The deeper screen is opaque and travels on top, so only it slides over
                            // (or away from) its parent, which just drifts a little underneath —
                            // cross-fading the two would show both screens' text through each other.
                            val spec = when {
                                forward -> slideInHorizontally(tween(NAV_ENTER_MS, easing = FastOutSlowInEasing)) { it } togetherWith
                                    slideOutHorizontally(tween(NAV_ENTER_MS, easing = FastOutSlowInEasing)) { -it / 5 }
                                backward -> slideInHorizontally(tween(NAV_ENTER_MS, easing = FastOutSlowInEasing)) { -it / 5 } togetherWith
                                    slideOutHorizontally(tween(NAV_ENTER_MS, easing = FastOutSlowInEasing)) { it }
                                else -> fadeIn(tween(NAV_ENTER_MS)) togetherWith fadeOut(tween(NAV_EXIT_MS))
                            }
                            // The screen that is deeper in the stack always travels on top.
                            spec.apply { targetContentZIndex = if (backward) -1f else 1f }
                        },
                        label = "screen",
                    ) { screen ->
                        val shown = if (screen == state.pushedScreen) state.also { lastStateFor[screen] = it } else lastStateFor[screen] ?: state
                        Box(Modifier.fillMaxSize().background(tokens.bg)) {
                            when (screen) {
                                null -> DashboardScreen(shown, viewModel, dashboardScroll)
                                PushedScreen.Thread -> ThreadScreen(shown, viewModel, threadScroll)
                                PushedScreen.ThreadInfo -> ThreadInfoScreen(shown, viewModel)
                                PushedScreen.Compose -> ComposeScreen(shown, viewModel)
                                PushedScreen.Settings -> SettingsScreen(shown, viewModel)
                                PushedScreen.RecycleBin -> RecycleBinScreen(shown, viewModel)
                                PushedScreen.Archived -> ArchivedScreen(shown, viewModel)
                                PushedScreen.PrivateChats -> PrivateChatsScreen(shown, viewModel)
                                PushedScreen.Drafts -> DraftsScreen(shown, viewModel)
                                PushedScreen.AccountDetail -> AccountDetailScreen(shown, viewModel)
                                PushedScreen.BackupList -> com.phuzle.labs.messages.ui.settings.BackupListScreen(viewModel)
                                PushedScreen.ScheduledMessages -> com.phuzle.labs.messages.ui.scheduled.ScheduledMessagesScreen(viewModel)
                            }
                        }
                    }
                }
            }

            if (buttonNavHeight > 0.dp) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(buttonNavHeight).background(tokens.surface))
            }

            NavDrawer(
                visible = state.showDrawer,
                onDismiss = viewModel::closeDrawer,
                items = listOfNotNull(
                    DrawerItem("Inbox", DrawerIconType.Inbox, viewModel::openMessagesTab, selected = state.activeTab == com.phuzle.labs.messages.ui.model.DashboardTab.Messages),
                    DrawerItem("Archived", DrawerIconType.Archived, viewModel::openArchivedScreen),
                    DrawerItem("Drafts", DrawerIconType.Drafts, viewModel::openDraftsScreen),
                    if (com.phuzle.labs.messages.domain.model.FeatureFlags.PASSBOOK_AND_REMINDERS_ENABLED) {
                        DrawerItem("Passbook", DrawerIconType.Passbook, viewModel::openPassbookTab, selected = state.activeTab == com.phuzle.labs.messages.ui.model.DashboardTab.Passbook)
                    } else null,
                    if (com.phuzle.labs.messages.domain.model.FeatureFlags.PASSBOOK_AND_REMINDERS_ENABLED) {
                        DrawerItem("Reminders", DrawerIconType.Reminders, viewModel::openRemindersTab, selected = state.activeTab == com.phuzle.labs.messages.ui.model.DashboardTab.Reminders)
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
                    MenuItem("Settings", icon = Icons.Filled.Settings, onClick = viewModel::openSettings),
                ),
            )

            val markAllIds = state.markAllReadConfirmThreadIds
            com.phuzle.labs.messages.ui.components.AppDialog(
                visible = markAllIds != null,
                onDismiss = viewModel::dismissMarkAllAsReadConfirm,
                title = "Mark all as read?",
                message = markAllIds?.let { ids ->
                    if (state.activeCategory == com.phuzle.labs.messages.domain.model.Category.All) {
                        "This marks all ${ids.size} conversation(s) in your inbox as read."
                    } else {
                        "This marks ${ids.size} conversation(s) in ${state.activeCategory.label} as read."
                    }
                },
                confirmLabel = "Mark as read",
                onConfirm = viewModel::confirmMarkAllAsRead,
            )

            ActionSheet(
                sheet = state.actionSheet,
                onDismiss = viewModel::closeActionSheet,
                onMarkRead = viewModel::sheetMarkRead,
                onArchive = viewModel::sheetArchive,
                onTogglePrivate = viewModel::sheetTogglePrivate,
                onDelete = viewModel::sheetDelete,
                onShare = viewModel::shareSheetContact,
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
