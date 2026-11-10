package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.model.OtpModalUi
import com.phuzle.labs.messages.ui.theme.JetBrainsMonoFontFamily
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeLarge
import com.phuzle.labs.messages.ui.theme.ShapeMedium
import com.phuzle.labs.messages.ui.theme.ShapePill
import kotlinx.coroutines.delay

private const val HOT_SWAP_WINDOW_MS = 30_000L

/** The 30-second OTP hot-swap modal: pops up when the app resumes to a fresh OTP. The countdown
 * shown here is purely cosmetic — AppViewModel.checkOtpHotSwap is what actually clears otpModal
 * once the window expires, this just mirrors that deadline so the overlay doesn't sit there with
 * no indication it's about to (or already did) time out. */
@Composable
fun OtpModal(otp: OtpModalUi?, onCopy: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val shown = rememberLast(otp) ?: return
    val tokens = MessagesTheme.tokens

    var remainingMs by remember(shown.expiresAtMillis) {
        mutableLongStateOf((shown.expiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0))
    }
    LaunchedEffect(shown.expiresAtMillis) {
        while (remainingMs > 0) {
            delay(250)
            remainingMs = (shown.expiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
        }
    }

    AppDialogHost(
        visible = otp != null,
        onDismiss = onDismiss,
        modifier = modifier,
        maxWidth = 296.dp,
        contentPadding = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
            Text(
                shown.senderLabel,
                color = tokens.modalText.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            )
            SelectionContainer {
                Text(
                    shown.code,
                    color = tokens.modalText,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    modifier = Modifier.padding(top = 16.dp, bottom = 20.dp),
                )
            }
            // Flat button like PrimaryButton (DESIGN.md: no Material `Button`), on the modal's own accent pair.
            Text(
                if (shown.copied) "Copied ✓" else "Copy to Clipboard",
                color = tokens.onModalAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tokens.modalAccent, ShapeMedium)
                    .clip(ShapeMedium)
                    .clickable(onClick = onCopy)
                    .heightIn(min = 52.dp)
                    .wrapContentHeight(Alignment.CenterVertically),
            )
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Dismiss", color = tokens.modalText.copy(alpha = 0.6f), fontSize = 14.sp)
            }
            LinearProgressIndicator(
                progress = { (remainingMs.toFloat() / HOT_SWAP_WINDOW_MS).coerceIn(0f, 1f) },
                color = tokens.modalAccent,
                trackColor = tokens.modalText.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp).clip(ShapePill),
            )
            Text(
                "Auto-dismisses in ${(remainingMs / 1000).toInt() + 1}s",
                color = tokens.modalText.copy(alpha = 0.45f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
    }
}
