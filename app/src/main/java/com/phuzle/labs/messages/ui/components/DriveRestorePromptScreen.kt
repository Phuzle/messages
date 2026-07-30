package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/**
 * Startup step shown when a Drive backup was found for the signed-in account (see
 * AppViewModel.checkFirstLaunchDriveRestore), offering to merge it into whatever is already on this
 * device.
 *
 * A full screen, not the floating dialog this used to be. StartupFlowScreen renders exactly one
 * step at a time with nothing behind it, so a scrim-over-content component had no content to sit
 * over: the translucent overlayBg fell straight through to the bare window, and the step rendered
 * as a small card adrift in a murky void — visibly broken next to the other startup steps, which
 * are all real screens. Same information, same two choices, DESIGN.md's startup pattern (centered
 * header block, actions pinned to the bottom) shared with SmsDisclosureScreen and
 * DriveSignInPromptScreen.
 */
@Composable
fun DriveRestorePromptScreen(onRestore: () -> Unit, onSkip: () -> Unit) {
    val tokens = MessagesTheme.tokens
    Box(Modifier.fillMaxSize().background(tokens.bg)) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp).padding(horizontal = 28.dp).padding(top = 120.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconBadge(icon = Icons.Filled.CloudDownload, size = 56.dp)
            Text(
                "Restore from Google Drive?",
                color = tokens.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                "We found a backup in your Google account. Restoring merges it in with anything already on this device — nothing local gets removed.",
                color = tokens.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(tokens.bg)
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 20.dp),
        ) {
            PrimaryButton(label = "Restore & Merge", onClick = onRestore)
            SecondaryTextButton(label = "Not now", onClick = onSkip, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
