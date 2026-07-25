package com.phuzle.labs.messages.ui.components

import androidx.compose.runtime.Composable
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.model.AppUiState

/**
 * The single parent for the whole startup sequence (disclosure -> sync -> Drive sign-in/restore
 * offer), replacing what used to be four independent early-return gates in AppRoot. Those gates
 * each covered their own condition correctly, but nothing covered the *gaps between* them: e.g.
 * the moment after history import finishes and before checkFirstLaunchDriveRestore has decided
 * anything, none of the four conditions were true, so AppRoot fell through and rendered the
 * dashboard for a frame or two before the next gate kicked in — a visible flicker through the
 * home screen on every launch, not an occasional glitch. AppRoot now shows this composable for the
 * *entire* span (see its own startupActive check, which includes settings.driveRestorePromptShown
 * specifically to bridge those gaps) and this `when` just picks which specific step to render;
 * the trailing `else` covers any such gap with a plain loader instead of ever revealing dashboard
 * content underneath it.
 */
@Composable
fun StartupFlowScreen(state: AppUiState, viewModel: AppViewModel) {
    when {
        // Covers both "never granted yet" (fresh install) and "granted once, then the user made a
        // different app default" identically — the app can't do anything useful without this role
        // either way.
        !state.isDefaultSmsApp -> com.phuzle.labs.messages.ui.onboarding.SmsDisclosureScreen(onContinue = viewModel::requestBecomeDefaultSmsApp)

        state.isImportingHistory -> SyncingScreen(
            done = state.importDone,
            total = state.importTotal,
            success = state.historySyncSuccess,
        )

        // Silent sign-in couldn't tell either way (see AppViewModel.checkFirstLaunchDriveRestore)
        // — offer the real interactive sign-in instead of silently giving up, still skippable.
        state.driveSignInNeededForRestore -> DriveSignInPromptScreen(
            onSignIn = viewModel::requestDriveSignInForRestore,
            onSkip = viewModel::skipDriveSignInForRestore,
        )

        state.driveRestoreAvailable -> DriveRestoreDialog(
            visible = true,
            onRestore = viewModel::confirmDriveRestore,
            onDismiss = viewModel::dismissDriveRestorePrompt,
        )

        // The actual download+merge is in flight (user tapped Restore & Merge) — stay on the
        // loader for its real duration instead of revealing the dashboard mid-merge.
        state.driveRestoreInProgress -> SyncingScreen(
            done = 0,
            total = 0,
            title = "Restoring your backup",
            subtitle = "Merging in messages from Google Drive…",
        )

        // Every condition above is false but settings.driveRestorePromptShown hasn't caught up
        // yet — one of the async gaps described in this file's doc comment. Never reachable for
        // more than a moment.
        else -> SyncingScreen(done = 0, total = 0, title = "Setting up Messages", subtitle = "Just a moment…")
    }
}
