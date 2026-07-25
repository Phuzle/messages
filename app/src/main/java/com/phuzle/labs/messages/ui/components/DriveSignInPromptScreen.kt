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
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** Startup step shown when silent Google Sign-In couldn't determine whether a Drive backup exists
 * (see AppViewModel.checkFirstLaunchDriveRestore) — most commonly right after this app's own data
 * was cleared, since that wipes the local session state silent sign-in relies on to skip
 * interaction. Offers the real, interactive sign-in instead of just giving up, still with Skip.
 *
 * Layout matches DESIGN.md's "Startup/status screens" pattern (same as SmsDisclosureScreen) —
 * centered header content, primary/secondary actions pinned to the bottom of the screen. */
@Composable
fun DriveSignInPromptScreen(onSignIn: () -> Unit, onSkip: () -> Unit) {
    val tokens = MessagesTheme.tokens
    Box(Modifier.fillMaxSize().background(tokens.bg)) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp).padding(horizontal = 28.dp).padding(top = 120.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconBadge(icon = Icons.Filled.CloudQueue, size = 56.dp)
            Text(
                "Check Google Drive for a backup?",
                color = tokens.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                "We couldn't tell without asking — sign in to check whether your Google account has a Messages backup to restore.",
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
            PrimaryButton(label = "Sign in to Google", onClick = onSignIn)
            SecondaryTextButton(label = "Skip", onClick = onSkip, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
