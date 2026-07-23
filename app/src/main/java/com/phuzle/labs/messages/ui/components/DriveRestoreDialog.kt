package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

/** First-launch-only prompt (see AppViewModel.checkFirstLaunchDriveRestore) — a Drive backup was
 * found via a silent sign-in before the user asked for anything, offering to merge it in. */
@Composable
fun DriveRestoreDialog(visible: Boolean, onRestore: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    if (!visible) return
    val tokens = MessagesTheme.tokens
    Box(
        modifier = modifier.fillMaxSize().background(tokens.overlayBg).padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 300.dp).background(tokens.modalBg, ShapeMedium).padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = tokens.accent, modifier = Modifier.padding(bottom = 12.dp))
            Text("Restore from Google Drive?", color = tokens.modalText, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                "We found a backup from your Google account. Restoring merges it in with anything already on this device — nothing local gets removed.",
                color = tokens.modalText.copy(alpha = 0.7f),
                fontSize = 13.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
            )
            Button(
                onClick = onRestore,
                colors = ButtonDefaults.buttonColors(containerColor = tokens.accent, contentColor = tokens.accentText),
                shape = ShapeMedium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Restore & Merge", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
            }
            TextButton(onClick = onDismiss) {
                Text("Not now", color = tokens.modalText.copy(alpha = 0.55f), fontSize = 13.sp)
            }
        }
    }
}
