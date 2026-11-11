package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
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
import com.phuzle.labs.messages.ui.model.UpdateInfoUi
import com.phuzle.labs.messages.ui.theme.IconSizes
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

/** Shown once Play's own In-App Update flow finishes downloading a flexible update in the
 * background (see UpdateChecker) — Play never restarts the app on its own for that flow, so this
 * is the one prompt this app still owns in the update process. */
@Composable
fun UpdateAvailableDialog(update: UpdateInfoUi?, onUpdate: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val shown = rememberLast(update) ?: return
    val tokens = MessagesTheme.tokens
    AppDialogHost(
        visible = update != null,
        onDismiss = onDismiss,
        modifier = modifier,
        maxWidth = 300.dp,
        contentPadding = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
            Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = tokens.accentLabel, modifier = Modifier.size(IconSizes.Large).padding(bottom = 12.dp))
            Text("Update Ready", color = tokens.modalText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(
                shown.message,
                color = tokens.modalText.copy(alpha = 0.7f),
                fontSize = 13.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
            )
            DialogPrimaryButton("Restart Now", onUpdate)
            DialogSecondaryButton("Later", onDismiss)
    }
}
